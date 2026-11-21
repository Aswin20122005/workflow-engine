package com.workflowengine.exception;

public class WorkflowAlreadyRunningException extends RuntimeException {
    public WorkflowAlreadyRunningException(Long id) {
        super("Workflow " + id + " is already running");
    }
}
