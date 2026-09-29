# StorageHub Mockups — Index & Notes

Sinh tự động bằng Google Stitch MCP (project `9794980915577530548`, design system từ `DESIGN.md`).
Ngày: 2026-09-11 · 27 màn · hướng "Control Room".
Hai spine `../DESIGN.md` + `../EXPERIENCE.md` là hợp đồng thắng mọi mock khi xung đột.

> **Lệch số liệu 2026-09-22 (quyết định deposit top-up — FR-16):** mock HTML không regen. Build theo số mới, không theo số trong mock:
> - F6-01 extend: bỏ câu "Your deposit stays held until final turnover checkout" — extension fee giờ = rent 690.000 ₫ **+ deposit top-up 69.000 ₫** (cọc giữ 103.500 → 172.500 ₫), Payment Modal liệt kê 2 dòng.
> - F2-04 / F3-01 / F7-05: climax settlement đổi "Refund 63.500 ₫" → **"Refund 132.500 ₫ after damage fee 40.000 ₫"** (cọc sau top-up 172.500 ₫).
> - Số booking (103.500 ₫ cọc, 1.035.000 ₫ rent) không đổi.

## Index (theo flow)

| File | Màn | Vai trò |
|---|---|---|
| F1-01-login | Login (+ forgot-password modal) | Auth |
| F1-02-register | Register | Auth |
| F1-03-browse-units | Browse Units ⭐ flagship | Customer |
| F1-04-unit-detail | Unit Detail (S-3) | Customer |
| F1-05-booking-summary | Booking Summary | Customer |
| F1-06-payment-modal | Payment Modal (3 states: Card/MoMo/VNPay, Success, Fail) | Customer |
| F2-01-task-board | Task Board (kanban) ⭐ staff landing | Staff |
| F2-02-checkin-task | Check-in Task | Staff |
| F2-03-my-rentals | My Rentals | Customer |
| F2-04-rental-detail | Rental Detail | Customer |
| F2-05-checkin-pass | Check-in pass (BK-1042) | Customer |
| F3-01-checkout-task | Checkout Task ⭐ settlement climax | Staff |
| F3-02-checkout-request | Checkout Request | Customer |
| F4-01-business-overview | Business Overview ⭐ dashboard-first | Business Ops |
| F4-02-policy-management | Policy Management ⭐ validation climax | Business Ops |
| F4-03-reports | Reports (Revenue) | Business Ops |
| F5-01-facility-overview | Facility Overview | Facility Manager |
| F5-02-unit-management | Unit Management + edit drawer ⭐ | Facility Manager |
| F5-03-staff-shifts | Staff & Shifts (conflict) | Facility Manager |
| F5-04-operations-monitor | Operations Monitor (live) | Facility Manager |
| F5-05-activity-log | Activity Log (append-only) | Facility Manager |
| F6-01-extend | Extend rental (blocked-range calendar) | Customer |
| F7-01-new-support | New Support Request | Customer |
| F7-02-support-list | Support List + detail drawer | Customer |
| F7-03-support-ticket | Support Ticket | Staff |
| F7-04-escalation-inbox | Escalation Inbox ⭐ severity climax | Facility Manager |
| F7-05-notification-center | Notification Center (bell dropdown) | Customer |

## Hợp đồng (CT-) — cập nhật 2026-09-14

Tính năng hợp đồng thêm vào sau khi chốt với user: **auto-draft hoàn toàn** (từ booking + Rental Policy version),
in tại check-in → KH ký → staff **chụp ảnh upload** → mới cấp access code. **Gia hạn = phụ lục giấy** ký tại quầy
trong 7 ngày (ngày mới hiệu lực ngay sau thanh toán — phụ lục là paperwork, không hold unit). **Checkout không ký**
— settlement receipt chính là biên bản. Chi tiết hợp đồng trong `../EXPERIENCE.md` → Contract lifecycle.

- **F2-02-checkin-task**: Stitch sinh **màn mới** `f49223df…` (thay `cec1ef01…`) — Step 3 "Contract & Signature
  Verification": preview CT-1042 read-only + Print + tile "Capture signed copy" + nút indigo "Attach signed copy";
  Step 4 handover có note "Requires signed contract on file". HTML+PNG mới đã lưu.
- **F2-04-rental-detail**: card "Contracts & Addenda" (CT-1042 Signed xanh + CT-1042-A1 Awaiting signature vàng,
  banner amber deadline) — edit in-place trên canvas; **file HTML export của Stitch không regenerate** nên đã chèn
  đúng DOM block đó vào HTML local. **PNG còn bản cũ** (chỉ là preview).
- **F6-01-extend**: card "After payment" (badge "Signature required at desk", 2 row CT-1042-A1 / new checkout) —
  cùng cơ chế in-place, DOM block chèn vào HTML local, PNG bản cũ. Lần gọi edit đầu timeout, gọi lại thành công.

## Trạng thái: HOÀN TẤT ✅ (verify lại toàn bộ ngày 2026-09-14)

Không màn nào BROKEN — layout, light mode, card trắng, định dạng ₫ đều đúng everywhere.
Đã sửa xong: **F3-01** (settlement 103.500 − 40.000 = 63.500 ✓, sạch label ✓),
**F5-02** (select "Preparing" ✓, "Retire unit" đỏ ✓), **F5-03** (top bar trắng ✓).

Verify 2026-09-14 (quét text toàn bộ 27 màn HTML): các lỗi nhỏ trước đây đều đã sạch —

- **F2-01-task-board**: codes đúng (BK-1042/RT-0871/SR-0032), không còn title gibberish, không còn nút indigo thừa.
- **F4-02-policy-management**: hết brand "StorageFlow" (chỉ còn StorageHub), chỉ còn 1 nút "Save policy".
- **F5-03-staff-shifts**: hết string gibberish ở legend/helper.
- **F7-04-escalation-inbox**: 2 option protocol đặt cạnh nhau qua `md:grid-cols-2` (stack chỉ ở mobile — đúng responsive).
- **F1-03-browse-units**: hết placeholder gibberish ở filter dropdowns.

Không tìm thấy brand sai / lorem / text rác trong bất kỳ màn nào. Mockups đóng băng làm reference;
DESIGN.md + EXPERIENCE.md vẫn là hợp đồng thắng mọi mock khi xung đột.

## Sửa một màn (Stitch MCP)

Tool: `edit_screens` với `screen: "projects/9794980915577530548/screens/<id>"` (id nằm trong
`../.working/manifest.jsonl`). Prompt ngắn, mô tả đúng chỗ cần sửa.
