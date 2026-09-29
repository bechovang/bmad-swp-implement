# StorageHub — Stitch Prompt Pack

29 screens · 9 batches · design direction "Control Room"

## Cách dùng (3 bước)

1. Mở **https://stitch.withgoogle.com** → chọn **Web** mode → tạo chat mới **cho mỗi batch**.
2. Tin nhắn đầu tiên của batch: dán nguyên văn **Design Direction block** (mục dưới).
3. Với mỗi màn: dán prompt tương ứng ("Generate screen: ..."). Stitch sinh 1 screen/lần — review, chỉnh bằng chat, rồi sang màn kế.
   - Free tier giới hạn số lệnh sinh mỗi ngày → làm hết batch nào hay batch đó, batch độc lập nhau.
   - Sau khi ưng: export/copy output, lưu vào thư mục `mockups/` cạnh file này (đặt tên `F1-browse-units.html`...). Hai file `DESIGN.md` + `EXPERIENCE.md` là hợp đồng thắng mọi mock nếu xung đột.

---

## 🎨 Design Direction block (dán đầu mỗi batch)

```
Design direction for every screen in this app, called "StorageHub" — a self-storage
rental web app (English UI, Vietnam market). Technical SaaS register, Stripe/Linear
lineage. LIGHT MODE ONLY. Desktop web, 1200px canvas.

Palette: app background #F4F6F9; white cards on 1px #E2E8F0 borders with a very
subtle shadow (never floating cards); text ink #0F172A, secondary #334155, muted
labels #64748B. ONE accent: indigo #4F46E5 — primary buttons, active nav, focus
rings only (tint #EEF2FF for selection backgrounds). Status semantics, never
decoration: green #059669 = available/go; amber #F59E0B bar + #B45309 text =
attention/turnover buffer; indigo = Reserved; slate #334155 = Rented; sky #0284C7 =
Preparing; orange #C2410C = Maintenance; grey #94A3B8 = Retired; red #DC2626 =
errors only. Status shown as tinted badge + text label, never color-only.

Typography: system sans; 13px base, 18px section headers, 21px page titles,
10.5px sentence-case labels; monospace for unit codes (S-3, M-2) and reservation
codes (BK-1042); tabular numerals for ALL numbers. Money is VND, dot-separated,
dong sign after: "1.150.000 ₫" — never abbreviated.

Shape & layout: 8px radius on every surface, 6px on chips/badges; 8px spacing
grid; top bar 54px with logo, nav links, small indigo role chip (e.g. "Customer"),
bell icon with red unread badge, user avatar. Signature element: a 3px colored
status bar on the LEFT EDGE of every card. Dense, calm, instrument-panel feel.
```

---

## 📦 Batch 1 — Auth + Flow 1: Đặt kho (6 screens)

### 1.1 Login
```
Generate screen: Login. Centered white card (max 400px) on the light grey canvas.
StorageHub logo mark + wordmark above the card. Fields with visible labels:
Email, Password (36px inputs, 8px radius). Primary indigo button "Sign in" full
width. Text link "Forgot password?" opens a modal ON this screen: the modal
(email field, secondary "Send reset link", confirmation copy "If an account for
that email exists, a reset link has been sent."). Below the card, quiet meta line:
"Staff and manager accounts are provisioned by administration." and link
"New here? Create an account".
```

### 1.2 Register
```
Generate screen: Register (customer self-signup). Same centered-card layout as
Login but taller: fields Full name, Phone, Email, Password, Confirm password
with helper text, and an "I agree to the Terms" checkbox. Primary button
"Create account". Meta note: "Self-registration is for customers. Storage staff
and managers receive accounts from administration." Success lands on Browse Units.
```

