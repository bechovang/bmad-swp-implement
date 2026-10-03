# Epic 6 Context: Checkout, Settlement & Turnover

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Provide a complete, transparent, and auditable end-to-end checkout and unit turnover workflow. Customers submit checkout requests with transparent deposit settlement explanations; staff verify unit and key handover through structured itemized inspections; damages and late fees are recorded with mandatory reasons; deposits are settled to the exact penny (with extra fees collected via QR/Cash if damages exceed deposit); permanent receipts are issued; and units transition to Preparing for cleaning and turnover buffering before becoming available again.

## Stories

- Story 6.1: Checkout Request
- Story 6.2: Checkout Task — nhận kho + Inspection
- Story 6.3: Settlement Charge + preview + Extra fee + đóng Rental
- Story 6.4: Settlement waiver trong trần WAIVER_CAP (P2)
- Story 6.5: Cleaning hoàn tất từ card + Turnover Buffer

## Requirements & Constraints

- **FR-17 (P1):** Checkout Request & Boundary Check: Customer submits request to checkout on a chosen date. Request is validated against the next reservation start date on the unit (same availability boundary rule as 4.1). Active reservation transitions to `CHECKOUT_REQUESTED`. A new `CHECKOUT` task card is created via `TaskService` registry in the same transaction. Submitting a new request supersedes and replaces any open checkout request.
- **FR-18 (P1):** Checkout Inspection & Settlement: Staff performs itemized inspection on unit & keys (`ACCESS_CARD`, `PADLOCK`, `CLEANLINESS`, `STRUCTURE`) recording status `OK`, `MINOR`, or `MAJOR`. Settlement charges require mandatory justification notes. Late return triggers `LATE_FEE`. Deposit minus charges equals refund; if charges exceed deposit, remaining balance is collected as `EXTRA_FEE` via PayOS QR or cash at desk. Closing rental sets status to `CLOSED`, transitions unit to `PREPARING`, creates `CLEANING` task, and saves permanent settlement receipt.
- **FR-19 & FR-20 (P1):** Turnover Buffer & Cleaning Completion: Staff marks Cleaning task complete directly on Kanban card. Unit transitions to `AVAILABLE` (or `RESERVED` if upcoming booking exists) only when the mandatory turnover buffer duration has elapsed; otherwise remains in `PREPARING`.
- **FR-41 (P2):** Charge Waiver Cap: Staff can waive charges up to `WAIVER_CAP` (e.g. 50,000 VND) with mandatory reason; exceeds cap is blocked.
- **NFR-7 & NFR-8:** Plain-word microcopy without technical jargon; clear deposit calculation breakdown and permanent customer receipts.

## Technical Decisions

- **Domain Model & Entities:**
  - `CHECKOUT_REQUESTS`: `id`, `reservation_id`, `requested_date`, `status` (`PENDING`, `COMPLETED`, `CANCELLED`), `notes`, `created_at`.
  - `INSPECTIONS`: `id`, `reservation_id`, `unit_id`, `inspector_staff_id`, `category` (`ACCESS_CARD`, `PADLOCK`, `CLEANLINESS`, `STRUCTURE`), `result` (`OK`, `MINOR`, `MAJOR`), `notes`, `created_at`.
  - `SETTLEMENTS`: `id`, `reservation_id`, `deposit_held`, `total_charges`, `refund_amount`, `extra_fee_amount`, `status` (`DRAFT`, `FINALIZED`), `created_at`.
  - `SETTLEMENT_CHARGES`: `id`, `settlement_id`, `charge_type` (`DAMAGE`, `LATE_FEE`, `CLEANING`, `KEY_REPLACEMENT`, `OTHER`), `amount`, `reason`, `waiver_amount`, `waiver_reason`.
- **State Machine Transitions:**
  - Reservation: `CHECKED_IN` -> `CHECKOUT_REQUESTED` -> `CLOSED`.
  - Unit: `RENTED` -> `PREPARING` (on checkout close) -> `AVAILABLE` (after cleaning + turnover buffer).
- **Task Registry:** Spawns `CHECKOUT` task on checkout request; spawns `CLEANING` task on settlement finalization.

## UX & Interaction Patterns

- **Customer Checkout Request:** Initiated from Rental Detail page. Date picker restricted by next reservation boundary, transparent 3-case settlement guide (Full Refund / Deduction / Extra Fee).
- **Staff Checkout Task & Inspection:** Itemized checklist for unit keys and 4 condition categories with OK/MINOR/MAJOR buttons.
- **Settlement Preview Modal:** Real-time financial arithmetic showing deposit held, itemized deductions, net refund, or required extra fee payment.

## Cross-Story Dependencies

- **Epic 2 & 3:** Depends on active checked-in reservations and `TaskService` Kanban registry.
- **Epic 4:** Reuses conflict boundary detection logic from rental extensions (Story 4.1).
- **Story 6.1:** Foundation for customer-initiated checkout and task generation.
- **Story 6.2:** Follows 6.1 with inspection checklist.
- **Story 6.3:** Concludes settlement, collects extra fees, closes rental, and triggers cleaning.
- **Story 6.5:** Handles cleaning task and turnover buffer verification.
