package top.mc520pro.menu.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import top.mc520pro.menu.PermissionDetector;
import top.mc520pro.menu.ServerDetector;

/**
 * 在线玩家列表页（v10）：
 * 一次列出全部在线玩家（名字 / 延迟 / 头衔显示名），每行右侧两个快捷按钮
 * —— 「传送」直接发起 /tpa，「召唤」发起 /tpahere，点击即发送，不用再填参数。
 */
public class OnlinePlayersScreen extends Screen {
	private static final int OUTER_PAD = 8;
	private final Screen parent;
	private final List<PermissionDetector.OnlinePlayer> players = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private int panelW;
	private int panelH;
	private int panelX;
	private int panelY;
	private int listTop;
	private int listH;
	private int scrollY;
	private float openT = 0f;
	private long lastMs = System.currentTimeMillis();
	private String toast;
	private long toastUntil;

	private static final int ROW_H = 22;
	private static final int BTN_W = 34;

	private static final class Row {
		int x;
		int y;
		int w;
		PermissionDetector.OnlinePlayer player;
	}

	public OnlinePlayersScreen(Screen parent) {
		super(Text.literal("在线玩家"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.players.clear();
		this.players.addAll(PermissionDetector.onlinePlayerDetails());

		int W = this.width;
		int H = this.height;
		this.panelW = Math.max(250, Math.min(400, W - 40));
		int headerH = 30;
		int footerH = 30;
		int visible = Math.max(3, (H - OUTER_PAD * 2 - headerH - footerH - 14) / ROW_H);
		if (this.players.isEmpty()) {
			this.listH = 52; // 空状态占位高度（两行说明 + 上下留白）
		} else {
			int maxRows = Math.max(1, Math.min(this.players.size(), visible));
			this.listH = maxRows * ROW_H;
		}
		this.panelH = headerH + 8 + this.listH + footerH + 8;
		this.panelH = Math.min(this.panelH, H - OUTER_PAD * 2);
		this.panelX = (W - this.panelW) / 2;
		this.panelY = Math.max(4, (H - this.panelH) / 2);
		this.listTop = this.panelY + headerH + 8;

		this.rows.clear();
		int listX = this.panelX + 12;
		int listW = this.panelW - 24;
		for (int i = 0; i < this.players.size(); i++) {
			Row r = new Row();
			r.x = listX;
			r.y = this.listTop + i * ROW_H;
			r.w = listW;
			r.player = this.players.get(i);
			this.rows.add(r);
		}
	}

	private void send(String command) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null && client.player.networkHandler != null) {
			client.player.networkHandler.sendChatCommand(command);
			client.setScreen(null);
		}
	}

