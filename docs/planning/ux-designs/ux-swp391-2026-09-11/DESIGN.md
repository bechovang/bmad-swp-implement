---
name: StorageHub Control Room
description: Visual identity for StorageHub, a self-storage rental platform — technical-SaaS register (Stripe/Linear lineage), one adaptive shell for five roles, status-first surfaces, light mode only.
status: final
updated: 2026-09-21
colors:
  # Surface & structure ramp (lifted from D4 "Control Room", directions-1.html)
  app-bg: '#F4F6F9'
  surface: '#FFFFFF'
  surface-subtle: '#F8FAFC'
  surface-muted: '#F1F5F9'
  border: '#E2E8F0'
  border-strong: '#CBD5E1'
  divider: '#F1F5F9'
  # Text ramp
  ink: '#0F172A'
  ink-secondary: '#334155'
  muted: '#64748B'
  faint: '#94A3B8'
  # Brand
  primary: '#4F46E5'
  primary-tint: '#EEF2FF'
  primary-border: '#E0E7FF'
  primary-outline-border: '#C7D2FE'
  # Unit lifecycle status semantics
  status-available: '#059669'
  status-available-tint: '#ECFDF5'
  status-available-border: '#D1FAE5'
  status-buffer: '#B45309'
  status-buffer-bar: '#F59E0B'
  status-buffer-tint: '#FFFBEB'
  status-buffer-border: '#FDE68A'
  status-reserved: '#4F46E5'
  status-reserved-tint: '#EEF2FF'
  status-reserved-border: '#E0E7FF'
  status-rented: '#334155'
  status-rented-tint: '#F1F5F9'
  status-rented-border: '#E2E8F0'
  status-preparing: '#0284C7'
  status-preparing-tint: '#F0F9FF'
  status-preparing-border: '#E0F2FE'
  status-maintenance: '#C2410C'
  status-maintenance-tint: '#FFF7ED'
  status-maintenance-border: '#FFEDD5'
  status-retired: '#94A3B8'
  status-retired-tint: '#F8FAFC'
  status-retired-border: '#E2E8F0'
  # System feedback
  success: '#059669'
  success-tint: '#ECFDF5'
  warning: '#B45309'
  warning-bar: '#F59E0B'
  warning-tint: '#FFFBEB'
  error: '#DC2626'
  error-tint: '#FEF2F2'
  error-border: '#FECACA'
typography:
  display:
    fontSize: 21px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: '-0.01em'
  kpi-value:
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: '-0.01em'
  headline:
    fontSize: 18px
    fontWeight: '600'
    lineHeight: '1.35'
  body:
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif'
    fontSize: 13px
    fontWeight: '400'
    lineHeight: '1.5'
  body-strong:
    fontSize: 13px
    fontWeight: '600'
    lineHeight: '1.5'
  meta:
    fontSize: 11.5px
    fontWeight: '500'
    lineHeight: '1.45'
  label:
    fontSize: 10.5px
    fontWeight: '600'
    lineHeight: '1.4'
  button:
    fontSize: 13px
    fontWeight: '600'
  nav:
    fontSize: 13px
    fontWeight: '500'
  price:
    fontSize: 15px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: '-0.01em'
  price-lg:
    fontSize: 19px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: '-0.01em'
  code:
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace'
    fontSize: 13px
    fontWeight: '600'
  code-sm:
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace'
    fontSize: 11px
    fontWeight: '600'
rounded:
  sm: 6px
  md: 8px
  full: 9999px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 24px
  '6': 32px
  '7': 48px
  page-x: 24px
  nav-height: 54px
