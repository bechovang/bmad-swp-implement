# ERD & State Chart — Drawio + DBML

Chuyển đổi từ báo cáo `../ERD_Statechart_LaTeX/main.tex` (22 thực thể / 34 quan hệ / 7 state chart) sang định dạng công cụ:

| File | Nội dung | Cách dùng |
|---|---|---|
| `ERD_StorageHub.dbml` | **ERD toàn hệ thống** — 22 bảng, 34 quan hệ, 20 ENUM | Mở https://dbdiagram.io/d → **New Diagram** → paste toàn bộ file |
| `Conceptual_Model_StorageHub.excalidraw` | **Mô hình khái niệm (bản đẹp, khuyên dùng)** — 17 thực thể, 28 quan hệ, layout ngôi sao quanh PHIÊN THUÊ, line thẳng + nhãn `động từ (1—N)` kiểu Chen, màu theo nhóm nghiệp vụ | Mở bằng excalidraw.com / VS Code extension / Excalidraw Desktop; nhãn line kéo trượt + double-click sửa được |
| `Conceptual_Model_StorageHub_Excalidraw.png` | Ảnh PNG của bản excalidraw trên | Chèn thẳng vào báo cáo / slide |
| `Conceptual_Model_StorageHub.drawio` | Mô hình khái niệm bản drawio (crow's foot bằng ER arrows) | Mở bằng app.diagrams.net / drawio Desktop |
| `StateChart1_Unit.drawio` / `_EN` | Vòng đời Unit (6 trạng thái, guard retire/merge) | Mở bằng app.diagrams.net / drawio Desktop |
| `StateChart2_Reservation.drawio` / `_EN` | Reservation BK- (PendingPayment → Reserved → CheckedIn/Expired/Cancelled) | Như trên |
| `StateChart3_Rental.drawio` / `_EN` | Rental RT- (Active → CheckoutRequested → Closed) | Như trên |
| `StateChart4_Contract.drawio` / `_EN` | Contract CT- (gốc + nhánh phụ lục AwaitingSignature) | Như trên |
| `StateChart5_Support_Ticket.drawio` / `_EN` | Ticket SR- (Open → InProgress → Resolved, nhánh Escalated) | Như trên |
| `StateChart6_Task.drawio` / `_EN` | Task kanban (ToDo → InProgress → Done, snap-back guard) | Như trên |
| `StateChart7_Payment.drawio` / `_EN` | Payment (Pending → Processing → Succeeded/Failed/Expired) | Như trên |

Mỗi sơ đồ có 2 file riêng: `StateChartN_*.drawio` (nhãn tiếng Việt) và `StateChartN_*_EN.drawio` (bản dịch tiếng Anh — sự kiện, guard, note; tên trạng thái giữ nguyên vì là giá trị ENUM trong DB).

## Quy ước trong file drawio

- Chấm đen đặc = điểm bắt đầu; vòng tròn viền đậm = điểm kết thúc.
- Nhãn cạnh = **sự kiện [guard]**; `→` trong nhãn = tác dụng phụ hệ thống.
- Màu trạng thái: xanh = bình thường, cam = cần xử lý (Maintenance/Escalated/AwaitingSignature), đỏ = lỗi (Failed), xám = kết thúc (Retired/Expired/Cancelled/Superseded/Closed), xanh lá = thành công (Resolved/Done/Succeeded).
- Note vàng = chú thích nghiệp vụ (buffer, merge, guard...) — giữ nguyên từ báo cáo LaTeX.

## Tương thích với state chart trong báo cáo

Mỗi giá trị ENUM trong ERD khớp đúng 1 trạng thái trên sơ đồ — mở DBML và drawio cạnh nhau để đối chiếu. Bảng chuyển dịch trạng thái đầy đủ (Từ → Sự kiện → Đến → Tác dụng phụ) nằm ở Section 3 của báo cáo LaTeX.
