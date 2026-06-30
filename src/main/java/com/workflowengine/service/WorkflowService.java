package com.workflowengine.service;

import com.workflowengine.model.ExecutionLog;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.repository.ExecutionLogRepository;
import com.workflowengine.repository.WorkflowRepository;
import com.workflowengine.repository.WorkflowStepRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final ExecutionLogRepository logRepository;
    private final AiSuggestionService aiSuggestionService;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowStepRepository stepRepository,
                           ExecutionLogRepository logRepository,
                           AiSuggestionService aiSuggestionService) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
        this.logRepository = logRepository;
        this.aiSuggestionService = aiSuggestionService;
    }

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    public Optional<Workflow> getWorkflowById(Long id) {
        return workflowRepository.findById(id);
    }

    public Workflow createWorkflow(Workflow workflow) {
        workflow.setStatus(Workflow.WorkflowStatus.DRAFT);
        Workflow saved = workflowRepository.save(workflow);
        addLog(saved.getId(), saved.getName(), null, null,
               ExecutionLog.LogLevel.INFO, "Workflow created: " + saved.getName(), "system");
        return saved;
    }

    public Workflow updateWorkflow(Long id, Workflow updated) {
        return workflowRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            existing.setStatus(updated.getStatus());
            return workflowRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
    }

    public void deleteWorkflow(Long id) {
        stepRepository.deleteByWorkflowId(id);
        workflowRepository.deleteById(id);
    }

    public List<WorkflowStep> getStepsByWorkflow(Long workflowId) {
        return stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
    }

    public WorkflowStep addStep(Long workflowId, WorkflowStep step) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
        step.setWorkflow(workflow);
        step.setStatus(WorkflowStep.StepStatus.PENDING);
        WorkflowStep saved = stepRepository.save(step);
        addLog(workflowId, workflow.getName(), saved.getId(), saved.getName(),
               ExecutionLog.LogLevel.INFO, "Step added: " + saved.getName(), "system");
        return saved;
    }

    public void deleteStep(Long stepId) {
        stepRepository.deleteById(stepId);
    }

    @Async
    public void executeWorkflow(Long workflowId, String triggeredBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));

        workflow.setStatus(Workflow.WorkflowStatus.ACTIVE);
        workflow.setTriggerCount(workflow.getTriggerCount() + 1);
        workflowRepository.save(workflow);

        addLog(workflowId, workflow.getName(), null, null,
               ExecutionLog.LogLevel.INFO, "Workflow execution started", triggeredBy);

        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
        boolean allSuccess = true;

        for (WorkflowStep step : steps) {
            try {
                step.setStatus(WorkflowStep.StepStatus.IN_PROGRESS);
                step.setExecutedAt(LocalDateTime.now());
                stepRepository.save(step);

                addLog(workflowId, workflow.getName(), step.getId(), step.getName(),
                       ExecutionLog.LogLevel.INFO, "Executing step: " + step.getName(), triggeredBy);

                simulateStepExecution(step);

                step.setStatus(WorkflowStep.StepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now());
                step.setExecutionNotes("Completed successfully");
                stepRepository.save(step);

                addLog(workflowId, workflow.getName(), step.getId(), step.getName(),
                       ExecutionLog.LogLevel.SUCCESS, "Step completed: " + step.getName(), triggeredBy);

            } catch (Exception e) {
                step.setStatus(WorkflowStep.StepStatus.FAILED);
                step.setExecutionNotes("Error: " + e.getMessage());
                stepRepository.save(step);

                addLog(workflowId, workflow.getName(), step.getId(), step.getName(),
                       ExecutionLog.LogLevel.ERROR, "Step failed: " + e.getMessage(), triggeredBy);
                allSuccess = false;
                break;
            }
        }

        if (allSuccess) {
            workflow.setStatus(Workflow.WorkflowStatus.COMPLETED);
            workflow.setSuccessCount(workflow.getSuccessCount() + 1);
            addLog(workflowId, workflow.getName(), null, null,
                   ExecutionLog.LogLevel.SUCCESS, "Workflow completed successfully", triggeredBy);
        } else {
            workflow.setStatus(Workflow.WorkflowStatus.FAILED);
            addLog(workflowId, workflow.getName(), null, null,
                   ExecutionLog.LogLevel.ERROR, "Workflow execution failed", triggeredBy);
        }

        List<WorkflowStep> updatedSteps = stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
        String suggestion = aiSuggestionService.getSuggestion(workflow, updatedSteps);
        workflow.setAiSuggestion(suggestion);
        workflowRepository.save(workflow);
    }

    private void simulateStepExecution(WorkflowStep step) throws InterruptedException {
        int delay = switch (step.getType()) {
            case APPROVAL -> 800;
            case EXTERNAL_API_CALL -> 600;
            case AI_ANALYSIS -> 1000;
            case DATA_TRANSFORM -> 400;
            default -> 300;
        };
        Thread.sleep(delay);
    }

    public String refreshAiSuggestion(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
        String suggestion = aiSuggestionService.getSuggestion(workflow, steps);
        workflow.setAiSuggestion(suggestion);
        workflowRepository.save(workflow);
        return suggestion;
    }

    public List<ExecutionLog> getLogsForWorkflow(Long workflowId) {
        return logRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    public List<ExecutionLog> getRecentLogs() {
        return logRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public Map<String, Long> getStats() {
        return Map.of(
            "total", workflowRepository.count(),
            "active", workflowRepository.countByStatus(Workflow.WorkflowStatus.ACTIVE),
            "completed", workflowRepository.countByStatus(Workflow.WorkflowStatus.COMPLETED),
            "failed", workflowRepository.countByStatus(Workflow.WorkflowStatus.FAILED),
            "draft", workflowRepository.countByStatus(Workflow.WorkflowStatus.DRAFT)
        );
    }

    private void addLog(Long workflowId, String workflowName, Long stepId, String stepName,
                        ExecutionLog.LogLevel level, String message, String triggeredBy) {
        ExecutionLog log = new ExecutionLog();
        log.setWorkflowId(workflowId);
        log.setWorkflowName(workflowName);
        log.setStepId(stepId);
        log.setStepName(stepName);
        log.setLevel(level);
        log.setMessage(message);
        log.setTriggeredBy(triggeredBy);
        logRepository.save(log);
    }
}
