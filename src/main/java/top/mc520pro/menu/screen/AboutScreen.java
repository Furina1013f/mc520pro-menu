package top.mc520pro.menu.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import top.mc520pro.menu.ModInfo;
import top.mc520pro.menu.PermissionDetector;
import top.mc520pro.menu.ServerDetector;
import top.mc520pro.menu.UpdateChecker;

/**
 * 关于页（v10）：作者、版本（读自 fabric.mod.json）、连接状态与头衔、
 * 更新日志（读自 Changelog）、GitHub / 爱发电 入口。
 */
public class AboutScreen extends Screen {
	private static final String GITHUB_URL = ModInfo.GITHUB_URL;
	private static final String AFDIAN_URL = ModInfo.AFDIAN_URL;

	private final Screen parent;
	private float openT = 0f;
	private long lastMs = System.currentTimeMillis();
	private String toast;

	public AboutScreen(Screen parent) {
		super(Text.literal("关于 520Pro 菜单"));
		this.parent = parent;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		long now = System.currentTimeMillis();
		this.openT = Math.min(1f, this.openT + Math.max(0f, now - this.lastMs) / 300f);
		this.lastMs = now;
		float open = easeOut(this.openT);

		int pw = Math.max(250, Math.min(420, this.width - 40));
		int ph = Math.min(this.height - 24, 300);
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;

		context.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, withAlpha(0xFF3A3466, open));
		context.fill(px, py, px + pw, py + ph, withAlpha(0xF4110F20, open));

