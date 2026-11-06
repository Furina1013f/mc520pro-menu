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
 * 关于页（现代风格）：作者、版本、更新状态、连接状态、更新日志、GitHub/爱发电入口。
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

		// 现代玻璃面板
		GuiKit.glassPanel(context, px, py, pw, ph, open);

		// 头部渐变横幅（现代圆角风格）
		float phase = ((now % 8000L) / 8000f);
		int h1 = GuiKit.mix(GuiKit.C_ACCENT, GuiKit.C_ACCENT2, phase);
		int h2 = GuiKit.mix(GuiKit.C_ACCENT2, GuiKit.C_ACCENT, phase);
		context.fillGradient(px + 2, py + 2, px + pw - 2, py + 26, GuiKit.withAlpha(h1, open), GuiKit.withAlpha(h2, open));
		// 圆角遮罩效果（用底部渐变覆盖）
		context.fillGradient(px + 2, py + 24, px + pw - 2, py + 32, GuiKit.withAlpha(h2, open * 0.5f), GuiKit.withAlpha(0xFF1A1A2E, open * 0.8f));

		// 标题
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("MC.520PRO.TOP 服务器菜单").styled(s -> s.withBold(true)),
				px + 16, py + 7, GuiKit.withAlpha(0xFFFFFFFF, open));
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("v" + ModInfo.version()), px + 16, py + 19, GuiKit.withAlpha(0xFFE0E0FF, open));

		int y = py + 38;
		// 更新状态
		UpdateChecker.checkAsync();
		UpdateChecker.Result upd = UpdateChecker.get();
		context.drawTextWithShadow(this.textRenderer, Text.literal("更新"), px + 16, y,
				GuiKit.withAlpha(GuiKit.C_MUTED, open));
		String updText;
		int updColor = GuiKit.C_MUTED;
		if (upd != null && upd.latestVersion() == null) {
			updText = "无法连接更新源";
			updColor = GuiKit.C_DANGER;
		} else if (upd != null && upd.hasUpdate()) {
			updText = "v" + upd.latestVersion() + " 可用（当前 v" + upd.currentVersion() + "）";
			updColor = GuiKit.C_WARNING;
		} else if (upd != null && upd.latestVersion() != null) {
			updText = "已是最新 v" + upd.currentVersion();
			updColor = GuiKit.C_SUCCESS;
		} else {
			updText = "检查中…";
		}
		context.drawTextWithShadow(this.textRenderer, Text.literal(updText), px + 56, y,
				GuiKit.withAlpha(updColor, open));
		y += 16;

		// 作者
		context.drawTextWithShadow(this.textRenderer, Text.literal("作者"), px + 16, y,
				GuiKit.withAlpha(GuiKit.C_MUTED, open));
		context.drawTextWithShadow(this.textRenderer,
				Text.literal(ModInfo.authors()).styled(s -> s.withBold(true)), px + 56, y,
				GuiKit.withAlpha(GuiKit.C_GOLD, open));
		y += 16;

		// 当前状态
		PermissionDetector.Rank rank = PermissionDetector.currentRank();
		context.drawTextWithShadow(this.textRenderer, Text.literal("状态"), px + 16, y,
				GuiKit.withAlpha(GuiKit.C_MUTED, open));
		String state = ServerDetector.isOnServer()
				? "已连接 " + ServerDetector.SERVER_HOST + " · " + rank.label
				: "未连接（功能预览模式）";
		context.drawTextWithShadow(this.textRenderer, Text.literal(state), px + 56, y,
				GuiKit.withAlpha(ServerDetector.isOnServer() ? rank.color : GuiKit.C_MUTED, open));
		y += 20;

		// 更新日志
		context.fill(px + 14, y, px + pw - 14, y + 1, GuiKit.withAlpha(GuiKit.C_BORDER, open));
		y += 6;
		context.drawTextWithShadow(this.textRenderer, Text.literal("更新日志"), px + 16, y,
				GuiKit.withAlpha(GuiKit.C_TEXT, open));
		y += 14;

		int btnH = 22;
		int btnY = py + ph - btnH - 6;
		int logLimit = btnY - 8;
		for (String[] line : Changelog.entries()) {
			if (y + 10 > logLimit) {
				break;
			}
			if (!line[0].isEmpty()) {
				// 版本号 + 星星
				context.drawTextWithShadow(this.textRenderer,
						Text.literal(line[0]).styled(s -> s.withBold(true)), px + 16, y,
						GuiKit.withAlpha(GuiKit.C_SUCCESS, open));
				// 星星图标
				TextureKit.draw(context, TextureKit.STAR_FULL, px + pw - 20, y, 10, 10);
				int descX = px + 52;
				int descW = pw - 28 - 36;
				context.drawTextWithShadow(this.textRenderer, Text.literal(fit(line[1], descW)),
						descX, y, GuiKit.withAlpha(0xFFE0E0FF, open));
			} else {
				int descX = px + 22;
				int descW = pw - 28 - 10;
				context.drawTextWithShadow(this.textRenderer, Text.literal(fit(line[1], descW)),
						descX, y, GuiKit.withAlpha(GuiKit.C_MUTED, open));
			}
			y += 12;
		}

		// 按钮：GitHub / 爱发电 / 返回
		int gap = 8;
		int btnW = (pw - 28 - gap * 2) / 3;
		boolean ghHover = mouseX >= px + 14 && mouseX < px + 14 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
		boolean afHover = mouseX >= px + 14 + btnW + gap && mouseX < px + 14 + (btnW + gap) * 2 && mouseY >= btnY && mouseY < btnY + btnH;
		boolean backHover = mouseX >= px + 14 + (btnW + gap) * 2 && mouseX < px + 14 + (btnW + gap) * 2 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
		GuiKit.pill(context, px + 14, btnY, btnW, btnH, "GitHub", this.textRenderer, ghHover,
				GuiKit.C_BLUE, open, GITHUB_URL.isEmpty());
		GuiKit.pill(context, px + 14 + btnW + gap, btnY, btnW, btnH, "爱发电", this.textRenderer, afHover,
				GuiKit.C_PURPLE, open, AFDIAN_URL.isEmpty());
		GuiKit.pill(context, px + 14 + (btnW + gap) * 2, btnY, btnW, btnH, "返回", this.textRenderer, backHover,
				GuiKit.C_ACCENT, open, false);

		if (this.toast != null) {
			context.drawTextWithShadow(this.textRenderer, Text.literal(this.toast), px + 16,
					btnY - 14, GuiKit.withAlpha(GuiKit.C_WARNING, open));
		}
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

	private static float easeOut(float t) {
		float k = Math.min(1f, Math.max(0f, t));
		float inv = 1f - k;
		return 1f - inv * inv * inv;
	}
}
