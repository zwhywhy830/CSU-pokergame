package com.csu.pokergame.liarspoker;

/** 骗子酒馆内部阶段。映射到通用 GamePhase：DECLARE/RESPOND/RESOLVE → PLAYING，FINISHED → FINISHED。 */
public enum LiarPhase {
    /** 当前出牌者宣告。 */
    DECLARE,
    /** 下家选择相信或质疑。 */
    RESPOND,
    /** 翻开并结算。 */
    RESOLVE,
    /** 已结束。 */
    FINISHED
}
