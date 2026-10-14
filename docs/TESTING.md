# Testing guide

This project combines a Spring Boot API and a Vite React frontend. The safest testing approach is to keep production data out of the loop and use the existing local/test setup files.

## 1. Prerequisites

- Java 21
- Maven 3.9+ or the bundled wrapper in `backend/`
- Node.js 22+ and npm
- MySQL 8+ for local database testing, or a disposable H2-based test profile for isolated automated checks

## 2. Backend testing

From the repository root:

```powershell
cd backend
./mvnw test
```

This runs the Spring Boot tests in `backend/src/test/java`. The project already includes H2 in the test scope, so the default automated backend checks can run without touching a real production database.

### Local database check

If you want to test against a real MySQL database instead of H2:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/finance_tracker?serverTimezone=UTC'
$env:DB_USERNAME = 'finance_tracker'
$env:DB_PASSWORD = 'your-local-password'
cd backend
./mvnw test
```

Important: never run automated tests against a shared or production database. Use a local-only database or a dedicated test schema.

## 3. Frontend validation

Install dependencies and run the UI checks:

```powershell
cd frontend
npm install
npm run lint
npm run build
```

The build step validates that the React app compiles correctly into a production bundle. Lint checks catch common quality issues such as unused imports and unsafe state updates.

## 4. Test email simulation

The app supports real SMTP settings through environment variables or `backend/.env` for local development. For automated tests, do not send real emails. Instead:

- keep SMTP settings in local-only environment files,
- mock the mail sender in automated tests, and
- prefer a test account or a disposable mailbox.

The application is designed to treat email delivery failures as a controlled error path rather than a hard crash.

## 5. Integration testing guidance

For API-level verification:

- keep the database isolated from real user data,
- use a dedicated test schema or H2 profile,
- verify authenticated and unauthenticated responses,
- check validation messages and ownership rules,
- confirm that user A cannot access user B’s budgets, transactions, notifications, or recurring schedules.

## 6. Reading results

- A passing backend run exits with code 0 and prints the Maven test summary.
- A passing frontend build exits with code 0 and produces the `dist/` bundle.
- A failing lint or test run shows the specific file and line so you can fix the problem before release.

## 7. Manual checks still worth doing

- verify the login flow in a browser with a test user,
- confirm OTP emails appear in the chosen test mailbox,
- test the dashboard totals with a small set of known transactions,
- ensure notifications fire only once per threshold and do not recreate duplicates.

This project is learning-oriented, so the goal is stable behavior, clear validation, and a safe default path when external services are missing.