components:
  button-primary:
    background: '{colors.primary}'
    foreground: '#FFFFFF'
    radius: '{rounded.md}'
    typography: '{typography.button}'
    shadow: '0 1px 2px rgba(79,70,229,.35)'
  button-secondary:
    background: '{colors.surface}'
    foreground: '{colors.ink-secondary}'
    border: '1px {colors.border-strong}'
    radius: '{rounded.md}'
    typography: '{typography.button}'
  button-ghost:
    background: 'transparent'
    foreground: '{colors.ink-secondary}'
    radius: '{rounded.md}'
    typography: '{typography.button}'
  input:
    background: '{colors.surface}'
    border: '1px {colors.border}'
    radius: '{rounded.md}'
    label-typography: '{typography.label}'
    label-color: '{colors.muted}'
  card:
    background: '{colors.surface}'
    border: '1px {colors.border}'
    radius: '{rounded.md}'
    shadow: '0 1px 2px rgba(15,23,42,.05)'
  card-status-bar:
    width: 3px
    position: 'absolute, left edge, full card height'
    default: '{colors.status-available}'
  badge:
    radius: '{rounded.sm}'
    typography: '{typography.label}'
    padding: '3px 8px'
  kpi-card:
    background: '{colors.surface}'
    border: '1px {colors.border}'
    radius: '{rounded.md}'
    value-typography: '{typography.kpi-value}'
    label-typography: '{typography.label}'
  table:
    header-typography: '{typography.label}'
    row-typography: '{typography.body}'
    row-divider: '1px {colors.divider}'
    row-hover: '{colors.surface-subtle}'
  kanban-card:
    background: '{colors.surface}'
    border: '1px {colors.border}'
    radius: '{rounded.md}'
    status-bar: '{components.card-status-bar}'
  modal:
    background: '{colors.surface}'
    border: '1px {colors.border}'
    radius: '{rounded.md}'
    max-width: 480px
    scrim: 'rgba(15,23,42,.4)'
    shadow: '0 12px 32px rgba(15,23,42,.14)'
  toast:
    background: '{colors.surface}'
    border: '1px {colors.border}'
    radius: '{rounded.md}'
    accent-bar: 3px
    position: 'bottom-right'
  drawer:
    background: '{colors.surface}'
    border-left: '1px {colors.border}'
    width: 420px
    scrim: 'rgba(15,23,42,.4)'
    shadow: '0 12px 32px rgba(15,23,42,.14)'
  tabs:
    item-typography: '{typography.nav}'
    active-color: '{colors.primary}'
    indicator: '2px {colors.primary}'
  empty-state:
    icon-tile: '{colors.surface-muted}'
    title-typography: '{typography.body-strong}'
    body-typography: '{typography.meta}'
    cta: '{components.button-secondary}'
---

# StorageHub — Design System (Control Room)

## Brand & Style

StorageHub rents lockable square meters and takes deposits for them. That is a business of commitments: a unit is either earning, promised to someone, or being turned over for the next tenant — and every screen in the product is ultimately an instrument for reading that state and acting on it. The visual identity takes this literally. It is a **technical SaaS register in the Stripe/Linear lineage**: cool slate surfaces, one indigo accent, an unbending 8px grid, tabular numerals, and status semantics (green/amber dots, a 3px left state bar) on every card. The product should feel like a well-instrumented control room, not a shop and not a brochure.

Three commitments follow from that posture. **Density over decoration** — the base type is 13px because users are comparing five unit cards or scanning a 40-row table, not skimming marketing. **One accent** — indigo `{colors.primary}` is reserved for action and selection (buttons, active nav, focus); everything chromatic beyond it is status, and status colors never moonlight as decoration. **State is always visible** — no card, row, or badge appears without its status being legible at a glance, because the entire value of the product is knowing what is available, promised, occupied, or out of service.

The shell is adaptive: one app frame, five role menus (Customer, Staff, Facility Manager, Business Ops, System Administrator). The signature marker of this is the **role chip** in the top bar — a small `{colors.primary-tint}` pill with a `{colors.primary-border}` border naming the current role — so a shared-looking screen is never mistaken for someone else's workspace. Light mode only; there is no dark surface to design for.

## Colors

The palette is a slate ramp (surface, text, border), one brand accent, and a semantic status layer. Structure stays achromatic so that the moment color appears, it means something.

**Surfaces & structure.** The app canvas is cool light grey `{colors.app-bg}`; everything interactive sits on white `{colors.surface}` cards with a 1px `{colors.border}` hairline. Two recessed tones — `{colors.surface-subtle}` and `{colors.surface-muted}` — are for chips, table hover, and empty-state tiles; they are fill-ins, never content surfaces. `{colors.border-strong}` is for controls that must stand alone without a card (secondary buttons, sort dropdowns). Inside cards, `{colors.divider}` separates rows more quietly than a full border would.

**Text ramp.** `{colors.ink}` for primary text, `{colors.ink-secondary}` for emphasis inside secondary UI (table text, count rows), `{colors.muted}` for labels and inactive nav, `{colors.faint}` for the quietest metadata (per-month suffixes, feature lines). Four steps, no more — if a fifth grey seems necessary, the hierarchy is wrong.

