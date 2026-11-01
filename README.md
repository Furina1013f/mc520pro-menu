# 520Pro 服务器菜单（mc.520pro.top 专属）

MC **1.21.8** Fabric **客户端** mod —— 游戏内置**图形化**菜单。

作者：**Furina** ｜ 版本：见「关于」页（自动读取 `fabric.mod.json`） ｜ 仓库：<https://github.com/Furina1013f/mc520pro-menu>

## 核心规则

- 按 **M 键**（可在「控制设置 → 520Pro 服务器菜单」分类里改键）随时呼出菜单。
- 实时检测当前连接的服务器：
  - 连接 **mc.520pro.top** → 头部绿色徽章「已解锁」+ 品牌/延迟/在线人数，功能卡片高亮可用；
  - 其它服务器 / 单机 / 主菜单 → **界面照样可打开**，但所有功能卡片变暗+红锁图标，悬停提示「已锁定」。
- 点击功能的瞬间再校验一次服务器归属（双保险）。
- **权限自动探测**：服务器只把玩家有权使用的指令下发给客户端，mod 据此逐条判断可用性；无权限的卡片右上角标橙色「限」。

## 界面（v1.1.0 向导 · 权限 · 头衔 · 在线玩家 · 关于）

自绘面板 + 物品图标卡片，内容 = 金山文档《有什么指令》+ 已推断插件的标准指令补全，已用开发客户端实机截图验证：

- 顶部流动渐变横幅：像素爱心 + 粗体标题 `MC.520PRO.TOP` + 状态徽章（绿=已解锁 / 红=已锁定，带呼吸脉冲点）+ **头衔徽章**（自动探测：普通玩家 / VIP / 管理员，各自配色）
- **10 个分类页签**（图标 + 中文名，填满面板宽度）：传送 / 领地 / 市场 / MMO / 趣味 / 社交 / 经济 / 工具 / 防熊 / 全部
- **页签切换动画**：缓入缓出滑动 + 淡入淡出，选中光标在页签间插值滑动并带弹性超调；卡片按顺序错峰入场，滑动方向随切换方向反向
- **90+ 条指令卡片，每条带两行中文用途说明**：文档收录的正常显示；插件自带但文档未提的标**青色小点**（浮窗注明"插件推断指令"）
- **带参数指令 → 二级向导页**（不再让玩家复制粘贴）：
  - 自动从指令模板解析参数槽（`/tpa <玩家名>` → 玩家字段；`/speed <1-5>` → 数字字段 1~5；`/pay <玩家名> <金额>` → 玩家 + 数字）
  - 玩家名 **▼ 点选在线玩家**（滚轮翻页），数字用 **−/+** 调节（数值显示在输入框内右端，不压按钮）
  - 底部**实时预览**最终指令，点「**完成并发送**」或按 **Enter** 直接执行；Esc / 返回 回到菜单
  - 图标（▼ 三角、✔ 对勾）用像素绘制，不依赖字体符号
- **在线玩家列表页**（传送分类第一条）：一次列出**全部**在线玩家（名字 / 延迟 / 头衔显示名），每行右侧两个按钮——
  「传」= 直接发 `/tpa <名字>`，「召」= 直接发 `/tpahere <名字>`；点名字复制；自己排最前并高亮
- 卡片区**滚动 + 裁剪 + 滚动条**（滚轮/↑↓ 键），小窗口不截断；条目少的分类自动用 3 列，一屏放完
- 底部胶囊按钮：关于 / 复制信息 / 关闭
- **关于页**：作者 **Furina**、当前版本（读自 `fabric.mod.json`）、连接状态与头衔、完整更新日志（读自 `assets/mc520pro/changelog.txt`）、GitHub 与爱发电入口
- **指令清单页**：未连服时显示居中空状态说明并把无效按钮置灰，不再是空面板
- 自适应：宽度决定列数（3/2/1 列），页签宽度随数量收缩，<320 高度进紧凑模式

### 插件推断

