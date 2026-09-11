# 局域网联机 + 人机混合对战

## Context

当前局域网联机（`LanHost` / `LanClient`）只支持真人客户端按加入顺序自动分配座位（主机固定 1 号），且必须等真人凑齐 `requiredPlayers` 才能开局。问题：
1. 客户端无法选择座位，被分配到哪个就是哪个
2. 开发期联调或 2 人想玩 3 人跑得快等场景，没有真人凑不齐
3. 主机大厅没有"加机器人补位"入口

本任务在 `feat/ai-network` 分支上为局域网联机增加两项能力：
1. **客户端选座位**：客户端加入时可请求 2/3/4 号位（主机固定 1 号），冲突时主机返回 SEAT_TAKEN 错误，客户端 UI 让用户重选
2. **人机混合**：主机在等待大厅可手动为指定空座位添加机器人补位，凑够人数即可开局；机器人由主机驱动、不占用 TCP 连接，客户端通过协议能识别机器人座位并显示标识

## 设计要点

### 核心思路
机器人在主机端"虚拟成玩家"。`LanHost` 持有 `Map<PlayerId, RuleBotController>`，开局后引擎推进到机器人回合时，主机在 JavaFX 线程触发 `PauseTransition` 延迟后调用 `bot.choose()` 拿命令，再走 `applyCommand(seat, cmd)` 流程，与真人命令走同一通道 → 客户端无感（只是收到该座位的 `SubmitCommand` 结果通过 snapshot 广播体现）。

### 协议变更
- `RoomPlayer` 新增 `boolean bot` 字段（`@JsonProperty("bot")`，默认 false 保持兼容）
- `RoomSnapshot` 结构不变，只是 players 列表里的 `RoomPlayer` 带 bot 标识
- `WireMessage.Join` 新增可选字段 `PlayerId requestedSeat`（null = 任意空位，非 null = 指定座位）：
  - 客户端请求 2/3/4 号位（主机固定 1 号，请求 SEAT_1 应被拒绝）
  - 主机校验该座位空闲：空闲 → 分配；被占 → 回 `Error("SEAT_TAKEN")`，客户端 UI 让用户重选
  - 客户端不传（兼容旧版）→ 主机 `nextFreeSeat()` 自动分配
- `WireMessage` 不新增消息类型：加/移机器人是主机本地操作，通过 `ROOM_SNAPSHOT` 广播即可让客户端看到

### 线程模型
沿用项目约定：
- 引擎 `apply` 永远在 JavaFX 线程（`LanHost.applyCommand` 由 UI / 客户端回调 `Platform.runLater` 调用）
- 机器人延迟用 `PauseTransition`（JavaFX 线程），到点后 `bot.choose()` 立即返回 → `applyCommand` → `broadcastSnapshots` → 再次检查下一回合是否是机器人（链式触发）

## 实施步骤

### 1. 协议层扩展
**文件**: `network/RoomPlayer.java`
- 新增 `boolean bot` 字段（第 4 个 record 组件），保留现有 4 参数构造的兼容方式：新增 5 参数 `@JsonCreator`，旧 4 参数构造兼容（bot 默认 false）
- 显示名约定：机器人叫"机器人 2/3/4"对应座位号

**文件**: `network/WireMessage.java` 改 Join
- `record Join(PlayerId requestedSeat)` 加可选字段，`@JsonCreator` + `@JsonProperty(value = "requestedSeat", required = false)`，无参构造仍兼容（requestedSeat = null）
- 客户端 `new WireMessage.Join(seat)` 传座位；`new WireMessage.Join()` 等价于 null

### 2. 主机层
**文件**: `network/LanHost.java` 关键改动
- 新增字段 `Map<PlayerId, RuleBotController> bots = new EnumMap<>(PlayerId.class)`
- 新增公开方法 `addBot(PlayerId seat)`：在未开局、座位空闲、当前 bots 数 < `requiredPlayers - 1` 时挂一个机器人到该座位，分配 RuleBotController（按 gameType 选 PdkBotPolicy / LiarRandomPolicy），更新 `displayNames`，广播 RoomSnapshot
- 新增公开方法 `removeBot(PlayerId seat)`：移除机器人座位，广播
- 修改 `currentRoom()`：构建 `RoomPlayer` 时带 `bot` 标识（座位在 `bots` map 里就 bot=true）
- 修改 `handleClient()` 读 JOIN 时取 `requestedSeat`：
  - null → `nextFreeSeat()` 自动分配（兼容旧客户端）
  - 非 null 且 = SEAT_1 → `Error("SEAT_RESERVED_HOST")` 拒绝
  - 非 null 且已被真人客户端或机器人占用 → `Error("SEAT_TAKEN")` 拒绝
  - 非 null 且空闲 → 分配该座位
  - 收到 SEAT_TAKEN/SEAT_RESERVED_HOST 后客户端 UI 应让用户重选座位
- 修改 `nextFreeSeat()`：跳过被机器人占用的座位（不分配给新客户端）
- 修改 `startGame()`：开局条件从"connectedCount >= required"改为"真人数 + 机器人数 >= required"。开局后调用 `maybeScheduleBotTurn()` 启动机器人循环
- 修改 `applyCommand()`：在 `broadcastSnapshots()` 之后调用 `maybeScheduleBotTurn()`
- 新增私有方法 `maybeScheduleBotTurn()`：取当前快照的 `currentPlayer()`，如果是机器人座位 → 创建 `PauseTransition(随机 500-1500ms)` → onFinished 调 `bot.choose(snapshot, legal)` → `applyCommand(seat, cmd)`（链式推进直到当前是真人）
- 新增 `createBotPolicy(GameType)`：跑得快返回 `new PdkBotPolicy()`，骗子酒馆返回 `new LiarRandomPolicy(new Random(seed))`

