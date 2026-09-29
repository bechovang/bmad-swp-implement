# Reconcile — DESIGN.md + NOTES.md

*Đối chiếu `ux-swp391-2026-09-11/DESIGN.md` + `mockups/NOTES.md` với `prd-storagehub-2026-09-17/prd.md`. Ngày 2026-09-17. Chỉ tập trung gap có hệ quả nghiệp vụ/hành vi — chi tiết thuần thị giác (màu, font, spacing) được bỏ qua chủ đích vì PRD đã giao cho DESIGN.md.*

## Verdict

PRD bám sát hai nguồn rất chặt ở tầng nghiệp vụ (hợp đồng CT-, tiền bạc, demo data, buffer semantics gần như khớp toàn bộ); gap đáng kể nhất là FR-12 chưa định nghĩa bước chụp/đính kèm ảnh bản ký Addendum để hoàn tất Contract-signature task — chỉ quy tắc "cánh cửa" duy nhất mà DESIGN.md nêu rõ nhưng PRD bỏ sót.

## Gaps

1. **Addendum thiếu bước signature-capture và gating hoàn tất task (FR-12, FR-22).**
   - **Nguồn:** DESIGN.md (Contract step): *"The parent flow's completing action — access-code handover in Check-in, **addendum completion in a Contract task** — stays disabled until the capture tile holds a photo."* NOTES.md 2026-09-14 (F2-04): addendum `CT-1042-A1` có trạng thái "Awaiting signature"; (F6-01): badge "Signature required at desk".
   - **PRD:** FR-12 sinh Addendum + deadline 7 ngày + amber banner + card Contract-signature + bell reminder, nhưng **không nói task Contract-signature hoàn tất bằng cách nào** — không có bước staff đính kèm ảnh bản ký phụ lục, không có flip Awaiting signature → Signed, không có nút vô hiệu đến khi có ảnh. FR-13 lại hứa hiển thị "ảnh bản ký" cho cả Addenda → bước capture phải tồn tại ở đâu đó nhưng không FR nào sở hữu. FR-22 (snap-back) chỉ liệt kê closing step của Check-in/Checkout, bỏ qua card Contract-signature.
   - **Severity: high** (FR P1 under-specified; SM-1 demo UJ-1 cần signing phụ lục tại quầy; stories sẽ phải tự bịa — trúng đúng SM-3).
   - **Fix:** Bổ sung vào FR-12: *"Contract-signature task hoàn tất khi staff đính kèm ảnh bản ký Addendum (Attach vô hiệu đến khi có ảnh); kèm ảnh vào chuỗi FR-13; FR-22 mở rộng closing step cho card Contract-signature."*

2. **Định dạng hiển thị VND thiếu separator và vị trí ký hiệu (NFR-1).**
   - **Nguồn:** DESIGN.md (Typography): *"VND formatting is non-negotiable: dot-separated thousands, dong sign after the amount, always the full figure — `1.150.000 ₫`, never `1.15tr` or `₫1,150,000`."*
   - **PRD:** NFR-1 chỉ nói "full precision (không viết tắt, không bỏ 3 số 0)" + tabular numerals — **không chốt dấu chấm ngăn cách hàng nghìn và `₫` đặt sau số**. Với UI English-only, dev dễ mặc định `1,150,000 ₫` hoặc `₫1.150.000` — đúng 2 dạng DESIGN.md cấm.
   - **Severity: medium.**
   - **Fix:** NFR-1 thêm: *"Định dạng chuẩn: dấu chấm ngăn cách hàng nghìn, ký hiệu ₫ đặt sau số — `1.150.000 ₫`."*

