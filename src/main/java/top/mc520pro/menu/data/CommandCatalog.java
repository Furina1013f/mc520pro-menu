package top.mc520pro.menu.data;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * 服务器指令目录（HAGo-Server / mc.520pro.top）。
 * 来源两类：
 * 1) 金山文档《有什么指令》明确收录的（fromDoc = true）；
 * 2) 文档未提及、但按已推断插件（EssentialsX / Residence / mcMMO / ExMarket / GSit /
 *    CoreProtect 等）标准功能补全的（fromDoc = false，界面标青色小点）。
 */
public final class CommandCatalog {
	/** 点击行为：直接执行 / 复制到剪贴板（需要补参数）/ 特殊动作。 */
	public enum Mode {
		RUN, COPY, CUSTOM
	}

	public enum Custom {
		NONE, OPEN_CMDS, OPEN_ABOUT, OPEN_PLAYERS, COPY_COORDS, COPY_INFO, COPY_DOC, COPY_ELEVATOR
	}

	public record Entry(String name, String command, String usage, ItemStack icon, Mode mode, Custom custom,
			boolean fromDoc) {
		static Entry run(String name, String command, String usage, Item icon) {
			return new Entry(name, command, usage, new ItemStack(icon), Mode.RUN, Custom.NONE, true);
		}

		static Entry runInf(String name, String command, String usage, Item icon) {
			return new Entry(name, command, usage, new ItemStack(icon), Mode.RUN, Custom.NONE, false);
		}

		static Entry copy(String name, String command, String usage, Item icon) {
			return new Entry(name, command, usage, new ItemStack(icon), Mode.COPY, Custom.NONE, true);
		}

		static Entry copyInf(String name, String command, String usage, Item icon) {
			return new Entry(name, command, usage, new ItemStack(icon), Mode.COPY, Custom.NONE, false);
		}

		static Entry custom(String name, String usage, Item icon, Custom action) {
			return new Entry(name, "", usage, new ItemStack(icon), Mode.CUSTOM, action, true);
		}
	}

	public record Category(String title, String desc, ItemStack icon, List<Entry> entries) {
	}

	// ---------- 传送 ----------
	public static final Category TP = new Category("传送", "主城 / 家 / 玩家互传 / 随机", new ItemStack(Items.COMPASS), List.of(
			Entry.custom("在线玩家", "一次列出全部在线玩家，每行可一键「传送到他」或「把他传过来」", Items.PLAYER_HEAD, Custom.OPEN_PLAYERS),
			Entry.run("回主城", "spawn", "传送到服务器主城集合点", Items.BEACON),
			Entry.run("回家", "home", "传送到你用 /sethome 设置的家", Items.OAK_DOOR),
			Entry.copyInf("设置家", "sethome [名字]", "在当前地点设置一个家，可设多个", Items.RED_BED),
			Entry.copyInf("删除家", "delhome <名字>", "删除指定的家", Items.LAVA_BUCKET),
			Entry.runInf("家的列表", "homes", "查看你设置的所有家", Items.BOOK),
			Entry.run("回到原点", "back", "返回死亡或被传送前的位置", Items.ENDER_PEARL),
			Entry.run("随机传送", "rtp", "随机传送到野外未探索区域", Items.COMPASS),
			Entry.copy("传送到玩家", "tpa <玩家名>", "传送到对方处，对方需在聊天栏点击接受（也可在「在线玩家」页一键发送）", Items.LEAD),
			Entry.copy("召唤玩家", "tpahere <玩家名>", "让对方传送到你处，需对方接受（也可在「在线玩家」页一键发送）", Items.SPYGLASS),
			Entry.runInf("接受传送", "tpaccept", "接受对方发来的传送请求", Items.EMERALD),
			Entry.runInf("拒绝传送", "tpdeny", "拒绝对方发来的传送请求", Items.RED_DYE),
			Entry.runInf("取消请求", "tpacancel", "取消自己发出的所有传送请求", Items.CLOCK),
			Entry.copyInf("公共传送点", "warp <传送点名>", "传送到服务器设置的公共传送点", Items.ENDER_EYE),
			Entry.runInf("传送点列表", "warps", "查看服务器所有公共传送点", Items.OAK_SIGN),
			Entry.run("飞行", "fly", "开关创造式飞行（需 VIP 权限组）", Items.FEATHER),
			Entry.copyInf("移动速度", "speed <1-5>", "调整飞行/行走移动速度（需权限）", Items.RABBIT_FOOT),
			Entry.runInf("向上跳", "jump", "向准星方向跳跃一格（有穿墙检查）", Items.SUGAR),
			Entry.runInf("世界顶部", "top", "传送到当前世界最高点", Items.DIAMOND_BLOCK),
			Entry.runInf("世界底部", "bottom", "传送到脚下基岩层", Items.DEEPSLATE),
			Entry.run("脱困传送", "res unstuck", "在他人领地内被陷阱困住时脱困", Items.CHAIN)));

