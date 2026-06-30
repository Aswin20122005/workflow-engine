package com.workflowengine.config;

import com.workflowengine.model.Workflow;
import com.workflowengine.model.WorkflowStep;
import com.workflowengine.service.WorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final WorkflowService workflowService;

    public DataSeeder(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public void run(String... args) {
        // Only seed if no workflows exist
        if (!workflowService.getAllWorkflows().isEmpty()) return;

        seedEmployeeOnboarding();
        seedInvoiceApproval();
        seedCustomerSupportEscalation();
    }

    private void seedEmployeeOnboarding() {
        Workflow wf = new Workflow();
        wf.setName("Employee Onboarding");
        wf.setDescription("End-to-end new hire onboarding: IT setup, HR docs, and team introduction.");
        Workflow saved = workflowService.createWorkflow(wf);

        addStep(saved.getId(), 1, "Collect Personal Details", WorkflowStep.StepType.MANUAL_TASK,
                "HR collects personal info and emergency contacts", "hr-team");
        addStep(saved.getId(), 2, "Background Verification", WorkflowStep.StepType.EXTERNAL_API_CALL,
                "API call to background check provider", "hr-team");
        addStep(saved.getId(), 3, "Approval by Manager", WorkflowStep.StepType.APPROVAL,
                "Hiring manager approves final onboarding", "manager");
        addStep(saved.getId(), 4, "IT Equipment Setup", WorkflowStep.StepType.MANUAL_TASK,
                "Laptop, email, and system access provisioning", "it-team");
        addStep(saved.getId(), 5, "Welcome Notification", WorkflowStep.StepType.NOTIFICATION,
                "Send welcome email to new hire and team", "system");
        addStep(saved.getId(), 6, "AI Role Recommendation", WorkflowStep.StepType.AI_ANALYSIS,
                "AI suggests training path based on role and skills", "ai-engine");
    }

    private void seedInvoiceApproval() {
        Workflow wf = new Workflow();
        wf.setName("Invoice Approval Pipeline");
        wf.setDescription("Automated multi-level invoice approval with fraud detection and audit trail.");
        Workflow saved = workflowService.createWorkflow(wf);

        addStep(saved.getId(), 1, "Invoice Data Extraction", WorkflowStep.StepType.DATA_TRANSFORM,
                "Parse and normalize invoice fields from PDF/email", "system");
        addStep(saved.getId(), 2, "Fraud Risk Analysis", WorkflowStep.StepType.AI_ANALYSIS,
                "AI model checks for duplicate or suspicious invoices", "ai-engine");
        addStep(saved.getId(), 3, "Amount Threshold Check", WorkflowStep.StepType.CONDITION_CHECK,
                "Route to manager if amount > ₹50,000", "system");
        addStep(saved.getId(), 4, "Finance Manager Approval", WorkflowStep.StepType.APPROVAL,
                "Finance manager reviews and approves", "finance-team");
        addStep(saved.getId(), 5, "ERP System Update", WorkflowStep.StepType.EXTERNAL_API_CALL,
                "Update accounting records via ERP REST API", "system");
        addStep(saved.getId(), 6, "Payment Notification", WorkflowStep.StepType.NOTIFICATION,
                "Notify vendor of approval and expected payment date", "system");
    }

    private void seedCustomerSupportEscalation() {
        Workflow wf = new Workflow();
        wf.setName("Customer Support Escalation");
        wf.setDescription("AI-powered ticket triage and multi-tier escalation for customer support.");
        Workflow saved = workflowService.createWorkflow(wf);

        addStep(saved.getId(), 1, "Ticket Intake", WorkflowStep.StepType.MANUAL_TASK,
                "Customer submits support ticket via portal", "customer");
        addStep(saved.getId(), 2, "AI Severity Classification", WorkflowStep.StepType.AI_ANALYSIS,
                "AI classifies ticket as Low / Medium / High / Critical", "ai-engine");
        addStep(saved.getId(), 3, "SLA Breach Check", WorkflowStep.StepType.CONDITION_CHECK,
                "Flag if response time exceeds SLA threshold", "system");
        addStep(saved.getId(), 4, "L1 Agent Assignment", WorkflowStep.StepType.APPROVAL,
                "Assign to available L1 support agent", "support-team");
        addStep(saved.getId(), 5, "Resolution Update", WorkflowStep.StepType.EXTERNAL_API_CALL,
                "Post resolution to CRM and customer portal", "system");
        addStep(saved.getId(), 6, "CSAT Survey", WorkflowStep.StepType.NOTIFICATION,
                "Send customer satisfaction survey via email", "system");
    }

    private void addStep(Long workflowId, int order, String name,
                          WorkflowStep.StepType type, String description, String assignee) {
        WorkflowStep step = new WorkflowStep();
        step.setName(name);
        step.setDescription(description);
        step.setType(type);
        step.setStepOrder(order);
        step.setAssignee(assignee);
        workflowService.addStep(workflowId, step);
    }
}
