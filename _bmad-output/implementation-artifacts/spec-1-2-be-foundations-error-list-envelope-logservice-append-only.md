---
title: 'Story 1.2 — BE foundations: error/list envelope + LogService append-only'
type: 'feature'
created: '2026-10-02'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '83ac27d1a1bca4bd38bdd2fbb58f8afbcebd5374'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1.1 mới có scaffold — chưa có quy ước trả lỗi/phân trang (Boot Security đang nhả body mặc định), chưa có đường ghi audit; mọi story sau đều tiêu thụ hai nền này.

**Approach:** Dựng backend foundations theo AD-8 + AD-6: một Error envelope duy nhất cho mọi non-2xx (kể cả security qua `AuthenticationEntryPoint`/`AccessDeniedHandler`), ListEnvelope `{items, page, pageSize, total}` (page 1-based, default 25), và `LogService` append-only ghi `activity_logs` với registry enum Action/EntityType khai báo reason-bắt-buộc tại registry.

## Boundaries & Constraints

**Always:**
- Khớp schema đã có trong `contracts/openapi.yaml` (`Error`: code UPPER_SNAKE + message + fieldErrors chỉ khi validation; `ListEnvelope` 4 field). JSON camelCase.
- Security lỗi cũng là envelope: `SecurityConfig` tối giản permit `/actuator/health` (giữ hành vi 1.1), còn lại authenticated, stateless, CSRF off, wire entry point 401 + denied handler 403. KHÔNG dựng auth/JWT — story 1.3.
- LogService sole writer `activity_logs`, ghi đồng bộ cùng transaction caller (không after-commit, không REQUIRES_NEW); reason-required vi phạm → từ chối exception, không INSERT.
- Repository chỉ expose `save` (extends `Repository` gốc, không `JpaRepository`) — append-only tầng dữ liệu (NFR-6).
- Layer AD-3: handler ở `controller`, DTO ở `dto`, entity + enum registry ở `entity`, LogService ở `service`; entity JPA không rời backend.
- Message theo NFR-7 (chuyện gì + hệ quả + một bước kế tiếp, không exclamation); 500 không lộ stacktrace.

**Never:**
- Không đụng migration (V1 đóng băng; timestamp activity_logs đã defer D5).
- Không thêm path thật vào openapi (path đầu = auth 1.3; components.responses + MSW error-mock defer D6).
- Không xây JWT/permission matrix/NotificationService/endpoint nghiệp vụ.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Validation 400 | DTO vi phạm Bean Validation | Envelope `VALIDATION_FAILED` + fieldErrors đủ từng field | GlobalExceptionHandler |
| Méo mó 400 | JSON unreadable / thiếu param / sai kiểu | Envelope `MALFORMED_REQUEST` | GlobalExceptionHandler |
| Ẩn danh 401 | Không auth, path guarded | Envelope `UNAUTHENTICATED` + `WWW-Authenticate: Bearer` | Entry point |
| Sai vai 403 | Authenticated, thiếu role (test guard) | Envelope `FORBIDDEN` | Denied handler |
| HTTP khác | Path lạ / method sai / media type sai | Envelope `NOT_FOUND` / `METHOD_NOT_ALLOWED` / `UNSUPPORTED_MEDIA_TYPE` | GlobalExceptionHandler |
| Business 409 | `BusinessRuleException(code,msg)` | Envelope với code caller đặt | GlobalExceptionHandler |
| Bất ngờ 500 | RuntimeException | Envelope `INTERNAL_ERROR`, message chung chung, stack chỉ vào SLF4J | GlobalExceptionHandler |
| List mặc định | page/pageSize trống | `ListQuery` → page=1, pageSize=25 | — |
| List sai bound | page=0 / pageSize=0 | `ListQuery.of` ném IAE (controller sau bind @Min(1) ra 400) | fail-fast |
| Log hợp lệ | append(đủ tham số) | 1 row INSERT đúng cột | — |
| Reason bắt buộc thiếu | requiresReason + reason blank | Exception, repository KHÔNG được gọi | LogService |
| Reason optional thiếu | không requiresReason + reason null | Row INSERT, reason `""` (cột NOT NULL) | — |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- schema Error/FieldError/ListEnvelope là chân lý hình dạng (1.1 ship sẵn); 1.2 chỉ bổ description danh mục code framework.
- `backend/src/main/resources/db/migration/V1__init_schema.sql:332` -- DDL `activity_logs` (Reason NOT NULL, ActorID FK users, không cột thời gian).
- `backend/src/main/resources/application.yml` -- `ddl-auto: validate`: entity phải khớp cột V1; pom đã đủ starter (validation/security + test starters tách module).
- Test 1.1 có sẵn đều guard `@EnabledIfEnvironmentVariable(named="DB_URL")` — test mới cần DB theo pattern này.
- Package seed AD-3: `{controller, service, repository, entity, dto, security, config}` (+`exception` — lệch nhỏ, xem Design Notes).

