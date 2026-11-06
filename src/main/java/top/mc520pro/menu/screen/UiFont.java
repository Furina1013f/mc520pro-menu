package top.mc520pro.menu.screen;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;

/**
 * 自定义 UI 字体：等线（Deng）位图字体，平滑无像素感。
 * 通过反射从 FontManager 拿 FontStorage map，用自定义字体 ID 构造独立 TextRenderer。
 * 任何异常时回退到原版 textRenderer（不炸游戏）。
 */
public final class UiFont {
	public static final Identifier FONT_ID = Identifier.of("mc520pro", "dengx");

	private static volatile TextRenderer custom;
	private static volatile boolean attempted;

	private UiFont() {
	}

	/** 获取自定义字体渲染器；不可用时返回原版渲染器（永不返回 null）。 */
	public static TextRenderer tr() {
		if (custom != null) {
			return custom;
		}
		if (attempted) {
			return MinecraftClient.getInstance().textRenderer;
		}
		attempted = true;
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			// 反射拿 private fontManager 字段
			Field fmField = MinecraftClient.class.getDeclaredField("fontManager");
			fmField.setAccessible(true);
			FontManager fm = (FontManager) fmField.get(client);
			Field f = FontManager.class.getDeclaredField("fontStorages");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<Identifier, FontStorage> storages = (Map<Identifier, FontStorage>) f.get(fm);
			Function<Identifier, FontStorage> getter = id -> {
				FontStorage s = storages.get(id);
				if (s != null) {
					return s;
				}
				// 触发懒加载：调 FontManager 的 getStorage
				try {
					java.lang.reflect.Method m = FontManager.class.getDeclaredMethod("getStorage", Identifier.class);
					m.setAccessible(true);
					return (FontStorage) m.invoke(fm, id);
				} catch (Throwable e) {
					return s;
				}
			};
			custom = new TextRenderer(getter, false);
		} catch (Throwable e) {
			custom = null;
		}
		return custom != null ? custom : MinecraftClient.getInstance().textRenderer;
	}

	/** 是否已成功创建自定义渲染器。 */
	public static boolean isCustom() {
		return custom != null;
	}
}
