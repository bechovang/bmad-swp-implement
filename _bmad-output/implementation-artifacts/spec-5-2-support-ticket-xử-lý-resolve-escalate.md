---
title: 'Story 5.2: Support Ticket — Xử lý, Resolve & Escalate'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '0f00186bc164084f2e27b759fd9b53a4610457bc'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-5-context.md'
  - '_bmad-output/implementation-artifacts/spec-5-1-tạo-route-support-ticket.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When a customer submits a support ticket (`SR-xxxx`), staff assigned to that unit/shift need a dedicated workflow on the Task Board to inspect the incident details, communicate action steps, resolve the ticket on the spot with resolution notes, or escalate severe/complex incidents to the Facility Manager with mandatory explanation. Without this, support cards cannot transition cleanly to Done and managers have no structured escalation inbox.

**Approach:**
1. **Support Task View & Ticket Inspection:**
   - In `TaskDetailPage.tsx`, when viewing a `SUPPORT` task card, render a rich Support Incident panel displaying: Incident Type, original verbatim Customer Description, Unit Code (monospace), Customer Information, Ticket Status, Assigned Staff, and the Resolution/Escalation Thread.
2. **Staff Resolve Action:**
   - Staff provides a resolution note and submits Resolve (`POST /api/v1/support-tickets/{id}/resolve`).
   - `TicketService` transitions ticket status to `RESOLVED`, records `resolutionNote`, marks the associated `SUPPORT` task card as `DONE`, logs `TICKET_RESOLVED` to `ActivityLog`, and sends a plain-words notification to the customer ("Resolved — <note>. See ticket for details.").
3. **Staff Escalate Action:**
   - Staff provides a mandatory escalation note explaining why manager intervention or relocation is required, and submits Escalate (`POST /api/v1/support-tickets/{id}/escalate`).
   - If note is empty or whitespace, the UI disables the button and API rejects with 400 Bad Request (`note: must not be blank`).
   - `TicketService` checks that ticket is not already escalated (enforcing `uk_escalations_ticket` / 1-time escalation rule). If duplicate, returns 409 Conflict (`ALREADY_ESCALATED`).
   - Creates an `Escalation` record (`PENDING` decision), sets ticket status to `ESCALATED`, notifies Facility Manager ("Escalated to Facility Manager — <note>"), and logs `TICKET_ESCALATED` to `ActivityLog`.
4. **API & Contract Definition:**
   - Add `POST /support-tickets/{id}/resolve` and `POST /support-tickets/{id}/escalate` endpoints to `contracts/openapi.yaml`.

## Boundaries & Constraints

**Always:**
- Ticket state transitions must be exclusively managed by `TicketService` as single owner.
- Resolving a ticket marks the corresponding `SUPPORT` task on the Kanban board as `DONE`.
- Escalating a ticket strictly requires a non-empty `note` (validated both on frontend and backend).
- Escalation can happen at most once per support ticket (`TicketID UNIQUE`). Subsequent escalation attempts must be rejected with 409 Conflict.
- Customer receives plain-language notification upon ticket resolution.
- Facility Manager receives notification upon ticket escalation.

