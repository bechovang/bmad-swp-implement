---
title: 'Story 1.1 — Monorepo starter template theo Structural Seed'
type: 'feature'
created: '2026-10-02'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'e398af61c4049ae6e3e9a7882acd66c6a38d1f21'
context:
  - '{project-root}/docs/planning/architecture/architecture-storagehub-2026-09-21/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/ERD_StateChart_Drawio/ERD_StorageHub.dbml'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Run mới chưa có gì ngoài planning docs — mọi story sau (1.2 trở đi) cần scaffold chốt stack, schema đầy đủ, seed demo tái lập được và contract skeleton để chỉ viết code nghiệp vụ vào chỗ đã chốt.

**Approach:** Dựng monorepo đúng Structural Seed tại repo root: `backend/` (Boot 4.1.1 · Java 21, Flyway V1 = schema 22 entity + V2 seed dev-only), `frontend/` (Vite 8.3 · React 19.3, proxy `/api` → `:8080`), `contracts/` (openapi.yaml + routes.yaml skeleton), `README.md` khởi chạy từ 0 + ghi nhận quyết định tuần 1.

## Boundaries & Constraints

**Always:**
- **FE scaffold dùng TypeScript** *(quyết định 2026-10-02)* —khớp contract-first, FE sinh type/MSW mock từ openapi.
- Stack pin đúng bảng §Stack trong ARCHITECTURE-SPINE, kể cả Boot 4 gotchas: starter `webmvc` (không phải `web`), test starter tách module, Jackson 3, bắt buộc `flyway-mysql`. `vn.payos:payos-java` 2.0.1 trong pom từ ngày đầu (verify compat Boot 4.1 khi init, ghi kết quả README).
- V1 = full 22-entity model V3 theo `ERD_StorageHub.dbml` + đúng 7 V1 deltas của AD-6: tiền `DECIMAL(15,0)`; `CONTRACTS` 1—N qua `SupersedesContractID` + `IsLatest`; `POLICY_RULES.RuleType` thêm `TURNOVER_BUFFER`/`DISCOUNT`/`WAIVER_CAP`; `users.FacilityID` nullable; `NOTIFICATIONS.CreatedAt DEFAULT CURRENT_TIMESTAMP`; `ESCALATIONS.TicketID UNIQUE`; `INSPECTIONS.Item` enum `ACCESS_CARD/PADLOCK/CLEANLINESS/STRUCTURE`.
- V2__seed_demo chỉ chạy profile `dev`, chứa đúng mock chuẩn: Lan/Minh/Hằng/Tuấn/Nam đủ 5 role; units S-3/M-2/M-5; BK-1042/RT-0871/SR-0032/CT-1042/CT-1042-A1; policy v3 — tái lập được từ 0, không insert tay.
- Secrets (DB/JWT/PayOS keys) qua env vars, không commit; `backend/storage/` gitignore.
- openapi.yaml skeleton: info `/api/v1` + Error envelope + List envelope schema theo AD-8 (machine code + field errors; `{items, page, pageSize, total}`, page 1-based).

**Never:**
- Không DDL tay, không hbm2ddl.
- Không implement tính năng epic 1 (auth, runtime envelope, LogService, NotificationService, UI components) — story 1.2+.
- Không sinh contract bằng springdoc — docs-only (AD-2).
- Không build shared FE component nào trước khi chốt quyết định tuần 1 (gate story 1.4).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| BE boot dev | MySQL local trống + profile `dev` + env vars | Flyway chạy V1 + V2; `/actuator/health` UP | Thiếu env → fail-fast, message nêu biến thiếu |
| BE boot non-dev | MySQL trống + profile khác `dev` | Chỉ V1 chạy; seed không chạy | N/A |
| FE dev server | `npm run dev` | Chạy + mọi request `/api/**` proxy tới `:8080` | BE chưa lên → lỗi hiện ở network tab, dev server không crash |
| Tái lập từ 0 | Drop schema + boot lại (dev) | Bộ mock chuẩn đầy đủ trở lại | N/A |

</frozen-after-approval>

## Code Map

