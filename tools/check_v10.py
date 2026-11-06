# -*- coding: utf-8 -*-
"""快速分析 v10 截图。"""
from PIL import Image

base = r"F:\Desktop\deepseek 工作区\mc520pro-mod\run\screenshots\screenshots"
names = ["v10_tab0_tp", "v10_tab1_res", "v10_tab6_econ", "v10_players", "v10_about", "v10_wizard_player"]

for n in names:
    p = f"{base}\\{n}.png"
    try:
        img = Image.open(p).convert("RGB")
    except Exception as e:
        print(f"{n}: {e}")
        continue
    px = img.load()
    w, h = img.size
    # 统计非背景像素（亮度 > 30 或 < 10 的都算内容）
    content = 0
    total = w * h
    for y in range(0, h, 3):
        for x in range(0, w, 3):
            r, g, b = px[x, y]
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum > 30 and lum < 250:
                content += 1
    pct = content * 9 * 100.0 / total
    print(f"{n}: {w}x{h} 内容占比 {pct:.1f}%")
