package com.workflowengine.controller;

import com.workflowengine.model.ExecutionLog;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.service.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // ─── WORKFLOW ENDPOINTS ───────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Workflow>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getWorkflow(@PathVariable Long id) {
        return workflowService.getWorkflowById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@Valid @RequestBody Workflow workflow) {
        return ResponseEntity.ok(workflowService.createWorkflow(workflow));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workflow> updateWorkflow(@PathVariable Long id,
                                                    @Valid @RequestBody Workflow workflow) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, workflow));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    // ─── STEP ENDPOINTS ───────────────────────────────────────────────────────

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<WorkflowStep>> getSteps(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getStepsByWorkflow(id));
    }

    @PostMapping("/{id}/steps")
    public ResponseEntity<WorkflowStep> addStep(@PathVariable Long id,
                                                 @Valid @RequestBody WorkflowStep step) {
        return ResponseEntity.ok(workflowService.addStep(id, step));
    }

    @DeleteMapping("/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(@PathVariable Long stepId) {
        workflowService.deleteStep(stepId);
        return ResponseEntity.noContent().build();
    }

    // ─── EXECUTION ENDPOINTS ──────────────────────────────────────────────────

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, String>> executeWorkflow(
            @PathVariable Long id,
            @RequestParam(defaultValue = "user") String triggeredBy) {
        workflowService.startExecution(id, triggeredBy);
        return ResponseEntity.accepted().body(Map.of(
            "status", "started",
            "message", "Workflow execution triggered successfully"
        ));
    }

    // ─── AI ENDPOINTS ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/suggest")
    public ResponseEntity<Map<String, String>> getSuggestion(@PathVariable Long id) {
        String suggestion = workflowService.refreshAiSuggestion(id);
        return ResponseEntity.ok(Map.of("suggestion", suggestion));
    }

    // ─── LOG ENDPOINTS ────────────────────────────────────────────────────────

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<ExecutionLog>> getLogs(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getLogsForWorkflow(id));
    }

    @GetMapping("/logs/recent")
    public ResponseEntity<List<ExecutionLog>> getRecentLogs() {
        return ResponseEntity.ok(workflowService.getRecentLogs());
    }

    // ─── STATS ENDPOINT ───────────────────────────────────────────────────────

    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(workflowService.getStats());
    }
}
