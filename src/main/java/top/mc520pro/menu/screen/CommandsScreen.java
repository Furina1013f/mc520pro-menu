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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import top.mc520pro.menu.ServerDetector;

/**
 * 服务器指令清单页（自适应 + 分页 + 淡入动画）。
 * 数据来自服务器下发的 DeclareCommands 指令树，可以据此反推服务器插件。
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
			// 拿不到指令树时保持空列表
		}
		Collections.sort(this.commands);
	}

	private int pageCount() {
		return Math.max(1, (this.commands.size() + this.perPage - 1) / this.perPage);
	}

	@Override
	protected void init() {
		int W = this.width;
		int H = this.height;
		this.panelW = Math.max(300, Math.min(540, W - OUTER_PAD * 2));
		this.cols = this.panelW >= 500 ? 6 : this.panelW >= 420 ? 5 : this.panelW >= 340 ? 4 : 3;
		int innerW = this.panelW - 28;
		this.chipW = (innerW - (this.cols - 1) * GAP_X) / this.cols;
		this.chipH = 18;

		int headerH = 34;
		int infoH = 16;
		int footerH = 30;
		int availH = H - OUTER_PAD * 2 - headerH - infoH - footerH - 10;
		this.rows = Math.max(3, availH / (this.chipH + GAP_Y));
		this.perPage = this.cols * this.rows;

		this.panelH = headerH + infoH + this.rows * (this.chipH + GAP_Y) + footerH + 6;
		this.panelX = (W - this.panelW) / 2;
		this.panelY = Math.max(4, (H - this.panelH) / 2);
		this.gridLeft = this.panelX + 14;
		this.gridTop = this.panelY + headerH + infoH;

		if (this.page > pageCount() - 1) {
			this.page = Math.max(0, pageCount() - 1);
		}

		this.pills.clear();
		int pillH = 22;
		int count = 5;
		int pillW = Math.max(48, (this.panelW - 24 - (count - 1) * 6) / count);
		int total = pillW * count + (count - 1) * 6;
		int left = this.panelX + (this.panelW - total) / 2;
		int y = this.panelY + this.panelH - pillH - 6;
		this.pills.add(new Pill(left, y, pillW, pillH, "◀ 上页", this::prevPage));
		this.pills.add(new Pill(left + (pillW + 6), y, pillW, pillH, "下页 ▶", this::nextPage));
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
		int from = this.page * this.perPage;
		int to = Math.min(this.commands.size(), from + this.perPage);
		if (from >= to) {
			return Collections.emptyList();
		}
		return this.commands.subList(from, to);
	}

	private String joined(int from, int to) {
		List<String> part = this.commands.subList(from, Math.min(this.commands.size(), to));
		return "/" + String.join("\n/", part);
	}

	private void copyPage() {
		int from = this.page * this.perPage;
		int to = Math.min(this.commands.size(), from + this.perPage);
		if (from < to) {
			MinecraftClient.getInstance().keyboard.setClipboard(joined(from, to));
		}
	}

	private void copyAll() {
		if (!this.commands.isEmpty()) {
			MinecraftClient.getInstance().keyboard.setClipboard(joined(0, this.commands.size()));
		}
	}

	private void back() {
		MinecraftClient client = MinecraftClient.getInstance();
		client.setScreen(this.parent != null ? this.parent : new MainMenuScreen());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		long now = System.currentTimeMillis();
		float dt = Math.min(0.1f, (now - this.lastFrameMs) / 1000f);
		this.lastFrameMs = now;
		float open = easeOut((now - this.createdAt) / 200f);
		float timeSec = now / 1000f;

		context.fill(this.panelX - 2, this.panelY - 2, this.panelX + this.panelW + 2,
				this.panelY + this.panelH + 2, withAlpha(0xFF3A3466, open));
		context.fill(this.panelX, this.panelY, this.panelX + this.panelW,
				this.panelY + this.panelH, withAlpha(0xF4110F20, open));

		float phase = (timeSec * 0.12f) % 1f;
		int h1 = mix(0xFF3E5C8A, 0xFF6C46C8, phase);
		int h2 = mix(0xFF1C5F7C, 0xFF3E5C8A, phase);
		context.fillGradient(this.panelX, this.panelY, this.panelX + this.panelW,
				this.panelY + 34, withAlpha(h1, open), withAlpha(h2, open));
		context.fill(this.panelX, this.panelY + 34, this.panelX + this.panelW,
				this.panelY + 35, withAlpha(0xFFB6A4FF, open));

		context.drawTextWithShadow(this.textRenderer, Text.literal("服务器指令清单"),
				this.panelX + 14, this.panelY + 6, withAlpha(0xFFFFFFFF, open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(ServerDetector.shortInfo()),
				this.panelX + 14, this.panelY + 19, withAlpha(0xFFCFE4FF, open));

		String info = this.onServer
				? "共 " + this.commands.size() + " 条指令 · 第 " + (this.page + 1) + "/" + pageCount()
					+ " 页 · 点击指令复制到剪贴板"
				: "未连接 " + ServerDetector.SERVER_HOST + "，无法读取指令";
		context.drawTextWithShadow(this.textRenderer, Text.literal(fit(info, this.panelW - 28)),
				this.panelX + 14, this.panelY + 38, withAlpha(0xFFB9AEDC, open));

		List<String> pageList = pageCommands();
		if (pageList.isEmpty()) {
			// 空状态：给出明确说明，而不是留一大片空白
			int cx = this.panelX + this.panelW / 2;
			int cy = this.gridTop + 8;
			String line1 = this.onServer ? "服务器暂未下发任何指令" : "未连接到服务器";
			String line2 = this.onServer ? "请稍后再试" : "进入 " + ServerDetector.SERVER_HOST + " 后即可查看并复制";
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line1), cx, cy,
					withAlpha(0xFF9C93C4, open));
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line2), cx, cy + 13,
					withAlpha(0xFF6E6890, open));
			context.fill(cx - 60, cy + 26, cx + 60, cy + 27, withAlpha(0xFF3A3560, open));
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("菜单里的「分类指令」不依赖此页，照常可用"), cx, cy + 34,
					withAlpha(0xFF6E6890, open));
		}
		for (int i = 0; i < pageList.size(); i++) {
			int col = i % this.cols;
			int row = i / this.cols;
			int x = this.gridLeft + col * (this.chipW + GAP_X);
			int y = this.gridTop + row * (this.chipH + GAP_Y);
			String cmd = pageList.get(i);
			boolean hovered = mouseX >= x && mouseX < x + this.chipW
					&& mouseY >= y && mouseY < y + this.chipH;
			context.fill(x, y, x + this.chipW, y + this.chipH,
					withAlpha(hovered ? 0xE6624FA0 : 0xD0282444, open));
			int border = hovered ? 0xFFFFFFFF : 0xFF5A4E8C;
			context.fill(x, y, x + this.chipW, y + 1, withAlpha(border, open));
			context.fill(x, y + this.chipH - 1, x + this.chipW, y + this.chipH, withAlpha(border, open));
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal(fit("/" + cmd, this.chipW - 6)), x + this.chipW / 2, y + 5,
					withAlpha(hovered ? 0xFFFFFF : 0xE6DDFF, open));
		}

		for (Pill pill : this.pills) {
			boolean disabled = this.commands.isEmpty() && !pill.label.equals("返回");
			boolean hovered = !disabled && mouseX >= pill.x && mouseX < pill.x + pill.w
					&& mouseY >= pill.y && mouseY < pill.y + pill.h;
			float target = hovered ? 1f : 0f;
			pill.hover += (target - pill.hover) * Math.min(1f, dt * 14f);
			int bg = disabled ? 0xB0201C33 : mix(0xD02A2542, 0xE6624FA0, pill.hover);
			context.fill(pill.x, pill.y, pill.x + pill.w, pill.y + pill.h, withAlpha(bg, open));
			int border = disabled ? 0xFF3E3A5C : (hovered ? 0xFFFFFFFF : 0xFF54497F);
			context.fill(pill.x, pill.y, pill.x + pill.w, pill.y + 1, withAlpha(border, open));
			context.fill(pill.x, pill.y + pill.h - 1, pill.x + pill.w, pill.y + pill.h, withAlpha(border, open));
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal(fit(pill.label, pill.w - 6)), pill.x + pill.w / 2, pill.y + 7,
					withAlpha(disabled ? 0xFF6E6890 : 0xFFFFFF, open));
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

	// ---------- 小工具 ----------

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

	/** 统一套 alpha；输入色若未带 alpha 通道（如 0xFFFFFF）按不透明处理。 */
	private static int withAlpha(int color, float alpha) {
		int baseA = (color >>> 24) & 0xFF;
		if (baseA == 0) {
			baseA = 0xFF;
		}
		int a = Math.round(baseA * clamp(alpha));
		return (clampInt(a) << 24) | (color & 0xFFFFFF);
	}

	private static float clamp(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static int clampInt(int v) {
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	private static int mix(int c1, int c2, float t) {
		float k = clamp(t);
		int a = Math.round(((c1 >>> 24) & 0xFF) * (1 - k) + ((c2 >>> 24) & 0xFF) * k);
		int r = Math.round(((c1 >> 16) & 0xFF) * (1 - k) + ((c2 >> 16) & 0xFF) * k);
		int g = Math.round(((c1 >> 8) & 0xFF) * (1 - k) + ((c2 >> 8) & 0xFF) * k);
		int b = Math.round((c1 & 0xFF) * (1 - k) + (c2 & 0xFF) * k);
		return (clampInt(a) << 24) | (clampInt(r) << 16) | (clampInt(g) << 8) | clampInt(b);
	}

	private static float easeOut(float t) {
		float k = clamp(t);
		float inv = 1f - k;
		return 1f - inv * inv * inv;
	}
}
