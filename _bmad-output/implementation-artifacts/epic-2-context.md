# Epic 2 Context: Discovery & Booking with Deposit

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Customer discovers storage units with real-time availability derived on-read from Turnover Buffer policy, reviews transparent price breakdowns calculated solely by a centralized `PricingEngine`, reviews every binding charge in Booking Summary, and pays the 10% refundable deposit via PayOS QR code (or cash at counter for walk-ins). Successful payment automatically records receipts, transitions reservation to RESERVED and unit to Reserved, logs append-only audit activity, dispatches toast and bell notifications, and auto-drafts the initial rental contract. Expired reservations without check-in forfeit deposit lazily and safely via exactly-once idempotent transitions.

## Stories

- Story 2.1: PricingEngine + Unit Detail transparent pricing breakdown
- Story 2.2: Browse Units + availability derived from Turnover Buffer
- Story 2.3: Reserve re-check + Booking Summary
- Story 2.4: Payment backend + PayOsPaymentGateway
- Story 2.5: My Rentals + Check-in Pass + base Rental Detail
- Story 2.6: Payment Modal FE — QR PayOS at Deposit, closing Flow 1
- Story 2.7: No-show EXPIRED — exactly-once lazy

## Requirements & Constraints

- **Pricing Transparency & Centralization (AD-11, AD-7):** Pricing is computed only in backend `PricingEngine` reading active `RentalPolicy` (seeded as v3). Money is strictly `BigDecimal` / `DECIMAL(15,0)` / JSON integer VND. Components must never calculate prices or surcharges. Breakdown calculates rent × duration + active surcharge lines + Deposit (10% refundable). Demo baseline: Unit S-3 at 345.000 ₫/month × 3 months = 1.035.000 ₫ rent, Deposit 10% = 103.500 ₫.
- **Availability on Read (AD-4):** Availability is derived at query time from existing reservations and active turnover buffer policy. Units in Rented, Maintenance, or Retired state are omitted from discovery. Units within turnover cleaning buffer show as "Available {date} · cleaning buffer" in amber and remain bookable for valid future start dates.
- **Race Protection & Snapshot (AD-11, FR-5):** Reserve CTA re-checks availability inside an optimistic/unique transaction guard. Stale selection yields HTTP 409 Conflict envelope and bounces back to grid with an informative toast ("S-3 was just reserved. 5 similar units still available."). Confirming booking creates Reservation `PENDING_PAYMENT` with an immutable price snapshot preventing retroactive policy price shifts.
- **Payment Single Source of Truth (AD-5, FR-8, FR-9):** PayOS SDK (`vn.payos:payos-java` 2.0.1) powers `PayOsPaymentGateway`. `POST /api/v1/payments/webhook` is the 4th public endpoint (no JWT) and must verify PayOS webhook checksums. Duplicate webhooks are strictly idempotent. Staff "Cash received" action requires STAFF permission. Successful payment flips business state within the same transaction: Payment SUCCESS, 1 Receipt generated, Reservation RESERVED, Unit Reserved, ActivityLog recorded, and Toast/Bell notifications dispatched.
- **No-Show Lazy Expiry (FR-36, AD-4, AD-7):** Past check-in date reservations without check-in evaluate lazily on first access (ICT / `Asia/Ho_Chi_Minh` calendar). Transition runs exactly once using idempotency constraint: marks EXPIRED, forfeits deposit receipt, releases Unit to Available, closes contract draft, logs audit, and notifies customer. No customer cancellation permitted in v1.
- **English-only, light mode only, desktop-first:** VND formatted as `1.150.000 ₫` with tabular numerals and dot grouping.

## Technical Decisions

- **Domain Model:**
  - Entities involved: `Unit`, `RentalPolicy`, `PolicyRule`, `Reservation`, `Payment`, `Receipt`, `ActivityLog`, `Notification`.
  - Schema delta: Flyway V1 already established the 22-entity schema with tables `units`, `rental_policies`, `policy_rules`, `reservations`, `payments`, `receipts`, etc.
  - `Payment.orderCode`: strictly derived from payment ID or unique numeric sequence as required by PayOS.
  - Price snapshot: stored on `reservations` (`base_rent`, `total_amount`, `deposit_amount`, `surcharges_json` / snapshot columns) to lock terms.
- **API & Contracts:**
  - Follow contract-first workflow: update `contracts/openapi.yaml` before backend implementation.
  - Unit Detail: `GET /api/v1/units/{id}` and `GET /api/v1/pricing/calculate`.
  - Discovery: `GET /api/v1/units/browse` with query parameters `type`, `size`, `startDate`, `durationMonths`.
  - Booking & Payment: `POST /api/v1/reservations`, `POST /api/v1/payments/create`, `POST /api/v1/payments/webhook`, `POST /api/v1/payments/{id}/confirm-cash`.
- **Backend Architecture:**
  - Controllers only serialize/deserialize and delegate to service layer.
  - Transactions encapsulate state changes + `LogService` audit + `NotificationService.send()` inside single atomic commits.
  - `PayOsPaymentGateway` handles API integration; mock SDK client in test harness.
- **Frontend Architecture:**
  - `PaymentModal` is a reusable overlay across 4 touchpoints (deposit QR, check-in 100%, extension, extra fees). For deposit touchpoint, only PayOS QR is rendered (no Cash option for online self-service deposit).
  - TanStack Query polling handles payment status updates while modal is awaiting confirmation.
  - Flow 1 termination: payment success modal deep-links to `/rentals/{id}` (Rental Detail); no standalone booking confirmation page.

## UX & Interaction Patterns

- **Unit Detail (F1-04):** Full-page screen showing photo gallery (`public/units/{code}.jpg`), specifications (dimensions, floor, access, security chips), clear pricing breakdown card, and Reserve CTA.
- **Booking Summary (F1-05):** Transparent line-item review card (Rent × Duration, surcharge lines, Total, Refundable Deposit due now), legal notice ("Contract auto-drafted from these exact terms, signed at check-in"), CTA triggering `PaymentModal`.
- **Browse Units Grid (F1-03):** 3px left status indicator bar (green for available, amber for buffer), tabular pricing, mono unit code chip, live count badge, search filter with explicit Search button (no keystroke refetch).
- **Payment Modal (F1-06):** Max 480px width, focus trap, right-aligned action buttons, QR countdown timer, explicit Cancel button calling cancel endpoint, check tile on success, retry / method switch on failure.
- **My Rentals & Check-in Pass (F2-03, F2-04):** Tabbed listing of reservations and active rentals, prominent mono `BK-` reservation pass code, base Rental Detail displaying unit metadata, dates, deposit status, and receipt history.

## Cross-Story Dependencies

- **Pre-requisites from Epic 1:**
  - `LogService` for audit logging (Story 1.2).
  - JWT Auth and Security filter (Story 1.3).
  - Design tokens, base primitives (Story 1.4).
  - `AdaptiveShell` role routing (Story 1.5).
  - `NotificationService` and `ToastProvider` (Story 1.6).
- **Intra-Epic Flow:**
  - Story 2.1 provides the `PricingEngine` foundation and Unit Detail screen consumed by 2.2 and 2.3.
  - Story 2.2 provides Browse Units grid routing to 2.1 Unit Detail.
  - Story 2.3 establishes Reservation drafting and 409 re-check leading into Story 2.4/2.6 payment.
  - Story 2.4 backend payment gateway feeds Story 2.6 FE modal.
  - Story 2.5 surfaces the created reservations and rentals.
  - Story 2.7 ensures cleanup and lifecycle completion for abandoned reservations.
