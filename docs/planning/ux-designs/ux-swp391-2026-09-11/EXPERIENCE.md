---
name: StorageHub
description: Experience spine for StorageHub — one adaptive web shell serving Customer, Staff, Facility Manager, Business Ops, and System Administrator roles across booking, check-in, checkout, extension, operations, policy, support, and administration.
status: final
updated: 2026-09-29
---

# StorageHub — Experience Spine

## Foundation

StorageHub is a web-responsive application — one app, one URL, one adaptive shell for all five roles: Customer, Staff (warehouse), Facility Manager, Business Ops, and System Administrator. There is no separate customer site and no separate admin console; after login the same top bar, the same `{rounded.md}` card surfaces, and the same interaction grammar serve everyone, with the nav menu and visible surfaces swapped by role. The role chip in the top bar (per `DESIGN.md` — `{colors.primary-tint}` pill naming the current role) makes the adaptive shell legible: you always know which hat you're wearing.

The UI system is a **custom token system defined in `DESIGN.md`** (the "Control Room" identity — Stripe/Linear register, 8px grid, tabular numerals, indigo action color, 3px left status bars). `DESIGN.md` is the visual identity reference and owns every color, type, radius, and elevation decision; this spine owns behavior and never restates visual values. Where a component is named here, its visual spec lives in `DESIGN.md` → Components.

Non-negotiables inherited from planning:

- **Light mode only.** There is no theme switch to design for.
- **English UI, VND money.** All microcopy in English; every amount full-precision VND (`1.150.000 ₫`) with tabular numerals per `DESIGN.md` → Typography.
- **Toast + notification bell.** Immediate results toast; durable events land in the Notification Center. No email, ever.
- **Real payment via PayOS + desk cash.** A modal at four touchpoints: 10% deposit at booking (QR only — no cash option in online booking), 100% rent at check-in, extension fee, and the extra fee when settlement charges exceed the deposit (QR or cash). QR = a real PayOS payment link rendered in the modal with live status polling; cash = collected at the desk and confirmed by staff ("Cash received"). Success/fail is explicit; fail always retries. *(Revised 2026-09-29 — replaces the mock Card/MoMo/VNPay gateway.)*
- **Paper contracts at the desk.** Every contract is auto-drafted from the booking and the active policy version — nobody composes one by hand. At check-in the staff prints it, the customer signs, and the staff photographs the signed copy into the rental. Extensions repeat the ritual as a paper addendum. No rental runs without a signed contract on file.
- **Kanban is the staff surface.** To do / In progress / Done for Check-in, Checkout, Cleaning, and Support work.
- **Back-office is dashboard-first.** KPI cards + charts first, data tables after.
- **Auth is full but split:** customers self-register; staff, manager, and administrator accounts are provisioned by the System Administrator (User Management, SYS-01). Forgot password is a modal on Login, not a screen.

The top bar carries, left to right: logo mark + wordmark, role menu (links per role), role chip, notification bell with unread count, avatar menu (profile, logout). The shell is identical across roles — only the menu's center links and the surfaces they open change.

## Information Architecture

29 screens — five role groups (System Admin group added 2026-09-21 per PRD §4.15). Screens reachable only as overlays (Payment Modal, forgot-password modal, the two drawers) are components, not screens, and are listed where they belong.

→ Composition reference: `.working/directions-1.html` (D4 "Control Room" hero + empty state, the approved visual direction). Spine wins on conflict.

### Auth (2)

| Screen | Reached from | Purpose |
|---|---|---|
| Login | App entry, logged out | Email + password. Forgot password opens **in a modal from this screen**: email field → confirmation ("If an account exists for that email, a reset link has been sent."). Staff/manager/administrator accounts are provisioned by the System Administrator (User Management) — self-registration is customer-only, stated plainly on the screen. |
| Register | Login link | Customer self-signup: full name, phone, email, password + confirmation, agree-to-terms. Success lands on Browse Units, logged in. Note: Field list beyond name/phone/email/password. |

### Customer (12)

