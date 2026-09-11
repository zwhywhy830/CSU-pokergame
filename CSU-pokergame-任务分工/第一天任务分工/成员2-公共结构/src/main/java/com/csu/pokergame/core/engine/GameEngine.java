package com.csu.pokergame.core.engine;

import java.util.List;

/**
 * 游戏引擎端口(只定义接口,具体游戏由成员 3 从 Day 2 开始实现)。
 * 引擎只在 JavaFX Application Thread 上被调用(见 docs/architecture.md)。
 */
public interface GameEngine {

    /** 以 viewer 视角返回不可变快照;只含 viewer 自己的手牌。 */
    GameSnapshot snapshotFor(PlayerId viewer);

    /** 开始游戏(洗牌、发牌、定首出)。 */
    void start();

    /** 校验并应用指令;非法指令抛 IllegalArgumentException 且不改变状态。 */
    void apply(GameCommand command);

    /** 返回指定玩家当前所有合法指令。 */
    List<GameCommand> legalCommands(PlayerId player);
}
