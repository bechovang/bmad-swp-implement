# StorageHub

Monorepo kho điểm danh không chính danh — BTL SWP391. Story 1.1 dựng khung
monorepo từ 0 theo *Structural Seed* (skeleton có thể build + migrate + chạy,
chưa có feature nào của Epic 1).

```
.
├── backend/     Spring Boot 4.1.1 · Java 21 · Flyway (V1 schema + V2 seed dev-only)
├── frontend/    Vite 8.3 · React 19.3 (TypeScript) · TanStack Query · MSW 2.x
├── contracts/   openapi.yaml (AD-8 envelope) + routes.yaml (FE skeleton)
└── docs/        planning artifacts (PRD, architecture, UX)
```

## Week-1 decisions

| Quyết định | Trạng thái | Ghi chú |
| --- | --- | --- |
| FE scaffold dùng **TypeScript** | **Đã chốt 2026-10-02** | `frontend/` là react-ts template; KHÔNG dùng JS |
| Component library | **Đã chốt 2026-10-02 (Story 1.4)** | Tailwind CSS v4 + Radix UI headless primitives + Control Room Primitives; không dùng UI suite cồng kềnh |
| Drag & drop (task board) | **Đã chốt 2026-10-02** | Chọn `@dnd-kit` (Kanban task board Epic 3); chưa cài vào package.json, cài khi bắt đầu Story Epic 3 |
| Chart library (BO dashboard) | **Đã chốt 2026-10-02** | Chọn `recharts` (Back-Office analytics dashboard Epic 7); chưa cài vào package.json, cài khi bắt đầu Story Epic 7 |
| QR/code scanner (check-in) | **Đã chốt 2026-10-02** | Chọn `html5-qrcode` (Check-in QR scanner Epic 3); chưa cài vào package.json, cài khi bắt đầu Story Epic 3 |

## Yêu cầu môi trường (từ 0)

- **JDK 21+** (build đã verify trên JDK 25, release target 21)
- **Node.js 20.19+ / 22.12+** (verify trên 24.x)
- **MySQL 8.4.11** (LTS) — toàn bộ DDL dùng InnoDB / utf8mb4_0900_ai_ci
- Maven: repo đã có wrapper `backend/mvnw`; máy có Maven cục bộ thì dùng `mvn` cũng được

## Biến môi trường (bắt buộc, không commit)

Backend đọc config 100% từ env — không có default nào cho secret.

Ai fail-fast ngay khi thiếu, ai chưa:

- **`DB_URL` / `DB_USERNAME` / `DB_PASSWORD`** — datasource bind ngay lúc
  khởi động: thiếu biến nào thì boot fail tại placeholder đó, message nêu
  rõ tên biến.
- **`JWT_SECRET`** — **từ story 1.3 đã fail-fast thật**: `app.jwt.*` được
  bind vào `JwtProperties` lúc khởi động. Thiếu env var thì boot chết ngay
  tại placeholder `${JWT_SECRET}`; đặt mà ngắn hơn 32 ký tự thì bị
  `JwtProperties` từ chối (HS256 cần key ≥ 256 bit) với message nêu rõ.
- **`PAYOS_*`** — đã khai báo trong `application.yml` (`app.payos.*`) nhưng
  chưa có code nào đọc trước epic 2 (PayOS webhook), nên boot scaffold chưa
  ép chúng.

| Biến | Ý nghĩa | Ví dụ |
| --- | --- | --- |
| `DB_URL` | JDBC URL (Flyway + JPA dùng chung) | `jdbc:mysql://localhost:3306/storagehub` |
| `DB_USERNAME` | user MySQL | `root` |
| `DB_PASSWORD` | password MySQL | — |
| `JWT_SECRET` | HMAC-SHA256 key cho JWT (TTL 24h, AD-5) | chuỗi ≥ 32 ký tự ngẫu nhiên |
| `PAYOS_CLIENT_ID` | PayOS credential (AD-9) | từ dashboard PayOS |
| `PAYOS_API_KEY` | PayOS credential | từ dashboard PayOS |
| `PAYOS_CHECKSUM_KEY` | PayOS credential | từ dashboard PayOS |

