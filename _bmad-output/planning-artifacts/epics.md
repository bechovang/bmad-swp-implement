---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - docs/planning/prds/prd-storagehub-2026-09-17/prd.md
  - docs/planning/architecture/architecture-storagehub-2026-09-21/ARCHITECTURE-SPINE.md
  - docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md
  - docs/planning/ux-designs/ux-swp391-2026-09-11/EXPERIENCE.md
---

# StorageHub - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for **StorageHub** (Self-Service Storage Rental and Management System — SWP391), decomposing the requirements from the PRD (final 2026-09-21), the Architecture Spine (final 2026-09-22), and the UX design contract (DESIGN.md + EXPERIENCE.md, final 2026-09-21) into implementable stories.

**Mockup reference (không extract, chỉ tham chiếu):** Stitch project "StorageHub SWP391 Control Room" — https://stitch.withgoogle.com/projects/9794980915577530548 (~41 screens, cùng identity "Control Room" với DESIGN.md; HTML local là bản chuẩn cho F2-04 Rental Detail và F6-01 Extend theo PRD §0).

**Source precedence (theo PRD §0):** PRD thắng về phạm vi và yêu cầu; EXPERIENCE.md thắng về chi tiết tương tác UI; ARCHITECTURE-SPINE là chân lý kỹ thuật (AD-1…AD-11).

## Requirements Inventory

### Functional Requirements

*41 FR (đánh số toàn cục PRD §4). P1 = bắt buộc demo 6 UJ trọn; P2 = hoàn thiện, cắt không phá flow.*

- **FR-1 (P1):** Login email + password; đổ landing theo role (Customer → Browse Units; Staff → Task Board; FM → Facility Overview; BO → Business Overview; SysAdmin → User Management); sai thông tin → inline error không tiết lộ field; URL chéo role → 403 đơn giản; avatar menu (profile, logout) mọi role.
- **FR-2 (P1):** Customer self-register (họ tên, phone, email, password + xác nhận, đồng ý điều khoản); thành công → Browse Units đã đăng nhập; form không có lựa chọn role.
- **FR-3 (P2):** Forgot password qua modal trên Login; phản hồi generic "If an account exists…"; không email thật.
- **FR-4 (P1):** Browse Units — filter loại/kích thước/ngày bắt đầu/thời hạn; lưới card + chip đếm "N units available · live"; sort mặc định giá thấp → cao; availability tính theo Reservation + Turnover Buffer tại thời điểm query; card buffer "Available {date} · cleaning buffer" vẫn đặt được; filter re-query khi bấm Search, persist phiên, echo chip removable ở empty state.
- **FR-5 (P1):** Reserve re-check chống stale — kiểm tra lại availability khi bấm Reserve; Unit bị chiếm → quay lưới + toast gợi ý unit tương tự; không bao giờ booking trên ngày invalid (chống double-booking, 409).
- **FR-6 (P1):** Unit Detail — spec đầy đủ (kích thước, tầng, access, an ninh) + bảng giá: rent × duration + từng dòng phụ thu từ Rental Policy active + Deposit đánh dấu refundable; khớp tuyệt đối Booking Summary và Payment Modal.
- **FR-7 (P1):** Booking Summary — duyệt toàn bộ dòng sẽ bị truy đòi; ghi chú Contract auto-draft từ đúng điều kiện này, ký tại check-in; CTA mở Payment Modal (Deposit); không dòng tiền nào xuất hiện ở payment mà vắng ở đây.
- **FR-36 (P1):** Reservation hết hạn no-show — hết ngày nhận kho chưa check-in → EXPIRED: mất cọc (receipt "Deposit forfeited — no-show"), Unit → Available, Contract Draft/Printed → Closed, Activity Log, notification khách; EXPIRED là lối ra rủi ro duy nhất, không có cancel trong v1.
- **FR-8 (P1):** Payment Modal dùng chung 4 touchpoint (Deposit, 100% rent, Extension fee, Extra fee) — **QR PayOS** (payment link thật `vn.payos:payos-java` 2.0.1; modal render QR từ checkoutUrl + poll tới khi webhook verify checksum xác nhận; đếm ngược theo link expiry do BE trả) + **Cash tại quầy** (chỉ touchpoint có staff: 100% rent, Extension fee, Extra fee — chọn Cash tạo payment PENDING_CASH, staff bấm "Cash received"; Deposit booking online = QR only, không có lựa chọn Cash); success check tile + amount + consequence line; fail "No money was taken" + Retry + Switch method; fail/hết hạn link không đổi trạng thái (link bị cancel); sau 2 lần fail gợi ý đổi; awaiting confirmation khóa điều hướng + nút Cancel tường minh *(sửa PayOS 2026-09-29 — thay mock Card/MoMo/VNPay)*.
- **FR-9 (P1):** Payment success ghi sổ — xác nhận từ đúng 1 nguồn: webhook PayOS (verify checksum) hoặc staff cash-confirm; Receipt (ghi rõ method QR/CASH) vào Rental Detail + toast + bell; flip trạng thái tương ứng (Reservation confirmed, Rental active, ngày checkout mới); mỗi payment thành công sinh đúng 1 receipt xem được vĩnh viễn.
- **FR-10 (P1):** Auto-draft Contract — sinh Draft ngay khi Deposit thành công; mã `CT-xxxx`; khóa phiên bản policy; read-only mọi nơi; sai sót sửa bằng re-draft — bản cũ → Superseded, vẫn đọc được trong chuỗi.
- **FR-11 (P1):** Check-in contract ritual — preview read-only → Print → chụp ảnh bản ký → Attach; nút trao Access Code vô hiệu đến khi có ảnh bản ký; Attach ghi `CONTRACT_SIGNED` Activity Log; không kích hoạt Rental khi Contract chưa Signed.
- **FR-12 (P1):** Addendum cho Extension — sinh `CT-…-A1` sau khi trả phí gia hạn; deadline ký tại quầy 7 ngày; amber banner trên Rental Detail + card Contract-signature trên Task Board + bell reminder hai bên; quá hạn → reminder tăng cường, không thu hồi ngày; hoàn tất card khi ảnh bản ký đính kèm → Signed.
- **FR-13 (P1):** Contract chain trên Rental Detail — chuỗi Contract gốc + Addenda: mã, badge trạng thái, ảnh bản ký; bản Signed xem được vĩnh viễn; bản Superseded vẫn đọc được.
- **FR-14 (P1):** Validate reservation tại quầy — nhập/mã reservation → validate tồn tại + Deposit đã trả; không hợp lệ → lý do cụ thể, chặn tiến trình.
- **FR-15 (P1):** Thu 100% rent + bàn giao — Payment Modal ngay tại Check-in Task; trao Access Code (sau Contract Signed); Rental ACTIVE, Unit RENTED, task hoàn tất; Deposit và rent full hiển thị tách bạch.
- **FR-16 (P1):** Extension với conflict boundary — chọn ngày checkout mới → kiểm tra Reservation kế tiếp; vùng xung đột đánh dấu trên picker; submit chặn kèm biên ("Latest new checkout: Oct 18"); phí = tiền thuê kỳ thêm theo Rental Policy active + top-up cọc = max(0, Deposit% × tổng tiền thuê sau gia hạn − cọc đang giữ); ngày mới hiệu lực ngay sau thanh toán; chỉ mở trước EndDate — qua EndDate giữ CHECKED_IN + phụ thu LATE_FEE tại Settlement.
- **FR-17 (P1):** Checkout Request — gửi từ Rental Detail kèm giải thích logic tất toán; Rental → CHECKOUT_REQUESTED; Checkout Task sinh ra; request mới đè request cũ; ngày xin trả bị chặn trên biên Reservation kế tiếp.
- **FR-18 (P1):** Checkout Task — nhận Unit + key (checklist) → Inspection từng hạng mục (access card, padlock, cleanliness, structure; OK/MINOR/MAJOR; MAJOR là căn cứ Settlement Charge) → khai Settlement Charge (số tiền + reason bắt buộc khi damage) → settlement preview (Deposit − charges = refund; charges vượt Deposit → khách trả phần chênh qua Payment Modal touchpoint Extra fee) → confirm đóng Rental; không đóng khi charge thiếu reason hoặc phần chênh chưa trả; receipt hai bên vĩnh viễn; Unit → Preparing; Cleaning Task sinh ra.
- **FR-41 (P2):** Settlement waiver trong trần WAIVER_CAP — giảm/miễn charge kèm reason bắt buộc → Activity Log; khách thấy dòng điều chỉnh + lý do trên receipt; vượt trần → nút vô hiệu kèm nêu trần; không áp dụng cho no-show forfeiture.
- **FR-19 (P1):** Cleaning hoàn tất từ card — kiểm tra Turnover Buffer trước khi Unit → Available (hoặc Reserved nếu có Reservation kế); buffer chưa đủ → giữ Preparing, card không hoàn tất.
- **FR-20 (P1):** Unit status guards — mọi chuyển Maintenance/Retired qua guard + confirm; sửa lệch thực tế qua "fix status" + reason bắt buộc → Activity Log; không silent status write; kết thúc Maintenance luôn qua Preparing + Cleaning Task theo buffer.
- **FR-21 (P1):** Kanban 5 loại card (Check-in/Checkout/Cleaning/Support/Contract-signature) × 3 cột (To do/In progress/Done); tab lọc theo loại; số lượng từng cột ở header; kéo thả hoặc keyboard Move; card Done render muted.
- **FR-22 (P1):** Snap-back chống Done ảo — kéo Check-in/Checkout/Contract-signature vào Done thiếu closing step (payment/settlement/ảnh bản ký) → card bật lại + toast nêu đúng bước thiếu (payload cấu trúc từ BE).
- **FR-23 (P1):** Tạo + route Support Ticket — Unit + incident type (LOST_ACCESS/DEVICE_ISSUE/SECURITY/CLEANLINESS/OTHER) + mô tả → route tới Staff trực ca theo Unit + shift; ticket không có Unit hợp lệ không gửi được.
- **FR-24 (P1):** Resolve / Escalate — Resolve kèm note (khách nhận plain words) hoặc Escalate kèm note bắt buộc → Escalation Inbox của FM; Escalate không note → nút vô hiệu.
- **FR-25 (P1):** Severity Decision + Relocation — severe → Unit chuyển Maintenance + Relocation (đổi Unit trên Rental hiện tại, giữ mã RT-/Contract/Deposit, access code mới, Activity Log RELOCATION) + tasks bảo trì/dọn + khách nhận notification từng bước; không severe → ticket về lại staff kèm hướng dẫn; sau mọi decision ticket về IN_PROGRESS, chỉ Resolved khi staff hoàn tất + note; ticket Resolved hiển thị trọn arc trong một drawer.
- **FR-26 (P1):** Unit Management + edit drawer — bảng Unit (code, size, type, zone/floor, status badge, link rental/reservation active, last activity) + drawer: sửa specs, Retire (guard + confirm), set Maintenance, Fix status (reason bắt buộc); Merge = P2 **không build v1** (quyết định 2026-09-22); retire bị chặn → blocker nêu tên + link; mọi write → Activity Log.
- **FR-27 (P1):** Staff & Shifts + conflict detection — phân công staff × Zone × ca (MORNING/AFTERNOON/EVENING) × ngày; conflict bị từ chối trước khi save kèm collision cụ thể + slot highlight; lưới lịch tuần; không lưu phân công trùng.
- **FR-28 (P1):** Activity Log append-only — timestamp, actor, entity, action, from → to, reason; lọc theo entity type; read-only; không UI xóa/sửa.
- **FR-29 (P1):** Policy Management validate-trước-khi-lưu — sửa inline bảng rule (base rent theo Unit Type, phụ thu + trần); Save chạy validation đầy đủ; giá trị vi phạm cắm cờ + banner nêu đúng luật; policy không hợp lệ không bao giờ persist; Save thành công đóng dấu version + effective date.
- **FR-30 (P1):** Policy chứa Turnover Buffer (ngày) + Deposit % như hai Policy Rule theo phiên bản; mọi tính toán availability/deposit đọc từ policy active; đổi chỉ ảnh hưởng từ phiên bản effective.
- **FR-40 (P2):** Discount % theo Unit Type — Policy Rule (RuleType `DISCOUNT`); dòng giảm giá ở Unit Detail + Booking Summary; giá sau giảm là căn cứ Contract auto-draft và tiền cọc; trần discount mặc định 50%.
- **FR-31 (P1 dashboards, P2 export):** Business Overview — KPI revenue / deposits held / surcharges / utilization + charts; Reports: tab Revenue/Deposits/Surcharges/Occupancy + preset kỳ + custom range + Export CSV (P2); mọi KPI bấm xuyên xuống bảng lọc đúng slice; deposits held không tính là revenue.
- **FR-32 (P1 overview, P2 monitor):** Facility Overview — occupancy / revenue mix / unit-status KPI + charts + bảng nền + KPI drill-down; Operations Monitor (P2): live tasks, escalations, turnover-buffer queue.
- **FR-33 (P1):** Toast + Bell — toast tức thời ~4s hover pause, ≤1 action link; sự kiện tiền phát cả toast lẫn bell; bell badge đếm unread; không email.
- **FR-34 (P1 feed, P2 day-grouping):** Notification Center — feed theo role; unread trước; deep link tới đối tượng; mark-all-read; unread persist giữa phiên; nhóm theo ngày (P2); khách nhận resolution plain words, staff nhận task assignment, manager nhận escalation + status writes, BO nhận policy save + export completion.
- **FR-35 (P1):** Ba màn neo phía khách — My Rentals (danh sách Reservations + Rentals + History, entry point tới Rental Detail/Extend/Checkout Request/Support); Check-in Pass (mã `BK-` hiển thị lớn); Support List (danh sách ticket + detail drawer); pointer surfaces — khớp nguồn, không tự tính lại; empty state có CTA trỏ flow đổ dữ liệu.
- **FR-37 (P1):** User Management SYS-01 — bảng user (họ tên, email, phone, role, status, last login) + search/filter; tạo tài khoản mọi role với mật khẩu tạm (trả 1 lần); đổi role kèm Activity Log; activate/deactivate/lock theo `users.Status` (0/1/2); không delete; reset password = mật khẩu tạm mới; email trùng chặn inline; đăng nhập bằng tài khoản deactivate/lock → message chung.
- **FR-38 (P1):** Login & Activity History SYS-02 — ghi `LOGIN`/`LOGIN_FAILED` vào Activity Log (EntityType USER) mỗi lần đăng nhập thành công lẫn thất bại; xem filter theo user/thời gian/kết quả; pointer surface read-only.
- **FR-39 (P2):** Permission matrix read-only — bảng Role × Permission (menu, hành động, phạm vi facility) đúng như server enforce; cố định trong code, không chỉnh runtime; Staff scope theo Zone, FM theo cơ sở, BO/SysAdmin toàn hệ.

