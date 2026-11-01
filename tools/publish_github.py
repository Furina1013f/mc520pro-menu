# -*- coding: utf-8 -*-
"""GitHub 发布助手。

用途：创建/更新仓库、打 tag、创建 Release、上传 Release 附件。
凭证：从环境变量 GITHUB_TOKEN 读取（绝不写入磁盘或日志）。

用法：
    set GITHUB_TOKEN=ghp_xxx
    python tools/publish_github.py --repo mc520pro-menu --version 1.1.0 --jar build/libs/mc520pro-menu-1.1.0.jar
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

API = "https://api.github.com"


def api(path, method="GET", data=None, token=None, raw=False):
    url = path if path.startswith("http") else API + path
    body = None
    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "mc520pro-publisher",
    }
    if token:
        headers["Authorization"] = "Bearer " + token
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            content = resp.read()
            if raw:
                return resp.status, content
            return resp.status, (json.loads(content) if content else None)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        return e.code, detail
    except Exception as e:  # noqa: BLE001
        return 0, str(e)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True, help="仓库名，如 mc520pro-menu")
    ap.add_argument("--owner", default="Furina1013f")
    ap.add_argument("--version", required=True, help="版本号，如 1.1.0")
    ap.add_argument("--jar", default=None, help="要附加到 Release 的 jar 路径")
    ap.add_argument("--notes", default=None, help="Release 说明文件")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        print("ERROR: 请先设置环境变量 GITHUB_TOKEN（PAT 需要 repo 权限）", file=sys.stderr)
        return 2

    repo_full = f"{args.owner}/{args.repo}"
    tag = f"v{args.version}"

    # 1) 确认登录身份
    status, me = api("/user", token=token)
    if status != 200:
        print(f"ERROR: token 无效或无权限（{status}）：{me}", file=sys.stderr)
        return 3
    print(f"[ok] 登录身份: {me.get('login')}")

    # 2) 创建仓库（已存在则忽略）
    status, body = api("/user/repos", "POST", {
        "name": args.repo,
        "description": "mc.520pro.top 服务器专属内置菜单（Fabric 1.21.8 客户端 mod）",
        "private": False,
        "auto_init": False,
        "has_wiki": False,
        "license_template": "mit",
    }, token=token)
    if status == 201:
        print(f"[ok] 已创建仓库 {repo_full}")
    elif status == 422:
        print(f"[ok] 仓库 {repo_full} 已存在")
    else:
        print(f"[warn] 创建仓库返回 {status}: {str(body)[:200]}")

    # 3) 创建 tag（若不存在）
    status, ref = api(f"/repos/{repo_full}/git/ref/tags/{tag}", token=token)
    if status != 200:
        status, body = api(f"/repos/{repo_full}/git/refs", "POST", {
            "ref": f"refs/tags/{tag}",
            "sha": "main",
        }, token=token)
        print(f"[{'ok' if status == 201 else 'warn'}] 创建 tag {tag}（{status}）")
        if status != 201:
            print(str(body)[:300])
    else:
        print(f"[ok] tag {tag} 已存在")

    # 4) 创建 Release
    notes = ""
    if args.notes and os.path.exists(args.notes):
        with open(args.notes, "r", encoding="utf-8") as f:
            notes = f.read()

    status, rel = api(f"/repos/{repo_full}/releases", "GET", token=token)
    existing = None
    if status == 200:
        for r in rel:
            if r.get("tag_name") == tag:
                existing = r
                break

    payload = {
        "tag_name": tag,
        "name": f"v{args.version}",
        "body": notes or f"mc520pro-menu v{args.version}",
        "draft": False,
        "prerelease": False,
    }
    if existing:
        status, body = api(f"/repos/{repo_full}/releases/{existing['id']}", "PATCH", payload, token=token)
        print(f"[{'ok' if status == 200 else 'warn'}] 更新 Release {tag}（{status}）")
        release_id = existing["id"] if status == 200 else None
    else:
        status, body = api(f"/repos/{repo_full}/releases", "POST", payload, token=token)
        print(f"[{'ok' if status == 201 else 'warn'}] 创建 Release {tag}（{status}）")
        if status != 201:
            print(str(body)[:300])
            return 4
        release_id = body["id"]

    # 5) 上传 jar 附件
    if args.jar and release_id and os.path.exists(args.jar):
        url = (f"https://uploads.github.com/repos/{repo_full}/releases/{release_id}/assets"
               f"?name={os.path.basename(args.jar)}")
        with open(args.jar, "rb") as f:
            content = f.read()
        req = urllib.request.Request(url, data=content, method="POST", headers={
            "Authorization": "Bearer " + token,
            "Content-Type": "application/octet-stream",
            "User-Agent": "mc520pro-publisher",
        })
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                print(f"[ok] 已上传附件 {os.path.basename(args.jar)}（{resp.status}）")
        except urllib.error.HTTPError as e:
            print(f"[warn] 附件上传失败 {e.code}: {e.read().decode('utf-8','replace')[:200]}")

    print(f"\n完成。仓库: https://github.com/{repo_full}")
    print(f"推送命令（在项目目录执行）：")
    print(f'  git remote add origin https://github.com/{repo_full}.git')
    print(f'  git push -u origin main')
    print(f'  git push origin {tag}')
    return 0


if __name__ == "__main__":
    sys.exit(main())
