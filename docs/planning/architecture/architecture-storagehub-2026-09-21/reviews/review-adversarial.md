---
lens: adversarial
target: ARCHITECTURE-SPINE.md (StorageHub, draft 2026-09-21, updated 2026-09-22)
reviewer: Reviewer Gate — lens adversarial
date: 2026-09-22
status: PASS-WITH-CONDITIONS (xem Verdict)
method: >-
  Giả lập 2 đơn vị cấp dưới cùng build song song theo ĐÚNG CHỮ ĐEN mọi AD, rồi tìm cặp triển khai
  vẫn hợp lệ từng AD nhưng ghép không nhau. Mỗi cặp = 1 lỗ hổng cần AD mới hoặc AD siết.
  Quy ước: **B1** = team "Booking & Payment" (Catalog\, Reservation\, Payment\, Quote) ·
  **B2** = team "Rental Ops & Paper" (Contract\, Extension\, Checkout\, Task\, Unit) ·
  **FE** = track frontend chống MSW.
---

# Review adversarial — Architecture Spine StorageHub

## Verdict

**PASS-WITH-CONDITIONS.** Spine chắc ở tầng lớp/contract (AD-1…3, 5, 7 format), nhưng rò ở **đường nối giữa các module**: ownership matrix không liệt kê, "on-read idempotent" cho phép 2 cách hiểu về persist, pipeline tiền tính 2 nơi, file bản ký và notification không có AD nào phủ. Có **11 cặp** hợp lệ-từng-AD mà ghép không nhau; 4 cặp (F1, F2, F6, F7) phá demo trực tiếp. Yêu cầu: bổ túc AD theo bảng "Đề xuất đóng lỗ hổng" trước khi đóng băng sprint contract.

---

## Quy tắc tấn công chung

Mọi cặp dưới đây đều có dạng: AD nói "cái gì bị cấm" nhưng không nói "ai là người duy nhất được làm" hoặc "làm ở thời điểm nào" — nên 2 đơn vị cùng hợp lệ. AD hiện có tự vệ tốt chống lỗi *tầng* (layer), kém chống lỗi *đường nối* (seam).

---

## Findings

### F1 — CHẶN: không ai là owner được ghi Unit / RESERVATIONS — AD-6 nói "một owner" nhưng không liệt kê, AD-4 nói "module owner" nhưng không định nghĩa khi event đến từ module khác `[nặng nhất]`

**Cặp tấn công:** FR-36 hết hạn → Unit → Available; FR-15 check-in → Unit RENTED; FR-18 settlement → Unit Preparing; FR-19 cleaning xong → Unit Available/Reserved; FR-25 severe → Unit Maintenance. Năm FR, bốn module.

- **B1** đọc AD-3 ("mọi business logic nằm ở service" — service *của mình*) và AD-4 ("transition đi qua service method của module owner" — owner của *nghiệp vụ đang chạy*): ReservationService tự `unit.setStatus(AVAILABLE)` khi hết hạn, trong transaction của chính nó, bằng UnitRepository.
- **B2** đọc AD-6 ("mỗi entity có đúng một owner service được ghi" — owner của *entity*): UnitService là nơi duy nhất ghi Unit; TaskService/ContractService chỉ được gọi `UnitService`.

Cả hai trích dẫn AD đúng chữ. Kết quả ghép: 2 đường ghi Unit song song, guard FR-20 "không silent status write" kiểm được đường của B2 nhưng mù đường của B1; race giữa expiry batch của B1 và cleaning completion của B2 cùng.flip Unit trong cùng đêm → Available hai lần / đè Reserved. Cùng vấn đề trên bảng `RESERVATIONS` (V3 gộp Rental): ExtensionService dịch EndDate, CheckoutService flip CHECKOUT_REQUESTED, PaymentService flip RESERVED — mỗi bên đều biện minh "logic nghiệp vụ của tôi nằm service tôi" theo AD-3.

