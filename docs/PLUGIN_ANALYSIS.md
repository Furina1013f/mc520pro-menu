# mc.520pro.top（HAGo-Server）插件 / Mod 推断与指令手册

> 依据：金山文档《有什么指令》长截图（2026-07-02 更新）+ 服务器状态探测 + 客户端指令树（168 条）。
> 服务器架构：**Velocity 代理**（status 探测 `version.name = "Velocity 1.7.2-1.21.11"`）+ **Paper 系后端**（`modinfo: none`，非 Forge/Fabric 服务端）。

## 一、插件推断总表

| 文档条目 | 指令 | 推断插件 / Mod | 依据 |
| --- | --- | --- | --- |
| 防熊 | `/co i` | **CoreProtect** | `/co inspect` 是其标志性指令：左键查方块挖掘者、右键查交互记录 |
| 举报 | `/rp`、`/report`、`/jubao` | **国产举报插件**（如 ReportSystem / 自研，接 QQ 群机器人） | 三别名同义；分类"外挂/言语/熊孩子/神权"是中文服典型模板 |
| 称号 | `/tags` | **TagAPI / PlayerTags 类称号插件** | 打开称号 GUI |
| 在线时间奖励 | `/playtime` | **在线奖励插件**（PlayTime / OnlineReward 类） | GUI 领取，5 级后每级有"限时死亡不掉落"等福利 |
| 领地 | `/res` 全家桶 | **Residence** | `/res create/padd/pdel/pset/expand/contract/unstuck` 完全匹配 Residence 指令树；"木锄选区、按体积计价"是 Residence 经典玩法 |
| MMO | `/mcstats`、`/skills` | **mcMMO** | 两条均为 mcMMO 标准指令 |
| 全球市场 | `/market open`、`/market box`、`/exmarket sell` | **ExMarket** | `/exmarket` 前缀即该插件；跨服全局市场（配合 Velocity 多后端），暂存盒是其特征 |
| 玩家间传送 | `/tpa`、`/tpahere` | **EssentialsX**（或 CMI） | 聊天点击接受是 EssentialsX 默认行为 |
| 帽子 | `/hat` | **EssentialsX** | 内置 `/hat`（手上物品放头上） |
| 基础传送 | `/spawn`、`/home`、`/back`、`/rtp`、`/afk`、`/list` | **EssentialsX**（+ EssentialsX RTP 扩展） | 全套标准指令；`/rtp` 需要 RTP 附加插件 |
| 飞行 | 飞行 | **EssentialsX `/fly`**（VIP 权限）或专用飞行插件 | 文档列在"有意思的东西"，无独立指令 → 大概率权限组开放 `/fly` |
| 坐 GSit | `/sit`、`/lay`、`/bellyflop`、`/spin`、`/crawl` | **GSit**（服务端插件，客户端无需 mod） | 五条指令组合是 GSit 独有签名；空手右键楼梯/台阶/地毯/玩家可坐 |
| 红包 | `/hongbao open` | **红包插件**（RedPacket / 中文红包类） | 拼音指令是国产插件特征 |
| 电梯 | 合成 + 跳跃/下蹲 | **电梯类插件**（Elevator/QuickTeleport 类）+ 自定义合成 | 8 羊毛+1 末影珍珠合成"电梯方块"，站在上面跳跃上楼、下蹲下楼 |
| 代理/组网 | — | **Velocity** | status ping 直接暴露 |
| 权限 | — | **LuckPerms**（几乎必然） | VIP 飞行、称号、领地数量等分组权限管理标配 |

**客户端侧**：文档未要求任何客户端 mod，全部功能服务端实现（GSit 坐姿对原版客户端可见）。玩家自装的 Sodium/Iris/CustomSkinLoader/Jade 等与本服功能无耦合。

## 二、完整指令手册（文档提取 + 用途）

### 防熊（CoreProtect）
| 指令 | 用途 |
| --- | --- |
| `/co i` | 开启方块检查模式：左键查看谁挖掘过该方块，右键查看谁交互过（开箱/放置等） |

### 举报
| 指令 | 用途 |
| --- | --- |
| `/rp` | 举报（文档推荐，更建议使用） |
| `/report` | 举报（同 `/rp`） |
| `/jubao` | 举报（拼音别名） |

可举报项目：外挂（杀戮光环/飞行/加速等）、言语（辱骂/人身攻击/骚扰）、熊孩子（拆家/偷盗/恶意破坏）、神权（管理人员滥用职权）、其他（需管理员手动审查）。

### 称号 / 在线奖励
| 指令 | 用途 |
| --- | --- |
| `/tags` | 打开称号 GUI，选择/更换称号 |
| `/playtime` | 在线时间奖励 GUI；5 级后每级有"限时死亡不掉落"等福利 |

### 领地（Residence）——约 0.40/m³，木锄选区
| 指令 | 用途 |
| --- | --- |
| `/res` | 查看全部领地功能/帮助 |
| `/res create <领地名称>` | 创建领地（先用木锄左键右键选定区域） |
| `/res remove <领地名称>` | 删除领地 |
| `/res rename <旧名称> <新名称>` | 领地重命名 |
| `/res padd <玩家名>` | 给玩家添加领地成员权限 |
| `/res pdel <玩家名>` | 删除玩家领地权限 |
| `/res pset <玩家名>` | 设定玩家的特定权限（开关式） |
| `/res expand <距离>` | 站在领地内朝目标方向，向该方向扩大领地 |
| `/res contract <距离>` | 站在领地内朝目标方向，向该方向缩小领地 |
| `/res unstuck` | 脱困：在别人的领地里被陷阱困住时传出 |

### MMO（mcMMO）
| 指令 | 用途 |
| --- | --- |
| `/mcstats` | 查看技能信息总览（各技能等级/经验） |
| `/skills` | 打开子技能选择界面 |

