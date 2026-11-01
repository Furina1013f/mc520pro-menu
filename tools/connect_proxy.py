# -*- coding: utf-8 -*-
"""
极简 HTTPS CONNECT 代理：把 maven.fabricmc.net 固定到实测可直连的 Cloudflare IP，
其它域名按系统 DNS 正常解析。供 Gradle / curl 通过 127.0.0.1:18080 访问使用。

本机网络存在按 IP 段 / SNI 的间歇性干扰：Gradle 插件解析需要反复重试，
代理侧负责「多 IP 候选 + 记住成功 IP + 异常日志」，配合外层 build 重试循环使用。
"""
import socket
import threading

PIN = {
    "maven.fabricmc.net": [
        "104.16.10.1",
        "104.16.30.1",
        "104.17.10.1",
        "104.18.10.1",
        "104.19.10.1",
        "104.20.10.1",
        "104.26.10.1",
    ],
    "meta.fabricmc.net": [
        "104.16.10.1",
        "104.17.10.1",
        "104.18.10.1",
    ],
    # GitHub：本机 Go/Python 的 TLS 握手被干扰，curl 却能通。
    # 这里把 GitHub 相关域名固定到实测可达的 IP，供 gh / python 使用。
    "github.com": [
        "140.82.113.4",
        "140.82.114.4",
        "140.82.112.4",
        "140.82.116.4",
    ],
    "api.github.com": [
        "140.82.113.6",
        "140.82.114.6",
        "140.82.112.6",
    ],
    "release-assets.githubusercontent.com": [
        "185.199.108.133",
        "185.199.109.133",
        "185.199.110.133",
        "185.199.111.133",
    ],
    "objects.githubusercontent.com": [
        "185.199.108.133",
        "185.199.109.133",
        "185.199.110.133",
        "185.199.111.133",
    ],
    "codeload.github.com": [
        "140.82.113.10",
        "140.82.114.10",
    ],
}
LISTEN = ("127.0.0.1", 18080)

GOOD_IP = {}  # 每个域名最近一次成功的 IP
LOCK = threading.Lock()


def pipe(src, dst):
    try:
        while True:
            data = src.recv(65536)
            if not data:
                break
            dst.sendall(data)
    except Exception:
        pass
    finally:
        for s in (src, dst):
            try:
                s.shutdown(socket.SHUT_RDWR)
            except Exception:
                pass


def connect_pinned(host, port):
    ips = PIN.get(host)
    if not ips:
        return socket.create_connection((host, port), timeout=30)
    with LOCK:
        cached = GOOD_IP.get(host)
    order = [cached] if cached else []
    for ip in ips:
        if ip not in order:
            order.append(ip)
    last = None
    for ip in order:
        try:
            conn = socket.create_connection((ip, port), timeout=10)
            with LOCK:
                GOOD_IP[host] = ip
            print(f"  -> {host} via {ip}", flush=True)
            return conn
        except Exception as exc:
            last = exc
            print(f"  !! {host} via {ip} failed: {exc!r}", flush=True)
    raise last or OSError("no route to " + host)


def handle(conn):
    upstream = None
    try:
        buf = b""
        while b"\r\n\r\n" not in buf and len(buf) < 65536:
            chunk = conn.recv(4096)
            if not chunk:
                conn.close()
                return
            buf += chunk
        head = buf.split(b"\r\n\r\n", 1)[0].decode("latin-1")
        line = head.split("\r\n")[0]
        parts = line.split()
        if len(parts) < 2 or parts[0].upper() != "CONNECT":
            conn.sendall(b"HTTP/1.1 405 Method Not Allowed\r\n\r\n")
            conn.close()
            return
        hostport = parts[1]
        if ":" in hostport:
            host, port = hostport.rsplit(":", 1)
            port = int(port)
        else:
            host, port = hostport, 443
        print(f"CONNECT {host}:{port}", flush=True)
        upstream = connect_pinned(host, port)
        conn.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")
        threading.Thread(target=pipe, args=(conn, upstream), daemon=True).start()
        pipe(upstream, conn)
    except Exception as exc:
        print(f"!! handler error: {exc!r}", flush=True)
        try:
            if upstream is not None:
                upstream.close()
            conn.close()
        except Exception:
            pass


def main():
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(LISTEN)
    srv.listen(128)
    print(f"CONNECT proxy listening on {LISTEN[0]}:{LISTEN[1]}", flush=True)
    while True:
        client, _ = srv.accept()
        threading.Thread(target=handle, args=(client,), daemon=True).start()


if __name__ == "__main__":
    main()
