# StorageHub — UI/UX Blueprint v4 ("Control Room", current-state)

You are working on the existing Stitch project **`9794980915577530548` — "StorageHub SWP391 Control Room"**:

"StorageHub — Self-Service Storage Rental and Management System."

This blueprint **supersedes `uiux_blueprint (3).md`**. It describes the system **as it exists today** (audited 2026-09-17) and governs all future edits. Where this document and blueprint (3) conflict, this document wins. Where a Stitch screen and the spine documents conflict, the spine documents win (see Sources of truth).

## Sources of truth (in order)

1. `docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md` — visual identity contract ("Control Room"). Owns every color, type, radius, and elevation decision.
2. `docs/planning/ux-designs/ux-swp391-2026-09-11/EXPERIENCE.md` — behavior contract. Owns IA, flows, states, component behavior, microcopy.
3. `docs/planning/ux-designs/ux-swp391-2026-09-11/mockups/` — 27 frozen reference mockups (F1–F7) + `NOTES.md` index.
4. The Stitch project itself (HTML screens generated from the above).

## 0. What changed vs blueprint (3) — mandatory reading

Blueprint (3) was written against a hypothetical 21-screen, five-role, pre-branding project. The project has since been designed and frozen. The differences:

| # | Blueprint (3) said | Current state (this blueprint) |
|---|---|---|
| 1 | Exactly **21** screens | Exactly **27** screens (F1–F7), plus overlay components that are not screens |
| 2 | **Five** roles incl. SYSTEM_ADMINISTRATOR | **Four** UI roles: Customer, Staff, Facility Manager, Business Ops. The System Administrator role, A-01 User & Role Management, A-02 (admin) Activity Logs, and SYS-01 error-state screens were **cut**. Staff/manager accounts are provisioned outside the UI; public registration creates Customer accounts only |
| 3 | Two shells: Customer horizontal header + Back-office sidebar | **One adaptive shell** for all roles: same top bar (logo, role menu, role chip, notification bell, avatar menu), only the center links swap by role |
| 4 | "Keep the current neutral visual direction. Do not apply a new brand style yet. One neutral accent color" | The brand **is applied and frozen**: the "Control Room" identity — indigo `#4F46E5` primary, semantic status color ramp, 3px left status bars on cards, tabular numerals, 8px grid, light mode only, system font stack. No neutralization, no re-theming |
| 5 | Detail navigation via `DetailDrawer` everywhere; "do not create a new main Contract screen" | **Full-page detail screens** are the pattern for deep work: Unit Detail, Rental Detail, Check-in Task, Checkout Task, Support Ticket. Drawers remain only for Support List detail and Unit edit. Contracts still have **no standalone screen** — they live on Rental Detail as a chain (kept from (3)) |
| 6 | Screens grouped C/S/M/B/A + AUTH/SYS | Screens grouped by **flow family F1–F7** and by role (Auth 2, Customer 12, Staff 4, Facility Manager 6, Business Ops 3) |
| 7 | AUTH-01 = one screen with Login/Register/Forgot variants | **Login** (F1-01, forgot-password is a modal on it) and **Register** (F1-02) are two separate screens |
| 8 | No Notification screen — bell only | **Notification Center** (F7-05) is a real screen (bell dropdown), role-scoped, unread-first, deep links |
| 9 | C-04 "My Storage" with tabs (Reservations / Active Rentals / History) | **My Rentals** (F2-03) card list + **Rental Detail** (F2-04) as the customer's home base. No tabs page |
| 10 | M-01 "Unit Floor Plan and Unit Management" | Floor plans and calendar availability views are **banned anti-patterns**. F5-02 Unit Management is a table + edit drawer only |
| 11 | M-04 Facility Operations Report (single screen) | Split into **Facility Overview** (F5-01 dashboard) and **Operations Monitor** (F5-04 live view) |
| 12 | B-01 Facility Management (Business Ops) | Facility dashboards belong to the **Facility Manager** (F5-01). Business Ops keeps Business Overview (F4-01), Policy Management (F4-02), Reports (F4-03) |
| 13 | B-02: "Do not directly edit an active policy. Create a new draft version before activation" | Policy Management uses **inline edit + Save with full validation**; an invalid policy never persists; save stamps a version + effective date (e.g. "Rental Policy v3") |
| 14 | Contract = ContractCard with View/Print/Upload/Confirm actions | Full **contract domain** (added 2026-09-14): auto-draft from booking + active policy version; print → customer signs → staff photographs the signed copy; **access code is blocked until the signed copy is attached**; extensions produce a paper addendum (`CT-1042-A1`) signed at the desk within 7 days. New `ContractStatus` lifecycle (see §7) |
| 15 | Extension semantics unspecified | Paying the extension fee moves the new checkout date **immediately** (unit held — customer paid); the addendum is provisional paperwork, never a hold on the unit |
| 16 | Payment = PaymentMethodSelector + PaymentStatusPanel | One **mock Payment Modal** (F1-06) shared by exactly three touchpoints (10% deposit, 100% rent at check-in, extension fee), methods **Card / MoMo / VNPay QR**, states: method select → processing (~1.5–2 s) → success / fail (retry) / QR expiry (~5 min countdown) |
| 17 | S-03 "TaskBoard or shared DataTable" | **Kanban is definitive** (To do / In progress / Done), five card types (Check-in, Checkout, Cleaning, Support, Contract-signature), drag + keyboard Move buttons, snap-back-with-toast when closing steps are incomplete |
| 18 | MetricCard/ChartPanel, no drill-down rule | **Dashboard-first + KPI drill-down contract**: every KPI card clicks through to its backing table pre-filtered to the same slice. A dashboard never shows a number whose rows cannot be reached |
| 19 | Mock data: units `TD-A-101`…, records `BK-2026-0001`…, facilities FPT Thu Duc / FPT Binh Thanh, 5 demo users incl. admin | Mock data: units `S-1/S-2/S-3/M-2/M-5`, records `BK-1042`, `RT-0871`, `SR-0032`, `CT-1042`, `CT-1042-A1`, facility **Tân Bình Depot**, 4 demo users (Lan, Minh, Hằng, Tuấn). Full values in §13 |
| 20 | Frame 1440 × 1024, no responsive design | Design target **1200px** desktop-first; responsive collapse behavior is specified (tablet 768–1199, mobile <768) but **no separate mobile/tablet frames are designed** |
| 21 | Project assumed clean | Stitch currently holds **39 visible instances of the 27 screens** (12 stale duplicates produced by edit-history), 1 hidden legacy screen, the DESIGN.md upload, and the design-system asset. Consolidation to exactly 27 canonical instances is required (§21) |

