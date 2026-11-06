# -*- coding: utf-8 -*-
"""把 Windows 等线字体（Deng.ttf）渲染成 MC 位图字体（glyph png + json）。

输出：
  assets/mc520pro/font/dengx/*.png     每页 256 字符
  assets/mc520pro/font/dengx/*.json    每页配置（含 advances）
  assets/mc520pro/font/dengx.json      字体定义（BMP 平面 0-3 + ascii）

用法：python tools/gen_bitmap_font.py <assets根目录>
"""
import json
import math
import os
import sys
from PIL import Image, ImageDraw, ImageFont

OUT_ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
FONT_NAME = "dengx"
FONT_PATH = r"C:\Windows\Fonts\Deng.ttf"
PIXEL_SIZE = 18          # 字体像素高（MC 默认字体是 8px 高 + 8px 行距，我们用 9+9=18 更清晰）
ROWS_PER_PAGE = 16
COLS_PER_PAGE = 16
PER_PAGE = ROWS_PER_PAGE * COLS_PER_PAGE

# 要覆盖的码点范围：ASCII + CJK 常用区（按频率精选，控制体积）
RANGES = [
    (0x0020, 0x007E),   # ASCII
    (0x00A0, 0x00FF),   # 拉丁补充
    (0x2000, 0x206F),   # 常用标点（… · — 等）
    (0x3000, 0x303F),   # CJK 标点
]


def collect_codepoints():
    cps = []
    for lo, hi in RANGES:
        cps.extend(range(lo, hi + 1))
    return cps


def main():
    font_dir = os.path.join(OUT_ROOT, "assets", "mc520pro", "textures", "font", FONT_NAME)
    os.makedirs(font_dir, exist_ok=True)

    # 从 mod 源码提取实际用到的 CJK 字符 + 常用 3500 字
    src_chars = set()
    java_root = os.path.join(OUT_ROOT, "assets")
    src_dir = os.path.join(OUT_ROOT, "..", "src", "main", "java")
    for dirpath, _dirs, files in os.walk(src_dir):
        for fn in files:
            if fn.endswith(".java"):
                with open(os.path.join(dirpath, fn), encoding="utf-8", errors="ignore") as f:
                    src_chars.update(f.read())
    src_cjk = [c for c in src_chars if 0x4E00 <= ord(c) <= 0x9FFF]
    print(f"CJK chars found in source: {len(src_cjk)}")

    cps = collect_codepoints()
    cps.extend(ord(c) for c in src_cjk)
    cps = sorted(set(cps))
    print(f"codepoints: {len(cps)}")

    font = ImageFont.truetype(FONT_PATH, PIXEL_SIZE)
    # 预测量所有字符宽度
    advances = {}
    for cp in cps:
        ch = chr(cp)
        try:
            w = font.getlength(ch)
        except Exception:
            w = PIXEL_SIZE
        adv = max(2, min(28, math.ceil(w) + 1))
        advances[cp] = adv
    max_advance = max(advances.values())
    print(f"max advance: {max_advance}")

    pages = math.ceil(len(cps) / PER_PAGE)
    # 按 256 字符一页：MC json 的 chars 数组第 i 项对应码点 i*256..i*256+255
    # 重新按码点组织分页
    cp_set = set(cps)
    max_cp = max(cps) + 1
    npages = math.ceil(max_cp / PER_PAGE)
    print(f"pages needed: {npages}")

    all_chars = []
    for p in range(npages):
        page_lo = p * PER_PAGE
        page_cps = [c for c in range(page_lo, min(page_lo + PER_PAGE, max_cp)) if c in cp_set]
        if not page_cps:
            continue
        # 每个 glyph 占 (adv+1) x (PIXEL_SIZE+2) 格子；页宽按本页字符 advance 累加
        cell_h = PIXEL_SIZE + 2
        cells = []
        x_cursor = 0
        for cp in page_cps:
            adv = advances[cp]
            cells.append((cp, x_cursor, adv))
            x_cursor += adv + 1
        img_w = x_cursor
        img = Image.new("RGBA", (img_w, ROWS_PER_PAGE * cell_h), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        # chars 数组：16 行（每行 16 字符），行内字符按码点顺序
        rows = []
        for r in range(ROWS_PER_PAGE):
            line = ""
            for c in range(COLS_PER_PAGE):
                cp = page_lo + r * COLS_PER_PAGE + c
                if cp in cp_set:
                    line += chr(cp)
                else:
                    line += "\0"
            rows.append(line)
        for cp, x0, adv in cells:
            idx = cp - page_lo
            col = idx % COLS_PER_PAGE
            row = idx // COLS_PER_PAGE
            y0 = row * cell_h
            d.text((x0, y0), chr(cp), font=font, fill=(255, 255, 255, 255))
        png_name = f"page{p}.png"
        img.save(os.path.join(font_dir, png_name), "PNG")
        # file 路径不带 textures/（引擎自动补全）；png 实际写入 assets/mc520pro/textures/font/
        all_chars.append((f"mc520pro:font/{FONT_NAME}/page{p}.png", rows))
        print(f"  page{p}: {len(page_cps)} glyphs, png {img_w}x{ROWS_PER_PAGE * cell_h}")

    font_def = {"providers": [
        {"type": "bitmap", "file": file, "height": PIXEL_SIZE + 2, "ascent": PIXEL_SIZE - 2,
         "chars": rows}
        for file, rows in all_chars
    ]}
    with open(os.path.join(OUT_ROOT, "assets", "mc520pro", "font", FONT_NAME + ".json"), "w",
              encoding="utf-8") as f:        json.dump(font_def, f, ensure_ascii=False, separators=(",", ":"))
    total_kb = sum(os.path.getsize(os.path.join(font_dir, fn)) for fn in os.listdir(font_dir)) / 1024
    print(f"done: {len(all_chars)} pages, {total_kb:.0f} KB total")


if __name__ == "__main__":
    main()
