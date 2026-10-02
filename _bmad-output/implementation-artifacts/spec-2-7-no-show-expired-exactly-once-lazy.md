---
title: 'Story 2.7: No-show EXPIRED — exactly-once lazy'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '3cf8b4a5a410ef3594d500df5a38640a99e8f130'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Reserved storage units (`RESERVED`) where the customer fails to check in by the scheduled start date remain indefinitely held unless explicitly processed, preventing unit inventory release and leaving deposit forfeiture unrecorded.

**Approach:** Implement lazy on-read expiration evaluating reservations against `Asia/Ho_Chi_Minh` calendar date. When an un-checked-in `RESERVED` reservation with a past start date is accessed during queries (unit browsing, reservation detail, customer rentals list), execute an idempotent, exactly-once state transition that flips the reservation to `EXPIRED`, releases the unit back to `AVAILABLE`, closes draft contracts, logs audit activity (`LogService`), and notifies the customer of deposit forfeiture.

## Boundaries & Constraints

**Always:**
- Expiry evaluation is lazy and executed on-read (AD-4) during read operations (`GET /api/v1/reservations/{id}`, `GET /api/v1/reservations/my`, and `GET /api/v1/units/browse`).
- Dates are strictly evaluated against the `Asia/Ho_Chi_Minh` timezone (`ZoneId.of("Asia/Ho_Chi_Minh")`, AD-7): a reservation is expired if `status == RESERVED` and `startDate.isBefore(todayInICT)`.
- Side effects execute strictly **exactly-once** inside an atomic transaction:
  - Reservation status transitions from `RESERVED` to `EXPIRED`.
  - Unit status flips from `RESERVED` to `AVAILABLE`.
  - Associated latest `Contract` in `DRAFT` or `PRINTED` status transitions to `CLOSED`.
  - Audit log is appended via `LogService` (`EntityType.RESERVATION`, `Action.STATUS_CHANGE`, `fromValue: "RESERVED"`, `toValue: "EXPIRED"`, description: `"Reservation expired due to no-show on check-in date"`).
  - Notification (toast + bell) is dispatched to customer regarding no-show expiration and deposit forfeiture.
- Concurrency protection: Parallel read requests must be idempotent without duplicate side-effects, double notifications, or database constraint violations.
- In v1, `EXPIRED` is the sole terminal failure path; self-service customer cancellation is not supported and forfeited deposits are non-refundable.

**Never:**
- Never run a separate background cron / scheduled batch polling loop for lazy on-read expiry.
- Never expire reservations with future start dates or reservations already in `CHECKED_IN` / `CLOSED` states.
- Never recalculate or alter historical locked price snapshots upon expiration.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| On-read Lazy Expiration | `GET /api/v1/reservations/{id}` where reservation is `RESERVED` and `startDate < todayICT` | 200 OK with `status: "EXPIRED"`; Unit -> `AVAILABLE`, Contract -> `CLOSED`, Log appended, Notification sent | 404 if not found |
| List My Rentals with Expired | `GET /api/v1/reservations/my` containing a past `RESERVED` booking | 200 OK; past booking appears in History tab with `EXPIRED` badge and deposit status `Forfeited` | N/A |
| Browse Units Releases Expired | `GET /api/v1/units/browse` when a unit was reserved by an expired no-show | 200 OK; unit is now available in browse inventory | N/A |
| Future Reservation Unchanged | `GET /api/v1/reservations/{id}` where `startDate >= todayICT` | 200 OK with `status: "RESERVED"`, unit remains `RESERVED` | N/A |
| Concurrent Parallel Reads | Two simultaneous HTTP requests reading the same expired reservation | Both return 200 OK with `EXPIRED`; side-effects (unit release, log, notification) run exactly once | Idempotent transaction guard |

</frozen-after-approval>

## Open Questions

## Code Map

