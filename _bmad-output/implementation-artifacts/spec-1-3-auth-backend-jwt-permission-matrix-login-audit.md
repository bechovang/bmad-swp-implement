---
title: 'Story 1.3 — Auth backend: JWT + permission matrix + LOGIN audit'
type: 'feature'
created: '2026-10-02'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '7b1e9b130427bec12a42a2e21d258c8d34c35b65'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Chưa có đăng nhập/đăng ký thật — mọi path ngoài `/actuator/health` trả 401; không có JWT, không phân quyền server-side, chưa có audit đăng nhập.

**Approach:** Theo AD-5: 3 endpoint public (`POST /api/v1/auth/login|register|forgot-password`) trả JWT HS256 (role claim, TTL 24h, không refresh), register tự-tạo CUSTOMER (BCrypt), filter per-request re-check `users.Status` (cache ngắn), ma trận quyền cố định trong code enforce từng endpoint, LOGIN/LOGIN_FAILED ghi qua `LogService` 1.2.

## Boundaries & Constraints

**Always:**
- Contract-first (AD-2): openapi.yaml nhận 3 path auth + schemas + `components.responses` (Unauthorized/Forbidden/ValidationError — defer D6 trả về story này) TRƯỚC khi code; mọi non-2xx vẫn là envelope AD-8 sẵn có.
- JWT HS256; secret `${JWT_SECRET}` (≥32 ký tự) fail-fast từ story này; claims chỉ `sub`=UserID, `role`=UPPER_SNAKE, `iat`, `exp`; TTL từ `app.jwt.ttl-hours` (24); logout = client drop token.
- BCrypt strength 10 (khớp hash seed `$2a$10$`); register: API không có field role, luôn tạo CUSTOMER Status=1; email trùng → 400 `VALIDATION_FAILED` + fieldErrors[email].
- Đúng 3 public path trên + `/actuator/health`; còn lại authenticated; sai/kém token → 401 envelope, thiếu role → 403 envelope — kể cả từ method security (`AccessDeniedException` không được rơi vào catch-all 500; fix latent 1.2).
- Permission matrix cố định trong code: map 5 role (roles.Name Title Case ↔ `RoleName` UPPER_SNAKE đúng seed V2) → authorities `ROLE_<ROLE>`; `@EnableMethodSecurity`; Login response + claim dùng UPPER_SNAKE (khớp routes.yaml cho FE 1.5).
- Mọi request đã auth: filter parse token → load user → check `Status`=1 (cache in-memory ~30s); login luôn đọc DB tươi.
- Entity mới (`User`, `Role`) bắt buộc `@Column(name=...)` PascalCase verbatim (rule naming-strategy 1.2); `users.Status` TINYINT map enum qua converter (0=inactive,1=active,2=locked).
- LOGIN/LOGIN_FAILED qua `LogService.append` (registry sẵn, EntityType USER), đồng bộ transaction; message sai credential là MỘT chuỗi chung cho mọi nhánh thất bại (không tiết lộ field nào sai, không tiết lộ email tồn tại). Mọi thất bại đều được ghi: email lạ → ActorID NULL (Reason chứa email đã thử) — quyết định Q1=A, duyệt tại checkpoint.