| Screen | Reached from | Purpose |
|---|---|---|
| Browse Units | Nav (default landing) | Flagship: filter bar (type / size / start date / duration) + unit card grid (photo, size, price, status). No calendar availability view, no floor map. |
| Unit Detail | Browse card | Photos, spec rows (dimensions, floor, access, security), price breakdown (rent × duration, 10% deposit), Reserve CTA. |
| Booking Summary | Unit Detail | Review before paying: unit, dates, line-item totals, deposit due now; a note states the contract is drafted from these exact terms and signed at check-in. Opens Payment Modal. |
| Payment Modal | Booking Summary / Extend / Check-in Task / Checkout Task | PayOS QR rendered in the modal (live status polling) or cash confirmed by staff at the desk; success/fail states; retry. |
| My Rentals | Nav | Active and past rentals as cards with status badges and next-action buttons. |
| Rental Detail | My Rentals | One rental: unit, dates, payment history, deposit status, contract chain (original + addenda, signed copies viewable); actions — Check-in, Extend, Checkout Request, New Support. |
| Check-in | Rental Detail | Customer-side arrival pass: reservation code (mono, large), desk instructions, what to bring — ID for signing the contract, payment for 100% of rent. |
| Extend | Rental Detail | Pick new end date → conflict check vs next reservation → extension fee → Payment Modal → addendum auto-drafted, signed on paper at the desk (due within 7 days). |
| Checkout Request | Rental Detail | Request a move-out date; explains the deposit settlement logic (refund / deduction / extra). |
| New Support | Rental Detail / Support List | Pick unit + incident type + description → submit Support Request. |
| Support List | Nav | Request history; **detail drawer** shows thread, routing, resolution. |
| Notification Center | Bell (all roles) | Durable event feed. |

Customer screen notes:

- **Booking Summary** carries the full arithmetic the customer will be held to: rent × duration, each surcharge line from active policy, total, and the 10% deposit as its own line marked refundable. Nothing appears at payment that didn't appear here. Note: Surcharge lines shown pre-payment are confirmed at planning.
- **Rental Detail** is the customer's home base after booking — payment receipts (deposit, rent, extension), deposit status (held / settled), and every downstream action hang off it, so no flow ever starts from a system menu.
- **Contracts** live on Rental Detail as a chain: the original contract plus one addendum per extension, each row carrying its mono code (`CT-1042`, `CT-1042-A1`), status badge, and signed photo. An unsigned addendum shows a warning banner naming the desk deadline; the signed copy is the record both sides see.
- **Check-in (customer view)** shows the reservation code big and mono (`{typography.code}` per `DESIGN.md`), plus a plain-English sequence of what happens at the desk, including that 100% of rent is collected there.
- **Notification Center**: unread first, grouped by day, each entry with deep link to the affected rental/ticket; mark-all-read action. Note: Grouping and ordering are confirmed at planning.

### Staff (4)

| Screen | Reached from | Purpose |
|---|---|---|
| Task Board (Kanban) | Nav (default landing) | To do / In progress / Done columns; cards for Check-in, Checkout, Cleaning, Support, and Contract-signature tasks for the shift. |
| Check-in Task | Board card | Verify reservation code → system validates reservation + deposit paid → collect 100% rent (Payment Modal) → print the auto-drafted contract, customer signs, photograph the signed copy → hand over key/access code → rental activates. |
| Checkout Task | Board card | Receive unit + key, inspect condition, record damage fee + reason if any, settle deposit (refund / deduct / extra charge), close rental → unit becomes Preparing. |
| Support Ticket | Board card | Incident detail; resolve with note, or escalate to Facility Manager. |

Staff screen notes:

