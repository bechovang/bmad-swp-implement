# Review — Reconcile PRD ↔ Architecture Spine

- **Ngày review:** 2026-09-22
- **Reviewer:** reconcile reviewer (bmad-architecture)
- **Đối tượng:** `ARCHITECTURE-SPINE.md` (updated 2026-09-22) đối chiếu `prd-storagehub-2026-09-17/prd.md` (final, updated 2026-09-21)
- **Nguồn phụ trợ:** `reconcile-design.md`, `reconcile-experience.md` (các fix đã duyệt), `ERD_Statechart_LaTeX/mermaid/erd.mmd` (xác minh 22 entity model V3)

## Verdict

Spine land tốt phần lớn khối lượng PRD — 9/14 nhóm FR khớp rõ, không quyết định nào **trái** PRD, các AD lõi (AD-4/6/7/8/9) khớp mạnh với semantic khó nhất (EXPIRED derive, money format, append-only, mock gateway). Sót lại: **1 lỗi map** (FR-41 đặt nhầm hàng), **1 NFR rơi hoàn toàn** (NFR-8 không nằm trong `binds` lẫn Capability Map), vài **quiet requirement không có nhà** (NFR-7 voice, NFR-2 a11y, seed data demo, side-effects của FR-36), **1 căng thẳng nội tại AD-6** (single-owner vs `Reservation*`/`Rental*` cùng ghi một bảng sau merge V3), và **2 quyết định PRD §9.1 giao cho architecture vẫn treo**.

---

## 1. Land-map — 14 nhóm FR (theo hàng Capability Map của spine)

| # | Nhóm (hàng Capability Map) | FR | Land vào đâu | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 1 | Auth & Account | FR-1…3 | AD-5; `Auth*` + `security/`; FE Login/Register/Forgot | **Khớp** (FR-1/2); FR-3 mờ | FR-3 (P2 stub) không được AD-5 đề cập — không liệt kê endpoint public (F-10) |
| 2 | Catalog & Booking | FR-4…7, 36 | AD-4 + AD-7; `Catalog*`, `Reservation*` | **Khớp** chính | FR-36: state derive land AD-4 nhưng side-effects chưa có nhà (F-03); FR-5 race chưa chốt (F-14) |
| 3 | Payment | FR-8…9 | AD-9 + AD-7; `Payment*` + `PaymentGateway` mock | **Khớp** | AD-9 thiếu con số delay 1.5–2s của NFR-5 (F-13) |
| 4 | Contract & Addendum | FR-10…13 (+41) | AD-4 + AD-6; `Contract*` (state chart, chain, addendum) | **FR-41 sai chỗ** (F-01) | FR-10…13 khớp tốt: Superseded đọc được trong chuỗi, addendum state chart 4 giá trị |
| 5 | Rental ops (check-in · extension · checkout) | FR-14…18 | AD-4 + AD-7; `Rental*` (conflict boundary, settlement) | **Khớp** nghiệp vụ; thiếu FR-41 (F-01); căng tên `Rental*` vs merge V3 (F-04) | |
| 6 | Cleaning & Unit guards | FR-19…20 | AD-4; `UnitService` + `Task*` | **Khớp** | Buffer-check trước Available + fix-status reason → ActivityLog (AD-6) |
| 7 | Task Board kanban | FR-21…22 | AD-4; `Task*`; FE Kanban | **Khớp** | Keyboard Move là mảnh NFR-2 (mờ — F-09) |
| 8 | Support tickets | FR-23…25 | AD-4; `Ticket*` (severity, relocation) | **Khớp** | Relocation cross-module (Ticket → Unit/Reservation) đi qua owner service theo AD-4 — ngầm hiểu, đủ |
| 9 | Facility Admin (unit · staff · policy) | FR-26…30, 40 | AD-6 + AD-7; `UnitMgmt*`, `Staffing*`, `Policy*` (policy versioning) | **Khớp**; 2 điểm mờ | Nhãn hàng gộp 2 role (FR-26-28 = Facility Manager; FR-29/30/40 = Business Ops) — cần tách đúng trong AD-5 matrix (F-15); RuleType DISCOUNT chưa chốt chỗ lưu (F-07) |
| 10 | Dashboards & Reports | FR-31…32 | AD-8; read-only query services; FE chart pages | **Khớp** mức spine | Drill-down P1 (cam kết FR-31/32) và Export CSV P2 chỉ ẩn trong hàng — không ai nêu (F-15) |
| 11 | Notifications (toast + bell) | FR-33…34 | AD-2; `Notification*`; FE toast/bell; pull-based; realtime = Deferred | **Khớp** định hướng; semantics mờ | NOTIFICATIONS có trong 22 entity (xác minh erd.mmd); cơ chế 2 kênh chưa ai sở hữu (F-12) |
| 12 | Customer anchor screens | FR-35 | AD-2; FE pages đọc API sẵn có | **Khớp** | Đúng tinh thần pointer surface; FR-35 tự tham chiếu NFR-8 → móc vào F-02 |
| 13 | SysAdmin | FR-37…39 | AD-5 + AD-6; `SysAdmin*`; FE SYS-01/02 | **Khớp**; 2 điểm mờ | LOGIN/LOGIN_FAILED có trong AD-5 (khớp FR-38); matrix cố định khớp FR-39; nhưng JWT stateless vs lock/đổi role giữa phiên (F-05) và scoping đa facility (F-07) |
| 14 | Cross-cutting (a11y · format · perf) | NFR-1…7 | "envelope AD-8, a11y FE, ActivityLog AD-6" | **Mờ** | Chỉ NFR-1 (AD-7) và NFR-6 (AD-6) land mạnh; NFR-2/5/7 mờ (F-08/F-09); NFR-8 vắng (F-02) |

