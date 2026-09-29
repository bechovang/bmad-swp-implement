# StorageHub — Blueprint UI/UX v4 ("Control Room", hiện trạng) — BẢN TIẾNG VIỆT

Bạn đang làm việc trên project Stitch hiện có **`9794980915577530548` — "StorageHub SWP391 Control Room"**:

"StorageHub — Hệ thống cho thuê và quản lý kho tự phục vụ."

Blueprint này **thay thế `uiux_blueprint (3).md`**. Nó mô tả hệ thống **như hiện tại** (đã audit ngày 2026-09-17) và chi phối mọi chỉnh sửa về sau. Nếu tài liệu này mâu thuẫn với blueprint (3), tài liệu này thắng. Nếu một màn Stitch mâu thuẫn với các tài liệu spine, tài liệu spine thắng (xem Nguồn sự thật).

> Lưu ý: đây là bản dịch tiếng Việt của `uiux_blueprint (4).md` để team đọc nội bộ. Mọi chuỗi UI nói trong tài liệu vẫn phải giữ nguyên tiếng Anh trong sản phẩm. Khi cần dẫn chiếu chính thức (đưa cho AI/Stitch, đối chiếu nhóm), dùng bản tiếng Anh.

## Nguồn sự thật (thứ tự ưu tiên)

1. `docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md` — hợp đồng nhận diện hình ảnh ("Control Room"). Sở hữu mọi quyết định về màu, chữ, bo góc, đổ bóng.
2. `docs/planning/ux-designs/ux-swp391-2026-09-11/EXPERIENCE.md` — hợp đồng hành vi. Sở hữu IA, flow, trạng thái, hành vi component, microcopy.
3. `docs/planning/ux-designs/ux-swp391-2026-09-11/mockups/` — 27 mockup tham chiếu đã đóng băng (F1–F7) + mục lục `NOTES.md`.
4. Chính project Stitch (các màn HTML sinh ra từ các nguồn trên).

## 0. Thay đổi gì so với blueprint (3) — bắt buộc đọc

Blueprint (3) được viết cho một dự án giả định 21 màn, 5 role, chưa có brand. Dự án từ đó đã được thiết kế xong và đóng băng. Các khác biệt:

| # | Blueprint (3) nói | Hiện trạng (blueprint này) |
|---|---|---|
| 1 | Đúng **21** màn | Đúng **27** màn (F1–F7), cộng thêm các overlay component không tính là màn |
| 2 | **5** role, có SYSTEM_ADMINISTRATOR | **4** role trong UI: Customer, Staff, Facility Manager, Business Ops. Role System Administrator, màn A-01 User & Role Management, A-02 Activity Log (admin), và SYS-01 màn lỗi đã bị **cắt**. Tài khoản staff/manager được cấp ngoài UI; đăng ký công khai chỉ tạo tài khoản Customer |
| 3 | Hai shell: header ngang cho Customer + sidebar cho back-office | **Một adaptive shell** cho mọi role: cùng top bar (logo, menu theo role, role chip, chuông thông báo, menu avatar), chỉ đổi các link ở giữa theo role |
| 4 | "Giữ hướng visual trung tính. Chưa áp dụng brand mới. Một màu accent trung tính" | Brand **đã áp dụng và đóng băng**: nhận diện "Control Room" — primary indigo `#4F46E5`, dải màu status ngữ nghĩa, thanh status 3px bên trái card, chữ số tabular, lưới 8px, chỉ light mode, system font stack. Không trung tính hóa, không đổi theme |
| 5 | Điều hướng chi tiết bằng `DetailDrawer` ở mọi nơi; "không tạo màn Contract riêng" | **Màn chi tiết nguyên trang** là pattern cho công việc sâu: Unit Detail, Rental Detail, Check-in Task, Checkout Task, Support Ticket. Drawer chỉ còn cho chi tiết Support List và chỉnh Unit. Hợp đồng vẫn **không có màn riêng** — sống trên Rental Detail dạng chuỗi (giữ từ (3)) |
| 6 | Nhóm màn C/S/M/B/A + AUTH/SYS | Nhóm màn theo **họ flow F1–F7** và theo role (Auth 2, Customer 12, Staff 4, Facility Manager 6, Business Ops 3) |
| 7 | AUTH-01 = một màn có biến thể Login/Register/Forgot | **Login** (F1-01, quên mật khẩu là modal trên màn này) và **Register** (F1-02) là hai màn riêng |
| 8 | Không có màn Notification — chỉ có chuông | **Notification Center** (F7-05) là màn thật (dropdown từ chuông), phân theo role, chưa đọc lên trước, có deep link |
| 9 | C-04 "My Storage" dạng tab (Reservations / Active Rentals / History) | **My Rentals** (F2-03) danh sách card + **Rental Detail** (F2-04) là "nhà" của khách. Không còn màn tab |
| 10 | M-01 "Unit Floor Plan and Unit Management" | Sơ đồ tầng và lịch availability là **anti-pattern bị cấm**. F5-02 Unit Management chỉ có bảng + edit drawer |
| 11 | M-04 Facility Operations Report (một màn) | Tách thành **Facility Overview** (F5-01 dashboard) và **Operations Monitor** (F5-04 xem trực tiếp) |
| 12 | B-01 Facility Management (Business Ops) | Dashboard cơ sở thuộc về **Facility Manager** (F5-01). Business Ops giữ Business Overview (F4-01), Policy Management (F4-02), Reports (F4-03) |
| 13 | B-02: "Không sửa trực tiếp policy đang active. Tạo bản draft version mới trước khi kích hoạt" | Policy Management dùng **inline edit + Save với validation đầy đủ**; policy không hợp lệ không bao giờ được lưu; lần save đóng dấu version + ngày hiệu lực (vd "Rental Policy v3") |
| 14 | Contract = ContractCard với các hành động View/Print/Upload/Confirm | **Domain hợp đồng** đầy đủ (thêm 2026-09-14): auto-draft từ booking + phiên bản policy đang effective; in → khách ký → staff chụp ảnh bản ký; **access code bị chặn đến khi bản ký được đính kèm**; gia hạn sinh phụ lục giấy (`CT-1042-A1`) ký tại quầy trong 7 ngày. Thêm lifecycle `ContractStatus` (xem §7) |
| 15 | Không định nghĩa semantics gia hạn | Thanh toán phí gia hạn dời ngày checkout mới **ngay lập tức** (unit được giữ — khách đã trả); phụ lục chỉ là giấy tờ thủ tục, không bao giờ hold unit |
| 16 | Payment = PaymentMethodSelector + PaymentStatusPanel | Một **Payment Modal giả lập** (F1-06) dùng chung cho đúng 3 touchpoint (cọc 10%, thuê 100% lúc check-in, phí gia hạn), phương thức **Card / MoMo / VNPay QR**, trạng thái: chọn phương thức → processing (~1,5–2 s) → success / fail (retry) / QR hết hạn (~5 phút đếm ngược) |
| 17 | S-03 "TaskBoard hoặc DataTable dùng chung" | **Kanban là dạng chốt** (To do / In progress / Done), 5 loại card (Check-in, Checkout, Cleaning, Support, Contract-signature), kéo thả + nút Move bằng bàn phím, snap-back kèm toast khi các bước kết thúc chưa xong |
| 18 | MetricCard/ChartPanel, không có quy tắc drill-down | **Dashboard-first + hợp đồng KPI drill-down**: mọi card KPI click xuyên xuống bảng dữ liệu nền được lọc sẵn theo cùng slice. Dashboard không bao giờ hiển thị con số mà không với tới được các dòng đằng sau |
| 19 | Mock data: unit `TD-A-101`…, bản ghi `BK-2026-0001`…, cơ sở FPT Thu Duc / FPT Binh Thanh, 5 user demo kể cả admin | Mock data: unit `S-1/S-2/S-3/M-2/M-5`, bản ghi `BK-1042`, `RT-0871`, `SR-0032`, `CT-1042`, `CT-1042-A1`, cơ sở **Tân Bình Depot**, 4 user demo (Lan, Minh, Hằng, Tuấn). Giá trị đầy đủ ở §13 |
| 20 | Frame 1440 × 1024, không thiết kế responsive | Đích thiết kế desktop-first **1200px**; hành vi collapse responsive đã được quy định (tablet 768–1199, mobile <768) nhưng **không thiết kế frame mobile/tablet riêng** |
| 21 | Giả định project sạch | Stitch hiện giữ **39 instance hiển thị của 27 màn** (12 bản trùng stale sinh ra từ lịch sử edit), 1 màn cũ ẩn, bản upload DESIGN.md, và asset design-system. Bắt buộc hợp nhất về đúng 27 instance chuẩn (§21) |