**Fix — siết AD-6 (bắt buộc thêm bảng):** thêm bảng ownership enumeration ngay trong AD-6: `RESERVATIONS→ReservationService, UNITS→UnitService, CONTRACTS/CONTRACT_ADDENDUMS→ContractService, PAYMENTS→PaymentService, TASKS→TaskService, ...`, kèm rule 1 câu: *"Module không-owner muốn đổi trạng thái entity ngoài scope phải gọi event method công khai của owner service (vd `UnitService.apply(UnitEvent)`); cấm set field / save repository entity ngoài owner — kể cả trong cùng transaction."*

---

### F2 — CHẶN: "tính on-read idempotent" có 2 cách hiểu — suy diễn rồi KHÔNG ghi, hay suy diễn rồi CÓ ghi lại — trong khi EXPIRED kéo theo tiền (mất cọc) `[nặng]`

**Cặp tấn công:** AD-4: "Trạng thái phái sinh từ thời gian (EXPIRED, turnover buffer) được tính on-read idempotent trong service".

- **B1** hiểu: derived state KHÔNG BAO GIỜ persist — cột `status` giữ RESERVED mãi mãi, EXPIRED là phép suy diễn ở DTO (`status==RESERVED && now > deadline`). Side effects FR-36 (receipt "Deposit forfeited", Unit → Available, Contract → Closed, Activity Log, notification) thực hiện lazily ngay trong read path lần đầu, guard exactly-once bằng unique receipt code.
- **B2** hiểu: on-read là *cửa đọc*, còn hệ quả thì một **scheduled batch** hằng ngày persist EXPIRED + đẩy side effects — để Report (FR-31) không phải tính lại.

Cả hai "idempotent". Ghép: trước giờ batch, API Reservation (B1) trả EXPIRED nhưng dashboard (B2) đếm RESERVED — KPI occupancy lệch bảng nguồn, vỡ FR-35 "mọi con số khớp nguồn". Nếu cả hai đường cùng chạy (B1 lazy + B2 batch) → 2 receipt forfeited cho 1 reservation, vỡ FR-9 "đúng 1 receipt". Nếu B2 dùng DB status để tính Unit "đã trả Available" còn B1 dùng derived → Browse (FR-4) và Unit Management (FR-26) hiện 2 tập Available khác nhau.

**Fix — siết AD-4 (thêm 2 câu):** *"Trạng thái suy diễn không bao giờ được persist ngược vào cùng trường trạng thái gốc; API trả derived state trong DTO."* + *"Mọi hệ quả của transition suy diễn theo thời gian (receipt, Unit flip, log, notification) do owner service thực hiện exactly-once lazily tại lần đọc/đụng tới đầu tiên, guarded bằng idempotency key (unique constraint); cấm scheduled batch side-effect song song."*

---

### F3 — Payment: AI flips 5 trạng thái, PROCESSING có persist không, tham số test nằm ở đâu, QR hết hạn ai quyết `[nặng]`

**Cặp tấn công trên 4 mảnh, đều tuân AD-9 chữ đen:**

1. **"Kết quả quyết bởi tham số test" — không nói tham số nằm đâu.** B1 để `mockOutcome` trong body request (ghi vào openapi.yaml, FE gửi `mockOutcome=FAILED` để demo fail) — nghĩa là FE *quyết định* kết quả, phá ngầm ý "FE không bao giờ tự quyết kết quả payment" ngay trong chính AD đó. B2 để tham số trong config gateway server-side (profile dev). FE build theo contract của B1 gửi field; BE của B2 bỏ qua → không bao giờ demo được luồng Failed, vỡ FR-8.
2. **PROCESSING persist hay không.** B1 tạo row PAYMENTS Pending → Processing → Succeeded (persist đủ 5 trạng thái, Report đếm được). B2 xử lý gateway sync trong 1 request, chỉ persist trạng thái cuối → state chart Payment (Pending/Processing/Succeeded/Failed/Expired) không có bản ghi trường nào phản ánh; nếu demo crash giữa chừng, B1 để lại row Processing mồ côi không ai dọn.
3. **QR ~5 phút hết hạn — FE countdown hay BE time.** FE đếm ngược và gọi "expire" (tự quyết thời điểm) vs BE tính_expired-on-read theo AD-4. Hai bên chọn khác nhau → modal của FE đã về method-select trong khi BE vẫn coi Payment Pending (valid).
4. **Modal "Processing" là local UI state hay phải từ API.** FE render spinner cục bộ ngay khi bấm Pay — có phải "tự quyết trạng thái"? AD-9 không phân biệt display-state vs persisted-state.

