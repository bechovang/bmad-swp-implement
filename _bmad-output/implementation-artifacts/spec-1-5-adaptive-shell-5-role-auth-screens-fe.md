---
title: 'Story 1.5 — Adaptive shell for 5 roles + auth screens FE'
type: 'feature'
created: '2026-10-02'
baseline_commit: '75d27b60e2152d3fbcde0463cf0e70cff458d344'
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

**Problem:** Frontend hiện tại chỉ có các base primitives và token system (từ Story 1.4), nhưng chưa có router, chưa có authentication state & context, chưa có các màn hình auth (Login, Customer Register, Forgot Password modal), và chưa có Adaptive Shell (5 role navigation, RoleChip, avatar menu, route protection, 403/404 handling) — người dùng chưa thể đăng nhập, đăng ký và trải nghiệm phân quyền theo 5 vai trò.

**Approach:** Cài đặt `react-router-dom` và xây dựng hệ thống định tuyến đồng bộ 100% với `contracts/routes.yaml`; thiết lập `AuthContext` + Axios interceptors quản lý JWT Bearer token và user session; cài đặt các màn hình auth chuẩn UX Control Room (`LoginPage`, `RegisterPage` customer-only không có role picker, `ForgotPasswordModal` trả lời generic); xây dựng `AdaptiveShell` (top bar 54px, Logo, dynamic per-role nav cho 5 vai trò, RoleChip, Avatar menu có Profile và Logout, Bell placeholder); xây dựng route guard (redirect unauthenticated sang `/login`, chặn cross-role URLs với 403 state và CTA, 404 catch-all); mock handlers MSW và verify toàn diện bằng Vitest + RTL.

## Boundaries & Constraints

**Always:**
- Bám sát `contracts/routes.yaml` và `contracts/openapi.yaml` verbatim:
  - Public routes: `/login`, `/register`, `/forgot-password`.
  - Role landings: `CUSTOMER` → `/units`, `STAFF` → `/tasks`, `FACILITY_MANAGER` → `/overview`, `BUSINESS_OPS` → `/business-overview`, `SYSTEM_ADMINISTRATOR` → `/users`.
  - Dynamic navigation per role:
    - Customer: Browse Units (`/units`), My Rentals (`/rentals`), Support (`/support`).
    - Staff: Task Board (`/tasks`), Support (`/support`).
    - Facility Manager: Facility Overview (`/overview`), Units (`/units`), Staff & Shifts (`/staffing`), Operations (`/operations`), Activity Log (`/activity-log`), Escalations (`/escalations`).
    - Business Ops: Business Overview (`/business-overview`), Policy (`/policy`), Reports (`/reports`).
    - System Administrator: User Management (`/users`), Login History (`/login-history`).
  - Cross-role common routes: `/profile` (Profile details + Logout), `/notifications` (placeholder).
- Login: Thất bại (401) chỉ hiển thị đúng MỘT generic inline error: `"The email or password is not correct."`, kèm một next step (thử lại hoặc quên mật khẩu); tuyệt đối không tiết lộ field nào sai.
- Register: Chỉ dành cho Customer (Customer-only). Form có các trường `fullName`, `phone`, `email`, `password`, `confirmPassword`, `agreeToTerms` checkbox. Tuyệt đối KHÔNG có role picker; payload gửi đi không chứa role; đăng ký thành công tự động đăng nhập và redirect đến `/units`.
- Forgot Password: Mở dạng Modal từ Login (hoặc qua route `/forgot-password`), luôn phản hồi generic: `"If an account exists with that email, a password reset link has been sent."` bất kể email có tồn tại hay không.
- Session & Token: Lưu JWT và User info trong `localStorage` (`storagehub_token`, `storagehub_user`); `apiClient` tự động đính kèm `Authorization: Bearer <token>`; nếu nhận 401 thì tự động clear session và chuyển về `/login`.
- Security & Guards: Truy cập route trái vai trò (cross-role) lập tức hiển thị màn hình 403 Forbidden với nút CTA quay về landing page của vai trò hiện tại; truy cập route khi chưa đăng nhập redirect về `/login?redirect=...`.
- UI Control Room: Sử dụng đúng token và primitives từ Story 1.4 (`Button`, `Input`, `Card`, `Badge`, `RoleChip`, `Modal`); top bar cao 54px, nền trắng `#FFFFFF`, viền `#E2E8F0`, padding 24px; tiếng Anh chuẩn (English-only), light mode only.

