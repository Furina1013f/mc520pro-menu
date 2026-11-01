# -*- coding: utf-8 -*-
"""GitHub Device Flow 轮询：授权完成后换取访问令牌。

用法：python tools/device_flow.py <client_id> <device_code>
输出：GITHUB_TOKEN=<token>（仅打印到 stdout，由调用方注入环境变量）
"""
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

CLIENT_ID = sys.argv[1] if len(sys.argv) > 1 else "Iv1.b507a08c87ecfe98"
DEVICE_CODE = sys.argv[2] if len(sys.argv) > 2 else ""


def post(url, data):
    body = urllib.parse.urlencode(data).encode()
    req = urllib.request.Request(url, data=body, headers={
        "Accept": "application/json",
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": "mc520pro-publisher",
    })
    last = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=45) as r:
                return r.status, json.loads(r.read())
        except urllib.error.HTTPError as e:
            try:
                return e.code, json.loads(e.read())
            except Exception:
                return e.code, {}
        except Exception as e:  # 网络抖动 → 重试
            last = e
            time.sleep(3)
    raise last


if not DEVICE_CODE:
    print("no device code", file=sys.stderr)
    sys.exit(2)

deadline = time.time() + 900
interval = 5
while time.time() < deadline:
    st, body = post("https://github.com/login/oauth/access_token", {
        "client_id": CLIENT_ID,
        "device_code": DEVICE_CODE,
        "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
    })
    if st == 200 and isinstance(body, dict) and body.get("access_token"):
        print("GITHUB_TOKEN=" + body["access_token"])
        sys.exit(0)
    err = (body or {}).get("error") if isinstance(body, dict) else "unknown"
    if err == "slow_down":
        interval += 5
    elif err in ("authorization_pending",):
        pass
    elif err in ("expired_token", "access_denied", "incorrect_device_code"):
        print("device flow failed: " + str(err), file=sys.stderr)
        sys.exit(3)
    else:
        print("unexpected: " + str(body)[:200], file=sys.stderr)
        sys.exit(4)
    time.sleep(interval)

print("timeout waiting for authorization", file=sys.stderr)
sys.exit(5)
