# -*- coding: utf-8 -*-
"""
构建驱动：Gradle 需要从 maven.fabricmc.net 解析插件/依赖，但本机 Java 的 TLS 被定向阻断。
方案：反复跑 gradle，把日志里出现的 fabric 工件 URL 用 curl（经本地固定 IP 代理，带重试）
下载到 local-repo/（标准 maven 布局），下一轮 gradle 就从 file:// 本地源解析。
"""
import os
import re
import subprocess
import sys

PROJ = r"F:\Desktop\deepseek 工作区\mc520pro-mod"
JAVA = os.path.join(PROJ, "tools", "jdk21-x", "jdk-21.0.13+9")
GRADLE = os.path.join(PROJ, "tools", "gradle-x", "gradle-8.14.3", "bin", "gradle.bat")
LOCAL = os.path.join(PROJ, "local-repo")
LOG = os.path.join(PROJ, "build-driver.log")
PROXY = "http://127.0.0.1:18080"

URL_RE = re.compile(r"https://maven\.fabricmc\.net/[^\s'\"<>()\[\]]+")


def curl_to(url, out_path):
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    tmp = out_path + ".part"
    cmd = [
        "curl.exe", "-4", "-sS", "-x", PROXY, "-L",
        "--retry", "10", "--retry-all-errors",
        "--connect-timeout", "15", "--max-time", "300",
        "-o", tmp, "-w", "%{http_code}", url,
    ]
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=330)
        code = (r.stdout or "").strip()
        if code.endswith("200") and os.path.exists(tmp) and os.path.getsize(tmp) > 0:
            os.replace(tmp, out_path)
            return True
        if os.path.exists(tmp):
            os.remove(tmp)
        return False
    except Exception:
        if os.path.exists(tmp):
            try:
                os.remove(tmp)
            except OSError:
                pass
        return False


def run_gradle():
    env = dict(os.environ)
    env["JAVA_HOME"] = JAVA
    env["GRADLE_USER_HOME"] = os.path.join(PROJ, ".gradle-home")
    cmd = [GRADLE, "-p", PROJ, "--console=plain", "--info", "build"]
    try:
        with open(LOG, "w", encoding="utf-8", errors="replace") as f:
            p = subprocess.run(cmd, stdout=f, stderr=subprocess.STDOUT, env=env, timeout=2400)
        return p.returncode
    except subprocess.TimeoutExpired:
        return 124


def seed_from(log_text):
    urls = sorted(set(URL_RE.findall(log_text)))
    seeded = 0
    for url in urls:
        rel = url.split("maven.fabricmc.net/", 1)[1].strip()
        if not rel or rel.endswith(".module"):
            continue
        dst = os.path.join(LOCAL, rel.replace("/", os.sep))
        if os.path.exists(dst):
            continue
        ok = curl_to(url, dst)
        sib = None
        if url.endswith(".pom"):
            sib = url[:-4] + ".jar"
        elif url.endswith(".jar"):
            sib = url[:-4] + ".pom"
        if sib:
            rel2 = sib.split("maven.fabricmc.net/", 1)[1].strip()
            dst2 = os.path.join(LOCAL, rel2.replace("/", os.sep))
            if not os.path.exists(dst2):
                curl_to(sib, dst2)
        if ok:
            seeded += 1
            print("seeded:", rel, flush=True)
    return seeded


def main():
    os.makedirs(LOCAL, exist_ok=True)
    for rnd in range(1, 26):
        print(f"===== round {rnd} =====", flush=True)
        code = run_gradle()
        log = open(LOG, encoding="utf-8", errors="replace").read()
        if code == 0:
            print("BUILD OK", flush=True)
            return 0
        seeded = seed_from(log)
        print("\n".join(log.splitlines()[-14:]), flush=True)
        if seeded:
            print(f"round {rnd}: seeded {seeded} new artifacts", flush=True)
            continue
        if "compileJava" in log and re.search(r"\berror:", log):
            print("COMPILE ERROR - stopping", flush=True)
            return 2
        print("NO PROGRESS - stopping", flush=True)
        return 3
    print("ROUNDS EXHAUSTED", flush=True)
    return 4


if __name__ == "__main__":
    sys.exit(main())