- **Task Board** can be filtered by task type via tabs above the columns; counts per column sit in the column header. Note: Type tabs + header counts are confirmed at planning.
- **Contract-signature cards** appear whenever an extension addendum awaits its paper signature: customer, unit, addendum code, due date. The card opens the same contract step as Check-in (preview → print → capture → attach) and completes when the signed photo lands.
- **Check-in Task** fields: reservation code entry/search, validation result (valid + deposit confirmed / rejected with reason), rent due with breakdown, Payment Modal trigger, contract step (read-only preview auto-drafted from the booking + active policy version, Print action, signed-copy photo capture, attach confirmation), access-code handover confirmation. Note: Field arrangement.
- **Checkout Task** fields: unit + key received checklist, condition inspection per item (walls, door, floor, cleanliness), damage fee amount + **required reason** when damage is found, settlement preview (deposit − fee = refund, or extra charge if fee exceeds deposit), close-rental confirm. Note: Inspection checklist items.
- **Support Ticket** fields: incident type + description (customer's words), unit + customer context, staff note field, actions Resolve / Escalate (escalate requires a note).

### Facility Manager (6)

| Screen | Reached from | Purpose |
|---|---|---|
| Facility Overview | Nav (default landing) | Dashboard-first: occupancy, revenue mix, unit-status KPIs + charts. |
| Unit Management | Nav | Unit table; **edit drawer**: edit, retire, set maintenance, fix wrong status + reason. Guards check Rental + Reservation + history. (Merge units → P2, không build v1 — 2026-09-22.) |
| Staff & Shifts | Nav | Staff list, zone/shift assignment with conflict detection. |
| Operations Monitor | Nav | Live view: today's tasks, escalations, turnover-buffer queue. |
| Activity Log | Nav | Append-only audit trail: status changes with reasons, merges, assignments. |
| Escalation Inbox | Nav | Escalated tickets; severity decision; trigger maintenance + customer relocation. |

Facility Manager screen notes:

- **Unit Management** table columns: code (mono), size, type, zone/floor, current status badge, active rental/reservation link, last activity; row click opens the edit drawer. Note: Column set.
- **Unit edit drawer** actions: edit specs, retire (guard + confirm), set Maintenance, fix status (requires reason). Every write lands in Activity Log. (Merge with adjacent unit → P2, không build v1 — 2026-09-22; demo beat dùng Retire S-2.)
- **Staff & Shifts**: staff roster; assignment form = staff × zone × shift × date; conflict check on submit; schedule grid showing current week. Note: Grid scope (weekly) is confirmed at planning.
- **Activity Log** rows: timestamp, actor, entity, action, from → to, reason (where required). Filter by entity type; no editing — append-only.
- **Escalation Inbox**: ticket list with severity state; open ticket shows the full thread and the severity decision panel (mark severe → maintenance + relocation; or return to staff with guidance).

### Business Ops (3)

| Screen | Reached from | Purpose |
|---|---|---|
| Business Overview | Nav (default landing) | KPI cards + charts: revenue, deposits held, surcharges, utilization per period. |
| Policy Management | Nav | Rent + surcharge configuration per unit type; validate before save. |
| Reports | Nav | Period reports with CSV export. |

Business Ops screen notes:

- **Policy Management**: editable table of rules (base rent per unit type/size, surcharge types with caps — e.g. late-checkout fee cap, damage fee handling); inline edit; Save runs full validation before persisting; a version/effective-date stamp on save. Note: Rule taxonomy beyond rent + surcharges.
- **Reports**: tabs for Revenue / Deposits / Surcharges / Occupancy over a period; period presets (this month, last month, quarter) + custom range; Export CSV button per tab. Note: Tab set + presets.

### System Admin (2) — added 2026-09-21 per PRD §4.15

| Screen | Reached from | Purpose |
|---|---|---|
| User Management | Nav (default landing) | User table (name, email, phone, role, status, last login); search + filter by role/status; create an account for **any** role (temp password shown once); change role; activate / deactivate / lock; reset password. No delete — status changes only. Every write lands in Activity Log. |
| Login & Activity History | Nav | LOGIN / LOGIN_FAILED events from the append-only Activity Log; filter by user, time, and result; read-only. |

System Admin screen notes:

- **User Management** table columns: full name, email (mono), phone, role chip, status badge (Active / Inactive / Locked), last login; pagination 25 rows like every admin table. Create opens a drawer: name, email, phone, role select, temp password (generated, copy-once). Guards: duplicate email blocked inline; locking your own account is blocked.
- **Login & Activity History** reuses the Activity Log table grammar (timestamp, actor, action, result) scoped to LOGIN / LOGIN_FAILED; a filter chip names the user under review. It is a pointer surface — nothing is recomputed here.
- A **read-only permission matrix** (role × menu/action/scope) may live as a section on User Management (P2, FR-39); it displays what the server enforces and is never editable at runtime.

### Per-role nav menus

Same shell, same bell, same avatar menu — only the center links change:

- **Customer:** Browse Units · My Rentals · Support
- **Staff:** Tasks · Support — Note: Staff nav carries no Units link after the Units screen was cut; unit context lives on the kanban task cards.
- **Facility Manager:** Overview · Units · Staff & Shifts · Operations · Activity Log · Escalations
- **Business Ops:** Overview · Policy · Reports
- **System Administrator:** Users · Login History

Overlay discipline: modal stacks stay one level deep (a drawer can open from a table; a modal never opens from a modal; drawers never stack on modals). Deep paths (Extend, Checkout Request, New Support) hang off Rental Detail so the rental — never the system — is the customer's anchor.

## Voice and Tone

Microcopy. Brand posture lives in `DESIGN.md` → Brand & Style.

Voice: **precise, competent, low-ceremony SaaS English**. StorageHub states facts, names amounts, and explains blocks in one breath. Money always appears in full VND with tabular numerals (`103.500 ₫`, never "~100k"). Errors always say what happened, what it cost (usually nothing), and what to do next. No exclamation marks, no streak-cheer, no marketing verbs — for any role. Managers get the same plain register as customers; seniority is not a tone of voice.

| Do | Don't |
|---|---|
| "Unit S-3 reserved. Deposit 103.500 ₫ received." | "🎉 Your booking is successful!" |
| "Payment failed. No money was taken. Retry or choose another method." | "Transaction error (code 05)." |
| "Oct 20 is taken by the next reservation. Latest new checkout: Oct 18." | "This date is unavailable." |
| "6 units available · live" | "Plenty of great options!" |
| "Checkout closed. Refund 132.500 ₫ to Lan after damage fee 40.000 ₫." | "Process complete." |
| "Escalated to Facility Manager — flooding, Unit M-2." | "Your issue has been forwarded to the relevant team." |
| "Reason required to change status." | "Error: validation failed." |

Sample strings for key moments:

- **Booking summary (review):** "Unit S-3 · 5 m² · Indoor, Tân Bình Depot. Oct 3 → Jan 3 (3 months). Rent 345.000 ₫/mo × 3 = 1.035.000 ₫. Deposit (10%, refundable) due now: 103.500 ₫."
- **Payment success (deposit):** "Deposit paid — 103.500 ₫. Unit S-3 is reserved for you until check-in."
- **Payment success (rent at check-in):** "Rent paid — 1.035.000 ₫. Rental active. Access code sent to your notifications."
- **Payment success (extension):** "Extension paid — 690.000 ₫ rent + 69.000 ₫ deposit top-up (held deposit now 172.500 ₫). New checkout date: Oct 18. Sign addendum CT-1042-A1 at the desk by Oct 25."
- **Contract drafted (after deposit):** "Contract CT-1042 drafted from your booking and Rental Policy v3. You'll sign it at check-in."
- **Contract signed (check-in):** "Contract CT-1042 signed and filed. Access code sent to your notifications."
- **Addendum reminder (unsigned):** "Addendum CT-1042-A1 is still unsigned. Sign at the desk — the new checkout date Oct 18 already applies."
- **Payment fail (any touchpoint):** "Payment failed. No money was taken. Retry the QR, or switch to cash at the desk."
- **Extension blocked by conflict:** "Can't extend to Oct 20 — M-2 has a reservation starting Oct 19. Latest possible checkout is Oct 18. Pick another date."
- **Shift conflict blocked:** "Minh is already assigned Morning shift, Zone B on Oct 12. Choose another staff member or another shift."
- **Policy validation fail:** "Policy not saved. Late-checkout fee 15% exceeds the 10% cap in Rental Policy v3. Fix the value, or edit the cap first."
- **Status fix (Activity Log entry):** "Tuấn changed M-2 from Rented to Preparing — 'Customer moved out Oct 1; status not updated at desk.'"
- **Retire confirm (thay Merge confirm — P2 2026-09-22):** "Retire S-2? No active rentals or reservations on this unit. This cannot be undone."
- **Escalation (staff → manager):** "Escalated to Facility Manager — flooding, Unit M-2. Customer relocation needed."
- **Manager severity decision:** "Mark severe — move M-2 to Maintenance and relocate the customer to M-5. This closes the rental and starts turnover."
- **Support resolved (customer-facing):** "Resolved — door hinge replaced by site staff. See ticket for details."

## Component Patterns

Behavioral. Visual specs live in `DESIGN.md` → Components.

| Component | Use | Behavioral rules |
|---|---|---|
| Filter bar | Browse, tables, Reports | Closed dropdowns (type / size / start date / duration on Browse) + Search button. Changing filters re-queries on Search, not per keystroke; the results-count chip ("6 units available · live") refreshes with each fetch. Active filters persist for the session and are echoed as removable chips in the empty state. |
| Unit card | Browse grid | Photo + mono code chip, size/type chips, feature line, price, availability line with status dot, Book. The 3px left bar encodes status (available green / buffer amber). Buffer cards say "Available Oct 5 · cleaning buffer" and keep a secondary Book. Click anywhere except Book → Unit Detail. Cards re-query against the rental calendar + turnover buffer, so the displayed start dates are the promise. |
| Payment modal | 4 touchpoints | See below. |
| Contract step | Check-in Task, Contract-signature card | Read-only preview of the auto-drafted contract — mono code, parties, unit, dates, rent/deposit lines, and the policy version it locks — followed by Print, a signed-copy photo capture tile, and Attach. Nothing in a contract is editable anywhere in the UI; the flow's completing action (access-code handover, addendum completion) stays disabled until a signed photo is attached. |
| Kanban board | Staff Task Board | Columns To do / In progress / Done; cards typed Check-in / Checkout / Cleaning / Support / Contract with type-colored left bar. Drag to move; keyboard alternative via per-card Move buttons. Click card → task screen. |
| KPI cards | All dashboards | Row of 3–4 label + tabular value + delta chip. Click-through drills into the backing table filtered to the same slice (e.g. occupancy 87% → Unit Management filtered to Rented). |
| Status badges | Tables, cards, drawers | Tint + border + label from the `DESIGN.md` status ramp (`{colors.status-*}`). Never color-only. |
| Toast + bell | Global | Toast for immediate results (auto-dismiss ~4s, hover pauses, one action link max). Bell badge counts unread; Notification Center is the durable feed with deep links. Money events emit both a toast and a bell entry. |
| Drawers | Support detail, Unit edit | Right-side overlay (`{components.drawer}` width). Read + light edit; destructive or guard-checked edits promote to confirm modal. Background list stays visible; drawer closes on Esc/scrim. |
| Confirm modals | Destructive actions | Retire unit, merge units, close rental with deduction, mark severity. States the consequence in money/status terms; requires a typed reason where the flow demands one. |
| Empty states | Anywhere | Icon tile, factual title, one-sentence explanation restating the constraint, mono chips echoing the query/filters, single CTA. Always offers the next move. |
| Skeletons | Anywhere loading | Layout-matching placeholders (card-grid skeletons on Browse, row skeletons on tables); results swap in without layout shift. Note: Skeleton pattern is confirmed at planning. |

**Unit card & Browse grid.** The card is a promise about dates: every availability line is computed against the rental calendar plus the turnover buffer at query time, so "Available Oct 3" is bookable for Oct 3 and "Available Oct 5 · cleaning buffer" is bookable from Oct 5 — with the Book button taking the customer straight into Booking Summary for those exact dates. Sort defaults to price low→high; the count row ("6 units available · live") refreshes on every re-query. The grid re-checks availability again on Reserve; if a unit was taken in the meantime, the customer bounces back to the grid with a toast ("S-3 was just reserved. 5 similar units still available.") — never a stale booking.

**Dashboards (KPI drill-down).** All three manager-facing dashboards (Facility Overview, Business Overview, Operations Monitor) follow the same contract: a KPI row of 3–4 cards, a chart block, then the backing table — in that order. Every KPI is clickable and drills into its table pre-filtered to the same slice (occupancy 87% → Unit Management filtered to Rented; surcharges this quarter → Reports on the Surcharges tab for the same period). The dashboard never shows a number the user cannot reach the rows behind.

**Payment modal (PayOS QR + desk cash).** Shared by all four touchpoints — 10% deposit (Booking Summary, QR only), 100% rent (Check-in Task), extension fee (Extend), extra fee (Checkout settlement). Anatomy: header names what is being paid and the amount in `{typography.price-lg}`; method segmented control: **PayOS QR** (backend creates a real payment link; the modal renders the QR and polls status until the PayOS webhook confirms — a countdown from the link's expiry, after which the modal returns to method selection with nothing charged) and **Cash at desk** (staff-present touchpoints only: rent at check-in, extension at the contract-signature desk, extra fee at settlement — selecting cash parks the payment as pending until staff presses "Cash received"; no booking/extension state moves until then). Pay/confirm → awaiting-confirmation state (polling spinner, no navigation away, explicit Cancel cancels the link and returns to method select) → success (check tile, amount, consequence line) or fail (error tile, "No money was taken", Retry + Switch method). Booking Confirmation is not a separate screen — the success state of this modal ends flow F1, and its "View rental" action deep-links to Rental Detail. *(Revised 2026-09-29.)*

**Kanban card.** Unit code (mono), task type chip, customer name / time slot, due chip. Drag between columns updates task state; moving a Check-in/Checkout card into Done is blocked until the task screen's closing steps (payment / settlement) are complete — the card snaps back with a toast naming the missing step. Support cards carry the Escalate action. Done cards render muted. Note: The snap-back-on-incomplete rule is confirmed at planning.

**Notification Center.** Role-scoped: customers see reservation/payment/support/contract events; staff see task assignments; managers see escalations and status writes; business ops sees policy saves and report completions. Entries deep-link to the affected screen; unread persist across sessions.

## State Patterns

**Unit status lifecycle.** `Available → Reserved → Rented → Preparing → (Maintenance | Retired) → Available`. UI meaning of each state:

| State | What the UI says |
|---|---|
| Available | Green dot/badge; bookable in Browse with start-date math. |
| Available soon (buffer) | Amber in Browse when a turnover buffer delays the start ("Available Oct 5 · cleaning buffer"); bookable with the later date. |
| Reserved | Indigo badge in management views once the deposit is paid; customer sees it as an active reservation on My Rentals with check-in countdown. |
| Rented | Slate badge; active rental exists; actions route to Rental Detail / tasks; excluded from Browse results. |
| Preparing | Sky badge after checkout; cleaning task auto-created on the kanban; not bookable until cleaning completes, then → Available (or Reserved if a next reservation exists). |
| Maintenance | Orange badge; excluded from Browse; visible in Operations Monitor; entered by manager action or severe escalation. |
| Retired | Grey badge; out of inventory; appears only in Unit Management + Activity Log. |

**Contract lifecycle.** `Draft → Printed → Signed → Active → Closed`, with `Superseded` marking a contract whose end date an addendum replaced. Contracts are auto-drafted the moment a deposit succeeds; every later money event appends to the same record.

| State | What the UI says |
|---|---|
| Draft | "Contract CT-1042 drafted from Rental Policy v3." Auto-filled from the booking; visible read-only in the Check-in Task; never editable by anyone. |
| Printed | "Awaiting customer signature" — printed at the desk mid-check-in; blocks access-code handover until signed. |
| Signed | Signed photo attached; Activity Log writes `CONTRACT_SIGNED` with actor + timestamp; Rental Detail shows the copy. |
| Active | The rental runs on this contract; checkout settlement references its number. |
| Closed | Settlement receipt names the contract; the signed copy stays viewable forever. |
| Superseded | End date replaced by addendum `CT-1042-A1`; the chain stays readable on Rental Detail. |

**Extension addendum semantics.** Paying the extension fee moves the new checkout date immediately (the unit is held — the customer paid) and auto-drafts the addendum as `Awaiting signature`: a warning banner on Rental Detail names the desk deadline (7 days), a Contract card appears on the staff board, and both sides get bell reminders. Signing at the desk (print → sign → photograph) completes it. The addendum is provisional paperwork riding on an already-effective date — never a hold on the unit.

**Payment states.** The mock gateway's states are specified once and shared by all three touchpoints:

| State | Treatment |
|---|---|
| Method select | PayOS QR panel — amount + payee visible, Pay button with the exact amount on it; "Cash at desk" option on staff-present touchpoints only (never on Booking Summary). |
| Awaiting confirmation | QR: modal polls payment status until the PayOS webhook confirms; Cash: payment parked pending until staff presses "Cash received". Button disabled; no back navigation mid-charge; explicit Cancel cancels the payment link and returns to method select. |
| Success | Check tile, amount, consequence line ("Unit S-3 is reserved" / "Rental active" / "New checkout: Oct 18") → toast with amount → underlying screen state flips (card bar to Reserved, rental to active, date moves, receipt appended to Rental Detail). Receipt names the method (QR / Cash). |
| Fail | Error tile, "No money was taken", Retry + Switch method; no state change anywhere; after two fails, switching method is suggested explicitly. |
| Link expiry | Countdown ends → returns to method select with a note; the link is cancelled; nothing charged. |

Never a dead end, never an ambiguous spinner.

**Form validation.** Inline field errors on blur (`{colors.error}` border + message per `DESIGN.md` → Inputs). Server-side blocks (policy validation, conflict checks) surface as a `{colors.error-tint}` banner at the top of the form restating the rule that failed — not a toast — because the user must read the rule to fix the value. Submit stays disabled only for missing required fields, never for soft warnings.

**Blocked-action scenarios** — each block names the exact rule and offers the legal alternative:

- **Policy validate fail:** save blocked, offending fields flagged, message names the violated cap/rule; after fixing, save succeeds and a success toast confirms version + effective date. An invalid policy never persists.
- **Shift-conflict rejection:** assignment blocked before save with the specific collision ("Minh is already on Morning, Zone B, Oct 12"); the conflicting slot is highlighted in the schedule view; user picks another staff member or slot.
- **Extension blocked by next reservation:** the date picker marks the conflicting range; on submit the block is restated with the exact boundary ("Latest new checkout: Oct 18"); user chooses a valid date and proceeds to payment.
- **Wrong-status fix requiring reason:** manager changes a unit's status from the edit drawer when it disagrees with reality; a reason textarea is required; the change lands in Activity Log with actor, timestamp, from → to, and reason. No silent status writes anywhere in the system.
- **Merge/retire guards:** merging or retiring a unit checks Rental + Reservation + history first; any blocker is named and linked (the live reservation), and the action stays unavailable until it clears.

**Empty / loading.** Loading = layout-matching skeletons (above). Empty states per the `DESIGN.md` component: factual ("0 of 42 units meet all four criteria"), filter chips echoed, one CTA (Clear filters / Widen dates). Zero-task kanban columns show a quiet "Nothing here" tile. First-run screens (no rentals, no tickets) link to the flow that fills them (Browse Units / New Support).

## Interaction Primitives

- **Click/tap** is the base verb everywhere; whole-row and whole-card click targets with trailing buttons excluded from the hit area.
- **Drag-drop on kanban** moves cards between To do / In progress / Done — with a keyboard alternative: focusing a card reveals ◀ / ▶ **Move buttons** (plus a status menu) that move the card one column at a time. Drag is never the only path.
- **Date-range pickers:** Browse start-date + duration (closed dropdowns, per the flagship decision); Extend uses a calendar picker that marks the conflict boundary; Reports uses period presets + custom range. Note: Extend/Reports picker shapes are confirmed at planning.
- **Inline edit:** policy values in Policy Management tables and staff assignment cells — click to edit, blur or Enter to save with validation; Esc cancels; unsaved-changes guard on navigation.
- **Confirm-destructive:** retire unit, merge units, close rental with deduction, severity escalation — modal, consequence stated in money/status terms, typed reason where required.
- **Banned:** hover-only affordances on touch sizes; multi-level modal stacks; infinite scroll on management tables (pagination, 25 rows). Note: Pagination size is confirmed at planning.

## Accessibility Floor

Modest floor, per planning — no WCAG AA mandate, light mode only:

- Every input has a visible label; no placeholder-only fields.
- Visible focus everywhere: the `{colors.primary}` 2px ring per `DESIGN.md` → Components.
- Full keyboard reachability: nav, tables, drawers, modals, and the kanban (Move buttons) operate without a pointer.
- Status is never communicated by color alone — always dot/badge + text label.
- Touch targets ≥ 40px on controls (buttons, inputs, kanban Move buttons).
- Focus is trapped inside open modals/drawers and returned to the trigger on close.

## Key Flows

### Flow 1 — Lan books, occupies, extends, and checks out (customer)

1. Lan logs in and lands on Browse Units; she sets Indoor / 5 m² / Oct 3 / 3 months and searches.
2. The grid returns 6 cards; she opens Unit Detail for S-3, sees the full breakdown (rent × 3 = 1.035.000 ₫, deposit 103.500 ₫ refundable).
3. Booking Summary restates every line → Payment Modal → she pays the 10% deposit by PayOS QR; success state confirms "Unit S-3 is reserved."
4. On move-in day she opens Rental Detail → Check-in, shows the reservation code at the desk; Minh validates it, she pays 100% of rent through the Payment Modal there, signs the printed contract CT-1042, Minh photographs the signed copy into the rental, and she receives her access code — the rental is active.
5. Mid-term she files a support request (New Support: sticky door) about the unit; it routes to the on-shift staff by unit + shift and resolves; she sees the resolution in the Support List drawer and a bell notification.
6. She tries to Extend to Oct 20 — **blocked**: the next reservation starts Oct 19; the screen shows the boundary and she picks Oct 18, pays the extension fee in the Payment Modal; the new checkout date lands on Rental Detail with a receipt, addendum CT-1042-A1 awaits her paper signature at the desk (due in 7 days), and she signs it on her next visit.
7. She submits a Checkout Request for her new end date.
8. At the desk, Minh inspects, finds minor damage, records a 40.000 ₫ fee with reason; the system settles: refund 132.500 ₫ to Lan.
9. **Climax:** Rental Detail shows the closed rental with a transparent settlement receipt — "Refund 132.500 ₫ after damage fee 40.000 ₫" — the deposit promise made in step 2 visibly kept, to the đồng.

Failure along the way: any payment fail leaves her on a retryable error, no state changed, nothing charged; a stale Browse result (unit taken meanwhile) re-queries on reserve and bounces her to the grid with a toast.

### Flow 2 — Minh runs the day on the kanban (staff)

1. Minh opens Tasks; the board holds his Check-in, Checkout, Cleaning, and Support cards for the shift.
2. He drags Lan's Check-in into In progress and opens it; the reservation code validates, deposit confirms.
3. He takes her 100% rent payment in the Payment Modal, prints contract CT-1042, photographs her signed copy, and hands over the access code; the card completes and the unit flips to Rented.
4. He works a Checkout task: receives unit + key, runs the condition checklist.
5. **Climax:** he finds wall damage — the moment the product exists for. The Checkout Task forces him to declare a damage fee (40.000 ₫) **with a required reason** before settlement can proceed; the system computes the deposit split and closes the rental with a receipt both sides can see. The unit moves to Preparing and a Cleaning card appears on his board.
6. A Support card (sticky door, another unit) he resolves in Support Ticket with a note; the customer is notified.
7. One flooding ticket he cannot fix — he escalates with a note (see Flow 5).

Failure: moving a card to Done with steps missing snaps it back with a toast naming the missing step.

### Flow 3 — Hằng ships a pricing policy (business ops)

1. Hằng opens Policy Management: rent and surcharge rules per unit type in an editable table.
2. She raises the late-checkout surcharge to 15% and saves.
3. **Climax:** validation fails — the save is blocked by an inline banner naming the rule ("15% exceeds the 10% cap in Rental Policy v3") with the offending field flagged; she fixes the value (or edits the cap), saves, and the success toast confirms the new version and effective date. The bad policy never existed.
4. She moves to Business Overview: KPI cards + charts for the quarter's revenue, deposits held, surcharges, utilization — the numbers reflecting the policies she just shipped.
5. She opens Reports, sets the period, and exports CSV for the capstone report.

Failure: a validation fail on any other rule cell behaves identically — block, name the rule, flag the field.

### Flow 4 — Tuấn restructures the floor (facility manager)

1. Tuấn opens Unit Management; the table shows every unit with live status badges.
2. He merges two adjacent small units into one 8 m² unit from the edit drawer; the guard confirms no active Rental or Reservation blocks it; the merge lands in Activity Log.
3. In Staff & Shifts he assigns Minh to Morning, Zone B — a conflict is rejected with the specific collision; he picks the afternoon slot instead and saves.
4. He notices a unit stuck showing Rented though the customer left; from the edit drawer he fixes the status to Preparing — **a reason is required** — and saves.
5. **Climax:** Activity Log shows the full audit line — actor, timestamp, Rented → Preparing, and his reason. The wrong data is corrected without the correction itself becoming invisible.

Failure: a merge attempted on a unit with an active reservation is blocked with the reservation named and linked.

### Flow 5 — The escalation (flood in Unit M-2)

1. A customer reports flooding in M-2 via New Support; the request routes to the on-shift staff by unit + shift.
2. Minh opens the Support Ticket, cannot resolve it, and escalates to the Facility Manager with a note.
3. The ticket lands in Tuấn's Escalation Inbox with full context (unit, customer, thread).
4. Tuấn marks it **severe**: M-2 moves to Maintenance (orange badge, out of Browse), and a relocation prepares another unit for the customer.
5. The customer's replacement rental activates; cleaning/repair tasks flow through the board; the customer is notified at each turn.
6. **Climax:** the ticket flips to Resolved with the whole arc visible in one drawer — incident, escalation, severity decision, maintenance, relocation — and the customer's Notification Center carries the same resolution in plain words.

Failure: if the manager disagrees with severity, the ticket returns to the staff queue with guidance — never dropped.

### Flow 6 — Nam provisions accounts and reviews login history (system admin — added 2026-09-21)

1. Nam logs in as System Administrator and lands on User Management.
2. He creates a Staff account for a new hire (name, email, phone, role, temp password) — the write lands in Activity Log.
3. He locks the account of a departed employee; the row's status badge flips to Locked, nothing is deleted.
4. A suspicion: he opens Login & Activity History, filters by user, and sees a chain of failed logins that morning with timestamps.
5. **Climax:** every provision, role change, and lock has an audit line — actor, target, from → to — and no user ever disappears from the system.
6. The new hire signs in with the temp password and lands on the Task Board.

Failure: creating an account with an existing email is blocked inline; a locked account gets the same generic sign-in error as any other failed login.

## Responsive & Platform

Desktop-first; the design target is **1200px** with the 3-column Browse grid and the full kanban.

| Breakpoint | Behavior |
|---|---|
| Desktop ≥ 1200px | Full layout: 3-col Browse grid, all kanban columns visible, tables full-width, drawers at `{components.drawer}` width. |
| Tablet 768–1199px | Browse drops to 2 columns; dashboards stack KPI row above charts above table; tables hide secondary columns; kanban shows ~1.5 columns. |
| Mobile < 768px | Single column; filter bar stacks and collapses behind a Filters button; **kanban columns scroll horizontally** (one column in view, sticky column headers); **tables become card lists** (row → card with label:value pairs); drawers go full-width. |

Payment modal, confirm modals, and drawers remain overlays at every size (full-screen sheet on mobile). The product is responsive web, not native; phones are supported for reading, simple actions, and payments — heavy management (policy tables, activity log) is desktop-grade.

## Anti-patterns

- **Hiding status.** No unit or task surface without its state legible at a glance — no hover-only status, no color-only badges, no status buried in a detail screen. The 3px bar and badge ramp exist precisely for this.
- **Surprising fees.** No money moves without a line-item breakdown before it — deposit at booking, 100% rent at check-in, extension fee, damage fee at checkout. Every fee carries a reason string where the flow requires one.
- **Dead-end errors.** Every fail state names what happened, what it cost (usually nothing), and the next move — retry, switch method, pick another date. Bare "error" toasts and browser-native error pages are banned.
- **Calendar/floor-map browsing.** Availability is communicated by the card grid + buffer dates, never by a calendar availability view or a floor map (planning decision for the flagship).
- **Email notifications.** Feedback is toast + bell only.
- **Silent writes.** Status changes without an actor/reason trail; policy saves without validation; shift assignments that ignore conflicts; merges that skip the guard.
- **Missing paper trail.** An access code handed over with no signed contract photo on file; an unsigned addendum past its due date with no reminder; a hand-composed contract. The signed photo is the record — auto-draft, print, sign, photograph, every time.
- **Role-blind chrome.** Same-looking screens for different roles without the role chip; customer marketing tone in staff surfaces, or system jargon in customer surfaces.
- **Visual noise.** Second accent colors, new hues per screen, uppercase micro-labels, shadows-as-hierarchy — see `DESIGN.md` → Do's and Don'ts. The register is the product.