见 [docs/PLUGIN_ANALYSIS.md](docs/PLUGIN_ANALYSIS.md)：CoreProtect（防熊）、Residence（领地）、mcMMO、ExMarket（全球市场）、EssentialsX（传送/帽子/经济/工具）、GSit（坐姿）、LuckPerms、Velocity 代理、国产举报/称号/在线奖励/红包/电梯插件。文档未提及的插件标准指令也已收录（标青色小点）。

### 头衔与权限是怎么判断的

1. **指令树特征**（主依据）：客户端收到的 DeclareCommands 只包含当前玩家有权用的指令。
   - 出现 `gamemode`/`op`/`deop`/`ban`/`gamerule` → 判为**管理员**
   - 出现 `fly`/`hat`/`nick`/`workbench`/`enderchest`/`speed` 但无管理指令 → 判为 **VIP**
   - 都没有 → **普通玩家**
2. **玩家列表显示名关键词**（辅助）：显示名含「管理/op/admin」→ 管理员；含「VIP/会员」→ VIP。
3. 结果显示在顶部徽章与关于页；指令可用性由指令树逐条判断，比头衔更准（权限组可能只开单条指令）。

### 开发自检（自动截图）

```bash
gradle runClient -Pautotest
```

会启动开发客户端 → 自动打开菜单各分类页、向导页（玩家/数字两种参数）、关于页、指令清单页 → 截图到 `run/screenshots/` 后退出。
仅在带 `-Pautotest` 时生效，正式游玩完全无影响。

> 自检会通过 `PermissionDetector.debugInject(...)` 注入一份典型指令树与 VIP 头衔，
> 用于在离线开发环境里验收「在线态」界面（头衔徽章、权限标、向导页）。
> 注入逻辑只在 `-Dmc520pro.autotest=true` 下可达，正式游戏无影响。

> 本机首次 `runClient` 若卡在 `downloadAssets`：先确认 `tools/connect_proxy.py` 在跑
> （Gradle 走 127.0.0.1:18080 代理），资源可直接从 PCL 的 `.minecraft/assets` 复制到
> `.gradle-home/caches/fabric-loom/assets/{objects,indexes}` 跳过下载。

## 用指令清单识别服务器插件

菜单 → 「全部指令」→ 「打开指令清单」→ 「复制全部」，把内容发出来即可分析。
本服（HAGo-Server）的完整推断结果已整理在 **[docs/PLUGIN_ANALYSIS.md](docs/PLUGIN_ANALYSIS.md)**。常见对应关系：

| 指令特征 | 对应插件 |
| --- | --- |
| `/home /sethome /tpa /spawn /warp /rtp /back /afk /msg /tpaccept` | EssentialsX / CMI / 类似基础插件 |
| `/res create /res padd /res expand` | Residence 领地 |
| `/mcstats /skills` | mcMMO |
| `/market open /exmarket sell /market box` | ExMarket 全球市场 |
| `/co i` | CoreProtect 防熊 |
| `/sit /lay /bellyflop /spin /crawl` | GSit |
| `/lp /luckperms` | LuckPerms 权限组 |
| `/rg /region /claim` | WorldGuard / GriefPrevention 领地 |
| `/mv /mvtp /multiverse` | Multiverse 多世界 |
| `/login /register /l /reg` | AuthMe 登录插件 |
| `/tt /tps /gc /timings` | 管理/监控指令 |
| `/skin /hat` | 趣味小插件 |
| `//wand /set /copy /paste` | WorldEdit |
| `/baltop /ah /pay /shop` | 经济/市场插件 |

## 安装

1. 客户端装 **Fabric Loader ≥ 0.16.9**（1.21.8）；
2. 装 **Fabric API 0.136.1+1.21.8**（或更新）；
3. 把 `mc520pro-menu-<版本>.jar` 丢进 `.minecraft/mods`；
4. 需要 Java 21+。

## 构建（开发）

```bash
JAVA_HOME=<JDK21> GRADLE_USER_HOME=./.gradle-home ./gradle-x/gradle-8.14.3/bin/gradle build
```

产物：`build/libs/mc520pro-menu-<版本>.jar`（另有 `-sources.jar`）。