Lưu ý: **Spring Boot / Maven không tự nạp file `.env*`** — đặt biến bằng
export trong shell hoặc env của IDE/run-config. Template tên biến xem ở
`.env.example` (file họ `.env` duy nhất được commit).

## Chạy từ 0

### 1. Tạo database

```sql
CREATE DATABASE storagehub CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

(Lưu ý MySQL 8.4: đổi `mysql_native_password` mặc định sang `caching_sha2_password`
nếu dùng user cũ tạo từ MySQL 5.x/8.0.)

### 2. Backend

```bash
cd backend
DB_URL="jdbc:mysql://localhost:3306/storagehub" \
DB_USERNAME=root DB_PASSWORD=<mk> \
JWT_SECRET=<chuỗi ngẫu nhiên ≥32 ký tự> \
PAYOS_CLIENT_ID=x PAYOS_API_KEY=x PAYOS_CHECKSUM_KEY=x \
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Profile **dev**: Flyway chạy `V1__init_schema.sql` **và** `V2__seed_demo.sql`
  (seed chỉ nằm trong `db/seed/dev`, được add vào `spring.flyway.locations`
  của profile dev — không có Java code phân nhánh).
- Profile **prod** (hoặc không profile): **chỉ V1 + V3, không seed** (V3 từ
  story 1.3: `activity_logs.ActorID` NULL để log được LOGIN_FAILED email lạ).
  Đừng chạy prod profile vào DB đã seed dev: Flyway validate fail cứng ngay
  lúc boot (`applied migration not resolved locally: 2` — history có V2 nhưng
  locations của prod không chứa seed), không phải chỉ "thiếu dữ liệu". Và vì
  `V2` đã thuộc về seed dev, các migration schema thật tiếp theo chạy số từ
  `V3` như hiện tại.
- `ddl-auto: validate` — Flyway là nguồn DDL duy nhất, JPA chỉ kiểm schema.
- Mọi connection ép session timezone **UTC** (`connectionTimeZone=UTC` qua
  Hikari `data-source-properties`) — `DEFAULT CURRENT_TIMESTAMP` ghi UTC,
  khớp chuẩn "timestamps stored as UTC" và literal UTC trong seed.

Health check: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

Đăng nhập demo (story 1.3) — 3 endpoint public `POST /api/v1/auth/login |
register | forgot-password`, trả JWT HS256 (claim `role` UPPER_SNAKE, TTL
24h, không refresh token; logout = client bỏ token):

