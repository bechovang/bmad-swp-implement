---
title: 'Story 1.4 — FE design token system "Control Room" + base primitives'
type: 'feature'
created: '2026-10-02'
baseline_commit: '3efd155a976d2c42213796a0d0174f711f097dcb'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Frontend hiện tại chỉ có scaffold mặc định (Vite + React 19 + Axios + React Query); chưa có hệ thống design tokens, chưa có các base primitives UI chuẩn theo DESIGN.md ("Control Room"), và chưa có hàm format tiền tệ VND chuẩn — khiến các màn hình tiếp theo (1.5, 1.6 và Epic 2+) có nguy cơ tự chế màu/kích thước/style không nhất quán.

**Approach:** Cài đặt toàn bộ design token system (CSS variables + TS constants + Tailwind CSS v4 @theme) bám sát DESIGN.md 100% (surface ramp 6 bậc, text ramp 4 bậc, brand indigo, 7 status lifecycle, typography 14 style, spacing 8px grid, radius md 8px / sm 6px / full 9999px, elevation whisper-shadow); dựng bộ primitives tái sử dụng trên nền headless Radix UI kết hợp Tailwind (Button, Input, Card với 3px left status bar, Badge, KpiCard, Modal, Drawer, Tabs, EmptyState, Skeleton, Table); xây dựng utility `formatMoney` (chuẩn VND `1.150.000 ₫` tabular-nums); đáp ứng a11y floor (label visible, focus ring 2px indigo, touch target ≥ 40px, focus trap modal/drawer qua Radix Dialog); verify đầy đủ bằng Vitest + RTL.

## Key Decisions

1. **Styling & Component Architecture:** Lựa chọn **Tailwind CSS v4 + Radix UI primitives**. CSS variables (`tokens.css`) định nghĩa toàn bộ giá trị chuẩn từ `DESIGN.md`; Tailwind v4 `@theme` ánh xạ các biến này vào utility classes (`bg-sh-surface`, `text-sh-ink`, `border-sh-border`, etc.). Các components phức tạp về a11y (Modal, Drawer, Tabs) sử dụng headless primitives của Radix UI (`@radix-ui/react-dialog`, `@radix-ui/react-tabs`) và được wrap / style chuẩn giao diện "Control Room".
2. **Companion Libraries Deferred Decisions:** Thống nhất danh sách companion libraries đã hoãn từ Week-1 và ghi nhận vào `README.md`:
   - Drag & Drop: `@dnd-kit` (Kanban task board Epic 3).
   - Chart library: `recharts` (Back-Office analytics dashboard Epic 7).
   - QR Scanner: `html5-qrcode` (Check-in QR scanner Epic 3).
   - Các thư viện này **chưa** cài vào `frontend/package.json` trong Story 1.4 để giữ dependency tối giản và sạch sẽ; chỉ cài khi bắt đầu story tương ứng.
3. **Scope Management:** Giữ nguyên toàn bộ spec (Keep full spec) để hoàn thiện đồng bộ tokens, format utilities và 11 UI primitives trong một story nền tảng thống nhất.

## Boundaries & Constraints

