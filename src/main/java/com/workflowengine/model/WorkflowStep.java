package com.workflowengine.model;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;
    @NotBlank @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StepType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StepStatus status = StepStatus.PENDING;
    @Column(name = "step_order", nullable = false) private int stepOrder;
    @Column(name = "assignee") private String assignee;
    @Column(name = "config_json", columnDefinition = "TEXT") private String configJson;
    @Column(name = "executed_at") private LocalDateTime executedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "execution_notes", columnDefinition = "TEXT") private String executionNotes;
    public enum StepType { APPROVAL, NOTIFICATION, DATA_TRANSFORM, CONDITION_CHECK, EXTERNAL_API_CALL, MANUAL_TASK, AI_ANALYSIS }
    public enum StepStatus { PENDING, IN_PROGRESS, COMPLETED, SKIPPED, FAILED }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Workflow getWorkflow() { return workflow; } public void setWorkflow(Workflow w) { this.workflow = w; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public StepType getType() { return type; } public void setType(StepType t) { this.type = t; }
    public StepStatus getStatus() { return status; } public void setStatus(StepStatus s) { this.status = s; }
    public int getStepOrder() { return stepOrder; } public void setStepOrder(int n) { this.stepOrder = n; }
    public String getAssignee() { return assignee; } public void setAssignee(String a) { this.assignee = a; }
    public String getConfigJson() { return configJson; } public void setConfigJson(String c) { this.configJson = c; }
    public LocalDateTime getExecutedAt() { return executedAt; } public void setExecutedAt(LocalDateTime t) { this.executedAt = t; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime t) { this.completedAt = t; }
    public String getExecutionNotes() { return executionNotes; } public void setExecutionNotes(String n) { this.executionNotes = n; }
}
