package com.kahootp46.live.shared.error;

import java.util.List;

/**
 * Semantic validation errors that Bean Validation cannot express (HTTP 400).
 */
public class InvalidInputException extends RuntimeException {

    private final List<String> errors;

    public InvalidInputException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public InvalidInputException(String error) {
        this(List.of(error));
    }

    public List<String> getErrors() {
        return errors;
    }
}