```bash
# Login đúng (mật khẩu chung của 5 tài khoản demo: Demo1234!)
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"lan@storagehub.dev","password":"Demo1234!"}'
# → 200 {"token":"<JWT>","user":{"id":1,"fullName":"Lan Nguyen",...,"role":"CUSTOMER",...}}
# và một hàng audit LOGIN (EntityType USER) trong activity_logs.

# Sai password — mọi nhánh thất bại (sai email / sai password / Status 0-2)
# trả CÙNG một message chung, kèm hàng LOGIN_FAILED (email lạ: ActorID NULL,
# Reason chứa email đã thử):
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"lan@storagehub.dev","password":"WrongPassword1"}'
# → 401 {"code":"UNAUTHENTICATED","message":"The email or password is not correct. ..."}

# Dùng token (filter parse + re-check users.Status, cache ~30s):
curl -s http://localhost:8080/api/v1/anything -H "Authorization: Bearer <JWT>"
# → 404 envelope NOT_FOUND (token hợp lệ nên đã qua 401; chưa có endpoint)
curl -s http://localhost:8080/api/v1/anything
# → 401 envelope UNAUTHENTICATED + WWW-Authenticate: Bearer (không token)

# Vô hiệu hóa tài khoản để thấy token chết trong ≤30s (không đợi TTL 24h):
#   UPDATE users SET Status = 2 WHERE Email = 'lan@storagehub.dev';
# rồi gọi lại curl có token sau ≤30s → 401. Reset Status=1 sau khi thử.
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

- Dev proxy: request `/api/*` từ Vite (5173) được forward sang
  `http://localhost:8080` (same-origin, không cần CORS).
- Mock: MSW worker chỉ bật khi `VITE_ENABLE_MSW=true` trong dev; handler mẫu
  đang trả ListEnvelope cho `GET /api/v1/units`.
- Build/test: `npm run build` (tsc + vite), `npm test` (vitest + RTL).

## Dữ liệu demo (V2, chỉ dev)

Snapshot cố định **2026-10-19** (sau cao trào UJ-1 — contract đã đóng,
settlement đã refund). Tất cả tài khoản mật khẩu `Demo1234!` (BCrypt $2a$10$):

| Email | Vai trò | Landing |
| --- | --- | --- |
| `lan@storagehub.dev` | Customer | /units |
| `minh@storagehub.dev` | Staff | /tasks |
| `tuan@storagehub.dev` | Facility Manager | /overview |
| `hang@storagehub.dev` | Business Ops | /business-overview |
| `nam@storagehub.dev` | System Administrator | /users |

Chuỗi nghiệp vụ mẫu: đơn **S-3** (PREPARING, 5m², PIN) — reservation `BK-1042`
(Lan, 2026-10-03 → 10-18, deposit 172,500đ, access code 482913, CLOSED) —
extension 1 ngày (10-17 → 10-18, phí 690,000đ) — checkout `RT-0871` —
contract `CT-1042` (policy v3, IsLatest=1) — addendum `CT-1042-A1`
(SIGNED) — settlement `RC-2026-0005` (damage fee 40,000đ, refund 132,500đ).
Đơn **M-2** (MAINTENANCE, 8m², QR) là đơn bị ngập gắn với ticket `SR-0033`
đang ESCALATED; **M-5** AVAILABLE. Policy matrix: v1/v2 RETIRED, **v3 ACTIVE**
từ 2026-10-01 với đủ 27 rules (7 RuleType, gồm 3 loại mới AD-6:
TURNOVER_BUFFER / DISCOUNT / WAIVER_CAP).

Quy ước dẫn xuất trong seed (không có column tương ứng trong dbml):

- Mã addendum `CT-1042-A1` lưu trong `ContentSnapshot.code` (quy ước
  `<mã contract>-A<số thứ tự>`), schema giữ nguyên 22 bảng + đúng 7 delta AD-6.
- `payments` ghi DEPOSIT / RENT / EXTENSION_FEE; damage fee 40,000đ không
  xuất hiện trong `payments` — nó được trừ trong settlement (refund = deposit
  đang giữ − damage fee), đúng nghiệp vụ Flow-2.

## Verify story 1.1 (đã chạy, kết quả PASS — đã chạy lại sau review patch)

Lần lượt trên MySQL 8.4.11 (Docker, port 3307):

1. `cd backend && mvn -q -DskipTests package` → jar build PASS
   (test compile riêng: `mvn test-compile`).
2. `cd frontend && npm install && npm run build && npm test` →
   PASS (2/2 test, build sạch tsc + vite; sau review đã bật
   `"strict": true` cho cả tsconfig.app và tsconfig.node — build vẫn sạch).
3. `cd backend && mvn test` với đầy đủ env (DB trên 3307) →
   PASS 3/3: smoke dev profile, non-dev profile (schema riêng
   `storagehub_nondev_test`: Flyway chỉ áp V1, bảng `users` rỗng — seed
   không bao giờ chạy ngoài dev), và test pin BCrypt hash của
   `Demo1234!` trong V2 seed.
4. Boot dev profile: `flyway_schema_history` có đúng V1 + V2; đếm được
   5 roles / 5 users / 3 units / 3 policies (v3 ACTIVE) / 27 policy_rules /
   1 reservation / 1 contract / 1 addendum / 1 settlement / 4 payments /
   4 inspections / 2 tickets / 1 escalation / 6 tasks / 10 notifications /
   23 activity_logs / 9 staff_assignments — khớp mock set của spec.
   Boot này gồm cả Hikari `connectionTimeZone=UTC` mới (sau review):
   healthy `/actuator/health` 200.
5. Boot prod profile trên DB mới: chỉ có V1, mọi bảng rỗng (không seed).
6. Drop + recreate DB rồi chạy lại dev → bộ đếm và các mã (BK-1042,
   CT-1042, RC-2026-0005…) lặp lại y hệt (reproducible từ 0).
7. Proxy: curl `http://localhost:5173/api/v1/...` nhận đúng response của
   Spring (Boot Security 401 hiện tại — epic 1 sẽ thay bằng JWT) chứng tỏ
   Vite forward `/api` sang 8080.
8. `git check-ignore`: `backend/storage/`, `.env*` (kể cả `backend/.env`,
   `prod.env`), `target/`, `node_modules/`, `dist/`, `_bmad/render/`
   đều khớp rule đúng; `git status` không thấy build artifact nào.

## PayOS compatibility (kết quả chốt)

`vn.payos:payos-java:2.0.1` đã nằm trong `backend/pom.xml` từ ngày đầu.
**Kết quả verify trên Spring Boot 4.1.1: PASS ở mức dependency-resolution +
compile + package** — jar build thành công với SDK này trong classpath,
không xung đột version với Boot 4 BOM (SDK chỉ phụ thuộc thư viện HTTP/JSON
nhẹ, không kéo Spring version nào).

**Chưa verify**: gọi thật vào PayOS sandbox (create payment link, webhook
signature). Việc này cần credential thật và thuộc story thanh toán của
epic sau — khi đó nếu phát hiện bất tương thích thì xử lý tại đó, hiện
chưa có tín hiệu xấu nào.

## Ghi chú cấu trúc

- **Không có feature Epic 1 nào** trong story này: chưa có auth, chưa có
  envelope filter/controller, chưa có LogService/NotificationService, chưa
  có UI component nào của app (App.tsx là scaffold home).
- `backend/storage/` (nơi runtime lưu file upload) đã gitignore, trống.
- Springdoc trong app chỉ để dev đọc (docs-only); `contracts/openapi.yaml`
  mới là nguồn sự thật (AD-2), viết tay theo AD-8 (Error + ListEnvelope).
- `contracts/routes.yaml` là skeleton routing FE: public login/register/
  forgot-password, landing theo 5 vai trò, các route detail — status
  `placeholder` chờ epic 1.
- Test backend cần MySQL thật qua env (`DB_URL`…); thiếu env thì tự **skip**
  (`@EnabledIfEnvironmentVariable`), không fail. Hai lớp test DB dùng schema
  riêng tách khỏi dev qua biến URL riêng: `LogServicePersistenceTests` (story
  1.2) đọc `DB_URL_LOGTEST` (mặc định
  `jdbc:mysql://localhost:3306/storagehub_log_test`, tự tạo),
  `StorageHubNonDevProfileTests` đọc `DB_URL_NONDEV` (mặc định schema
  `storagehub_nondev_test`) — guard chạy vẫn theo `DB_URL`, nên khi trỏ env
  sang server khác phải set thêm biến riêng tương ứng.
  `StorageHubNonDevProfileTests` chứng minh seed không chạy ngoài dev;
  `SeedDemoCredentialsTests` pin hash BCrypt `Demo1234!`.
  (Testcontainers tính tiếp ở story sau.)