### 1.3 Browse Units ⭐ flagship
```
Generate screen: Browse Units — the customer's default landing. Top bar: logo,
nav "Browse Units" (active, indigo underline) · My Rentals · Support, indigo role
chip "Customer", bell with red badge "3", avatar "Lan". Below: a filter bar card
with closed dropdowns Type (Indoor/Outdoor/Climate) · Size (3/5/8/12 m²) ·
Start date (Oct 3, 2026) · Duration (3 months) + primary "Search" button.
Results row: count chip "6 units available · live" and a sort dropdown
"Price: low to high". Then a 3-column unit card grid. Each card: photo area
(CSS-gradient placeholder) with a mono code chip, size + type chips, one feature
line, price with "/mo" suffix, availability line with 7px status dot, Book
button; and the signature 3px LEFT status bar. Cards:
S-1 · 3 m² · Indoor · 450.000 ₫/mo · Available Oct 3 (green bar)
S-3 · 5 m² · Indoor · 345.000 ₫/mo · Available Oct 3 (green)
M-2 · 8 m² · Outdoor · 720.000 ₫/mo · Available Oct 3 (green)
M-5 · 8 m² · Climate · 890.000 ₫/mo · "Only 1 left" tag on photo (green)
L-1 · 12 m² · Indoor · 1.150.000 ₫/mo · Available Oct 3 (green)
L-3 · 12 m² · Climate · 1.280.000 ₫/mo · "Available Oct 5 · cleaning buffer"
(amber bar, secondary outline Book button instead of indigo).
All prices tabular numerals.
```

### 1.4 Unit Detail
```
Generate screen: Unit Detail for Unit S-3, customer role, breadcrumb
"Browse Units / S-3". Two-column: left = large photo block with mono chip "S-3"
+ 3 small thumbnails; right = spec card with label:value rows (Size 5 m²,
Type Indoor, Floor 2, Access "Personnel door", Security "24/7 PIN + CCTV",
Depot "Tân Bình"). Below specs a price-breakdown card: line items
"Rent 345.000 ₫/mo × 3 months = 1.035.000 ₫", divider, "Deposit (10%,
refundable) due now: 103.500 ₫" in larger price type, then a full-width primary
button "Reserve Unit". Availability line with green dot "Available from Oct 3".
```

### 1.5 Booking Summary
```
Generate screen: Booking Summary (review before paying), customer role.
Header "Review your booking". Left column (60%): booking recap card — unit chip
S-3 + "5 m² · Indoor, Tân Bình Depot", dates "Oct 3 → Jan 3 (3 months)", then a
line-item table: "Rent 345.000 ₫/mo × 3 = 1.035.000 ₫", any policy surcharge
lines, divider, bold "Total 1.035.000 ₫", own line "Deposit (10%, refundable)
due now: 103.500 ₫" with a small green "refundable" badge. Right column (40%):
sticky summary card repeating Total + Deposit and a primary button
"Pay deposit 103.500 ₫", quiet meta "You will pay the remaining rent at check-in."
Nothing appears at payment that doesn't appear here.
```

### 1.6 Payment Modal (mock gateway) — 3 states
```
Generate screen: Payment Modal over a dimmed Booking Summary (scrim). Centered
modal, max 480px: header "Pay deposit — 103.500 ₫" in large price type. Method
segmented control with three options: "Card" (active — mock form: card number,
expiry, CVC with inline validation), "MoMo" (phone + OTP step), "VNPay QR"
(QR tile with countdown "4:58"). Primary button "Pay 103.500 ₫", ghost "Cancel".
Tiny meta: "Simulated gateway — no real charge."
Then two more variants of the same modal:
SUCCESS state — green check tile, "Deposit paid — 103.500 ₫. Unit S-3 is
reserved for you until check-in.", actions "View rental" (primary) + "Done".
FAIL state — red error tile, "Payment failed. No money was taken. Retry with
Card, or switch to MoMo / VNPay QR.", actions "Retry" (primary) +
"Switch method" (secondary).
```

---

## 📦 Batch 2 — Flow 2: Check-in & bàn giao (5 screens)

