# Hướng dẫn tự sửa sơ đồ draw.io theo Conceptual Model V3

**Ngày chốt V3:** 2026-09-18 — 4 quyết định kiến trúc dữ liệu:

1. **VAI TRÒ — NGƯỜI DÙNG: 1 — N** (mỗi user đúng 1 vai trò)
2. **ĐẶT CHỖ — HỢP ĐỒNG: 1 — 1** ("sinh" — mỗi đặt chỗ sinh đúng một hợp đồng gốc)
3. **Gộp PHIÊN THUÊ (Rental) vào ĐẶT CHỖ (Reservation)** — một bản ghi trọn vòng đời *đặt chỗ → thuê → trả*; cọc duy trì phiên; `CHECKED_IN` = trạng thái "đang thuê"
4. **PHỤ LỤC (Addendum) tách bảng riêng** — `HỢP ĐỒNG 1—N PHỤ LỤC` + `GIA HẠN 1—0..1 PHỤ LỤC` (tùy chọn), bỏ self-FK `ParentContractID`

Tài liệu đã đồng bộ V3 (tôi đã sửa): PRD glossary + §9.1, báo cáo LaTeX (`ERD_Statechart_LaTeX/` — 22 thực thể / **31 quan hệ** / 7 state chart, PDF 38 trang), DBML (`ERD_Statechart_Drawio/ERD_StorageHub.dbml` — **22 bảng / 31 refs**), và `Conceptual_Model_StorageHub_V3.drawio` (+EN) ở root.

> ✅ **CẬP NHẬT 2026-09-18:** Các mục **A → E đã làm xong** (Claude sửa trực tiếp theo yêu cầu):
> - **A.** StateChart2 Reservation (+EN) — gộp vòng đời Rental: thêm CheckoutRequested / Closed / final, self-loop gia hạn, note "CheckedIn = đang thuê"
> - **B.** StateChart3 — tạo mới `StateChart3_Addendum.drawio` (+EN); 2 file Rental cũ đã chuyển vào `ERD_Statechart_Drawio/archive/`
> - **C.** StateChart4 Contract (+EN) — bỏ AwaitingSignature, e4 = "check-in hoàn tất", note chỉ tới contract_addendums, thêm final cho Superseded
> - **D.** StateChart1 Unit — guard "không reservation" (bản EN giữ nguyên — đã mất guard khi bạn sửa tay)
> - **E.** StateChart7 Payment (+EN) — thêm "extension fee / extra fee"
> - Thư mục **`StateCharts_V3/`** (root) chứa bản sao 7 state chart VN mới nhất.
> - ✅ **Cập nhật thêm 2026-09-18 (phương án C):** State Chart 3 Addendum có thêm 2 terminal **EXPIRED** (khách không đến ký — staff đóng hồ sơ) + **VOIDED** (staff huỷ chủ động: soạn nhầm / trả kho sớm) → `addendum_status` 4 giá trị, **Extension 1—N Addendum** (bỏ UNIQUE). Đã ripple: chart 3 VN+EN, DBML, LaTeX (PDF 38 trang), PRD glossary + §9.1, Conceptual V3 VN+EN (nhãn cạnh "N"). `StateCharts_V3/` giờ có cả 7 bản EN.
>
> **Còn lại bạn tự làm:** F (archive V1/V2 conceptual), G (V3_EN xoá note "Full lifecycle"), H (Flow5 — tùy chọn).

Các mục dưới đây giữ nguyên làm tài liệu đối chiếu. Sửa theo thứ tự A → E là khớp truyện nhất.

---

## Mẹo thao tác draw.io dùng chung

| Việc | Cách làm |
|---|---|
| Tìm shape theo chữ | **Ctrl+F** (Edit → Find), gõ vài chữ trong label |
| Sửa label | Double-click vào shape/cạnh |
| Nhân bản shape (giữ nguyên style) | Chọn shape → **Ctrl+D** (Duplicate) rồi kéo đi |
| Sao chép style | Chọn shape mẫu → **Ctrl+Shift+C** (Copy Style) → chọn shape mới → **Ctrl+Shift+V** (Paste Style) |
| Sửa label cạnh | Double-click cạnh, hoặc click cạnh → gõ chữ trực tiếp |
| Tạo điểm bắt đầu/kết thúc | Kéo hình **Ellipse** từ khung trái, thu nhỏ thành chấm đặc (●) / chấm tròn trắng (◦) — hoặc copy sẵn có trong file |
| Self-loop (mũi tên quay lại chính nó) | Kéo cạnh từ shape thả **ngay chính shape đó**, rồi kéo điểm vàng giữa cạnh để cong ra ngoài |

