# Reconcile — EXPERIENCE.md

*Đối chiếu `docs/planning/ux-designs/ux-swp391-2026-09-11/EXPERIENCE.md` (hợp đồng hành vi UX) với PRD này (`prd.md`, 2026-09-17). Quy tắc §0: EXPERIENCE thắng về chi tiết tương tác, PRD thắng về phạm vi — nên báo cáo này chỉ nêu hành vi bị **rơi khỏi phạm vi yêu cầu hoàn toàn** hoặc **bị mâu thuẫn**, không nêu khác biệt mức chi tiết. Các quyết định có chủ đích (27 màn frozen, Cleaning hoàn tất từ card, refund display-only, Buffer + Deposit % là Policy Rule, bỏ reviewer) không được flag.*

## Verdict

PRD hấp thụ gần như trọn hợp đồng hành vi của EXPERIENCE.md vào 34 FR + 6 NFR (các nhịp nghiệp vụ cốt lõi đều có FR tương ứng); phần còn thiếu tập trung ở các **rule ngang mang tính định tính** (voice/microcopy, empty/loading state, form discipline) và một **mâu thuẫn nội bộ về mức ưu tiên KPI drill-down** — không có gap nào ở mức critical.

## Gaps

1. **Hợp đồng Voice & Tone / microcopy không có NFR tương ứng.**
   - EXPERIENCE.md ("Voice and Tone" + Anti-patterns "Dead-end errors", "Role-blind chrome"): mọi error/toast nêu *chuyện gì xảy ra, tổn thất bao nhiêu (thường là không), bước kế là gì*; register plain như nhau cho cả 4 role ("seniority is not a tone of voice"); cấm exclamation mark, streak-cheer, marketing verbs; tiền luôn full VND.
   - PRD: NFR-1 chỉ chốt ngôn ngữ + định dạng tiền/ngày. Tone chỉ xuất hiện rải rác dưới dạng instance trong từng FR (FR-8 "No money was taken", FR-29 banner nêu luật, FR-1 "nêu rõ bước kế tiếp") — không có rule chung, nên story/QA viết message sẽ tự bịa tone.
   - **Severity: medium.**
   - Fix: thêm NFR-7 (Voice & microcopy): mọi error/empty/toast nêu nguyên nhân + hệ quả tiền/trạng thái + đúng một bước kế; register thống nhất cho 4 role; cấm marketing tone toàn app.

2. **Mức ưu tiên KPI drill-down mâu thuẫn giữa FR-31/32 và §7.2.**
   - EXPERIENCE.md ("Component Patterns → Dashboards (KPI drill-down)"): *mọi* KPI trên cả ba dashboard đều click xuyên xuống bảng lọc đúng slice — "The dashboard never shows a number the user cannot reach the rows behind."
   - PRD: FR-31/32 đưa drill-down vào phần P1 ("P1 dashboards", consequence occupancy 87% → Unit Management lọc Rented), nhưng §7.2 [NOTE FOR PM] lại gọi "KPI drill-through toàn bộ dashboard là **P2** đáng nâng cấp đầu tiên" — đọc theo note thì drill-through có thể bị cắt khi gộp thời gian, trái với FR-31/32 và với cam kết "mọi số liệu với tới được rows" trong JTBD Business Ops.
   - **Severity: medium.**
   - Fix: diễn giải lại note §7.2 — drill-down trên Business Overview + Facility Overview là P1 (theo FR-31/32); chỉ phần mở rộng "toàn bộ" (Operations Monitor đang P2 + drill ở cấp chart) mới là P2.

3. **Empty / loading state contract bị bỏ hoàn toàn.**
   - EXPERIENCE.md ("Component Patterns → Empty states, Skeletons" + "State Patterns → Empty / loading"): skeleton khớp layout, swap không layout shift; empty state factual + chip filter echo + đúng 1 CTA ("Always offers the next move"); first-run screen link tới flow đổ dữ liệu (Browse Units / New Support); cột kanban trống có tile "Nothing here".
   - PRD: không FR/NFR nào nhắc empty/loading state (NFR-5 chỉ nói trang load < 2s). Đây là nửa còn lại của lời hứa "never a dead end" (nửa error đã phủ ở từng FR).
   - **Severity: low.**
   - Fix: mở rộng NFR-5 (hoặc thêm NFR): mọi màn danh sách/dashboard có skeleton không layout shift và empty state 1 CTA trỏ bước kế tiếp hợp lệ.

4. **Form validation contract tổng quát (banner-not-toast, submit-disable) không được khái quát.**
   - EXPERIENCE.md ("State Patterns → Form validation"): inline error on blur; block phía server hiển thị banner đầu form **không phải toast** "vì user phải đọc luật để sửa giá trị"; Submit chỉ disable khi thiếu required field, không bao giờ disable vì soft warning.
   - PRD: chỉ có instance rời (FR-29 banner policy, FR-1 inline error login); rule ngang cho mọi form không có.
   - **Severity: low.**
   - Fix: thêm 1 dòng NFR: server-block hiển thị banner đầu form (không toast); submit chỉ disable theo missing required fields.

5. **Logout / avatar menu không nằm trong FR nào.**
   - EXPERIENCE.md ("Foundation"): top bar mang avatar menu (profile, logout) cho mọi role.
   - PRD: FR-1–3 cover login/register/forgot; không FR nào định nghĩa logout hay avatar menu — epics/stories sinh từ PRD sẽ thiếu việc này.
   - **Severity: low.**
   - Fix: thêm consequence FR-1: avatar menu (profile, logout) có ở mọi role; logout đưa về Login.