### 2.1 Task Board (Kanban) ⭐ staff landing
```
Generate screen: Task Board — staff default landing. Top bar: logo, nav
"Tasks" (active) · Support, role chip "Staff", bell, avatar "Minh". Below title
"Today · Morning shift": type filter tabs (All · Check-in · Checkout · Cleaning ·
Support) with counts, then a 3-column kanban: To do (3), In progress (2), Done (2),
column counts in headers. Cards are compact with the 3px LEFT bar encoding task
type, mono unit code, meta line, due chip:
"Check-in · BK-1042 · Lan Nguyen · 09:00" (indigo bar)
"Checkout · RT-0871 · Unit M-2 · 14:00 · inspect" (sky bar)
"Cleaning · Unit S-1 · 16:00 · after checkout" (amber bar)
"Support · SR-0032 · sticky door · Unit S-4" (red bar, small "Escalate" ghost
button). Done cards render muted at 60% ink without the bar. One in-progress
Check-in card shows ◀ ▶ Move buttons (keyboard alternative to drag).
```

### 2.2 Check-in Task (staff)
```
Generate screen: Check-in Task detail, staff role, back link to board.
Header "Check-in · BK-1042" with indigo badge "To do". Section 1 — code search:
mono input "BK-1042" + "Verify" button; validation result card with green check:
"Reservation valid. Deposit 103.500 ₫ confirmed. Customer: Lan Nguyen.
Unit S-3 · 5 m² · Indoor." Section 2 — rent due card: "Remaining rent
1.035.000 ₫ collected at desk", primary button "Collect payment 1.035.000 ₫"
(opens Payment Modal). Section 3 — access handover (locked/disabled state):
"Access code issued after payment", with a placeholder that becomes a large
mono code. Footer: "Complete check-in" primary, disabled with helper text
"Payment and handover required first".
```

### 2.3 My Rentals
```
Generate screen: My Rentals, customer role. Title "My rentals". Vertical list of
wide cards (not a table): each card has 3px left bar, unit chip, term, status
badge, and trailing action buttons.
Active: "S-3 · 5 m² · Indoor" · "Oct 3 → Jan 3" · slate badge "Rented" ·
ghost buttons "Extend" · "Request checkout" · "New support".
Upcoming: "M-5 · 8 m² · Climate" · "starts Oct 5 (cleaning buffer)" · indigo
badge "Reserved" · ghost "Check-in pass" + note "Pay remaining rent at the desk".
Past: "S-1 · 3 m²" · "closed Aug 30" · grey badge "Closed" · meta
"Refund 63.500 ₫ after damage fee 40.000 ₫".
```

### 2.4 Rental Detail
```
Generate screen: Rental Detail — the customer's home base, customer role.
Header row: mono chip "S-3", title "Unit S-3 · 5 m² · Indoor · Tân Bình Depot",
slate status badge "Rented", term "Oct 3 → Jan 3". Card grid: (1) Deposit card —
"Held: 103.500 ₫" with meta "10% of rent · refundable at checkout · settled by
policy"; (2) Payment history card — receipt rows with dates: "Deposit 103.500 ₫
· paid Oct 1", "Rent 1.035.000 ₫ · paid Oct 3", "Extension 690.000 ₫ · paid
later", each row mono amounts, tabular; (3) Access card — "Access code sent to
your notifications" with a "View code" secondary button. Action row at bottom:
primary "Extend rental", secondary "Request checkout", ghost "New support
request".
```

### 2.5 Check-in pass (customer)
```
Generate screen: Check-in pass, customer role, reached from Rental Detail.
Purpose: show at the storage depot desk. Hero: huge monospace reservation code
"BK-1042" (very large, centered, on a white card with subtle border), above it
label "Your reservation code". Below, a numbered plain-English steps card:
"1. Show this code at the Tân Bình depot desk. 2. Pay the remaining rent
(1.035.000 ₫) — card, MoMo or VNPay QR. 3. Receive your access code and move in."
Meta row: "Unit S-3 · Oct 3, from 09:00" and a quiet note "Bring your ID."
```

---

## 📦 Batch 3 — Flow 3: Checkout / trả kho (2 screens)