Mọi thứ khác từ blueprint (3) không bị mâu thuẫn ở trên vẫn còn hiệu lực — đặc biệt: UI chỉ tiếng Anh, các quy tắc kiểm soát dữ liệu (§5), model thực thể (§6), ý tưởng status dictionary (§7), mutation pattern (§18), và các quy tắc nhất quán (§19).

# 1. Yêu cầu ngôn ngữ

Toàn bộ project phải dùng đúng tiếng Anh. Bao gồm tiêu đề trang, điều hướng, nút bấm, nhãn form, văn bản hỗ trợ, thông báo validation, nhãn trạng thái, empty state, cảnh báo, hộp thoại xác nhận, header bảng, tooltip, notification, và mô tả dữ liệu demo. Không để lại chữ tiếng Việt trong UI.

**Tên riêng** và **tên địa danh** tiếng Việt được giữ như danh từ riêng không dấu khi cần, ví dụ: `Lan`, `Minh`, `Hang`, `Tuan`, `Tân Bình Depot` (ghi `Tan Binh Depot` nơi không có dấu).

Tiền luôn là VND đủ độ chính xác với dấu chấm ngăn cách hàng nghìn và ký hiệu `₫`: `1.150.000 ₫`. Không bao giờ `~100k`, không USD.

# 2. Nền tảng đích

Web responsive desktop-first. Một app, một URL, một adaptive shell.

* Đích thiết kế bề rộng: **1200px** (frame Stitch là desktop rộng 1280).
* Lưới Browse: 3 cột ở ≥1200px, gutter 14px, lề ngoài 24px.
* Thanh điều hướng trên cố định cao: 54px.
* Filter hiển thị trực tiếp trên trang (dropdown đóng + nút Search; re-query khi bấm Search, không query theo từng phím gõ).
* Lưới unit: 3–4 cột. Bảng giữ các cột quan trọng; phân trang 25 dòng cho bảng quản trị (không infinite scroll).
* Overlay luôn là overlay ở mọi kích thước; modal không chồng trên modal; drawer có thể mở từ bảng nhưng không từ modal; drawer không chồng trên modal.
* Collapse responsive (chỉ hành vi — KHÔNG thiết kế frame mobile/tablet riêng): tablet giảm Browse còn 2 cột, dashboard xếp dọc KPI → chart → bảng, bảng ẩn cột phụ, kanban hiện ~1,5 cột; mobile một cột, filter bar thu vào sau nút Filters, cột kanban cuộn ngang, bảng thành danh sách card, drawer full-width.

# 3. Kiến trúc lõi

Thứ tự quan tâm:

```
Data Model → Design Token (DESIGN.md) → Pattern nguyên thủy → Pattern dùng chung → Domain component → 27 Màn
```

* Lớp token đã tồn tại và có hiệu lực ràng buộc: design system Stitch ("StorageHub Control Room") nạp từ DESIGN.md. Không bịa màu, font, bo góc, đổ bóng mới; tham chiếu các token có tên (`primary`, `status-*`, `ink`, `muted`, `border`, …).
* Màn phải là composition của thư viện pattern dùng chung (§9–§11). Khi một pattern như unit card, status badge, payment modal, hay KPI card thay đổi, mọi màn dùng nó phải được cập nhật theo — mockup trong `mockups/` là tham chiếu đóng băng cho việc "theo" nghĩa là gì.
* Nếu sinh code: React component dùng lại được, props, data model có kiểu, một kho mock-data dùng chung. Nếu sinh frame thiết kế: master component, variant, property, instance. Không bao giờ nhân thủ công các card, dòng, badge, form, hay phần tử điều hướng giống hệt nhau.

# 4. Phạm vi hình ảnh — "Control Room" (đã khóa)

Nhận diện hình ảnh là **chốt**. Tính cách: chuyên nghiệp, kỹ thuật, thực dụng — hạ tầng kiểu developer-first và công cụ tài chính hiện đại (chất Linear, Stripe). "Mật độ hơn trang trí" và "Trạng thái luôn hiển thị rõ".

