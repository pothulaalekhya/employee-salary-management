# Employee Salary Management System

Salary management system for a 10,000-employee organization — built with
Spring Boot + Angular, with salary history tracking and pay analytics for HR.

## Problem Statement
ACME's HR team currently manages salary data for 10,000 employees across
multiple countries using spreadsheets. This project replaces that with a
web-based system that lets an HR Manager manage salary records and answer
basic questions about how the organization pays its people.

See [`requirements.md`](./requirements.md) for the full scope, and
[`design.md`](./design.md) for architecture and data model decisions.

## Tech Stack
- **Backend**: Java 17, Spring Boot, Spring Data JPA (Hibernate), Maven, JUnit 5
- **Frontend**: Angular, TypeScript, Angular Material
- **Database**: MySQL (relational)
- **Testing**: JUnit 5 (backend), Jasmine/Karma (frontend)

## Features
- Employee CRUD (create, view, edit, deactivate)
- Salary history tracking — every salary change is recorded, not overwritten
- Search and filter by name, country, department, salary range
- Paginated employee list (performant at 10,000+ rows)
- Analytics: average/median salary by country and department, headcount and
  total pay by country
- Seed script to generate 10,000 realistic employee records

## Running Locally

### Prerequisites
- Java 17+, Maven
- Node.js 18+, Angular CLI
- MySQL 8+ (or update `application.properties` for another DB)

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Seed the database
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--seed
```

### Frontend
```bash
cd frontend
npm install
ng serve
```

App runs at `http://localhost:4200`, API at `http://localhost:8080`.

## Live Demo
- **Deployed app**: [link]
- **Demo video**: [link]

## Testing
```bash
# Backend
cd backend && mvn test

# Frontend
cd frontend && ng test
```

## Artifacts
- [`requirements.md`](./requirements.md) — scope, features, and what was
  deliberately left out (with reasoning)
- [`design.md`](./design.md) — architecture and data model
- [`ai-notes.md`](./ai-notes.md) — how AI tools were used during development
