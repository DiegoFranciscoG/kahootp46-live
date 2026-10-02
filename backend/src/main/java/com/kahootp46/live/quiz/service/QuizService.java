package com.kahootp46.live.quiz.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kahootp46.live.quiz.PlayableQuiz;
import com.kahootp46.live.quiz.QuizCatalog;
import com.kahootp46.live.quiz.QuizUsageGuard;
import com.kahootp46.live.quiz.domain.Question;
import com.kahootp46.live.quiz.domain.Quiz;
import com.kahootp46.live.quiz.dto.ChoiceRequest;
import com.kahootp46.live.quiz.dto.QuestionRequest;
import com.kahootp46.live.quiz.dto.QuizRequest;
import com.kahootp46.live.quiz.dto.QuizResponse;
import com.kahootp46.live.quiz.dto.QuizSummaryResponse;
import com.kahootp46.live.quiz.mapper.QuizMapper;
import com.kahootp46.live.quiz.repository.QuizRepository;
import com.kahootp46.live.shared.error.BusinessRuleException;
import com.kahootp46.live.shared.error.ResourceNotFoundException;

@Service
@Transactional
public class QuizService implements QuizCatalog {

    private final QuizRepository quizzes;
    private final QuizRules rules;
    private final QuizUsageGuard usageGuard;
    private final Clock clock;

    QuizService(QuizRepository quizzes, QuizRules rules, QuizUsageGuard usageGuard, Clock clock) {
        this.quizzes = quizzes;
        this.rules = rules;
        this.usageGuard = usageGuard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> list(UUID ownerId) {
        return quizzes.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream()
                .map(QuizMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse get(UUID ownerId, UUID quizId) {
        return QuizMapper.toResponse(loadOwned(ownerId, quizId));
    }

    public QuizResponse create(UUID ownerId, QuizRequest request) {
        rules.validate(request);
        Quiz quiz = Quiz.create(ownerId, request.title(), request.description(), clock.instant());
        addQuestions(quiz, request.questions());
        return QuizMapper.toResponse(quizzes.save(quiz));
    }

    public QuizResponse update(UUID ownerId, UUID quizId, QuizRequest request) {
        rules.validate(request);
        Quiz quiz = loadOwned(ownerId, quizId);
        ensureNotPlaying(quizId);
        quiz.describe(request.title(), request.description(), clock.instant());
        quiz.clearQuestions();
        // Flush the orphan deletes first: Hibernate orders inserts before deletes, which would
        // collide with the (quiz_id, order_index) unique constraint.
        quizzes.saveAndFlush(quiz);
        addQuestions(quiz, request.questions());
        return QuizMapper.toResponse(quizzes.saveAndFlush(quiz));
    }

    public void delete(UUID ownerId, UUID quizId) {
        Quiz quiz = loadOwned(ownerId, quizId);
        ensureNotPlaying(quizId);
        quizzes.delete(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayableQuiz> findPlayable(UUID quizId) {
        return quizzes.findById(quizId).map(QuizMapper::toPlayable);
    }

    @Transactional(readOnly = true)
    public boolean hasQuizzes(UUID ownerId) {
        return quizzes.existsByOwnerId(ownerId);
    }

    private Quiz loadOwned(UUID ownerId, UUID quizId) {
        // Another owner's quiz answers 404 as well, so ids cannot be probed (OWASP API1).
        return quizzes.findById(quizId)
                .filter(quiz -> quiz.isOwnedBy(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("Quiz no encontrado"));
    }

    private void ensureNotPlaying(UUID quizId) {
        if (usageGuard.isInActiveGame(quizId)) {
            throw new BusinessRuleException("El quiz tiene una partida en curso; ciérrala antes de modificarlo.");
        }
    }

    private static void addQuestions(Quiz quiz, List<QuestionRequest> questions) {
        for (QuestionRequest request : questions) {
            Question question = quiz.addQuestion(request.text(), request.typeOrDefault(), request.timeLimit());
            for (ChoiceRequest choice : request.choices()) {
                question.addChoice(choice.text(), choice.correct());
            }
        }
    }
}