* Chỉ light mode. Không có công tắc theme.
* Mặt nền slate mát (`#F4F6F9` nền app, `#FFFFFF` card), indigo `#4F46E5` dành riêng cho hành động, lựa chọn, và focus. Màu không bao giờ dùng để trang trí.
* Dải màu status ngữ nghĩa (bộ ba tint + border + text): Available xanh emerald `#059669`, buffer/cảnh báo amber `#B45309`/`#F59E0B`, Reserved indigo, Rented slate `#334155`, Preparing xanh trời `#0284C7`, Maintenance cam cháy `#C2410C`, Retired xám `#94A3B8`.
* Phần tử đặc trưng: **thanh status 3px bên trái** card vận hành, cao suốt card, màu theo trạng thái lifecycle.
* Typography: system font stack; cấp bậc qua độ đậm (400/500/600) và cỡ khiêm tốn (display 21, headline 18, body 13, meta 11,5, label 10,5, price 15 / price-lg 19, code mono 13). **Chữ số tabular ở mọi nơi các con số căn hàng** (bảng, KPI, tiền).
* Lưới cơ sở 8px với nửa bước 4px. Bo góc: 6px phần tử nhỏ, 8px phổ quát (card, nút, input, modal, drawer), full (9999px) chỉ cho chấm status 7px và bộ đếm chưa đọc.
* Độ sâu: card nằm trên viền 1px + `0 1px 2px rgba(15,23,42,.05)`; hover tối viền, không phóng bóng; độ sâu thật chỉ cho modal/drawer/overlay (`0 12px 32px rgba(15,23,42,.14)`, scrim 40%).
* Cấm: gradient, kính mờ, minh họa trang trí, đồ họa marketing, màu accent thứ hai, hue mới mỗi màn, micro-label viết HOA, bóng đổ làm cấp bậc, emoji trong toast hệ thống.

# 5. Quy tắc kiểm soát dữ liệu

Không bịa dữ liệu nghiệp vụ, KPI, giá, phần trăm, địa chỉ, kích thước, cơ sở vật chất, tiện ích, chính sách, hay thống kê.

Mọi giá trị hiển thị phải thuộc một trong:

1. `Stored field` — lưu trực tiếp trong data model.
2. `Derived field` — tính toán từ dữ liệu lưu (phải ghi chép lại).
3. `Demo-only value` — định nghĩa một lần trong nguồn mock-data dùng chung (§13).

Không bịa: điểm bảo mật, đánh giá khách, số người thuê trước, phần trăm tăng trưởng doanh thu, giám sát AI, bảo hiểm, nhiệt độ/độ ẩm, phần trăm giảm giá, độ phổ thông, số lượng unit trống chưa tính toán, phí dịch vụ phát sinh, hay tính năng không có trong data model.

Không hiển thị ID kỹ thuật thô. Hiển thị mã nghiệp vụ: Reservation `BK-`, Rental `RT-`, Support `SR-`, Contract `CT-` (phụ lục `CT-…-A1`), payment/receipt theo mock data. Mã unit là mã mono ngắn (`S-3`, `M-2`).

Component không được tự tính giá hay bịa quy tắc nghiệp vụ. Giá đến từ phiên bản **Rental Policy** đang effective (demo: v3); mọi dòng tiền hiển thị trước khi thanh toán phải tái hiện đúng những gì policy sinh ra. Biên xung đột gia hạn ("Latest new checkout: Oct 18") do dữ liệu cung cấp, không phải UI tự tính tùy tiện.

# 6. Thực thể và role chuẩn

Thực thể (không đổi so blueprint (3), vẫn là nguồn dữ liệu UI):

Role, User, Facility, Zone, StaffAssignment, UnitType, Unit, RentalPolicy, PolicyRule, Reservation, Rental, Extension, CheckoutRequest, Contract, Payment, Settlement, SettlementCharge, Inspection, SupportTicket, Escalation, Task, Notification, ActivityLog.

**Đúng 4 role xuất hiện trong UI:**

* CUSTOMER
* STAFF
* FACILITY_MANAGER
* BUSINESS_OPERATIONS_MANAGER

Việc cấp tài khoản staff/manager diễn ra ngoài UI. Đăng ký công khai chỉ tạo tài khoản Customer — nói rõ điều này trên màn Register.

Các quy tắc model mà UI phải tôn trọng:

* Facility → Zone → Unit. Staff được phân theo ngày làm việc, ca, và zone; xung đột phân ca bị từ chối kèm thông tin va chạm cụ thể.
* Availability được suy ra từ reservation, rental, và **turnover buffer**. "Available soon (cleaning buffer)" là nhãn suy ra trong Browse, không bao giờ là trạng thái Unit lưu trong DB.
* Giá đến từ RentalPolicy có phiên bản; hợp đồng khóa phiên bản policy mà nó được soạn từ đó.
* Một Reservation tạo tối đa một Rental; một Extension thuộc về một Rental và sinh một phụ lục Contract; một Settlement chứa nhiều bản ghi SettlementCharge; một SupportTicket có thể có một Escalation.
* Hợp đồng được **auto-draft** từ booking + phiên bản policy đang effective. Không ai soạn hợp đồng thủ công; không có gì trong hợp đồng được chỉnh sửa ở bất kỳ đâu trong UI.
* ActivityLog chỉ được phép ghi thêm (append-only). Mọi thay đổi trạng thái quan trọng đòi hỏi actor và, nơi flow yêu cầu, một **lý do**. Không ghi trạng thái ngầm.
* Hành động đăng nhập không cần lý do nghiệp vụ.

# 7. Trạng thái chuẩn

Một `statusDictionary` dùng chung; không bao giờ viết tay nhãn trạng thái trong từng trang; dictionary phân biệt theo thực thể (`ACTIVE` nghĩa khác nhau trên Rental và Contract).

## UnitStatus (kèm ý nghĩa UI)

| Giá trị | Nhãn | UI |
|---|---|---|
| AVAILABLE | Available | Xanh lá; đặt được trong Browse với phép toán ngày bắt đầu |
| RESERVED | Reserved | Chàm trong view quản lý sau khi đặt cọc; khách thấy reservation đang hoạt động |
| RENTED | Rented | Slate; loại khỏi Browse |
| PREPARING | Preparing | Xanh trời; sau checkout; tự tạo task dọn dẹp; chưa đặt được đến khi dọn xong |
| MAINTENANCE | Under Maintenance | Cam; loại khỏi Browse; hiển thị trong Operations Monitor |
| RETIRED | Retired | Xám; chỉ Unit Management + Activity Log |

Nhãn suy ra (không phải status): **"Available soon"** — card buffer màu amber trong Browse ("Available Oct 5 · cleaning buffer"), đặt được với ngày muộn hơn.

Lifecycle: `Available → Reserved → Rented → Preparing → (Maintenance | Retired) → Available`.

## ReservationStatus

PENDING_PAYMENT → Pending Payment · RESERVED → Reserved · CHECKED_IN → Checked In · EXPIRED → Expired · CANCELLED → Cancelled

## RentalStatus

