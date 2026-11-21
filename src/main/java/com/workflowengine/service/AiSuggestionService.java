package com.workflowengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiSuggestionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.key:demo-mode}")
    private String apiKey;

    @Value("${ai.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${ai.api.model:claude-haiku-5-5}")
    private String model;

    public AiSuggestionService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generates an AI suggestion for the next best action in a workflow
     * based on the current state and execution history.
     */
    public String getSuggestion(Workflow workflow, List<WorkflowStep> steps) {
        // Demo mode: return intelligent mock suggestion without real API call
        if ("demo-mode".equals(apiKey)) {
            return buildDemoSuggestion(workflow, steps);
        }

        try {
            String prompt = buildPrompt(workflow, steps);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 300,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            String response = webClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("content").get(0).path("text").asText();

        } catch (Exception e) {
            return buildDemoSuggestion(workflow, steps);
        }
    }

    private String buildPrompt(Workflow workflow, List<WorkflowStep> steps) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a business process automation advisor.\n");
        sb.append("Workflow: ").append(workflow.getName()).append("\n");
        sb.append("Description: ").append(workflow.getDescription()).append("\n");
        sb.append("Current Status: ").append(workflow.getStatus()).append("\n");
        sb.append("Trigger Count: ").append(workflow.getTriggerCount()).append("\n");
        sb.append("Success Rate: ");

        if (workflow.getTriggerCount() > 0) {
            double rate = (double) workflow.getSuccessCount() / workflow.getTriggerCount() * 100;
            sb.append(String.format("%.1f%%", rate));
        } else {
            sb.append("N/A (no executions yet)");
        }

        sb.append("\nSteps:\n");
        for (WorkflowStep step : steps) {
            sb.append("  - ").append(step.getStepOrder()).append(". ")
              .append(step.getName())
              .append(" [").append(step.getType()).append("] ")
              .append("[").append(step.getStatus()).append("]\n");
        }

        sb.append("\nBased on the workflow state and history, suggest ONE specific, ");
        sb.append("actionable next step to optimize or improve this workflow. ");
        sb.append("Keep your response under 100 words and be specific.");

        return sb.toString();
    }

    private String buildDemoSuggestion(Workflow workflow, List<WorkflowStep> steps) {
        long pendingSteps = steps.stream()
            .filter(s -> s.getStatus() == WorkflowStep.StepStatus.PENDING)
            .count();

        long failedSteps = steps.stream()
            .filter(s -> s.getStatus() == WorkflowStep.StepStatus.FAILED)
            .count();

        if (failedSteps > 0) {
            return "⚠️ " + failedSteps + " step(s) have failed. Review error logs and consider adding retry logic or fallback handlers to improve resilience. Adding a NOTIFICATION step after failures can alert the team immediately.";
        }

        if (workflow.getTriggerCount() > 5 && workflow.getSuccessCount() < workflow.getTriggerCount() / 2) {
            return "📉 Success rate below 50%. Analyze the CONDITION_CHECK steps for edge cases. Consider adding a DATA_TRANSFORM step before critical validations to normalize inputs and reduce failures.";
        }

        if (steps.isEmpty()) {
            return "🧩 This workflow has no steps yet. Start by adding the first task, for example a MANUAL_TASK to collect inputs, then an APPROVAL step before anything irreversible.";
        }

        if (pendingSteps == steps.size()) {
            return "🚀 Workflow is ready to activate. Ensure all assignees are notified before activating. Consider adding a NOTIFICATION step at the start to broadcast workflow initiation to stakeholders.";
        }

        if (workflow.getTriggerCount() > 10) {
            return "📊 High-frequency workflow detected. Consider adding an AI_ANALYSIS step to auto-categorize incoming requests and route them to the appropriate team, reducing manual review time.";
        }

        return "✅ Workflow looks healthy. To further optimize, add an AI_ANALYSIS step after data collection to auto-prioritize tasks based on historical patterns and reduce manual decision-making bottlenecks.";
    }
}