**Never:**
- Never allow escalation without a non-blank note.
- Never allow a customer to resolve or escalate tickets directly (guarded by `ROLE_STAFF`, `ROLE_FACILITY_MANAGER`, `ROLE_ADMIN`).
- Never allow resolving or escalating non-existent tickets (returns 404).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Staff Resolves Ticket | Ticket SR-0032 in OPEN/IN_PROGRESS, Staff submits note "Door hinge replaced by site staff" | Ticket -> `RESOLVED`, `resolutionNote` saved, Task -> `DONE`, Customer receives notification, ActivityLog recorded | 200 OK with `SupportTicketDto` |
| Staff Escalates Ticket with Note | Ticket SR-0033, Staff submits note "Flooding, Unit M-2. Customer relocation needed." | Ticket -> `ESCALATED`, Escalation record created in FM inbox, Manager notified, ActivityLog recorded | 200 OK with `SupportTicketDto` |
| Staff Escalates with Blank Note | Note is empty string or only whitespace | Request blocked by validation | 400 Bad Request with field error `note: must not be blank` |
| Re-escalating an Escalated Ticket | Ticket is already `ESCALATED` | Request rejected (1-time escalation invariant) | 409 Conflict with code `ALREADY_ESCALATED` |
| Unauthorized Customer Call | Customer JWT attempts to call resolve or escalate | Access denied | 403 Forbidden |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- Add `POST /support-tickets/{id}/resolve` and `POST /support-tickets/{id}/escalate` endpoints and request schemas (`ResolveSupportTicketRequest`, `EscalateSupportTicketRequest`).
- `backend/src/main/java/com/storagehub/entity/Escalation.java` & `EscalationDecision.java` -- JPA entity for `escalations` table mapped to MySQL schema.
- `backend/src/main/java/com/storagehub/repository/EscalationRepository.java` -- Spring Data repository for `Escalation` entities.
- `backend/src/main/resources/db/migration/V5__support_tickets_notes.sql` -- Flyway migration adding `ResolutionNote` column to `support_tickets` table if needed.
- `backend/src/main/java/com/storagehub/entity/SupportTicket.java` -- Add `resolutionNote` field.
- `backend/src/main/java/com/storagehub/dto/ResolveSupportTicketRequest.java` & `EscalateSupportTicketRequest.java` -- Request DTOs with `@NotBlank` validation annotations.
- `backend/src/main/java/com/storagehub/dto/SupportTicketDto.java` -- Include `resolutionNote` and `escalationNote` fields in response DTO.
- `backend/src/main/java/com/storagehub/service/TicketService.java` -- Implement `resolveTicket` and `escalateTicket` domain methods with validation, task completion, notifications, and activity logging.
- `backend/src/main/java/com/storagehub/controller/SupportTicketController.java` -- Expose `/api/v1/support-tickets/{id}/resolve` and `/api/v1/support-tickets/{id}/escalate`.
- `backend/src/test/java/com/storagehub/support/SupportTicketHandlingTests.java` -- Integration tests for resolve, escalate, empty note validation, duplicate escalation rejection, and task synchronization.
- `frontend/src/types/support.ts` & `frontend/src/api/support.ts` -- Add `ResolveSupportTicketRequest`, `EscalateSupportTicketRequest`, `resolveSupportTicket()`, and `escalateSupportTicket()`.
- `frontend/src/pages/staff/TaskDetailPage.tsx` -- Implement Support incident handling view with context display, staff note input, Resolve and Escalate buttons.
- `frontend/src/mocks/handlers.ts` -- Add MSW mock endpoints for ticket resolve and escalate.
- `frontend/src/test/support-ticket-handling.test.tsx` -- Vitest tests covering support task details, resolve flow, escalate flow, note validation, and status transitions.

## Tasks & Acceptance

**Execution:**
- [ ] Add resolve and escalate endpoints and request schemas to `contracts/openapi.yaml`.
- [ ] Create Flyway migration `V5__support_tickets_notes.sql` adding `ResolutionNote` column to `support_tickets`.
- [ ] Create `Escalation.java` and `EscalationDecision.java` entity and `EscalationRepository.java`.
- [ ] Update `SupportTicket.java` and `SupportTicketDto.java` with resolution and escalation note fields.
- [ ] Implement `resolveTicket` and `escalateTicket` methods in `TicketService.java`.
- [ ] Add endpoints to `SupportTicketController.java` guarded by Staff and Manager roles.
- [ ] Write backend integration tests in `SupportTicketHandlingTests.java`.
- [ ] Update frontend types in `frontend/src/types/support.ts` and API functions in `frontend/src/api/support.ts`.
- [ ] Implement Support Ticket details panel in `frontend/src/pages/staff/TaskDetailPage.tsx` with Resolve & Escalate actions.
- [ ] Update MSW mock handlers in `frontend/src/mocks/handlers.ts`.
- [ ] Write frontend tests in `frontend/src/test/support-ticket-handling.test.tsx`.

**Acceptance Criteria:**
- Given a staff user viewing a `SUPPORT` task card, when opening the task detail, then full incident context (incident type, original customer description, unit code, customer name, status) is displayed alongside a staff note field.
- Given a staff user submitting Resolve with a note, then the ticket status updates to `RESOLVED`, the associated Kanban task card is marked `DONE`, the resolution note is saved, and a notification is sent to the customer.
- Given a staff user attempting to Escalate without entering a note, then the Escalate button is disabled and the backend rejects blank notes with 400 Bad Request.
- Given a staff user submitting Escalate with a note, then an `Escalation` record is created for the Facility Manager, ticket status updates to `ESCALATED`, and the manager is notified.
- Given an already escalated ticket, when attempting to escalate again, then the system returns 409 Conflict (`ALREADY_ESCALATED`).

## Implementation Notes

## Design Notes

- Notification to customer on resolution:
  - Type: `TICKET_RESOLVED`
  - Title: `Resolved — <note>. See ticket for details.`
  - DeepLink: `/support`
- Notification to Facility Manager on escalation:
  - Type: `TICKET_ESCALATED`
  - Title: `Escalated to Facility Manager — <note>`
  - DeepLink: `/escalations` (or `/staff/tasks`)
- Duplicate escalation prevention: Check `escalationRepository.findByTicketId(ticket.getId())` before creating new escalation.

## Verification

**Commands:**
- `mvn test -Dtest=SupportTicketHandlingTests` -- expected: All backend resolve and escalate tests pass.
- `npx vitest run src/test/support-ticket-handling.test.tsx` -- expected: All frontend support ticket handling tests pass.
- `npm run build` -- expected: Frontend build succeeds with zero errors.
- `mvn test` -- expected: Full backend test suite passes with zero regressions.
