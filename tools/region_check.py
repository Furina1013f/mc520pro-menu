# -*- coding: utf-8 -*-
"""分区检测（按 GUI 逻辑坐标 ×2 = PNG 像素）。

重要：截图是 framebuffer（854x480），而 MC 的 GUI 逻辑空间是 427x240（guiScale=2）。
绘制代码里的坐标是逻辑坐标，这里统一 ×2 后再取样。
"""
from PIL import Image

base = r"F:\Desktop\deepseek 工作区\mc520pro-mod\run\screenshots\screenshots"
S = 2  # framebuffer / GUI 比例


def bright_ratio(px, x0, y0, x1, y1, thr=150):
    n = 0
    total = 0
    for y in range(max(0, y0), max(0, y1)):
        for x in range(max(0, x0), max(0, x1)):
            r, g, b = px[x, y]
            total += 1
            if max(r, g, b) > thr:
                n += 1
    return n, total, (n * 100.0 / total if total else 0)


def report(name, regions):
    p = f"{base}\\{name}.png"
    try:
        img = Image.open(p).convert("RGB")
    except Exception as e:
        print(f"{name}: {e}")
        return
    px = img.load()
    w, h = img.size
    print(f"=== {name} ({w}x{h}) ===")
    for label, (x0, y0, x1, y1) in regions.items():
        X0, Y0, X1, Y1 = x0 * S, y0 * S, x1 * S, y1 * S
        X1 = min(X1, w)
        Y1 = min(Y1, h)
        n, total, ratio = bright_ratio(px, X0, Y0, X1, Y1)
        flag = "OK" if n > 12 else "!! 可能缺字/空白"
        print(f"  {label:20s} gui[{x0},{y1}] px[{X0},{X1}] 亮={n:5d} ({ratio:5.2f}%) {flag}")


def wizard_regions():
    """向导页：pw=min(400,427-40)=387, ph=100+42n, px=(427-387)//2=20"""
    pw = min(400, 427 - 40)
    ph = 100 + 42
    px = (427 - pw) // 2
    py = (240 - ph) // 2
    return {
        "标题栏": (px + 8, py + 4, px + 250, py + 30),
        "参数标签": (px + 16, py + 52, px + 360, py + 65),
        "输入框区": (px + 20, py + 64, px + 360, py + 84),
        "预览行": (px + 14, py + ph - 62, px + pw - 14, py + ph - 42),
        "底部按钮": (px + 14, py + ph - 34, px + pw - 14, py + ph - 12),
    }


def about_regions():
    pw = min(420, 427 - 40)
    ph = min(240 - 32, 262)
    px = (427 - pw) // 2
    py = (240 - ph) // 2
    return {
        "标题栏": (px + 8, py + 4, px + 300, py + 30),
        "作者行": (px + 14, py + 38, px + 200, py + 52),
        "状态行": (px + 14, py + 54, px + 400, py + 68),
        "更新日志": (px + 14, py + 88, px + pw - 14, py + ph - 44),
        "底部按钮": (px + 14, py + ph - 32, px + pw - 14, py + ph - 10),
    }


def menu_regions():
    pw = min(460, 427 - 16)
    px = (427 - pw) // 2
    return {
        "顶部横幅": (px + 8, 16 + 4, px + pw - 8, 16 + 34),
        "页签行": (px + 12, 16 + 38 + 4, px + pw - 12, 16 + 38 + 4 + 30),
        "卡片区": (px + 12, 16 + 100, px + pw - 12, 16 + 170),
        "底部按钮": (px + 12, 16 + 170, px + pw - 12, 16 + 205),
    }


for n in ("v9_tab0_tp", "v9_tab1_res", "v9_tab6_econ"):
    report(n, menu_regions())
for n in ("v9_wizard_player", "v9_wizard_number"):
    report(n, wizard_regions())
report("v9_about", about_regions())
report("v9_cmds", {
    "标题栏": (20, 16, 300, 44),
    "信息行": (20, 50, 400, 64),
    "网格区": (20, 66, 400, 180),
    "底部按钮": (20, 186, 410, 210),
})
