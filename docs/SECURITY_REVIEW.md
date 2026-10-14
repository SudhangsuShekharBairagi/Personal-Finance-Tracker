# Security review

## Scope reviewed

- backend authentication and session handling
- CSRF and CORS rules
- user isolation in transactions, budgets, notifications, and recurring records
- OTP and password reset flow
- environment-driven configuration for database and mail settings
- frontend quality issues that can cause runtime instability

## Security observations

### Authentication and sessions

The backend uses Spring Security with server-side session handling and CSRF protection for state-changing requests. The session cookie is HTTP-only, the frontend does not store the session in browser storage, and the API uses a CSRF cookie and header pattern consistent with a cookie-based app.

Important safety points already in the code:

- passwords are encoded before saving,
- verification codes and reset tokens are treated as time-limited and single-use data,
- the backend checks the authenticated user when loading protected resources,
- the reset and verification flow stores a generic message rather than exposing whether a user exists.

### User isolation

The main service methods enforce ownership by checking the authenticated user before returning or changing data. This is the correct design for a multi-user personal finance tracker. Authorization should always be enforced in the backend rather than trusting the UI.

### Configuration safety

The project expects secrets and connection details to come from environment variables or `.env` files that are not committed to version control. The repository should remain free of real credentials, and production deployments should keep SMTP and database settings outside source control.

## Issues found and fixes applied

We fixed two frontend quality issues that were blocking a clean lint run:

1. an unused `Info` icon import in `ConnectedAuthPage.jsx`
2. a React effect pattern in `NotificationsPage.jsx` that called `setState` directly inside the effect body

The fix kept the same behavior while making the page conform to the React hooks lint rules.

## Remaining risks and limitations

- this review was code-level and configuration-level, not a live penetration test,
- SMTP delivery still requires a real provider setup outside CI/test runs,
- production deployments should verify cookie security, HTTPS, and correct CORS origin values,
- any new third-party dependency should still pass a dependency review before release.

## Production security checklist

Before shipping to production, verify the following manually:

- `COOKIE_SECURE=true` is enabled behind HTTPS
- `FRONTEND_ORIGIN` matches the live site exactly
- MySQL credentials and SMTP credentials are stored in a real secret store or secure environment variables
- test users and test credentials are not left in shared environments
- application logs do not contain password values, OTP codes, or reset tokens

The project’s security posture is reasonable for a learning application, but production hardening still requires environment-specific verification.
