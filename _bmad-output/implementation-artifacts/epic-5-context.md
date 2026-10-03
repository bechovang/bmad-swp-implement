# Epic 5 Context: Support & Escalation with Relocation

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Enable customers to submit support tickets for rented units, route them automatically to the on-duty staff member based on unit zone and current shift, allow staff to resolve on-site or escalate to the Facility Manager, and enable the FM to make severity decisions including severe maintenance triggers with customer unit relocation (preserving rental, contract, and deposit identifiers while re-keying access credentials). Customers track the entire ticket resolution arc via the Support List and detail drawer.

## Stories

- Story 5.1: Tạo + route Support Ticket
- Story 5.2: Support Ticket xử lý — Resolve / Escalate
- Story 5.3: Severity Decision + Relocation
- Story 5.4: Support List + detail drawer (khách)

## Requirements & Constraints

- **FR-23 (P1):** Create & Route Support Ticket: Customer selects unit from active rental, chooses `IncidentType` (`LOST_ACCESS`, `DEVICE_ISSUE`, `SECURITY`, `CLEANLINESS`, `OTHER`), provides required description. Ticket code format `SR-xxxx`. Automatically routed to on-duty staff based on `staff_assignments` (Unit's Zone × current shift MORNING/AFTERNOON/EVENING on current date in `Asia/Ho_Chi_Minh` timezone). Creates `SUPPORT` task card via `TaskService` registry in the same transaction. If no on-duty staff found in zone, falls back to unassigned queue + notification to FM. Non-active/invalid unit submission is blocked with inline validation errors.
- **FR-24 (P1):** Ticket Handling (Staff): Staff opens Support Task card. View contains verbatim customer description, incident type, unit mono code, customer info. Staff can `Resolve` with resolution note (customer receives plain-word notification, card marked complete) or `Escalate` to Facility Manager. Escalation strictly requires non-empty note (button disabled when note is empty). `TicketID` is UNIQUE in escalations table (cannot escalate twice).
- **FR-25 (P1):** Severity Decision & Relocation (FM): FM views Escalation Inbox with full thread. Marking severe triggers confirmation modal explaining consequences. On confirm severe: Unit moves to `MAINTENANCE` via `UnitService` public event method; Relocation swaps Unit on active Rental while preserving `RT-` rental code, `CT-` contract, and deposit; issues new Access Code (revealed via `SensitiveValue`); creates maintenance and cleaning tasks via `TaskService` registry; logs `RELOCATION` in Activity Log; notifies customer at each step. Non-severe returns ticket to staff with FM instructions. After severity decision, ticket transitions to `IN_PROGRESS` and resolves only when staff completes remaining actions with note. Resolved ticket displays entire chronological arc in a drawer.
- **FR-35 (P1):** Customer Support List: List of customer tickets with `SR-` code, incident type, unit, status badge, created timestamp. Click opens 420px detail drawer showing complete chronological event thread. Empty state has single CTA to "New Support".
- **NFR-7:** Plain-word microcopy without system jargon for customer notifications and resolution notes.

## Technical Decisions

- **Architecture & Ownership (AD-4, AD-6):** `TicketService` is the sole owner of ticket lifecycle, routing, and escalation state machine. Inter-service actions call public domain methods (`UnitService.markMaintenance()`, `TaskService.createTask()`, `NotificationService.sendNotification()`, `LogService.logActivity()`).
- **Shift Routing:** Look up `staff_assignments` matching Unit's Zone and current shift slot for the current date in ICT (`Asia/Ho_Chi_Minh`).
- **Data Models:**
  - `TICKETS` table: `TicketID`, `TicketCode` (`SR-xxxx`), `RentalID`, `UnitID`, `CustomerID`, `AssignedStaffID`, `IncidentType`, `Status` (`OPEN`, `IN_PROGRESS`, `ESCALATED`, `RESOLVED`, `CLOSED`), `Description`, `StaffNote`, `CreatedAt`, `UpdatedAt`.
  - `ESCALATIONS` table: `EscalationID`, `TicketID` (UNIQUE), `EscalatedByStaffID`, `EscalationNote`, `SeverityDecision` (`PENDING`, `SEVERE`, `NOT_SEVERE`), `ManagerDecisionNote`, `CreatedAt`, `ResolvedAt`.
  - `TASKS`: `SUPPORT` task type linked to `TicketID`.
- **API Envelope & Contract First (AD-2, AD-8):** `contracts/openapi.yaml` defines all ticket and escalation endpoints.

## UX & Interaction Patterns

- **Customer New Support:** Form accessible from Rental Detail and Support List. Selects active unit, incident type dropdown, description textarea with blur validation.
- **Support Task Card & Staff View:** Red left status bar for `SUPPORT` card on Kanban. Details include full context, resolve modal/form with required note, escalate action with mandatory note.
- **FM Escalation Inbox & Severity Modal:** Destructive confirmation modal for severe decision explaining unit maintenance and relocation consequences.
- **Customer Support List & Drawer:** 420px slide-over drawer showing vertical timeline/thread of all ticket interactions.

## Cross-Story Dependencies

- **Epic 3:** Depends on `TaskService` registry and Kanban board (Story 3.2).
- **Story 5.1:** Foundation for ticket creation and shift-based routing.
- **Story 5.2:** Builds upon 5.1 for staff resolution and FM escalation.
- **Story 5.3:** Builds upon 5.2 for FM severity decision, unit maintenance, and relocation.
- **Story 5.4:** Builds upon 5.1-5.3 for customer self-service list and detail drawer view.
