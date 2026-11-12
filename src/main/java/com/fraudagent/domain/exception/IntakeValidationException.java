package com.fraudagent.domain.exception;

import java.util.List;

public class IntakeValidationException extends RuntimeException {

    private final List<String> errors;

    public IntakeValidationException(List<String> errors) {
        super("Intake validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
