# Reconcile — blueprint v4 + ERD/statecharts + swimlanes

*Đối chiếu 2026-09-17 giữa PRD (`prd.md`, bản cùng ngày) và: `ref/uiux_blueprint (4).md` (nội dung v4 current-state thực tế nằm ở `ref/uiux_blueprint new.md` — bản English — và `ref/uiux_blueprint new vie.md` — bản dịch; file đúng tên `(4)` không tồn tại trong `ref/`, chính PRD §0 cũng đang trỏ reference hỏng này), báo cáo `ERD_Statechart_LaTeX/main.tex` (22 thực thể + 7 state chart + 34 quan hệ), và 7 swimlane trong `SWP391_7_Flows_Drawio_Swimlane/`. Chỉ tập trung rule nghiệp vụ / enum / chuyển trạng thái — không đánh giá visual hay screen inventory.*

## Verdict

PRD bao phủ tốt các happy path và phần lớn guard "ép" (Payment 5 trạng thái, contract ritual, snap-back, conflict detection), nhưng **thiếu trọn vẹn phía rủi ro của vòng đời Reservation (EXPIRED/CANCELLED + hoàn cọc + dọn contract treo)** và còn **4 mâu thuẫn dữ liệu với ERD** (Turnover Buffer không có RuleType, touchpoint tiền thứ 4 lúc settlement, hạng mục Inspection, cách ghi nhận Relocation) — viết story sẽ phải tự bịa nếu không vá.

## Gaps

1. **Reservation EXPIRED / CANCELLED — không FR nào xử lý.**
   - *Nguồn:* State chart Reservation (main.tex §3.3): `Reserved → Expired` (quá hạn check-in: Unit trả về Available + Activity Log) và `Reserved → Cancelled` (khách huỷ: **hoàn cọc theo policy** + Activity Log); state chart Contract (§3.4): `Draft/Printed → Closed` khi booking huỷ/quá hạn "đóng hợp đồng gốc chưa hiệu lực, không treo lơ lửng". Blueprint §7 `ReservationStatus`: `PENDING_PAYMENT / RESERVED / CHECKED_IN / EXPIRED / CANCELLED`.
   - *PRD:* Glossary "Reservation" không có trạng thái; không FR nào cho khách huỷ booking, cho cơ chế hết hạn giữ chỗ (kể cả định nghĩa "hạn check-in" là gì), cho hoàn cọc ngoài checkout, hay cho đóng Contract Draft/Printed đi kèm. FR-9 chỉ flip "Reservation confirmed". Ngoài ra glossary nói Reservation tồn tại "sau khi cọc thành công", trong khi ERD sinh bản ghi ở `PENDING_PAYMENT` ngay tại Booking Summary (unit vẫn hiển thị Available, bỏ modal = không giữ unit).
   - *Severity:* **critical** — 2 trong 3 lối ra của Reserved không có chủ; vi phạm SM-3 (>5 FR phải bịa) và làm cụm "Tất toán cọc minh bạch" chỉ đúng một nửa.
   - *FR fix đề xuất:* "FR mới (P1): Reservation quá hạn check-in → EXPIRED (Unit → Available, Activity Log) hoặc khách huỷ → CANCELLED (hoàn cọc theo policy + receipt); Contract Draft/Printed tương ứng → Closed; không đổi trạng thái gì với Unit đã bị chiếm chỗ khác."

2. **Touchpoint tiền thứ 4 — "thu thêm" lúc settlement mâu thuẫn định nghĩa Payment 3 touchpoint.**
   - *Nguồn:* ERD `PAYMENTS` (main.tex §2.3): FK `SettlementID` + `Purpose ENUM('DEPOSIT','RENT','EXTENSION_FEE','DAMAGE_FEE','EXTRA_FEE')`. Swimlane Flow 3: khách "Thanh toán phần còn thiếu" khi cọc không đủ. Blueprint §11 SettlementPanel: "additional payment when charges exceed the deposit".
   - *PRD:* Glossary Payment + FR-8 chốt cứng "đúng 3 touchpoint"; FR-18 có nói "thu thêm nếu âm" nhưng không nói thu *bằng cách nào* (Payment Modal? ai bấm? flip trạng thái gì), và glossary mâu thuẫn ngược với ERD.
   - *Severity:* **high** — climax UJ-2 có thể rơi vào nhánh này và story không có cơ chế thu tiền.
   - *FR fix đề xuất:* "Sửa FR-8/FR-18: khi Settlement Charge vượt Deposit, Payment Modal mở touchpoint thứ 4 (purpose EXTRA_FEE) cho khách trả phần chênh trước khi Rental → CLOSED; receipt ghi rõ."

