# AI Workflow & Pair Programming Notes

## 1. AI Tooling Overview
- **Primary AI Assistant**: Google DeepMind **Antigravity** (Advanced Agentic Pair Programming Assistant).
- **Collaboration Model**: Structured, test-first, stage-by-stage pair programming with strict verification gates at each milestone.

### Areas of AI Delegation:
- **Scaffolding & Architecture**: Initializing Spring Boot 3.3.4 (Java 17 LTS) and Angular 21 with Angular Material and Tailwind-free custom CSS design system.
- **Domain Modeling & JPA Layer**: Entity definition (`Employee`, `SalaryHistory`, `ExchangeRate`), repository specifications, and database constraints.
- **REST API & Business Services**: Layered DTO architecture, soft delete implementation, immutable salary adjustment audit log, and `@RestControllerAdvice` unified exception handling.
- **Realistic High-Volume Seeding**: `DataSeeder` generating 10,000 employees with country-specific compensation curves and multi-currency exchange rates.
- **SQL Analytics Engine**: Native SQL aggregations with CTE window functions (`ROW_NUMBER()` + `COUNT(*)`) for exact mathematical medians across currencies in USD.
- **Angular Frontend**: Standalone components (`EmployeeListComponent`, `EmployeeDetailComponent`, `AnalyticsDashboardComponent`), reactive RxJS `EmployeeFilterService`, custom `CurrencyFormatPipe` (with JPY 0-decimal handling), and `ng2-charts` / Chart.js dashboards.
- **Deployment & Containerization**: Multi-stage Dockerfiles and `docker-compose.yml` orchestration.

---

## 2. Stage-by-Stage Implementation & AI Collaboration Timeline

The application was built systematically through 11 disciplined, incremental stages (0 to 10), enforcing strict test verification gates at each milestone:

| Stage | Milestone | Objective | AI Collaboration & Prompts | Verification Gate |
|:---:|---|---|---|---|
| **Stage 0** | **Scaffolding & Health Shell** | Initialize Java 17 Spring Boot & Angular 21 projects with dual-dev server setup. | Prompted project scaffolds, build descriptors, and dynamic health polling component. | Verified `GET /api/health` returns `200 OK` and frontend displays live pulse badge. |
| **Stage 1** | **Domain Entities & JPA** | Design MySQL schema, JPA entities, and repositories with indexing. | Directed entity modeling for `Employee`, `SalaryHistory`, and `ExchangeRate` with composite indexes. | Verified schema generation and repository interface compilation. |
| **Stage 2** | **Employee Read & Filter API** | Build paginated employee directory queries with multi-parameter filtering. | Implemented `GET /api/employees` using Spring `Pageable` and Criteria/JPA queries. | Verified with 8 unit/integration tests for sorting, pagination, and filters. |
| **Stage 3** | **Employee Write Operations** | Implement employee creation, updates, and soft deletion. | Generated DTOs, Bean Validation constraints, and soft-delete business logic (`active=false`). | Verified with 7 controller & service tests including `400 Bad Request` validations. |
| **Stage 4** | **Salary History & Audit Trail** | Build immutable audit log for employee salary adjustments. | Prompted `PATCH /api/employees/{id}/salary` to append historical snapshots while updating `current_salary`. | Verified with 6 tests verifying immutable log creation and effective dating. |
| **Stage 5** | **10,000 Data Seeding Engine** | Generate 10,000 realistic employees across 8 countries and 6 departments. | Built `DataSeeder` using DataFaker with realistic currency distributions and 500-record batch commits. | Executed seed command verifying all 10,000 rows and 8 FX rates inserted cleanly. |
| **Stage 6** | **SQL Pay Analytics Engine** | Calculate org-wide pay statistics in USD reporting currency. | Engineered native SQL CTE Window Functions (`ROW_NUMBER()` + `COUNT(*)`) for exact medians and aggregations. | Verified 8 analytics tests across country, department, and total payroll endpoints. |
| **Stage 7** | **Frontend Directory & Details** | Create responsive employee table, search debouncing, and edit dialogs. | Built Angular Material table, RxJS 300ms debounce pipe, custom `CurrencyFormatPipe`, and detail view. | Verified 15 component/pipe tests with Vitest + interactive browser subagent. |
| **Stage 8** | **Frontend Analytics Dashboard** | Visualize pay distributions, comparisons, and headcount KPIs. | Integrated Chart.js & `ng2-charts` for Avg vs. Median bar charts and country distribution doughnuts. | Verified 14 dashboard tests + verified interactive client-side filtering in browser. |
| **Stage 9** | **Hardening & Dockerization** | Add global exception handling and multi-stage container orchestration. | Implemented `@RestControllerAdvice`, multi-stage Dockerfiles (JRE 17 Alpine & Nginx), and Docker Compose. | Verified 45 backend + 29 frontend tests passing; all 3 Docker containers healthy. |
| **Stage 10** | **Cloud Deployment & Docs** | Deploy full-stack app to cloud and finalize architectural documentation. | Configured Render (Backend), Vercel (Frontend), Aiven (Cloud MySQL), and wrote `README.md` / `ai-notes.md`. | Verified live cloud endpoints (`employee-salary-management-six.vercel.app`). |

---

## 3. Key Prompting Strategies & Prompt Patterns Used

Rather than issuing open-ended generic requests, development was driven by structured, constraint-rich prompt engineering techniques. Below are the core prompt patterns with concrete prompt examples used during development:

