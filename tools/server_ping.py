# -*- coding: utf-8 -*-
"""对 mc.520pro.top 做 Minecraft Server List Ping，输出服务器版本/MOTD/玩家数/模组信息。"""
import json
import socket
import struct
import sys

HOST = "mc.520pro.top"
PORT = 25565
PROTO = 772  # 1.21.8 协议版本


def pack_varint(v):
    out = b""
    while True:
        part = v & 0x7F
        v >>= 7
        if v:
            out += bytes([part | 0x80])
        else:
            out += bytes([part])
            return out


def read_varint(sock):
    n = 0
    shift = 0
    while True:
        b = sock.recv(1)
        if not b:
            raise EOFError("closed")
        val = b[0]
        n |= (val & 0x7F) << shift
        shift += 7
        if not val & 0x80:
            return n


def packet(pid, data):
    body = pack_varint(pid) + data
    return pack_varint(len(body)) + body


def main():
    with socket.create_connection((HOST, PORT), timeout=10) as s:
        host_b = HOST.encode()
        hs = (pack_varint(PROTO) + pack_varint(len(host_b)) + host_b
              + struct.pack(">H", PORT) + pack_varint(1))
        s.sendall(packet(0, hs))
        s.sendall(packet(0, b""))
        length = read_varint(s)
        pid = read_varint(s)
        remaining = length - 1
        data = b""
        while len(data) < remaining:
            chunk = s.recv(remaining - len(data))
            if not chunk:
                break
            data += chunk
    i = 0
    shift = 0
    slen = 0
    while True:
        b = data[i]
        i += 1
        slen |= (b & 0x7F) << shift
        shift += 7
        if not b & 0x80:
            break
    raw = data[i:i + slen].decode("utf-8", "replace")
    try:
        info = json.loads(raw)
    except json.JSONDecodeError:
        print("RAW:", raw)
        return
    print(json.dumps(info, ensure_ascii=False, indent=2)[:4000])
    ver = info.get("version", {})
    print("\n--- summary ---")
    print("version.name :", ver.get("name"))
    print("protocol     :", ver.get("protocol"))
    print("players      : %s/%s" % (info.get("players", {}).get("online"),
                                   info.get("players", {}).get("max")))
    if "modinfo" in info:
        modinfo = info.get("modinfo") or {}
        mods = modinfo.get("modList") or []
        print("modinfo.type :", modinfo.get("type"))
        print("mods(%d)     :" % len(mods))
        for m in mods:
            print("   -", m.get("modid"), m.get("version"))
    else:
        print("modinfo      : none (Forge/NeoForge 才会带)")


if __name__ == "__main__":
    sys.exit(main())