## Tasks & Acceptance

**Execution:**

- [x] `dto/ApiError.java` + `FieldError.java` + `ApiErrorCode.java` -- record khớp schema Error (fieldErrors vắng khi trống); enum: VALIDATION_FAILED, MALFORMED_REQUEST, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE, INTERNAL_ERROR.
- [x] `dto/PageResponse.java` + `ListQuery.java` -- record `{items,page,pageSize,total}` generic + resolver default 1/25, IAE bound <1.
- [x] `exception/BusinessRuleException.java` -- code+message do caller đặt; handler map 409.
- [x] `controller/GlobalExceptionHandler.java` + `ErrorEnvelopeWriter.java` -- @RestControllerAdvice phủ các case matrix; writer tái dùng cho security handlers.
- [x] `security/EnvelopeAuthenticationEntryPoint.java` + `EnvelopeAccessDeniedHandler.java`; `config/SecurityConfig.java` -- chain như Boundaries.
- [x] `entity/ActivityLog.java` + `Action.java` + `EntityType.java` -- khớp V1 (actorId Long thường); `Action.requiresReason()`: cần reason = FIX_STATUS, WAIVER, DAMAGE_CHARGE, ESCALATION, ADDENDUM_VOID; không cần = LOGIN, LOGIN_FAILED, STATUS_CHANGE, CONTRACT_SIGNED, RELOCATION. EntityType: USER, UNIT, RESERVATION, CONTRACT, ADDENDUM, PAYMENT, SETTLEMENT, TASK, TICKET, ESCALATION, POLICY, INSPECTION.
- [x] `repository/ActivityLogRepository.java` -- extends `Repository<ActivityLog,Long>`, chỉ `save`.
- [x] `service/LogService.java` -- `append(actorId, entityType, entityId, action, fromValue, toValue, reason)` ép reason theo registry.
- [x] `contracts/openapi.yaml` -- description schema Error liệt kê danh mục code framework (business code do từng operation khai).
- [x] Test handler + security (src/test: `TestPingController` + TestSecurityConfig cho 403) -- MockMvc phủ 7 hàng lỗi của matrix.
- [x] Test list -- default 1/25, IAE bound, Jackson serial ra đúng 4 field camelCase.
- [x] Test LogService (Mockito: mapping, từ chối reason-required với verify repo không gọi, optional → "") + append-only (reflection: interface không expose delete/update) + persistence (@DataJpaTest replace=NONE, guard DB_URL: row thật, enum UPPER_SNAKE).

**Acceptance Criteria:**

- Given lỗi bất kỳ (400/401/403/404/405/409/500), when response trả về, then đúng một envelope Error khai báo trong openapi.yaml — kể cả security, không lọt body mặc định Spring.
- Given DTO vi phạm Bean Validation, when handler bắt, then envelope kèm đầy đủ field errors.
- Given danh sách, when endpoint trả về, then envelope `{items, page, pageSize, total}` page 1-based default 25 (ListQuery + serialization test).
- Given LogService với registry + reason khai báo tại registry, when ghi hợp lệ, then INSERT đúng (actor, entity, action, from→to, reason); repository không expose update/delete.
- Given action cần reason mà caller thiếu, when append, then từ chối exception, không row nào INSERT.
- Given `mvn test` với DB env, when chạy, then PASS toàn bộ (envelope từng loại lỗi, pagination 1-based, append-only + reason) + không regression 3 test 1.1.

## Implementation Notes

