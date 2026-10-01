---
title: StorageHub — Product Requirements Document
status: final
created: 2026-09-17
updated: 2026-09-29
---

# PRD: StorageHub

*Working title — StorageHub: Self-Service Storage Rental and Management System (xác nhận).*

## 0. Document Purpose

PRD này dành cho đội phát triển SWP391 (5 vai trò UI: Customer, Staff, Facility Manager, Business Ops, System Administrator — vai trò thứ 5 thêm lại 2026-09-21, §4.15) và các workflow hạ nguồn (architecture, epics & stories, QA). Cấu trúc: thuật ngữ neo vào Glossary (§3), tính năng nhóm với FR đánh số toàn cục (FR-N), giả định gắn thẻ `[ASSUMPTION]` inline và tập hợp ở §10.

PRD **xây trên** các artefact đã chốt, không sao chép lại:
- `docs/planning/ux-designs/ux-swp391-2026-09-11/` — DESIGN.md (hợp đồng hình ảnh "Control Room") + EXPERIENCE.md (hợp đồng hành vi: IA 29 màn — 27 gốc + 2 màn SYS 2026-09-21, flows, states, microcopy) + 27 mockups (2 màn SYS chờ sinh mockup — prompt sẵn trong STITCH-PROMPTS.md Batch 9).
- `ERD_Statechart_LaTeX/` — mô hình dữ liệu 22 thực thể + 7 state chart.
- `SWP391_7_Flows_Drawio_Swimlane/` — 7 flows nghiệp vụ gốc (nguồn đối chiếu).
- `ref/uiux_blueprint new.md` — blueprint current-state của Stitch project (bản dịch tiếng Việt: `ref/uiux_blueprint new vie.md`).

Khi PRD mâu thuẫn với EXPERIENCE.md về hành vi UI, EXPERIENCE.md thắng về chi tiết tương tác; PRD thắng về phạm vi và yêu cầu.

Lưu ý reference: với F2-04 (Rental Detail) và F6-01 (Extend), **HTML local là bản chuẩn** — PNG còn bản cũ thiếu tính năng hợp đồng (giới hạn công cụ export Stitch).

## 1. Vision

StorageHub là hệ thống web tự phục vụ cho hoạt động cho thuê kho mini (self-storage) — một app, một URL, năm vai trò dùng chung một adaptive shell. Khách hàng tự tìm kho, tự đặt, tự thanh toán; nhân viên vận hành ngày làm việc trên một bảng kanban; quản lý cơ sở nhìn trạng thái mọi kho ngay lập tức; quản lý vận hành kinh doanh định giá và đọc doanh thu trên dashboard; quản trị hệ thống cấp tài khoản, phân quyền và soát nhật ký đăng nhập.

Ba giá trị cốt lõi phân biệt StorageHub với cách vận hành sổ-tay/gọi-điện hiện nay:

1. **Lời hứa ngày chính xác.** Mọi ngày "có thể thuê" được tính dựa trên lịch thuê + turnover buffer tại thời điểm truy vấn — khách thấy ngày nào thì đúng ngày đó, kể cả kho đang trong chu kỳ dọn dẹp.
2. **Nghi thức hợp đồng giấy có vết.** Hợp đồng tự soạn (auto-draft) từ booking + phiên bản chính sách giá, in tại quầy, khách ký, nhân viên chụp ảnh lưu vào hệ thống — không có kho nào chạy không có bản ký; không ai soạn hợp đồng tay.
3. **Tất toán cọc minh bạch đến đồng.** Mỗi khoản phí phát sinh phải có lý do được ghi lại; khách thấy đúng dòng "Hoàn 132.500 ₫ sau phí hư hại 40.000 ₫" — lời hứa cọc lúc đặt được giữ trọn.

Thành công của dự án (capstone): demo trọn 5 user journeys chính không đứt gãy, tài liệu nghiệp vụ chuẩn nghiệp đội (PRD → architecture → epics/stories), code đủ sạch làm portfolio cá nhân.

## 2. Target User

### 2.1 Jobs To Be Done

**Customer (khách thuê kho):**
- "Tôi cần chốn để đồ EXTRA trong 3 tháng, đặt được online như đặt phòng khách sạn — thấy giá, ngày trống, và tiền cọc rõ ràng trước khi trả." (functional)
- "Tôi cần biết chắc ngày tôi đến nhận kho thì kho sẵn sàng — không bị hủy giờ chót." (emotional → trust)
- "Khi trả kho, tôi cần biết tiền cọc của tôi bị trừ cái gì, bao nhiêu, vì sao — không tranh cãi tại quầy." (functional + emotional)

**Staff (nhân viên kho):**
- "Tôi cần mở app là thấy việc của ca hôm nay, làm theo thứ tự, không bỏ sót check-in nào." (functional)
- "Tôi cần hệ thống ép tôi ghi lý do trước khi trừ tiền cọc khách — để tôi không phải tự chịu trách nhiệm phán đoán." (social/protective)

**Facility Manager:**
- "Tôi cần phát hiện kho nào hiển thị sai trạng thái và sửa có vết audit — không sửa chui." (functional + accountability)
- "Tôi cần sự cố nghiêm trọng (ngập nước) tự lộ ra cho tôi kèm đủ ngữ cảnh để quyết ngay: bảo trì + dời khách." (functional)

**Business Ops Manager:**
- "Tôi cần đổi giá/phụ thu mà không bao giờ lưu được một chính sách vô lý (phụ thu vượt trần) — hệ thống chặn tôi trước khi khách bị ảnh hưởng." (functional)
- "Tôi cần nhìn doanh thu, tiền cọc đang giữ, phụ thu theo kỳ — và bấm xuyên xuống được đúng các dòng sinh ra con số đó." (functional)

**System Administrator:**
- "Tôi cần cấp tài khoản staff mới trước ca sáng mai mà không phải chạy script." (functional)
- "Tôi cần biết ai đăng nhập hôm nay, ai đăng nhập thất bại nhiều lần, ai đã bị khoá — để trả lời ngay khi có nghi vấn." (functional + accountability)

### 2.2 Non-Users (v1)

- **Khách vãng lai chưa đăng ký** — không xem kho trước khi có tài khoản (browse yêu cầu đăng nhập).
- **Chủ kho bên thứ ba / marketplace** — StorageHub quản lý cơ sở của một bên vận hành, không phải sàn giao dịch kho.

### 2.3 Key User Journeys

*Chuẩn hóa từ 5 Key Flows trong EXPERIENCE.md (giữ trùng khớp ID nguồn: Flow 1–5) + đối chiếu 7 swimlane nghiệp vụ. Nhân vật demo: Lan (Customer), Minh (Staff), Hằng (Business Ops), Tuấn (Facility Manager), Nam (System Administrator). Chi tiết beat đầy đủ: EXPERIENCE.md → Key Flows.*

**UJ-1. Lan đặt kho, nhận kho, dùng và trả kho — vòng đời khách hàng trọn vẹn.** *(swimlane Flow 1 + 2 + 3 phần khách)*
- **Persona + context:** Lan cần cất đồ 3 tháng, muốn tự xử như đặt khách sạn.
- **Entry state:** đã đăng nhập, đứng ở Browse Units.
- **Path:** lọc Indoor/5 m²/Oct 3/3 tháng → mở Unit Detail S-3 thấy đủ bảng giá → Booking Summary duyệt từng dòng → trả cọc 10% (103.500 ₫) qua PayOS QR → ngày nhận kho: mở Rental Detail → Check-in Pass mang mã đặt chỗ → tại quầy trả 100% tiền thuê, ký hợp đồng CT-1042, nhận access code → giữa kỳ gửi support (cửa kẹt) → gia hạn bị chặn Oct 19 (reservation kế) → chọn Oct 18, trả 690.000 ₫ + 69.000 ₫ top-up cọc (cọc giữ lên 172.500 ₫), ký phụ lục CT-1042-A1 → gửi Checkout Request.
- **Climax:** ngày trả, settlement receipt trên Rental Detail: "Refund 132.500 ₫ after damage fee 40.000 ₫" — lời hứa cọc lúc đặt được giữ đến đồng.
- **Resolution:** rental đóng, receipt xem được mãi; notification xác nhận từng bước suốt hành trình.
- **Edge case:** thanh toán fail — không đụng gì đến trạng thái, luôn có retry/đổi phương thức; unit bị giành giữa chừng → bounce về lưới + toast gợi ý unit tương tự.

