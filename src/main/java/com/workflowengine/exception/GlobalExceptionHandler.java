package com.workflowengine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Turns domain exceptions into JSON error responses with the right status code. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({WorkflowNotFoundException.class, StepNotFoundException.class})
    public ResponseEntity<Map<String, String>> notFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
