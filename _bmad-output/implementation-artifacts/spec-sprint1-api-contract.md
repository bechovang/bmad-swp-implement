---
title: 'Sprint 1 API contract — contracts/openapi.yaml + routes.yaml'
type: 'feature'
created: '2026-09-22'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - D:/FPT_Ky5/SWP/StorageHub/docs/architecture/ARCHITECTURE-SPINE.md
  - D:/FPT_Ky5/SWP/StorageHub/docs/prd/prd.md
  - D:/FPT_Ky5/SWP/StorageHub/docs/models/ERD_StorageHub.dbml
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Sprint 1 (13 US, 22/09–05/10) cần API contract đóng băng **trước** khi BE (An/Phúc/Huy) và FE (Phú/Tuấn Anh) code song song — AD-2 contract-first, chính là deliverable của US-1 "Project Setup + API Contract". Repo StorageHub hiện chưa có thư mục `contracts/`.

**Approach:** Viết `contracts/openapi.yaml` (OpenAPI 3.0.3, base `/api/v1`) phủ đúng API surface Sprint 1: auth (login / register / forgot-password / me), browse units + filter-options, unit detail + quote (giá single-source AD-11), reservations (create có re-check FR-5, list, detail, check-in pass), payments (init / confirm / get / list — mock gateway 3 phương thức AD-9), contract chain (FR-10/13), notifications (list / unread-count / mark-all-read). Kèm `contracts/routes.yaml` — route table FE + deep-link registry (AD-6). Envelope lỗi + list theo AD-8 (machine code, page 1-based), JWT + 3 public endpoints theo AD-5, money/time theo AD-7. Endpoint ngoài Sprint 1 không khai báo (đóng băng từng sprint).

</frozen-after-approval>

## Implementation Notes

- Files created: `StorageHub/contracts/openapi.yaml` (~1016 dòng, OpenAPI 3.0.3, 21 operations / 6 tags), `StorageHub/contracts/routes.yaml` (route table + AD-6 deep-link registry + landing-by-role).
- **Đặt tên:** version `1.0.0-sprint1`; mọi operationId camelCase; query param kebab-case (`type-id`, `size-m2`, `start-date`, `unread-only`) theo AD-8.
- **Auth:** login 200 (không 201 vì không tạo resource); register 201 trả AuthSession luôn (đăng nhập ngay — khớp EXPERIENCE "rơi vào Browse Units đã đăng nhập"); lock account 423 AUTH_ACCOUNT_LOCKED; forgot-password luôn 200 generic (không lộ email tồn tại). `/auth/me` trả UserSummary + landingRoute cho boot shell.
- **Units:** availability là object `{status, availableFromDate}` (AVAILABLE / AVAILABLE_SOON) thay vì boolean — card buffer cần ngày để hiển thị "Available {date} · cleaning buffer". `baseMonthlyRent` đưa vào UnitSummary làm căn cứ sort giá. filter-options tách endpoint riêng (dropdown). Quote là GET có start-date + duration-months required — dùng chung cho Unit Detail price table và Booking Summary nền.
- **Quote/AD-11:** line kinds RENT/SURCHARGE/DISCOUNT/DEPOSIT (DISCOUNT để dành FR-40 P2, ghi rõ chưa dùng Sprint 1); response có totalRent + depositAmount + dueNow (= deposit khi booking) + policyVersion. Snapshot into ReservationDetail.quote.
- **Payments/AD-9:** một POST /payments dùng chung 3 method — method-specific fields (card object / momoPhone) mô tả điều kiện trong description; không có field outcome trong request. CARD→PROCESSING poll; MOMO→PENDING + otpRequired → POST confirm; VNPAY→PENDING + qrPayload/qrExpiresAt poll. Confirm chỉ dành MoMo (ghi trong description). PaymentResult gôm payment + receipt + reservation (sau flip) + contract draft + NotificationEvent (FE bắn toast từ response — Conventions).
- **EXPIRED on-read (AD-4):** mô tả tại GET /reservations/{id}; depositStatus FORFEITED + depositForfeitReason ("Deposit forfeited — no-show") đại diện receipt no-show FR-36.
- **Contract:** chain endpoint dưới /reservations/{id}/contracts (scope rõ), detail /contracts/{id} với contentSnapshot string (FE render print view — không PDF service). signedPhotoUrl example trỏ /api/v1/attachments/1 — endpoint attachment thuộc Sprint 2, chỉ giữ URL shape.
- **Machine codes:** 15 code trong enum Error duy nhất (AD-8 "liệt kê đầy đủ 4xx" — dùng components.responses BadRequest/Unauthorized/Forbidden/UnitNotFound/ReservationNotFound + inline cho 409 nghiệp vụ). 409 = business block theo AD-8.
- **routes.yaml:** modal không có route (Payment, Forgot Password, Contract print); 4 landing Sprint 2+/3 khai báo placeholder để notification tương lai không đổi tên route; mọi deep-link Sprint 1 trỏ /rentals/{reservationId}.
- Nhánh code repo: master trực tiếp (file contract, không code — đúng AD-2 "PR vào file này trước", repo mới chưa có flow PR).

