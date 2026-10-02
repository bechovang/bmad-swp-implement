---
title: 'Story 3.5: Snap-back chống Done ảo'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'b1c2d3e4f5012370cb315dca5839db7b7aaf79'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Staff members working on the Kanban Task Board may inadvertently or prematurely drag a task card directly to the "Done" column (or use keyboard navigation to mark it complete) before critical business milestones (e.g. rent collection, physical contract signing, access code handover) have been completed. This leads to phantom completion ("Done ảo") and inconsistent operational states.

**Approach:**
1. Implement a server-side guard registry in `TaskService` that verifies all mandatory closing steps for the specific task type before allowing a transition to `DONE`.
2. For `CHECK_IN` tasks, verify that: (a) Rent payment is completed (`SUCCEEDED`), (b) Contract is `SIGNED`, and (c) Check-in activation/access code handover has occurred (`CHECKED_IN`).
3. If any closing step is missing, reject `PATCH /api/v1/tasks/{id}/status` with **HTTP 409 Conflict** containing a structured error payload (`missingStep` machine code, `stepLabel` human message).
4. On the frontend Kanban board (`TaskBoardPage`), intercept 409 errors on task moves, **snap the card back** immediately to its previous column, and display a high-visibility toast rendered directly from the backend payload's `stepLabel`.

## Boundaries & Constraints

**Always:**
- Enforce closing step guards on all status transition paths (drag-and-drop, card action menus, and keyboard shortcuts).
- Return structured 409 Conflict payloads with unambiguous machine codes (e.g., `RENT_PAYMENT_PENDING`, `CONTRACT_UNSIGNED`, `CHECKIN_NOT_ACTIVATED`).
- Render UI feedback and error toasts directly from backend payload data without brittle frontend string parsing.
- Card snap-back must be immediate and synchronous in the UI to prevent visual desync.

**Never:**
- Never allow a task to persist in `DONE` status when backend validation fails.
- Never hardcode error messages on frontend that could diverge from backend domain rules.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Move Check-in to Done with Unpaid Rent | Move `CHECK_IN` card to `DONE` while rent is `PENDING` | 409 Conflict `{ code: "CLOSING_STEP_MISSING", missingStep: "RENT_PAYMENT_PENDING", stepLabel: "Rent payment is still pending on this check-in." }` | Card snaps back to original column, toast displayed |
| Move Check-in to Done with Unsigned Contract | Move `CHECK_IN` card to `DONE` while rent paid but contract `DRAFT` | 409 Conflict `{ code: "CLOSING_STEP_MISSING", missingStep: "CONTRACT_UNSIGNED", stepLabel: "Contract signature is required before completing check-in." }` | Card snaps back to original column, toast displayed |
| Move Check-in to Done with All Steps Complete | Move `CHECK_IN` card to `DONE` with rent paid, contract signed, code revealed | 200 OK with task status `DONE` | Card renders in Done column with 60% muted styling |
| Cleaning / Simple Task to Done | Move `CLEANING` or `SUPPORT` task to `DONE` | 200 OK (no closing step required) | Card successfully moves to Done |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Specify 409 `MissingClosingStepErrorDto` payload in task status update endpoint.
- `backend/src/main/java/com/storagehub/service/TaskService.java` -- Closing step validation registry in `updateTaskStatus`.
- `backend/src/main/java/com/storagehub/exception/MissingClosingStepException.java` -- Custom exception with machine code and step label.
- `backend/src/test/java/com/storagehub/task/TaskSnapBackTests.java` -- Backend unit and integration tests for snap-back guards.
- `frontend/src/pages/staff/TaskBoardPage.tsx` -- Drag/drop and keyboard status transition handlers with snap-back rollback logic.
- `frontend/src/mocks/handlers.ts` -- MSW mock for 409 snap-back responses.
- `frontend/src/test/task-snap-back.test.tsx` -- Vitest tests verifying card snap-back and toast notification.

## Tasks & Acceptance

**Execution:**
- [x] Declare 409 structured error schema in `contracts/openapi.yaml`.
- [x] Implement closing step guard registry in `TaskService.java` and `TaskController.java`.
- [x] Add backend unit tests in `TaskSnapBackTests.java`.
- [x] Implement frontend snap-back rollback and toast notification in `TaskBoardPage.tsx`.
- [x] Add frontend test suite in `task-snap-back.test.tsx`.

**Acceptance Criteria:**
- Given a staff member dragging an incomplete check-in card to Done, when backend rejects with 409, then the card snaps back to its origin column and a toast displays the specific missing step.
- Given a fully completed check-in card (rent paid, contract signed, PIN handed over), when moved to Done, then the task status updates to `DONE` and renders muted.
- Given keyboard navigation to move a card to Done, then the exact same closing step guards are enforced.

## Verification

**Commands:**
- `mvn test` -- All backend task snap-back tests pass.
- `npm test` -- All frontend tests pass including `task-snap-back.test.tsx`.
- `npm run build` -- Production build succeeds.
