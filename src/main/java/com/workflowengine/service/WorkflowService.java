package com.workflowengine.service;

import com.workflowengine.exception.StepNotFoundException;
import com.workflowengine.exception.WorkflowAlreadyRunningException;
import com.workflowengine.exception.WorkflowNotFoundException;
import com.workflowengine.model.ExecutionLog;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.repository.ExecutionLogRepository;
import com.workflowengine.repository.WorkflowRepository;
import com.workflowengine.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final WorkflowExecutor workflowExecutor;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowStepRepository stepRepository,
                           ExecutionLogRepository logRepository,
                           AiSuggestionService aiSuggestionService,
                           WorkflowExecutor workflowExecutor) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
        this.logRepository = logRepository;
        this.aiSuggestionService = aiSuggestionService;
        this.workflowExecutor = workflowExecutor;
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
            // Status is optional in the request; don't wipe it when the client omits it.
            if (updated.getStatus() != null) {
                existing.setStatus(updated.getStatus());
            }
            return workflowRepository.save(existing);
        }).orElseThrow(() -> new WorkflowNotFoundException(id));
    }

    public void deleteWorkflow(Long id) {
        if (!workflowRepository.existsById(id)) {
            throw new WorkflowNotFoundException(id);
        }
        stepRepository.deleteByWorkflowId(id);
        workflowRepository.deleteById(id);
    }

    public List<WorkflowStep> getStepsByWorkflow(Long workflowId) {
        requireWorkflow(workflowId);
        return stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
    }

    public WorkflowStep addStep(Long workflowId, WorkflowStep step) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        step.setWorkflow(workflow);
        step.setStatus(WorkflowStep.StepStatus.PENDING);
        WorkflowStep saved = stepRepository.save(step);
        addLog(workflowId, workflow.getName(), saved.getId(), saved.getName(),
               ExecutionLog.LogLevel.INFO, "Step added: " + saved.getName(), "system");
        return saved;
    }

    public void deleteStep(Long stepId) {
        if (!stepRepository.existsById(stepId)) {
            throw new StepNotFoundException(stepId);
        }
        stepRepository.deleteById(stepId);
    }

    /**
     * Validate and start a run. Marks the workflow ACTIVE here (synchronously) so a
     * second request can't start a parallel run, then hands off to the async executor.
     * Throws WorkflowNotFoundException (404) or WorkflowAlreadyRunningException (409);
     * previously a missing id was only discovered on the background thread and the API
     * still answered "started".
     */
    public void startExecution(Long workflowId, String triggeredBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        if (workflow.getStatus() == Workflow.WorkflowStatus.ACTIVE) {
            throw new WorkflowAlreadyRunningException(workflowId);
        }
        workflow.setStatus(Workflow.WorkflowStatus.ACTIVE);
        workflow.setTriggerCount(workflow.getTriggerCount() + 1);
        workflowRepository.save(workflow);
        // Start only after this transaction commits, so the executor sees the ACTIVE status.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workflowExecutor.execute(workflowId, triggeredBy);
                }
            });
        } else {
            workflowExecutor.execute(workflowId, triggeredBy);
        }
    }

    public String refreshAiSuggestion(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrder(workflowId);
        String suggestion = aiSuggestionService.getSuggestion(workflow, steps);
        workflow.setAiSuggestion(suggestion);
        workflowRepository.save(workflow);
        return suggestion;
    }

    public List<ExecutionLog> getLogsForWorkflow(Long workflowId) {
        requireWorkflow(workflowId);
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

    private void requireWorkflow(Long workflowId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowNotFoundException(workflowId);
        }
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
