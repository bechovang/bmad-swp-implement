---
title: 'Story 6.3: Settlement Charge + preview + Extra fee + đóng Rental'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'e2b777f1fae8a719c8f074d284a1a0f8b8bc92d1'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-6-context.md'
  - '_bmad-output/implementation-artifacts/spec-6-2-checkout-task-nhan-kho-inspection.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** After inspection of a vacated unit, staff must calculate the exact financial settlement between the held deposit, damage charges (if any), and late fees. If damage fees are assessed, a mandatory reason is required to maintain trust and prevent arbitrary deductions. If total charges exceed the deposit, the remaining balance must be collected as an Extra Fee (via PayOS QR or Cash at desk) before the rental can be closed. Once settled, the rental must transition to `CLOSED`, the unit to `PREPARING`, a `CLEANING` task spawned on the Kanban board, and a permanent settlement receipt made available for both customer and staff.

**Approach:**
1. **Settlement Calculation & Real-Time Preview (`SettlementService.java`, `SettlementController.java`):**
   - Provide `GET /api/v1/reservations/{reservationId}/settlement-preview` (and `POST /api/v1/reservations/{reservationId}/settlement-preview`):
     - Inputs: optional `damageFee`, `damageReason`, `checkoutDate`.
     - Deposit held is retrieved from the reservation (e.g. 172.500 ₫).
     - Late fee is auto-computed if checkout date > reservation `endDate` (days late × daily rate).
     - Total deductions = `damageFee + lateFee`.
     - If `depositHeld >= totalDeductions`: `refundAmount = depositHeld - totalDeductions`, `extraFeeAmount = 0`.
     - If `totalDeductions > depositHeld`: `refundAmount = 0`, `extraFeeAmount = totalDeductions - depositHeld`.
     - Mandatory validation: If `damageFee > 0` and `damageReason` is blank -> flag `damageReasonRequired` and block confirmation.
2. **Extra Fee Collection Touchpoint:**
   - If `extraFeeAmount > 0`, customer/staff can choose:
     - **PayOS QR:** Initiates a payment order for `EXTRA_FEE` via PayOS QR code.
     - **Cash at desk:** Creates a `PENDING_CASH` payment and allows staff to confirm "Cash received" at desk.
   - Rental closure is blocked if `extraFeeAmount > 0` and extra fee payment has not succeeded (`409 EXTRA_FEE_UNPAID`).
3. **Atomic Closure Transaction (`POST /api/v1/reservations/{reservationId}/settlement`):**
   - Executes in a single transactional boundary:
     - Finalizes `Settlement` entity with unique `ReceiptCode` (`STL-YYYY-XXXX`), `depositHeld`, `damageFee`, `damageReason`, `lateFee`, `refundAmount`, `extraFeeAmount`, `status = FINALIZED`.
     - Flips `Reservation.status` from `CHECKOUT_REQUESTED` / `CHECKED_IN` -> `CLOSED`.
     - Flips active `Contract.status` -> `CLOSED`.
     - Flips `Unit.status` from `RENTED` -> `PREPARING`.
     - Spawns `CLEANING` task card via `TaskService.createCleaningTask(unit)`.
     - Emits `DAMAGE_CHARGE` audit log (if `damageFee > 0`), `STATUS_CHANGE` audit log for reservation & unit.
     - Sends settlement receipt notification to customer.
     - Completes the `CHECKOUT` task card (`TaskStatus.DONE`), satisfying snap-back guard.
4. **Permanent Settlement Receipt (`RentalDetailPage.tsx` & `SettlementReceiptModal.tsx`):**
   - Both customer and staff can view the immutable receipt permanently at `GET /api/v1/reservations/{reservationId}/settlement`:
     - Displays formatted breakdown: "Refund 132.500 ₫ after damage fee 40.000 ₫" or "Extra fee 20.000 ₫ paid via Cash".