### 1. Stage-Gated Milestone Prompting
- **Strategy**: Broke the entire project into 10 sequential, isolated stages (0 to 10). Each prompt specified strict boundaries to prevent AI hallucination, scope creep, and untested assumptions from propagating across layers.
- **Representative Prompt Example**:
  ```markdown
  "Build Stage 6 only — Pay Analytics Endpoints.
  Requirements:
  1. Add native SQL queries to compute Average, Median, Min, Max, Headcount, and Total Payroll in USD.
  2. Implement GET /api/analytics/salary-by-country, /salary-by-department, /headcount-by-country, and /total-payroll.
  3. Write unit & integration tests covering all 4 endpoints.
  4. Do not proceed to Stage 7 until all backend tests pass with zero failures."
  ```

### 2. Specification & Technical Constraint Injection
- **Strategy**: Injected domain constraints directly into prompts to guarantee optimal O(1) memory footprint and native database execution.
- **Representative Prompt Example**:
  ```markdown
  "Implement median salary calculation in Spring Boot repository using native SQL CTE Window Functions:
  - Join Employee with ExchangeRate on employee.currency = exchange_rate.currency_code
  - Use ROW_NUMBER() OVER (PARTITION BY ... ORDER BY current_salary * rate) and COUNT(*) OVER (PARTITION BY ...)
  - Calculate exact median at the database level rather than streaming 10,000 entities into Java memory."
  ```

### 3. Verification-First & Test-Driven Directives
- **Strategy**: Every prompt that introduced or modified code required accompanying unit/integration tests with explicit test assertions.
- **Representative Prompt Example**:
  ```markdown
  "Add full MockMvc unit tests for EmployeeController:
  1. Verify GET /api/employees returns 200 with paginated JSON structure.
  2. Verify POST with duplicate employeeCode returns 409 Conflict with structured ErrorResponse.
  3. Verify PATCH /api/employees/{id}/salary with negative salary returns 400 Bad Request with field validation errors.
  4. Verify GET for nonexistent ID returns 404 Not Found."
  ```

### 4. Active Human-in-the-Loop Interventions & Counter-Prompting
- **Strategy**: When AI generated superficial placeholders or hit environment bottlenecks, explicit correction prompts were issued to replace placeholders with real dynamic implementations.
- **Representative Prompt Example**:
  ```markdown
  "The current health badge in the navbar is hardcoded to 'API Online'. Refactor AppComponent to inject ApiService and poll GET /api/health every 5 seconds. If the backend is unreachable or returns non-200, dynamically update the badge to 'API Offline' with an orange pulse indicator."
  ```

---

## 4. Specific Course Corrections & Discrepancies Caught During Review

Rather than blindly accepting generated code, active human-in-the-loop review identified and corrected several critical issues:

### 1. Stage 0: False "API Online" Status Badge
- **Issue**: During initial frontend shell construction, the navigation header contained a static badge displaying "API Online" before verifying whether the Spring Boot backend process on port 8081 was actually listening.
- **Correction**: Caught during code review. Refactored `AppComponent` to inject `ApiService` and actively poll `GET /api/health`, dynamically updating the badge with a pulse animation for "Online" vs. "Offline" states.

### 2. Stage 4: Analytics Commit Duplication
- **Issue**: An ambiguous prompt instruction during analytics verification led to a redundant commit of the analytics layer in git history.
- **Correction**: Identified during `git log --oneline` audit. Enforced strict conventional commit guidelines (`feat: ...`, `fix: ...`, `chore: ...`) and mandated verifying working tree cleanliness before proceeding to subsequent stages.

### 3. Stage 8: PowerShell JSON Quoting & Exception Leaking
- **Issue**: When testing live error handling with curl in PowerShell, unescaped JSON caused Jackson to throw `HttpMessageNotReadableException`, which initially fell into the generic 500 handler.
- **Correction**: Caught during manual verification. Added a dedicated `@ExceptionHandler(HttpMessageNotReadableException.class)` in `GlobalExceptionHandler` returning `400 Bad Request` with a clean error message, preventing any stack trace leakage.

### 4. Stage 9: Docker Host Port Conflict & Daemon Verification
- **Issue**: The initial `docker-compose.yml` mapped MySQL container port to host `3306:3306`, which caused a bind collision because the native Windows MySQL service was already running on port 3306. Additionally, deployment instructions were initially provided before confirming that the Docker daemon was active.
- **Correction**: Verified the failure live, mapped the container's external MySQL port to `3307:3306` (preserving internal `3306` communication over `salary-network`), started Docker Desktop, and executed `docker compose ps` and `docker compose run --rm backend --seed` to confirm all 3 containers were healthy and 10,000 rows were seeded.

---

## 5. Review & Verification Workflow

The project followed a rigorous 4-step verification loop for every stage:

1. **Diff Inspection**: All generated code diffs were inspected before execution to ensure no unwanted side effects or regressions.
2. **Automated Test Suites**:
   - Backend: Ran `mvn test` verifying all 45 unit, service, repository, and controller tests pass with 0 failures.
   - Frontend: Ran `npm test -- --watch=false` verifying all 29 component, service, and pipe tests pass with 0 failures.
3. **Live REST & Error Verification**: Manually triggered live endpoints via curl / PowerShell (e.g. testing duplicate employee code for `409 Conflict`, negative salary for `400 Bad Request`, nonexistent ID for `404 Not Found`).
4. **End-to-End Browser Verification**: Used the Antigravity browser subagent to interact with the live running UI (`http://localhost:4200`) against the 10,000-employee database, verifying sorting, filtering, debounced search, salary updates, and chart rendering.