**Never:**
- Không có role selection trên trang Register.
- Không gửi token qua URL query params.
- Không hardcode danh sách roles khác với 5 vai trò hệ thống (`CUSTOMER`, `STAFF`, `FACILITY_MANAGER`, `BUSINESS_OPS`, `SYSTEM_ADMINISTRATOR`).
- Không dùng alert/confirm native của trình duyệt.
- Không cho phép truy cập trái vai trò mà không hiển thị 403.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Login thành công | Email/password hợp lệ (vd: `lan@storagehub.dev` / `Demo1234!`) | Lưu token, set AuthUser (role CUSTOMER), redirect về `/units` | — |
| Login sai credentials | Email hoặc password sai (401 từ API) | Hiển thị inline error `"The email or password is not correct."`, không đổi trang | Giữ nguyên email, focus input |
| Register thành công | Dữ liệu hợp lệ, agreeToTerms=true | 201 Created, tự động đăng nhập, redirect về `/units` | — |
| Register trùng email | Email đã tồn tại (400 từ API) | Inline error hiển thị ngay dưới trường Email: `"Email is already registered."` | Focus trường email |
| Register sai mật khẩu xác nhận | password != confirmPassword | Inline error dưới confirmPassword: `"Passwords do not match."`, chặn submit | Không gửi request |
| Register chưa đồng ý điều khoản | agreeToTerms=false | Inline error: `"You must agree to the terms and privacy policy."` | Chặn submit |
| Forgot password submit | Nhập email bất kỳ | Modal hiển thị generic success message `"If an account exists with that email, a password reset link has been sent."` | Không leak account existence |
| Chưa login truy cập `/units` | Token = null | Redirect về `/login?redirect=/units` | Lưu intended redirect |
| Login sau khi bị redirect | Đăng nhập thành công từ `/login?redirect=/units` | Redirect về `/units` (intended path) thay vì default landing | Xóa redirect param |
| Đã login truy cập `/login` | Token != null, role CUSTOMER | Tự động chuyển hướng về `/units` (landing page của role) | Không hiển thị form login |
| Truy cập trái role | Role CUSTOMER truy cập `/users` (System Admin) | Render màn hình 403 Forbidden với message rõ ràng + CTA `"Return to Browse Units"` | Chặn hoàn toàn nội dung admin |
| Đổi vai trò nav | Role STAFF đăng nhập | Top bar hiển thị đúng 2 mục nav (`Tasks`, `Support`), RoleChip `"Staff"` | Không thấy menu của Admin/Manager |
| Logout | Click "Log out" trong Avatar Menu | Xóa token/user khỏi localStorage, reset AuthContext, chuyển về `/login` | — |
| Token hết hạn / 401 khi gọi API | API trả về 401 Unauthorized | Axios interceptor xóa session, redirect về `/login` | Hiển thị thông báo phiên hết hạn |
| Route không tồn tại | Truy cập `/random-invalid-path` | Render màn hình 404 Not Found với CTA `"Go to Home"` | — |

</frozen-after-approval>

## Code Map

