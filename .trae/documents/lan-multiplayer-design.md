# 局域网联机模式设计方案

## Context

CSU-pokergame 当前仅支持单机人机对战,Task 13(局域网联机)在原项目计划文档中已规划但未实现。本次需求:实现主机权威裁决的局域网多人对战,支持每种游戏的原生人数(跑得快 3 人 / 骗子酒馆 4 人),手动 IPv4 加入,无账号、无 mDNS、无断线重连,断线即结束本局。

设计约束严格遵守 `docs/architecture.md`:
- 领域层 `core / paodekuai / liarspoker` 不得引入任何网络/JavaFX 类型
- 引擎只在 JavaFX Application Thread 调用,网络线程只能通过 `Platform.runLater` 投递命令
- `snapshotFor(viewer)` 天然支持信息隐藏 —— 只给客户端发其自己视角的快照,即可避免泄露手牌

用户决策(已通过 AskUserQuestion 确认):
- 支持每种游戏原生人数(跑得快 3 / 骗子酒馆 4)
- 引入 Jackson 依赖做 JSON 序列化
- 精简 MVP:主机权威 + 三/四人 + 最小 UI

---

## 协议设计

### 端口

固定 `46888`(沿用计划文档约定),`LanHost` 监听此端口,客户端连接此端口。

### 帧格式

TCP 长度前缀帧:4 字节 big-endian 长度 + JSON 字节。长度合法区间 `[1, 1_048_576]`,超出立即关闭连接。

### WireMessage 类型(Jackson 多态 record)

```
JOIN            客户端 → 主机   无 payload
ROOM_SNAPSHOT   主机 → 客户端   RoomSnapshot payload
START_GAME      主机 → 客户端   GameType payload
GAME_SNAPSHOT   主机 → 客户端   对应 viewer 的 GameSnapshot JSON
SUBMIT_COMMAND  客户端 → 主机   GameCommand JSON
ERROR           主机 → 客户端   错误码字符串(如 "ROOM_FULL" / "INVALID_COMMAND")
GAME_ENDED      主机 → 客户端   结束原因字符串(如 "PLAYER_DISCONNECTED" / "GAME_OVER")
```

客户端只能发 `JOIN` 和 `SUBMIT_COMMAND`;主机拒绝其他消息、重复 join、任何声称姓名或座位的参数。

---

## 新建文件清单

### `network` 包(纯 Java,零 JavaFX 依赖)

| 文件 | 职责 |
|---|---|
| `src/main/java/com/csu/pokergame/network/WireMessage.java` | Jackson 多态 sealed interface,定义 7 种消息类型 |
| `src/main/java/com/csu/pokergame/network/RoomSnapshot.java` | record:玩家列表 `List<RoomPlayer> seats`、`boolean full`、`GameType gameType` |
| `src/main/java/com/csu/pokergame/network/RoomPlayer.java` | record:`PlayerId seat` + `String displayName` + `boolean host` + `boolean connected` |
| `src/main/java/com/csu/pokergame/network/GameType.java` | enum:`PAO_DE_KUAI`(3 人)、`LIARS_POKER`(4 人);带 `requiredPlayers()` 方法 |
| `src/main/java/com/csu/pokergame/network/LanHost.java` | 主机权威:ServerSocket + 单线程执行器 + 持有唯一 `GameEngine`;接收客户端命令并 `apply`;状态变更后向每个客户端 `snapshotFor(seat)` 广播 |
| `src/main/java/com/csu/pokergame/network/LanClient.java` | 客户端:Socket + 读线程;提供 `join()`、`submitCommand(GameCommand)`、`onSnapshot(Consumer<GameSnapshot>)`、`onEnded(Consumer<String>)` 回调 |
| `src/main/java/com/csu/pokergame/network/JsonCodec.java` | Jackson `ObjectMapper` 单例,封装 `writeAsBytes` / `readMessage`;含 `MixIn` 注册多态 WireMessage |
| `src/main/java/com/csu/pokergame/network/CommandCodec.java` | 把 `GameCommand` 与子类型(PlayPdkCards/PassPdkTurn/DeclareLiarCards/TrustDeclaration/ChallengeDeclaration)在 JSON 中加 `@type` 字段,便于反序列化 |

### 测试

| 文件 | 职责 |
|---|---|
| `src/test/java/com/csu/pokergame/network/JsonCodecTest.java` | 验证 WireMessage / GameCommand / GameSnapshot 的反序列化往返 |
| `src/test/java/com/csu/pokergame/network/LanHostTest.java` | 启动 port=0 的 host,断言加入顺序、座位分配、ROOM_FULL 拒绝、未齐无法 start、断线结束 |

---

## 修改文件清单

### `pom.xml`

