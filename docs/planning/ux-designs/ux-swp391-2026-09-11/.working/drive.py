#!/usr/bin/env python
# Stitch batch driver: reads prompts.json, generates each screen via curl JSON-RPC,
# downloads HTML+PNG to ../mockups/<key>.*, resumes from manifest.jsonl.
import os
import json, os, subprocess, sys, time, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
os.chdir(HERE)
MANIFEST = "manifest.jsonl"
RPC_FILE = "tmp_rpc.json"
RESP_FILE = "tmp_resp.txt"

CFG = json.load(open("prompts.json", encoding="utf-8"))
PROJECT = CFG["project"]
DESIGN_SYSTEM = CFG["designSystem"]
STYLE_TAIL = CFG["styleTail"]

def load_done():
    done = {}
    if os.path.isfile(MANIFEST):
        for line in open(MANIFEST, encoding="utf-8"):
            line = line.strip()
            if not line:
                continue
            try:
                rec = json.loads(line)
                if rec.get("ok"):
                    done[rec["key"]] = rec
            except Exception:
                pass
    return done

def log(rec):
    with open(MANIFEST, "a", encoding="utf-8") as f:
        f.write(json.dumps(rec, ensure_ascii=False) + "\n")

def say(msg):
    print(msg, flush=True)

def generate(prompt):
    env = {"jsonrpc": "2.0", "id": 20, "method": "tools/call",
           "params": {"name": "generate_screen_from_text",
                      "arguments": {"projectId": PROJECT, "designSystem": DESIGN_SYSTEM,
                                    "deviceType": "DESKTOP", "prompt": prompt}}}
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
        return None, "curl exit %s: %s" % (p.returncode, p.stderr[:300])
    raw = open(RESP_FILE, encoding="utf-8", errors="replace").read().strip()
    if not raw:
        return None, "empty response"
    try:
        data = json.loads(raw)
    except Exception:
        lines = [l[5:] for l in raw.splitlines() if l.startswith("data:")]
        if not lines:
            return None, "unparseable: " + raw[:300]
        data = json.loads(lines[-1])
    if "error" in data:
        return None, "rpc error: " + json.dumps(data["error"])[:300]
    for item in data.get("result", {}).get("content", []):
        if item.get("type") == "text":
            try:
                payload = json.loads(item["text"])
            except Exception:
                continue
            for comp in payload.get("outputComponents", []):
                scr = comp.get("design", {}).get("screens", [])
                if scr:
                    return scr[0], None
            return None, "no screens in payload: " + item["text"][:300]
    return None, "no content block"

def fetch(url, path):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=120) as r:
        blob = r.read()
    with open(path, "wb") as f:
        f.write(blob)
    return len(blob)

def main():
    only = set(sys.argv[1:])
    done = load_done()
    pending = [s for s in CFG["screens"] if s["key"] not in done or s["key"] in only]
    say("pending %d / %d screens" % (len(pending), len(CFG["screens"])))
    for i, s in enumerate(pending, 1):
        key = s["key"]
        say("[%d/%d] %s — generating (~60s)..." % (i, len(pending), key))
        scr, err = generate(s["prompt"] + " " + STYLE_TAIL)
        if not scr:
            say("  FAIL %s: %s" % (key, err))
            log({"key": key, "ok": False, "error": err})
            time.sleep(3)
            continue
        name = scr.get("name", "?")
        try:
            hb = fetch(scr["htmlCode"]["downloadUrl"], os.path.join("..", "mockups", key + ".html"))
            pb = fetch(scr["screenshot"]["downloadUrl"], os.path.join("..", "mockups", key + ".png"))
            log({"key": key, "ok": True, "screen": name, "html": hb, "png": pb})
            say("  OK %s html=%d png=%d" % (key, hb, pb))
        except Exception as e:
            log({"key": key, "ok": False, "screen": name, "error": "download: %s" % e})
            say("  DOWNLOAD-FAIL %s: %s (screen saved: %s)" % (key, e, name))
        time.sleep(4)
    say("batch complete")

if __name__ == "__main__":
    main()
