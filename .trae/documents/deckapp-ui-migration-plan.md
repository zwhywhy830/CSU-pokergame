# deckapp-ui → pokergame 重构移植方案

## Context（背景）

当前 `pokergame` 项目已用 AppShell + RouteTable 注册表模式重构（commit `cdd0773`），UI 简洁但只有最小功能（跑得快牌桌、骗子酒馆牌桌、局域网大厅、设置弹层）。

`~/Downloads/game/CSU-pokergame-feat-deckapp-ui` 是同一业务层之上的 UI 增强分支，单文件巨石 `DeckApp.java`（179KB）管 8 个 Scene，含完整动画体系、主题系统、玩家档案、音效、成就、商店等。业务层（core/network/liarspoker/paodekuai）两边一致。

**目标**：把 deckapp-ui 的 UI/动画/主题/背景/音效/player 业务包完整移植到 pokergame，按 AppShell 注册表模式重构成独立 View 类。**先搭动画体系（含主题前置），再做 UI 组件**——因为 UI 组件的 `GrowthResultPanel` 依赖 `GameAnimationService`，牌桌 UI 大量依赖动画服务。

## 范围确认（用户已答）
- player 包：**全搬**（13+ 服务）
- audio + settings：**完整搬**（含音效资源）
- SceneTransition：**改造适配 AppShell 单 Scene**
- 测试：**补单元测试**

## 依赖层次（拓扑序，决定移植顺序）

```
Layer 0（无内部依赖）：DesignTokens, Theme, ParticleField
Layer 1（依赖 L0）：CardFlyAnimation, SuitParticleSystem, CardPortal, SceneTransitionOverlay
Layer 2（依赖 L1）：SceneTransition（含 AppShell 改造）
Layer 3（依赖 L0+L1）：BackgroundManager
Layer 4（业务支持，独立）：audio.AudioService/SoundEffect, settings.SettingsService/GameSettings
Layer 5（业务，独立）：player 包 13+ 服务
Layer 6（纯 UI 组件，依赖 L0+L1）：CoinBar, SeatView, AvatarView, HandCardView, PlayedCardsView, GameToast
Layer 7（动画总服务，依赖 L1+L4+GameToast）：GameAnimationService, ParticleEffect, WinCelebration
Layer 8（依赖 L5+L7）：GrowthResultPanel
Layer 9（牌桌 UI，依赖 L0-L8）：com/cards/ui/pdk/*, com/cards/ui/liar/*
Layer 10（页面，依赖一切）：LoginView, ProfileView, SettingsView
Layer 11（集成）：RouteTable + Launcher + AppShell 接入
Layer 12（测试 + 验证）
```

**强耦合组**（必须一起搬才能编译）：
- SceneTransition + SceneTransitionOverlay + CardPortal + SuitParticleSystem（转场四件套）
- GameAnimationService + AudioService + SoundEffect + SettingsService + GameSettings（动画+音频+设置）
- player 包所有服务（互相依赖，整体搬）

## 实施步骤（12 阶段）

### 阶段 1：主题层（Layer 0）
**目标**：搭好设计令牌，后续所有动画/组件引用统一 token。

新增文件（保持原包名 `com.cards.ui.theme`）：
- `src/main/java/com/cards/ui/theme/DesignTokens.java`
- `src/main/java/com/cards/ui/theme/Theme.java`

不动其他文件。验证：`mvn compile` 通过。

### 阶段 2：独立动画 + 粒子（Layer 1）
**目标**：搬入不依赖业务的动画类，为 SceneTransition 铺路。

新增文件（保持原包名 `com.cards.ui.*`）：
- `com/cards/ui/ParticleField.java`
- `com/cards/ui/animation/CardFlyAnimation.java`
- `com/cards/ui/animation/SuitParticleSystem.java`
- `com/cards/ui/animation/CardPortal.java`
- `com/cards/ui/animation/SceneTransitionOverlay.java`

验证：`mvn compile` 通过，这些类暂未被引用但能编译。

### 阶段 3：SceneTransition 改造（Layer 2）
**目标**：把切 Scene 的转场改造成在 AppShell 单 Scene 上叠 Overlay。

**架构改造**：原版 `SceneTransition.navigate(Stage stage, Scene newScene, Type type)` 通过 `stage.setScene` 切换。改造后：

