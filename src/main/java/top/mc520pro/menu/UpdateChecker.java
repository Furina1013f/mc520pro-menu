package top.mc520pro.menu;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.text.Text;

/**
 * 版本更新检测：查询 GitHub Releases API，与当前版本比较。
 * 结果缓存 1 小时，避免频繁请求。
 */
public final class UpdateChecker {
	/** 仓库地址（与 ModInfo.GITHUB_URL 对应）。 */
	public static final String REPO = "Furina1013f/mc520pro-menu";
	/** 查询间隔：1 小时。 */
	private static final long CACHE_MS = 3600_000L;

	/** 更新检测结果。 */
	public record Result(boolean upToDate, String currentVersion, String latestVersion,
			String releaseUrl, String publishedAt, String notes, long checkedAt) {

		public boolean hasUpdate() {
			return !upToDate;
		}

		public boolean isChecking() {
			return currentVersion == null && latestVersion == null;
		}

		public String latestOrCurrent() {
			return latestVersion != null ? latestVersion : currentVersion;
		}
	}

	private static volatile Result cached;
	private static volatile Result failed;
	private static volatile boolean refreshing;

	private UpdateChecker() {
	}

	/** 当前版本（读自 fabric.mod.json）。 */
	public static String currentVersion() {
		return ModInfo.version();
	}

	/** 获取缓存的检查结果（不发起网络请求）。 */
	public static Result get() {
		return cached;
	}

	/** 上次检查是否失败。 */
	public static Result getFailed() {
		return failed;
	}

	/** 是否正在查询（尚未拿到结果）。 */
	public static boolean checking() {
		return cached == null && refreshing;
	}

	/**
	 * 检查更新。立即返回缓存结果（可能为 null），并在后台异步查询。
	 * 已有缓存且未过期时不重复请求。
	 */
	public static void checkAsync() {
		if (refreshing) {
			return;
		}
		if (cached != null && System.currentTimeMillis() - cached.checkedAt() < CACHE_MS) {
			return;
		}
		refreshing = true;
		Thread t = new Thread(UpdateChecker::fetch, "mc520pro-update-check");
		t.setDaemon(true);
		t.start();
	}

	private static void fetch() {
		try {
			String latest = latestReleaseTag();
			if (latest == null) {
				failed = new Result(true, currentVersion(), null, null, null, null,
						System.currentTimeMillis());
				return;
			}
			String cur = currentVersion();
			boolean upToDate = compareVersions(cur, latest) >= 0;
			cached = new Result(upToDate, cur, latest,
					"https://github.com/" + REPO + "/releases/tag/" + latest, null, null,
					System.currentTimeMillis());
		} catch (Throwable e) {
			failed = new Result(true, currentVersion(), null, null, null, null,
					System.currentTimeMillis());
		} finally {
			refreshing = false;
		}
	}

	/** 从 API 获取最新 release tag。 */
	private static String latestReleaseTag() {
		String url = "https://api.github.com/repos/" + REPO + "/releases/latest";
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestProperty("Accept", "application/vnd.github+json");
			conn.setRequestProperty("User-Agent", "mc520pro-menu");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(15000);
			int code = conn.getResponseCode();
			if (code != 200) {
				return null;
			}
			String body = readAll(conn.getInputStream());
			Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
			if (m.find()) {
				return m.group(1).replaceFirst("^v", "");
			}
			return null;
		} catch (Throwable e) {
			return null;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static String readAll(InputStream in) throws Exception {
		if (in == null) {
			return "";
		}
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(in, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line).append('\n');
			}
			return sb.toString();
		}
	}

	/**
	 * 版本比较：1.2.0 > 1.1.9。返回正数表示 a 更新。
	 */
	public static int compareVersions(String a, String b) {
		if (a == null || b == null) {
			return a == b ? 0 : (a == null ? -1 : 1);
		}
		String[] pa = a.replaceFirst("^v", "").trim().split("[.\\-+]");
		String[] pb = b.replaceFirst("^v", "").trim().split("[.\\-+]");
		int len = Math.max(pa.length, pb.length);
		for (int i = 0; i < len; i++) {
			int va = i < pa.length ? parseNum(pa[i]) : 0;
			int vb = i < pb.length ? parseNum(pb[i]) : 0;
			if (va != vb) {
				return Integer.compare(va, vb);
			}
		}
		return 0;
	}

	private static int parseNum(String s) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			// 非数字段（如 build 号）按 0 处理
			return 0;
		}
	}

	/** 生成一行描述：当前版本 / 最新版本 / 状态。 */
	public static Text statusLine() {
		Result r = cached != null ? cached : failed;
		if (r == null) {
			return Text.literal("检查更新");
		}
		if (r.latestVersion() == null) {
			return Text.literal("无法连接更新源");
		}
		if (r.upToDate()) {
			return Text.literal("已是最新版本 v" + r.currentVersion());
		}
		return Text.literal("发现新版本 v" + r.latestVersion() + "（当前 v" + r.currentVersion() + "）");
	}
}
