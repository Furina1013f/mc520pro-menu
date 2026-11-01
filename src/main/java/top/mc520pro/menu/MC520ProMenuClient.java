package top.mc520pro.menu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mc520pro.menu.screen.MainMenuScreen;

/**
 * 520Pro 服务器专属菜单（mc.520pro.top）。
 * 加载后注册 M 键呼出内置菜单；是否解锁功能由 ServerDetector 实时判定。
 */
public class MC520ProMenuClient implements ClientModInitializer {
	public static final String MOD_ID = "mc520pro";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		LOGGER.info("[520Pro] 菜单 Mod 加载中，专属服务器: {}", ServerDetector.SERVER_HOST);

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.mc520pro.open_menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				"key.categories.mc520pro"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.wasPressed()) {
				client.setScreen(new MainMenuScreen());
			}
		});

		if (Boolean.getBoolean("mc520pro.autotest")) {
			ClientTickEvents.END_CLIENT_TICK.register(AutoTest::tick);
			LOGGER.info("[520Pro] 自检模式已启用");
		}

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				LOGGER.info("[520Pro] 进入服务器，检测结果 = {}（仅 {} 解锁功能）",
						ServerDetector.isOnServer(), ServerDetector.SERVER_HOST));

		LOGGER.info("[520Pro] 菜单 Mod 加载完成，按 M 键打开菜单");
	}
}
