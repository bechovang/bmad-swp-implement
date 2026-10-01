# Epic 1 Context: Platform Foundation & Identity

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

After this epic, every user can register and log in, land on the correct role-specific screen inside the five-role adaptive shell (role chip + per-role nav), and receive feedback through both channels: transient toast and the persistent bell/Notification Center. Equally important, it lays the foundation every later epic stands on — the monorepo starter with a pinned stack, the contract-first API skeleton, the full Flyway schema plus a reproducible demo seed, unified error/list envelopes, append-only audit logging, JWT auth behind a server-enforced permission matrix, and the "Control Room" design token system.

## Stories

- Story 1.1: Monorepo starter template per Structural Seed
- Story 1.2: Backend foundations — error/list envelope + append-only LogService
- Story 1.3: Auth backend — JWT + permission matrix + LOGIN audit
- Story 1.4: FE design token system "Control Room" + base primitives
- Story 1.5: Adaptive shell for 5 roles + auth screens FE
- Story 1.6: Toast + Bell + Notification Center

## Requirements & Constraints

- Login (email + password) routes to the role landing: Customer → Browse Units, Staff → Task Board, Facility Manager → Facility Overview, Business Ops → Business Overview, System Administrator → User Management. Wrong credentials show one generic inline error that never reveals which field failed, plus one next step. Cross-role URLs yield a simple 403 state. Avatar menu (profile, logout) exists for every role.
- Self-register is customer-only: name, phone, email, password + confirmation, agree-to-terms; no role picker on the form and the API accepts no role field; success lands on Browse Units logged in; duplicate email blocked inline.
- Forgot password (P2) is a modal opened from Login that always answers generically ("If an account exists…") — no real email in v1.
- Security baseline: BCrypt password hashing; every app route requires login; permissions enforced server-side per endpoint (hidden menus are UX only); the Role × Permission matrix is fixed in code; deactivated/locked accounts (`users.Status` 0/2) cannot log in; a per-request filter re-checks account status (short cache) so tokens die on lock, not at TTL expiry.
- Notifications: toast ~4s with hover pause, at most one action link; bell badge counts unread in error-red (alert and action stay separate channels); money events fire both toast and bell; never email. Notification Center is role-scoped (customers: reservation/payment/support/contract; staff: task assignment; managers: escalation + status writes; business ops: policy saves + exports), unread first, deep-links to the affected object, mark-all-read, unread persists across sessions via server-side read state.
- UI is English-only, light mode only, desktop-first with a 1200px target (narrower windows still usable). VND renders full precision `1.150.000 ₫` (dot separator, ₫ after the amount) in tabular numerals.

## Technical Decisions

- Monorepo layout: `storagehub/backend` (Spring Boot), `storagehub/frontend` (Vite), `storagehub/contracts` (`openapi.yaml` + `routes.yaml`), `docs/`. Vite dev server proxies `/api` → backend `:8080`; sole health endpoint is `/actuator/health`; `backend/storage/` is gitignored.
- Stack pins: Spring Boot 4.1.1 / Java 21 (starters webmvc · data-jpa · security · validation · flyway · actuator · flyway-mysql); React 19.3.0 + Vite 8.3.0; MySQL 8.4.11; TanStack Query + Axios; MSW 2.x; JUnit Jupiter 6 / Mockito 5.23 / AssertJ 3.27.7; Vitest + RTL; PayOS SDK `vn.payos:payos-java` 2.0.1 in the pom from day one. Boot 4 gotchas: starter `web` is renamed `webmvc`; test starters split per module (`-security-test` for `@WithMockUser`); Jackson 3 (`tools.jackson`) is default; Flyway needs `flyway-mysql`.
- Contract-first: `contracts/openapi.yaml` is the single API source of truth — frozen at sprint start, changed before code; the FE develops against MSW mocks generated from it while the BE catches up; springdoc renders docs only, never generates the contract.
- Layer-first: controllers only do HTTP ↔ DTO and call exactly one service; all business logic and transactions live in services; repositories only query; JPA entities never leave the backend; cross-module reads go through public service query methods (derive-on-read, no denormalization).
- Flyway owns the schema: V1 = the full 22-entity model V3 with the pinned deltas listed in story 1.1; V2__seed_demo runs only on profile `dev` and must reproduce the standard demo data set (all five demo roles, demo units/rentals/contracts, policy v3) from zero. No manual DDL, no hbm2ddl.
- Auth: JWT Bearer with role claim, 24h TTL, no refresh token, logout = client drops token. Exactly three public endpoints in this epic (`POST /api/v1/auth/login`, `/auth/register`, `/auth/forgot-password`); Epic 2 adds the fourth (payments webhook). Everything else returns 401/403 in the standard envelope. Both successful and failed logins are audited as `LOGIN`/`LOGIN_FAILED` (EntityType USER).
- One error envelope everywhere (machine code + human message + field errors when present), including security errors via `AuthenticationEntryPoint`/`AccessDeniedHandler` — no default Spring bodies. Business-rule blocks = 409 + envelope. Lists use `{items, page, pageSize, total}` with 1-based page, default 25. Enums UPPER_SNAKE, JSON camelCase, filter/sort params kebab-case.
- LogService is the append-only audit writer: INSERT-only at the data layer; Action/EntityType are shared enums in a registry that also declares which actions require a reason — a caller omitting a required reason is refused, so no orphan logs.
- NotificationService exclusively owns NOTIFICATIONS: business modules trigger it via a typed public event API (`NotificationEvent` + deep-link registry declared in the contract) inside the same transaction; direct INSERTs from other modules are banned. Successful mutations return a `notification` object in the response; the FE renders the toast from that data and never composes its own strings. Bell/unread-count uses a lightweight dedicated endpoint polled with TanStack `refetchInterval`.
- Money as `BigDecimal` / `DECIMAL(15,0)` / JSON number — float/double banned. Timestamps stored as UTC `Instant`, transported ISO-8601 with offset, displayed in `Asia/Ho_Chi_Minh`.
- Conventions: REST plural kebab-case nouns; DTOs `XxxRequest/Response`; FE components PascalCase, hooks `useXxx`; TanStack Query for server state; secrets (JWT secret, DB credentials, PayOS keys) via env vars only, never committed.
- Week-1 deferred decisions recorded in the README by story 1.1: TypeScript vs JS, component library, dnd, chart, QR code lib — must be settled before story 1.4 starts; no shared FE components get built before then.

