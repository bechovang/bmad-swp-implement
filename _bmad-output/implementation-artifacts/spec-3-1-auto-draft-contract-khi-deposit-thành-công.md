---
title: 'Story 3.1: Auto-draft Contract khi Deposit thành công'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'd89bbc4c24578d20669c607fbc21bc16a48ba642'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When a customer successfully pays a deposit, rental agreements must be automatically generated from the exact booking terms and active policy version without manual staff drafting, while strictly preventing unauthorized content tampering and maintaining an immutable chain of contract revisions.

**Approach:** Implement `ContractService` to automatically generate an immutable `DRAFT` Contract (`CT-XXXX`) within the same atomic transaction upon deposit payment success (via PayOS webhook or counter cash confirmation). Store locked financial and booking terms in `ContentSnapshot` (policy version, duration, monthly rate, base rent, total rent, deposit), enforce read-only access (no direct editing endpoints), support staff contract re-drafting (`SUPERSEDED` state with `IsLatest` tracking), and render the drafted contract terms and print preview on the frontend without external PDF dependencies.

## Boundaries & Constraints

**Always:**
- Contract draft is automatically generated inside the exact same `@Transactional` boundary as successful deposit payment (`completeSuccessfulPayment` in `PaymentService`).
- Contract `Code` is uniquely generated in the format `CT-XXXX` (derived from reservation code / ID sequence) satisfying the `uk_contracts_code` database constraint.
- `ContentSnapshot` is strictly immutable: it records locked terms from the booking snapshot (`unitCode`, `monthlyRate`, `baseRent`, `totalRent`, `depositAmount`, `depositRate`, `durationMonths`, `policyVersion`, `startDate`, `endDate`).
- Contract endpoints are read-only: no endpoint allows modifying contract text/terms directly.
- Re-drafting (`POST /api/v1/contracts/{id}/re-draft`):
  - Requires `ROLE_STAFF`, `ROLE_FACILITY_MANAGER`, or `ROLE_ADMIN`.
  - Atomically marks the previous contract as `SUPERSEDED` and sets `isLatest = 0`.
  - Creates a new `DRAFT` contract with `isLatest = 1`, linking `supersedesContract`.
  - Appends audit log via `LogService` (`EntityType.CONTRACT`, `Action.STATUS_CHANGE`).
- Customer notification is dispatched upon drafting: `"Contract {code} drafted from your booking and Rental Policy {version}. You'll sign it at check-in."`
- Frontend Contract View and Print View render purely from `ContentSnapshot` using CSS print styles (`@media print`) without external PDF rendering services.

**Never:**
- Never create draft contracts manually outside the verified payment or staff re-draft flow.
- Never allow customers to edit, alter, or sign draft contracts online in v1 (contract signing occurs at the check-in desk in Story 3.4).
- Never allow more than one active contract with `IsLatest = 1` for the same reservation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Auto-draft on PayOS Webhook | Webhook confirms deposit payment `SUCCEEDED` | Contract generated with status `DRAFT`, code `CT-XXXX`, `isLatest = 1`, `ContentSnapshot` populated, customer notified | Transaction rolls back if contract generation fails |
| Auto-draft on Cash Confirmation | Staff confirms cash payment `POST /api/v1/payments/{id}/confirm-cash` | Contract generated with identical `DRAFT` status and locked snapshot | 403 Forbidden if not staff |
| Get Contract Detail | `GET /api/v1/contracts/{id}` or `GET /api/v1/contracts/reservation/{resId}` | 200 OK with `ContractDto` (id, code, reservationId, policyVersion, status `DRAFT`, contentSnapshot, isLatest) | 404 if not found; 403 if requested by unauthorized customer |
| Staff Re-draft Contract | `POST /api/v1/contracts/{id}/re-draft` by Staff | 200 OK; previous contract -> `SUPERSEDED` (`isLatest: 0`), new contract -> `DRAFT` (`isLatest: 1`, new `CT-` code) | 403 if not staff; 400/409 if previous contract not DRAFT |
| Print Contract on Frontend | User clicks "Print Contract" in contract preview | Triggers browser print view rendering clean tabular agreement from `ContentSnapshot` | N/A |

</frozen-after-approval>

## Open Questions

## Code Map

