package top.mc520pro.menu.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import top.mc520pro.menu.PermissionDetector;
import top.mc520pro.menu.data.CommandCatalog;

/**
 * 二级向导页（现代风格）：带参数指令的填写界面。
 * 玩家名可点选在线列表，数字用 +/- 调节，底部预览，完成即发送。
 */
public class CommandWizardScreen extends Screen {
	public static final class Field {
		final String label;
		final PermissionDetector.ArgType type;
		String value = "";
		int number = 1;
		int min = 1;
		int max = 999;

		Field(String label, PermissionDetector.ArgType type) {
			this.label = label;
			this.type = type;
		}

		String resolved() {
			return switch (this.type) {
				case NUMBER -> String.valueOf(this.number);
				default -> this.value.trim();
			};
		}
	}

	private static final int ROW_H = 42;

	private final Screen parent;
	private final CommandCatalog.Entry entry;
	private final List<Field> fields = new ArrayList<>();
	private final List<TextFieldWidget> boxes = new ArrayList<>();
	private int pickerFor = -1;
	private int pickerScroll = 0;
	private float openT = 0f;
	private float openAlpha = 0f;
	private long lastMs = System.currentTimeMillis();
	private String toast;

	/** 从指令模板解析参数。 */
	public static List<Field> parseFields(String command) {
		List<Field> out = new ArrayList<>();
		if (command == null) {
			return out;
		}
		int i = 0;
		while (i < command.length()) {
			char c = command.charAt(i);
			if (c == '<' || c == '[') {
				char close = c == '<' ? '>' : ']';
				int end = command.indexOf(close, i + 1);
				if (end < 0) {
					break;
				}
				String raw = command.substring(i + 1, end).trim();
				out.add(new Field(raw, inferType(raw)));
				i = end + 1;
			} else {
				i++;
			}
		}
		return out;
	}

	private static PermissionDetector.ArgType inferType(String raw) {
		String n = raw.toLowerCase(Locale.ROOT);
		if (n.contains("玩家") || n.contains("player") || n.contains("target") || n.contains("好友")) {
			return PermissionDetector.ArgType.PLAYER;
		}
		if (n.matches(".*\\d+\\s*[-~]\\s*\\d+.*") || n.contains("数量") || n.contains("金额")
				|| n.contains("距离") || n.contains("价格") || n.contains("等级") || n.contains("速度")
				|| n.contains("半径") || n.contains("amount") || n.contains("number") || n.contains("count")
				|| n.contains("distance") || n.contains("radius") || n.contains("price")
				|| n.contains("level") || n.contains("speed")) {
			return PermissionDetector.ArgType.NUMBER;
		}
		if (n.contains("消息") || n.contains("理由") || n.contains("内容") || n.contains("message")
				|| n.contains("reason") || n.contains("text")) {
			return PermissionDetector.ArgType.TEXT;
		}
		return PermissionDetector.ArgType.WORD;
	}

	public CommandWizardScreen(Screen parent, CommandCatalog.Entry entry) {
		super(Text.literal("向导 · " + entry.name()));
		this.parent = parent;
		this.entry = entry;
		this.fields.addAll(parseFields(entry.command()));
		for (Field f : this.fields) {
			if (f.type == PermissionDetector.ArgType.NUMBER) {
				String digits = f.label.replaceAll("[^0-9\\-~]", " ");
				String[] parts = digits.trim().split("[\\s\\-~]+");
				try {
					if (parts.length >= 2) {
						f.min = Integer.parseInt(parts[0].trim());
						f.max = Integer.parseInt(parts[1].trim());
					} else if (parts.length == 1 && !parts[0].isEmpty()) {
						f.max = Math.max(1, Integer.parseInt(parts[0].trim()));
					}
				} catch (NumberFormatException ignored) {
				}
				if (f.max < f.min) {
					int t = f.min;
					f.min = f.max;
					f.max = t;
				}
				f.number = f.min;
			}
		}
	}

	private int panelW() {
		return Math.max(250, Math.min(400, this.width - 40));
	}

	private int panelH() {
		return Math.min(this.height - 24, 100 + this.fields.size() * ROW_H);
	}

