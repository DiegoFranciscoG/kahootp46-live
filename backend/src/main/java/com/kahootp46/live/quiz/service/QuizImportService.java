package com.kahootp46.live.quiz.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kahootp46.live.quiz.QuestionType;
import com.kahootp46.live.quiz.dto.ChoiceRequest;
import com.kahootp46.live.quiz.dto.ImportedQuiz;
import com.kahootp46.live.quiz.dto.QuestionRequest;
import com.kahootp46.live.quiz.dto.QuizRequest;
import com.kahootp46.live.quiz.dto.QuizResponse;
import com.kahootp46.live.shared.error.InvalidInputException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Imports a quiz from JSON or CSV. Both formats are converted to a {@link QuizRequest} and go through the
 * same validation as quizzes created by form.
 */
@Service
public class QuizImportService {

    static final long MAX_BYTES = 256 * 1024;
    private static final List<String> CHOICE_COLUMNS =
            List.of("choice_a", "choice_b", "choice_c", "choice_d", "choice_e", "choice_f");
    private static final String LETTERS = "ABCDEF";
    private static final Set<String> JSON_TYPES = Set.of("application/json", "text/json");
    private static final Set<String> CSV_TYPES =
            Set.of("text/csv", "application/csv", "application/vnd.ms-excel", "text/plain");

    private final QuizService quizService;
    private final JsonMapper jsonMapper;

    QuizImportService(QuizService quizService, JsonMapper jsonMapper) {
        this.quizService = quizService;
        this.jsonMapper = jsonMapper;
    }

    public QuizResponse importFile(UUID ownerId, MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("El archivo está vacío");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidInputException("El archivo supera 256 KB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String content = readUtf8(file);
        QuizRequest request = switch (extension) {
            case "json" -> {
                requireType(contentType, JSON_TYPES);
                yield fromJson(content);
            }
            case "csv" -> {
                requireType(contentType, CSV_TYPES);
                yield fromCsv(content, title);
            }
            default -> throw new InvalidInputException("Formato no soportado: usa un archivo .json o .csv");
        };
        return quizService.create(ownerId, request);
    }

    public QuizRequest fromJson(String json) {
        ImportedQuiz imported;
        try {
            imported = jsonMapper.readValue(json, ImportedQuiz.class);
        } catch (JacksonException ex) {
            throw new InvalidInputException("JSON inválido: revisa la estructura del archivo");
        }
        if (imported == null || imported.questions() == null) {
            throw new InvalidInputException("El JSON debe tener 'title' y 'questions'");
        }
        List<QuestionRequest> questions = imported.questions().stream()
                .map(q -> q == null ? null : new QuestionRequest(q.question(), q.type(),
                        q.timeLimit() == null ? QuizRules.DEFAULT_TIME_LIMIT : q.timeLimit(), q.choices()))
                .toList();
        return new QuizRequest(imported.title(), imported.description(), questions);
    }

    /**
     * CSV (RFC 4180) with header {@code question,choice_a,choice_b,choice_c,choice_d,correct,time_limit};
     * {@code choice_c..choice_f} and {@code time_limit} are optional, {@code correct} is a letter A-F.
     */
    public QuizRequest fromCsv(String csv, String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidInputException("Para importar CSV indica el título del quiz");
        }
        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        List<QuestionRequest> questions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Reader reader = new StringReader(csv); CSVParser parser = CSVParser.parse(reader, format)) {
            List<String> header = parser.getHeaderNames().stream().map(h -> h.toLowerCase(Locale.ROOT)).toList();
            if (!header.containsAll(List.of("question", "choice_a", "choice_b", "correct"))) {
                throw new InvalidInputException("Cabecera CSV esperada: question,choice_a,choice_b,choice_c,choice_d,correct,time_limit");
            }
            for (CSVRecord row : parser) {
                questions.add(toQuestion(row, header, errors));
            }
        } catch (IOException | IllegalStateException | IllegalArgumentException ex) {
            throw new InvalidInputException("CSV inválido: " + ex.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new InvalidInputException(errors);
        }
        return new QuizRequest(title, null, questions);
    }

    private static QuestionRequest toQuestion(CSVRecord row, List<String> header, List<String> errors) {
        String line = "Fila " + row.getRecordNumber() + ": ";
        List<String> texts = new ArrayList<>();
        for (String column : CHOICE_COLUMNS) {
            if (header.contains(column) && row.isSet(column) && !row.get(column).isBlank()) {
                texts.add(row.get(column));
            }
        }
        String correct = row.isSet("correct") ? row.get("correct").toUpperCase(Locale.ROOT) : "";
        int correctIndex = correct.length() == 1 ? LETTERS.indexOf(correct.charAt(0)) : -1;
        if (correctIndex < 0 || correctIndex >= texts.size()) {
            errors.add(line + "'correct' debe ser una letra entre A y " + LETTERS.charAt(Math.max(texts.size() - 1, 0)));
        }
        Integer timeLimit = QuizRules.DEFAULT_TIME_LIMIT;
        if (header.contains("time_limit") && row.isSet("time_limit") && !row.get("time_limit").isBlank()) {
            try {
                timeLimit = Integer.valueOf(row.get("time_limit"));
            } catch (NumberFormatException ex) {
                errors.add(line + "'time_limit' debe ser un número de segundos");
            }
        }
        List<ChoiceRequest> choices = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            choices.add(new ChoiceRequest(texts.get(i), i == correctIndex));
        }
        QuestionType type = texts.size() == 2 && isTrueFalse(texts) ? QuestionType.TRUE_FALSE : QuestionType.SINGLE_CHOICE;
        String text = row.isSet("question") ? row.get("question") : "";
        return new QuestionRequest(text, type, timeLimit, choices);
    }

    private static boolean isTrueFalse(List<String> texts) {
        Set<String> values = Set.of(texts.get(0).toLowerCase(Locale.ROOT), texts.get(1).toLowerCase(Locale.ROOT));
        return values.equals(Set.of("verdadero", "falso")) || values.equals(Set.of("true", "false"));
    }

    private static void requireType(String contentType, Set<String> allowed) {
        String mediaType = contentType.split(";")[0].trim();
        if (!mediaType.isEmpty() && !allowed.contains(mediaType) && !"application/octet-stream".equals(mediaType)) {
            throw new InvalidInputException("Tipo de archivo no permitido: " + mediaType);
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String readUtf8(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] bytes = in.readNBytes((int) MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new InvalidInputException("El archivo supera 256 KB");
            }
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException ex) {
            throw new InvalidInputException("El archivo debe estar codificado en UTF-8");
        } catch (IOException ex) {
            throw new InvalidInputException("No se pudo leer el archivo");
        }
    }
}
