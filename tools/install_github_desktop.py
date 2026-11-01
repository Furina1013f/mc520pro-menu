# -*- coding: utf-8 -*-
"""安装 GitHub Desktop（免管理员，绿色版方式）。

流程：解压 zip 到 %LOCALAPPDATA%\\Programs\\GitHubDesktop → 建桌面快捷方式。
用法：python tools/install_github_desktop.py <zip路径>
"""
import os
import shutil
import subprocess
import sys
import zipfile

ZIP = sys.argv[1]
PROGRAMS = os.path.join(os.environ["LOCALAPPDATA"], "Programs")
TARGET = os.path.join(PROGRAMS, "GitHubDesktop")
DESKTOP = os.path.join(os.path.expanduser("~"), "Desktop")


def main():
    if not os.path.exists(ZIP):
        print(f"zip not found: {ZIP}", file=sys.stderr)
        return 2
    os.makedirs(TARGET, exist_ok=True)
    print(f"extracting to {TARGET} ...", flush=True)
    with zipfile.ZipFile(ZIP) as z:
        z.extractall(TARGET)

    # 找主程序
    exe = None
    for root, _dirs, files in os.walk(TARGET):
        for f in files:
            if f.lower() == "githubdesktop.exe":
                exe = os.path.join(root, f)
                break
        if exe:
            break
    if not exe:
        print("GitHubDesktop.exe not found in archive", file=sys.stderr)
        return 3
    print(f"exe: {exe}")

    # 桌面快捷方式
    os.makedirs(DESKTOP, exist_ok=True)
    lnk = os.path.join(DESKTOP, "GitHub Desktop.lnk")
    ps = (
        "$s=(New-Object -ComObject WScript.Shell).CreateShortcut('%s');"
        "$s.TargetPath='%s';$s.Save()" % (lnk, exe)
    )
    subprocess.run(["powershell", "-NoProfile", "-Command", ps], check=False)
    print(f"shortcut: {lnk}")
    print("done")
    return 0


if __name__ == "__main__":
    sys.exit(main())
