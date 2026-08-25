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

## 2. Key Prompting Strategies & Prompt Patterns Used

Rather than issuing open-ended generic requests, development was driven by structured, constraint-rich prompt engineering techniques:

### 1. Stage-Gated Milestone Prompting
- **Strategy**: Broke the entire project into 10 sequential, isolated stages (0 to 10). Each prompt specified strict boundaries: *"Build Stage N only. Do not touch or advance to Stage N+1 until Stage N passes all tests and verification."*
- **Benefit**: Prevented AI hallucination, scope creep, and untested assumptions from propagating across layers.

### 2. Specification & Constraint Injection
- **Strategy**: Injected domain constraints directly into prompts (e.g. *"Use native SQL CTE Window Functions `ROW_NUMBER()` + `COUNT(*)` for calculating exact medians at the database level rather than loading all 10,000 entities into JVM memory"*).
- **Benefit**: Guaranteed optimal O(1) memory footprint and native database execution.

### 3. Verification-First & Test-Driven Directives
- **Strategy**: Every prompt that introduced or modified code required accompanying unit/integration tests with explicit test assertions (e.g. testing `409 Conflict` on duplicate employee codes, `400 Bad Request` on negative salary, and `404 Not Found` on nonexistent IDs).
- **Benefit**: Ensured 100% test pass rate across all 45 backend and 29 frontend test suites.

### 4. Active Human-in-the-Loop Interventions & Counter-Prompting
- **Strategy**: When AI generated superficial placeholders (e.g., a static "API Online" badge or an unhandled JSON parse exception), explicit correction prompts were issued to replace placeholders with real dynamic implementations.

---

## 3. Specific Course Corrections & Discrepancies Caught During Review

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

## 4. Review & Verification Workflow

The project followed a rigorous 4-step verification loop for every stage:

1. **Diff Inspection**: All generated code diffs were inspected before execution to ensure no unwanted side effects or regressions.
2. **Automated Test Suites**:
   - Backend: Ran `mvn test` verifying all 45 unit, service, repository, and controller tests pass with 0 failures.
   - Frontend: Ran `npm test -- --watch=false` verifying all 29 component, service, and pipe tests pass with 0 failures.
3. **Live REST & Error Verification**: Manually triggered live endpoints via curl / PowerShell (e.g. testing duplicate employee code for `409 Conflict`, negative salary for `400 Bad Request`, nonexistent ID for `404 Not Found`).
4. **End-to-End Browser Verification**: Used the Antigravity browser subagent to interact with the live running UI (`http://localhost:4200`) against the 10,000-employee database, verifying sorting, filtering, debounced search, salary updates, and chart rendering.