	// ---------- 领地 ----------
	public static final Category RES = new Category("领地", "Residence 领地管理（木锄选区）", new ItemStack(Items.OAK_SIGN), List.of(
			Entry.run("领地总览", "res", "查看全部领地功能与帮助", Items.BOOK),
			Entry.copy("创建领地", "res create <领地名称>", "用木锄左/右键选定区域后创建，约 0.40/m³", Items.WOODEN_HOE),
			Entry.copy("删除领地", "res remove <领地名称>", "删除指定领地并退还部分费用", Items.LAVA_BUCKET),
			Entry.copy("重命名领地", "res rename <旧名称> <新名称>", "给领地改名字", Items.NAME_TAG),
			Entry.copy("添加成员", "res padd <玩家名>", "给玩家开通本领地成员权限", Items.GREEN_DYE),
			Entry.copy("移除成员", "res pdel <玩家名>", "删除玩家的领地权限", Items.RED_DYE),
			Entry.copy("细分权限", "res pset <玩家名>", "设定玩家的特定开关权限（建造/交互等）", Items.COMPARATOR),
			Entry.copy("扩大领地", "res expand <距离>", "站在领地内朝目标方向看，向该方向扩大", Items.STICKY_PISTON),
			Entry.copy("缩小领地", "res contract <距离>", "站在领地内朝目标方向看，向该方向缩小", Items.PISTON),
			Entry.copyInf("传送到领地", "res tp <领地名称>", "直接传送到自己的领地", Items.CHORUS_FRUIT),
			Entry.copyInf("领地信息", "res info <领地名称>", "查看领地详情（主人/成员/权限）", Items.GRAY_DYE),
			Entry.runInf("我的领地", "res list", "查看自己拥有的所有领地", Items.PAPER),
			Entry.runInf("公共领地", "res listpublic", "查看服务器公共领地列表", Items.OAK_SIGN),
			Entry.copyInf("领地开关", "res set <选项 值>", "设置领地内开关（防怪/火焰/苦力怕等）", Items.REPEATER),
			Entry.copyInf("领地旗标", "res flag <旗标 值>", "细粒度权限旗标（build/use/container 等）", Items.REDSTONE),
			Entry.copyInf("子区域", "res subzone <子区域名>", "在领地内划出子区域单独授权", Items.SLIME_BLOCK)));

	// ---------- 全球市场 ----------
	public static final Category MARKET = new Category("市场", "ExMarket 跨服拍卖行", new ItemStack(Items.EMERALD), List.of(
			Entry.run("打开市场", "market open", "打开全球市场，浏览/购买跨服挂单", Items.CHEST),
			Entry.copy("上架物品", "exmarket sell [价格]", "把手上持有的物品挂到全球市场", Items.GOLD_INGOT),
			Entry.run("市场暂存盒", "market box", "购买成功或下架返还的物品都在这里领取", Items.HOPPER),
			Entry.copyInf("搜索市场", "market search <关键词>", "按名称搜索市场挂单", Items.COMPASS),
			Entry.copyInf("查看行情", "market price <物品>", "查看某物品的市场参考价", Items.DIAMOND),
			Entry.runInf("取消挂单", "market cancel", "取消自己的挂单（物品退回暂存盒）", Items.BARRIER),
			Entry.runInf("领取物品", "market collect", "从暂存盒领取物品", Items.BARREL)));

