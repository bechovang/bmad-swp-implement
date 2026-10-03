---
title: 'Story 6.1: Checkout Request'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'dbb60b422cfacb9c516b4f16207a38448f061f5d'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-6-context.md'
  - '_bmad-output/implementation-artifacts/spec-5-4-support-list-detail-drawer-khách.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers with active storage rentals need to initiate the checkout process by scheduling a key handover date at the depot and understanding how their security deposit will be settled. Without a structured checkout request mechanism, customers arrive unannounced, staff are unprepared for itemized unit inspections, and customers lack clarity on deposit refund rules.

**Approach:**
1. **Customer Checkout Request Flow (`RentalDetailPage.tsx` & `CheckoutRequestModal.tsx`):**
   - Active rentals (`CHECKED_IN` or `CHECKOUT_REQUESTED`) provide a clear "Request Checkout" / "Update Checkout" action.
   - The modal displays:
     - Desired checkout date picker (validated >= today).
     - Availability boundary check: ensures the requested checkout date does not collide with or exceed upcoming reservations on the same unit (reusing boundary logic from Story 4.1).
     - Transparent 3-Case Settlement Guide:
       1. **Full Refund:** Returned in good order with keys -> 100% deposit refunded.
       2. **Damage / Late Deductions:** Damage fees or late fees deducted directly from deposit.
       3. **Extra Fee:** If damage/late fees exceed the deposit amount, the difference is payable via QR/Cash at desk before closure.
     - Notes / comments input for staff.
2. **Backend Processing (`CheckoutService.java` & `CheckoutController.java`):**
   - Endpoint `POST /api/v1/reservations/{id}/checkout-request`:
     - Enforces customer ownership and valid state (`CHECKED_IN` or `CHECKOUT_REQUESTED`).
     - Validates date against next reservation boundary.
     - Supersedes any existing `PENDING` checkout request on the reservation (`latest request wins`).
     - Transitions `Reservation.status` to `CHECKOUT_REQUESTED`.
     - Creates `CHECKOUT` task on Kanban Task Board via `TaskService` registry assigned to on-duty staff.
     - Sends notifications to staff and customer.
     - Logs `CHECKOUT_REQUESTED` in Activity Log.
   - Endpoint `GET /api/v1/reservations/{id}/checkout-request`:
     - Returns active checkout request details.
3. **Rental Detail Page Status Banner:**
   - When rental is in `CHECKOUT_REQUESTED` status, displays a prominent banner detailing the requested checkout date, next steps (key & padlock return, desk inspection), and action to modify the date.
4. **Automated Verification:**
   - 100% automated tests in backend (`mvn test`) and frontend (`npx vitest run`), clean build.

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/storagehub/entity/CheckoutRequest.java`: Entity for `checkout_requests` table.
- `backend/src/main/java/com/storagehub/entity/CheckoutRequestStatus.java`: Status enum (`PENDING`, `DONE`, `CANCELLED`).
- `backend/src/main/java/com/storagehub/repository/CheckoutRequestRepository.java`: JPA repository for checkout requests.
- `backend/src/main/java/com/storagehub/dto/CheckoutRequestDto.java` & `CreateCheckoutRequest.java`: DTOs.
- `backend/src/main/java/com/storagehub/service/CheckoutService.java`: Service coordinating boundary check, reservation status transition, task creation, and notifications.
- `backend/src/main/java/com/storagehub/controller/CheckoutController.java`: REST controller for checkout requests.
- `contracts/openapi.yaml`: OpenAPI definitions for checkout requests.
- `frontend/src/types/checkout.ts`: Frontend TypeScript interfaces for checkout requests.
- `frontend/src/api/checkout.ts`: Frontend API client functions (`createCheckoutRequest`, `getCheckoutRequest`).
- `frontend/src/components/checkout/CheckoutRequestModal.tsx`: Modal component with date selector, boundary check, and settlement guide.
- `frontend/src/pages/rentals/RentalDetailPage.tsx`: Integration of checkout request modal and status banner.
- `frontend/src/mocks/handlers.ts`: MSW mock handlers for checkout request endpoints.
- `frontend/src/test/checkout-request.test.tsx`: Comprehensive frontend tests.

## Implementation Plan

1. **Backend Layer:**
   - Create `CheckoutRequest` entity, enum, repository, DTOs.
   - Implement `CheckoutService` with boundary check, task creation, and superseding logic.
   - Implement `CheckoutController` and unit tests (`CheckoutRequestTests`, `CheckoutControllerTests`).
2. **OpenAPI & Contracts:**
   - Add checkout request schemas and endpoints to `contracts/openapi.yaml`.
3. **Frontend Layer:**
   - Implement `CheckoutRequestModal.tsx` with date picker and 3-case settlement explanation.
   - Update `RentalDetailPage.tsx` with checkout trigger and `CHECKOUT_REQUESTED` status banner.
   - Add MSW handlers in `frontend/src/mocks/handlers.ts`.
4. **Testing & Verification:**
   - Write `checkout-request.test.tsx`.
   - Run `mvn test`, `npx vitest run`, and `npm run build`.

## Acceptance Criteria

- **AC-1:** Customer with active rental (`CHECKED_IN`) can open the Checkout Request modal from Rental Detail page.
- **AC-2:** Modal presents date picker, boundary validation, and transparent 3-case settlement financial logic.
- **AC-3:** Submitting checkout request validates boundary against next reservation, updates reservation status to `CHECKOUT_REQUESTED`, supersedes older pending requests, creates `CHECKOUT` task on Kanban board, and sends notifications.
- **AC-4:** `RentalDetailPage` renders a prominent `CHECKOUT_REQUESTED` status banner with scheduled date and modification option.
- **AC-5:** All backend and frontend automated tests pass 100% and production build is clean.
