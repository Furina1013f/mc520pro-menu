package top.mc520pro.menu.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * 纹理渲染 UI 工具库：用纹理图集画 UI，彻底脱离 MC 方块质感。
 */
public final class GuiKit {
	// ---- 配色（仅用于文字和强调色） ----
	public static final int C_TEXT = 0xFFFFFFFF;
	public static final int C_MUTED = 0xFF9CA3AF;
	public static final int C_DIM = 0xFF6B7280;
	public static final int C_ACCENT = 0xFF7C5FE8;
	public static final int C_ACCENT2 = 0xFF22D3EE;
	public static final int C_SUCCESS = 0xFF22C55E;
	public static final int C_WARNING = 0xFFF59E0B;
	public static final int C_DANGER = 0xFFEF4444;
	public static final int C_PURPLE = 0xFFA855F7;
	public static final int C_BLUE = 0xFF3B82F6;
	public static final int C_GOLD = 0xFFFBBF24;

	private GuiKit() {
	}

	// ---- 面板 ----

	/** 玻璃面板：纹理绘制。 */
	public static void glassPanel(DrawContext ctx, int x, int y, int w, int h, float alpha) {
		TextureKit.draw(ctx, TextureKit.PANEL_GLASS, x, y, w, h);
	}

	// ---- 卡片 ----

	/** 卡片：纹理绘制。 */
	public static void card(DrawContext ctx, int x, int y, int w, int h, int accent,
			float alpha, boolean hover, boolean selected, boolean locked) {
		String tex;
		if (locked) {
			tex = TextureKit.CARD_DISABLED;
		} else if (selected || hover) {
			tex = TextureKit.CARD_SELECTED;
		} else if (hover) {
			tex = TextureKit.CARD_HOVER;
		} else {
			tex = TextureKit.CARD_NORMAL;
		}
		TextureKit.draw(ctx, tex, x, y, w, h);
	}

	// ---- 按钮 ----