### 全球市场（ExMarket）
| 指令 | 用途 |
| --- | --- |
| `/market open` | 打开全球市场（跨服拍卖行） |
| `/exmarket sell [价格]` | 把当前手持物品上架 |
| `/market box` | 市场暂存盒：购买到的或下架返还的物品在这里 |

### 玩家间传送（EssentialsX）
| 指令 | 用途 |
| --- | --- |
| `/tpa <玩家名>` | 传送到对方（对方聊天栏点击"接受"） |
| `/tpahere <玩家名>` | 让对方传送到自己（同样需接受） |

### 趣味动作（GSit / Hat）
| 指令 | 用途 |
| --- | --- |
| `/hat` | 把手上物品戴到头上（需单个物品） |
| `/sit` | 坐在准星所指方块上（也可空手右键楼梯/台阶/地毯） |
| `/lay` | 躺在准星所指方块上 |
| `/bellyflop` | 在准星所指方块上俯冲（肚皮跳） |
| `/spin` | 在准星所指方块上旋转 |
| `/crawl` | 进入爬行状态 |

### 红包
| 指令 | 用途 |
| --- | --- |
| `/hongbao open` | 打开红包菜单（发/抢） |

### 电梯（自定义合成）
- 合成：**任意颜色羊毛 ×8 + 末影珍珠 ×1 → 电梯方块**
- 至少 2 块电梯方块才能用，两块垂直间距 ≥ 2 格
- 站在电梯方块上：**跳跃=上楼，下蹲=下楼**（在相邻电梯方块间传送）

### 基础（EssentialsX）
| 指令 | 用途 |
| --- | --- |
| `/spawn` | 回主城 |
| `/home` | 回设置的的家 |
| `/back` | 回死亡/传送前位置 |
| `/rtp` | 随机传送 |
| `/afk` | 挂机状态 |
| `/list` | 在线玩家 |
| `/fly` | 飞行（需 VIP 权限组） |

## 三、文档未提及、但插件自带的标准指令（推断补全）

以下指令文档里没写，但按已推断出的插件几乎必然存在（以服务器实际指令树为准，菜单里标青色小点）。若某条在服里不可用，属于权限未开或插件配置关闭。

### EssentialsX 基础
| 指令 | 用途 |
| --- | --- |
| `/sethome [名字]` | 在当前地点设置家（可多个） |
| `/delhome <名字>` / `/homes` | 删除家 / 查看家列表 |
| `/warp <点名>` / `/warps` | 传送到公共传送点 / 查看列表 |
| `/tpaccept` / `/tpdeny` / `/tpacancel` | 接受 / 拒绝 / 取消传送请求 |
| `/speed <1-5>` | 调整移动速度（需权限） |
| `/jump` / `/top` / `/bottom` | 跳一格 / 传送到世界顶 / 世界底 |
| `/msg <玩家> <消息>` / `/r <消息>` | 私聊 / 回复 |
| `/nick <昵称>` | 改显示昵称（需权限） |
| `/me <动作>` | 动作形式发言 |
| `/seen <玩家>` / `/who` | 查询玩家最后上线 / 在线详情 |
| `/balance` / `/pay <玩家> <金额>` / `/baltop` | 查余额 / 转账 / 富豪榜 |
| `/kit <名>` / `/kits` | 领取礼包 / 礼包列表 |
| `/workbench` / `/enderchest` / `/chest` / `/anvil` / `/grindstone` / `/stonecutter` | 随身打开合成台/末影箱/箱子/铁砧/砂轮/切石机 |
| `/heal` / `/feed` / `/repair` | 治疗 / 饱食 / 修耐久（可能收费） |

### Residence 领地
| 指令 | 用途 |
| --- | --- |
| `/res tp <领地>` | 传送到自己的领地 |
| `/res info <领地>` / `/res list` / `/res listpublic` | 领地详情 / 我的领地 / 公共领地 |
| `/res set <选项 值>` | 领地开关（防怪/火焰/苦力怕爆炸等） |
| `/res flag <旗标 值>` | 细粒度权限（build/use/container/bed…） |
| `/res subzone <名>` | 领地内划子区域单独授权 |

### mcMMO
| 指令 | 用途 |
| --- | --- |
| `/mctop <技能\|all>` / `/mcrank` | 技能排行 / 我的名次 |
| `/mcboost` / `/mccooldown` | 经验加成状态 / 技能冷却 |
| `/mcinspect <物品>` | 查看物品属性加成 |
| `/parties` | mcMMO 组队 |
| `/togglexp <技能>` | 开关技能经验提示 |

### ExMarket 市场
| 指令 | 用途 |
| --- | --- |
| `/market search <关键词>` | 搜索挂单 |
| `/market price <物品>` | 查看参考价 |
| `/market cancel` / `/market collect` | 取消挂单 / 领取暂存盒物品 |

### CoreProtect
| 指令 | 用途 |
| --- | --- |
| `/co lookup <参数>` | 按玩家/方块/时间查修改记录 |
| `/co rollback <玩家/半径>` / `/co restore` | 回滚破坏 / 恢复（需管理权限） |
| `/co counter <坐标 半径>` | 区域破坏统计（需管理权限） |

## 四、Mod 侧落地

以上全部指令（文档收录 + 插件推断）已内置到 mod 的「指令目录」（`CommandCatalog.java`），主菜单按 **10 个分类页**展示：传送 / 领地 / 市场 / MMO / 趣味 / 社交 / 经济 / 工具 / 防熊 / 全部。每条带中文用途；文档收录的正常显示，插件推断的卡片右下角标**青色小点**、浮窗注明"插件推断指令"。无参数指令点击直接执行，带参数的点击复制到剪贴板。
