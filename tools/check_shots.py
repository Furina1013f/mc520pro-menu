# -*- coding: utf-8 -*-
"""客观检查截图：面板边界、空白区域、文字像素密度。"""
import sys
from PIL import Image

names = ["v8_tab0_tp", "v8_tab1_res", "v8_tab6_econ", "v8_wizard", "v8_about", "v8_cmds"]
base = r"F:\Desktop\deepseek 工作区\mc520pro-mod\run\screenshots\screenshots"

for n in names:
    p = f"{base}\\{n}.png"
    try:
        img = Image.open(p).convert("RGB")
    except Exception as e:
        print(f"{n}: 打开失败 {e}")
        continue
    w, h = img.size
    px = img.load()
    # 找出非背景（暗紫面板）范围：亮度 > 60 视为内容
    minx, miny, maxx, maxy = w, h, -1, -1
    light = 0
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            r, g, b = px[x, y]
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum > 55:
                light += 1
                if x < minx: minx = x
                if y < miny: miny = y
                if x > maxx: maxx = x
                if y > maxy: maxy = y
    total = (w // 2) * (h // 2)
    print(f"=== {n} ({w}x{h}) ===")
    print(f"  内容包围盒: x[{minx},{maxx}] y[{miny},{maxy}]  占比 {light*100.0/max(1,total):.1f}%")
    # 检查底部 40px 是否有内容（按钮区）
    bottom_light = 0
    for y in range(h - 40, h, 2):
        for x in range(0, w, 2):
            r, g, b = px[x, y]
            if 0.299 * r + 0.587 * g + 0.114 * b > 55:
                bottom_light += 1
    print(f"  底部40px亮点: {bottom_light}")