3. **Turnover Buffer là Policy Rule (FR-30) nhưng ERD không có chỗ lưu.**
   - *Nguồn:* ERD `POLICY_RULES.RuleType ENUM('DEPOSIT_RATE','RENT_RATE','LATE_FEE','SURCHARGE')` — không có loại rule nào cho buffer.
   - *PRD:* FR-30 tuyên bố Turnover Buffer là "hai Policy Rule theo phiên bản — đúng swimlane gốc" và là nền của flagship "lời hứa ngày chính xác" (FR-4).
   - *Severity:* **high** — mâu thuẫn trực tiếp PRD ↔ data model; architecture sẽ phải tự bịa chỗ lưu.
   - *FR fix đề xuất:* "Giữ FR-30, ghi chú data-model: bổ sung `TURNOVER_BUFFER` vào RuleType (hoặc field riêng trên RENTAL_POLICIES) — chốt trước bmad-architecture."

4. **Relocation: glossary nói "rental mới", ERD bắt Rental phải sinh từ Reservation 1:1.**
   - *Nguồn:* PRD glossary Relocation ("rental mới giữ điều kiện tương đương") ↔ ERD: `rentals.ReservationID` FK UNIQUE, quan hệ `reservations ||--|| rentals` "check-in sinh đúng một phiên thuê"; trong 22 thực thể không có Relocation entity hay field ghi nhận việc dời khách (M-2 → M-5).
   - *PRD:* FR-25 mô tả behavior đầy đủ ở mức UX nhưng không chốt representation: Rental mới hay đổi Unit trên Rental cũ? Deposit/Contract/access code xử lý ra sao?
   - *Severity:* **high** — UJ-5 là P1 demo; hai nguồn đang mâu thuẫn về cùng một sự kiện.
   - *FR fix đề xuất:* "Sửa FR-25/glossary: Relocation = đổi Unit trên Rental hiện tại + Activity Log (RELOCATION) + giữ nguyên Contract/Deposit/Access Code — không sinh Rental mới."

5. **ContractStatus thiếu AWAITING_SIGNATURE (3 nguồn, 3 phiên bản enum).**
   - *Nguồn:* ERD `CONTRACTS.Status` có 7 giá trị (`…,'AWAITING_SIGNATURE',…`); blueprint §7 chỉ 6 (không có AWAITING_SIGNATURE); PRD glossary vẽ chain `Draft → Printed → Signed → Active → Closed (+ Superseded)`.
   - *PRD:* FR-12 mô tả đủ *hành vi* addendum chờ ký nhưng glossary thiếu state tên riêng — blueprint §21 Bước 2 yêu cầu đối chiếu statusDictionary với báo cáo ERD đúng chỗ này.
   - *Severity:* **medium** — thuần enum/glossary nhưng là dữ liệu story sẽ dùng.
   - *FR fix đề xuất:* "Glossary Contract: thêm AWAITING_SIGNATURE (trạng thái Addendum từ khi trả phí đến khi có ảnh bản ký), lấy ERD làm chuẩn 7 giá trị."

6. **Ticket sau quyết định manager: thiếu luật ESCALATED → IN_PROGRESS.**
   - *Nguồn:* State chart Ticket (§3.5): cả hai nhánh quyết định (severe + phân công lại NV, hoặc trả về kèm guidance) đều `Escalated → InProgress`; ticket chỉ Resolved khi staff tự hoàn tất.
   - *PRD:* FR-25 chỉ nêu nhánh "không severe → ticket về lại staff"; nhánh severe kết thúc ở "tasks + notification" và consequence "ticket Resolved hiển thị trọn arc" — không ai Resolved, khi nào.
   - *Severity:* **medium**.
   - *FR fix đề xuất:* "Sửa FR-25 consequence: sau mọi Severity Decision (severe hay không), ticket quay lại IN_PROGRESS và chỉ Resolved khi staff hoàn tất phần việc (kèm note)."

