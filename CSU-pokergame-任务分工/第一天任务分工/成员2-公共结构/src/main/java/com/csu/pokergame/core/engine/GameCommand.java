package com.csu.pokergame.core.engine;

/**
 * 玩家指令的标记接口。具体指令由各游戏定义(如跑得快的出牌/过,骗子的宣告/信任/质疑)。
 * 引擎通过 {@code apply} 校验当前玩家、阶段与指令合法性,非法时抛 IllegalArgumentException 且不改变状态。
 */
public interface GameCommand {
}