> **Lưu ý PNG:** ảnh trong báo cáo LaTeX được render từ mã Mermaid (`render_v3.py`), KHÔNG lấy từ draw.io — nên bạn **không cần** xuất PNG lại cho báo cáo. Chỉ Export PNG (File → Export as → PNG, Zoom 100%, Border 10) nếu muốn dùng ảnh ở nơi khác (slide…).

---

## A. `ERD_Statechart_Drawio/StateChart2_Reservation.drawio` (+ `_EN`) — GỘP VÒNG ĐỜI RENTAL

Đây là file sửa nhiều nhất: vòng đời Rental cũ (Active / CheckoutRequested / Closed) chuyển hết vào đây.

### A1. Đổi tiêu đề
- Tìm `STATE CHART 2 — RESERVATION (đặt chỗ BK-)`
- → `STATE CHART 2 — RESERVATION (đặt chỗ — thuê BK-)`
- Bản EN: `STATE CHART 2 — RESERVATION (booking BK-)` → `STATE CHART 2 — RESERVATION (booking — rental BK-)`

### A2. Sửa cạnh Reserved → CheckedIn
- Cạnh đang ghi: `trả 100% rent + ký hợp đồng (sinh Rental)`
- → `trả 100% rent + ký hợp đồng (cấp access code)` *(không còn "sinh Rental" — Rental đã gộp vào đây)*

### A3. Thêm 2 trạng thái mới (sau CheckedIn)
Nhân bản 2 shape trạng thái hiện có (vd CheckedIn) rồi đổi tên:
- `CheckoutRequested`
- `Closed`

Nối cạnh (dùng style cạnh hiện có trong file):
- `CheckedIn` → `CheckoutRequested`: **`khách gửi checkout request`**
- `CheckoutRequested` → `Closed`: **`settlement hoàn tất [refund = cọc − phí hư hại]`**

### A4. Thêm self-loop gia hạn tại CheckedIn
- Kéo cạnh từ `CheckedIn` thả lại `CheckedIn`, label:
  **`gia hạn [phí đã trả]: EndDate dịch ngay, sinh extension + addendum chờ ký`**

### A5. Thêm điểm kết thúc ◦ (final state)
- `Closed` → ◦ và `Expired` → ◦ (nhân bản chấm kết thúc sẵn có, vd sau `Cancelled`… nếu file chưa có chấm kết thúc thì vẽ ellipse nhỏ nền đen cho `Closed`/`Expired`; `Cancelled` nếu đang có chấm kết thúc thì giữ nguyên)

### A6. Ghi chú dự phòng cho Cancelled
- Cạnh `huỷ đặt [hoàn cọc theo policy]` → đổi thành:
  **`huỷ đặt [hoàn cọc theo policy — dự phòng, v1 không kích hoạt]`**

### A7. Thêm note giải thích (copy ý từ file StateChart3 cũ trước khi làm mục B)
Thêm 1 shape Note (khung vàng) cạnh `CheckedIn`, nội dung:

```
CheckedIn = trạng thái "đang thuê"
(Rental gộp vào Reservation — V3):
giữ cọc, AccessCode, EndDate dịch ngay khi gia hạn.
Conflict guard: không đè reservation kế tiếp
("Latest new checkout: Oct 18").
```

### Bản EN — làm tương tự với label tiếng Anh:
- `pays 100% rent + signs contract (issues access code)`
- `customer sends checkout request`
- `settlement completed [refund = deposit − damage fee]`
- `renewal [fee paid]: EndDate shifts immediately, creates extension + addendum awaiting signature`
- `cancel [refund per policy — reserved, not active in v1]`

---

## B. `ERD_Statechart_Drawio/StateChart3_Rental.drawio` (+ `_EN`) — BIẾN THÀNH STATE CHART ADDENDUM

