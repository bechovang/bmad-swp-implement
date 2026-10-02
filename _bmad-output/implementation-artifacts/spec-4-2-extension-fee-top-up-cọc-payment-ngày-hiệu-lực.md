---
title: 'Story 4.2: Extension fee + top-up cọc + payment + ngày hiệu lực'
type: 'feature'
created: '2026-10-03'
status: 'ready-for-dev'
baseline_commit: 'd3e4f5012370cb315dca5839db7b7aaf81'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When a customer decides to extend their rental period, they must pay the total extension fee (additional rent period + deposit top-up) through either PayOS QR online or Cash at the front desk. Upon confirmed payment, the rental's `endDate` must immediately shift to the new date, deposit held must be adjusted, a payment receipt must be recorded, notifications sent, and an addendum draft `CT-…-A1` generated so the desk staff can follow up for physical signing (Story 4.3).

**Approach:**
1. Support payment creation for purpose `EXTENSION_FEE` via `POST /api/v1/payments/create` (with payload containing `reservationId`, `amount`, `purpose: "EXTENSION_FEE"`, `method: "PAYOS"` or `"CASH"`, and `newEndDate`).
2. Integrate `PaymentModal` touchpoint into the Extension flow:
   - For **PayOS QR**: Customer pays online via QR. When payment is confirmed (`SUCCEEDED`), execute atomic extension application:
     - Shift `reservation.endDate` to `newEndDate`.
     - Update `reservation.depositAmount` by adding `depositTopUp`.
     - Update `reservation.baseRent` / `reservation.totalRent` by adding `additionalRent`.
     - Generate Receipt `RC-` (purpose `EXTENSION_FEE`, method `PAYOS`).
     - Auto-draft Addendum `CT-…-A1` with status `AWAITING_SIGNATURE` (signing deadline: 7 days).
     - Spawn a `CONTRACT` (Contract-signature) task on the Task Board for staff to track physical signature.
     - Dispatch notification with breakdown and new checkout date.
   - For **Cash at Desk**: Customer selects Cash. Payment is created with `PENDING_CASH`.
     - Spawns a `CONTRACT` task on the Task Board for staff with note "Collect extension cash + sign addendum".
     - `reservation.endDate` remains unchanged until staff clicks `"Cash received"` (`POST /api/v1/payments/{id}/confirm-cash`).
     - When staff confirms cash: payment becomes `SUCCEEDED` (method `CASH`), `endDate` shifts immediately, deposit and rent totals update, addendum `CT-…-A1` is drafted, and receipt is generated in one atomic transaction.

## Boundaries & Constraints

**Always:**
- Payment modal for extension fee must offer both **PayOS QR** and **Cash at Desk** options.
- The extension payment amount must exactly match `totalFee = additionalRent + depositTopUp`.
- Shifting `endDate` occurs immediately upon payment confirmation (`SUCCEEDED`) — the customer's new checkout date is officially locked because payment was collected.
- Addendum `CT-…-A1` is drafted automatically upon payment confirmation with a 7-day signing deadline. Failure to physically sign within 7 days does NOT revoke the extension date (addendum is procedural documentation — Story 4.3).
- Cash payments remain `PENDING_CASH` and do NOT shift `endDate` until confirmed by staff via `"Cash received"`.
- If payment fails or is cancelled, no state changes occur on the reservation.

**Never:**
- Never shift `endDate` on unconfirmed or pending cash payments.
- Never recalculate rates based on updated system policies; always use the locked monthly rate snapshot.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Create Extension Payment (QR) | `POST /api/v1/payments/create` `{ reservationId, amount: 759000, purpose: "EXTENSION_FEE", method: "PAYOS", newEndDate: "2027-02-05" }` | 201 Created with `checkoutUrl` and QR code; status `PENDING` | 404 if reservation not found; 409 if date conflicts |
| PayOS QR Payment Succeeded | Webhook / confirm call on extension payment | Payment `SUCCEEDED`, `reservation.endDate` updated, `depositAmount` updated, Addendum `CT-1042-A1` drafted, receipt generated, task created | Atomic rollback on error |
| Create Extension Payment (Cash) | `POST /api/v1/payments/create` with `method: "CASH"` | 201 Created with status `PENDING_CASH`; `CONTRACT` task spawned on board; `endDate` unchanged | N/A |
| Staff Confirms Extension Cash | `POST /api/v1/payments/{id}/confirm-cash` | Payment `SUCCEEDED`, `endDate` shifted, deposit updated, Addendum drafted, receipt generated | 403 if not staff |
| Payment Cancelled / Expired | Customer cancels payment in modal | Payment `EXPIRED`; reservation remains unchanged | N/A |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Update `PaymentPurpose` enum to include `EXTENSION_FEE`, add `newEndDate` to `CreatePaymentRequest`.
- `backend/src/main/java/com/storagehub/entity/PaymentPurpose.java` -- Add `EXTENSION_FEE`.
- `backend/src/main/java/com/storagehub/entity/Payment.java` -- Add `newEndDate` field or extension metadata.
- `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Implement atomic extension application on payment completion (shift `endDate`, update deposit and total rent, auto-draft addendum, spawn task, send notification).
- `backend/src/main/java/com/storagehub/service/ContractService.java` -- Method to create addendum `CT-…-A1` linked to base contract.
- `backend/src/test/java/com/storagehub/extension/RentalExtensionPaymentTests.java` -- Backend integration tests for QR and Cash extension payment flows.
- `frontend/src/types/payment.ts` & `frontend/src/api/payment.ts` -- Update payment types and API client.
- `frontend/src/components/rentals/ExtensionModal.tsx` & `RentalDetailPage.tsx` -- Hook extension quote into `PaymentModal` with `purpose="EXTENSION_FEE"`.
- `frontend/src/mocks/handlers.ts` -- Update mock handlers for `EXTENSION_FEE` payments, cash confirmation, and addendum drafting.
- `frontend/src/test/rental-extension-payment.test.tsx` -- Vitest tests for extension payment flow.

## Tasks & Acceptance

**Execution:**
- [ ] Update `contracts/openapi.yaml` with `EXTENSION_FEE` payment purpose and extension parameters.
- [ ] Implement backend `EXTENSION_FEE` processing in `PaymentService` for both QR and Cash confirmation paths.
- [ ] Implement addendum draft creation in `ContractService`.
- [ ] Write backend integration tests in `RentalExtensionPaymentTests.java`.
- [ ] Update frontend `ExtensionModal` and `RentalDetailPage` to trigger `PaymentModal` for extension fee.
- [ ] Update MSW mock handlers in `handlers.ts`.
- [ ] Write frontend tests in `rental-extension-payment.test.tsx`.

**Acceptance Criteria:**
- Given an extension quote, when customer clicks "Proceed to Payment", then `PaymentModal` opens with PayOS QR and Cash at desk options.
- Given PayOS QR payment confirmation, then the reservation `endDate` shifts immediately, held deposit updates, Addendum `CT-…-A1` is drafted, and receipt is generated.
- Given Cash payment, then payment is recorded as `PENDING_CASH` and a task is created on the Task Board without shifting `endDate` until staff confirms cash.
- Given staff clicking "Cash received" on cash extension, then payment is confirmed, `endDate` shifts immediately, and the addendum is drafted in the same transaction.

## Verification

**Commands:**
- `mvn test` -- All extension payment tests pass.
- `npm test` -- All frontend tests pass.
- `npm run build` -- Production build succeeds.
