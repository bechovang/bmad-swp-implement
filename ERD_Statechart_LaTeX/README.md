# StorageHub — Tài liệu ERD & State Chart (LaTeX)

Báo cáo thiết kế CSDL cho dự án **SWP391 StorageHub** (kho thuê tự phục vụ),
bám theo cấu trúc báo cáo môn DBI202 trong thư mục `latex dbi/`.

## Nội dung

| Phần | Nội dung |
|---|---|
| Section 1 | Giới thiệu tổng quan: bối cảnh self-storage, 4 vai trò, nguồn tài liệu (7 flows swimlane + spine UX), công cụ |
| Section 2 | **ERD** crow's foot: 22 thực thể / 8 nhóm — bảng thuộc tính đầy đủ kiểu SQL, bảng 34 mối quan hệ, hình ERD toàn hệ thống (trang ngang) |
| Section 3 | **7 state chart**: Unit, Reservation, Rental, Contract, Support Ticket, Task, Payment — mỗi sơ đồ có bảng chuyển dịch (Từ / Sự kiện [guard] / Đến / Tác dụng phụ) + hình render |
| Phụ lục | Toàn bộ mã nguồn Mermaid của 8 sơ đồ |

## Cấu trúc thư mục

```
ERD_Statechart_LaTeX/
├── main.tex          # tài liệu LaTeX (XeLaTeX)
├── auto_build.bat    # watch main.tex → tự biên dịch lại
├── mermaid/          # mã nguồn .mmd (erd + 7 state chart)
└── images/           # ảnh PNG render từ Mermaid (1800px)
```

## Build

Yêu cầu MiKTeX (XeLaTeX) + font Times New Roman / Arial / Courier New.

```bat
:: build tay (chạy 2 lần để có mục lục)
xelatex -interaction=nonstopmode main.tex
xelatex -interaction=nonstopmode main.tex

:: hoặc watch tự động
auto_build.bat
```

## Render lại ảnh Mermaid

Ảnh render qua dịch vụ trực tuyến (kroki.io cho ERD lớn, mermaid.ink cho
state chart — ERD quá dài cho GET URL nên phải POST):

```bash
# state chart (nhỏ): GET qua mermaid.ink
python - <<'EOF'
import base64, urllib.request
code = open('mermaid/state-unit.mmd', encoding='utf-8').read()
b64 = base64.urlsafe_b64encode(code.encode('utf-8')).decode('ascii')
url = 'https://mermaid.ink/img/' + b64 + '?type=png&width=1800'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
open('images/state-unit.png', 'wb').write(urllib.request.urlopen(req, timeout=60).read())
EOF

# ERD (lớn): POST qua kroki.io
python - <<'EOF'
import urllib.request
code = open('mermaid/erd.mmd', encoding='utf-8').read()
req = urllib.request.Request('https://kroki.io/mermaid/png',
    data=code.encode('utf-8'),
    headers={'Content-Type': 'text/plain; charset=utf-8', 'User-Agent': 'Mozilla/5.0'})
open('images/erd.png', 'wb').write(urllib.request.urlopen(req, timeout=120).read())
EOF
```

## Quy ước nghiệp vụ đã phản ánh trong mô hình

- Mã hồ sơ: đặt chỗ `BK-`, thuê `RT-`, hỗ trợ `SR-`, hợp đồng `CT-`, task `TL-`
- Cọc 10% khi đặt, 100% tiền thuê khi check-in; phí hư hại bắt buộc có lý do
- Hợp đồng auto-draft khi cọc thành công, khoá phiên bản policy; gia hạn sinh
  addendum hạn ký 7 ngày; `EndDate` của rental dịch ngay khi phí gia hạn được thanh toán
- Guard: chống retire/merge unit khi còn lịch; `UNIQUE(StaffID, WorkDate, Shift)`
  chống trùng ca trực; kanban chặn Done khi thiếu bước chốt (snap-back + toast)
- `activity_logs` append-only, mọi thay đổi trạng thái kèm lý do