Everything else from blueprint (3) that is not contradicted above remains in force — especially: English-only UI, the data-control rules (§5), the entity model (§6), the status dictionary idea (§7), the mutation pattern (§18), and the consistency rules (§19).

# 1. Language requirement

The entire project must use English only. This includes page titles, navigation, buttons, form labels, helper text, validation messages, status labels, empty states, alerts, confirmation dialogs, table headers, tooltips, notifications, and demo data descriptions. Do not leave any Vietnamese text in the UI.

Vietnamese **personal names** and **place names** may remain as proper nouns without accents when necessary, for example: `Lan`, `Minh`, `Hang`, `Tuan`, `Tân Bình Depot` (rendered `Tan Binh Depot` where accents are unavailable).

Money is always full-precision VND with dot thousands separators and the `₫` symbol: `1.150.000 ₫`. Never `~100k`, never USD.

# 2. Target platform

Desktop-first responsive web. One app, one URL, one adaptive shell.

* Design target width: **1200px** (Stitch frames are 1280-wide desktop).
* Browse grid: 3 columns at ≥1200px, 14px gutters, 24px outer margins.
* Fixed top navigation bar height: 54px.
* Filters shown directly on the page (closed dropdowns + Search button; re-query on Search, not per keystroke).
* Unit grids: 3–4 columns. Tables keep important columns; pagination at 25 rows on management tables (no infinite scroll).
* Overlays stay overlays at every size; modals never stack on modals; a drawer may open from a table but never from a modal; drawers never stack on modals.
* Responsive collapse (behavior only — do NOT design separate mobile/tablet frames): tablet drops Browse to 2 columns, dashboards stack KPI → charts → table, tables hide secondary columns, kanban shows ~1.5 columns; mobile goes single-column, filter bar collapses behind a Filters button, kanban columns scroll horizontally, tables become card lists, drawers go full-width.

# 3. Core architecture

Order of concern:

```
Data Model → Design Tokens (DESIGN.md) → Primitive patterns → Shared patterns → Domain components → 27 Screens
```

* The token layer already exists and is binding: the Stitch design system ("StorageHub Control Room") loaded from DESIGN.md. Do not invent new colors, fonts, radii, or shadows; reference the named tokens (`primary`, `status-*`, `ink`, `muted`, `border`, …).
* Screens must be compositions of the shared pattern library (§9–§11). When a pattern such as the unit card, status badge, payment modal, or KPI card changes, every screen using it must be updated to match — the mockups in `mockups/` are the frozen reference for what "matching" means.
* If code is generated: reusable React components, props, typed data models, one shared mock-data store. If design frames are generated: master components, variants, properties, instances. Never manually duplicate identical cards, rows, badges, forms, or navigation elements.

# 4. Visual scope — "Control Room" (locked)

The visual identity is **final**. Personality: professional, technical, utilitarian — developer-first infrastructure and modern financial tools (Linear, Stripe register). "Density over decoration" and "State is always visible."

