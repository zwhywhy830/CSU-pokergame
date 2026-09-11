# 本地扑克牌平台 MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付一个 Java/JavaFX 三人扑克牌平台：支持 1 名本地玩家对 2 名规则机器人，以及无需账户、由一台主机权威裁决的三人局域网联机；首发包含湖南跑得快和骗子酒馆，规则可测试、交互可玩、牌面资源可替换，并为日后的 AI 保留边界。

**Architecture:** 游戏规则与状态演进放在纯 Java `core` 模块，JavaFX 仅负责展示状态和将玩家操作转换为命令；因此规则测试不依赖 UI。每个游戏实现同一套 `GameEngine` / `PlayerController` 端口：本地玩家控制器通过 UI 取得指令，规则型机器人控制器根据合法操作选择指令；局域网主机持有唯一引擎实例，客户端只接收快照并提交命令；未来 AI 控制器只需替换该端口实现。

**Tech Stack:** JDK 21、Maven、JavaFX 21、JUnit 5、AssertJ、Jackson（规则配置/存档）、SLF4J + Logback；第一阶段不引入数据库、网络框架或 AI SDK。

---

## 0. MVP 范围与验收边界

### 纳入本期

- 单机三人：1 名本地玩家对 2 名确定性规则机器人；座位固定为本地玩家在底部、机器人 A 在左侧、机器人 B 在顶部，回合按逆时针方向流转。
- 局域网三人：一名玩家点击“创建房间”成为主机，主机即玩家 1；另外两名玩家手动输入主机 IPv4 地址加入。无需账户、昵称输入或注册，按照成功占用座位的先后固定分配“玩家 1（主机）”“玩家 2”“玩家 3”，房主在三人齐全后开始游戏。
- 跑得快：发牌、出牌/不出、牌型校验、牌型比较、回合与一轮结束、胜负结算、重新开始。
- 骗子酒馆扑克牌模式：宣告、质疑、失败惩罚、回合流转、胜负结算、重新开始。
- JavaFX：启动首页、设置、规则、游戏类型选择、对局方式选择、游戏桌、手牌选择、操作按钮、日志/提示、结算层、音效与动画的扩展点。
- 本地 PNG 牌面贴图：通过资源映射加载；缺图时以文字牌作为开发兜底。
- 规则机器人：固定、可复现的启发式策略，并显示其行动理由。

### 不纳入本期

- 自动局域网发现、断线重连、账号、聊天、战绩排行、真实货币、在线 AI 服务。MVP 的局域网联机使用手动 IPv4 加入；房间满员或已开局时拒绝新连接。
- 复杂美术、完整音效、粒子特效；但 UI 尺寸、状态和资源接口必须支持后续接入。

### 需在开发前由产品确认的规则清单