**Always:**
- Bám sát `docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md` verbatim:
  - Palette: Surface ramp 6 bậc (`app-bg` #F4F6F9, `surface` #FFFFFF, `surface-subtle` #F8FAFC, `surface-muted` #F1F5F9, `border` #E2E8F0, `border-strong` #CBD5E1, `divider` #F1F5F9); Text ramp 4 bậc (`ink` #0F172A, `ink-secondary` #334155, `muted` #64748B, `faint` #94A3B8); Brand indigo `#4F46E5` (`primary-tint` #EEF2FF, `primary-border` #E0E7FF, `primary-outline-border` #C7D2FE); 7 status semantics (Available, Buffer, Reserved, Rented, Preparing, Maintenance, Retired) đủ dot/tint/border/bar; Feedback tokens (success, warning, error: `error` #DC2626, `error-tint` #FEF2F2, `error-border` #FECACA).
  - Typography: 14 styles (display 21/600, kpi-value 24/600, headline 18/600, body 13/400, body-strong 13/600, meta 11.5/500, label 10.5/600, button 13/600, nav 13/500, price 15/600, price-lg 19/600, code 13/600 mono, code-sm 11/600 mono). Font sans hệ thống (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto...`) và monospace (`ui-monospace, SFMono-Regular, Menlo, Consolas...`).
  - Radius: `md` 8px cho mọi surface/control/modal/drawer/toast; `sm` 6px cho badge/chip/role chip; `full` 9999px chỉ dùng cho 7px status dot và notification badge count.
  - Spacing 8px grid (4, 8, 12, 16, 24, 32, 48px; `page-x` 24px, `nav-height` 54px).
  - Elevation: border 1px + shadow thì thầm `0 1px 2px rgba(15,23,42,.05)`, hover chỉ làm tối border thành `border-strong`; overlay (modal/drawer) dùng `0 12px 32px rgba(15,23,42,.14)` với scrim `rgba(15,23,42,.4)`.
- Format tiền VND (UX-DR2): bắt buộc dạng `1.150.000 ₫` (phân cách hàng nghìn bằng dấu chấm, ký hiệu `₫` đặt sau số có dấu cách, full precision không làm tròn tắt thành `tr`, luôn dùng `tabular-nums`).
- Signature moves: Card hỗ trợ 3px left status bar (`card-status-bar`) theo 7 màu status; Button primary có indigo shadow `0 1px 2px rgba(79,70,229,.35)`; strictly đúng 1 primary action trên mỗi view/card.
- A11y floor: Label luôn visible phía trên Input; focus visible ring 2px indigo; Modal và Drawer có focus trap, đóng bằng phím Escape, trả focus về trigger element khi đóng (sử dụng Radix Dialog).
- Light mode only; desktop-first (target 1200px); tiếng Anh (English-only).

**Never:**
- Không dùng webfonts (chỉ dùng system sans và ui-monospace).
- Không dark mode, không theme picker.
- Không uppercase micro-labels (labels là sentence case).
- Không zebra striping trên Table; không shadow to nổi floating cho card thường.
- Không pill buttons; không mix radius (chỉ 8px, 6px, 9999px).
- Không viết logic màn hình nghiệp vụ (để Story 1.5, 1.6 và Epic 2+).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Format tiền chuẩn | `formatMoney(1150000)` | `"1.150.000 ₫"` với CSS class hoặc style tabular-nums | — |
| Format tiền = 0 | `formatMoney(0)` | `"0 ₫"` | — |
| Format tiền số âm | `formatMoney(-50000)` | `"-50.000 ₫"` | — |
| Format tiền số lẻ/NaN/null | `formatMoney(null as any)` / `NaN` | `"0 ₫"` hoặc graceful fallback | Không throw runtime |
| Button Primary | variant="primary" | Nền #4F46E5, chữ trắng, shadow indigo, cao 36px, radius 8px | — |
| Button Secondary | variant="secondary" | Nền trắng, border-strong, chữ ink-secondary, cao 36px | — |
| Button Destructive | variant="destructive" | Nền trắng, border-strong, chữ error-red #DC2626 | — |
| Button Loading/Disabled | disabled=true | Opacity giảm, cursor not-allowed, không trigger onClick | Chặn click |
| Input chuẩn | label="Email", value="" | Label visible phía trên (10.5px/600), input cao 36px, border #E2E8F0 | — |
| Input focus | user focus vào input | Outline/ring 2px indigo #4F46E5 offset | — |
| Input lỗi validation | error="Invalid email address" | Border đổi sang error #DC2626, message error hiển thị phía dưới field | — |
| Card kèm status bar | status="available" | Card trắng, viền border, thanh 3px sát mép trái màu #059669 | — |
| Badge theo 7 status | status="buffer" | Nền #FFFBEB, viền #FDE68A, chữ #B45309, dot #F59E0B | — |
| Modal open | isOpen=true | Scrim rgba(15,23,42,.4), dialog căn giữa max-width 480px, focus trap Radix | Escape đóng modal |
| Drawer open | isOpen=true | Slide in từ mép phải 420px, scrim rgba(15,23,42,.4), focus trap Radix | Escape đóng drawer |
| Table hiển thị tiền | column numeric | Chữ căn phải (text-right), `tabular-nums`, header căn phải khớp | — |

</frozen-after-approval>

## Code Map

- `docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md` -- đặc tả visual identity gốc: token hex, typography ramp, radius, spacing, components.
- `frontend/package.json` + `frontend/vite.config.ts` -- cấu hình Tailwind CSS v4 plugin (`@tailwindcss/vite`) và cài đặt `@radix-ui/react-dialog`, `@radix-ui/react-tabs`, `clsx`, `tailwind-merge`.
- `frontend/src/tokens/tokens.css` -- định nghĩa toàn bộ CSS custom properties (`--sh-color-*`, `--sh-font-*`, `--sh-radius-*`, `--sh-spacing-*`, `--sh-shadow-*`) và ánh xạ Tailwind `@theme`.
- `frontend/src/tokens/index.ts` -- export TS constants, status color maps, token types phục vụ type safety trong code React.
- `frontend/src/lib/format.ts` -- hàm tiện ích `formatMoney(amount: number): string` chuẩn VND (`1.150.000 ₫`) + `formatUnitCode`.
- `frontend/src/lib/utils.ts` -- tiện ích `cn()` kết hợp `clsx` và `tailwind-merge`.
- `frontend/src/components/ui/Button.tsx` -- primitive button (primary, secondary, ghost, destructive; 36px / 40px).
- `frontend/src/components/ui/Input.tsx` -- primitive input với visible top label, 2px indigo ring, error state.
- `frontend/src/components/ui/Card.tsx` -- primitive card (white surface, border, whisper shadow, 3px left status bar).
- `frontend/src/components/ui/Badge.tsx` -- primitive badge (7 unit statuses + neutral, kèm dot indicator).
- `frontend/src/components/ui/KpiCard.tsx` -- primitive KPI card (label, value tabular numerals, delta chip).
- `frontend/src/components/ui/Modal.tsx` -- primitive modal (max-width 480px, centered, scrim, Radix Dialog focus trap, Esc dismiss).
- `frontend/src/components/ui/Drawer.tsx` -- primitive drawer (420px right slide-in, scrim, Radix Dialog focus trap, Esc dismiss).
- `frontend/src/components/ui/Tabs.tsx` -- primitive text tabs (Radix Tabs, nav grammar, 2px indigo underline indicator).
- `frontend/src/components/ui/EmptyState.tsx` -- primitive empty state (56px icon tile, factual title, query echo chips, CTA).
- `frontend/src/components/ui/Skeleton.tsx` -- primitive skeleton placeholder với pulse effect.
- `frontend/src/components/ui/Table.tsx` -- primitive data table (sticky header, 40px row, hover row, tabular numeric align, no zebra).
- `frontend/src/components/ui/index.ts` -- barrel export toàn bộ primitive components.
- `frontend/src/test/format.test.ts` -- unit tests cho `formatMoney`.
- `frontend/src/test/primitives.test.tsx` -- React Testing Library tests cho Button, Input, Card, Badge, Modal, Drawer, Tabs.

## Tasks & Acceptance

**Execution:**

- [x] `frontend/package.json` + `frontend/vite.config.ts` -- cài đặt và cấu hình `@tailwindcss/vite`, `tailwindcss`, `@radix-ui/react-dialog`, `@radix-ui/react-tabs`, `clsx`, `tailwind-merge`.
- [x] `frontend/src/tokens/tokens.css` -- khai báo CSS custom properties + Tailwind `@theme` (colors, typography, spacing, radius, shadows, status ramp).
- [x] `frontend/src/tokens/index.ts` -- khai báo TypeScript constants, enums/types cho Status, Variants, Roles.
- [x] `frontend/src/index.css` -- import `tokens.css`, thiết lập base styles (system font, background `#F4F6F9`, tabular numerals utility, reset box-sizing).
- [x] `frontend/src/lib/utils.ts` -- helper `cn()` cho Tailwind classes.
- [x] `frontend/src/lib/format.ts` -- hàm `formatMoney` (`1.150.000 ₫` tabular-nums) và `formatUnitCode`.
- [x] `frontend/src/components/ui/Button.tsx` -- Button primitive (primary/secondary/ghost/destructive, sizes 36px/40px, loading, disabled).
- [x] `frontend/src/components/ui/Input.tsx` -- Input primitive (top label visible, error message, focus ring, helper text).
- [x] `frontend/src/components/ui/Card.tsx` -- Card primitive (border, whisper shadow, 3px left status bar cho 7 status).
- [x] `frontend/src/components/ui/Badge.tsx` -- Badge primitive (7 status variants + neutral, dot indicator).
- [x] `frontend/src/components/ui/KpiCard.tsx` -- KpiCard primitive (label, tabular-nums value, trend delta chip).
- [x] `frontend/src/components/ui/Modal.tsx` -- Modal primitive (480px, scrim, actions right-aligned, Radix Dialog focus trap, a11y Esc/close).
- [x] `frontend/src/components/ui/Drawer.tsx` -- Drawer primitive (420px right-side, scrim, Radix Dialog focus trap, a11y Esc/close).
- [x] `frontend/src/components/ui/Tabs.tsx` -- Tabs primitive (Radix Tabs, nav grammar, 2px indigo underline).
- [x] `frontend/src/components/ui/EmptyState.tsx` -- EmptyState primitive (56px icon tile, factual description, query chips, CTA).
- [x] `frontend/src/components/ui/Skeleton.tsx` -- Skeleton primitive (layout matching, subtle shimmer).
- [x] `frontend/src/components/ui/Table.tsx` -- Table primitive (sticky 40px header, numeric right-align tabular, row hover, no zebra).
- [x] `frontend/src/components/ui/index.ts` -- barrel export các UI primitives.
- [x] `frontend/src/test/format.test.ts` + `frontend/src/test/primitives.test.tsx` -- unit & component tests cho utils và primitives.
- [x] `README.md` -- cập nhật trạng thái các quyết định FE (Component library: Tailwind v4 + Radix UI + Control Room Primitives; dnd: @dnd-kit; chart: recharts; qr: html5-qrcode).

**Acceptance Criteria:**

- Given DESIGN.md token specifications, when inspecting tokens, then CSS variables & TS constants provide the exact values for 6 surface levels, 4 text levels, brand indigo, 7 status semantics (Available, Buffer, Reserved, Rented, Preparing, Maintenance, Retired), 14 typography ramps, 8px spacing grid, and 8px/6px/9999px radii.
- Given `formatMoney(1150000)`, then output is strictly `"1.150.000 ₫"` with dot separator and dong symbol following space.
- Given Button component, when rendered with `variant="primary"`, then it has #4F46E5 background, white text, 8px radius, 36px height, and indigo shadow; destructive variant renders with error text.
- Given Input component, then label is always visible above the field in `typography.label`, focus ring is 2px indigo, and error message renders below in `typography.meta` error color.
- Given Card component with `status="available"`, then a 3px bar runs the full height of the left edge in green `#059669`.
- Given Modal and Drawer components, when opened, then background is dimmed with `rgba(15,23,42,.4)`, keyboard focus is trapped within the container, and pressing `Escape` closes the overlay and restores focus to the trigger.
- Given `npm test` inside `frontend/`, then Vitest passes all format and primitive component tests without errors.
- Given `npm run build` inside `frontend/`, then TypeScript compilation and Vite build succeed with zero errors.

## Implementation Notes

- Configured Tailwind CSS v4 via `@tailwindcss/vite` plugin and installed `@radix-ui/react-dialog`, `@radix-ui/react-tabs`, `clsx`, `tailwind-merge`.
- Created authoritative token system in `frontend/src/tokens/tokens.css` with exact values from `DESIGN.md` (6 surface ramp, 4 text levels, brand indigo, 7 unit lifecycle statuses, feedback tokens, 8px grid spacing, 8px/6px/9999px radii, whisper/button/overlay shadows, scrim) mapped into Tailwind `@theme`.
- Created TypeScript token definitions and status helpers in `frontend/src/tokens/index.ts`.
- Implemented `formatMoney` conforming strictly to VND format (`1.150.000 ₫`, tabular numerals) and `formatUnitCode` in `frontend/src/lib/format.ts`.
- Implemented 11 base UI primitives in `frontend/src/components/ui/` (`Button`, `Input`, `Card` with 3px left status bar, `Badge`, `KpiCard`, `Modal`, `Drawer`, `Tabs`, `EmptyState`, `Skeleton`, `Table`) plus `RoleChip` helper.
- Updated `README.md` recording settled frontend decisions.

## Spec Change Log

## Review Triage Log

- `frontend/src/lib/format.ts:19-23` -- [low] Negative zero: small fractions between -0.5 and 0 produced "-0 ₫". Fixed by checking `rounded < 0`. (patch)
- `frontend/src/lib/format.ts:15` -- [low] Non-finite numbers: `Infinity` passed `Number.isNaN`. Fixed with `!Number.isFinite(amount)`. (patch)
- `frontend/src/lib/format.ts:32-35` -- [low] `formatUnitCode` non-string handling. Fixed with `typeof code === 'string'`. (patch)
- `frontend/src/components/ui/Badge.tsx:93` -- [medium] Logical OR `{children || defaultLabel}` swallowed numeric zero. Fixed with nullish coalescing `??`. (patch)
- `frontend/src/tokens/index.ts:259` -- [medium] `normalizeStatus` defaulted unknown statuses to 'available' (green). Fixed to return `undefined` so unknown status doesn't produce false green indicators. (patch)
- `frontend/src/components/ui/Modal.tsx:118` -- [medium] Non-element trigger caused Radix Slot error. Fixed with `React.isValidElement(trigger)` check. (patch)
- `frontend/src/components/ui/Drawer.tsx:126` -- [medium] Non-element trigger in Drawer. Fixed with `React.isValidElement(trigger)` check. (patch)
- `frontend/src/components/ui/Button.tsx:40` -- [false] Touch target 40px vs 36px: 36px is explicit design specification in DESIGN.md. (rejected)
- `frontend/src/components/ui/Modal.tsx` & `Drawer.tsx` animations -- [medium] Inert Tailwind v3 animation classes. Replaced with clean CSS keyframes and transitions. (patch)
- `frontend/src/components/ui/Modal.tsx:121` -- [medium] Missing Radix Dialog accessible title when title omitted. Fixed with visually hidden title fallback for screen readers. (patch)
- `frontend/src/components/ui/Button.tsx` -- [medium] Missing default `type="button"`. Fixed with `type = 'button'` default prop. (patch)
- `frontend/src/components/ui/*` -- [low] Hardcoded pixel border radii. Replaced with `rounded-sh-md` and `rounded-sh-sm` token utility classes. (patch)
- `frontend/src/tokens/tokens.css` -- [low] Spacing scale omitted from `@theme`. Mapped `--spacing-sh-1`..`7`, `pageX`, `navHeight`. (patch)
- `frontend/src/tokens/tokens.css` -- [low] Hardcoded scrim in overlays. Exposed `--color-sh-scrim` in `@theme` and used `bg-sh-scrim`. (patch)
- `frontend/src/components/ui/RoleChip.tsx` -- [low] RoleChip helper component created and exported for Story 1.5. (patch)
- `frontend/src/components/ui/Toast` -- [false] Toast primitive is explicitly deferred to Story 1.6 (`1-6-toast-bell-notification-center`). (rejected)
- `frontend/src/components/ui/Input.tsx` -- [low] Missing 2px ring offset. Added `focus:ring-offset-1`. (patch)
- `frontend/src/components/ui/Table.tsx` -- [low] Table header height mismatch (`h-[36px]` vs 40px). Updated `TableHead` to `h-[40px]`, added `containerClassName`. (patch)
- `frontend/src/components/ui/Button.tsx` -- [low] Missing loading accessibility. Added `aria-busy={isLoading}` and `<span className="sr-only">Loading...</span>`. (patch)
- `frontend/src/components/ui/Modal.tsx` & `Drawer.tsx` -- [medium] Missing header close button. Added accessible header close ("✕") button with `aria-label="Close"`. (patch)
- `frontend/src/test/primitives.test.tsx` (Verification Gap) -- [medium] Closed overlay state `open={false}` was untested. Added tests verifying modal and drawer do not render when closed. (patch)
- `frontend/src/test/primitives.test.tsx` (Verification Gap) -- [medium] KpiCard delta classes and negative/neutral deltas unverified. Added assertions for classes and negative/neutral deltas. (patch)
- `frontend/src/test/primitives.test.tsx` (Verification Gap) -- [medium] Dot indicator colors for 4 lifecycle statuses unverified. Added assertions for all 7 statuses. (patch)

## Design Notes

- Tabular numerals: Sử dụng CSS `font-variant-numeric: tabular-nums` cho mọi thẻ hiển thị tiền, ngày tháng, mã kho, số đếm.
- 3px left status bar: Sử dụng pseudo-element `::before` hoặc absolute positioned bar `w-[3px] left-0 top-0 bottom-0` để đảm bảo thanh trạng thái gắn liền với card mà không làm dịch chuyển layout nội dung bên trong.
- Radix UI accessible foundations: Tận dụng headless Dialog và Tabs của Radix để đảm bảo 100% WAI-ARIA compliance (focus trap, ARIA attributes, key navigation), phủ lớp visual identity Control Room bằng Tailwind CSS v4.

## Verification

**Commands:**

- `cd frontend && npm test` -- expected: PASS toàn bộ test Vitest (formatMoney, Button, Input, Card, Badge, Modal, Drawer, Tabs).
- `cd frontend && npm run build` -- expected: `tsc -b && vite build` hoàn thành với exit code 0, không có lỗi kiểu TypeScript.
- `cd frontend && npm run lint` -- expected: `oxlint` sạch sẽ, 0 lỗi.
