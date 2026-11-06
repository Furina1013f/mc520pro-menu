package top.mc520pro.menu.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * 纹理渲染工具：用纹理图集画 UI，取代 fill 画的方块。
 * 纹理图集 512x512，16x16 格子，每格 32x32。
 */
public final class TextureKit {
	public static final Identifier ATLAS = Identifier.of("mc520pro", "textures/ui_atlas.png");
	private static final int COLS = 16;

	private TextureKit() {
	}

	/** UV 坐标。 */
	private static float uv(int col) {
		return col / (float) COLS;
	}

	/** 画纹理（接受格子索引 0-255，自动计算列/行）。 */
	public static void draw(DrawContext ctx, int cellIndex, int x, int y, int w, int h) {
		int texCol = cellIndex % COLS;
		int texRow = cellIndex / COLS;
		ctx.drawTexturedQuad(ATLAS, x, y, w, h,
				uv(texCol), uv(texRow), uv(texCol + 1), uv(texRow + 1));
	}

	// ---- 预定义格子坐标 ----
	// Row 0: 面板与卡片
	public static final int PANEL_GLASS = 0;
	public static final int PANEL_SOLID = 1;
	public static final int CARD_NORMAL = 2;
	public static final int CARD_HOVER = 3;
	public static final int CARD_SELECTED = 4;
	public static final int CARD_DISABLED = 5;
	// Row 1: 按钮
	public static final int BTN_NORMAL = 16;
	public static final int BTN_ACCENT = 17;
	public static final int BTN_DISABLED = 18;
	public static final int BTN_SUCCESS = 19;
	public static final int BTN_DANGER = 20;
	public static final int BTN_BLUE = 21;
	// Row 2: 徽章
	public static final int BADGE_GREEN = 32;
	public static final int BADGE_RED = 33;
	public static final int BADGE_ORANGE = 34;
	public static final int BADGE_BLUE = 35;
	public static final int BADGE_GOLD = 36;
	public static final int BADGE_PURPLE = 37;
	// Row 3: 图标徽章、滚动条、浮窗、预览、分隔线
	public static final int ICON_BADGE = 48;
	public static final int SCROLLBAR_TRACK = 49;
	public static final int SCROLLBAR_THUMB = 50;
	public static final int TOOLTIP_BG = 51;
	public static final int PREVIEW_BG = 52;
	public static final int DIVIDER = 53;
}
