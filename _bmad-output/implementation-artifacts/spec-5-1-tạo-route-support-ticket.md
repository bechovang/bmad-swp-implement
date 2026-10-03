---
title: 'Story 5.1: Tạo + route Support Ticket'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: 'fcf52d47d52469d5718abdab3f58a0dce9659c00'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-5-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers experiencing problems with their rented units (such as access issues, hardware failures, security concerns, or cleanliness) need a structured way to report incidents. Currently, there is no support submission mechanism, ticket tracking entity, or automatic shift-aware routing to assign tasks to the correct staff member on duty in that unit's zone.

**Approach:**
1. **Support Ticket Creation & Validation:**
   - Provide a New Support Ticket form accessible from both `RentalDetailPage` and `SupportPage` (Support List).
   - Validate customer input: unit must belong to one of the customer's active/checked-in rentals, `incidentType` (`LOST_ACCESS`, `DEVICE_ISSUE`, `SECURITY`, `CLEANLINESS`, `OTHER`), and mandatory description.
2. **Shift-Aware Routing:**
   - On ticket creation (`SR-xxxx`), query `staff_assignments` for the unit's `ZoneID` on the current date (`Asia/Ho_Chi_Minh`) during the current shift slot (`MORNING`: 06:00–14:00, `AFTERNOON`: 14:00–22:00, `EVENING`: 22:00–06:00).
   - If on-duty staff is found, assign the ticket to that staff member.
   - If no staff is assigned in that zone/shift, fallback gracefully to unassigned status / default facility manager notification so tickets are never silently dropped.
3. **Transaction Coordination & Notifications:**
   - Create a `SUPPORT` task card via `TaskService` registry in the same transaction.
   - Send `TASK_ASSIGNED` notification to the assigned staff and `TICKET_RECEIVED` notification to the customer.
   - Append `SUPPORT_TICKET` creation to `ActivityLog`.
4. **API & Contract Definition:**
   - Expose `POST /api/v1/support-tickets` (Customer submission), `GET /api/v1/support-tickets` (Customer / Staff list query), and `GET /api/v1/support-tickets/{id}` in `contracts/openapi.yaml`.

## Boundaries & Constraints

**Always:**
- A customer can only create a support ticket for a unit that belongs to their own `CHECKED_IN` / active rental.
- The ticket code must follow format `SR-xxxx` (e.g. `SR-0034`).
- Support ticket creation must automatically generate a `SUPPORT` task on the Kanban board.
- The shift routing must evaluate current date and time in `Asia/Ho_Chi_Minh` timezone.
- Activity logs and dual notifications (staff task assignment + customer ticket receipt) must execute in the same transaction.

**Never:**
- Never allow ticket submission without a valid unit or with an empty description.
- Never silently drop a ticket when no staff is scheduled on shift — must assign to fallback queue / notify FM.
- Never allow customers to create tickets for other customers' units.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Customer Creates Valid Ticket | Customer selects rented unit S-3, incident `LOST_ACCESS`, description "Keypad not accepting PIN" | Ticket `SR-xxxx` created with status `OPEN`, assigned to on-duty staff Minh, `SUPPORT` task card created on board, customer and staff notified | 201 Created with `SupportTicketDto` |
| Ticket Submission for Unit Not Owned | Customer submits unit M-2 belonging to another user | Request rejected | 400/403 with `INVALID_UNIT_OR_RENTAL` |
| Ticket Submission with Blank Description | Description is empty or only whitespace | Validation blocks request | 400 Bad Request with field error `description: must not be blank` |
| No Staff Scheduled on Current Shift | Unit is in Zone B, no staff assignment for today's current shift | Ticket created, `assignedStaff` set to null (or fallback staff), `SUPPORT` task created, manager notified | 201 Created with unassigned ticket status |
| Query Customer Tickets | `GET /api/v1/support-tickets` with Customer JWT | Returns list of tickets created by authenticated customer | 200 OK |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Add support ticket schemas (`CreateSupportTicketRequest`, `SupportTicketDto`, `IncidentType`, `SupportTicketStatus`) and endpoints (`POST /support-tickets`, `GET /support-tickets`, `GET /support-tickets/{id}`).
- `backend/src/main/resources/db/migration/V4__support_tickets_description.sql` -- Add migration if description column is required on `support_tickets` table.
- `backend/src/main/java/com/storagehub/entity/SupportTicket.java` & `IncidentType.java` & `SupportTicketStatus.java` -- JPA entities matching `support_tickets` table.
- `backend/src/main/java/com/storagehub/entity/StaffAssignment.java` & `Shift.java` -- JPA entities for `staff_assignments` lookup.
- `backend/src/main/java/com/storagehub/repository/SupportTicketRepository.java` & `StaffAssignmentRepository.java` -- Spring Data repositories for ticket and shift queries.
- `backend/src/main/java/com/storagehub/service/TicketService.java` -- Core ticket domain service managing ticket creation, shift routing, task creation, notifications, and audit logging.
- `backend/src/main/java/com/storagehub/controller/SupportTicketController.java` -- REST controller exposing `/api/v1/support-tickets` endpoints.
- `backend/src/main/java/com/storagehub/service/TaskService.java` -- Support task creation and DTO enrichment for `SUPPORT` task types.
- `backend/src/test/java/com/storagehub/support/SupportTicketCreationTests.java` -- Integration tests for ticket creation, validation, and shift routing.
- `frontend/src/types/support.ts` & `frontend/src/api/support.ts` -- TypeScript definitions and API client for support tickets.
- `frontend/src/pages/support/NewSupportModal.tsx` & `frontend/src/pages/support/SupportPage.tsx` -- New Support form modal and support ticket list page for customers.
- `frontend/src/pages/rentals/RentalDetailPage.tsx` -- Add "Report Issue / New Support" CTA button opening the support modal.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers for ticket creation and listing.
- `frontend/src/test/support-ticket.test.tsx` -- Vitest tests covering support ticket form validation, submission, and list display.