## Review Triage Log

Blind Hunter: 20 findings (floor N=7). Verdicts sau khi đối chiếu file:

1. `$ref` + siblings bị ignore trong OAS 3.0.3 (depositStatus nullable, quote/reservation description, contract nullable) — **high, patched**: bọc `allOf` (nullable kèm `type` trong subschema).
2. Poll endpoint mô tả countdown `qrExpiresAt` nhưng trả PaymentRecord không có field — **high, patched**: GET /payments/{id} trả PaymentSession (thêm receiptCode/paidAt nullable).
3. PaymentResult.notification lúc FAILED không có — **medium, patched** (nullable + điều kiện trong description). Ghi chú: claim "is required" là false — không có required array; phần ambiguity là thật.
4. MoMo OTP không có expiry — **medium, patched**: `otpExpiresAt` + message PAYMENT_EXPIRED chung "QR/OTP".
5. Thiếu POST /notifications/{id}/read (mark-read từng item) — **medium, deferred**: thêm public API = sửa frozen Intent; FR-34 nguyên văn chỉ đòi mark-all-read. Ghi deferred-work.
6. EXPIRED no-show chưa khai báo ở GET /reservations (list) — **high, patched**: description ghi rõ on-read áp cả list (AD-4).
7. Thiếu `required` khắp schemas — **high, patched**: 26 required arrays cho mọi schema chính (codegen FE/BE).
8. card/momoPhone conditional chỉ trong prose — **low, rejected**: OAS 3.0 oneOf làm codegen nặng hơn lợi ích; VALIDATION_FAILED fieldErrors là kênh báo lỗi của team.
9. Không 5xx/INTERNAL_ERROR, không 429 — **medium patched** (INTERNAL_ERROR vào enum + note 500 ở header comment) / **429 rejected** (không có yêu cầu PRD/NFR Sprint 1).
10. 423 AUTH_ACCOUNT_LOCKED leak account state — **medium, patched**: rationale trong description (chỉ trả sau khi mật khẩu đúng → không phải vector enumeration email).
11. 404 Unit inline trùng components.responses — **low, patched**: đổi sang $ref UnitNotFound.
12. Landing enum trùng routes.yaml landing-by-role — **low, rejected**: OAS không import được; đã có cross-ref description "khớp routes.yaml".
13. deep-links lặp path template — **low, patched**: giá trị = key trong routes map (DRY một chỗ).
14. notifications dưới heading Customer — **low, patched**: nhóm "Mọi role" (bell cross-role).
15. Không root `/`, non-CUSTOMER Sprint 1 thấy gì chưa định nghĩa — **medium, patched**: note root redirect theo landingRoute + Sprint 1 landing placeholder = shell + empty state.
16. Notification chỉ có title — **medium, patched**: `body` nullable (Notification + NotificationEvent).
17. Forgot-password message tiếng Anh — **low, patched**: đổi tiếng Việt theo NFR-7.
18. PaymentRecord thiếu reservationId — **medium, patched**: thêm required (map receipt về rental trong list không filter).
19. signedPhotoUrl trỏ endpoint chưa khai báo — **low, patched**: annotation "endpoint Sprint 2, URL giữ shape".
20. Browse yêu cầu đăng nhập chưa nói rõ — **medium, patched**: ghi rõ AD-5 (đúng 3 public endpoints).

Tổng: 16 patched, 1 deferred (finding 5), 3 rejected (8, 12, phần 429 của 9).