	/** 按钮：纹理绘制。 */
	public static void button(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha, boolean disabled) {
		String tex;
		if (disabled) {
			tex = TextureKit.BTN_DISABLED;
		} else if (color == C_SUCCESS) {
			tex = TextureKit.BTN_SUCCESS;
		} else if (color == C_DANGER) {
			tex = TextureKit.BTN_DANGER;
		} else if (color == C_BLUE) {
			tex = TextureKit.BTN_BLUE;
		} else {
			tex = TextureKit.BTN_ACCENT;
		}
		TextureKit.draw(ctx, tex, x, y, w, h);
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 6,
				withAlpha(disabled ? C_DIM : 0xFFFFFFFF, alpha));
	}

	/** 胶囊按钮（等同于 button）。 */
	public static void pill(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha, boolean disabled) {
		button(ctx, x, y, w, h, label, tr, hover, color, alpha, disabled);
	}

	// ---- 徽章 ----

	/** 徽章：纹理绘制。 */
	public static void badge(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, int color, float alpha) {
		String tex;
		if (color == C_SUCCESS) {
			tex = TextureKit.BADGE_GREEN;
		} else if (color == C_DANGER) {
			tex = TextureKit.BADGE_RED;
		} else if (color == C_WARNING) {
			tex = TextureKit.BADGE_ORANGE;
		} else if (color == C_BLUE) {
			tex = TextureKit.BADGE_BLUE;
		} else if (color == C_GOLD) {
			tex = TextureKit.BADGE_GOLD;
		} else {
			tex = TextureKit.BADGE_PURPLE;
		}
		TextureKit.draw(ctx, tex, x, y, w, h);
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 4,
				withAlpha(0xFFFFFFFF, alpha));
	}

	// ---- 图标徽章 ----

	/** 图标徽章：纹理绘制。 */
	public static void iconBadge(DrawContext ctx, int x, int y, int w, int h, float alpha) {
		TextureKit.draw(ctx, TextureKit.ICON_BADGE, x, y, w, h);
	}

	// ---- 文字 ----

	public static void text(DrawContext ctx, TextRenderer tr, Text text, int x, int y,
			int color, float alpha) {
		ctx.drawTextWithShadow(tr, text, x, y, withAlpha(color, alpha));
	}

	public static void boldText(DrawContext ctx, TextRenderer tr, Text text, int x, int y,
			int color, float alpha) {
		ctx.drawTextWithShadow(tr, Text.literal(text.getString()).styled(s -> s.withBold(true)),
				x, y, withAlpha(color, alpha));
	}

	// ---- 颜色 ----

	public static final int C_BORDER = 0x406C5CE7;
	public static final int C_BORDER_HI = 0xFF6C5CE7;
	public static final int C_BORDER_GLOW = 0xFF6C5CE7;
	public static final int C_CARD = 0xFF2A2A3E;
	public static final int C_CARD_HOVER = 0xFF3A3A52;

	public static int withAlpha(int color, float alpha) {
		int baseA = (color >>> 24) & 0xFF;
		if (baseA == 0) {
			baseA = 0xFF;
		}
		int a = Math.round(baseA * Math.min(1f, Math.max(0f, alpha)));
		return (Math.min(255, Math.max(0, a)) << 24) | (color & 0xFFFFFF);
	}

	public static int mix(int c1, int c2, float t) {
		float k = Math.min(1f, Math.max(0f, t));
		int r = Math.round(((c1 >> 16) & 0xFF) * (1 - k) + ((c2 >> 16) & 0xFF) * k);
		int g = Math.round(((c1 >> 8) & 0xFF) * (1 - k) + ((c2 >> 8) & 0xFF) * k);
		int b = Math.round((c1 & 0xFF) * (1 - k) + (c2 & 0xFF) * k);
		int a = Math.round(((c1 >>> 24) & 0xFF) * (1 - k) + ((c2 >>> 24) & 0xFF) * k);
		if (a == 0) {
			a = 0xFF;
		}
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ---- 圆角矩形（兼容旧方法，用纹理画） ----

	/** 圆角矩形：用纹理画。 */
	public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
		// 用纹理图集里的圆角面板格子
		TextureKit.draw(ctx, TextureKit.PANEL_GLASS, x, y, w, h);
	}

	public static void roundedRectA(DrawContext ctx, int x, int y, int w, int h, int r, int color, float alpha) {
		TextureKit.draw(ctx, TextureKit.PANEL_GLASS, x, y, w, h);
	}

	public static void roundedRectStroke(DrawContext ctx, int x, int y, int w, int h, int r,
			int color, float alpha) {
		// 用细线画边框
		int c = withAlpha(color, alpha);
		ctx.fill(x, y, x + w, y + 1, c);
		ctx.fill(x, y + h - 1, x + w, y + h, c);
		ctx.fill(x, y, x + 1, y + h, c);
		ctx.fill(x + w - 1, y, x + w, y + h, c);
	}

	/** 图标徽章（方形圆角，用纹理画）。 */
	public static void iconBadgeRect(DrawContext ctx, int x, int y, int w, int h, int color, float alpha) {
		TextureKit.draw(ctx, TextureKit.ICON_BADGE, x, y, w, h);
	}

	// ---- 滚动条 ----

	/** 滚动条轨道：纹理绘制。 */
	public static void scrollbarTrack(DrawContext ctx, int x, int y, int w, int h) {
		TextureKit.draw(ctx, TextureKit.SCROLLBAR_TRACK, x, y, w, h);
	}

	/** 滚动条滑块：纹理绘制。 */
	public static void scrollbarThumb(DrawContext ctx, int x, int y, int w, int h) {
		TextureKit.draw(ctx, TextureKit.SCROLLBAR_THUMB, x, y, w, h);
	}

	// ---- 预览/浮窗 ----

	/** 预览背景：纹理绘制。 */
	public static void previewBg(DrawContext ctx, int x, int y, int w, int h) {
		TextureKit.draw(ctx, TextureKit.PREVIEW_BG, x, y, w, h);
	}

	/** 浮窗背景：纹理绘制。 */
	public static void tooltipBg(DrawContext ctx, int x, int y, int w, int h) {
		TextureKit.draw(ctx, TextureKit.TOOLTIP_BG, x, y, w, h);
	}
}
