---
title: 'Story 2.5: My Rentals + Check-in Pass + Rental Detail cơ bản'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'fbb83fc57266a404b729227b3d6c524ab8c173a2'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers require dedicated portal views to manage their reservations and ongoing storage rentals, access their counter Check-in Pass (`BK-` code) to complete in-person contract signing, and review unit details, lease timelines, deposit status, and receipt history without frontend client-side financial recalculations.

**Approach:** Implement `GET /api/v1/reservations/my` and extend reservation details with payment receipt history. Build `MyRentalsPage` (`/rentals`) with 3 tabs (Active Rentals, Reservations, History), modal/view `CheckInPass` with prominent monospace `BK-` code and counter instructions, and `RentalDetailPage` (`/rentals/:id`) with modular card-based layout displaying unit specs, rental timeline, deposit status (`held`), and payment receipt ledger.

## Boundaries & Constraints

**Always:**
- My Rentals and Rental Detail are pointer surfaces: all monetary amounts, lease dates, and reservation statuses originate directly from backend source of truth without client-side recalculation (FR-35).
- Data access is role-scoped: Customer can only retrieve their own reservations and rentals; Staff and Facility Managers can query any customer rental.
- Check-in Pass displays the `BK-` reservation code in prominent monospace font with counter instructions (bring National ID, sign rental agreement, pay remaining 100% rent balance at reception).
- Rental Detail uses modular card-based layouts allowing future epics to add contract chains (Epic 3), addendums (Epic 4), and settlement receipts (Epic 6) without layout refactoring.
- Next-actions are strictly mapped to active business state; unimplemented future flows (Extend -> E4, Checkout Request -> E6, Support Ticket -> E5) must not render broken or dead links.
- Empty states must be factual and render exactly one primary CTA directing to Browse Units (`/units`).

**Never:**
- Never allow customers to view or access another user's rental or reservation (enforce 403 Forbidden).
- Never recalculate or mutate pricing/surcharges in frontend components.
- Never render non-functional dead action buttons.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| List My Rentals (Customer) | `GET /api/v1/reservations/my` with Customer JWT | 200 OK with list of reservations/rentals grouped/tabbed by status | 401 if unauthenticated |
| Get Rental Detail (Owner) | `GET /api/v1/reservations/{id}` by owning Customer | 200 OK with reservation details, unit specs, locked price snapshot, and payment receipt history | 404 if not found; 403 if belonging to different user |
| Get Rental Detail (Staff/Admin) | `GET /api/v1/reservations/{id}` with Staff JWT | 200 OK with full reservation and payment details | 404 if not found |
| Render My Rentals (Empty) | Customer with 0 reservations/rentals | Factual empty state ("You have no active rentals") + 1 CTA to `/units` | N/A |
| Open Check-in Pass | Customer clicks "View Check-in Pass" for `RESERVED` reservation | Modal / sheet opens showing large monospace `BK-` code + counter instructions | N/A |
| Tab Switching | User toggles Active Rentals / Reservations / History | Cards filtered to respective statuses (`CHECKED_IN`, `PENDING_PAYMENT`/`RESERVED`, `CLOSED`/`EXPIRED`) | N/A |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Declare `GET /reservations/my` endpoint and enrich `ReservationDto` schema with `payments` receipt list.
- `backend/src/main/java/com/storagehub/repository/ReservationRepository.java` -- Add `findByCustomerIdOrderByCreatedAtDesc` query method.
- `backend/src/main/java/com/storagehub/repository/PaymentRepository.java` -- Add query method `findByReservationIdOrderByCreatedAtAsc`.
- `backend/src/main/java/com/storagehub/dto/ReservationDto.java` -- Add `payments` list to transfer receipt ledger.
- `backend/src/main/java/com/storagehub/service/ReservationService.java` -- Implement `getMyReservations` and enrich `getReservation` with payment receipts.
- `backend/src/main/java/com/storagehub/controller/ReservationController.java` -- Expose `GET /api/v1/reservations/my`.
- `backend/src/test/java/com/storagehub/reservation/ReservationControllerTests.java` -- Controller tests for `/my` and detail endpoint with payments.
- `backend/src/test/java/com/storagehub/reservation/ReservationServiceTests.java` -- Service unit tests for customer-scoped query and payments enrichment.
- `frontend/src/types/rental.ts` -- TypeScript models for rentals, reservations, and receipts.
- `frontend/src/api/rental.ts` -- API client functions for my rentals and rental detail.
- `frontend/src/components/rentals/CheckInPassModal.tsx` -- Monospace code display and counter instructions modal.
- `frontend/src/pages/rentals/MyRentalsPage.tsx` -- 3-tab listing with status badges, factual empty state, and action buttons.
- `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Detail view with unit info, schedule, deposit status (held), and receipts ledger.
- `frontend/src/router/routes.tsx` -- Update router config with real `MyRentalsPage` and `RentalDetailPage`.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for `/api/v1/reservations/my` and `/api/v1/reservations/:id`.
- `frontend/src/test/my-rentals.test.tsx` -- Vitest RTL tests for tab navigation, empty states, and Check-in Pass modal.
- `frontend/src/test/rental-detail.test.tsx` -- Vitest RTL tests for rental detail, deposit status, and receipt list.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Add `GET /reservations/my` and payments list in ReservationDto -- Enforce contract-first specification.
- [x] `backend/src/main/java/com/storagehub/repository/` -- Add repository query methods in `ReservationRepository` and `PaymentRepository` -- Enable customer-scoped lookups.
- [x] `backend/src/main/java/com/storagehub/dto/ReservationDto.java` -- Update DTO to include payments list -- Deliver receipts in response.
- [x] `backend/src/main/java/com/storagehub/service/ReservationService.java` -- Implement `getMyReservations` and payment enrichment -- Service layer business logic.
- [x] `backend/src/main/java/com/storagehub/controller/ReservationController.java` -- Expose `GET /api/v1/reservations/my` -- REST endpoint mapping.
- [x] `backend/src/test/java/com/storagehub/reservation/` -- Add unit and integration tests -- Guarantee backend correctness.
- [x] `frontend/src/types/` & `frontend/src/api/` -- Implement `rental.ts` types and API client -- Frontend data layer.
- [x] `frontend/src/components/rentals/CheckInPassModal.tsx` -- Build Check-in Pass modal with large monospace code and desk instructions -- UX check-in pass.
- [x] `frontend/src/pages/rentals/MyRentalsPage.tsx` -- Implement My Rentals tabbed page with status badges and factual empty state -- Anchor portal screen.
- [x] `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Implement Rental Detail screen with unit specs, timeline, deposit status, and receipts -- Core detail view.
- [x] `frontend/src/router/routes.tsx` & `frontend/src/mocks/handlers.ts` -- Connect routes and MSW mock handlers -- Full integration.
- [x] `frontend/src/test/` -- Implement Vitest RTL test suites for MyRentals and RentalDetail -- Guarantee frontend verification.

