# ⚡ FlowMind — AI-Powered Workflow Automation Engine

A full-stack **Java Spring Boot** application that lets users design, execute, and optimize multi-step business workflows with an **AI-driven suggestion engine**. Built to demonstrate enterprise-grade backend development and intelligent automation — directly aligned with low-code/no-code platforms like Joget.

---

## 🎯 Why This Project

This project mirrors the core concepts behind business process automation platforms:
- **Workflow modeling** — define steps, types, assignees, and execution order
- **Runtime execution** — async step-by-step processing with real-time status updates
- **AI advisory layer** — context-aware suggestions to optimize workflow design
- **Audit logging** — full execution history for compliance and debugging

---

## 🛠️ Tech Stack

| Layer       | Technology                              |
|-------------|------------------------------------------|
| Backend     | Java 17, Spring Boot 3.2, Spring Data JPA|
| Database    | H2 (dev), MySQL (prod)                   |
| AI Layer    | Anthropic Claude API (demo mode included)|
| Frontend    | HTML5, CSS3, Vanilla JS                  |
| Testing     | JUnit 5, Mockito                         |
| DevOps      | Docker, GitHub Actions CI/CD             |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run locally (demo mode — no API key needed)
```bash
git clone https://github.com/<your-username>/workflow-engine.git
cd workflow-engine
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

### With real AI suggestions (optional)
```bash
export AI_API_KEY=your_anthropic_api_key
mvn spring-boot:run
```

### Run with Docker
```bash
docker build -t workflow-engine .
docker run -p 8080:8080 workflow-engine
```

---

## 📁 Project Structure

```
workflow-engine/
├── src/
│   ├── main/
│   │   ├── java/com/workflowengine/
│   │   │   ├── WorkflowEngineApplication.java  # Entry point
│   │   │   ├── model/
│   │   │   │   ├── Workflow.java               # Workflow entity
│   │   │   │   ├── WorkflowStep.java           # Step entity
│   │   │   │   └── ExecutionLog.java           # Audit log entity
│   │   │   ├── repository/                     # JPA repositories
│   │   │   ├── service/
│   │   │   │   ├── WorkflowService.java        # Core business logic
│   │   │   │   └── AiSuggestionService.java    # AI integration
│   │   │   ├── controller/
│   │   │   │   └── WorkflowController.java     # REST API
│   │   │   └── config/
│   │   │       └── DataSeeder.java             # Sample data seeder
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/                         # Frontend (HTML/CSS/JS)
│   └── test/                                   # Unit tests
├── Dockerfile
├── .github/workflows/ci.yml
└── pom.xml
```

---

## 🔌 REST API Reference

### Workflows
| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| GET    | `/api/workflows`                  | List all workflows       |
| POST   | `/api/workflows`                  | Create a workflow        |
| GET    | `/api/workflows/{id}`             | Get a workflow           |
| PUT    | `/api/workflows/{id}`             | Update a workflow        |
| DELETE | `/api/workflows/{id}`             | Delete a workflow        |
| POST   | `/api/workflows/{id}/execute`     | Trigger execution        |
| POST   | `/api/workflows/{id}/suggest`     | Get AI suggestion        |
| GET    | `/api/workflows/stats/summary`    | Dashboard stats          |

### Steps
| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| GET    | `/api/workflows/{id}/steps`       | Get steps for workflow   |
| POST   | `/api/workflows/{id}/steps`       | Add a step               |
| DELETE | `/api/workflows/steps/{stepId}`   | Delete a step            |

### Logs
| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| GET    | `/api/workflows/{id}/logs`        | Logs for a workflow      |
| GET    | `/api/workflows/logs/recent`      | Last 50 logs globally    |

---

## 🧩 Step Types

| Type               | Description                                    |
|--------------------|------------------------------------------------|
| `MANUAL_TASK`      | Human-action required step                    |
| `APPROVAL`         | Requires a designated approver                |
| `NOTIFICATION`     | Sends alerts or emails                        |
| `CONDITION_CHECK`  | Evaluates business rules / thresholds         |
| `DATA_TRANSFORM`   | Parses, normalizes, or reshapes data          |
| `EXTERNAL_API_CALL`| Calls third-party REST APIs                   |
| `AI_ANALYSIS`      | AI-driven classification or recommendation    |

---

## 🤖 AI Suggestion Engine

The AI advisor analyzes:
- Workflow status and execution history
- Step types and failure patterns
- Success rate trends

And returns a **concrete, actionable next step** — no vague advice. In demo mode, intelligent rule-based suggestions are returned without any API key. Set `AI_API_KEY` for live Claude API suggestions.

---

## ✅ Running Tests

```bash
mvn test
```

Tests cover:
- Workflow CRUD operations
- Step lifecycle management
- Status transitions
- Repository interactions (mocked)

---

## 👤 Author

**Ashwin** — B.Tech CSE, SRM Institute of Science and Technology  
AWS Certified Cloud Practitioner | RHCSA | SAP Certified Data Analyst  
[Portfolio](https://aswin-portfolio-eosin.vercel.app/)

---

## 📄 License

MIT License — free to use, modify, and distribute.
