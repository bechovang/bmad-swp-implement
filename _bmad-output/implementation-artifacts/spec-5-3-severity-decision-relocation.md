---
title: 'Story 5.3: Severity Decision & Relocation'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '3a90080d066bad359bb212fde1b182c4dd9ee7fb'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-5-context.md'
  - '_bmad-output/implementation-artifacts/spec-5-2-support-ticket-xử-lý-resolve-escalate.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** When a support ticket is escalated by front-desk staff (e.g. water leak, lock malfunction, structural damage), the Facility Manager (FM) needs a dedicated Escalation Inbox to review the full chronological incident thread (original customer report, on-site staff triage notes, escalation rationale) and make an authoritative **Severity Decision**:
1. **Severe (Relocation required):** Moves the damaged unit to `MAINTENANCE`, relocates the customer to an available unit (preserving `Reservation/Rental` codes, `Contract` agreement chain, and paid deposit escrow), issues a new 6-digit access security PIN, automatically registers maintenance and turnover cleaning tasks on the Kanban board, logs `RELOCATION` in the append-only Activity Log, and sends plain-word notifications to the customer.
2. **Not Severe (Return to staff):** Rejects emergency relocation, provides specific management instructions to the staff, returns ticket to `IN_PROGRESS` status assigned to staff, and notifies staff.
Without this workflow, managers cannot triage building emergencies, customers cannot be safely relocated during active rentals, and maintenance tasks fail to register systematically.

**Approach:**
1. **Escalation Inbox & Querying:**
   - Add endpoint `GET /api/v1/escalations` (and `GET /api/v1/escalations/{id}`) allowing `FACILITY_MANAGER`, `ADMIN`, `SYSTEM_ADMINISTRATOR` to query escalations with full ticket context.
2. **Severity Decision API (`POST /api/v1/escalations/{id}/decision`):**
   - Implemented in `TicketService` coordinating with `UnitService`, `TaskService`, `NotificationService`, and `LogService` within a single database transaction.
   - Request DTO `SeverityDecisionRequest`: `{ decision: "SEVERE" | "NOT_SEVERE", managerNote: string, targetUnitId?: number }`.
   - Returns `EscalationDto` with updated decision details and relocation details (if severe).
3. **Severe Path Execution:**
   - If `decision === 'SEVERE'`:
     - Requires `targetUnitId` pointing to an `AVAILABLE` unit.
     - Damaged unit transitions to `MAINTENANCE` via `unitService.markMaintenance(unitId, reason)`.
     - Relocation: updates `Reservation.unit` to `targetUnit` while preserving `id`, `code`, `depositAmountPaid`, `status`, and `contract`.
     - Target unit transitions to `RENTED`.
     - Issues new 6-digit access code for the customer.
     - Spawns maintenance task for damaged unit and cleaning task for turnover via `taskService`.
     - Ticket status transitions back to `IN_PROGRESS` with FM instruction for staff to complete handover.
     - Records Activity Log `RELOCATION` (`actor: FM, fromUnit -> toUnit`).
     - Plain-language customer notifications sent for relocation and new access code.
4. **Not-Severe Path Execution:**
   - If `decision === 'NOT_SEVERE'`:
     - Requires non-blank `managerNote` instructions.
     - Ticket transitions back to `IN_PROGRESS` with FM instructions.
     - Notifies staff with FM instructions.
     - Records Activity Log `SEVERITY_DECISION`.
5. **FM Escalations Page (`frontend/src/pages/manager/EscalationsPage.tsx`):**
   - Replace placeholder at `/escalations` with a full-featured Escalation Inbox.
   - Displays list of escalated tickets with status badges (`PENDING`, `SEVERE`, `NOT_SEVERE`), ticket code, incident type, unit code, customer name, and timestamps.
   - Review panel displays full chronological event thread (customer description, staff resolution notes, escalation note).
   - "Mark Severe (Relocate)" button opens destructive confirmation modal explaining consequences, selecting available target unit, and confirming relocation.
   - "Not Severe (Return to Staff)" button prompts for instructions note.
   - After decision, displays updated decision banner and revealed new access credentials if relocated.

