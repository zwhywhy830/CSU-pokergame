# 架构

## 分层

- **`core`（纯 Java 领域层）**：牌、引擎端口、玩家控制端口。**零 JavaFX 依赖**，规则测试不依赖 UI。
- **`paodekuai` / `liarspoker`（纯 Java 游戏层）**：具体游戏的规则、状态、引擎，实现 `core` 端口。
- **`ui`（JavaFX 展示层）**：只负责把 `GameSnapshot` 渲染出来，并把玩家操作转成 `GameCommand`。
- **`network`（未来）**：局域网主机权威裁决，客户端只收快照、提交命令。
- **`persistence`（未来）**：存档与设置。

## 端口

- `GameEngine`：`snapshotFor(viewer)` / `start()` / `apply(command)` / `legalCommands(player)`。
- `PlayerController.choose(snapshot, legal)` 返回 `CompletableFuture<GameCommand>`。
- 本地玩家控制器 = UI 按钮；规则机器人 = 立即返回；未来 AI = 替换端口实现。

## 线程规则

- 引擎**只在 JavaFX Application Thread** 上被调用，通过 `Platform.runLater` 投递。
- 未来网络线程只能**投递命令**，不得直接改状态。
- **领域层（core / paodekuai / liarspoker）不得引用任何 JavaFX 类型**。

## 信息隐藏

- `snapshotFor(viewer)` 只含 viewer 自己的手牌；其他玩家仅公开剩余牌数 / 生命 / 公开事件。
- 局域网客户端据此天然看不到他人手牌；本地模式与联机模式共用同一视图逻辑。
