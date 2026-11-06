package top.mc520pro.menu.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * UI 绘制工具库：阴影 + 渐变 + 图标徽章 + 质感边框。
 */
public final class GuiKit {
	// ---- 配色 ----
	public static final int C_BG_DEEP = 0xFF14141E;
	public static final int C_PANEL = 0xFF2D2D44;
	public static final int C_PANEL_DIM = 0xFF1E1E36;
	public static final int C_PANEL_HI = 0xFF3D3D5C;
	public static final int C_SHADOW = 0xFF0D0D15;
	public static final int C_CARD = 0xFF2A2A3E;
	public static final int C_CARD_DIM = 0xFF1E1E2E;
	public static final int C_CARD_HOVER = 0xFF3A3A52;
	public static final int C_CARD_LOCK = 0xFF1A1A28;
	public static final int C_BORDER = 0xFF4A4A6A;
	public static final int C_BORDER_HI = 0xFF5A5A7A;
	public static final int C_ACCENT = 0xFF6C5CE7;
	public static final int C_ACCENT_DIM = 0xFF5A4AD7;
	public static final int C_ACCENT2 = 0xFF00CEC9;
	public static final int C_SUCCESS = 0xFF00B894;
	public static final int C_WARNING = 0xFFFDCB6E;
	public static final int C_DANGER = 0xFFE17055;
	public static final int C_PURPLE = 0xFFA55EEA;
	public static final int C_BLUE = 0xFF5B8DEF;
	public static final int C_GOLD = 0xFFFFD700;
	public static final int C_TEXT = 0xFFFFFFFF;
	public static final int C_MUTED = 0xFFB2BEC3;
	public static final int C_DIM = 0xFF6E6E8E;

	private GuiKit() {
	}

	/** 矩形阴影。 */
	public static void shadowRect(DrawContext ctx, int x, int y, int w, int h, float alpha) {
		ctx.fill(x + 3, y + 3, x + w + 3, y + h + 3, withAlpha(C_SHADOW, alpha));
		ctx.fill(x + 4, y + 4, x + w + 4, y + h + 4, withAlpha(C_SHADOW, alpha * 0.7f));
	}

	/** 质感面板。 */
	public static void panel(DrawContext ctx, int x, int y, int w, int h, float alpha) {
		shadowRect(ctx, x, y, w, h, alpha);
		int midY = y + (int) (h * 0.5f);
		ctx.fillGradient(x, y, x + w, midY, withAlpha(C_PANEL, alpha), withAlpha(C_PANEL_DIM, alpha));
		ctx.fillGradient(x, midY, x + w, y + h, withAlpha(C_PANEL_DIM, alpha), withAlpha(0xFF181828, alpha));
		ctx.fill(x, y, x + w, y + 1, withAlpha(C_PANEL_HI, alpha));
		ctx.fill(x, y + h - 1, x + w, y + h, withAlpha(0xFF2A2A40, alpha));
		ctx.fill(x, y, x + 1, y + h, withAlpha(C_PANEL_HI, alpha));
		ctx.fill(x + w - 1, y, x + w, y + h, withAlpha(0xFF2A2A40, alpha));
		ctx.fillGradient(x + 2, y + 2, x + w - 2, y + 4, withAlpha(0xFF4A4A6A, alpha), withAlpha(0xFF2D2D44, alpha));
	}

	/** 质感卡片。 */
	public static void card(DrawContext ctx, int x, int y, int w, int h, int accent,
			float alpha, boolean hover, boolean selected, boolean locked) {
		shadowRect(ctx, x, y, w, h, alpha * (hover ? 0.8f : 0.5f));
		int midY = y + (int) (h * 0.5f);
		if (locked) {
			ctx.fillGradient(x, y, x + w, midY, withAlpha(C_CARD_LOCK, alpha), withAlpha(0xFF14141E, alpha));
		} else if (hover) {
			ctx.fillGradient(x, y, x + w, midY, withAlpha(C_CARD_HOVER, alpha), withAlpha(C_CARD, alpha));
		} else if (selected) {
			ctx.fillGradient(x, y, x + w, midY, withAlpha(0xFF4A4A6A, alpha), withAlpha(C_CARD, alpha));
		} else {
			ctx.fillGradient(x, y, x + w, midY, withAlpha(C_CARD, alpha), withAlpha(C_CARD_DIM, alpha));
		}
		ctx.fillGradient(x, midY, x + w, y + h, withAlpha(C_CARD_DIM, alpha), withAlpha(0xFF181828, alpha));
		if (!locked && accent != 0) {
			ctx.fill(x, y + 3, x + 3, y + h - 3, withAlpha(accent, alpha));
		}
		int borderColor = locked ? 0xFF33334A : (hover ? C_BORDER_HI : C_BORDER);
		if (selected) {
			borderColor = C_ACCENT;
		}
		ctx.fill(x, y, x + w, y + 1, withAlpha(borderColor, alpha));
		ctx.fill(x, y + h - 1, x + w, y + h, withAlpha(borderColor, alpha));
		ctx.fill(x, y, x + 1, y + h, withAlpha(borderColor, alpha));
		ctx.fill(x + w - 1, y, x + w, y + h, withAlpha(borderColor, alpha));
	}