- `frontend/package.json` -- thêm dependency `react-router-dom`.
- `contracts/routes.yaml` -- đặc tả danh sách route, role landing và nav menu.
- `contracts/openapi.yaml` -- đặc tả API auth và DTO schemas (`LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`, `ForgotPasswordRequest`, `ForgotPasswordResponse`, `AuthUser`, `Role`).
- `frontend/src/api/client.ts` -- cấu hình Axios interceptors (Bearer token injection, 401 unauthorized handling).
- `frontend/src/api/auth.ts` -- API functions (`loginApi`, `registerApi`, `forgotPasswordApi`).
- `frontend/src/context/AuthContext.tsx` -- Context & Provider quản lý auth state (`user`, `token`, `login`, `register`, `logout`, `isAuthenticated`).
- `frontend/src/hooks/useAuth.ts` -- hook truy cập AuthContext tiện lợi.
- `frontend/src/components/layout/TopBar.tsx` -- thanh top bar 54px: Logo, per-role nav links, RoleChip, Bell icon placeholder, Avatar menu.
- `frontend/src/components/layout/AdaptiveShell.tsx` -- layout shell bao bọc các protected routes, chứa TopBar và content container.
- `frontend/src/components/layout/ProtectedRoute.tsx` -- route guard kiểm tra authentication và authorization theo roles.
- `frontend/src/pages/auth/LoginPage.tsx` -- màn hình Login với form email/password, generic error inline, link Register, modal Forgot Password.
- `frontend/src/pages/auth/RegisterPage.tsx` -- màn hình Register (customer-only, validation fields, agreeToTerms, redirect sau khi tạo tài khoản).
- `frontend/src/pages/auth/ForgotPasswordModal.tsx` -- modal quên mật khẩu với generic notification.
- `frontend/src/pages/error/ForbiddenPage.tsx` -- màn hình 403 Forbidden chuẩn Control Room với nút CTA.
- `frontend/src/pages/error/NotFoundPage.tsx` -- màn hình 404 Not Found chuẩn Control Room với nút CTA.
- `frontend/src/pages/profile/ProfilePage.tsx` -- màn hình Profile hiển thị thông tin tài khoản và nút Logout.
- `frontend/src/pages/placeholders/` -- placeholder screens cho các role views (`BrowseUnitsPage`, `MyRentalsPage`, `TaskBoardPage`, `FacilityOverviewPage`, `BusinessOverviewPage`, `UserManagementPage`, `SupportPage`, etc.).
- `frontend/src/router/routes.tsx` -- cấu hình React Router với toàn bộ public và protected routes.
- `frontend/src/mocks/handlers.ts` -- bổ sung MSW handlers cho `/api/v1/auth/login`, `/register`, `/forgot-password`.
- `frontend/src/test/auth.test.tsx` -- RTL tests cho login, register, validation, forgot password.
- `frontend/src/test/shell.test.tsx` -- RTL tests cho adaptive shell navigation theo 5 vai trò, route guard 403/404, logout.

## Tasks & Acceptance

**Execution:**

- [x] `frontend/package.json` -- cài đặt `react-router-dom`.
- [x] `frontend/src/types/auth.ts` -- định nghĩa TypeScript types khớp `openapi.yaml` (`Role`, `AuthUser`, `LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`, `ForgotPasswordRequest`).
- [x] `frontend/src/api/client.ts` -- thêm request interceptor (Authorization: Bearer token) và response interceptor (xử lý 401).
- [x] `frontend/src/api/auth.ts` -- cài đặt các hàm gọi API auth (`loginApi`, `registerApi`, `forgotPasswordApi`).
- [x] `frontend/src/context/AuthContext.tsx` + `frontend/src/hooks/useAuth.ts` -- tạo AuthProvider với persistent session trong localStorage.
- [x] `frontend/src/components/layout/TopBar.tsx` -- tạo TopBar với Logo, per-role nav links, RoleChip, avatar dropdown menu (Profile, Logout).
- [x] `frontend/src/components/layout/AdaptiveShell.tsx` -- tạo layout shell bao bọc protected routes.
- [x] `frontend/src/components/layout/ProtectedRoute.tsx` -- tạo guard kiểm tra authentication (redirect `/login`) và authorization (chặn 403).
- [x] `frontend/src/pages/auth/ForgotPasswordModal.tsx` -- tạo Modal quên mật khẩu với generic message.
- [x] `frontend/src/pages/auth/LoginPage.tsx` -- tạo màn hình Login (email, password, generic error 401, toggle Forgot Password modal, link Register).
- [x] `frontend/src/pages/auth/RegisterPage.tsx` -- tạo màn hình Register (customer-only, fullName, phone, email, password, confirmPassword, agreeToTerms, auto-login).
- [x] `frontend/src/pages/error/ForbiddenPage.tsx` + `NotFoundPage.tsx` -- tạo màn hình 403 và 404 với nút CTA quay về landing page.
- [x] `frontend/src/pages/profile/ProfilePage.tsx` -- tạo trang Profile hiển thị thông tin user và nút logout.
- [x] `frontend/src/pages/placeholders/` -- tạo các placeholder screens theo đúng `routes.yaml` cho 5 vai trò.
- [x] `frontend/src/router/routes.tsx` + `frontend/src/App.tsx` -- thiết lập RouterProvider kết nối đầy đủ routes.
- [x] `frontend/src/mocks/handlers.ts` -- bổ sung MSW handlers cho auth endpoints.
- [x] `frontend/src/test/auth.test.tsx` + `frontend/src/test/shell.test.tsx` -- viết và chạy toàn bộ unit & component tests.

