---
title: 'Story 2.1: PricingEngine + Unit Detail bảng giá minh bạch'
type: 'feature'
created: '2026-10-02'
status: 'done'
baseline_commit: 'ef9c9c51eaac42d8dc1cd04ded9de6ad81ae7bf4'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers exploring storage units lack rich unit specifications (dimensions, floor level, access mechanism, security provisions) and cannot view an exact, transparent pricing breakdown before committing to a reservation.

**Approach:** Implement a centralized backend `PricingEngine` reading the active `RentalPolicy` (v3) to compute base rent, surcharge lines, and a refundable 10% deposit in `BigDecimal` VND. Expose `GET /api/v1/units/{code}` and `GET /api/v1/pricing/calculate`. On the frontend, build the full-page `UnitDetailPage` (`/units/:code`) featuring unit photos (`/units/{code}.jpg`), specification chips, interactive duration selector with live pricing breakdown, and a Reserve CTA.

## Boundaries & Constraints

**Always:**
- Single source of truth for pricing calculations is backend `PricingEngine` (AD-11). Components and client code must never compute prices, deposits, or surcharges (AD-7).
- All monetary amounts use `BigDecimal` / `DECIMAL(15,0)` / JSON numbers formatted via `formatMoney` (`1.150.000 ₫`) with tabular numerals.
- Active policy resolution picks `status = 1` and `effectiveDate <= queryDate` (seed demo: policy v3).
- Seed demo baseline test check: Unit S-3 (Type S) at 345.000 ₫/month × 3 months = 1.035.000 ₫ base rent, Deposit 10% = 103.500 ₫ refundable.
- Unit photos load from `public/units/{code}.jpg` with a graceful UI fallback if missing.
- Design tokens adhere to "Control Room" system: indigo `#4F46E5` CTA, 3px left status bar, tabular numerals, light mode only.

**Never:**
- Never hardcode or calculate prices in frontend components or templates.
- Never use floating point numbers (`float`/`double`) for monetary calculations.
- Never allow unauthenticated access to unit details or pricing APIs (all app routes require JWT except the 3 auth endpoints and webhook).
- Never modify the database schema via manual DDL or `hbm2ddl` (all schema is owned by Flyway).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| S-3 Standard 3 Months | `unitCode: "S-3"`, `durationMonths: 3`, `startDate: "2026-10-03"` | `monthlyRate: 345000`, `baseRent: 1035000`, `depositRate: 10`, `depositAmount: 103500`, `depositRefundable: true`, `totalRent: 1035000` | N/A |
| M-5 1 Month Pricing | `unitCode: "M-5"`, `durationMonths: 1` | `monthlyRate: 690000`, `baseRent: 690000`, `depositRate: 10`, `depositAmount: 69000`, `depositRefundable: true` | N/A |
| Unknown Unit Code | `GET /api/v1/units/UNKNOWN-99` | 404 Resource Not Found envelope | `ResourceNotFoundException("Unit UNKNOWN-99 not found")` |
| Invalid Duration | `durationMonths: 0` or negative | 400 Invalid Request envelope | Field error on `durationMonths`: must be >= 1 |
| Missing Active Policy | No active `RentalPolicy` in DB | 409 Conflict envelope | `BusinessRuleException("ACTIVE_POLICY_NOT_FOUND")` |
| Unit Detail Page Load | Navigate to `/units/S-3` | Full-page view: photo, specs, live duration selector, pricing breakdown table, Reserve CTA | Render error card if API fails |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Declare `/api/v1/units/{code}` and `/api/v1/pricing/calculate` with DTO schemas.
- `backend/src/main/java/com/storagehub/entity/` -- Add JPA entities: `UnitType`, `Facility`, `Zone`, `Unit`, `RentalPolicy`, `PolicyRule`, `UnitStatus`, `PolicyRuleType`.
- `backend/src/main/java/com/storagehub/repository/` -- Add `UnitRepository`, `RentalPolicyRepository`, `PolicyRuleRepository`.
- `backend/src/main/java/com/storagehub/dto/` -- Add `UnitDetailDto`, `PricingBreakdownDto`, `PricingCalculateRequest`, `SurchargeItemDto`.
- `backend/src/main/java/com/storagehub/service/PricingEngine.java` -- Core pricing calculator implementing policy resolution and deposit breakdown.
- `backend/src/main/java/com/storagehub/service/UnitService.java` -- Service reading unit specifications, features, and default pricing.
- `backend/src/main/java/com/storagehub/controller/UnitController.java` -- REST controller for `GET /api/v1/units/{code}`.
- `backend/src/main/java/com/storagehub/controller/PricingController.java` -- REST controller for `GET /api/v1/pricing/calculate`.
- `frontend/src/types/unit.ts` & `frontend/src/types/pricing.ts` -- TypeScript interfaces matching contract schemas.
- `frontend/src/api/unit.ts` & `frontend/src/api/pricing.ts` -- Axios API client methods.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for `/api/v1/units/:code` and `/api/v1/pricing/calculate`.
- `frontend/src/pages/unit/UnitDetailPage.tsx` -- Main Unit Detail page component.
- `frontend/src/router/routes.tsx` -- Connect `UnitDetailPage` to `/units/:code`.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Add `GET /api/v1/units/{code}` and `GET /api/v1/pricing/calculate` endpoints and schemas -- Enforce contract-first API design.
- [x] `backend/src/main/java/com/storagehub/entity/` -- Create JPA entities for `Unit`, `UnitType`, `Facility`, `Zone`, `RentalPolicy`, `PolicyRule`, and associated enums -- Map existing Flyway tables.
- [x] `backend/src/main/java/com/storagehub/repository/` -- Create `UnitRepository`, `RentalPolicyRepository`, and `PolicyRuleRepository` -- Provide data access layer.
- [x] `backend/src/main/java/com/storagehub/service/PricingEngine.java` -- Implement pricing engine calculating base rent, surcharges, and refundable deposit according to active policy -- Enforce single pricing calculation source (AD-11).
- [x] `backend/src/main/java/com/storagehub/service/UnitService.java` -- Implement unit detail retrieval with security features and specs -- Supply unit catalog data.
- [x] `backend/src/main/java/com/storagehub/controller/` -- Implement `UnitController` and `PricingController` -- Expose REST endpoints with standard error envelopes.
- [x] `backend/src/test/java/com/storagehub/pricing/PricingEngineTests.java` -- Unit tests verifying S-3 (345k * 3 = 1.035m, 10% dep = 103.5k) and policy resolution -- Guard calculation accuracy.
- [x] `backend/src/test/java/com/storagehub/unit/UnitControllerTests.java` -- Integration tests for Unit Detail and Pricing endpoints -- Verify security and response shapes.
- [x] `frontend/src/types/` & `frontend/src/api/` -- Implement TypeScript types and Axios clients for unit and pricing -- Establish FE contract boundary.
- [x] `frontend/src/mocks/handlers.ts` -- Add MSW mock handlers for unit details and pricing calculation -- Support offline/mock testing.
- [x] `frontend/src/pages/unit/UnitDetailPage.tsx` -- Build full-page Unit Detail screen with photo gallery, spec chips, duration selector, and live pricing breakdown -- Deliver Customer discovery UX (F1-04).
- [x] `frontend/src/test/unit-detail.test.tsx` -- RTL tests for Unit Detail rendering, duration changes, price updates, and Reserve CTA -- Ensure FE robustness.

