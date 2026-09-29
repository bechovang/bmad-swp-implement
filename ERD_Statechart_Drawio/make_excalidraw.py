# -*- coding: utf-8 -*-
"""
Sinh mo hinh khai niem StorageHub dang file .excalidraw (star quanh PHIEN THUE)
+ preview.svg de kiem tra hinh anh.
17 thuc the / 28 quan he. Dong ket noi = line thang (khong mui ten),
nhan dong tu o giua, do tuong quan 1 / N o hai dau (kieu Chen, khong dung crow's foot).
"""
import json, math, random, sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
random.seed(42)

# ---------------------------------------------------------------- du lieu -----
# (id, ten, x, y, w, h, mau, fontSize)
ENTITIES = [
    ("hub",     "PHIÊN THUÊ",     1150,  630, 260, 76, "orange", 26),
    ("resv",    "ĐẶT CHỖ",         950,  110, 190, 54, "orange", 20),
    ("contract","HỢP ĐỒNG",       1790,  220, 190, 54, "orange", 20),
    ("ext",     "GIA HẠN",        1750,  950, 190, 54, "orange", 20),
    ("chk",     "YÊU CẦU TRẢ",    2020,  690, 200, 54, "orange", 20),
    ("pay",     "THANH TOÁN",     1070, 1200, 210, 54, "yellow", 20),
    ("sett",    "THANH LÝ",       1520, 1150, 190, 54, "yellow", 20),
    ("user",    "NGƯỜI DÙNG",      290,  630, 210, 54, "blue",   20),
    ("role",    "VAI TRÒ",           60,  420, 160, 54, "blue",   20),
    ("unit",    "UNIT",             610,  300, 170, 54, "green",  20),
    ("zone",    "KHU VỰC",          320,  140, 180, 54, "green",  20),
    ("fac",     "CƠ SỞ",             40,  120, 170, 54, "green",  20),
    ("utype",   "LOẠI UNIT",        660,   60, 170, 54, "green",  20),
    ("policy",  "CHÍNH SÁCH GIÁ",  1330,   40, 210, 54, "green",  20),
    ("task",    "TASK",              60,  730, 150, 54, "purple", 20),
    ("ticket",  "TICKET",           280, 1000, 160, 54, "purple", 20),
    ("esc",     "ESCALATION",        30, 1180, 190, 54, "purple", 20),
]

# (start, end, dong tu, card1-o-dau-start, card2-o-dau-end)
EDGES = [
    ("resv",    "hub",     "kích hoạt",      "1", "1"),
    ("user",    "hub",     "thuê",           "1", "N"),
    ("unit",    "hub",     "cho thuê",       "1", "N"),
    ("hub",     "ext",     "gia hạn",        "1", "N"),
    ("hub",     "chk",     "xin trả",        "1", "N"),
    ("hub",     "contract","mang",           "1", "N"),
    ("hub",     "sett",    "thanh lý",       "1", "0..1"),
    ("hub",     "pay",     "tiền thuê",      "1", "N"),
    ("role",    "user",    "phân vai",       "1", "N"),
    ("user",    "zone",    "trực ca",        "N", "N"),
    ("user",    "task",    "nhận việc",      "1", "N"),
    ("fac",     "zone",    "chứa",           "1", "N"),
    ("zone",    "unit",    "chứa",           "1", "N"),
    ("utype",   "unit",    "phân loại",      "1", "N"),
    ("utype",   "policy",  "áp dụng",        "N", "N"),
    ("policy",  "contract","khoá phiên bản", "1", "N"),
    ("contract","sett",    "đối chiếu",      "1", "N"),
    ("unit",    "resv",    "được giữ",       "1", "N"),
    ("user",    "resv",    "đặt",            "1", "N"),
    ("resv",    "pay",     "cọc giữ chỗ",    "1", "N"),
    ("ext",     "pay",     "phí gia hạn",    "1", "0..1"),
    ("sett",    "pay",     "phí phát sinh",  "1", "N"),
    ("user",    "pay",     "trả tiền",       "1", "N"),
    ("user",    "ticket",  "gửi",            "1", "N"),
    ("unit",    "ticket",  "liên quan",      "1", "N"),
    ("ticket",  "esc",     "nâng cấp",       "1", "0..1"),
    ("user",    "esc",     "quyết định",     "1", "N"),
]
SELF = ("contract", "phụ lục của", "1", "N")   # tu quan he

# vi tri nhan tren line (ty le t tu start->end), mac dinh 0.5; chinh cho line dai tranh de hop
LABEL_T = {("resv", "pay"): 0.40, ("unit", "ticket"): 0.58}

