#!/usr/bin/env python
# Stitch MCP caller. Usage: python mcp.py <tool> [arguments-json-file-or-inline]
import json, sys, urllib.request

URL = "https://stitch.googleapis.com/mcp"
HEADERS = {
    "X-Goog-Api-Key": os.environ["STITCH_API_KEY"],
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
    "MCP-Protocol-Version": "2025-06-18",
}

def call(method, params, timeout=540):
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params}).encode()
    req = urllib.request.Request(URL, data=body, headers=HEADERS, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as r:
        raw = r.read().decode().strip()
    if not raw:
        return {"raw": "EMPTY_RESPONSE"}
    try:
        return json.loads(raw)
    except Exception:
        lines = [l[5:] for l in raw.splitlines() if l.startswith("data:")]
        return json.loads(lines[-1]) if lines else {"raw": raw[:800]}

def main():
    tool = sys.argv[1]
    args = {}
    if len(sys.argv) > 2 and sys.argv[2].strip():
        a = sys.argv[2].strip()
        import os
        if os.path.isfile(a):
            args = json.load(open(a, encoding="utf-8"))
        else:
            args = json.loads(a)
    data = call("tools/call", {"name": tool, "arguments": args})
    if "error" in data:
        print("RPC_ERROR:", json.dumps(data["error"])[:800]); return 1
    r = data.get("result", {})
    for item in r.get("content", []):
        if item.get("type") == "text":
            print(item["text"][:4000])
    if not r.get("content"):
        print(json.dumps(r)[:4000])
    return 0

if __name__ == "__main__":
    sys.exit(main())
