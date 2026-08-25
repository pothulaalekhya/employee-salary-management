# Design & Architecture

## Overview
A standard 3-tier web app: Angular SPA -> Spring Boot REST API -> MySQL.
Chosen for simplicity and because it directly matches the tech stack
Incubyte's JD calls for (Spring, Hibernate, Angular, JUnit).

```
[Angular SPA] --HTTP/JSON--> [Spring Boot REST API] --JPA/Hibernate--> [MySQL]
```

## Backend Architecture (layered)

```
controller/   -> REST endpoints, request/response DTOs, input validation
service/      -> business logic (salary update rules, history recording,
                  analytics aggregation)
repository/   -> Spring Data JPA interfaces, one per entity
entity/       -> JPA entities (Employee, SalaryHistory)
dto/          -> request/response shapes, kept separate from entities so
                  the API contract doesn't leak DB structure
config/       -> CORS, seed-runner config, exception handling
```

**Why layered and not "fat controllers"**: keeps business rules (e.g. "a
salary update always writes a history row, never overwrites") testable in
isolation from HTTP concerns. Services are unit-tested directly; controllers
get thinner integration tests.

**Why DTOs instead of exposing entities directly**: avoids accidentally
leaking internal fields (e.g. audit columns) through the API, and lets the
API shape evolve independently of the schema.

## Data Model

### `employee`
| column        | type          | notes                          |
|---------------|---------------|---------------------------------|
| id            | BIGINT PK     | auto-increment                 |
| employee_code | VARCHAR       | unique, human-readable ID      |
| name          | VARCHAR       |                                 |
| country       | VARCHAR       | ISO country code                |
| department    | VARCHAR       |                                 |
| title         | VARCHAR       |                                 |
| currency      | VARCHAR(3)    | ISO currency code               |
| current_salary| DECIMAL(12,2) | denormalized for fast reads     |
| active        | BOOLEAN       | soft-delete/deactivate flag     |
| created_at    | TIMESTAMP     |                                 |

### `salary_history`
| column        | type          | notes                          |
|---------------|---------------|---------------------------------|
| id            | BIGINT PK     |                                 |
| employee_id   | BIGINT FK     | -> employee.id                 |
| amount        | DECIMAL(12,2) |                                 |
| currency      | VARCHAR(3)    |                                 |
| effective_date| DATE          |                                 |
| changed_at    | TIMESTAMP     | when the change was recorded    |
| note          | VARCHAR       | optional reason for change      |

**Why denormalize `current_salary` onto `employee`**: the list view (10,000
rows, paginated, sortable/filterable by salary) is the most frequent read.
Joining against `salary_history` and picking the latest row for every list
query would be slower and more complex than keeping one source of truth for
"current" and appending to history on change. History stays the append-only
audit log; `employee.current_salary` is a read-optimized cache of the latest
entry, updated in the same transaction as the history insert.

**Indexes**: `employee(country)`, `employee(department)`, `employee(active)`,
`salary_history(employee_id)` — these back the filter/search and history
lookup queries.

### `exchange_rate`
| column          | type          | notes                              |
|-----------------|---------------|-------------------------------------|
| id              | BIGINT PK     |                                     |
| currency_code   | VARCHAR(3)    | ISO code, e.g. "INR", "EUR"        |
| rate_to_base    | DECIMAL(12,6) | multiply local amount by this to get base-currency value |
| base_currency    | VARCHAR(3)    | e.g. "USD" — fixed constant for this system |
| updated_at      | TIMESTAMP     |                                     |

Added per Incubyte's clarification: employees store salary in their own
native currency (first-class), and org-wide aggregates convert to a single
base reporting currency (USD) using this fixed, seeded table — not a live
FX API call. Simpler, deterministic, and easy to test; the trade-off is
that rates don't reflect real-time markets, which is acceptable since this
is internal HR reporting, not a financial system.

## API Endpoints (high level)

```
GET    /api/employees?country=&department=&minSalary=&maxSalary=&page=&size=
GET    /api/employees/{id}
POST   /api/employees
PUT    /api/employees/{id}
PATCH  /api/employees/{id}/salary        -> triggers history write
DELETE /api/employees/{id}               -> soft delete (active=false)
GET    /api/employees/{id}/salary-history

GET    /api/analytics/salary-by-country       -> avg/median/min/max, in base currency
GET    /api/analytics/salary-by-department    -> avg/median/min/max, in base currency
GET    /api/analytics/headcount-by-country
GET    /api/analytics/total-payroll            -> org-wide total, in base currency
```

## Analytics Approach
Aggregates (avg/median/headcount/total) run as SQL `GROUP BY` queries
(JPQL or native), not loaded into Java and computed in-memory. At 10,000
rows this isn't strictly required for performance, but it's the correct
default: it scales to much larger datasets without code changes, and lets
the database do what it's optimized for. 

**Median SQL Technique**: Since MySQL has no built-in `MEDIAN()` function,
medians are calculated via a native Common Table Expression (CTE) using
window functions (`ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)` and
`COUNT(*) OVER (PARTITION BY ...)`), selecting the midpoint indices
(`FLOOR((total_count + 1.0)/2.0)` and `CEIL((total_count + 1.0)/2.0)`) and
averaging them to handle both even and odd count distributions accurately in SQL.

Currency conversion to base currency happens at query time by joining
against `exchange_rate` on `employee.currency`, rather than storing
pre-converted values — this keeps `employee.current_salary` as the honest
source of truth in native currency, and conversion logic in one place
(easy to swap for a live FX service later without touching stored data).

Per Incubyte's clarification, the frontend analytics view is a predefined
dashboard: KPI cards (total payroll, avg/median pay, headcount) + charts
(bar/pie by country and department) + interactive filters. 

**Dashboard Filtering**: Interactive country/department filters on the
analytics dashboard operate on the pre-fetched aggregate dataset client-side.
This provides instantaneous UI responsiveness and chart transitions without
issuing redundant aggregate database roundtrips.

## Frontend Architecture

```
src/app/
  core/           -> API service (HttpClient wrapper), shared models
  employees/
    employee-list/       -> paginated table, search/filter bar
    employee-detail/     -> view/edit single employee, salary update form
  analytics/
    analytics-dashboard/ -> KPI cards + charts (ng2-charts/Chart.js) +
                             interactive filters, from analytics endpoints
  shared/
    pipes/                -> e.g. currency-format pipe (per-currency, not
                             hardcoded to one locale)
```

State is kept simple: a small `EmployeeFilterService` holds current
search/filter state (shared between list and any child components) rather
than pulling in a full state-management library — not warranted at this
scale.

## Testing Strategy
- **Service layer**: JUnit unit tests for salary-update logic (history
  recorded, current_salary updated correctly) and analytics aggregation
  (given known seed data, expect known results).
- **Repository layer**: `@DataJpaTest` with an in-memory or test MySQL
  instance for query correctness (filtering, pagination).
- **Controller layer**: `@WebMvcTest` / MockMvc for request validation and
  status codes, business logic mocked out.
- **Frontend**: Jasmine/Karma unit tests for the filter service and the
  currency pipe; component tests for list rendering and pagination.
- Kept fast and deterministic: no tests depend on real seeded 10k data or
  external services — small fixed fixtures only.

## Trade-offs & What I'd Reconsider at Larger Scale
- Denormalized `current_salary` needs care to keep in sync with history —
  acceptable at this scale, would reconsider with a CQRS-style read model
  if this became a much larger system with more write paths.
- No caching layer (e.g. Redis) for analytics — at 10,000 rows, `GROUP BY`
  queries are fast enough to run live; would add caching before it became
  necessary at higher scale or query volume.