**UJ-2. Minh chạy ca trên kanban — một ngày của nhân viên kho.** *(swimlane Flow 2 + 3 phần NV)*
- **Persona + context:** Minh trực ca sáng, mở Tasks là thấy toàn bộ việc.
- **Entry state:** đăng nhập với role Staff, landing = Task Board.
- **Path:** kéo card Check-in của Lan sang In progress → mở Check-in Task: mã đặt chỗ validate + cọc đã xác nhận → thu 100% tiền thuê qua Payment Modal → in CT-1042, Lan ký, chụp ảnh upload → trao access code (nút khóa đến khi có ảnh ký) → sang Checkout Task khác: nhận kho + chìa, chạy checklist tình trạng.
- **Climax:** phát hiện hư tường — hệ thống ép khai phí 40.000 ₫ **kèm lý do bắt buộc** trước khi tất toán; hệ thống tự tính phần cọc hoàn 132.500 ₫ và đóng rental với receipt hai bên cùng thấy; kho chuyển Preparing, card Cleaning xuất hiện.
- **Resolution:** kho sạch → Available; không card nào kéo vào Done được khi thiếu bước kết (snap-back + toast chỉ bước thiếu).
- **Edge case:** kéo nhầm card vào Done thiếu bước → card bật lại kèm toast.

**UJ-3. Hằng phát hành chính sách giá — độ chặt của pricing.** *(swimlane Flow 4)*
- **Persona + context:** Hằng quản lý vận hành kinh doanh, cần tăng phụ thu late-checkout.
- **Entry state:** đăng nhập Business Ops, mở Policy Management.
- **Path:** sửa phụ thu late-checkout lên 15% → bấm Save.
- **Climax:** validation chặn: banner nêu đúng luật vi phạm ("15% exceeds the 10% cap in Rental Policy v3") + field vi phạm bị cắm cờ — chính sách xấu không bao giờ tồn tại.
- **Resolution:** sửa giá về hợp lệ (hoặc sửa trần), Save thành công, toast xác nhận phiên bản + ngày hiệu lực; Business Overview/Reports phản ánh số liệu theo chính sách mới.
- **Edge case:** cell khác vi phạm luật khác → cùng một pattern chặn-nêu luật-cắm cờ.

**UJ-4. Tuấn tái cấu trúc sàn kho — quản lý có vết audit.** *(swimlane Flow 5)*
- **Persona + context:** Tuấn quản lý cơ sở, cho nghỉ 1 kho nhỏ đang trống, và phát hiện 1 kho "sai trạng thái".
- **Entry state:** đăng nhập Facility Manager, mở Unit Management.
- **Path:** retire S-2 (guard kiểm không Rental/Reservation đang hoạt động) → Staff & Shifts phân Minh ca sáng Zone B, bị từ chối vì trùng → đổi ca chiều → mở drawer sửa kho đang "Rented" sai thực tế thành "Preparing".
- **Climax:** Activity Log hiện nguyên dòng audit: actor, thời điểm, Rented → Preparing, và lý do anh gõ vào — sửa sai mà không làm biến mất chính việc sửa.
- **Resolution:** mọi thao tác ghi viết đều có dòng log tương ứng; KPI trên Facility Overview click xuyên xuống Unit Management lọc đúng slice.
- **Edge case:** retire kho đang có reservation → bị chặn, reservation được nêu tên + link.
- *(Cập nhật 2026-09-22: Merge 2 unit → P2 không build; beat demo đổi từ "gộp S-1 + S-2" sang Retire S-2 — vẫn demo đủ guard + blocker + confirm.)*

**UJ-5. Mức nước trong M-2 — vòng escalation nghiêm trọng.** *(swimlane Flow 7)*
- **Persona + context:** khách báo ngập nước M-2 qua New Support; ticket đẩy về NV trực ca theo kho + ca.
- **Entry state:** Minh đang mở Support Ticket SR.
- **Path:** xem mô tả khách, xác nhận không tự xử được → Escalate **kèm ghi chú bắt buộc** → ticket vào Escalation Inbox của Tuấn với đầy đủ ngữ cảnh → Tuấn đánh dấu **severe**.
- **Climax:** một quyết định kéo cả chuỗi: M-2 → Maintenance, chuẩn bị kho khác (M-5) di dời khách, task dọn/ửa về kanban, khách nhận notification từng bước bằng lời giản dị.
- **Resolution:** ticket Resolved với toàn bộ diễn biến trong một drawer (sự cố → escalation → quyết định → bảo trì → di dời); nếu Tuấn không đồng ý mức severe → ticket về lại hàng staff kèm hướng dẫn, không bị bỏ rơi.
- **Edge case:** NV xử lý được ngay → Resolve + ghi chú, khách thấy kết quả ở Support List + bell.

**UJ-6. Nam cấp tài khoản và soát đăng nhập — một buổi của quản trị hệ thống.** *(đối chiếu đề actor 5 — System Administrator; không có swimlane nguồn)*
- **Persona + context:** Nam quản trị hệ thống; nhân viên mới bắt đầu ca tuần sau, và sáng nay có tài khoản đăng nhập fail liên tục.
- **Entry state:** đăng nhập System Administrator, landing = User Management (SYS-01).
- **Path:** tạo tài khoản Staff mới (họ tên, email, phone, mật khẩu tạm) → ghi Activity Log → khoá tài khoản của nhân viên đã nghỉ (Status → locked) → mở Login & Activity History (SYS-02), lọc theo user nghi vấn → thấy chuỗi LOGIN_FAILED sáng nay kèm thời điểm.
- **Climax:** mọi thao tác cấp/đổi/khoá đều có dòng audit đầy đủ actor + from → to; không có user nào bị xóa khỏi hệ thống — chỉ đổi trạng thái.
- **Resolution:** tài khoản mới đăng nhập được ngay (đổi mật khẩu ở lần đầu); lịch sử đăng nhập trả lời được câu hỏi "ai, khi nào, kết quả".
- **Edge case:** tạo email đã tồn tại → chặn inline; đăng nhập bằng tài khoản bị lock → message chung, không tiết lộ lý do phía client.

**Map swimlane → UJ:** Flow 1 (Đặt kho) → UJ-1 · Flow 2 (Check-in) → UJ-1/UJ-2 · Flow 3 (Checkout) → UJ-1/UJ-2 · Flow 4 (Chính sách & doanh thu) → UJ-3 · Flow 5 (Cơ sở vật chất & vận hành) → UJ-4 · Flow 6 (Gia hạn) → UJ-1 · Flow 7 (Hỗ trợ) → UJ-5 · UJ-6 (Quản trị) → đề actor System Administrator (không có swimlane nguồn).

## 3. Glossary

*Thuật ngữ dùng thống nhất toàn tài liệu và các artefact hạ nguồn. FR/UJ dùng đúng các từ này — không đồng nghĩa.*

