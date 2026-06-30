package com.workflowengine.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "workflows")
public class Workflow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WorkflowStatus status = WorkflowStatus.DRAFT;
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowStep> steps;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "trigger_count") private int triggerCount = 0;
    @Column(name = "success_count") private int successCount = 0;
    @Column(name = "ai_suggestion", columnDefinition = "TEXT") private String aiSuggestion;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
    public enum WorkflowStatus { DRAFT, ACTIVE, PAUSED, COMPLETED, FAILED }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public WorkflowStatus getStatus() { return status; } public void setStatus(WorkflowStatus s) { this.status = s; }
    public List<WorkflowStep> getSteps() { return steps; } public void setSteps(List<WorkflowStep> s) { this.steps = s; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
    public int getTriggerCount() { return triggerCount; } public void setTriggerCount(int n) { this.triggerCount = n; }
    public int getSuccessCount() { return successCount; } public void setSuccessCount(int n) { this.successCount = n; }
    public String getAiSuggestion() { return aiSuggestion; } public void setAiSuggestion(String s) { this.aiSuggestion = s; }
}
