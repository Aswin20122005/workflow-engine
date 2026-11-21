package com.workflowengine.service;

import com.workflowengine.exception.WorkflowNotFoundException;
import com.workflowengine.model.ExecutionLog;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.repository.ExecutionLogRepository;
import com.workflowengine.repository.WorkflowRepository;
import com.workflowengine.repository.WorkflowStepRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs a workflow's steps in the background.
 *
 * Deliberately NOT @Transactional: every save commits on its own, so the UI can see
 * each step move to IN_PROGRESS and COMPLETED while the workflow is running. When this
 * lived in the class-level @Transactional WorkflowService, the whole run was one
 * transaction and nothing became visible until the end.
 */
@Service
public class WorkflowExecutor {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final ExecutionLogRepository logRepository;
    private final AiSuggestionService aiSuggestionService;

    public WorkflowExecutor(WorkflowRepository workflowRepository,
                            WorkflowStepRepository stepRepository,
                            ExecutionLogRepository logRepository,
                            AiSuggestionService aiSuggestionService) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
        this.logRepository = logRepository;
        this.aiSuggestionService = aiSuggestionService;
    }

    @Async
    public void execute(Long workflowId, String triggeredBy) {
        run(workflowId, triggeredBy);
    }

    /** Synchronous body of {@link #execute}, also used directly by tests. */
    public void run(Long workflowId, String triggeredBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        addLog(workflow, null, ExecutionLog.LogLevel.INFO, "Workflow execution started", triggeredBy);

        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
        boolean allSuccess = true;

        for (WorkflowStep step : steps) {
            try {
                step.setStatus(WorkflowStep.StepStatus.IN_PROGRESS);
                step.setExecutedAt(LocalDateTime.now());
                step.setCompletedAt(null);
                stepRepository.save(step);
                addLog(workflow, step, ExecutionLog.LogLevel.INFO, "Executing step: " + step.getName(), triggeredBy);

                simulateStepExecution(step);

                step.setStatus(WorkflowStep.StepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now());
                step.setExecutionNotes("Completed successfully");
                stepRepository.save(step);
                addLog(workflow, step, ExecutionLog.LogLevel.SUCCESS, "Step completed: " + step.getName(), triggeredBy);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markFailed(workflow, step, "Execution interrupted", triggeredBy);
                allSuccess = false;
                break;
            } catch (RuntimeException e) {
                markFailed(workflow, step, e.getMessage(), triggeredBy);
                allSuccess = false;
                break;
            }
        }

        if (allSuccess) {
            workflow.setStatus(Workflow.WorkflowStatus.COMPLETED);
            workflow.setSuccessCount(workflow.getSuccessCount() + 1);
            addLog(workflow, null, ExecutionLog.LogLevel.SUCCESS, "Workflow completed successfully", triggeredBy);
        } else {
            workflow.setStatus(Workflow.WorkflowStatus.FAILED);
            addLog(workflow, null, ExecutionLog.LogLevel.ERROR, "Workflow execution failed", triggeredBy);
        }

        List<WorkflowStep> updatedSteps = stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
        workflow.setAiSuggestion(aiSuggestionService.getSuggestion(workflow, updatedSteps));
        workflowRepository.save(workflow);
    }

    private void markFailed(Workflow workflow, WorkflowStep step, String reason, String triggeredBy) {
        step.setStatus(WorkflowStep.StepStatus.FAILED);
        step.setExecutionNotes("Error: " + reason);
        stepRepository.save(step);
        addLog(workflow, step, ExecutionLog.LogLevel.ERROR, "Step failed: " + reason, triggeredBy);
    }

    protected void simulateStepExecution(WorkflowStep step) throws InterruptedException {
        int delay = switch (step.getType()) {
            case APPROVAL -> 800;
            case EXTERNAL_API_CALL -> 600;
            case AI_ANALYSIS -> 1000;
            case DATA_TRANSFORM -> 400;
            default -> 300;
        };
        Thread.sleep(delay);
    }

    private void addLog(Workflow workflow, WorkflowStep step, ExecutionLog.LogLevel level,
                        String message, String triggeredBy) {
        ExecutionLog log = new ExecutionLog();
        log.setWorkflowId(workflow.getId());
        log.setWorkflowName(workflow.getName());
        if (step != null) {
            log.setStepId(step.getId());
            log.setStepName(step.getName());
        }
        log.setLevel(level);
        log.setMessage(message);
        log.setTriggeredBy(triggeredBy);
        logRepository.save(log);
    }
}