ACTIVE → Active · CHECKOUT_REQUESTED → Checkout Requested · CLOSED → Closed

## ExtensionStatus

PENDING_PAYMENT → Pending Payment · APPLIED → Applied · CANCELLED → Cancelled

## PaymentStatus (các trạng thái gateway giả lập)

PENDING → Method select · PROCESSING → Processing (~1,5–2 s) · SUCCEEDED → Successful · FAILED → Failed (retry) · EXPIRED → QR expired (~5 phút đếm ngược, quay về chọn phương thức, không tính tiền)

## TicketStatus

OPEN → Open · IN_PROGRESS → In Progress · ESCALATED → Escalated · RESOLVED → Resolved

## TaskStatus (cột kanban)

TODO → To Do · IN_PROGRESS → In Progress · DONE → Done

## ContractStatus — **mới so blueprint (3)**

| Giá trị | Nhãn | UI |
|---|---|---|
| DRAFT | Draft | "Contract CT-1042 drafted from Rental Policy v3." Preview chỉ-đọc trong Check-in Task |
| PRINTED | Printed | "Awaiting customer signature" — chặn việc trao access code |
| SIGNED | Signed | Ảnh bản ký đã đính kèm; ActivityLog ghi CONTRACT_SIGNED; bản copy xem được trên Rental Detail |
| ACTIVE | Active | Rental chạy trên hợp đồng này; settlement tham chiếu số hợp đồng |
| CLOSED | Closed | Receipt settlement nêu tên hợp đồng; bản ký xem được mãi mãi |
| SUPERSEDED | Superseded | Ngày kết thúc bị phụ lục `CT-1042-A1` thay thế; chuỗi vẫn đọc được |

Status badge không bao giờ chỉ có màu: bộ ba tint + border + nhãn chữ, từ dải token `status-*`.

# 8. Mục design system

Project Stitch đã mang asset design-system sinh từ DESIGN.md. Giữ nó là nguồn token duy nhất. Mọi công việc component phải định nghĩa, cho từng component: tên, mục đích, props/property, variant, trạng thái, ánh xạ thực thể/trường, các màn dùng nó, một master, các instance mẫu — và phải dùng tên token thay vì giá trị hex thô.

# 9. Pattern nguyên thủy

Button (primary filled indigo / secondary-ghost viền), IconButton, TextInput, PasswordInput, NumberInput, TextArea, Select, MultiSelect, DatePicker, DateTimePicker, DateRangePicker, Checkbox, RadioGroup, SearchInput, FormField, FieldError, StatusBadge, Tag, Tooltip, Divider, Tabs, Pagination, Breadcrumb, Toast, InlineAlert, Skeleton, Spinner, EmptyState, ConfirmDialog, FormModal, DetailDrawer, DropdownMenu.

Mọi control form dùng cùng cấu trúc FormField: nhãn (hiện rõ, không bao giờ chỉ có placeholder) → chỉ dấu bắt buộc → input → văn bản hỗ trợ → thông báo validation. Lỗi inline kích hoạt khi rời trường (viền lỗi + thông điệp). Block phía server (validation policy, kiểm tra xung đột) hiển thị thành banner tint lỗi ở đầu form, diễn đạt lại quy tắc thất bại — không phải toast. Nút Submit chỉ vô hiệu vì thiếu trường bắt buộc, không bao giờ vì cảnh báo mềm.

Empty state: ô icon, tiêu đề khách quan, một câu giải thích diễn đạt lại ràng buộc, chip mono lặp lại truy vấn/filter, một CTA. Cột kanban không có task hiện ô "Nothing here" yên tĩnh. Skeleton khớp bố cục (skeleton lưới card trên Browse, skeleton dòng trên bảng); không lệch bố cục khi hoán đổi.

# 10. Bố cục — một adaptive shell

## AppShell (duy nhất, mọi role)

Thanh trên, trái → phải: logo + wordmark · menu theo role (link tùy role) · role chip (pill tint primary ghi tên role hiện tại) · chuông thông báo kèm số chưa đọc · menu avatar (hồ sơ, đăng xuất). Bên dưới: nội dung trang. Footer tối giản nơi có.

Không còn tách customer header và back-office sidebar (đã bỏ so blueprint (3)). Role chip làm adaptive shell dễ nhận biết.

## Cấu trúc trang dùng chung

PageHeader · FilterToolbar (dropdown đóng + Search) · DataTable (dày đặc, 13px, chữ số tabular, đường phân dòng `#F1F5F9` nhẹ, hover `#F8FAFC`, phân trang 25) · CardGrid · EntityListPageTemplate · DashboardPageTemplate (hàng KPI → khối chart → bảng nền, theo thứ tự đó) · TransactionPageTemplate · Stepper · MetricCard/KpiCard (giá trị 24px tabular + chip delta, bấm được) · ChartPanel · PrimaryActionBar.

**Hợp đồng KPI drill-down:** mọi card KPI xuyên xuống bảng nền được lọc sẵn theo cùng slice (occupancy 87% → Unit Management lọc Rented; phụ thu quý này → tab Surcharges của Reports, cùng kỳ). Dashboard không bao giờ hiện con số mà người dùng không với tới được các dòng đằng sau.

# 11. Hợp đồng domain component

## UnitCard (lưới Browse)

Ảnh + chip mã mono, chip kích thước/loại, dòng tính năng, giá, dòng availability với chấm trạng thái, nút Book. Thanh trái 3px mã hóa trạng thái (available xanh / buffer amber). Card buffer ghi "Available Oct 5 · cleaning buffer" và vẫn giữ nút Book phụ. Bấm mọi nơi trừ Book → Unit Detail. Dòng availability được tính theo lịch thuê + turnover buffer tại thời điểm truy vấn — ngày hiển thị chính là lời cam kết. Reserve kiểm tra lại availability; kết quả cũ bật lại về lưới kèm toast ("S-3 was just reserved. 5 similar units still available.").

## UnitRow / UnitTable (quản lý)

Cột: mã (mono), kích thước, loại, zone/tầng, badge trạng thái hiện tại, link rental/reservation đang hoạt động, hoạt động gần nhất. Bấm dòng mở edit drawer.

## UnitEditDrawer

Hành động: sửa thông số · gộp với unit liền kề (điều kiện: không có Rental hay Reservation đang hoạt động ở cả hai phía) · nghỉ hưu (điều kiện + xác nhận) · đặt Maintenance · sửa trạng thái sai (**bắt buộc lý do**). Mọi ghi viết đều vào Activity Log. Chỉnh sửa phá hủy/có điều kiện nâng cấp thành confirm modal nêu hệ quả bằng thuật ngữ tiền/trạng thái.