### 3.1 Checkout Task (staff) ⭐ climax screen
```
Generate screen: Checkout Task detail, staff role. Header
"Checkout · RT-0871 · Unit M-2 · Lan Nguyen" with sky badge "In progress".
Section 1 — receive checklist: checkboxes "Unit emptied by customer",
"Key / access card returned". Section 2 — condition inspection card: rows
Walls / Door / Floor / Cleanliness, each with an OK–Issue toggle; the Walls row
is set to "Issue". Section 3 — damage fee card (appears because an issue was
flagged): amount input "40.000 ₫" + REQUIRED reason textarea with label
"Reason (required)" and helper "Recorded on the settlement receipt the customer
sees"; the reason field is empty with an inline error "A reason is required
before settlement." Section 4 — settlement preview card, the visual centerpiece:
"Held deposit 103.500 ₫ − damage fee 40.000 ₫ = Refund 63.500 ₫ to Lan",
amounts in large tabular type, then "Close rental" primary button and meta
"Unit moves to Preparing; a Cleaning task is created."
```

### 3.2 Checkout Request (customer)
```
Generate screen: Checkout Request, customer role, from Rental Detail.
Title "Request checkout". Step 1 card: move-out date — calendar-style date
picker with selectable range, chosen date circled in indigo. Step 2 card:
"What happens to your deposit" — three plain-words rows with icons:
"No issues → full refund of 103.500 ₫", "Damage or fees → deducted, remainder
refunded", "Fees exceed deposit → you pay the difference" — meta line
"The staff inspection at the desk decides; you get an itemized receipt either
way." Footer: primary "Submit request", meta "Staff will confirm your slot."
```

---

## 📦 Batch 4 — Flow 4: Chính sách & doanh thu (3 screens, Business Ops)

### 4.1 Business Overview ⭐ dashboard-first
```
Generate screen: Business Overview — Business Ops default landing. Top bar:
logo, nav "Overview" (active) · Policy · Reports, role chip "Business Ops",
bell, avatar "Hằng". Dashboard-first: row of 4 KPI cards (label, big tabular
value, delta chip): "Revenue (Q3) 37.450.000 ₫ +4.2% green",
"Deposits held 4.890.000 ₫", "Surcharges 1.240.000 ₫ +11.8% green",
"Utilization 87% −1.1% red". Below: a wide chart card "Revenue by month"
(bar chart, 6 months, indigo bars, tabular axis labels). Below that: a compact
table "Recent policy versions": columns Version · Effective · Changed by ·
Status — rows "v3 · Oct 1 · Hằng · Active (green badge)", "v2 · Jul 1 · Hằng ·
Superseded (grey badge)". KPI cards are clickable (subtle hover).
```

### 4.2 Policy Management ⭐ validation climax
```
Generate screen: Policy Management, Business Ops role. Title row: "Policy" +
version stamp meta "Rental Policy v3 · effective Oct 1" + primary "Save policy"
+ ghost "Discard changes". At the TOP of the content, a red-tinted validation
banner (the point of this screen): error icon + "Policy not saved.
Late-checkout fee 15% exceeds the 10% cap in Rental Policy v3. Fix the value,
or edit the cap first." Below: editable rules table — group header "Base rent
per month" with rows S · Indoor · 345.000 ₫ / S · Outdoor · 380.000 ₫ /
M · Climate · 890.000 ₫ / L · Climate · 1.280.000 ₫; group "Surcharges" with
rows "Late checkout · 15% (cell flagged red with error border)" /
"Damage handling · at desk, reason required" / "Turnover buffer · 2 days".
Cells are inline-editable (one cell shows an active edit state with indigo
focus ring); numeric columns right-aligned tabular.
```

### 4.3 Reports
```
Generate screen: Reports, Business Ops role. Title "Reports". Text tabs
"Revenue · Deposits · Surcharges · Occupancy" (Revenue active with 2px indigo
underline). Period selector: preset chips "This month · Last month · This
quarter" ("This quarter" active) + "Custom range" ghost. Main: chart card
"Revenue — this quarter" (line chart with tabular axis) above a data table
with columns Month · Rent · Deposits · Surcharges · Total (VND right-aligned
tabular; e.g. row "Sep · 9.860.000 ₫ · 1.240.000 ₫ · 310.000 ₫ ·
11.410.000 ₫"). Top-right of the table header: secondary button "Export CSV".
```