新增 Jackson 依赖:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
</dependency>
```

### `AppShell.java`

新增方法:
- `startLanHost(GameType type)` — 创建 `LanHost`,UI 切到新的 `LanHostLobbyView`
- `startLanClient(String hostIp)` — 创建 `LanClient`,UI 切到新的 `LanClientLobbyView`
- `openLanTable(LanClient client, GameType type)` — 进入联机桌面 `LanGameTableView`
- `openLanHostTable(LanHost host)` — 主机进入联机桌面(同时扮演 SEAT_1 玩家)

### `LanLobbyView.java`(重写占位页)

替换原"功能开发中"占位为两个选择卡片:
- **创建房间**:弹 GameType 二选一 → 启动 `LanHost` → 显示主机 IPv4 + 端口 46888 + 当前已加入玩家列表 + "开始游戏"按钮(人数齐时启用)
- **加入房间**:输入 IPv4 + GameType 二选一 → 调用 `LanClient.join` → 进入等待主机开始界面

### 新建联机桌面

| 文件 | 职责 |
|---|---|
| `src/main/java/com/csu/pokergame/ui/scene/LanGameTableView.java` | 联机桌面:用 `LanClient` 收到的 `GameSnapshot` 渲染,玩家操作时 `submitCommand`,无 AI 调度;复用现有 CardView/CSS |

桌面 UI 复用现有 PdkTableView/LiarTableView 的渲染逻辑,但改为通用结构:`GameSnapshot` cast 为 `PdkSnapshot` 或 `LiarSnapshot` 后分发渲染。座位名改为远程玩家名(由 `RoomPlayer.displayName` 提供)。

---

## 关键实现要点

### 主机权威裁决

`LanHost` 持有唯一 `GameEngine`(PdkEngine 或 LiarEngine),按以下流程:

1. 接受客户端 TCP 连接
2. 读 `JOIN`,按加入顺序分配 `SEAT_2` / `SEAT_3` / `SEAT_4`(主机自己是 SEAT_1),满则发 `ERROR("ROOM_FULL")` 并关闭
3. 每次加入/离开后向所有客户端广播 `ROOM_SNAPSHOT`
4. 人数齐时主机 UI 触发 `startGame()`,创建带随机 seed 的引擎并 `start()`,广播 `START_GAME`
5. 主机 UI 在 JavaFX 线程调用 `engine.apply(localCmd)`;客户端命令由网络线程读出后通过 `Platform.runLater` 投递到 JavaFX 线程,先 `legalCommands(seat).contains(cmd)` 校验再 `apply`
6. 每次 `apply` 后,主机对每个 seat 调 `snapshotFor(seat)` 并向对应 socket 发送 `GAME_SNAPSHOT`
7. 出现 winner 或客户端断线时,广播 `GAME_ENDED`

### 座位映射

- 主机 = `SEAT_1` = "玩家 1(主机)"
- 第 1 个加入客户端 = `SEAT_2` = "玩家 2"
- 第 2 个加入客户端 = `SEAT_3` = "玩家 3"
- (骗子酒馆)第 3 个加入客户端 = `SEAT_4` = "玩家 4"

跑得快只需 3 人(SEAT_1/2/3),第 4 个加入发 ROOM_FULL;骗子酒馆需 4 人(SEAT_1/2/3/4)。

### 序列化多态

`WireMessage` 用 Jackson `@JsonTypeInfo(use=NAME, property="type")` + `@JsonSubTypes`,各消息类型用 record。

`GameCommand` 同样方式多态:`@JsonTypeInfo(property="@type")` + `@JsonSubTypes` 列出 5 个子类型。注意:多态 `@JsonTypeInfo` 加在 `GameCommand` 接口上,核心层会引入 Jackson 注解依赖 —— 这是非 JavaFX 的纯 Java 库,**不违反 `core` 零 JavaFX 依赖规则**,可接受。

### 网络线程规则

- `LanHost` 用 `Executors.newSingleThreadExecutor` 串行处理所有客户端读写
- `LanClient` 读线程独立,收到 `GAME_SNAPSHOT` 后通过回调 `Platform.runLater` 投递到 JavaFX 线程
- 引擎 `apply` 永远只在 JavaFX 线程调用

### 断线处理

- 开局前客户端断开:释放其座位,下次加入者填补该座位名
- 开局后任一玩家断开:主机广播 `GAME_ENDED("PLAYER_DISCONNECTED")`,UI 显示"玩家连接已断开,本局结束"并返回大厅
- 主机自身断开:客户端读到 socket EOF,显示"主机已关闭"并返回大厅

---

## 验证步骤

### 单元测试

1. `mvn test -Dtest=JsonCodecTest` — 验证所有 WireMessage、GameCommand、GameSnapshot 子类的序列化往返
2. `mvn test -Dtest=LanHostTest` — 验证 port=0 启动、3 人加入、第 4 人 ROOM_FULL、未齐无法 start、断线广播 GAME_ENDED
3. `mvn test` — 全量回归,49 + 新增测试全部通过

### 端到端冒烟

在同机用 127.0.0.1:
1. `mvn javafx:run` 启动一次,选"局域网联机"→"创建房间"→跑得快
2. 第二次 `mvn javafx:run`,选"加入房间",输 127.0.0.1
3. 第三次同上,人齐后主机点"开始游戏"
4. 三人完成一局跑得快
5. 重复以上对骗子酒馆(需启动 4 次)
6. 验证客户端从不看到他人手牌
7. 中途关闭一个客户端,验证主机广播"玩家连接已断开"

---

## 不实现的范围(MVP 外)

- mDNS 房间发现(仍需手动输 IPv4)
- 断线重连与房主迁移
- 序列号去重(主机串行处理天然无乱序)
- 聊天功能
- 账户系统
