package top.mc520pro.menu.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import top.mc520pro.menu.PermissionDetector;
import top.mc520pro.menu.ServerDetector;
import top.mc520pro.menu.UpdateChecker;
import top.mc520pro.menu.data.CommandCatalog;

/**
 * 520Pro 服务器内置菜单（图形版 v6）。
 * 分类页签 + 指令卡片（每条带用途说明），内容来自《有什么指令》文档整理。
 * 不在 mc.520pro.top 时，需要服务器执行的卡片全部锁定。
 */
public class MainMenuScreen extends Screen {
	private static final int OUTER_PAD = 8;
	private static final int GAP = 6;
	private static final int CARD_PAD_X = 12;
	private static final int TAB_W = 36;
	private static final int TAB_H = 30;

	private final boolean onServer = ServerDetector.isOnServer();
	private final long createdAt = System.currentTimeMillis();
	private final List<CommandCatalog.Category> categories = CommandCatalog.CATEGORIES;

	private int tab = 0;
	private final List<Card> cards = new ArrayList<>();
	private final List<TabChip> chips = new ArrayList<>();
	private final List<Pill> pills = new ArrayList<>();

	// ---- 切换动画状态 ----
	/** 正在从 prevTab 滑向 tab；-1 表示没有过渡。 */
	private int prevTab = -1;
	private float tabT = 1f;
	/** 卡片入场进度（每个卡片有错峰延迟）。 */
	private float cardT = 1f;
	private long lastFrameMs = createdAt;
	private float timeSec = 0f;

	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int headerH;
	private int tabsY;
	private int descY;
	private int gridY;
	private int visibleGridH;
	private int naturalGridH;
	private int scrollY;
	private boolean scrollable;
	private boolean compact;

	private String toast = null;
	private long toastUntil = 0;

	private static final class Card {
		final int x;
		final int y;
		final int w;
		final int h;
		final CommandCatalog.Entry entry;
		final List<String> usageLines;

