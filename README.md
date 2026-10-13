# Personal Finance Tracker

A learning project for building a personal finance application with a React frontend and a Spring Boot API. Authentication uses verified email, one-time codes, and server-side sessions. Transactions, monthly category budgets, and recurring templates are persisted per authenticated account. The dashboard's example budget caps remain illustrative.

## Technology Stack

- Frontend: React, Vite, JavaScript, Tailwind CSS, React Router, Axios, Motion for React, Recharts, and Lucide React
- Backend: Java 21, Spring Boot 4.1.1, Maven, Spring Web MVC, Spring Security, Spring Data JPA, Hibernate, MySQL, Validation, and Spring Boot Mail

## Prerequisites

- Node.js 22.12 or newer and npm
- Java Development Kit (JDK) 21
- Maven 3.9 or the included Maven wrapper
- MySQL Server 8 or newer

## Project Structure

```text
frontend/
  src/
    app/                 React Router and application setup
    components/          Shared UI components
    features/
      auth/pages/        Registration, verification, sign-in, and recovery screens
      budgets/           Authenticated monthly category budget management
      dashboard/pages/   Dashboard totals from saved transactions
      transactions/      Authenticated income and expense management
      recurring/         Authenticated recurring templates and scheduling
    layouts/             Shared page layouts
    services/            Axios API client
backend/
  src/main/java/com/personalfinancetracker/
    config/               Password, clock, and auth beans
    controller/           Health, authentication, transaction, budget, and recurring endpoints
    dto/                  Validated API requests and safe responses
    entity/               User, transaction, budget, recurring template/execution, OTP, and reset grant
    exception/            Sanitized API error handling
    repository/           Spring Data JPA persistence, due locking, and spending aggregation
    security/              Cookie session, CSRF, CORS, and session rules
    service/               Authentication, OTP, token, and mail logic
  src/main/resources/    Spring application configuration
  src/test/              Automated tests
```

## Frontend Setup

```powershell
Set-Location frontend
npm ci
Copy-Item .env.example .env
npm run dev
```

Vite prints the local URL, usually `http://localhost:5173`. Use `npm run build` to create a production build and `npm run lint` to run ESLint.

`frontend/.env` is read by Vite. `VITE_API_BASE_URL` configures the shared Axios client; the default is `http://localhost:8080/api`.

## MySQL Setup

Create a database and a local application user using MySQL Workbench or another MySQL client. Replace the example password with a private value and do not commit it:

```sql
CREATE DATABASE finance_tracker;
CREATE USER 'finance_tracker'@'localhost' IDENTIFIED BY 'replace-with-a-local-password';
GRANT ALL PRIVILEGES ON finance_tracker.* TO 'finance_tracker'@'localhost';
```

The backend uses environment variables for its connection. In the same PowerShell session where you start it, set:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/finance_tracker?serverTimezone=UTC'
$env:DB_USERNAME = 'finance_tracker'
$env:DB_PASSWORD = 'your-local-password'
```

The application configuration has a local database URL default, but database username/password must be supplied through the environment or `backend/.env`. Hibernate schema updates are enabled for local development; use a migration tool before production.

For local setup, copy the safe template and edit `backend/.env` on your machine:

```powershell
Copy-Item backend/.env.example backend/.env
```

When using the commands below from the project root, Spring imports `backend/.env`. Never commit it. `backend/.env.example` contains names and safe defaults only. `JPA_DDL_AUTO=update` preserves existing tables/data; avoid `create` or `create-drop` outside isolated tests. Review schema changes and use a migration tool before production.

### SMTP Setup

Verification and recovery require a real SMTP account. For Gmail, enable 2-step verification and create an app password; use `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, your mailbox for `MAIL_USERNAME` and `MAIL_FROM`, and the app password for `MAIL_PASSWORD`. Do not use your normal mailbox password. Other SMTP providers should use their own host, TLS port, username, and password. Keep all credentials only in `backend/.env` or the process environment.

If SMTP host/from/credentials are not configured or delivery fails, registration returns a service-unavailable response instead of pretending the email was sent. Password reset deliberately returns a generic response to avoid revealing whether an email is registered.

## Authentication and Sessions