6. **Pagination bảng quản trị (cấm infinite scroll) bị rơi.**
   - EXPERIENCE.md ("Interaction Primitives → Banned"): infinite scroll trên management tables bị cấm — pagination 25 rows.
   - PRD: FR-26 (Unit Management), FR-28 (Activity Log), FR-31 (Reports) không chốt cửa sổ dữ liệu nào.
   - **Severity: low.**
   - Fix: thêm consequence chung FR-26/28/31: bảng quản trị dùng pagination 25 rows, không infinite scroll.

7. **Unsaved-changes guard cho inline edit bị rơi.**
   - EXPERIENCE.md ("Interaction Primitives → Inline edit"): click-to-edit, blur/Enter save, Esc cancel, **unsaved-changes guard khi rời màn**.
   - PRD: FR-29 chỉ nói inline edit + Save chạy validation; FR-27 (staff assignment) không nêu guard — mất dữ liệu cell đang sửa khi navigate không được chặn.
   - **Severity: low.**
   - Fix: thêm consequence FR-29/FR-27: cảnh báo khi rời trang còn cell đang sửa chưa commit.

8. **Focus trap + return focus trong modal/drawer thiếu ở NFR-2.**
   - EXPERIENCE.md ("Accessibility Floor"): focus bị trap trong modal/drawer đang mở và trả về trigger khi đóng.
   - PRD: NFR-2 có label/focus visible/keyboard/≥40px/không color-only nhưng thiếu 2 hành vi trap + return này.
   - **Severity: low.**
   - Fix: bổ sung 1 dòng vào NFR-2.

9. **Filter re-query discipline + session persistence không chốt.**
   - EXPERIENCE.md ("Component Patterns → Filter bar"): đổi filter chỉ re-query trên nút Search (không per keystroke); active filter persist theo session và echo thành chip removable ở empty state.
   - PRD: FR-4 có filter bar + sort + count chip "live" nhưng không nói query trigger khi nào hay filter có persist không.
   - **Severity: low.**
   - Fix: thêm consequence FR-4: query chỉ chạy trên Search; filter persist trong phiên và echo ở empty state.

10. **Khóa điều hướng giữa Payment Processing bị rơi.**
    - EXPERIENCE.md ("State Patterns → Payment states" — Processing): spinner ~1.5–2s, button disabled, **"no back navigation mid-charge"** — chống bỏ cuộc giữa charging.
    - PRD: FR-8 nêu processing ~1.5–2s nhưng không khóa back/close modal trong trạng thái này.
    - **Severity: low.**
    - Fix: thêm consequence FR-8: trong Processing, modal không đóng/điều hướng được.

11. **Notification mapping thiếu role Business Ops.**
    - EXPERIENCE.md ("Component Patterns → Notification Center"): role-scoped — business ops nhận policy saves + report completions.
    - PRD: FR-34 consequences liệt kê event mapping cho customer/staff/manager nhưng bỏ mất business ops.
    - **Severity: low.**
    - Fix: bổ sung consequence FR-34: business ops nhận policy save + report export completion.

## Covered highlights

- **Các nhịp hành vi lõi đều có FR:** availability + buffer + stale re-check (FR-4/5), payment 3 touchpoint × 5 states + retry + QR expiry (FR-8/9), contract ritual + access-code lock + `CONTRACT_SIGNED` (FR-10/11), addendum 7 ngày + hiệu lực ngay + không hold Unit (FR-12/13 + Glossary), check-in validate + thu 100% rent (FR-14/15), extension conflict boundary (FR-16), checkout inspection + charge reason bắt buộc + settlement preview (FR-17/18), cleaning từ card + buffer check (FR-19), kanban 5 loại + snap-back + keyboard Move + Done muted (FR-21/22), support route + escalate note bắt buộc + severity + relocation (FR-23–25), merge/retire guard + fix-status reason + không silent write (FR-20/26, NFR-6), shift conflict trước save (FR-27), Activity Log append-only (FR-28), policy validate-trước-khi-lưu + version stamp + Buffer/Deposit % là Policy Rule (FR-29/30), KPI drill-down (FR-31/32 — trừ mâu thuẫn ưu tiên ở Gap 2), toast + bell + feed role-scoped + unread persist (FR-33/34).
- **Anti-patterns khớp:** no email, no calendar/floor-map, no silent writes, không money move thiếu line-item breakdown, không access code khi thiếu ảnh ký, status không color-only (NFR-2).
- **UJ-1…UJ-5** map đủ 5 Key Flows của EXPERIENCE.md, giữ cả nhân vật, climax và edge case (payment fail không đổi state, stale reserve bounce + toast, snap-back, merge bị chặn nêu tên reservation, ticket không severe quay lại staff kèm hướng dẫn).
- **Floor định tính khớp:** VND full precision + tabular numerals + English UI (NFR-1), a11y floor gồm kanban keyboard (NFR-2), light mode + desktop-first 1200px (NFR-3), server-side role enforcement (NFR-4).
- **Các defer có chủ đích được tôn trọng, không flag:** 27 màn frozen (§7.1), responsive tablet/mobile polish P2, forgot-password chỉ phản hồi UI generic, CSV export P2, Operations Monitor P2, Notification day-grouping P2, refund display-only, Cleaning hoàn tất từ card.