3. **Ba màn customer trong 27 màn không có FR sở hữu: F2-03 My Rentals, F2-05 Check-in Pass, F7-02 Support List.**
   - **Nguồn:** NOTES.md index 27 màn liệt kê rõ 3 màn này (Check-in pass mang mã `BK-1042`; Support List + detail drawer là nơi khách thấy kết quả ticket).
   - **PRD:** chỉ vỗn qua trong path của UJ-1/UJ-5 ("Check-in Pass mang mã đặt chỗ", "khách thấy kết quả ở Support List"); không FR nào định nghĩa hành vi tối thiểu (My Rentals là entry point tới Rental Detail / extend / checkout request; Check-in Pass hiển thị mã đặt chỗ cho staff validate ở FR-14; Support List là nơi khách đọc resolution). Scope có qua §7.1 nhưng.FR bỏ trống.
   - **Severity: medium.**
   - **Fix:** Thêm 1 FR mỏng gom 3 surface customer này, hoặc 1 dòng §7.1 liệt kê chúng thuộc P1 kèm hành vi tối thiểu từng màn.

4. **§7.1 tuyên "27 màn in scope P1" nhưng FR-32/§7.2 cắt Operations Monitor (F5-04) về P2.**
   - **Nguồn:** NOTES.md liệt kê F5-04 Operations Monitor (live) là 1 trong 27 màn.
   - **PRD:** §7.1 nói "27 màn theo UX package (đã chốt)" đủ chạy 5 UJ; §7.2 lại đưa "Operations Monitor live view (FR-32)" vào out-of-scope MVP. Không rõ P1 có 26 màn, hay có monitor dạng tĩnh (non-live).
   - **Severity: medium** (mâu thuẫn nội tại về mặt đếm màn hình; SM-C1 quản lý bằng số màn).
   - **Fix:** §7.1 ghi rõ *"26 màn P1; F5-04 Operations Monitor (live) là P2"* hoặc định nghĩa monitor dạng tĩnh thuộc P1.

5. **Mô hình trạng thái: DESIGN.md nói "seven states" (gồm Available soon/buffer) vs PRD 6 trạng thái + derived label.**
   - **Nguồn:** DESIGN.md (Colors): *"Every unit is in exactly one of seven states"* — bảng 7 hàng, trong đó "Available soon (buffer)" là một state.
   - **PRD:** §3 Unit liệt kê 6 trạng thái; §3 Availability giải thích "Available soon" là derived label, không phải trạng thái Unit (đúng theo ERD). PRD đúng, nhưng không có ghi chú ánh xạ —下游 architecture/epics đọc DESIGN.md có thể sinh thực thể trạng thái thứ 7.
   - **Severity: low.**
   - **Fix:** Thêm 1 dòng vào §3: *"DESIGN.md hiển thị 7 nhãn trạng thái = 6 trạng thái lưu + 1 nhãn derived 'Available soon'."*

6. **Demo data: §7.1 thiếu mã `RT-0871`, `SR-0032` trong danh sách mock-data chuẩn.**
   - **Nguồn:** NOTES.md verify F2-01: codes đúng `BK-1042/RT-0871/SR-0032`.
   - **PRD:** §7.1 liệt kê "Lan/Minh/Hằng/Tuấn, S-3, BK-1042, CT-1042…" — có prefix `RT-`/`SR-` ở Glossary nhưng không giá trị demo cụ thể; seed data dễ lệch mockups.
   - **Severity: low.**
   - **Fix:** Bổ sung `RT-0871`, `SR-0032`, `CT-1042-A1`, `M-2`/`M-5` vào chuỗi mock-data chuẩn ở §7.1.

7. **PNG của F2-04 và F6-01 còn bản cũ (thiếu card Contracts & Addenda / After payment) — PRD không cảnh báo.**
   - **Nguồn:** NOTES.md 2026-09-14 ghi rõ: HTML export của Stitch không regenerate, đã chèn DOM block vào HTML local; **PNG còn bản cũ, chỉ là preview**.
   - **PRD:** coi "27 mockups" là frozen reference nhưng không chú thích màn nào là stale — QA/epics soi PNG sẽ thấy bản thiếu tính năng hợp đồng.
   - **Severity: low.**
   - **Fix:** 1 dòng ở §0 hoặc §7.1: *"HTML local là bản chuẩn cho F2-04/F6-01 (PNG stale); khi mâu thuẫn, DESIGN.md/EXPERIENCE.md thắng mọi mock."*