5. **Automated Verification:**
   - 100% test coverage across backend (`mvn test`) and frontend (`npx vitest run`), clean build.

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/migration/V9__settlements_enhancements.sql`: Flyway migration enhancing `settlements` table.
- `backend/src/main/java/com/storagehub/entity/Settlement.java`: JPA entity for Settlement records.
- `backend/src/main/java/com/storagehub/repository/SettlementRepository.java`: JPA repository for settlements.
- `backend/src/main/java/com/storagehub/dto/SettlementPreviewDto.java` & `FinalizeSettlementRequest.java` & `SettlementReceiptDto.java`: DTOs.
- `backend/src/main/java/com/storagehub/service/SettlementService.java`: Service orchestrating settlement calculation, validation, extra fee checks, and atomic closure.
- `backend/src/main/java/com/storagehub/controller/SettlementController.java`: REST endpoints for settlement preview, finalize, and receipt retrieval.
- `backend/src/main/java/com/storagehub/service/TaskService.java`: Added `createCleaningTask(Unit unit)` and updated snap-back guard.
- `contracts/openapi.yaml`: OpenAPI contract specifications.
- `frontend/src/types/settlement.ts`: TypeScript types for settlement preview, request, and receipt.
- `frontend/src/api/settlement.ts`: API client functions for settlement operations.
- `frontend/src/components/checkout/CheckoutSettlementSection.tsx`: Settlement calculation & closure UI for staff in Checkout Task detail.
- `frontend/src/pages/rentals/RentalDetailPage.tsx`: Permanent settlement receipt banner and breakdown for customer & staff.
- `frontend/src/mocks/handlers.ts`: MSW mock handlers for settlement endpoints.
- `frontend/src/test/settlement-flow.test.tsx`: Vitest suite for settlement calculation, extra fee, and receipt view.

## Implementation Plan

1. **Database & Backend Entity/Repo:**
   - Create `V9__settlements_enhancements.sql` adding `DepositHeld`, `LateFee`, `ExtraFee`, `Status`, `CreatedAt`, `Notes`, making `ContractID` nullable.
   - Implement `Settlement` entity and `SettlementRepository`.
2. **Backend Service & Controller:**
   - Implement `SettlementService` with preview arithmetic, mandatory `damageReason` checks, late fee logic, extra fee payment validation, and atomic `CLOSED` transaction.
   - Add `createCleaningTask(Unit unit)` in `TaskService`.
   - Implement `SettlementController` (`GET/POST .../settlement-preview`, `POST .../settlement`, `GET .../settlement`).
3. **OpenAPI Specification:**
   - Document new endpoints and schemas in `contracts/openapi.yaml`.
4. **Frontend API & Components:**
   - Add types in `types/settlement.ts` and API in `api/settlement.ts`.
   - Implement `CheckoutSettlementSection.tsx` inside `TaskDetailPage.tsx` and `CheckoutTaskModal.tsx`.
   - Update `RentalDetailPage.tsx` to render immutable Settlement Receipt when `reservation.status === 'CLOSED'`.
   - Update MSW mocks in `handlers.ts`.
5. **Testing & Verification:**
   - Backend tests (`SettlementServiceTests.java`, `SettlementControllerTests.java`).
   - Frontend tests (`settlement-flow.test.tsx`).
   - Run full verification suite (`mvn test`, `npx vitest run`, `npm run build`).

## Acceptance Criteria

- **AC-1:** Given inspection completed, staff can view and calculate settlement preview with deposit held, damage fee, and late fee.
- **AC-2:** Entering a `damageFee > 0` strictly requires a `damageReason`. Leaving reason blank disables confirmation and returns `DAMAGE_REASON_REQUIRED` (400/422).
- **AC-3:** If checkout date is past reservation `endDate`, `LATE_FEE` is automatically computed and displayed as an itemized line.
- **AC-4:** Settlement preview calculates exact arithmetic:
  - Deposit Held - Total Charges = Refund (e.g. 172.500 ₫ - 40.000 ₫ = 132.500 ₫ refund).
  - If Total Charges > Deposit Held, the difference is calculated as `Extra Fee` (e.g. Charges 200.000 ₫ - Deposit 172.500 ₫ = 27.500 ₫ Extra fee).
- **AC-5:** When `Extra fee > 0`, confirming settlement is blocked until extra fee is paid (via PayOS QR or Cash at counter).
- **AC-6:** Finalizing settlement in a single atomic transaction:
  - Updates Reservation status -> `CLOSED`.
  - Updates Unit status -> `PREPARING`.
  - Spawns `CLEANING` task card via `TaskService`.
  - Generates immutable `Settlement` receipt record (`STL-YYYY-XXXX`).
  - Allows `CHECKOUT` task card to transition to `DONE`.
- **AC-7:** Both customer and staff can view the permanent settlement receipt in `RentalDetailPage.tsx`.
- **AC-8:** 100% automated tests pass across backend and frontend with zero regressions.