- **Facility** — Một cơ sở kho; chứa nhiều **Zone**. Demo: Tân Bình Depot.
- **Zone** — Khu vực trong Facility; chứa nhiều **Unit**. Phân công Staff theo Zone.
- **Unit** — Một kho cho thuê (Storage Unit trong văn trang trọng). Thuộc một Zone. Mã mono ngắn (`S-3`). Trạng thái: Available / Reserved / Rented / Preparing / Maintenance / Retired. (DESIGN.md hiển thị 7 nhãn = 6 trạng thái lưu + 1 nhãn derived "Available soon".)
- **Unit Type** — Loại kho (vd Indoor 5 m²); là căn cứ của các Policy Rule giá.
- **Turnover Buffer** — Số ngày chuẩn bị (dọn dẹp) sau checkout trước khi Unit cho thuê lại. Là **Policy Rule**, không phải trạng thái.
- **Rental Policy** — Bộ định giá có phiên bản (v3 demo). Mỗi phiên bản chứa các **Policy Rule**: giá thuê theo Unit Type, các loại phụ thu + trần, **Deposit %**, **Turnover Buffer (ngày)**, **Discount % theo Unit Type** (FR-40), **WAIVER_CAP** — trần mức miễn phí (FR-41). Hợp đồng khóa phiên bản mà nó soạn từ đó.
- **Reservation** — Bản ghi sinh ở trạng thái PENDING_PAYMENT ngay khi khách xác nhận Booking Summary (chưa giữ Unit — bỏ modal không tạo giữ chỗ); trả cọc thành công → RESERVED (Unit bị giữ từ đây). **Gồm trọn vòng đời thuê sau check-in** (quyết định V3 2026-09-18 — gộp bảng Rental cũ vào Reservation): RESERVED → CHECKED_IN (đang thuê) → CHECKOUT_REQUESTED → CLOSED; EndDate dịch ngay khi gia hạn; Access Code thuộc bản ghi này. Mã `BK-` duy nhất suốt vòng đời. V1 có đúng một lối ra rủi ro: hết ngày nhận kho mà chưa check-in → EXPIRED, mất cọc (FR-36); không có cancel (§6).
- **Deposit** — Tiền cọc = Deposit % × tổng tiền thuê (demo 10%), hoàn lại được, tất toán qua **Settlement**.
- **Rental** — Giai đoạn của **Reservation** từ check-in (CHECKED_IN) đến khi đóng (CLOSED): khái niệm hiển thị trên UI (Rental Detail, My Rentals; nhãn `RT-` trong mock data), **không còn là bảng riêng** (gộp vào Reservation theo V3 2026-09-18). Giai đoạn này sở hữu **Extension**, **Contract**, **Payment**, **Inspection**, **Settlement**; có **Access Code**.
- **Extension** — Yêu cầu gia hạn thuộc một Rental. Ngày checkout mới có hiệu lực **ngay sau khi thanh toán phí gia hạn**; sinh **Addendum** (phụ lục auto-draft sau thanh toán — không có đường "soạn nhầm"; Voided chỉ khi khách trả kho sớm trước khi ký, staff đóng hồ sơ, không soạn lại — 2026-09-22). Trạng thái: PENDING_PAYMENT → APPLIED (đóng modal chưa trả = không ghi nhận; không có CANCELLED trong v1). Cọc đang giữ được top-up lên mức mới khi gia hạn (FR-16, 2026-09-22).
- **Contract** — Hợp đồng gốc auto-draft (từ Reservation + Rental Policy version), không ai soạn tay, không chỉnh sửa được. Mã `CT-`; trạng thái (6 giá trị): Draft → Printed → Signed → Active → Closed, kèm Superseded (bị thay bởi re-draft). AWAITING_SIGNATURE chuyển sang vòng đời riêng của **Addendum** (V3 2026-09-18).
- **Addendum** — Bản ghi riêng (entity `CONTRACT_ADDENDUMS` — V3 2026-09-18) thuộc **Contract** (`CT-…-A1`), sinh từ Extension; ký giấy tại quầy trong 7 ngày; là giấy tờ thủ tục, **không hold Unit**. Trạng thái (4 giá trị, chốt 2026-09-18): AWAITING_SIGNATURE → Signed (khi có ảnh bản ký); hai lối đóng hồ sơ không ký: **Expired** (khách không đến ký — staff đóng sau khi hết nhắc) và **Voided** (staff huỷ chủ động: soạn nhầm → soạn lại bản ghi mới, hoặc khách trả kho sớm).
- **Payment** — Giao dịch tại 4 touchpoint: Deposit (booking), 100% rent (check-in), Extension fee, và Extra fee (phần chênh khi Settlement Charge vượt Deposit — trả trước khi Rental đóng). Hai phương thức *(quyết định PayOS 2026-09-29 — thay mock 3 phương thức)*: **QR PayOS** — payment link thật (SDK `vn.payos:payos-java`), khách quét QR ngân hàng ngay trong modal, xác nhận qua webhook + checksum; **Cash** — thu tại quầy, staff bấm "Cash received" xác nhận. **Deposit (booking online) = QR only** — booking online không cho chọn Cash; Cash chỉ ở touchpoint có staff tại quầy (100% rent, Extension fee, Extra fee).
- **Settlement** — Bảng tất toán lúc checkout: Deposit ± các **Settlement Charge** → hoàn / thu thêm. Settlement receipt là biên bản (khách không ký thêm gì).
- **Settlement Charge** — Một khoản phí khấu trừ (vd damage fee) — **bắt buộc có reason**.
- **Inspection** — Checklist tình trạng Unit lúc checkout (tường, cửa, sàn, vệ sinh).
- **Support Ticket** — Yêu cầu hỗ trợ. Mã `SR-`. Route theo Unit + ca trực. Có thể có một **Escalation**.
- **Escalation** — Ticket đẩy lên Facility Manager kèm note bắt buộc; kết thúc bằng **Severity Decision**.
- **Severity Decision** — Manager đánh dấu severe → Unit chuyển Maintenance + **Relocation**; hoặc trả lại staff kèm hướng dẫn.
- **Relocation** — Sự cố severe: **đổi Unit trên Rental hiện tại** — giữ mã RT-/Contract/Deposit, cấp access code mới, ghi Activity Log RELOCATION. Không sinh Rental mới.
- **Task** — Việc vận hành trên kanban, 5 loại: Check-in, Checkout, Cleaning, Support, Contract-signature. Cleaning hoàn tất ngay trên card. Mã `TL-`.
- **Shift** — Ca trực: MORNING / AFTERNOON / EVENING; Staff được phân công theo Zone × ca × ngày, không trùng người-ca-ngày.
- **IncidentType** — Loại sự cố của Support Ticket: LOST_ACCESS / DEVICE_ISSUE / SECURITY / CLEANLINESS / OTHER.
- **Activity Log** — Audit trail append-only: actor, thời điểm, thực thể, hành động (gồm cả đăng nhập — LOGIN/LOGIN_FAILED, FR-38), from → to, reason (nơi bắt buộc). Không có ghi chép gì bị xóa.
- **Permission Matrix** — Bảng Role × Permission (menu, hành động, phạm vi facility) cố định phía server (NFR-4); System Administrator xem read-only (FR-39), không chỉnh runtime.
- **Access Code** — Mã vào kho; chỉ cấp sau khi Contract có ảnh bản ký.
- **Availability** — Giá trị suy ra (Reservation + Turnover Buffer); nhãn "Available soon" là derived label, không phải trạng thái Unit.
- **Notification / Toast** — Bell feed bền vững (deep link, unread persist) / phản hồi tức thời ~4s. Không có email.
- **KPI drill-down** — Hợp đồng dashboard: mọi số liệu bấm xuyên xuống bảng dữ liệu lọc đúng slice.

## 4. Features

*FR đánh số toàn cục. Độ ưu tiên build: **P1** = bắt buộc để demo 6 UJ trọn (fallback cắt), **P2** = hoàn thiện — cắt không phá flow. Phân cấp P1/P2 duyệt 2026-09-17; FR-37…41 thêm 2026-09-21 (FR-37/38 P1, FR-39/40/41 P2).*

### 4.1 Identity & Access *(realizes UJ-1…UJ-5)*

**Description:** Một adaptive shell cho 5 role; đăng nhập đổ vào landing theo role; role chip + menu theo role. Tài khoản staff/manager do System Administrator provisioning trong UI (FR-37) — quyết định 2026-09-21 thay seed script.

**Functional Requirements:**

