# -*- coding: utf-8 -*-
"""基于 curl 的多线程分片下载器。

Python 的 TLS 与 GitHub CDN 握手不稳，改用 curl（实测可用）。
用法：python tools/curl_chunks.py <url> <out> [threads]
"""
import os
import subprocess
import sys
import threading
import time

URL = sys.argv[1]
OUT = sys.argv[2]
THREADS = int(sys.argv[3]) if len(sys.argv) > 3 else 8
CURL = "curl"


def sh(args):
    return subprocess.run(args, capture_output=True, text=True, timeout=90).stdout.strip()


def main():
    # 1) 解析最终跳转地址
    probe = sh([CURL, "-4", "-sS", "-o", "NUL", "-r", "0-100", "-w", "%{url_effective}",
                "-L", "--max-time", "40", "--connect-timeout", "15", URL])
    final = probe if probe.startswith("http") else URL

    # 2) 用 Range 探测拿总长度（该 CDN 对 HEAD 不返回 Content-Length）
    size = 0
    rng = sh([CURL, "-4", "-sS", "-D", "-", "-o", "NUL", "-r", "0-100", "-L",
              "--max-time", "40", "--connect-timeout", "15", final])
    for line in rng.splitlines():
        low = line.lower()
        if low.startswith("content-range:") and "/" in low:
            tail = low.split("/", 1)[1].strip()
            if tail.isdigit():
                size = int(tail)
    if size <= 0:
        # 退化：用完整下载头
        hdr = sh([CURL, "-4", "-sSI", "-L", "--max-time", "40", "--connect-timeout", "15", final])
        for line in hdr.splitlines():
            if line.lower().startswith("content-length:"):
                try:
                    size = int(line.split(":", 1)[1].strip())
                except ValueError:
                    pass
    print(f"size={size} final={final[:100]}", flush=True)
    if size <= 0:
        print("cannot determine size", file=sys.stderr)
        return 2

    with open(OUT, "wb") as f:
        f.truncate(size)

    chunk = (size + THREADS - 1) // THREADS
    done = {"n": 0}
    lock = threading.Lock()

    def worker(s, e):
        part = f"{OUT}.part{s}"
        pos = s
        for _ in range(80):
            if pos > e:
                break
            # 下载到分片临时文件，再合并（避免并发写同一文件不同区段的问题）
            r = subprocess.run(
                [CURL, "-4", "-sS", "-L", "--max-time", "300", "--connect-timeout", "20",
                 "-r", f"{pos}-{e}", "-o", part, final],
                capture_output=True, text=True)
            try:
                got = os.path.getsize(part)
            except OSError:
                got = 0
            if got <= 0:
                time.sleep(2)
                continue
            with open(OUT, "r+b") as f:
                with open(part, "rb") as p:
                    f.seek(pos)
                    f.write(p.read())
            with lock:
                done["n"] += got
            pos += got
            os.remove(part)
            if pos > e:
                break
        try:
            os.remove(part)
        except OSError:
            pass

    threads = []
    for i in range(THREADS):
        s = i * chunk
        e = min(size - 1, s + chunk - 1)
        if s > e:
            break
        t = threading.Thread(target=worker, args=(s, e), daemon=True)
        t.start()
        threads.append(t)

    last = 0
    while any(t.is_alive() for t in threads):
        time.sleep(3)
        with lock:
            d = done["n"]
        print(f"  {d*100.0/size:5.1f}%  {d/1048576:6.2f} MB  {(d-last)/3.0/1024:7.1f} KB/s",
              flush=True)
        last = d
    for t in threads:
        t.join()
    actual = os.path.getsize(OUT)
    print(f"done: {actual} / {size}", flush=True)
    return 0 if actual == size else 1


if __name__ == "__main__":
    sys.exit(main())