Vị trí chart số 3 giờ là **Contract Addendum** (khớp báo cáo LaTeX: Unit, Reservation, Contract, **Contract Addendum**, Ticket, Task, Payment — vẫn 7 chart).

### B1. Đổi tên file
1. Mở `StateChart3_Rental.drawio` trong draw.io
2. **File → Save As** → `StateChart3_Addendum.drawio` (cùng thư mục `ERD_Statechart_Drawio/`)
3. Làm tương tự với bản `_EN`
4. Sau khi sửa xong nội dung (B2), **xoá 2 file Rental cũ** (hoặc chuyển vào thư mục `archive/` nếu muốn giữ)

### B2. Sửa nội dung
Xoá toàn bộ state/label cũ (Ctrl+A → Delete), giữ lại khung tiêu đề + các shape "trống" rồi vẽ lại:

**Tiêu đề:** `STATE CHART 3 — CONTRACT ADDENDUM (phụ lục CT-…-A1)`
(Bản EN: `STATE CHART 3 — CONTRACT ADDENDUM (CT-…-A1)`)

**Trạng thái (2):** `AwaitingSignature`, `Signed`

**Cạnh:**
- ● → `AwaitingSignature`: **`phí gia hạn thành công [auto-draft, hạn ký 7 ngày]`**
- `AwaitingSignature` → `Signed`: **`ký tại quầy + staff chụp ảnh attach`**
- `AwaitingSignature` → `AwaitingSignature` (self-loop): **`quá hạn [banner amber + nhắc việc]`**
- `Signed` → ◦: **`phụ lục hoàn tất hồ sơ giấy`**

**Note (1, khung vàng):**
```
Quá hạn không có terminal:
EndDate đã dịch từ lúc trả phí,
phụ lục chỉ là hồ sơ giấy — không hold unit.
Bell reminder trước hạn.
```

Bản EN label: `renewal fee paid [auto-draft, 7-day signing deadline]` / `signed at counter + staff attaches photo` / `overdue [amber banner + reminders]` / `addendum paperwork complete`.

---

## C. `ERD_Statechart_Drawio/StateChart4_Contract.drawio` (+ `_EN`) — BỎ NHÁNH AWAITING SIGNATURE

Phụ lục đã có chart riêng (mục B) nên chart Contract chỉ còn **hợp đồng gốc**.

### C1. Xoá nhánh phụ lục
Xoá 4 cạnh + 1 trạng thái sau:
- Trạng thái **`AwaitingSignature`**
- Cạnh vào: `addendum gia hạn [phí đã trả] [type=ADDENDUM]`
- Cạnh ra: `ký tại quầy [hạn 7 ngày, bell reminder]`
- Self-loop: `quá hạn [banner amber + nhắc việc]`
- Cạnh `addendum đã filed [type=ADDENDUM]` (từ Signed → ◦ của phụ lục)

### C2. Sửa cạnh Signed → Active
- Đang ghi: `rental kích hoạt [type=ORIGINAL]`
- → **`check-in hoàn tất (reservation CHECKED_IN)`**
- (EN: `check-in completed (reservation CHECKED_IN)`)

### C3. Thêm điểm kết thúc cho Superseded
- `Superseded` → ◦ (nhân bản chấm kết thúc sau `Closed`)
- `Closed` → ◦ nếu chưa có thì thêm luôn

### C4. Sửa note bên phải
Note hiện chứa 3 ý, thay câu giữa:

| Cũ | Mới |
|---|---|
| `Không ai sửa được hợp đồng — mọi thay đổi = phụ lục mới (ParentContractID trỏ về bản gốc).` | `Không ai sửa được hợp đồng — mọi thay đổi = phụ lục riêng (bảng contract_addendums — state chart 3).` |

Câu cuối `Quá hạn ký phụ lục: nhắc vô hạn định…` **xoá** (đã chuyển sang note của chart Addendum).

### C5. Rà lại guard
Sau khi xong, **Ctrl+F tìm `type=`** — không được còn sót guard `type=ORIGINAL/ADDENDUM` nào (enum `contract_type` đã bị xoá khỏi model V3).

---

## D. `ERD_Statechart_Drawio/StateChart1_Unit.drawio` (+ `_EN`) — SỬA NHỎ GUARD

Rental không còn là bảng riêng nên mọi guard "rental/reservation" chỉ còn "reservation":