COLORS = {
    "orange": ("#ffe6cc", "#d79b00"),
    "yellow": ("#fff2cc", "#d6b656"),
    "blue":   ("#dae8fc", "#6c8ebf"),
    "green":  ("#d5e8d4", "#82b366"),
    "purple": ("#e1d5e7", "#9673a6"),
}
INK      = "#1e1e1e"
GAP      = 6          # khoang cach mui dong voi hop
FS_LABEL = 16         # dong tu tren line
FS_CARD  = 15         # so 1 / N

BOX = {e[0]: dict(x=e[2], y=e[3], w=e[4], h=e[5], name=e[1], color=e[6], fs=e[7]) for e in ENTITIES}

def center(i):
    b = BOX[i]; return b["x"] + b["w"] / 2, b["y"] + b["h"] / 2

def edge_point(i, tx, ty, pad=GAP):
    """diem tren bien hop (i) huong toi (tx,ty) tu tam, + pad"""
    b = BOX[i]; cx, cy = center(i)
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0: return cx, cy
    hw, hh = b["w"] / 2 + pad, b["h"] / 2 + pad
    sx = hw / abs(dx) if dx else 1e9
    sy = hh / abs(dy) if dy else 1e9
    s = min(sx, sy)
    return cx + dx * s, cy + dy * s

def txt_w(s, fs):
    return fs * 0.62 * len(s)

# ------------------------------------------------------------- excalidraw -----
elements = []
def nonce(): return random.randint(100000, 999999)

def base_el(id_, type_, x, y, w, h, **kw):
    el = dict(id=id_, type=type_, x=x, y=y, width=w, height=h, angle=0,
             strokeColor=kw.get("stroke", INK), backgroundColor=kw.get("fill", "transparent"),
             fillStyle="solid", strokeWidth=kw.get("sw", 2), strokeStyle="solid",
             roughness=kw.get("rough", 1), opacity=100, groupIds=[], frameId=None,
             roundness=kw.get("round"), seed=nonce(), version=1, versionNonce=nonce(),
             isDeleted=False, boundElements=kw.get("bound", []), updated=1,
             link=None, locked=False)
    return el

def add_text(id_, s, x, y, fs, color=INK, container=None, align="center", valign="middle", bold=False):
    w, h = txt_w(s, fs), fs * 1.25
    el = base_el(id_, "text", x - w / 2, y - h / 2, w, h, stroke=color, sw=1, rough=0)
    el.update(text=s, rawText=s, originalText=s, fontSize=fs, fontFamily=2,
              textAlign=align, verticalAlign=valign,
              containerId=container, lineHeight=1.25, baseline=int(fs * 0.92))
    elements.append(el)
    return el

# --- title
add_text("title", "MÔ HÌNH KHÁI NIỆM STORAGEHUB — 17 thực thể · 28 quan hệ",
         1180, 1480 + 0, 28)  # dat lai x,y sau
elements[-1]["x"], elements[-1]["y"] = 40, 16
elements[-1]["textAlign"] = "left"

# --- entity boxes + nhan
for i, b in BOX.items():
    fill, stroke = COLORS[b["color"]]
    rect = base_el(i, "rectangle", b["x"], b["y"], b["w"], b["h"],
                   fill=fill, stroke=stroke, sw=3 if i == "hub" else 2,
                   round={"type": 3})
    elements.append(rect)
    add_text(i + "_t", b["name"], *center(i), b["fs"], container=i)

# --- arrows
for n, (s, e, verb, c1, c2) in enumerate(EDGES):
    scx, scy = center(s); ecx, ecy = center(e)
    sx, sy = edge_point(s, ecx, ecy)
    ex, ey = edge_point(e, scx, scy)
    aid = f"a{n}"
    arr = base_el(aid, "arrow", sx, sy, ex - sx, ey - sy, stroke=INK, sw=2)
    arr.update(points=[[0, 0], [ex - sx, ey - sy]],
               startBinding=dict(elementId=s, focus=0, gap=GAP),
               endBinding=dict(elementId=e, focus=0, gap=GAP),
               lastCommittedPoint=None, startArrowhead=None, endArrowhead=None)
    elements.append(arr)
    # nhan duy nhat gan lien line: dong tu + tuong quan (keo duoc theo line)
    t = LABEL_T.get((s, e), 0.5)
    mx, my = sx + t * (ex - sx), sy + t * (ey - sy)
    add_text(aid + "_t", f"{verb} ({c1}—{c2})", mx, my, FS_LABEL, container=aid)

# --- tu quan he cua HOP DONG (vong nho tren dinh)
b = BOX["contract"]
lx, ly = b["x"] + b["w"] / 2 - 25, b["y"]          # (1845,220)
pts = [(lx, ly), (lx + 22, ly - 52), (lx + 50, ly)]
arr = base_el("a_self", "arrow", pts[0][0], pts[0][1],
              pts[-1][0] - pts[0][0], pts[-1][1] - pts[0][1], stroke=INK, sw=2)