8. **Hai quy tắc hành vi UI của DESIGN.md chưa được nâng thành ràng buộc ngang hàng: empty state luôn có next move, toast không bao giờ thay thế bell.**
   - **Nguồn:** DESIGN.md (Empty state): *"Empty states always offer the next move; they never dead-end"*; (Toast): *"Toasts report what happened; the notification bell is the durable record — the two are never substitutes."*
   - **PRD:** bảo đảm no-dead-end chỉ ở Payment Modal (FR-8); FR-33 chỉ bắt "sự kiện tiền phát cả toast lẫn bell" — quy tắc tổng quát cho mọi empty state và mọi kênh thông báo không có chỗ neo NFR.
   - **Severity: low.**
   - **Fix:** Thêm 2 câu vào NFR-2: *"Empty state luôn có CTA/bước kế (không dead-end); toast là phản hồi tức thời, bell là record bền vững — toast không bao giờ thay thế bell."*

9. **Cơ chế sửa Contract Draft sai: "corrected by re-drafting" không có FR.**
   - **Nguồn:** DESIGN.md (Contract step): *"contracts are drafted by the system, **corrected by re-drafting**, never edited in place."*
   - **PRD:** FR-10 cấm sửa nội dung (đúng), state machine có Superseded, FR-13 cho đọc bản Superseded — nhưng không FR nào nói khi nào/tại sao phát sinh re-draft (vd Draft in ra phát hiện sai dữ liệu trước khi ký).
   - **Severity: low.**
   - **Fix:** Thêm 1 câu vào FR-10 consequences: *"Sai sót ở Draft được sửa bằng re-draft (bản cũ → Superseded, vẫn đọc được trong chuỗi)."*

## Covered highlights

- **Nghi thức hợp đồng check-in (NOTES 2026-09-14) → FR-10/FR-11 đầy đủ:** auto-draft khi Deposit thành công, khóa Rental Policy version, read-only mọi nơi, Print → khách ký → staff chụp ảnh upload → mới trao Access Code, `CONTRACT_SIGNED` ghi Activity Log.
- **Semantics Addendum đúng khớp NOTES:** ký tại quầy trong 7 ngày, ngày mới hiệu lực ngay sau thanh toán, paperwork không hold Unit (FR-12); **checkout không ký** — settlement receipt là biên bản (§3 Settlement).
- **Demo data tiền bạc & nhân vật khớp tuyệt đối:** cọc 10% = 103.500 ₫, phí gia hạn 690.000 ₫, damage 40.000 ₫, refund 63.500 ₫ (103.500 − 40.000); policy v3 + trần 10% vs 15% (UJ-3); Lan/Minh/Hằng/Tuấn; S-3, M-2, M-5; Tân Bình Depot.
- **Buffer = amber attention, không phải error:** FR-4 consequence "card buffer hiển thị Available {date} · cleaning buffer và vẫn đặt được với ngày đó" đúng tinh thần DESIGN.md.
- **Quy tắc "không color-only" của badge (DESIGN.md) → NFR-2** (label + icon); card Done render muted → FR-21; toast ~4s hover-pause → FR-33; input luôn có label, không placeholder-only → NFR-2.
- **Kanban 5 loại card + tab lọc theo loại + keyboard Move** (DESIGN.md kanban/tabs) → FR-21/FR-22; landing theo role (Staff → Task Board) → FR-1.
- **Full amount breakdown trước payment** (DESIGN.md Do/Don't) → FR-6/FR-7 ("không có dòng tiền nào xuất hiện ở payment mà vắng ở Booking Summary").