> 本机说明：`maven.fabricmc.net` 解析到的 Cloudflare IP 被运营商屏蔽，首次拉依赖前需
> `python tools/connect_proxy.py` 起本地固定 IP 代理（配置在 `.gradle-home/gradle.properties`）。
> 依赖已缓存，日常增量构建不需要代理；**项目本身不含代理配置**，可直接在其他机器构建。

## 发版流程（版本号只改两处）

1. 改 `gradle.properties` 的 `mod_version`（如 `1.1.0`）——关于页显示的版本会自动同步
2. 在 `src/main/resources/assets/mc520pro/changelog.txt` 顶部追加本次更新条目
3. 构建并自检：

   ```bash
   gradle build
   gradle runClient -Pautotest     # 自动打开各页面截图到 run/screenshots/
   ```

4. 提交并发布到 GitHub：

   ```bash
   git add -A && git commit -m "feat(v1.1.0): ..."
   export GITHUB_TOKEN=<你的 PAT>     # 仅环境变量，不写入任何文件
   python tools/publish_github.py --repo mc520pro-menu --version 1.1.0 \
          --jar build/libs/mc520pro-menu-1.1.0.jar \
          --notes docs/RELEASE_NOTES_v1.1.0.md
   git push -u origin main && git push origin v1.1.0
   ```

> `tools/publish_github.py` 会自动：建仓库（已存在则跳过）→ 打 tag → 建 Release → 上传 jar 附件。
> 凭证只从环境变量读取，脚本与仓库都不含任何密钥。
> 授权方式二选一：
> - **PAT**（推荐）：<https://github.com/settings/tokens/new?scopes=repo>
> - **Device Flow**：`python tools/device_flow.py` → 按提示在 <https://github.com/login/device> 授权，无需密码

## 目录结构

```
mc520pro-mod/
├── build.gradle / settings.gradle / gradle.properties
├── docs/
│   ├── PLUGIN_ANALYSIS.md          # 插件推断 + 完整指令手册（每条含用途）
│   ├── VISUAL_REVIEW.md            # 界面视觉审查报告
│   └── RELEASE_NOTES_v1.1.0.md     # Release 说明
├── src/main/java/top/mc520pro/menu/
│   ├── MC520ProMenuClient.java    # 入口：按键注册 + 连接日志
│   ├── ServerDetector.java        # 服务器归属检测 + 信息聚合
│   ├── PermissionDetector.java    # 权限/头衔探测 + 指令树解析 + 在线玩家
│   ├── ModInfo.java               # 版本/作者/主页（读 fabric.mod.json）
│   ├── AutoTest.java              # 开发自检（-Pautotest 才生效）
│   ├── data/CommandCatalog.java   # 分类指令目录（文档整理，含用途）
│   └── screen/
│       ├── MainMenuScreen.java    # 图形化主菜单（分类页签 + 卡片 + 动画）
│       ├── CommandWizardScreen.java # 带参数指令的二级向导页
│       ├── OnlinePlayersScreen.java # 在线玩家列表页
│       ├── CommandsScreen.java    # 服务器指令清单（分页）
│       ├── AboutScreen.java       # 关于页
│       └── Changelog.java         # 更新日志读取
├── src/main/resources/            # fabric.mod.json / 语言文件 / 图标 / changelog.txt
└── tools/                         # 构建辅助（图标、代理、探测、发布、截图分析）
```

## 已知限制

- 归属按**连接域名**匹配（`mc.520pro.top`，忽略大小写/端口）；纯 IP 直连会被判定为"非专属服务器"。
- 自定义背景贴图需要走资源包/图集方案，当前用原版毛玻璃 + 自绘面板 + 原版物品图标（不需要额外资源）。

## 后续完善（计划）

- [x] 指令清单页做成可点击执行与分类
- [x] 带参数指令的二级向导页
- [x] 在线玩家列表页
- [ ] 按钮/命令可配置化（`config/mc520pro.json`）
- [ ] 与服务端插件联动（签到 / 传送点 / 领地 / 经济面板），需配套服务端插件
- [ ] 纯 IP 直连白名单、ModMenu 集成
- [ ] 自定义背景图 / Logo（资源包或图集方式）
- [ ] NeoForge 版本（如玩家端使用 NeoForge）