**Acceptance Criteria:**

- Given an unauthenticated user, when navigating to `/units` or any protected route, then the application redirects to `/login?redirect=...`.
- Given valid credentials (e.g. `lan@storagehub.dev` / `Demo1234!`), when submitting Login, then token and user are stored in localStorage and user is redirected to role landing (`/units` for Customer).
- Given invalid credentials (401), when submitting Login, then a single generic inline error `"The email or password is not correct."` is displayed, without revealing which field was wrong.
- Given customer register form, when submitted with valid fields and agreeToTerms=true, then account is created, user is automatically logged in, and navigated to `/units`.
- Given customer register form with mismatched passwords, when submitted, then validation stops with inline error on confirmPassword without network request.
- Given customer register form, then no role selector or role input exists on the page or in the payload.
- Given forgot password modal, when any email is submitted, then a generic confirmation message is shown without revealing account existence.
- Given a logged-in user with role `CUSTOMER`, when navigating to `/users` (System Administrator route), then a 403 Forbidden screen is rendered with a CTA button to return to `/units`.
- Given a logged-in user with role `STAFF`, when viewing TopBar, then only Staff navigation items (`Tasks`, `Support`) and RoleChip `"Staff"` are rendered.
- Given a logged-in user, when clicking "Log out" in the avatar menu, then auth state is cleared and user is redirected to `/login`.
- Given an invalid route (e.g. `/unknown-path`), then a 404 Not Found screen is rendered with a CTA button.
- Given `npm test` inside `frontend/`, then all tests pass without errors.
- Given `npm run build` inside `frontend/`, then TypeScript check and Vite build succeed with zero errors.

## Implementation Notes

- Added `react-router-dom` and configured complete routing tree per `contracts/routes.yaml`.
- Built typed `AuthContext` and `apiClient` interceptors with JWT Bearer injection, 401 session clearance, and cross-tab storage synchronization.
- Created `AdaptiveShell` with 54px `TopBar` (Logo, per-role dynamic nav, `RoleChip`, Bell placeholder, Avatar menu with Profile link and Logout).
- Implemented `LoginPage` with generic 401 inline error, redirect preservation/sanitization, and expired session banner.
- Implemented `RegisterPage` customer-only (no role picker, field validations, auto-login redirect).
- Implemented `ForgotPasswordModal` and `ForgotPasswordPage` returning generic acknowledgment without account leaking.
- Implemented `ProtectedRoute` enforcing role authorization and 403 Forbidden page with role landing CTA.
- Created placeholder pages for all routes across the 5 system roles.
- Expanded MSW mock handlers and test suite (80/80 tests passing).

## Spec Change Log

## Review Triage Log

