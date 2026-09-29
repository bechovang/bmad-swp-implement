#!/usr/bin/env python
# Stitch edit driver: fixes known issues via edit_screens, re-downloads HTML+PNG.
# Usage: python edit_drive.py [key ...]   (no args = all edits)
import os
import json, os, subprocess, sys, time, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
os.chdir(HERE)
RPC_FILE = "tmp_rpc_edit.json"
RESP_FILE = "tmp_resp_edit.txt"

def say(m):
    print(m, flush=True)

def screens_map():
    m = {}
    for line in open("manifest.jsonl", encoding="utf-8"):
        line = line.strip()
        if not line:
            continue
        try:
            rec = json.loads(line)
            if rec.get("ok") and rec.get("screen"):
                m[rec["key"]] = rec["screen"]  # last ok wins
        except Exception:
            pass
    return m

def call_edit(screen, prompt):
    scr_id = screen.rstrip("/").split("/")[-1]  # strip projects/../screens/ prefix
    env = {"jsonrpc": "2.0", "id": 30, "method": "tools/call",
           "params": {"name": "edit_screens",
                      "arguments": {"projectId": "9794980915577530548",
                                    "selectedScreenIds": [scr_id], "prompt": prompt}}}
    with open(RPC_FILE, "w", encoding="utf-8") as f:
        json.dump(env, f, ensure_ascii=False)
    p = subprocess.run(
        ["curl", "-s", "--max-time", "540", "-X", "POST", "https://stitch.googleapis.com/mcp",
         "-H", "X-Goog-Api-Key: " + os.environ["STITCH_API_KEY"],
         "-H", "Content-Type: application/json",
         "-H", "Accept: application/json, text/event-stream",
         "-H", "MCP-Protocol-Version: 2025-06-18",
         "-d", "@" + RPC_FILE, "-o", RESP_FILE],
        capture_output=True, text=True)
    if p.returncode != 0:
        return None, "curl exit %s" % p.returncode
    raw = open(RESP_FILE, encoding="utf-8", errors="replace").read().strip()
    if not raw:
        return None, "empty response"
    try:
        data = json.loads(raw)
    except Exception:
        lines = [l[5:] for l in raw.splitlines() if l.startswith("data:")]
        if not lines:
            return None, "unparseable: " + raw[:250]
        data = json.loads(lines[-1])
    if "error" in data:
        return None, "rpc error: " + json.dumps(data["error"])[:400]
    # find screens anywhere in text payloads
    for item in data.get("result", {}).get("content", []):
        if item.get("type") != "text":
            continue
        try:
            payload = json.loads(item["text"])
        except Exception:
            continue
        stack = [payload]
        while stack:
            node = stack.pop()
            if isinstance(node, dict):
                if "htmlCode" in node and "downloadUrl" in node.get("screenshot", {}):
                    return node, None
                stack.extend(node.values())
            elif isinstance(node, list):
                stack.extend(node)
    return None, "no screen object in response"

def fetch(url, path):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=120) as r:
        blob = r.read()
    with open(path, "wb") as f:
        f.write(blob)
    return len(blob)

EDITS = [
    {"key": "F3-01-checkout-task", "prompt": "Fix text and numbers only, keep layout, colors and light mode unchanged. 1) Settlement preview card must read exactly: 'Held deposit 103.500 ₫ − damage fee 40.000 ₫ = Refund 63.500 ₫ to Lan' in large tabular numerals. 2) Replace every garbled/gibberish label with clean English: 'Receive checklist', 'Condition inspection', 'Damage fee', 'Settlement preview'. 3) Inline error under the reason field: 'A reason is required before settlement.' 4) Header: 'Checkout · RT-0871 · Unit M-2 · Lan Nguyen'."},
    {"key": "F4-02-policy-management", "prompt": "Fix text only, keep layout and light mode unchanged. 1) Brand in the top bar must read 'StorageHub' (not SaaSOps Hub). 2) Role chip text: 'Business Ops'. 3) Remove the 'Request Fiscal Cap Override' button — only one indigo primary button 'Save policy' on this screen. 4) Replace garbled gray microcopy with clean English."},
    {"key": "F5-03-staff-shifts", "prompt": "Fix styling and text, keep layout unchanged. 1) Top bar must be WHITE with a light bottom border (light mode) — not dark navy. 2) Replace garbled strings ('On duty, Onswev', 'Back to chandon...' etc.) with clean English: 'On duty', shift names 'Morning · Zone B', 'Afternoon · Zone C'. 3) Keep the red-bordered conflict cell and the red conflict banner as they are."},
    {"key": "F2-01-task-board", "prompt": "Fix text only, keep layout and colors unchanged. 1) Card codes must read exactly (monospace): BK-1042, RT-0871, SR-0032. 2) Replace gibberish card titles with: 'Check-in', 'Checkout', 'Cleaning', 'Support' with meta lines 'Lan Nguyen · 09:00', 'Unit M-2 · 14:00', 'Unit S-1 · 16:00', 'sticky door · Unit S-4'. 3) Only ONE indigo filled button on the screen; other actions render as ghost/outline buttons."},
    {"key": "F5-02-unit-management", "prompt": "Fix text and one action, keep layout unchanged. 1) The 'Fix status' select value must read 'Preparing'. 2) Add a 'Retire unit' action in the drawer action list, rendered with red text (destructive). 3) Replace garbled strings with clean English."},
    {"key": "F7-04-escalation-inbox", "prompt": "Fix layout of the decision panel and text, keep colors unchanged. 1) The two severity options must be TWO LARGE EQUAL-SIZE CARDS SIDE BY SIDE: red-tinted selected 'Mark severe — move M-2 to Maintenance and relocate the customer to M-5.' and neutral 'Return to staff with guidance' with a guidance textarea of the same card size. 2) Fix garbled strings: 'Sump pump failed; beyond on-site repair. Customer needs a unit.' 3) Ensure the access card does not overlap the queue list."},
]

def main():
    only = set(sys.argv[1:])
    smap = screens_map()
    for e in EDITS:
        key = e["key"]
        if only and key not in only:
            continue
        scr = smap.get(key)
        if not scr:
            say("SKIP %s: no screen id in manifest" % key)
            continue
        say("editing %s (%s)..." % (key, scr))
        node, err = call_edit(scr, e["prompt"])
        if not node:
            say("  FAIL %s: %s" % (key, err))
            continue
        try:
            hb = fetch(node["htmlCode"]["downloadUrl"], os.path.join("..", "mockups", key + ".html"))
            pb = fetch(node["screenshot"]["downloadUrl"], os.path.join("..", "mockups", key + ".png"))
            say("  OK %s html=%d png=%d" % (key, hb, pb))
        except Exception as ex:
            say("  DOWNLOAD-FAIL %s: %s" % (key, ex))
        time.sleep(3)
    say("edits complete")

if __name__ == "__main__":
    main()
