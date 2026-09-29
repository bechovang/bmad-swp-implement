---
type: review-reconcile
target: ARCHITECTURE-SPINE.md (architecture-storagehub-2026-09-21)
sources-checked:
  - docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md (final 2026-09-21)
  - docs/planning/ux-designs/ux-swp391-2026-09-11/EXPERIENCE.md (final 2026-09-21, IA 29 màn)
  - ERD_Statechart_LaTeX/main.tex + mermaid/erd.mmd + 7 file state-*.mmd (model V3)
date: 2026-09-22
verdict: PASS-WITH-CONDITIONS — khớp Model V3 về con số và các quan hệ đã chốt; còn 2 lỗ hổng phải chốt trước khi dựng repo (GAP-1 upload ảnh bản ký, GAP-2 xung đột kiểu tiền) và 2 điểm cần làm rõ wording (GAP-3, GAP-4).
---

# Review reconcile — UX package × Data model V3 × Architecture Spine

## Phần 1 — UX check: 29 màn có giả định kiến trúc nào spine chưa phủ

Đối chiếu từng concern đặt ra trong nhiệm vụ với EXPERIENCE.md (IA + Component Patterns + State Patterns + Key Flows) và DESIGN.md.

### 1.1 SPA routing & deep link — **CHƯA PHỦ (GAP-3, medium)**

Spine chỉ nói "React SPA" (AD-1) và "FE: server-state bằng TanStack Query" (Conventions); không có quyết định nào về router, sơ đồ URL, hay hợp đồng deep link. Trong khi UX 29 màn phụ thuộc routing ở 4 chỗ:

1. **`NOTIFICATIONS.DeepLink` là dữ liệu backend lưu trong DB** (entity V3 có cột `DeepLink VARCHAR(255)`) — chuỗi route FE do BE sinh/gắn vào bản ghi notification. Nếu FE đổi route mà BE không biết → chuông chết link. Cần quy ước: FE sở hữu bảng route là một artifact trong `contracts/`, BE chỉ được ghi DeepLink theo danh sách route đã chốt (relative path, ví dụ `/rentals/{id}`).
2. **KPI drill-down** (EXPERIENCE → Dashboards): "occupancy 87% → Unit Management filtered to Rented; surcharges → Reports tab Surcharges + cùng period" — yêu cầu state màn hình (filter, tab, period) phải encode được vào URL (query params), nếu không deep link từ KPI và từ notification không chia sẻ được cùng một cơ chế.
3. **"View rental" action** sau Payment success deep-link sang Rental Detail — cùng nhu cầu.
4. Tương tác với **Deferred OQ-2**: history-mode SPA cần server fallback về `index.html` (khác nhau giữa nhúng `static/` trong jar vs nginx). Chọn router/hash nên chốt cùng lúc với OQ-2.

**Đề xuất:** thêm 1 dòng vào Consistency Conventions (FE owns route table, deep-link = relative path theo contract) + gộp "SPA router & fallback" vào Deferred OQ-2. Không cần AD mới.

### 1.2 Toast + bell FR-33/34 (realtime hay pull) — **PULL-BASE ĐỦ (confirm, không phải gap)**

- Spine Capability Map đã ghi rõ: `Notification*`; FE toast/bell; pull-based; realtime = Deferred. Với demo capstone, TanStack Query `refetchInterval` đủ cho: badge unread trên chuông, Notification Center feed, mark-all-read.
- UX không yêu cầu gì vượt quá pull: toast là phản hồi tức thời của mutation chính nó (không cần push); bell entry tạo bởi cùng transaction backend; "unread persist across sessions" chỉ là query lại.
- **2 ghi chú nhỏ khi viết openapi.yaml** (không đổi AD): (a) **Operations Monitor** được UX mô tả là "Live view" — với pull nghĩa là polling, nên ghi rõ cadence (ví dụ 10–15s) trong contract để FE không tự chế; (b) cần endpoint riêng nhẹ cho unread-count (top bar) tách khỏi endpoint list full của Notification Center.

