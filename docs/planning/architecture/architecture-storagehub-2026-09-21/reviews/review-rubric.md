---
reviewer: rubric-walker (Reviewer Gate)
date: 2026-09-22
target: ARCHITECTURE-SPINE.md (updated 2026-09-22, status: draft)
rubric: 6 mục — diverge points · AD enforceability · Deferred an toàn · tech verified-current · Capability Map đủ · chiều im lặng (operational envelope)
verdict: PASS WITH CONDITIONS
---

# Rubric Review — Architecture Spine StorageHub

Kết luận nhanh: spine đúng bệnh cho bối cảnh 2 BE + 2 FE code song song 8 tuần — AD-2 (contract-first + MSW) là khớp xương cho 2 track, AD-3/AD-6 giữ 2 BE cùng một style, AD-4 chặn đúng lớp lỗi nguy hiểm nhất (FE tự suy trạng thái). Có 2 điều kiện phải xử lý trước khi freeze (seed data, entity-owner registry); các mục còn lại là note nhỏ.

---

## Mục 1 — Diverge points thật cho epic/story (2 track BE/FE độc lập)

### Diverge đã chặn đúng

| Diverge giữa 2 BE / 2 FE | Chặn bởi |
| --- | --- |
| Chọn stack lệch nhau, trượt JSP | AD-1 |
| BE implement lệch kỳ vọng FE, big-bang cuối kỳ | AD-2 (contract đóng băng mỗi sprint, MSW sinh từ contract, demo E2E sprint) |
| 2 style tổ chức code backend | AD-3 + Naming conventions (`XxxController/Service/Repository` theo feature) |
| FE tự tính EXPIRED/buffer từ ngày | AD-4 (on-read idempotent, FE hiển thị nguyên văn) |
| Enforce quyền ở FE (trust-the-client) | AD-5 |
| Schema drift giữa 2 máy BE | AD-6 (Flyway V1, cấm DDL tay, cấm hbm2ddl) |
| Tiền/múi giờ lệch FE-BE | AD-7 |
| Parse lỗi/list mỗi endpoint một kiểu | AD-8 |
| Mock payment rải khắp nơi | AD-9 (1 interface, 1 impl deterministic) |

Nhận định: bộ AD chọn đúng các mặt cắt mà 2 người làm song song dễ lệch nhất. Không có diverge cấp paradigm nào bị bỏ sót.

### Diverge còn hở

- **F1 — MEDIUM-HIGH · Entity-owner registry thiếu.** AD-6 tuyên bố "mỗi entity có đúng một owner service được ghi" nhưng không có bảng entity → owner. Bằng chứng nội tại trong chính spine: hàng "Cleaning & Unit guards" (FR-19/20) ghi `UnitService`, hàng "Facility Admin" (FR-26…30, 40) ghi `UnitMgmt*` — hai họ service cùng ngả về entity Unit. 2 BE hoàn toàn có thể cùng viết vào Unit mà không ai vi phạm chữ nào trong spine. Fix 5 dòng: thêm bảng (hoặc cột owner) 22 entity → service, và thống nhất tên một service Unit duy nhất trong Capability Map.
- **F2 — HIGH · Seed/demo data không ai sở hữu.** Demo E2E mỗi sprint trên API thật + demo cuối kỳ 5 role/29 màn cần dataset tái lập được (user 5 role, chuỗi hợp đồng phụ lục EXPIRED/VOIDED, payment, task, ticket, notification, activity log). Không có convention nào nói seed thuộc Flyway (V2__seed / afterMigrate), data.sql, hay script riêng → 2 BE sẽ diverge (một người seed bằng migration, một người insert tay), DB dev hai máy khác nhau, FE không có trạng thái ổn định để build màn hình. Đây là cả diverge BE/BE lẫn điểm chặn BE/FE. Nên mở rộng AD-6 thêm một câu.
- **F3 — MEDIUM · TypeScript deferred không có deadline; FE test stack bỏ trống.** Component library có bound "F1 chốt tuần 1" nhưng TypeScript thì không — nếu không gán tuần 1, 2 FE có thể trộn .ts/.jsx ngay sprint đầu. Bất đối xứng: BE pin JUnit 5 + Mockito + AssertJ, FE không có dòng nào về test (Vitest? Testing Library?) trong Stack lẫn Deferred.
- **F4 — LOW · Convention nhỏ thiếu.** (a) JSON field casing (camelCase?) không ghi — contract hấp thụ được nhưng thêm 1 dòng giảm churn; (b) cú pháp query param filter/sort không có (AD-8 chỉ chặn shape response); (c) AD-5 nói "permission matrix Role × Permission cố định" nhưng không chỉ định artifact chứa nó (code? phụ lục spine? PRD?) — 2 BE enforce "từng endpoint" cần một bản để soi.

## Mục 2 — Enforceability từng AD