**Acceptance Criteria:**
- Given active policy v3 seed, when querying pricing for S-3 with duration 3 months, then base rent equals 1.035.000 ₫, deposit equals 103.500 ₫ (marked refundable), and total rent equals 1.035.000 ₫.
- Given Unit Detail page for S-3 (`/units/S-3`), when rendered, then unit specs (dimensions, floor, access, security), unit photo (`/units/S-3.jpg`), live pricing breakdown, and Reserve CTA are displayed.
- Given a user changing duration selector on Unit Detail, when selecting different months (e.g. 1, 3, 6, 12), then the pricing breakdown updates dynamically via the PricingEngine API.

## Implementation Notes
- Created full domain entities and repositories for `Unit`, `UnitType`, `Facility`, `Zone`, `RentalPolicy`, `PolicyRule` mapped to existing Flyway schema.
- Implemented `PricingEngine` as the single calculation engine (AD-11, AD-7), computing base rent, surcharge breakdown, and 10% refundable deposit in `BigDecimal` VND.
- Built full-page `UnitDetailPage` (`/units/:code`) with photo card (`/units/{code}.jpg`), unit code in mono font with status badge, specifications grid, security provisions, interactive duration selector (1, 3, 6, 12 months), live itemized pricing breakdown, refundable deposit highlight card, and primary Reserve CTA button.
- Registered MSW mock handlers for `/api/v1/units/:code` and `/api/v1/pricing/calculate`.
- Verified 124 backend tests and 110 frontend tests passing with zero errors.

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Route | Evidence / Action |
|---------|---------|-------|-------------------|
| Invalid JPQL query syntax with `LIMIT 1` in `RentalPolicyRepository.java` | medium | patch | Replaced with Spring Data derived query `findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc`. |
| Hardcoded empty surcharges list in `PricingEngine.java` | false | rejected | Active policy v3 contains no upfront surcharge rules for initial booking; late checkout surcharges are evaluated in Epic 6 settlement. |
| Premature scale truncation on `depositRate` before calculation | low | patch | Preserved full rate precision during deposit calculation. |
| Missing null check on `UnitType` in `PricingEngine.java` | low | patch | Added null checks for unit and unit.getUnitType(). |
| Missing error banner on pricing calculation failure in `UnitDetailPage.tsx` | low | patch | Added inline error alert banner when pricing query fails. |
| Hardcoded policy badge and deposit % in `UnitDetailPage.tsx` | low | patch | Replaced with dynamic policy version and deposit rate from API response. |
| UTC timezone day shift hazard in date selector formatting | low | patch | Replaced `toISOString()` with local calendar formatting. |
| Missing upper-bound `@Max(120)` constraint on rental duration | low | patch | Added `@Max(120)` in `PricingController.java` and contract openapi schema with tests. |

## Design Notes

Unit Detail layout follows desktop-first 2-column structure:
- Left column (approx 7/12 width): Photo card (`/units/{code}.jpg`), unit code in mono font with status badge, specifications grid (Floor, Area, Access method, Zone/Depot), and security features list.
- Right column (approx 5/12 width): Transparent Pricing card with duration selector pill buttons (1 mo, 3 mo, 6 mo, 12 mo), start date picker, itemized breakdown table (`Monthly rate`, `Duration`, `Base rent`, `Deposit (10% refundable)`), prominent "Deposit due now" highlight, and primary Reserve CTA button.

## Verification

**Commands:**
- `mvn test -Dtest=PricingEngineTests,UnitControllerTests` -- expected: All pricing engine and unit tests pass.
- `mvn test` -- expected: Full backend test suite passes without regressions.
- `npm run test` (in `frontend/`) -- expected: All Vitest frontend tests pass.
- `npm run build` (in `frontend/`) -- expected: Frontend TypeScript compile and Vite bundle succeed cleanly.