## 2. Land-map — NFR-1…8 + constraint §6 / §7.1 / §8 / §9.1

| Req | Nội dung chính | Land vào đâu | Trạng thái |
|---|---|---|---|
| NFR-1 | VND full precision, `1.150.000 ₫`, ngày nhất quán, English-only | **AD-7** (BigDecimal + DECIMAL(15,0) + JSON number + UTC/ISO-8601 + `Asia/Ho_Chi_Minh` + format hiển thị trích NFR-1) + hàng convention "Data & formats" | **Khớp mạnh** — đã hấp thụ fix reconcile-design #2. Mảnh FE (English-only, tabular numerals) ngoài spine — nên ghi 1 dòng giao DESIGN.md |
| NFR-2 | A11y floor: label, focus trap/return, keyboard, ≥40px, no color-only, empty có bước kế | Một từ "a11y FE" ở hàng 14; không AD nào chứa rule; không hàng convention | **Mờ** (F-09) |
| NFR-3 | Desktop-first 1200px, light-only | Ẩn trong hàng 14 | Mờ, tác động thấp (thuần FE) |
| NFR-4 | Hash at rest, mọi route yêu cầu đăng nhập, server enforcement, SensitiveValue reveal, audit reason | AD-5 (hash, matrix, per-endpoint) + AD-6 (audit) | **Khớp lớn**; 2 mảnh mờ: Access Code reveal (F-11), hiệu lực lock giữa phiên (F-05) |
| NFR-5 | Availability đúng > nhanh; <2s trên demo với seed; gateway deterministic ~1.5–2s | AD-9 (deterministic) + AD-4 (on-read đúng hơn nhanh); <2s gắn OQ-2 (Deferred) | **Nửa land** — có lý do (OQ-2 mở); nhưng seed data thì không (F-06) |
| NFR-6 | ActivityLog append-only ở tầng dữ liệu; mọi biến động tiền sinh receipt; không silent write | **AD-6** ("chỉ INSERT, không tồn tại đường write update/delete") + AD-4 (không silent state) | **Khớp mạnh**; mảnh "biến động tiền" của FR-36 mờ (F-03) |
| NFR-7 | Voice & microcopy: 3 phần (chuyện gì + hệ quả + đúng 1 bước kế), plain 5 role, cấm "!"/marketing | Không có — AD-8 chỉ có "message human" (cấu trúc envelope, không phải voice) | **Mờ** (F-08) |
| **NFR-8** | States & form discipline: skeleton no-shift, empty 1 CTA, banner-not-toast, submit-disable, unsaved guard, pagination 25 | **KHÔNG land**: frontmatter `binds: NFR-1…NFR-7`, hàng map cũng NFR-1…7. Chỉ slice pagination-25 lọt vào AD-8 | **Miss** (F-02) |
| §6 Non-Goals | No email, mock gateway only, no cancel/OVERDUE, no dark/mobile, no marketplace | AD-4 (Prevents: FE tự suy EXPIRED), AD-9 (đúng một impl mock), không nơi nào thêm email | **Khớp** — không vi phạm |
| §7.1 Stack + scope | FE/BE tách riêng + REST thật cho toàn bộ nghiệp vụ; 29 màn; seed data chuẩn | AD-1 (đúng thẩm quyền chốt OQ-1), AD-2 (contract), scope "29 màn" khớp SM-C1 | **Khớp**; seed data chuẩn chưa có nhà (F-06) |
| §8 Success Metrics | SM-1 demo 6 UJ với seed; SM-2 các bước "ép" | SM-2 land qua AD-4/AD-6; SM-1 phụ thuộc seed (F-06) | Khớp có điều kiện |
| §9.1 Data-model deltas | RuleType thêm TURNOVER_BUFFER/DISCOUNT/WAIVER_CAP; `users.FacilityID` nullable — PRD ghi "chốt ở bmad-architecture" | Ẩn trong "V1 = toàn bộ model V3"; 2 quyết định được PRD giao chưa được quyết và không nằm trong Deferred | **Treo** (F-07) |

