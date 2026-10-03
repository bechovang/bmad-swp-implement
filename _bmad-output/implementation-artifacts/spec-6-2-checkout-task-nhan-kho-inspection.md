---
title: 'Story 6.2: Checkout Task — nhận kho + Inspection'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'fb7206505acc7d8a23b672b72e9a3afcc120da57'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-6-context.md'
  - '_bmad-output/implementation-artifacts/spec-6-1-checkout-request.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When a customer arrives at the facility to check out, on-duty staff need a structured desk ritual to accept the unit and keys, and conduct a standardized 4-point physical inspection (`ACCESS_CARD`, `PADLOCK`, `CLEANLINESS`, `STRUCTURE`). Without a recorded inspection, any damage deductions lack evidence and accountability, and staff could prematurely close checkout tasks without verifying unit condition.

**Approach:**
1. **Checkout Task Reception & Inspection Ritual (`CheckoutTaskModal.tsx` & `TaskBoardPage.tsx`):**
   - Opening a `CHECKOUT` task card on the Kanban Board presents the Checkout Reception & Inspection workflow.
   - **Step 1: Return Checklist:** Interactive confirmation for key handover and unit emptying (`Key/Access Card Returned`, `Unit Emptied`).
   - **Step 2: 4-Item Inspection Matrix:**
     - `ACCESS_CARD`: OK / MINOR / MAJOR
     - `PADLOCK`: OK / MINOR / MAJOR
     - `CLEANLINESS`: OK / MINOR / MAJOR
     - `STRUCTURE`: OK / MINOR / MAJOR
     - Staff notes for each inspection line.
   - Highlights any `MAJOR` result with a warning banner: *"MAJOR finding will form the required basis for Settlement Charges in Step 6.3."*
2. **Backend Persistence & API (`InspectionService.java` & `InspectionController.java`):**
   - Endpoint `GET /api/v1/reservations/{reservationId}/inspections` (or `GET /api/v1/checkout-tasks/{taskId}`): returns checkout task reception status and current inspections.
   - Endpoint `POST /api/v1/reservations/{reservationId}/inspections`:
     - Validates staff authentication (`ROLE_STAFF` / `ROLE_FACILITY_MANAGER` / `ROLE_ADMIN`).
     - Persists the 4 inspection items in the `inspections` table.
     - Logs `INSPECTION_COMPLETED` in `ActivityLog`.
     - Returns inspection items and computed `hasMajorDamage` / `majorItems` summary.
3. **Snap-back Closing Guard (Story 3.5 Registry):**
   - Moving a `CHECKOUT` task card to `DONE` is blocked by snap-back guard until Story 6.3 settlement is finalized, preventing premature task completion.
4. **Automated Verification:**
   - 100% automated backend tests (`mvn test`) and frontend tests (`npx vitest run`), clean production build.

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/migration/V8__inspections_enhancements.sql`: Migration linking `inspections` to `reservations` and making `SettlementID` nullable.
- `backend/src/main/java/com/storagehub/entity/Inspection.java`: JPA entity for inspection records.
- `backend/src/main/java/com/storagehub/entity/InspectionItem.java`: Enum (`ACCESS_CARD`, `PADLOCK`, `CLEANLINESS`, `STRUCTURE`).
- `backend/src/main/java/com/storagehub/entity/InspectionResult.java`: Enum (`OK`, `MINOR`, `MAJOR`).
- `backend/src/main/java/com/storagehub/repository/InspectionRepository.java`: JPA repository for inspection records.
- `backend/src/main/java/com/storagehub/dto/InspectionItemDto.java` & `SubmitInspectionRequest.java` & `InspectionSummaryDto.java`: DTOs.
- `backend/src/main/java/com/storagehub/service/InspectionService.java`: Service managing inspection recording and MAJOR item aggregation.
- `backend/src/main/java/com/storagehub/controller/InspectionController.java`: REST controller for inspection endpoints.
- `contracts/openapi.yaml`: OpenAPI definitions for inspection endpoints.
- `frontend/src/types/checkout.ts`: TypeScript interfaces for inspections and checkout task detail.
- `frontend/src/api/checkout.ts`: Frontend API client functions for checkout tasks and inspections.
- `frontend/src/components/checkout/CheckoutTaskModal.tsx`: Modal component for checkout reception and inspection matrix.
- `frontend/src/pages/tasks/TaskBoardPage.tsx`: Integration to launch checkout inspection modal on `CHECKOUT` card click.
- `frontend/src/mocks/handlers.ts`: MSW mock handlers for checkout task inspection.
- `frontend/src/test/checkout-task-inspection.test.tsx`: Comprehensive frontend Vitest suite.

## Implementation Plan

1. **Backend Layer:**
   - Create Flyway migration `V8__inspections_enhancements.sql`.
   - Implement `Inspection` entity, enums (`InspectionItem`, `InspectionResult`), repository, and DTOs.
   - Implement `InspectionService` and `InspectionController`.
   - Update `TaskService` snap-back guard for `CHECKOUT` tasks.
   - Write backend tests (`InspectionTests.java`, `InspectionControllerTests.java`).
2. **OpenAPI & Contracts:**
   - Update `contracts/openapi.yaml` with inspection schemas and endpoints.
3. **Frontend Layer:**
   - Add TypeScript types in `frontend/src/types/checkout.ts`.
   - Add API methods in `frontend/src/api/checkout.ts`.
   - Create `CheckoutTaskModal.tsx` with return checklist and 4-point inspection matrix.
   - Wire `CheckoutTaskModal` into `TaskBoardPage.tsx`.
   - Add MSW handlers in `frontend/src/mocks/handlers.ts`.
4. **Testing & Verification:**
   - Write `frontend/src/test/checkout-task-inspection.test.tsx`.
   - Run `mvn test`, `npx vitest run`, and `npm run build`.

## Acceptance Criteria

- **AC-1:** Staff can open a `CHECKOUT` task card from the Kanban Task Board to view reception & inspection ritual.
- **AC-2:** Step 1 requires confirming key return and unit vacating checklist.
- **AC-3:** Step 2 presents the 4-item inspection matrix (`ACCESS_CARD`, `PADLOCK`, `CLEANLINESS`, `STRUCTURE`) with evaluation status (`OK`, `MINOR`, `MAJOR`) and notes.
- **AC-4:** Any `MAJOR` item triggers a warning banner and is recorded in the summary for Story 6.3 Settlement Charges.
- **AC-5:** Submitting inspection persists all 4 items to the database and logs `INSPECTION_COMPLETED` in `ActivityLog`.
- **AC-6:** Attempting to drag/move `CHECKOUT` task to `DONE` before settlement closure is rejected by the snap-back guard.
- **AC-7:** 100% backend and frontend tests pass cleanly.