**Fix — siết AD-9:** *"Trạng thái Payment là state machine của PaymentService duy nhất; đủ 5 trạng thái được persist theo state chart; tham số outcome là cấu hình server-side của MockPaymentGateway (không bao giờ là field API); mọi mốc thời gian (QR expiry, processing timeout) do BE quyết định on-read — FE chỉ hiển thị đồng hồ đếm từ giá trị server trả; spinner local của modal được phép và không coi là 'quyết kết quả'."*

---

### F4 — Ảnh bản ký FR-11/12: không AD nào phủ upload/lưu/phục vụ file — 2 đường ghép không nhau và lọt auth `[nặng]`

**Cặp tấn công:** ERD chỉ có `varchar SignedPhotoUrl`. B1: `POST /contracts/{id}/signature-photo` multipart → lưu filesystem `uploads/` → lưu URL tuyệt đối → phục vụ bằng static resource handler của Boot. B2: trả ảnh base64 inline trong DTO contract (vì AD-3 "entity không rời backend — API trả DTO" không nói file phải là URL; MSW sinh từ openapi.yaml không có binary thật nên FE tin shape base64). Ghép: FE chờ base64, BE trả URL → nút Attach (FR-11) và card Contract-signature (FR-12) gãy ở integration cuối sprint — đúng cái big-bang AD-2 muốn chống. Tệ hơn: static handler của B1 **bypass JWT filter** → ảnh hợp đồng ký tay truy cập được không đăng nhập, vỡ NFR-4 trong khi B1 không vi vọng AD nào. Cột `SignedPhotoUrl` cũng có 2 ứng viên ghi (ContractService vs một AttachmentService chung) — lại F1.

**Fix — AD mới "AD-10 — File storage & serving":** rule 1 câu: *"Ảnh bản ký upload qua endpoint riêng (multipart), lưu tại đường dẫn backend quản lý, phục vụ về FE CHỈ qua controller stream đã qua JWT + permission check; cột `*PhotoUrl` chỉ lưu API path tương đối (`/api/v1/...`), do đúng một owner service ghi; cấm static resource mapping công khai và cấm base64 trong DTO list."*

---

### F5 — Bell "record bền vững": ai ghi NOTIFICATIONS, khi nào ghi, vocab Type/DeepLink `[trung bình]`

**Cặp tấn công:** NFR-2 "toast tức thời, bell record bền vững — toast không bao giờ thay thế bell", nhưng spine không nói ai INSERT vào NOTIFICATIONS.

- B1: mỗi feature service tự insert NotificationRepository trong transaction nghiệp vụ của mình (biện minh AD-3), Type tự đặt (`PAYMENT_SUCCEEDED`), DeepLink tự đặt (`/rentals/RT-0871`).
- B2: xây `NotificationService.notify(user, event, ref)` trung tâm + nghe event — nhưng B1 không biết mà dùng.

Ghép: Notification Center (B2/FE) nhóm theo Type không khớp vocab của B1; DeepLink một bên FE-route-path, một bên `rental:87` → router gãy; tách transaction (event after-commit) thì payment success (FR-9) ghi receipt + flip được mà **mất bell** — toast hiện, bell trống, vỡ đúng điều NFR-2 cấm.

