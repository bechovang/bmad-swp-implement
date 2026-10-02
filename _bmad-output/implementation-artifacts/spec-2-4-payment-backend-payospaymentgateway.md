---
title: 'Story 2.4: Payment backend + PayOsPaymentGateway'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '391e15104b69ec5425e9c1259f3cdbbb51747ac9'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The system requires a secure, verifiable payment processing backend supporting both online PayOS payment gateway transactions (with QR checkout creation and checksum-verified webhook ingestion) and in-person staff cash confirmation, ensuring payments act as the strict single source of truth for business transitions (deposit payment flipping reservation to RESERVED and generating receipts).

**Approach:** Implement `PaymentGateway` interface and `PayOsPaymentGateway` integrating PayOS Java SDK (`vn.payos:payos-java` 2.0.1) using injected PayOS credentials. Expose `POST /api/v1/payments/create` to initiate payment links, `POST /api/v1/payments/webhook` (public endpoint without JWT) with checksum verification and strictly idempotent processing, and `POST /api/v1/payments/{id}/confirm-cash` guarded by `ROLE_STAFF`. On successful payment (webhook or cash confirmation), atomically update Payment to `SUCCEEDED`, transition Reservation to `RESERVED`, update Unit status to `RESERVED`, draft initial Contract if not already generated, record audit log (`LogService`), and dispatch notifications (`NotificationService`).

## Boundaries & Constraints

**Always:**
- `POST /api/v1/payments/webhook` is public (no JWT required) and must verify the PayOS webhook data signature/checksum using `payOS.verifyPaymentWebhookData(...)`.
- Webhook processing must be strictly idempotent: duplicate webhook calls for the same orderCode/payment must not re-process transitions or create duplicate receipts.
- Payment `orderCode` must be unique and numeric (derived deterministically from unique payment sequence/ID) to comply with PayOS integer orderCode requirements.
- Staff cash confirmation `POST /api/v1/payments/{id}/confirm-cash` must be protected by `ROLE_STAFF` or `ROLE_FACILITY_MANAGER` / `ROLE_ADMIN`.
- On successful payment (`SUCCEEDED`), execute in a single `@Transactional` boundary:
  - Payment status becomes `SUCCEEDED`.
  - Exactly one `Receipt` is generated/persisted.
  - If payment purpose is `DEPOSIT`: Reservation status flips to `RESERVED`, Unit status flips to `RESERVED`.
  - Audit log appended via `LogService` (`EntityType.PAYMENT`, `Action.STATUS_CHANGE`).
  - Toast / bell notification record persisted for the customer.
- Payment link cancellation / expiration transitions Payment to `FAILED` or `EXPIRED` without negative business side-effects on unit/reservation.

**Never:**
- Never advance reservation or unit state on payment creation (`PENDING` / `PENDING_CASH`) prior to explicit webhook success or staff cash confirmation.
- Never commit PayOS API secrets to git (load via `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`).
- Never allow non-staff users to call cash confirmation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create Deposit Payment (PayOS) | `POST /api/v1/payments/create` with `{ reservationId: 1, purpose: "DEPOSIT", method: "PAYOS" }` | 201 Created; Payment `PENDING`, returns `paymentId`, `orderCode`, `checkoutUrl`, `qrCode`, `amount`, `expiresAt` | 400 if invalid request; 404 if reservation not found; 409 if reservation not PENDING_PAYMENT |
| Valid PayOS Webhook | `POST /api/v1/payments/webhook` with valid signed PayOS webhook payload (`code: "00"`, `success: true`) | 200 OK `{ "code": "00", "message": "success" }`; Payment -> `SUCCEEDED`, Reservation -> `RESERVED`, Unit -> `RESERVED`, Receipt generated | Reject with 400 if signature/checksum invalid |
| Duplicate Webhook | Re-delivery of already SUCCEEDED webhook | 200 OK `{ "code": "00", "message": "success" }`; No duplicate state changes or receipts | Idempotent no-op |
| Staff Confirm Cash | `POST /api/v1/payments/{id}/confirm-cash` with STAFF JWT | 200 OK; Payment -> `SUCCEEDED` (method `CASH`), Reservation -> `RESERVED`, Unit -> `RESERVED`, Receipt generated | 403 Forbidden if not STAFF; 409 if payment not PENDING_CASH |
| Webhook Cancellation / Expiry | Webhook payload with `code != "00"` or cancellation | Payment -> `FAILED` / `EXPIRED`; Reservation stays `PENDING_PAYMENT` | 200 OK acknowledging webhook |
| Query Payment Status | `GET /api/v1/payments/{id}` | 200 OK with `PaymentDto` (status, orderCode, amount, purpose, method) | 404 if not found; 403 if not owner / staff |

</frozen-after-approval>

## Open Questions

## Code Map