- `docs/planning/architecture/architecture-storagehub-2026-09-21/ARCHITECTURE-SPINE.md` — AD-6 (7 deltas + ownership matrix), §Stack (version pins + Boot 4 gotchas), §Structural Seed (layout chuẩn).
- `ERD_StateChart_Drawio/ERD_StorageHub.dbml` — nguồn chân lý bảng/cột/khóa của model V3 cho V1 SQL.
- `payos ref code/payos-demo-java-spring/` — tham chiếu dựng PayOS SDK (controller, type request), đối chiếu compat Boot 4.1.
- `_bmad-output/implementation-artifacts/epic-1-context.md` — context chưng cất epic 1.
- Repo root: greenfield — chỉ docs/planning tồn tại; không sửa file hiện có ngoài README.md và .gitignore.

## Tasks & Acceptance

**Execution:**

- [x] `README.md` -- dựng hướng dẫn khởi chạy từ 0 (yêu cầu env, MySQL 8.4.11, cách chạy BE/FE) + mục "Week-1 decisions": **TypeScript** (chốt 2026-10-02), component lib + dnd + chart + QR lib = deferred, chốt trước story 1.4 -- để mọi người tái lập môi trường mà không hỏi.
- [x] `backend/pom.xml` + `backend/src/main/java/com/storagehub/StorageHubApplication.java` -- Boot 4.1.1, Java 21; starters webmvc/data-jpa/security/validation/flyway/actuator + flyway-mysql + springdoc 3.1.1 (docs-only) + payos-java 2.0.1; test starter tách module -- đúng stack pin.
- [x] `backend/src/main/resources/application.yml` (+ `application-dev.yml`/`application-prod.yml`) -- datasource/JWT/PayOS đọc env vars; Flyway locations thêm `classpath:db/seed/dev` chỉ ở dev -- mechanism seed dev-only.
- [x] `backend/src/main/resources/db/migration/V1__init_schema.sql` -- 22 bảng theo dbml + 7 deltas AD-6 -- schema đầy đủ một migration.
- [x] `backend/src/main/resources/db/seed/dev/V2__seed_demo.sql` -- mock chuẩn (5 user 5 role, S-3/M-2/M-5, BK-1042/RT-0871/SR-0032/CT-1042/CT-1042-A1, policy v3) -- tái lập demo.
- [x] `backend/src/test/java/com/storagehub/StorageHubApplicationTests.java` -- smoke: context loads (chạy với MySQL local profile dev) -- bắt lỗi cấu hình sớm.
- [x] `contracts/openapi.yaml` -- skeleton: info `/api/v1` 1.0.0, schema Error + ListEnvelope (AD-8) -- chân lý API bắt đầu từ đây (AD-2).
- [x] `contracts/routes.yaml` -- route table FE skeleton + placeholder route epic 1 (login/register/forgot-password + landing 5 role) -- FE + BE cùng đọc một bảng.
- [x] `frontend/` -- scaffold Vite 8.3 + React 19.3 (TypeScript) + deps TanStack Query, Axios, MSW 2.x, Vitest + RTL; `vite.config` proxy `/api` → `localhost:8080`; thư mục `src/pages|components|api|hooks|lib/`; `public/units/s-3.jpg`, `m-2.jpg`, `m-5.jpg` (placeholder đơn giản) -- Structural Seed FE.
- [x] `.gitignore` -- thêm `backend/storage/`, `target/`, `node_modules/`, `dist/`, `.env*` -- giữ repo sạch.

**Acceptance Criteria:**

- Given MySQL 8.4.11 local + env vars đủ, when chạy BE profile `dev`, then Flyway áp V1 + V2 và `/actuator/health` trả `{"status":"UP"}`.
- Given BE chạy profile khác `dev`, when kiểm tra `flyway_schema_history`, then chỉ có V1.
- Given FE `npm run dev` đang chạy, when gọi đường dẫn `/api/**`, then request tới được `:8080` (dù BE trả 404).
- Given scaffold xong, when `git status`, then `backend/storage/` và build artifacts không bao giờ xuất hiện.
- Given drop database rồi boot lại (dev), when seed chạy xong, then 5 user đủ 5 role, 3 unit S-3/M-2/M-5, chuỗi BK-1042/RT-0871/SR-0032/CT-1042/CT-1042-A1, policy v3 đều tồn tại.

## Implementation Notes

## Spec Change Log

## Review Triage Log

Iteration 0 — three layers (blind-hunter / edge-case-hunter / verification-gap), diff 334 kB / 7382 dòng.