### 1.3 Kanban drag FR-21/22 — **gần đủ; thiếu hợp đồng lỗi "snap-back" (minor)**

- Đủ phần cứng: state transition nằm ở service (AD-4), FE không tự quyết (paradigm), error envelope có machine code (AD-8). UX đòi "kéo sang Done bị chặn → snap-back + toast chỉ đúng tên bước thiếu" và **keyboard alternative (Move buttons)** — a11y floor của UX yêu cầu drag không phải đường duy nhất.
- Thiếu: AD-8 envelope cần một **payload có cấu trúc cho blocked-transition** (guard nào, bước chốt còn thiếu tên gì) để FE render toast "thiếu bước X" từ dữ liệu thay vì parse message người đọc. Ghi bổ sung vào định nghĩa envelope trong openapi.yaml.
- FE-side: thư viện dnd (dnd-kit…) chưa nằm trong Deferred "FE component library" — mở rộng mục Deferred F1 thành "FE libs (component + dnd + chart)".

### 1.4 Upload ảnh bản ký FR-11/12 (binary upload) — **SPINE IM LẶNG HOÀN TOÀN (GAP-1, high)**

Đây là lỗ hổng lớn nhất của spine. Chuỗi nghiệp vụ lõi (contract step trong Check-in Task và Contract-signature card): *capture tile → thumbnail + Retake → Attach*; **access-code handover bị disable tới khi có ảnh** ( Printed→Signed là điều kiện cấp access code theo state chart Contract); ảnh bản ký hiển thị "viewable forever" cho customer trên Rental Detail (contract chain). Model V3 có sẵn chỗ chứa: `CONTRACTS.SignedPhotoUrl`, `CONTRACT_ADDENDUMS.SignedPhotoUrl` (VARCHAR 255). Nhưng spine không trả lời bất kỳ câu nào:

- **Đường upload**: multipart POST `/api/v1/...` hay base64 trong JSON? AD-2 chỉ nói REST JSON contract.
- **Lưu đâu**: filesystem trong deploy? DB BLOB? (không có object storage trong stack). Structural seed không có module file/storage nào.
- **Phục vụ thế nào**: ảnh bản ký là dữ liệu cá nhân (chữ ký) — không thể là URL public; phải qua download endpoint có check quyền theo role + ownership. Ai được GET (customer của rental đó, staff, manager)?
- **Ràng buộc**: giới hạn size/format, nén ảnh chụp camera điện thoại.

Cả 2 flow P1 (FR-11 contract check-in, FR-12 addendum signature) đều đứng trên nền này — **không được để Deferred kiểu "chốt sau"**, tối thiểu cần một AD-10 (hoặc OQ kèm default): ví dụ *"Attachment = upload multipart qua `/api/v1/attachments`, lưu filesystem local theo key, phục vụ qua endpoint có authorize, entity giữ path; không URL public"* — và quyết định serving tương thích với OQ-2 (jar static vs nginx).

### 1.5 Export report FR-31 (P2, CSV) — **gần đủ (minor)**

UX: "Export CSV button per tab" (Revenue / Deposits / Surcharges / Occupancy, period presets + custom range). Spine map đúng chỗ ("read-only query services; FE chart pages; AD-8"). Chỉ lưu ý: endpoint CSV là **ngoại lệ content-type** của "REST JSON dưới /api/v1" — thêm 1 dòng vào AD-8 hoặc contract: download `text/csv` (headers: filename, encoding UTF-8 BOM cho Excel tiếng Việt), generated-on-request, không lưu file. P2 nên chốt trong openapi.yaml nhưng không cần quyết kiến trúc mới.

### 1.6 Screenshot/attachment các loại — rà toàn bộ 29 màn

