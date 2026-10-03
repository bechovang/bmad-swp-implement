---
title: 'Story 5.4: Support List & Detail Drawer (Khách)'
type: 'feature'
created: '2026-10-03'
status: 'done'
baseline_commit: '9e36a79d5f64ba63bec6c9a0c4659d1abafdc5ff'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'contracts/openapi.yaml'
  - 'contracts/routes.yaml'
  - '_bmad-output/planning-artifacts/epics.md'
  - '_bmad-output/implementation-artifacts/epic-5-context.md'
  - '_bmad-output/implementation-artifacts/spec-5-3-severity-decision-relocation.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Customers need to track their submitted support tickets, understand the progress of on-site repairs or facility investigations, and review the entire chronological lifecycle arc when a ticket is resolved. Currently, the Support Page lists basic cards without a detail drawer, lacking visibility into staff triage notes, manager severity evaluations, emergency relocations, and final plain-word resolution summaries.

**Approach:**
1. **Customer Support List View (`SupportPage.tsx`):**
   - Displays all customer tickets with `SR-` mono badge, incident type, unit mono code, color-coded status badge (`OPEN`, `IN_PROGRESS`, `ESCALATED`, `RESOLVED`), and created timestamp.
   - Pointer surface on cards: clicking any card opens the 420px slide-over detail drawer.
   - Filter tabs: `ALL`, `OPEN`, `IN_PROGRESS`, `RESOLVED`, `ESCALATED`.
   - Empty state: factual message with a single primary CTA button: "New Support Ticket" (or "Open First Ticket").
2. **Support Detail Drawer (`SupportDetailDrawer.tsx`):**
   - 420px slide-over panel on the right side with backdrop scrim and Escape/close button (UX-DR5).
   - Visual chronological timeline rendering the entire resolution arc:
     - **Stage 1: Ticket Reported:** Verbatim customer description, incident category, timestamp.
     - **Stage 2: Staff Assigned & Triage:** Assigned staff name and work schedule.
     - **Stage 3: Escalation (if applicable):** Staff escalation note explaining why manager intervention was required.
     - **Stage 4: Manager Severity Decision (if applicable):**
       - If Severe Relocation: Plain-words notice indicating unit relocation (e.g. "Severe damage confirmed — Relocated from Unit M-2 to Unit M-5. New access PIN issued.").
       - If Not Severe: Notice indicating return to staff with management instructions.
     - **Stage 5: Final Resolution (when `RESOLVED`):** Plain-words customer resolution note (NFR-7), completion timestamp, and resolving staff member.
3. **Backend DTO & Mapping Enhancement:**
   - Enrich `SupportTicketDto` in both backend and frontend with escalation context (`managerDecision`, `managerNote`, `relocatedToUnitId`, `relocatedToUnitCode`).
   - Ensure `TicketService.mapToDto` fetches and populates escalation data if present.
4. **Automated Verification:**
   - Unit tests covering customer support list rendering, drawer opening/closing, chronological arc stages, empty state CTA, and API responses.
   - Clean `mvn test` (backend) and `npx vitest run` / `npm run build` (frontend).

</frozen-after-approval>

## Code Map

- `backend/src/main/java/com/storagehub/dto/SupportTicketDto.java`: Add `managerDecision`, `managerNote`, `relocatedToUnitId`, `relocatedToUnitCode`.
- `backend/src/main/java/com/storagehub/service/TicketService.java`: Update `mapToDto(SupportTicket ticket)` to populate escalation decision and relocation details.
- `contracts/openapi.yaml`: Update `SupportTicketDto` schema with optional escalation decision fields.
- `frontend/src/types/support.ts`: Update `SupportTicketDto` interface with `managerDecision`, `managerNote`, `relocatedToUnitId`, `relocatedToUnitCode`.
- `frontend/src/components/support/SupportDetailDrawer.tsx`: Create 420px slide-over drawer with chronological timeline and plain-word status explanations.
- `frontend/src/pages/support/SupportPage.tsx`: Integrate `SupportDetailDrawer`, card click handler, and empty state.
- `frontend/src/mocks/handlers.ts`: Update mock support tickets and `/api/v1/support-tickets` handlers to return enriched escalation data.
- `frontend/src/test/customer-support-drawer.test.tsx`: Comprehensive tests for customer support list and detail drawer.

## Implementation Plan

1. **Backend & Contracts:**
   - Update `SupportTicketDto.java` and `contracts/openapi.yaml`.
   - Update `TicketService.mapToDto` to retrieve `Escalation` and map decision, note, and relocation unit.
   - Run `mvn test` to verify backend integrity.
2. **Frontend Types & Mock Handlers:**
   - Update `frontend/src/types/support.ts` and `frontend/src/mocks/handlers.ts`.
3. **Frontend Components & Page:**
   - Implement `SupportDetailDrawer.tsx` with chronological timeline styling.
   - Connect drawer in `SupportPage.tsx`.
4. **Testing & Build:**
   - Create and run `customer-support-drawer.test.tsx`.
   - Run `npx vitest run` and `npm run build`.

## Acceptance Criteria

- **AC-1:** Customer views Support List with `SR-` code, incident type, unit code, status badge, and created date.
- **AC-2:** Clicking any ticket card opens the 420px right slide-over `SupportDetailDrawer` with scrim backdrop.
- **AC-3:** `SupportDetailDrawer` displays the full chronological event thread (Reported -> Assigned -> Escalated -> Severity Decision -> Resolved).
- **AC-4:** Resolved tickets display the complete resolution note in plain, customer-friendly language without technical jargon.
- **AC-5:** If no tickets exist, the empty state displays a single CTA to create a new support ticket.
- **AC-6:** Drawer closes on close button click, backdrop click, or Escape key press.
- **AC-7:** 100% automated tests pass in backend (`mvn test`) and frontend (`npx vitest run`), and `npm run build` succeeds cleanly.