### 3. 客户端层
**文件**: `network/LanClient.java`
- 新增 `join(PlayerId requestedSeat)` 重载：发 `new WireMessage.Join(requestedSeat)`，原 `join()` 等价于 `join(null)`
- 复用 `onError` 回调处理 SEAT_TAKEN/SEAT_RESERVED_HOST 错误码，UI 收到后让用户重选座位

### 4. UI 层：大厅加机器人入口与选座位
**文件**: `ui/scene/LanLobbyView.java`

主机面板 `openHostRoom()` 改动：
- `playersBox` 渲染逻辑：对每个 `RoomPlayer` 显示带座位号的状态行
  - 主机：显示"1 号 · 玩家 1（主机）"
  - 真人客户端：显示"N 号 · 玩家 N"
  - 机器人：显示"N 号 · 机器人 N [AI] [移除]"（移除按钮调 `host.removeBot(seat)`）
  - 空座位：显示"N 号 · 空位 [加机器人]"按钮（调 `host.addBot(seat)`）
- 开局按钮 enable 条件：`snapshot.full()`（已经计算 真人+机器人 >= required），无需改逻辑

客户端面板 `openClientRoom()` 改动：
- 加入前显示座位选择：用 ToggleButton 组 "2 号 / 3 号 / 4 号"（根据 gameType.requiredPlayers - 1 决定可见数量），用户选好后 `client = new LanClient(hostIp)` + `client.join(selectedSeat)`
- 收到 `onError` 含 SEAT_TAKEN/SEAT_RESERVED_HOST 时：弹提示"该座位已被占用，请重新选择"，重新启用座位选择
- 加入成功后渲染 playersBox 时识别 bot 字段加 [AI] 标签

### 5. UI 层：对局桌显示机器人标签
**文件**: `ui/scene/LanGameTableView.java`
- 渲染玩家信息时，机器人座位显示"[AI]"后缀（主机视角从 host.currentRoom() 查；客户端视角需要 LanClient 缓存最近一次 RoomSnapshot）
- 出牌逻辑不变：机器人出牌通过 snapshot 广播体现为桌面变化，UI 自然刷新

### 6. 测试
**新增**: `src/test/java/com/csu/pokergame/network/LanHostBotTest.java`
- 单元测试覆盖：
  - `addBot` 成功挂机器人、超过 N-1 拒绝
  - `removeBot` 移除后座位空
  - `startGame` 在 1 主机 + N-1 机器人时成功
  - `applyCommand` 触发机器人链式出牌直到回到真人回合（用 PdkEngine 验证：3 人跑得快，主机不出，2 个机器人轮流出完，最终 winner 是机器人）
- 客户端选座位逻辑可加入现有 `LanClientTest`（如存在）：构造 `Join(SEAT_3)` → 主机分配 SEAT_3

## 关键复用

- **AI 策略**: 复用 `paodekuai/PdkBotPolicy.java`、`liarspoker/LiarRandomPolicy.java`，不写新策略
- **Bot 控制器**: 复用 `core/player/RuleBotController.java`，`choose()` 立即返回 `CompletableFuture`
- **延迟模式**: 参考 `PdkTableView.java#L244-253` 的 `PauseTransition` 用法
- **座位枚举**: `core/engine/PlayerId.java`（SEAT_1..4）
- **广播机制**: 复用 `LanHost.broadcastRoom()` / `broadcastSnapshots()`

## 验证

1. `mvn -q compile` 编译通过
2. `mvn -q test -Dtest=LanHostBotTest` 新测试通过
3. `mvn -q test` 全量测试不回归
4. 手动验证：
   - 启动主机选跑得快 → 大厅点"加机器人 SEAT_2"、"加机器人 SEAT_3" → 显示 [AI] 标签 → "开始游戏"按钮变可用 → 点开局 → 主机不出牌时机器人轮流自动出牌 → 对局正常结束
   - 启动主机选骗子酒馆 → 加 3 个机器人 → 开局 → 机器人轮流 declare/challenge/trust
   - 客户端连入主机后，大厅能看到机器人座位带 [AI] 标签
   - 客户端加入时选 3 号位 → 成功加入并显示"3 号 · 玩家 N"
   - 两个客户端同时选 3 号位 → 后到者收到 SEAT_TAKEN 错误，UI 提示重选
   - 客户端选 1 号位 → 收到 SEAT_RESERVED_HOST 错误

## 影响范围
- 改动文件: `RoomPlayer.java`, `WireMessage.java`, `LanHost.java`, `LanClient.java`, `LanLobbyView.java`, `LanGameTableView.java`
- 新增文件: `LanHostBotTest.java`
- 协议变更: `RoomPlayer` 加 bot 字段、`WireMessage.Join` 加 requestedSeat 字段（均向后兼容旧客户端/旧主机）
- 不影响: 本地人机 `PdkTableView`/`LiarTableView`、AI 策略本身、核心引擎
