package top.mc520pro.menu.screen;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * 纹理渲染工具：每个 UI 元素一个独立 sprite（nine-slice 拉伸）。
 * sprite 位于 textures/gui/sprites/mc520pro/，带 mcmeta 定义九宫格边框。
 * drawGuiTexture 自动处理 sprite atlas 与 nine_slice 缩放。
 */
public final class TextureKit {
	private static final String PREFIX = "mc520pro";

	private TextureKit() {
	}

	private static Identifier id(String name) {
		return Identifier.of(PREFIX, "textures/gui/sprites/mc520pro/" + name);
	}

	/** 画一个 UI sprite（nine-slice 自动拉伸）。 */
	public static void draw(DrawContext ctx, String name, int x, int y, int w, int h) {
		ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, id(name), x, y, w, h);
	}

	// ---- sprite 名称常量 ----
	public static final String PANEL_GLASS = "panel_glass";
	public static final String PANEL_SOLID = "panel_solid";
	public static final String CARD_NORMAL = "card_normal";
	public static final String CARD_HOVER = "card_hover";
	public static final String CARD_SELECTED = "card_selected";
	public static final String CARD_DISABLED = "card_disabled";
	public static final String BTN_ACCENT = "btn_accent";
	public static final String BTN_SUCCESS = "btn_success";
	public static final String BTN_DANGER = "btn_danger";
	public static final String BTN_BLUE = "btn_blue";
	public static final String BTN_DISABLED = "btn_disabled";
	public static final String BADGE_GREEN = "badge_green";
	public static final String BADGE_RED = "badge_red";
	public static final String BADGE_ORANGE = "badge_orange";
	public static final String BADGE_BLUE = "badge_blue";
	public static final String BADGE_GOLD = "badge_gold";
	public static final String BADGE_PURPLE = "badge_purple";
	public static final String ICON_BADGE = "icon_badge";
	public static final String SCROLLBAR_TRACK = "scrollbar_track";
	public static final String SCROLLBAR_THUMB = "scrollbar_thumb";
	public static final String TOOLTIP_BG = "tooltip_bg";
	public static final String PREVIEW_BG = "preview_bg";
	public static final String PROGRESS_BAR = "progress_bar";
	public static final String AVATAR_DEFAULT = "avatar_default";
	public static final String AVATAR_PLAYER = "avatar_player";
	public static final String SEARCH_BOX = "search_box";
	public static final String STAR_FULL = "star_full";
	public static final String STAR_EMPTY = "star_empty";
	public static final String LOADING_SPINNER = "loading_spinner";
	public static final String BADGE_DOT = "badge_dot";
	public static final String DIVIDER = "divider";
}