* Light mode only. No theme switch.
* Cool slate surfaces (`#F4F6F9` app background, `#FFFFFF` cards), indigo `#4F46E5` reserved for actions, selection, and focus. Color is never decorative.
* Semantic status ramp (tint + border + text triplets): Available emerald `#059669`, buffer/warning amber `#B45309`/`#F59E0B`, Reserved indigo, Rented slate `#334155`, Preparing sky `#0284C7`, Maintenance burnt orange `#C2410C`, Retired gray `#94A3B8`.
* Signature element: **3px left status bar** on operational cards, full card height, color-coded by lifecycle state.
* Typography: system font stack; hierarchy via weight (400/500/600) and modest sizes (display 21, headline 18, body 13, meta 11.5, label 10.5, price 15 / price-lg 19, code mono 13). **Tabular numerals everywhere numbers align** (tables, KPI, money).
* 8px base grid with 4px half-step. Radii: 6px small elements, 8px universal (cards, buttons, inputs, modals, drawers), full (9999px) only for 7px status dots and unread counters.
* Elevation: cards sit on 1px border + `0 1px 2px rgba(15,23,42,.05)`; hover darkens borders, never grows shadows; real depth only for modals/drawers/overlays (`0 12px 32px rgba(15,23,42,.14)`, 40% scrim).
* Banned: gradients, glass, decorative illustration, marketing graphics, second accent colors, new hues per screen, uppercase micro-labels, shadows-as-hierarchy, emoji in system toasts.

# 5. Data control rules

Do not invent business data, KPIs, prices, percentages, addresses, dimensions, facilities, amenities, policies, or statistics.

Every visible value must be one of:

1. `Stored field` — directly stored in the data model.
2. `Derived field` — calculated from stored data (document it).
3. `Demo-only value` — defined once in the shared mock-data source (§13).

Do not invent: security scores, customer ratings, previous-renter counts, revenue growth percentages, AI monitoring, insurance, temperature/humidity, discount percentages, popularity, uncalculated availability counts, extra service fees, or features absent from the data model.

Do not display raw technical IDs. Display business codes: Reservation `BK-`, Rental `RT-`, Support `SR-`, Contract `CT-` (addendum `CT-…-A1`), payments/receipts per mock data. Unit codes are short mono codes (`S-3`, `M-2`).

Components must not calculate prices or invent business rules. Pricing comes from the active **Rental Policy** version (demo: v3); every money line shown before payment must restate what the policy produces. The extension conflict boundary ("Latest new checkout: Oct 18") is supplied by data, not computed ad hoc in the UI.

# 6. Canonical entities and roles

Entities (unchanged from blueprint (3), still the UI data source):

Role, User, Facility, Zone, StaffAssignment, UnitType, Unit, RentalPolicy, PolicyRule, Reservation, Rental, Extension, CheckoutRequest, Contract, Payment, Settlement, SettlementCharge, Inspection, SupportTicket, Escalation, Task, Notification, ActivityLog.

**Exactly four roles appear in the UI:**

* CUSTOMER
* STAFF
* FACILITY_MANAGER
* BUSINESS_OPERATIONS_MANAGER

Account provisioning for staff/managers happens outside the UI. Public registration creates Customer accounts only — state this plainly on the Register screen.

Model rules that the UI must respect:

* Facility → Zones → Units. Staff are assigned by work date, shift, and zone; assignment conflicts are rejected with the specific collision.
* Availability is derived from reservations, rentals, and the **turnover buffer**. "Available soon (cleaning buffer)" is a derived Browse label, never a stored Unit status.
* Pricing values come from a versioned RentalPolicy; the contract locks the policy version it was drafted from.
* A Reservation creates at most one Rental; an Extension belongs to one Rental and produces one Contract addendum; a Settlement contains SettlementCharge records; a SupportTicket may have one Escalation.
* Contracts are **auto-drafted** from the booking + active policy version. Nobody composes a contract by hand; nothing in a contract is editable anywhere in the UI.
* ActivityLog is append-only. Every important state change requires an actor and, where the flow demands, a **reason**. No silent status writes.
* Login actions do not require a business reason.

# 7. Canonical statuses

One shared `statusDictionary`; never hand-write status labels inside pages; the dictionary is entity-aware (`ACTIVE` means different things on a Rental vs a Contract).

## UnitStatus (with UI meaning)

| Value | Label | UI |
|---|---|---|
| AVAILABLE | Available | Green; bookable in Browse with start-date math |
| RESERVED | Reserved | Indigo in management views after deposit; customer sees active reservation |
| RENTED | Rented | Slate; excluded from Browse |
| PREPARING | Preparing | Sky; after checkout; cleaning task auto-created; not bookable until cleaning completes |
| MAINTENANCE | Under Maintenance | Orange; excluded from Browse; visible in Operations Monitor |
| RETIRED | Retired | Gray; only Unit Management + Activity Log |

Derived label (not a status): **"Available soon"** — amber buffer card in Browse ("Available Oct 5 · cleaning buffer"), bookable from the later date.

Lifecycle: `Available → Reserved → Rented → Preparing → (Maintenance | Retired) → Available`.

## ReservationStatus

PENDING_PAYMENT → Pending Payment · RESERVED → Reserved · CHECKED_IN → Checked In · EXPIRED → Expired · CANCELLED → Cancelled