| AD | Prevents khai báo | Check được khi review? | Phán |
| --- | --- | --- | --- |
| AD-1 | Stack lệch, JSP | Có — đọc pom/build.gradle, package.json, отсутствие template engine | Đạt |
| AD-2 | BE lệch FE, big-bang | Có — PR chạm contract trước code; MSW sinh từ yaml; E2E sprint chạy API thật | Đạt — AD mạnh nhất spine |
| AD-3 | Logic rải rác, 2 style | Có — controller không import repository; response chỉ là DTO | Đạt |
| AD-4 | 2 nguồn sự thật trạng thái | Có — grep FE không có date-diff → status; transition chỉ nằm trong service của owner | Đạt — chọn "on-read idempotent" còn triệt bỏ luôn diverge scheduler/DB-state |
| AD-5 | Trust-the-client | Có — mỗi endpoint có check role server-side; BCrypt; TTL; ActivityLog LOGIN | Đạt, trừ artifact matrix (F4c) |
| AD-6 | Schema drift, 2 module ghi 1 entity | Flyway + append-only: có. Single-owner: KHÔNG check được khi chưa có registry (F1) | Đạt có điều kiện |
| AD-7 | Precision/timezone drift | Có — grep float/double cho tiền; DECIMAL(15,0); lưu UTC; FE format VN | Đạt — dễ enforce nhất |
| AD-8 | Parse lỗi/list mỗi kiểu | Có — envelope định nghĩa trong openapi.yaml; default 25 | Đạt |
| AD-9 | Mock payment rải rác | Có — chỉ 1 impl; FE không quyết kết quả | Đạt |

Tension cần dứt khoát: **springdoc-openapi sinh spec từ code, còn AD-2 tuyên bố `contracts/openapi.yaml` là nguồn chân lý duy nhất.** Nếu ai đó dùng springdoc generate rồi coi bản generate là contract, spine có 2 nguồn sự thật. Nên ghi rõ: springdoc chỉ render docs (nếu dùng), cấm dùng làm nguồn sinh contract.

## Mục 3 — Deferred có an toàn không?

| Item | An toàn? | Ghi chú |
| --- | --- | --- |
| OQ-2 deploy demo (jar nhúng static/ vs docker-compose) | Có | Cả 2 nhánh đều same-origin (không cần CORS), không đổi AD nào. Note nhỏ: cả 2 nhánh đều cần SPA fallback `/* → index.html` — đáng thêm 1 dòng để không ai quên. Deadline tuần 6–8 hợp lý vì không chặn dev hằng ngày (proxy Vite đã định nghĩa). |
| FE component library | Có điều kiện | Bound tuần 1 là đủ sớm, nhưng phải bảo đảm không có story FE dựng shared component nào chạy trước lúc chốt. |
| TypeScript | CHƯA | Không có deadline (khác component library) → rủi ro trộn .ts/.jsx giữa 2 FE. Gán: chốt tuần 1, cùng buổi với library. |
| Realtime notifications | Có | Baseline pull-based đã được quyết (nằm trong Capability Map); defer chỉ là nâng cấp. Đúng cách defer: quyết baseline, defer phần mở rộng. |
| CI/CD chi tiết | Có | Bound "khi dựng repo" — hẹp, không cho 2 unit diverge trong chờ đợi (monorepo đã định nghĩa trong Structural Seed). |
| Test strategy & E2E tool | Nửa | BE đã pin unit stack nên nửa BE an toàn; nửa FE hở (F3). |
| Sprint/story breakdown | Đúng | Thuộc artifact khác, không phải việc spine. |

## Mục 4 — Tech verified-current & rủi ro springdoc

- Phần lớn stack ghi "version verify web 2026-09-21" — chấp nhận theo marker; các version nội tại nhất quán và đều là dòng LTS/ổn định phù hợp đội sinh viên (Java 21 LTS, SB 4.1.1, MySQL 8.4 LTS, React 19.3, Vite 8.3). "current" cho TanStack Query/Axios/MSW hơi lỏng nhưng chấp nhận được ở altitude này.
- **springdoc-openapi "tương thích SB 4.x — verify khi init repo": rủi ro MEDIUM.** Lý do: (1) live-verify hôm nay không thực hiện được (web search rate-limited), nên mục này là dòng duy nhất trong Stack chưa có bằng cứ; (2) lịch sử springdoc luôn trễ major Spring Boot một khoảng đáng kể (dòng 2.x chỉ target SB 3.x; SB 4/Framework 7 cần major line mới) — khả năng cao lúc dựng repo phải đổi version vài lần hoặc dùng bản chưa ổn định. **Tác động kiến trúc LOW nếu nêu sẵn fallback**: vì AD-2 là contract-first viết tay, springdoc chỉ mang vai docs — nếu không tương thích, serve Swagger UI trực tiếp từ `contracts/openapi.yaml` (swagger-ui standalone/webjar), không thay AD nào. Đề xuất: thêm 1 dòng fallback vào Stack + câu "springdoc docs-only, cấm sinh contract" (khớp finding ở Mục 2).

## Mục 5 — Capability Map có phủ đủ spec không (tự đánh giá từ bảng)