- BH1 `frontend/tsconfig.app.json` + `tsconfig.node.json` thiếu `"strict": true` (template react-ts mặc định có) — **medium**: type sinh từ openapi về sau không được strict-check; verified trong diff, không có key `strict`. → patch (G4).
- BH2 README/spec hứa "thiếu biến nào thì fail-fast" nhưng chỉ DB_* thực sự fail — `app.jwt.secret`/`app.payos.*` chưa có consumer nào bind nên placeholder không bao giờ được resolve — **medium**: mechanism verified (không có @Value/@ConfigurationProperties trong backend/src); matrix cell (frozen) vẫn đúng theo cách đọc tự nhiên (mọi env thiếu → fail tại `${DB_URL}`) — sự over-promise nằm ở README. → patch (G1).
- BH3 README bảo đặt env vào `.env.local` nhưng không gì nạp `.env*` (không spring-dotenv, không spring.config.import) + thiếu `.env.example` + `.env*` trong .gitignore chặn luôn `.env.example` — **medium**: newcomer theo README vẫn crash; verified bằng grep. → patch (G1+G8).
- BH4 `/actuator/health` bị Security chặn → curl README không thể pass — **false**: edge-case-hunter đã mổ xẻ `spring-boot-security-4.1.1.jar` trong ~/.m2 chứng minh management chain permit health anonymous; và curl firsthand (không auth) trên instance đã boot trả 200 `{"status":"UP"}`. Reject.
- BH5 Prod profile trên DB đã dev sẽ fail cứng tại Flyway validate ("applied migration not resolved locally: 2"), README chỉ nói "thiếu dữ liệu"; V2 bị seed chiếm → migration kế tiếp phải từ V3 — **low**: mechanism validateOnMigrate mặc định là đúng; lỗi diễn tả symptom sai. → patch (G2).
- BH6a Matrix row "non-dev chỉ V1" không có test tự động — **medium**: đúng, chỉ verify thủ công; automation là 1 test boot default-profile lên schema mới. → patch (G3, trùng gốc với VG1).
- BH6b "backend test chưa từng chạy" — **false**: surefire firsthand sau review-run ghi `tests="1" failures="0" errors="0"`; README item 3 cũng ghi boot dev + bộ đếm seed. Reject phần claim này.
- BH6c/BH7 Không có CI pipeline — **low**: đúng (không .github); intent các planning doc không đòi CI, verification model của run là lệnh theo story. → defer (D1).
- BH6d Test backend cần MySQL + env thật nên fail trên máy trống/CI — **low**: đúng theo thiết kế (spec ghi rõ), nhưng guard skip khi thiếu env là fix nhỏ. → patch (G3).
- BH8 Ảnh unit lowercase (`s-3.jpg`) vs `units.Code` uppercase (`S-3`), V1 comment ghi `public/units/{code}.jpg` — code tương lai derive URL từ Code sẽ 404 trên hosting case-sensitive — **low**: verified trong diff. → patch (G9: rename file theo Code).
- BH9 `routes.yaml` thiếu `/notifications` (story 1.6) và `/profile` (avatar menu mọi role) — **low**: đúng, bảng tự ghi mục đích là "reserve path"; phần catch-all/404 là chuyện router FE story 1.5 — reject riêng phần catch-all. → patch (G7: thêm 2 route placeholder).
- BH10 `_bmad/render/` chứa absolute path máy-specific nhưng chưa bị gitignore — **low**: verified; là runtime output của skill, không phải sản phẩm story. → patch (G8).
- BH11 `policy_rules` không UNIQUE (PolicyID, TypeID, RuleType) — **medium** harm tiềm năng nhưng schema bị freeze = dbml + đúng 7 deltas; thêm key là delta thứ 8, intent loại trừ. → defer (D4).
- BH12 Thiếu CHECK `EndDate>=StartDate`, `NewEndDate>OldEndDate`, `SizeM2>0` — cùng lớp BH11, ngoài pinned model; layer-first đặt validation ở service. → defer (D4).
- BH13a `ListEnvelope` thiếu `additionalProperties: false` trong khi `Error` có — bất đối xứng strictness — **low**: verified. → patch (G11).
- BH13b openapi chưa có `components.responses` (Unauthorized/…) + MSW chưa có handler Error-envelope mẫu — **low**: skeleton spec chỉ yêu cầu Error+ListEnvelope; giá trị nảy ra khi path đầu tiên land. → defer (D6).
- BH14 MSW handler trả `items: []` lệch bộ demo — **low**, reject: handler tự ghi là "envelope-convention example", chưa có consumer; fix = nhồi data seed vào handler tạo nguồn sự thật thứ hai (drift).
- BH15 Node engines không được ép (`engines`, `.nvmrc` vắng) — **low**: verified. → patch (G10: thêm `engines`).
- BH16 `activity_logs` không có cột timestamp (đúng dbml) — Login History (9.2) mất trục thời gian — **medium** nhưng schema freeze; chỉ giải quyết bằng delta migration khi spec epic 9. → defer (D5).
- BH17 Không gì verify BCrypt hash trong V2 thực sự mã hoá `Demo1234!` — **low**: đúng, lần kiểm tra đầu là login story 1.3. → patch (G12: unit test matches).
- BH18 `frontend/package.json` thiếu newline cuối file — **low**, negligible. → patch (G13).
- ECH1 `main.tsx` — nếu `worker.start()` reject thì `.then` không chạy, app trắng (dev opt-in MSW) — **low**: verified chuỗi promise không có catch. → patch (G6).
- ECH2 Session timezone MySQL ≠ UTC → `DEFAULT CURRENT_TIMESTAMP` ghi local trong khi seed ghi literal UTC — **medium**: đúngrisk; Docker mysql mặc định UTC nhưng máy local VN thường +07. → patch (G5: ép connectionTimeZone=UTC qua hikari data-source-properties).
- ECH3 Test backend break máy không có DB/env — trùng BH6d. → patch (G3).
- ECH4 `.gitignore` `.env*` không khớp file kiểu `prod.env` (không dot đầu) — **low**: đúng. → patch (G8: thêm `*.env`).
- ECH5 `pageSize` không có maximum — **low**, reject: chưa endpoint nào tiêu thụ; chọn cap là quyết định contract-policy thuộc story thêm list path đầu tiên, không phải defect của scaffold.
- ECH6 Seed đụng DB đã có row → duplicate PK, boot abort — **low**, reject: trigger được nêu ("sau khi dùng prod profile") sai sự thật — prod boot để bảng trống; trigger thật cần insert tay trước lần dev boot đầu; failure loud ngay; guard TRUNCATE thêm tính phá hoại vào migration chạy-một-lần.
- VG1 Seed dev-only không có bảo vệ tự động khỏi regressed vào non-dev — **medium** (pre-verified theo evidence rules của layer; disposition filed: patch). → patch (G3).
- VG2 Proxy pairing chỉ verify thủ công — disposition filed: defer. → defer (D2).
- VG3 MSW gate không có kiểm chứng bundle — disposition filed: defer. → defer (D3).
- VG-O README `.env.local` không được nạp — trùng gốc BH3. → patch (G1).