#### FR-1: Login (P1)
[User] đăng nhập email + password, đổ vào landing theo role (Customer → Browse Units; Staff → Task Board; Facility Manager → Facility Overview; Business Ops → Business Overview; System Administrator → User Management). Sai thông tin → inline error nêu rõ bước kế tiếp.
- **Consequences:** sai 1 lần → message cụ thể, không tiết lộ field nào sai; menu chỉ hiện link của role; truy cập URL chéo role → error state 403 đơn giản. [Quyết định 2026-09-21: đưa lại vai trò System Administrator + 2 màn SYS, sau khi từng cắt ở blueprint v4 — xem §4.15] Avatar menu (thông tin tài khoản, logout) hiện trên top bar cho mọi role; logout đưa về Login.

#### FR-2: Customer self-register (P1)
[Visitor] tự tạo tài khoản CUSTOMER (họ tên, phone, email, password + xác nhận, đồng ý điều khoản); thành công → Browse Units đã đăng nhập. Registration công khai chỉ tạo CUSTOMER.
- **Consequences:** thử chọn role khác khi đăng ký → không có lựa chọn role trong form.

#### FR-3: Forgot password (P2)
[User] quên mật khẩu qua modal trên Login: nhập email → xác nhận generic ("If an account exists…"). [ASSUMPTION: v1 không gửi email thật — chỉ phản hồi UI; mail server out of scope]

### 4.2 Discovery & Booking *(realizes UJ-1)*

**Description:** Khách tìm kho với ngày chính xác theo Turnover Buffer, xem đủ bảng giá trước khi đặt, duyệt từng dòng ở Booking Summary trước khi trả cọc.

#### FR-4: Browse Units với availability chính xác (P1)
[Customer] lọc Unit theo loại / kích thước / ngày bắt đầu / thời hạn; kết quả là lưới card + chip đếm "N units available · live"; sort mặc định giá thấp → cao.
- **Consequences:** mỗi dòng availability tính theo Reservation + Turnover Buffer (Policy Rule) tại thời điểm query; Unit đang Rented/Maintenance/Retired không xuất hiện; card buffer hiển thị "Available {date} · cleaning buffer" và vẫn đặt được với ngày đó. Filter chỉ re-query khi bấm Search (không per keystroke); filter active persist trong phiên và được echo thành chip removable ở empty state.

#### FR-5: Reserve re-check chống stale (P1)
[Customer] bấm Reserve → hệ thống kiểm tra lại availability; Unit đã bị chiếm giữa chừng → quay về lưới + toast gợi ý unit tương tự.
- **Consequences:** không bao giờ tạo booking trên ngày đã invalid.

#### FR-6: Unit Detail với bảng giá minh bạch (P1)
[Customer] xem spec đầy đủ (kích thước, tầng, access, an ninh) + bảng giá: rent × duration + từng dòng phụ thu từ Rental Policy active + Deposit (đánh dấu refundable).
- **Consequences:** mọi dòng tiền ở đây khớp tuyệt đối Booking Summary và Payment Modal.

#### FR-7: Booking Summary — hợp đồng trước khi trả tiền (P1)
[Customer] duyệt lại toàn bộ dòng sẽ bị truy đòi; ghi chú Contract được auto-draft từ đúng các điều kiện này và ký tại check-in; CTA mở Payment Modal (Deposit).
- **Consequences:** không có dòng tiền nào xuất hiện ở payment mà vắng ở đây.

#### FR-36: Reservation hết hạn — no-show (P1) *(quyết định 2026-09-18)*
[Hệ thống] hết ngày nhận kho mà Reservation chưa check-in → EXPIRED: mất cọc (receipt "Deposit forfeited — no-show"), Unit → Available, Contract Draft/Printed → Closed, ghi Activity Log; khách nhận notification kết thúc.
- **Consequences:** không hoàn cọc cho no-show; EXPIRED là lối ra rủi ro duy nhất của Reservation — không có cancel trong v1 (§6 Non-Goals), huỷ chủ động xử lý ngoài hệ thống.

### 4.3 Payment — mock gateway *(realizes UJ-1, UJ-2)*

**Description:** Một modal dùng chung 4 touchpoint (Deposit, 100% rent, Extension fee, Extra fee khi charge vượt Deposit), 2 phương thức (QR PayOS + Cash tại quầy), trạng thái tường minh, không bao giờ dead-end *(sửa PayOS 2026-09-29)*.

#### FR-8: Payment Modal — QR PayOS + Cash tại quầy (P1) *(sửa 2026-09-29 — thay mock 3 phương thức)*
[User] thanh toán qua **QR PayOS** — BE tạo payment link thật, modal render QR từ `checkoutUrl` + poll trạng thái tới khi webhook xác nhận (verify checksum), đếm ngược theo expiry của link do BE trả; hoặc **Cash** — chỉ touchpoint có staff tại quầy (100% rent, Extension fee, Extra fee): chọn Cash tạo payment `PENDING_CASH`, staff bấm "Cash received" xác nhận nhận tiền. **Deposit (booking online) = QR only** — không có lựa chọn Cash ở Booking Summary. Success hiển thị check tile + amount + consequence line; fail/timeout hiển thị "No money was taken" + Retry + Switch method.
- **Consequences:** fail/hết hạn link không đổi trạng thái gì anywhere (link bị cancel, không tính tiền); sau 2 lần fail gợi ý đổi phương thức rõ ràng. Cash `PENDING_CASH` — nghiệp vụ tương ứng (RESERVED / Rental active / EndDate mới / Settlement khép) chỉ tiến khi staff confirm. Modal dùng chung cho cả touchpoint Extra fee khi Settlement Charge vượt Deposit (FR-18); trong trạng thái awaiting confirmation, modal khóa điều hướng nhưng có nút Cancel tường minh (cancel link + về method select).

#### FR-9: Payment success ghi sổ (P1)
[Hệ thống] xác nhận thanh toán từ đúng một trong hai nguồn: webhook PayOS (đã verify checksum) hoặc staff cash-confirm; ghi Receipt (ghi rõ method QR/CASH) vào Rental Detail + toast + bell entry; flip trạng thái tương ứng (Reservation confirmed, Rental active, ngày checkout mới).
- **Consequences:** mỗi payment thành công sinh đúng 1 receipt xem được vĩnh viễn.

### 4.4 Contracts — nghi thức giấy có vết *(realizes UJ-1, UJ-2)*

**Description:** Contract auto-draft từ Reservation + Rental Policy version; in tại quầy; khách ký; staff chụp ảnh upload; access code bị khóa đến khi có bản ký. Extension sinh Addendum giấy.

#### FR-10: Auto-draft contract (P1)
[Hệ thống] sinh Contract Draft ngay khi Deposit thành công; mã `CT-xxxx`; khóa phiên bản policy; read-only ở mọi nơi.
- **Consequences:** không có UI nào cho phép sửa nội dung contract; không ai soạn contract tay. Sai sót ở bản Draft được sửa bằng re-draft — bản cũ chuyển Superseded, vẫn đọc được trong chuỗi (FR-13).

#### FR-11: Check-in contract ritual (P1)
[Staff] preview read-only → Print → chụp ảnh bản ký → Attach; nút trao Access Code vô hiệu đến khi ảnh bản ký đính kèm; Attach ghi `CONTRACT_SIGNED` vào Activity Log.
- **Consequences:** không thể kích hoạt Rental mà Contract chưa Signed.

#### FR-12: Addendum cho Extension (P1)
[Hệ thống] sinh Addendum `CT-…-A1` sau khi trả phí gia hạn; deadline ký tại quầy 7 ngày; amber banner trên Rental Detail + card Contract-signature trên Task Board + bell reminder hai bên.
- **Consequences:** ngày checkout mới hiệu lực ngay sau thanh toán (Addendum không hold Unit); Addendum quá hạn → reminder tăng cường, không thu hồi ngày. Addendum ở trạng thái AWAITING_SIGNATURE đến khi staff đính kèm ảnh bản ký — nút hoàn tất card Contract-signature vô hiệu đến khi capture tile có ảnh; sau đó → Signed và ảnh vào chuỗi (FR-13).

