# -*- coding: utf-8 -*-
"""生成独立 UI sprite（每个元素一个 PNG + nine_slice mcmeta）。

输出到 assets/mc520pro/textures/gui/sprites/mc520pro/
用法：python tools/gen_ui_sprites.py <输出目录>
"""
import math
import os
import sys
from PIL import Image, ImageDraw

S = 32  # 每个 sprite 32x32

PANEL = (30, 30, 50, 235)
CARD = (42, 42, 62, 210)
CARD_HOVER = (60, 60, 90, 230)
CARD_SEL = (80, 80, 120, 240)
CARD_DIS = (30, 30, 40, 150)
BORDER = (90, 70, 160, 220)
GLOW = (124, 95, 232, 255)
ACCENT = (124, 95, 232, 255)
ACCENT2 = (34, 211, 238, 255)
SUCCESS = (34, 197, 94, 255)
WARNING = (245, 158, 11, 255)
DANGER = (239, 68, 68, 255)
PURPLE = (168, 85, 247, 255)
BLUE = (59, 130, 246, 255)
GOLD = (251, 191, 36, 255)


def rr(d, x, y, w, h, r, fill=None, outline=None, ow=1):
	r = min(r, w / 2, h / 2)
	d.rounded_rectangle([x, y, x + w, y + h], radius=r, fill=fill, outline=outline, width=ow)


def sprite(name, draw_fn, border=None):
	img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
	draw_fn(ImageDraw.Draw(img), S)
	img.save(os.path.join(OUT, name + ".png"), "PNG")
	if border:
		mc = ('{"gui":{"scaling":{"type":"nine_slice","width":%d,"height":%d,'
			  '"border":{"left":%d,"top":%d,"right":%d,"bottom":%d}}}}'
			  % (S, S, border[0], border[1], border[2], border[3]))
	else:
		mc = '{"gui":{"scaling":{"type":"nine_slice","width":%d,"height":%d,"border":%d}}}' % (S, S, 0)
	with open(os.path.join(OUT, name + ".png.mcmeta"), "w", encoding="utf-8") as f:
		f.write(mc)


