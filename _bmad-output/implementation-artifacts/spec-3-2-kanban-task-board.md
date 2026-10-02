---
title: 'Story 3.2: Kanban Task Board'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'faa87124d912127c43ef5b1af9eea0d6dbf80266'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Staff members managing shift operations lack a centralized, real-time Kanban board to view, filter, and progress operational tasks (check-in, checkout, cleaning, support, contract) across shift columns, and no unified task registry exists to automatically generate check-in tasks when reservations become reserved.

**Approach:** Implement `TaskService` providing a centralized task registry where `CHECK_IN` tasks are automatically generated in the same transaction when a reservation transitions to `RESERVED`. Build REST endpoints (`GET /api/v1/tasks`, `GET /api/v1/tasks/{id}`, `PATCH /api/v1/tasks/{id}/status`) guarded by `ROLE_STAFF`. On the frontend, replace the `/tasks` placeholder with a 3-column Kanban board (**To do**, **In progress**, **Done**) featuring 5 card types with 3px left accent bars (Check-in: indigo, Checkout: sky, Cleaning: amber, Support: red, Contract: slate), header column counters, card type filters, accessible movement controls (drag-and-drop + keyboard/menu controls), and empty state tiles.

## Boundaries & Constraints

**Always:**
- Task creation is centralized in `TaskService.createTask` (AD-6): when reservation transitions to `RESERVED` upon deposit payment, a `CHECK_IN` task is automatically generated in the same atomic transaction.
- 5 task types are supported with distinct 3px left bar colors:
  - `CHECK_IN`: Indigo
  - `CHECKOUT`: Sky
  - `CLEANING`: Amber
  - `SUPPORT`: Red
  - `CONTRACT`: Slate
- `DONE` column cards render muted at 60% opacity with the left colored bar omitted.
- Column headers always display real-time numeric card counts.
- Card movement supports dual interaction: HTML5 drag-and-drop **and** accessible button/menu controls (`◀ Move Left`, `▶ Move Right`, status select) to ensure keyboard accessibility (UX-DR6/DR10).
- Empty columns display a factual `"Nothing here"` tile.
- Task status transitions are recorded in `activity_logs` via `LogService` (`EntityType.TASK`, `Action.STATUS_CHANGE`).
- Board is protected by staff/admin roles (`STAFF`, `FACILITY_MANAGER`, `SYSTEM_ADMINISTRATOR`, `ADMIN`).

**Never:**
- Never allow tasks to be created outside the centralized `TaskService` registry.
- Never rely exclusively on drag-and-drop without providing accessible alternative click/keyboard controls.
- Never allow customers or unauthenticated users to access staff task board endpoints.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Auto-generate Check-in Task | Deposit payment completes successfully (`RESERVED`) | `CHECK_IN` task created with `refCode = reservation.code`, `status = TODO`, `workDate = reservation.startDate` | Transaction rolls back if task creation fails |
| List Tasks (Staff) | `GET /api/v1/tasks?workDate=2026-10-03` with Staff JWT | 200 OK with list of tasks matching filter | 403 if Customer JWT |
| Filter by Task Type | Staff selects "Check-in" filter tab on Kanban board | Board displays only `CHECK_IN` cards; column header counts update to reflect filtered subset | N/A |
| Move Task Column | Staff drags card or clicks "Move to In Progress" (`PATCH /api/v1/tasks/{id}/status`) | 200 OK; task updates to `IN_PROGRESS`, card moves to second column, audit log recorded | 400 if invalid transition; 404 if task not found |
| Render Empty Column | Column has 0 tasks matching active filters | Column body renders `"Nothing here"` tile with subtle dashed container | N/A |

</frozen-after-approval>

## Open Questions

## Code Map