		Card(int x, int y, int w, int h, CommandCatalog.Entry entry, List<String> usageLines) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.entry = entry;
			this.usageLines = usageLines;
		}
	}

	private static final class TabChip {
		final int x;
		final int y;
		final int w;
		final int h;
		final int index;

		TabChip(int x, int y, int w, int h, int index) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.index = index;
		}
	}

	private static final class Pill {
		final int x;
		final int y;
		final int w;
		final int h;
		final String label;
		final ItemStack icon;
		final Runnable action;

		Pill(int x, int y, int w, int h, String label, ItemStack icon, Runnable action) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.label = label;
			this.icon = icon;
			this.action = action;
		}
	}

	public MainMenuScreen() {
		super(Text.literal("MC.520PRO.TOP 服务器菜单"));
	}

	/** 调试/自检用：切换分类页签。 */
	public void debugSetTab(int index) {
		if (index >= 0 && index < this.categories.size() && index != this.tab) {
			switchTab(index);
		}
	}

	/** 调试/自检用：滚动到底部。 */
	public void debugScrollBottom() {
		this.scrollY = Math.max(0, this.naturalGridH - this.visibleGridH);
	}

	/**
	 * 切换页签（带过渡动画）：旧页向左滑出淡出，新页从右侧滑入淡入。
	 */
	private void switchTab(int index) {
		if (index < 0 || index >= this.categories.size() || index == this.tab) {
			return;
		}
		this.prevTab = this.tab;
		this.tab = index;
		this.scrollY = 0;
		this.tabT = 0f;
		this.cardT = 0f;
		rebuild();
	}

	@Override
	protected void init() {
		rebuild();
	}

	private void rebuild() {
		this.cards.clear();
		this.chips.clear();
		this.pills.clear();

		int W = this.width;
		int H = this.height;
		this.compact = H < 320;
		this.headerH = this.compact ? 30 : 38;
		this.panelW = Math.max(240, Math.min(460, W - OUTER_PAD * 2));
		CommandCatalog.Category cat = this.categories.get(this.tab);
		int entryCount = cat.entries().size();
		// 列数按条目数自适应：条目少时用 3 列，一屏放完不必滚动
		int cols;
		if (entryCount <= 6) {
			cols = this.panelW >= 330 ? 3 : (this.panelW >= 230 ? 2 : 1);
		} else {
			cols = this.panelW >= 380 ? 2 : 1;
		}
		int cardW = (this.panelW - CARD_PAD_X * 2 - (cols - 1) * GAP) / cols;

		int descH = 12;
		int footerH = this.compact ? 26 : 30;
		int cardH = this.compact ? 34 : 44;
		int rows = (entryCount + cols - 1) / cols;
		this.naturalGridH = rows * cardH + Math.max(0, rows - 1) * GAP;

		// 固定区：横幅 + 页签 + 说明行 + 底部按钮
		int fixed = this.headerH + 4 + TAB_H + 3 + descH + 4 + footerH + 12;
		int maxH = H - OUTER_PAD * 2;

		if (fixed + this.naturalGridH <= maxH) {
			// 全部放得下
			this.visibleGridH = this.naturalGridH;
		} else {
			// 放不下：算出能完整显示几行（逐行累加，末行不需要 GAP）
			this.visibleGridH = 0;
			int used = 0;
			for (int r = 0; r < rows; r++) {
				int need = (r == 0 ? 0 : GAP) + cardH;
				if (used + need > maxH - fixed) {
					break;
				}
				used += need;
				this.visibleGridH = used;
			}
			if (this.visibleGridH <= 0) {
				// 极端小窗：压缩卡片高度，至少显示一行
				cardH = Math.max(26, maxH - fixed);
				this.naturalGridH = rows * cardH + Math.max(0, rows - 1) * GAP;
				this.visibleGridH = cardH;
			}
		}
		this.panelH = fixed + this.visibleGridH;
		this.scrollable = this.visibleGridH < this.naturalGridH;

		this.panelX = (W - this.panelW) / 2;
		this.panelY = Math.max(4, (H - this.panelH) / 2);
		this.tabsY = this.panelY + this.headerH + 6;
		this.descY = this.tabsY + TAB_H + 4;
		this.gridY = this.descY + descH + 6;
		this.scrollY = clampi(this.scrollY, 0, Math.max(0, this.naturalGridH - this.visibleGridH));

		int chipGap = 3;
		int chipW = Math.min(TAB_W, (this.panelW - CARD_PAD_X * 2 - (this.categories.size() - 1) * chipGap)
				/ this.categories.size());
		int chipRowW = this.categories.size() * chipW + (this.categories.size() - 1) * chipGap;
		int chipX = this.panelX + (this.panelW - chipRowW) / 2;
		for (int i = 0; i < this.categories.size(); i++) {
			this.chips.add(new TabChip(chipX + i * (chipW + chipGap), this.tabsY, chipW, TAB_H, i));
		}

		TextRenderer tr = this.textRenderer;
		int usageMaxW = cardW - 32;
		int maxLines = 2;
		for (int i = 0; i < cat.entries().size(); i++) {
			CommandCatalog.Entry e = cat.entries().get(i);
			int cx = CARD_PAD_X + (i % cols) * (cardW + GAP);
			int cy = (i / cols) * (cardH + GAP);
			List<String> lines = wrap(tr, e.usage(), usageMaxW, maxLines);
			this.cards.add(new Card(cx, cy, cardW, cardH, e, lines));
		}

		int pillH = this.compact ? 20 : 24;
		int pillGap = GAP;
		int pillW = Math.min(150, (this.panelW - CARD_PAD_X * 2 - pillGap * 2) / 3);
		int pillY = this.panelY + this.panelH - pillH - 8;
		int pillLeft = this.panelX + (this.panelW - (pillW * 3 + pillGap * 2)) / 2;
		this.pills.add(new Pill(pillLeft, pillY, pillW, pillH, "关于",
				new ItemStack(Items.NAME_TAG), () -> {
					MinecraftClient c = MinecraftClient.getInstance();
					c.setScreen(new AboutScreen(this));
				}));
		this.pills.add(new Pill(pillLeft + pillW + pillGap, pillY, pillW, pillH, "复制信息",
				new ItemStack(Items.WRITTEN_BOOK), this::copyServerInfo));
		this.pills.add(new Pill(pillLeft + (pillW + pillGap) * 2, pillY, pillW, pillH, "关闭",
				ItemStack.EMPTY, this::close));
	}

	// ---------- 动作 ----------

	private void activate(CommandCatalog.Entry e) {
		MinecraftClient client = MinecraftClient.getInstance();
		switch (e.mode()) {
			case RUN -> {
				if (!this.onServer) {
					showToast("仅服务器内可用");
					return;
				}
				if (!PermissionDetector.canUse(e.command())) {
					showToast("你没有权限使用该指令");
					return;
				}
				if (client.player != null && client.player.networkHandler != null) {
					client.player.networkHandler.sendChatCommand(e.command());
					this.close();
				}
			}
			case COPY -> {
				// 带参数指令 → 进入二级向导页，填写后直接发送
				if (!this.onServer) {
					showToast("仅服务器内可用（离线仅可查看用途）");
					return;
				}
				if (!PermissionDetector.canUse(e.command())) {
					showToast("你没有权限使用该指令");
					return;
				}
				List<CommandWizardScreen.Field> fields = CommandWizardScreen.parseFields(e.command());
				if (fields.isEmpty()) {
					// 无占位符，直接发送
					if (client.player != null && client.player.networkHandler != null) {
						client.player.networkHandler.sendChatCommand(e.command());
						this.close();
					}
				} else {
					client.setScreen(new CommandWizardScreen(this, e));
				}
			}
			case CUSTOM -> {
				switch (e.custom()) {
					case OPEN_CMDS -> client.setScreen(new CommandsScreen(this));
					case OPEN_ABOUT -> client.setScreen(new AboutScreen(this));
					case OPEN_PLAYERS -> client.setScreen(new OnlinePlayersScreen(this));
					case OPEN_UPDATES -> handleCheckUpdate();
					case COPY_COORDS -> copyCoords();
					case COPY_INFO -> copyServerInfo();
					case COPY_DOC -> {
						client.keyboard.setClipboard(ServerDetector.DOC_URL);
						showToast("已复制指令文档链接");
					}
					case COPY_ELEVATOR -> {
						client.keyboard.setClipboard("电梯方块合成: 8x任意同色羊毛 + 1x末影珍珠；至少2块且垂直间距≥2格；站上去[跳跃]上楼 [下蹲]下楼");
						showToast("已复制电梯玩法说明");
					}
					default -> {
					}
				}
			}
			default -> {
			}
		}
	}

	private void copyCoords() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			showToast("需要进入世界后使用");
			return;
		}
		String pos = String.format("我现在的坐标: X=%d Y=%d Z=%d (%s)",
				(int) Math.floor(client.player.getX()),
				(int) Math.floor(client.player.getY()),
				(int) Math.floor(client.player.getZ()),
				client.world.getRegistryKey().getValue());
		client.keyboard.setClipboard(pos);
		showToast("已复制当前坐标");
	}

	private void copyServerInfo() {
		MinecraftClient.getInstance().keyboard.setClipboard(ServerDetector.buildServerInfo());
		showToast("已复制服务器信息");
	}

	/** 检查更新：有新版 → 复制下载链接；无新版 → 提示已是最新。 */
	private void handleCheckUpdate() {
		UpdateChecker.checkAsync();
		UpdateChecker.Result r = UpdateChecker.get();
		if (r == null && UpdateChecker.checking()) {
			showToast("正在检查更新…（首次约需数秒）");
			return;
		}
		if (r == null || r.latestVersion() == null) {
			showToast("无法连接更新源，请稍后重试");
			return;
		}
		if (r.upToDate()) {
			showToast("已是最新版本 v" + r.currentVersion());
			return;
		}
		// 有更新：复制 Release 页面链接
		MinecraftClient client = MinecraftClient.getInstance();
		client.keyboard.setClipboard(r.releaseUrl());
		showToast("发现新版 v" + r.latestVersion() + "，已复制下载链接");
	}

	private void showToast(String text) {
		this.toast = text;
		this.toastUntil = System.currentTimeMillis() + 1800;
	}

	// ---------- 绘制 ----------

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		long now = System.currentTimeMillis();
		float dt = Math.min(0.1f, (now - this.lastFrameMs) / 1000f);
		this.lastFrameMs = now;
		this.timeSec += dt;

		float open = easeOut((now - this.createdAt) / 220f);
		if (this.panelW <= 0) {
			return;
		}

		// 页签过渡进度
		if (this.tabT < 1f) {
			this.tabT = Math.min(1f, this.tabT + dt / 0.32f);
		}
		if (this.cardT < 1f) {
			this.cardT = Math.min(1f, this.cardT + dt / 0.52f);
		}
		float slide = easeInOut(this.tabT);
		float contentAlpha = this.tabT >= 1f ? 1f : easeOut(Math.min(1f, this.tabT * 1.8f));
		// 切换方向：向右切 → 新内容从右滑入；向左切 → 从左滑入
		int dir = (this.prevTab >= 0 && this.tab < this.prevTab) ? -1 : 1;

		CommandCatalog.Category cat = this.categories.get(this.tab);

		// 现代玻璃面板
		GuiKit.glassPanel(context, this.panelX, this.panelY, this.panelW, this.panelH, open);

		// 顶部流动渐变横幅（更浓烈）
		float phase = (this.timeSec * 0.12f) % 1f;
		int h1 = GuiKit.mix(GuiKit.C_ACCENT, GuiKit.C_ACCENT2, phase);
		int h2 = GuiKit.mix(GuiKit.C_ACCENT2, GuiKit.C_ACCENT, phase);
		context.fillGradient(this.panelX, this.panelY, this.panelX + this.panelW,
				this.panelY + this.headerH, GuiKit.withAlpha(h1, open), GuiKit.withAlpha(h2, open));
		// 横幅底部分隔 + 高光
		context.fill(this.panelX, this.panelY + this.headerH,
				this.panelX + this.panelW, this.panelY + this.headerH + 1,
				GuiKit.withAlpha(GuiKit.mix(h1, 0xFFFFFFFF, 0.5f), open));
		context.fillGradient(this.panelX + 4, this.panelY + 2, this.panelX + this.panelW - 4,
				this.panelY + 5, GuiKit.withAlpha(GuiKit.mix(h1, 0xFFFFFFFF, 0.4f), open),
				GuiKit.withAlpha(h1, open * 0.5f));

		// 横幅文字
		Text titleText = Text.literal("MC.520PRO.TOP").styled(style -> style.withBold(true));
		int titleY = this.panelY + (this.compact ? 5 : 8);
		// 爱心带辉光
		float glow = 0.5f + 0.3f * (float) Math.sin(this.timeSec * 3.0);
		context.fill(this.panelX + 11, titleY, this.panelX + 20, titleY + 7,
				GuiKit.withAlpha(GuiKit.C_DANGER, open * glow * 0.4f));
		drawHeart(context, this.panelX + 12, titleY + 1, open);
		context.drawTextWithShadow(this.textRenderer, titleText, this.panelX + 26, titleY,
				GuiKit.withAlpha(0xFFFFFFFF, open));
		if (!this.compact) {
			context.drawTextWithShadow(this.textRenderer, Text.literal("服务器菜单 · 按 M 呼出"),
					this.panelX + 26, this.panelY + 21, GuiKit.withAlpha(0xFFDCD2FF, open));
		}

		// 更新状态（进度条 / 徽章）
		UpdateChecker.Result upd = UpdateChecker.get();
		String updateLabel = null;
		int updateTint = GuiKit.C_WARNING;
		boolean checking = upd == null && UpdateChecker.checking();
		if (upd != null && upd.hasUpdate()) {
			updateLabel = "有更新 v" + upd.latestVersion();
		} else if (upd == null && !checking) {
			UpdateChecker.checkAsync();
		}
		int updateW = updateLabel == null && !checking ? 0
				: (updateLabel != null ? this.textRenderer.getWidth(updateLabel) + 16 : 28);

		// 头衔徽章 + 状态徽章
		PermissionDetector.Rank rank = PermissionDetector.currentRank();
		int rankW = this.textRenderer.getWidth(rank.label) + 14;
		int rankX = this.panelX + this.panelW - 10 - rankW;
		if (updateW > 0) {
			rankX -= updateW + 6;
		}
		int rankY = this.panelY + (this.compact ? 6 : 9);
		GuiKit.badge(context, rankX, rankY, rankW, 16, rank.label, this.textRenderer,
				rank.color, open);

		if (updateW > 0) {
			int upX = rankX - 6 - updateW;
			int upY = rankY;
			if (checking) {
				// 检查中：显示进度条
				TextureKit.draw(context, TextureKit.PROGRESS_BAR, upX, upY, updateW, 16);
				// 旋转动画
				float spin = (this.timeSec * 2.0f) % 1f;
				TextureKit.draw(context, TextureKit.LOADING_SPINNER,
						upX + (int) (updateW * spin) % updateW, upY + 4, 8, 8);
			} else {
				GuiKit.badge(context, upX, upY, updateW, 16, updateLabel, this.textRenderer,
						updateTint, open);
			}
		}

		String status = this.onServer ? ServerDetector.shortInfo() : "功能已锁定";
		int badgeH = 16;
		int badgeW = Math.min(this.textRenderer.getWidth(status) + 22,
				Math.max(84, this.panelW / 2));
		badgeW = Math.min(badgeW, Math.max(60, rankX - this.panelX - 24));
		int badgeX = rankX - 6 - badgeW;
		int badgeY = this.panelY + (this.compact ? 6 : 9);
		int badgeColor = this.onServer ? GuiKit.C_SUCCESS : GuiKit.C_DANGER;
		// 脉冲点
		float pulse = 0.55f + 0.45f * (float) Math.sin(this.timeSec * 4.0);
		context.fill(badgeX + 5, badgeY + 6, badgeX + 9, badgeY + 10,
				GuiKit.withAlpha(GuiKit.mix(badgeColor, 0xFFFFFFFF, 0.5f), open * pulse));
		GuiKit.badge(context, badgeX, badgeY, badgeW, badgeH, status, this.textRenderer,
				badgeColor, open);

		// 分类页签（选中态有滑动高亮）
		TabChip hoveredChip = null;
		for (TabChip chip : this.chips) {
			CommandCatalog.Category c = this.categories.get(chip.index);
			boolean active = chip.index == this.tab;
			boolean hovered = !active && mouseX >= chip.x && mouseX < chip.x + chip.w
					&& mouseY >= chip.y && mouseY < chip.y + chip.h;
			int accent = accentFor(c);
			int bg = active ? GuiKit.mix(accent, 0xFFFFFFFF, 0.3f) : (hovered ? GuiKit.mix(accent, 0xFFFFFFFF, 0.15f) : 0xFF1A1A2E);
			context.fill(chip.x, chip.y, chip.x + chip.w, chip.y + chip.h, GuiKit.withAlpha(bg, open));
			int borderColor = active ? accent : (hovered ? GuiKit.C_BORDER_HI : GuiKit.C_BORDER);
			outline(context, chip.x, chip.y, chip.w, chip.h, GuiKit.withAlpha(borderColor, open));
			// 图标徽章 + 文字首字（现代风格，不用 MC 物品图标）
			GuiKit.iconBadge(context, chip.x + (chip.w - 14) / 2, chip.y + 3, 14, accent, open);
			drawIconText(context, c.title(), chip.x + chip.w / 2, chip.y + 10, accent, open);
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal(fit(c.title(), chip.w - 2)), chip.x + chip.w / 2, chip.y + 19,
					GuiKit.withAlpha(active ? 0xFFFFFFFF : GuiKit.C_MUTED, open));
			if (active) {
				context.fill(chip.x + 4, chip.y + chip.h - 3, chip.x + chip.w - 4, chip.y + chip.h - 1,
						GuiKit.withAlpha(accent, open));
			}
			if (hovered) {
				hoveredChip = chip;
			}
		}

		// 选中态滑动光标：在旧页签与新页签之间插值
		if (this.tabT < 1f && this.prevTab >= 0 && this.prevTab < this.chips.size()) {
			TabChip from = this.chips.get(this.prevTab);
			TabChip to = this.chips.get(this.tab);
			float k = slide;
			int sx = Math.round(from.x + (to.x - from.x) * k);
			int sw = Math.round(from.w + (to.w - from.w) * k);
			int sy = Math.round(from.y + (to.y - from.y) * k);
			int sh = Math.round(from.h + (to.h - from.h) * k);
			// 光标带一点弹性（超调再回落），更利落
			float over = (float) Math.sin(Math.min(1f, this.tabT) * Math.PI) * 3f;
			context.fill(sx, sy - (int) over, sx + sw, sy - (int) over + sh,
					withAlpha(0x38FFFFFF, open));
			outline(context, sx, sy - (int) over, sw, sh, withAlpha(0xFFFFFFFF, open));
		}

		// 分类说明行
		context.fill(this.panelX + CARD_PAD_X, this.descY + 2, this.panelX + CARD_PAD_X + 3,
				this.descY + 9, withAlpha(0xFFFF5C7A, open));
		context.drawTextWithShadow(this.textRenderer, Text.literal(cat.title()),
				this.panelX + CARD_PAD_X + 8, this.descY, withAlpha(0xFFFFFFFF, open));
		context.drawTextWithShadow(this.textRenderer,
				Text.literal(fit("· " + cat.desc(), this.panelW - CARD_PAD_X * 2 - 8 - this.textRenderer.getWidth(cat.title()))),
				this.panelX + CARD_PAD_X + 12 + this.textRenderer.getWidth(cat.title()), this.descY,
				withAlpha(0xFF9C93C4, open));

		// 卡片（滚动视口 + 裁剪 + 入场动画）
		int viewTop = this.gridY;
		int viewBottom = this.gridY + this.visibleGridH;
		context.enableScissor(this.panelX + 2, viewTop, this.panelX + this.panelW - 2, viewBottom);
		Card hoveredCard = null;
		int cardIndex = 0;
		int cardCount = this.cards.size();
		for (Card card : this.cards) {
			int x = this.panelX + card.x;
			// 切换时整体位移 + 淡入；卡片逐个错峰入场
			float stagger = cardCount <= 1 ? 1f
					: clamp01((this.cardT - (float) cardIndex / cardCount * 0.5f) / 0.5f);
			float local = easeOut(stagger);
			int offsetX = Math.round((1f - local) * 30 * dir);
			int y = viewTop + card.y - this.scrollY;
			if (y + card.h < viewTop || y > viewBottom) {
				cardIndex++;
				continue;
			}
			boolean noPerm = this.onServer && card.entry.mode() != CommandCatalog.Mode.CUSTOM
					&& !PermissionDetector.canUse(card.entry.command());
			boolean locked = (!this.onServer && card.entry.mode() != CommandCatalog.Mode.CUSTOM) || noPerm;
			boolean hovered = mouseX >= x && mouseX < x + card.w
					&& mouseY >= y && mouseY < y + card.h
					&& mouseY >= viewTop && mouseY <= viewBottom;
			int lift = hovered && !locked ? 1 : 0;
			y -= lift;
			x += offsetX;

			// 质感卡片（纹理绘制）
			int accent = accentFor(cat);
			float cardAlpha = open * contentAlpha * local;
			boolean selected = hovered && !locked;
			GuiKit.card(context, x, y, card.w, card.h, accent, cardAlpha, hovered, selected, locked);

			// 图标徽章 + 文字首字（纹理绘制）
			int iconX = x + 8;
			int iconY = y + (card.h - 20) / 2;
			if (!locked) {
				TextureKit.draw(context, TextureKit.ICON_BADGE, iconX, iconY, 20, 20);
			}
			drawIconText(context, card.entry.name(), iconX + 10, iconY + 10, accent, cardAlpha);

			int textX = x + 32;
			int nameColor = locked ? GuiKit.C_DIM : 0xFFFFFFFF;
			context.drawTextWithShadow(this.textRenderer,
					Text.literal(fit(card.entry.name(), card.w - 36)), textX, y + 4,
					GuiKit.withAlpha(nameColor, cardAlpha));
			int lineY = y + 15;
			for (String line : card.usageLines) {
				if (lineY + 9 > y + card.h) {
					break;
				}
				context.drawTextWithShadow(this.textRenderer, Text.literal(line), textX, lineY,
						GuiKit.withAlpha(locked ? GuiKit.C_DIM : GuiKit.C_MUTED, cardAlpha));
				lineY += 10;
			}
			if (noPerm || locked) {
				// 无权限/锁定：红色圆点
				TextureKit.draw(context, TextureKit.BADGE_DOT, x + card.w - 10, y + 8, 6, 6);
			} else if (card.entry.mode() == CommandCatalog.Mode.COPY) {
				// 带参数：右下角箭头 + 青色点（推断）
				context.drawTextWithShadow(this.textRenderer, Text.literal("▶"),
						x + card.w - 10, y + card.h - 13, GuiKit.withAlpha(GuiKit.C_SUCCESS, cardAlpha));
				if (!card.entry.fromDoc()) {
					GuiKit.iconBadge(context, x + card.w - 7, y + card.h - 7, 3, GuiKit.C_ACCENT2, cardAlpha);
				}
			} else if (!card.entry.fromDoc()) {
				GuiKit.iconBadge(context, x + card.w - 7, y + card.h - 7, 3, GuiKit.C_ACCENT2, cardAlpha);
			}
			if (hovered) {
				hoveredCard = card;
			}
		}
		context.disableScissor();

		// 滚动条（质感）
		if (this.scrollable) {
			int trackH = this.visibleGridH;
			int thumbH = Math.max(20, (int) ((long) trackH * trackH / this.naturalGridH));
			int maxScroll = this.naturalGridH - this.visibleGridH;
			int thumbY = viewTop + (maxScroll <= 0 ? 0 : (int) ((long) (trackH - thumbH) * this.scrollY / maxScroll));
			int barX = this.panelX + this.panelW - 8;
			context.fill(barX, viewTop, barX + 5, viewTop + trackH, GuiKit.withAlpha(0xFF1A1A2E, open));
			context.fill(barX, thumbY, barX + 5, thumbY + thumbH, GuiKit.withAlpha(GuiKit.C_ACCENT, open));
			context.fill(barX, thumbY, barX + 5, thumbY + 3, GuiKit.withAlpha(GuiKit.mix(GuiKit.C_ACCENT, 0xFFFFFFFF, 0.4f), open));
		}

		// 底部胶囊按钮（现代风格）
		Pill hoveredPill = null;
		for (Pill pill : this.pills) {
			boolean hovered = mouseX >= pill.x && mouseX < pill.x + pill.w
					&& mouseY >= pill.y && mouseY < pill.y + pill.h;
			int pillColor = GuiKit.C_ACCENT;
			if (pill.label.equals("关闭")) {
				pillColor = GuiKit.C_DANGER;
			} else if (pill.label.equals("关于")) {
				pillColor = GuiKit.C_PURPLE;
			} else {
				pillColor = GuiKit.C_ACCENT2;
			}
			GuiKit.pill(context, pill.x, pill.y, pill.w, pill.h, fit(pill.label, pill.w - 20),
					this.textRenderer, hovered, pillColor, open, false);
			if (hovered) {
				hoveredPill = pill;
			}
		}

		// 提示条（toast）
		if (this.toast != null) {
			long left = this.toastUntil - now;
			if (left <= 0) {
				this.toast = null;
			} else {
				float a = clamp(left / 400f) * open;
				int w = this.textRenderer.getWidth(this.toast) + 16;
				int tx = this.panelX + (this.panelW - w) / 2;
				int ty = this.panelY + this.panelH + 6;
				if (ty + 18 > this.height) {
					ty = this.height - 20;
				}
				context.fill(tx, ty, tx + w, ty + 18, withAlpha(0xE0100F1C, a));
				outline(context, tx, ty, w, 18, withAlpha(0xFF7C6BD9, a));
				context.drawTextWithShadow(this.textRenderer, Text.literal(this.toast), tx + 8, ty + 5,
						withAlpha(0xFFFFFFFF, a));
			}
		}

		// 浮窗最后画，避免被遮挡
		if (hoveredCard != null) {
			CommandCatalog.Entry e = hoveredCard.entry;
			String hint = switch (e.mode()) {
				case RUN -> this.onServer ? "点击立即执行" : "仅服务器内可用";
				case COPY -> "点击复制，粘贴后补参数";
				case CUSTOM -> "点击使用";
			};
			if (!e.fromDoc()) {
				hint += " · 插件推断指令";
			}
			drawTooltip(context, mouseX, mouseY, e.name(), e.usage(),
					e.command().isEmpty() ? "" : "/" + e.command(), hint,
					!this.onServer && e.mode() == CommandCatalog.Mode.RUN, open);
		}
		if (hoveredChip != null) {
			CommandCatalog.Category c = this.categories.get(hoveredChip.index);
			drawSimpleTooltip(context, mouseX, mouseY, c.title() + " · " + c.desc(), open);
		}
	}

	private void drawTooltip(DrawContext context, int mouseX, int mouseY, String name, String usage,
			String command, String hint, boolean locked, float alpha) {
		List<String> usageLines = wrap(this.textRenderer, usage, 180, 3);
		List<String> lines = new ArrayList<>();
		lines.add(name + (command.isEmpty() ? "" : "  " + command));
		lines.addAll(usageLines);
		lines.add(hint);

		int w = 0;
		for (String l : lines) {
			w = Math.max(w, this.textRenderer.getWidth(l));
		}
		w += 14;
		int h = lines.size() * 11 + 8;
		int tx = Math.min(mouseX + 12, this.width - w - 6);
		int ty = Math.max(4, Math.min(mouseY - 12, this.height - h - 6));
		context.fill(tx, ty, tx + w, ty + h, withAlpha(0xF8100F1C, alpha));
		context.fill(tx, ty, tx + w, ty + 1, withAlpha(locked ? 0xFFFF7080 : 0xFF7C6BD9, alpha));
		context.fill(tx, ty + h - 1, tx + w, ty + h, withAlpha(locked ? 0xFFFF7080 : 0xFF7C6BD9, alpha));
		int yy = ty + 5;
		for (int i = 0; i < lines.size(); i++) {
			int color = i == 0 ? 0xFFFFFFFF
					: (i == lines.size() - 1 ? (locked ? 0xFFFF8F9A : 0xFF97F5B4) : 0xFFB9AEDC);
			context.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(i)), tx + 7, yy,
					withAlpha(color, alpha));
			yy += 11;
		}
	}

	private void drawSimpleTooltip(DrawContext context, int mouseX, int mouseY, String text, float alpha) {
		int w = this.textRenderer.getWidth(text) + 12;
		int h = 16;
		int tx = Math.min(mouseX + 12, this.width - w - 6);
		int ty = Math.max(4, Math.min(mouseY - 10, this.height - h - 6));
		context.fill(tx, ty, tx + w, ty + h, withAlpha(0xF8100F1C, alpha));
		outline(context, tx, ty, w, h, withAlpha(0xFF7C6BD9, alpha));
		context.drawTextWithShadow(this.textRenderer, Text.literal(text), tx + 6, ty + 5,
				withAlpha(0xFFFFFFFF, alpha));
	}

	/** 按分类返回强调色。 */
	private static int accentFor(CommandCatalog.Category c) {
		String t = c.title();
		if (t.equals("传送")) {
			return GuiKit.C_ACCENT2;
		}
		if (t.equals("领地")) {
			return GuiKit.C_SUCCESS;
		}
		if (t.equals("市场")) {
			return GuiKit.C_GOLD;
		}
		if (t.equals("MMO")) {
			return GuiKit.C_ACCENT;
		}
		if (t.equals("趣味")) {
			return GuiKit.C_PURPLE;
		}
		if (t.equals("社交")) {
			return GuiKit.C_BLUE;
		}
		if (t.equals("经济")) {
			return GuiKit.C_GOLD;
		}
		if (t.equals("工具")) {
			return GuiKit.C_ACCENT2;
		}
		if (t.equals("防熊")) {
			return GuiKit.C_DANGER;
		}
		return GuiKit.C_ACCENT;
	}

	/** 在图标徽章中央画文字首字（现代图标）。 */
	private void drawIconText(DrawContext ctx, String label, int cx, int cy, int color, float alpha) {
		String ch = label != null && label.length() > 0 ? label.substring(0, 1) : "";
		ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(ch), cx, cy - 6,
				GuiKit.withAlpha(color, alpha));
	}

	private void outline(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y, x + 1, y + h, color);
		context.fill(x + w - 1, y, x + w, y + h, color);
	}

	private void drawX(DrawContext context, int x, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			context.fill(x + i, y + i, x + i + 1, y + i + 1, color);
			context.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, color);
		}
	}

	/** 标题左侧的小像素爱心（520 主题）。 */
	private void drawHeart(DrawContext context, int x, int y, float alpha) {
		int color = withAlpha(0xFFFF5C7A, alpha);
		int[][] cells = {
				{1, 2, 4, 5},
				{0, 1, 2, 3, 4, 5, 6},
				{0, 1, 2, 3, 4, 5, 6},
				{1, 2, 3, 4, 5},
				{2, 3, 4},
				{3},
		};
		for (int row = 0; row < cells.length; row++) {
			for (int col : cells[row]) {
				context.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}

	// ---------- 交互 ----------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}
		for (TabChip chip : this.chips) {
			if (mouseX >= chip.x && mouseX < chip.x + chip.w && mouseY >= chip.y && mouseY < chip.y + chip.h) {
				switchTab(chip.index);
				return true;
			}
		}
		for (Card card : this.cards) {
			int x = this.panelX + card.x;
			int y = this.gridY + card.y - this.scrollY;
			boolean inView = y + card.h >= this.gridY && y <= this.gridY + this.visibleGridH;
			if (inView && mouseX >= x && mouseX < x + card.w && mouseY >= y && mouseY < y + card.h) {
				activate(card.entry);
				return true;
			}
		}
		for (Pill pill : this.pills) {
			if (mouseX >= pill.x && mouseX < pill.x + pill.w && mouseY >= pill.y && mouseY < pill.y + pill.h) {
				pill.action.run();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.scrollable && mouseY >= this.gridY && mouseY <= this.gridY + this.visibleGridH
				&& mouseX >= this.panelX && mouseX <= this.panelX + this.panelW) {
			int maxScroll = Math.max(0, this.naturalGridH - this.visibleGridH);
			this.scrollY = clampi(this.scrollY - (int) Math.signum(verticalAmount) * 26, 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.scrollable && (keyCode == 264 || keyCode == 265)) { // DOWN / UP
			int maxScroll = Math.max(0, this.naturalGridH - this.visibleGridH);
			int step = this.compact ? 38 : 48;
			this.scrollY = clampi(this.scrollY + (keyCode == 264 ? step : -step), 0, maxScroll);
			return true;
		}
		if (keyCode == 263) { // LEFT
			switchTab((this.tab - 1 + this.categories.size()) % this.categories.size());
			return true;
		}
		if (keyCode == 262) { // RIGHT
			switchTab((this.tab + 1) % this.categories.size());
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	// ---------- 小工具 ----------

	private List<String> wrap(TextRenderer tr, String text, int maxWidth, int maxLines) {
		List<String> out = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return out;
		}
		StringBuilder cur = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (tr.getWidth(cur + "" + ch) > maxWidth) {
				out.add(cur.toString());
				cur.setLength(0);
				if (out.size() == maxLines) {
					break;
				}
			}
			cur.append(ch);
		}
		if (out.size() < maxLines && cur.length() > 0) {
			out.add(cur.toString());
		}
		if (out.size() == maxLines) {
			String last = out.get(maxLines - 1);
			while (last.length() > 1 && tr.getWidth(last + "…") > maxWidth) {
				last = last.substring(0, last.length() - 1);
			}
			if (!last.equals(out.get(maxLines - 1))) {
				out.set(maxLines - 1, last + "…");
			}
		}
		return out;
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

	/** 统一套 alpha；输入色若未带 alpha 通道按不透明处理。 */
	private static int withAlpha(int color, float alpha) {
		int baseA = (color >>> 24) & 0xFF;
		if (baseA == 0) {
			baseA = 0xFF;
		}
		int a = Math.round(baseA * clamp(alpha));
		return (clampInt(a) << 24) | (color & 0xFFFFFF);
	}

	private static int clampi(int v, int lo, int hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	private static float clamp(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	/** 平滑的缓入缓出（用于页签滑动，比 easeOut 更柔和）。 */
	private static float easeInOut(float t) {
		float k = clamp(t);
		return k < 0.5f ? 4f * k * k * k : 1f - (float) Math.pow(-2f * k + 2f, 3) / 2f;
	}

	private static int clampInt(int v) {
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	private static int mix(int c1, int c2, float t) {
		float k = clamp(t);
		int a1 = (c1 >>> 24) & 0xFF;
		int a2 = (c2 >>> 24) & 0xFF;
		if (a1 == 0) {
			a1 = 0xFF;
		}
		if (a2 == 0) {
			a2 = 0xFF;
		}
		int a = Math.round(a1 * (1 - k) + a2 * k);
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
