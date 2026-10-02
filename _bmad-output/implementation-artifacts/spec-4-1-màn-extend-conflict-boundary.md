---
title: 'Story 4.1: Màn Extend + conflict boundary'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'c2d3e4f5012370cb315dca5839db7b7aaf80'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers with active rentals (`CHECKED_IN`) want to extend their rental duration safely without double-booking into upcoming reservations on the same unit. The user needs clear visibility on the calendar picker of the latest possible checkout date, immediate top-of-form error feedback when attempting to select a conflict date (NFR-8), and an itemized 2-line financial breakdown based on locked rental rate snapshots and deposit top-up rules.

**Approach:**
1. Provide `GET /api/v1/reservations/{id}/extension-boundary` to inspect upcoming reservations on the unit and determine `latestPossibleCheckoutDate` ($T - 1\text{ day}$ where $T$ is the start date of the next reservation).
2. Provide `POST /api/v1/reservations/{id}/extension-quote` validating the proposed `newEndDate` against conflict boundaries and computing:
   - **Line 1 (Additional Rent):** $\text{dailyRate} \times \text{additionalDays}$ (using the rental's original locked rate snapshot).
   - **Line 2 (Deposit Top-up AD-11):** $\max(0, \text{DepositRate}\% \times (\text{baseRent} + \text{additionalRent}) - \text{currentHeldDeposit})$.
3. Build `ExtensionModal` on the frontend with:
   - Calendar date picker bounded by current checkout and max allowed date.
   - Top-of-form error banner (NFR-8, not a toast): `"Can't extend to [date] — [unitCode] has a reservation starting [conflictDate]. Latest possible checkout is [latestDate]. Pick another date."`
   - Itemized 2-line financial breakdown card.
4. On `RentalDetailPage`, display "Extend Rental" button only for `CHECKED_IN` rentals that are not past `endDate`. Past `endDate` rentals have no Extend entry (late fees are computed at Settlement in Epic 6; no `OVERDUE` status).

## Boundaries & Constraints

**Always:**
- If an upcoming reservation begins on date $T$, the latest possible checkout date for the extension is $T - 1\text{ day}$.
- When a user inputs or submits a date $\ge T$, return 409 Conflict and render the prominent top-of-form error banner.
- Price calculation uses the locked monthly rate snapshot on the reservation, not current or modified policy rates (AD-11).
- Top-up calculation floors at zero: if new required deposit $\le$ currently held deposit, top-up is 0 (any excess is settled during checkout).
- Closing the extension modal without payment creates no state change or unconfirmed drafts (AD-4).

**Never:**
- Never allow an extension to overwrite or overlap an upcoming reservation.
- Never show an Extend button on rentals past their `endDate`.
- Never create unconfirmed draft records when a customer opens or closes the extension modal without paying.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Query Extension Boundary with Upcoming Conflict | `GET /api/v1/reservations/{id}/extension-boundary` (next booking on 2027-01-15) | 200 OK `{ latestPossibleCheckoutDate: "2027-01-14", conflictStartDate: "2027-01-15", conflictReservationCode: "BK-UPCOMING-99", isExtendable: true }` | N/A |
| Query Extension Boundary without Conflict | Unit has no upcoming reservations | 200 OK `{ latestPossibleCheckoutDate: null, isExtendable: true }` | N/A |
| Extension Quote Valid Date | `POST /api/v1/reservations/{id}/extension-quote` with `newEndDate = "2026-11-05"` | 200 OK `{ additionalDays: 31, additionalRent: 713000, currentHeldDeposit: 207000, newTotalDepositRequired: 278300, depositTopUp: 71300, totalFee: 784300 }` | N/A |
| Extension Quote Conflict Date | `POST /api/v1/reservations/{id}/extension-quote` with date $\ge T$ | 409 Conflict `{ code: "EXTENSION_DATE_CONFLICT", message: "Can't extend to ... latest possible checkout is ..." }` | Displays top-of-form error banner |
| Rental Past End Date | Rental status `CHECKED_IN` but `endDate < today` | Extend button is hidden on `RentalDetailPage`; API returns 409/400 | N/A |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Declare `/reservations/{id}/extension-boundary` and `/reservations/{id}/extension-quote`.
- `backend/src/main/java/com/storagehub/dto/ExtensionBoundaryDto.java`, `ExtensionQuoteRequest.java`, `ExtensionQuoteDto.java` -- DTO classes.
- `backend/src/main/java/com/storagehub/repository/ReservationRepository.java` -- Query `findUpcomingReservationsForUnit`.
- `backend/src/main/java/com/storagehub/service/ReservationService.java` -- Business logic for conflict boundary derivations and 2-line quote calculation.
- `backend/src/main/java/com/storagehub/controller/ReservationController.java` -- Expose extension boundary and quote endpoints.
- `backend/src/test/java/com/storagehub/extension/RentalExtensionBoundaryTests.java` -- Unit & boundary integration tests.
- `frontend/src/types/extension.ts` & `frontend/src/api/rental.ts` -- Extension TypeScript definitions and API client functions.
- `frontend/src/components/rentals/ExtensionModal.tsx` -- Modal component with calendar picker, NFR-8 error banner, and 2-line financial card.
- `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Integration of Extend button and modal.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for extension boundary and quote.
- `frontend/src/test/rental-extension.test.tsx` -- Vitest tests for extension modal, conflict banner, and financial calculation.

## Tasks & Acceptance

**Execution:**
- [x] Declare extension boundary and quote schemas in `contracts/openapi.yaml`.
- [x] Implement backend conflict checking and 2-line fee calculations in `ReservationService.java` and `ReservationController.java`.
- [x] Write backend tests in `RentalExtensionBoundaryTests.java`.
- [x] Implement frontend `ExtensionModal.tsx` with top-of-form conflict banner and itemized fee breakdown.
- [x] Integrate `ExtensionModal` into `RentalDetailPage.tsx`.
- [x] Write frontend tests in `rental-extension.test.tsx`.

**Acceptance Criteria:**
- Given a customer viewing a `CHECKED_IN` rental before `endDate`, when clicking "Extend Rental", then the modal opens showing the conflict boundary and allowed checkout window.
- Given a selected date beyond the latest allowed date, then a top-of-form error banner displays the exact conflict date and latest possible checkout date, disabling payment progression.
- Given a valid extension date, then the 2-line financial breakdown renders additional rent and deposit top-up computed from locked snapshots.
- Given a rental past its `endDate`, then the "Extend Rental" button is not shown.

## Verification

**Commands:**
- `mvn test` -- All extension boundary backend tests pass.
- `npm test` -- All frontend tests pass including `rental-extension.test.tsx`.
- `npm run build` -- Production build succeeds.