- Đủ theo đếm: các dải FR ghép lại phủ liền FR-1→41 không trống ô nào (1–3, 4–7+36, 8–9, 10–13+41, 14–18, 19–20, 21–22, 23–25, 26–30+40, 31–32, 33–34, 35, 37–39); NFR-1…7 có hàng cross-cutting. Mỗi hàng đều có đủ "Lives in" + "Governed by" → chiều trace FR/NFR → vị trí code → AD quản đạt chuẩn.
- Chú ý nhỏ: (a) NFR gom một hàng duy nhất — mỏng nhưng hợp lý ở altitude feature; (b) FR-38 (activity log) xuất hiện cả ở AD-5 lẫn hàng SysAdmin — là cross-reference, không phải lỗi; (c) hàng SysAdmin ghi FR-37…39 và ghi chú mockup Batch 9 — nhất quán với bối cảnh 2 màn SYS chưa có mockup; (d) trùng lặp owner Unit giữa hai hàng là lỗi thật duy nhất tìm thấy (F1).

## Mục 6 — Chiều im lặng (đặc biệt operational envelope)

Đã có: môi trường dev (Vite proxy `/api` → :8080, MySQL local), profile `dev`/`prod`, demo = OQ-2.

Rơi khỏi spine:

| Khía cạnh | Phán | Đề xuất |
| --- | --- | --- |
| **Seed data demo** | Rơi thật, hại nhất (F2) — diverge BE/BE + chặn FE cần trạng thái ổn định | Thêm vào AD-6: seed = Flyway migration riêng, cấp quyền cho 5 role, tái lập được từ 0 |
| **Secrets/config** | JWT secret + DB credentials không có convention (application.yml có profile nhưng không nói secrets) — 2 BE hardcode lệch nhau, nguy cơ commit secret | 1 dòng: secrets qua env vars, không commit |
| **Forgot-password delivery (FR-3)** | FE có trang Forgot nhưng không có quyết định kênh phát reset (email thật? mock? admin reset?) — nếu cần email thì infra email hoàn toàn im lặng | 1 câu quyết: mock/inbox hoặc admin-reset |
| **Log vận hành vs ActivityLog** | Không có ranh giới: log kỹ thuật (level, format) thuộc BE nào viết, cái gì vào ActivityLog nghiệp vụ | 1 dòng convention |
| **Health/smoke (actuator)** | Không named trong stack — demo sẽ cần ít nhất 1 health check | Nhẹ: thêm actuator hoặc 1 endpoint ping |
| **Backup/restore** | Im lặng — với capstone 8 tuần chấp nhận bỏ được, nhưng nên ghi "conscious ignore" 1 dòng để khỏi bị hiểu là quên | Ghi chú bỏ qua có chủ đích |
| **Monitoring** | Idem backup | Idem |

 scheduler jobs: không rơi — AD-4 chọn on-read idempotent nên không cần scheduler, đây là quyết định giỏi. CORS: cả 2 nhánh OQ-2 đều same-origin nên không cần — im lặng ở đây là an toàn.

---

## Tổng hợp findings (xếp theo severity)

1. **HIGH — Seed/demo data không có chủ sở hữu hay convention** (Mục 1 F2, Mục 6): diverge BE/BE (Flyway vs insert tay) và làm FE mất trạng thái ổn định để build/demo. Fix ~2 câu mở rộng AD-6.
2. **MEDIUM-HIGH — AD-6 thiếu entity-owner registry; Capability Map còn mâu thuẫn tên `UnitService` vs `UnitMgmt*`** (Mục 1 F1): rule single-owner không enforce được khi chưa có bảng đối chiếu. Fix: bảng 22 entity → owner service, thống nhất tên trong map.
3. **MEDIUM — TypeScript deferred thiếu deadline; FE test stack bỏ trống** (Mục 1 F3, Mục 3): rủi ro trộn .ts/.jsx và 2 FE lệch kiểu test. Fix: chốt tuần 1 cùng component library; thêm 1 dòng FE test vào Stack hoặc Deferred có bound.
4. **MEDIUM — springdoc-openapi chưa verify + nguy cơ dual-truth với AD-2** (Mục 2, Mục 4): cần câu "docs-only, cấm sinh contract" + fallback Swagger UI từ yaml nếu không tương thích SB 4.x.
5. **LOW-MEDIUM — Operational envelope còn rơi: secrets convention, kênh forgot-password, ranh giới log vận hành vs ActivityLog, health endpoint** (Mục 6): mỗi mục 1 dòng là đủ; backup/monitoring nên ghi rõ là bỏ qua có chủ đích.
6. **LOW — Convention nhỏ: JSON casing, filter/sort param, artifact permission matrix** (Mục 1 F4): contract hấp thụ phần lớn, thêm để giảm churn.

## Verdict

**PASS WITH CONDITIONS.** Spine đủ chắc để làm nền cho epic/story breakdown: đúng các diverge lớn nhất của mô hình 2 track, AD hầu hết enforce được khi review, map phủ kín 41 FR + 7 NFR. Điều kiện trước khi chuyển `status: final`: xử lý finding 1 (seed data) và 2 (entity-owner registry) — cả hai chỉ tốn vài dòng; finding 3–5 nên lấy luôn trong cùng một lần sửa.