## RentalStatus

ACTIVE → Active · CHECKOUT_REQUESTED → Checkout Requested · CLOSED → Closed

## ExtensionStatus

PENDING_PAYMENT → Pending Payment · APPLIED → Applied · CANCELLED → Cancelled

## PaymentStatus (mock gateway states)

PENDING → Method select · PROCESSING → Processing (~1.5–2 s) · SUCCEEDED → Successful · FAILED → Failed (retry) · EXPIRED → QR expired (~5 min countdown, return to method select, nothing charged)

## TicketStatus

OPEN → Open · IN_PROGRESS → In Progress · ESCALATED → Escalated · RESOLVED → Resolved

## TaskStatus (kanban columns)

TODO → To Do · IN_PROGRESS → In Progress · DONE → Done

## ContractStatus — **new vs blueprint (3)**

| Value | Label | UI |
|---|---|---|
| DRAFT | Draft | "Contract CT-1042 drafted from Rental Policy v3." Read-only preview in Check-in Task |
| PRINTED | Printed | "Awaiting customer signature" — blocks access-code handover |
| SIGNED | Signed | Signed photo attached; ActivityLog writes CONTRACT_SIGNED; copy viewable on Rental Detail |
| ACTIVE | Active | The rental runs on this contract; settlement references its number |
| CLOSED | Closed | Settlement receipt names the contract; signed copy viewable forever |
| SUPERSEDED | Superseded | End date replaced by addendum `CT-1042-A1`; chain stays readable |

Status badges are never color-only: tint + border + text label, from the `status-*` token ramp.

# 8. Design system section

The Stitch project already carries the design-system asset generated from DESIGN.md. Keep it as the single token source. Any component work must define, per component: name, purpose, props/properties, variants, states, entity/field mapping, screens using it, one master, example instances — and must reuse the token names rather than raw hex values.

# 9. Primitive patterns

Button (primary indigo filled / secondary-ghost bordered), IconButton, TextInput, PasswordInput, NumberInput, TextArea, Select, MultiSelect, DatePicker, DateTimePicker, DateRangePicker, Checkbox, RadioGroup, SearchInput, FormField, FieldError, StatusBadge, Tag, Tooltip, Divider, Tabs, Pagination, Breadcrumb, Toast, InlineAlert, Skeleton, Spinner, EmptyState, ConfirmDialog, FormModal, DetailDrawer, DropdownMenu.

All form controls use the same FormField structure: label (visible, never placeholder-only) → required indicator → input → helper text → validation message. Inline errors fire on blur (error border + message). Server-side blocks (policy validation, conflict checks) surface as an error-tint banner at the top of the form restating the failed rule — not a toast. Submit disables only for missing required fields, never for soft warnings.

Empty states: icon tile, factual title, one-sentence explanation restating the constraint, mono chips echoing the query/filters, single CTA. Zero-task kanban columns show a quiet "Nothing here" tile. Skeletons match layout (card-grid skeletons on Browse, row skeletons on tables); no layout shift on swap.

# 10. Layout — one adaptive shell

## AppShell (single, all roles)

Top bar, left → right: logo mark + wordmark · role menu (links per role) · role chip (primary-tint pill naming the current role) · notification bell with unread count · avatar menu (profile, logout). Below: page content. Minimal footer where present.

There is no customer header vs back-office sidebar split (removed vs blueprint (3)). The role chip makes the adaptive shell legible.

## Shared page structures

PageHeader · FilterToolbar (closed dropdowns + Search) · DataTable (dense, 13px, tabular numerals, quiet `#F1F5F9` separators, hover `#F8FAFC`, pagination 25) · CardGrid · EntityListPageTemplate · DashboardPageTemplate (KPI row → chart block → backing table, in that order) · TransactionPageTemplate · Stepper · MetricCard/KpiCard (24px tabular value + delta chip, clickable) · ChartPanel · PrimaryActionBar.

**KPI drill-down contract:** every KPI card drills into its backing table pre-filtered to the same slice (occupancy 87% → Unit Management filtered to Rented; surcharges this quarter → Reports Surcharges tab, same period). The dashboard never shows a number the user cannot reach the rows behind.

# 11. Domain component contracts

## UnitCard (Browse grid)

Photo + mono code chip, size/type chips, feature line, price, availability line with status dot, Book button. The 3px left bar encodes status (available green / buffer amber). Buffer cards say "Available Oct 5 · cleaning buffer" and keep a secondary Book. Click anywhere except Book → Unit Detail. Availability lines are computed against the rental calendar + turnover buffer at query time — the displayed start dates are the promise. Reserve re-checks availability; a stale result bounces back to the grid with a toast ("S-3 was just reserved. 5 similar units still available.").

## UnitRow / UnitTable (management)

Columns: code (mono), size, type, zone/floor, current status badge, active rental/reservation link, last activity. Row click opens the edit drawer.

## UnitEditDrawer

