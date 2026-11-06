package top.mc520pro.menu.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * 现代扁平化 UI 工具库：玻璃面板、圆角卡片、渐变、霓虹描边。
 * 不使用 MC 像素方块质感，追求现代 App 风格。
 */
public final class GuiKit {
	// ---- 现代配色 ----
	public static final int C_BG_DEEP = 0xFF0F0F1A;
	public static final int C_BG_PANEL = 0x661A1A2E;
	public static final int C_BG_PANEL_OPAQUE = 0xFF1A1A2E;
	public static final int C_CARD = 0x802A2A44;
	public static final int C_CARD_HOVER = 0xB03A3A5A;
	public static final int C_BORDER = 0x406C5CE7;
	public static final int C_BORDER_HI = 0xFF6C5CE7;
	public static final int C_BORDER_GLOW = 0xFF6C5CE7;
	public static final int C_ACCENT = 0xFF7C5FE8;
	public static final int C_ACCENT_DIM = 0xFF5A4AD7;
	public static final int C_ACCENT2 = 0xFF22D3EE;
	public static final int C_SUCCESS = 0xFF22C55E;
	public static final int C_WARNING = 0xFFF59E0B;
	public static final int C_DANGER = 0xFFEF4444;
	public static final int C_PURPLE = 0xFFA855F7;
	public static final int C_BLUE = 0xFF3B82F6;
	public static final int C_GOLD = 0xFFFBBF24;
	public static final int C_TEXT = 0xFFFFFFFF;
	public static final int C_MUTED = 0xFF9CA3AF;
	public static final int C_DIM = 0xFF6B7280;

	private GuiKit() {
	}

	// ---- 圆角矩形 ----