**Fix — siết Capability map + thêm câu vào AD-6:** *"NOTIFICATIONS do NotificationService sở hữu độc quyền ghi, gọi đồng bộ trong cùng transaction nghiệp vụ qua API event có kiểu (`NotificationEvent` enum + deep-link registry đặt trong openapi.yaml, FE route phải khớp registry); cấm module tự INSERT notification."*

---

### F6 — Mâu thuẫn nội tại bindings: RESERVATIONS 1—1 CONTRACTS (UNIQUE FK) vs FR-10 re-draft "bản cũ → Superseded, vẫn đọc trong chuỗi" `[nặng — dữ liệu]`

**Cặp tấn công:** AD-6 buộc Flyway V1 = model V3 với `FK ReservationID UNIQUE trên contracts`; FR-10 buộc "sai sót Draft sửa bằng re-draft — bản cũ chuyển Superseded" (nghĩa là một reservation có ≥2 contract). B1 (tuân model V3) không thể insert contract thứ hai — crashes UNIQUE; B2 (tuân FR-10) viết migration V2 thả unique constraint → schema drift giữa 2 nhánh code, đúng tội AD-6 định chống. Hai bên đều "đúng theo tài liệu nguồn" vì spine bind hai nguồn mâu thuẫn nhau mà không xử. Liên quan: state chart Contract ghi `Active → Superseded : addendum thay ngày kết thúc` nhưng không nói **lúc nào** — B1 flip Superseded khi addendum *được thanh toán*, B2 flip khi addendum *được ký*; FR-13 chain hiển thị badge khác nhau giữa 2 build; FR-10 cũng dùng nhãn Superseded cho re-draft — một nhãn, hai nghĩa.

**Fix — xử trong spine trước khi V1 Flyway đóng băng:** chọn 1 trong 2: (a) `CONTRACTS 1—N` với cột `IsLatest`/`SupersedesContractID` (khuyến nghị, giữ FR-10), hoặc (b) giữ 1—1 và re-draft = version mới của cùng row + snapshot log. Kèm câu siết AD-4: *"Superseded có đúng một nghĩa: bị thay thế bởi bản mới hơn trong chuỗi; thời điểm flip là khi bản thay thế có hiệu lực (addendum: sau thanh toán; re-draft: ngay khi bản mới sinh)."*

---

### F7 — Pipeline tiền: AD-7 cấm float nhưng không cấm "2 nơi tính" — Booking Summary vs Contract auto-draft vs KPI `[nặng — tiền]`

**Cặp tấn công:** AD-7 "mọi phép tính tiền ở service" — *service nào* không nói. B1 xây `QuoteService` tính breakdown (rent × duration + phụ thu + Deposit% + Discount FR-40) theo policy active **tại lúc browse**. B2 xây `ContractService` auto-draft tính lại **tại lúc deposit thành công** từ "phiên bản policy khóa". Nếu Business Ops save policy version effective giữa 2 thời điểm (FR-29 cho phép): Unit Detail/Booking Summary (FR-6 "khớp tuyệt đối") hiện 1.150.000 ₫, contract draft 1.200.000 ₫, receipt lại số thứ ba — vỡ FR-7 "không dòng tiền nào vắng ở breakdown". Tương tự FR-15: 100% rent tính theo policy version nào (lúc booking hay lúc check-in) — 2 answers đều đọc được từ "khóa phiên bản policy". Tầng báo cáo: B1 tính revenue = rent terms, B2 = SUM(payments); "deposits held không phải revenue" (FR-31) hai người hiểu hai mức → KPI drill-down (P1, cam kết "mọi số liệu với tới rows") ra tổng ≠ tổng bảng rows.

**Fix — AD mới "AD-11 — Pricing single-source":** rule 1 câu: *"Toàn bộ con số tiền sinh ra từ một PricingEngine duy nhất đọc policy theo đúng một quy tắc resolve (phiên bản hiệu lực tại thời điểm Booking Summary, snapshot vào reservation; contract/receipt/check-in dùng lại snapshot đó); Report chỉ được tổng hợp từ receipt/payment đã ghi, cấm tính lại từ term."*

