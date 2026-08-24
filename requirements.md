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
- **Analytics dashboard**: predefined KPI cards (total payroll, average/
  median pay by department or country, headcount) plus visual charts, with
  interactive filters (by country/department). Confirmed with Incubyte —
  an open-ended ad-hoc query interface is explicitly not expected; a
  well-built predefined dashboard is the target.
- **Multi-currency support**: store each employee's salary in their native
  local currency (first-class property), and additionally convert to a
  single base reporting currency (e.g. USD) using a fixed exchange-rate
  table, so org-wide aggregates are meaningful. Confirmed with Incubyte via
  clarifying question — this is the intended balance between local accuracy
  and org-wide comparability.
- **Seed data**: script to generate 10,000 realistic employee records across
  several countries/departments for demo and load-realism.

## Deliberately Out of Scope (v1)
- **Live/real-time FX rates** — a fixed, seeded exchange-rate table is used
  for base-currency conversion (per Incubyte's clarification) rather than
  calling a live FX API; this avoids an external dependency and keeps
  conversions deterministic/testable, at the cost of not reflecting
  real-time rate fluctuations. A pluggable live-rate service is a natural
  v2 extension point.
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
- **Drill-down/ad-hoc BI tooling** — Incubyte confirmed predefined KPI cards,
  charts, and interactive filters are the expectation (not an open-ended
  query builder), so the dashboard stays a fixed, well-designed set of
  views rather than a general analytics tool.

## Clarifications Received (from Incubyte, in response to submitted questions)
- **Currency**: store native currency per employee (first-class) + convert
  to a fixed base reporting currency via a static exchange-rate table for
  org-wide aggregates. Trade-off: simpler and fully deterministic/testable,
  but rates won't reflect real-time market fluctuations — acceptable for
  this system's purpose (internal HR reporting, not financial trading).
- **Analytics**: predefined KPI cards + charts + interactive filters
  expected; no ad-hoc query interface needed.
- **Deployment**: free-tier hosting (Vercel/Render/Railway) is acceptable;
  a Docker Compose setup + demo video is an acceptable fallback if hosting
  has friction.
- **AI artifacts**: a summarized write-up of AI tool usage and prompt
  strategy is sufficient; raw prompt logs are not required.

## Assumptions
- One salary record can be "current" per employee, with a history log of
  previous records.
- Currency values are stored as entered (assume already-validated ISO codes),
  no conversion.
- 10,000 employees is the scale target for seed data and query performance,
  not a hard scaling requirement (no need to prove million-row behavior).