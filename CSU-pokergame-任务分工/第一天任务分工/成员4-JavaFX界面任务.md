# 成员 4:JavaFX 界面任务 —— 第一天

> 角色定位:做出**清楚、能操作**的界面骨架。牌图片和美术后面再换,今天全部用文字牌 + 普通按钮。
> 前置依赖:成员 2 的 core 包(你只用到 PlayerId,依赖最轻,可以最早开工)。

## 今日目标

1. 应用外壳:单 Scene 页面切换
2. 三个页面:首页、模式选择、设置浮层
3. 深色主题基础样式
4. `mvn javafx:run` 能跑起来

## 代码任务清单

| 文件路径 | 内容 |
|---|---|
| `ui/AppShell.java` | 页面切换外壳,契约见下 |
| `ui/scene/HomeView.java` | 首页 |
| `ui/scene/GameModeView.java` | 模式选择页 |
| `ui/scene/SettingsOverlay.java` | 设置浮层(StackPane 叠在当前页之上) |
| `ui/theme/app.css` | 深色主题基础 token |
| `ui/component/CardView.java` | 文字牌组件(今天先用 Label 风格,明天接牌桌) |

## 关键契约(与全队约定,方法名不要改)

```java
public final class AppShell {
    public AppShell(Scene scene);          // 挂载 root、加载 app.css
    public void showHome();                // 首页
    public void showGameModes();           // 模式选择
    public void startPdk();                // 今天先弹「开发中」占位,明天接成员3的引擎
    public void startLiar();               // 同上
    public void showRules();               // 规则页(两个 Tab,读 resources/rules/*.md;今天可以先做骨架)
    public void openSettings();            // 叠加设置浮层,不销毁底层页面
    public void closeSettings();
    public void exit();
}
```

## 页面内容(来自团队计划)

**首页**:游戏名称「CSU Poker Game / 中南棋牌室」、背景、「开始游戏」按钮、右上角设置按钮。

**模式选择页**:两张卡片 —— 「本地对战」「局域网联机」。局域网今天只做入口跳转占位(联机大厅由成员 5 明后天下)。

**设置浮层**(右上角打开,点空白关闭):音量滑块、亮度滑块(今天都做占位,不接真实逻辑)、查看规则、返回主页、退出游戏。

**页面约定**(给明天的游戏桌立规矩):
- 每台电脑自己的手牌始终放底部
- 其他玩家显示在上方/两侧
- 非当前回合时,出牌按钮禁用

## app.css 深色主题基础 token

```css
.root { -fx-background-color: #1b1b1f; }
.home-title { -fx-text-fill: #f5f5f5; -fx-font-size: 28px; -fx-font-weight: bold; }
.primary { -fx-background-color: #e0a82e; -fx-text-fill: #1b1b1f; }
.card    { -fx-background-color: #2a2a30; -fx-text-fill: #f5f5f5; -fx-background-radius: 8px; }
```

(数值可以自己调,但类名 `.root / .home-title / .primary / .card` 要固定,其他成员会复用)

## 与其他成员的协作

- **成员 2**:Launcher 由他建,你把 AppShell 接上去;如果他没好,你可以先自建临时 Application 类开发页面
- **成员 1**:页面流程以他的 `docs/页面流程.md` 为准,有出入找他确认
- **成员 3/5**:今天不用管,明天游戏桌会消费他们提供的引擎快照

## 自测命令

```bash
mvn javafx:run
```

## 验收标准

- [ ] 程序能启动,显示首页
- [ ] 首页 → 模式选择 → 返回,可来回切换
- [ ] 设置浮层能打开/关闭,底层页面状态不丢
- [ ] 深色主题生效,右上角有设置入口
- [ ] 17:30 push 到 `day1-成员4` 分支

## 明日预告(Day 2)

做「本地对战游戏选择页」和游戏桌:三个玩家区(自己底部、其余上方)、手牌区、桌面牌区、事件日志、操作按钮;接成员 3 的引擎后实现「非当前回合按钮禁用」。