---

## 📦 Batch 5 — Flow 5: Vận hành cơ sở (5 screens, Facility Manager)

### 5.1 Facility Overview
```
Generate screen: Facility Overview — Facility Manager default landing. Top bar:
logo, nav "Overview (active) · Units · Staff & Shifts · Operations · Activity
Log · Escalations", role chip "Facility Manager", bell with red badge "1",
avatar "Tuấn". KPI row: "Occupancy 87% (drills to Units)" · "Units in service
41 / 42" · "Tasks today 12" · "Open escalations 1 (red)". Chart card
"Units by status" — horizontal stacked bar with the status ramp colors and a
legend (Available 5 · Reserved 3 · Rented 29 · Preparing 2 · Maintenance 1 ·
Retired 2). Below: compact table "Today's turnover queue": columns
Unit · From → To · Task · Due — rows "M-2 · Rented → Preparing · Checkout ·
14:00", "S-1 · Preparing → Available · Cleaning · 16:00".
```

### 5.2 Unit Management + edit drawer ⭐
```
Generate screen: Unit Management, Facility Manager role, WITH the edit drawer
open on the right (420px wide, over a dimmed table). Main: filter chips
(All · Available · Reserved · Rented · Preparing · Maintenance · Retired) and
a dense table — columns Code (mono) · Size · Type · Zone · Status (badge) ·
Active link · Last activity: rows "S-3 · 5 m² · Indoor · A2 · Rented (slate) ·
BK-1042 / RT pending · Oct 1", "M-2 · 8 m² · Outdoor · B1 · Preparing (sky) ·
RT-0871 · today 14:00", "L-3 · 12 m² · Climate · C3 · Maintenance (orange) ·
— · Sep 28". Drawer "Edit Unit M-2": spec fields (size, type, zone), then
action list: secondary "Merge with adjacent unit", "Set maintenance",
"Retire unit" (red text), and a "Fix status" section with a status select +
REQUIRED reason textarea + helper "Logged to Activity Log with your name".
Drawer footer: "Save changes" primary. Guard note card in drawer:
"No active rental or reservation blocks maintenance on M-2."
```

### 5.3 Staff & Shifts
```
Generate screen: Staff & Shifts, Facility Manager role. Two zones. Left (40%):
"Roster" list — rows with avatar, name, role, zones: "Minh Nguyễn · Staff ·
Zones A, B", "Mai Trần · Staff · Zone C", "Hùng Lê · Senior staff · All zones".
Right (60%): "Schedule · this week" grid — columns Mon–Sun, rows = staff; cells
hold shift chips "Morning · Zone B" / "Afternoon · Zone C" (subtle tinted
chips). ONE cell is highlighted with a red border: the conflict the manager just
triggered, with an inline banner above the grid: "Minh is already assigned
Morning shift, Zone B on Oct 12. Choose another staff member or another shift."
Top-right: primary "New assignment" opening an inline form row
(staff × zone × shift × date) with a disabled "Save" and helper text
"Conflict check runs on save."
```

### 5.4 Operations Monitor
```
Generate screen: Operations Monitor — live view, Facility Manager role. Three
stacked cards on a 2-column grid. Card "Today's tasks" (60% width): compact
feed rows with task-type colored dots — "Check-in BK-1042 · Minh · in progress
(amber)", "Checkout RT-0871 · unassigned · 14:00", "Cleaning S-1 · queued ·
16:00". Card "Turnover buffer queue" (40%): rows "M-2 · cleaning · ready
~17:30", "S-1 · cleaning · ready ~18:10" with meta "not bookable until done".
Card "Escalations" (full width, red left bar): "SR-0035 · flooding · Unit M-2 ·
escalated by Minh · awaiting severity" + secondary "Open in inbox". Header has
a quiet "live" indicator dot (green pulse).
```