---

### F8 — Envelope lỗi: Spring Security 401/403 thoát khỏi @ControllerAdvice; page 0-based/1-based; error code theo endpoint `[trung bình]`

**Cặp tấn công:** AD-8 "mọi lỗi HTTP theo một envelope duy nhất". B1 tin rằng envelope là trách nhiệm @ControllerAdvice — nhưng 401/403 sinh ở **security filter trước khi vào controller**, Spring trả body mặc định `{timestamp,status,error}`. B2 (chưa đụng security) review code không thấy chỗ nào vi phạm. FE parse envelope của B2 crash khi gặp 401 body mặc định giữa demo (JWT TTL 24h không refresh — chắc chắn xảy ra). AD-8 cũng không chốt: `page` bắt đầu 0 hay 1; business-rule block (FR-5 "unit bị chiếm", FR-27 conflict) là 409 + envelope hay 200 + flag; filter/sort param cho drill-down (FR-31 KPI → Unit Management lọc Rented) encode kiểu gì — 2 FE tự chọn khác nhau, deep-link chia sẻ cho nhau gãy.

**Fix — siết AD-8:** *"Envelope áp dụng cho CẢ lỗi tầng security (cài AuthenticationEntryPoint/AccessDeniedHandler trả đúng envelope); `page` 1-based; mọi operation trong openapi.yaml phải liệt kê đầy đủ response 4xx kèm code máy; business-rule block = 409 + envelope; filter param kebab-case nằm trong contract."*

---

### F9 — Biên "ngày" không có AD: UTC vs Asia/Ho_Chi_Minh quyết định EXPIRED flipping 7 giờ `[trung bình]`

**Cặp tấn công:** AD-7 chốt *format* (lưu UTC, hiển thị ICT) nhưng không chốt *ngữ nghĩa day-boundary*. FR-36 "hết ngày nhận kho", FR-12 deadline ký 7 ngày, FR-27 shift theo ngày — B1 lưu `Instant` UTC và so `now > deadline` ở UTC → EXPIRED flip 07:00 sáng ICT; B2 dùng `LocalDate` theo lịch ICT → flip 00:00 ICT. Cả hai "lưu UTC, hiển thị ICT" đúng chữ. Ghép: đêm trước demo, một bên đã trả Unit về Available (khách còn đặt được), một bên vẫn Reserved; report hai bên đếm no-show khác nhau.

**Fix — siết AD-7:** *"Mọi biên nghiệp vụ theo ngày (deadline, shift, expiry, due date) được đánh giá theo lịch Asia/Ho_Chi_Minh; cột kiểu ngày nghiệp vụ dùng DATE/LocalDate ngữ nghĩa ICT, timestamp dùng UTC Instant — quy đổi ở service, không ở query so sánh thô."*

---

### F10 — Task guard đọc chéo module: TaskService inject PaymentRepository/ContractRepository `[nhẹ]`

**Cặp tấn công:** FR-22 snap-back cần biết "payment/settlement/ảnh bản ký xong chưa". AD-3 cấm controller→repository nhưng không cấm service A đọc repository của module B. B1: TaskService @Autowired PaymentRepository + ContractRepository, tự JOIN điều kiện closable; B2: gọi service của module chủ (`paymentService.isSettled(ref)`). Cả hai hợp lệ; build của B1 compile-couple vào schema module khác — khi Payment refactor (điều chắc chắn xảy ra sau F7), Task gãy âm thầm. Phiên bản thứ hai của cùng lỗ: B1 denormalize cờ `closableAt` khi payment xong, B2 derive on-read → kéo Done lúc payment vừa flip có kết quả khác nhau giữa 2 build (tái phát F2 ở cấp task).

**Fix — thêm 1 câu vào AD-3 hoặc AD-6:** *"Repository chỉ được inject trong cùng module; module khác đọc qua service method công khai (query method không đổi trạng thái); cấm denormalize trạng thái của entity khác module — đọc chéo luôn derive-on-read."*