- Verify firsthand (không theo report subagent), chạy lại sau vòng patch review: `mvn test` 48/48 PASS, 0 skipped (15 handler + 1 access-denied + 6 ListQuery + 3 serialization + 9 LogService + 2 append-only + 2 BusinessRuleException + 2 column-mapping + 3 persistence DB + 6 boot smoke/nondev/seed 1.1); boot dev + curl `/api/v1/anything` → 401 JSON envelope `UNAUTHENTICATED` + `WWW-Authenticate: Bearer`, `/actuator/health` 200 public — cùng hai assertion này giờ còn nằm trong `StorageHubApplicationTests` (RANDOM_PORT, thật HTTP).
- Vòng patch step-04 (tự áp — subagent implement không còn continuable): 6 nhóm — handler `HandlerMethodValidationException`→400 VALIDATION_FAILED (F1), guard VARCHAR(255) trong LogService (F4), regex UPPER_SNAKE trong ctor BusinessRuleException (F7), test HTTP thật health-public + 401-envelope (F10), test luôn-chạy mapping cột ActivityLog↔V1 + guard dòng naming-strategy yml (F26), document `DB_URL_LOGTEST`/`DB_URL_NONDEV` ở README (F9).
- Quirk Boot 4 gặp khi patch: `TestRestTemplate` dọn về module `spring-boot-resttestclient` (cần thêm cả `spring-boot-restclient` test-dep cho `RestTemplateBuilder`) + bắt buộc `@AutoConfigureTestRestTemplate`; Spring 7 API là `getParameterValidationResults()` (không phải `getAllValidationResults()`); build Hibernate metadata không-DB cần `hibernate.dialect` tường minh.
- Chạy trên MySQL local 9.2.0 (Docker daemon máy này đang down; story không đổi SQL so với 1.1 — 8.4.11 đã verify ở 1.1).
- `application.yml`: thêm `spring.jpa.hibernate.naming.physical-strategy=PhysicalNamingStrategyStandardImpl` — Boot 4 mặc định snake_case hoá tên cột, vỡ `ddl-auto: validate` với DDL PascalCase đóng băng. Entity mới từ 1.3 trở đi BẮT BUỘC `@Column(name=...)` tường minh.
- Local dev DB `storagehub` bị drop/recreate trong run này (Flyway checksum stale từ trước, pre-existing) — seed demo tái lập V1+V2 sạch.
- Latent risk cho 1.3: `@ExceptionHandler(Exception.class)` sẽ nuốt `AccessDeniedException` sinh từ method security (`@PreAuthorize`) → 500 thay vì 403. Khi 1.3 thêm permission matrix: thêm handler rethrow `AccessDeniedException` (để filter chain/denied handler xử lý) hoặc chỉ authorize ở `authorizeHttpRequests`. (Cũng ghi tại deferred-work.md.)
- Latent cho 1.3: `LOGIN_FAILED` với email không tồn tại không có `ActorID` hợp lệ (cột NOT NULL + FK users) — story 1.3 phải chốt cách ghi (vd. không log actor ẩn danh, hoặc nới FK — đụng schema thì phải renegotiate).
- Subagent implement bị gián đoạn giữa chừng (host restart) — resume qua message, không mất tiến độ; mọi file đã trên đĩa từ trước khi gián đoạn.
- Chạy trên MySQL local 9.2.0 (Docker daemon máy này đang down; story không đổi SQL so với 1.1 — 8.4.11 đã verify ở 1.1).
- `application.yml`: thêm `spring.jpa.hibernate.naming.physical-strategy=PhysicalNamingStrategyStandardImpl` — Boot 4 mặc định snake_case hoá tên cột, vỡ `ddl-auto: validate` với DDL PascalCase đóng băng. Entity mới từ 1.3 trở đi BẮT BUỘC `@Column(name=...)` tường minh.
- Local dev DB `storagehub` bị drop/recreate trong run này (Flyway checksum stale từ trước, pre-existing) — seed demo tái lập V1+V2 sạch.
- Latent risk cho 1.3: `@ExceptionHandler(Exception.class)` sẽ nuốt `AccessDeniedException` sinh từ method security (`@PreAuthorize`) → 500 thay vì 403. Khi 1.3 thêm permission matrix: thêm handler rethrow `AccessDeniedException` (để filter chain/denied handler xử lý) hoặc chỉ authorize ở `authorizeHttpRequests`.
- Latent cho 1.3: `LOGIN_FAILED` với email không tồn tại không có `ActorID` hợp lệ (cột NOT NULL + FK users) — story 1.3 phải chốt cách ghi (vd. không log actor ẩn danh, hoặc nới FK — đụng schema thì phải renegotiate).
- Subagent implement bị gián đoạn giữa chừng (host restart) — resume qua message, không mất tiến độ; mọi file đã trên đĩa từ trước khi gián đoạn.

## Spec Change Log

## Review Triage Log

