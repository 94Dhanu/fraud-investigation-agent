package com.fraudagent.api.rest;

import com.fraudagent.domain.exception.CaseNotFoundException;
import com.fraudagent.domain.exception.IntakeValidationException;
import com.fraudagent.domain.exception.InvalidStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(Instant timestamp, int status, String error, List<String> details) {
    }

    @ExceptionHandler(IntakeValidationException.class)
    public ResponseEntity<ApiError> intakeValidation(IntakeValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.errors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> beanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<ApiError> notFound(CaseNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiError> badState(InvalidStateTransitionException ex) {
        return build(HttpStatus.CONFLICT, List.of(ex.getMessage()));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), details));
    }
}