#### FR-13: Contract chain trên Rental Detail (P1)
[Customer] xem chuỗi Contract gốc + Addenda: mã, badge trạng thái, ảnh bản ký; bản Signed xem được vĩnh viễn.
- **Consequences:** Contract bị Superseded vẫn đọc được trong chuỗi.

### 4.5 Check-in *(realizes UJ-2)*

#### FR-14: Validate reservation tại quầy (P1)
[Staff] nhập/mã reservation → hệ thống validate tồn tại + Deposit đã trả; không hợp lệ → lý do cụ thể, chặn tiến trình.
- **Consequences:** reservation chưa cọc không thể check-in.

#### FR-15: Thu 100% rent + bàn giao (P1)
[Staff] thu 100% tiền thuê qua Payment Modal ngay tại Check-in Task → trao Access Code (sau Contract Signed) → Rental ACTIVE, Unit RENTED, task hoàn tất.
- **Consequences:** Deposit và rent full hiển thị là 2 giá trị tách bạch.

### 4.6 Extension *(realizes UJ-1)*

#### FR-16: Extension với conflict boundary (P1)
[Customer] chọn ngày checkout mới → hệ thống kiểm tra Reservation kế tiếp; vùng xung đột được đánh dấu trên picker; submit bị chặn kèm biên rõ ràng ("Latest new checkout: Oct 18"); phí gia hạn tính theo Rental Policy active.
- **Consequences:** không thể extend vào ngày đã có Reservation; thanh toán thành công → ngày mới + Addendum (FR-12) + cọc đang giữ nâng lên mức mới. Phí gia hạn = tiền thuê kỳ thêm theo Rental Policy active **+ top-up cọc** = max(0, Deposit% × tổng tiền thuê hợp đồng sau gia hạn − cọc đang giữ) *(quyết định 2026-09-22 — thay cho "Deposit giữ nguyên booking gốc" của 2026-09-21: gia hạn dài phải giữ cọc tỷ lệ với tổng giá trị hợp đồng; Deposit% giảm giữa chừng → top-up floor 0, phần chênh hoàn tại Settlement; số demo chuẩn: extension CT-1042 = 690.000 ₫ + 69.000 ₫ top-up, cọc 103.500 → 172.500 ₫)*. Settlement (FR-18) dùng số cọc hiện giữ — công thức refund không đổi. Extension chỉ mở **trước EndDate** (quyết định 2026-09-21): qua EndDate Rental giữ CHECKED_IN, mỗi ngày trễ tính phụ thu LATE_FEE tại Settlement (FR-18) — không có trạng thái OVERDUE (§6).

### 4.7 Checkout & Settlement *(realizes UJ-1, UJ-2)*

#### FR-17: Checkout Request (P1)
[Customer] gửi yêu cầu trả kho từ Rental Detail kèm giải thích logic tất toán (hoàn / trừ / thu thêm).
- **Consequences:** Rental → CHECKOUT_REQUESTED; Checkout Task sinh ra trên board. Request mới đè request cũ (cũ tự hủy); request DONE khi settlement hoàn tất; ngày xin trả bị chặn trên biên Reservation kế tiếp (như Extension).

#### FR-18: Checkout Task — climax tất toán (P1)
[Staff] nhận Unit + key (checklist) → Inspection từng hạng mục (theo ERD: access card, padlock, cleanliness, structure; kết quả OK / MINOR / MAJOR — hạng mục MAJOR là căn cứ Settlement Charge) → khai Settlement Charge (số tiền + **reason bắt buộc** khi có damage; từng charge có thể giảm/miễn trong trần WAIVER_CAP kèm reason — FR-41) → settlement preview (Deposit − charges = refund; nếu charges vượt Deposit → khách trả phần chênh bằng Payment Modal, touchpoint Extra fee) → confirm đóng Rental.
- **Consequences:** không thể đóng Rental khi có charge thiếu reason hoặc phần chênh chưa trả; settlement receipt hiển thị hai bên vĩnh viễn; Unit → Preparing; Cleaning Task sinh ra.

#### FR-41: Settlement waiver trong trần WAIVER_CAP (P2) *(quyết định 2026-09-21 — đối chiếu đề "fee-waiver policies")*
[Staff] giảm hoặc miễn một Settlement Charge **trong trần WAIVER_CAP** (Policy Rule do Business Ops cấu hình — FR-29/FR-40) với **reason bắt buộc** → Activity Log; khách thấy dòng charge điều chỉnh + lý do trên settlement receipt.
- **Consequences:** vượt trần → nút vô hiệu kèm nêu trần; không có charge nào bị miễn mà vắng reason; waiver không áp dụng cho deposit forfeiture no-show (FR-36).

### 4.8 Turnover & Unit Status *(realizes UJ-2, UJ-5)*

#### FR-19: Cleaning hoàn tất từ card (P1) *(quyết định 2026-09-17: không màn riêng)*
[Staff] hoàn tất Cleaning Task ngay trên card Task Board; hệ thống kiểm tra Turnover Buffer trước khi chuyển Unit → Available (hoặc Reserved nếu có Reservation kế).
- **Consequences:** buffer chưa đủ ngày → Unit giữ Preparing, card không hoàn tất được.

#### FR-20: Unit status guards (P1)
[Facility Manager / Hệ thống] mọi chuyển trạng thái Unit qua Maintenance / Retired phải qua guard + confirm; sai lệch thực tế sửa qua "fix status" + **reason bắt buộc** → Activity Log.
- **Consequences:** không có silent status write ở bất kỳ đâu. Kết thúc Maintenance luôn đi qua PREPARING + Cleaning Task theo Turnover Buffer — không bao giờ về thẳng Available.

### 4.9 Task Board — kanban *(realizes UJ-2, UJ-5)*

#### FR-21: Kanban 5 loại card (P1)
[Staff] xem việc ca hiện tại theo cột To do / In progress / Done; card loại Check-in / Checkout / Cleaning / Support / Contract-signature; tab lọc theo loại; số lượng từng cột ở header; kéo thả hoặc Move bằng keyboard.
- **Consequences:** drag không phải con đường duy nhất (keyboard Move); card Done render muted.

#### FR-22: Snap-back chống Done ảo (P1)
[Staff] kéo Check-in/Checkout/Contract-signature vào Done khi thiếu closing step (payment/settlement/ảnh bản ký) → card bật lại + toast nêu đúng bước thiếu.
- **Consequences:** không có task Done mà nghiệp vụ chưa khép.

### 4.10 Support & Escalation *(realizes UJ-5)*

#### FR-23: Tạo + route Support Ticket (P1)
[Customer] tạo ticket (Unit + incident type + mô tả) → hệ thống route tới Staff trực ca theo Unit + shift.
- **Consequences:** ticket không có Unit hợp lệ không gửi được.

#### FR-24: Resolve / Escalate (P1)
[Staff] Resolve kèm note (khách nhận kết quả plain words) hoặc Escalate **kèm note bắt buộc** → ticket vào Escalation Inbox của Facility Manager.
- **Consequences:** Escalate không note → nút vô hiệu.

#### FR-25: Severity Decision + Relocation (P1)
[Facility Manager] quyết severe → Unit chuyển Maintenance + Relocation — **đổi Unit trên Rental hiện tại** (giữ mã RT-/Contract/Deposit, access code cấp mới, Activity Log RELOCATION) + tasks bảo trì/dọn sinh ra + khách nhận notification từng bước; không severe → ticket về lại staff kèm hướng dẫn.
- **Consequences:** sau mọi Severity Decision (severe hay không), ticket quay lại IN_PROGRESS và chỉ Resolved khi staff hoàn tất phần việc kèm note; ticket Resolved hiển thị trọn arc (sự cố → escalation → quyết định → bảo trì → di dời) trong một drawer.

### 4.11 Facility Management *(realizes UJ-4)*

