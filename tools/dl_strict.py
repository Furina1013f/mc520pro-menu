# -*- coding: utf-8 -*-
"""严格分片下载器：每片校验长度，写入后 flush，最终校验总长。

用法：python tools/dl_strict.py <url> <out> <size> [threads]
"""
import os
import subprocess
import sys
import threading
import time

URL = sys.argv[1]
OUT = sys.argv[2]
SIZE = int(sys.argv[3])
N = int(sys.argv[4]) if len(sys.argv) > 4 else 16

lock = threading.Lock()
state = {"done": 0}


def fetch(start, end, tries=40):
    """下载 [start,end] 区间到临时文件，校验长度后返回路径。"""
    want = end - start + 1
    part = f"{OUT}.p{start}.tmp"
    for _ in range(tries):
        if os.path.exists(part):
            try:
                os.remove(part)
            except OSError:
                pass
        subprocess.run(
            ["curl", "-4", "-sS", "-L", "--max-time", "300", "--connect-timeout", "20",
             "-r", f"{start}-{end}", "-o", part, URL],
            capture_output=True)
        try:
            got = os.path.getsize(part)
        except OSError:
            got = 0
        if got == want:
            return part, want
        # 不对就丢弃重下（可能被 CDN 截断）
        try:
            os.remove(part)
        except OSError:
            pass
        time.sleep(1.5)
    return None, 0


def worker(start, end):
    part, got = fetch(start, end)
    if part is None:
        print(f"  !! failed range {start}-{end}", flush=True)
        return
    with lock:
        with open(OUT, "r+b") as f:
            with open(part, "rb") as p:
                data = p.read()
            f.seek(start)
            f.write(data)
            f.flush()
            os.fsync(f.fileno())
            state["done"] += len(data)
    try:
        os.remove(part)
    except OSError:
        pass


def main():
    with open(OUT, "wb") as f:
        f.truncate(SIZE)
    chunk = (SIZE + N - 1) // N
    threads = []
    for i in range(N):
        s = i * chunk
        e = min(SIZE - 1, s + chunk - 1)
        if s > e:
            break
        t = threading.Thread(target=worker, args=(s, e), daemon=True)
        t.start()
        threads.append(t)

    last = 0
    while any(t.is_alive() for t in threads):
        time.sleep(4)
        with lock:
            d = state["done"]
        print(f"  {d * 100.0 / SIZE:5.1f}%  {d / 1048576:7.2f} MB  "
              f"{(d - last) / 4.0 / 1024:7.1f} KB/s", flush=True)
        last = d
    for t in threads:
        t.join()

    actual = os.path.getsize(OUT)
    print(f"final {actual} / {SIZE}", flush=True)
    if actual != SIZE:
        return 1
    # 头部必须是 zip 魔数
    with open(OUT, "rb") as f:
        magic = f.read(4)
    if magic != b"PK\x03\x04":
        print(f"bad magic: {magic!r}", flush=True)
        return 2
    print("zip magic OK", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
