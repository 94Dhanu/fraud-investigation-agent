package com.fraudagent.domain.exception;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(Long caseId) {
        super("Fraud case not found: " + caseId);
    }
}