### 5.5 Activity Log
```
Generate screen: Activity Log, Facility Manager role. Title row + filter chips
by entity (All · Units · Rentals · Staff · Policy). Append-only table, columns
Time · Actor · Entity · Action · Change · Reason:
"Oct 1 · 14:22 · Tuấn · M-2 · Status · Rented → Preparing · 'Customer moved
out Oct 1; status not updated at desk.'"
"Oct 1 · 09:05 · Minh · BK-1042 · Check-in · Reserved → Rented · —"
"Sep 30 · 16:40 · Tuấn · S-1+S-2 · Merge · 2 units → M-6 (8 m²) · 'Restructure
zone A'."
Change column renders from → to with small arrows; no row actions, no edit —
meta note under title: "Append-only. Every status change carries an actor and
a reason." Bottom: pagination "1–25 of 214".
```

---

## 📦 Batch 6 — Flow 6: Gia hạn theo ngày (1 screen)

### 6.1 Extend
```
Generate screen: Extend rental, customer role, from Rental Detail. Header
"Extend rental · Unit S-3" with meta "current checkout Oct 17". Card 1 —
calendar picker for the new checkout date: selectable dates in indigo when
valid; a BLOCKED range Oct 19+ marked with red diagonal/hatched dates and a
legend "Already reserved for the next customer"; Oct 18 selected with an amber
boundary marker. Below the calendar, an amber-tinted warning banner (the point
of this screen): "Can't extend to Oct 20 — M-2 has a reservation starting
Oct 19. Latest possible checkout is Oct 18. Pick another date." Card 2 —
extension summary: "New checkout Oct 18 (+1 day)" · "Extension fee 690.000 ₫
due now" in large price type · meta "Your deposit stays held until checkout."
Footer: primary "Pay extension 690.000 ₫" (opens the shared Payment Modal),
ghost "Cancel".
```

---

## 📦 Batch 7 — Flow 7: Hỗ trợ & sự cố + Notification Center (5 screens)

### 7.1 New Support Request
```
Generate screen: New Support Request, customer role. Title "Report an issue".
Card 1 — "Which unit?": shows the customer's rented unit as a selectable chip
row: "S-3 · 5 m² · Indoor" (selected, indigo tint). Card 2 — "What happened?":
incident type select (Door / Access code / Water leak / Power / Other) with
"Door" chosen, and a description textarea with label + counter, filled with
sample text "The roll-up door sticks halfway and won't close fully."
Footer: primary "Submit request", meta "Routed to the on-shift staff for your
unit. You'll get updates in Notifications."
```

### 7.2 Support List + detail drawer
```
Generate screen: Support List, customer role, WITH the detail drawer open on
the right. Main: list rows with 3px left bars and status badges:
"SR-0032 · Door · Unit S-3 · Oct 2" — green badge "Resolved";
"SR-0035 · Water leak · Unit M-2 · Oct 4" — amber badge "In progress" with meta
"Escalated to Facility Manager";
"SR-0030 · Access code · Unit S-3 · Sep 12" — grey badge "Closed".
Drawer "SR-0032 · Door · Unit S-3": vertical thread — customer message (the
sticky-door text), staff note "Hinge replaced; tested 3 cycles." with avatar +
timestamp, and a resolution banner (green check): "Resolved — door hinge
replaced by site staff. See ticket for details."
```

### 7.3 Support Ticket (staff)
```
Generate screen: Support Ticket detail, staff role, back link to board.
Header "SR-0032 · Door" with red badge "Support" and meta
"Unit S-4 · Mai Trần · reported 08:40". Card 1 — customer report: quoted text
"Sticky door, won't close fully", photo placeholder chip. Card 2 — context:
unit chip, rental term, last check-in note. Card 3 — staff note textarea with
label "Resolution note (required to resolve)". Footer actions: primary
"Resolve with note" + secondary "Escalate to Facility Manager"; under Escalate,
quiet red meta: "Escalation requires a note explaining what you tried."
```

