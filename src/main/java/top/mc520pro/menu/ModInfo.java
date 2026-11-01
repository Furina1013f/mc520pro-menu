package top.mc520pro.menu;

import java.util.Optional;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;

/**
 * Mod 元数据（版本 / 作者 / 主页 / 描述），从 fabric.mod.json 读取，
 * 避免与构建版本号不同步。
 */
public final class ModInfo {
	public static final String MOD_ID = "mc520pro";
	/** 仓库地址（GitHub）。 */
	public static final String GITHUB_URL = "https://github.com/Furina1013f/mc520pro-menu";
	/** 爱发电（待开通后填具体主页）。 */
	public static final String AFDIAN_URL = "";
	/** 作者显示名。 */
	public static final String AUTHOR = "Furina";

	private ModInfo() {
	}

	/** 读取 mod 版本号（如 1.1.0）。 */
	public static String version() {
		try {
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
			if (container.isPresent()) {
				return container.get().getMetadata().getVersion().getFriendlyString();
			}
		} catch (Throwable ignored) {
		}
		return "1.0.0";
	}

	/** 读取作者列表。 */
	public static String authors() {
		try {
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
			if (container.isPresent()) {
				StringBuilder sb = new StringBuilder();
				for (net.fabricmc.loader.api.metadata.Person a : container.get().getMetadata().getAuthors()) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(a.getName());
				}
				if (sb.length() > 0) {
					return sb.toString();
				}
			}
		} catch (Throwable ignored) {
		}
		return AUTHOR;
	}

	/** 读取自定义元数据里的主页地址（fabric.mod.json 的 contact.homepage）。 */
	public static String homepage() {
		try {
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
			if (container.isPresent()) {
				CustomValue contact = container.get().getMetadata().getCustomValue("contact");
				if (contact != null && contact.getAsObject() != null) {
					CustomValue home = contact.getAsObject().get("homepage");
					if (home != null && home.getAsString() != null) {
						return home.getAsString();
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return GITHUB_URL;
	}
}
