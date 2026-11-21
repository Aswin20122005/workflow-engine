package com.workflowengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.service.AiSuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Demo mode (no API key) — the built-in, rule-based suggestions. */
class AiSuggestionServiceTest {

    private final AiSuggestionService service = new AiSuggestionService(RestClient.builder(), new ObjectMapper());

    {
        ReflectionTestUtils.setField(service, "apiKey", "demo-mode");
    }

    private Workflow workflow(int triggers, int successes) {
        Workflow w = new Workflow();
        w.setName("WF");
        w.setTriggerCount(triggers);
        w.setSuccessCount(successes);
        return w;
    }

    private WorkflowStep step(WorkflowStep.StepStatus status) {
        WorkflowStep s = new WorkflowStep();
        s.setName("s");
        s.setType(WorkflowStep.StepType.MANUAL_TASK);
        s.setStatus(status);
        return s;
    }

    @Test
    void emptyWorkflowIsToldToAddSteps() {
        String s = service.getSuggestion(workflow(0, 0), List.of());
        assertTrue(s.contains("no steps"), s);
    }

    @Test
    void failedStepsAreMentionedFirst() {
        String s = service.getSuggestion(workflow(3, 3),
            List.of(step(WorkflowStep.StepStatus.FAILED), step(WorkflowStep.StepStatus.COMPLETED)));
        assertTrue(s.contains("1 step(s) have failed"), s);
    }

    @Test
    void lowSuccessRateIsFlagged() {
        String s = service.getSuggestion(workflow(10, 2), List.of(step(WorkflowStep.StepStatus.COMPLETED)));
        assertTrue(s.contains("below 50%"), s);
    }

    @Test
    void neverRunWorkflowIsReadyToActivate() {
        String s = service.getSuggestion(workflow(0, 0), List.of(step(WorkflowStep.StepStatus.PENDING)));
        assertTrue(s.contains("ready to activate"), s);
    }

    @Test
    void apiFailureFallsBackToBuiltInSuggestion() {
        ReflectionTestUtils.setField(service, "apiKey", "not-a-real-key");
        ReflectionTestUtils.setField(service, "apiUrl", "http://127.0.0.1:9/unreachable");
        ReflectionTestUtils.setField(service, "model", "test-model");
        String s = service.getSuggestion(workflow(0, 0), List.of());
        assertTrue(s.contains("no steps"), s);
    }
}