#### FR-26: Unit Management + edit drawer (P1)
[Facility Manager] bảng Unit (code, size, type, zone/floor, status badge, link rental/reservation active, last activity) + drawer: sửa specs, **Retire** (guard + confirm), set **Maintenance**, **Fix status** (reason bắt buộc); **Merge** (guard: không Rental/Reservation active hai bên; confirm "cannot be undone") → **P2, không build v1** *(quyết định 2026-09-22 — giữ nguyên thiết kế guard, cắt không phá flow; beat demo UJ-4 thay bằng Retire)*.
- **Consequences:** retire bị chặn → blocker nêu tên + link; mọi write → Activity Log.

#### FR-27: Staff & Shifts + conflict detection (P1)
[Facility Manager] phân công staff × Zone × ca × ngày; conflict bị từ chối **trước khi save** kèm collision cụ thể ("Minh is already on Morning, Zone B, Oct 12") + slot xung đột highlight; lưới lịch tuần.
- **Consequences:** không lưu được phân công trùng.

#### FR-28: Activity Log append-only (P1)
[Facility Manager] xem audit trail: timestamp, actor, entity, action, from → to, reason; lọc theo entity type; read-only.
- **Consequences:** không có UI xóa/sửa log.

### 4.12 Policy & Business Intelligence *(realizes UJ-3)*

#### FR-29: Policy Management validate-trước-khi-lưu (P1)
[Business Ops] sửa inline bảng rule (base rent theo Unit Type, phụ thu + trần); Save chạy validation đầy đủ — giá trị vi phạm bị cắm cờ + banner nêu đúng luật vi phạm; **policy không hợp lệ không bao giờ persist**; Save thành công đóng dấu version + effective date.
- **Consequences:** phụ thu vượt trần → chặn, không có bản lưu xấu tồn tại.

#### FR-30: Policy chứa Buffer + Deposit % (P1) *(quyết định 2026-09-17)*
[Business Ops] cấu hình **Turnover Buffer (ngày)** và **Deposit %** như hai Policy Rule theo phiên bản — đúng swimlane gốc; mọi tính toán availability/deposit đọc từ policy active.
- **Consequences:** đổi buffer/deposit chỉ ảnh hưởng tính toán từ phiên bản effective. [Data-model delta] `POLICY_RULES.RuleType` cần thêm `TURNOVER_BUFFER` — chốt chỗ lưu trước bmad-architecture (xem §9.1).

#### FR-40: Discount % theo Unit Type (P2) *(quyết định 2026-09-21 — đối chiếu đề "discount policies")*
[Business Ops] cấu hình **Discount % theo Unit Type** như Policy Rule trong cùng bảng validate (FR-29, RuleType `DISCOUNT` — delta §9.1); dòng giảm giá hiển thị ở Unit Detail + Booking Summary; giá sau giảm là căn cứ Contract auto-draft và tiền cọc.
- **Consequences:** discount chỉ hiệu lực từ phiên bản effective; không có dòng giảm nào xuất hiện ở payment mà vắng ở Booking Summary (khớp FR-7); Discount % vượt trần validation bị chặn như mọi rule khác. [ASSUMPTION: trần discount mặc định 50% — đặt tại Policy validation, chỉnh được khi cấu hình]

#### FR-31: Business Overview + Reports + KPI drill-down (P1 dashboards, P2 export)
[Business Ops] Business Overview: KPI revenue / deposits held / surcharges / utilization + charts; Reports: tab Revenue / Deposits / Surcharges / Occupancy + preset kỳ + custom range + **Export CSV (P2)**; mọi KPI bấm xuyên xuống bảng lọc đúng slice.
- **Consequences:** deposits held **không** tự động tính là revenue; không có số liệu nào không với tới được rows.

#### FR-32: Facility Overview + Operations Monitor (P1 overview, P2 monitor live)
[Facility Manager] Facility Overview: occupancy / revenue mix / unit-status KPI + charts + bảng nền; Operations Monitor (P2): live tasks hôm nay, escalations, turnover-buffer queue.
- **Consequences:** KPI occupancy 87% → click → Unit Management lọc Rented.

### 4.13 Notifications *(realizes UJ-1…UJ-5)*

#### FR-33: Toast + Bell (P1)
[Hệ thống] toast tức thời (~4s, hover pause, ≤1 action link); sự kiện tiền phát cả toast lẫn bell; bell badge đếm unread.
- **Consequences:** không email, không bao giờ.

#### FR-34: Notification Center (P1 feed, P2 day-grouping)
[User] xem feed theo role; unread trước; deep link tới đối tượng; mark-all-read; unread persist giữa các phiên; nhóm theo ngày (P2).
- **Consequences:** khách nhận resolution plain words; staff nhận task assignment; manager nhận escalation + status writes; business ops nhận policy save + report export completion.

### 4.14 Customer Surfaces *(realizes UJ-1, UJ-5)*

#### FR-35: Ba màn neo phía khách (P1)
[Customer] My Rentals (danh sách Reservations + Rentals + History — entry point tới Rental Detail, Extend, Checkout Request, Support); Check-in Pass (mã đặt chỗ `BK-` hiển thị lớn để staff validate tại quầy — FR-14); Support List (danh sách ticket + detail drawer — nơi khách đọc resolution).
- **Consequences:** ba màn này là pointer surfaces — mọi con số/trạng thái khớp nguồn (Rental Detail / ticket detail), không tự tính lại; empty state có CTA trỏ flow đổ dữ liệu (NFR-8).

### 4.15 System Administration *(realizes UJ-6 — đối chiếu đề actor System Administrator; vai trò thêm lại 2026-09-21)*

**Description:** Vai trò thứ 5: quản lý tài khoản, phân quyền, nhật ký đăng nhập. Shell adaptive thành 5 role; landing SysAdmin = User Management. Thay thế provisioning bằng seed script của các bản trước (§4.1).

**Functional Requirements:**

#### FR-37: User Management — SYS-01 (P1) *(quyết định 2026-09-21)*
[System Administrator] bảng user (họ tên, email, phone, role, status, last login) + search/filter theo role & status; **tạo tài khoản cho mọi role** (Customer/Staff/Facility Manager/Business Ops/System Administrator) với mật khẩu tạm; đổi role kèm Activity Log; activate / deactivate / lock theo `users.Status` (0/1/2 — đã có sẵn trong model, §9.1); **không có delete** — user chỉ đổi trạng thái; reset password = cấp mật khẩu tạm mới (v1 không gửi email — khớp FR-3).
- **Consequences:** email trùng bị chặn inline; không user nào biến mất khỏi hệ thống; mọi thao tác ghi viết có dòng Activity Log (actor, target user, from → to); đăng nhập bằng tài khoản deactivate/lock → chặn với message chung.

#### FR-38: Login & Activity History — SYS-02 (P1) *(quyết định 2026-09-21)*
[Hệ thống] ghi event `LOGIN` / `LOGIN_FAILED` vào Activity Log (EntityType USER) ở mỗi lần đăng nhập — thành công lẫn thất bại; [System Administrator] xem SYS-02: filter theo user / thời gian / kết quả, mỗi dòng có actor, thời điểm, kết quả.
- **Consequences:** không có lần đăng nhập nào vắng log; log đăng nhập append-only như mọi Activity Log; SYS-02 là pointer surface — hiển thị đúng nguồn, không tự tính lại gì.

#### FR-39: Permission matrix read-only (P2) *(quyết định 2026-09-21)*
[System Administrator] xem bảng **Role × Permission** (menu, hành động, phạm vi facility) đúng như server enforce (NFR-4); read-only — ma trận cố định trong code, không chỉnh runtime.
- **Consequences:** UI không bao giờ mâu thuẫn với enforcement thật; "configure permissions" nghĩa là thiết kế ma trận lúc build, không phải tính năng runtime; phạm vi facility: Staff theo Zone phân công, Facility Manager theo cơ sở, Business Ops/System Administrator toàn hệ. [ASSUMPTION: demo 1 facility — scoping đa facility chốt ở architecture (§9.1)]

## 5. Cross-Cutting Non-Functional Requirements

*Rút từ DESIGN.md + EXPERIENCE.md — là ràng buộc cho mọi FR ở §4, không lặp lại trong từng FR.*

