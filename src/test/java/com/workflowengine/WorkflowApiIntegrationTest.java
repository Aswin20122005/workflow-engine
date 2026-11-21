package com.workflowengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Starts the whole application (H2, seeded data) and exercises the REST API. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private long createWorkflow(String name) throws Exception {
        String body = mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":\"test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private void addStep(long workflowId, String name, String type, int order) throws Exception {
        mvc.perform(post("/api/workflows/" + workflowId + "/steps").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"stepOrder\":" + order + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void seededWorkflowsAreListed() throws Exception {
        mvc.perform(get("/api/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.name == 'Employee Onboarding')]").exists());
    }

    @Test
    void unknownWorkflowReturns404WithMessage() throws Exception {
        mvc.perform(get("/api/workflows/99999"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/workflows/99999/steps"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Workflow not found: 99999"));
        mvc.perform(delete("/api/workflows/99999"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/workflows/99999/execute"))
            .andExpect(status().isNotFound());
    }

    @Test
    void unknownStepDeleteReturns404() throws Exception {
        mvc.perform(delete("/api/workflows/steps/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Step not found: 99999"));
    }

    @Test
    void blankWorkflowNameIsRejectedWithFieldError() throws Exception {
        mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void stepWithoutTypeOrWithZeroOrderIsRejected() throws Exception {
        long id = createWorkflow("Validation test");
        mvc.perform(post("/api/workflows/" + id + "/steps").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"No type\",\"stepOrder\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.type").exists());
        mvc.perform(post("/api/workflows/" + id + "/steps").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Zero\",\"type\":\"MANUAL_TASK\",\"stepOrder\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.stepOrder").exists());
    }

    @Test
    void updateWithoutStatusKeepsTheStatus() throws Exception {
        long id = createWorkflow("Update test");
        mvc.perform(put("/api/workflows/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\",\"description\":\"new\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void executeReturns202AndASecondRunIsRejectedWhileRunning() throws Exception {
        long id = createWorkflow("Execution test");
        addStep(id, "Slow approval", "APPROVAL", 1);
        addStep(id, "Analysis", "AI_ANALYSIS", 2);

        mvc.perform(post("/api/workflows/" + id + "/execute"))
            .andExpect(status().isAccepted());
        mvc.perform(post("/api/workflows/" + id + "/execute"))
            .andExpect(status().isConflict());

        // The run finishes in the background (~2 s of simulated work).
        JsonNode workflow = null;
        for (int i = 0; i < 50; i++) {
            Thread.sleep(200);
            String body = mvc.perform(get("/api/workflows/" + id)).andReturn().getResponse().getContentAsString();
            workflow = json.readTree(body);
            if ("COMPLETED".equals(workflow.get("status").asText())) break;
        }
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", workflow.get("status").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, workflow.get("successCount").asInt());
        org.junit.jupiter.api.Assertions.assertFalse(workflow.get("aiSuggestion").asText().isBlank());
    }

    @Test
    void statsSummaryCountsWorkflows() throws Exception {
        mvc.perform(get("/api/workflows/stats/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").isNumber());
    }
}