### 7.4 Escalation Inbox ⭐ severity climax
```
Generate screen: Escalation Inbox, Facility Manager role. Left (40%): queue
list — "SR-0035 · Water leak · M-2 · escalated 09:12 by Minh" (red bar,
selected) and "SR-0029 · Power · L-1 · escalated Sep 30" (muted, resolved).
Right (60%): open ticket card "SR-0035 · Water leak · Unit M-2": thread —
customer: "Water pooling at the unit entrance after rain."; Minh:
"Sump pump failed; beyond on-site repair. Customer needs a unit."
Severity decision panel (the centerpiece) — two clear options as large
selectable cards: (1) red-tinted "Mark severe — move M-2 to Maintenance and
relocate the customer to M-5. This closes the rental and starts turnover."
(selected, with checklist preview: "M-2 → Maintenance", "Relocation offer:
M-5 · 8 m² · Climate", "Repair task created") and (2) neutral "Return to staff
with guidance" with a guidance textarea. Footer: primary "Confirm severity
decision" + meta "The customer is notified at each turn."
```

### 7.5 Notification Center
```
Generate screen: Notification Center, customer role — the bell dropdown panel
open from the top bar (or a full page with the same anatomy). Header
"Notifications" + ghost "Mark all read". Grouped by day with day labels.
Unread entries have an indigo dot and slightly tinted row; every entry has a
small category chip and deep-links (chevron):
Today — "Deposit paid — 103.500 ₫. Unit S-3 reserved." (money chip);
"Resolved — door hinge replaced by site staff." (support chip, read);
Yesterday — "Rental active. Access code sent." (money, read);
"Escalated to Facility Manager — flooding, Unit M-2." (alert chip, unread);
Older — "Extension paid — 690.000 ₫. New checkout date: Oct 18."
Bell in the top bar shows the red unread count matching the unread rows.
```

---

## ✅ Checklist sau khi Stitch sinh màn

- [ ] Mọi card/kanban có **3px status bar trái** + badge chữ (không chỉ màu)
- [ ] Tiền VND đầy đủ `1.150.000 ₫`, số tabular, KHÔNG viết tắt
- [ ] Mỗi card/view chỉ **một** nút indigo primary
- [ ] Microcopy đúng giọng spine (xem EXPERIENCE.md → Voice and Tone) — không "🎉", không jargon
- [ ] Top bar có **role chip** đúng vai trò của màn
- [ ] Unit codes / mã đặt chỗ dùng **monospace**
- [ ] Sai khác với DESIGN.md/EXPERIENCE.md → sửa bằng chat Stitch, hoặc ghi chú vào `mockups/NOTES.md`

*Lưu output vào `mockups/` (tạo cạnh file này), đặt tên `F<n>-<slug>.html`. Hai spine là hợp đồng thắng mọi mock.*

---

## 📦 Batch 8 — Hợp đồng (CT-) — edits 2026-09-14 (edit_screens, không sinh màn mới trừ F2-02)

Quyết định chốt: auto-draft hoàn toàn từ booking + Rental Policy version · in tại check-in → KH ký → staff chụp
ảnh upload → mới cấp access code · gia hạn = phụ lục giấy ký tại quầy trong 7 ngày (ngày mới hiệu lực ngay sau
thanh toán) · checkout không ký (settlement receipt là biên bản). Chi tiết: `EXPERIENCE.md` → Contract lifecycle.

### 8.1 F2-02 Check-in Task — chèn Step "Contract & Signature Verification"
```
Insert a new step panel between rent payment and access-code handover: read-only
contract preview as a white document page card (1px border, mono "CT-1042",
rows: Customer Lan Nguyen · Unit S-3 5 m² Indoor · Term Oct 3 → Jan 3 ·
Rent 345.000 ₫/mo × 3 = 1.035.000 ₫ · Deposit 103.500 ₫ (held) · footer
"Auto-drafted from Rental Policy v3 · read-only"); secondary "Print contract"
button; dashed-border "Capture signed copy" photo tile with camera icon;
primary indigo "Attach signed copy" button. Handover section shows muted note
"Requires signed contract on file" with its button disabled.
```
→ Stitch sinh **màn mới** `f49223df…` (manifest ghi thay thế `cec1ef01…`).

