# -*- coding: utf-8 -*-
"""创建 Release 并上传 jar（纯 API，绕过 git 协议）。

用法：
    python tools/make_release.py <repo> <tag> <name> <jar路径> [notes文件]
"""
import base64
import json
import os
import subprocess
import sys

API = "https://api.github.com"
TOKEN = os.environ.get("GITHUB_TOKEN", "").strip()
REPO = sys.argv[1]
TAG = sys.argv[2]
NAME = sys.argv[3]
JAR = sys.argv[4]
NOTES = sys.argv[5] if len(sys.argv) > 5 else None


def api(method, path, payload=None, raw_body=None, headers=None, want_status=False):
    url = API + path
    tmp = os.path.join(os.environ.get("TEMP", "."), "_rel_req.bin")
    args = ["curl", "-4", "-sS", "-X", method, "-H",
            "Authorization: Bearer " + TOKEN,
            "-H", "Accept: application/vnd.github+json"]
    h = {"Authorization": "Bearer " + TOKEN, "Accept": "application/vnd.github+json"}
    if headers:
        for k, v in headers.items():
            args += ["-H", f"{k}: {v}"]
    if raw_body is not None:
        with open(tmp, "wb") as f:
            f.write(raw_body)
        args += ["--data-binary", "@" + tmp]
    elif payload is not None:
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False)
        args += ["-H", "Content-Type: application/json", "--data-binary", "@" + tmp]
    args.append(url)
    r = subprocess.run(args, capture_output=True, timeout=120,
                       encoding="utf-8", errors="replace")
    out = r.stdout or ""
    if want_status:
        return out
    try:
        return json.loads(out) if out.strip() else None
    except json.JSONDecodeError:
        return out


def main():
    if not TOKEN:
        print("no GITHUB_TOKEN", file=sys.stderr)
        return 2

    notes = ""
    if NOTES and os.path.exists(NOTES):
        with open(NOTES, "r", encoding="utf-8") as f:
            notes = f.read()
    if not notes:
        notes = f"## {TAG}\n\n安装：把 jar 放进 .minecraft/mods，重启游戏按 M 呼出菜单。\n"

    # 查是否已有该 tag 的 Release
    rels = api("GET", f"/repos/{REPO}/releases?per_page=50")
    existing = None
    if isinstance(rels, list):
        for r in rels:
            if r.get("tag_name") == TAG:
                existing = r
                break

    payload = {
        "tag_name": TAG,
        "name": NAME,
        "body": notes,
        "draft": False,
        "prerelease": False,
        "make_latest": "true",
    }

    if existing:
        print(f"release {TAG} 已存在，更新说明...")
        data = api("PATCH", f"/repos/{REPO}/releases/{existing['id']}", payload)
        rid = existing["id"]
        print(f"updated release id={rid}")
    else:
        print(f"创建 release {TAG}...")
        data = api("POST", f"/repos/{REPO}/releases", payload)
        if isinstance(data, dict) and data.get("id"):
            rid = data["id"]
            print(f"created release id={rid} url={data.get('html_url')}")
        else:
            print(f"FAILED: {str(data)[:400]}", file=sys.stderr)
            return 3

    # 上传 jar 附件
    if JAR and os.path.exists(JAR):
        fname = os.path.basename(JAR)
        with open(JAR, "rb") as f:
            jar_bytes = f.read()
        upload_url = (f"https://uploads.github.com/repos/{REPO}/releases/{rid}/assets"
                      f"?name={fname}")
        with open(os.path.join(os.environ.get("TEMP", "."), "_jar.bin"), "wb") as f:
            f.write(jar_bytes)
        r = api("POST", "", raw_body=jar_bytes,
                headers={"Content-Type": "application/octet-stream"},
                want_status=True)
        # 上面 api() 不支持换 base，直接用 uploads URL
        # 改用直接 curl
        tmp = os.path.join(os.environ.get("TEMP", "."), "_jar.bin")
        res = subprocess.run(
            ["curl", "-4", "-sS", "-X", "POST",
             "-H", "Authorization: Bearer " + TOKEN,
             "-H", "Content-Type: " + "application/octet-stream",
             "-H", "Accept: application/vnd.github+json",
             "--data-binary", "@" + tmp, "--connect-timeout", "30",
             "--max-time", "300", "-w", "__HTTP__%{http_code}", upload_url],
            capture_output=True, timeout=360, encoding="utf-8", errors="replace")
        out = res.stdout or ""
        code = out.rpartition("__HTTP__")[-1].strip() if "__HTTP__" in out else "?"
        print(f"upload jar: http={code}")
        try:
            j = json.loads(out.rpartition("__HTTP__")[0])
            if j.get("browser_download_url"):
                print(f"asset url: {j['browser_download_url']}")
                print(f"size: {j.get('size')} bytes")
        except Exception:
            print(out[:200])
    else:
        print(f"jar not found: {JAR}")

    print(f"\nRelease 完成: https://github.com/{REPO}/releases/tag/{TAG}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
