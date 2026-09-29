# -*- coding: utf-8 -*-
"""Render mermaid/*.mmd -> images/*.png (V3 update 2026-09-18).

- State charts: GET https://mermaid.ink/img/<base64url-json>?type=png&width=1800
  (can gia User-Agent de khong bi chan 403)
- ERD: dai hon gioi han URL GET (HTTP 414) -> POST https://kroki.io/mermaid/png
  voi Content-Type: text/plain

Chay:  python render_v3.py            (render tat ca)
       python render_v3.py erd        (chi ERD)
       python render_v3.py state-reservation state-contract ...
"""
import base64
import json
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).parent
MERMAID_DIR = HERE / "mermaid"
IMG_DIR = HERE / "images"
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StorageHub-Doc-Render/1.0"


def render_state_chart(name: str) -> None:
    src = (MERMAID_DIR / f"{name}.mmd").read_text(encoding="utf-8")
    payload = json.dumps({"code": src, "mermaid": {"theme": "default"}}, ensure_ascii=False)
    b64 = base64.urlsafe_b64encode(payload.encode("utf-8")).decode("ascii").rstrip("=")
    url = f"https://mermaid.ink/img/{b64}?type=png&width=1800"
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = resp.read()
    out = IMG_DIR / f"{name}.png"
    out.write_bytes(data)
    print(f"[OK] {out.relative_to(HERE)} ({len(data):,} bytes)")


def render_erd() -> None:
    src = (MERMAID_DIR / "erd.mmd").read_text(encoding="utf-8")
    req = urllib.request.Request(
        "https://kroki.io/mermaid/png",
        data=src.encode("utf-8"),
        headers={"Content-Type": "text/plain", "User-Agent": UA},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = resp.read()
    out = IMG_DIR / "erd.png"
    out.write_bytes(data)
    print(f"[OK] {out.relative_to(HERE)} ({len(data):,} bytes)")


def main() -> None:
    IMG_DIR.mkdir(exist_ok=True)
    targets = sys.argv[1:]
    state_charts = [
        "state-unit",
        "state-reservation",
        "state-contract",
        "state-addendum",
        "state-ticket",
        "state-task",
        "state-payment",
    ]
    if not targets:
        render_erd()
        targets = state_charts
    for t in targets:
        if t == "erd":
            continue
        render_state_chart(t)


if __name__ == "__main__":
    main()