### 8.2 F2-04 Rental Detail — card "Contracts & Addenda"
```
Add a Contracts section after payment history: amber warning banner
"Addendum CT-1042-A1 is still unsigned. Sign at the desk by Oct 25 — the new
checkout date Oct 18 already applies."; two rows — CT-1042 "Original contract"
green Signed badge + "View signed copy" link; CT-1042-A1 "Extension addendum ·
new checkout Oct 18" amber Awaiting signature badge + meta "Sign at the desk
by Oct 25".
```
→ Edit in-place trên canvas; HTML export không regenerate → DOM block chèn tay vào file local.

### 8.3 F6-01 Extend — card "After payment"
```
Add an "After payment" info card after the extension fee summary: amber badge
"Signature required at desk"; body "Addendum CT-1042-A1 is drafted
automatically once the extension is paid. Sign the paper addendum at the desk
by Oct 25 — the new checkout date applies immediately after payment."; two-row
list: CT-1042-A1 · Extension addendum · Awaiting signature; New checkout Oct
18 · unit held for you · green "Effective on payment" badge.
```
→ Edit in-place; DOM block chèn tay vào file local. (Lần gọi đầu timeout — gọi lại thành công.)

---

## 📦 Batch 9 — System Admin (SYS-) — thêm 2026-09-21 (PRD §4.15, FR-37/38)

Vai trò thứ 5 · landing SysAdmin = User Management · nav `Users · Login History` · bảng quản trị chuẩn
(pagination 25 rows, badge + text label, mono cho email) · không có delete — chỉ đổi trạng thái.

### 9.1 SYS-01 User Management
```
Generate screen: User Management (system administrator, default landing). Top bar
with logo, nav links "Users · Login History", small indigo role chip "System
Administrator", bell with unread badge, avatar. Page title "Users" + primary indigo
"Create user" button. Admin table, 25-row pagination: columns Full name, Email
(mono), Phone, Role (text chip: Customer / Staff / Facility Manager / Business
Ops / System Administrator), Status (tinted badge + label: green "Active", grey
"Inactive", red "Locked"), Last login (relative, tabular numerals). Filter bar
above table: search input + role select + status select + "Filter" button.
Sample rows: "Minh Tran · minh.tran@storagehub.vn · Staff · Active · 08:12 today";
"Lan Nguyen · lan.nguyen@gmail.com · Customer · Active · yesterday";
"Hoang Vu · hoang.vu@storagehub.vn · Staff · Locked · Aug 30". One row selected
showing a right-side drawer: heading "Hoang Vu · Staff", read-only meta rows
(role chip, email mono, phone, created date), action buttons "Reactivate"
(primary), "Reset password" and "Lock account" (secondary, red text), and a muted
note "Users are never deleted — status changes only, all written to the Activity
Log." Table caption: "3 of 42 users".
```

### 9.2 SYS-02 Login & Activity History
```
Generate screen: Login & Activity History (system administrator). Same top bar,
nav link "Login History" active, indigo role chip "System Administrator". Page
title "Login & Activity History". Filter bar: user search input (mono email),
result select (All / Successful / Failed), date range inputs, "Filter" button;
one removable filter chip "user: hoang.vu@storagehub.vn". Append-only event table:
columns Time (tabular, "Sep 21 · 07:58"), User (name + mono email), Action
(LOGIN / LOGIN_FAILED as small mono tags), Result (green "Signed in" or red
"Failed" badge + text), Source row hidden on narrow widths. Sample rows showing
a chain of three LOGIN_FAILED for hoang.vu at 07:58, 07:59, 08:01 followed by a
successful LOGIN for minh.tran at 08:12. A quiet banner above the table:
"Login events are recorded automatically and cannot be edited or removed."
Table caption: "4 of 128 events · Sep 21".
```