| Loại | Nơi xuất hiện trong UX | Model V3 | Spine | Đánh giá |
|---|---|---|---|---|
| Ảnh bản ký hợp đồng/phụ lục | Contract step (Check-in Task, Contract-signature card), Rental Detail chain | Có (SignedPhotoUrl ×2) | **Im lặng** | GAP-1 ở trên |
| **Ảnh unit** (Browse card "photo", Unit Detail "Photos") | Browse/Unit Detail — flagship | **KHÔNG có** — UNITS không có cột photo, không có entity UNIT_PHOTOS | Im lặng | **Lệch UX↔model**: 22 entity không chứa chỗ nào cho ảnh unit. Cần chốt 1 trong 2: (a) ảnh unit = static asset FE (`public/units/{code}.jpg`) cho demo — ghi 1 dòng convention; (b) thêm attribute/entity → phá con số 22. Khuyến nghị (a). |
| Ảnh inspection khi trả kho | Checkout Task checklist | Không — INSPECTIONS chỉ có Note | Im lặng | UX chỉ đòi text note + damage fee/reason → **không cần ảnh**, khớp model. Không gap. |
| Attachment ticket hỗ trợ | New Support | Không (description text) | — | UX không đòi → khớp. |
| Avatar (top bar) | Shell | Không | — | Initials/static — không gap. |
| QR VNPay | Payment Modal | — | AD-9 mock gateway | QR render phía FE theo tham số mock — nằm trong impl MockPaymentGateway, đủ. |

### 1.7 Các giả định nhỏ khác bắt được khi rà

- **Forgot password** (Auth screen): UX nói "reset link has been sent" nhưng sản phẩm **"No email, ever"** → không có email infra. Con đường reset thật là SysAdmin reset (SYS-01). Spine AD-5 nên ghi rõ forgot-password = endpoint stub trả message generic (anti user-enumeration), tránh BE dev tự chế email sender.
- **Temp password "shown once"** (SYS-01): cần trả password thẳng trong response một lần — mâu thuẫn nhẹ với thói quen "không trả secret qua API"; chấp nhận được cho demo, nên ghi chú trong contract.
- **Contract print**: UX có nút Print (in tại quầy) — bản in render từ đâu (FE print view của contract snapshot, hay PDF BE)? `ContentSnapshot TEXT` + FE render là hợp lý nhất; nên ghi 1 dòng để tránh ai đó làm PDF service.
- **Availability re-check on Reserve** ("S-3 was just reserved… bounce back") + conflict-check Extend ("Latest new checkout: Oct 18"): cần endpoint check tồn tính/idempotent-guard — thuộc chi tiết openapi (AD-2), không phải gap AD.

## Phần 2 — Model V3 check

### 2.1 Số lượng & tên state chart — **KHỚP AD-4 (confirm)**

7 file `mermaid/state-*.mmd` + 7 mục Section 3 trong `main.tex`:

| # | Tên thật trong LaTeX/mmd | AD-4 ghi | Khớp |
|---|---|---|---|
| 1 | Unit | Unit | ✅ |
| 2 | Reservation (gộp vòng đời Rental — quyết định V3) | Reservation | ✅ |
| 3 | Contract (hợp đồng gốc CT-) | Contract | ✅ |
| 4 | Contract Addendum (CT-…-A1) | Addendum | ✅ |
| 5 | Support Ticket | Ticket | ✅ |
| 6 | Task (kanban) | Task | ✅ |
| 7 | Payment | Payment | ✅ |

Không state chart thứ 8 ẩn nào (ERD là sơ đồ thứ 8 nhưng không phải state chart). Mỗi giá trị ENUM Status ở Section 2 khớp từng trạng thái trên sơ đồ — không lệch.

### 2.2 Số lượng entity — **KHỚP: đúng 22 (confirm)**

Đếm từ §2.2 main.tex và erd.mmd (8 nhóm): ROLES, USERS, FACILITIES, ZONES, STAFF_ASSIGNMENTS (5) · UNIT_TYPES, UNITS (2) · RENTAL_POLICIES, POLICY_RULES (2) · RESERVATIONS, EXTENSIONS, CHECKOUT_REQUESTS (3) · CONTRACTS, CONTRACT_ADDENDUMS (2) · PAYMENTS, SETTLEMENTS, INSPECTIONS (3) · SUPPORT_TICKETS, ESCALATIONS (2) · TASKS, NOTIFICATIONS, ACTIVITY_LOGS (3) = **22**, kèm **31 quan hệ** (bao gồm self-ref UNITS→UNITS qua MergedIntoID). Spine scope "22 entity, 7 state chart" — đúng con số.