#### NFR-1: Ngôn ngữ & định dạng tiền/ngày
UI English-only; tiền VND full precision (không viết tắt "1tr", không bỏ 3 số 0); định dạng chuẩn: dấu chấm ngăn cách hàng nghìn, ký hiệu ₫ đặt sau số — `1.150.000 ₫`; số tiền dùng tabular numerals; định dạng ngày nhất quán toàn app.
- **Nguồn:** DESIGN.md (money rule — typography).

#### NFR-2: Accessibility floor
Mọi input có label; focus visible; thao tác chính chạy được bằng keyboard (kanban có Move ngoài drag); status không bao giờ chỉ phân biệt bằng màu (label + icon); click target ≥ 40px. Focus bị trap trong modal/drawer đang mở và trả về trigger khi đóng; empty state luôn có bước kế (không dead-end); toast là phản hồi tức thời, bell là record bền vững — toast không bao giờ thay thế bell.
- **Nguồn:** EXPERIENCE.md anti-patterns + state patterns.

#### NFR-3: Platform
Web responsive desktop-first; mục tiêu chính 1200px content width; light mode only; cửa sổ hẹp hơn vẫn dùng được (không thiết kế riêng mobile).
- **Nguồn:** DESIGN.md + blueprint v4 (platform).

#### NFR-4: Security baseline
Password hash at rest; mọi route app yêu cầu đăng nhập; role enforcement ở phía server (ẩn menu chỉ là UX, không phải bảo mật); Permission matrix cố định phía server, hiển thị read-only cho System Administrator (FR-39); tài khoản bị deactivate/lock không đăng nhập được (FR-37); Access Code hiển thị qua cơ chế reveal (SensitiveValue); mọi thay đổi trạng thái quan trọng phải có reason ghi audit.
- **Nguồn:** entity model (SensitiveValue), EXPERIENCE.md.

#### NFR-5: Hiệu năng (quy mô demo)
Tính đúng của availability query ưu tiên hơn tốc độ; trang load < 2s trên môi trường demo với seed data. Thanh toán qua PayOS thật (không còn mock gateway — sửa 2026-09-29): demo yêu cầu internet + credentials PayOS qua env vars + URL public cho webhook (xem §9 OQ-2). [ASSUMPTION: số liệu quy mô demo cho capstone, không phải SLA production — cân lại khi biết môi trường deploy, xem §9 OQ-2]
- **Nguồn:** capstone stakes + OQ-2.

#### NFR-6: Data integrity
ActivityLog append-only ở tầng dữ liệu (không chỉ UI read-only); mọi biến động tiền sinh receipt/record tương ứng; không có silent write trạng thái.
- **Nguồn:** ERD + EXPERIENCE.md.

#### NFR-7: Voice & microcopy
Mọi error/empty/toast nêu đủ ba thứ: chuyện gì xảy ra, hệ quả tiền/trạng thái (thường là "không có gì"), đúng một bước kế tiếp. Register plain thống nhất cho cả 5 role; cấm exclamation mark, streak-cheer, marketing verbs.
- **Nguồn:** EXPERIENCE.md Voice and Tone + Anti-patterns.

#### NFR-8: States & form discipline
Skeleton khớp layout (swap không layout shift); empty state factual + echo filter đang active + đúng 1 CTA. Block phía server hiển thị banner đầu form (không phải toast); Submit chỉ disable khi thiếu required field, không disable vì soft warning. Rời màn mà còn cell inline edit chưa commit → guard cảnh báo. Bảng quản trị dùng pagination 25 rows, không infinite scroll.
- **Nguồn:** EXPERIENCE.md State Patterns + Interaction Primitives.

## 6. Non-Goals (Explicit)

- **Không** hoàn tiền thật — PayOS chỉ thu (payment link một chiều, không dùng Payouts); refund tại Settlement là receipt hiển thị + record, không có gateway hoàn tiền. *(Sửa 2026-09-29: thanh toán thật qua PayOS QR + Cash tại quầy giờ **có** trong scope — trước đây là mock gateway.)*
- **Không** gửi email — phản hồi chỉ toast + bell.
- **Không** dark mode, không mobile native — web responsive light-only.
- **Không** calendar availability view / floor map — availability chỉ qua card grid + buffer dates (quyết định flagship).
- **Không** là marketplace kho đa chủ — một bên vận hành, nhiều Facility cùng hệ.
- **Không** UI tiếng Việt — UI English-only, VND full precision.
- **Không** có cancel booking trong v1 — no-show → Reservation EXPIRED + mất cọc (FR-36); huỷ chủ động xử lý ngoài hệ thống.
- **Không** có trạng thái OVERDUE (chốt 2026-09-21, đối chiếu đề Flow 6 "Renewal and Overdue Handling") — rent thu 100% trước tại check-in (FR-15) nên không tồn tại nợ thuê; **trả trễ = phụ thu LATE_FEE** (Policy Rule do Business Ops quản — đúng dòng "overdue fees" của đề) tính theo ngày tại Settlement; **hết ngày nhận kho không đến = Reservation EXPIRED + mất cọc** (FR-36). Renewal = FR-16 (chỉ trước EndDate); swimlane Flow 6 gốc (gia hạn theo ngày) không có nhánh overdue.

## 7. MVP Scope

### 7.1 In Scope (P1)
- 28/29 màn theo UX package ở mức P1 — Operations Monitor (F5-04) là P2 (§7.2); đủ chạy trọn 6 UJ: Identity (Login/Register), Discovery & Booking (gồm no-show expiry), Payment PayOS (QR thật) + Cash tại quầy, Contracts (ritual + addendum), Check-in, Extension, Checkout & Settlement, Turnover (cleaning từ card), Task Board, Support & Escalation (kèm Relocation), Facility Management (unit guards, shifts conflict, activity log), Policy (gồm Buffer + Deposit %) + Business Overview + Reports, Customer surfaces (My Rentals / Check-in Pass / Support List), System Administration (SYS-01 User Management + SYS-02 Login & Activity History — FR-37/38), Toast + Bell + Notification feed.
- Backend REST thật cho toàn bộ nghiệp vụ trên (FE/BE tách riêng); dữ liệu demo theo mock-data chuẩn (Lan/Minh/Hằng/Tuấn; S-3, M-2, M-5; BK-1042, RT-0871, SR-0032, CT-1042, CT-1042-A1).

### 7.2 Out of Scope for MVP (P2 — cắt không phá flow)
- Forgot password đầy đủ (FR-3) — v1 chỉ phản hồi UI generic.
- Export CSV Reports (FR-31).
- Operations Monitor live view (FR-32).
- Notification day-grouping (FR-34).
- Permission matrix read-only (FR-39) — bảng tĩnh hiển thị, gần như miễn phí nhưng không cần cho demo UJ.
- Discount % theo Unit Type + dòng giảm giá ở Unit Detail / Booking Summary (FR-40).
- Settlement waiver trong trần WAIVER_CAP (FR-41).
- Merge 2 unit thành unit lớn (FR-26) — giữ thiết kế guard, không build v1 (quyết định 2026-09-22).
- Polish responsive tablet/mobile đầy đủ — desktop 1200px là đích demo.
- [NOTE FOR PM] Drill-down KPI → rows trên Business Overview + Facility Overview là **P1** (FR-31/32 — cam kết "không số liệu nào không với tới rows"). P2 chỉ là phần mở rộng: drill-through tới mọi chart và cấp Operations Monitor.

## 8. Success Metrics

**Primary**
- **SM-1**: Demo trọn vẹn — 6 UJ (UJ-1…UJ-6) chạy end-to-end không đứt gãy trên môi trường demo với seed data chuẩn. Validates FR-1…FR-38 (P1).
- **SM-2**: Nghiệp vụ chặt — 100% các bước "ép" hoạt động đúng khi test phá: checkout không charge thiếu reason, policy xấu không lưu được, card Done thiếu bước bật lại, access code không cấp khi thiếu ảnh ký. Validates FR-11, FR-18, FR-22, FR-29.