```java
// 新签名（不改原 SceneTransition 类，而是加适配方法）
public static void transition(AppShell shell, String routeName, Type type, Runnable afterTransition)
// 1. 在 shell.root (StackPane) 上叠 SceneTransitionOverlay
// 2. 播放对应 type 的转场 Timeline
// 3. 动画中段调 shell.navigate(routeName) 切 root
// 4. 动画结束移除 Overlay，调 afterTransition
```

修改文件：
- `com/cards/ui/animation/SceneTransition.java`（新增 transition 方法，保留原 navigate 作内部用）
- `com/csu/pokergame/ui/AppShell.java`：新增 `transitionTo(String route, SceneTransition.Type type)` API，封装"叠 Overlay + navigate"逻辑，对外暴露 root 给 SceneTransition 用（加 `getRoot()` getter）

**关键**：AppShell 现有 `register/navigate/openSettings/closeSettings/exit` 不动，只新增 `transitionTo` 和 `getRoot()`。

测试：`SceneTransitionTest` 测 `transition(shell, route, FADE, ...)` 调用后 shell.navigate 被调用、Overlay 被移除。

### 阶段 4：背景系统（Layer 3）
新增文件：
- `com/cards/ui/background/BackgroundManager.java`

依赖阶段 1 Theme + 阶段 2 ParticleField。无改造，原样搬。

### 阶段 5：audio + settings 业务支持（Layer 4）
**目标**：解开 GameAnimationService 的强依赖前置。

新增文件（原包名 `com.csu.pokergame.audio` / `.settings`）：
- `com/csu/pokergame/audio/AudioService.java`
- `com/csu/pokergame/audio/SoundEffect.java`
- `com/csu/pokergame/settings/GameSettings.java`
- `com/csu/pokergame/settings/SettingsService.java`

资源文件（从 `~/Downloads/game/CSU-pokergame-feat-deckapp-ui/src/main/resources/` 复制）：
- `src/main/resources/audio/effect/*.wav`（8 个音效）
- `src/main/resources/audio/bgm/*.wav`（背景音乐）
- `src/main/resources/assets/background/*`、`assets/effect/*`、`assets/avatar/*`、`assets/icon/*`
- `src/main/resources/player/*.json`（玩家档案种子数据）

修改 `pom.xml`：新增 `javafx-media` 依赖（game 项目 pom 有，目标项目无）：
```xml
<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-media</artifactId>
  <version>${javafx.version}</version>
</dependency>
```

测试：`SettingsServiceTest` 测加载/保存 settings.json；`AudioServiceTest` 测无资源时静默跳过不抛异常。

### 阶段 6：player 业务包（Layer 5，全搬）
**目标**：完整搬入玩家档案/金币/成就/排行/库存/商店/账务/成长/战绩。

新增文件（原包名 `com.csu.pokergame.player`），从 game 项目整包复制：
- AccountService, PlayerManager, PlayerProfile, PlayerAccount
- CoinService, CoinRechargeService, CoinLogService
- AchievementService, Achievement
- LeaderboardService, LeaderboardEntry
- InventoryService, ItemUseService, ShopService, ShopItem
- PlayerGrowthService, PlayerStatsService, StatisticsService
- GameRecordService, GameRecord

**注意**：game 项目 player 包里部分类引用了 `com.csu.pokergame.audio` / `.settings`，已在阶段 5 就位。

运行时数据目录：复制 `data/settings.json`、`data/players/*.json` 到目标项目根 `data/`。

测试：`PlayerManagerTest` 测档案加载/保存；`CoinServiceTest` 测金币增减与日志；`AchievementServiceTest` 测成就解锁条件。

### 阶段 7：纯 UI 组件（Layer 6）
**目标**：搬入不依赖业务的 UI 组件，为动画总服务和牌桌 UI 铺路。

新增文件（原包名 `com.cards.ui.component`）：
- `CoinBar.java`（依赖 DesignTokens + Theme）
- `SeatView.java`（依赖 DesignTokens + Theme）
- `AvatarView.java`（依赖 DesignTokens + Theme + animation/effect）
- `HandCardView.java`（依赖 DesignTokens）
- `PlayedCardsView.java`（依赖 DesignTokens）
- `GameToast.java`（独立，被 GameAnimationService 依赖）

原样搬，不改逻辑。验证：`mvn compile` 通过。

