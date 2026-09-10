package com.csu.pokergame.core.engine;

/** 通用阶段。具体游戏可定义自己的细粒度阶段(如骗子的 DECLARE / RESPOND)。 */
public enum GamePhase {
    /** 尚未开始(等待 start())。 */
    WAITING,
    /** 进行中。 */
    PLAYING,
    /** 已结束(存在 winner)。 */
    FINISHED
}
