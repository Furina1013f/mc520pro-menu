package top.mc520pro.menu;

import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

/**
 * 服务器归属与信息检测：只有当前连接的是 mc.520pro.top 时才返回 true。
 * 每次打开菜单 / 执行功能时实时判定，避免状态过期。
 */
public final class ServerDetector {
	public static final String SERVER_HOST = "mc.520pro.top";
	/** 金山文档《有什么指令》。 */
	public static final String DOC_URL = "https://www.kdocs.cn/l/coTJB3ba0Ctl";

	private ServerDetector() {
	}

	public static boolean isOnServer() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null) {
			return false; // 未连接任何服务器
		}
		if (client.getServer() != null) {
			return false; // 单机 / 局域网世界（内置服务端）
		}
		ServerInfo entry = client.getCurrentServerEntry();
		return entry != null && matches(entry.address);
	}

	/** 地址匹配：忽略大小写与端口，mc.520pro.top / mc.520pro.top:25565 均可。 */
	public static boolean matches(String address) {
		if (address == null) {
			return false;
		}
		String a = address.trim().toLowerCase();
		int colon = a.indexOf(':');
		if (colon >= 0) {
			a = a.substring(0, colon);
		}
		return a.equals(SERVER_HOST);
	}

	/** 服务器品牌（Velocity/Paper/Fabric 等由服务端上报）。 */
	public static String brand() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null) {
			return "未连接";
		}
		String b = client.getNetworkHandler().getBrand();
		return b == null || b.isEmpty() ? "未知" : b;
	}

	/** 当前延迟（毫秒），拿不到返回 -1。 */
	public static int pingMs() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null || client.player == null) {
			return -1;
		}
		PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
		return entry == null ? -1 : entry.getLatency();
	}

	/** 标签列表里的在线人数，拿不到返回 -1。 */
	public static int onlinePlayers() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getNetworkHandler() == null) {
			return -1;
		}
		return client.getNetworkHandler().getPlayerList().size();
	}

	/** 菜单头部用的一行摘要。 */
	public static String shortInfo() {
		if (!isOnServer()) {
			return "未连接";
		}
		int ping = pingMs();
		return brand() + " · " + (ping >= 0 ? ping + "ms" : "?ms") + " · " + onlinePlayers() + " 人在线";
	}

	/** 复制到剪贴板的服务器信息。 */
	public static String buildServerInfo() {
		MinecraftClient client = MinecraftClient.getInstance();
		StringBuilder sb = new StringBuilder();
		sb.append("服务器: ").append(SERVER_HOST).append('\n');
		sb.append("品牌: ").append(brand()).append('\n');
		sb.append("延迟: ").append(pingMs()).append(" ms\n");
		sb.append("在线: ").append(onlinePlayers()).append(" 人\n");
		if (client.player != null) {
			sb.append(String.format(Locale.ROOT, "坐标: %.1f, %.1f, %.1f%n",
					client.player.getX(), client.player.getY(), client.player.getZ()));
		}
		sb.append("检测: ").append(isOnServer() ? "专属服务器，功能已解锁" : "非专属服务器，功能锁定");
		return sb.toString();
	}
}