- `contracts/openapi.yaml` -- Declare task endpoints (`GET /tasks`, `GET /tasks/{id}`, `PATCH /tasks/{id}/status`, `POST /tasks`) and schemas (`TaskDto`, `TaskType`, `TaskStatus`, `UpdateTaskStatusRequest`).
- `backend/src/main/java/com/storagehub/entity/Task.java` -- JPA Entity mapping `tasks` table.
- `backend/src/main/java/com/storagehub/entity/TaskType.java` & `TaskStatus.java` -- Enum definitions.
- `backend/src/main/java/com/storagehub/repository/TaskRepository.java` -- Repository for task lookups by date, staff, status, refCode.
- `backend/src/main/java/com/storagehub/dto/TaskDto.java` & `UpdateTaskStatusRequest.java` -- Data transfer records.
- `backend/src/main/java/com/storagehub/service/TaskService.java` -- Task registry service: creating tasks on business triggers, status transitions, query filters, and audit logging.
- `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Trigger `TaskService.createCheckInTask` on successful deposit payments.
- `backend/src/main/java/com/storagehub/controller/TaskController.java` -- REST controller for staff task board.
- `backend/src/test/java/com/storagehub/task/TaskServiceTests.java` & `TaskControllerTests.java` -- Unit & integration test suites.
- `frontend/src/types/task.ts` & `frontend/src/api/task.ts` -- TypeScript definitions and Axios API functions for tasks.
- `frontend/src/components/task/TaskCard.tsx` -- Kanban card component with 3px left bar, type chips, due date, customer/unit metadata, and move actions.
- `frontend/src/components/task/KanbanColumn.tsx` -- Column component with header badge count, droppable area, and "Nothing here" empty state.
- `frontend/src/pages/staff/TaskBoardPage.tsx` -- Full Kanban board screen replacing placeholder at `/tasks` with type filters, date picker, and move handlers.
- `frontend/src/router/routes.tsx` -- Connect real `TaskBoardPage` for `STAFF` role landing at `/tasks`.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for task endpoints.
- `frontend/src/test/task-board.test.tsx` -- Vitest RTL test suite for Kanban columns, filtering, keyboard/drag movement, and status updates.

## Tasks & Acceptance

**Execution:**
- [x] `contracts/openapi.yaml` -- Define task schemas and endpoints -- Enforce contract-first specification.
- [x] `backend/src/main/java/com/storagehub/entity/Task.java` & `repository/TaskRepository.java` -- Implement Task entity and repository -- Database access layer.
- [x] `backend/src/main/java/com/storagehub/service/TaskService.java` & `controller/TaskController.java` -- Implement TaskService registry and REST controller -- Backend business logic.
- [x] `backend/src/main/java/com/storagehub/service/payment/PaymentService.java` -- Hook Check-in task generation on successful deposit payment -- Auto-task creation.
- [x] `backend/src/test/java/com/storagehub/task/` -- Add unit & controller tests for task workflows -- Backend verification.
- [x] `frontend/src/types/task.ts` & `frontend/src/api/task.ts` -- Implement frontend task data layer -- Frontend API client.
- [x] `frontend/src/components/task/` & `frontend/src/pages/staff/TaskBoardPage.tsx` -- Build 3-column Kanban board with 5 card types, filters, and drag/keyboard movement -- UI implementation.
- [x] `frontend/src/router/routes.tsx` & `frontend/src/mocks/handlers.ts` -- Connect `/tasks` route and MSW handlers -- Route integration.
- [x] `frontend/src/test/task-board.test.tsx` -- Write Vitest RTL tests for Kanban board interactions -- UI test verification.

**Acceptance Criteria:**
- Given a staff user accessing `/tasks`, then the Kanban board renders 3 columns (To do, In progress, Done) with header counts and active type filter tabs.
- Given a task card on the board, then it renders with its corresponding 3px left accent bar (Check-in: indigo, Checkout: sky, Cleaning: amber, Support: red, Contract: slate) and unit/reference details.
- Given a task in the Done column, then it renders muted at 60% opacity with no colored left bar.
- Given a staff user moving a card via drag-and-drop or the Move action buttons, then `PATCH /api/v1/tasks/{id}/status` is called and column counts update immediately.
- Given an empty column, then a `"Nothing here"` tile is displayed.

## Implementation Notes
- Created `Task` JPA entity mapping `tasks` table with enums `TaskType` (`CHECK_IN`, `CHECKOUT`, `CLEANING`, `SUPPORT`, `CONTRACT`) and `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`).
- Implemented `TaskService` central task registry with automatic `CHECK_IN` task generation on deposit payment success (`PaymentService.completeSuccessfulPayment`), query filtering, and status transitions with audit logging via `LogService`.
- Created `TaskController` exposing `GET /tasks`, `POST /tasks`, `GET /tasks/{id}`, and `PATCH /tasks/{id}/status` secured by staff/admin roles.
- Created `TaskCard.tsx` with 3px left colored accent bars (Check-in: indigo, Checkout: sky, Cleaning: amber, Support: red, Contract: slate), 60% muted opacity for Done column, and dual movement interaction (HTML5 drag-and-drop + keyboard/buttons).
- Created `KanbanColumn.tsx` with live column counts, droppable area, and `"Nothing here"` empty state container.
- Implemented `TaskBoardPage.tsx` at `/tasks` with task type tabs, date filter defaulting to today, and real-time column updates.
- Added comprehensive unit, controller, and RTL tests (186 backend tests and 160 frontend tests passing).

## Spec Change Log

## Review Triage Log
| Finding | Verdict | Evidence | Action |
|---------|---------|----------|--------|
| Form control focus in TaskCard onKeyDown | Low | Keyboard navigation inside select was intercepted by arrow keys | Patched to ignore keydown when target is SELECT/INPUT |
| Redundant drop mutation on same column in KanbanColumn | Low | Dropping onto same column shouldn't trigger network request or toast | Patched with early return when currentStatus === status |
| Default selectedDate in TaskBoardPage | Low | Initializing to today's date helps staff see shift tasks immediately | Patched default to today in YYYY-MM-DD format |

## Verification

**Commands:**
- `mvn test` -- expected: All backend task tests pass (180+ tests).
- `npm run test` -- expected: All frontend tests pass including new `task-board.test.tsx`.
- `npm run build` -- expected: Production TypeScript and Vite build succeeds with 0 errors.