7. **Lối ra khỏi Maintenance không có FR.**
   - *Nguồn:* State chart Unit (§3.2): `Maintenance → Preparing` khi "sửa chữa xong" (sinh task cleaning trước khi cho thuê lại, không về thẳng Available). Swimlane Flow 3 có nhánh "Kho có cần bảo trì? → Thực hiện bảo trì" ngay sau checkout.
   - *PRD:* FR-20 chỉ cover *vào* Maintenance/Retired; FR-25 cover vào Maintenance từ severe; không FR nào cover *kết thúc* bảo trì.
   - *Severity:* **medium**.
   - *FR fix đề xuất:* "Sửa FR-20: kết thúc Maintenance → Unit PREPARING + Cleaning Task theo Turnover Buffer (không bao giờ về thẳng Available)."

8. **Checklist Inspection: ERD và PRD/blueprint liệt kê hai bộ hạng mục khác nhau.**
   - *Nguồn:* ERD `INSPECTIONS`: `Item ENUM('ACCESS_CARD','PADLOCK','CLEANLINESS','STRUCTURE')` + `Result ENUM('OK','MINOR','MAJOR')`, ghi chú "Major có thể là căn cứ thu phí hư hại". PRD FR-18 (đi theo blueprint §11): "tường, cửa, sàn, vệ sinh".
   - *PRD:* FR-18 không có khái niệm Result (OK/MINOR/MAJOR) và không nối kết quả Major với Settlement Charge.
   - *Severity:* **medium** — mâu thuẫn danh sách + thiếu rule dẫn xuất.
   - *FR fix đề xuất:* "Chốt một bộ hạng mục + Result OK/MINOR/MAJOR cho Inspection (đề nghị theo ERD vì đã tính cả bàn giao thẻ/khóa), FR-18 ghi: charge nên có căn cứ hạng mục MAJOR."

9. **CheckoutRequest: thực thể có vòng đời riêng, PRD chỉ cover bước tạo.**
   - *Nguồn:* ERD `CHECKOUT_REQUESTS`: `Status ENUM('PENDING','DONE')`, quan hệ `rentals ||--o{ checkout_requests` (1:N — một rental có thể nhiều request), mô tả "ngày trả mới nhất luôn thắng để không đè reservation kế tiếp".
   - *PRD:* FR-17 dừng ở "Rental → CHECKOUT_REQUESTED; Checkout Task sinh ra" — không nói request sau với request trước, khi nào DONE, ngày xin trả có bị chặn bởi Reservation kế (như FR-16 làm cho extension) hay không.
   - *Severity:* **medium**.
   - *FR fix đề xuất:* "Sửa FR-17: request mới đè request cũ (cũ tự hủy), request DONE khi settlement hoàn tất; ngày xin trả bị chặn trên biên Reservation kế tiếp."

10. **ExtensionStatus: ba nguồn ba kiểu.**
    - *Nguồn:* Blueprint §7: `PENDING_PAYMENT / APPLIED / CANCELLED`; ERD `EXTENSIONS.Status`: chỉ `PENDING_PAYMENT / APPLIED`.
    - *PRD:* Glossary Extension không liệt kê trạng thái; không FR cho việc bỏ/hủy một extension đang chờ thanh toán.
    - *Severity:* **low**.
    - *FR fix đề xuất:* "Glossary Extension chốt PENDING_PAYMENT → APPLIED (bỏ CANCELLED cho khớp ERD; bỏ modal = extension không được ghi nhận / bị hủy)."

11. **Công thức phí gia hạn của swimlane không được PRD mang theo.**
    - *Nguồn:* Swimlane Flow 6: "Tính phí gia hạn = cọc cũ − cọc mới và phí mới" (cộng dồn phần cọc theo kỳ mới).
    - *PRD:* FR-16 chỉ nói "phí gia hạn tính theo Rental Policy active" — không nói deposit có được top-up theo tổng tiền thuê mới hay giữ nguyên (demo 690.000 ₫ giữ nguyên cọc 103.500 ₫).
    - *Severity:* **low**.
    - *FR fix đề xuất:* "FR-16 thêm một dòng: deposit giữ nguyên theo booking gốc, phí gia hạn = tiền thuê kỳ thêm theo policy (tránh story tự bịa công thức cọc mới − cọc cũ)."

