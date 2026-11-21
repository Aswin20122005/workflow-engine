package com.workflowengine.exception;

public class StepNotFoundException extends RuntimeException {
    public StepNotFoundException(Long id) {
        super("Step not found: " + id);
    }
}