### 2.3 AD-6 khớp V3 — **khớp về Flyway/append-only; CẦN LÀM RÕ wording owner (GAP-4, medium)**

- **Flyway V1 = toàn bộ model V3**: khả thi — 22 entity đều có thuộc tính đầy đủ trong main.tex/erd.mmd, đủ diện để sinh DDL V1.
- **ActivityLog append-only**: khớp — main.tex mô tả ACTIVITY_LOGS "chỉ INSERT, cấm UPDATE/DELETE ở tầng ứng dụng", spine AD-6 nói y hệt. ✅
- **"Mỗi entity đúng một owner service được phép ghi"** — wiring này **đúng nhưng dễ đọc sai** đối chiếu capability map: `UNITS.Status` chuyển trạng thái từ booking (deposit→Reserved), check-in (Rented), checkout (Preparing), cleaning task xong (Task*→Available), escalate severe (Ticket*→Maintenance), manager ops (UnitMgmt*), merge/retire — và `RESERVATIONS.Status/EndDate` được ghi từ cả `Reservation*` (Booking) lẫn `Rental*` (check-in/extension/checkout). Nếu hiểu AD-6 theo nghĩa đen "chỉ một service module duy nhất được ghi" thì map FR-4…FR-20 vi phạm ngay. Cách hiểu đúng (và nhất quán với AD-4 "transition đi qua service method của module owner"): **owner = service class sở hữu repository của entity; module khác muốn ghi phải gọi service đó, không bao giờ đụng repository/entity của nó.** Đề xuất sửa 1 câu trong AD-6 để chốt cách hiểu này trước khi 2 BE chia module.

### 2.4 Các quan hệ đã chốt — **KHỚP hết (confirm)**

| Quan hệ đã chốt | Bằng chứng trong V3 | Kết luận |
|---|---|---|
| RESERVATIONS 1—1 CONTRACTS | erd: `RESERVATIONS \|\|--\|\| CONTRACTS : "sinh"`; CONTRACTS.ReservationID có UNIQUE (1:1); auto-draft đúng 1 hợp đồng mỗi đặt cọc | ✅ |
| CONTRACT_ADDENDUMS tách riêng | entity độc lập, CONTRACTS 1—N addendums, EXTENSIONS 1—N addendums (ExtensionID NULL cho phụ lục loại khác); bỏ self-FK ParentContractID cũ; terminal EXPIRED/VOIDED khớp state chart addendum V3 | ✅ |
| ROLES 1—N USERS | erd: `ROLES \|\|--o{ USERS`; USERS.RoleID FK | ✅ |

### 2.5 Xung đột kiểu dữ liệu tiền — **GAP-2 (high): DECIMAL(18,2) vs AD-7 DECIMAL(15,0)**

- Model V3 (main.tex §1.4 + mọi cột tiền của 6 entity: DepositAmount, ExtensionFee, Value/Cap, Amount, DamageFee, RefundAmount) ghi **`DECIMAL(18,2)`**.
- Spine AD-7 chốt **`DECIMAL(15,0)`, đơn vị đồng, cấm phần thập phân** — đúng cho VND (không có tiền lẻ) và sinh sau (2026-09-21/22).

Flyway V1 không thể simultaneously theo cả hai. Vì spine là artifact sau và AD-7 là ADOPTED, **V1 theo AD-7 (15,0)**; cần ghi đè minh bạch: thêm 1 dòng vào AD-6/AD-7 ("V1 dịch kiểu tiền của model V3 về DECIMAL(15,0) theo AD-7") và/hoặc note ở main.tex để người đọc LaTeX không copy `18,2` vào DDL. DECIMAL(15,0) (~10^15) dư sức chứa doanh thu demo.

### 2.6 Lệch nhỏ bắt thêm ở V3 (không chặn spine, nên ghi lại để sprint đầu xử lý)

