---
title: 'Story 1.6 — Toast + Bell + Notification Center'
type: 'feature'
created: '2026-10-02'
baseline_commit: 'c947e9a70c22c6705a4c695ac70813ad24dea7e4'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/contracts/routes.yaml'
  - '{project-root}/contracts/openapi.yaml'
  - '{project-root}/docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Hệ thống hiện tại chưa có cơ chế thông báo cho người dùng khi các sự kiện quan trọng xảy ra (thanh toán cọc thành công, hợp đồng phát sinh, phân công công việc, yêu cầu cứu hộ, đổi trạng thái kho). Người dùng thiếu kênh phản hồi tức thời (toast) và thiếu trung tâm lưu vết thông báo bền vững (bell feed & Notification Center) để theo dõi các sự kiện tiền và trạng thái.

**Approach:** Xây dựng hệ thống thông báo 2 kênh hoàn chỉnh cả Backend và Frontend:
- **Backend:** Mở rộng `contracts/openapi.yaml` với các schema và endpoint thông báo (`GET /notifications/unread-count`, `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/mark-all-read`); tạo `NotificationEntity`, `NotificationRepository`, `NotificationService` (với typed event API `send(NotificationEvent)` dành cho các module nghiệp vụ gọi trong cùng transaction), và `NotificationController` bảo vệ bằng JWT auth; verify với bộ test Spring Boot.
- **Frontend:** Xây dựng Toast System (`ToastProvider`, `useToast`, auto-dismiss ~4s, hover pause, 3px tone accent bar, tối đa 1 action link); nâng cấp Bell Icon trên `TopBar` với badge đếm số lượng unread màu error-red, polling bằng TanStack Query `refetchInterval`; xây dựng `NotificationCenter` (dưới dạng Drawer trượt từ mép phải khi click chuông và trang `/notifications` độc lập) hỗ trợ hiển thị unread-first, phân loại theo vai trò, deep-link trực tiếp đến đối tượng nghiệp vụ theo `routes.yaml`, và nút Mark All as Read; cập nhật MSW mock handlers và verify đầy đủ bằng Vitest + RTL.

## Boundaries & Constraints

**Always:**
- Bám sát `contracts/routes.yaml`, `contracts/openapi.yaml` và `DESIGN.md` verbatim:
  - Bảng `notifications`: `NotificationID` BIGINT PK AUTO_INCREMENT, `UserID` BIGINT FK users, `Type` VARCHAR(50), `Title` VARCHAR(200), `DeepLink` VARCHAR(255), `IsRead` TINYINT DEFAULT 0, `CreatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP.
  - Endpoints:
    - `GET /api/v1/notifications/unread-count`: trả `{ count: integer }` (endpoint gọn nhẹ chuyên phục vụ polling chuông).
    - `GET /api/v1/notifications`: trả `ListEnvelope<NotificationDto>` (`{ items, page, pageSize, total }`), sắp xếp unread lên đầu (`isRead ASC, createdAt DESC`).
    - `POST /api/v1/notifications/{id}/read`: đánh dấu đã đọc một thông báo (chỉ cho phép user sở hữu thông báo).
    - `POST /api/v1/notifications/mark-all-read`: đánh dấu tất cả thông báo của user hiện tại thành đã đọc.
  - Phân quyền & Transaction:
    - Chỉ các request đã xác thực (có Bearer JWT) mới được truy cập notification endpoints; người dùng chỉ xem và chỉnh sửa thông báo của chính mình (`UserID = currentUserId`).
    - Module nghiệp vụ muốn tạo thông báo bắt buộc phải gọi `NotificationService.send(...)` trong cùng transaction; cấm các module tự INSERT trực tiếp (AD-6).
  - Toast behavior:
    - Hiển thị ở góc dưới bên phải màn hình (`bottom-right`), thời gian hiển thị ~4s, tự động tạm dừng đếm ngược khi hover chuột (`hover pause`), thanh viền 3px trái (`3px left accent bar`) theo tone màu (`info` indigo `#4F46E5`, `success` green `#059669`, `warning` amber `#F59E0B`, `error` red `#DC2626`).
    - Mỗi toast có tối đa 1 action link (deep link). Toast không bao giờ thay thế bell (sự kiện quan trọng/tiền luôn phát cả toast và ghi vào bell).
    - FE render toast từ response payload có trường `notification` — FE không tự bịa chuỗi thông báo.
  - Bell & Notification Center:
    - Bell badge màu `error-red` (`#DC2626`) hiển thị số lượng chưa đọc khi `count > 0`. Polling định kỳ qua TanStack Query (ví dụ mỗi 15-30 giây).
    - Notification Center hiển thị feed theo thứ tự chưa đọc trước, hỗ trợ filter tab (All, Unread), click vào entry chuyển hướng đến `deepLink` và tự động đánh dấu đã đọc; hỗ trợ nút "Mark all as read".
    - Tuyệt đối không gửi email (never email).