## Boundaries & Constraints

**Always:**
- Single transaction coordination managed through `TicketService` domain orchestration.
- Relocation MUST preserve reservation ID, booking code (`BK-xxxx`), contract (`CT-xxxx`), deposit receipt, and rental dates.
- Unit status transitions must use public domain methods (`UnitService.markMaintenance(unitId, reason)`).
- Relocation requires target unit to be in `AVAILABLE` status.
- New 6-digit PIN code generated and provided for relocated customer.
- ActivityLog records `RELOCATION` with from/to unit codes and FM actor.
- Plain-language microcopy for customer notifications.

**Never:**
- Never allow severe relocation to an occupied or maintenance unit (returns 400 Validation Error / 409 Conflict).
- Never allow severity decisions from customers or regular staff (guarded by `ROLE_FACILITY_MANAGER`, `ROLE_ADMIN`, `ROLE_SYSTEM_ADMINISTRATOR`).
- Never allow a second severity decision on an already resolved escalation (returns 409 Conflict).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| FM views Escalation Inbox | GET `/api/v1/escalations` with FM JWT | 200 OK with list of escalations + tickets context | 403 if Customer/Staff |
| FM decides SEVERE with target unit | Escalation ID 1 (Ticket SR-0033 on M-2), decision: `SEVERE`, targetUnitId: 3 (M-5), note: "Pipe burst, customer relocated to M-5" | Unit M-2 -> MAINTENANCE; Reservation unit -> M-5; M-5 -> RENTED; new 6-digit PIN generated; Maintenance & Cleaning tasks created; ActivityLog recorded; Customer notified; Ticket -> IN_PROGRESS | 200 OK with `EscalationDto` |
| FM decides SEVERE with unavailable unit | targetUnitId is already RENTED or MAINTENANCE | Operation rejected | 400 Bad Request / 409 Conflict with code `UNIT_UNAVAILABLE` |
| FM decides SEVERE without target unit | targetUnitId is null / missing | Operation rejected | 400 Bad Request with field error `targetUnitId: must not be null for SEVERE decision` |
| FM decides NOT_SEVERE with note | decision: `NOT_SEVERE`, note: "Staff to use temporary dehumidifier" | Escalation decision -> `NOT_SEVERE`; Ticket -> `IN_PROGRESS`; Staff notified with FM instructions; ActivityLog recorded | 200 OK with `EscalationDto` |
| Second decision on decided escalation | Escalation already has decision `SEVERE` or `NOT_SEVERE` | Request rejected | 409 Conflict with code `DECISION_ALREADY_MADE` |

## Verification Plan

### Automated Backend Tests
- Create `backend/src/test/java/com/storagehub/support/SeverityDecisionTests.java`:
  - Unit test `TicketService.processSeverityDecision` for `SEVERE` path: verifies unit maintenance, reservation swap, access code generation, task creation, log activity.
  - Unit test `TicketService.processSeverityDecision` for `NOT_SEVERE` path: verifies staff notification and ticket status transition to `IN_PROGRESS`.
  - Edge case tests: invalid target unit, double decision conflict, unauthorized role rejection.
- Run `mvn test` verifying 100% pass across all tests.

### Automated Frontend Tests
- Create `frontend/src/test/escalations-inbox.test.tsx`:
  - Renders `/escalations` for Facility Manager with table/list of escalated tickets.
  - Opens review modal/panel showing verbatim customer description and staff notes.
  - Tests `Severe` decision workflow: selecting available unit, submitting destructive confirmation modal, verifying relocation result and access code display.
  - Tests `Not Severe` decision workflow: submitting manager instructions, verifying ticket return to staff.
  - Tests role-based access control (403 for Customer / Staff).
- Run `npx vitest run` and `npm run build`.

</frozen-after-approval>