## ReservationCard / RentalCard

Reservation: mã, unit, cơ sở, ngày, tiền cọc, trạng thái. Không có access code (code thuộc về Rental).
Rental: mã, unit, cơ sở, ngày kết thúc, tiền cọc đang giữ, trạng thái, access code qua xử lý SensitiveValue; chuỗi hợp đồng (gốc + phụ lục) với mã mono, badge trạng thái, ảnh bản ký; phụ lục chưa ký hiện banner cảnh báo amber nêu hạn chót tại quầy. Receipt thanh toán (cọc, thuê, gia hạn) và trạng thái tiền cọc (đang giữ / đã tất toán) treo trên Rental Detail — "nhà" của khách.

## ContractStep (nghi thức giấy tờ) — mới so (3)

Preview chỉ-đọc của hợp đồng auto-draft — mã mono, các bên, unit, ngày, các dòng thuê/cọc, phiên bản policy bị khóa — rồi Print, ô chụp ảnh bản ký, và Attach. Không gì chỉnh sửa được. Hành động hoàn tất flow (trao access code, hoàn tất phụ lục) giữ vô hiệu đến khi ảnh bản ký được đính kèm. Xuất hiện trong Check-in Task và trên card kanban Contract-signature.

## MoneySummary

Chỉ các dòng nhận từ dữ liệu: tiền thuê, tiền cọc (ghi hoàn lại được), phí gia hạn, các khoản settlement charge (mỗi khoản có lý do), tiền hoàn, khoản phải trả thêm, tổng phải thanh toán. Không có gì xuất hiện lúc thanh toán mà không xuất hiện trong bước duyệt trước đó.

## PaymentModal (gateway giả lập) — mới so (3)

Dùng chung cho đúng 3 touchpoint: cọc 10% (Booking Summary), thuê 100% (Check-in Task), phí gia hạn (Extend). Header nêu đang trả gì + số tiền bằng price-lg. Segmented control phương thức: **Card** (form số thẻ/hạn/CVC giả lập với validation inline) · **MoMo** (điện thoại + bước OTP giả lập) · **VNPay QR** (QR vẽ sẵn, đếm ngược ~5 phút; hết hạn quay về chọn phương thức, không tính tiền). Nút Pay mang đúng số tiền. Processing spinner ~1,5–2 s, không điều hướng lùi. Success: ô check, số tiền, dòng hệ quả → toast → trạng thái màn nền đổi. Fail: ô lỗi, "No money was taken", Retry + Switch method; sau hai lần fail, gợi ý đổi phương thức rõ ràng. Booking Confirmation **không phải** màn riêng — trạng thái success kết thúc flow F1 và deep-link sang Rental Detail.

## KanbanCard / TaskBoard

Cột To do / In progress / Done; tab loại phía trên; số mỗi cột ở header. Card: mã unit (mono), chip loại task, tên khách / khung giờ, chip hạn, thanh trái màu theo loại. Kéo để di chuyển; thay thế bàn phím bằng nút Move trên từng card (kéo không bao giờ là con đường duy nhất). Chuyển card Check-in/Checkout vào Done bị chặn đến khi các bước kết thúc của màn task (thanh toán / tất toán) hoàn tất — card bật lại kèm toast nêu bước còn thiếu. Card Support mang hành động Escalate. Card Done hiển thị mờ. Card Contract-signature xuất hiện khi phụ lục chờ chữ ký giấy (khách, unit, mã phụ lục, hạn chót) và hoàn tất khi ảnh bản ký về.

## TicketCard / TicketDetail

Card/dòng danh sách: mã ticket, unit, loại sự cố, mức ưu tiên, trạng thái, người xử lý, tạo/cập nhật. Toàn bộ luồng thảo luận, phân tuyến, và kết quả nằm trong drawer chi tiết (Support List phía khách) hoặc màn Support Ticket (staff). Hành động staff: Resolve kèm ghi chú, hoặc Escalate (**bắt buộc ghi chú**). Panel quyết định severity của manager: đánh dấu severe → unit sang Maintenance + di dời khách; hoặc trả lại staff kèm hướng dẫn — không bao giờ bị bỏ rơi.

## SettlementPanel (Checkout Task)

Nhận unit + chìa khóa theo checklist → kiểm tra tình trạng theo hạng mục (tường, cửa, sàn, vệ sinh) → số tiền phí hư hại + **lý do bắt buộc** khi phát hiện hư hại → preview tất toán (cọc − phí = hoàn, hoặc thu thêm nếu phí vượt cọc) → xác nhận đóng rental. Hỗ trợ hoàn cọc toàn bộ, hoàn một phần sau khi trừ phí, và thu thêm khi phí vượt cọc.

## KpiCard / ChartPanel

Theo DESIGN.md; giá trị suy ra từ dữ liệu demo dùng chung; không bịa phần trăm xu hướng. Bấm được theo hợp đồng drill-down.

## UserRow, ActivityLogRow

UserRow: họ tên, email, role, phạm vi cơ sở/zone, trạng thái tài khoản.
ActivityLogRow: thời điểm, actor, thực thể, hành động, from → to, lý do (nếu bắt buộc); lọc theo loại thực thể; chỉ đọc, append-only. Chi tiết giá trị cũ/mới thuộc về view chi tiết log.

# 12. Sử dụng component dùng chung (27 màn)

| Component | Màn (mã F) |
|---|---|
| AppShell | Cả 27 màn |
| Lưới UnitCard | F1-03; biến thể compact trong bối cảnh escalation/di dời |
| UnitDetailPanel | F1-04 |
| MoneySummary | F1-04, F1-05, F6-01, F2-02, F3-01 |
| PaymentModal | F1-05, F2-02, F6-01 |
| RentalCard + chuỗi hợp đồng | F2-03, F2-04, F6-01 |
| ContractStep | F2-02, card Contract trên Task Board |
| KanbanBoard | F2-01 |
| SettlementPanel | F3-01 |
| TicketCard / chi tiết | F7-02, F7-03, F7-04 |
| DataTable | F5-02, F5-03, F5-05, F4-02, F4-03 |
| DashboardPageTemplate + KpiCard + ChartPanel | F5-01, F5-04, F4-01 |
| NotificationCenter | Chuông trên mọi màn → F7-05 |
| StatusBadge | Mọi màn có trạng thái |
| FilterToolbar | Mọi danh sách tìm/lọc được |
| EmptyState / Skeleton | Mọi bề mặt danh sách và loading |
| ConfirmDialog (+ lý do nhập vào) | Retire, merge, severity, đóng kèm khấu trừ, sửa trạng thái |