- Passwords use BCrypt and never appear in API responses.
- Verified users receive a server-side session in an HTTP-only `JSESSIONID` cookie; credentials or session IDs are not stored in local storage.
- State-changing requests use a CSRF cookie/header. Axios first requests `/api/auth/csrf`, sends credentials, and copies `XSRF-TOKEN` to `X-XSRF-TOKEN`.
- CORS accepts only `FRONTEND_ORIGIN` (default `http://localhost:5173`) with credentials.
- Email codes are six digits, expire after 10 minutes by default, are stored as BCrypt hashes, are single-use, and allow five attempts. Resends have a one-minute cooldown and a five-per-hour limit. Reset grants are random, one-use, and expire after 15 minutes by default.
- Production must use HTTPS and set `COOKIE_SECURE=true`; configure the actual frontend origin and SMTP credentials there.
- Dashboard income, expense, and recent-transaction data is loaded from saved transactions. Its budget-preview limits are example values. Recurring templates are server-backed and scoped to the authenticated user.

## Authentication API

All request/response bodies are JSON. Use `Content-Type: application/json`; all POSTs also require the CSRF cookie and header described above.

| Method and path                      | Request                           | Success                                                                       |
| ------------------------------------ | --------------------------------- | ----------------------------------------------------------------------------- |
| `GET /api/auth/csrf`                 | none                              | CSRF header name; sets `XSRF-TOKEN`                                           |
| `POST /api/auth/register`            | `{ "name", "email", "password" }` | `202` generic verification message                                            |
| `POST /api/auth/verify-email`        | `{ "email", "code" }`             | `200` current-user profile; does not sign in                                  |
| `POST /api/auth/resend-verification` | `{ "email" }`                     | `202` generic message                                                         |
| `POST /api/auth/login`               | `{ "email", "password" }`         | `200` `{ "user": { "id", "name", "email", "createdAt" } }` and session cookie |
| `POST /api/auth/logout`              | empty JSON body                   | `204`; invalidates the server session                                         |
| `GET /api/auth/me`                   | authenticated session             | `200` basic profile; otherwise `401`                                          |
| `POST /api/auth/forgot-password`     | `{ "email" }`                     | `202` generic response whether or not the address exists                      |
| `POST /api/auth/verify-reset-code`   | `{ "email", "code" }`             | `200` short-lived reset grant; keep it in memory only                         |
| `POST /api/auth/reset-password`      | `{ "resetToken", "newPassword" }` | `200` message; old sessions are expired                                       |

Expected errors use `{ "timestamp", "status", "code", "message", "fieldErrors" }`. Invalid/expired codes share a generic response. Validation errors return `400`; duplicate registration returns a generic `409`; unverified login returns `403`; invalid credentials return `401`; SMTP failure during registration returns `503`.

## Monthly Category Budgets

Phase 5 supports category-specific monthly budgets only, matching the existing Budgets page. An overall monthly budget is not implemented. A category budget is unique per authenticated user, calendar year/month, and trimmed category name (category matching is case-insensitive; interior whitespace remains significant). Only saved `EXPENSE` transactions for the same user, category, and month count. Income, other users, and other months are excluded. Spending is summed from transaction rows at read time, so creating, editing, or deleting a transaction changes the next budget calculation.

Budget limits must be positive and fit `DECIMAL(19,2)` (maximum `99999999999999999.99`). The database uses a foreign key, a unique owner/period/category-key constraint, checks for valid period and positive amount, and indexes for owner/period and transaction spending lookups. Schema updates use the existing non-destructive Hibernate `update` configuration.

`GET /api/budgets?year=2026&month=10` lists the authenticated user's budgets for one month. The other authenticated endpoints are `POST /api/budgets`, `GET /api/budgets/{id}`, `PUT /api/budgets/{id}`, and `DELETE /api/budgets/{id}`. Create/update JSON looks like:

```json
{
  "year": 2026,
  "month": 10,
  "category": "Food",
  "amount": "25000.00"
}
```

The budget response includes `id`, `year`, `month`, `category`, `budgetLimit`, `actualSpending`, `remainingAmount`, `percentageUsed`, and `status` (`NORMAL`, `WARNING`, or `EXCEEDED`). Monetary response fields are decimal strings, preserving every cent when JavaScript displays values near the database maximum. Percentage is rounded to two decimal places with `HALF_UP`; status comparisons use exact decimals: below 80% is `NORMAL`, 80% to below 100% is `WARNING`, and 100% or more is `EXCEEDED`. Remaining amounts may be negative. Full rules and matching behavior are in [docs/phase-5-budget-rules.md](docs/phase-5-budget-rules.md).

### Manual Budget Verification

1. Start MySQL and the backend, then start the frontend and sign in to a verified account.
2. Open Budgets, select a month, and create a category budget with a positive limit.
3. Open Transactions and create an expense with the same category/date. Return to Budgets or refresh the selected period; actual spending should include it.
4. Add income in the same category and month; budget spending should not change.
5. Edit the expense amount/category or delete it; the matching budget response should update on the next read.
6. Create another budget for the same user/category/month with different casing; the API should reject it with `409`. A different month is allowed.
7. Try the same budget ID while signed in as another user; the API should return `404`.

