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
| Backend     | Java 21, Spring Boot 3.3, Spring Data JPA|
| Database    | H2 (dev), MySQL (prod)                   |
| AI Layer    | Anthropic Claude API (demo mode included)|
| Frontend    | HTML5, CSS3, Vanilla JS                  |
| Testing     | JUnit 5, Mockito                         |
| DevOps      | Docker, GitHub Actions CI/CD             |

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+

### Run locally (demo mode — no API key needed)
```bash
git clone https://github.com/Aswin20122005/workflow-engine.git
cd workflow-engine
mvn spring-boot:run
```

Open **http://localhost:8080** in your browser.

### With real AI suggestions (optional)
```bash
export AI_API_KEY=your_anthropic_api_key
export AI_MODEL=claude-haiku-5-5      # optional, this is the default
mvn spring-boot:run
```

### Profiles
| Profile | How | What it does |
|---|---|---|
| *(default)* | `mvn spring-boot:run` | In-memory H2, schema recreated on each start, sample workflows seeded |
| `dev` | `-Dspring-boot.run.profiles=dev` | Adds the H2 console at `/h2-console` and SQL logging |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | MySQL via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; schema kept between restarts |

### Run with Docker
```bash
docker build -t workflow-engine .
docker run -p 8080:8080 workflow-engine
```
The image runs as a non-root user, reports health via `/actuator/health/liveness`, sizes the JVM heap from the container memory limit, and shuts down gracefully on `docker stop`.

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
│   └── test/                                   # Unit + integration tests
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
| POST   | `/api/workflows/{id}/execute`     | Start execution: `202` started, `409` already running |
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

### Errors and health
- Unknown workflow or step → `404 {"error": "Workflow not found: 42"}`
- Invalid body → `400 {"error": "Validation failed", "errors": {"name": "must not be blank"}}`
- `GET /actuator/health` → `{"status": "UP"}` (the only actuator endpoint exposed)

### How execution works
`POST /execute` checks the workflow exists and isn't already running, marks it `ACTIVE`, and hands it to `WorkflowExecutor`, which runs the steps on a background thread. Each step's status change is committed immediately, so the UI shows steps moving through `IN_PROGRESS` → `COMPLETED` live. A failing step marks the workflow `FAILED` and stops the run. Execution logs are kept when a workflow is deleted, as an audit trail.

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
mvn verify
```

32 tests:
- **Integration** (`@SpringBootTest` + MockMvc, full app with H2): CRUD, 404/400/409 responses, background execution to completion, health endpoint
- **WorkflowExecutor**: success and failure paths, per-step commits, audit logs
- **WorkflowService**: CRUD, not-found handling, starting execution
- **AiSuggestionService**: rule-based suggestions and fallback when the API is unreachable

CI runs `mvn verify`, then builds the Docker image, waits for the health check, smoke-tests the API and checks graceful shutdown.

---

## 👤 Author

**Ashwin** — B.Tech CSE, SRM Institute of Science and Technology  
AWS Certified Cloud Practitioner | RHCSA | SAP Certified Data Analyst  
[Portfolio](https://aswin-portfolio-eosin.vercel.app/)

---

## 📄 License

MIT License — free to use, modify, and distribute.