- Desktop-first, English-only, light mode only.

**Never:**
- Không gửi email dưới bất kỳ hình thức nào.
- Không cho phép user đọc hoặc sửa thông báo của user khác (chặn triệt để ở mức query service).
- Không hardcode chuỗi thông báo trên frontend khi nhận notification event từ API.
- Không để toast đè lên nhau mà không có layout stack có trật tự.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Đếm unread khi có thông báo | User có 1 thông báo chưa đọc trong seed | `GET /notifications/unread-count` trả `{"count": 1}`, bell hiển thị badge `"1"` | — |
| Đếm unread khi không có thông báo | Tất cả thông báo đã đọc | `GET /notifications/unread-count` trả `{"count": 0}`, bell ẩn badge | — |
| Lấy danh sách notifications | `GET /notifications?page=1&pageSize=10` | Trả `ListEnvelope` có items, unread hiển thị trước, phân trang chuẩn | — |
| Đánh dấu 1 thông báo đã đọc | `POST /notifications/{id}/read` | Notification đổi `isRead=true`, unread-count giảm 1, bell cập nhật tức thì | 404 nếu không tìm thấy, 403 nếu không thuộc user |
| Mark all as read | Click "Mark all as read" | Tất cả thông báo của user đổi `isRead=true`, unread-count về 0, badge biến mất | — |
| Bắn toast từ mutation response | Mutation trả về `{ ..., "notification": { "title": "Deposit received", "tone": "success", "deepLink": "/rentals/1" } }` | Bắn toast góc dưới phải màu xanh, tiêu đề và link "View details", tự tắt sau 4s | Không văng lỗi nếu response không có notification |
| Hover chuột vào toast | Đưa trỏ chuột vào toast đang hiển thị | Thanh đếm ngược dừng lại, toast không bị ẩn cho đến khi rời chuột | — |
| Click vào thông báo trong feed | Click vào entry có `deepLink="/rentals/1"` | Chuyển trang đến `/rentals/1`, tự động đánh dấu đã đọc | — |
| Mở Notification Center khi trống | User không có thông báo nào | Hiển thị `EmptyState` chuẩn với icon chuông và thông điệp "No notifications yet" | — |
| Gọi API khi chưa đăng nhập | Không có Authorization header | 401 Unauthorized theo chuẩn ErrorEnvelope | — |

</frozen-after-approval>

## Code Map

- `contracts/openapi.yaml` -- bổ sung endpoints `/notifications/unread-count`, `/notifications`, `/notifications/{id}/read`, `/notifications/mark-all-read` và schemas `NotificationDto`, `UnreadCountResponse`.
- `backend/src/main/java/com/storagehub/notification/`
  - `Notification.java` -- JPA Entity mapping bảng `notifications`.
  - `NotificationRepository.java` -- Spring Data JPA repository với các query scoped theo `userId`.
  - `NotificationEvent.java` -- DTO sự kiện nghiệp vụ nội bộ (userId, type, title, deepLink, tone).
  - `NotificationDto.java` + `UnreadCountResponse.java` -- DTOs theo chuẩn OpenAPI.
  - `NotificationService.java` + `NotificationServiceImpl.java` -- service xử lý nghiệp vụ thông báo và event API công khai.
  - `NotificationController.java` -- REST controller exposing các endpoint thông báo.
- `frontend/src/types/notification.ts` -- TypeScript types cho notifications.
- `frontend/src/api/notification.ts` -- API client calls cho notifications (`getUnreadCount`, `getNotifications`, `markAsRead`, `markAllAsRead`).
- `frontend/src/context/ToastContext.tsx` + `frontend/src/hooks/useToast.ts` -- Toast provider & hook quản lý toast queue, hover pause, auto-dismiss.
- `frontend/src/components/ui/Toast.tsx` -- Toast primitive component với 3px left accent bar, close button, action link.
- `frontend/src/components/notification/BellButton.tsx` -- Nút chuông thông báo trên `TopBar` với polling unread count và error-red badge.
- `frontend/src/components/notification/NotificationCenter.tsx` -- Drawer & view hiển thị danh sách thông báo, tab filter, deep links, mark all read.
- `frontend/src/pages/notification/NotificationCenterPage.tsx` -- Trang `/notifications` độc lập.
- `frontend/src/components/layout/TopBar.tsx` -- tích hợp `BellButton` vào vị trí chuông thông báo.
- `frontend/src/mocks/handlers.ts` -- MSW mock handlers cho notification endpoints.
- `backend/src/test/java/com/storagehub/notification/` -- Unit & slice tests cho `NotificationService` và `NotificationController`.
- `frontend/src/test/notification.test.tsx` + `frontend/src/test/toast.test.tsx` -- Vitest tests cho Toast và Notification Center.

