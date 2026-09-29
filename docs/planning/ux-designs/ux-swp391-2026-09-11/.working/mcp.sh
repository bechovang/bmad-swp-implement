#!/bin/bash
# Helper: call Stitch MCP (JSON-RPC over HTTP). Usage: mcp.sh <id> <method> <params-json>
ID="$1"; METHOD="$2"; PARAMS="${3:-{}}"
curl -s --max-time 540 -X POST https://stitch.googleapis.com/mcp \
  -H "X-Goog-Api-Key: ${STITCH_API_KEY:?set STITCH_API_KEY env var}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -d "{\"jsonrpc\":\"2.0\",\"id\":$ID,\"method\":\"$METHOD\",\"params\":$PARAMS}" | python -c "
import json,sys
raw = sys.stdin.read().strip()
if not raw:
    print('EMPTY_RESPONSE'); sys.exit(0)
try:
    data = json.loads(raw)
except Exception:
    lines = [l[5:] for l in raw.splitlines() if l.startswith('data:')]
    if not lines:
        print('UNPARSEABLE:', raw[:500]); sys.exit(0)
    data = json.loads(lines[-1])
if 'error' in data:
    print('RPC_ERROR:', json.dumps(data['error'])[:600])
else:
    r = data.get('result', {})
    c = r.get('content', [])
    if c:
        for item in c:
            if item.get('type') == 'text':
                print(item['text'][:3000])
    else:
        print(json.dumps(r)[:3000])
"
