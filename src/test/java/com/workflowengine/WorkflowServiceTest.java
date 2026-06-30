package com.workflowengine;

import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.repository.ExecutionLogRepository;
import com.workflowengine.repository.WorkflowRepository;
import com.workflowengine.repository.WorkflowStepRepository;
import com.workflowengine.service.AiSuggestionService;
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
}