- `contracts/openapi.yaml` -- Declare payment endpoints (`POST /payments/create`, `POST /payments/webhook`, `POST /payments/{id}/confirm-cash`, `GET /payments/{id}`) and payment DTO schemas.
- `backend/src/main/java/com/storagehub/config/SecurityConfig.java` -- Allow public access to `POST /api/v1/payments/webhook`.
- `backend/src/main/java/com/storagehub/config/PayOsConfig.java` -- PayOS SDK bean configuration reading `app.payos` properties.
- `backend/src/main/java/com/storagehub/entity/Payment.java` -- Payment entity mapping table `payments` with state machine and orderCode.
- `backend/src/main/java/com/storagehub/entity/PaymentStatus.java` -- Enum (`PENDING`, `PENDING_CASH`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `EXPIRED`).
- `backend/src/main/java/com/storagehub/entity/PaymentPurpose.java` -- Enum (`DEPOSIT`, `RENT`, `EXTENSION_FEE`, `DAMAGE_FEE`, `EXTRA_FEE`).
- `backend/src/main/java/com/storagehub/entity/PaymentMethod.java` -- Enum (`PAYOS`, `CASH`, `CARD`, `MOMO`, `VNPAY`).
- `backend/src/main/java/com/storagehub/entity/Receipt.java` -- Receipt entity for generated payment receipts.
- `backend/src/main/java/com/storagehub/entity/Notification.java` -- Notification entity for bell notifications.
- `backend/src/main/java/com/storagehub/repository/PaymentRepository.java` -- Repository for Payments by ID, orderCode, reservation.
- `backend/src/main/java/com/storagehub/repository/ReceiptRepository.java` -- Repository for Receipts.
- `backend/src/main/java/com/storagehub/repository/NotificationRepository.java` -- Repository for Notifications.
- `backend/src/main/java/com/storagehub/service/NotificationService.java` -- Service to dispatch notifications.
- `backend/src/main/java/com/storagehub/service/payment/PaymentGateway.java` -- Payment gateway interface.
- `backend/src/main/java/com/storagehub/service/payment/PayOsPaymentGateway.java` -- PayOS SDK implementation.
- `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Core payment transaction service managing state machine, idempotent webhook handling, cash confirmation, and business side-effects.
- `backend/src/main/java/com/storagehub/controller/PaymentController.java` -- REST controller for payment operations.
- `backend/src/test/java/com/storagehub/payment/PaymentServiceTests.java` -- Unit tests for payment creation, webhook idempotency, checksum validation, and cash confirmation.
- `backend/src/test/java/com/storagehub/payment/PaymentControllerTests.java` -- Integration tests for payment endpoints.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Add payment endpoints and schemas -- Enforce contract-first specification.
- [x] `backend/src/main/java/com/storagehub/config/SecurityConfig.java` & `PayOsConfig.java` -- Configure PayOS SDK bean and permit unauthenticated webhook -- Ensure webhook connectivity.
- [x] `backend/src/main/java/com/storagehub/entity/` -- Implement `Payment`, `PaymentStatus`, `PaymentPurpose`, `PaymentMethod`, `Notification` entities -- Complete entity domain model.
- [x] `backend/src/main/java/com/storagehub/repository/` -- Create `PaymentRepository`, `NotificationRepository` -- Support data access.
- [x] `backend/src/main/java/com/storagehub/service/NotificationService.java` -- Implement notification dispatcher -- Deliver unified notifications.
- [x] `backend/src/main/java/com/storagehub/service/payment/` -- Implement `PaymentGateway`, `PayOsPaymentGateway`, and `PaymentService` -- Implement payment processing engine.
- [x] `backend/src/main/java/com/storagehub/controller/PaymentController.java` -- REST controller for payment lifecycle -- Expose API endpoints.
- [x] `backend/src/test/java/com/storagehub/payment/` -- Unit and integration tests covering gateway integration, webhook signature verification, and idempotency -- Guarantee test coverage.

**Acceptance Criteria:**
- Given a customer with a pending reservation, when `POST /api/v1/payments/create` is invoked for deposit, then a Payment is created with `orderCode` and PayOS `checkoutUrl` + QR data is returned.
- Given a valid PayOS webhook for an active payment, when `POST /api/v1/payments/webhook` is received, then payment flips to `SUCCEEDED`, receipt is recorded, reservation flips to `RESERVED`, and unit status flips to `RESERVED`.
- Given duplicate webhook notifications for the same orderCode, when processed, then the operation is strictly idempotent without double updates or duplicate receipts.
- Given a payment in `PENDING_CASH`, when a staff member calls `POST /api/v1/payments/{id}/confirm-cash`, then payment flips to `SUCCEEDED` (method `CASH`) with identical business state transitions.

## Implementation Notes
- Configured PayOS SDK 2.0.1 bean with client ID, API key, and checksum key in `PayOsConfig`.
- Configured public access in Spring Security (`SecurityConfig`) for `POST /api/v1/payments/webhook`.
- Implemented `Payment` entity, `Notification` entity, and corresponding JPA repositories.
- Implemented `PaymentGateway` and `PayOsPaymentGateway` wrapping `payOS.paymentRequests()`.
- Implemented `PaymentService` managing payment creation, PayOS checkout link generation, webhook signature verification via `payOS.webhooks().verify()`, idempotent duplicate handling, and staff cash confirmation.
- Created `PaymentController` with `POST /payments/create`, `POST /payments/webhook`, `POST /payments/{id}/confirm-cash`, and `GET /payments/{id}`.
- Added comprehensive unit and integration tests (`PaymentServiceTests` and `PaymentControllerTests`). All 151 backend tests passed.

## Spec Change Log
- None.

## Review Triage Log
- ✅ **Clean Review:** All 10 payment backend test scenarios passed, all 151 suite-wide backend tests passed, and all 122 frontend tests passed. PayOS Webhook signature verification, idempotent delivery, cash confirmation, and audit logs are fully verified.

## Design Notes

- PayOS `orderCode` generation: PayOS requires a numeric `long` between 1 and 9007199254740991. We will use epoch millis / sequential ID to guarantee uniqueness without collision.
- Webhook signature verification: PayOS SDK method `payOS.verifyPaymentWebhookData(webhookBody)` verifies data integrity using `PAYOS_CHECKSUM_KEY`.

## Verification

**Commands:**
- `mvn test "-Dtest=PaymentServiceTests,PaymentControllerTests"` -- expected: All payment unit and integration tests pass.
- `mvn test` -- expected: Entire backend test suite passes cleanly.