### 阶段 8：动画总服务 + 庆祝（Layer 7）
**目标**：搭好 GameAnimationService，后续牌桌/页面统一调用。

新增文件：
- `com/cards/ui/effect/GameAnimationService.java`（依赖阶段 2 动画 + 阶段 4 audio/settings + 阶段 7 GameToast）
- `com/cards/ui/effect/ParticleEffect.java`
- `com/cards/ui/effect/WinCelebration.java`（依赖 GameAnimationService + 粒子）

**注意**：GameAnimationService import `com.cards.ui.component.GameToast`（阶段 7 已就位）、`com.csu.pokergame.audio.AudioService`（阶段 5 已就位）、`com.csu.pokergame.settings.SettingsService`（阶段 5 已就位），全部就绪可编译。

测试：`GameAnimationServiceTest` 测动画总开关（SettingsService 关闭后 playXxx 不抛异常）；`WinCelebrationTest` 测构造不抛异常。

### 阶段 9：GrowthResultPanel（Layer 8）
新增文件：
- `com/cards/ui/component/GrowthResultPanel.java`（依赖 GameAnimationService + Theme + PlayerGrowthService）

阶段 6 player + 阶段 8 GameAnimationService 已就位，可直接搬。

### 阶段 10：牌桌 UI 拆分（Layer 9）
**目标**：把 game 项目 `com/cards/ui/pdk/*` 和 `com/cards/ui/liar/*` 搬入，替换目标项目现有的单文件 `PdkTableView.java` / `LiarTableView.java`。

新增文件（pdk 包）：
- `com/cards/ui/pdk/PdkCardView.java`
- `com/cards/ui/pdk/PdkActionBar.java`
- `com/cards/ui/pdk/PdkPlayerSeat.java`
- `com/cards/ui/pdk/PdkHandView.java`
- `com/cards/ui/pdk/PdkTableHeader.java`
- `com/cards/ui/pdk/PdkTableView.java`（替换原 `com/csu/pokergame/ui/scene/PdkTableView.java`）

新增文件（liar 包）：
- `com/cards/ui/liar/LiarActionBar.java`
- `com/cards/ui/liar/LiarPlayerSeat.java`
- `com/cards/ui/liar/LiarHandView.java`
- `com/cards/ui/liar/LiarClaimPanel.java`
- `com/cards/ui/liar/LiarRiskIndicator.java`
- `com/cards/ui/liar/LiarTableView.java`（替换原 `com/csu/pokergame/ui/scene/LiarTableView.java`）

**改造要点**：原版 PdkTableView/LiarTableView 在 DeckApp 内被 buildXxxScene 调用，构造时接收 Stage/DeckApp 引用。移植后改成接收 `AppShell`，所有"切 Scene"调用改 `shell.transitionTo(route, type)`，所有"返回大厅"改 `shell.navigate("lan-lobby")`。

删除原文件：
- `src/main/java/com/csu/pokergame/ui/scene/PdkTableView.java`
- `src/main/java/com/csu/pokergame/ui/scene/LiarTableView.java`

修改 `RouteTable.java`：把 `"pdk"` 和 `"liar"` 的工厂改成 new `com.cards.ui.pdk.PdkTableView(shell)` / `com.cards.ui.liar.LiarTableView(shell)`。

### 阶段 11：附加页面 + 集成（Layer 10-11）
**目标**：搬入 LoginView/ProfileView/SettingsView，改造 DeckApp 的 buildXxxScene 为独立 View 类，接入 RouteTable。

新增页面（原包名 `com.cards.ui`）：
- `com/cards/ui/LoginView.java`（依赖 background + component + player + settings）
- `com/cards/ui/ProfileView.java`（依赖 background + component + player + animation）
- `com/cards/ui/SettingsView.java`（依赖 background + settings + animation）
- `com/cards/ui/CardCell.java`（扑克展示用，被 DeckScene 用）