跑得快使用**湖南 16 张三人规则**：由 54 张标准牌移除大小王、红桃/梅花/方块 2、黑桃 A，得到 48 张，三人各 16 张；三家各自为战，按逆时针出牌，获得发牌时随机明牌的玩家首出。点数为 3 < 4 < … < A < 2，只比点数不比花色，并采用“有大必出”；最后一张牌时自动显示“报单”。支持单张、对子、三带二（最后一手可带 0–2 张）、顺子（5 张以上，3–A）、连对（2 对以上）、三顺、飞机带翅膀、四带一（炸弹）和四带三；同型同张数才可比较，炸弹可压任意非炸弹。最先出完手牌者获胜，结算两名失败者的剩牌数；某失败者一张未出时标记“关门”并按配置倍数计分。该基线综合了公开湖南规则资料；如课程指定长沙以外的细则，仅修改 `PaoDeKuaiRules` 配置及测试，不能把差异写进 UI。[湖南跑得快规则说明](https://www.gameabc.com/news/201905/5411.html)

骗子酒馆模式采用三人、每人 6 发生命、每轮公布目标点数（A/K/Q/J 轮换）、各发 5 张、可出 1–3 张并宣告其均为目标点数。当前出牌者的下家可“相信”或“质疑”：相信后由该下家成为新出牌者；质疑则翻开本次所出牌，宣告为真时质疑者失去 1 点生命，否则出牌者失去 1 点生命。完成惩罚后，所有仍在局的玩家补到 5 张，目标点数切换，并从受惩罚者的下家开始新回合；生命为 0 的玩家出局，剩下最后一人获胜。以上是可玩的三人课堂 MVP 规则，和 Steam 原作细节不等同；正式开发前须将教师/产品确认版本写入 `docs/rules/骗子酒馆.md`。

## 1. 目标代码结构

```text
.
├── pom.xml
├── README.md
├── docs/
│   ├── architecture.md
│   ├── rules/
│   │   ├── 跑得快.md
│   │   └── 骗子酒馆.md
│   └── superpowers/plans/2026-09-07-local-card-platform.md
├── src/main/java/com/csu/pokergame/
│   ├── Launcher.java
│   ├── core/
│   │   ├── card/{Suit,Rank,Card,Deck}.java
│   │   ├── engine/{GameEngine,GameCommand,GameSnapshot,GamePhase,PlayerId}.java
│   │   ├── player/{PlayerController,RuleBotController,LocalPlayerController}.java
│   │   └── persistence/{GameSave,SaveRepository,UserPreferences,PreferencesRepository}.java
│   ├── paodekuai/{PaoDeKuaiRules,PdkMove,PdkMoveType,PdkState,PdkEngine}.java
│   ├── liarspoker/{LiarRules,LiarMove,LiarState,LiarEngine}.java
│   ├── network/{LanHost,LanClient,NetworkPlayerController,WireMessage,RoomSnapshot}.java
│   └── ui/
│       ├── AppShell.java
│       ├── scene/{HomeView,SettingsView,RulesView,GameModeView,MatchTypeView,LanLobbyView,PdkTableView,LiarTableView}.java
│       ├── component/{CardView,HandView,ActionBar,GameLogView,ResultOverlay}.java
│       ├── asset/CardImageRepository.java
│       └── theme/AppTheme.java
├── src/main/resources/rules/{跑得快,骗子酒馆}.md
└── src/test/java/com/csu/pokergame/
    ├── core/card/DeckTest.java
    ├── paodekuai/{PaoDeKuaiRulesTest,PdkEngineTest}.java
    ├── liarspoker/{LiarRulesTest,LiarEngineTest}.java
    ├── core/persistence/PreferencesRepositoryTest.java
    ├── network/{LanHostTest,LanClientTest}.java
    └── ui/asset/CardImageRepositoryTest.java
```

## 2. 分阶段任务

### Task 1: 初始化可运行且可测试的 JavaFX 工程

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/csu/pokergame/Launcher.java`
- Create: `src/test/java/com/csu/pokergame/LauncherTest.java`
- Create: `.gitignore`
- Create: `README.md`

- [ ] **Step 1: 创建 Maven 项目和依赖。** 在 `pom.xml` 固定 `maven.compiler.release` 为 `21`，添加 `org.openjfx:javafx-controls:21.0.5`、`org.junit.jupiter:junit-jupiter:5.11.0`、`org.assertj:assertj-core:3.26.3`；用 Surefire 3.5.0 执行测试，用 `javafx-maven-plugin:0.0.8` 指向 `com.csu.pokergame.Launcher`。

- [ ] **Step 2: 先写失败测试。** 在 `LauncherTest` 写入：

```java
@Test void applicationTitleIsStable() {
    assertThat(Launcher.APP_TITLE).isEqualTo("CSU Poker Game");
}
```

- [ ] **Step 3: 运行测试并确认失败。** 运行 `mvn test -Dtest=LauncherTest`；预期失败，原因是 `Launcher` 不存在。

- [ ] **Step 4: 实现最小启动器。** `Launcher` 继承 `Application`，公开 `public static final String APP_TITLE = "CSU Poker Game"`，在 `start(Stage stage)` 设置标题、最小宽高 `1100 × 720`，并展示后续 `AppShell` 的根节点。

- [ ] **Step 5: 验证并提交。** 运行 `mvn test`（预期 `BUILD SUCCESS`）和 `mvn javafx:run`（预期出现空壳窗口）；提交：`git add pom.xml .gitignore README.md src && git commit -m "chore: initialize JavaFX game platform"`。

### Task 2: 建立可复用的牌与洗牌领域模型

**Files:**
- Create: `src/main/java/com/csu/pokergame/core/card/Suit.java`
- Create: `src/main/java/com/csu/pokergame/core/card/Rank.java`
- Create: `src/main/java/com/csu/pokergame/core/card/Card.java`
- Create: `src/main/java/com/csu/pokergame/core/card/Deck.java`
- Create: `src/test/java/com/csu/pokergame/core/card/DeckTest.java`

- [ ] **Step 1: 写失败测试。** 覆盖标准牌组数量、无重复、指定种子洗牌可复现：

```java
@Test void standardDeckHas52UniqueCards() {
    Deck deck = Deck.standard(new Random(7));
    assertThat(deck.draw(52)).hasSize(52).doesNotHaveDuplicates();
    assertThat(deck.remaining()).isZero();
}

@Test void sameSeedProducesSameOrder() {
    assertThat(Deck.standard(new Random(9)).draw(5))
        .containsExactlyElementsOf(Deck.standard(new Random(9)).draw(5));
}

@Test void hunanPaodekuaiDeckHas48ConfiguredCards() {
    assertThat(Deck.hunanPaodekuai(new Random(3)).draw(48))
        .hasSize(48)
        .noneMatch(card -> card.rank() == Rank.TWO && card.suit() != Suit.SPADES)
        .noneMatch(card -> card.rank() == Rank.ACE && card.suit() == Suit.SPADES);
}
```

- [ ] **Step 2: 运行 `mvn test -Dtest=DeckTest` 并确认失败。**

- [ ] **Step 3: 实现不可变 `Card` 与受控牌堆。** `Suit` 定义 `CLUBS, DIAMONDS, HEARTS, SPADES`；`Rank` 定义 `THREE` 至 `ACE, TWO`，每个枚举带展示文字和跑得快比较值；`Deck.standard(Random)` 生成 52 张后洗牌；`Deck.hunanPaodekuai(Random)` 从该 52 张牌移除红桃/梅花/方块 2 和黑桃 A 后洗牌，生成 48 张；`draw(int)` 在数量不足时抛出 `IllegalStateException`。

- [ ] **Step 4: 运行 `mvn test -Dtest=DeckTest`，预期全部通过；提交 `feat: add deterministic card deck`。**

### Task 3: 定义引擎、命令和控制器边界

**Files:**
- Create: `src/main/java/com/csu/pokergame/core/engine/PlayerId.java`
- Create: `src/main/java/com/csu/pokergame/core/engine/GamePhase.java`
- Create: `src/main/java/com/csu/pokergame/core/engine/GameCommand.java`
- Create: `src/main/java/com/csu/pokergame/core/engine/GameSnapshot.java`
- Create: `src/main/java/com/csu/pokergame/core/engine/GameEngine.java`
- Create: `src/main/java/com/csu/pokergame/core/player/PlayerController.java`
- Create: `docs/architecture.md`

- [ ] **Step 1: 写出端口契约。** `GameEngine` 仅暴露 `GameSnapshot snapshotFor(PlayerId viewer)`、`void start()`、`void apply(GameCommand command)`、`List<GameCommand> legalCommands(PlayerId player)`；所有 `apply` 必须校验当前玩家、阶段和指令合法性，非法时抛 `IllegalArgumentException`，不得改变状态。`snapshotFor` 只包含 viewer 自己的手牌，其他玩家仅公开剩余牌数、生命和公开事件，以支持局域网客户端而不泄露手牌。

- [ ] **Step 2: 实现不可变快照。** `GameSnapshot` 使用 record，至少有 `gameId`、`phase`、`currentPlayer`、`winner`、`publicEvents`；每个具体游戏快照另带其公开状态和仅本地玩家可见的手牌。跑得快快照额外暴露三家的 `remainingCardCounts` 和 `closedDoorPlayers`，首名出完即设置 `winner` 并进入 `FINISHED`。

- [ ] **Step 3: 定义控制器。** `PlayerController.choose(GameSnapshot snapshot, List<GameCommand> legal)` 返回 `CompletableFuture<GameCommand>`；UI 控制器在按钮点击时完成 future，两个机器人控制器立即返回选定命令。`PlayerId` 固定为中性的 `SEAT_1, SEAT_2, SEAT_3`，由每个游戏的 `nextActive(PlayerId)` 统一计算下一位玩家；本地模式将 `SEAT_1` 呈现为“你”、另两个座位呈现为机器人，联机模式将其呈现为系统分配的玩家名称。这样网络或 AI 以后可实现同一接口。

- [ ] **Step 4: 写入架构图和线程规则。** `docs/architecture.md` 明确引擎只在 JavaFX Application Thread 的 `Platform.runLater` 调用，未来网络线程只能投递命令；不得从领域层引用 JavaFX 类型。

- [ ] **Step 5: 运行 `mvn test` 并提交 `feat: define game engine ports`。**

### Task 4: 以测试驱动实现跑得快牌型识别与比较

**Files:**
- Create: `src/main/java/com/csu/pokergame/paodekuai/PdkMoveType.java`
- Create: `src/main/java/com/csu/pokergame/paodekuai/PdkMove.java`
- Create: `src/main/java/com/csu/pokergame/paodekuai/PaoDeKuaiRules.java`
- Create: `src/test/java/com/csu/pokergame/paodekuai/PaoDeKuaiRulesTest.java`
- Create: `docs/rules/跑得快.md`

- [ ] **Step 1: 用表驱动测试固定规则。** 测试 `classify(List<Card>, boolean isFinalHand)` 对 `SINGLE`、`PAIR`、`TRIPLE_WITH_PAIR`、`STRAIGHT`、`CONSECUTIVE_PAIRS`、`AIRPLANE_WITH_WINGS`、`FOUR_WITH_ONE`、`FOUR_WITH_THREE` 的识别；验证顺子/连对/三顺包含 `TWO` 时返回空，普通三带二不能只带一张，而最后一手三带可带 0–2 张：

```java
@ParameterizedTest
@MethodSource("invalidRuns")
void runsCannotContainTwo(List<Card> cards) {
    assertThat(rules.classify(cards)).isEmpty();
}

@Test void fourWithOneBeatsAnyNonBomb() {
    assertThat(rules.canBeat(fourWithOne("7"), straight("3", "7"))).isTrue();
}
```

- [ ] **Step 2: 运行 `mvn test -Dtest=PaoDeKuaiRulesTest` 并确认失败。**

- [ ] **Step 3: 实现分类与比较。** `PdkMove` 保存不可变、按点数排序的牌、`PdkMoveType`、主比较点数和张数；`canBeat(candidate, table)` 对空桌返回真，对不同非炸弹类型或长度返回假，对同型按主点数比较；`FOUR_WITH_ONE` 为炸弹且可压所有非炸弹，`FOUR_WITH_THREE` 只按同型比较、不是炸弹。

- [ ] **Step 4: 补齐反例。** 添加“三带一在非最后一手无效”“同点数但花色不同不能压”“小四带一不能压大四带一”“连续三张长度不同不能压”“四带三不能当炸弹压顺子”的测试。

- [ ] **Step 5: 在 `docs/rules/跑得快.md` 写入 Task 0 的规则及例外，运行 `mvn test`，提交 `feat: add paodekuai move rules`。**

### Task 5: 实现跑得快局状态、回合和结算

**Files:**
- Create: `src/main/java/com/csu/pokergame/paodekuai/PdkState.java`
- Create: `src/main/java/com/csu/pokergame/paodekuai/PdkEngine.java`
- Create: `src/test/java/com/csu/pokergame/paodekuai/PdkEngineTest.java`

- [ ] **Step 1: 写失败测试。** 使用固定 `Random` 构造引擎，验证牌堆恰有 48 张且不含红桃/梅花/方块 2、黑桃 A；三家各 16 张；随机明牌持有者先手；首出后按 `SEAT_1 → SEAT_2 → SEAT_3 → SEAT_1` 逆时针轮到下一家；能压时 `PASS` 非法；连续两次无牌可压后桌面清空并由最后出牌者继续；第一名出完后 `winner` 设置、阶段为 `FINISHED`，且保存两名失败者剩牌数和未出过牌者的关门标记。

- [ ] **Step 2: 运行 `mvn test -Dtest=PdkEngineTest` 并确认失败。**

- [ ] **Step 3: 实现命令与状态机。** 建立 `PlayPdkCards(List<Card> cards)`、`PassPdkTurn()` 两种 `GameCommand`；`PdkState` 不可变保存三家手牌、明牌、桌面牌、最后出牌者、连续 pass 数、赢家、剩牌数和关门标记。引擎生成的合法命令在存在任何可压牌时排除 `PassPdkTurn`；清桌后的新一轮不允许 pass。

- [ ] **Step 4: 对每个非法命令加“不变性”测试。** 分别对“非当前玩家”“牌不在手中”“牌型无效”“能压时 pass”“无法压过桌面”“新一轮 pass”调用 `apply`，断言抛异常且 `snapshot` 与调用前相同。

- [ ] **Step 5: 运行 `mvn test`，提交 `feat: implement paodekuai game engine`。**

### Task 6: 以测试驱动实现骗子酒馆规则和状态机

**Files:**
- Create: `src/main/java/com/csu/pokergame/liarspoker/LiarMove.java`
- Create: `src/main/java/com/csu/pokergame/liarspoker/LiarRules.java`
- Create: `src/main/java/com/csu/pokergame/liarspoker/LiarState.java`
- Create: `src/main/java/com/csu/pokergame/liarspoker/LiarEngine.java`
- Create: `src/test/java/com/csu/pokergame/liarspoker/LiarRulesTest.java`
- Create: `src/test/java/com/csu/pokergame/liarspoker/LiarEngineTest.java`
- Create: `docs/rules/骗子酒馆.md`

- [ ] **Step 1: 写失败测试。** 覆盖仅允许 1–3 张手牌宣告、仅当前出牌者下家可相信/质疑、质疑时准确判定真/假、真宣告扣质疑者生命、假宣告扣出牌者生命、惩罚后所有存活玩家补至 5 张、目标点数切换、生命到 0 的玩家出局且最后存活者获胜。

- [ ] **Step 2: 运行 `mvn test -Dtest=LiarRulesTest,LiarEngineTest` 并确认失败。**

- [ ] **Step 3: 实现命令与阶段。** 使用 `DeclareLiarCards(List<Card> cards)`、`TrustDeclaration()`、`ChallengeDeclaration()`；状态阶段严格为 `DECLARE`、`RESPOND`、`RESOLVE`、`FINISHED`。仅 `RESPOND` 阶段、且为出牌者下家的存活玩家可作出相信/质疑选择；相信后轮到该响应者宣告。

- [ ] **Step 4: 实现惩罚和补牌。** 每次质疑解决后生成 `ResolutionEvent`（公开展示声明、实际牌、挑战者和失血者），所有存活玩家从同一确定性牌堆补至 5 张，目标点数切换，并从受惩罚者的存活下家开始新一轮；牌堆不足时重建并洗牌，不向任一玩家公开补牌顺序。

- [ ] **Step 5: 写规则文档、运行 `mvn test` 并提交 `feat: implement liar poker engine`。**

### Task 7: 实现确定性规则机器人与可替换策略

**Files:**
- Create: `src/main/java/com/csu/pokergame/core/player/RuleBotController.java`
- Create: `src/main/java/com/csu/pokergame/paodekuai/PdkBotPolicy.java`
- Create: `src/main/java/com/csu/pokergame/liarspoker/LiarBotPolicy.java`
- Create: `src/test/java/com/csu/pokergame/core/player/RuleBotControllerTest.java`

- [ ] **Step 1: 写失败测试。** 为相同快照及合法命令列表断言两个机器人每次选同一指令，且结果必在 `legalCommands` 内；跑得快在可压时选择“张数最少、主点最小”的可压牌，只有不能压时 pass；骗子酒馆在上次宣告 3 张且自身生命不多于 2 时选择 challenge，否则 trust。

- [ ] **Step 2: 运行 `mvn test -Dtest=RuleBotControllerTest` 并确认失败。**

- [ ] **Step 3: 实现策略。** 策略返回 `BotDecision(GameCommand command, String reason)`，控制器将原因写成公开事件，例如“机器人选择质疑：风险较高”。策略禁止使用随机数和 UI 类型。

- [ ] **Step 4: 运行 `mvn test` 并提交 `feat: add deterministic rule bots`。**

### Task 8: 创建启动首页、设置、规则与对局选择流程

**Files:**
- Create: `src/main/java/com/csu/pokergame/ui/AppShell.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/HomeView.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/SettingsView.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/RulesView.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/GameModeView.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/MatchTypeView.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/LanLobbyView.java`
- Create: `src/main/java/com/csu/pokergame/ui/scene/GameTableView.java`
- Create: `src/main/java/com/csu/pokergame/ui/theme/AppTheme.java`
- Create: `src/main/resources/com/csu/pokergame/ui/theme/app.css`
- Create: `src/main/java/com/csu/pokergame/core/persistence/UserPreferences.java`
- Create: `src/main/java/com/csu/pokergame/core/persistence/PreferencesRepository.java`
- Create: `src/main/resources/rules/跑得快.md`
- Create: `src/main/resources/rules/骗子酒馆.md`
- Create: `src/test/java/com/csu/pokergame/core/persistence/PreferencesRepositoryTest.java`

- [ ] **Step 1: 写入设置存储的失败测试。** 为 `PreferencesRepository` 写入以下测试，要求首次读取返回默认值、保存后可完整读回：

```java
@Test void savesAndLoadsVolumeAndBrightness(@TempDir Path directory) {
    PreferencesRepository repository = new PreferencesRepository(directory);
    UserPreferences expected = new UserPreferences(0.35, 0.80);
    repository.save(expected);
    assertThat(repository.load()).isEqualTo(expected);
}

@Test void missingFileUsesDefaults(@TempDir Path directory) {
    assertThat(new PreferencesRepository(directory).load())
        .isEqualTo(new UserPreferences(0.70, 1.00));
}
```

- [ ] **Step 2: 运行 `mvn test -Dtest=PreferencesRepositoryTest` 并确认失败。**

- [ ] **Step 3: 实现设置值和应用外壳。** `UserPreferences` 为 `record UserPreferences(double masterVolume, double brightness)`，音量限制在 `[0.0, 1.0]`，亮度限制在 `[0.60, 1.20]`；`PreferencesRepository` 将其保存为用户目录 `~/.csu-poker-game/preferences.json`。`AppShell` 用 `BorderPane` 管理单一 `Scene`，公开 `showHome()`、`showSettings()`、`showRules()`、`showGameModes()`、`showMatchTypes(GameType type)`、`showLanLobby(GameType type)`、`startLocalBotGame(GameType type)`；所有返回按钮均返回其真实上一页而不是关闭窗口。

- [ ] **Step 4: 实现启动首页。** 程序运行后只显示 `HomeView`：页面中央上方为游戏名称“CSU Poker Game”，下方为一句副标题；整个页面使用 `app.css` 的牌桌背景色或可替换背景图；中央主按钮为“开始游戏”；右上角仅放带文字提示的齿轮按钮“设置”。点击“开始游戏”进入 `GameModeView`，点击齿轮进入 `SettingsView`。

- [ ] **Step 5: 实现设置与规则页。** `SettingsView` 必须包含“游戏音量”滑条（0–100%）、“游戏亮度”滑条（60–120%）、“规则”按钮和“退出游戏”按钮。拖动音量立即作用于 `MediaPlayer` 的 `volume`；亮度通过根节点的 `ColorAdjust`（brightness 值为偏移后的 `brightness - 1.0`）立即预览，离开页面仍保留；两项均在滑条松开时保存。规则按钮进入 `RulesView`，退出游戏按钮弹出确认框“确定退出游戏吗？”，确认后执行 `Platform.exit()`，取消则留在设置页。`RulesView` 顶部提供“湖南跑得快 / 骗子酒馆”标签页，以 UTF-8 从 `/rules/跑得快.md`、`/rules/骗子酒馆.md` 读取文本并在只读、自动换行的 `TextArea` 中展示，正文可滚动，底部有“返回设置”；两个资源文件分别复制 `docs/rules/` 的同名规则内容，以便 JAR 内运行时无需访问源码目录。

- [ ] **Step 6: 实现游戏和对局方式选择。** `GameModeView` 并列展示两块大卡片“湖南跑得快”“骗子酒馆”，每张卡含简短玩法说明、三人图标和“选择”按钮；点击任一卡片进入 `MatchTypeView`。`MatchTypeView` 显示已选模式名，提供两张等宽卡片：“本地人机对战”和“局域网联机”。选择本地人机对战后，`AppShell.startLocalBotGame` 创建新引擎、1 个本地控制器和 2 个机器人控制器，直接进入对应游戏桌。选择局域网联机进入 `LanLobbyView`：显示“创建房间”“加入房间”与“返回对局方式选择”；创建者成为“玩家 1（主机）”并显示本机 IPv4、端口 `46888`、三个座位与“等待玩家加入”；加入者仅输入主机 IPv4 地址，连接成功后按空座位顺序显示“玩家 2”“玩家 3”。房主仅在三人齐全时可点“开始游戏”；网络调用由 Task 13 实现。

- [ ] **Step 7: 定义所有桌面必须显示的状态。** `GameTableView` 固定区域：底部始终显示当前客户端所属座位的手牌，左侧和顶部显示其余两席的显示名、剩余牌/生命，中央显示桌面/声明区，右侧显示事件日志，底部显示操作栏。其余两席按当前视角的逆时针顺序映射到左侧、顶部；本地模式显示“你、机器人 A、机器人 B”，联机模式显示“玩家 1（主机）/玩家 2/玩家 3”。窗口宽度不足 980 时给根节点加 `ScrollPane`，避免控件不可达。

- [ ] **Step 8: 编写 CSS token。** 在 `app.css` 定义 `-color-table: #0f5132`、`-color-surface: #f4ecd8`、`-color-accent: #d99124`、`-color-danger: #b42318`、圆角 `12px`、间距 `8px/16px/24px`；首页背景图路径为 `ui/images/home-background.png`，文件缺失时回退到 `-color-table` 纯色背景；不得将颜色散落写进 Java 类。

- [ ] **Step 9: 验证并提交。** 运行 `mvn test`（预期 `BUILD SUCCESS`）；运行 `mvn javafx:run`，手工依次验证：首页 → 设置 → 修改音量/亮度 → 规则 → 返回设置 → 首页 → 开始游戏 → 两个游戏卡片 → 本地人机对战 → 对应游戏桌；再验证局域网卡片只进入说明页且两个房间按钮不可用。用 1100×720 与 1440×900 检查文字、按钮均可见；提交 `feat: add launch settings and game selection flow`。

### Task 9: 接入可替换牌面资源与手牌选择组件

**Files:**
- Create: `src/main/java/com/csu/pokergame/ui/asset/CardImageRepository.java`
- Create: `src/main/java/com/csu/pokergame/ui/component/CardView.java`
- Create: `src/main/java/com/csu/pokergame/ui/component/HandView.java`
- Create: `src/main/resources/cards/README.md`
- Create: `src/main/resources/cards/back.png`
- Test: `src/test/java/com/csu/pokergame/ui/asset/CardImageRepositoryTest.java`

- [ ] **Step 1: 固定资源命名协议。** `CardImageRepository` 以 `cards/{rank}_{suit}.png` 查找，例如 `cards/A_spades.png`、`cards/10_hearts.png`；牌背为 `cards/back.png`。在资源 README 写明 PNG 尺寸为 360×504、透明背景、不可包含版权来源不明的素材。

- [ ] **Step 2: 写失败测试。** 测试 `resourcePath(new Card(Rank.ACE, Suit.SPADES))` 返回 `/cards/A_spades.png`，缺失图片时 `fallbackLabel` 返回 `A♠`。

- [ ] **Step 3: 实现显示与选择。** `CardView` 接收 `Card`、face-up 与 selected；图片缺失则画白底文本卡。`HandView` 维护 `Set<Card> selectedCards()`；鼠标和键盘 Enter 均可切换选中，选中牌向上偏移 `-18px` 并有可见焦点边框。

- [ ] **Step 4: 人工验收。** 临时放置至少一张 `A_spades.png` 后运行程序，确认该牌显示图片、缺图牌显示文字、键盘可选择；提交 `feat: add replaceable card assets and hand selection`。

### Task 10: 完成跑得快交互闭环

**Files:**
- Create: `src/main/java/com/csu/pokergame/ui/scene/PdkTableView.java`
- Create: `src/main/java/com/csu/pokergame/ui/component/ActionBar.java`
- Create: `src/main/java/com/csu/pokergame/ui/component/GameLogView.java`
- Create: `src/main/java/com/csu/pokergame/ui/component/ResultOverlay.java`

- [ ] **Step 1: 绑定状态到 UI。** 每次引擎快照变化，刷新当前客户端手牌、另外两席各自的剩余数量、明牌、桌面最后出牌、回合提示和日志；玩家剩 1 张时显示“报单”。不得从 UI 自行推断合法性，出牌按钮仅在选择恰好构成合法 `PlayPdkCards` 时启用。

- [ ] **Step 2: 实现行动顺序。** 本地玩家点“出牌”后调用 `engine.apply`；若轮到任一机器人，显示对应名称的“思考中…”，用 `PauseTransition(Duration.millis(450))` 后取得该机器人命令并调用引擎；点击“不出”仅在 `legalCommands` 中存在 `PassPdkTurn` 时可用。

- [ ] **Step 3: 实现结束层。** `winner` 非空时显示“你赢了/机器人赢了”、两名失败者的剩牌数和关门标识，并提供重新开始和返回大厅。重新开始必须创建全新 `PdkEngine`，而不是修改已结束状态。

- [ ] **Step 4: 人工验收脚本。** 开局明牌和首出者提示明确；选择非法组合按钮不可点；有可压牌时“不出”不可点；可完成至少一局三人对局；日志依序包含出牌、pass、清桌、报单、结束。修复控制台异常后提交 `feat: make hunan paodekuai playable in JavaFX`。

### Task 11: 完成骗子酒馆交互闭环

**Files:**
- Create: `src/main/java/com/csu/pokergame/ui/scene/LiarTableView.java`
- Modify: `src/main/java/com/csu/pokergame/ui/component/ActionBar.java`
- Modify: `src/main/java/com/csu/pokergame/ui/component/ResultOverlay.java`

- [ ] **Step 1: 表达隐藏信息。** 中央区只展示当前出牌者“宣告 N 张目标牌”，不得展示其具体牌；左侧与顶部机器人亦不得互相泄露手牌；质疑结算时才在 `ResolutionEvent` 区域翻开展示本次牌，停留至少 1200ms 后继续。

- [ ] **Step 2: 绑定四类操作。** 本地 `DECLARE` 阶段仅在选择 1–3 张时启用“宣告 N 张”；本地 `RESPOND` 阶段显示“相信”和“质疑”；其他按钮隐藏而非仅禁用，防止玩家误解当前规则。

- [ ] **Step 3: 加入生命与目标点数可视化。** 左侧、顶部、底部各显示 6 个生命标记；中央显示本轮目标点数及当前响应者；结算用“宣告属实 / 宣告被拆穿”及失血对象的文字，颜色同时配图标，不能只靠红绿区分。

- [ ] **Step 4: 人工验收脚本。** 分别走一遍真宣告被质疑、假宣告被质疑、相信后由下家出牌、任一玩家生命归零出局、仅剩一人；确认隐藏牌未在结算前暴露。提交 `feat: make three-player liar poker playable in JavaFX`。

### Task 12: 本地存档、可观测性与回归质量门禁

**Files:**
- Create: `src/main/java/com/csu/pokergame/core/persistence/GameSave.java`
- Create: `src/main/java/com/csu/pokergame/core/persistence/SaveRepository.java`
- Create: `src/main/java/com/csu/pokergame/ui/component/Toast.java`
- Create: `src/test/java/com/csu/pokergame/core/persistence/SaveRepositoryTest.java`
- Modify: `README.md`

- [ ] **Step 1: 写失败测试。** 用临时目录保存 `GameSave(version, gameType, seed, commands)`，读回后与原对象相等；非法 JSON 和未知版本返回 `Optional.empty()` 且不抛出未处理异常。

- [ ] **Step 2: 实现仅命令日志存档。** 存档写到用户目录 `~/.csu-poker-game/saves/current-game.json`，保存初始随机种子和已应用命令，而非序列化 JavaFX 或内部状态；加载时用相同 seed 重放命令，任意重放失败即拒绝加载并显示 Toast。

- [ ] **Step 3: 接入菜单。** 仅在非结束局显示“保存并返回大厅”；大厅显示“继续上一局”（无有效存档时禁用）。每个受控异常通过日志记录技术详情，Toast 显示用户可理解信息。

- [ ] **Step 4: 完善 README。** 写明 JDK 21、`mvn test`、`mvn javafx:run`、图片资源位置、两套 MVP 规则、存档位置、局域网主机端口 `46888`、手动 IPv4 加入方式、无需账户及 AI 尚未实现。

- [ ] **Step 5: 执行质量门禁。** 运行 `mvn clean test`、`mvn -q javafx:run` 的手工冒烟测试，并逐项执行 Task 10/11 的验收脚本；提交 `feat: add local save and release documentation`。

### Task 13: 实现无账户的主机权威局域网三人房间

**Files:**
- Create: `src/main/java/com/csu/pokergame/network/WireMessage.java`
- Create: `src/main/java/com/csu/pokergame/network/RoomSnapshot.java`
- Create: `src/main/java/com/csu/pokergame/network/LanHost.java`
- Create: `src/main/java/com/csu/pokergame/network/LanClient.java`
- Create: `src/main/java/com/csu/pokergame/network/NetworkPlayerController.java`
- Create: `src/test/java/com/csu/pokergame/network/LanHostTest.java`
- Create: `src/test/java/com/csu/pokergame/network/LanClientTest.java`
- Modify: `src/main/java/com/csu/pokergame/ui/scene/LanLobbyView.java`
- Modify: `src/main/java/com/csu/pokergame/ui/AppShell.java`

- [ ] **Step 1: 写失败测试，固定命名和容量规则。** 启动端口为 `0` 的 `LanHost`，以三个测试 `LanClient` 连接；断言房主立即为 `SEAT_1` / “玩家 1（主机）”，前两个成功客户端依次为 `SEAT_2` / “玩家 2” 与 `SEAT_3` / “玩家 3”，第四个客户端收到 `ROOM_FULL`；三人未齐时 `startGame()` 抛出 `IllegalStateException`，三人齐全后才可开始。

```java
@Test void assignsNamesByJoinOrderAndRejectsFourthPlayer() {
    host = LanHost.create(0, GameType.PAO_DE_KUAI);
    assertThat(host.roomSnapshot().players()).extracting(RoomPlayer::displayName)
        .containsExactly("玩家 1（主机）");
    assertThat(client("127.0.0.1", host.port()).join().displayName()).isEqualTo("玩家 2");
    assertThat(client("127.0.0.1", host.port()).join().displayName()).isEqualTo("玩家 3");
    assertThatThrownBy(() -> client("127.0.0.1", host.port()).join())
        .hasMessageContaining("ROOM_FULL");
}
```

- [ ] **Step 2: 运行 `mvn test -Dtest=LanHostTest,LanClientTest` 并确认失败。**

- [ ] **Step 3: 实现受限 JSON 通信协议。** `WireMessage` 使用 Jackson 带 `type` 字段的 record：`JOIN`、`ROOM_SNAPSHOT`、`START_GAME`、`GAME_SNAPSHOT`、`SUBMIT_COMMAND`、`ERROR`、`GAME_ENDED`。消息使用 TCP 长度前缀帧；`LanHost` 在读取长度小于 `1` 或大于 `1_048_576` 字节时关闭连接。客户端只能发送 `JOIN` 和 `SUBMIT_COMMAND`；主机拒绝未知消息、重复 join、无效 JSON 和任何声称姓名或座位的 join 参数。

- [ ] **Step 4: 实现主机唯一裁决。** `LanHost` 在单线程执行器中持有唯一 `GameEngine`；房主点击开始后创建带随机 seed 的引擎，所有客户端命令经 `legalCommands(player)` 验证后才调用 `apply`。每次状态变化，主机分别调用 `snapshotFor(SEAT_1)`、`snapshotFor(SEAT_2)`、`snapshotFor(SEAT_3)` 并只向对应 socket 发送各自快照；客户端不得计算牌型、洗牌、发牌或胜负。

- [ ] **Step 5: 实现加入、离开与 UI 绑定。** `LanLobbyView` 的“创建房间”启动 `LanHost`、显示当前可用 IPv4 地址和固定端口 `46888`；“加入房间”只请求 IPv4 地址，调用 `LanClient.join`，成功后显示系统分配的名称与座位。房主只能在三人齐全时启用“开始游戏”。开局前客户端断开则释放其座位，下一位成功加入者取得该空座位对应名字；开局后任一远端玩家断开，主机广播 `GAME_ENDED` 和“玩家连接已断开，本局结束”，回到对局方式选择。整个 UI 更新均通过 `Platform.runLater` 执行。

- [ ] **Step 6: 实现网络控制器。** `NetworkPlayerController` 在主机端等待指定远端玩家的 `SUBMIT_COMMAND`，并将其转换为 `CompletableFuture<GameCommand>`；`LanClient` 在本地玩家点操作栏按钮时发送命令，在收到 `GAME_SNAPSHOT` 后刷新 `GameTableView`。主机自身继续使用 `LocalPlayerController`；任何本地 UI 操作只在本玩家为 `currentPlayer` 且命令位于合法命令列表时可提交。

- [ ] **Step 7: 验证并提交。** 运行 `mvn test`，预期 `BUILD SUCCESS`；在同一局域网的三台机器上，对两种游戏分别执行：主机创建房间 → 两名客户端按序加入并显示“玩家 2”“玩家 3” → 房主开始 → 三人完成一局。确认客户端从不看到其他人的手牌，第四名玩家被拒绝，断线显示结束原因。提交 `feat: add host-authoritative LAN multiplayer`。

## 3. 后续迭代接口

### LAN 联机增强

在当前手动 IPv4 加入的主机模式之上，增加 mDNS 房间发现、单调递增的 `sequence` 去重、断线重连和房主迁移；仍由主机唯一裁决，客户端不得自行裁决牌型或胜负。

### AI 接入

新增 `AiPlayerController` 适配本地/远程模型，但必须让模型输出先通过 `legalCommands` 白名单验证；无效、超时或服务异常时回退 `RuleBotController`。游戏逻辑不能依赖模型响应格式。

### 美术接入

只替换 `src/main/resources/cards/` 和 CSS token；不允许美工改动 `core` 或游戏规则。美术验收包括缺图降级、不同 DPI 下清晰度、深色/浅色桌面可读性以及键盘焦点可见性。

## 4. 完成定义（Definition of Done）

- 两种模式均能从空工程启动、完整玩到结束并重开。
- `mvn clean test` 通过，规则测试覆盖合法、非法和边界状态；领域层零 JavaFX 依赖。
- 对同一 seed 和同一命令序列，游戏结果完全一致；机器人每次决策可复现。
- 玩家不会在 UI 上执行非法动作；骗子酒馆的隐藏牌在结算前绝不泄露。
- 局域网模式无需注册或昵称输入；主机为“玩家 1（主机）”，两名成功加入者依次为“玩家 2”“玩家 3”，且只有主机裁决状态与胜负。
- 缺失任何单张 PNG 时程序不崩溃并显示文字牌；README 能让新成员从零启动。
- `git status --short` 仅包含本次计划范围内、准备提交的文件。

## 5. 计划自检

- 覆盖性：Task 1–3 建立工程和可扩展边界；Task 4–5 覆盖跑得快；Task 6 覆盖骗子酒馆；Task 7 覆盖固定人机；Task 8–11 覆盖 JavaFX 交互、贴图与结算；Task 12 覆盖存档、文档和质量门禁；Task 13 覆盖无账户的主机权威局域网联机。
- 规则不确定性：两种模式的基线均明确写出；跑得快地区差异和骗子酒馆非原作细节集中在 `docs/rules/`，不会污染 UI。
- 一致性：两种模式都通过 `GameEngine`、`GameCommand`、`GameSnapshot`、`PlayerController` 接入；LAN 通过 `NetworkPlayerController` 和每位玩家的受限 `snapshotFor` 接入，AI 以替换控制器为后续扩展点。
- 可执行性：每一任务给出准确路径、测试或可观察的人工验收、命令和提交信息；没有待填项。
