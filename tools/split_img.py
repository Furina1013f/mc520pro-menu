# -*- coding: utf-8 -*-
"""把超长截图按原始分辨率切成易读条目（不做整体缩放）。"""
import sys

from PIL import Image

src = sys.argv[1]
outdir = sys.argv[2]
slice_h = int(sys.argv[3]) if len(sys.argv) > 3 else 1200
overlap = 80

img = Image.open(src)
w, h = img.size
print("size:", img.size)
y = 0
idx = 0
while y < h:
    y2 = min(h, y + slice_h)
    crop = img.crop((0, y, w, y2))
    path = f"{outdir}\\part{idx:02d}.png"
    crop.save(path)
    print("saved:", path, crop.size)
    idx += 1
    if y2 >= h:
        break
    y += slice_h - overlap
