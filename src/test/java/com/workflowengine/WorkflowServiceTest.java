package com.workflowengine;

import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.repository.ExecutionLogRepository;
import com.workflowengine.repository.WorkflowRepository;
import com.workflowengine.repository.WorkflowStepRepository;
import com.workflowengine.exception.StepNotFoundException;
import com.workflowengine.exception.WorkflowAlreadyRunningException;
import com.workflowengine.exception.WorkflowNotFoundException;
import com.workflowengine.service.AiSuggestionService;
import com.workflowengine.service.WorkflowExecutor;
import com.workflowengine.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowStepRepository stepRepository;
    @Mock ExecutionLogRepository logRepository;
    @Mock AiSuggestionService aiSuggestionService;
    @Mock WorkflowExecutor workflowExecutor;

    @InjectMocks WorkflowService workflowService;

    private Workflow sampleWorkflow;

    @BeforeEach
    void setUp() {
        sampleWorkflow = new Workflow();
        sampleWorkflow.setId(1L);
        sampleWorkflow.setName("Test Workflow");
        sampleWorkflow.setDescription("A test workflow");
        sampleWorkflow.setStatus(Workflow.WorkflowStatus.DRAFT);
        sampleWorkflow.setTriggerCount(0);
        sampleWorkflow.setSuccessCount(0);
    }

    @Test
    void createWorkflow_ShouldSetDraftStatus() {
        when(workflowRepository.save(any())).thenReturn(sampleWorkflow);
        Workflow result = workflowService.createWorkflow(sampleWorkflow);
        assertEquals(Workflow.WorkflowStatus.DRAFT, result.getStatus());
        verify(workflowRepository, times(1)).save(any());
    }

    @Test
    void getWorkflowById_ShouldReturnWorkflow() {
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(sampleWorkflow));
        Optional<Workflow> result = workflowService.getWorkflowById(1L);
        assertTrue(result.isPresent());
        assertEquals("Test Workflow", result.get().getName());
    }

    @Test
    void getWorkflowById_ShouldReturnEmptyForMissing() {
        when(workflowRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<Workflow> result = workflowService.getWorkflowById(99L);
        assertFalse(result.isPresent());
    }

    @Test
    void addStep_ShouldSetPendingStatus() {
        WorkflowStep step = new WorkflowStep();
        step.setName("Test Step");
        step.setType(WorkflowStep.StepType.APPROVAL);
        step.setStepOrder(1);

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(sampleWorkflow));
        when(stepRepository.save(any())).thenAnswer(inv -> {
            WorkflowStep s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        WorkflowStep result = workflowService.addStep(1L, step);
        assertEquals(WorkflowStep.StepStatus.PENDING, result.getStatus());
    }

    @Test
    void getAllWorkflows_ShouldReturnList() {
        when(workflowRepository.findAll()).thenReturn(List.of(sampleWorkflow));
        List<Workflow> result = workflowService.getAllWorkflows();
        assertEquals(1, result.size());
    }

    @Test
    void deleteWorkflow_ShouldCallRepositories() {
        when(workflowRepository.existsById(1L)).thenReturn(true);
        workflowService.deleteWorkflow(1L);
        verify(stepRepository).deleteByWorkflowId(1L);
        verify(workflowRepository).deleteById(1L);
    }

    @Test
    void updateWorkflow_ShouldUpdateFields() {
        Workflow updated = new Workflow();
        updated.setName("Updated Name");
        updated.setDescription("Updated Desc");
        updated.setStatus(Workflow.WorkflowStatus.ACTIVE);

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(sampleWorkflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Workflow result = workflowService.updateWorkflow(1L, updated);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Desc", result.getDescription());
    }

    @Test
    void updateWorkflow_KeepsStatusWhenOmitted() {
        Workflow updated = new Workflow();
        updated.setName("Renamed");
        updated.setStatus(null);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(sampleWorkflow));
        when(workflowRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(Workflow.WorkflowStatus.DRAFT, workflowService.updateWorkflow(1L, updated).getStatus());
    }

    @Test
    void updateWorkflow_MissingThrowsNotFound() {
        when(workflowRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(WorkflowNotFoundException.class, () -> workflowService.updateWorkflow(99L, new Workflow()));
    }

    @Test
    void deleteWorkflow_MissingThrowsNotFound() {
        when(workflowRepository.existsById(99L)).thenReturn(false);
        assertThrows(WorkflowNotFoundException.class, () -> workflowService.deleteWorkflow(99L));
        verify(stepRepository, never()).deleteByWorkflowId(any());
    }

    @Test
    void deleteStep_MissingThrowsNotFound() {
        when(stepRepository.existsById(5L)).thenReturn(false);
        assertThrows(StepNotFoundException.class, () -> workflowService.deleteStep(5L));
    }

    @Test
    void startExecution_MarksActiveCountsTriggerAndDispatches() {
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(sampleWorkflow));
        workflowService.startExecution(1L, "user");

        assertEquals(Workflow.WorkflowStatus.ACTIVE, sampleWorkflow.getStatus());
        assertEquals(1, sampleWorkflow.getTriggerCount());
        verify(workflowExecutor).execute(1L, "user");
    }

    @Test
    void startExecution_AlreadyRunningIsRejected() {
        sampleWorkflow.setStatus(Workflow.WorkflowStatus.ACTIVE);
        when(workflowRepository.findById(1L)).thenReturn(Optional.of(sampleWorkflow));

        assertThrows(WorkflowAlreadyRunningException.class, () -> workflowService.startExecution(1L, "user"));
        verify(workflowExecutor, never()).execute(any(), any());
    }

    @Test
    void startExecution_MissingWorkflowIsRejectedBeforeDispatch() {
        when(workflowRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(WorkflowNotFoundException.class, () -> workflowService.startExecution(99L, "user"));
        verify(workflowExecutor, never()).execute(any(), any());
    }
}
