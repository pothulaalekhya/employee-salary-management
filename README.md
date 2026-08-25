# ACME Employee Salary Management System

A high-performance enterprise compensation management and analytics platform built with **Spring Boot 3 (Java 17 LTS)**, **MySQL 8.0**, and **Angular 21**. 

Designed to replace legacy spreadsheet workflows for a **10,000-employee global organization**, the system provides immutable salary history auditing, multi-currency compensation management, and SQL-level executive pay analytics.

---

## Live Demo
- **Live Frontend (Vercel)**: [https://employee-salary-management-six.vercel.app](https://employee-salary-management-six.vercel.app)
- **Live Backend API (Render)**: [https://employee-salary-backend-x7u4.onrender.com/api/health](https://employee-salary-backend-x7u4.onrender.com/api/health)
- **Cloud Database**: Managed MySQL 8.0 on Aiven (10,000 active employee records)
- **Demo Video Walkthrough**: [Watch Demo Video (Google Drive)](https://drive.google.com/file/d/1atYArE2jrnSDT5UmzpHNIoE9bcwdd2JW/view?usp=sharing)

---

## Tech Stack
- **Backend**: Java 17 LTS, Spring Boot 3.3.4, Spring Data JPA (Hibernate 6), Lombok 1.18.38, DataFaker 2.4.2, Maven, JUnit 5, Mockito, MockMvc
- **Frontend**: Angular 21, TypeScript 5.9, Angular Material 21, Chart.js 4.5 & ng2-charts 10.0, RxJS 7.8, Vitest Test Runner
- **Database**: MySQL 8.0 with InnoDB engine and indexing
- **DevOps & Containerization**: Docker (Multi-stage builds with Eclipse Temurin 17 JRE Alpine & Nginx 1.27 Alpine), Docker Compose

---

## Key Features
1. **High-Volume Directory with Server-Side Pagination**:
   - Paginated table querying 10,000+ employee records directly from Spring Boot (`Pageable`).
   - Multi-field filtering: Country, Department, Min/Max Salary range, and 300ms debounced search by name.
2. **Multi-Currency Compensation & Formatting**:
   - Employees maintain native local currency codes (USD, GBP, EUR, INR, BRL, JPY, AUD, CAD).
   - Custom `CurrencyFormatPipe` formats currency with native grouping and handles currency edge cases (e.g. zero decimal places for Japanese Yen `¥`).
3. **Immutable Salary History & Audit Trail**:
   - Salary updates (`PATCH /api/employees/{id}/salary`) append to an immutable `salary_history` log with effective date and rationale.
   - Denormalized `current_salary` on `employee` guarantees fast reads.
4. **SQL-Level Executive Pay Analytics Dashboard**:
   - Aggregations (Average, Median, Min, Max, Headcount, Total Payroll) are computed at the database level with native CTE window functions (`ROW_NUMBER()` + `COUNT(*)`).
   - Real-time conversion to reporting base currency (USD) joining against the fixed `exchange_rate` table.
   - Interactive KPI cards, Bar Charts (Avg vs. Median Salary by Country & Department), and Global Headcount Distribution Doughnut Chart (`Chart.js`).
5. **Unified Error Handling & Security**:
   - Global `@RestControllerAdvice` returning standard JSON error responses with field-level details.
   - Stack trace suppression for unhandled 500 errors.
   - Environment-driven CORS configuration.

---

## Getting Started

### Option A: Docker Compose (Recommended)

Run the complete multi-container stack (MySQL 8, Spring Boot Backend, Angular Frontend) with one command:

```bash
# 1. Clean previous state and start all 3 services
docker compose down -v
docker compose up -d --build

# 2. Verify all containers are up and healthy
docker compose ps

# 3. Seed 10,000 realistic employee records and exchange rates
docker compose run --rm backend --seed
```

#### Verified Port Mappings:
- **Frontend UI**: `http://localhost:4200`
- **Backend REST API**: `http://localhost:8081` (Health: `http://localhost:8081/api/health`)
- **MySQL Database**: `localhost:3307` externally (mapped to `3306` inside `salary-network` to prevent conflict with local MySQL)

---

### Option B: Native Local Run

#### Prerequisites
- JDK 17+
- Node.js 20+ & npm
- MySQL 8.0 running locally on port 3306 (database `salary_db`, user `salary_user`, password `salary_secure_pass123!`)

#### 1. Backend Setup & Seeding
```bash
cd backend

# Run Spring Boot backend on port 8081
mvn spring-boot:run

# Seed 10,000 employee records (run in a separate terminal)
mvn spring-boot:run -Dspring-boot.run.arguments=--seed
```

#### 2. Frontend Setup
```bash
cd frontend

# Install dependencies and start Angular development server on port 4200
npm install
npm start
```
Open `http://localhost:4200` in your browser.

---

## Testing & Quality Assurance

All features were built test-first with unit, service, repository, and controller test coverage:

```bash
# Run Backend Test Suite (45 tests: DataJpaTest, Service unit tests, MockMvc controller tests)
cd backend
mvn test

# Run Frontend Test Suite (29 tests: Pipe, Filter service, List, Detail, and Analytics dashboard tests)
cd frontend
npm test -- --watch=false
```

### Test Summary:
- **Backend Tests**: **45 / 45 Passed** (0 Failures, 0 Errors)
- **Frontend Tests**: **29 / 29 Passed** (0 Failures, 0 Errors)

---

## REST API Overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health status check |
| `GET` | `/api/employees` | Paginated list with filters (`country`, `department`, `minSalary`, `maxSalary`, `name`, `page`, `size`, `sort`) |
| `GET` | `/api/employees/{id}` | Get employee profile by ID |
| `GET` | `/api/employees/{id}/salary-history` | Get historical salary adjustment audit log |
| `POST` | `/api/employees` | Create employee (generates initial salary history record) |
| `PUT` | `/api/employees/{id}` | Update non-salary employee profile attributes |
| `PATCH` | `/api/employees/{id}/salary` | Update salary with audit tracking |
| `DELETE` | `/api/employees/{id}` | Soft delete (deactivate employee) |
| `GET` | `/api/analytics/salary-by-country` | Average, median, min, max salary by country (USD converted) |
| `GET` | `/api/analytics/salary-by-department` | Average, median, min, max salary by department (USD converted) |
| `GET` | `/api/analytics/headcount-by-country` | Headcount breakdown and percentages by country |
| `GET` | `/api/analytics/total-payroll` | Org-wide total payroll in USD and country breakdown |

---

## AI Workflow & Engineering Notes

This project was built through pair programming with Google DeepMind's **Antigravity** AI Assistant. 

For full details on the development workflow, including specific real-world course corrections (API status polling refactoring, duplicate commit remediation, Jackson JSON quoting edge cases, and Docker host port conflict resolution), see [`ai-notes.md`](./ai-notes.md).

---

## Project Documentation
- [`requirements.md`](./requirements.md): Scope, user stories, and architectural boundaries.
- [`design.md`](./design.md): Layered architecture, schema design, CTE SQL aggregation, and trade-off analysis.
- [`ai-notes.md`](./ai-notes.md): AI pair programming workflow and verification logs.