**Never:**
- Không refresh token, không logout endpoint, không gửi email thật (forgot-password P2 luôn trả generic "If an account exists…").
- Không đụng V1/V2; migration duy nhất được phép là V3 thành NULL `activity_logs.ActorID` (Q1=A).
- Không thêm endpoint nghiệp vụ hay permission key mịn thuộc story 2.x+ (scope Staff-theo-Zone/FM-theo-cơ sở đến cùng endpoint của nó); không đổi seed V2, không đụng PayOS.
- Secret/token không hardcode; token không mang dữ liệu nhạy ngoài claims kể trên.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Login đúng | active + email/password khớp (vd. lan@storagehub.dev) | 200 `{token, user{id,fullName,email,role,phone}}` + LOGIN row (ActorID=user) | — |
| Sai password | user có thật, sai hash | 401 envelope UNAUTHENTICATED, message chung duy nhất + LOGIN_FAILED (ActorID=user) | Entry point |
| Email không tồn tại | không khớp user nào | 401 cùng message chung + LOGIN_FAILED (ActorID NULL, Reason chứa email đã thử) | Entry point |
| Status 0/2 | inactive/locked | 401 cùng message chung + LOGIN_FAILED (ActorID=user) | Entry point |
| Thiếu/garbage token | path guarded | 401 envelope + `WWW-Authenticate: Bearer` (giữ hành vi 1.2) | Entry point |
| Token còn hạn, user mới bị lock | lock sau issue | 401 ngay qua filter (staleness ≤30s), không đợi TTL | Filter |
| Register hợp lệ | đủ trường, đồng ý điều khoản | 201 user CUSTOMER Status=1 BCrypt; field `role` trong JSON bị bỏ qua | — |
| Email trùng | uk_users_email | 400 VALIDATION_FAILED + fieldErrors[email] | GlobalExceptionHandler |
| Cross-field sai | password≠confirm / agreeToTerms=false | 400 VALIDATION_FAILED + fieldErrors tương ứng | Service raise |
| Forgot-password | bất kỳ email | 200 generic "If an account exists…", không phân biệt tồn tại | — |
| Thiếu role trên endpoint bảo vệ | token role khác @PreAuthorize | 403 envelope FORBIDDEN (không bị nuốt thành 500) | Advice map AccessDeniedException |

</frozen-after-approval>

## Code Map

- `backend/src/main/resources/db/migration/V1__init_schema.sql:49` -- DDL users (UNIQUE Email, Status TINYINT 0/1/2, FacilityID NULL [D4]); `:22` roles (UNIQUE Name).
- `backend/src/main/resources/db/seed/dev/V2__seed_demo.sql` -- 5 role Title Case (1=Customer…5=System Administrator) + 5 user Status=1 password `Demo1234!` ($2a$10$).
- `backend/src/main/resources/application.yml` -- `app.jwt.secret/ttl-hours` sẵn (chưa có code đọc); `${JWT_SECRET}` placeholder fail-fast sẵn sàng.
- `backend/pom.xml` -- CHƯA có thư viện JWT: thêm jjwt (api/impl/jackson 0.13.x, impl+jackson runtime); BCrypt đã có qua starter-security.
- `contracts/openapi.yaml` -- `paths: {}` trống + bearerAuth + default security bearerAuth: 3 path auth cần `security: []` override; schemas Error/ListEnvelope sẵn.
- `contracts/routes.yaml` -- role UPPER_SNAKE + landing per role (FE 1.5 tiêu thụ token/role cùng quy ước).
- `config/SecurityConfig.java`, `security/Envelope*Handler.java` -- chain 1.2 để mở rộng (thêm filter + 3 permitAll + method security).
- `controller/GlobalExceptionHandler.java`, `service/LogService.java`, `entity/Action.java` (LOGIN/LOGIN_FAILED không cần reason), `dto/ApiError*` -- nền 1.2 tiêu thụ nguyên vẹn.
- Test 1.1/1.2 phải xanh lại: `StorageHubApplicationTests` (RANDOM_PORT thật) + `StorageHubNonDevProfileTests` boot full context → từ 1.3 CẦN property `app.jwt.secret` test (nếu không context fail); `TestPingController`/`AccessDeniedEnvelopeTests` chain test-only không được vỡ.

## Tasks & Acceptance

**Execution:**