	/** 画圆角矩形（用像素近似）。 */
	public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
		if (r > w / 2) r = w / 2;
		if (r > h / 2) r = h / 2;
		ctx.fill(x + r, y, x + w - r, y + h, color);
		ctx.fill(x, y + r, x + w, y + h - r, color);
		// 四个角（2 像素逼近）
		for (int i = 0; i < r; i++) {
			ctx.fill(x + i, y + i, x + i + 1, y + i + 1, color);
			ctx.fill(x + w - i - 1, y + i, x + w - i, y + i + 1, color);
			ctx.fill(x + i, y + h - i - 1, x + i + 1, y + h - i, color);
			ctx.fill(x + w - i - 1, y + h - i - 1, x + w - i, y + h - i, color);
		}
	}

	/** 圆角矩形带 alpha。 */
	public static void roundedRectA(DrawContext ctx, int x, int y, int w, int h, int r, int color, float alpha) {
		roundedRect(ctx, x, y, w, h, r, withAlpha(color, alpha));
	}

	/** 圆角矩形渐变填充。 */
	public static void roundedRectGradient(DrawContext ctx, int x, int y, int w, int h, int r,
			int c1, int c2, float alpha) {
		int midY = y + (int) (h * 0.5f);
		ctx.fillGradient(x + r, y, x + w - r, midY, withAlpha(c1, alpha), withAlpha(c2, alpha));
		ctx.fillGradient(x + r, midY, x + w - r, y + h, withAlpha(c2, alpha), withAlpha(c1, alpha));
		ctx.fill(x, y + r, x + w, y + h - r, withAlpha(c1, alpha));
		// 角
		for (int i = 0; i < r; i++) {
			ctx.fill(x + i, y + i, x + i + 1, y + i + 1, withAlpha(c1, alpha));
			ctx.fill(x + w - i - 1, y + i, x + w - i, y + i + 1, withAlpha(c1, alpha));
			ctx.fill(x + i, y + h - i - 1, x + i + 1, y + h - i, withAlpha(c1, alpha));
			ctx.fill(x + w - i - 1, y + h - i - 1, x + w - i, y + h - i, withAlpha(c1, alpha));
		}
	}

	// ---- 玻璃面板 ----

	/** 玻璃拟态面板：半透明 + 圆角 + 细描边 + 顶部高光。 */
	public static void glassPanel(DrawContext ctx, int x, int y, int w, int h, float alpha) {
		roundedRectA(ctx, x, y, w, h, 10, 0xCC14141A, alpha);
		roundedRectA(ctx, x, y, w, h, 10, 0x80FFFFFF, alpha * 0.04f); // 内层高光
		// 顶部高光条
		roundedRectA(ctx, x + 2, y + 2, w - 4, 2, 1, 0x60FFFFFF, alpha * 0.15f);
		// 细描边
		roundedRectStroke(ctx, x, y, w, h, 10, C_BORDER, alpha);
	}

	/** 圆角描边。 */
	public static void roundedRectStroke(DrawContext ctx, int x, int y, int w, int h, int r,
			int color, float alpha) {
		if (r > w / 2) r = w / 2;
		if (r > h / 2) r = h / 2;
		int c = withAlpha(color, alpha);
		ctx.fill(x + r, y, x + w - r, y + 1, c);
		ctx.fill(x + r, y + h - 1, x + w - r, y + h, c);
		ctx.fill(x, y + r, x + 1, y + h - r, c);
		ctx.fill(x + w - 1, y + r, x + w, y + h - r, c);
		for (int i = 0; i < r; i++) {
			ctx.fill(x + i, y + i, x + i + 1, y + i + 1, c);
			ctx.fill(x + w - i - 1, y + i, x + w - i, y + i + 1, c);
			ctx.fill(x + i, y + h - i - 1, x + i + 1, y + h - i, c);
			ctx.fill(x + w - i - 1, y + h - i - 1, x + w - i, y + h - i, c);
		}
	}

	// ---- 卡片 ----

	/** 现代卡片：圆角 + 玻璃 + 分类色左侧竖条。 */
	public static void card(DrawContext ctx, int x, int y, int w, int h, int accent,
			float alpha, boolean hover, boolean selected, boolean locked) {
		int bg = locked ? 0x401A1A28 : (hover ? C_CARD_HOVER : C_CARD);
		roundedRectA(ctx, x, y, w, h, 8, bg, alpha);
		// 左侧强调条（竖向渐变）
		if (!locked && accent != 0) {
			ctx.fillGradient(x, y + 4, x + 3, y + h - 4,
					withAlpha(accent, alpha), withAlpha(accent, alpha * 0.5f));
		}
		// 描边
		int borderColor = locked ? 0x304A4A5A : (selected ? C_BORDER_GLOW : (hover ? C_BORDER_GLOW : 0x204A4A6A));
		roundedRectStroke(ctx, x, y, w, h, 8, borderColor, alpha);
		// 选中/悬停光晕
		if (selected || hover) {
			roundedRectA(ctx, x - 1, y - 1, w + 2, h + 2, 9, C_BORDER_GLOW, alpha * 0.15f);
		}
	}

	// ---- 图标徽章 ----

	/** 图标徽章：圆角方块 + 渐变 + 细描边。 */
	public static void iconBadge(DrawContext ctx, int x, int y, int sz, int color, float alpha) {
		roundedRectA(ctx, x, y, sz, sz, 4, withAlpha(color, alpha * 0.3f), alpha);
		roundedRectStroke(ctx, x, y, sz, sz, 4, color, alpha);
		// 顶部高光
		ctx.fill(x + 2, y + 1, x + sz - 2, y + 2, withAlpha(color, alpha * 0.5f));
	}

	// ---- 按钮 ----

	/** 现代按钮：圆角 + 渐变 + 阴影 + 文字。 */
	public static void button(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha, boolean disabled) {
		if (disabled) {
			roundedRectA(ctx, x, y, w, h, 6, 0x301A1A28, alpha);
			ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 7,
					withAlpha(C_DIM, alpha));
			return;
		}
		int dim = mix(color, 0xFF0F0F1A, 0.3f);
		int bright = mix(color, 0xFFFFFFFF, hover ? 0.3f : 0.1f);
		roundedRectGradient(ctx, x, y, w, h, 6, dim, bright, alpha);
		// 顶部高光
		int hi = withAlpha(mix(color, 0xFFFFFFFF, 0.5f), alpha * (hover ? 0.6f : 0.3f));
		roundedRectA(ctx, x + 2, y + 1, w - 4, 1, 3, hi, 1f);
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 7, withAlpha(0xFFFFFFFF, alpha));
	}

	/** 胶囊按钮。 */
	public static void pill(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha, boolean disabled) {
		button(ctx, x, y, w, h, label, tr, hover, color, alpha, disabled);
	}

	/** 玻璃小药丸按钮（半透明）。 */
	public static void glassPill(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, boolean hover, int color, float alpha) {
		roundedRectA(ctx, x, y, w, h, h / 2, hover ? 0x403A3A5A : 0x202A2A44, alpha);
		roundedRectStroke(ctx, x, y, w, h, h / 2, color, alpha);
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 6, withAlpha(0xFFFFFFFF, alpha));
	}

	// ---- 徽章 ----

	/** 现代徽章：圆角 + 渐变 + 文字。 */
	public static void badge(DrawContext ctx, int x, int y, int w, int h, String label,
			TextRenderer tr, int color, float alpha) {
		int dim = mix(color, 0xFF0F0F1A, 0.35f);
		roundedRectGradient(ctx, x, y, w, h, h / 2, dim, color, alpha);
		ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + 4, withAlpha(0xFFFFFFFF, alpha));
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
