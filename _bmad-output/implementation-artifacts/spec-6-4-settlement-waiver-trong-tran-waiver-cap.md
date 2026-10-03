# Spec 6.4: Settlement waiver trong trần WAIVER_CAP (P2)

## Story Information
- **Story ID:** `6-4-settlement-waiver-trong-trần-waiver_cap-p2`
- **Epic:** Epic 6: Checkout, Settlement & Turnover
- **Status:** done

## Goal
Provide staff with the capability to apply a controlled discount / fee waiver on settlement charges (damages or late fees) up to `WAIVER_CAP` (e.g., 50,000 VND from active `RentalPolicy` rule `WAIVER_CAP` or fallback default 50,000 VND) with a mandatory reason. Waiving beyond the cap is strictly blocked with informative feedback. The adjustment and its reason are permanently recorded in the settlement receipt and logged in the append-only activity log under action `WAIVER`. Deposit forfeiture for no-show is strictly excluded from waiver eligibility.

## Acceptance Criteria
1. **Waiver Cap Enforcement (`WAIVER_CAP`):**
   - Settlement calculation loads `WAIVER_CAP` from active `RentalPolicy` rules (or fallback 50,000 VND).
   - Staff can enter a waiver amount up to `WAIVER_CAP`.
   - The total deduction is adjusted: `netCharges = Math.max(0, (damageFee + lateFee) - waiverAmount)`.
2. **Mandatory Waiver Reason:**
   - Any waiver amount $> 0$ strictly requires a non-blank reason (`waiverReason`).
   - Missing reason blocks confirmation with `400 WAIVER_REASON_REQUIRED`.
3. **Exceeding Cap Guard:**
   - Entering a waiver amount $> \text{WAIVER\_CAP}$ disables confirmation button and displays clear warning: `"Waiver exceeds the 50.000 ₫ cap in Rental Policy v3"`.
   - Backend strictly rejects with `400 WAIVER_EXCEEDS_CAP`.
4. **No-Show Forfeiture Exclusion:**
   - Waiver cannot be applied to deposit forfeiture for expired / no-show reservations (`FR-36`).
5. **Permanent Receipt & Audit Trail:**
   - Receipt records `waiverAmount` and `waiverReason` for both customer and staff inspection.
   - Audit trail appends `Action.WAIVER` with `entityType = SETTLEMENT`, `newValue = waiverAmount`, and `reason = waiverReason`.

## Technical Plan
1. **Migration (`V10__settlement_waivers.sql`):**
   - Add `WaiverAmount DECIMAL(15,0) NOT NULL DEFAULT 0` and `WaiverReason VARCHAR(255) NULL` to `settlements` table.
2. **Backend Domain & Services:**
   - Update `Settlement.java` with `waiverAmount` and `waiverReason`.
   - Update `PolicyRuleRepository.java` with query method for `WAIVER_CAP`.
   - Update `SettlementService.java` to calculate net charges with waiver, enforce `WAIVER_CAP`, validate mandatory reason, and record `Action.WAIVER` audit log.
   - Update `SettlementPreviewDto.java`, `FinalizeSettlementRequest.java`, and `SettlementReceiptDto.java`.
   - Update `SettlementController.java` to pass waiver query parameters to preview and finalize endpoints.
   - Update backend tests in `SettlementTests.java` and `SettlementControllerTests.java`.
3. **Contracts & Frontend:**
   - Update `contracts/openapi.yaml`.
   - Update `frontend/src/types/settlement.ts`.
   - Update `frontend/src/api/settlement.ts`.
   - Update `frontend/src/components/checkout/CheckoutSettlementSection.tsx` with waiver row, live cap validation, warning message, and receipt itemization.
   - Update `frontend/src/pages/rentals/RentalDetailPage.tsx` receipt card.
   - Update MSW handlers in `frontend/src/mocks/handlers.ts`.
   - Write frontend tests in `frontend/src/test/settlement-waiver.test.tsx`.