- [x] `contracts/openapi.yaml` -- thêm 3 path auth + request/response schemas + components.responses (D6); `security: []` cho 3 path -- contract đóng băng trước code (AD-2).
- [x] `backend/pom.xml` -- thêm jjwt-api + jjwt-impl/jjwt-jackson (runtime) -- thư viện sign/verify HS256.
- [x] `entity/User.java` + `entity/Role.java` (+ enum `UserStatus` + converter, `RoleName` map Title Case→UPPER_SNAKE) -- @Column tường minh, User ManyToOne Role LAZY.
- [x] `repository/UserRepository.java` + `RoleRepository.java` -- findByEmail/findByName + save (register).
- [x] `security/JwtProperties.java` + `service/JwtService.java` -- bind `app.jwt.*`, fail-fast secret <32; issue(sub,role)/parse+verify exp.
- [x] `security/JwtAuthenticationFilter.java` (+ status cache ~30s) + sửa `config/SecurityConfig.java` -- 3 permitAll, đăng ký filter, `@EnableMethodSecurity`.
- [x] `controller/AuthController.java` + `dto/` (Login/Register/Forgot request + Login/Register response) -- 3 endpoint đúng matrix.
- [x] `service/AuthService.java` -- login (BCrypt match, status check, log LOGIN/LOGIN_FAILED), register (CUSTOMER, email trùng → field error, cross-field confirm/agree), forgot (generic).
- [x] `controller/GlobalExceptionHandler.java` -- thêm handler `AccessDeniedException` → 403 FORBIDDEN envelope (fix latent 1.2; không còn rơi catch-all).
- [x] `db/migration/V3__activity_logs_actor_nullable.sql` + nới `ActivityLog`/`LogService` actor nullable -- log được thất bại email lạ (Q1=A).
- [x] Test: JwtService (issue/verify/expired/TTL), AuthService (các hàng login/register/forgot matrix, Mockito + verify LogService gọi đúng), filter status-cache (locked → 401), matrix enforcement 403 qua CHAIN THẬT (MockMvc + token ký thật), integration DB-gated (login seeded → 200 + row LOGIN trong activity_logs; sai → 401 + LOGIN_FAILED), mở rộng `StorageHubApplicationTests` (login thật HTTP đúng/sai) — mọi full-context test có `app.jwt.secret` test property.
- [x] `README.md` -- JWT_SECRET giờ fail-fast thật + curl login demo.

**Acceptance Criteria:**

- Given user active + đúng credentials, when POST /auth/login, then JWT (role claim UPPER_SNAKE, TTL 24h) + user info, ActivityLog LOGIN (EntityType USER).
- Given sai email/password hoặc Status 0/2, when login, then 401 envelope message chung + LOGIN_FAILED (email lạ: ActorID NULL, Reason chứa email đã thử).
- Given token còn hạn nhưng user bị lock/deactivate, when gọi endpoint bất kỳ, then filter chặn 401 (staleness ≤30s) — không đợi TTL.
- Given register hợp lệ, when POST /auth/register, then user CUSTOMER duy nhất (BCrypt); JSON có field `role` cũng bị bỏ qua; email trùng → 400 fieldErrors[email].
- Given forgot-password với email bất kỳ, then luôn 200 generic — không tiết lộ tồn tại.
- Given đúng 3 public auth path (+health), khi không token vẫn vào được; mọi path khác thiếu/kém token → 401 envelope; role không đủ → 403 envelope qua method security.
- Given `mvn test` (DB env như README), then PASS toàn bộ gồm 48 test cũ không regression.

## Implementation Notes

- 11/11 task hoàn thành. Đầy đủ entity User/Role, Flyway V3 (ActorID nullable), AuthService, AuthController, JwtService, JwtAuthenticationFilter, GlobalExceptionHandler (AccessDeniedException -> 403).
- Matrix Test Audit bổ sung test HTTP trong `StorageHubApplicationTests` cho nhánh duplicate email và cross-field validation.
- Đã chạy verify: toàn bộ unit/integration tests xanh; kịch bản live xác nhận token hợp lệ, 401 envelope khi sai credentials/token rác, và cache ~30s chặn ngay khi user bị lock.

## Spec Change Log

## Review Triage Log