	private int panelX() {
		return (this.width - panelW()) / 2;
	}

	private int panelY() {
		return Math.max(8, (this.height - panelH()) / 2);
	}

	private int btnY() {
		return panelY() + panelH() - 34;
	}

	@Override
	protected void init() {
		this.clearChildren();
		this.boxes.clear();
		int px = panelX();
		int py = panelY();
		for (int i = 0; i < this.fields.size(); i++) {
			Field f = this.fields.get(i);
			int boxW = panelW() - 38;
			if (f.type == PermissionDetector.ArgType.NUMBER) {
				boxW -= 56;
			} else if (f.type == PermissionDetector.ArgType.PLAYER) {
				boxW -= 30;
			}
			TextFieldWidget box = new TextFieldWidget(this.textRenderer, px + 20,
					py + 52 + i * ROW_H + 13, Math.max(60, boxW), 18, Text.literal(f.label));
			box.setMaxLength(80);
			box.setText(f.value);
			final int idx = i;
			box.setChangedListener(text -> this.fields.get(idx).value = text);
			this.addDrawableChild(box);
			this.boxes.add(box);
		}
	}

	private String buildCommand() {
		String[] parts = this.entry.command().split(" ");
		StringBuilder sb = new StringBuilder(parts[0]);
		int slot = 0;
		for (int i = 1; i < parts.length; i++) {
			String p = parts[i];
			if (p.startsWith("<") || p.startsWith("[")) {
				if (slot < this.fields.size()) {
					String v = this.fields.get(slot).resolved();
					if (!v.isEmpty()) {
						sb.append(' ').append(v);
					}
				}
				slot++;
			} else {
				sb.append(' ').append(p);
			}
		}
		return sb.toString();
	}