- `frontend/src/api/client.ts:14-20` (Verification Gap) -- [medium] Missing test for Bearer token injection. Added test in `auth.test.tsx`. (patch)
- `frontend/src/api/client.ts:23-40` (Verification Gap) -- [medium] Missing test for 401 response session clearing. Added test in `auth.test.tsx`. (patch)
- `frontend/src/pages/error/ForbiddenPage.tsx:11-14` -- [low] Unauthenticated visitor to /403 saw misleading CTA. Fixed to display "Return to Sign In". (patch)
- `frontend/src/pages/auth/LoginPage.tsx` (Spec matrix gap) -- [low] Session expired notice missing on LoginPage. Added `?expired=true` support and alert banner. (patch)
- `frontend/src/pages/auth/LoginPage.tsx:25-30` -- [medium] Unsanitized redirect param. Added `sanitizeRedirect` helper blocking loop / open redirects. (patch)
- `frontend/src/pages/auth/ForgotPasswordModal.tsx:21-26` -- [low] Missing timer cleanup on modal close. Added ref cleanup on unmount. (patch)
- `frontend/src/pages/auth/RegisterPage.tsx:72-92` -- [medium] Missing inline error bindings on fullName, phone, password. Bound error prop to all fields. (patch)
- `frontend/src/App.tsx:9-10` -- [medium] Router recreated on render. Extracted defaultRouter as module-level singleton. (patch)
- `frontend/src/components/layout/TopBar.tsx:44` -- [low] Whitespace fullName initial. Added trim check with 'U' fallback. (patch)
- `frontend/src/api/client.ts:32-33` -- [low] Potential window ReferenceError. Added typeof window check. (patch)
- `frontend/src/api/client.ts:29` -- [low] 401 exemption missing /auth/ paths. Updated to `!requestUrl.includes('/auth/')`. (patch)
- `frontend/src/context/auth-context-base.ts` -- [medium] Expired JWT restored on startup. Added `isJwtExpired` helper and cleared expired sessions on load. (patch)
- `frontend/src/context/AuthContext.tsx:45-60` -- [medium] Auto-login failure after registration. Handled with redirect to /login and informational message. (patch)
- `frontend/src/context/AuthContext.tsx` -- [low] Cross-tab session sync missing. Added storage event listener. (patch)
- `frontend/src/pages/auth/ForgotPasswordPage.tsx` -- [low] Authenticated user on forgot password. Added redirect to role landing. (patch)
- `frontend/src/components/layout/TopBar.tsx:121` -- [low] Dropdown shadow token. Updated to `shadow-sh-overlay`. (patch)
- `frontend/src/components/layout/TopBar.tsx` -- [low] Facility manager 6-item nav overflow. Added `overflow-x-auto`. (patch)
- `frontend/src/test/shell.test.tsx` -- [medium] Parameterized detail routes untested. Added test cases in `shell.test.tsx`. (patch)
- `frontend/src/index.css:30-34` -- [medium] Stray main 960px global style. Removed to support 1200px Control Room shell. (patch)
- `frontend/src/pages/auth/RegisterPage.tsx` a11y -- [low] Checkbox accessibility attributes. Added id, aria-invalid, aria-describedby. (patch)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` -- [false] Intermediate status during review step. (rejected)

## Design Notes

- Top bar: Chiều cao 54px (`h-sh-nav-height`), background white `#FFFFFF`, border-b `#E2E8F0`. Bố cục flex items-center justify-between với padding x 24px (`px-sh-page-x`).
- Nav active indicator: Sử dụng `NavLink` của `react-router-dom` với class `text-sh-primary font-semibold relative after:absolute after:bottom-[-16px] after:left-0 after:right-0 after:h-[2px] after:bg-sh-primary` để tạo đường gạch dưới 2px indigo chuẩn Control Room.
- Avatar menu: Hiển thị avatar tròn với chữ cái đầu của tên người dùng, click mở dropdown popover với tên, email, vai trò, link Profile và nút Log out (variant destructive hoặc secondary text error).

## Verification

**Commands:**

- `cd frontend && npm test` -- expected: PASS toàn bộ test Vitest (auth tests, shell tests, primitive tests, format tests).
- `cd frontend && npm run build` -- expected: `tsc -b && vite build` hoàn thành với exit code 0, 0 lỗi TypeScript.
- `cd frontend && npm run lint` -- expected: `oxlint` sạch sẽ, 0 lỗi.
