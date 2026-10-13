# Phase 5: Budget Rules

## Scope and ownership

- Phase 5 supports category-specific monthly expense budgets only. The existing Budgets page is organized around expense categories, so an overall monthly budget is intentionally not part of this phase.
- Each budget belongs to the user represented by the authenticated server-side session. The API does not accept an owner ID.
- A budget is unique for one user, one calendar year/month, and one category. The database unique constraint is the final guard against concurrent duplicate requests.
- Limits must be positive and may not exceed `99999999999999999.99`, matching `DECIMAL(19,2)`.

## Category and period matching

- Category names are trimmed at both ends when budgets and transactions are saved. Comparison is case-insensitive; interior whitespace and punctuation remain significant. For example, `Food` and `food` match, while `Dining Out` and `Dining  Out` do not.
- A period is the calendar year and month of a transaction's `LocalDate`. Queries include the first day of that month and exclude the first day of the following month.
- Only saved `EXPENSE` transactions for the same owner, normalized category, and period contribute to spending. Income, another user's records, and records in other months are excluded.
- Spending is queried from transaction rows each time a budget is read. It is not a user-editable or stored budget field; transaction create, update, and delete changes the next calculation automatically.
- There is no overall budget, so this phase never combines an overall cap with category caps. Category caps are separately displayed and are not represented as one total-spending measure.

## Calculations and status

- Actual spending is the database `SUM` of matching expense amounts, with no cap at the budget limit.
- Remaining amount is `budget limit - actual spending` and may be negative.
- Percentage used is `actual spending * 100 / budget limit`, rounded to two decimal places using `HALF_UP` for the response. Status checks use the exact amounts, not the rounded percentage.
- `NORMAL` is below 80%; `WARNING` is at least 80% and below 100%; `EXCEEDED` is 100% or more.
- Because a valid budget limit is always positive, division by zero is not possible.

## Persistence

The `budgets` table references `app_users`, stores limits as `DECIMAL(19,2)`, and has a unique key across owner, year, month, and normalized category key. Hibernate uses the existing non-destructive `update` schema strategy; no create/drop operation is used by the application.