## Tasks & Acceptance

**Execution:**
- [x] Add support ticket and incident endpoints and schemas to `contracts/openapi.yaml`.
- [x] Create Flyway migration `V4__support_tickets_description.sql` adding `Description` column to `support_tickets`.
- [x] Implement backend entities (`SupportTicket`, `IncidentType`, `SupportTicketStatus`, `StaffAssignment`, `Shift`) and repositories (`SupportTicketRepository`, `StaffAssignmentRepository`).
- [x] Implement `TicketService.java` with shift routing logic based on unit zone, current date, and time window in `Asia/Ho_Chi_Minh`.
- [x] Implement `SupportTicketController.java` with customer ticket submission and listing endpoints.
- [x] Update `TaskService.java` with `createSupportTask` integration and DTO mapping.
- [x] Write backend integration tests in `SupportTicketCreationTests.java`.
- [x] Add frontend types and API client in `frontend/src/types/support.ts` and `frontend/src/api/support.ts`.
- [x] Implement `NewSupportModal.tsx` with unit dropdown, incident type selector, and description validation.
- [x] Implement `SupportPage.tsx` customer support list and link "Report Issue" in `RentalDetailPage.tsx`.
- [x] Update MSW mock handlers in `frontend/src/mocks/handlers.ts`.
- [x] Write frontend tests in `frontend/src/test/support-ticket.test.tsx`.

**Acceptance Criteria:**
- Given a customer with a `CHECKED_IN` rental, when submitting the New Support form with an incident type and description, then a support ticket (`SR-xxxx`) is created and routed to the on-duty staff for that unit's zone.
- Given a support ticket created, then a `SUPPORT` task card appears on the Kanban board with unit code, incident details, and assigned staff.
- Given a ticket creation request with no unit or blank description, then the API returns 400 with descriptive field errors.
- Given a customer viewing `RentalDetailPage` or `SupportPage`, then they can open the New Support modal and submit issues easily.

## Implementation Notes

<!-- Agent-owned. Append-only during implementation. -->

## Design Notes

- Shift time definitions in `Asia/Ho_Chi_Minh`:
  - `MORNING`: 06:00 to 14:00
  - `AFTERNOON`: 14:00 to 22:00
  - `EVENING`: 22:00 to 06:00
- Shift determination checks `LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))`.
- If no assignment exists for `(ZoneID, WorkDate, Shift)`, `TicketService` checks for any staff assigned in the facility, or leaves `AssignedStaffID` null and notifies the Facility Manager (Tuan Le, ID 3).

## Verification

**Commands:**
- `mvn test -Dtest=SupportTicketCreationTests` -- expected: All backend ticket creation tests pass.
- `npx vitest run src/test/support-ticket.test.tsx` -- expected: All frontend support ticket component tests pass.
- `npm run build` -- expected: Frontend build succeeds with zero errors.
