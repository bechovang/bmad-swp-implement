# Spec 6.5: Cleaning hoàn tất từ card + Turnover Buffer

## Story Information
- **Story ID:** `6-5-cleaning-hoàn-tất-từ-card-turnover-buffer`
- **Epic:** Epic 6: Checkout, Settlement & Turnover
- **Status:** done

## Goal
Enable staff to complete unit Cleaning tasks directly from the Kanban board task cards (without navigating to a separate screen, fulfilling FR-19). The system verifies the Turnover Buffer duration defined in the active `RentalPolicy` rule `TURNOVER_BUFFER` (e.g. 2 days). If the turnover buffer has not yet elapsed, moving the task to `DONE` is blocked and snaps back with an informative toast displaying the date when the buffer clears. Once the buffer duration has elapsed, completing the cleaning task transitions the unit from `PREPARING` to `AVAILABLE` (or `RESERVED` if an upcoming booking exists), and records the state transition in the append-only activity log.

## Acceptance Criteria
1. **Completion Directly on Card (FR-19):**
   - Staff can complete a Cleaning task directly on the Kanban task card via quick actions, drag-and-drop, or status dropdown without requiring a separate screen.
2. **Turnover Buffer Validation:**
   - Backend evaluates `bufferClearedDate = task.workDate + turnoverBufferDays`.
   - If `currentDate < bufferClearedDate`:
     - Blocks moving task to `DONE` with `409 CLOSING_STEP_MISSING` / `TURNOVER_BUFFER_PENDING`.
     - Frontend snaps card back and displays toast: `"Unit S-3 turnover buffer pending until 2026-10-05"`.
3. **Unit State Transition & Next Reservation Check:**
   - When buffer is satisfied and Cleaning task is marked `DONE`:
     - System checks if there are upcoming bookings for this unit (`PENDING_PAYMENT`, `RESERVED`, `CHECKED_IN`).
     - If next reservation exists: Unit status -> `RESERVED`.
     - If no upcoming reservation: Unit status -> `AVAILABLE`.
     - Appends `Action.STATUS_CHANGE` for `EntityType.UNIT` in Activity Log.
4. **Maintenance Turnover Alignment (FR-20):**
   - Ending maintenance routines follow the same `PREPARING` -> `CLEANING` -> Turnover Buffer path, ensuring units never bypass the turnover buffer.

## Technical Plan
1. **Backend Service (`TaskService.java`):**
   - Inject `RentalPolicyRepository` and `PolicyRuleRepository`.
   - Implement `validateCleaningClosingGuards(Task task)`: calculates buffer cleared date from policy rule `TURNOVER_BUFFER` (or default 2 days) and blocks premature completion.
   - Implement `onCleaningCompleted(Task task, Long actorId)`: updates unit status to `AVAILABLE` (or `RESERVED` if upcoming booking exists) and logs activity.
   - Write comprehensive unit & integration tests in `TaskServiceTests.java` and `CleaningTaskTests.java`.
2. **Frontend Kanban & Mock Layer:**
   - Update `frontend/src/mocks/handlers.ts` to enforce turnover buffer check on `PATCH /tasks/:id/status`.
   - Ensure `TaskCard.tsx` and `TaskBoardPage.tsx` handle turnover buffer validation error toasts and smooth transitions.
   - Write automated frontend tests in `frontend/src/test/cleaning-turnover.test.tsx`.
