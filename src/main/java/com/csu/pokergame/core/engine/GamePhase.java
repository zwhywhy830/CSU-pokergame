package com.csu.pokergame.core.engine;

/** 通用阶段。具体游戏定义自己的阶段（如跑得快的 PLAYING / 骗子的 DECLARE 等）。 */
public enum GamePhase {
    /** 尚未开始（等待 start()）。 */
    WAITING,
    /** 进行中。 */
    PLAYING,
    /** 已结束（存在 winner）。 */
    FINISHED
}
