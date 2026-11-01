package top.mc520pro.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import top.mc520pro.menu.data.CommandCatalog;

/**
 * 自动探测：
 * 1) 服务器下发的指令树（只包含当前玩家有权限使用的指令）→ 判断功能可用性；
 * 2) 玩家头衔（VIP/管理/普通）：从玩家列表显示名的颜色格式 + 指令树特征双重推断；
 * 3) 带参数指令的参数结构（从指令树 children 解析：玩家名/数字/文本）。
 * 结果缓存在会话内，打开菜单时刷新。
 */
public final class PermissionDetector {
	public enum Rank {
		UNKNOWN("未知", 0xFFB9AEDC),
		PLAYER("普通玩家", 0xFF9CFFC9),
		VIP("VIP", 0xFFFFD766),
		ADMIN("管理员", 0xFFFF8F9A);

		public final String label;
		public final int color;

		Rank(String label, int color) {
			this.label = label;
			this.color = color;
		}
	}

	/** 参数类型（用于二级向导页）。 */
	public enum ArgType {
		PLAYER, NUMBER, TEXT, WORD
	}

	public record ArgSpec(String name, ArgType type) {
	}

	private static Set<String> available = Set.of();
	private static List<String> argNames = List.of();
	private static Rank rank = Rank.UNKNOWN;
	private static long lastScan = 0;
	/** 开发自检用：注入假的指令树/头衔，用于验证在线态界面（仅 -Pautotest 生效）。 */
	private static Set<String> injectedCommands = null;
	private static Rank injectedRank = null;
	/** 开发自检用：注入假的在线玩家列表。 */
	private static List<OnlinePlayer> injectedPlayers = null;

	private PermissionDetector() {
	}

	/** 开发自检：注入模拟的服务器指令树与头衔。 */
	public static void debugInject(Set<String> commands, Rank forcedRank) {
		injectedCommands = commands;
		injectedRank = forcedRank;
		lastScan = 0;
	}

	/** 开发自检：注入模拟的在线玩家列表（用于验收在线玩家页渲染）。 */
	public static void debugInjectPlayers(List<OnlinePlayer> players) {
		injectedPlayers = players;
	}

	/** 开发自检：清除注入。 */
	public static void debugClear() {
		injectedCommands = null;
		injectedRank = null;
		injectedPlayers = null;
		lastScan = 0;
	}

