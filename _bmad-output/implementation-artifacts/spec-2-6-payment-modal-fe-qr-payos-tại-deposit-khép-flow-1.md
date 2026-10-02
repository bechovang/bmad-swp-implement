---
title: 'Story 2.6: Payment Modal FE — QR PayOS tại Deposit, khép Flow 1'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '6abc07d619cc252dd30b8207273c52e227015766'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers reserving a storage unit currently have no frontend interface to complete online PayOS QR code payments for their required 10% refundable deposit, poll real-time transaction confirmation, or cleanly navigate directly to their active rental detail upon success to close Booking Flow 1.

**Approach:** Implement a reusable, multi-touchpoint `PaymentModal` component (`max-w-[480px]`, focus trapped) supporting PayOS QR generation (`POST /api/v1/payments/create`), real-time status polling (`GET /api/v1/payments/{id}`) with countdown derived strictly from backend `expiresAt`, explicit cancel action, and failure recovery. Integrate `PaymentModal` at the Deposit touchpoint in `BookingSummaryPage` and `MyRentalsPage` (`PENDING_PAYMENT` state), seamlessly deep-linking to `/rentals/{id}` upon payment confirmation to close Flow 1.

## Boundaries & Constraints

**Always:**
- Payment Modal is a reusable component shared across 4 business touchpoints: Deposit (Story 2.6), 100% check-in rent (Epic 3), Extension top-up (Epic 4), and Extra fee settlement (Epic 6).
- For the Deposit touchpoint, method selection strictly renders only **PayOS QR** (no Cash option allowed for self-service online deposits).
- QR countdown timer is derived strictly from `expiresAt` returned by backend `POST /api/v1/payments/create` (FE never hardcodes timeout).
- While awaiting payment confirmation (`AWAITING_PAYMENT`), modal backdrop click/escape is locked and polling runs every 2 seconds via TanStack Query without blocking the UI.
- Explicit "Cancel" button calls backend cancel endpoint / marks payment cancelled and reverts to method selection without charging or mutating reservation state.
- On successful payment (`SUCCEEDED`):
  - Displays check tile with paid amount and consequence notice: `"Unit {unitCode} is reserved for you until check-in."`
  - Dispatches toast notification with payment amount and triggers bell notification update.
  - Primary CTA `"View Rental"` deep-links directly to `/rentals/{id}` (Rental Detail), closing Flow 1 without a separate confirmation screen.
- On failed payment or expired countdown:
  - Displays error tile: `"No money was taken."`
  - Offers "Retry" (re-creates payment link) and hints at counter assistance if failed twice.
  - Does NOT mutate unit or reservation status.
- UI complies with UX-DR5 guidelines: max width 480px, right-aligned action buttons, light mode, tabular numerals.

**Never:**
- Never render a Cash payment option during self-service deposit checkout.
- Never advance reservation status to `RESERVED` on the frontend before polling receives confirmed `SUCCEEDED` status from backend.
- Never calculate price amounts or timeouts client-side.
- Never show a standalone intermediate booking confirmation page.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Open Modal at Deposit | `PaymentModal` triggered with `purpose: "DEPOSIT"`, `amount: 103500`, `unitCode: "S-3"` | Modal opens (`max-w-[480px]`), shows "Deposit Payment", tabular price `103.500 ₫`, PayOS QR pre-selected | N/A |
| Create PayOS Link | User clicks "Pay with QR" | Calls `POST /api/v1/payments/create`; renders PayOS QR code, orderCode, and countdown timer derived from `expiresAt` | 400/500 shows error banner with retry |
| Await Webhook Success | Active payment link, polling `GET /api/v1/payments/{id}` returns `SUCCEEDED` | Transitions to Success state: green check tile, paid amount, consequence line, dispatches toast, primary CTA `"View Rental"` -> `/rentals/{id}` | N/A |
| User Cancels Payment | User clicks "Cancel Payment" while in Awaiting state | Reverts to Method Select state; payment link marked cancelled, no reservation side-effects | N/A |
| Countdown Expiry | Polling or timer reaches `expiresAt` without payment | Transitions to Expired state: `"No money was taken"`, shows `"Payment link expired"` with Retry button | N/A |
| Repeated Payment Failures | Payment fails 2 times | Renders hint: `"Having trouble? You may also complete your booking with staff at our reception counter."` | N/A |

</frozen-after-approval>

## Open Questions

## Code Map

