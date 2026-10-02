---
title: 'Story 2.3: Reserve re-check + Booking Summary'
type: 'feature'
created: '2026-10-02'
status: 'done'
baseline_commit: '391e15104b69ec5425e9c1259f3cdbbb51747ac9'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When multiple customers attempt to reserve the same unit simultaneously or from a stale catalog grid, there is a risk of double-booking, and customers lack a binding Booking Summary step that itemizes every legal and financial term with an immutable price snapshot before money is collected.

**Approach:** Implement transactional reservation creation (`POST /api/v1/reservations`) with atomic availability re-checking against overlapping active reservations and turnover buffer rules. Concurrent conflicts return HTTP 409 Conflict with code `UNIT_UNAVAILABLE`. On success, create a `PENDING_PAYMENT` Reservation with unique code `BK-` and store an immutable price snapshot (base rent, duration, deposit, monthly rate). On the frontend, build the `BookingSummaryPage` (`/booking/summary`) displaying full itemized terms, contractual notice ("Contract auto-drafted from these exact terms, signed at check-in"), and "Proceed to Deposit Payment" CTA, with automatic 409 bounce back to `/units` accompanied by an informative toast.

## Boundaries & Constraints

**Always:**
- Availability must be atomically re-checked within the reservation transaction before inserting the new reservation.
- Conflicting bookings must return HTTP 409 Conflict with standard error envelope (`code: UNIT_UNAVAILABLE`, message naming the unit).
- Frontend catching 409 on reserve/booking must bounce the user back to `/units` and display an error toast (e.g., `"S-3 was just reserved. Similar units still available."`).
- Reservation creation locks an immutable price snapshot (`baseRent`, `totalRent`, `depositAmount`, `monthlyRate`, `policyVersion`) so subsequent policy updates never alter existing booking terms (AD-11).
- Reservation code format starts with `BK-` (e.g. `BK-2026-0001` or `BK-1043`), unique across the lifecycle.
- Monetary amounts use `BigDecimal` / `DECIMAL(15,0)` / JSON numbers formatted as `1.150.000 ₫` with tabular numerals.

**Never:**
- Never create a reservation if the unit is `RENTED`, `MAINTENANCE`, `RETIRED`, or has conflicting active reservations (`PENDING_PAYMENT`, `RESERVED`, `CHECKED_IN`).
- Never compute pricing lines ad hoc in the UI (all financial line items originate from `PricingEngine`).
- Never allow unauthenticated reservation creation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid Reservation Creation | `POST /api/v1/reservations` with `unitCode: "M-5"`, `startDate: "2026-10-03"`, `durationMonths: 1` | 201 Created; Reservation `PENDING_PAYMENT` created with unique `code: "BK-..."`, dates `2026-10-03` -> `2026-11-03`, deposit `69.000 ₫`, price snapshot stored | N/A |
| Concurrent 409 Conflict | Two parallel requests for unit `M-5` on same dates | 1st request wins (201 Created); 2nd request receives 409 Conflict envelope | `BusinessRuleException("UNIT_UNAVAILABLE", "Unit M-5 is no longer available...")` |
| Reservation for Maintenance Unit | `unitCode: "M-2"` (MAINTENANCE) | 409 Conflict envelope | `BusinessRuleException("UNIT_UNAVAILABLE", "Unit M-2 is under maintenance")` |
| FE 409 Bounce Handling | 409 received on Booking Summary confirm | FE navigates to `/units` and emits toast: `"{unitCode} was just reserved. Similar units still available."` | Handled in Axios / Mutation catch block |
| Booking Summary Display | Navigate to `/booking/summary?unit=S-3&duration=3&startDate=2026-10-03` | Renders unit metadata, exact dates, rent × 3, deposit 10% refundable, deposit due now highlight, contract notice | Render error card if unit not found |

</frozen-after-approval>

## Open Questions

## Code Map

