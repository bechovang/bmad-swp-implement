---
title: 'Story 4.3: Addendum trail — 7 ngày đeo bám + desk ritual + chain'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '342f7f4'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** While rental extensions take immediate effect upon payment confirmation (Story 4.2), physical contract addenda (`CT-…-A1`) require legal tracking and signing at the front desk within a 7-day window. If not tracked across customer, staff, and system channels, paper trails get lost without accountability.

**Approach:**
1. **3-Channel Reminders:**
   - Customer banner on `RentalDetailPage.tsx` indicating deadline and signing instructions.
   - Kanban `CONTRACT` signature task on `TaskBoard.tsx` with customer, unit, addendum code, and 7-day due date.
   - Bell notifications to both customer and desk staff upon addendum generation and approaching deadline.
2. **Desk Ritual for Addendum Signing:**
   - Dedicated Task Detail view for `CONTRACT` tasks:
     - Read-only Addendum preview with financial breakdown and new checkout date.
     - Cash-collection shortcut if extension payment is still `PENDING_CASH`.
     - Print Addendum button.
     - Upload attachment of physical signed copy via `POST /api/v1/attachments`.
     - "Confirm Signature" button transitions Addendum to `SIGNED`, records `CONTRACT_SIGNED` in Activity Log, and marks task complete.
3. **Guard Registry & Snap-back:**
   - Enforce Story 3.5 snap-back: dragging/moving `CONTRACT` card to `DONE` without an attached signed photo returns 409 Conflict with structured payload (`CLOSING_STEP_MISSING`, `CONTRACT_UNSIGNED`), causing card to snap back and display error toast.
4. **Exception Handling:**
   - Allow staff to close addenda as `EXPIRED` (when customer fails to sign after reminder period) or `VOIDED` (with mandatory reason log) without revoking the paid rental extension date.
5. **Contract Chain View:**
   - Display addenda rows beneath the base contract on `RentalDetailPage.tsx` with monospace code chips, status badges, signing deadlines, and permanent view access to signed photo copies.

## Boundaries & Constraints

**Always:**
- Expiry or failure to physically sign an addendum within 7 days does NOT revoke the paid extension or revert `endDate` — extension is legally locked by payment; addendum is procedural audit documentation.
- Completing a `CONTRACT` task requires a verified signed copy photo URL or a valid `EXPIRED`/`VOIDED` transition with logged reason.
- Uploaded attachment paths must be authenticated API routes (`/api/v1/attachments/...`), never public static paths.
- Addendum content snapshots remain read-only across all customer and staff UI.

**Never:**
- Never allow manual editing of addendum content terms.
- Never let `CONTRACT` task move to `DONE` without signed photo (unless explicitly closed via Expired/Voided).
- Never hide superseded or closed addenda from the contract chain history.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| View Addendum in Chain | Customer views `RentalDetailPage` with addendum `CT-1042-A1` | Amber reminder banner displayed if `AWAITING_SIGNATURE`; addenda rows rendered under base contract | N/A |
| Staff Signs Addendum | `POST /api/v1/contracts/{id}/sign` with `signedPhotoUrl` | Status `SIGNED`, activity log appended, customer notified, task can move to `DONE` | 400 if photo missing; 404 if not found |
| Staff Completes Task Without Signature | Drag `CONTRACT` card to `DONE` while unsigned | 409 Conflict with `CONTRACT_UNSIGNED`, card snaps back | Toast displays missing step |
| Staff Closes Addendum as Expired | `POST /api/v1/contracts/{id}/expire` with reason | Status `EXPIRED`, activity log recorded, task completed | 400 if invalid state |
| Staff Voids Addendum | `POST /api/v1/contracts/{id}/void` with mandatory reason | Status `VOIDED`, activity log recorded with reason, task completed | 400 if reason empty |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Add endpoints for contract/addendum expire (`POST /contracts/{id}/expire`) and void (`POST /contracts/{id}/void`).
- `backend/src/main/java/com/storagehub/service/ContractService.java` -- Add `expireContract`, `voidContract`, and ensure `signContract` handles addenda and triggers notifications.
- `backend/src/main/java/com/storagehub/controller/ContractController.java` -- Expose `/expire` and `/void` endpoints for staff.
- `backend/src/main/java/com/storagehub/service/TaskService.java` -- Enforce contract closing guard on task completion and link task to addendum codes.
- `backend/src/test/java/com/storagehub/extension/AddendumTrailTests.java` -- Backend integration tests for addendum desk ritual, reminders, snap-back, and expire/void flows.
- `frontend/src/types/contract.ts` & `frontend/src/api/contract.ts` -- Add API methods for `expireContract`, `voidContract`, and addendum chain models.
- `frontend/src/pages/staff/TaskDetailPage.tsx` -- Implement dedicated `CONTRACT` task ritual UI (Addendum preview, cash confirm button, print, photo upload, sign confirmation, expire/void actions).
- `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Render 7-day addendum reminder banner and addenda rows inside the contract chain.
- `frontend/src/mocks/handlers.ts` -- MSW handlers for addendum sign, expire, void, and chain retrieval.
- `frontend/src/test/addendum-trail.test.tsx` -- Vitest tests covering desk ritual, reminder banner, and contract chain presentation.

## Tasks & Acceptance

**Execution:**
- [x] Update `contracts/openapi.yaml` with contract expire and void endpoints.
- [x] Implement backend `expireContract` and `voidContract` in `ContractService.java` and expose via `ContractController.java`.
- [x] Verify/enhance `TaskService.java` closing step guard for `CONTRACT` task types.
- [x] Write backend integration tests in `AddendumTrailTests.java`.
- [x] Update frontend types and API in `contract.ts`.
- [x] Enhance `TaskDetailPage.tsx` to handle `CONTRACT` signature tasks with addendum desk ritual.
- [x] Update `RentalDetailPage.tsx` with 7-day reminder banner and addenda chain listing.
- [x] Update MSW mock handlers in `handlers.ts`.
- [x] Write frontend unit tests in `addendum-trail.test.tsx`.

**Acceptance Criteria:**
- Given an extension payment confirmed, when customer views `RentalDetailPage`, then an amber banner shows the 7-day signing deadline and the addendum appears in the contract chain.
- Given a `CONTRACT` task on the board, when staff views `TaskDetailPage`, then they can preview the addendum, confirm pending cash (if applicable), print, upload the signed photo, and complete the ritual.
- Given an unsigned addendum task, when staff attempts to move it to `DONE`, then backend returns 409 Conflict and frontend snaps back with a descriptive toast.
- Given staff clicking Expire or Void with a reason, then the addendum is transitioned with audit log and the task is closed.

## Implementation Notes

<!-- Agent-owned. Append-only during implementation. -->

## Spec Change Log

<!-- Append-only. Populated by step-04 during review loops. -->

## Review Triage Log

<!-- Append-only. Populated by step-04 on every review pass. -->