	private boolean allFilled() {
		if (this.fields.isEmpty()) {
			return true;
		}
		for (Field f : this.fields) {
			if (f.resolved().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private void finish() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.player.networkHandler == null) {
			this.toast = "未连接到服务器";
			return;
		}
		String cmd = buildCommand();
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1);
		}
		client.player.networkHandler.sendChatCommand(cmd);
		client.setScreen(null);
	}

	// ---------- 绘制：面板底 ----------

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		long now = System.currentTimeMillis();
		this.openT = Math.min(1f, this.openT + Math.max(0f, (now - this.lastMs) / 300f));
		this.lastMs = now;
		float open = easeOut(this.openT);
		this.openAlpha = open;

		super.renderBackground(context, mouseX, mouseY, delta);

		int px = panelX();
		int py = panelY();
		int pw = panelW();
		int ph = panelH();

		GuiKit.glassPanel(context, px, py, pw, ph, open);

		float phase = (this.openT * 3.0f) % 1f;
		int h1 = GuiKit.mix(GuiKit.C_ACCENT, GuiKit.C_ACCENT2, phase);
		int h2 = GuiKit.mix(GuiKit.C_ACCENT2, GuiKit.C_ACCENT, phase);
		context.fillGradient(px + 2, py + 2, px + pw - 2, py + 26, GuiKit.withAlpha(h1, open), GuiKit.withAlpha(h2, open));
		context.fillGradient(px + 2, py + 24, px + pw - 2, py + 32, GuiKit.withAlpha(h2, open * 0.5f), GuiKit.withAlpha(0xFF1A1A2E, open * 0.8f));

		context.drawTextWithShadow(this.textRenderer,
				Text.literal(this.entry.name()).styled(s -> s.withBold(true)), px + 16, py + 7,
				GuiKit.withAlpha(0xFFFFFFFF, open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(fit(this.entry.usage(), pw - 24)),
				px + 16, py + 19, GuiKit.withAlpha(0xFFE0E0FF, open));

		// 参数标签
		for (int i = 0; i < this.fields.size(); i++) {
			Field f = this.fields.get(i);
			int rowY = py + 52 + i * ROW_H;
			String hint = switch (f.type) {
				case PLAYER -> "点右侧三角选在线玩家";
				case NUMBER -> "范围 " + f.min + "~" + f.max;
				case TEXT -> "自由填写";
				case WORD -> "填写名称";
			};
			context.drawTextWithShadow(this.textRenderer,
					Text.literal(f.label + "  ·  " + hint), px + 20, rowY, GuiKit.withAlpha(GuiKit.C_MUTED, open));
		}
	}

	// ---------- 绘制：控件之上 ----------

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		float open = this.openAlpha;
		int px = panelX();
		int py = panelY();
		int pw = panelW();
		int ph = panelH();

		// 数字 / 玩家 附加按钮
		for (int i = 0; i < this.fields.size(); i++) {
			Field f = this.fields.get(i);
			int rowY = py + 52 + i * ROW_H;
			int boxY = rowY + 13;
			if (f.type == PermissionDetector.ArgType.NUMBER) {
				int textRight = px + pw - 56;
				String num = String.valueOf(f.number);
				int numW = this.textRenderer.getWidth(num);
				context.drawTextWithShadow(this.textRenderer, Text.literal(num),
						textRight - numW, boxY + 5, GuiKit.withAlpha(GuiKit.C_SUCCESS, open));
				int bx = px + pw - 46;
				drawSmall(context, bx, boxY, 20, 18, "−", open, mouseX, mouseY, f.number > f.min);
				drawSmall(context, bx + 24, boxY, 20, 18, "+", open, mouseX, mouseY, f.number < f.max);
			} else if (f.type == PermissionDetector.ArgType.PLAYER) {
				int bx = px + pw - 44;
				boolean hov = mouseX >= bx && mouseX < bx + 22 && mouseY >= boxY && mouseY < boxY + 18;
				GuiKit.roundedRect(context, bx, boxY, 22, 18, 4,
						GuiKit.withAlpha(hov ? GuiKit.C_ACCENT : 0xCC1A1A2E, open));
				GuiKit.roundedRectStroke(context, bx, boxY, 22, 18, 4, GuiKit.C_ACCENT, open);
				drawTriangleDown(context, bx + 11, boxY + 9, 5, GuiKit.withAlpha(0xFFFFFFFF, open));
			}
		}

		// 预览
		String preview = "/" + buildCommand();
		int previewY = py + ph - 62;
		GuiKit.roundedRect(context, px + 14, previewY, pw - 28, 20, 4, GuiKit.withAlpha(0xCC1A1A2E, open));
		GuiKit.roundedRectStroke(context, px + 14, previewY, pw - 28, 20, 4, GuiKit.C_BORDER, open);
		context.drawTextWithShadow(this.textRenderer, Text.literal(fit("→ " + preview, pw - 36)),
				px + 20, previewY + 6,
				GuiKit.withAlpha(allFilled() ? GuiKit.C_SUCCESS : GuiKit.C_MUTED, open));

		// 完成按钮
		int btnH = 22;
		int by = btnY();
		int okW = 108;
		int okX = px + pw - okW - 14;
		boolean canDo = allFilled();
		boolean okHover = mouseX >= okX && mouseX < okX + okW && mouseY >= by && mouseY < by + btnH;
		GuiKit.pill(context, okX, by, okW, btnH, canDo ? "完成并发送" : "请填写参数",
				this.textRenderer, okHover, canDo ? GuiKit.C_SUCCESS : GuiKit.C_DIM, open, !canDo);

		// 返回按钮
		int backW = 66;
		int backX = px + 14;
		boolean backHover = mouseX >= backX && mouseX < backX + backW && mouseY >= by
				&& mouseY < by + btnH;
		GuiKit.pill(context, backX, by, backW, btnH, "← 返回", this.textRenderer, backHover,
				GuiKit.C_ACCENT, open, false);

		// 快捷键提示
		String hint = "Enter 完成 · Esc 返回";
		int hintW = this.textRenderer.getWidth(hint);
		int hintLeft = backX + backW + 8;
		int hintRightLimit = okX - 8;
		if (hintRightLimit - hintLeft >= hintW) {
			context.drawTextWithShadow(this.textRenderer, Text.literal(hint),
					hintLeft, by + 8, GuiKit.withAlpha(GuiKit.C_DIM, open));
		}

		if (this.toast != null) {
			context.drawTextWithShadow(this.textRenderer, Text.literal(this.toast),
					px + 20, previewY - 12, GuiKit.withAlpha(GuiKit.C_WARNING, open));
		}

		if (this.pickerFor >= 0 && this.pickerFor < this.fields.size()) {
			drawPicker(context, mouseX, mouseY, open);
		}
	}

	private void drawSmall(DrawContext context, int x, int y, int w, int h, String label, float open,
			int mouseX, int mouseY, boolean enabled) {
		boolean hov = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		GuiKit.roundedRect(context, x, y, w, h, 4,
				GuiKit.withAlpha(!enabled ? 0x301A1A28 : (hov ? GuiKit.C_ACCENT : 0xCC1A1A2E), open));
		GuiKit.roundedRectStroke(context, x, y, w, h, 4,
				GuiKit.withAlpha(enabled ? GuiKit.C_ACCENT : GuiKit.C_DIM, open), 1f);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), x + w / 2, y + 5,
				GuiKit.withAlpha(enabled ? 0xFFFFFFFF : GuiKit.C_DIM, open));
	}

	private int pickerVisibleRows() {
		List<String> players = new ArrayList<>(PermissionDetector.onlinePlayers());
		if (this.pickerFor < 0) {
			return 1;
		}
		int py = panelY();
		int ph = panelH();
		int rowY = py + 52 + this.pickerFor * ROW_H;
		int belowY = rowY + 34;
		int bottomLimit = py + ph - 70;
		int byHeight = (bottomLimit - belowY - 8) / 12;
		int aboveHeight = (rowY - (py + 50) - 8) / 12;
		int byPanel = Math.max(byHeight, aboveHeight);
		return Math.max(1, Math.min(Math.min(8, byPanel), Math.max(1, players.size())));
	}

	private int[] pickerRect() {
		List<String> players = new ArrayList<>(PermissionDetector.onlinePlayers());
		int px = panelX();
		int pw = panelW();
		int py = panelY();
		int ph = panelH();
		int rowY = py + 52 + this.pickerFor * ROW_H;
		int listX = px + 20;
		int listW = pw - 40;
		int visible = pickerVisibleRows();
		int rows = Math.max(1, Math.min(visible, players.size()));
		int listH = rows * 12 + 8;

		int belowY = rowY + 34;
		int aboveY = rowY - listH - 2;
		int topLimit = py + 50;
		int bottomLimit = py + ph - 70;
		int listY;
		if (belowY + listH <= bottomLimit) {
			listY = belowY;
		} else if (aboveY >= topLimit) {
			listY = aboveY;
		} else {
			listY = Math.max(topLimit, Math.min(belowY, bottomLimit - listH));
		}
		return new int[] { listX, listY, listW, listH };
	}

	private void drawPicker(DrawContext context, int mouseX, int mouseY, float open) {
		List<String> players = new ArrayList<>(PermissionDetector.onlinePlayers());
		int[] r = pickerRect();
		int listX = r[0];
		int listY = r[1];
		int listW = r[2];
		int listH = r[3];

		GuiKit.roundedRect(context, listX, listY, listW, listH, 4, GuiKit.withAlpha(0xCC1A1A2E, open));
		GuiKit.roundedRectStroke(context, listX, listY, listW, listH, 4, GuiKit.C_BORDER, open);

		if (players.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.literal("暂无其他在线玩家，可直接手动输入"), listX + 6, listY + 5,
					GuiKit.withAlpha(GuiKit.C_MUTED, open));
			return;
		}

		int visible = pickerVisibleRows();
		int maxStart = Math.max(0, players.size() - visible);
		this.pickerScroll = Math.max(0, Math.min(maxStart, this.pickerScroll));
		int y = listY + 4;
		for (int i = this.pickerScroll; i < Math.min(players.size(), this.pickerScroll + visible); i++) {
			boolean hov = mouseX >= listX + 2 && mouseX < listX + listW - 2 && mouseY >= y
					&& mouseY < y + 12;
			if (hov) {
				GuiKit.roundedRect(context, listX + 2, y - 1, listW - 4, 12, 2,
						GuiKit.withAlpha(0x50FFFFFF, open));
			}
			context.drawTextWithShadow(this.textRenderer, Text.literal(players.get(i)), listX + 6, y,
					GuiKit.withAlpha(0xFFE0E0FF, open));
			y += 12;
		}
		if (players.size() > visible) {
			context.drawTextWithShadow(this.textRenderer,
					Text.literal("滚轮翻页 " + (this.pickerScroll + 1) + "-"
							+ Math.min(players.size(), this.pickerScroll + visible)
							+ " / " + players.size()),
					listX + listW - 96, listY + listH + 2, GuiKit.withAlpha(GuiKit.C_DIM, open));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		int px = panelX();
		int py = panelY();
		int pw = panelW();

		// 玩家下拉列表点击
		if (this.pickerFor >= 0 && this.pickerFor < this.fields.size()) {
			List<String> players = new ArrayList<>(PermissionDetector.onlinePlayers());
			int[] r = pickerRect();
			if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
				int idx = (int) ((mouseY - r[1] - 4) / 12) + this.pickerScroll;
				if (idx >= 0 && idx < players.size()) {
					Field f = this.fields.get(this.pickerFor);
					f.value = players.get(idx);
					if (this.pickerFor < this.boxes.size()) {
						this.boxes.get(this.pickerFor).setText(f.value);
					}
				}
				this.pickerFor = -1;
				return true;
			}
			this.pickerFor = -1;
		}

		// 完成 / 返回
		int btnH = 22;
		int by = btnY();
		int okW = 108;
		int okX = px + pw - okW - 14;
		if (mouseX >= okX && mouseX < okX + okW && mouseY >= by && mouseY < by + btnH) {
			if (allFilled()) {
				finish();
			} else {
				this.toast = "还有参数没填完";
			}
			return true;
		}
		int backW = 66;
		int backX = px + 14;
		if (mouseX >= backX && mouseX < backX + backW && mouseY >= by && mouseY < by + btnH) {
			if (this.client != null) {
				this.client.setScreen(this.parent);
			}
			return true;
		}

		// +/− 与下拉
		for (int i = 0; i < this.fields.size(); i++) {
			Field f = this.fields.get(i);
			int boxY = py + 52 + i * ROW_H + 13;
			if (f.type == PermissionDetector.ArgType.NUMBER) {
				int bx = px + pw - 46;
				if (mouseX >= bx && mouseX < bx + 20 && mouseY >= boxY && mouseY < boxY + 18) {
					f.number = Math.max(f.min, f.number - 1);
					return true;
				}
				if (mouseX >= bx + 24 && mouseX < bx + 44 && mouseY >= boxY && mouseY < boxY + 18) {
					f.number = Math.min(f.max, f.number + 1);
					return true;
				}
			} else if (f.type == PermissionDetector.ArgType.PLAYER) {
				int bx = px + pw - 44;
				if (mouseX >= bx && mouseX < bx + 22 && mouseY >= boxY && mouseY < boxY + 18) {
					this.pickerFor = this.pickerFor == i ? -1 : i;
					this.pickerScroll = 0;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
			double verticalAmount) {
		if (this.pickerFor >= 0) {
			List<String> players = new ArrayList<>(PermissionDetector.onlinePlayers());
			int maxStart = Math.max(0, players.size() - pickerVisibleRows());
			this.pickerScroll = Math.max(0, Math.min(maxStart,
					this.pickerScroll - (int) Math.signum(verticalAmount)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 335) {
			if (allFilled()) {
				finish();
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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

	private void drawTriangleDown(DrawContext context, int cx, int cy, int size, int color) {
		for (int i = 0; i < size; i++) {
			int half = i;
			context.fill(cx - half, cy - size / 2 + i, cx + half + 1, cy - size / 2 + i + 1, color);
		}
	}

	private static float easeOut(float t) {
		float k = Math.min(1f, Math.max(0f, t));
		float inv = 1f - k;
		return 1f - inv * inv * inv;
	}
}
