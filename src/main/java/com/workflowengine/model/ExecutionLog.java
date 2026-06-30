package com.workflowengine.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "execution_logs")
public class ExecutionLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "workflow_id", nullable = false) private Long workflowId;
    @Column(name = "workflow_name") private String workflowName;
    @Column(name = "step_id") private Long stepId;
    @Column(name = "step_name") private String stepName;
    @Enumerated(EnumType.STRING) private LogLevel level;
    @Column(columnDefinition = "TEXT") private String message;
    @Column(name = "triggered_by") private String triggeredBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
    public enum LogLevel { INFO, SUCCESS, WARNING, ERROR }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getWorkflowId() { return workflowId; } public void setWorkflowId(Long w) { this.workflowId = w; }
    public String getWorkflowName() { return workflowName; } public void setWorkflowName(String w) { this.workflowName = w; }
    public Long getStepId() { return stepId; } public void setStepId(Long s) { this.stepId = s; }
    public String getStepName() { return stepName; } public void setStepName(String s) { this.stepName = s; }
    public LogLevel getLevel() { return level; } public void setLevel(LogLevel l) { this.level = l; }
    public String getMessage() { return message; } public void setMessage(String m) { this.message = m; }
    public String getTriggeredBy() { return triggeredBy; } public void setTriggeredBy(String t) { this.triggeredBy = t; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
