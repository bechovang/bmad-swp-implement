---
title: 'Story 3.4: Contract ritual + Access Code + kích hoạt Rental + contract chain'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'a9b1c2d3e4f5012370cb315dca5839db7b7aaf78'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Front desk staff checking in customers must follow an audited contract ritual (print contract draft -> physical customer signature -> take photo & upload signed contract image) before unlocking the unit's Access Code PIN/QR. Activating the rental must be strictly gated so no customer receives an access code without full payment and an attached signed contract on file. Additionally, customers and staff need a persistent contract chain viewer to inspect signed contracts and historical revisions.

**Approach:** 
1. Build contract ritual workflow in `TaskDetailPage` at `/tasks/:id` with 4 sequential steps:
   - Read-only preview rendered from contract `contentSnapshot` (parties, unit code, duration, locked pricing terms, policy version).
   - "Print" action rendering the print-optimized contract template (`POST /api/v1/contracts/{id}/print` -> transitions contract status to `PRINTED`).
   - "Capture / Upload" photo attachment via `POST /api/v1/attachments` (multipart file storage in `backend/storage/`, secured streaming via `AttachmentController`), calling `POST /api/v1/contracts/{id}/sign` with `signedPhotoUrl` -> transitions contract to `SIGNED`.
   - "Handover Access Code & Activate Rental" via `POST /api/v1/tasks/{id}/activate-checkin`: atomically transitions Reservation to `CHECKED_IN`, Unit to `RENTED`, Contract to `ACTIVE`, and Task to `DONE`, revealing the secret Access Code PIN.
2. In `RentalDetailPage` (`/rentals/:id`), render the contract preview card and contract revision tabs for viewing historical superseded and signed contracts.

## Boundaries & Constraints

**Always:**
- Access Code generation/handover is strictly disabled until rent payment is `SUCCEEDED` and Contract is `SIGNED` (FR-11).
- Contract signing must save relative API path `signedPhotoUrl` pointing to secure streaming endpoint `/api/v1/attachments/{filename}` (AD-10).
- Attachment files are served exclusively through controller streaming with JWT authentication and permission checks (customer of rental, staff, facility manager, admin). No public static folder mappings.
- Access Code is treated as `SensitiveValue` (AD-5) and returned only through dedicated reveal endpoints (`/api/v1/tasks/{id}/activate-checkin` and `/api/v1/reservations/{id}/access-code`), never embedded in bulk list queries.
- Contract revision chain retains all previous versions (`isLatest = 0`, status `SUPERSEDED`) alongside the latest active signed contract (FR-13).

**Never:**
- Never reveal access code or activate rental if the contract status is not `SIGNED`.
- Never allow unauthenticated or unauthorized users to stream signed contract images.
- Never edit contract text manually; contracts are strictly generated from policy and reservation snapshot.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Print Contract Draft | `POST /api/v1/contracts/{id}/print` | 200 OK with contract status `PRINTED` | 404 if not found; 409 if status invalid |
| Attach Signed Photo | `POST /api/v1/attachments` (file multipart) | 201 Created `{ fileUrl, fileName, size, contentType }` | 400 on empty file |
| Sign Contract | `POST /api/v1/contracts/{id}/sign` with `{ signedPhotoUrl }` | 200 OK with contract status `SIGNED`, audit logged `CONTRACT_SIGNED` | 400 if photo URL missing |
| Activate Check-in & Handover PIN | `POST /api/v1/tasks/{id}/activate-checkin` | 200 OK `{ accessCode, reservationStatus: "CHECKED_IN", unitStatus: "RENTED", taskStatus: "DONE" }` | 409 if rent unpaid or contract unsigned |
| Get Access Code | `GET /api/v1/reservations/{id}/access-code` | 200 OK `{ accessCode, accessType, unitCode }` | 403 if user does not own reservation |
| View Contract Chain | `GET /api/v1/contracts/reservation/{id}/chain` | 200 OK list of `ContractDto` sorted by revision | N/A |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Declare `/contracts/{id}/print`, `/contracts/{id}/sign`, `/attachments`, `/tasks/{id}/activate-checkin`, `/reservations/{id}/access-code`, and `/contracts/reservation/{id}/chain`.
- `backend/src/main/java/com/storagehub/controller/ContractController.java` & `TaskController.java` -- Expose contract workflow and check-in activation endpoints.
- `backend/src/main/java/com/storagehub/controller/AttachmentController.java` -- Implement secured multipart file upload and authenticated image stream.
- `backend/src/main/java/com/storagehub/service/ContractService.java` & `TaskService.java` -- Business logic for contract printing, signing, access code generation, and atomic activation.
- `backend/src/test/java/com/storagehub/contract/ContractRitualTests.java` -- Integration tests for contract ritual, access code gating, and attachment authentication.
- `frontend/src/types/contract.ts` & `frontend/src/api/contract.ts` -- TypeScript definitions and API client functions for contract ritual.
- `frontend/src/components/contract/ContractRitualStep.tsx` & `ContractPreviewCard.tsx` -- UI components for preview, print, file capture, attach, and access code reveal.
- `frontend/src/pages/staff/TaskDetailPage.tsx` & `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Integration of contract ritual into desk task workflow and rental detail page.
- `frontend/src/test/contract-ritual.test.tsx` -- Vitest tests for the complete check-in desk flow.

## Tasks & Acceptance

**Execution:**
- [x] Declare contract ritual and attachment endpoints in `contracts/openapi.yaml`.
- [x] Implement backend services and controllers for contract print, upload, sign, and check-in activation.
- [x] Implement secured attachment storage and JWT-authenticated image streaming.
- [x] Write backend integration tests in `ContractRitualTests.java`.
- [x] Implement frontend contract ritual UI components and rental contract chain viewer.
- [x] Wire contract ritual into `TaskDetailPage.tsx` and `RentalDetailPage.tsx`.
- [x] Write frontend tests in `contract-ritual.test.tsx` verifying the end-to-end check-in flow.

**Acceptance Criteria:**
- Given a staff member in a check-in task with rent paid, when viewing the contract step, then the read-only contract preview renders from `contentSnapshot`.
- Given a printed contract signed by customer, when staff attaches the photo, then contract status becomes `SIGNED` and `signedPhotoUrl` is recorded.
- Given a signed contract and paid rent, when staff clicks "Reveal Access Code & Activate", then the access code is displayed, the reservation becomes `CHECKED_IN`, the unit becomes `RENTED`, and the task is marked `DONE`.
- Given an unauthenticated or unauthorized user, when requesting access code or contract attachment, then the request is rejected with 401/403.
- Given a customer on `RentalDetailPage`, then the full contract chain history is accessible with previous superseded revisions.

## Verification

**Commands:**
- `mvn test` -- All backend contract tests pass (200+ tests).
- `npm test` -- All frontend tests pass including `contract-ritual.test.tsx`.
- `npm run build` -- Production build succeeds.
