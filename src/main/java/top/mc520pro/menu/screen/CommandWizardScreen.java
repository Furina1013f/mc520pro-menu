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
 * 二级向导页（v8）：带参数的指令不再让玩家自己复制粘贴，
 * 而是进入本页按字段填写 —— 玩家名可点选在线列表，数字可 +/- 调节，
 * 底部实时预览最终指令，点「完成并发送」或按 Enter 直接执行。
 *
 * 绘制顺序：面板底色放在 renderBackground（先画），输入框等控件随后由 super.render 绘制，
 * 弹层与预览在 render 末尾绘制，保证永远不会被面板盖住。
 */
public class CommandWizardScreen extends Screen {
	/** 单个参数槽位。 */
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

	/** 从指令模板解析参数：`tpa <玩家名>` → 1 个玩家字段。 */
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
					// 保留默认范围
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

	// ---------- 尺寸 ----------

	private int panelW() {
		return Math.max(250, Math.min(400, this.width - 40));
	}

	private int panelH() {
		// 紧凑：标题30 + 标签/输入框区(42n) + 预览20 + 按钮22 + 间隙
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

	// ---------- 初始化 ----------

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

	// ---------- 指令组装 ----------

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

	// ---------- 绘制：面板底（先画） ----------

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

		context.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, withAlpha(0xFF3A3466, open));
		context.fill(px, py, px + pw, py + ph, withAlpha(0xF4110F20, open));
		context.fillGradient(px, py, px + pw, py + 30, withAlpha(0xFF6C46C8, open),
				withAlpha(0xFF2E6D9C, open));
		context.fill(px, py + 30, px + pw, py + 31, withAlpha(0xFFB6A4FF, open));

