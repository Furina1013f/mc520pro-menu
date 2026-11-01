package top.mc520pro.menu;

import java.io.File;
import java.util.List;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import top.mc520pro.menu.data.CommandCatalog;
import top.mc520pro.menu.screen.AboutScreen;
import top.mc520pro.menu.screen.CommandWizardScreen;
import top.mc520pro.menu.screen.CommandsScreen;
import top.mc520pro.menu.screen.MainMenuScreen;
import top.mc520pro.menu.screen.OnlinePlayersScreen;

/**
 * 开发自检（仅 -Dmc520pro.autotest=true 时启用，正常游戏不会执行）：
 * 自动打开菜单 → 各分类页 → 切换动画中间帧 → 在线玩家页 → 向导页 → 关于页 → 指令清单页，
 * 逐张截图后退出。
 */
public final class AutoTest {
	private static int ticks = 0;
	private static int menuTicks = 0;
	private static MainMenuScreen menu;
	private static boolean menuOpen = false;

	private AutoTest() {
	}

	public static void tick(MinecraftClient client) {
		ticks++;

		// 首次运行会弹出「无障碍设置」向导，标记为已看过并关闭，避免挡住所有截图
		if (!menuOpen) {
			try {
				if (client.options != null && client.options.onboardAccessibility) {
					client.options.setAccessibilityOnboarded();
				}
			} catch (Throwable ignored) {
			}
			if (client.currentScreen != null) {
				String cls = client.currentScreen.getClass().getSimpleName();
				if (cls.contains("Accessibility") || cls.contains("Narrator")) {
					client.currentScreen.close();
					return;
				}
			}
		}

		if (ticks == 40) {
			// 模拟在线态：注入一份典型指令树 + VIP 头衔，用于验收头衔徽章与权限标
			PermissionDetector.debugInject(Set.of("spawn", "home", "sethome", "delhome", "homes",
					"back", "rtp", "tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel", "warp",
					"warps", "fly", "speed", "jump", "top", "bottom", "res", "co", "market",
					"exmarket", "mcstats", "skills", "sit", "lay", "bellyflop", "spin", "crawl",
					"hat", "hongbao", "rp", "report", "jubao", "tags", "playtime", "list", "afk",
					"msg", "r", "nick", "me", "seen", "who", "balance", "pay", "baltop", "kit",
					"kits", "workbench", "enderchest", "chest", "anvil", "grindstone", "stonecutter",
					"heal", "feed", "repair"), PermissionDetector.Rank.VIP);
		} else if (ticks == 60) {
			menu = new MainMenuScreen();
			client.setScreen(menu);
			menuOpen = true;
			// 注入模拟在线玩家，验证在线玩家页渲染
			PermissionDetector.debugInjectPlayers(List.of(
					new PermissionDetector.OnlinePlayer("Furina", 28, "§6[VIP] Furina", true),
					new PermissionDetector.OnlinePlayer("Steve", 63, "Steve", false),
					new PermissionDetector.OnlinePlayer("Alex", 142, "§b[会员] Alex", false),
					new PermissionDetector.OnlinePlayer("Notch", 215, "§c[管理员] Notch", false),
					new PermissionDetector.OnlinePlayer("Herobrine", 47, "Herobrine", false),
					new PermissionDetector.OnlinePlayer("Creeper_Yuki", 89, "§a[Creeper] Creeper_Yuki", false)));
		}

		if (!menuOpen) {
			return;
		}
		menuTicks++;

		if (menuTicks == 60) {
			shot(client, "v10_tab0_tp");
		} else if (menuTicks == 75) {
			menu.debugSetTab(1); // 触发切换动画，稍后截中间帧
		} else if (menuTicks == 80) {
			shot(client, "v10_anim_mid");
		} else if (menuTicks == 140) {
			shot(client, "v10_tab1_res");
		} else if (menuTicks == 160) {
			menu.debugSetTab(6);
		} else if (menuTicks == 220) {
			shot(client, "v10_tab6_econ");
		} else if (menuTicks == 240) {
			client.setScreen(new OnlinePlayersScreen(null));
		} else if (menuTicks == 310) {
			shot(client, "v10_players");
		} else if (menuTicks == 330) {
			client.setScreen(new CommandWizardScreen(null, findExact("tpa")));
		} else if (menuTicks == 400) {
			shot(client, "v10_wizard_player");
		} else if (menuTicks == 420) {
			client.setScreen(new CommandWizardScreen(null, findExact("speed")));
		} else if (menuTicks == 490) {
			shot(client, "v10_wizard_number");
		} else if (menuTicks == 510) {
			client.setScreen(new AboutScreen(null));
		} else if (menuTicks == 580) {
			shot(client, "v10_about");
		} else if (menuTicks == 600) {
			client.setScreen(new CommandsScreen(null));
		} else if (menuTicks == 680) {
			shot(client, "v10_cmds");
		} else if (menuTicks == 720) {
			client.scheduleStop();
		}
	}

	/** 按指令名（可含参数）精确匹配，例如 "tpa <玩家名>"。 */
	private static CommandCatalog.Entry findExact(String command) {
		for (CommandCatalog.Category c : CommandCatalog.CATEGORIES) {
			for (CommandCatalog.Entry e : c.entries()) {
				if (e.command().equals(command)) {
					return e;
				}
			}
		}
		for (CommandCatalog.Category c : CommandCatalog.CATEGORIES) {
			for (CommandCatalog.Entry e : c.entries()) {
				if (e.command().startsWith(command + " ") || e.command().startsWith(command + "<")) {
					return e;
				}
			}
		}
		return CommandCatalog.CATEGORIES.get(0).entries().get(0);
	}

	private static void shot(MinecraftClient client, String name) {
		File dir = new File(client.runDirectory, "screenshots");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		ScreenshotRecorder.saveScreenshot(dir, name + ".png", client.getFramebuffer(), 1, text -> { });
	}
}
