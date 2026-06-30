package com.workflowengine.repository;

import com.workflowengine.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    List<Workflow> findByStatus(Workflow.WorkflowStatus status);

    List<Workflow> findByNameContainingIgnoreCase(String name);

    @Query("SELECT w FROM Workflow w ORDER BY w.triggerCount DESC")
    List<Workflow> findTopByUsage();

    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.status = :status")
    long countByStatus(Workflow.WorkflowStatus status);
}