arr.update(points=[[p[0] - pts[0][0], p[1] - pts[0][1]] for p in pts],
           startBinding=dict(elementId="contract", focus=-0.2, gap=GAP),
           endBinding=dict(elementId="contract", focus=0.2, gap=GAP),
           lastCommittedPoint=None, startArrowhead=None, endArrowhead=None)
elements.append(arr)
add_text("a_self_t", "phụ lục của (1—N)", lx + 25, ly - 62, FS_LABEL, container="a_self")

# bo sung boundElements cho rect / arrow (Excalidraw can de label & binding song)
by_id = {el["id"]: el for el in elements}
for el in elements:
    cid = el.get("containerId")
    if cid and cid in by_id:
        owner = by_id[cid]
        if el["id"] not in [b["id"] for b in owner["boundElements"]]:
            owner["boundElements"].append({"id": el["id"], "type": "text"})
for n, (s, e, *_ ) in enumerate(EDGES):
    aid = f"a{n}"
    for eid in (s, e):
        if aid not in [b["id"] for b in by_id[eid]["boundElements"]]:
            by_id[eid]["boundElements"].append({"id": aid, "type": "arrow"})
for eid in ("contract",):
    if "a_self" not in [b["id"] for b in by_id[eid]["boundElements"]]:
        by_id[eid]["boundElements"].append({"id": "a_self", "type": "arrow"})

scene = dict(type="excalidraw", version=2, source="https://excalidraw.com",
             elements=elements, appState=dict(viewBackgroundColor="#ffffff", gridSize=None),
             files={})

with open("Conceptual_Model_StorageHub.excalidraw", "w", encoding="utf-8") as f:
    json.dump(scene, f, ensure_ascii=False, indent=1)
print("OK .excalidraw:", len(elements), "elements")

# ----------------------------------------------------------------- preview ----
W, H = 2350, 1330
svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
       f'<rect width="{W}" height="{H}" fill="#ffffff"/>',
       '<rect x="30" y="6" width="1480" height="40" fill="none"/>',
       f'<text x="40" y="34" font-family="Segoe UI, Arial" font-size="28" font-weight="bold" fill="#1e1e1e">'
       'MÔ HÌNH KHÁI NIỆM STORAGEHUB — 17 thực thể · 28 quan hệ</text>']

def esc(s): return s.replace("&", "&amp;").replace("<", "&lt;")

for n, (s, e, verb, c1, c2) in enumerate(EDGES):
    scx, scy = center(s); ecx, ecy = center(e)
    sx, sy = edge_point(s, ecx, ecy); ex, ey = edge_point(e, scx, scy)
    svg.append(f'<line x1="{sx:.0f}" y1="{sy:.0f}" x2="{ex:.0f}" y2="{ey:.0f}" stroke="#1e1e1e" stroke-width="2"/>')
    t = LABEL_T.get((s, e), 0.5)
    mx, my = sx + t * (ex - sx), sy + t * (ey - sy)
    label = f"{verb} ({c1}—{c2})"
    tw, th = txt_w(label, FS_LABEL), FS_LABEL * 1.25
    svg.append(f'<rect x="{mx-tw/2-4:.0f}" y="{my-th/2-2:.0f}" width="{tw+8:.0f}" height="{th+4:.0f}" fill="#ffffff" opacity="0.9"/>')
    svg.append(f'<text x="{mx:.0f}" y="{my+5:.0f}" text-anchor="middle" font-family="Segoe UI, Arial" font-size="{FS_LABEL}" fill="#1e1e1e">{esc(label)}</text>')

# self loop
b = BOX["contract"]; lx, ly = b["x"] + b["w"] / 2 - 25, b["y"]
svg.append(f'<path d="M {lx} {ly} L {lx+22} {ly-52} L {lx+50} {ly}" fill="none" stroke="#1e1e1e" stroke-width="2"/>')
svg.append(f'<text x="{lx+25}" y="{ly-58}" text-anchor="middle" font-family="Segoe UI, Arial" font-size="{FS_LABEL}">phụ lục của (1—N)</text>')

for i, b in BOX.items():
    fill, stroke = COLORS[b["color"]]
    svg.append(f'<rect x="{b["x"]}" y="{b["y"]}" width="{b["w"]}" height="{b["h"]}" fill="{fill}" stroke="{stroke}" stroke-width="{3 if i=="hub" else 2}"/>')
    cx, cy = center(i)
    svg.append(f'<text x="{cx:.0f}" y="{cy+7:.0f}" text-anchor="middle" font-family="Segoe UI, Arial" font-size="{b["fs"]}" fill="#1e1e1e">{esc(b["name"])}</text>')

svg.append('</svg>')
with open("preview.svg", "w", encoding="utf-8") as f:
    f.write("\n".join(svg))
print("OK preview.svg")