# 13. Mock data dùng chung (nguồn duy nhất)

Mọi giá trị demo định nghĩa một lần; màn không bao giờ định nghĩa dữ liệu cục bộ.

User demo:

* **Lan** — Customer (đặt S-3, gia hạn, checkout; hoàn 63.500 ₫)
* **Minh** — Staff (vận hành kanban; check-in, checkout, support)
* **Hằng** — Business Operations Manager (phát hành policy v3)
* **Tuấn** — Facility Manager (gộp unit, sửa trạng thái, xử lý escalation)

Cơ sở demo: **Tân Bình Depot** (ghi "Tan Binh Depot" nơi không có dấu).

Unit demo: `S-1`, `S-2` (ứng viên gộp → một unit 8 m²), `S-3` (5 m² Indoor — unit demo chủ lực), `M-2` (escalation ngập nước), `M-5` (đích di dời).

Bản ghi demo:

* `BK-1042` — reservation, đã đặt cọc, chờ check-in
* `RT-0871` — rental đang hoạt động (task checkout)
* `CT-1042` — hợp đồng gốc, đã ký lúc check-in
* `CT-1042-A1` — phụ lục gia hạn, chờ ký tại quầy (hạn 7 ngày)
* `SR-0032` — ticket support đang xử lý (cửa kẹt); kịch bản escalation = ngập nước ở M-2
* **Rental Policy v3** — phiên bản định giá đang effective (nguồn của mọi dòng tiền)

Số tiền demo (chính xác, dùng lại mọi nơi):

* Thuê S-3: `345.000 ₫/tháng` × 3 tháng = `1.035.000 ₫`
* Cọc (10%, hoàn lại được): `103.500 ₫`
* Phí gia hạn: `690.000 ₫` (checkout mới Oct 18; vùng chặn bắt đầu Oct 19)
* Phí hư hại lúc checkout: `40.000 ₫` (kèm lý do)
* Hoàn tất toán: `63.500 ₫` (= 103.500 − 40.000)
* Một phân ca sáng cho staff (Minh, kịch bản xung đột Zone B ngày Oct 12)
* Một task check-in, một task checkout, một task dọn dẹp, một card contract-signature

# 14. 27 màn bắt buộc (chính xác)

## Auth (2)

| Mã | Màn | Ghi chú |
|---|---|---|
| F1-01 | Login | Email + mật khẩu. Quên mật khẩu = **modal** (email → "If an account exists for that email, a reset link has been sent."). Nêu rõ tài khoản staff/manager do quản trị cấp |
| F1-02 | Register | Khách tự đăng ký: họ tên, điện thoại, email, mật khẩu + xác nhận, đồng ý điều khoản. Thành công đổ vào Browse Units, đã đăng nhập |

## Customer (12)

| Mã | Màn | Ghi chú |
|---|---|---|
| F1-03 | Browse Units ⭐ | Màn chủ lực. Thanh filter (loại / kích thước / ngày bắt đầu / thời hạn) + lưới card unit; chip đếm kết quả "6 units available · live"; sort giá thấp→cao; card buffer đặt được ở ngày muộn hơn. Không có view lịch, không có sơ đồ tầng |
| F1-04 | Unit Detail | Ảnh, dòng thông số (kích thước, tầng, kiểu ra vào, an ninh), bảng giá (thuê × thời hạn, cọc 10%), CTA Reserve |
| F1-05 | Booking Summary | Mọi dòng khách sẽ bị ràng buộc: thuê × thời hạn, từng dòng phụ thu từ policy effective, tổng, cọc 10% ghi hoàn lại được; ghi chú hợp đồng được soạn từ đúng các điều kiện này và ký lúc check-in. Mở Payment Modal |
| F1-06 | Payment Modal | Overlay component được ghi chép như sheet trạng thái màn (3 phương thức × các state). Không phải đích điều hướng |
| F2-03 | My Rentals | Rental đang hoạt động và quá khứ dạng card với badge trạng thái và nút hành động tiếp theo |
| F2-04 | Rental Detail | Nhà bàn: unit, ngày, lịch sử thanh toán, trạng thái cọc, **chuỗi hợp đồng** (gốc + phụ lục, xem được bản ký, banner amber trên phụ lục chưa ký), hành động — Check-in, Extend, Checkout Request, New Support |
| F2-05 | Check-in Pass | Phiếu đến của khách: mã reservation to và mono, hướng dẫn tại quầy, cần mang gì (CMND/CCCD để ký hợp đồng, tiền 100% tiền thuê) |
| F6-01 | Extend | Chọn ngày kết thúc mới → kiểm tra xung đột với reservation kế (vùng xung đột được đánh dấu; nêu biên: "Latest new checkout: Oct 18") → phí gia hạn → Payment Modal → phụ lục auto-draft, ký tại quầy trong 7 ngày |
| F3-02 | Checkout Request | Yêu cầu ngày trả unit; giải thích logic tất toán cọc (hoàn / trừ / thu thêm) |
| F7-01 | New Support | Chọn unit + loại sự cố + mô tả → gửi |
| F7-02 | Support List | Lịch sử yêu cầu; **drawer chi tiết** hiện luồng thảo luận, phân tuyến, kết quả |
| F7-05 | Notification Center | Dropdown chuông; chưa đọc trước, nhóm theo ngày, deep link, đánh dấu tất cả đã đọc |

## Staff (4)

| Mã | Màn | Ghi chú |
|---|---|---|
| F2-01 | Task Board ⭐ | Màn đổ vào của staff. Kanban To do / In progress / Done; card Check-in / Checkout / Cleaning / Support / Contract-signature; tab loại; số cột |
| F2-02 | Check-in Task | Xác minh mã reservation → validation reservation + cọc đã trả → thu 100% tiền thuê (Payment Modal) → **ContractStep** (preview CT-1042 chỉ-đọc → Print → chụp bản ký → Attach; ghi chú bàn giao "Requires signed contract on file") → trao access code → rental kích hoạt |
| F3-01 | Checkout Task ⭐ | Cao trào tất toán: nhận unit + chìa khóa, checklist kiểm tra, phí hư hại + lý do bắt buộc, preview tất toán, đóng rental → unit thành Preparing |
| F7-03 | Support Ticket | Chi tiết sự cố (lời khách), ngữ cảnh unit + khách, ghi chú staff, Resolve / Escalate (bắt buộc ghi chú) |

## Facility Manager (6)