## Design Notes

- **Seed dev-only mechanism:** dùng Flyway locations, không dùng code/callback — V2 nằm ở `db/seed/dev/`; chỉ profile dev khai báo location đó trong `spring.flyway.locations`. Đây là cách duy nhất không lòi code Java.
- **Ảnh unit placeholder:** tạo JPG đặc đơn + mã unit (script hoặc công cụ bất kỳ) — "chuẩn static asset" nghĩa là đường dẫn + kích thước ổn định cho Browse Units (epic 2), không phải ảnh thật.
- **JPA entity theo từng story:** scaffold chỉ cần schema SQL; entity class sinh khi story đầu tiên cần (1.2 → ACTIVITY_LOGS, 1.3 → USERS…) — tránh 22 entity chết chưa có caller, vẫn không drift vì V1 là chân lý.
- **PayOS compat:** demo chính thức chạy Boot 3.1.4/Java 17 — nếu conflict với Boot 4.1 tại init, giữ dependency + ghi known-issue vào README (epic 2 mới thực sự gọi SDK).

## Verification

**Commands:**

- `cd backend && ./mvnw -q -DskipTests package` -- expected: BUILD SUCCESS.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` + `curl http://localhost:8080/actuator/health` -- expected: `{"status":"UP"}`.
- `mysql -e "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank"` -- expected: dev có V1 + V2 success; non-dev chỉ V1.
- `cd frontend && npm run build` -- expected: build pass.
- `npm run dev` rồi request `/api/anything` -- expected: response từ BE (404 envelope) chứng tỏ proxy sống.

**Manual checks (if no CLI):**

- `git check-ignore backend/storage/x` trả đúng rule; README chạy được trên máy mới không có gì ngoài repo + MySQL + JDK 21 + Node.