**改造 HomeView**：原 [HomeView.java](file:///Users/zhouyuyan/Desktop/CSU-pokergame%20%283%29/src/main/java/com/csu/pokergame/ui/scene/HomeView.java) 34 行极简版，改成集成 BackgroundManager + AvatarView + CoinBar 的国风大厅（参考 DeckApp.buildMenuScene 的逻辑，但拆成独立 View，不再持有 DeckApp 引用）。

修改 `RouteTable.java` 新增注册：
```java
shell.register("login", () -> new LoginView(shell));
shell.register("profile", () -> new ProfileView(shell));
shell.register("settings", () -> new SettingsView(shell));
```

修改 `Launcher.java`：启动首屏从 `navigate("home")` 改成 `transitionTo("login", Type.NONE)`（登录后跳 home）。

修改 `AppShell.java`：openSettings() 改成 `transitionTo("settings", Type.FADE)`，跳转完成后由 SettingsView 内部"返回"按钮 navigate 回原路由。

### 阶段 12：CSS 合并 + 测试 + 验证
**CSS**：把 game 项目 `out/app.css`（或 target/classes/app.css）的内容合并到目标项目 [src/main/resources/com/csu/pokergame/ui/theme/app.css](file:///Users/zhouyuyan/Desktop/CSU-pokergame%20%283%29/src/main/resources/com/csu/pokergame/ui/theme/app.css)。注意 AppShell 当前加载路径是 `/com/csu/pokergame/ui/theme/app.css`，要么把 CSS 放这路径，要么改 AppShell 加载路径到 `/app.css`（推荐前者，保持现有约定）。

**测试补充**：
- `DesignTokensTest`：测关键 token 非空非零
- `SceneTransitionTest`：测 transition 后 navigate 被调用、Overlay 被移除
- `SettingsServiceTest`：测加载/保存 settings.json 往返
- `AudioServiceTest`：测无资源时 playEffect/playBgm 静默不抛
- `PlayerManagerTest`：测档案加载/保存
- `CoinServiceTest`：测金币增减 + CoinLog 写入
- `AchievementServiceTest`：测解锁条件
- `GameAnimationServiceTest`：测总开关关闭时 playXxx 不抛

**端到端验证**：
1. `mvn test` 全绿
2. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home mvn javafx:run`
3. 验收路径：登录页 → 输入昵称进主页（FADE 转场）→ 点"开始游戏"（ENTER_GAME 转场：大厅缩小+金色光环+花色粒子+牌背旋转+牌桌展开）→ 跑得快牌桌出牌（飞牌动画+音效+Toast）→ 结算（WinCelebration 粒子+皇冠+GrowthResultPanel 成长反馈）→ 返回大厅（RETURN_LOBBY 转场）→ 个人中心（OPEN_PROFILE 转场）→ 设置中心改音量（实时生效）

## 关键风险与对策

1. **SceneTransition 改造工作量最大**：从切 Scene 改成单 Scene Overlay 叠加，需要重写 navigate 方法的核心逻辑。对策：保留原 navigate 作内部辅助，新增 transition 作为 AppShell 友好的入口，渐进改造。

2. **player 包互相依赖复杂**：13+ 服务可能循环引用。对策：整包一次性搬入，遇循环依赖时按 game 项目原样保留（既然 game 项目能编译，依赖关系是合法的）。

3. **CSS 路径冲突**：目标项目 CSS 在 `/com/csu/pokergame/ui/theme/app.css`，game 项目 CSS 在 `/app.css`。对策：合并内容到目标路径，把 game 项目代码里硬编码 `/app.css` 的引用改成目标路径（用 Grep 找 `getResource("/app.css")` 统一替换）。

4. **DeckApp 单文件拆解**：buildXxxScene 方法可能不只是构建，还耦合状态加载和场景引用。对策：每拆一个页面，先 Read 对应方法全文，识别"构建逻辑"和"状态管理"两类，构建逻辑搬进 View 构造函数，状态管理搬进 AppShell 或独立 Service。

5. **JDK 21 + javafx.web 兼容**：之前已踩坑（Zulu 21 必需）。本方案不涉及 webview，javafx-media 在 Zulu 21 下可用（game 项目已验证）。

## 实施节奏建议

- 阶段 1-4（主题+动画+背景）：基础层，约 10 个新文件，可在一次会话完成
- 阶段 5-6（audio+settings+player）：业务支持层，约 20 个新文件 + 资源，独立会话
- 阶段 7-9（组件+动画服务+成长面板）：UI 中层，约 10 个新文件
- 阶段 10（牌桌 UI 拆分）：高潮，替换两个核心 View + 12 个新文件
- 阶段 11-12（页面+集成+CSS+测试）：收尾，路由接入 + 测试 + 验证

每个阶段完成即 `mvn compile` + `git commit`，避免大爆炸式改动难以回滚。