Actions: edit specs · merge with adjacent unit (guard: no active Rental or Reservation on either side) · retire (guard + confirm) · set Maintenance · fix wrong status (**reason required**). Every write lands in Activity Log. Destructive/guard-checked edits promote to confirm modals stating the consequence in money/status terms.

## ReservationCard / RentalCard

Reservation: code, unit, facility, dates, deposit, status. No access code (codes belong to Rentals).
Rental: code, unit, facility, end date, deposit held, status, access code via a SensitiveValue treatment; contract chain (original + addenda) with mono codes, status badges, signed photos; unsigned addendum shows an amber warning banner naming the desk deadline. Payment receipts (deposit, rent, extension) and deposit status (held / settled) hang off Rental Detail — the customer's home base.

## ContractStep (the paper ritual) — new vs (3)

Read-only preview of the auto-drafted contract — mono code, parties, unit, dates, rent/deposit lines, locked policy version — then Print, a signed-copy photo capture tile, and Attach. Nothing is editable. The flow's completing action (access-code handover, addendum completion) stays disabled until a signed photo is attached. Appears in Check-in Task and on Contract-signature kanban cards.

## MoneySummary

Line items received from data only: rental amount, deposit (marked refundable), extension fee, settlement charges (each with reason), refund amount, additional amount due, total payable. Nothing appears at payment that did not appear in the review before it.

## PaymentModal (mock gateway) — new vs (3)

Shared by exactly three touchpoints: 10% deposit (Booking Summary), 100% rent (Check-in Task), extension fee (Extend). Header names what is being paid + amount in price-lg. Method segmented control: **Card** (mock number/expiry/CVC with inline validation) · **MoMo** (phone + mock OTP step) · **VNPay QR** (rendered QR, ~5 min countdown; expiry returns to method select, nothing charged). Pay button carries the exact amount. Processing ~1.5–2 s spinner, no back navigation. Success: check tile, amount, consequence line → toast → underlying screen state flips. Fail: error tile, "No money was taken", Retry + Switch method; after two fails, suggest switching explicitly. Booking Confirmation is **not** a separate screen — the success state ends flow F1 and deep-links to Rental Detail.

## KanbanCard / TaskBoard

Columns To do / In progress / Done; type tabs above; per-column counts in headers. Card: unit code (mono), task type chip, customer name / time slot, due chip, type-colored left bar. Drag to move; keyboard alternative via per-card Move buttons (drag is never the only path). Moving a Check-in/Checkout card into Done is blocked until the task screen's closing steps (payment / settlement) are complete — the card snaps back with a toast naming the missing step. Support cards carry Escalate. Done cards render muted. Contract-signature cards appear when an addendum awaits its paper signature (customer, unit, addendum code, due date) and complete when the signed photo lands.

## TicketCard / TicketDetail

Card/list row: ticket code, unit, incident type, priority, status, assignee, created/updated. Full thread, routing, and resolution live in the detail drawer (customer Support List) or Support Ticket screen (staff). Staff actions: Resolve with note, or Escalate (**note required**). Manager severity decision panel: mark severe → unit to Maintenance + customer relocation; or return to staff with guidance — never dropped.

## SettlementPanel (Checkout Task)

Receive unit + key checklist → condition inspection per item (walls, door, floor, cleanliness) → damage fee amount + **required reason** when damage found → settlement preview (deposit − fee = refund, or extra charge if fee exceeds deposit) → close-rental confirm. Support full refund, partial refund after charges, and additional payment when charges exceed the deposit.

## KpiCard / ChartPanel

Per DESIGN.md; values derived from shared demo data; no invented trend percentages. Clickable per the drill-down contract.

## UserRow, ActivityLogRow

UserRow: full name, email, role, facility/zone scope, account status.
ActivityLogRow: timestamp, actor, entity, action, from → to, reason (where required); filter by entity type; read-only, append-only. Previous/new value detail belongs in the log detail view.

# 12. Shared component usage (27 screens)

| Component | Screens (F-codes) |
|---|---|
| AppShell | All 27 |
| UnitCard grid | F1-03; compact variants on escalation/relocation contexts |
| UnitDetailPanel | F1-04 |
| MoneySummary | F1-04, F1-05, F6-01, F2-02, F3-01 |
| PaymentModal | F1-05, F2-02, F6-01 |
| RentalCard + contract chain | F2-03, F2-04, F6-01 |
| ContractStep | F2-02, Task Board contract cards |
| KanbanBoard | F2-01 |
| SettlementPanel | F3-01 |
| TicketCard / detail | F7-02, F7-03, F7-04 |
| DataTable | F5-02, F5-03, F5-05, F4-02, F4-03 |
| DashboardPageTemplate + KpiCard + ChartPanel | F5-01, F5-04, F4-01 |
| NotificationCenter | Bell on all screens → F7-05 |
| StatusBadge | Every screen with a status |
| FilterToolbar | Every searchable/filterable list |
| EmptyState / Skeleton | Every list and loading surface |
| ConfirmDialog (+ typed reason) | Retire, merge, severity, close-with-deduction, status fix |