1. **NOTIFICATIONS thiếu mốc thời gian**: entity không có `CreatedAt`, nhưng UX Notification Center "grouped by day" + sắp thứ tự — cần thêm `CreatedAt DEFAULT CURRENT_TIMESTAMP` khi viết V1 (không đổi số 22).
2. **ESCALATIONS 0..1 mỗi ticket** (TicketID UNIQUE) vs state chart Support Ticket cho phép vòng InProgress→Escalated lặp lại nhiều lần — escalation lần 2 không có chỗ ghi. Hoặc nới 1—N, hoặc ghi chú "mỗi ticket chỉ escalate 1 lần" là quy ước v1.
3. **INSPECTIONS.Item enum** = ACCESS_CARD/PADLOCK/CLEANLINESS/STRUCTURE vs UX Checkout Task checklist "walls, door, floor, cleanliness" — taxonomy chưa khớp chữ; rà lại khi freeze openapi + V1.
4. **Ảnh unit không có chỗ trong model** (đã nêu 1.6) — chốt convention static asset FE hoặc thêm attribute.
5. USERS không có LastLogin nhưng SYS-01 hiển thị "last login" — nguồn = truy vấn ACTIVITY_LOGS (LOGIN events); nhất quán với "Login & Activity History là pointer surface, không recompute" → OK, chỉ cần biết derive ở đâu (chấp nhận truy vấn log).

## Bảng findings tổng hợp

| ID | Mức | Nội dung | Hành động đề xuất |
|---|---|---|---|
| GAP-1 | High | Spine im lặng về binary upload/lưu trữ/phục vụ ảnh bản ký (FR-11/12, P1; SignedPhotoUrl ×2 entity; ảnh là guard cấp access code) | Thêm AD-10 (multipart qua `/api/v1/attachments`, filesystem local, download có authorize) hoặc OQ kèm default; ăn khớp OQ-2 |
| GAP-2 | High | DECIMAL(18,2) (model V3) mâu thuẫn DECIMAL(15,0) (AD-7) — Flyway V1 phải chọn | Ghi rõ V1 theo AD-7; note đè tại nguồn LaTeX |
| GAP-3 | Med | Chưa có quy ước SPA routing/deep link: NOTIFICATIONS.DeepLink do BE lưu, KPI drill-down cần URL state, history fallback phụ thuộc OQ-2 | 1 dòng Convention (FE owns route table trong contracts/) + mở rộng OQ-2 |
| GAP-4 | Med | AD-6 "1 owner service" dễ hiểu sai: UNITS.Status/RESERVATIONS bị ghi từ nhiều module (Booking/Rental/Task/Ticket/UnitMgmt) | Sửa wording AD-6: owner = sở hữu repository; module khác gọi qua service owner |
| MIN-1..8 | Low | Snap-back payload (AD-8); CSV content-type ngoại lệ; Operations Monitor cadence + unread-count endpoint; forgot-password stub no-email; temp password one-time; print contract = FE render; FE libs (dnd/chart) vào Deferred F1; NOTIFICATIONS.CreatedAt, ESCALATIONS 0..1 vs re-escalate, INSPECTIONS enum vs UX checklist, ảnh unit static asset | Ghi vào openapi.yaml / conventions khi dựng repo; không chặn spine |

## Verdict

**PASS-WITH-CONDITIONS.** Spine khớp Model V3 ở toàn bộ các mục kiểm chứng số liệu (đúng 7 state chart với tên khớp AD-4; đúng 22 entity / 31 quan hệ; RESERVATIONS 1—1 CONTRACTS, CONTRACT_ADDENDUMS tách riêng, ROLES 1—N USERS; ActivityLog append-only) và các quyết định pull-based (FR-33) + kanban guard (FR-21) đều đứng vững trên AD-4/AD-8. Điều kiện trước khi freeze spine/dựng repo: xử lý GAP-1 (đường đi của ảnh bản ký — P1 mà spine đang im lặng) và GAP-2 (chốt DECIMAL tiền cho Flyway V1); nên luôn làm rõ GAP-3 (deep link) và GAP-4 (wording owner service).
