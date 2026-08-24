# Employee Salary Management System — Requirements

## Goal
Give ACME's HR Manager a web application to manage salary data for ~10,000 employees
across multiple countries, replacing spreadsheets, and to let them answer basic
questions about how the org pays people (e.g. distribution, averages, outliers).

## Primary User
HR Manager — not an engineer, needs a simple, fast, trustworthy UI. Occasionally
needs to look at aggregate pay data, not just individual records.

## In Scope (v1)
- **Employee records**: name, employee ID, country, currency, department/title,
  base salary, effective date of salary.
- **CRUD**: create, view, edit, deactivate an employee's salary record.
- **Search & filter**: by name, country, department, salary range.
- **Pagination**: list view must perform well at 10,000 rows.
- **Salary history**: track changes over time (who changed what, when) rather
  than overwriting — this is core to "how the org pays people" being answerable
  later, and to auditability, which any real HR system needs.
- **Basic analytics/reporting**: average/median salary by country and by
  department; headcount and total pay by country. This is the "answer
  questions about how the org pays people" requirement.
- **Multi-currency support**: store currency alongside salary; do not force a
  single-currency conversion (see Out of Scope).
- **Seed data**: script to generate 10,000 realistic employee records across
  several countries/departments for demo and load-realism.

## Deliberately Out of Scope (v1)
- **Live currency conversion / FX rates** — real-time FX adds an external
  dependency and doesn't change the core CRUD/analytics story; showing salary
  in native currency is more honest for HR data anyway. Could be a v2 feature
  behind a pluggable FX-rate service.
- **Payroll processing / tax calculation / disbursement** — this is a salary
  *management* tool, not a payroll engine. Payroll math varies wildly by
  country and is its own product.
- **Role-based access control / multi-tenant auth** — out of scope for a
  take-home; would add a full auth subsystem without demonstrating the core
  ask. I'll note where it would plug in architecturally.
- **Employee self-service portal** — persona is explicitly the HR Manager,
  not employees.
- **Org chart / reporting-line management** — not needed to answer pay
  questions; adds a graph-modeling problem that's orthogonal to the goal.
- **Bulk import from Excel** — a real nice-to-have for the stated problem
  ("everything managed via Excel"), but adds file-parsing/validation scope
  that competes with core CRUD + analytics for the time budget. Called out
  as the top v2 candidate.
- **Advanced BI/dashboards** (charts, drill-downs) — a handful of numeric
  aggregates answers "how does the org pay people" without building a full
  analytics UI.

## Assumptions
- One salary record can be "current" per employee, with a history log of
  previous records.
- Currency values are stored as entered (assume already-validated ISO codes),
  no conversion.
- 10,000 employees is the scale target for seed data and query performance,
  not a hard scaling requirement (no need to prove million-row behavior).