## UX & Interaction Patterns

- Identity "Control Room": technical SaaS register (Stripe/Linear lineage), cool slate surfaces, a single indigo accent `#4F46E5` reserved strictly for action/selection, 8px grid, tabular numerals, and a 3px left status bar on every unit/task card. Light mode only.
- Token system ships complete in one pass: 6-step surface ramp, 4-step text ramp, indigo + tint/border variants, seven unit-status semantics each with dot/tint/border (Available green, Buffer amber, Reserved indigo, Rented slate, Preparing sky, Maintenance orange, Retired grey), feedback tokens, 14 typography styles on system fonts with mono for codes, radius md 8px / sm 6px chips / full only dots, spacing scale 4→48 plus `page-x` 24 and `nav-height` 54. Elevation = border + 1px shadow; hover darkens the border; only modal/drawer earn a real shadow.
- Base primitives follow one grammar: buttons primary/secondary/ghost 36px (40px page-level, destructive = secondary + error text, exactly one primary per card); inputs 36px with always-visible label, 2px indigo focus ring, invalid border + message; modal max 480px with right-aligned actions; drawer 420px right, Esc/scrim close; tabs 2px underline; empty states (56px icon tile, factual title, mono chips echoing filters, exactly one CTA); layout-matching skeletons; tables sticky-header 40px rows, right-aligned tabular numerals, hover row, no zebra.
- A shared `formatMoney` util emits `1.150.000 ₫` — dot separator, ₫ after the amount, full precision, `tabular-nums`; unit codes render mono.
- Adaptive shell: one app frame, five role menus. Top bar left → right: logo + role menu + role chip (primary-tint pill naming the current role) + bell + avatar menu. Nav per role: Customer — Browse Units · My Rentals · Support; Staff — Tasks · Support; Facility Manager — Overview · Units · Staff & Shifts · Operations · Activity Log · Escalations; Business Ops — Overview · Policy · Reports; System Administrator — Users · Login History.
- Overlay discipline is one level deep: a modal never opens from a modal; drawers never stack on modals.
- Accessibility floor: visible labels on every input (no placeholder-only), visible focus, full keyboard reach, click targets ≥ 40px, focus trapped in open modal/drawer and returned to the trigger.
- Microcopy: precise, low-ceremony SaaS English in one plain register for all five roles; every error/empty/toast states what happened, the money/status consequence (usually none), and exactly one next step. No exclamation marks, streak-cheer, or marketing verbs.

## Cross-Story Dependencies

- Story 1.1 gates all other stories (repo, schema, seed, contract skeleton).
- 1.2's envelopes and LogService are the shared conventions later stories consume; 1.3 audits logins through 1.2's LogService.
- The week-1 stack decision from 1.1 must land before 1.4 starts; 1.5 needs both 1.4's primitives and 1.3's auth endpoints frozen in the contract — until the BE is ready, 1.5 develops against MSW mocks.
- 1.6's NotificationService event API and deep-link registry are the notification mechanism every later epic calls (payment success in Epic 2, contracts in Epic 3, addendum reminders in Epic 4, escalations in Epic 5); FE routes must match the registry.
- The permission matrix fixed in 1.3 is later surfaced read-only in Epic 9.