1. 2 cạnh `retire [guard: không rental/reservation]` → **`retire [guard: không reservation]`**
2. Note Merge: `Merge 2 unit liền kề [guard: không rental/reservation cả 2 phía]…` → **`không reservation cả 2 phía`**
3. (Tùy chọn) Note cạnh `Rented` vẫn hợp lệ — không cần đổi.

---

## E. `ERD_Statechart_Drawio/StateChart7_Payment.drawio` (+ `_EN`) — THÊM TOUCHPOINT THỨ 4

- Cạnh ● → Pending đang ghi: `mở Payment Modal [deposit / rent / extension fee]`
- → **`mở Payment Modal [deposit / rent / extension fee / extra fee]`**
- (`extra fee` = phí phát sinh vượt cọc khi thanh lý — Payment giờ có 4 touchpoint)

Các chart **5 (Ticket), 6 (Task)** không đổi.

---

## F. Conceptual model bản cũ — LƯU TRỮ

| File | Trạng thái | Việc cần làm |
|---|---|---|
| `ERD_Statechart_Drawio/Conceptual_Model_StorageHub.drawio` (V1) | Lỗi thời (còn hub PHIÊN THUÊ, self-FK phụ lục, 17 thực thể) | Chuyển vào `archive/` hoặc xoá — đã có V3 |
| `Conceptual_Model_StorageHub_V2.drawio` / `_EN` (root) | Lỗi thời (V2 chưa có 4 quyết định V3) | Chuyển vào `archive/` hoặc xoá |
| `Conceptual_Model_StorageHub_V3.drawio` (root) | ✅ Đã đúng V3 | Không sửa |
| `Conceptual_Model_StorageHub_V3_EN.drawio` (root) | Gần xong | Xem mục G |

Gợi ý: tạo thư mục `archive/` ở root, git mv V1/V2 vào đó — giữ lịch sử mà thư mục làm việc sạch.

---

## G. `Conceptual_Model_StorageHub_V3_EN.drawio` — XOÁ 1 NOTE SOT

Bản Việt bạn đã tự xoá note cạnh RESERVATION, nhưng **bản EN còn**:

1. Ctrl+F tìm: `Full lifecycle (Rental Session merged in)` — một shape text 2 dòng cạnh RESERVATION
2. Xoá shape đó (giữ nguyên mọi thứ khác)

---

## H. Swimlane (7 flows) — GẦ NHƯ KHÔNG ĐỔI

Đã rà toàn bộ 7 file: không có mã `RT-`, không tham chiếu bảng Rental — các chữ "phiên thuê" chỉ là từ ngữ nghiệp vụ (giữ nguyên). Duy nhất 1 chỗ **tùy chọn**:

- `Flow5_Gon_Wide_System_NoOverlap.drawio`: label `Kiểm tra Rental, Reservation và trạng thái các kho liên quan` → có thể đổi thành `Kiểm tra Reservation và trạng thái các kho liên quan` cho ăn khớp V3.

---

## Checklist tổng

- [x] A. StateChart2 Reservation (+EN): thêm CheckoutRequested/Closed/self-loop/note, sửa 2 label — *Claude đã sửa 2026-09-18*
- [x] B. StateChart3 → Addendum (+EN): tạo file mới `StateChart3_Addendum.drawio` (+EN); Rental cũ → `ERD_Statechart_Drawio/archive/` — *Claude đã sửa 2026-09-18*
- [x] C. StateChart4 Contract (+EN): xoá nhánh AwaitingSignature, sửa guard, thêm Superseded → ◦ — *Claude đã sửa 2026-09-18*
- [x] D. StateChart1 Unit (+EN): guard "không reservation" — *Claude đã sửa 2026-09-18 (bản EN giữ nguyên)*
- [x] E. StateChart7 Payment (+EN): thêm `/ extra fee` — *Claude đã sửa 2026-09-18*
- [x] ➕ Tạo thư mục `StateCharts_V3/` (root) chứa 7 state chart VN mới nhất — *Claude đã tạo 2026-09-18*
- [ ] F. V1/V2 → `archive/`
- [ ] G. V3_EN: xoá note "Full lifecycle (Rental Session merged in)"
- [ ] H. Flow5 (tùy chọn): bỏ chữ "Rental,"
