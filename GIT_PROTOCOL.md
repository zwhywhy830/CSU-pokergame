# Git 协作规范（三人协作）

> 本文件供本仓库所有协作者（含 AI 编程助手）遵守。任何 git 操作前请先读完整文件。

## 1. 分支模型

仓库使用**长期 feature 分支 + 受保护 main** 模型：

```
main                  ← 永远可编译、可演示，受保护
├── feat/ai-network   ← 负责人：你自己（AI + 局域网）
├── feat/loading       ← 负责人：A（入口/加载页）
└── feat/gameplay      ← 负责人：B（出牌/桌面页）
```

- 每人**只在自己负责的 feature 分支上提交**
- main 受保护，**禁止直接 push 到 main**
- feature 分支合并到 main 后**不删除**，继续使用，下次基于最新 main rebase
- **不使用** Git Flow 的 develop/release/hotfix 分支

## 2. 文件归属表

修改文件前，先确认是否归自己负责。改动非归属文件前必须在群里沟通并征得归属人同意。

| 模块 | 负责分支 | 主要文件 |
|---|---|---|
| AI / Bot | feat/ai-network | `core/player/*`、`liarspoker/Liar*Policy`、`paodekuai/PdkBotPolicy` |
| 局域网 | feat/ai-network | `network/*`、`ui/scene/LanLobbyView`、`ui/scene/LanGameTableView` |
| 加载/入口页 | feat/loading | `ui/scene/HomeView`、`GameModeView`、`LocalGameSelectView`、`RulesView`、`SettingsOverlay` |
| 出牌/桌面页 | feat/gameplay | `ui/scene/PdkTableView`、`ui/scene/LiarTableView`、`ui/component/CardView` |
| 共享路由 | 协调改动 | `ui/AppShell.java`（见第 5 节） |

## 3. 每日开工流程

开始任何编码工作**之前**，先同步 main 最新状态：

```bash
git fetch origin
git rebase origin/main
```

如果存在未推送的本地提交导致 rebase 失败，先 `git stash` 暂存改动，rebase 完成后 `git stash pop` 恢复。

## 4. 提交规范

- 只在自己负责的 feature 分支提交，不要切换到他人分支
- commit message 使用前缀识别来源：
  - `feat(ai):` —— AI / Bot 相关
  - `feat(net):` —— 局域网相关
  - `feat(ui-loading):` —— 加载/入口页
  - `feat(ui-game):` —— 出牌/桌面页
  - `fix:` —— 修 bug
  - `refactor:` —— 重构（不改变行为）
- 单次提交聚焦一个目的，避免混合多类改动
- 提交前确保 `mvn compile` 通过（至少保证编译不挂）

## 5. 共享文件 AppShell.java 处理约定

`ui/AppShell.java` 是三人都会碰的路由中枢，已采用**注册表模式**避免冲突。

### 当前实现（已重构为注册表模式）

AppShell 只暴露两个方法：

```java
public void register(String name, Supplier<Parent> factory)   // 注册路由
public void navigate(String name)                              // 切换页面
```

加上 `openSettings() / closeSettings() / exit()` 三个辅助方法。**AppShell 主体不再变动**，新增页面**不要**在 AppShell 加方法。

### 新增无参数页面

只改两处，**不碰 AppShell**：

1. 在自己的 View 类（如 `AboutView.java`）里实现页面
2. 在 [RouteTable.java](file:///Users/zhouyuyan/Desktop/CSU-pokergame%20%283%29%20/src/main/java/com/csu/pokergame/ui/RouteTable.java) 对应分区追加一行注册：

```java
// 在 RouteTable.install() 内
shell.register("about", () -> new AboutView(shell));
```

跳转时：
```java
shell.navigate("about");
```

### 新增带参数页面

不进 RouteTable，由调用方在跳转前**临时注册**：

```java
shell.register("lan-host-table", () -> new LanGameTableView(shell, host));
shell.navigate("lan-host-table");
```

### RouteTable.java 本身的冲突处理

三人都要在 RouteTable 加行，理论上仍是共享文件，但：
- 每人加的是独立一行 `shell.register(...)`
- 不同行 → git 自动合并成功率接近 100%
- 万一冲突，肉眼可见秒解决

**绝不**在他人注册行中间插入代码，新行追加在自己分区末尾即可。

### 已注册的路由名清单

| 路由名 | 页面 | 负责分支 |
|---|---|---|
| `home` | HomeView | feat/loading |
| `game-modes` | GameModeView | feat/loading |
| `local-select` | LocalGameSelectView | feat/loading |
| `rules` | RulesView | feat/loading |
| `pdk` | PdkTableView | feat/gameplay |
| `liar` | LiarTableView | feat/gameplay |
| `lan-lobby` | LanLobbyView | feat/ai-network |
| `lan-host-table` | LanGameTableView（主机视角，临时注册） | feat/ai-network |
| `lan-client-table` | LanGameTableView（客户端视角，临时注册） | feat/ai-network |

新增路由名请同步追加到此表。

## 6. 合并到 main 的流程

完成一个可演示的功能后，按以下顺序合并：

```bash
# 1. 先在自己分支 rebase 最新 main
git checkout feat/xxx
git fetch origin
git rebase origin/main

# 2. 解决冲突（若有），编译通过后继续
git add .
git rebase --continue

# 3. 切到 main，拉取最新，合并自己分支
git checkout main
git pull origin main
git merge feat/xxx

# 4. 推送
git push origin main
```

冲突解决优先级：**归属人主导**。冲突涉及他人归属文件时，必须联系归属人确认方案，不可擅自取舍。

## 7. 冲突处理原则

- git 是**合并**工具，不是覆盖工具
- 不同文件自动合并，无需干预
- 同文件不同位置自动合并，无需干预
- 同文件同位置冲突时，手动打开文件保留双方代码（删除 `<<<` `===` `>>>` 标记）
- **绝不使用 `git checkout --theirs` 或 `--ours` 一刀切丢弃对方工作**

## 8. 禁止事项

- 禁止 `git push -f` 到任何分支（包括自己的 feature 分支与他人分支、main）
- 禁止直接在 main 上提交或推送
- 禁止修改非归属文件（除非已征得归属人同意）
- 禁止跳过 `mvn compile` 验证直接推送
- 禁止合并包含编译错误的分支到 main
- 禁止使用 `git rebase -i` 交互式变基（操作不可控）

## 9. AI 编程助手特别指令

如果你是 AI 编程助手，处理本仓库 git 操作时必须：

1. **先读本文件** `GIT_PROTOCOL.md` 全文，再执行任何 git 命令
2. 确认当前分支归属与待改文件归属一致；不一致时**停止并提示用户**
3. 执行 `git commit / merge / rebase / push` 前，向用户简述将要执行的命令及影响
4. 涉及 main 分支或他人 feature 分支的写操作，必须获得用户明确确认
5. 遇到冲突，**优先保留双方代码**并提示用户复核，禁止自动选择一方丢弃另一方
6. 不主动执行 `git push -f`、`git reset --hard`、`git branch -D` 等破坏性操作
7. 合并到 main 前，运行 `mvn -q compile` 确认编译通过；失败则不合并并报告
8. 完成操作后，用一句话报告：执行了什么、main 当前 HEAD、是否还有未推送提交