| Mã | Màn | Ghi chú |
|---|---|---|
| F5-01 | Facility Overview | Màn đổ vào. Dashboard-first: KPI công suất, cơ cấu doanh thu, trạng thái unit + chart + bảng nền |
| F5-02 | Unit Management ⭐ | Bảng unit + **edit drawer** (sửa, gộp, retire, maintenance, sửa trạng thái + lý do; điều kiện kiểm Rental + Reservation + lịch sử) |
| F5-03 | Staff & Shifts | Danh sách staff; phân ca = staff × zone × ca × ngày; kiểm tra xung đột khi lưu; lưới lịch tuần |
| F5-04 | Operations Monitor | Trực tiếp: task hôm nay, escalation, hàng đợi turnover-buffer |
| F5-05 | Activity Log | Audit trail append-only: thời điểm, actor, thực thể, hành động, from → to, lý do; lọc theo loại thực thể |
| F7-04 | Escalation Inbox ⭐ | Ticket đã escalate với panel quyết định severity: severe → Maintenance + di dời; hoặc trả lại staff kèm hướng dẫn |

## Business Ops (3)

| Mã | Màn | Ghi chú |
|---|---|---|
| F4-01 | Business Overview ⭐ | Màn đổ vào. Card KPI + chart: doanh thu, cọc đang giữ, phụ thu, hiệu suất sử dụng theo kỳ |
| F4-02 | Policy Management ⭐ | Cao trào validation. Quy tắc thuê + phụ thu theo loại unit trong bảng chỉnh sửa được; inline edit; Save chạy validation đầy đủ (policy sai không bao giờ được lưu); đóng dấu phiên bản + ngày hiệu lực |
| F4-03 | Reports | Tab Revenue / Deposits / Surcharges / Occupancy theo kỳ; preset (tháng này, tháng trước, quý) + khoảng tùy chỉnh; Export CSV mỗi tab |

## Đối chiếu với mã blueprint (3)

| Cũ | Nay |
|---|---|
| AUTH-01 | F1-01 + F1-02 (Register tách màn riêng; quên mật khẩu thành modal) |
| SYS-01 | **Đã bỏ** |
| C-01 | F1-03 |
| C-02 | F1-04 + F1-05 |
| C-03 | F1-05 + F1-06 (xác nhận = trạng thái success của payment) |
| C-04 | F2-03 + F2-04 |
| C-05 | F6-01 |
| C-06 | F3-02 |
| C-07 | F7-01 + F7-02 (+ F7-05 Notification Center) |
| S-01 | F2-02 (+ F2-05 phiếu phía khách) |
| S-02 | F3-01 |
| S-03 | F2-01 (+ F7-03) |
| M-01 | F5-02 (đã cắt sơ đồ tầng — bị cấm) |
| M-02 | F5-03 |
| M-03 | F7-04 |
| M-04 | F5-04 (+ F5-01) |
| B-01 | F5-01 (chuyển sang Facility Manager) |
| B-02 | F4-02 (quy trình draft-version → inline edit + save có validation + đóng dấu phiên bản) |
| B-03 | F4-01 + F4-03 |
| A-01 | **Đã bỏ** (không có role admin) |
| A-02 | F5-05 (chuyển sang Facility Manager) |
| — | Mới: F1-06, F2-05, F7-05 được ghi chép như màn/overlay |

# 15. Điều hướng theo role

Cùng shell, cùng chuông, cùng menu avatar — chỉ đổi các link ở giữa:

* **Customer:** Browse Units · My Rentals · Support
* **Staff:** Tasks · Support (không link Units; ngữ cảnh unit nằm trên card kanban)
* **Facility Manager:** Overview · Units · Staff & Shifts · Operations · Activity Log · Escalations
* **Business Ops:** Overview · Policy · Reports

Chỉ hiển thị điều hướng được phép của role hiện tại. Role chip luôn nêu tên role hiện tại. Các đường sâu phía khách (Extend, Checkout Request, New Support, Check-in) treo từ Rental Detail để rental — không phải hệ thống — là mỏ neo của khách.

# 16. Giọng văn và tông (chu microcopy ràng buộc)

Tiếng Anh SaaS chính xác, đủ năng lực, ít nghi thức. Nêu sự thật, gọi tên số tiền, giải thích vật cản trong một hơi. Không dấu chấm than, không cổ vũ liên hoàn, không động từ marketing — cho mọi role; thâm niên không phải là một tông giọng. Lỗi luôn nói chuyện gì đã xảy ra, tổn thất bao nhiêu (thường là không), và bước tiếp theo.

Nên / Không nên:

* "Unit S-3 reserved. Deposit 103.500 ₫ received." / "🎉 Your booking is successful!"
* "Payment failed. No money was taken. Retry or choose another method." / "Transaction error (code 05)."
* "Oct 20 is taken by the next reservation. Latest new checkout: Oct 18." / "This date is unavailable."
* "6 units available · live" / "Plenty of great options!"
* "Checkout closed. Refund 63.500 ₫ to Lan after damage fee 40.000 ₫." / "Process complete."
* "Reason required to change status." / "Error: validation failed."

# 17. Trạng thái prototype dùng chung

Dùng một nguồn trạng thái dùng chung để hành động trên màn này cập nhật các màn liên quan. Hành vi demo bắt buộc (5 flow chủ chốt):

* **F1 (Lan đặt unit):** Browse → Unit Detail → Booking Summary → trả cọc (MoMo) → Reservation xác nhận + hợp đồng `CT-1042` auto-draft; Rental Detail phản ánh receipt; reserve trên lưới cũ bật lại kèm toast.
* **F2 (Minh check-in):** mã reservation validate → thu 100% tiền thuê → hợp đồng in/ký/chụp → cấp access code → Rental kích hoạt, Unit → RENTED, card hoàn tất.
* **Checkout:** kiểm tra → phí hư hại 40.000 ₫ kèm lý do → hoàn tất toán 63.500 ₫ → Rental CLOSED, Unit → PREPARING, card Cleaning xuất hiện trên bảng; dọn xong → kiểm tra turnover buffer → Available (hoặc Reserved nếu có reservation kế).
* **Gia hạn:** xung đột chặn từ Oct 19 → chọn Oct 18 → trả 690.000 ₫ → ngày checkout mới có hiệu lực ngay → phụ lục `CT-1042-A1` chờ ký tại quầy (hạn 7 ngày, banner amber, card Contract trên bảng staff, nhắc chuông cả hai bên).
* **Escalation:** báo ngập M-2 → staff escalate kèm ghi chú → manager đánh dấu severe → M-2 → MAINTENANCE + khách di dời sang M-5 → ticket Resolved với toàn bộ diễn biến trong một drawer; khách nhận thông báo bằng lời giản dị.
* Ghi viết của manager (gộp, sửa trạng thái kèm lý do, phân ca bị từ chối khi xung đột) vào Activity Log; hành động quan trọng tạo Notification (sự kiện tiền phát cả toast lẫn mục chuông).

