package top.mc520pro.menu.screen;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 更新日志：读取资源内嵌的 {@code assets/mc520pro/changelog.txt}。
 * 每行格式：
 * <ul>
 * <li>{@code v1.2.0 | 说明} → 版本行（加粗显示）</li>
 * <li>{@code - 说明} → 缩进条目</li>
 * </ul>
 * 版本发布时只需改这个资源文件 + gradle.properties 的 mod_version。
 */
public final class Changelog {
	private Changelog() {
	}

	/** 读取更新日志条目；每项是 {版本/空, 说明}。 */
	public static List<String[]> entries() {
		List<String[]> out = new ArrayList<>();
		try (InputStream in = Changelog.class.getResourceAsStream("/assets/mc520pro/changelog.txt")) {
			if (in == null) {
				return out;
			}
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty()) {
						continue;
					}
					if (line.startsWith("v") && line.contains("|")) {
						int i = line.indexOf('|');
						out.add(new String[] { line.substring(0, i).trim(), line.substring(i + 1).trim() });
					} else {
						String text = line.startsWith("- ") ? line.substring(2).trim() : line;
						out.add(new String[] { "", "· " + text });
					}
				}
			}
		} catch (Throwable ignored) {
		}
		if (out.isEmpty()) {
			out.add(new String[] { "", "· 暂无更新日志" });
		}
		return out;
	}
}