		float phase = ((now % 8000L) / 8000f);
		context.fillGradient(px, py, px + pw, py + 30, withAlpha(mix(0xFF6C46C8, 0xFF2E6D9C, phase), open),
				withAlpha(mix(0xFF2E6D9C, 0xFF6C46C8, phase), open));
		context.fill(px, py + 30, px + pw, py + 31, withAlpha(0xFFB6A4FF, open));
		drawHeart(context, px + 12, py + 9, open);
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("MC.520PRO.TOP 服务器菜单").styled(s -> s.withBold(true)),
				px + 26, py + 7, withAlpha(0xFFFFFFFF, open));
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("v" + ModInfo.version()), px + 26, py + 19, withAlpha(0xFFDCD2FF, open));

		int y = py + 40;
		// 更新状态（自动检测 GitHub Releases）
		UpdateChecker.checkAsync();
		UpdateChecker.Result upd = UpdateChecker.get();
		context.drawTextWithShadow(this.textRenderer, Text.literal("更新"), px + 16, y,
				withAlpha(0xFF8C86A8, open));
		String updText;
		int updColor = 0xFF9C93C4;
		if (upd != null && upd.latestVersion() == null) {
			updText = "无法连接更新源";
			updColor = 0xFFFF8F9A;
		} else if (upd != null && upd.hasUpdate()) {
			updText = "v" + upd.latestVersion() + " 可用（当前 v" + upd.currentVersion() + "）";
			updColor = 0xFFFFD766;
		} else if (upd != null && upd.latestVersion() != null) {
			updText = "已是最新 v" + upd.currentVersion();
			updColor = 0xFF9CFFC9;
		} else {
			updText = "检查中…";
			updColor = 0xFF9C93C4;
		}
		context.drawTextWithShadow(this.textRenderer, Text.literal(updText), px + 56, y,
				withAlpha(updColor, open));
		y += 16;

		// 作者
		context.drawTextWithShadow(this.textRenderer, Text.literal("作者"), px + 16, y,
				withAlpha(0xFF8C86A8, open));
		context.drawTextWithShadow(this.textRenderer,
				Text.literal(ModInfo.authors()).styled(s -> s.withBold(true)), px + 56, y,
				withAlpha(0xFFFFD766, open));
		y += 16;

		// 当前状态
		PermissionDetector.Rank rank = PermissionDetector.currentRank();
		context.drawTextWithShadow(this.textRenderer, Text.literal("状态"), px + 16, y,
				withAlpha(0xFF8C86A8, open));
		String state = ServerDetector.isOnServer()
				? "已连接 " + ServerDetector.SERVER_HOST + " · " + rank.label
				: "未连接（功能预览模式）";
		context.drawTextWithShadow(this.textRenderer, Text.literal(state), px + 56, y,
				withAlpha(ServerDetector.isOnServer() ? rank.color : 0xFF9C93C4, open));
		y += 20;

		// 更新日志
		context.fill(px + 14, y, px + pw - 14, y + 1, withAlpha(0xFF3A3560, open));
		y += 6;
		context.drawTextWithShadow(this.textRenderer, Text.literal("更新日志"), px + 16, y,
				withAlpha(0xFFD9D2FF, open));
		y += 14;

		int btnH = 22;
		int btnY = py + ph - btnH - 6;
		int logLimit = btnY - 8;
		for (String[] line : Changelog.entries()) {
			if (y + 10 > logLimit) {
				break;
			}
			if (!line[0].isEmpty()) {
				context.drawTextWithShadow(this.textRenderer,
						Text.literal(line[0]).styled(s -> s.withBold(true)), px + 16, y,
						withAlpha(0xFF97F5B4, open));
				int descX = px + 52;
				int descW = pw - 28 - 36;
				context.drawTextWithShadow(this.textRenderer, Text.literal(fit(line[1], descW)),
						descX, y, withAlpha(0xFFE4DEFF, open));
			} else {
				int descX = px + 22;
				int descW = pw - 28 - 10;
				context.drawTextWithShadow(this.textRenderer, Text.literal(fit(line[1], descW)),
						descX, y, withAlpha(0xFFB9AEDC, open));
			}
			y += 12;
		}

		// 按钮：GitHub / 爱发电 / 返回
		int gap = 8;
		int btnW = (pw - 28 - gap * 2) / 3;
		drawPill(context, px + 14, btnY, btnW, btnH, "GitHub", !GITHUB_URL.isEmpty(), mouseX, mouseY,
				open, 0xFF5B7FD6);
		drawPill(context, px + 14 + btnW + gap, btnY, btnW, btnH, "爱发电", !AFDIAN_URL.isEmpty(),
				mouseX, mouseY, open, 0xFFD6527F);
		drawPill(context, px + 14 + (btnW + gap) * 2, btnY, btnW, btnH, "返回", true, mouseX, mouseY,
				open, 0xFF6B5FA6);

		if (this.toast != null) {
			context.drawTextWithShadow(this.textRenderer, Text.literal(this.toast), px + 16,
					btnY - 14, withAlpha(0xFFFFB86B, open));
		}
	}

	private void drawPill(DrawContext context, int x, int y, int w, int h, String label, boolean enabled,
			int mouseX, int mouseY, float open, int tint) {
		boolean hov = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		int bg = enabled ? (hov ? mix(tint, 0xFFFFFFFF, 0.28f) : tint) : 0xFF2A2542;
		context.fill(x, y, x + w, y + h, withAlpha(bg, open * (enabled ? 0.92f : 0.6f)));
		outline(context, x, y, w, h,
				withAlpha(enabled ? (hov ? 0xFFFFFFFF : 0xFF7E74C0) : 0xFF4E4870, open));
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal(enabled ? label : label + " 待开通"), x + w / 2, y + 7,
				withAlpha(enabled ? 0xFFFFFFFF : 0xFF9A96B5, open));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		int pw = Math.max(250, Math.min(420, this.width - 40));
		int ph = Math.min(this.height - 24, 300);
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;
		int btnH = 22;
		int btnY = py + ph - btnH - 6;
		int gap = 8;
		int btnW = (pw - 28 - gap * 2) / 3;

		if (mouseY >= btnY && mouseY < btnY + btnH) {
			if (mouseX >= px + 14 && mouseX < px + 14 + btnW) {
				openLink(GITHUB_URL, "GitHub");
				return true;
			}
			if (mouseX >= px + 14 + btnW + gap && mouseX < px + 14 + (btnW + gap) * 2) {
				openLink(AFDIAN_URL, "爱发电");
				return true;
			}
			if (mouseX >= px + 14 + (btnW + gap) * 2 && mouseX < px + 14 + (btnW + gap) * 2 + btnW) {
				if (this.client != null) {
					this.client.setScreen(this.parent);
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void openLink(String url, String name) {
		if (url == null || url.isEmpty()) {
			this.toast = name + " 链接尚未开通，敬请期待";
			return;
		}
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			client.keyboard.setClipboard(url);
			this.toast = "已复制：" + url;
		} catch (Throwable ignored) {
			this.toast = "打开失败";
		}
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void drawHeart(DrawContext context, int x, int y, float alpha) {
		int color = withAlpha(0xFFFF5C7A, alpha);
		int[][] cells = {
				{ 1, 2, 4, 5 },
				{ 0, 1, 2, 3, 4, 5, 6 },
				{ 0, 1, 2, 3, 4, 5, 6 },
				{ 1, 2, 3, 4, 5 },
				{ 2, 3, 4 },
				{ 3 },
		};
		for (int row = 0; row < cells.length; row++) {
			for (int col : cells[row]) {
				context.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}

	private void outline(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y, x + 1, y + h, color);
		context.fill(x + w - 1, y, x + w, y + h, color);
	}

	/** 超宽文本截断（加省略号）。 */
	private String fit(String text, int maxWidth) {
		if (text == null) {
			return "";
		}
		if (this.textRenderer.getWidth(text) <= maxWidth) {
			return text;
		}
		String out = text;
		while (out.length() > 1 && this.textRenderer.getWidth(out + "…") > maxWidth) {
			out = out.substring(0, out.length() - 1);
		}
		return out + "…";
	}

	private static int withAlpha(int color, float alpha) {
		int baseA = (color >>> 24) & 0xFF;
		if (baseA == 0) {
			baseA = 0xFF;
		}
		int a = Math.round(baseA * Math.min(1f, Math.max(0f, alpha)));
		return (Math.min(255, Math.max(0, a)) << 24) | (color & 0xFFFFFF);
	}

	private static int mix(int c1, int c2, float t) {
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

	private static float easeOut(float t) {
		float k = Math.min(1f, Math.max(0f, t));
		float inv = 1f - k;
		return 1f - inv * inv * inv;
	}
}