Budget amounts and spending are `BigDecimal` values in Java. The repository's `SUM` query returns a decimal total from MySQL/H2; the service calculates remaining amount and percentage with `BigDecimal`, not binary floating-point arithmetic. The React page displays the returned figures and does not calculate authoritative spending.

## Recurring Transactions

Recurring templates create transaction records in this application's database. They do not withdraw funds, transfer money, pay bills, or contact a bank. A template is not itself a transaction; only a committed occurrence appears in transaction lists, dashboard totals, or category budget calculations.

Supported frequencies are `DAILY`, `WEEKLY`, `MONTHLY`, and `YEARLY`; the former mock-only `QUARTERLY` option is intentionally not persisted. Dates use `LocalDate` and UTC. Start/end dates are inclusive. Monthly schedules preserve their original day and clamp to the last valid day in shorter months, then restore the anchor day the following month. Yearly February 29 schedules use February 28 in non-leap years and return to February 29 in leap years. See [docs/phase-7-recurring-rules.md](docs/phase-7-recurring-rules.md) for the complete recurrence/edit/resume rules.

The scheduled runner uses a configurable fixed delay, default 60 seconds (`RECURRING_FIXED_DELAY_MS`), and processes at most 100 due templates per tick (`RECURRING_BATCH_SIZE`). It creates at most one oldest-due occurrence per template in one tick; downtime/pause backlogs are retained and gradually processed in later ticks. Set `RECURRING_ENABLED=false` to disable automatic ticks (tests call the processor directly with a fixed date).

`POST /api/recurring-transactions`, `GET /api/recurring-transactions`, `GET /api/recurring-transactions/{id}`, and `PUT /api/recurring-transactions/{id}` create, list, read, and edit the authenticated user's templates. `PATCH /api/recurring-transactions/{id}/pause` and `/resume` change state. `DELETE /api/recurring-transactions/{id}` soft-deletes the template; existing transactions remain. Create/update request:

```json
{
  "description": "Apartment rent",
  "amount": "28000.00",
  "type": "expense",
  "category": "Housing",
  "frequency": "MONTHLY",
  "startDate": "2026-11-02",
  "endDate": null,
  "notes": "Lease payment"
}
```

The database serializes workers with a pessimistic lock on each template. A unique `(template, occurrence date)` execution row and unique generated-transaction link are additional guards. The execution row, transaction, and next-date advance commit in one transaction, so a failure rolls all of them back. This works across app instances that share the same MySQL database. Worker logs include the template ID and exception type only.

The frontend reloads all transaction API pages after authentication and refreshes on focus/visibility and every minute, so scheduler-created rows reach its local dashboard calculations and recent-transaction table. Budgets calculate their spending directly from saved transaction rows.

### Manual Recurring Verification

1. Configure MySQL credentials, then start the backend and frontend and sign in.
2. Create a template with a start date of today and a small amount. Confirm the next due date and `ACTIVE` state.
3. Invoke the job deterministically in a test or wait for the configured scheduler tick; verify exactly one transaction with the template's owner, category, amount, type, and occurrence date.
4. Refresh the dashboard and Budgets. Verify the generated expense appears in recent activity and increases matching budget spending; verify generated income appears in income totals.
5. Pause a due template, run a tick, and verify no transaction is made. Resume it and confirm the frozen missed occurrence is processed gradually.
6. Create overlapping workers in the concurrency test and confirm one execution/transaction; simulate transaction persistence failure and confirm no execution row or next-date advance remains.
7. Delete a template after it has run and verify its generated transaction remains in transaction history.

The public `GET http://localhost:8080/api/health` endpoint reports process liveness only. The frontend’s protected routes call `/api/auth/me` on startup and redirect unauthenticated visitors to sign-in.

## Backend Commands

From the project root:

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml spring-boot:run
```

The included Maven wrapper can be used instead: `backend\mvnw.cmd -f backend/pom.xml test` and `backend\mvnw.cmd -f backend/pom.xml spring-boot:run`.

Backend tests use in-memory H2 databases and do not need MySQL or SMTP. The local API itself still needs a reachable MySQL database and credentials. Dashboard analytics remain client-side calculations over saved transactions, and the dashboard budget preview still uses example limits. Email budget alerts are not implemented. No automated frontend test script is configured; use the documented lint and production build checks.