		context.drawTextWithShadow(this.textRenderer,
				Text.literal(this.entry.name()).styled(s -> s.withBold(true)), px + 12, py + 7,
				withAlpha(0xFFFFFFFF, open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(fit(this.entry.usage(), pw - 24)),
				px + 12, py + 19, withAlpha(0xFFDCD2FF, open));

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
					Text.literal(f.label + "  ·  " + hint), px + 20, rowY, withAlpha(0xFFB9AEDC, open));
		}
	}

	// ---------- 绘制：控件之上（后画） ----------

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta); // 背景 + 输入框

		float open = this.openAlpha;
		int px = panelX();
		int py = panelY();
		int pw = panelW();
		int ph = panelH();

		// 数字 / 玩家 附加按钮（画在输入框之后，保证可见）
		for (int i = 0; i < this.fields.size(); i++) {
			Field f = this.fields.get(i);
			int rowY = py + 52 + i * ROW_H;
			int boxY = rowY + 13;
			if (f.type == PermissionDetector.ArgType.NUMBER) {
				// 数值画在输入框内部右端（输入框右端由 init 预留了 52px 空间）
				int textRight = px + pw - 56;
				String num = String.valueOf(f.number);
				int numW = this.textRenderer.getWidth(num);
				context.drawTextWithShadow(this.textRenderer, Text.literal(num),
						textRight - numW, boxY + 5, withAlpha(0xFF9CF5C4, open));
				// −/+ 按钮放在面板右端，两者之间留 4px 间隙
				int bx = px + pw - 46;
				drawSmall(context, bx, boxY, 20, 18, "−", open, mouseX, mouseY, f.number > f.min);
				drawSmall(context, bx + 24, boxY, 20, 18, "+", open, mouseX, mouseY, f.number < f.max);
			} else if (f.type == PermissionDetector.ArgType.PLAYER) {
				int bx = px + pw - 44;
				boolean hov = mouseX >= bx && mouseX < bx + 22 && mouseY >= boxY && mouseY < boxY + 18;
				context.fill(bx, boxY, bx + 22, boxY + 18,
						withAlpha(hov ? 0xE6614FA6 : 0xD02A2542, open));
				outline(context, bx, boxY, 22, 18,
						withAlpha(hov ? 0xFFFFFFFF : 0xFF54497F, open));
				drawTriangleDown(context, bx + 11, boxY + 9, 5, withAlpha(0xFFFFFFFF, open));
			}
		}

		// 预览
		String preview = "/" + buildCommand();
		int previewY = py + ph - 62;
		context.fill(px + 14, previewY, px + pw - 14, previewY + 20, withAlpha(0xC018152B, open));
		outline(context, px + 14, previewY, pw - 28, 20, withAlpha(0x50372F62, open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(fit("→ " + preview, pw - 36)),
				px + 20, previewY + 6,
				withAlpha(allFilled() ? 0xFF97F5B4 : 0xFF8C86A8, open));

		// 完成按钮
		int btnH = 22;
		int by = btnY();
		int okW = 108;
		int okX = px + pw - okW - 14;
		boolean canDo = allFilled();
		boolean okHover = mouseX >= okX && mouseX < okX + okW && mouseY >= by && mouseY < by + btnH;
		context.fill(okX, by, okX + okW, by + btnH,
				withAlpha(!canDo ? 0xB0243A30 : (okHover ? 0xE646B07C : 0xD02A7A54), open));
		outline(context, okX, by, okW, btnH,
				withAlpha(canDo ? (okHover ? 0xFFFFFFFF : 0xFF5FB98D) : 0xFF3A4A44, open));
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal(canDo ? "完成并发送" : "请填写参数"), okX + okW / 2, by + 7,
				withAlpha(canDo ? 0xFFFFFFFF : 0xFF7F8F88, open));

		// 返回按钮
		int backW = 66;
		int backX = px + 14;
		boolean backHover = mouseX >= backX && mouseX < backX + backW && mouseY >= by
				&& mouseY < by + btnH;
		context.fill(backX, by, backX + backW, by + btnH,
				withAlpha(backHover ? 0xE6614FA6 : 0xD02A2542, open));
		outline(context, backX, by, backW, btnH,
				withAlpha(backHover ? 0xFFFFFFFF : 0xFF54497F, open));
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("← 返回"),
				backX + backW / 2, by + 7, withAlpha(0xFFFFFFFF, open));

		// 快捷键提示：只在「返回」与「完成」之间的空隙够宽时才画，避免压住按钮
		String hint = "Enter 完成 · Esc 返回";
		int hintW = this.textRenderer.getWidth(hint);
		int hintLeft = backX + backW + 8;
		int hintRightLimit = okX - 8;
		if (hintRightLimit - hintLeft >= hintW) {
			context.drawTextWithShadow(this.textRenderer, Text.literal(hint),
					hintLeft, by + 8, withAlpha(0xFF6E6890, open));
		}

		if (this.toast != null) {
			context.drawTextWithShadow(this.textRenderer, Text.literal(this.toast),
					px + 20, previewY - 12, withAlpha(0xFFFF8F9A, open));
		}

		if (this.pickerFor >= 0 && this.pickerFor < this.fields.size()) {
			drawPicker(context, mouseX, mouseY, open);
		}
	}

	private void drawSmall(DrawContext context, int x, int y, int w, int h, String label, float open,
			int mouseX, int mouseY, boolean enabled) {
		boolean hov = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		context.fill(x, y, x + w, y + h,
				withAlpha(!enabled ? 0xB0201C33 : (hov ? 0xE6614FA6 : 0xD02A2542), open));
		outline(context, x, y, w, h,
				withAlpha(enabled ? (hov ? 0xFFFFFFFF : 0xFF54497F) : 0xFF332F4A, open));
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), x + w / 2, y + 5,
				withAlpha(enabled ? 0xFFFFFFFF : 0xFF6E6890, open));
	}

	/** 下拉列表可见行数：受面板剩余高度限制，最多 8 行。 */
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
		int topLimit = py + 50; // 不压住参数标签行
		int bottomLimit = py + ph - 70; // 不压住预览/按钮

		int listY;
		if (belowY + listH <= bottomLimit) {
			listY = belowY;
		} else if (aboveY >= topLimit) {
			listY = aboveY;
		} else {
			// 上下都放不下：夹在面板内
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

		context.fill(listX, listY, listX + listW, listY + listH, withAlpha(0xFC100F1C, open));
		outline(context, listX, listY, listW, listH, withAlpha(0xFF8F7FD9, open));

		if (players.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.literal("暂无其他在线玩家，可直接手动输入"), listX + 6, listY + 5,
					withAlpha(0xFF9C93C4, open));
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
				context.fill(listX + 2, y - 1, listX + listW - 2, y + 11,
						withAlpha(0x50FFFFFF, open));
			}
			context.drawTextWithShadow(this.textRenderer, Text.literal(players.get(i)), listX + 6, y,
					withAlpha(0xFFE4DEFF, open));
			y += 12;
		}
		if (players.size() > visible) {
			context.drawTextWithShadow(this.textRenderer,
					Text.literal("滚轮翻页 " + (this.pickerScroll + 1) + "-"
							+ Math.min(players.size(), this.pickerScroll + visible)
							+ " / " + players.size()),
					listX + listW - 96, listY + listH + 2, withAlpha(0xFF8C86A8, open));
		}
	}

	// ---------- 交互 ----------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		int px = panelX();
		int py = panelY();
		int pw = panelW();
		int ph = panelH();

		// 玩家弹层
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
		if (keyCode == 257 || keyCode == 335) { // Enter / NumpadEnter
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

	/** 向下的实心三角（像素绘制，避免字体缺字）。 */
	private void drawTriangleDown(DrawContext context, int cx, int cy, int size, int color) {
		for (int i = 0; i < size; i++) {
			int half = i;
			context.fill(cx - half, cy - size / 2 + i, cx + half + 1, cy - size / 2 + i + 1, color);
		}
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
		int a = Math.round(baseA * Math.min(1f, Math.max(0f, alpha)));
		return (Math.min(255, Math.max(0, a)) << 24) | (color & 0xFFFFFF);
	}

	private static float easeOut(float t) {
		float k = Math.min(1f, Math.max(0f, t));
		float inv = 1f - k;
		return 1f - inv * inv * inv;
	}
}