**Brand.** Indigo `{colors.primary}` is the only brand color and it means *act* or *selected*: primary buttons, active nav underline, focus ring, links. Its tint `{colors.primary-tint}` (border `{colors.primary-border}`, outline border `{colors.primary-outline-border}` for secondary indigo buttons on buffer cards) carries selection backgrounds and the role chip. Indigo is **not** a status — except that *Reserved* deliberately borrows it (below), because a reservation is a commitment the customer made through the system.

**Unit lifecycle status.** Every unit is in exactly one of seven states, each with a dot/badge color, a tint background, and a tint border. Green and amber come from the chosen direction; the remaining five extend the same Tailwind-adjacent family so the ramp stays coherent. Note: `status-reserved` through `status-retired` hexes (and their tints/borders) are invented beyond the D4 direction's green/amber pair.

| Status | Dot / bar | Meaning |
|---|---|---|
| Available | `{colors.status-available}` | Rentable for the requested dates — the green "go" |
| Available soon (buffer) | dot `{colors.status-buffer-bar}`, text `{colors.status-buffer}` | Rentable, but a Turnover Buffer delays the start date — amber attention, not an error |
| Reserved | `{colors.status-reserved}` | Deposit paid, waiting for check-in — indigo, a commitment |
| Rented | `{colors.status-rented}` | Occupied, in service — ink-strength slate, the steady state |
| Preparing | `{colors.status-preparing}` | Turnover in progress (cleaning after checkout) — transient sky |
| Maintenance | `{colors.status-maintenance}` | Out of service for repair — a hotter amber-orange than buffer |
| Retired | `{colors.status-retired}` | Permanently out of inventory — grey, de-emphasized |

The buffer family keeps two tones on purpose: `#B45309` text for legibility on `{colors.status-buffer-tint}`, and the brighter `{colors.status-buffer-bar}` for the 3px card bar and 7px dot, where the darker tone would read muddy. All other statuses use their main token for both dot and bar. Retired never uses black — it must recede, not alarm.

**System feedback.** `{colors.success}` (with `{colors.success-tint}`) confirms completed money movements and resolutions; `{colors.warning}` / `{colors.warning-bar}` / `{colors.warning-tint}` flag attention short of failure (blocked extensions, validation near-misses); `{colors.error}` with `{colors.error-tint}` and `{colors.error-border}` marks failed payments, blocked saves, and destructive confirmations. Note: The error tint/border and the status-tint border hexes are extensions of the direction's token set. The notification bell's unread count is always `{colors.error}` red — never indigo — so *alert* and *action* remain separate channels even at the smallest scale.

## Typography

System fonts only — `-apple-system, "Segoe UI", Roboto` sans and `ui-monospace` — because the product must render natively everywhere it is opened and hand off cleanly to Stitch without webfont baggage. The type system earns its keep through weight discipline and numeric behavior, not through a display face.

The ramp: `{typography.display}` (21px/600) for page titles, `{typography.kpi-value}` (24px/600) for dashboard numbers, `{typography.headline}` (18px/600) for section headers and modal titles, `{typography.body}` (13px) for everything readable, `{typography.meta}` (11.5px) for feature lines and helper text, `{typography.label}` (10.5px/600) for field labels and badges. Labels are sentence case, never uppercase — uppercase micro-labels belong to the industrial register this system rejected. Buttons use `{typography.button}`, nav links `{typography.nav}`; both are 13px so controls and content share one optical size.

**Numerals are the brand.** Every price, date, count, KPI, and table figure renders with `font-variant-numeric: tabular-nums` so columns of VND amounts align and deltas read instantly. Prices use `{typography.price}` (15px/600) on cards and `{typography.price-lg}` (19px/600) for totals in booking summaries and payment modals. Note: The 21/24/19px sizes extend the direction's documented 13/15px pair. Unit codes (`S-3`, `M-2`) are codes, not words — they render in `{typography.code}` mono, echoed at `{typography.code-sm}` for photo chips and filter echoes.

**VND formatting is non-negotiable:** dot-separated thousands, dong sign after the amount, always the full figure — `1.150.000 ₫`, never `1.15tr` or `₫1,150,000`. Amounts pair with a muted suffix for the unit (`/mo`, `/term`) set in `{typography.meta}`.

## Layout & Spacing