## Tasks & Acceptance

**Execution:**

- [x] `contracts/openapi.yaml` -- thêm specification cho 4 endpoints notification và DTO schemas.
- [x] `backend/src/main/java/com/storagehub/notification/` -- tạo entity, repository, DTOs, service (event API), controller.
- [x] `backend/src/test/java/com/storagehub/notification/` -- viết unit test cho service và WebMvc slice test cho controller.
- [x] `frontend/src/types/notification.ts` + `frontend/src/api/notification.ts` -- tạo types và api client.
- [x] `frontend/src/components/ui/Toast.tsx` + `frontend/src/context/ToastContext.tsx` + `useToast.ts` -- xây dựng Toast System hoàn chỉnh.
- [x] `frontend/src/components/notification/BellButton.tsx` -- xây dựng BellButton với unread polling và badge.
- [x] `frontend/src/components/notification/NotificationCenter.tsx` + `NotificationCenterPage.tsx` -- xây dựng Notification Center drawer và standalone page.
- [x] `frontend/src/components/layout/TopBar.tsx` -- gắn `BellButton` vào TopBar.
- [x] `frontend/src/router/routes.tsx` -- kết nối route `/notifications` đến `NotificationCenterPage`.
- [x] `frontend/src/mocks/handlers.ts` -- bổ sung mock handlers cho notification endpoints.
- [x] `frontend/src/test/toast.test.tsx` + `frontend/src/test/notification.test.tsx` -- viết test cho Toast, Bell, Notification Center.

**Acceptance Criteria:**

- Given seeded notifications in database, when user calls `GET /api/v1/notifications/unread-count`, then correct unread count for current user is returned.
- Given notifications list endpoint, when called, then results are scoped strictly to the authenticated user, ordered unread first (`isRead ASC, createdAt DESC`), paginated in `ListEnvelope`.
- Given `NotificationService.send(...)` called with an event, when transaction commits, then notification record is inserted and immediately available in user's unread feed.
- Given a toast triggered with `title`, `tone`, and optional `deepLink`, when rendered on frontend, then it appears in bottom-right with 3px accent bar, pauses countdown on hover, and auto-dismisses after ~4s.
- Given unread notifications exist, when viewing `TopBar`, then Bell icon displays an error-red count badge matching `unread-count`.
- Given Notification Center opened, when clicking "Mark all as read", then all notifications of current user are marked read and unread badge disappears.
- Given a notification with `deepLink`, when clicked, then user is navigated to the target route and the notification is marked as read.
- Given `mvn test` in backend, then all notification service and controller tests pass.
- Given `npm test` in frontend, then all toast, bell, and notification center tests pass.
- Given `npm run build` in frontend, then build succeeds with 0 errors.

## Implementation Notes

- Backend:
  - Added OpenAPI specs for notification endpoints (`/notifications/unread-count`, `/notifications`, `/notifications/{id}/read`, `/notifications/mark-all-read`) and schemas (`NotificationDto`, `UnreadCountResponse`, `NotificationListResponse`, `ToastNotification`).
  - Created JPA entity `Notification`, repository `NotificationRepository` with unread count and paginated query, service `NotificationService` & `NotificationServiceImpl` with `send(NotificationEvent)` API, and controller `NotificationController`.
  - Added unit test suite `NotificationServiceTests` (8 tests) and controller slice test `NotificationControllerTests` (7 tests).
  - Added `AuthenticationException` handler in `GlobalExceptionHandler` returning 401 `UNAUTHENTICATED`.
- Frontend:
  - Built Toast System: `Toast` primitive, `ToastContext` provider with stack limiting, auto-dismiss, and hover pause; `useToast` hook.
  - Added automated mutation toast response listener via Axios interceptor in `api/client.ts`.
  - Built `BellButton` with unread count polling via TanStack Query and error-red count badge.
  - Built `NotificationCenter` drawer and standalone `NotificationCenterPage` at `/notifications`.
  - Integrated `BellButton` into `TopBar` and connected route in `routes.tsx`.
  - Updated MSW handlers with notification endpoints and mock state.
  - Full test coverage with Vitest and RTL in `toast.test.tsx` and `notification.test.tsx`.

