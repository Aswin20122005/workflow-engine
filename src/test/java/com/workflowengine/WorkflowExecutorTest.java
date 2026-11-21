package com.workflowengine;

import com.workflowengine.model.ExecutionLog;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.repository.ExecutionLogRepository;
import com.workflowengine.repository.WorkflowRepository;
import com.workflowengine.repository.WorkflowStepRepository;
import com.workflowengine.service.AiSuggestionService;
import com.workflowengine.service.WorkflowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowExecutorTest {

    WorkflowRepository workflowRepository = mock(WorkflowRepository.class);
    WorkflowStepRepository stepRepository = mock(WorkflowStepRepository.class);
    ExecutionLogRepository logRepository = mock(ExecutionLogRepository.class);
    AiSuggestionService ai = mock(AiSuggestionService.class);

    Workflow workflow;

    /** Executor with no sleeping; a step named "boom" throws. */
    WorkflowExecutor executor = new WorkflowExecutor(workflowRepository, stepRepository, logRepository, ai) {
        @Override
        protected void simulateStepExecution(WorkflowStep step) {
            if ("boom".equals(step.getName())) {
                throw new IllegalStateException("vendor API down");
            }
        }
    };

    private WorkflowStep step(long id, String name, int order) {
        WorkflowStep s = new WorkflowStep();
        s.setId(id);
        s.setName(name);
        s.setStepOrder(order);
        s.setType(WorkflowStep.StepType.MANUAL_TASK);
        return s;
    }

    @BeforeEach
    void setUp() {
        workflow = new Workflow();
        workflow.setId(1L);
        workflow.setName("WF");
        workflow.setStatus(Workflow.WorkflowStatus.ACTIVE);
        workflow.setTriggerCount(1);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
        when(ai.getSuggestion(any(), any())).thenReturn("suggestion");
    }

    @Test
    void allStepsSucceed_workflowCompletedAndSuccessCounted() {
        List<WorkflowStep> steps = List.of(step(1, "a", 1), step(2, "b", 2));
        when(stepRepository.findByWorkflowIdOrderByStepOrder(1L)).thenReturn(steps);

        executor.run(1L, "test");

        assertEquals(Workflow.WorkflowStatus.COMPLETED, workflow.getStatus());
        assertEquals(1, workflow.getSuccessCount());
        assertTrue(steps.stream().allMatch(s -> s.getStatus() == WorkflowStep.StepStatus.COMPLETED));
        assertEquals("suggestion", workflow.getAiSuggestion());
    }

    @Test
    void failingStepStopsTheRunAndMarksWorkflowFailed() {
        WorkflowStep later = step(3, "c", 3);
        List<WorkflowStep> steps = List.of(step(1, "a", 1), step(2, "boom", 2), later);
        when(stepRepository.findByWorkflowIdOrderByStepOrder(1L)).thenReturn(steps);

        executor.run(1L, "test");

        assertEquals(Workflow.WorkflowStatus.FAILED, workflow.getStatus());
        assertEquals(0, workflow.getSuccessCount());
        assertEquals(WorkflowStep.StepStatus.FAILED, steps.get(1).getStatus());
        assertEquals("Error: vendor API down", steps.get(1).getExecutionNotes());
        assertEquals(WorkflowStep.StepStatus.PENDING, later.getStatus());
    }

    @Test
    void everyStepIsSavedBeforeAndAfterRunning() {
        when(stepRepository.findByWorkflowIdOrderByStepOrder(1L)).thenReturn(List.of(step(1, "a", 1)));
        executor.run(1L, "test");
        // IN_PROGRESS save + COMPLETED save, each committed separately
        verify(stepRepository, times(2)).save(any());
    }

    @Test
    void stepLogsCarryStepIdAndName() {
        when(stepRepository.findByWorkflowIdOrderByStepOrder(1L)).thenReturn(List.of(step(7, "a", 1)));
        executor.run(1L, "alice");

        ArgumentCaptor<ExecutionLog> logs = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository, atLeastOnce()).save(logs.capture());
        assertTrue(logs.getAllValues().stream()
            .anyMatch(l -> Long.valueOf(7L).equals(l.getStepId()) && "a".equals(l.getStepName())));
        assertTrue(logs.getAllValues().stream().allMatch(l -> "alice".equals(l.getTriggeredBy())));
    }
}