---

## 3. Findings chi tiết

### F-01 — FR-41 land nhầm hàng Capability Map `[High · lỗi map]`

Hàng "Contract & Addendum" nhận `FR-10…13, 41` với Lives in `Contract*`. Nhưng FR-41 (Settlement waiver trong trần WAIVER_CAP, §4.7) là hành động của Staff **trong Checkout Task tại settlement** (FR-18), nhà đúng là hàng 5 "Rental ops" (`Settlement*` trong `Rental*`). Đồng thời FR-41 nói rõ WAIVER_CAP là "Policy Rule do Business Ops cấu hình (FR-29/FR-40)" — tức cũng cần mặt ở hàng 9 (Policy) như FR-40 vẫn có. AD-7 binds FR-41 đúng (money), chỉ "Lives in" sai. **Impact:** story cho waiver sẽ sinh ra đụng module contract thay vì settlement. **Fix:** di chuyển FR-41 sang hàng 5; thêm 1 chú thích ở hàng 9 rằng WAIVER_CAP là RuleType do `Policy*` quản.

### F-02 — NFR-8 (States & form discipline) rơi hoàn toàn `[Medium-High · miss]`

PRD §5 có **8** NFR; spine frontmatter `binds: [FR-1…FR-41, NFR-1…NFR-7]` và hàng Cross-cutting "NFR-1…7" — tuyên bố "toàn bộ PRD" nhưng thiếu NFR-8. Bản thân FR-35 tham chiếu `(NFR-8)` ("empty state có CTA trỏ flow đổ dữ liệu"). Chỉ slice pagination-25 lọt vào AD-8 qua trích FR-26/28/31. Phần còn lại — skeleton khớp layout, empty state 1 CTA + echo filter, server-block hiển thị banner đầu form (không toast), Submit chỉ disable theo missing required, unsaved-changes guard khi rời màn — không chỗ nào trong spine sở hữu. Đây đúng nhóm "rule ngang định tính" mà reconcile-experience đã một lần bắt PRD thiếu (gap #3, #4, #7); giờ rơi tiếp ở tầng architecture. **Fix:** sửa binds + hàng map thành NFR-1…8; thêm 1 hàng Consistency Conventions cho FE states/forms (hoặc ghi rõ giao EXPERIENCE.md làm chân lý + đưa vào checklist QA story).

### F-03 — AD-4 "on-read idempotent" chưa giải side-effects của FR-36 `[Medium-High · căng thẳng thiết kế]`

AD-4: trạng thái phái sinh thời gian (EXPIRED) tính on-read trong service — xử lý được **hiển thị**, nhưng FR-36 đòi kèm **writes**: receipt "Deposit forfeited — no-show", Contract Draft/Printed → Closed, dòng Activity Log, và **notification kết thúc cho khách** (bell = record bền vững — không thể derive mỗi lần đọc). Derived-on-read không sinh được notification đúng lúc nếu không ai chạm bản ghi. **Fix:** AD-4 thêm 1 câu chốt cơ chế: scheduled job hằng ngày (hoặc materialize-side-effects lúc read đầu tiên, vẫn idempotent) — chọn một, nêu rõ.

### F-04 — AD-6 "mỗi entity đúng một owner service" mâu thuẫn với chính Capability Map sau merge V3 `[Medium · mâu thuẫn nội tại]`

Model V3 gộp RENTALS vào RESERVATIONS (xác minh erd.mmd: 22 entity, không còn bảng RENTALS; PRD Glossary: Rental "không còn là bảng riêng"). Nhưng map tách `Reservation*` (hàng 2 — booking phase) và `Rental*` (hàng 5 — check-in/extension/checkout) **cùng ghi một bảng RESERVATIONS** → hai writer trên một entity, phá nguyên văn AD-6. **Fix:** hoặc xác định owner = một `Reservation*` module sở hữu trọn vòng đời (hàng 5 là phase cùng module), hoặc nới AD-6 thành "một owner **module**" — nhưng phải viết ra, không để hai hàng ngầm mâu thuẫn.

### F-05 — JWT stateless TTL 24h + không revocation vs FR-37 lock/đổi role giữa phiên `[Medium · quyết có rủi ro chưa ghi nhận]`

AD-5 quyết JWT Bearer TTL 24h, không refresh, logout = client drop token (PRD không nói gì — đây là quyết riêng của spine, chấp nhận được). Nhưng enforce là "Role × Permission cố định" — không nói **kiểm tra `users.Status` mỗi request**. Hệ quả: tài khoản bị deactivate/lock (FR-37) hoặc **đổi role** (FR-37, kèm Activity Log) vẫn mang token hợp lệ đến 24h. PRD NFR-4 chỉ bắt "không đăng nhập được" nên chưa tính trái PRD, nhưng spine quyết cơ chế rồi mà không nêu tradeoff/bù. **Fix:** thêm vào AD-5: mỗi request kiểm tra users.Status active (một query/refresh cache) — hoặc thu ngắn TTL và ghi rõ chấp nhận hở.

### F-06 — Seed data demo không có nhà `[Medium · miss]`

SM-1, NFR-5 và §7.1 đều dựa trên seed data chuẩn (Lan/Minh/Hằng/Tuấn/Nam; S-3, M-2, M-5; BK-1042, RT-0871, SR-0032, CT-1042, CT-1042-A1; policy v3 + trần 10%). PRD OQ-4 đã chốt hướng: SYS-01 cho tài khoản, **script seed vẫn cần cho dữ liệu nghiệp vụ số lượng lớn** (units, reservations lịch sử). Spine AD-6 chỉ nói Flyway V1 = schema; không chữ nào về seed (V2__seed_demo? profile dev? repeatable migration?). **Fix:** 1 dòng AD-6 hoặc 1 mục Deferred — seed demo là migration versioned riêng, chạy ở profile dev, chứa đúng bộ mã mock chuẩn.

### F-07 — Hai quyết PRD §9.1 giao cho architecture bị bỏ lơ `[Medium · treo]`

PRD ghi tường minh "chốt chỗ lưu trước bmad-architecture" / "chốt ở bmad-architecture": (a) TURNOVER_BUFFER/DISCOUNT/WAIVER_CAP — thêm vào `POLICY_RULES.RuleType` **hay field riêng trên RENTAL_POLICIES**; (b) `users.FacilityID` nullable cho scoping đa facility của Facility Manager. Spine không quyết và không đưa vào Deferred — rơi tự do giữa hai artifact. **Fix:** AD-6 chốt luôn (đề xuất: giữ enum RuleType cho đủ loại rule — ERD đã có sẵn `enum RuleType`; `users.FacilityID` nullable, demo 1 facility) hoặc立 OQ trong Deferred.

### F-08 — NFR-7 voice & microcopy land mờ `[Medium · mờ]`

Không AD hay convention nào chứa khuôn thông điệp 3 phần (chuyện gì xảy ra + hệ quả tiền/trạng thái + đúng một bước kế), register plain cho cả 5 role, cấm exclamation mark/marketing verbs. "Message human" trong AD-8 là cấu trúc envelope, không phải voice. Nếu không neo, story/QA viết message sẽ tự bịa tone — đúng rủi ro reconcile-experience #1 đã sửa ở tầng PRD, nay tái diễn ở tầng architecture. **Fix:** 1 hàng Consistency Conventions: "Error/empty/toast message theo khuôn NFR-7; nguồn chân trình bày microcopy = EXPERIENCE.md (Voice and Tone); cấm '!' trong chuỗi hiển thị".

### F-09 — NFR-2 a11y floor land một từ `[Low-Medium · mờ]`

Hàng Cross-cutting: "a11y FE", governed "AD-5…8" — nhưng không AD nào thực sự chứa rule a11y (label, focus visible, focus trap/return trong modal/drawer, keyboard Move cho kanban, click target ≥40px, không color-only). Spine tự nhận binds NFR-2 nên cần ít nhất một điểm neo. **Fix:** thêm hàng convention "A11y floor = NFR-2; mỗi màn P1 pass keyboard 1 lượt trước demo" hoặc ghi giao DESIGN/EXPERIENCE + QA checklist.

### F-10 — Endpoint public không được AD-5 nêu; FR-3 land mờ `[Low-Medium · mờ]`

AD-5 là AD sở hữu auth nhưng không phân biệt public vs protected: login, register công khai (FR-2), forgot-password (FR-3, generic response, không email) là 3 endpoint duy nhất không cần JWT — NFR-4 nói "mọi route app yêu cầu đăng nhập". FR-3 (P2) ngoài việc nằm trong binds AD-5 thì không dấu vết nào. **Fix:** AD-5 liệt kê 3 endpoint public; quên là dễ để endpoint bảo vệ sai chiều (bỏ qua JWT hoặc khóa nhầm login).

### F-11 — Access Code "SensitiveValue / reveal" (NFR-4) không chỗ `[Low · mờ]`

NFR-4: Access Code hiển thị qua cơ chế reveal (SensitiveValue). Cấp code tại FR-15/FR-25, hiển thị ở Check-in Pass/Rental Detail. Spine không nói gì về field nhạy cảm trong DTO/envelope (AD-8) hay permission đọc. **Fix:** 1 dòng ở AD-8 hoặc AD-5: giá trị nhạy cảm không trả trong response danh sách; chỉ qua endpoint reveal có permission.

### F-12 — FR-33/34: cơ chế hai kênh toast vs bell chưa ai sở hữu `[Low · mờ]`

Hàng Notifications đúng hướng (toast + bell tách bạch, pull-based, realtime deferred — hợp lý cho demo, PRD không hứa push). Nhưng hợp đồng "sự kiện tiền phát cả toast lẫn bell" + "unread persist giữa các phiên" + "bell badge đếm unread" ngầm đòi: service tạo bản ghi NOTIFICATIONS (đã có trong 22 entity) sau mutation, mutation response mang đủ thông tin để FE bắn toast, FE refetch badge. Không AD/convention nào nêu cơ chế này — để mặc cho AD-2 "contract sẽ tính". **Fix:** 1 dòng ở hàng Notifications hoặc convention: mutation thành công trả `notification` object trong response; FE toast từ response, bell từ polling query.

### F-13 — Binds minh họa thiếu FR-16/FR-36 (tiền) và delay 1.5–2s `[Low · nit]`

AD-7 binds "FR-6…9, 15, 18, 29, 30, 40, 41" — bỏ FR-16 (phí gia hạn tính theo policy — phép tính tiền) và FR-36 (receipt forfeit — biến động tiền). AD-9 nói deterministic outcome nhưng không nhắc processing cố định ~1.5–2s (NFR-5) — con số demo quan trọng để demo ổn định. Binds là minh họa nhưng đủ bộ giúp story không quên.

### F-14 — FR-5 stale re-check: chưa chốt biện pháp chống race `[Low · dưới-altitude]`

"Sự kiện tiền" bảo đảm không booking trên ngày invalid, nhưng hai khách reserve cùng unit sát nhau cần chốt optimistic lock / unique constraint / serializable transaction ở một chỗ (AD-6 tự nhiên là chỗ). Dưới altitude spine — 1 dòng là đủ.

### F-15 — Các điểm mờ nhỏ còn lại `[Info]`

(a) Nhãn hàng "Facility Admin" gộp FR-26-28 (Facility Manager) và FR-29/30/40 (Business Ops) — không sai nghiệp vụ nhưng AD-5 permission matrix phải phân hai role này đúng; nên đổi nhãn hoặc chú thích. (b) KPI drill-down là **P1** cho Business Overview + Facility Overview (FR-31/32 + §7.2 [NOTE FOR PM]) — hàng Dashboards không nêu, dễ bị story hóa thành P2. (c) Export CSV (P2) không dấu vết. (d) NFR-3 desktop 1200px light-only không chỗ (thuần FE, tác động thấp).

---

## 4. Spine quyết ĐIỀU PRD KHÔNG NÓI (không trái — chấp nhận, ghi nhận)

| Quyết của spine | PRD nói gì | Đánh giá |
|---|---|---|
| JWT Bearer, TTL 24h, không refresh token, logout = drop token (AD-5) | Im lặng | Chấp nhận cho capstone — nhưng xem F-05 về lock/đổi role |
| Notification pull-based; realtime deferred | Im lặng (chỉ hứa toast ~4s + bell bền) | Hợp lý demo; cần cơ chế 2 kênh (F-12) |
| ID Long → JSON number; enum UPPER_SNAKE string | Im lặng | OK |
| Default 25 rows cho **mọi** danh sách (AD-8) | NFR-8 chỉ bắt bảng quản trị | Mở rộng an toàn, không trái |
| TypeScript `[ASSUMPTION]`, FE component library deferred | Im lặng | Đã đánh dấu đúng cách |
| Contract đóng băng mỗi sprint + FE chống MSW (AD-2) | Im lặng (quy trình) | OK |
| Chọn stack cụ thể (SB 4.1.1/Java 21/React 19.3/Vite 8.3/MySQL 8.4.11) | OQ-1 giao architecture chốt; chỉ ràng buộc tách FE/BE + REST | Đúng thẩm quyền |

Không phát hiện chỗ nào spine **trái** PRD (không email, không payment thật, không cancel/OVERDUE, không dark mode, mock gateway duy nhất — tất cả được tôn trọng).

## 5. Xác nhận nhanh các nhóm còn lại khớp

- **FR-4…7, 36** (hàng 2): availability theo Reservation + Buffer tại query, EXPIRED một lối ra — AD-4 Prevents đúng trọng tâm ("FE tự suy EXPIRED"); FR-36 state land (chỉ side-effects là F-03).
- **FR-8…9** (hàng 3): đúng một impl `MockPaymentGateway` sau interface, FE không tự quyết kết quả — khớp NFR-5 deterministic + §6.
- **FR-10…13** (hàng 4): auto-draft, khóa policy version (Contract lưu PolicyID — erd.mmd xác nhận), chain Superseded, Addendum 4 trạng thái — khớp V3 + reconcile-design #1 (bước capture ảnh ký đã nằm trong FR-12 gốc PRD).
- **FR-14…18** (hàng 5): validate → thu 100% → settlement với charge reason bắt buộc — AD-4/7 phủ.
- **FR-19…22** (hàng 6-7): buffer guard, snap-back, state machine task — AD-4.
- **FR-23…25** (hàng 8): severity → Maintenance + Relocation (đổi UnitID trên Rental, không entity mới — khớp §9.1) — qua owner services.
- **FR-26…30, 40** (hàng 9): guards + conflict trước save + policy versioning, validation trước khi persist — AD-6/7.
- **FR-31/32** (hàng 10): read-only query service tách khỏi write path — hợp layered; drill-down/CSV xem F-15.
- **FR-35** (hàng 12): "đọc API sẵn có" đúng tinh thần pointer surface của PRD.
- **FR-37…39** (hàng 13): provisioning trong UI thay seed script (khớp quyết định 2026-09-21), LOGIN/LOGIN_FAILED trong AD-5, matrix read-only khớp NFR-4.
- **NFR-1, NFR-6**: land mạnh nhất (AD-7, AD-6) — gồm cả fix reconcile-design #2 về format `1.150.000 ₫`.
- **§6/§7.1**: không vi phạm; AD-1 đúng ràng buộc "FE/BE tách riêng + REST" của PRD.

## 6. Khuyến nghị chỉnh spine (tổng hợp theo thứ tự ưu tiên)

1. **Sửa map FR-41** (F-01): chuyển từ hàng 4 sang hàng 5; chú thích WAIVER_CAP ở hàng 9.
2. **Bind NFR-8** (F-02): sửa frontmatter + hàng Cross-cutting thành NFR-1…8; thêm hàng convention FE states/forms.
3. **AD-4 bổ sung side-effects EXPIRED** (F-03): scheduled job hay materialize-on-first-read — chốt một.
4. **AD-6 làm rõ owner sau merge V3** (F-04) + **chốt 2 quyết §9.1** (F-07) + **thêm seed demo** (F-06).
5. **AD-5**: liệt kê endpoint public (F-10); thêm kiểm tra `users.Status` mỗi request hoặc ghi tradeoff TTL (F-05).
6. **Consistency Conventions**: thêm microcopy NFR-7 (F-08), a11y NFR-2 (F-09), sensitive-field reveal (F-11), cơ chế toast-from-response/bell-from-polling (F-12).
7. **Nit**: AD-7 binds thêm FR-16/36; AD-9 nêu delay 1.5–2s; AD-6 1 dòng unique constraint chống double-booking (F-13/F-14); nhãn hàng "Facility Admin" tách 2 role + ghi drill-down là P1 (F-15).