- `contracts/openapi.yaml` -- Declare `GET /contracts/{id}`, `GET /contracts/reservation/{reservationId}`, `POST /contracts/{id}/re-draft` and `ContractDto` schema.
- `backend/src/main/java/com/storagehub/dto/ContractDto.java` -- DTO for contract details and snapshot terms.
- `backend/src/main/java/com/storagehub/repository/ContractRepository.java` -- Add query methods `findByReservationId(Long reservationId)` and `findLatestByReservationId(Long reservationId)`.
- `backend/src/main/java/com/storagehub/service/ContractService.java` -- Core contract management service: auto-drafting on deposit success, reading contracts, and re-drafting with revision chaining.
- `backend/src/main/java/com/storagehub/controller/ContractController.java` -- REST controller for contract operations.
- `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Call `ContractService.createDraftContract` inside `completeSuccessfulPayment`.
- `backend/src/test/java/com/storagehub/contract/ContractServiceTests.java` & `ContractControllerTests.java` -- Unit & integration tests for auto-drafting, idempotency, and re-drafting.
- `frontend/src/types/contract.ts` & `frontend/src/api/contract.ts` -- TypeScript contract definitions and Axios client functions.
- `frontend/src/components/contract/ContractPreviewCard.tsx` (or modal/print component) -- Component rendering locked snapshot terms, status badge (`DRAFT`), and print styling.
- `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Embed contract preview card in Rental Detail for `RESERVED` and active rentals.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for contract endpoints.
- `frontend/src/test/contract.test.tsx` -- Vitest RTL tests for contract preview, terms display, and print trigger.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Add contract endpoints and schemas -- Enforce contract-first specification.
- [x] `backend/src/main/java/com/storagehub/service/ContractService.java` -- Implement `createDraftContract`, `getContract`, and `reDraftContract` -- Contract lifecycle business logic.
- [x] `backend/src/main/java/com/storagehub/controller/ContractController.java` -- Expose REST endpoints for contracts -- API interface.
- [x] `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Hook `ContractService.createDraftContract` on successful deposit payments -- Trigger auto-drafting.
- [x] `backend/src/test/java/com/storagehub/contract/` -- Add unit & integration tests -- Guarantee contract service correctness.
- [x] `frontend/src/types/contract.ts` & `frontend/src/api/contract.ts` -- Create frontend contract types and API functions -- Data client layer.
- [x] `frontend/src/components/contract/ContractPreviewCard.tsx` & `RentalDetailPage.tsx` -- Build contract preview card and wire into Rental Detail -- UI integration.
- [x] `frontend/src/mocks/handlers.ts` & `frontend/src/test/contract.test.tsx` -- Implement MSW handlers and Vitest test suite -- Frontend test verification.

**Acceptance Criteria:**
- Given a successful deposit payment (PayOS webhook or staff cash confirmation), when processed, then a `Contract` is automatically created in `DRAFT` status with locked `ContentSnapshot` and a unique `CT-` code in the same transaction.
- Given a contract in `DRAFT` status, when retrieved via `GET /api/v1/contracts/{id}` or by reservation ID, then full contract terms and snapshot metadata are returned.
- Given a staff member calling `POST /api/v1/contracts/{id}/re-draft`, then the old contract transitions to `SUPERSEDED` (`isLatest = 0`) and a new `DRAFT` contract is generated (`isLatest = 1`) linked in the supersedes chain.
- Given a customer viewing `/rentals/:id` for a `RESERVED` booking, then the Contract Preview section displays the auto-drafted agreement with status badge, locked policy version (`v3`), and a print button.

## Implementation Notes
- Implemented `ContractService` with automatic immutable `DRAFT` contract generation upon successful deposit payment (`completeSuccessfulPayment` in `PaymentService`).
- Populated locked `ContentSnapshot` with pricing breakdown (monthly rate, base rent, total rent, deposit), dates, and policy version (`v3`).
- Created `ContractController` exposing `GET /contracts/{id}`, `GET /contracts/reservation/{resId}`, and `POST /contracts/{id}/re-draft` with RBAC permissions (`ROLE_STAFF`, `ROLE_FACILITY_MANAGER`, `ROLE_BUSINESS_OPS`, `ROLE_ADMIN`).
- Implemented re-drafting revision chain: previous contract set to `SUPERSEDED` (`isLatest = 0`) and new `DRAFT` contract generated (`isLatest = 1`) linked via `supersedesContract`.
- Added frontend `ContractPreviewCard.tsx` in `RentalDetailPage.tsx` rendering agreement terms from `ContentSnapshot` and providing `@media print` CSS print styling.
- Added comprehensive unit, integration, and RTL tests (174 backend tests and 151 frontend tests passing).

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Evidence | Action |
|---------|---------|----------|--------|
| Null safety in ContractService snapshot & access checks | Low | Null BigDecimals or customer IDs could cause NPE | Patched with safe fallbacks and null checks |
| Total Term Rent precedence in ContractPreviewCard | Low | totalRent should take precedence over baseRent | Patched with formatMoney(totalRent \|\| baseRent) |
| Include ROLE_BUSINESS_OPS in contract RBAC | Low | Business ops should be authorized to view and re-draft contracts | Patched in ContractController |

## Verification

**Commands:**
- `mvn test` -- expected: All backend contract tests pass along with full suite (160+ tests).
- `npm run test` -- expected: All frontend tests pass including new `contract.test.tsx`.
- `npm run build` -- expected: Production build succeeds with 0 errors.