# 13. Shared mock data (single source)

All demo values defined once; screens never define local data.

Demo users:

* **Lan** — Customer (books S-3, extends, checks out; refund 63.500 ₫)
* **Minh** — Staff (runs the kanban; check-in, checkout, support)
* **Hang** — Business Operations Manager (ships policy v3)
* **Tuan** — Facility Manager (merges units, fixes status, handles escalation)

Demo facility: **Tân Bình Depot** (render "Tan Binh Depot" where accents are unavailable).

Demo units: `S-1`, `S-2` (merge candidates → one 8 m² unit), `S-3` (5 m² Indoor — the flagship demo unit), `M-2` (flooding escalation), `M-5` (relocation target).

Demo records:

* `BK-1042` — reservation, deposit paid, waiting for check-in
* `RT-0871` — active rental (checkout task)
* `CT-1042` — original contract, signed at check-in
* `CT-1042-A1` — extension addendum, awaiting desk signature (7-day deadline)
* `SR-0032` — support ticket in progress (sticky door); escalation scenario = flooding in M-2
* **Rental Policy v3** — active pricing version (source of every money line)

Demo amounts (exact, reuse everywhere):

* Rent S-3: `345.000 ₫/mo` × 3 months = `1.035.000 ₫`
* Deposit (10%, refundable): `103.500 ₫`
* Extension fee: `690.000 ₫` (new checkout Oct 18; blocked range starts Oct 19)
* Damage fee at checkout: `40.000 ₫` (with reason)
* Settlement refund: `63.500 ₫` (= 103.500 − 40.000)
* One morning-shift staff assignment (Minh, Zone B conflict scenario on Oct 12)
* One check-in task, one checkout task, one cleaning task, one contract-signature card

# 14. Required 27 screens (exactly)

## Auth (2)

| ID | Screen | Notes |
|---|---|---|
| F1-01 | Login | Email + password. Forgot password = **modal** (email → "If an account exists for that email, a reset link has been sent."). States plainly that staff/manager accounts are provisioned by administration |
| F1-02 | Register | Customer self-signup: full name, phone, email, password + confirmation, agree-to-terms. Success lands on Browse Units, logged in |

## Customer (12)

| ID | Screen | Notes |
|---|---|---|
| F1-03 | Browse Units ⭐ | Flagship. Filter bar (type / size / start date / duration) + unit card grid; results-count chip "6 units available · live"; sort price low→high; buffer cards bookable at later date. No calendar view, no floor map |
| F1-04 | Unit Detail | Photos, spec rows (dimensions, floor, access, security), price breakdown (rent × duration, 10% deposit), Reserve CTA |
| F1-05 | Booking Summary | Every line the customer will be held to: rent × duration, each surcharge line from active policy, total, 10% deposit marked refundable; note that the contract is drafted from these exact terms and signed at check-in. Opens Payment Modal |
| F1-06 | Payment Modal | Overlay component documented as a screen-state sheet (3 methods × states). Not a navigation destination |
| F2-03 | My Rentals | Active and past rentals as cards with status badges and next-action buttons |
| F2-04 | Rental Detail | Home base: unit, dates, payment history, deposit status, **contract chain** (original + addenda, signed copies viewable, amber banner on unsigned addendum), actions — Check-in, Extend, Checkout Request, New Support |
| F2-05 | Check-in Pass | Customer-side arrival pass: reservation code big and mono, desk instructions, what to bring (ID for contract, payment for 100% of rent) |
| F6-01 | Extend | Pick new end date → conflict check vs next reservation (conflicting range marked; boundary stated: "Latest new checkout: Oct 18") → extension fee → Payment Modal → addendum auto-drafted, signed at desk within 7 days |
| F3-02 | Checkout Request | Request move-out date; explains deposit settlement logic (refund / deduction / extra) |
| F7-01 | New Support | Pick unit + incident type + description → submit |
| F7-02 | Support List | Request history; **detail drawer** shows thread, routing, resolution |
| F7-05 | Notification Center | Bell dropdown; unread first, grouped by day, deep links, mark-all-read |

## Staff (4)