	// ---------- MMO ----------
	public static final Category MMO = new Category("MMO", "mcMMO 角色成长", new ItemStack(Items.DIAMOND_SWORD), List.of(
			Entry.run("技能总览", "mcstats", "查看各项技能等级与经验总览", Items.PAPER),
			Entry.run("技能选择", "skills", "打开子技能选择界面", Items.DIAMOND_SWORD),
			Entry.copyInf("技能排行", "mctop <技能|all>", "查看某技能或综合排行榜", Items.DIAMOND),
			Entry.runInf("我的排名", "mcrank", "查看自己在各技能中的名次", Items.EXPERIENCE_BOTTLE),
			Entry.runInf("加成查看", "mcboost", "查看当前经验/掉落加成状态", Items.GOLDEN_CARROT),
			Entry.runInf("技能冷却", "mccooldown", "查看技能与物品冷却时间", Items.CLOCK),
			Entry.copyInf("物品鉴定", "mcinspect <物品>", "查看手持/指定物品的属性加成", Items.GLASS_BOTTLE),
			Entry.runInf("组队系统", "parties", "mcMMO 组队：邀请/共享经验", Items.BELL),
			Entry.copyInf("经验提示", "togglexp <技能>", "开关某技能的经验获取提示", Items.EXPERIENCE_BOTTLE)));

	// ---------- 趣味动作 ----------
	public static final Category FUN = new Category("趣味", "GSit 坐姿 / 帽子 / 红包", new ItemStack(Items.ARMOR_STAND), List.of(
			Entry.run("坐下", "sit", "坐在准星所指方块上（也可空手右键楼梯/台阶/地毯）", Items.OAK_STAIRS),
			Entry.run("躺下", "lay", "躺在准星所指方块上", Items.WHITE_CARPET),
			Entry.run("俯冲", "bellyflop", "在准星所指方块上肚皮跳", Items.SLIME_BALL),
			Entry.run("旋转", "spin", "在准星所指方块上转圈圈", Items.NOTE_BLOCK),
			Entry.run("爬行", "crawl", "进入爬行状态，钻低缝", Items.RABBIT_HIDE),
			Entry.run("戴帽子", "hat", "把手上的物品戴到头上（需单个物品）", Items.CHAINMAIL_HELMET),
			Entry.run("红包菜单", "hongbao open", "打开红包菜单：发红包 / 抢红包", Items.FIREWORK_ROCKET),
			Entry.custom("电梯玩法", "合成：8 羊毛 + 1 末影珍珠 = 电梯方块；至少 2 块、垂直间距≥2 格；站上去 [跳跃]上楼 [下蹲]下楼", Items.CHEST_MINECART, Custom.COPY_ELEVATOR)));

	// ---------- 社交 ----------
	public static final Category SOCIAL = new Category("社交", "举报 / 称号 / 私聊 / 福利", new ItemStack(Items.PLAYER_HEAD), List.of(
			Entry.copy("举报(推荐)", "rp <玩家名> [理由]", "举报外挂/言语/熊孩子/神权，文档更建议使用", Items.IRON_BARS),
			Entry.copy("举报", "report <玩家名> [理由]", "举报（同 /rp）", Items.WRITTEN_BOOK),
			Entry.copy("举报(拼音)", "jubao <玩家名> [理由]", "举报（拼音别名，同 /rp）", Items.PAPER),
			Entry.run("称号", "tags", "打开称号 GUI，选择/更换自己的称号", Items.NAME_TAG),
			Entry.run("在线奖励", "playtime", "在线时间奖励 GUI：5 级后每级有限时死亡不掉落等福利", Items.CLOCK),
			Entry.run("在线玩家", "list", "查看当前在线玩家名单", Items.PLAYER_HEAD),
			Entry.run("挂机状态", "afk", "标记挂机，免被传送打扰", Items.HAY_BLOCK),
			Entry.copyInf("私聊", "msg <玩家名> <消息>", "发送私聊消息", Items.PAPER),
			Entry.copyInf("回复私聊", "r <消息>", "回复最近一条私聊", Items.BOOK),
			Entry.copyInf("改昵称", "nick <新昵称>", "修改自己的显示昵称（需权限）", Items.PAINTING),
			Entry.copyInf("动作消息", "me <动作内容>", "以动作形式发言（* 玩家 做了某事）", Items.ARMOR_STAND),
			Entry.copyInf("玩家查询", "seen <玩家名>", "查看玩家最后上线时间与信息", Items.SPYGLASS),
			Entry.runInf("在线详情", "who", "查看在线玩家及其所在世界", Items.PLAYER_HEAD)));

