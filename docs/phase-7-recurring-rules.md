# Phase 7: Recurring Transaction Rules

## What recurring means

A recurring template is an instruction to write an income or expense record into this application's database on due dates. It never contacts a bank, transfers funds, pays a bill, or sends a budget alert. Templates are not transactions and do not appear in spending or dashboard calculations until an occurrence is committed as a transaction.

## Supported schedule

- Frequencies are `DAILY`, `WEEKLY`, `MONTHLY`, and `YEARLY`. `QUARTERLY` from the old mock-only form is not supported; it is not a persisted business frequency in this phase.
- Dates use `LocalDate` and UTC as the application's clock zone. There is no time-of-day or daylight-saving conversion.
- `startDate` is the first eligible occurrence date. Both start and optional end dates are inclusive.
- Daily adds one calendar day; weekly adds seven calendar days.
- Monthly recurrence uses the day-of-month from its schedule anchor (initially `startDate`). In a shorter month it uses that month's last day, but subsequent months return to the original anchor. For example, an anchor on January 31 generates February 28 (or 29 in a leap year), then March 31.
- Yearly recurrence uses the anchor month/day. A February 29 anchor produces February 28 in a non-leap year and returns to February 29 in a leap year.

## Downtime, pause, edits, and completion

- Each scheduled tick processes due templates oldest-next-date first. It creates at most one due occurrence per template per tick and examines at most 100 templates per tick by default. A backlog is preserved in `nextScheduledDate` and continues on later ticks. This bounds one worker invocation during a long outage without discarding occurrences.
- Pausing leaves `nextScheduledDate` unchanged. Resuming does not skip or shift missed dates; the backlog is processed using the same one-per-template-per-tick policy.
- Editing description, amount, type, category, notes, frequency, or end date changes not-yet-created occurrences only. Already generated transactions remain unchanged. The current `nextScheduledDate` is kept unless the edited `startDate` is later, in which case the next date moves forward to the new start date. The edited frequency and anchor apply after that pending occurrence.
- An occurrence on the end date is allowed. Once the next occurrence would be after the end date, the template becomes terminal `ENDED`; create a new template to start the schedule again.
- Deleting a template is a soft delete (`DELETED`). Its execution records and generated transactions remain for history and idempotency.

## Atomicity and duplicate prevention

Each template row is locked pessimistically while an occurrence is processed. The database also enforces a unique key on `(recurring_template_id, occurrence_date)` in `recurring_executions`. The execution row, generated transaction, and next-date advancement commit in one database transaction. A failure rolls all three back. A generated transaction has a unique foreign key to its execution row. These locks serialize competing instances, and the unique key is the final duplicate guard.

The scheduler uses a configurable fixed delay (default 60 seconds), UTC `Clock`, and batch size (default 100 templates per tick). Scheduled execution logs identifiers and exception types only, not descriptions, categories, amounts, or other user data.