12. **ActivityLogs.Reason NOT NULL (ERD) vs "reason nơi bắt buộc" (PRD/blueprint).**
    - *Nguồn:* ERD: `Reason NOT NULL`; blueprint §6: reason chỉ bắt buộc "where the flow demands", thậm chí "login actions do not require a business reason"; PRD glossary theo blueprint.
    - *Severity:* **low** — mâu thuẫn nhỏ về ràng buộc cột.
    - *FR fix đề xuất:* "Đánh dấu cần sửa ERD: Reason để NULL-able, NOT NULL chỉ với action loại status-change/charge (khớp NFR-4)."

13. **Bảng vai trò trong báo cáo ERD cho Facility Manager "phê duyệt policy".**
    - *Nguồn:* main.tex §1.2 (Facility Manager: "phê duyệt policy"); swimlane Flow 4 + blueprint #13 + PRD FR-29: Business Ops tự cấu hình và lưu qua validation, không có bước duyệt.
    - *Severity:* **low** — báo cáo ERD là bên lệch; chỉ cần ghi nhận để thống nhất tài liệu.
    - *FR fix đề xuất:* "Không đổi PRD; ghi chú sửa bảng vai trò trong báo cáo ERD (policy không qua manager approval)."

14. ** Enum rời chưa vào glossary: `IncidentType` (`LOST_ACCESS/DEVICE_ISSUE/SECURITY/CLEANLINESS/OTHER`), `Shift` (`MORNING/AFTERNOON/EVENING`), mã task `TL-` (chỉ xuất hiện trong ERD §1.3 + RefCode; blueprint §5 không cấp prefix cho Task).** PRD không sai gì, nhưng data contract (blueprint §21 Bước 2) sẽ cần chúng. *Severity:* **low**. *Fix:* thêm 3 dòng vào glossary §3 khi chốt data contract.

## Covered highlights

- **Payment state machine khớp gần 100%:** 5 trạng thái (kể cả `EXPIRED` QR ~5 phút, retry/đổi method, "No money was taken", chỉ SUCCEEDED mới đổi trạng thái nghiệp vụ) — FR-8/FR-9 phủ đúng từng chuyển dịch của state chart Payment.
- **Contract ritual đầy đủ:** auto-draft + khóa policy version (FR-10), in → ký → chụp ảnh → attach, access code khóa đến khi có ảnh ký (FR-11, SM-2), Addendum 7 ngày + reminder không thu hồi ngày (FR-12), chain đọc được kể cả Superseded (FR-13).
- **Guard kanban:** Done thiếu bước chốt (payment/settlement/ảnh ký) → snap-back + toast — FR-22 dịch đúng Task chart.
- **Buffer/deposit là policy theo phiên bản** — FR-30 đúng swimlane gốc (chỉ thiếu chỗ lưu ở ERD, gap 3); tính availability tại query time + re-check stale — FR-4/FR-5.
- **Cleaning từ card + kiểm Turnover Buffer trước khi Available, hoặc Reserved nếu có reservation kế** — FR-19 khớp 2 transition `Preparing →` của Unit chart.
- **Ràng buộc dữ liệu then chốt đã xuống FR:** UNIQUE(StaffID, WorkDate, Shift) → FR-27; DamageReason bắt buộc khi fee > 0 → FR-18; Escalations.Note NOT NULL → FR-24; append-only ActivityLog → FR-28/NFR-6; 1:1 Reservation → Rental → glossary.
- **Merge/Retire guard + MergedIntoID tự tham chiếu** — FR-26 nêu đúng guard hai phía và blocker có tên + link.
- **Extension conflict boundary "Latest new checkout: Oct 18" + ngày dịch ngay sau thanh toán, Addendum không hold Unit** — FR-16/FR-12 khớp state chart Rental + Contract.
- **Escalation arc ở mức hành vi** (severe → Maintenance + Relocation + tasks + notification; không severe → trả về staff) — FR-25 (chỉ thiếu representation, gap 4, và luật về IN_PROGRESS, gap 6).
- **Policy validate-trước-khi-lưu + version/effective stamp** — FR-29 khớp blueprint #13 và swimlane Flow 4 (hợp lệ? → lưu / không hợp lệ → nêu lý do).
- **Notification:** DeepLink + IsRead persist theo người — FR-34 khớp thực thể NOTIFICATIONS; không email — Non-Goals.
