# -*- coding: utf-8 -*-
"""通过 GitHub Git Data API 一次性提交整个工作区（绕过 git 协议，只用 api.github.com）。

用法：
    set GITHUB_TOKEN=ghp_xxx
    python tools/push_via_api.py <repo> <branch> <message> [workdir]

适合 git push 被网络阻断、但 api.github.com 可用的场景。
"""
import base64
import json
import os
import subprocess
import sys

API = "https://api.github.com"
TOKEN = os.environ.get("GITHUB_TOKEN", "").strip()
REPO = sys.argv[1] if len(sys.argv) > 1 else "Furina1013f/mc520pro-menu"
BRANCH = sys.argv[2] if len(sys.argv) > 2 else "main"
MESSAGE = sys.argv[3] if len(sys.argv) > 3 else "commit via api"
WORKDIR = sys.argv[4] if len(sys.argv) > 4 else "."

SKIP_DIRS = {".git", "build", ".gradle", "run", "logs", ".gradle-home", "local-repo",
             "out", ".idea", ".vscode", "img_parts", "jdk21-x", "gradle-x"}
SKIP_EXT = {".zip", ".log", ".part", ".tmp"}
SKIP_FILES = {"proxy.err", "proxy.log", "runclient.log", "kdocs.html"}


def curl_json(method, path, payload=None, want_status=False):
    """用 curl 发请求（Python 的 TLS 对 GitHub 不稳，curl 可靠）。"""
    url = API + path
    tmp = os.path.join(os.environ.get("TEMP", "."), "_gh_req.json")
    args = ["curl", "-4", "-sS", "-X", method, "-H",
            "Authorization: Bearer " + TOKEN, "-H", "Accept: application/vnd.github+json",
            "-H", "Content-Type: application/json", "--connect-timeout", "20",
            "--max-time", "90", "-w", "\n__STATUS__%{http_code}"]
    if payload is not None:
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(payload, f)
        args += ["--data-binary", "@" + tmp]
    args.append(url)
    r = subprocess.run(args, capture_output=True, text=True, timeout=120,
                       encoding="utf-8", errors="replace")
    out = r.stdout or ""
    status = 0
    if "__STATUS__" in out:
        body, _, st = out.rpartition("__STATUS__")
        try:
            status = int(st.strip())
        except ValueError:
            status = 0
        out = body
    try:
        data = json.loads(out) if out.strip() else None
    except json.JSONDecodeError:
        data = out
    if want_status:
        return status, data
    if status and status >= 400:
        raise RuntimeError(f"{method} {path} -> {status}: {str(data)[:300]}")
    return data


def collect_files(root):
    """收集要提交的文件（跳过忽略项）。"""
    out = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in filenames:
            if fn in SKIP_FILES:
                continue
            ext = os.path.splitext(fn)[1].lower()
            if ext in SKIP_EXT:
                continue
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, root).replace("\\", "/")
            try:
                if os.path.getsize(full) > 5 * 1024 * 1024:
                    continue
            except OSError:
                continue
            out.append((rel, full))
    out.sort()
    return out


def main():
    if not TOKEN:
        print("no GITHUB_TOKEN", file=sys.stderr)
        return 2

    files = collect_files(WORKDIR)
    print(f"files to commit: {len(files)}", flush=True)

    # 1) 当前分支头部（空仓库可能没有）
    status, ref = curl_json("GET", f"/repos/{REPO}/git/ref/heads/{BRANCH}", want_status=True)
    parent = ref["object"]["sha"] if status == 200 and isinstance(ref, dict) else None
    print(f"parent: {parent}", flush=True)

    # 2) 创建 blobs
    tree = []
    for i, (rel, full) in enumerate(files, 1):
        with open(full, "rb") as f:
            data = f.read()
        blob = curl_json("POST", f"/repos/{REPO}/git/blobs", {
            "content": base64.b64encode(data).decode(),
            "encoding": "base64",
        })
        tree.append({"path": rel, "mode": "100644", "type": "blob", "sha": blob["sha"]})
        if i % 10 == 0 or i == len(files):
            print(f"  blobs {i}/{len(files)}", flush=True)

    # 3) 创建 tree
    payload = {"tree": tree}
    if parent:
        payload["base_tree"] = parent
    t = curl_json("POST", f"/repos/{REPO}/git/trees", payload)
    print(f"tree: {t['sha']}", flush=True)

    # 4) 创建 commit
    commit_payload = {"message": MESSAGE, "tree": t["sha"]}
    if parent:
        commit_payload["parents"] = [parent]
    c = curl_json("POST", f"/repos/{REPO}/git/commits", commit_payload)
    print(f"commit: {c['sha']}", flush=True)

    # 5) 更新/创建分支引用
    if parent:
        curl_json("PATCH", f"/repos/{REPO}/git/refs/heads/{BRANCH}", {"sha": c["sha"], "force": True})
    else:
        curl_json("POST", f"/repos/{REPO}/git/refs",
                  {"ref": f"refs/heads/{BRANCH}", "sha": c["sha"]})
    print(f"branch {BRANCH} -> {c['sha']}", flush=True)
    print("DONE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
