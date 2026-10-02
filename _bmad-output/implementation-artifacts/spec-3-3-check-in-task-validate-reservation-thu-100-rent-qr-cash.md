---
title: 'Story 3.3: Check-in Task — validate reservation + thu 100% rent (QR/Cash)'
type: 'feature'
created: '2026-10-03'
status: 'in-progress'
baseline_commit: 'e8e3c9c59329012370cb315dca5839db7b7aaf77'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Staff executing check-in tasks need a secure, guided desk workflow to validate customer reservation codes (`BK-`), verify paid deposit status, and collect the remaining 100% rental balance via either PayOS QR or cash-at-desk with staff confirmation, ensuring payment acts as the strict single source of truth without prematurely activating the rental prior to contract signing.

**Approach:** Implement `POST /api/v1/tasks/{id}/validate-reservation` validating reservation existence, `RESERVED` status, and confirmed deposit, returning a two-line breakdown (10% deposit held vs 100% rent due — FR-15). In the check-in task interface (`TaskDetailPage` / `/tasks/:id`), provide desk actions to collect 100% rent via `PaymentModal` supporting both **PayOS QR** and **Cash at Desk** (`PENDING_CASH` with staff `"Cash received"` confirmation). On rent payment confirmation, persist the rent receipt and advance task payment status while strictly holding reservation in `RESERVED` until the contract ritual in Story 3.4.

## Boundaries & Constraints

**Always:**
- Reservation validation requires `RESERVED` status and confirmed deposit payment. Invalid cases (not found, unpaid deposit, expired, or already checked-in) must halt progress with specific, actionable error messages.
- The financial breakdown must explicitly display **Deposit paid** and **100% Rent due** as two separate, distinct line items (FR-15).
- For the 100% rent touchpoint, `PaymentModal` allows both **PayOS QR** and **Cash at Desk** methods.
- Selecting Cash creates a `PENDING_CASH` payment; staff must explicitly click `"Cash received"` (`POST /api/v1/payments/{id}/confirm-cash`) to confirm receipt and generate the `CASH` receipt.
- Rent payment success generates exactly one `Receipt` (method `PAYOS QR` or `CASH`), logs audit trail via `LogService`, and records payment in the task.
- **Strict Invariant:** Successful 100% rent payment **DOES NOT** transition the reservation to `CHECKED_IN`. The reservation remains in `RESERVED` status until contract signing and access code release in Story 3.4.
- All endpoints are secured by staff/admin roles (`ROLE_STAFF`, `ROLE_FACILITY_MANAGER`, `ROLE_BUSINESS_OPS`, `ROLE_ADMIN`).

**Never:**
- Never advance a reservation to `CHECKED_IN` automatically upon rent payment.
- Never allow check-in validation for reservations without a verified deposit payment.
- Never allow non-staff users to confirm cash receipt.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Validate Valid Reservation | `POST /api/v1/tasks/{id}/validate-reservation` with valid `BK-` code | 200 OK `{ valid: true, depositAmount: 103500, totalRentDue: 1035000, reservation: {...} }` | N/A |
| Validate Unpaid Deposit | Reservation in `PENDING_PAYMENT` | 400/409 `{ valid: false, code: "DEPOSIT_UNPAID", message: "Deposit has not been paid" }` | Halts check-in progression |
| Validate Non-existent Code | Invalid `BK-9999` code | 404 `{ valid: false, code: "RESERVATION_NOT_FOUND", message: "Reservation BK-9999 not found" }` | Displays invalid code error |
| 100% Rent via PayOS QR | Staff initiates PayOS QR payment for rent | Payment created with QR data; polling awaits webhook; flips to `SUCCEEDED` (method `PAYOS`), receipt generated | Link cancellation allows retry/method switch |
| 100% Rent via Cash | Staff initiates Cash payment for rent | Payment in `PENDING_CASH`; staff clicks "Cash received" -> `SUCCEEDED` (method `CASH`), receipt generated | 403 if not staff |
| Successful Rent Paid State | 100% rent paid | Task displays "Rent Paid (Receipt RC-XXXX)"; reservation remains `RESERVED` awaiting contract signing | N/A |

</frozen-after-approval>

## Open Questions