Không cần backend thật. Công thức chưa xác nhận dùng phản hồi giả lập cố định.

# 18. Pattern thao tác ghi (mutation)

Mọi hành động thay đổi dữ liệu gồm: trigger rõ ràng → form hoặc xác nhận → validation → trạng thái loading → kết quả success/error → cập nhật trạng thái dùng chung → ActivityLog khi áp dụng → Notification khi người dùng khác bị ảnh hưởng.

Ví dụ:

* **Hoàn tất check-in:** trao access code giữ vô hiệu đến khi ảnh hợp đồng ký được đính kèm.
* **Phí hư hại:** tất toán không thể tiến hành đến khi ghi đủ số tiền phí **và lý do**.
* **Sửa trạng thái unit:** bắt buộc textarea lý do; thay đổi vào Activity Log với actor, thời điểm, from → to, lý do; mọi UnitCard/UnitRow hiển thị unit đó cập nhật.
* **Lưu policy:** validation chặn lưu; cờ trường vi phạm; thông điệp nêu quy tắc/ngưỡng bị vi phạm; thành công xác nhận phiên bản + ngày hiệu lực.
* **Phân ca:** xung đột bị từ chốt trước khi lưu với thông tin va chạm cụ thể; slot xung đột được tô sáng.
* **Gộp/retire:** điều kiện kiểm Rental + Reservation + lịch sử; vật cản được nêu tên và link; hành động bất khả dụng đến khi thông thoáng; confirm modal nêu hệ quả ("This cannot be undone").

# 19. Quy tắc nhất quán

* Cùng tên trường thực thể mọi nơi; cùng nhãn tiếng Anh cho mỗi trạng thái; tiền luôn VND đủ chính xác (`1.150.000 ₫`) với chữ số tabular; ngày tháng nhất quán; cùng tiền tố mã nghiệp vụ mọi nơi.
* "Storage Unit" trong tiêu đề trang và mô tả trang trọng; "Unit" trong nhãn compact và tên component. Một thuật ngữ cho một thực thể — không bao giờ đồng nghĩa.
* Không hard-code nhãn trạng thái (statusDictionary), không hard-code giá trị policy trong component, không mock data riêng từng trang, không sửa dữ liệu trực tiếp bằng cách đổi chữ trong card.
* Bảng cho dữ liệu quản trị dày đặc; card chỉ khi người dùng so sánh ít thuộc tính chủ chốt.
* Trạng thái không bao giờ chỉ có màu; mọi bề mặt mang trạng thái phải lộ ngay (thanh 3px / dải badge).
* Không đồng tiền nào chuyển tay không có bảng phân tách dòng trước đó; mọi khoản phí mang chuỗi lý do nơi flow yêu cầu.
* Phản hồi chỉ toast + chuông — không bao giờ email. Toast tự tắt ~4 s, hover tạm dừng, tối đa một link hành động.
* Sàn tiếp cận (accessibility): nhãn hiện rõ cho mọi input; vòng focus 2px màu primary hiện rõ; thao tác được hoàn toàn bằng bàn phím (kể cả nút Move kanban); trạng thái không chỉ màu; mục chạm ≥ 40px; focus bị giữ trong modal/drawer và trả về phần tử mở.

# 20. Kết quả đầu ra bắt buộc

1. Mục design-system phản ánh token DESIGN.md (đã có — giữ đồng bộ).
2. Danh mục component/pattern với variant, trạng thái, ma trận sử dụng (§12).
3. Từ điển dữ liệu UI + statusDictionary (§7).
4. Nguồn mock-data dùng chung (§13) + danh mục trường suy ra + danh mục giá trị demo.
5. Đúng 27 màn chuẩn khớp mockup đóng băng; overlay component (Payment Modal, modal quên mật khẩu, drawer) ghi chép như component, không phải màn.
6. Điều hướng prototype hoạt động theo §15 và trạng thái demo dùng chung theo §17.
7. Danh sách trường hiển thị vẫn thiếu nguồn dữ liệu hợp lệ (mục tiêu: rỗng).

# 21. Thứ tự thực thi (cho mọi công việc Stitch tương lai)

## Bước 1 — Audit

Project Stitch hiện giữ **39 instance hiển thị của 27 màn unique** — 12 instance dư là bản trùng stale từ lịch sử edit (Policy Management ×3, Task Board ×3, Checkout Task Detail ×4, Staff & Shifts ×2, Check-in Task Detail ×2, Browse Units ×2, Unit Management ×2, Escalations ×2), cộng một màn cũ ẩn. Xác định instance chuẩn của mỗi tiêu đề (bản khớp với xuất khẩu mockup đóng băng; với Check-in Task đó là instance mới hơn có ContractStep), và đánh dấu phần còn lại để lưu trữ/xóa.

## Bước 2 — Hợp đồng dữ liệu

Xác nhận từ điển dữ liệu và từ điển trạng thái (gồm ContractStatus) với báo cáo ERD/state-chart.

## Bước 3 — Thư viện pattern

Ánh xạ mọi vùng màn về một pattern dùng chung (§9–§11); đánh dấu vùng nào nhân bản markup của màn khác mà không dùng chung pattern.

## Bước 4 — Hợp nhất

Giảm về đúng 27 instance chuẩn; không bộ màn trùng.

## Bước 5 — Kết nối

Điều hướng + trạng thái prototype dùng chung theo §17.

## Bước 6 — Kiểm chứng

* Đúng 27 màn; không trùng; không frame mobile/tablet.
* Toàn bộ chữ UI là tiếng Anh; không chữ tiếng Việt trong giao diện; tiền là VND đủ chính xác.
* Không có trường hiển thị thiếu nguồn đã biết; không tự chế trạng thái; tôn trọng statusDictionary.
* Một cập nhật thực thể xuất hiện trên mọi màn liên quan; điều hướng theo role đúng; có role chip.
* Mọi KPI xuyên xuống được; mọi nút quan trọng thực hiện một hành động prototype; mọi dòng tiền hiển thị trước thanh toán khớp phiên bản policy.
* Nghi thức hợp đồng được thực thi: không access code khi chưa có ảnh bản ký; phụ lục chưa ký quá hạn sinh nhắc nhở.
* Không ghi ngầm: có lý do và vết audit nơi flow yêu cầu.

Trước tiên cung cấp một audit ngắn của project hiện tại theo blueprint này. Rồi thực hiện công việc trực tiếp. Không dừng lại sau khi trình bày kế hoạch.