	/** 刷新探测（60 秒内不重复扫描）。 */
	public static void refresh() {
		long now = System.currentTimeMillis();
		if (now - lastScan < 60_000 && !available.isEmpty()) {
			return;
		}
		lastScan = now;

		// 开发自检注入优先（模拟在线态，用于截图验收）
		if (injectedCommands != null) {
			available = injectedCommands;
			rank = injectedRank != null ? injectedRank : Rank.PLAYER;
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		if (handler == null) {
			available = Set.of();
			rank = Rank.UNKNOWN;
			return;
		}
		Set<String> cmds = new HashSet<>();
		try {
			CommandDispatcher<ClientCommandSource> dispatcher = handler.getCommandDispatcher();
			RootCommandNode<ClientCommandSource> root = dispatcher.getRoot();
			for (CommandNode<ClientCommandSource> node : root.getChildren()) {
				cmds.add(node.getName().toLowerCase(Locale.ROOT));
			}
		} catch (Throwable ignored) {
			// 指令树未就绪时跳过
		}
		available = cmds;

		// 头衔推断：指令树特征优先，其次玩家列表显示名颜色
		boolean adminish = cmds.contains("gamemode") || cmds.contains("op") || cmds.contains("deop")
				|| cmds.contains("ban") || cmds.contains("tp") || cmds.contains("gamerule");
		boolean vipish = cmds.contains("fly") || cmds.contains("hat") || cmds.contains("nick")
				|| cmds.contains("workbench") || cmds.contains("enderchest") || cmds.contains("speed");
		if (adminish) {
			rank = Rank.ADMIN;
		} else if (vipish) {
			rank = Rank.VIP;
		} else {
			rank = Rank.PLAYER;
		}
		// 列表显示名里的关键词（很多服把头衔放在 player list 显示名）
		try {
			if (client.player != null) {
				PlayerListEntry self = handler.getPlayerListEntry(client.player.getUuid());
				if (self != null && self.getDisplayName() != null) {
					String disp = self.getDisplayName().getString()
							.replaceAll("§.", "").toLowerCase(Locale.ROOT);
					if (disp.contains("管理") || disp.contains("op") || disp.contains("admin")) {
						rank = Rank.ADMIN;
					} else if (disp.contains("vip") || disp.contains("会员")) {
						rank = Rank.VIP;
					} else if (rank == Rank.UNKNOWN) {
						rank = Rank.PLAYER;
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}

	/** 指令是否可用（未在服务器=全解锁预览；在服务器=查指令树）。 */
	public static boolean canUse(String command) {
		if (!ServerDetector.isOnServer()) {
			return true; // 预览模式不做限制
		}
		if (available.isEmpty()) {
			return true; // 指令树未就绪，宽松处理
		}
		String root = rootOf(command);
		return available.contains(root);
	}

	/** 解析指令的参数结构（用于向导页）。返回空列表 = 无参数。 */
	public static List<ArgSpec> argsOf(String command) {
		List<ArgSpec> out = new ArrayList<>();
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		if (handler == null || command == null || command.isEmpty()) {
			return out;
		}
		String[] parts = command.trim().split("\\s+");
		try {
			CommandNode<ClientCommandSource> node = handler.getCommandDispatcher().getRoot();
			for (String part : parts) {
				CommandNode<ClientCommandSource> next = null;
				for (CommandNode<ClientCommandSource> child : node.getChildren()) {
					if (child.getName().equalsIgnoreCase(part)) {
						next = child;
						break;
					}
				}
				if (next == null) {
					break;
				}
				node = next;
			}
			// node 现在指向最后一个字面量；其 children 即参数
			for (CommandNode<ClientCommandSource> child : node.getChildren()) {
				if (child instanceof LiteralCommandNode) {
					out.add(new ArgSpec(child.getName(), ArgType.WORD));
				} else if (child instanceof ArgumentCommandNode) {
					out.add(new ArgSpec(child.getName(), classify(child.getName())));
				}
			}
		} catch (Throwable ignored) {
		}
		return out;
	}

	private static ArgType classify(String argName) {
		String n = argName.toLowerCase(Locale.ROOT);
		if (n.contains("player") || n.contains("target") || n.contains("name") && n.contains("play")) {
			return ArgType.PLAYER;
		}
		if (n.contains("amount") || n.contains("number") || n.contains("distance") || n.contains("radius")
				|| n.contains("level") || n.contains("count") || n.contains("price") || n.contains("value")
				|| n.contains("seconds") || n.contains("time")) {
			return ArgType.NUMBER;
		}
		if (n.contains("message") || n.contains("reason") || n.contains("text") || n.contains("content")) {
			return ArgType.TEXT;
		}
		return ArgType.WORD;
	}

	private static String rootOf(String command) {
		String c = command.trim();
		int sp = c.indexOf(' ');
		return (sp < 0 ? c : c.substring(0, sp)).toLowerCase(Locale.ROOT);
	}

	public static Rank currentRank() {
		refresh();
		return rank;
	}

	public static Set<String> availableCommands() {
		refresh();
		return available;
	}

	/** 给卡片用的可用性判断：RUN/COPY 类指令在服务器且无权限时返回 false。 */
	public static boolean entryUsable(CommandCatalog.Entry entry) {
		if (!ServerDetector.isOnServer()) {
			return false; // 服务器外一律锁定（mod 核心规则）
		}
		if (entry.mode() == CommandCatalog.Mode.CUSTOM) {
			return true;
		}
		return canUse(entry.command());
	}

	/** 在线玩家名列表（向导页选玩家用）。 */
	public static Collection<String> onlinePlayers() {
		MinecraftClient client = MinecraftClient.getInstance();
		List<String> out = new ArrayList<>();
		if (client == null || client.getNetworkHandler() == null) {
			return out;
		}
		for (PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
			try {
				out.add(e.getProfile().getName());
			} catch (Throwable ignored) {
			}
		}
		return out;
	}

	/** 在线玩家详情（名字 + 延迟 + 显示名/头衔），用于在线列表页。 */
	public record OnlinePlayer(String name, int latency, String displayName, boolean self) {
	}

	/** 在线玩家详情列表。 */
	public static List<OnlinePlayer> onlinePlayerDetails() {
		if (injectedPlayers != null) {
			return injectedPlayers;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		List<OnlinePlayer> out = new ArrayList<>();
		if (client == null || client.getNetworkHandler() == null) {
			return out;
		}
		String selfName = client.player != null ? client.player.getGameProfile().getName() : null;
		for (PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
			try {
				String name = e.getProfile().getName();
				String disp = e.getDisplayName() == null ? name : e.getDisplayName().getString();
				out.add(new OnlinePlayer(name, e.getLatency(), disp, name.equals(selfName)));
			} catch (Throwable ignored) {
			}
		}
		out.sort((a, b) -> {
			if (a.self() != b.self()) {
				return a.self() ? -1 : 1; // 自己排最前
			}
			return a.name().compareToIgnoreCase(b.name());
		});
		return out;
	}
}