**Secondary**
- **SM-3**: Chất lượng hạ nguồn — architecture + epics/stories sinh trực tiếp từ PRD này không phát sinh gap nghiệp vụ lớn (>5 FR phải tự bịa khi viết story). Validates §4.

**Counter-metrics (do not optimize)**
- **SM-C1**: Số màn hình / tính năng — không tối ưu bằng cách thêm màn ngoài 29 đã chốt (27 gốc + 2 SYS, 2026-09-21); mọi màn muốn thêm phải qua đổi EXPERIENCE.md trước. Cân bằng SM-1: demo đẹp không đồng nghĩa thêm tính năng.
- **SM-C2**: Tốc độ demo — không trade đúng nghiệp vụ lấy mượt demo (vd hard-code bypass validation khi demo).

## 9. Open Questions

1. FE framework cụ thể (React? Vue?) và backend language/DB — chốt ở bmad-architecture; PRD chỉ ràng buộc FE/BE tách riêng + REST.
2. Môi trường deploy demo (cloud? máy local? FPT lab?) — ảnh hưởng NFR hiệu năng thực tế.
3. Team bao nhiêu người / bao nhiêu tuần / phân công role — cần để căn P1/P2 lại theo capacity thật (hiện P1/P2 là đề xuất).
4. Cách seed demo data — script seed phía BE hay tạo qua UI (SYS-01)? (2026-09-21: tài khoản tạo được qua SYS-01 — FR-37; script seed vẫn cần cho dữ liệu nghiệp vụ số lượng lớn như units/reservations lịch sử).
5. Quy trình môn học (Scrum ceremony, commit convention, báo cáo cuối kỳ) — PRD không ép nhưng epics/stories nên khớp sprint của môn.

### 9.1 Data-model deltas — chốt sẵn cho architecture (không phải open question)

Reconciliation với `ERD_Statechart_LaTeX` (2026-09-17/18) phát hiện các lệch; bên dưới là phía cần đổi khi vào architecture:

- `POLICY_RULES.RuleType`: thêm `TURNOVER_BUFFER` (nền FR-30/FR-4) — hoặc field riêng trên `RENTAL_POLICIES`. Thêm `DISCOUNT` (FR-40) và `WAIVER_CAP` (FR-41) — quyết định 2026-09-21.
- `EXTENSIONS.Status`: bỏ `CANCELLED` — chỉ `PENDING_PAYMENT / APPLIED`.
- `ActivityLogs.Reason`: đổi NULL-able; NOT NULL chỉ áp dụng cho action loại status-change/charge (khớp NFR-4).
- Relocation: đổi `UnitID` trên Rental + ActivityLog `RELOCATION` — không cần entity mới (quyết định 2026-09-18).
- Reservation: sinh bản ghi `PENDING_PAYMENT` ngay tại Booking Summary (không chỉ sau cọc).
- Báo cáo ERD §1.2: bỏ dòng "Facility Manager phê duyệt policy" — policy không qua bước duyệt (FR-29).
- `PAYMENTS`: purpose `EXTRA_FEE` + FK SettlementID đã đúng — PRD đã cập nhật touchpoint thứ 4 theo ERD (FR-8/FR-18).

**V3 conceptual model (chốt 2026-09-18, xem `Conceptual_Model_StorageHub_V3.drawio`) — 4 thay đổi kiến trúc dữ liệu, triển khai trực tiếp khi vào architecture:**

- **Role 1—N User** (đã đúng trong ERD/DBML; conceptual model V2 cũ vẽ N:N sai): mỗi user đúng một role; đăng ký không cho chọn role (FR-1), staff provisioning qua seed.
- **Gộp `RENTALS` vào `RESERVATIONS`** — một bảng cho trọn vòng đời đặt chỗ → thuê → trả: cọc duy trì phiên (không tồn tại "booking không có rental"); `reservation_status` gộp: `PENDING_PAYMENT / RESERVED / CHECKED_IN / CHECKOUT_REQUESTED / CLOSED / EXPIRED` (+`CANCELLED` dự phòng theo blueprint §7 — v1 không kích hoạt, §6); `reservations` nhận thêm `AccessCode`; các FK `RentalID` trên `extensions`, `checkout_requests`, `settlements`, `support_tickets`, `payments` đổi trỏ `reservations.ReservationID`; `payments` bỏ cột `RentalID` (còn 3 cột tham chiếu + CHECK đúng-một).
- **Tách bảng `CONTRACT_ADDENDUMS`** — `CONTRACTS` 1—N `CONTRACT_ADDENDUMS` (bỏ self-FK `ParentContractID` và `Type ORIGINAL/ADDENDUM`); `EXTENSIONS` 1—N `CONTRACT_ADDENDUMS` (bỏ UNIQUE 2026-09-18 — huỷ Voided rồi soạn lại = bản ghi mới cùng extension; chỉ phụ lục gia hạn có extension); `SignatureDueDate` + ảnh bản ký phụ lục thuộc Addendum; `CONTRACTS.Status` còn 6 giá trị, `ContractAddendums.Status` = `AWAITING_SIGNATURE / SIGNED / EXPIRED / VOIDED`.
- **`RESERVATIONS` 1—1 `CONTRACTS`** — mỗi đặt chỗ sinh đúng một hợp đồng gốc (auto-draft FR-10); FK `ReservationID` UNIQUE trên `contracts`. *[Architecture override 2026-09-22: để giữ FR-10 re-draft (bản cũ → Superseded, vẫn đọc được trong chuỗi), `contracts` nới thành 1—N qua `SupersedesContractID` + cờ `IsLatest` — "đúng một hợp đồng gốc" nghĩa là đúng một bản hiệu lực (`IsLatest`). Xem ARCHITECTURE-SPINE AD-6.]*

**SysAdmin & audit deltas (quyết định 2026-09-21 — nền FR-37…39):**

- `roles`: seed thêm dòng System Administrator — đổi data, không đổi schema; `users.Status` (0=không hoạt động / 1=hoạt động / 2=bị khoá) đã có sẵn, dùng cho activate/deactivate/lock.
- `ActivityLogs`: thêm action `LOGIN` / `LOGIN_FAILED` (EntityType `USER`) — tái dùng delta Reason NULL-able đã liệt kê ở trên.
- Phạm vi theo facility (đề: "permissions based on user role and assigned facility"): demo 1 facility — Staff scoping qua `staff_assignments` (Zone); nếu cần scoping đa facility cho Facility Manager, thêm `users.FacilityID` nullable; chốt ở bmad-architecture.

## 10. Assumptions Index

- §4 FR-1 — unauthorized access dùng error state 403 đơn giản. Màn SYS từng bị cắt ở blueprint v4; **đưa lại 2026-09-21** kèm vai trò System Administrator (§4.15).
- §4.15 — System Administrator thêm lại 2026-09-21 để phủ đủ 5 actor của đề; FR-37/38 là P1, FR-39 là P2.
- §4 FR-40 — trần Discount % mặc định 50% do Policy validation đặt (FR-29), chỉnh được khi cấu hình.
- §4.15 FR-39 — demo 1 facility; scoping đa facility chốt ở architecture (§9.1).
- §4 FR-3 — forgot password v1 không gửi email thật (chỉ phản hồi UI generic).
- §5 NFR-5 — ngưỡng hiệu năng là quy mô demo, cân lại khi chốt môi trường deploy (OQ-2).
- §8 SM-3 — ngưỡng ">5 FR phải tự bịa" là heuristic để đo chất lượng PRD phục vụ hạ nguồn.
- §2.2 — browse yêu cầu đăng nhập (Non-User "khách vãng lai") — suy ra từ thiết kế UX, không có màn public browse.
- §4.3 FR-8/FR-9 — thanh toán thật PayOS QR + Cash tại quầy thay mock gateway (2026-09-29); team đã có tài khoản PayOS (my.payos.vn) — credentials qua env vars; webhook cần URL public khi deploy demo (OQ-2); booking online không cho chọn Cash (Deposit = QR only).