- F1 (blind) `GlobalExceptionHandler` không map `HandlerMethodValidationException` — method validation Built-in của Spring 7 trên param handler (đúng đường `@RequestParam @Min(1)` mà matrix frozen + javadoc ListQuery hướng controller tương lai) rơi vào catch-all → 500 thay vì 400 như matrix hứa. Đã kiểm: không handler nào bắt nó; chưa có endpoint nào dùng param constraint hôm nay nhưng guidance ships trong diff này chắc chắn bị theo sau. **medium → patch**.
- F2 (blind + vg-other) catch-all sẽ nuốt `AccessDeniedException` từ method security (500 thay 403) khi 1.3 thêm `@PreAuthorize`; `EnvelopeAccessDeniedHandler` cũng không thể trigger qua chain thật hôm nay (chain không có role rule). Đã kiểm; spec Implementation Notes đã ghi, không có production path nào chạm hôm nay. **medium → defer** (fix thuộc 1.3 permission matrix, nơi nó test được).
- F3 (blind + edge) không chặn trên pageSize ở bất kỳ đâu (ListQuery chỉ ≥1; openapi không `maximum`): `pageSize=10000000` được chấp nhận; LIMIT không bounded + tràn int `(page-1)*pageSize` xuất hiện cùng endpoint list thật đầu tiên. Đã kiểm; chưa có consumer SQL. **medium → defer** (giá trị cap + `maximum` contract là quyết định của story list đầu tiên).
- F4 (blind + edge) LogService không ép VARCHAR(255) của Reason/FromValue/ToValue (V1:332-340): reason 300 ký tự qua registry check rồi chết ở INSERT data-too-long → 500 cho input của caller. Đã kiểm DDL; justification do người dùng nhập sẽ vượt 255 khi caller 1.3+ landed. **medium → patch** (fail-fast length check trước repo, cùng style reason rule).
- F5 (blind + edge) LogService null audit keys (action/entityType/actorId/entityId) → NPE/DB-constraint 500. Đã kiểm — lỗi lập trình, fail loud, transaction còn nguyên: hành vi đúng, không phải harm everyday. **low → reject**.
- F6 (blind) IAE reason-required ra HTTP thành 500 không được document. Đã kiểm; 500 generic là fail-loud chấp nhận được cho vi phạm registry (matrix đã ghi "fail-fast"), chỉ là nicety docs. **low → reject**.
- F7 (blind + edge) `BusinessRuleException` nhận code string bất kỳ → code không UPPER_SNAKE lọt ra envelope 409 vi phạm pattern openapi. Đã kiểm: ctor không validate, javadoc đã tự hứa pattern. **medium → patch** (regex fail-fast trong ctor).
- F8 (blind) không test đồng bộ danh mục Error.code (openapi) ↔ `ApiErrorCode`. Đã kiểm: hôm nay đang khớp (8/8); drift là rủi ro bảo trì tương lai, fix = thêm test class parse YAML. **low → reject** (hardening, không phải defect).
- F9 (blind + edge + vg-other) `LogServicePersistenceTests` guard theo `DB_URL` nhưng URL đọc `DB_URL_LOGTEST` mặc định localhost — biến enable ≠ biến route; `DB_URL_LOGTEST` không được document ở đâu. Đã kiểm annotation/property. **low → patch** (thêm 1 dòng document `DB_URL_LOGTEST` ở README; derive từ DB_URL sẽ chỉ test vào dev DB có seed — sai thiết kế schema riêng).
- F10 (blind + vg-gap) `/actuator/health` public không có test tự động (chỉ curl thủ công); xóa dòng permitAll vẫn xanh toàn suite. Pre-verified của verification-gap theo evidence rules, disposition patch. **medium → patch** (mở rộng StorageHubApplicationTests: RANDOM_PORT + TestRestTemplate — health nặc danh 200 + `/api/v1/anything` 401 envelope + `WWW-Authenticate`).
- F11 (blind + edge) 406 `HttpMediaTypeNotAcceptableException` + binding thiếu header/cookie + async timeout không map → 500/không envelope. Đã kiểm: không handler, các case này rơi catch-all; không endpoint hiện tại đọc header hay yêu cầu Accept lạ; matrix frozen liệt kê 404/405/415. **medium → defer** (mở rộng catalog framework khi endpoint thật cần — enum mới + sửa contract).
- F12 (blind) spec `in-review` vs sprint-status `in-progress` mâu thuẫn. **false → reject**: đó là trạng thái trung gian của chính workflow — step-03 đặt sprint-status, step-04 đặt spec status, step-05 sync sprint-status về review.
- F13 (blind) resolver MATME mang raw của `page` + tên "page/pageSize" khi `pageSize` là đứa sai. Đã kiểm; envelope như nhau mọi nhánh (malformed không có fieldErrors) và IAE nêu đúng param sống trong cause chain — giá trị sai không tới mắt ai. **low → reject**.
- F14 (blind) entry point/denied handler không log dòng nào. Đã kiểm; 401 sẽ tạo noise cho mọi probe nặc danh, ops trail HTTP là việc access log — audit của story này là `activity_logs` (AD-6). **low → reject**.
- F15 (blind) openapi Error.code examples "chỉ có VALIDATION_FAILED". **false → reject**: schema có 3 examples (VALIDATION_FAILED, UNIT_UNAVAILABLE, BLOCKED_TRANSITION) tại contracts/openapi.yaml:79-82 — reviewer đọc diff hunk bị cắt.
- F16 (blind) permit exact `/actuator/health` bỏ sót subpath health-group/dấu gạch chéo → 401. Đã kiểm; không health group tồn tại, intent frozen "tối giản permit /actuator/health (giữ hành vi 1.1)"; nới `/**` mở surface cho thứ không tồn tại. **low → reject**.
- F17 (edge) Accept không含 JSON → 406 unmapped. Trùng F11. **medium → defer** (gộp F11).
- F18 (edge) vi phạm class-level constraint → VALIDATION_FAILED không fieldErrors và message của constraint bị rơi (chỉ đọc getFieldErrors). Đã kiểm; chưa có class-level constraint nào hôm nay nhưng DTO cross-field (khoảng ngày) là chắc chắn ở 2.x; biểu diễn error không field là quyết định shape spec không chốt. **medium → defer**.
- F19 (edge) `BusinessRuleException` code null/sai pattern. Trùng F7. **medium → patch** (gộp F7).
- F20 (edge) LogService null keys → 500. Trùng F5. **low → reject**.
- F21 (edge) reason/from/to >255 chết ở INSERT. Trùng F4. **medium → patch** (gộp F4).
- F22 (edge) page/pageSize cực đại → LIMIT không bounded. Trùng F3. **medium → defer** (gộp F3).
- F23 (edge) preflight CORS OPTIONS trên path guarded → 401 không có header CORS. **false → reject**: deployment model same-origin by design (openapi servers: "/api/v1 — Same-origin (Vite dev proxy / SPA-served deployment)"); client cross-origin nằm ngoài intent.
- F24 (edge) `DB_URL` trỏ server khác trong khi `DB_URL_LOGTEST` chưa set → test vẫn chạy localhost. Trùng F9. **low → patch** (gộp F9).
- F25 (edge) `PageResponse.of(null items)` NPE → 500. Đã kiểm; lỗi lập trình fail loud, mọi caller truyền list. **low → reject**.
- F26 (vg-gap) mapping entity↔cột V1 + naming-strategy chỉ được verify bởi test guard `DB_URL` tự skip khi thiếu env; xóa dòng yml physical-strategy vẫn xanh `mvn test` thường trong khi app unbootable (validate fail). Pre-verified, disposition patch. **medium → patch** (test luôn-chạy không DB: physical column names của ActivityLog dưới StandardImpl == tên cột V1 + guard sự hiện diện của dòng cấu hình).
- F27 (vg-other) endpoint springdoc (/v3/api-docs, /swagger-ui) thành 401 tới khi 1.3 có JWT. **false → reject**: hệ quả có chủ đích của quy tắc frozen "còn lại authenticated"; contract file vẫn là tài liệu.
- F28 (vg-other) `EnvelopeAccessDeniedHandler` không verify được qua chain thật. Cùng root cause F2. **medium → defer** (gộp F2).

## Design Notes

- Không cần endpoint thật: `TestPingController` (src/test, `/api/v1/__test/**`) ném đủ loại lỗi; 403 dùng test-only `.hasRole("TESTER")` — role thật thuộc matrix 1.3.
- Reason NOT NULL ở DB: action không cần reason → lưu `""`; registry quyết định "meaningful reason", DB chỉ cần giá trị.
- `ActivityLog.actorId` giữ Long (User entity chưa tồn tại — note "entity theo từng story" của 1.1); FK vẫn đúng tầng DB.
- `exception` package ngoài seed list: seed liệt kê package lõi, không phải whitelist; ràng buộc thật là layering.
- BusinessRuleException là contract service→HTTP; code caller đặt (UNIT_UNAVAILABLE, SHIFT_CONFLICT…) khớp pattern openapi `^[A-Z][A-Z0-9_]*$`.

## Verification

**Commands:**

- `cd backend && mvn test` (DB env như README) -- expected: PASS toàn bộ, không regression.
- Boot + `curl -i http://localhost:8080/api/v1/anything` -- expected: 401, JSON envelope `UNAUTHENTICATED`, header `WWW-Authenticate`.