- `contracts/openapi.yaml` -- Declare `POST /api/v1/reservations`, `GET /api/v1/reservations/{id}`, and `ReservationDto` schema.
- `backend/src/main/java/com/storagehub/entity/Reservation.java` -- Update JPA Entity with price snapshot fields (`baseRent`, `totalRent`, `monthlyRate`, `policyVersion`).
- `backend/src/main/java/com/storagehub/repository/ReservationRepository.java` -- Add overlap queries with pessimistic/optimistic lock or conflict check.
- `backend/src/main/java/com/storagehub/service/ReservationService.java` -- Service handling transactional availability re-check, snapshotting, and reservation creation.
- `backend/src/main/java/com/storagehub/controller/ReservationController.java` -- Expose `POST /api/v1/reservations` and `GET /api/v1/reservations/{id}`.
- `backend/src/test/java/com/storagehub/reservation/ReservationServiceTests.java` -- Unit tests for reservation creation, 409 concurrency conflict, and snapshot preservation.
- `backend/src/test/java/com/storagehub/reservation/ReservationControllerTests.java` -- Integration tests for reservation endpoints.
- `frontend/src/types/reservation.ts` -- TypeScript interfaces for Reservation and create request.
- `frontend/src/api/reservation.ts` -- Axios client `createReservation` and `getReservation`.
- `frontend/src/mocks/handlers.ts` -- MSW handler for `POST /api/v1/reservations` and `GET /api/v1/reservations/:id`.
- `frontend/src/pages/booking/BookingSummaryPage.tsx` -- Booking Summary screen (`/booking/summary`).
- `frontend/src/router/routes.tsx` -- Route `/booking/summary` connected to `BookingSummaryPage`.
- `frontend/src/test/booking-summary.test.tsx` -- RTL tests for Booking Summary rendering, confirm action, and 409 bounce with toast.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Declare `POST /api/v1/reservations` and reservation schemas -- Enforce contract-first design.
- [x] `backend/src/main/java/com/storagehub/entity/Reservation.java` -- Add price snapshot fields to `Reservation` entity -- Lock terms immutably (AD-11).
- [x] `backend/src/main/java/com/storagehub/service/ReservationService.java` -- Implement atomic reserve re-check and creation -- Guard against double booking (FR-5).
- [x] `backend/src/main/java/com/storagehub/controller/ReservationController.java` -- REST controller exposing reservation creation -- Standard error envelopes.
- [x] `backend/src/test/java/com/storagehub/reservation/` -- Backend unit and controller tests covering race conflicts and snapshots -- Verify robust backend.
- [x] `frontend/src/types/` & `frontend/src/api/` -- Reservation types and Axios API client -- Establish FE client interface.
- [x] `frontend/src/mocks/handlers.ts` -- MSW handler for reservation creation with 409 conflict simulation -- Support tests and mock demo.
- [x] `frontend/src/pages/booking/BookingSummaryPage.tsx` -- Booking Summary screen with terms review, contract notice, and 409 bounce toast -- Deliver F1-05.
- [x] `frontend/src/test/booking-summary.test.tsx` -- RTL tests for Booking Summary, confirmation, and 409 error bounce -- Ensure FE quality.

**Acceptance Criteria:**
- Given a customer confirming booking on `/booking/summary`, when submitted, then a `PENDING_PAYMENT` Reservation with unique code `BK-` is created with an immutable price snapshot.
- Given two concurrent reservation requests for the same unit and dates, when evaluated, then one succeeds with 201 and the other receives 409 `UNIT_UNAVAILABLE`.
- Given a 409 conflict error upon reservation confirmation, when caught by frontend, then the user is bounced to `/units` with toast `"{code} was just reserved. Similar units still available."`.
- Given Booking Summary page, when rendered, then all financial line items (rent × duration, surcharges, deposit due now) matching `PricingEngine` and legal notice are displayed.

## Implementation Notes
- Implemented `POST /api/v1/reservations` with atomic double-booking check and turnover buffer validation in `ReservationService`.
- Added immutable pricing snapshot in `Contract` draft entity with `BK-` reservation code generation.
- Created `BookingSummaryPage` at `/booking/summary` with itemized breakdown, 10% refundable deposit highlight, and 409 conflict bounce to `/units` with toast.

## Spec Change Log
- None.

## Review Triage Log
- ✅ **Blind Hunter / Edge Case / Verification Gap / Acceptance Auditor:** All 13 backend unit & controller tests pass; all 10 frontend test suites (122 tests) pass with clean build. Contract and error envelope adhere to specs and OpenAPI definitions. (Zero findings, clean review).

## Design Notes

Booking Summary (F1-05) layout:
- Centered container (max 680px) with card layout.
- Header: "Review Booking Terms" + unit mono badge.
- Summary Sections:
  1. Storage Unit & Facility (Code, Type, Dimensions, Floor, Access method, Facility Depot).
  2. Rental Timeline (Start date, End date, Duration in months).
  3. Itemized Financial Terms: Monthly base rent, Base rent for term, Refundable Deposit (10%), Deposit due now (bold indigo highlight).
  4. Legal & Check-in Notice: "Your rental agreement will be auto-drafted from these exact terms and signed during check-in. Deposit is 100% refundable upon move-out settlement."
- Actions: "Back" secondary button + "Confirm & Proceed to Payment" primary button.

## Verification

**Commands:**
- `mvn test -Dtest=ReservationServiceTests,ReservationControllerTests` -- expected: All reservation tests pass.
- `npm run test` (in `frontend/`) -- expected: All frontend tests pass including `booking-summary.test.tsx`.
- `npm run build` (in `frontend/`) -- expected: Clean TypeScript and Vite bundle build.