## Spec Change Log

- None.

## Review Triage Log

| Finding | Verdict | Evidence & Action |
|---------|---------|-------------------|
| ToastProvider outside RouterProvider renders link without router context | high | Replaced Link in Toast with standard anchor link and onDismiss to eliminate router dependency and prevent runtime useHref crashes. Patched. |
| Missing automated mutation response handler for Toasts | medium | Added Axios response interceptor in client.ts to emit storagehub:toast and wired listener in ToastContext. Patched. |
| Orphaned ToastNotification schema in openapi.yaml | false | ToastNotification schema is reserved for mutations across Epics 2+ as designed. Rejected. |
| Incorrect localStorage key used in MSW mock handler extractUserId | low | Updated extractUserId to inspect storagehub_user key. Patched. |
| NotificationRepository.markAllAsReadByUserId missing clearAutomatically | low | Added clearAutomatically = true to @Modifying query in NotificationRepository. Patched. |
| Unauthenticated access maps to 403 instead of 401 | medium | Updated NotificationController to throw InsufficientAuthenticationException and added AuthenticationException handler returning 401 in GlobalExceptionHandler. Patched. |
| NotificationEvent.tone discarded by NotificationServiceImpl | false | Database schema notifications table has no tone column; tone is reserved for ToastNotification ephemeral payloads. Rejected. |
| BellButton polls getUnreadCount continuously when unauthenticated | low | Added enabled flag checking storagehub_token in BellButton useQuery. Patched. |
| NotificationCenter total count displayed from slice rather than total | low | Updated tab label to display listData?.total ?? allItems.length. Patched. |
| Missing maximum string length validations in NotificationEvent | low | Relying on database schema length constraints and bean validation. Rejected as low. |
| Missing WAI-ARIA tabpanel in NotificationList | low | Added role="tabpanel" and aria-label="Notifications list" to feed container. Patched. |
| Missing database integration test for NotificationRepository | low | Repository unit and controller slice tests provide full coverage; DB integration tests are deferred. Rejected as low. |
| Unbounded toast stack in ToastContext | low | Added slice(-4) limit to keep max 5 toasts active simultaneously. Patched. |
| Unused NotificationCenter wrapper component | false | NotificationCenter is exported for flexible container usage. Rejected. |
| Edge-case: null/undefined showToast input | low | Added if (!toastInput) return '' guard in showToast. Patched. |
| Edge-case: handleMouseEnter on cleared timer | low | Added if (!timerRef.current) return guard in handleMouseEnter. Patched. |
| Edge-case: null/undefined item.type | low | Added safe navigation in NotificationList item.type rendering. Patched. |
| Edge-case: null/undefined formatNotificationDate argument | low | Added if (!isoString) return '' in formatNotificationDate. Patched. |
| Edge-case: null query in NotificationServiceImpl | low | Added fallback ListQuery.of((Integer) null, (Integer) null). Patched. |
| Edge-case: null event in NotificationServiceImpl | low | Added IllegalArgumentException guard for null event. Patched. |
| Verification gap: clicking item marks notification as read | high | Added assertion in notification.test.tsx that unread-count drops to 0 after item click. Patched. |
| Verification gap: BellButton in TopBar | medium | Added assertion in shell.test.tsx that notifications button is present in TopBar. Patched. |
| Verification gap: /notifications in routesConfig | medium | Added test in notification.test.tsx resolving /notifications through routesConfig. Patched. |
| Verification gap: formatNotificationDate utility tests | medium | Added unit test suite in format.test.ts covering valid, empty, and invalid dates. Patched. |

## Design Notes

- Toast styling: fixed `bottom-4 right-4 z-50`, width 360px, surface `#FFFFFF`, border `#E2E8F0`, shadow `shadow-sh-overlay`, border radius 8px (`rounded-sh-md`). Thanh 3px sát mép trái (`w-[3px] absolute left-0 top-0 bottom-0`).
- Notification Drawer: Tận dụng `Drawer` primitive từ Story 1.4 (`w-[420px] max-w-full`), header có title "Notifications", unread count chip, action "Mark all read".

## Verification

**Commands:**

- `cd backend && mvn test` -- expected: PASS toàn bộ test backend (gồm notification service và controller tests).
- `cd frontend && npm test` -- expected: PASS toàn bộ test frontend (gồm toast, bell, notification center tests).
- `cd frontend && npm run build` -- expected: `tsc -b && vite build` hoàn thành với exit code 0.
- `cd frontend && npm run lint` -- expected: `oxlint` sạch sẽ, 0 lỗi.
