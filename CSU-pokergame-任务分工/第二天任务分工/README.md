# 第二天任务分工(2026-09-09)

> Day 1 完成了项目骨架(可编译、可测试);Day 2 把规则接进引擎、快照具体化、UI 游戏桌落地、网络骨架成型。
> 累计完成约 **2/5** 总工作量。

## 今日里程碑(晚上 18:00 合并验收)

1. `mvn clean test` 全绿(含引擎、序列化、联机骨架测试)
2. `mvn javafx:run` 能跑通:首页 → 模式选择 → 本地对战 → 跑得快/骗子酒馆游戏桌,可完整对局
3. 引擎可脱离界面运行(在 main() 里跑完一局,Day03 分层判定标准)
4. Jackson 序列化往返测试全绿;LanHost 127.0.0.1 自连测试通过

## 成员文件对照

| 文件 | 成员 | 今日产出 |
|---|---|---|
| [成员1-PM与设计](成员1-PM与设计) | 成员 1 | 概要设计说明书(类图/包图/时序图)、更新追踪表 |
| [成员2-快照与状态](成员2-快照与状态) | 成员 2 | PdkSnapshot/LiarSnapshot record、PdkState/LiarState、Jackson 多态注解 |
| [成员3-引擎与人机](成员3-引擎与人机) | 成员 3 | PdkEngine/LiarEngine 实现、人机策略、命令类、辅助类 |
| [成员4-游戏桌UI](成员4-游戏桌UI) | 成员 4 | LocalGameSelectView、PdkTableView、LiarTableView、AppShell 更新 |
| [成员5-网络与测试](成员5-网络与测试) | 成员 5 | WireMessage/JsonCodec、LanHost/LanClient 骨架、序列化与引擎测试 |

## 依赖时序

| 时间 | 事件 |
|---|---|
| 上午 10:30 前 | 成员 2 交付 PdkSnapshot/LiarSnapshot/Jackson 注解并 push |
| 上午 | 成员 3 依赖快照实现引擎;成员 5 依赖注解写序列化测试 |
| 下午 | 成员 4 依赖引擎接口实现游戏桌;成员 5 实现 LanHost/LanClient 骨架 |
| 17:30 | 全员 push 到各自分支 |
| 18:00 | 成员 1 合并 + 冒烟(`mvn clean test`、`mvn javafx:run`) |

## 分层架构(Day03 要求)

```
View (ui.*)         → Controller (core.player.*) → Model (core.* / paodekuai / liarspoker)
                                                     ↑
                                                  network.*
```

- Model 层(core / paodekuai / liarspoker)**禁止 import JavaFX**
- View 层只渲染快照、把点击转成命令,**不写规则**
- network 线程只投递命令,不改游戏状态

## 与 Day03 概要设计的对应

- 三层职责:View=ui.scene.*, Controller=core.player.*, Model=core.*/paodekuai/liarspoker
- 面向接口:GameEngine / GameSnapshot / GameCommand / PlayerController / BotPolicy 均为接口
- Model 可脱离界面运行:见 `成员3-引擎与人机/src/test/.../PdkEngineTest.java`(纯 main 驱动)
- 无循环依赖:View → Controller → Model,反向一律禁止