| # | Layer | Finding | Verdict | Evidence / Disposition |
|---|-------|---------|---------|------------------------|
| 1 | blind-hunter | `JwtAuthenticationFilter` blocks public endpoints if expired Bearer token present | `medium` | Request to `/actuator/health` or `POST /api/v1/auth/*` carrying expired Bearer header fails in filter with 401 instead of reaching public endpoints. Group 1 -> patch. |
| 2 | blind-hunter | 500 error on failed sign-in with email > 223 chars exceeding `LogService` column limit | `medium` | Unknown email > 223 chars concatenates with prefix to > 255 chars, causing `LogService.append` to throw `IllegalArgumentException` (500). Group 2 -> patch. |
| 3 | blind-hunter | Missing `@Size` validation constraints on `LoginRequest` and `ForgotPasswordRequest` | `medium` | OpenAPI specifies `maxLength: 100` (email) and `72` (password); DTOs lacked `@Size`, allowing unbounded payload. Group 2 -> patch. |
| 4 | blind-hunter | Unhandled `DataIntegrityViolationException` on concurrent registration | `medium` | Concurrent registrations passing `findByEmail` simultaneously fail on DB unique constraint `uk_users_email`, returning 500 instead of 400. Group 3 -> patch. |
| 5 | blind-hunter | Default platform charset used for JWT signing key generation | `medium` | `properties.secret().getBytes()` in `JwtService` defaults to platform encoding instead of `StandardCharsets.UTF_8`. Group 4 -> patch. |
| 6 | blind-hunter | Unbounded in-memory `ConcurrentHashMap` in `JwtAuthenticationFilter` | `low` | Status cache follows the spec design note ("ConcurrentHashMap userId->(status, fetchAt), TTL 30s"); single-node memory risk negligible. Rejected per low finding rule. |
| 7 | blind-hunter | Missing CORS configuration causes browser preflight OPTIONS requests to fail with 401 | `defer` | CORS was not configured in 1.2 or 1.3 spec; pre-existing omission across foundations, deferred to story 1.5 when frontend auth is wired. Appended to deferred-work.md. |
| 8 | blind-hunter | Unsupported HTTP methods on public auth endpoints return 401 instead of 405 | `false` | Spring Security rules designate all non-permitted methods as authenticated; returning 401 envelope for unauthenticated calls matches the spec boundary rule. |
| 9 | blind-hunter | Email inputs are not trimmed or normalized | `false` | Jakarta `@Email` validator inherently rejects addresses with surrounding whitespace, and MySQL collation `utf8mb4_0900_ai_ci` handles case insensitivity. |
| 10 | blind-hunter | `JwtService.parse()` does not verify required token claims (`exp`, `iat`, `sub`) | `low` | Tokens are HMAC-signed by the server and cannot be forged; missing subject is already caught by `NumberFormatException`. Direct check added defensively. Group 4 -> patch. |
| 11 | blind-hunter | Missing `equals()` and `hashCode()` on JPA entities `User` and `Role` | `low` | Entities are not stored in collections or sets across detached states in story 1.3. Rejected per low finding rule. |
| 12 | blind-hunter | `GlobalExceptionHandler.onInvalidCredentials` omits `WWW-Authenticate` header | `low` | RFC 7235 requires `WWW-Authenticate` on 401 responses; adding header ensures full HTTP compliance. Group 5 -> patch. |
| 13 | blind-hunter | Javadoc broken reference and null handling in `Role.java` | `low` | `@link RoleName#fromTitleCase(Name)` has capitalized parameter causing doclint warning. Group 5 -> patch. |
| 14 | blind-hunter | `sprint-status.yaml` status is out of sync with story implementation completion | `false` | Sprint status transitions to `review` during step-05 per build workflow instructions. |
| 15 | verification-gap | Challenge header unverified in garbage token test (`AuthSecurityChainTests`) | `low` | Pre-verified gap: `garbageTokenAnswersThe401EnvelopeWithTheChallengeHeader` omits header assertion for `WWW-Authenticate: Bearer`. Group 6 -> patch. |
| 16 | verification-gap | WebMvc mapping of `InvalidCredentialsException` and `InvalidRequestException` unverified without database | `medium` | Pre-verified gap: Standalone MockMvc tests in `AuthSecurityChainTests` needed to verify exception handler mapping independently of DB. Group 6 -> patch. |
| 17 | verification-gap | Successful self-registration persistence and generated ID unverified in database-backed tests | `medium` | Pre-verified gap: Missing end-to-end integration test in `StorageHubApplicationTests` verifying successful registration insert, returned ID, and subsequent login. Group 6 -> patch. |
| 18 | verification-gap | `LoginRequest` `@Size(max = 100)` and `@Size(max = 72)` validation missing vs contract / audit reason limit | `medium` | Pre-verified gap / duplicate of finding 2 & 3. Group 2 -> patch. |
| 19 | edge-case-hunter | `JwtAuthenticationFilter.java:78-81`: `filterChain.doFilter` inside try-catch block catching `IllegalArgumentException` | `medium` | Downstream application `IllegalArgumentException` is caught and converted into a false 401 `BadCredentialsException`. Group 1 -> patch. |
| 20 | edge-case-hunter | `JwtAuthenticationFilter.java:63-81`: Request sent to public auth endpoints carrying expired/invalid Bearer token returns 401 | `medium` | Duplicate of finding 1. Group 1 -> patch. |
| 21 | edge-case-hunter | `AuthService.java:94-96`: Reason overflow in LogService on long unknown email | `medium` | Duplicate of finding 2. Group 2 -> patch. |
| 22 | edge-case-hunter | `AuthService.java:110`: Null password in register causes NPE | `low` | Direct service call with null password throws NPE on `.equals()`; `Objects.equals` prevents NPE. Group 3 -> patch. |
| 23 | edge-case-hunter | `AuthService.java:118-135`: Concurrent registration unique constraint violation | `medium` | Duplicate of finding 4. Group 3 -> patch. |
| 24 | edge-case-hunter | `AuthService.java:83-85`: Malformed password hash in database causes BCrypt to throw IllegalArgumentException | `low` | Bad hash in database could trigger 500 error instead of 401; defensive try-catch handles corrupted hash. Group 3 -> patch. |
| 25 | edge-case-hunter | `JwtService.java:30`: Default platform charset used instead of UTF-8 | `medium` | Duplicate of finding 5. Group 4 -> patch. |
| 26 | edge-case-hunter | Public endpoints and health accessible without authentication blocked if invalid token sent | `medium` | Duplicate of finding 1. Group 1 -> patch. |
| 27 | edge-case-hunter | `LoginRequest` missing `@Size` vs openapi contract | `medium` | Duplicate of finding 3. Group 2 -> patch. |

