# -*- coding: utf-8 -*-
"""生成 UI 纹理图集 v2（512x512，16x16 格子，每格 32x32）。

比 v1 增加了：进度条、头像、开关、搜索框、标签、星星、加载动画等。
"""
import math
import os
import sys
from PIL import Image, ImageDraw

SIZE = 512
CELL = 32
COLS = 16

# 现代配色
PANEL_GLASS = (30, 30, 50, 180)
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
BG_DEEP = (15, 15, 26, 255)


def rrect(d, x, y, w, h, r, fill=None, outline=None, ow=1):
	if r > w / 2:
		r = w / 2
	if r > h / 2:
		r = h / 2
	d.rounded_rectangle([x, y, x + w, y + h], radius=r, fill=fill, outline=outline, width=ow)


def draw_cell(img, row, col, draw_fn):
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
		d.rounded_rectangle([x + 4, y + 4, x + s - 4, y + 6], radius=2, fill=(255, 255, 255, 30))
	draw_cell(img, 0, 0, panel_glass)

	def panel_solid(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 6, fill=(30, 30, 50, 255))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 6, outline=BORDER, ow=1)
	draw_cell(img, 0, 1, panel_solid)

	def card_normal(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, fill=CARD)
		rrect(d, x + 2, y + 2, s - 4, s - 4, 5, outline=(70, 70, 100, 100), ow=1)
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

	# Row 3: 图标徽章、滚动条、浮窗
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

	# Row 4: 进度条、头像、开关、搜索框
	def progress_bar(d, x, y, s):
		# 轨道
		rrect(d, x + 2, y + s / 2 - 3, s - 4, 6, 3, fill=(20, 20, 30, 200))
		# 进度（半填充）
		rrect(d, x + 2, y + s / 2 - 3, s / 2, 6, 3, fill=ACCENT)
	draw_cell(img, 4, 0, progress_bar)

	def avatar_default(d, x, y, s):
		# 圆形头像
		cx, cy = x + s / 2, y + s / 2
		r = s / 2 - 2
		d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(60, 60, 90, 200))
		d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=BORDER, width=1)
		# 简单人像
		d.ellipse([cx - 4, cy - 6, cx + 4, cy + 2], fill=(150, 150, 170, 255))
		d.rounded_rectangle([cx - 6, cy + 2, cx + 6, cy + r], radius=2, fill=(150, 150, 170, 255))
	draw_cell(img, 4, 1, avatar_default)

	def avatar_player(d, x, y, s):
		cx, cy = x + s / 2, y + s / 2
		r = s / 2 - 2
		d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(40, 60, 80, 220))
		d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=ACCENT2, width=1)
		d.ellipse([cx - 4, cy - 6, cx + 4, cy + 2], fill=(200, 220, 240, 255))
		d.rounded_rectangle([cx - 6, cy + 2, cx + 6, cy + r], radius=2, fill=(200, 220, 240, 255))
	draw_cell(img, 4, 2, avatar_player)

	def toggle_on(d, x, y, s):
		rrect(d, x + 2, y + s / 2 - 4, s - 4, 8, 4, fill=SUCCESS)
		d.ellipse([x + s - 10, y + s / 2 - 6, x + s - 4, y + s / 2 + 2], fill=(255, 255, 255, 255))
	draw_cell(img, 4, 3, toggle_on)

	def toggle_off(d, x, y, s):
		rrect(d, x + 2, y + s / 2 - 4, s - 4, 8, 4, fill=(60, 60, 80, 180))
		d.ellipse([x + 4, y + s / 2 - 6, x + 10, y + s / 2 + 2], fill=(150, 150, 170, 255))
	draw_cell(img, 4, 4, toggle_off)

	def search_box(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, fill=(20, 20, 30, 200))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, outline=BORDER, ow=1)
		# 搜索图标（放大镜）
		cx, cy = x + 8, y + s / 2
		d.ellipse([cx - 2, cy - 2, cx + 4, cy + 4], outline=(200, 200, 210, 255), width=1)
		d.line([cx + 3, cy + 3, cx + 6, cy + 6], fill=(200, 200, 210, 255), width=1)
	draw_cell(img, 4, 5, search_box)

	# Row 5: 标签、星星、加载、圆点
	def tab_normal(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, fill=(40, 40, 60, 180))
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, outline=(70, 70, 100, 100), ow=1)
	draw_cell(img, 5, 0, tab_normal)

	def tab_active(d, x, y, s):
		rrect(d, x + 2, y + 2, s - 4, s - 4, 4, fill=ACCENT)
		d.rounded_rectangle([x + 4, y + 4, x + s - 4, y + 6], radius=2, fill=(255, 255, 255, 40))
	draw_cell(img, 5, 1, tab_active)

	def star_full(d, x, y, s):
		cx, cy = x + s / 2, y + s / 2
		# 五角星
		points = []
		for i in range(5):
			ang = -math.pi / 2 + i * 2 * math.pi / 5
			points.append((cx + math.cos(ang) * 10, cy + math.sin(ang) * 10))
			ang2 = ang + math.pi / 5
			points.append((cx + math.cos(ang2) * 4, cy + math.sin(ang2) * 4))
		d.polygon(points, fill=GOLD)
	draw_cell(img, 5, 2, star_full)

	def star_empty(d, x, y, s):
		cx, cy = x + s / 2, y + s / 2
		points = []
		for i in range(5):
			ang = -math.pi / 2 + i * 2 * math.pi / 5
			points.append((cx + math.cos(ang) * 10, cy + math.sin(ang) * 10))
			ang2 = ang + math.pi / 5
			points.append((cx + math.cos(ang2) * 4, cy + math.sin(ang2) * 4))
		d.polygon(points, outline=(100, 100, 110, 150), width=1)
	draw_cell(img, 5, 3, star_empty)

	def loading_spinner(d, x, y, s):
		cx, cy = x + s / 2, y + s / 2
		# 弧线
		d.arc([cx - 10, cy - 10, cx + 10, cy + 10], 0, 270, fill=ACCENT, width=3)
	draw_cell(img, 5, 4, loading_spinner)

	def badge_dot(d, x, y, s):
		cx, cy = x + s / 2, y + s / 2
		r = s / 2 - 4
		d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=DANGER)
	draw_cell(img, 5, 5, badge_dot)

	# 保存
	img.save(out, "PNG")
	print(f"saved: {out}  {img.size}")


if __name__ == "__main__":
	main()