	/** 图标徽章（圆形）。 */
	public static void iconBadge(DrawContext ctx, int cx, int cy, int r, int color, float alpha) {
		int dim = mix(color, 0xFF14141E, 0.4f);
		int bright = mix(color, 0xFFFFFFFF, 0.3f);
		for (int dy = -r; dy <= r; dy++) {
			float rowAlpha = alpha * (1f - Math.abs(dy) * 0.15f);
			for (int dx = -r; dx <= r; dx++) {
				if (dx * dx + dy * dy > r * r) {
					continue;
				}
				int c = dx > r * 0.3f ? bright : (dx < -r * 0.3f ? dim : color);
				ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, withAlpha(c, rowAlpha));
			}
		}
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist > r - 1 && dist <= r) {
					ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, withAlpha(bright, alpha));
				}
			}
		}
	}

	/** 图标徽章（方形圆角）。 */
	public static void iconBadgeRect(DrawContext ctx, int x, int y, int w, int h, int color, float alpha) {
		int dim = mix(color, 0xFF14141E, 0.4f);
		int midY = y + (int) (h * 0.5f);
		ctx.fillGradient(x, y, x + w, midY, withAlpha(dim, alpha), withAlpha(color, alpha));
		ctx.fillGradient(x, midY, x + w, y + h, withAlpha(color, alpha), withAlpha(dim, alpha));
		int br = Math.min(Math.min(4, w / 4), h / 4);
		ctx.fill(x, y, x + br, y + 1, withAlpha(0xFFFFFFFF, alpha * 0.3f));
		ctx.fill(x, y, x + 1, y + br, withAlpha(0xFFFFFFFF, alpha * 0.3f));
	}

	/** 质感按钮。 */
	public static void button(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha) {
		shadowRect(ctx, x, y, w, h, alpha * 0.6f);
		int dim = mix(color, 0xFF14141E, 0.3f);
		int bright = mix(color, 0xFFFFFFFF, hover ? 0.35f : 0.15f);
		int midY = y + (int) (h * 0.4f);
		ctx.fillGradient(x, y, x + w, midY, withAlpha(dim, alpha), withAlpha(bright, alpha));
		ctx.fillGradient(x, midY, x + w, y + h, withAlpha(bright, alpha), withAlpha(dim, alpha));
		ctx.fill(x + 2, y + 1, x + w - 2, y + 2, withAlpha(mix(color, 0xFFFFFFFF, 0.5f), alpha * (hover ? 0.8f : 0.4f)));
		ctx.fill(x, y, x + w, y + 1, withAlpha(mix(color, 0xFFFFFFFF, 0.4f), alpha));
		ctx.fill(x, y + h - 1, x + w, y + h, withAlpha(dim, alpha));
		ctx.fill(x, y, x + 1, y + h, withAlpha(bright, alpha));
		ctx.fill(x + w - 1, y, x + w, y + h, withAlpha(dim, alpha));
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 6, withAlpha(0xFFFFFFFF, alpha));
	}

	/** 胶囊按钮。 */
	public static void pill(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha, boolean disabled) {
		shadowRect(ctx, x, y, w, h, alpha * 0.4f);
		int dim = mix(color, 0xFF14141E, disabled ? 0.6f : 0.3f);
		int bright = mix(color, 0xFFFFFFFF, hover && !disabled ? 0.3f : 0.1f);
		int midY = y + (int) (h * 0.4f);
		ctx.fillGradient(x, y, x + w, midY, withAlpha(dim, alpha), withAlpha(bright, alpha));
		ctx.fillGradient(x, midY, x + w, y + h, withAlpha(bright, alpha), withAlpha(dim, alpha));
		ctx.fill(x + 2, y + 1, x + w - 2, y + 2, withAlpha(mix(color, 0xFFFFFFFF, 0.5f), alpha * (hover && !disabled ? 0.6f : 0.3f)));
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 6,
				withAlpha(disabled ? 0xFF6E6E8E : 0xFFFFFFFF, alpha));
	}

	/** 状态徽章。 */
	public static void badge(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, int color, float alpha) {
		shadowRect(ctx, x, y, w, h, alpha * 0.3f);
		int dim = mix(color, 0xFF14141E, 0.3f);
		int midY = y + (int) (h * 0.5f);
		ctx.fillGradient(x, y, x + w, midY, withAlpha(dim, alpha), withAlpha(color, alpha));
		ctx.fillGradient(x, midY, x + w, y + h, withAlpha(color, alpha), withAlpha(dim, alpha));
		ctx.fill(x, y, x + w, y + 1, withAlpha(mix(color, 0xFFFFFFFF, 0.4f), alpha));
		ctx.fill(x, y, x + 1, y + h, withAlpha(mix(color, 0xFFFFFFFF, 0.3f), alpha));
		ctx.fill(x + w - 1, y, x + w, y + h, withAlpha(dim, alpha));
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 4, withAlpha(0xFFFFFFFF, alpha));
	}

	/** 带阴影文字。 */
	public static void text(DrawContext ctx, TextRenderer tr, Text text, int x, int y,
			int color, float alpha) {
		ctx.drawTextWithShadow(tr, text, x, y, withAlpha(color, alpha));
	}

	/** 粗体文字。 */
	public static void boldText(DrawContext ctx, TextRenderer tr, Text text, int x, int y,
			int color, float alpha) {
		ctx.drawTextWithShadow(tr, Text.literal(text.getString()).styled(s -> s.withBold(true)),
				x, y, withAlpha(color, alpha));
	}

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
}