## Code Map

- `contracts/openapi.yaml` -- Declare `POST /tasks/{id}/validate-reservation` and `CheckInValidationDto` schema.
- `backend/src/main/java/com/storagehub/dto/ValidateCheckInRequest.java` & `CheckInValidationDto.java` -- DTO records for check-in validation.
- `backend/src/main/java/com/storagehub/service/TaskService.java` -- Implement `validateCheckInReservation(Long taskId, String reservationCode)` and rent payment verification helper.
- `backend/src/main/java/com/storagehub/controller/TaskController.java` -- Expose `POST /api/v1/tasks/{id}/validate-reservation`.
- `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Ensure `RENT` purpose payments generate receipts without prematurely flipping reservation status to `CHECKED_IN`.
- `backend/src/test/java/com/storagehub/task/CheckInTaskTests.java` -- Unit & integration tests for validation cases (valid, missing deposit, expired, not found), cash confirmation, and rent-paid state invariants.
- `frontend/src/types/task.ts` & `frontend/src/api/task.ts` -- Add check-in validation request/response types and API function `validateCheckInReservation`.
- `frontend/src/components/payment/PaymentModal.tsx` -- Update to support `CASH` option with staff cash confirmation UI when `allowedMethods` includes `CASH`.
- `frontend/src/pages/staff/TaskDetailPage.tsx` -- Build check-in task detail screen replacing placeholder at `/tasks/:id` with reservation validation, two-line financial breakdown, rent payment trigger, and receipt banner.
- `frontend/src/router/routes.tsx` -- Wire real `TaskDetailPage` at `/tasks/:id`.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for validation and task detail endpoints.
- `frontend/src/test/check-in-task.test.tsx` -- Vitest RTL test suite for validation error handling, two-line financial display, QR/Cash rent payment, and receipt confirmation.

## Tasks & Acceptance

**Execution:**
- [ ] `contracts/openapi.yaml` -- Declare validation endpoint and DTO schema -- Contract-first specification.
- [ ] `backend/src/main/java/com/storagehub/` -- Implement `validateCheckInReservation` in `TaskService` and expose via `TaskController` -- Backend validation logic.
- [ ] `backend/src/test/java/com/storagehub/task/CheckInTaskTests.java` -- Add test coverage for all 3 validation cases and rent payment invariants -- Backend test verification.
- [ ] `frontend/src/types/` & `frontend/src/api/` -- Add check-in validation TypeScript types and API client functions -- Frontend data client.
- [ ] `frontend/src/components/payment/PaymentModal.tsx` -- Support `CASH` method and counter cash confirmation UI -- Multi-method payment modal.
- [ ] `frontend/src/pages/staff/TaskDetailPage.tsx` -- Build Check-in Task Detail page with validation, two-line rent breakdown, and payment trigger -- UI workflow.
- [ ] `frontend/src/router/routes.tsx` & `frontend/src/mocks/handlers.ts` -- Connect `/tasks/:id` route and MSW mock handlers -- Integration.
- [ ] `frontend/src/test/check-in-task.test.tsx` -- Vitest RTL tests for check-in task validation, QR/Cash payment, and rent receipt status -- UI verification.

**Acceptance Criteria:**
- Given a staff member opening a check-in task, when entering a valid `BK-` reservation code, then the system validates the booking and renders the two-line breakdown (Deposit paid vs 100% Rent due).
- Given an invalid reservation (unpaid deposit or non-existent code), then the system displays a specific error message and blocks payment progression.
- Given rent payment via PayOS QR, when webhook confirms payment, then a receipt is generated (method `PAYOS QR`) and rent is marked paid.
- Given rent payment via Cash at Desk, when staff clicks "Cash received", then payment flips to `SUCCEEDED` (method `CASH`) and receipt is generated.
- Given successful rent payment, then the reservation remains in `RESERVED` status (does NOT become `CHECKED_IN`) awaiting the contract ritual in Story 3.4.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `mvn test` -- expected: All backend check-in task tests pass (190+ tests).
- `npm run test` -- expected: All frontend tests pass including new `check-in-task.test.tsx`.
- `npm run build` -- expected: Production TypeScript and Vite build succeeds with 0 errors.
