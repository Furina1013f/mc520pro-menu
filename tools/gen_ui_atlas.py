# -*- coding: utf-8 -*-
"""生成 UI 纹理图集（512x512，16x16 格子，每格 32x32）。

用法：python tools/gen_ui_atlas.py <输出路径>
"""
import math
import os
import sys
from PIL import Image, ImageDraw

SIZE = 512
CELL = 32
COLS = 16
ROWS = 16

# 现代配色
BG_DEEP = (15, 15, 26, 255)
PANEL_GLASS = (30, 30, 50, 180)
PANEL_GLASS_BR = (50, 50, 80, 200)
BORDER = (90, 70, 160, 200)
BORDER_GLOW = (124, 95, 232, 255)
CARD = (42, 42, 62, 200)
CARD_HOVER = (60, 60, 90, 220)
CARD_SELECTED = (80, 80, 120, 230)
CARD_DISABLED = (30, 30, 40, 150)
ACCENT = (124, 95, 232, 255)
ACCENT2 = (34, 211, 238, 255)
SUCCESS = (34, 197, 94, 255)
WARNING = (245, 158, 11, 255)
DANGER = (239, 68, 68, 255)
PURPLE = (168, 85, 247, 255)
BLUE = (59, 130, 246, 255)
GOLD = (251, 191, 36, 255)
TEXT = (255, 255, 255, 255)
MUTED = (156, 163, 175, 255)


def rrect(draw, x, y, w, h, r, fill=None, outline=None, ow=1):
	if r > w / 2:
		r = w / 2
	if r > h / 2:
		r = h / 2
	draw.rounded_rectangle([x, y, x + w, y + h], radius=r, fill=fill, outline=outline, width=ow)


def draw_cell(img, row, col, draw_fn):
	"""在指定格子里画纹理。"""
	x0 = col * CELL
	y0 = row * CELL
	d = ImageDraw.Draw(img)
	draw_fn(d, x0, y0, CELL)


def main():
	out = sys.argv[1] if len(sys.argv) > 1 else "atlas.png"
	img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

	# Row 0: 面板与卡片
	def panel_glass(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 6, fill=PANEL_GLASS)
		rrect(d, x + 2, y + 2, s - 4, s - 4, 6, outline=BORDER, ow=1)
		# 顶部高光
		d.rounded_rectangle([x + 4, y + 4, x + s - 4, y + 6], radius=2, fill=(255, 255, 255, 30))
	draw_cell(img, 0, 0, panel_glass)

	def panel_solid(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 6, fill=(30, 30, 50, 255))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 6, outline=BORDER, ow=1)
	draw_cell(img, 0, 1, panel_solid)

	def card_normal(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, fill=CARD)
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, outline=(70, 70, 100, 100), ow=1)
		# 左侧强调条
		d.rounded_rectangle([x + 3, y + 4, x + 5, y + s - 4], radius=1, fill=ACCENT)
	draw_cell(img, 0, 2, card_normal)

	def card_hover(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, fill=CARD_HOVER)
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, outline=BORDER_GLOW, ow=1)
		d.rounded_rectangle([x + 3, y + 4, x + 5, y + s - 4], radius=1, fill=ACCENT)
	draw_cell(img, 0, 3, card_hover)

	def card_selected(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, fill=CARD_SELECTED)
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, outline=BORDER_GLOW, ow=2)
		d.rounded_rectangle([x + 3, y + 4, x + 5, y + s - 4], radius=1, fill=ACCENT)
	draw_cell(img, 0, 4, card_selected)

	def card_disabled(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, fill=CARD_DISABLED)
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, outline=(80, 80, 90, 100), ow=1)
	draw_cell(img, 0, 5, card_disabled)

	# Row 1: 按钮
	def btn(d, x, y, s, color):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, fill=color)
		# 顶部高光
		d.rounded_rectangle([x + 4, y + 4, x + s - 4, y + 6], radius=2, fill=(255, 255, 255, 40))
	draw_cell(img, 1, 0, lambda d, x, y, s: btn(d, x, y, s, (90, 70, 160, 220)))
	draw_cell(img, 1, 1, lambda d, x, y, s: btn(d, x, y, s, ACCENT))
	draw_cell(img, 1, 2, lambda d, x, y, s: btn(d, x, y, s, (60, 60, 80, 180)))
	draw_cell(img, 1, 3, lambda d, x, y, s: btn(d, x, y, s, SUCCESS))
	draw_cell(img, 1, 4, lambda d, x, y, s: btn(d, x, y, s, DANGER))
	draw_cell(img, 1, 5, lambda d, x, y, s: btn(d, x, y, s, BLUE))

	# Row 2: 徽章
	def badge(d, x, y, s, color):
		rrect(d, x + 2, y + 2, s - 4, s - 4, s / 2 - 2, fill=color)
	draw_cell(img, 2, 0, lambda d, x, y, s: badge(d, x, y, s, SUCCESS))
	draw_cell(img, 2, 1, lambda d, x, y, s: badge(d, x, y, s, DANGER))
	draw_cell(img, 2, 2, lambda d, x, y, s: badge(d, x, y, s, WARNING))
	draw_cell(img, 2, 3, lambda d, x, y, s: badge(d, x, y, s, BLUE))
	draw_cell(img, 2, 4, lambda d, x, y, s: badge(d, x, y, s, GOLD))
	draw_cell(img, 2, 5, lambda d, x, y, s: badge(d, x, y, s, PURPLE))

	# Row 3: 图标徽章、滚动条、浮窗、预览
	def icon_badge(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, fill=(60, 60, 90, 200))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, outline=ACCENT, ow=1)
		d.rounded_rectangle([x + 4, y + 4, x + s - 4, y + 6], radius=2, fill=(255, 255, 255, 30))
	draw_cell(img, 3, 0, icon_badge)

	def scrollbar_track(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 2, fill=(20, 20, 30, 200))
	draw_cell(img, 3, 1, scrollbar_track)

	def scrollbar_thumb(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 2, fill=ACCENT)
		d.rounded_rectangle([x + 4, y + 4, x + s - 4, y + 6], radius=2, fill=(255, 255, 255, 40))
	draw_cell(img, 3, 2, scrollbar_thumb)

	def tooltip_bg(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, fill=(20, 20, 35, 230))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, outline=BORDER, ow=1)
	draw_cell(img, 3, 3, tooltip_bg)

	def preview_bg(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, fill=(20, 20, 30, 200))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, outline=(70, 70, 100, 100), ow=1)
	draw_cell(img, 3, 4, preview_bg)

	def divider(d, x, y, s):
		d.rounded_rectangle([x + 4, y + s / 2 - 1, x + s - 4, y + s / 2], radius=1, fill=(70, 70, 100, 150))
	draw_cell(img, 3, 5, divider)

	# 保存
	img.save(out, "PNG")
	print(f"saved: {out}  {img.size}")


if __name__ == "__main__":
	main()