### NonFunctional Requirements

*8 NFR (PRD §5) — ràng buộc cho mọi FR, không lặp trong từng FR.*

- **NFR-1:** Ngôn ngữ & định dạng — UI English-only; tiền VND full precision `1.150.000 ₫` (dấu chấm ngăn cách, ₫ sau số, không viết tắt); tabular numerals; định dạng ngày nhất quán toàn app.
- **NFR-2:** Accessibility floor — mọi input có label; focus visible; thao tác chính chạy bằng keyboard (kanban có Move ngoài drag); status không chỉ phân biệt bằng màu; click target ≥ 40px; focus trap trong modal/drawer + trả về trigger; empty state luôn có bước kế; toast không bao giờ thay thế bell.
- **NFR-3:** Platform — web responsive desktop-first; mục tiêu chính 1200px; light mode only; cửa sổ hẹp hơn vẫn dùng được.
- **NFR-4:** Security baseline — password hash at rest (BCrypt); mọi route app yêu cầu đăng nhập; role enforcement phía server (ẩn menu chỉ là UX); permission matrix cố định server-side; tài khoản deactivate/lock không đăng nhập được; Access Code qua cơ chế reveal (SensitiveValue); mọi thay đổi trạng thái quan trọng có reason ghi audit.
- **NFR-5:** Hiệu năng quy mô demo — tính đúng availability ưu tiên hơn tốc độ; trang load < 2s trên demo với seed data; thanh toán PayOS thật (không còn mock deterministic) — demo cần internet + credentials env + webhook URL public.
- **NFR-6:** Data integrity — ActivityLog append-only ở tầng dữ liệu; mọi biến động tiền sinh receipt/record; không silent write trạng thái.
- **NFR-7:** Voice & microcopy — mọi error/empty/toast nêu: chuyện gì xảy ra + hệ quả tiền/trạng thái + đúng một bước kế tiếp; register plain cho 5 role; cấm exclamation mark, streak-cheer, marketing verbs.
- **NFR-8:** States & form discipline — skeleton khớp layout (không layout shift); empty state factual + echo filter + đúng 1 CTA; block server hiển thị banner đầu form (không phải toast); Submit chỉ disable khi thiếu required; guard unsaved-changes khi rời màn inline edit; bảng quản trị pagination 25 rows.

### Additional Requirements

*Từ ARCHITECTURE-SPINE (AD-1…AD-11 + Conventions + Stack + Structural Seed).*

- **Starter template (Epic 1 Story 1):** Greenfield monorepo theo Structural Seed — `storagehub/` chứa `backend/` (Spring Boot), `frontend/` (Vite), `contracts/` (openapi.yaml + routes.yaml), `docs/`. Stack pin: **Spring Boot 4.1.1 / Java 21 LTS** (starters: webmvc · data-jpa · security · validation · flyway · actuator), **React 19.3.0 + Vite 8.3.0**, **MySQL 8.4.11** (+ `flyway-mysql`), TanStack Query + Axios, MSW 2.x, JUnit Jupiter 6.0.3 + Mockito 5.23.0 + AssertJ 3.27.7, Vitest + React Testing Library, **PayOS SDK `vn.payos:payos-java` 2.0.1** (verify compat Boot 4 khi init). Boot 4 gotchas: starter `web` đổi tên `webmvc`; test starter tách module (`-security-test` cho `@WithMockUser`); Jackson 3 (`tools.jackson`); Flyway cần `flyway-mysql`.
- **AD-2 Contract-first:** `contracts/openapi.yaml` là nguồn chân lý API duy nhất; đóng băng đầu sprint; đổi contract trước code sau; FE phát triển chống MSW sinh từ contract; springdoc chỉ render docs, cấm dùng làm nguồn contract.
- **AD-3 Layer-first:** controller chỉ HTTP ↔ DTO + gọi đúng một service; mọi business logic + transaction ở service; repository chỉ query; cấm controller gọi repository; entity JPA không rời backend; module khác đọc chéo qua service query công khai, derive-on-read, cấm denormalize.
- **AD-4 Backend độc quyền state & thời gian:** mọi state transition qua owner service; trạng thái phái sinh (EXPIRED, buffer) tính on-read idempotent, trả trong DTO, không persist ngược; side-effect suy diễn theo thời gian exactly-once lazily guarded bằng idempotency key (unique constraint); cấm scheduled batch side-effect song song.
- **AD-5 Auth & permission:** JWT Bearer, role claim, TTL 24h, không refresh token, logout = drop token; permission matrix enforce server-side từng endpoint; BCrypt; LOGIN/LOGIN_FAILED ghi Activity Log; mỗi request check `users.Status` active (cache ngắn); đúng 4 endpoint public (`POST /auth/login|register|forgot-password` + `POST /api/v1/payments/webhook` PayOS — xác thực checksum, không JWT); Access Code không nằm trong response danh sách — chỉ qua endpoint reveal có permission.
- **AD-6 Schema & ownership:** Flyway sở hữu schema (V1 = model V3 22 entity; V2__seed_demo chỉ profile dev, chứa bộ mock chuẩn Lan/Minh/Hằng/Tuấn/Nam, S-3/M-2/M-5, BK-1042/RT-0871/SR-0032/CT-1042/CT-1042-A1, policy v3); mỗi entity đúng một owner service (ownership matrix 14 dòng — UserService, UnitService, StaffingService, PolicyService, ReservationService, ExtensionService, CheckoutService, ContractService, PaymentService, TicketService, TaskService, NotificationService, LogService); ActivityLog chỉ INSERT; chống double-booking bằng transaction guard unique/optimistic (Unit, ngày) — thua nhận 409. V1 deltas: tiền `DECIMAL(15,0)`; `CONTRACTS` 1—N qua `SupersedesContractID` + `IsLatest`; `POLICY_RULES.RuleType` thêm `TURNOVER_BUFFER`/`DISCOUNT`/`WAIVER_CAP`; `users.FacilityID` nullable; `NOTIFICATIONS.CreatedAt`; `ESCALATIONS.TicketID UNIQUE` (escalate đúng 1 lần); `INSPECTIONS.Item` enum; ảnh unit = static asset FE (`public/units/{code}.jpg`).
- **AD-7 Money & time:** VND = `BigDecimal` + `DECIMAL(15,0)` + JSON number, cấm float/double; tính tiền chỉ ở service; thời gian lưu UTC `Instant`, vận chuyển ISO-8601 kèm offset, hiển thị `Asia/Ho_Chi_Minh`; biên nghiệp vụ theo ngày dùng `DATE`/`LocalDate` ngữ nghĩa ICT, quy đổi ở service.
- **AD-8 Error & list envelope:** một envelope lỗi duy nhất (machine code + human message + field errors); pagination offset `{items, page, pageSize, total}` 1-based mặc định 25; enum UPPER_SNAKE; JSON camelCase; envelope áp cả lỗi security (AuthenticationEntryPoint/AccessDeniedHandler); business-rule block = 409 + envelope; blocked-transition (snap-back) trả payload cấu trúc (guard vi phạm + closing step thiếu); filter/sort param kebab-case; ngoại lệ duy nhất: CSV export (P2) `text/csv` + UTF-8 BOM.
- **AD-9 Payment gateway PayOS sau interface (sửa 2026-09-29):** interface `PaymentGateway` + đúng một impl `PayOsPaymentGateway` (SDK `vn.payos:payos-java` 2.0.1; credentials `PAYOS_CLIENT_ID/API_KEY/CHECKSUM_KEY` env — team có sẵn tài khoản my.payos.vn; không dùng Payouts); nguồn sự thật = webhook PayOS verify checksum + status query `paymentRequests().get(orderCode)` + endpoint staff cash-received; `orderCode` sinh từ PaymentID (idempotent, unique constraint); FE chỉ render QR từ checkoutUrl + poll; link hết hạn/Cancel → FAIL/EXPIRED không side-effect; Payment vẫn là state machine 5 trạng thái của PaymentService. Ref code: `payos ref code/payos-demo-java-spring`.
- **AD-10 File storage (ảnh bản ký):** upload multipart `POST /api/v1/attachments`; lưu `backend/storage/` (gitignore); phục vụ qua controller stream đã qua JWT + permission; cột `*PhotoUrl` chỉ lưu API path tương đối; cấm static mapping công khai, cấm base64 trong DTO list.
- **AD-11 Pricing single-source:** một `PricingEngine` duy nhất; policy resolve tại thời điểm Booking Summary, snapshot vào reservation; contract auto-draft, check-in rent, extension fee, receipt dùng lại snapshot — không tính lại theo policy mới; report chỉ tổng hợp từ receipt/payment đã ghi; deposits held ≠ revenue.
- **Conventions:** naming REST danh từ số nhiều kebab-case, DTO `XxxRequest/Response`, FE component PascalCase + hook `useXxx`; TanStack Query cho server-state; mutation thành công trả kèm `notification` object → FE bắn toast; bell/unread-count qua polling riêng có endpoint nhẹ; DeepLink BE ghi theo `contracts/routes.yaml` (relative path), KPI drill-down encode filter/tab/period vào URL query params; JWT secret + DB credentials qua env vars; temp password trả trong response đúng 1 lần; log kỹ thuật SLF4J tách LogService; Print render từ `ContentSnapshot` bằng print view FE (không PDF service).
- **Quyết định Deferred chốt theo mốc:** FE libs (component + dnd + chart + **QR code lib**) + TypeScript — quyết **tuần 1**, không story FE nào dựng shared component trước lúc chốt; deploy demo (jar static vs docker-compose, cùng SPA fallback + URL public cho webhook PayOS) — tuần 6–8; realtime notification = pull-based đủ demo; health check duy nhất `/actuator/health`; bỏ backup/monitoring có chủ đích.
- **Ngữ cảnh đội hình & sprint (từ note 2026-09-22):** 3 sprint × 2 tuần; lane BE: An/Phúc/Huy; lane FE: Phú/Tuấn Anh. *(Sprint_Backlog.xlsx cũ đã xóa khỏi repo — epics/stories này là nguồn mới.)*

### UX Design Requirements

*Từ DESIGN.md + EXPERIENCE.md — first-class input, extract đầy đủ để sinh story có AC test được.*