| ID | Screen | Notes |
|---|---|---|
| F2-01 | Task Board ⭐ | Staff landing. Kanban To do / In progress / Done; Check-in / Checkout / Cleaning / Support / Contract-signature cards; type tabs; column counts |
| F2-02 | Check-in Task | Verify reservation code → validate reservation + deposit paid → collect 100% rent (Payment Modal) → **ContractStep** (preview CT-1042 read-only → Print → capture signed copy → Attach; handover note "Requires signed contract on file") → hand over access code → rental activates |
| F3-01 | Checkout Task ⭐ | Settlement climax: receive unit + key, inspection checklist, damage fee + required reason, settlement preview, close rental → unit becomes Preparing |
| F7-03 | Support Ticket | Incident detail (customer's words), unit + customer context, staff note, Resolve / Escalate (note required) |

## Facility Manager (6)

| ID | Screen | Notes |
|---|---|---|
| F5-01 | Facility Overview | Landing. Dashboard-first: occupancy, revenue mix, unit-status KPIs + charts + backing table |
| F5-02 | Unit Management ⭐ | Unit table + **edit drawer** (edit, merge, retire, maintenance, fix status + reason; guards check Rental + Reservation + history) |
| F5-03 | Staff & Shifts | Staff roster; assignment = staff × zone × shift × date; conflict check on submit; weekly schedule grid |
| F5-04 | Operations Monitor | Live: today's tasks, escalations, turnover-buffer queue |
| F5-05 | Activity Log | Append-only audit trail: timestamp, actor, entity, action, from → to, reason; filter by entity type |
| F7-04 | Escalation Inbox ⭐ | Escalated tickets with severity decision panel: mark severe → Maintenance + relocation; or return to staff with guidance |

## Business Ops (3)

| ID | Screen | Notes |
|---|---|---|
| F4-01 | Business Overview ⭐ | Landing. KPI cards + charts: revenue, deposits held, surcharges, utilization per period |
| F4-02 | Policy Management ⭐ | Validation climax. Rent + surcharge rules per unit type in an editable table; inline edit; Save runs full validation (an invalid policy never persists); version + effective-date stamp |
| F4-03 | Reports | Tabs Revenue / Deposits / Surcharges / Occupancy over a period; presets (this month, last month, quarter) + custom range; Export CSV per tab |

## Traceability to blueprint (3) codes

| Old | Now |
|---|---|
| AUTH-01 | F1-01 + F1-02 (Register became its own screen; forgot-password a modal) |
| SYS-01 | **Removed** |
| C-01 | F1-03 |
| C-02 | F1-04 + F1-05 |
| C-03 | F1-05 + F1-06 (confirmation = payment success state) |
| C-04 | F2-03 + F2-04 |
| C-05 | F6-01 |
| C-06 | F3-02 |
| C-07 | F7-01 + F7-02 (+ F7-05 Notification Center) |
| S-01 | F2-02 (+ F2-05 customer-side pass) |
| S-02 | F3-01 |
| S-03 | F2-01 (+ F7-03) |
| M-01 | F5-02 (floor plan cut — banned) |
| M-02 | F5-03 |
| M-03 | F7-04 |
| M-04 | F5-04 (+ F5-01) |
| B-01 | F5-01 (moved to Facility Manager) |
| B-02 | F4-02 (draft-version workflow → inline edit + validation-gated save + version stamp) |
| B-03 | F4-01 + F4-03 |
| A-01 | **Removed** (no admin role) |
| A-02 | F5-05 (moved to Facility Manager) |
| — | New: F1-06, F2-05, F7-05 documented as screens/overlays |

# 15. Role-based navigation

Same shell, same bell, same avatar menu — only the center links change:

* **Customer:** Browse Units · My Rentals · Support
* **Staff:** Tasks · Support (no Units link; unit context lives on kanban cards)
* **Facility Manager:** Overview · Units · Staff & Shifts · Operations · Activity Log · Escalations
* **Business Ops:** Overview · Policy · Reports

Show only the navigation allowed for the current role. The role chip names the current role at all times. Deep customer paths (Extend, Checkout Request, New Support, Check-in) hang off Rental Detail so the rental — never the system — is the customer's anchor.

# 16. Voice and tone (binding microcopy register)

Precise, competent, low-ceremony SaaS English. State facts, name amounts, explain blocks in one breath. No exclamation marks, no streak-cheer, no marketing verbs — for any role; seniority is not a tone of voice. Errors always say what happened, what it cost (usually nothing), and what to do next.

Do / Don't:

* "Unit S-3 reserved. Deposit 103.500 ₫ received." / "🎉 Your booking is successful!"
* "Payment failed. No money was taken. Retry or choose another method." / "Transaction error (code 05)."
* "Oct 20 is taken by the next reservation. Latest new checkout: Oct 18." / "This date is unavailable."
* "6 units available · live" / "Plenty of great options!"
* "Checkout closed. Refund 63.500 ₫ to Lan after damage fee 40.000 ₫." / "Process complete."
* "Reason required to change status." / "Error: validation failed."

# 17. Shared prototype state

Use one shared state source so that actions on one screen update related screens. Required demo behavior (the five key flows):

* **F1 (Lan books):** Browse → Unit Detail → Booking Summary → deposit payment (MoMo) → Reservation confirmed + contract `CT-1042` auto-drafted; Rental Detail reflects receipts; stale-grid reserve bounces back with toast.
* **F2 (Minh check-in):** reservation code validates → 100% rent collected → contract printed/signed/photographed → access code issued → Rental activates, Unit → RENTED, card completes.
* **Checkout:** inspection → damage fee 40.000 ₫ with reason → settlement refund 63.500 ₫ → Rental CLOSED, Unit → PREPARING, Cleaning card appears on the board; cleaning completes → turnover buffer checked → Available (or Reserved if a next reservation exists).
* **Extend:** conflict blocks Oct 19+ → pick Oct 18 → pay 690.000 ₫ → new checkout date lands immediately → addendum `CT-1042-A1` awaits desk signature (7-day deadline, amber banner, Contract card on staff board, bell reminders both sides).
* **Escalation:** flooding reported in M-2 → staff escalates with note → manager marks severe → M-2 → MAINTENANCE + customer relocated to M-5 → ticket Resolved with the full arc in one drawer; customer notified in plain words.
* Manager writes (merge, status fix with reason, assignments with conflict rejection) land in Activity Log; important actions create Notifications (money events emit both toast and bell entry).

No real backend. Unconfirmed formulas use fixed mock responses.

# 18. Mutation pattern

Every data-changing action includes: clear trigger → form or confirmation → validation → loading state → success/error result → shared-state update → ActivityLog when applicable → Notification when another user is affected.

Examples:

* **Check-in completing:** access-code handover stays disabled until the signed contract photo is attached.
* **Damage fee:** settlement cannot proceed until a fee amount **and reason** are recorded.
* **Unit status fix:** reason textarea required; the change lands in Activity Log with actor, timestamp, from → to, reason; every UnitCard/UnitRow showing that unit updates.
* **Policy save:** validation blocks persisting; offending field flagged; message names the violated cap/rule; success confirms version + effective date.
* **Shift assignment:** conflict rejected before save with the specific collision; conflicting slot highlighted.
* **Merge/retire:** guards check Rental + Reservation + history; blockers named and linked; action unavailable until clear; confirm modal states consequences ("This cannot be undone").

# 19. Consistency rules

* Same entity field name everywhere; same English label per status; money always full-precision VND (`1.150.000 ₫`) with tabular numerals; dates consistent; same business-code prefix everywhere.
* "Storage Unit" in page titles and formal descriptions; "Unit" in compact labels and component names. One term per entity — never synonyms.
* No hard-coded status labels (statusDictionary), no hard-coded policy values inside components, no page-specific mock data, no direct data edits by changing text inside a card.
* Tables for dense administrative data; cards only where users compare a few key attributes.
* Status never color-only; every surface carrying state shows it at a glance (3px bar / badge ramp).
* No money moves without a line-item breakdown before it; every fee carries a reason string where the flow requires one.
* Feedback is toast + bell only — never email. Toasts auto-dismiss ~4 s, hover pauses, max one action link.
* Accessibility floor: visible labels on every input; visible 2px primary focus ring; full keyboard reachability (incl. kanban Move buttons); status never color-only; touch targets ≥ 40px; focus trapped in modals/drawers and returned to trigger.

# 20. Required output

1. The design-system section reflecting DESIGN.md tokens (already present — keep synchronized).
2. Component/pattern inventory with variants, states, usage matrix (§12).
3. UI data dictionary + statusDictionary (§7).
4. Shared mock-data source (§13) + derived-field inventory + demo-value inventory.
5. Exactly 27 canonical screens matching the frozen mockups; overlay components (Payment Modal, forgot-password modal, drawers) documented as components, not screens.
6. Working prototype navigation per §15 and shared demo state per §17.
7. A list of visible fields that still lack a valid data source (target: empty).

# 21. Execution order (for any future Stitch work)

## Step 1 — Audit

The Stitch project currently holds **39 visible screen instances of 27 unique screens** — the surplus 12 instances are stale duplicates from edit history (Policy Management ×3, Task Board ×3, Checkout Task Detail ×4, Staff & Shifts ×2, Check-in Task Detail ×2, Browse Units ×2, Unit Management ×2, Escalations ×2), plus one hidden legacy screen. Identify the canonical instance of each title (the one matching the frozen mockup export; for Check-in Task this is the newer instance with the ContractStep), and mark the rest for archival/deletion.

## Step 2 — Data contract

Confirm the data dictionary and status dictionary (incl. ContractStatus) against the ERD/state-chart report.

## Step 3 — Pattern library

Map every screen region to a shared pattern (§9–§11); flag any region that duplicates another screen's markup without sharing the pattern.

## Step 4 — Consolidate

Reduce to exactly 27 canonical instances; no duplicate screen sets.

## Step 5 — Connect

Navigation + shared prototype state per §17.

## Step 6 — Verify

* Exactly 27 screens; no duplicates; no mobile/tablet frames.
* All visible UI text is English; no Vietnamese interface text; money is full-precision VND.
* No visible field lacks a known source; no custom statuses invented; statusDictionary respected.
* One entity update appears on every related screen; role-based navigation correct; role chip present.
* Every KPI drills through; every important button performs a prototype action; every money line shown before payment matches the policy version.
* Contract ritual enforced: no access code without a signed photo; unsigned addendum past due generates reminders.
* No silent writes: reasons and audit trail where the flow demands them.

First provide a short audit of the current project against this blueprint. Then perform the work directly. Do not stop after presenting a plan.
