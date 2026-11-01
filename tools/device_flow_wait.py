# -*- coding: utf-8 -*-
"""健壮的 GitHub Device Flow 轮询：网络抖动自动重试，直到拿到令牌或超时。

用法：python tools/device_flow_wait.py <device_code> [minutes]
成功时把令牌写入 %TEMP%/gh_token.txt 并打印 TOKEN_OK。
"""
import os
import sys
import time
import urllib.parse

CLIENT_ID = "Iv1.b507a08c87ecfe98"
DEVICE_CODE = sys.argv[1] if len(sys.argv) > 1 else ""
MINUTES = float(sys.argv[2]) if len(sys.argv) > 2 else 14.0
OUT = os.path.join(os.environ.get("TEMP", "."), "gh_token.txt")


def poll_once():
    """用 curl 轮询一次（curl 的 TLS 在本机比 python 稳）。"""
    import subprocess
    payload = urllib.parse.urlencode({
        "client_id": CLIENT_ID,
        "device_code": DEVICE_CODE,
        "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
    })
    for extra in ([], ["-x", "http://127.0.0.1:18080"]):
        cmd = ["curl", "-4", "-sS", "-X", "POST", "-H", "Accept: application/json",
               "-d", payload, "--connect-timeout", "12", "--max-time", "25"] + extra + [
               "https://github.com/login/oauth/access_token"]
        try:
            r = subprocess.run(cmd, capture_output=True, text=True, timeout=40)
            if r.stdout and "access_token" in r.stdout:
                return r.stdout
            if r.stdout and "expired_token" in r.stdout:
                return "EXPIRED"
        except Exception:
            pass
    return ""


def main():
    if not DEVICE_CODE:
        print("no device code", file=sys.stderr)
        return 2
    deadline = time.time() + MINUTES * 60
    n = 0
    while time.time() < deadline:
        n += 1
        res = poll_once()
        if res == "EXPIRED":
            print("EXPIRED", flush=True)
            return 3
        if res and '"access_token"' in res:
            import json
            try:
                token = json.loads(res)["access_token"]
            except Exception:
                token = res.split('"access_token":"')[1].split('"')[0]
            with open(OUT, "w", encoding="utf-8") as f:
                f.write(token)
            print(f"TOKEN_OK len={len(token)} after {n} polls", flush=True)
            return 0
        if n % 10 == 0:
            print(f"  ...still pending ({n} polls)", flush=True)
        time.sleep(5)
    print("TIMEOUT", flush=True)
    return 5


if __name__ == "__main__":
    sys.exit(main())