	// ---------- 经济 ----------
	public static final Category ECON = new Category("经济", "EssentialsX 金币 / 礼包", new ItemStack(Items.GOLD_INGOT), List.of(
			Entry.runInf("查询余额", "balance", "查看自己的金钱余额", Items.GOLD_NUGGET),
			Entry.copyInf("转账", "pay <玩家名> <金额>", "给玩家转账金币", Items.GOLD_INGOT),
			Entry.runInf("富豪榜", "baltop", "查看服务器财富排行榜", Items.GOLD_BLOCK),
			Entry.copyInf("领取礼包", "kit <礼包名>", "领取新手/活动礼包", Items.BUNDLE),
			Entry.runInf("礼包列表", "kits", "查看当前可领取的礼包", Items.CHEST)));

	// ---------- 便捷工具 ----------
	public static final Category TOOLS = new Category("工具", "EssentialsX 随身功能", new ItemStack(Items.CRAFTING_TABLE), List.of(
			Entry.runInf("合成台", "workbench", "随时打开一个临时合成台", Items.CRAFTING_TABLE),
			Entry.runInf("末影箱", "enderchest", "远程打开自己的末影箱", Items.ENDER_CHEST),
			Entry.runInf("背包整理", "chest", "打开自己的背包箱子界面", Items.BARREL),
			Entry.runInf("铁砧", "anvil", "打开临时铁砧界面", Items.ANVIL),
			Entry.runInf("砂轮", "grindstone", "打开临时砂轮（拆附魔返材料）", Items.GRINDSTONE),
			Entry.runInf("切石机", "stonecutter", "打开临时切石机", Items.STONECUTTER),
			Entry.runInf("治疗", "heal", "恢复生命值（可能消耗金币/有冷却）", Items.GOLDEN_APPLE),
			Entry.runInf("饱食", "feed", "立即恢复饥饿值（可能消耗金币）", Items.BREAD),
			Entry.runInf("修复装备", "repair", "修复手持装备耐久（可能收费）", Items.DIAMOND)));

	// ---------- 防熊 ----------
	public static final Category GUARD = new Category("防熊", "CoreProtect 方块追溯", new ItemStack(Items.SHIELD), List.of(
			Entry.run("方块检查", "co i", "开启检查模式：左键查谁挖掘过方块，右键查谁交互过", Items.GRAY_DYE),
			Entry.copyInf("记录查询", "co lookup <参数>", "按玩家/方块/时间查询修改记录（需权限）", Items.WRITABLE_BOOK),
			Entry.copyInf("回滚操作", "co rollback <玩家/半径>", "回滚某玩家的破坏（需管理权限）", Items.TNT),
			Entry.copyInf("恢复操作", "co restore <参数>", "恢复被回滚的内容（需管理权限）", Items.TOTEM_OF_UNDYING),
			Entry.copyInf("破坏统计", "co counter <坐标 半径>", "统计区域内各方块被破坏数量（需管理权限）", Items.LIME_DYE),
			Entry.custom("复制坐标", "复制当前 XYZ 坐标与维度到剪贴板", Items.MAP, Custom.COPY_COORDS),
			Entry.custom("复制服务器信息", "地址 / 版本 / 延迟 / 人数 一键复制", Items.WRITTEN_BOOK, Custom.COPY_INFO),
			Entry.custom("指令文档", "复制金山文档《有什么指令》链接", Items.PAPER, Custom.COPY_DOC)));

	// ---------- 全部 ----------
	public static final Category ALL = new Category("全部", "完整指令树 / 关于本菜单", new ItemStack(Items.BOOK), List.of(
			Entry.custom("打开指令清单", "浏览服务器全部指令（约 168 条），点击复制", Items.BOOKSHELF, Custom.OPEN_CMDS),
			Entry.custom("关于", "作者 Furina · 版本信息 · 更新日志 · GitHub / 爱发电", Items.NAME_TAG, Custom.OPEN_ABOUT)));

	public static final List<Category> CATEGORIES = List.of(TP, RES, MARKET, MMO, FUN, SOCIAL, ECON, TOOLS, GUARD, ALL);

	private CommandCatalog() {
	}
}