The 8px grid is the skeleton of the register. The spacing scale is `{spacing.1}` 4 / `{spacing.2}` 8 / `{spacing.3}` 12 / `{spacing.4}` 16 / `{spacing.5}` 24 / `{spacing.6}` 32 / `{spacing.7}` 48 — every gap between surfaces, every control padding, every grid gutter resolves to this scale, with the 4px half-step allowed only inside components (chip padding, icon gaps). The page gutter is `{spacing.page-x}` 24px; the top nav is `{spacing.nav-height}` 54px tall.

Content is card-first. The Browse grid is three columns at 1200px+ with a 14px gutter (grid gap sits between scale steps; 14px is the direction's documented value). Back-office surfaces are dashboard-first per the product decision: a KPI row of 3–4 cards, then a chart block, then the data table — never the table alone. Density is a feature: tables show 25 rows comfortably because rows are 40px and type is 13px.

## Elevation & Depth

Elevation is expressed as **weight of border, plus a 1px whisper of shadow** — never as floating cards. The standard card carries `1px {colors.border}` and `0 1px 2px rgba(15,23,42,.05)`: enough to seat a white surface on the grey canvas, not enough to read as "lifted." Hover states respond by darkening the border to `{colors.border-strong}`, not by growing a shadow.

Two exceptions carry the whole depth language. The primary button gets an indigo-tinted shadow (`0 1px 2px rgba(79,70,229,.35)`) so the one action that matters on a card is the visually loudest element at equal radius. Overlays (modal, drawer) earn real elevation — `{components.modal.shadow}` — over a `rgba(15,23,42,.4)` scrim, because interrupting the surface is precisely their job. Nothing else in the system may exceed these shadows; toasts sit at card depth and rely on position.

## Shapes

One corner language, applied without exception: `{rounded.md}` 8px on every surface — cards, buttons, inputs, modals, drawers, toasts, image containers, avatar tiles, the logo mark. `{rounded.sm}` 6px is for small elements whose height makes 8px look round: chips, badges, code chips, the role chip. `{rounded.full}` is reserved for the two smallest shapes in the system — the 7px status dots and the bell's unread count. No nested-radius mismatches, no pill buttons, no circles for anything larger than a dot. The consistency is the aesthetic: when every corner is the same, corners stop being information and status bars become information.

The **3px left status bar** is the system's signature shape move. An absolutely-positioned 3px bar runs the full height of a card's left edge (over the photo on unit cards, over the border on list cards), colored by the card's status via `{components.card-status-bar}` — green available, amber buffer, indigo reserved, slate rented, sky preparing, orange maintenance, grey retired. It lets a grid of cards be triaged by scanning one vertical edge, which is exactly how staff and managers read the board.

## Components

**Buttons.** Primary: `{colors.primary}` fill, white text, `{rounded.md}`, the indigo shadow — one per card or view; if two primaries compete, one is wrong. Secondary: white fill, `{colors.border-strong}` border, `{colors.ink-secondary}` text — used for the Book button on buffer cards and modal dismissals. Ghost: transparent, for table row actions and empty-state CTAs. Destructive actions use secondary geometry with `{colors.error}` text. Buttons are 36px tall (40px for page-level CTAs), `{typography.button}`, sentence case.

**Inputs.** White fill, 1px `{colors.border}`, `{rounded.md}`, 36px tall, `{typography.body}` value text. Labels sit above in `{typography.label}` `{colors.muted}` — always visible, never placeholder-only. Focus: indigo 2px ring offset from the field. Invalid: `{colors.error}` border, `{typography.meta}` message in `{colors.error}` below the field, tinted `{colors.error-tint}` for server-side blocks. Date and duration filters present as closed dropdowns, not free text.

**Cards.** The base unit of every surface: `{colors.surface}`, 1px `{colors.border}`, `{rounded.md}`, card shadow, plus the 3px left status bar whenever the card represents a unit or a task. The unit card (Browse) stacks photo with mono code chip → code + size/type chips → feature line in `{colors.faint}` → price line above a `{colors.divider}` rule → availability line with 7px dot → Book button. Management cards replace the photo with a denser spec row.

**Badges.** Status badges are the in-line form of the status bar: `{rounded.sm}`, 3px 8px padding, `{typography.label}`, tint background with matching border and text from the status ramp (e.g. Available = `{colors.status-available-tint}` / `{colors.status-available-border}` / `{colors.status-available}`). Neutral badges use `{colors.surface-muted}` + `{colors.ink-secondary}`. Never color-only: every badge carries its label.

**KPI card.** Dashboard-first surfaces lead with a row of these: label in `{typography.label}` `{colors.muted}`, value in `{typography.kpi-value}` tabular numerals, optional delta chip reusing success/error badge semantics (`+4.2%` green, `−1.1%` red), optional `{colors.faint}` period note. No sparklines inside the card — trend charts get their own block below the KPI row.

**Table.** Sticky header row in `{typography.label}` `{colors.muted}` on `{colors.surface-subtle}`; 40px rows in `{typography.body}` separated by `{colors.divider}` hairlines; hover tints the row `{colors.surface-subtle}`; zebra striping is banned. Numeric columns right-align in tabular numerals; a status column uses badges; row actions are ghost buttons revealed on the right. Mobile and >30-column cases degrade to card lists (see EXPERIENCE.md).

**Kanban card.** The staff workhorse: compact card with the 3px left bar encoding task type — Check-in `{colors.status-reserved}`, Checkout `{colors.status-preparing}`, Cleaning `{colors.status-buffer-bar}`, Support `{colors.error}`, Contract `{colors.status-rented}` — Note: this type-to-color mapping is an extension of the status ramp. Mono unit code, `{typography.meta}` customer/slot line, due chip. Cards in Done drop to 60% ink and lose the bar so the eye skips them.

**Modal.** Centered, max 480px, `{rounded.md}`, border + `{components.modal.shadow}` over the scrim. Title in `{typography.headline}`, body in `{typography.body}`, actions right-aligned (secondary left of primary). The payment modal is the canonical modal of the product; the forgot-password modal and all confirm-destruction dialogs share this anatomy. One modal deep, never stacked.

**Toast.** Bottom-right, card depth, 3px left accent bar by tone (success / warning / error / indigo-info), `{typography.body}` message, auto-dismiss around 4s with a hover-pause. Note: Position and duration are confirmed at planning. Toasts report *what happened*; the notification bell is the durable record — the two are never substitutes.

**Drawer.** Right-side overlay, 420px wide (`{components.drawer}`), same card chrome, used for reading and light editing without leaving the list behind: support ticket detail, unit edit. Note: Width is confirmed at planning. Contains title, `{typography.body}` content, and a footer with actions; complex edits still promote to a full modal or screen.

**Contract step.** The desk ritual as one panel: a document preview tile (white page card, 1px `{colors.border}`, mono contract code `CT-1042` in `{typography.code}`, parties/unit/dates/rent/deposit/policy-version lines in tabular numerals), a secondary Print button, a signed-copy capture tile (dashed `{colors.border-strong}` border, camera icon, `{colors.surface-muted}` fill) that flips to a thumbnail + Retake once a photo lands, and the primary Attach action. The parent flow's completing action — access-code handover in Check-in, addendum completion in a Contract task — stays disabled until the capture tile holds a photo. The preview is read-only everywhere in the product: contracts are drafted by the system, corrected by re-drafting, never edited in place.

**Tabs.** Text tabs matching the nav's grammar: `{typography.nav}` `{colors.muted}`, active item `{colors.primary}` with a 2px `{colors.primary}` underline. Used for report periods, task-type filters on the board, and settings groups.

**Empty state.** Per the direction's D4 variant: a 56px `{colors.surface-muted}` icon tile with `{colors.border}`, a `{typography.body-strong}` title, a `{colors.meta}` explanation that restates the facts ("0 of 42 units meet all four criteria"), mono chips echoing the active query, and one ghost or secondary CTA. Empty states always offer the next move; they never dead-end.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Show status on every unit/task surface — bar, badge, or dot with label | Hide state behind hover, drill-downs, or color-only chips |
| One indigo primary action per card or view | Competing primary buttons; indigo for decoration or status |
| Tabular numerals for every VND figure, date, and count | Proportional numerals in columns; abbreviated VND (`1.15tr`) |
| 8px radius on every surface, 6px on chips, `{rounded.full}` only for dots | Mix radii, pill buttons, or rounded cards with square inputs |
| Borders + 1px shadows; hover darkens border | Grow shadows on hover, floating-card elevation |
| Green = go, amber = attention/buffer, red = failure, slate = occupied, grey = retired | Reuse status colors as theme colors, or mint new hues per screen |
| System sans + mono codes, sentence case labels | Webfonts, uppercase micro-labels, display serifs |
| Full amount breakdown before any payment | Surprising fees without a line-item explanation |
| Light mode only | Design dark-mode variants or theming hooks |
