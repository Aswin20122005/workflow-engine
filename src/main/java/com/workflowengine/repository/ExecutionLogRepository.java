package com.workflowengine.repository;

import com.workflowengine.model.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    List<ExecutionLog> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    List<ExecutionLog> findTop50ByOrderByCreatedAtDesc();
}