**Acceptance Criteria:**
- Given an authenticated customer with reservations in various states, when `GET /api/v1/reservations/my` is called, then only reservations belonging to that customer are returned.
- Given a customer viewing My Rentals, when switching tabs (Active Rentals, Reservations, History), then cards are filtered correctly with status badges and valid next-action buttons (no dead buttons).
- Given a customer with a `RESERVED` booking, when opening Check-in Pass, then the `BK-` code is displayed in large monospace font along with in-person desk instructions.
- Given a customer navigating to `/rentals/:id`, then the screen renders unit specifications, lease timeline, deposit status (`held`), and all associated payment receipts showing payment method (`PAYOS QR` / `CASH`).
- Given a customer with no rentals in a tab, then a factual empty state is rendered with a single CTA directing to `/units`.

## Implementation Notes
- Added `GET /api/v1/reservations/my` endpoint in `ReservationController` and `ReservationService` scoped to authenticated customer ID.
- Enriched `ReservationDto` with `List<PaymentDto> payments` field populated from `PaymentRepository.findByReservationIdOrderByCreatedAtAsc`.
- Created `frontend/src/pages/rentals/MyRentalsPage.tsx` with 3-tab layout (Active Rentals, Reservations, History), status badges, factual empty state with CTA to `/units`, and conditional next-action buttons.
- Created `frontend/src/components/rentals/CheckInPassModal.tsx` displaying prominent monospace `BK-` reservation code and desk check-in instructions.
- Created `frontend/src/pages/rentals/RentalDetailPage.tsx` with modular sections for Unit Specifications, Rental Schedule Timeline, Financial Terms & Deposit Status (`Held`), and Payment Receipts Ledger (`PAYOS QR` / `CASH`).
- Added MSW mock handlers and comprehensive Vitest RTL test suites (`my-rentals.test.tsx`, `rental-detail.test.tsx`).
- Verified all 153 backend tests (`mvn test`) and 133 frontend tests (`npm test` & `npm run build`) passed with zero errors.

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Evidence | Action |
|---------|---------|----------|--------|
| Unconditional Held deposit status & missing pending notice | Medium | Displaying Held on unpaid reservations is misleading | Patched with dynamic badge helper and pending notice banner |
| Null check & safe orderCode parsing in ReservationService | Low | Preempt potential NPE on detached payer associations | Patched with null check and fallback to payment id |
| Accessibility attribute on RentalDetailPage back button | Low | Improved screen reader semantics with aria-label | Patched with explicit aria-label |
| Missing createdAt column in Payment entity | False | V1 Flyway schema does not include a created_at column on payments table | Dismissed |
| Ambiguous 100% rent calculation in Check-in Pass | False | Domain rule states 100% rent is due at desk check-in, deposit is separate | Dismissed |
| Inconsistent query ordering in Payment/Reservation repo | False | Primary keys are auto-incrementing integers reflecting insertion order | Dismissed |
| Missing pagination on GET /reservations/my | False | Customer portal dataset is inherently small and scoped to current user | Dismissed |

## Verification

**Commands:**
- `mvn test` -- expected: All backend unit and controller tests pass (150+ tests).
- `npm run test` -- expected: All frontend Vitest tests pass including `my-rentals.test.tsx` and `rental-detail.test.tsx`.
- `npm run build` -- expected: TypeScript and Vite production build succeeds with zero errors.