---

### F11 — ActivityLog: APPEND-ONLY được giữ, nhưng vocab Action/EntityType và kênh ghi thì không `[nhẹ]`

**Cặp tấn công:** AD-6 "append-only, chỉ INSERT" — hai bên đều INSERT. B1 ghi trong cùng transaction nghiệp vụ; B2 qua event listener after-commit (đẹp hơn, nhưng crash giữa commit-log là mất log — vỡ FR-38 "không lần đăng nhập nào vắng log" cho LOGIN event). Action string tự đặt: `CONTRACT_SIGNED` vs `CONTRACT_ATTACH` cho cùng sự kiện FR-11; SYS-02 filter theo action không khớp; FR-41 waiver + FR-20 fix-status cần `Reason` NOT NULL theo loại action — mỗi service tự phân loại loại-nào-cần-reason.

**Fix — siét AD-6 thêm câu:** *"ActivityLog ghi đồng bộ trong cùng transaction qua một LogService với Action/EntityType enum chung trong code (registry), khai báo bắt-buộc-reason theo loại action tại registry — không phải tại từng caller."*

---

## Bảng tổng — Đề xuất đóng lỗ hổng

| # | Loại | AD | Rule mới/siét (tóm tắt 1 câu) |
| --- | --- | --- | --- |
| F1 | Siết AD-6 | Ownership matrix | Liệt kê owner từng entity + cross-module chỉ được gọi event method của owner, cấm set field/save repo ngoài owner |
| F2 | Siết AD-4 | Derived-state boundary | Derived state không persist ngược; side-effect exactly-once lazily, cấm batch song song |
| F3 | Siết AD-9 | Payment statemachine | PaymentService duy nhất flips 5 trạng thái persist; outcome param là config server-side; mọi timeout/QR expiry do BE quyết |
| F4 | AD mới AD-10 | File handling | Upload qua endpoint riêng, phục vụ qua controller stream có JWT; PhotoUrl = API path tương đối, một owner ghi |
| F5 | Siết AD-6 + Capability map | Notification | NotificationService độc quyền ghi, cùng transaction, event enum + deep-link registry trong contract |
| F6 | Xử mâu thuẫn bindings | Contract 1—1 vs re-draft | Chọn 1—N + IsLatest (khuyến nghị) hoặc version-in-place; định nghĩa duy nhất cho Superseded + thời điểm flip |
| F7 | AD mới AD-11 | Pricing single-source | Một PricingEngine, policy snapshot tại Booking Summary, contract/receipt/report dùng lại snapshot/chỉ đọc receipt |
| F8 | Siết AD-8 | Envelope trọn vẹn | Security layer cũng trả envelope; page 1-based; mỗi operation liệt kê đủ 4xx; filter kebab-case trong contract |
| F9 | Siết AD-7 | Day-boundary ICT | Biên ngày nghiệp vụ theo Asia/Ho_Chi_Minh; DATE = LocalDate ICT, timestamp = UTC |
| F10 | Siết AD-3 | Module boundary read | Repo chỉ inject trong module; đọc chéo qua service query method; cấm denormalize state module khác |
| F11 | Siết AD-6 | Log channel | Một LogService đồng transaction, Action enum registry, reason-required khai báo tại registry |

## Ghi chú

- Các cặp F1/F2/F3/F6/F7 phá demo trực tiếp (SM-1/SM-2) — nên chốt trước khi `contracts/openapi.yaml` đóng băng sprint đầu (AD-2), vì F3/F7/F8 làm thay đổi shape của contract.
- F6 là lỗi *trong tài liệu nguồn* (model V3 vs FR-10), không phải lỗi diễn giải — spine buộc phải phán một bên vì hiện bind cả hai.
- Không tìm thấy đường tấn công qua AD-1 (stack), AD-5 (auth matrix — chỉ còn lỗ 401/403 envelope đã gộp vào F8) — hai AD này đủ đặc.
