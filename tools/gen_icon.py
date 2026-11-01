# -*- coding: utf-8 -*-
"""生成 mod 图标（128x128 PNG）：渐变底 + 爱心 + 520PRO 文字。"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

SIZE = 128


def lerp(a, b, t):
    return int(a + (b - a) * t)


def main():
    out = sys.argv[1]
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    top, bot = (36, 42, 70), (74, 28, 68)
    for y in range(SIZE):
        t = y / (SIZE - 1)
        d.line([(0, y), (SIZE, y)],
               fill=(lerp(top[0], bot[0], t), lerp(top[1], bot[1], t),
                     lerp(top[2], bot[2], t), 255))

    mask = Image.new("L", (SIZE, SIZE), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([2, 2, SIZE - 3, SIZE - 3], radius=22, fill=255)
    img.putalpha(mask)

    # 爱心
    hx, hy, r = SIZE // 2, 42, 16
    d.ellipse([hx - r, hy - r, hx, hy], fill=(255, 82, 112, 255))
    d.ellipse([hx, hy - r, hx + r, hy], fill=(255, 82, 112, 255))
    d.polygon([(hx - r, hy - 4), (hx + r, hy - 4), (hx, hy + r + 10)],
              fill=(255, 82, 112, 255))

    font = None
    for fp in [r"C:\Windows\Fonts\msyhbd.ttc", r"C:\Windows\Fonts\msyh.ttc",
               r"C:\Windows\Fonts\arialbd.ttf"]:
        if os.path.exists(fp):
            try:
                font = ImageFont.truetype(fp, 24)
                break
            except Exception:
                pass
    if font is None:
        font = ImageFont.load_default()
    d.text((hx, 98), "520PRO", font=font, fill=(255, 255, 255, 255), anchor="mm")

    img.save(out, "PNG")
    print("icon saved:", out)


if __name__ == "__main__":
    main()