def main():
	global OUT
	OUT = sys.argv[1]
	os.makedirs(OUT, exist_ok=True)

	def panel(d, s):
		rr(d, 1, 1, s - 2, s - 2, 7, fill=PANEL, outline=BORDER, ow=1)
		d.rounded_rectangle([4, 3, s - 4, 5], radius=2, fill=(255, 255, 255, 28))
	sprite("panel_glass", panel, (8, 8, 8, 8))

	def panel_solid(d, s):
		rr(d, 1, 1, s - 2, s - 2, 7, fill=(30, 30, 50, 255), outline=BORDER, ow=1)
	sprite("panel_solid", panel_solid, (8, 8, 8, 8))

	def card(d, s, fill, outline, ow=1):
		def fn(dd, ss):
			rr(dd, 1, 1, ss - 2, ss - 2, 6, fill=fill, outline=outline, ow=ow)
			dd.rounded_rectangle([2, 4, 5, ss - 4], radius=1, fill=ACCENT)
		return fn
	sprite("card_normal", card(None, None, CARD, (70, 70, 100, 120)), (7, 7, 7, 7))
	sprite("card_hover", card(None, None, CARD_HOVER, GLOW), (7, 7, 7, 7))
	sprite("card_selected", card(None, None, CARD_SEL, GLOW, 2), (7, 7, 7, 7))

	def card_dis(d, s):
		rr(d, 1, 1, s - 2, s - 2, 6, fill=CARD_DIS, outline=(80, 80, 90, 90), ow=1)
	sprite("card_disabled", card_dis, (7, 7, 7, 7))

	def btn(d, s, color):
		rr(d, 1, 1, s - 2, s - 2, 6, fill=color)
		d.rounded_rectangle([4, 3, s - 4, 5], radius=2, fill=(255, 255, 255, 45))
	sprite("btn_accent", lambda d, s: btn(d, s, (100, 80, 200, 235)), (7, 7, 7, 7))
	sprite("btn_success", lambda d, s: btn(d, s, (30, 150, 80, 235)), (7, 7, 7, 7))
	sprite("btn_danger", lambda d, s: btn(d, s, (190, 60, 55, 235)), (7, 7, 7, 7))
	sprite("btn_blue", lambda d, s: btn(d, s, (50, 110, 220, 235)), (7, 7, 7, 7))
	sprite("btn_disabled", lambda d, s: btn(d, s, (55, 55, 70, 180)), (7, 7, 7, 7))

	def badge(d, s, color):
		rr(d, 1, 1, s - 2, s - 2, s / 2 - 1, fill=color)
		d.rounded_rectangle([s / 2 - 6, 3, s / 2 + 6, 5], radius=2, fill=(255, 255, 255, 60))
	sprite("badge_green", lambda d, s: badge(d, s, SUCCESS))
	sprite("badge_red", lambda d, s: badge(d, s, DANGER))
	sprite("badge_orange", lambda d, s: badge(d, s, WARNING))
	sprite("badge_blue", lambda d, s: badge(d, s, BLUE))
	sprite("badge_gold", lambda d, s: badge(d, s, GOLD))
	sprite("badge_purple", lambda d, s: badge(d, s, PURPLE))

	def icon_badge(d, s):
		rr(d, 1, 1, s - 2, s - 2, 5, fill=(55, 55, 85, 210), outline=ACCENT, ow=1)
		d.rounded_rectangle([4, 3, s - 4, 5], radius=2, fill=(255, 255, 255, 35))
	sprite("icon_badge", icon_badge, (7, 7, 7, 7))

	def sb_track(d, s):
		rr(d, 1, 1, s - 2, s - 2, 2, fill=(18, 18, 28, 200))
	sprite("scrollbar_track", sb_track, (3, 3, 3, 3))

	def sb_thumb(d, s):
		rr(d, 1, 1, s - 2, s - 2, 2, fill=ACCENT)
		d.rounded_rectangle([4, 3, s - 4, 5], radius=2, fill=(255, 255, 255, 50))
	sprite("scrollbar_thumb", sb_thumb, (3, 3, 3, 3))

	def tooltip(d, s):
		rr(d, 1, 1, s - 2, s - 2, 5, fill=(18, 18, 32, 235), outline=BORDER, ow=1)
	sprite("tooltip_bg", tooltip, (7, 7, 7, 7))

	def preview(d, s):
		rr(d, 1, 1, s - 2, s - 2, 5, fill=(20, 20, 32, 210), outline=(80, 80, 110, 130), ow=1)
	sprite("preview_bg", preview, (7, 7, 7, 7))

	def progress(d, s):
		rr(d, 1, s / 2 - 3, s - 2, 6, 3, fill=(18, 18, 28, 220))
		rr(d, 1, s / 2 - 3, s / 2, 6, 3, fill=ACCENT)
	sprite("progress_bar", progress, (0, 4, 0, 4))

	def avatar(d, s, bg, ring, skin):
		cx = s / 2
		r = s / 2 - 2
		d.ellipse([cx - r, 2, cx + r, 2 + 2 * r], fill=bg, outline=ring, width=1)
		d.ellipse([cx - 4, 9, cx + 4, 17], fill=skin)
		rr(d, cx - 6, 18, 12, 9, 3, fill=skin)
	sprite("avatar_default", lambda d, s: avatar(d, s, (60, 60, 90, 220), BORDER, (150, 150, 170, 255)))
	sprite("avatar_player", lambda d, s: avatar(d, s, (40, 60, 85, 230), ACCENT2, (205, 225, 245, 255)))

	def search(d, s):
		rr(d, 1, 1, s - 2, s - 2, 5, fill=(18, 18, 30, 210), outline=BORDER, ow=1)
	sprite("search_box", search, (7, 7, 7, 7))

	def star(d, s, fill, outline):
		cx, cy = s / 2, s / 2
		pts = []
		for i in range(5):
			a = -math.pi / 2 + i * 2 * math.pi / 5
			pts.append((cx + math.cos(a) * 12, cy + math.sin(a) * 12))
			a2 = a + math.pi / 5
			pts.append((cx + math.cos(a2) * 5, cy + math.sin(a2) * 5))
		d.polygon(pts, fill=fill, outline=outline)
	sprite("star_full", lambda d, s: star(d, s, GOLD, (120, 90, 10, 255)))
	sprite("star_empty", lambda d, s: star(d, s, None, (110, 110, 125, 180)))

	def spinner(d, s):
		cx = s / 2
		d.arc([cx - 11, 5, cx + 11, 27], 0, 280, fill=ACCENT, width=3)
	sprite("loading_spinner", spinner)

	def dot(d, s):
		d.ellipse([7, 7, s - 7, s - 7], fill=DANGER)
	sprite("badge_dot", dot)

	def divider(d, s):
		d.rounded_rectangle([3, s / 2 - 1, s - 3, s / 2], radius=1, fill=(80, 80, 110, 170))
	sprite("divider", divider, (0, 16, 0, 16))

	print("sprites written to", OUT)


if __name__ == "__main__":
	main()
