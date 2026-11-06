package top.mc520pro.menu.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import top.mc520pro.menu.ServerDetector;

/**
 * 服务器指令清单页（现代风格）：
 * 从客户端 DeclareCommands 获取服务器下发的指令列表，分页展示，点击复制到剪贴板。
 */
public class CommandsScreen extends Screen {
	private static final int OUTER_PAD = 8;
	private static final int GAP_X = 5;
	private static final int GAP_Y = 5;

	private final Screen parent;
	private final boolean onServer = ServerDetector.isOnServer();
	private final long createdAt = System.currentTimeMillis();
	private final List<String> commands = new ArrayList<>();
	private final List<Pill> pills = new ArrayList<>();

	private int page = 0;
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int gridLeft;
	private int gridTop;
	private int cols = 5;
	private int rows = 6;
	private int chipW = 84;
	private int chipH = 18;
	private int perPage = 30;
	private long lastFrameMs = createdAt;
	private float open;
	private float timeSec;
	private net.minecraft.client.gui.widget.TextFieldWidget searchField;
	private String searchText = "";

	private static final class Pill {
		final int x;
		final int y;
		final int w;
		final int h;
		final String label;
		final Runnable action;
		float hover;

		Pill(int x, int y, int w, int h, String label, Runnable action) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.label = label;
			this.action = action;
		}
	}

	public CommandsScreen(Screen parent) {
		super(Text.literal("服务器指令清单"));
		this.parent = parent;
		this.loadCommands();
	}

	private void loadCommands() {
		this.commands.clear();
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler handler = client.getNetworkHandler();
		if (handler == null || !this.onServer) {
			return;
		}
		try {
			for (CommandNode<ClientCommandSource> node : handler.getCommandDispatcher().getRoot().getChildren()) {
				String name = node.getName();
				if (name != null && !name.isEmpty()) {
					this.commands.add(name);
				}
			}
		} catch (Exception ignored) {
		}
		Collections.sort(this.commands);
	}

	private List<String> filteredCommands() {
		if (this.searchText.isEmpty()) {
			return this.commands;
		}
		List<String> out = new ArrayList<>();
		for (String cmd : this.commands) {
			if (cmd.toLowerCase(java.util.Locale.ROOT).contains(this.searchText)) {
				out.add(cmd);
			}
		}
		return out;
	}

	private int pageCount() {
		int size = filteredCommands().size();
		return Math.max(1, (size + this.perPage - 1) / this.perPage);
	}

	@Override
	protected void init() {
		this.clearChildren();
		int W = this.width;
		int H = this.height;
		this.panelW = Math.max(300, Math.min(540, W - OUTER_PAD * 2));
		this.cols = this.panelW >= 500 ? 6 : this.panelW >= 420 ? 5 : this.panelW >= 340 ? 4 : 3;
		int innerW = this.panelW - 28;
		this.chipW = (innerW - (this.cols - 1) * GAP_X) / this.cols;
		this.chipH = 18;

		int headerH = 34;
		int infoH = 16;
		int searchH = 22;
		int footerH = 30;
		int availH = H - OUTER_PAD * 2 - headerH - infoH - searchH - footerH - 12;
		this.rows = Math.max(3, availH / (this.chipH + GAP_Y));
		this.perPage = this.cols * this.rows;

		this.panelH = headerH + infoH + searchH + this.rows * (this.chipH + GAP_Y) + footerH + 6;
		this.panelX = (W - this.panelW) / 2;
		this.panelY = Math.max(4, (H - this.panelH) / 2);
		this.gridLeft = this.panelX + 14;
		this.gridTop = this.panelY + headerH + infoH + searchH;

		if (this.page > pageCount() - 1) {
			this.page = Math.max(0, pageCount() - 1);
		}

		// 搜索框（纹理背景 + 输入框）
		this.searchField = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer,
				this.panelX + 34, this.panelY + headerH + infoH + 2, this.panelW - 48, 18,
				Text.literal("搜索指令"));
		this.searchField.setMaxLength(40);
		this.searchField.setText(this.searchText);
		this.searchField.setChangedListener(text -> {
			this.searchText = text.toLowerCase(java.util.Locale.ROOT).trim();
			this.page = 0;
		});
		this.addDrawableChild(this.searchField);

		this.pills.clear();
		int pillH = 22;
		int count = 5;
		int pillW = Math.max(48, (this.panelW - 24 - (count - 1) * 6) / count);
		int total = pillW * count + (count - 1) * 6;
		int left = this.panelX + (this.panelW - total) / 2;
		int y = this.panelY + this.panelH - pillH - 6;
		this.pills.add(new Pill(left, y, pillW, pillH, "上页", this::prevPage));
		this.pills.add(new Pill(left + (pillW + 6), y, pillW, pillH, "下页", this::nextPage));
		this.pills.add(new Pill(left + (pillW + 6) * 2, y, pillW, pillH, "复制本页", this::copyPage));
		this.pills.add(new Pill(left + (pillW + 6) * 3, y, pillW, pillH, "复制全部", this::copyAll));
		this.pills.add(new Pill(left + (pillW + 6) * 4, y, pillW, pillH, "返回", this::back));
	}

	private void prevPage() {
		if (this.page > 0) {
			this.page--;
		}
	}

	private void nextPage() {
		if (this.page < pageCount() - 1) {
			this.page++;
		}
	}

	private List<String> pageCommands() {
		List<String> filtered = filteredCommands();
		int from = this.page * this.perPage;
		int to = Math.min(filtered.size(), from + this.perPage);
		if (from >= to) {
			return Collections.emptyList();
		}
		return filtered.subList(from, to);
	}

	private String joined(int from, int to) {
		List<String> filtered = filteredCommands();
		List<String> part = filtered.subList(from, Math.min(filtered.size(), to));
		return "/" + String.join("\n/", part);
	}

	private void copyPage() {
		List<String> filtered = filteredCommands();
		int from = this.page * this.perPage;
		int to = Math.min(filtered.size(), from + this.perPage);
		if (from < to) {
			MinecraftClient.getInstance().keyboard.setClipboard(joined(from, to));
		}
	}

	private void copyAll() {
		List<String> filtered = filteredCommands();
		if (!filtered.isEmpty()) {
			MinecraftClient.getInstance().keyboard.setClipboard(joined(0, filtered.size()));
		}
	}

	private void back() {
		MinecraftClient client = MinecraftClient.getInstance();
		client.setScreen(this.parent != null ? this.parent : new MainMenuScreen());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		long now = System.currentTimeMillis();
		float dt = Math.min(0.1f, (now - this.lastFrameMs) / 1000f);
		this.lastFrameMs = now;
		this.open = easeOut((now - this.createdAt) / 200f);
		this.timeSec = now / 1000f;

		// 现代玻璃面板
		GuiKit.glassPanel(context, this.panelX, this.panelY, this.panelW, this.panelH, this.open);

		// 头部渐变
		float phase = (this.timeSec * 0.12f) % 1f;
		int h1 = GuiKit.mix(GuiKit.C_ACCENT, GuiKit.C_ACCENT2, phase);
		int h2 = GuiKit.mix(GuiKit.C_ACCENT2, GuiKit.C_ACCENT, phase);
		context.fillGradient(this.panelX + 2, this.panelY + 2, this.panelX + this.panelW - 2, this.panelY + 32,
				GuiKit.withAlpha(h1, this.open), GuiKit.withAlpha(h2, this.open));
		context.fillGradient(this.panelX + 2, this.panelY + 30, this.panelX + this.panelW - 2, this.panelY + 36,
				GuiKit.withAlpha(h2, this.open * 0.5f), GuiKit.withAlpha(0xFF1A1A2E, this.open * 0.8f));

		context.drawTextWithShadow(this.textRenderer, Text.literal("服务器指令清单"),
				this.panelX + 14, this.panelY + 6, GuiKit.withAlpha(0xFFFFFFFF, this.open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(ServerDetector.shortInfo()),
				this.panelX + 14, this.panelY + 19, GuiKit.withAlpha(0xFFE0E0FF, this.open));

		int shown = filteredCommands().size();
		String info = this.onServer
				? (this.searchText.isEmpty()
					? "共 " + this.commands.size() + " 条指令 · 第 " + (this.page + 1) + "/" + pageCount()
						+ " 页 · 点击指令复制到剪贴板"
					: "匹配 " + shown + "/" + this.commands.size() + " 条 · 第 " + (this.page + 1) + "/"
						+ pageCount() + " 页")
				: "未连接 " + ServerDetector.SERVER_HOST + "，无法读取指令";
		context.drawTextWithShadow(this.textRenderer, Text.literal(fit(info, this.panelW - 28)),
				this.panelX + 14, this.panelY + 38, GuiKit.withAlpha(GuiKit.C_MUTED, this.open));

		// 搜索框背景（纹理，画在输入框之下）
		TextureKit.draw(context, TextureKit.SEARCH_BOX, this.panelX + 14, this.panelY + 52, this.panelW - 28, 18);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta); // 输入框画在面板之上
		float open = this.open;

		List<String> pageList = pageCommands();
		if (pageList.isEmpty()) {
			// 空状态
			int cx = this.panelX + this.panelW / 2;
			int cy = this.gridTop + 8;
			String line1 = this.onServer ? "服务器暂未下发任何指令" : "未连接到服务器";
			String line2 = this.onServer ? "请稍后再试" : "进入 " + ServerDetector.SERVER_HOST + " 后即可查看并复制";
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line1), cx, cy,
					GuiKit.withAlpha(GuiKit.C_MUTED, open));
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line2), cx, cy + 13,
					GuiKit.withAlpha(GuiKit.C_DIM, open));
			context.fill(cx - 60, cy + 26, cx + 60, cy + 27, GuiKit.withAlpha(GuiKit.C_BORDER, open));
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("菜单里的「分类指令」不依赖此页，照常可用"), cx, cy + 34,
					GuiKit.withAlpha(GuiKit.C_DIM, open));
		}
		for (int i = 0; i < pageList.size(); i++) {
			int col = i % this.cols;
			int row = i / this.cols;
			int x = this.gridLeft + col * (this.chipW + GAP_X);
			int y = this.gridTop + row * (this.chipH + GAP_Y);
			String cmd = pageList.get(i);
			boolean hovered = mouseX >= x && mouseX < x + this.chipW
					&& mouseY >= y && mouseY < y + this.chipH;
			// 纹理绘制 chip
			TextureKit.draw(context, hovered ? TextureKit.CARD_HOVER : TextureKit.CARD_NORMAL,
					x, y, this.chipW, this.chipH);
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal(fit("/" + cmd, this.chipW - 6)), x + this.chipW / 2, y + 5,
					GuiKit.withAlpha(hovered ? 0xFFFFFF : GuiKit.C_MUTED, open));
		}

		for (Pill pill : this.pills) {
			boolean disabled = filteredCommands().isEmpty() && !pill.label.equals("返回");
			boolean hovered = !disabled && mouseX >= pill.x && mouseX < pill.x + pill.w
					&& mouseY >= pill.y && mouseY < pill.y + pill.h;
			float target = hovered ? 1f : 0f;
			pill.hover += (target - pill.hover) * Math.min(1f, delta * 14f);
			int pillColor = GuiKit.C_ACCENT;
			if (pill.label.equals("返回")) {
				pillColor = GuiKit.C_PURPLE;
			} else if (pill.label.contains("复制")) {
				pillColor = GuiKit.C_ACCENT2;
			}
			GuiKit.pill(context, pill.x, pill.y, pill.w, pill.h, fit(pill.label, pill.w - 6),
					this.textRenderer, hovered, pillColor, open, disabled);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			List<String> pageList = pageCommands();
			for (int i = 0; i < pageList.size(); i++) {
				int col = i % this.cols;
				int row = i / this.cols;
				int x = this.gridLeft + col * (this.chipW + GAP_X);
				int y = this.gridTop + row * (this.chipH + GAP_Y);
				if (mouseX >= x && mouseX < x + this.chipW
						&& mouseY >= y && mouseY < y + this.chipH) {
					MinecraftClient.getInstance().keyboard.setClipboard("/" + pageList.get(i) + " ");
					return true;
				}
			}
			for (Pill pill : this.pills) {
				if (mouseX >= pill.x && mouseX < pill.x + pill.w
						&& mouseY >= pill.y && mouseY < pill.y + pill.h) {
					pill.action.run();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
			prevPage();
			return true;
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
			nextPage();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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