- `frontend/src/types/payment.ts` -- Define payment request, response, method, purpose, and modal state models.
- `frontend/src/api/payment.ts` -- API client functions: `createPaymentLink`, `getPaymentStatus`, and `cancelPayment`.
- `frontend/src/components/payment/PaymentModal.tsx` -- Main multi-touchpoint PaymentModal component with state machine (`METHOD_SELECT`, `AWAITING`, `SUCCESS`, `FAILED`, `EXPIRED`), QR rendering, polling, and countdown timer.
- `frontend/src/pages/booking/BookingSummaryPage.tsx` -- Integrate `PaymentModal` upon booking confirmation to immediately launch deposit QR payment.
- `frontend/src/pages/rentals/MyRentalsPage.tsx` -- Connect `PaymentModal` to `PENDING_PAYMENT` card actions.
- `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Connect `PaymentModal` to pending deposit banner.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for `POST /api/v1/payments/create` and `GET /api/v1/payments/:id`.
- `frontend/src/test/payment-modal.test.tsx` -- Vitest RTL test suite for payment modal state machine (select -> awaiting -> success/fail/expired), polling, cancel, and deep-link routing.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/src/types/payment.ts` & `frontend/src/api/payment.ts` -- Implement payment TypeScript types and Axios client functions -- Data layer contracts.
- [x] `frontend/src/components/payment/PaymentModal.tsx` -- Build reusable Payment Modal with PayOS QR, timer, polling, success tile, and error recovery -- Core UI component.
- [x] `frontend/src/pages/booking/BookingSummaryPage.tsx` -- Connect `PaymentModal` to confirm action, closing Flow 1 into `/rentals/{id}` -- Booking flow completion.
- [x] `frontend/src/pages/rentals/MyRentalsPage.tsx` & `RentalDetailPage.tsx` -- Wire `PaymentModal` for pending deposit reservations -- Portal payment triggers.
- [x] `frontend/src/mocks/handlers.ts` -- Add/update MSW mock handlers for payment creation and polling simulation -- Integration mock harness.
- [x] `frontend/src/test/payment-modal.test.tsx` -- Add comprehensive Vitest RTL tests for PaymentModal and Flow 1 closing -- Test verification.

**Acceptance Criteria:**
- Given a customer confirming a booking on `BookingSummaryPage`, when clicking confirm, then `PaymentModal` opens displaying deposit amount in `price-lg` format and PayOS QR method.
- Given the modal in `AWAITING_PAYMENT` state, when PayOS QR is displayed, then countdown derives from `expiresAt` and polling checks status every 2 seconds.
- Given a payment confirmed as `SUCCEEDED` during polling, then the modal transitions to Success state with check tile, amount, consequence text, toast notification, and "View Rental" button navigating to `/rentals/{id}`.
- Given a user clicking "Cancel Payment", then polling stops, payment is cancelled, and modal returns to method selection without changing reservation state.
- Given a payment that fails or expires, then the modal displays `"No money was taken"`, shows Retry CTA, and after 2 failures suggests reception counter help.

## Implementation Notes
- Created `frontend/src/types/payment.ts` and `frontend/src/api/payment.ts` for payment types and API operations.
- Implemented `PaymentModal.tsx` (`max-w-[480px]`) supporting state machine (`METHOD_SELECT`, `AWAITING`, `SUCCESS`, `FAILED`, `EXPIRED`), PayOS QR rendering, derived `expiresAt` countdown timer, TanStack Query polling every 2s, backdrop/escape lock while awaiting, cancel action, success tile with consequence notice, failure recovery, reception counter hint on 2 failures, and primary CTA deep-linking directly to `/rentals/{id}`.
- Integrated `PaymentModal` in `BookingSummaryPage.tsx` upon booking confirmation to immediately launch deposit QR payment and close Flow 1.
- Connected `PaymentModal` to `MyRentalsPage.tsx` and `RentalDetailPage.tsx` for pending deposit reservations.
- Added MSW mock handlers and comprehensive Vitest RTL test suite (`payment-modal.test.tsx`, 8 test scenarios).
- Verified full frontend test suite (142 tests passing), Vite production build, and backend test suite (153 tests passing).

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Evidence | Action |
|---------|---------|----------|--------|
| Missing EXPIRED state test coverage in payment-modal.test.tsx | Medium | Automated verification needed for countdown expiration and retry CTA | Patched with new test case simulating EXPIRED status and Generate New QR |
| Double increment of failureCount on concurrent expiration events | Low | Racing timer and polling update could increment failureCount twice | Patched with guard checking previous modal state |
| Stale currentPayment retained on cancellation | Low | Clearing currentPayment prevents leftover data leakage | Patched with setCurrentPayment(null) |
| Safe fallback for expiresAt calculation | Low | Null-safe timestamp calculation avoids NaN countdown | Patched with default 15-minute fallback |
| Reset failureCount on modal open | Low | Fresh modal instances start with zero failure count | Patched with setFailureCount(0) on open |

## Verification

**Commands:**
- `npm run test` -- expected: All frontend tests pass including new `payment-modal.test.tsx`.
- `npm run build` -- expected: Production TypeScript and Vite build succeeds with zero errors.
- `mvn test` -- expected: All backend integration and unit tests pass.
