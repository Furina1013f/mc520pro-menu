# -*- coding: utf-8 -*-
"""多线程分片下载器（应对 GitHub CDN 单连接挂起）。

用法：python tools/fast_download.py <url> <out> [threads]
"""
import os
import sys
import threading
import time
import urllib.request

URL = sys.argv[1]
OUT = sys.argv[2]
THREADS = int(sys.argv[3]) if len(sys.argv) > 3 else 8


def head_size(url):
    req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "mc520pro-dl"})
    with urllib.request.urlopen(req, timeout=45) as r:
        return int(r.headers.get("Content-Length", 0)), r.geturl()


def worker(url, out, start, end, idx, state, lock):
    pos = start
    tries = 0
    while pos <= end and tries < 60:
        tries += 1
        try:
            req = urllib.request.Request(url, headers={
                "User-Agent": "mc520pro-dl",
                "Range": f"bytes={pos}-{end}",
            })
            with urllib.request.urlopen(req, timeout=40) as r:
                with open(out, "r+b") as f:
                    f.seek(pos)
                    while True:
                        chunk = r.read(65536)
                        if not chunk:
                            break
                        f.write(chunk)
                        pos += len(chunk)
                        with lock:
                            state["done"] += len(chunk)
            if pos > end:
                return
        except Exception:
            time.sleep(1.5)
            continue


def main():
    size, real = head_size(URL)
    print(f"size={size/1048576:.2f} MB  final={real}", flush=True)
    if size <= 0:
        print("cannot determine size", file=sys.stderr)
        return 2
    with open(OUT, "wb") as f:
        f.truncate(size)

    chunk = (size + THREADS - 1) // THREADS
    state = {"done": 0}
    lock = threading.Lock()
    threads = []
    for i in range(THREADS):
        s = i * chunk
        e = min(size - 1, s + chunk - 1)
        if s > e:
            break
        t = threading.Thread(target=worker,
                             args=(real, OUT, s, e, i, state, lock), daemon=True)
        t.start()
        threads.append(t)

    last = 0
    while any(t.is_alive() for t in threads):
        time.sleep(3)
        with lock:
            done = state["done"]
        pct = done * 100.0 / size
        speed = (done - last) / 3.0 / 1024
        last = done
        print(f"  {pct:5.1f}%  {done/1048576:6.2f} MB  {speed:7.1f} KB/s", flush=True)
    for t in threads:
        t.join()

    actual = os.path.getsize(OUT)
    print(f"done: {actual} bytes (expect {size})", flush=True)
    return 0 if actual == size else 1


if __name__ == "__main__":
    sys.exit(main())