- `backend/src/main/java/com/storagehub/service/ReservationExpiryService.java` (or method inside `ReservationService.java`) -- Implement `checkAndExpireReservation` and `checkAndExpireReservationsForCustomer/Unit` with transactional idempotency and ICT timezone checking.
- `backend/src/main/java/com/storagehub/service/ReservationService.java` -- Integrate lazy expiry check before returning single reservation or customer reservations list.
- `backend/src/main/java/com/storagehub/service/unit/UnitBrowseService.java` -- Integrate lazy expiry check during availability queries so expired reservations do not block inventory.
- `backend/src/main/java/com/storagehub/repository/ReservationRepository.java` -- Add query to find past active reservations requiring expiration.
- `backend/src/test/java/com/storagehub/reservation/ReservationExpiryTests.java` -- Unit & integration tests for ICT timezone evaluation, atomic state transitions, and concurrency safety.
- `frontend/src/test/my-rentals.test.tsx` & `rental-detail.test.tsx` -- Verify that expired reservations render with `EXPIRED` status badge and `Forfeited` deposit status in the UI.

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/java/com/storagehub/service/` -- Implement lazy on-read expiry logic in `ReservationService` with ICT timezone checks and atomic side-effects -- Core business logic.
- [x] `backend/src/main/java/com/storagehub/service/unit/UnitBrowseService.java` -- Trigger lazy expiry during unit browsing to release inventory -- Availability accuracy.
- [x] `backend/src/test/java/com/storagehub/reservation/` -- Add unit and integration tests covering ICT date evaluation, unit release, contract closing, and idempotency -- Test verification.
- [x] `frontend/src/` & `frontend/src/test/` -- Ensure frontend and test suites reflect expired reservation displays and History tab grouping -- UI verification.

**Acceptance Criteria:**
- Given a `RESERVED` reservation whose `startDate` is prior to today in `Asia/Ho_Chi_Minh`, when read via any API endpoint, then the reservation status lazily transitions to `EXPIRED`.
- Given an expired reservation transition, then the unit status flips to `AVAILABLE`, any draft contract flips to `CLOSED`, an audit log entry is recorded, and a customer notification is created.
- Given multiple concurrent read requests for an expired reservation, then state transitions and side effects execute strictly once without error.
- Given a reservation with a future start date or `CHECKED_IN` status, when queried, then its status remains untouched.

## Implementation Notes
- Created `ReservationExpiryService` with `ZoneId.of("Asia/Ho_Chi_Minh")` (ICT) timezone evaluation.
- Evaluates whether reservation has `status == RESERVED` and `startDate < todayInICT`.
- Executes atomic, exactly-once state transitions:
  - Flips `Reservation.status` to `EXPIRED`.
  - Releases `Unit.status` from `RESERVED` to `AVAILABLE`.
  - Closes latest draft/printed contract to `CLOSED`.
  - Appends audit log via `LogService` (`STATUS_CHANGE: RESERVED -> EXPIRED`).
  - Dispatches customer notification regarding deposit forfeiture.
- Integrated lazy on-read checks in `ReservationService.getMyReservations`, `ReservationService.getReservation`, `UnitService.browseUnits`, and `UnitService.getUnitDetail`.
- Added 6 dedicated unit & integration test scenarios in `ReservationExpiryTests.java`.
- Verified frontend displays `EXPIRED` status badge and `Forfeited` deposit status in `my-rentals.test.tsx` and `rental-detail.test.tsx`.
- Verified all 159 backend tests and 144 frontend tests pass.

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Evidence | Action |
|---------|---------|----------|--------|
| Inconsistent timezone usage in UnitService | Low | Consistency with ICT timezone needed for date comparison | Patched UnitService to use ICT_ZONE |
| Concurrency check in expireReservation | Low | Idempotency guard inside @Transactional prevents duplicate mutations | Verified |
| Full table scan on browsing | False | Query searches strictly active RESERVED records with past start dates | Verified |

## Verification

**Commands:**
- `mvn test` -- expected: All backend tests pass including new lazy expiry tests.
- `npm run test` -- expected: All frontend tests pass.
- `npm run build` -- expected: TypeScript and Vite production build succeeds.