- **UX-DR1: Design token system "Control Room".** Cài đầy đủ token: surface ramp (app-bg #F4F6F9 → surface → surface-subtle → surface-muted → border → border-strong → divider); text ramp 4 bậc (ink/ink-secondary/muted/faint); brand indigo #4F46E5 + primary-tint/primary-border/primary-outline-border (indigo = action/selection, không phải trang trí); 7 status semantics đủ bộ dot/tint/border (Available, Buffer, Reserved, Rented, Preparing, Maintenance, Retired); feedback (success/warning/warning-bar/error + tints); typography ramp 14 style (display 21, kpi-value 24, headline 18, body 13, body-strong, meta 11.5, label 10.5, button, nav, price 15, price-lg 19, code, code-sm — system fonts + mono cho code); radius md 8px mọi surface / sm 6px chips / full chỉ dots; spacing scale 4→48 + page-x 24px + nav-height 54px; elevation = border + 1px shadow thì thầm, hover darkens border, chỉ modal/drawer được shadow thật.
- **UX-DR2: Numerals & VND formatting.** `font-variant-numeric: tabular-nums` cho mọi giá, ngày, count, KPI, cột số; VND dot-separated full precision, ₫ sau số (`1.150.000 ₫`), cấm `1.15tr`; unit code (`S-3`, `M-2`) render mono.
- **UX-DR3: Status-first surfaces.** 3px left status bar trên mọi card unit/task (signature move); status badge tint + border + label, never color-only; 7 nhãn unit lifecycle hiển thị đúng語 nghĩa từng trạng thái (Available=green go, Buffer=amber attention, Reserved=indigo commitment, Rented=slate steady, Preparing=transient sky, Maintenance=hot orange, Retired=recede grey).
- **UX-DR4: Adaptive shell.** Một app frame, 5 role: top bar trái→phải = logo + role menu + role chip (primary-tint pill) + bell unread + avatar menu; 5 nav menu theo role (Customer: Browse Units·My Rentals·Support; Staff: Tasks·Support; FM: Overview·Units·Staff & Shifts·Operations·Activity Log·Escalations; BO: Overview·Policy·Reports; SysAdmin: Users·Login History); overlay discipline: modal không mở từ modal, drawer không stack trên modal, một tầng sâu.
- **UX-DR5: Thư viện component chuẩn.** Buttons primary/secondary/ghost 36px (page-level 40px), destructive = secondary + error text, đúng 1 primary/card; input 36px label trên luôn hiển thị, focus ring 2px indigo, invalid border + message; card nền trắng + border + shadow + status bar; badge 6px/3px-8px; KPI card (label + kpi-value tabular + delta chip, không sparkline trong card); table sticky header 40px rows, numeric right-align tabular, hover row, cấm zebra, row action ghost; kanban card type-to-color (Check-in indigo/Checkout sky/Cleaning amber/Support red/Contract slate, Done 60% muted mất bar); modal max 480px actions right-aligned; toast bottom-right ~4s hover-pause 3px accent ≤1 action; drawer 420px phải Esc/scrim; contract step panel (preview tile mono code + tabular lines, Print secondary, capture tile dashed → thumbnail + Retake, Attach primary disabled-trừ-khi-có-ảnh); tabs 2px underline; empty state (56px icon tile, title factual, mono chips echo filter, đúng 1 CTA); skeleton layout-matching; filter bar (dropdown đóng, re-query trên Search, count chip live refresh).
- **UX-DR6: Accessibility floor triển khai.** Label visible mọi input (cấm placeholder-only); focus ring visible; keyboard reach toàn app (nav, tables, drawers, modals, kanban Move ◀/▶ + status menu); status dot/badge + text luôn; target ≥ 40px; focus trap modal/drawer + trả về trigger; mỗi màn P1 pass một lượt keyboard trước demo.
- **UX-DR7: State & form patterns.** Loading = skeleton khớp layout, swap không layout shift; empty state factual ("0 of 42 units meet all four criteria") + echo filter + 1 CTA (Clear filters/Widen dates), kanban rỗng = "Nothing here" tile, first-run link flow đổ dữ liệu; validation inline on blur; server block = error-tint banner đầu form nêu lại luật (không phải toast); Submit disable chỉ theo missing required; inline edit (policy, staffing) click-to-edit, blur/Enter save, Esc cancel, unsaved-changes guard.
- **UX-DR8: Microcopy voice.** Precise, competent, low-ceremony SaaS English; khuôn 3 phần (chuyện gì + hệ quả tiền/trạng thái + 1 bước kế); dùng đúng 16 sample strings chuẩn của EXPERIENCE.md (booking summary, payment success ×3, contract drafted/signed, addendum reminder, payment fail, extension blocked, shift conflict, policy validation fail, status fix, retire confirm, escalation, severity decision, support resolved); cùng register plain cho cả 5 role.
- **UX-DR9: Responsive behavior.** Desktop ≥1200px full (3-col browse, đủ cột kanban, drawer 420px); tablet 768–1199 browse 2-col, KPI stack, table ẩn cột phụ, kanban ~1.5 cột; mobile <768 single column, filter collapse sau nút Filters, kanban scroll ngang + sticky column header, table → card list label:value, drawer full-width; overlay luôn là overlay (full-screen sheet mobile).
- **UX-DR10: Interaction primitives.** Drag-drop kanban + keyboard alternative (focus card → Move ◀/▶); Browse date/duration = dropdown đóng; Extend = calendar picker đánh dấu vùng xung đột + biên; Reports = preset + custom range; confirm-destructive modal nêu hệ quả tiền/trạng thái + typed reason nơi yêu cầu (retire, close rental with deduction, severity); cấm hover-only affordance, multi-level modal, infinite scroll bảng quản trị.
- **UX-DR11: Dashboard-first + KPI drill-down.** Mọi dashboard theo thứ tự KPI row (3–4 card) → chart block → backing table; mọi KPI clickable drill vào bảng pre-filter đúng slice (occupancy 87% → Unit Management filter Rented; surcharges quarter → Reports tab Surcharges cùng kỳ); filter/tab/period encode vào URL query params; không số liệu nào không với tới rows.
- **UX-DR12: Notification Center UX.** Role-scoped feed (khách: reservation/payment/support/contract; staff: task assignment; manager: escalation + status writes; BO: policy save + report); unread đầu + persist giữa phiên; deep link tới đối tượng affected; mark-all-read; bell unread count luôn error-red (tách kênh alert vs action); day-grouping P2.

### FR Coverage Map

- FR-1 → Epic 1 (login + landing theo role + 403 + avatar menu)
- FR-2 → Epic 1 (customer self-register, không role picker)
- FR-3 → Epic 1 (P2 — forgot password modal, generic response)
- FR-4 → Epic 2 (Browse Units + availability theo buffer)
- FR-5 → Epic 2 (Reserve re-check, 409 chống double-booking)
- FR-6 → Epic 2 (Unit Detail + bảng giá minh bạch)
- FR-7 → Epic 2 (Booking Summary — hợp đồng trước khi trả tiền)
- FR-8 → Epic 2 (Payment Modal QR PayOS + Cash tại quầy)
- FR-9 → Epic 2 (Payment success ghi sổ — webhook/cash-confirm)
- FR-36 → Epic 2 (no-show EXPIRED + mất cọc, exactly-once lazy)
- FR-10 → Epic 3 (auto-draft contract khi deposit thành công)
- FR-11 → Epic 3 (check-in contract ritual + access code lock)
- FR-13 → Epic 3 (contract chain trên Rental Detail; mở rộng addenda ở Epic 4)
- FR-14 → Epic 3 (validate reservation tại quầy)
- FR-15 → Epic 3 (thu 100% rent — QR/cash + bàn giao access code)
- FR-21 → Epic 3 (kanban 5 loại card, 3 cột, drag + keyboard Move)
- FR-22 → Epic 3 (snap-back chống Done ảo, payload cấu trúc)
- FR-16 → Epic 4 (extension + conflict boundary + top-up cọc)
- FR-12 → Epic 4 (addendum 7 ngày — banner + card + bell reminders)
- FR-23 → Epic 5 (tạo + route ticket theo Unit + shift)
- FR-24 → Epic 5 (resolve/escalate kèm note bắt buộc)
- FR-25 → Epic 5 (severity decision + relocation giữ Contract/Deposit)
- FR-35 → Epic 2 (My Rentals + Check-in Pass) + Epic 5 (Support List + drawer)
- FR-17 → Epic 6 (checkout request → CHECKOUT_REQUESTED + task)
- FR-18 → Epic 6 (checkout task: inspection + charge ép reason + settlement)
- FR-41 → Epic 6 (P2 — settlement waiver trong WAIVER_CAP)
- FR-19 → Epic 6 (cleaning từ card + buffer → Available/Reserved)
- FR-20 → Epic 7 (unit status guards + fix status reason; chân Preparing/Buffer ở Epic 6)
- FR-26 → Epic 7 (unit management + edit drawer; Merge P2 không build)
- FR-27 → Epic 7 (staff & shifts + conflict detection)
- FR-28 → Epic 7 (activity log append-only view)
- FR-32 → Epic 7 (facility overview + KPI drill-down; monitor P2)
- FR-29 → Epic 8 (policy validate-trước-khi-lưu)
- FR-30 → Epic 8 (buffer + deposit % là policy rule)
- FR-40 → Epic 8 (P2 — discount % theo unit type)
- FR-31 → Epic 8 (business overview + reports + drill-down; CSV P2)
- FR-33 → Epic 1 (toast + bell hai kênh)
- FR-34 → Epic 1 (notification center; day-grouping P2)
- FR-37 → Epic 9 (user management SYS-01)
- FR-38 → Epic 9 (login & activity history SYS-02)
- FR-39 → Epic 9 (P2 — permission matrix read-only)

*NFR-1…8 và UX-DR1…12 là ràng buộc xuyên suốt, nhúng vào AC từng story; UX-DR1 (token system) + UX-DR4 (adaptive shell) có story riêng ở Epic 1.*

## Epic List

### Epic 1: Platform Foundation & Identity *(Sprint 1)*
Sau epic này, mọi user đăng ký/đăng nhập được, đổ vào landing đúng role trong adaptive shell 5 role (role chip + nav theo role); feedback toast + bell hoạt động兩 kênh; repo monorepo + contract + schema + seed dựng xong (Story 1 = starter template theo Structural Seed).
**FRs covered:** FR-1, FR-2, FR-3 (P2), FR-33, FR-34 (+ UX-DR1 token system, UX-DR4 shell; nền tảng NFR-4)

### Epic 2: Discovery & Booking with Deposit *(Sprint 1)*
Khách lọc kho với ngày chính xác theo Turnover Buffer tại thời điểm query, xem giá minh bạch khớp tuyệt đối, duyệt từng dòng ở Booking Summary, trả cọc qua PayOS QR (payment link thật + webhook) hoặc — tại quầy — 100% rent/extension/extra fee bằng Cash do staff xác nhận; contract draft sinh ngay khi cọc thành công; hết ngày nhận kho → EXPIRED mất cọc.
**FRs covered:** FR-4, FR-5, FR-6, FR-7, FR-8, FR-9, FR-36, FR-35 (My Rentals + Check-in Pass)

### Epic 3: Check-in & Contract Ritual *(Sprint 1)*
Staff chạy ca trên kanban: validate reservation + cọc đã trả, thu 100% rent (QR PayOS hoặc Cash — nút "Cash received"), in hợp đồng auto-draft, khách ký, staff chụp ảnh upload (access code khóa đến khi có bản ký), trao access code; Unit → Rented; card kéo Done thiếu bước bật lại.
**FRs covered:** FR-10, FR-11, FR-13, FR-14, FR-15, FR-21, FR-22

### Epic 4: Extension & Addendum Trail *(Sprint 2)*
Khách gia hạn an toàn trong biên Reservation kế tiếp (picker đánh dấu vùng xung đột, "Latest new checkout"), trả phí theo PricingEngine + top-up cọc (QR hoặc Cash tại quầy), ngày mới hiệu lực ngay sau xác nhận thanh toán, addendum giấy `CT-…-A1` 7 ngày đeo bám đủ kênh reminder (banner + card + bell).
**FRs covered:** FR-16, FR-12 (+ mở rộng FR-13 addenda rows)

### Epic 5: Support & Escalation with Relocation *(Sprint 2)*
Ticket route đúng Staff trực ca theo Unit + shift; staff resolve kèm note hoặc escalate kèm note bắt buộc; FM quyết severe → Unit Maintenance + Relocation (đổi Unit trên Rental hiện tại, giữ mã RT-/Contract/Deposit, access code mới) + tasks bảo trì/dọn + khách nhận notification từng bước; ticket Resolved hiện trọn arc trong một drawer; khách theo dõi qua Support List.
**FRs covered:** FR-23, FR-24, FR-25, FR-35 (Support List + detail drawer)

### Epic 6: Checkout, Settlement & Turnover *(Sprint 2)*
Climax sản phẩm: khách gửi checkout request; staff nhận kho + key, inspection từng hạng mục, hệ thống ép khai charge kèm reason bắt buộc (waiver trong trần — P2), settlement preview đến đồng, phần chênh trả qua QR/Cash, receipt hai bên xem vĩnh viễn; Unit → Preparing → Cleaning task → Available theo Turnover Buffer.
**FRs covered:** FR-17, FR-18, FR-41 (P2), FR-19 (+ FR-20 chân Preparing/Buffer)

### Epic 7: Facility Management & Oversight *(Sprint 3)*
FM quản lý cơ sở có vết audit: retire/set-maintenance/fix-status qua guard + confirm + reason trong edit drawer; phân công staff × Zone × ca × ngày chống trùng; Activity Log append-only xem được; Facility Overview KPI bấm xuyên xuống đúng slice.
**FRs covered:** FR-26, FR-20, FR-27, FR-28, FR-32

### Epic 8: Pricing Policy & Business Intelligence *(Sprint 3)*
Chính sách xấu không bao giờ tồn tại (validate-trước-khi-lưu + cắm cờ + banner nêu luật); policy chứa giá thuê, phụ thu trần, Turnover Buffer, Deposit % (+ Discount P2); Business Overview + Reports KPI xuyên xuống rows, tiền sinh từ PricingEngine snapshot.
**FRs covered:** FR-29, FR-30, FR-40 (P2), FR-31

### Epic 9: System Administration *(Sprint 3)*
SysAdmin cấp tài khoản mọi role với mật khẩu tạm (không cần script), đổi role, activate/deactivate/lock (không delete); mọi thao tác có dòng audit; soi nhật ký LOGIN/LOGIN_FAILED; permission matrix read-only (P2).
**FRs covered:** FR-37, FR-38, FR-39 (P2)

---

## Epic 1: Platform Foundation & Identity *(Sprint 1)*

Sau epic này, mọi user đăng ký/đăng nhập được, đổ vào landing đúng role trong adaptive shell 5 role (role chip + nav theo role); feedback toast + bell hoạt động hai kênh; repo monorepo + contract + schema + seed dựng xong (Story 1 = starter template theo Structural Seed).
**FRs covered:** FR-1, FR-2, FR-3 (P2), FR-33, FR-34 (+ UX-DR1 token system, UX-DR4 shell; nền tảng NFR-4)

### Story 1.1: Monorepo starter template theo Structural Seed

As a **developer**,
I want monorepo scaffold đúng Structural Seed với schema đầy đủ + seed demo + contract skeleton,
So that mọi story sau chỉ viết code nghiệp vụ vào cấu trúc/stack đã chốt, tái lập được từ 0.

**Acceptance Criteria:**

**Given** repo scaffold theo Structural Seed (`storagehub/backend/` + `frontend/` + `contracts/` + `docs/`)
**When** khởi chạy theo README
**Then** BE chạy `:8080` với `/actuator/health` → UP; FE dev server chạy và proxy `/api` → `:8080`; `backend/storage/` đã gitignore

**Given** MySQL 8.4.11 local + profile `dev`
**When** BE start
**Then** Flyway chạy **V1 = full 22 entity model V3** kèm V1 deltas (tiền `DECIMAL(15,0)`; `CONTRACTS` 1—N `SupersedesContractID`+`IsLatest`; `RuleType` thêm `TURNOVER_BUFFER`/`DISCOUNT`/`WAIVER_CAP`; `users.FacilityID` nullable; `NOTIFICATIONS.CreatedAt`; `ESCALATIONS.TicketID UNIQUE`; `INSPECTIONS.Item` enum) + **V2__seed_demo chỉ profile dev** với đúng bộ mock chuẩn (Lan/Minh/Hằng/Tuấn/Nam đủ 5 role; S-3/M-2/M-5; BK-1042/RT-0871/SR-0032/CT-1042/CT-1042-A1; policy v3)

**And** profile khác `dev` → seed không chạy; không DDL tay, không hbm2ddl

**And** stack pin đúng: Boot 4.1.1/Java 21 (webmvc · data-jpa · security · validation · flyway · flyway-mysql · actuator — đúng Boot 4 gotchas: starter `webmvc`, test starter tách module, Jackson 3, `flyway-mysql`), React 19.3 + Vite 8.3, JUnit 6/Mockito/AssertJ, Vitest+RTL, `vn.payos:payos-java` 2.0.1 trong pom

**And** `contracts/openapi.yaml` skeleton (info `/api/v1` + error envelope + list envelope schema), `contracts/routes.yaml` route table FE; `frontend/public/units/{code}.jpg` chuẩn static asset; `application.yml` dev/prod, secrets (DB/JWT/PayOS keys) qua env vars không commit

**And** quyết định tuần 1 ghi nhận trong README: TypeScript hay JS + component lib + dnd + chart + **QR code lib** (Deferred — chốt trước khi story 1.4 start)

### Story 1.2: BE foundations — error/list envelope + LogService append-only

As a **developer**,
I want một error envelope + list envelope duy nhất và `LogService` append-only,
So that mọi endpoint/service ở epic sau dùng chung một quy ước parse lỗi, phân trang và ghi vết audit.

**Acceptance Criteria:**

**Given** lỗi bất kỳ loại nào (validation 400, business block 409, security 401/403, 500)
**When** response trả về
**Then** đúng **một envelope** (machine code + human message + field errors khi có) khai báo trong openapi.yaml — kể cả lỗi security qua `AuthenticationEntryPoint`/`AccessDeniedHandler`, không lọt body mặc định Spring

**Given** DTO vi phạm Bean Validation
**When** handler bắt
**Then** envelope kèm đầy đủ field errors

**Given** danh sách many-items
**When** endpoint trả về
**Then** envelope `{items, page, pageSize, total}` — `page` 1-based, mặc định 25; enum UPPER_SNAKE; JSON camelCase; filter/sort param kebab-case

**Given** `LogService` với registry Action/EntityType enum chung + khai báo bắt-buộc-reason **tại registry**
**When** service ghi log hợp lệ
**Then** ActivityLog INSERT đúng (actor, entity, action, from → to, reason); repository không expose đường update/delete (append-only ở tầng dữ liệu)

**And** caller ghi action cần reason mà thiếu reason → LogService từ chối, không ghi log mồ côi

**And** unit test (JUnit/AssertJ): envelope shape cho từng loại lỗi, pagination 1-based, append-only + reason enforcement

### Story 1.3: Auth backend — JWT + permission matrix + LOGIN audit

As a **user (mọi role)**,
I want đăng nhập/đăng ký an toàn với phân quyền enforce server-side,
So that tài khoản tôi được bảo vệ và mỗi role chỉ làm được đúng những gì ma trận quyền cho phép.

**Acceptance Criteria:**

**Given** user active, email + password đúng
**When** `POST /api/v1/auth/login`
**Then** trả JWT (role claim, TTL 24h) + user info; ActivityLog ghi `LOGIN` (EntityType USER)

**Given** sai email hoặc password
**When** login
**Then** 401 envelope message chung không tiết lộ field nào sai; ActivityLog ghi `LOGIN_FAILED`

**Given** tài khoản Status = deactivate (0) hoặc lock (2)
**When** login
**Then** chặn với message chung; ghi `LOGIN_FAILED`

**Given** JWT còn hạn nhưng user bị deactivate/lock sau khi phát token
**When** gọi endpoint bất kỳ
**Then** per-request filter check `users.Status` (cache ngắn) chặn ngay — không đợi hết TTL

**Given** visitor submit đăng ký (họ tên, phone, email, password + xác nhận, đồng ý điều khoản)
**When** `POST /api/v1/auth/register`
**Then** tạo user CUSTOMER duy nhất (BCrypt hash), API không nhận field role; email trùng → field error inline

**Given** user quên mật khẩu (P2)
**When** `POST /api/v1/auth/forgot-password`
**Then** luôn trả generic "If an account exists…" — không tiết lộ tồn tại, không email thật

**And** đúng 3 endpoint public không JWT (login/register/forgot — endpoint public thứ 4 `payments/webhook` thuộc Epic 2); còn lại thiếu/kém JWT → 401 envelope, role không đủ → 403 envelope

**And** permission matrix Role × Permission cố định trong code, enforce từng endpoint; contract các endpoint trên đóng băng trong openapi.yaml trước khi story 1.5 start (AD-2)

**And** unit test: JWT issue/verify + TTL, BCrypt, status-check filter, matrix enforcement, LOGIN/LOGIN_FAILED logged

### Story 1.4: FE design token system "Control Room" + base primitives

As a **developer FE**,
I want token system + bộ primitive chuẩn theo DESIGN.md,
So that mọi màn ở epic sau render đúng identity đã chốt mà không ai tự chọn màu/kích thước/định dạng.

**Acceptance Criteria:**

**Given** token spec DESIGN.md (UX-DR1)
**When** cài token
**Then** đủ: surface ramp 6 bậc; text ramp 4 bậc; brand indigo `#4F46E5` + tint/border/outline-border (indigo = action/selection); **7 status semantics** đủ dot/tint/border (Available/Buffer/Reserved/Rented/Preparing/Maintenance/Retired); feedback tokens; typography 14 style (system fonts + mono cho code); radius md 8 / sm 6 / full chỉ dots; spacing 4→48 + `page-x` 24 + `nav-height` 54; elevation = border + 1px shadow thì thầm, hover darkens border, shadow thật chỉ modal/drawer

**Given** primitives (UX-DR5)
**When** dựng
**Then** Button primary/secondary/ghost 36px (page-level 40px, destructive = secondary + error text, đúng 1 primary/card); Input 36px label trên luôn hiển thị + focus ring 2px indigo + invalid border/message; Card trắng + border + shadow + status bar 3px trái; Badge; Modal max 480px actions right-aligned; Drawer 420px phải Esc/scrim; Tabs 2px underline; Empty state (56px icon tile + factual title + mono chips echo filter + đúng 1 CTA); Skeleton layout-matching; Table sticky header 40px rows + numeric right-align tabular + hover row, cấm zebra

**Given** tiền VND
**When** qua util `formatMoney`
**Then** `1.150.000 ₫` — dot separator, ₫ sau số, full precision, `tabular-nums` (UX-DR2); unit code render mono

**And** a11y floor đúng primitives (UX-DR6): label visible mọi input, focus visible, target ≥ 40px, focus trap modal/drawer + trả về trigger

**And** Vitest smoke: `formatMoney`, Button/Input các trạng thái, focus trap

### Story 1.5: Adaptive shell 5 role + Auth screens FE

As a **user (5 role)**,
I want một shell thích ứng theo role và màn đăng nhập/đăng ký,
So that sau login tôi đổ đúng landing của role mình, chỉ thấy nav đúng vai, và truy cập chéo bị chặn rõ ràng.

**Acceptance Criteria:**

**Given** đăng nhập thành công
**When** shell render
**Then** top bar trái→phải: logo + role menu + **role chip** (primary-tint pill) + bell + avatar menu (profile, logout); logout = drop token → về Login

**Given** từng role
**When** đăng nhập
**Then** nav + landing đúng FR-1: Customer → Browse Units (Browse Units·My Rentals·Support); Staff → Task Board (Tasks·Support); FM → Facility Overview (Overview·Units·Staff & Shifts·Operations·Activity Log·Escalations); BO → Business Overview (Overview·Policy·Reports); SysAdmin → User Management (Users·Login History)

**Given** user role A
**When** truy cập URL thuộc role B
**Then** error state 403 đơn giản

**Given** visitor ở Login
**When** submit sai
**Then** inline error message chung + đúng 1 bước kế; link mở Register; **Forgot password mở modal từ Login** (P2) → generic response

**Given** visitor ở Register
**When** submit hợp lệ
**Then** tạo tài khoản CUSTOMER, đổ Browse Units đã đăng nhập; form không có role picker; password + confirm mismatch → inline error

**And** mọi route app yêu cầu đăng nhập; route table FE khớp `contracts/routes.yaml`; BE chưa xong thì dev chống MSW sinh từ contract (AD-2)

**And** overlay discipline một tầng sâu (modal không mở từ modal); responsive desktop-first 1200px, hẹp hơn vẫn dùng được (UX-DR9 cơ bản)

### Story 1.6: Toast + Bell + Notification Center

As a **user (mọi role)**,
I want toast tức thời + bell feed bền vững,
So that tôi không bỏ lỡ sự kiện tiền/trạng thái nào và luôn quay lại được đúng đối tượng đã sinh sự kiện.

**Acceptance Criteria:**

**Given** mutation thành công trả response kèm `notification` object (convention AD)
**When** FE nhận
**Then** bắn toast render từ response data — FE không tự bịa chuỗi

**Given** toast hiển thị
**When** chạy
**Then** bottom-right, ~4s auto-dismiss, hover pause, 3px accent, ≤1 action link; toast không bao giờ thay thế bell

**Given** user có unread
**When** top bar render
**Then** bell badge error-red đếm unread qua endpoint unread-count riêng, polling TanStack `refetchInterval` (tách khỏi list full)

**Given** user mở Notification Center
**When** feed render
**Then** role-scoped (khách: reservation/payment/support/contract; staff: task assignment; manager: escalation + status writes; BO: policy save + export completion); unread trước; mỗi entry deep link tới đối tượng affected theo `routes.yaml`; mark-all-read; unread persist giữa phiên (read state server-side); không email

**Given** module nghiệp vụ cần bắn notification
**When** gọi event method công khai của `NotificationService` (`NotificationEvent` có kiểu + deep-link registry trong openapi)
**Then** INSERT notification trong cùng transaction nghiệp vụ — cấm module tự INSERT (AD-6); sự kiện tiền phát **cả toast lẫn bell**

**And** test: NotificationService ghi đúng role-scoping + unread-count; FE toast/polling behavior (Vitest)

---

## Epic 2: Discovery & Booking with Deposit *(Sprint 1)*

Khách lọc kho với ngày chính xác theo Turnover Buffer tại thời điểm query, xem giá minh bạch khớp tuyệt đối, duyệt từng dòng ở Booking Summary, trả cọc qua PayOS QR (payment link thật + webhook); contract draft sinh ngay khi cọc thành công; hết ngày nhận kho → EXPIRED mất cọc.
**FRs covered:** FR-4, FR-5, FR-6, FR-7, FR-8, FR-9, FR-36, FR-35 (My Rentals + Check-in Pass)

### Story 2.1: PricingEngine + Unit Detail bảng giá minh bạch

As a **Customer**,
I want xem spec đầy đủ + bảng giá minh bạch của unit,
So that tôi biết chính xác mình sẽ bị truy đòi bao nhiêu trước khi đặt cọc.

**Acceptance Criteria:**

**Given** `PricingEngine` duy nhất (AD-11) đọc policy active
**When** tính breakdown cho unit × duration × ngày bắt đầu
**Then** resolve phiên bản policy hiệu lực tại thời điểm query; tính rent × duration + **từng dòng phụ thu** từ Rental Policy active + Deposit = Deposit% × tổng tiền thuê (đánh dấu refundable); tiền `BigDecimal`/`DECIMAL(15,0)`/JSON number, tính tiền chỉ ở service (AD-7)

**Given** màn Unit Detail
**When** render
**Then** spec đầy đủ (kích thước, tầng, access, an ninh) + ảnh `public/units/{code}.jpg` + bảng giá các dòng trên + Reserve CTA

**And** mọi dòng tiền sau này (Booking Summary 2.3, Payment Modal 2.6, Contract E3, Receipt) sinh từ đúng engine này — không nơi nào tự tính lại

**And** unit test: engine khớp số demo chuẩn theo policy v3 seed — S-3: 345.000 ₫/mo × 3 = 1.035.000 ₫, Deposit 10% = 103.500 ₫

### Story 2.2: Browse Units + availability theo Turnover Buffer

As a **Customer**,
I want lọc kho theo loại/kích thước/ngày/thời hạn và thấy availability chính xác tại thời điểm query,
So that ngày tôi thấy là ngày tôi đặt được.

**Acceptance Criteria:**

**Given** Browse Units (landing Customer)
**When** render
**Then** filter bar dropdown đóng (type / size / start date / duration) + Search button; **re-query chỉ khi bấm Search** (không per keystroke); filter active persist trong phiên; sort mặc định giá thấp → cao

**Given** query availability
**When** BE tính
**Then** derive **on-read** theo Reservation + Turnover Buffer (Policy Rule) tại thời điểm query (AD-4 — không persist derived state); Unit đang Rented/Maintenance/Retired không xuất hiện

**Given** unit trong chu kỳ buffer
**When** hiển thị
**Then** card "Available {date} · cleaning buffer" amber, vẫn đặt được với ngày đó (Book secondary)

**And** lưới card: ảnh + mono code chip + size/type chips + feature line + price tabular + availability line status dot + **3px left status bar** (available green / buffer amber); chip đếm "N units available · live" refresh mỗi fetch; click card (ngoài Book) → Unit Detail

**And** empty state factual ("0 of 42 units meet all four criteria") + mono chips echo filter removable + đúng 1 CTA (Clear filters / Widen dates)

**And** test: availability đúng với seed (S-3 available Oct 3; M-2 Rented không hiện; unit buffer hiện đúng ngày = checkout + buffer policy)

### Story 2.3: Reserve re-check + Booking Summary

As a **Customer**,
I want đặt chỗ có kiểm tra lại chống stale và duyệt đầy đủ dòng tiền trước khi trả,
So that không bao giờ có booking trên ngày invalid và không có dòng tiền bất ngờ ở payment.

**Acceptance Criteria:**

**Given** Booking Summary mở
**When** render
**Then** unit, dates, rent × duration + từng dòng phụ thu + total + **Deposit due now** (refundable) — tất cả từ `PricingEngine`; ghi chú "Contract auto-drafted from these exact terms, signed at check-in"; CTA mở Payment Modal

**Given** bấm confirm ở Booking Summary
**When** BE xử lý
**Then** sinh Reservation `PENDING_PAYMENT` ngay (bỏ modal không tạo giữ chỗ) + **snapshot giá vào reservation** (AD-11); policy đổi sau đó không ảnh hưởng booking này

**Given** bấm Reserve/Book
**When** BE re-check availability trong transaction guard unique/optimistic (Unit, ngày)
**Then** còn → vào Booking Summary; **bị chiếm giữa chừng → 409 + bounce về lưới + toast** ("S-3 was just reserved. 5 similar units still available."); hai khách cùng unit sát nhau chỉ một thắng

**And** Booking Summary là nguồn chân lý dòng tiền — không dòng tiền nào xuất hiện ở payment mà vắng ở đây (FR-7)

**And** test: 2 request song song cùng unit → 1 thắng 1 nhận 409; snapshot persist đúng

### Story 2.4: Payment backend + PayOsPaymentGateway

As a **Customer/Staff**,
I want thanh toán được xác nhận từ đúng một nguồn sự thật (webhook PayOS / cash-received) và ghi sổ tự động,
So that trạng thái nghiệp vụ chỉ tiến khi tiền thật sự về.

**Acceptance Criteria:**

**Given** interface `PaymentGateway` + đúng 1 impl `PayOsPaymentGateway` (SDK `vn.payos:payos-java` 2.0.1; credentials `PAYOS_CLIENT_ID`/`PAYOS_API_KEY`/`PAYOS_CHECKSUM_KEY` env)
**When** tạo payment
**Then** Payment record theo state machine 5 trạng thái (`PaymentService` owner); `orderCode` derive từ PaymentID (**idempotent**, unique constraint); tạo payment link (orderCode, amount, returnUrl, cancelUrl) → trả `checkoutUrl` + expiry cho FE render QR + đếm ngược

**Given** webhook `POST /api/v1/payments/webhook` (endpoint public thứ 4 — không JWT, AD-5)
**When** nhận
**Then** verify checksum qua SDK — chỉ checksum hợp lệ mới xử lý; sai → từ chối

**Given** xác nhận thanh toán đến
**When** xử lý
**Then** **đúng 1 bản ghi thắng** (webhook duplicate không double ghi); Payment → SUCCESS → FR-9: receipt đúng 1 bản (ghi rõ method QR) + flip nghiệp vụ (Deposit → Reservation `RESERVED` + Unit Reserved) + Activity Log + notification (toast + bell) trong cùng transaction

**Given** staff bấm "Cash received" (endpoint permission STAFF)
**When** xác nhận payment `PENDING_CASH`
**Then** Payment → SUCCESS method CASH + receipt + flip nghiệp vụ như webhook; chọn Cash chỉ tạo `PENDING_CASH` — nghiệp vụ đứng yên cho tới confirm

**Given** link hết hạn hoặc Cancel
**When** xử lý
**Then** Payment → FAIL/EXPIRED, link cancel phía PayOS, **không side-effect nghiệp vụ gì** — không đổi trạng thái, không notification

**And** test: gateway impl (mock SDK), checksum verify, duplicate webhook idempotent, state machine transitions, cash-received permission STAFF; dev chạy webhook qua ngrok (OQ-2)

### Story 2.5: My Rentals + Check-in Pass + Rental Detail cơ bản

As a **Customer**,
I want ba màn neo xem trạng thái đặt chỗ/thuê của mình,
So that tôi luôn biết mình đang ở đâu và bước kế là gì.

**Acceptance Criteria:**

**Given** My Rentals
**When** render
**Then** danh sách Reservations (`PENDING_PAYMENT`/`RESERVED`) + Rentals active + History (`CLOSED`/`EXPIRED`) — card + status badge + next-action theo trạng thái; là pointer surface: mọi số/trạng thái khớp nguồn, **không tự tính lại** (FR-35)

**And** next-action mapping theo trạng thái; các flow chưa build (Extend → E4, Checkout Request → E6, New Support → E5) bổ sung ở epic tương ứng — story này không render nút chết

**Given** Check-in Pass (từ reservation RESERVED)
**When** render
**Then** mã `BK-` hiển thị **lớng mono** + hướng dẫn tại quầy (mang ID để ký hợp đồng, 100% rent thu tại quầy)

**Given** Rental Detail cơ bản
**When** render
**Then** unit info + dates + deposit status (held) + payment history receipts (method QR/CASH hiển thị); layout hợp khối để E3 bổ sung contract chain, E4 addendum, E6 settlement receipt

**And** empty state factual + đúng 1 CTA trỏ flow đổ dữ liệu (Browse Units)

**And** test: list đúng theo user đăng nhập; receipts hiển thị đúng method

### Story 2.6: Payment Modal FE — QR PayOS tại Deposit, khép Flow 1

As a **Customer**,
I want trả cọc qua QR PayOS ngay trong modal với trạng thái tường minh,
So that tôi biết chắc tiền đã nhận và unit được giữ cho mình.

**Acceptance Criteria:**

**Given** Payment Modal component **dùng chung 4 touchpoint** (method select config theo touchpoint)
**When** mở tại Deposit từ Booking Summary
**Then** header tên khoản trả + amount `price-lg`; method select **chỉ PayOS QR — không render option Cash** ở touchpoint này

**Given** chọn PayOS QR + Pay
**When** BE trả `checkoutUrl` + expiry
**Then** modal render QR + **poll trạng thái payment** tới khi webhook xác nhận + đếm ngược từ expiry BE trả (FE không tự đặt timeout)

**Given** đang awaiting confirmation
**When** modal mở
**Then** khóa điều hướng (không back/close ngầm) + nút **Cancel** tường minh → gọi BE cancel link → về method select, không tính tiền

**Given** poll trả SUCCESS
**When** hiển thị
**Then** success state: check tile + amount + consequence line ("Unit S-3 is reserved for you until check-in") + toast amount + bell; flip trạng thái màn nền (reservation → RESERVED); receipt append Rental Detail; **"View rental" deep-link Rental Detail — khép Flow 1**, không có màn Booking Confirmation riêng

**Given** fail (link fail / hủy ở cổng / hết hạn countdown)
**When** hiển thị
**Then** error tile "No money was taken" + Retry + Switch method; **không đổi trạng thái gì anywhere**; sau 2 lần fail gợi ý đổi phương thức rõ ràng (tại Deposit QR-only → hướng liên hệ quầy); countdown hết → method select + note link cancelled

**And** modal đúng pattern UX-DR5 (max 480px, focus trap, actions right); poll không chặn UI

**And** test (Vitest): state machine modal (select → awaiting → success/fail/expired), poll interval, cancel gọi API đúng 1 lần

### Story 2.7: No-show EXPIRED — exactly-once lazy

As a **hệ thống**,
I want reservation hết ngày nhận kho mà chưa check-in tự thành EXPIRED đúng một lần,
So that cọc forfeit có vết, unit quay lại inventory và không side-effect nào chạy hai lần.

**Acceptance Criteria:**

**Given** Reservation `RESERVED` hết ngày nhận kho (đánh giá theo lịch `Asia/Ho_Chi_Minh` — AD-7), chưa check-in
**When** lần **đọc/đụng tới đầu tiên** (list/detail/availability query)
**Then** derive `EXPIRED` on-read (AD-4 — không scheduled batch song song)

**Given** EXPIRED được derive lần đầu
**When** side effects chạy
**Then** **exactly-once** guarded idempotency key (unique constraint): receipt "Deposit forfeited — no-show", Unit → Available, Contract Draft/Printed → Closed, Activity Log, notification khách — mỗi hiệu ứng đúng 1 lần kể cả khi đọc song song

**And** API trả DTO mang state đã suy diễn — FE hiển thị nguyên văn, không tự tính từ ngày; derived state không persist ngược vào trường trạng thái gốc

**And** EXPIRED là lối ra rủi ro duy nhất của Reservation — không cancel trong v1; no-show không hoàn cọc

**And** test: derive đúng theo ngày ICT; 2 đọc song song → side effects chạy đúng 1 lần; receipt + log + notification đủ

---

## Epic 3: Check-in & Contract Ritual *(Sprint 1)*

Staff chạy ca trên kanban: validate reservation + cọc đã trả, thu 100% rent (QR PayOS hoặc Cash — nút "Cash received"), in hợp đồng auto-draft, khách ký, staff chụp ảnh upload (access code khóa đến khi có bản ký), trao access code; Unit → Rented; card kéo Done thiếu bước bật lại.
**FRs covered:** FR-10, FR-11, FR-13, FR-14, FR-15, FR-21, FR-22

### Story 3.1: Auto-draft Contract khi Deposit thành công

As a **Customer**,
I want contract tự soạn ngay khi cọc thành công,
So that không ai soạn tay và điều kiện khóa đúng phiên bản policy tôi đã đặt.

**Acceptance Criteria:**

**Given** deposit payment SUCCESS (webhook QR hoặc cash-confirm — 2.4)
**When** flip Reservation → `RESERVED` chạy
**Then** `ContractService` sinh **Contract Draft trong cùng transaction**; mã `CT-xxxx`; `ContentSnapshot` từ reservation snapshot (điều kiện booking + dòng tiền + policy version khóa)

**Given** contract đã sinh
**When** đọc từ bất kỳ đâu
**Then** read-only — API chỉ có read + state transitions, không có đường edit nội dung; không UI nào cho sửa

**Given** sai sót ở bản Draft
**When** re-draft
**Then** bản cũ → `Superseded`, bản mới `IsLatest`; bản cũ vẫn đọc được trong chuỗi (hiển thị ở 3.4)

**And** notification khách: "Contract CT-1042 drafted from your booking and Rental Policy v3. You'll sign it at check-in."

**And** Print (3.4) render từ `ContentSnapshot` bằng print view FE — không PDF service (Conventions)

**And** test: draft sinh đúng ở cả 2 path xác nhận (QR + cash); snapshot khóa policy version; re-draft supersede + `IsLatest` đúng

### Story 3.2: Kanban Task Board

As a **Staff**,
I want việc ca hiện tại trên kanban 3 cột với đủ loại card,
So that mở app là thấy việc và làm theo thứ tự không bỏ sót.

**Acceptance Criteria:**

**Given** Staff login
**When** landing Task Board
**Then** 3 cột To do / In progress / Done; card 5 loại **Check-in (indigo) / Checkout (sky) / Cleaning (amber) / Support (red) / Contract (slate)** — 3px left bar màu theo loại; card: unit code mono + type chip + customer name/time slot + due chip

**Given** task registry của `TaskService`
**When** reservation → `RESERVED`
**Then** sinh **Check-in Task** trong cùng transaction (ownership AD-6); các rule sinh task khác (Cleaning/Support/Contract/Checkout) bổ sung ở E4–E7 **qua đúng registry này** — không chỗ nào tự INSERT task

**Given** board
**When** lọc + xem
**Then** tab lọc theo loại card; **số lượng từng cột ở header**; card Done render muted 60% + mất left bar

**Given** một card
**When** di chuyển
**Then** drag-drop **hoặc keyboard**: focus card → Move ◀/▶ (+ status menu) — drag không phải con đường duy nhất (UX-DR6/DR10); move qua TaskService transition

**And** cột rỗng → "Nothing here" tile; mobile: kanban scroll ngang + sticky column header (UX-DR9)

**And** test: registry sinh Check-in task khi RESERVED; move qua API đúng transition + count header

### Story 3.3: Check-in Task — validate reservation + thu 100% rent (QR/Cash)

As a **Staff**,
I want validate reservation tại quầy và thu 100% rent ngay trong task (QR hoặc Cash),
So that chỉ reservation hợp lệ + đã cọc mới được check-in và tiền thu luôn có receipt.

**Acceptance Criteria:**

**Given** Check-in Task mở
**When** staff nhập/mã reservation `BK-`
**Then** validate tồn tại + **Deposit đã trả**; hợp lệ → result "valid + deposit confirmed" + rent due breakdown (**Deposit và rent full hiển thị tách bạch 2 dòng** — FR-15); không hợp lệ → **lý do cụ thể** (không tồn tại / chưa trả cọc), chặn tiến trình

**Given** rent due
**When** mở Payment Modal (touchpoint 100% rent)
**Then** method select có **PayOS QR + Cash at desk** (touchpoint có staff — đối lập Deposit QR-only)

**Given** chọn Cash
**When** tạo
**Then** payment `PENDING_CASH`; task hiển thị chờ + nút **"Cash received"** (permission STAFF); bấm → confirm endpoint 2.4 → SUCCESS method CASH + receipt + toast + bell

**Given** chọn QR
**When** pay
**Then** flow như 2.6: render QR + poll + đếm ngược; success → receipt + notification

**Given** rent payment thành công
**When** ghi sổ
**Then** receipt vào Rental Detail; Rental **chưa kích hoạt** — chờ contract ritual (3.4); payment fail → task đứng yên, retry/switch method

**And** test: 3 case validate (hợp lệ / không tồn tại / chưa cọc); PENDING_CASH park + confirm flip; rent success **không** tự `CHECKED_IN`

### Story 3.4: Contract ritual + Access Code + kích hoạt Rental + contract chain

As a **Staff**,
I want nghi thức hợp đồng có vết (in → ký → chụp ảnh) khóa việc trao access code,
So that không có kho nào chạy không có bản ký trên hồ sơ.

**Acceptance Criteria:**

**Given** Check-in Task đã validate + rent đã trả
**When** mở contract step
**Then** panel: **preview read-only** từ `ContentSnapshot` (mono code, parties, unit, dates, rent/deposit lines tabular, policy version) + **Print** (secondary — print view FE) + **capture tile** dashed (chụp/tải ảnh bản ký → thumbnail + Retake) + **Attach** (primary — disabled đến khi có ảnh)

**Given** Attach
**When** upload
**Then** multipart `POST /api/v1/attachments` (AD-10): lưu `backend/storage/`; `*PhotoUrl` chỉ lưu API path tương đối; phục vụ qua controller stream **đã qua JWT + permission** (customer của rental / staff / manager) — cấm static mapping công khai; Contract → Signed; Activity Log `CONTRACT_SIGNED` (actor + timestamp); notification hai bên

**Given** Contract chưa Signed
**When** xem nút trao Access Code
**Then** vô hiệu — **không kích hoạt Rental khi Contract chưa Signed** (FR-11)

**Given** Contract Signed + rent đã trả
**When** staff trao Access Code
**Then** reveal qua endpoint riêng (SensitiveValue — không nằm trong response danh sách, AD-5); Reservation → `CHECKED_IN`, Unit → `RENTED`, Check-in Task → Done — một transaction; notification "Access code sent to your notifications."

**Given** Rental Detail (khách)
**When** render contract chain
**Then** chuỗi contract gốc: mã `CT-` mono + badge trạng thái + ảnh bản ký; **Signed xem vĩnh viễn; Superseded vẫn đọc được** (FR-13)

**And** test: attach stream auth (không JWT → 401; customer lạ → 403); access code locked until signed; flip `CHECKED_IN` + Unit `RENTED` + task Done cùng transaction; chain hiển thị đủ bản Superseded

### Story 3.5: Snap-back chống Done ảo

As a **Staff**,
I want card không vào được Done khi thiếu bước kết,
So that không tồn tại task Done mà nghiệp vụ chưa khép.

**Acceptance Criteria:**

**Given** kéo Check-in card vào Done khi thiếu closing step (rent chưa trả / contract chưa ký)
**When** BE xử lý move
**Then** **409 + payload cấu trúc** (AD-8): guard vi phạm + tên closing step còn thiếu (machine code + human label) — FE render toast **từ dữ liệu payload**, không parse message người đọc

**Given** FE nhận block
**When** render
**Then** card snap-back về cột cũ + toast nêu đúng bước thiếu ("Rent payment is still pending on this check-in.")

**Given** guard registry theo loại card
**When** epic sau mở rộng
**Then** Checkout (settlement — E6) + Contract-signature (ảnh bản ký — E4) dùng đúng registry; Cleaning/Support hoàn tất trực tiếp không cần closing step

**Given** move hợp lệ (đủ closing steps)
**When** kéo vào Done
**Then** task Done, render muted

**And** keyboard Move đi qua đúng guard như drag

**And** test: BE guard trả payload đúng cấu trúc + machine code; move hợp lệ pass; FE snap-back + toast render từ payload (Vitest)

---

## Epic 4: Extension & Addendum Trail *(Sprint 2)*

Khách gia hạn an toàn trong biên Reservation kế tiếp (picker đánh dấu vùng xung đột, "Latest new checkout"), trả phí theo PricingEngine + top-up cọc (QR hoặc Cash tại quầy), ngày mới hiệu lực ngay sau xác nhận thanh toán, addendum giấy `CT-…-A1` 7 ngày đeo bám đủ kênh reminder (banner + card + bell).
**FRs covered:** FR-16, FR-12 (+ mở rộng FR-13 addenda rows)

### Story 4.1: Màn Extend + conflict boundary

As a **Customer**,
I want chọn ngày checkout mới với vùng xung đột được đánh dấu sẵn trên picker,
So that tôi không submit bao giờ vào ngày đã có reservation kế tiếp và luôn biết biên tối đa của mình.

**Acceptance Criteria:**

**Given** Rental `CHECKED_IN` còn trước EndDate, mở Extend từ Rental Detail
**When** calendar picker render
**Then** **vùng xung đột** với Reservation kế tiếp của Unit được đánh dấu trên picker (UX-DR10); hiển thị biên hợp lệ tối đa

**Given** submit ngày nằm trong vùng conflict
**When** BE validate
**Then** block với banner nêu đúng biên — "Can't extend to Oct 20 — M-2 has a reservation starting Oct 19. Latest possible checkout is Oct 18. Pick another date." (banner đầu form, không phải toast — NFR-8)

**Given** khách xác nhận thanh toán (ngày hợp lệ + payment intent)
**When** BE xử lý
**Then** sinh Extension `PENDING_PAYMENT`; **đóng modal chưa trả = không ghi nhận gì** — không có đường "soạn nhầm" (AD-4; Extension chỉ `PENDING_PAYMENT` → `APPLIED`)

**Given** Rental đã qua EndDate
**When** xem Rental Detail
**Then** **không có entry Extend** — Rental giữ `CHECKED_IN`; ghi chú trả trễ tính phụ thu `LATE_FEE` tại Settlement (E6); không có trạng thái OVERDUE

**And** ngày gia hạn bị chặn trên biên Reservation kế tiếp (cùng rule availability — reuse 2.2/2.3)

**And** test: conflict boundary tính đúng từ seed; submit ngày conflict → block đúng biên; qua EndDate không mở extension

### Story 4.2: Extension fee + top-up cọc + payment + ngày hiệu lực

As a **Customer**,
I want trả phí gia hạn (rent kỳ thêm + top-up cọc) qua QR hoặc Cash tại quầy,
So that ngày checkout mới của tôi có hiệu lực ngay sau khi tiền được xác nhận.

**Acceptance Criteria:**

**Given** extension ngày hợp lệ
**When** tính phí
**Then** **phí = tiền thuê kỳ thêm theo snapshot giá của rental** + **top-up cọc = max(0, Deposit% × tổng tiền thuê hợp đồng sau gia hạn − cọc đang giữ)** (AD-11 — không tính lại theo policy mới; Deposit% giảm → top-up floor 0, phần chênh hoàn tại Settlement); hiển thị 2 dòng tách bạch

**Given** Payment Modal touchpoint Extension fee
**When** mở
**Then** method select **PayOS QR + Cash at desk**; amount đúng tổng fee

**Given** chọn Cash
**When** tạo
**Then** payment `PENDING_CASH` + **Contract-signature card sinh trên Task Board** (nhiệm vụ desk: thu tiền + ký phụ lục — 4.3); EndDate **chưa** dịch

**Given** chọn QR
**When** pay thành công (webhook)
**Then** Extension → `APPLIED`; **EndDate dịch ngay** (Unit được giữ — khách đã trả); receipt; notification: "Extension paid — 690.000 ₫ rent + 69.000 ₫ deposit top-up (held deposit now 172.500 ₫). New checkout date: Oct 18. Sign addendum CT-1042-A1 at the desk by Oct 25."

**Given** staff bấm "Cash received" trên Contract-signature card (extension `PENDING_CASH`)
**When** confirm
**Then** SUCCESS method CASH → Extension `APPLIED` + EndDate dịch + addendum draft tức thì (một transaction) — desk tiếp tục ký ngay (4.3)

**And** payment fail → không gì đổi (reuse 2.4/2.6); số demo chuẩn: extension CT-1042 = 690.000 ₫ + 69.000 ₫ top-up, cọc 103.500 → 172.500 ₫

**And** test: fee tính đúng theo snapshot + công thức top-up (case floor 0); QR path + cash path đều dịch EndDate đúng 1 lần; fail không đổi trạng thái

### Story 4.3: Addendum trail — 7 ngày đeo bám + desk ritual + chain

As a **Customer/Staff**,
I want phụ lục gia hạn được nhắc ký đủ kênh trong 7 ngày và đóng hồ sơ bằng ảnh bản ký,
So rằng giấy tờ đi sau nhưng không bao giờ bị bỏ quên.

**Acceptance Criteria:**

**Given** extension fee đã SUCCESS (QR path)
**When** ghi sổ
**Then** Addendum `CT-…-A1` auto-draft (`AWAITING_SIGNATURE`, deadline ký 7 ngày từ ngày trả phí — `Asia/Ho_Chi_Minh`); **không hold Unit** — ngày mới đã hiệu lực từ 4.2; Contract-signature card sinh trên board

**Given** addendum `AWAITING_SIGNATURE`
**When** hiển thị các kênh
**Then** **3 kênh reminder**: amber banner trên Rental Detail (nêu deadline) + Contract-signature card trên Task Board (customer, unit, addendum code, due date) + bell hai bên; gần deadline → reminder tăng cường

**Given** staff mở Contract-signature card
**When** desk ritual chạy
**Then** đúng contract step 3.4 (preview read-only → Print → capture tile → Attach — disabled đến khi có ảnh) + **nút "Cash received"** nếu extension còn `PENDING_CASH`; attach ảnh → Addendum → `SIGNED` + `CONTRACT_SIGNED` Activity Log + card hoàn tất (guard snap-back 3.5 mở cho loại Contract: kéo Done thiếu ảnh ký → bật lại + toast)

**Given** addendum đã Signed
**When** hiển thị
**Then** ảnh bản ký vào chuỗi — **Signed xem vĩnh viễn**; notification "Addendum CT-1042-A1 signed and filed."

**Given** quá 7 ngày chưa ký
**When** xử lý
**Then** reminder tăng cường; **không thu hồi ngày checkout mới** — addendum là giấy tờ thủ tục

**Given** hai lối đóng hồ sơ không ký
**When** staff thao tác
**Then** **Expired** (khách không đến ký — staff đóng sau khi hết nhắc) hoặc **Voided** (staff hủy có lý do bắt buộc — vd khách trả kho sớm); cả hai ghi Activity Log với reason

**Given** Rental Detail
**When** render contract chain
**Then** chuỗi bổ sung **addenda rows** dưới contract gốc: mã `CT-1042-A1` mono + badge trạng thái + ảnh bản ký + deadline (nếu chưa ký) — FR-13 mở rộng

**And** addendum read-only mọi nơi như contract; không có UI soạn tay

**And** test: draft chỉ sau payment success; 3 kênh reminder sinh đúng; attach → SIGNED + card Done; snap-back thiếu ảnh; Expired/Voided ghi log reason; chain hiển thị đủ addenda

---

## Epic 5: Support & Escalation with Relocation *(Sprint 2)*

Ticket route đúng Staff trực ca theo Unit + shift; staff resolve kèm note hoặc escalate kèm note bắt buộc; FM quyết severe → Unit Maintenance + Relocation (đổi Unit trên Rental hiện tại, giữ mã RT-/Contract/Deposit, access code mới) + tasks bảo trì/dọn + khách nhận notification từng bước; ticket Resolved hiện trọn arc trong một drawer; khách theo dõi qua Support List.
**FRs covered:** FR-23, FR-24, FR-25, FR-35 (Support List + detail drawer)

### Story 5.1: Tạo + route Support Ticket

As a **Customer**,
I want gửi yêu cầu hỗ trợ cho unit tôi đang thuê và nó tự tới đúng nhân viên trực ca,
So that tôi không phải biết ai chịu trách nhiệm gì — hệ thống biết.

**Acceptance Criteria:**

**Given** form New Support (từ Rental Detail / Support List)
**When** render
**Then** chọn **Unit thuộc rental của khách** + **IncidentType** (`LOST_ACCESS` / `DEVICE_ISSUE` / `SECURITY` / `CLEANLINESS` / `OTHER`) + mô tả bắt buộc; validation inline on blur

**Given** submit hợp lệ
**When** BE xử lý
**Then** sinh ticket mã `SR-`; **route tới Staff trực ca theo Unit + shift** (đọc `staff_assignments` theo Zone của Unit × ca hiện tại — query ngày ICT, AD-7); **Support Task card sinh qua registry TaskService** (3.2) trong cùng transaction; notification staff (task assignment) + khách (ticket received)

**Given** không có Unit hợp lệ (không chọn / unit không thuộc rental)
**When** submit
**Then** **không gửi được** — field error chặn (FR-23)

**Given** khu vực không có staff trực ca
**When** route
**Then** fallback rõ ràng (unassigned queue trên board + notification manager) — không mất ticket âm thầm

**And** test: route đúng theo seed assignments × ca; card sinh đúng loại; thiếu Unit bị chặn

### Story 5.2: Support Ticket xử lý — Resolve / Escalate

As a **Staff**,
I want xử lý ticket tại chỗ hoặc đẩy lên manager khi quá tay,
So rằng khách luôn có lời giải hoặc sự cố nghiêm trọng lên đúng người có quyền quyết.

**Acceptance Criteria:**

**Given** staff mở Support Task card → màn Support Ticket
**When** render
**Then** ngữ cảnh đầy đủ: incident type + mô tả khách **nguyên văn** + unit (mono code) + khách + trạng thái; **staff note field**; hai action **Resolve / Escalate**

**Given** staff Resolve
**When** submit kèm note
**Then** ticket → `RESOLVED`; khách nhận notification **plain words**: "Resolved — door hinge replaced by site staff. See ticket for details." (FR-24; NFR-7); note lưu vào thread; card hoàn tất

**Given** staff Escalate
**When** field note trống
**Then** **nút vô hiệu** — không thể escalate không note (FR-24)

**Given** Escalate kèm note ("flooding, Unit M-2. Customer relocation needed.")
**When** submit
**Then** tạo Escalation (một lần duy nhất — `TicketID UNIQUE`, escalate lần hai bị chặn) vào **Escalation Inbox** của Facility Manager + notification manager: "Escalated to Facility Manager — flooding, Unit M-2. Customer relocation needed."

**And** ticket state chuyển qua `TicketService` owner duy nhất (AD-4)

**And** test: resolve + note + notification; escalate thiếu note chặn; escalate lần 2 bị từ chối; thread lưu đủ

### Story 5.3: Severity Decision + Relocation

As a **Facility Manager**,
I want quyết mức nghiêm trọng của sự cố escalated với đầy đủ ngữ cảnh,
So rằng một quyết định kéo cả chuỗi bảo trì + di dời khách đúng chỗ, có vết audit.

**Acceptance Criteria:**

**Given** Escalation Inbox của FM (nav Escalations)
**When** render
**Then** danh sách ticket escalated với severity state; mở ticket thấy **đủ ngữ cảnh**: unit, khách, thread đầy đủ (mô tả gốc → staff note → escalation note)

**Given** FM đánh dấu severe
**When** bấm
**Then** **confirm-destructive modal** nêu hệ quả tiền/trạng thái: "Mark severe — move M-2 to Maintenance and relocate the customer to M-5. This closes the rental and starts turnover." (UX-DR10)

**Given** confirm severe
**When** BE xử lý (một transaction, `TicketService` điều phối)
**Then** Unit M-2 → `MAINTENANCE` **qua event method công khai của `UnitService`** (AD-6 — không set field trực tiếp); **Relocation: đổi Unit trên Rental hiện tại** (M-2 → M-5) — **giữ mã RT-/Contract/Deposit**, **access code mới cấp** (reveal SensitiveValue như 3.4), Activity Log `RELOCATION` (actor, from → to unit); **tasks bảo trì + dọn sinh ra** trên board qua registry; **khách nhận notification từng bước** plain words (relocation, access code mới)

**Given** FM không đồng ý severe
**When** quyết "not severe"
**Then** ticket **về lại staff kèm hướng dẫn** của FM — không bị bỏ rơi

**Given** sau mọi Severity Decision (severe hay không)
**When** ticket state
**Then** quay lại `IN_PROGRESS`; **chỉ `RESOLVED` khi staff hoàn tất phần việc kèm note** (FR-25)

**And** test: severe path flip Unit + đổi Unit trên rental + giữ mã + code mới + log RELOCATION + tasks; not-severe path về staff; transition IN_PROGRESS sau decision

### Story 5.4: Support List + detail drawer (khách)

As a **Customer**,
I want theo dõi ticket của mình và đọc trọn diễn biến khi nó khép,
So rằng tôi biết chuyện gì đã xảy ra với kho của mình và đã được xử lý thế nào.

**Acceptance Criteria:**

**Given** Support List (nav Customer)
**When** render
**Then** danh sách ticket của khách: mã `SR-` mono + incident type + unit + trạng thái badge + thời điểm; **pointer surface** — khớp nguồn, không tự tính lại (FR-35)

**Given** mở một ticket
**When** detail drawer mở (420px phải, Esc/scrim — UX-DR5)
**Then** thread theo trình tự thời gian: mô tả gốc → routing → staff note → escalation (nếu có) → severity decision → bảo trì/di dời (nếu có) → resolution; **ticket `RESOLVED` hiển thị trọn arc trong một drawer** (FR-25)

**Given** chưa có ticket nào
**When** empty state
**Then** factual + đúng 1 CTA **New Support** (trỏ flow đổ dữ liệu — NFR-8)

**And** resolution hiển thị bằng plain words khách đọc được (NFR-7) — không jargon hệ thống

**And** test: list đúng theo user; drawer hiển thị đủ arc theo state; empty state CTA đúng route

---

## Epic 6: Checkout, Settlement & Turnover *(Sprint 2)*

Climax sản phẩm: khách gửi checkout request; staff nhận kho + key, inspection từng hạng mục, hệ thống ép khai charge kèm reason bắt buộc (waiver trong trần — P2), settlement preview đến đồng, phần chênh trả qua QR/Cash, receipt hai bên xem vĩnh viễn; Unit → Preparing → Cleaning task → Available theo Turnover Buffer.
**FRs covered:** FR-17, FR-18, FR-41 (P2), FR-19 (+ FR-20 chân Preparing/Buffer)

### Story 6.1: Checkout Request

As a **Customer**,
I want gửi yêu cầu trả kho kèm giải thích logic tất toán,
So rằng tôi biết trước tiền cọc của tôi sẽ được xử lý thế nào trước khi đến quầy.

**Acceptance Criteria:**

**Given** Rental `CHECKED_IN`, mở Checkout Request từ Rental Detail
**When** render
**Then** form chọn ngày xin trả + giải thích logic tất toán (hoàn đủ / trừ phí / thu thêm nếu charge vượt cọc)

**Given** ngày xin trả
**When** validate
**Then** **bị chặn trên biên Reservation kế tiếp** (cùng rule availability — reuse 4.1)

**Given** submit hợp lệ
**When** BE xử lý
**Then** Reservation → `CHECKOUT_REQUESTED`; **Checkout Task sinh qua registry** (3.2); notification staff

**Given** gửi request mới khi đã có request đang mở
**When** BE xử lý
**Then** **request mới đè request cũ** (cũ tự hủy — không chồng request); request DONE khi settlement hoàn tất (6.3)

**And** test: boundary chặn đúng biên; request đè request; task sinh đúng loại

### Story 6.2: Checkout Task — nhận kho + Inspection

As a **Staff**,
I want nhận Unit + key và chạy checklist tình trạng từng hạng mục,
So rằng bằng chứng cho mọi khoản charge có nguồn.

**Acceptance Criteria:**

**Given** Checkout Task mở
**When** staff nhận kho
**Then** checklist nhận **Unit + key** (confirm từng dòng)

**Given** nhận kho xong
**When** inspection chạy
**Then** từng hạng mục theo ERD: `ACCESS_CARD` / `PADLOCK` / `CLEANLINESS` / `STRUCTURE` — kết quả **OK / MINOR / MAJOR**; ghi `INSPECTIONS`

**Given** hạng mục có kết quả MAJOR
**When** sang bước settlement (6.3)
**Then** MAJOR items hiện làm **căn cứ Settlement Charge**; không có charge nào tự sinh không qua khai báo

**And** kéo card vào Done sau bước này vẫn bị guard 3.5 chặn (settlement chưa khép)

**And** test: inspection persist đủ hạng mục + kết quả; MAJOR surfacing sang settlement

### Story 6.3: Settlement Charge + preview + Extra fee + đóng Rental

As a **Staff**,
I want hệ thống ép tôi khai charge kèm lý do và tính settlement đến đồng trước khi đóng,
So rằng lời hứa cọc được giữ trọn và không có rental nào đóng khi tiền chưa khớp.

**Acceptance Criteria:**

**Given** inspection xong
**When** khai Settlement Charge
**Then** số tiền + **reason bắt buộc** khi damage; thiếu reason → không tiến được (nút vô hiệu)

**Given** rental trả trễ (qua EndDate)
**When** tính preview
**Then** phụ thu `LATE_FEE` tự sinh theo ngày trễ từ policy — hiện thành dòng riêng trong preview (FR-16)

**Given** các charge đã khai
**When** settlement preview render
**Then** Deposit đang giữ − tổng charges = refund; **charges vượt Deposit → phần chênh = Extra fee** — trả qua Payment Modal touchpoint Extra fee (**PayOS QR + Cash at desk**; cash → `PENDING_CASH` + nút "Cash received" ngay trong Checkout Task)

**Given** charge thiếu reason HOẶC phần chênh chưa trả
**When** bấm confirm đóng Rental
**Then** **bị chặn** (FR-18)

**Given** confirm hợp lệ
**When** BE xử lý (một transaction)
**Then** Reservation → `CLOSED`; **settlement receipt hai bên xem vĩnh viễn** (khách thấy ở Rental Detail — "Refund 132.500 ₫ after damage fee 40.000 ₫"); Unit → `PREPARING`; **Cleaning Task sinh** qua registry; notification khách; card Done

**And** refund tại Settlement là receipt + record (không hoàn tiền thật — Non-Goals)

**And** test: preview đúng số demo (cọc 172.500 − 40.000 = 132.500 hoàn); chặn thiếu reason; chặn chênh chưa trả; flip `CLOSED` + Unit `PREPARING` + Cleaning task cùng transaction; receipt vĩnh viễn

### Story 6.4: Settlement waiver trong trần WAIVER_CAP *(P2)*

As a **Staff**,
I want giảm/miễn một charge trong trần có lý do,
So rằng mọi điều chỉnh tiền đều minh bạch và có vết.

**Acceptance Criteria:**

**Given** một Settlement Charge
**When** giảm/miễn **trong trần WAIVER_CAP** (Policy Rule) + reason bắt buộc
**Then** charge điều chỉnh + Activity Log; khách thấy **dòng điều chỉnh + lý do** trên settlement receipt

**Given** mức giảm vượt trần
**When** thao tác
**Then** nút vô hiệu + nêu trần ("Waiver exceeds the 50.000 ₫ cap in Rental Policy v3")

**And** waiver không áp dụng cho deposit forfeiture no-show (FR-36)

**And** test: trong trần OK + log + receipt; vượt trần chặn; no-show loại trừ

### Story 6.5: Cleaning hoàn tất từ card + Turnover Buffer

As a **Staff**,
I want hoàn tất cleaning ngay trên card và hệ thống tự tính khi unit sẵn sàng,
So rằng lời hứa ngày "Available" luôn đúng buffer.

**Acceptance Criteria:**

**Given** Cleaning Task trên board
**When** hoàn tất
**Then** thao tác **ngay trên card** (không màn riêng — FR-19)

**Given** cleaning hoàn tất
**When** BE kiểm tra Turnover Buffer
**Then** đủ → Unit → `AVAILABLE` (**hoặc `RESERVED` nếu có Reservation kế**); chưa đủ ngày → **giữ `PREPARING`, card không hoàn tất được** (block + toast nêu ngày đủ buffer)

**And** kết thúc Maintenance (5.3 / 7.1) cũng đi qua `PREPARING` + Cleaning Task theo buffer — cùng một đường, không bao giờ về thẳng Available (FR-20 chân)

**And** test: buffer đủ → Available/Reserved đúng theo reservation kế; buffer thiếu → giữ Preparing + card mở

---

## Epic 7: Facility Management & Oversight *(Sprint 3)*

FM quản lý cơ sở có vết audit: retire/set-maintenance/fix-status qua guard + confirm + reason trong edit drawer; phân công staff × Zone × ca × ngày chống trùng; Activity Log append-only xem được; Facility Overview KPI bấm xuyên xuống đúng slice.
**FRs covered:** FR-26, FR-20, FR-27, FR-28, FR-32

### Story 7.1: Unit Management + edit drawer

As a **Facility Manager**,
I want quản lý unit có guard + vết audit,
So that không có thay đổi trạng thái nào diễn ra âm thầm.

**Acceptance Criteria:**

**Given** Unit Management
**When** render
**Then** bảng: code mono, size, type, zone/floor, status badge, link rental/reservation active, last activity; pagination 25; row click → edit drawer

**Given** edit drawer
**When** thao tác
**Then** sửa specs; **Retire** — guard kiểm không Rental/Reservation active (blocker **nêu tên + link** reservation), confirm "cannot be undone"; **set Maintenance** — guard + confirm; **Fix status** — **reason bắt buộc** (textarea) khi trạng thái lệch thực tế

**Given** fix status submit
**When** lưu
**Then** Activity Log: actor, timestamp, from → to, reason ("Tuấn changed M-2 from Rented to Preparing — 'Customer moved out Oct 1; status not updated at desk.'")

**Given** kết thúc Maintenance
**When** xử lý
**Then** luôn qua `PREPARING` + Cleaning Task theo buffer (cùng đường 6.5) — không bao giờ thẳng Available (FR-20)

**And** Merge = P2 **không build v1** (demo beat dùng Retire S-2); mọi write → Activity Log — không silent write

**And** test: retire bị chặn khi có reservation (nêu tên + link); fix status thiếu reason chặn; đủ dòng log

### Story 7.2: Staff & Shifts + conflict detection

As a **Facility Manager**,
I want phân công staff theo Zone × ca × ngày với phát hiện xung đột,
So that không bao giờ có hai phân công trùng người-ca-ngày.

**Acceptance Criteria:**

**Given** Staff & Shifts
**When** render
**Then** staff roster + form phân công staff × Zone × ca (`MORNING`/`AFTERNOON`/`EVENING`) × ngày; **lưới lịch tuần** hiển thị phân công hiện tại

**Given** phân công trùng (cùng staff × ca × ngày)
**When** submit
**Then** **từ chối trước khi save** + collision cụ thể: "Minh is already assigned Morning shift, Zone B on Oct 12. Choose another staff member or another shift." + **slot xung đột highlight** trong lưới

**And** không lưu được phân công trùng (unique constraint); user chọn staff/ca khác rồi save thành công

**And** test: conflict rejected trước save với collision đúng; slot highlight; hợp lệ lưu

### Story 7.3: Activity Log append-only view

As a **Facility Manager**,
I want xem audit trail đầy đủ của mọi thay đổi,
So rằng mọi thao tác đều truy được actor, thời điểm và lý do.

**Acceptance Criteria:**

**Given** Activity Log
**When** render
**Then** bảng: timestamp, actor, entity, action, from → to, reason (nơi bắt buộc); **lọc theo entity type**; read-only; pagination 25

**And** không có UI xóa/sửa; dữ liệu chỉ INSERT (đảm bảo tầng dữ liệu từ 1.2)

**And** test: filter theo entity type; đủ cột + reason

### Story 7.4: Facility Overview + KPI drill-down

As a **Facility Manager**,
I want dashboard tổng quan cơ sở với KPI bấm xuyên xuống được,
So rằng không có con số nào tôi không truy được rows phía sau.

**Acceptance Criteria:**

**Given** Facility Overview (landing FM)
**When** render
**Then** theo thứ tự UX-DR11: **KPI row** (occupancy / revenue mix / unit-status) → chart block → backing table

**Given** một KPI
**When** click
**Then** drill vào Unit Management **pre-filter đúng slice** (occupancy 87% → filter `Rented`); filter encode vào URL query params — chia sẻ lại được

**And** không số liệu nào không với tới rows (FR-32)

**And** test: KPI khớp bảng nền; drill encode filter đúng URL

### Story 7.5: Operations Monitor live view *(P2)*

As a **Facility Manager**,
I want màn vận hành trực tiếp,
So rằng tôi thấy việc hôm nay, escalation và hàng chờ buffer đang diễn ra.

**Acceptance Criteria:**

**Given** Operations Monitor
**When** render
**Then** live: tasks hôm nay, escalations, turnover-buffer queue; polling 10–15s theo contract (Conventions)

**And** P2 — cắt không phá flow (FR-32)

---

## Epic 8: Pricing Policy & Business Intelligence *(Sprint 3)*

Chính sách xấu không bao giờ tồn tại (validate-trước-khi-lưu + cắm cờ + banner nêu luật); policy chứa giá thuê, phụ thu trần, Turnover Buffer, Deposit % (+ Discount P2); Business Overview + Reports KPI xuyên xuống rows, tiền sinh từ PricingEngine snapshot.
**FRs covered:** FR-29, FR-30, FR-40 (P2), FR-31

### Story 8.1: Policy Management validate-trước-khi-lưu

As a **Business Ops**,
I want cấu hình chính sách với validation chặn trước khi lưu,
So that một chính sách vô lý không bao giờ tồn tại để ảnh hưởng khách.

**Acceptance Criteria:**

**Given** Policy Management
**When** render
**Then** bảng rule **inline edit** (click-to-edit, blur/Enter save, Esc cancel, unsaved-changes guard — UX-DR7): base rent theo Unit Type, phụ thu + trần, **Turnover Buffer (ngày)**, **Deposit %** — đều là Policy Rule theo phiên bản (FR-30)

**Given** bấm Save
**When** validation chạy
**Then** giá trị vi phạm **cắm cờ** + **banner đầu form nêu đúng luật**: "Policy not saved. Late-checkout fee 15% exceeds the 10% cap in Rental Policy v3. Fix the value, or edit the cap first." — **policy không hợp lệ không bao giờ persist** (FR-29)

**Given** Save thành công
**When** lưu
**Then** đóng dấu **version + effective date** + toast xác nhận; notification BO (policy save)

**Given** policy mới có effective date
**When** booking cũ đọc lại
**Then** vẫn dùng snapshot (AD-11); mọi tính toán availability/deposit **đọc từ policy active** tại thời điểm query

**And** test: bản xấu không persist; version + effective stamp; snapshot booking cũ không đổi sau policy mới

### Story 8.2: Discount % theo Unit Type *(P2)*

As a **Business Ops**,
I want cấu hình giảm giá theo loại kho như một policy rule,
So that khuyến mãi có kiểm soát và hiện rõ cho khách trước khi trả tiền.

**Acceptance Criteria:**

**Given** Policy Rule `DISCOUNT`
**When** cấu hình
**Then** nằm trong cùng bảng validate của 8.1; trần discount mặc định 50% (chỉnh được); vượt trần → chặn như mọi rule

**Given** discount hiệu lực
**When** khách xem
**Then** **dòng giảm giá hiển thị ở Unit Detail + Booking Summary**; giá sau giảm là căn cứ Contract auto-draft + tiền cọc (AD-11)

**And** không có dòng giảm nào xuất hiện ở payment mà vắng ở Booking Summary (khớp FR-7)

**And** test: discount tính đúng vào breakdown + snapshot; trần chặn

### Story 8.3: Business Overview + Reports + KPI drill-down

As a **Business Ops**,
I want đọc doanh thu theo kỳ với mọi KPI xuyên xuống được rows,
So rằng tôi tin được con số vì tôi với tới được nguồn của nó.

**Acceptance Criteria:**

**Given** Business Overview (landing BO)
**When** render
**Then** KPI **revenue / deposits held / surcharges / utilization** + charts theo kỳ, theo thứ tự UX-DR11

**Given** Reports
**When** render
**Then** tabs **Revenue / Deposits / Surcharges / Occupancy** + preset kỳ (this month, last month, quarter) + custom range

**Given** một KPI
**When** click
**Then** drill xuống bảng **pre-filter đúng slice** (surcharges quarter → tab Surcharges cùng kỳ); filter/tab/period **encode vào URL query params** (UX-DR11)

**And** **deposits held ≠ revenue** — tính từ PAYMENTS theo purpose (AD-11); mọi tiền tổng hợp từ receipt/payment đã ghi, cấm tính lại từ term

**And** test: KPI khớp rows; deposits held tách khỏi revenue; drill encode URL đúng

### Story 8.4: Export CSV Reports *(P2)*

As a **Business Ops**,
I want xuất báo cáo ra CSV,
So rằng tôi nộp báo cáo cuối kỳ mà không chép tay.

**Acceptance Criteria:**

**Given** một tab Reports đã lọc kỳ
**When** bấm Export CSV
**Then** trả `text/csv` + **UTF-8 BOM** (mở Excel đúng tiếng Việt — ngoại lệ duy nhất khỏi JSON envelope, AD-8); sinh on-request, không lưu file

**And** notification export completion (BO — FR-34)

**And** test: BOM present; nội dung khớp slice đã lọc

---

## Epic 9: System Administration *(Sprint 3)*

SysAdmin cấp tài khoản mọi role với mật khẩu tạm (không cần script), đổi role, activate/deactivate/lock (không delete); mọi thao tác có dòng audit; soi nhật ký LOGIN/LOGIN_FAILED; permission matrix read-only (P2).
**FRs covered:** FR-37, FR-38, FR-39 (P2)

### Story 9.1: User Management — SYS-01

As a **System Administrator**,
I want cấp và quản lý tài khoản mọi role trong UI,
So that nhân viên mới có tài khoản trước ca sáng mà không cần script, và không ai biến mất khỏi hệ thống.

**Acceptance Criteria:**

**Given** User Management (landing SysAdmin)
**When** render
**Then** bảng: họ tên, email mono, phone, role chip, status badge (Active / Inactive / Locked), last login; search + filter theo role & status; pagination 25

**Given** Create drawer
**When** tạo tài khoản
**Then** name, email, phone, **role select đủ 5 role**, **temp password generated — copy-once, trả trong response đúng 1 lần** (AD Conventions); email trùng → **chặn inline**

**Given** đổi role / activate / deactivate / lock / reset password
**When** thao tác
**Then** ghi Activity Log (actor, target user, from → to); Status theo `users.Status` (0/1/2); **không có delete**; reset password = temp password mới (không email — khớp FR-3)

**Given** thao tác lên chính tài khoản mình đang dùng
**When** deactivate/lock chính mình
**Then** bị chặn

**Given** tài khoản bị deactivate/lock
**When** đăng nhập
**Then** message chung (đã enforce 1.3 — hiển thị đúng ở đây)

**And** test: create + temp password once; status flip + dòng log; tự khóa chặn; email trùng chặn

### Story 9.2: Login & Activity History — SYS-02

As a **System Administrator**,
I want soi nhật ký đăng nhập của toàn hệ thống,
So rằng nghi vấn nào cũng trả lời được "ai, khi nào, kết quả".

**Acceptance Criteria:**

**Given** Login & Activity History
**When** render
**Then** danh sách event `LOGIN` / `LOGIN_FAILED` từ Activity Log (EntityType USER): actor, thời điểm, kết quả; **filter theo user / thời gian / kết quả**; filter chip nêu user đang soi; pagination 25

**And** pointer surface read-only — hiển thị đúng nguồn, không tự tính lại (FR-38); không lần đăng nhập nào vắng log (ghi từ 1.3)

**And** test: filter theo user/kết quả; event đủ sau các lần login thành công/thất bại

### Story 9.3: Permission matrix read-only *(P2)*

As a **System Administrator**,
I want xem ma trận phân quyền đúng như server enforce,
So rằng UI không bao giờ mâu thuẫn với enforcement thật.

**Acceptance Criteria:**

**Given** permission matrix (section trên User Management)
**When** render
**Then** bảng **Role × Permission** (menu, hành động, phạm vi facility) — sinh từ **cùng định nghĩa matrix trong code** (1.3), không copy tay; read-only, không chỉnh runtime

**And** phạm vi: Staff theo Zone phân công, Facility Manager theo cơ sở, Business Ops / System Administrator toàn hệ (FR-39)

**And** test: ma trận khớp định nghĩa code từng role