### Grouping and Routing

- **Group 1 (Filter public endpoint bypass & exception isolation)**: Findings 1, 19, 20, 26 -> `patch` (Highest verdict: `medium`)
- **Group 2 (Request DTO `@Size` constraints & defensive audit reason truncation)**: Findings 2, 3, 18, 21, 27 -> `patch` (Highest verdict: `medium`)
- **Group 3 (Concurrent registration constraint handling & null/hash defense)**: Findings 4, 22, 23, 24 -> `patch` (Highest verdict: `medium`)
- **Group 4 (JWT signing key charset & claim verification)**: Findings 5, 10, 25 -> `patch` (Highest verdict: `medium`)
- **Group 5 (RFC 7235 `WWW-Authenticate` header & Role Javadoc)**: Findings 12, 13 -> `patch` (Highest verdict: `low`)
- **Group 6 (Verification gaps in test suite)**: Findings 15, 16, 17 -> `patch` (Highest verdict: `medium`)
- **Group 7 (CORS configuration)**: Finding 7 -> `defer` (Appended to `deferred-work.md`)

## Design Notes

- Message chung cho mọi thất bại login (unknown email/wrong password/inactive/locked): một chuỗi duy nhất kiểu NFR-7 (chuyện gì + bước kế), không exclamation — tránh timing oracle: mọi nhánh đi qua cùng bcrypt+lookup path (kiểm tra hash cả khi không tìm thấy user).
- Cross-field register settle defer 1.2 (F18): service tự raise với fieldErrors[confirmPassword|agreeToTerms] — không dùng class-level constraint.
- Permission matrix hôm nay = role-level (`ROLE_<ROLE>`); key mịn + scope facility (Staff theo Zone, FM theo cơ sở — FR-39) sẽ vào cùng endpoint của story 2.x+, neo về MỘT class matrix để 9.3 đọc đúng định nghĩa enforce.
- jjwt-jackson kéo Jackson 2 runtime — chung sống với Jackson 3 của Boot 4 (package khác nhau); nếu xung đột bean thì thay bằng serialize claims bằng tools.jackson.
- Status cache: `ConcurrentHashMap` userId→(status, fetchAt), TTL 30s, chỉ chặn Status≠1; single-node assumption.
- Register không audit (registry Action 1.2 không có REGISTER, AC không yêu cầu) — khi 9.x cần thì thêm action vào registry.

## Verification

**Commands:**

- `cd backend && mvn test` (DB env như README) -- expected: PASS toàn bộ (mới + 48 cũ), 0 skipped ngoài DB-gated pattern.
- Boot dev + curl: login seeded `lan@storagehub.dev`/`Demo1234!` → 200 + token; sai password → 401 envelope message chung; GET `/api/v1/anything` với token vừa nhận → 401 (chưa có endpoint) nhưng KHÔNG phải 401-without-envelope; health 200 public.
- `SET locked: UPDATE users SET Status=2 WHERE Email='lan@storagehub.dev'` → token cũ gọi lại → 401 trong ≤30s; reset Status=1 sau test.