	private void showToast(String text) {
		this.toast = text;
		this.toastUntil = System.currentTimeMillis() + 2000;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		long now = System.currentTimeMillis();
		this.openT = Math.min(1f, this.openT + Math.max(0f, (now - this.lastMs) / 300f));
		this.lastMs = now;
		float open = easeOut(this.openT);

		int px = this.panelX;
		int py = this.panelY;
		int pw = this.panelW;

		context.fill(px - 2, py - 2, px + pw + 2, py + this.panelH + 2, withAlpha(0xFF3A3466, open));
		context.fill(px, py, px + pw, py + this.panelH, withAlpha(0xF4110F20, open));
		context.fillGradient(px, py, px + pw, py + 30, withAlpha(0xFF6C46C8, open),
				withAlpha(0xFF2E6D9C, open));
		context.fill(px, py + 30, px + pw, py + 31, withAlpha(0xFFB6A4FF, open));

		context.drawTextWithShadow(this.textRenderer,
				Text.literal("在线玩家").styled(s -> s.withBold(true)), px + 12, py + 6,
				withAlpha(0xFFFFFFFF, open));
		String count = this.players.size() + " 人在线"
				+ (ServerDetector.isOnServer() ? " · 点击右侧按钮直接操作" : " · 未连接服务器，列表为本地数据");
		context.drawTextWithShadow(this.textRenderer, Text.literal(count), px + 12, py + 19,
				withAlpha(0xFFCFE4FF, open));

		int viewBottom = this.listTop + this.listH;
		context.enableScissor(px + 2, this.listTop, px + pw - 2, viewBottom);

		if (this.players.isEmpty()) {
			// 空状态说明（离线或服务器还没下发列表）
			int cx = px + pw / 2;
			int cy = this.listTop + 6;
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal(ServerDetector.isOnServer() ? "暂无其他在线玩家" : "未连接到服务器"),
					cx, cy, withAlpha(0xFF9C93C4, open));
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal(ServerDetector.isOnServer()
							? "稍等片刻或刷新菜单即可看到最新列表"
							: "进入 " + ServerDetector.SERVER_HOST + " 后即可查看在线玩家"),
					cx, cy + 13, withAlpha(0xFF6E6890, open));
		}

		Row hoveredRow = null;
		for (Row r : this.rows) {
			int y = r.y - this.scrollY;
			if (y + ROW_H < this.listTop || y > viewBottom) {
				continue;
			}
			boolean hovered = mouseX >= r.x && mouseX < r.x + r.w && mouseY >= y && mouseY < y + ROW_H;
			boolean self = r.player.self();
			int bg = self ? 0xC0302A58 : (hovered ? 0xE62E2750 : 0xC81F1B36);
			context.fill(r.x, y, r.x + r.w, y + ROW_H, withAlpha(bg, open));
			outline(context, r.x, y, r.w, ROW_H, withAlpha(self ? 0xFF8F7FD9 : 0xFF3E3862, open));

			int textX = r.x + 8;
			int pingX = r.x + r.w - BTN_W * 2 - 42;
			// 名字（自己高亮金色）
			String nameText = fit(r.player.name(), 90);
			context.drawTextWithShadow(this.textRenderer,
					Text.literal(nameText), textX, y + 7,
					withAlpha(self ? 0xFFFFD766 : 0xFFFFFFFF, open));
			// 头衔/显示名：紧跟名字宽度，剥掉 § 颜色码
			String disp = r.player.displayName();
			if (disp != null && !disp.isEmpty()) {
				String plain = disp.replaceAll("§.", "");
				if (!plain.equals(r.player.name())) {
					int titleX = textX + this.textRenderer.getWidth(nameText) + 8;
					int titleMax = Math.max(0, pingX - titleX - 6);
					if (titleMax > 24) {
						context.drawTextWithShadow(this.textRenderer,
								Text.literal(fit(plain, titleMax)), titleX, y + 7,
								withAlpha(0xFFB9AEE8, open));
					}
				}
			}
			// 延迟
			String ping = r.player.latency() >= 0 ? r.player.latency() + "ms" : "?ms";
			context.drawTextWithShadow(this.textRenderer, Text.literal(ping), pingX, y + 7,
					withAlpha(r.player.latency() < 80 ? 0xFF9CFFC9
							: (r.player.latency() < 160 ? 0xFFFFD766 : 0xFFFF8F9A), open));

			// 两个操作按钮
			drawAction(context, r.x + r.w - BTN_W * 2 - 4, y + 2, "传", mouseX, mouseY, open,
					r.player.name(), "tpa");
			drawAction(context, r.x + r.w - BTN_W - 4, y + 2, "召", mouseX, mouseY, open,
					r.player.name(), "tpahere");

			if (hovered) {
				hoveredRow = r;
			}
		}
		context.disableScissor();

		// 底部：返回
		int btnH = 22;
		int btnY = py + this.panelH - btnH - 6;
		int btnW = 90;
		int backX = px + (pw - btnW) / 2;
		boolean backHover = mouseX >= backX && mouseX < backX + btnW && mouseY >= btnY
				&& mouseY < btnY + btnH;
		context.fill(backX, btnY, backX + btnW, btnY + btnH,
				withAlpha(backHover ? 0xE6614FA6 : 0xD02A2542, open));
		outline(context, backX, btnY, btnW, btnH,
				withAlpha(backHover ? 0xFFFFFFFF : 0xFF54497F, open));
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("← 返回"),
				backX + btnW / 2, btnY + 7, withAlpha(0xFFFFFFFF, open));

		if (this.toast != null) {
			long left = this.toastUntil - now;
			if (left <= 0) {
				this.toast = null;
			} else {
				int tw = this.textRenderer.getWidth(this.toast) + 16;
				int tx = px + (pw - tw) / 2;
				int ty = btnY - 20;
				context.fill(tx, ty, tx + tw, ty + 17, withAlpha(0xE0100F1C, open * clamp(left / 400f)));
				context.drawTextWithShadow(this.textRenderer, Text.literal(this.toast), tx + 8, ty + 5,
						withAlpha(0xFFFF8F9A, open * clamp(left / 400f)));
			}
		}

		if (hoveredRow != null) {
			drawHint(context, mouseX, mouseY,
					hoveredRow.player.name() + "：" + hoveredRow.player.latency() + "ms"
							+ " · 「传」= /tpa 到他 · 「召」= 把他传到你",
					open);
		}
	}

	private void drawAction(DrawContext context, int x, int y, String label, int mouseX, int mouseY,
			float open, String playerName, String action) {
		boolean hov = mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + 18;
		boolean enabled = ServerDetector.isOnServer();
		int tint = action.equals("tpa") ? 0xFF3E9E6E : 0xFF5B7FD6;
		context.fill(x, y, x + BTN_W, y + 18,
				withAlpha(!enabled ? 0xB0201C33 : (hov ? mix(tint, 0xFFFFFFFF, 0.3f) : tint), open));
		outline(context, x, y, BTN_W, 18,
				withAlpha(enabled ? (hov ? 0xFFFFFFFF : 0xFF7E74C0) : 0xFF3E3A5C, open));
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label),
				x + BTN_W / 2, y + 5, withAlpha(enabled ? 0xFFFFFFFF : 0xFF6E6890, open));
	}

	private void drawHint(DrawContext context, int mouseX, int mouseY, String text, float open) {
		int w = this.textRenderer.getWidth(text) + 14;
		int h = 18;
		int tx = Math.min(mouseX + 12, this.width - w - 6);
		int ty = Math.max(4, Math.min(mouseY - 10, this.height - h - 6));
		context.fill(tx, ty, tx + w, ty + h, withAlpha(0xF8100F1C, open));
		outline(context, tx, ty, w, h, withAlpha(0xFF7C6BD9, open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(text), tx + 7, ty + 5,
				withAlpha(0xFFE4DEFF, open));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		int viewBottom = this.listTop + this.listH;
		if (mouseY >= this.listTop && mouseY <= viewBottom) {
			for (Row r : this.rows) {
				int y = r.y - this.scrollY;
				if (mouseY < y || mouseY >= y + ROW_H) {
					continue;
				}
				if (mouseX >= r.x + r.w - BTN_W * 2 - 4 && mouseX < r.x + r.w - BTN_W - 4) {
					if (!ServerDetector.isOnServer()) {
						showToast("仅服务器内可用");
						return true;
					}
					send("tpa " + r.player.name());
					return true;
				}
				if (mouseX >= r.x + r.w - BTN_W - 4 && mouseX < r.x + r.w - 4) {
					if (!ServerDetector.isOnServer()) {
						showToast("仅服务器内可用");
						return true;
					}
					send("tpahere " + r.player.name());
					return true;
				}
				// 点名字 → 复制
				MinecraftClient.getInstance().keyboard.setClipboard(r.player.name());
				showToast("已复制 " + r.player.name());
				return true;
			}
		}

		int btnH = 22;
		int btnY = this.panelY + this.panelH - btnH - 6;
		int btnW = 90;
		int backX = this.panelX + (this.panelW - btnW) / 2;
		if (mouseX >= backX && mouseX < backX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
			if (this.client != null) {
				this.client.setScreen(this.parent);
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
			double verticalAmount) {
		int maxScroll = Math.max(0, this.rows.size() * ROW_H - this.listH);
		this.scrollY = clampi(this.scrollY - (int) Math.signum(verticalAmount) * 18, 0, maxScroll);
		return true;
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

	private static int clampi(int v, int lo, int hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	private static float clamp(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
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

	private void outline(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y, x + 1, y + h, color);
		context.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static int withAlpha(int color, float alpha) {
		int baseA = (color >>> 24) & 0xFF;
		if (baseA == 0) {
			baseA = 0xFF;
		}
		int a = Math.round(baseA * clamp(alpha));
		return (clampInt(a) << 24) | (color & 0xFFFFFF);
	}

	private static int clampInt(int v) {
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	private static int mix(int c1, int c2, float t) {
		float k = clamp(t);
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
		float k = clamp(t);
		float inv = 1f - k;
		return 1f - inv * inv * inv;
	}
}
