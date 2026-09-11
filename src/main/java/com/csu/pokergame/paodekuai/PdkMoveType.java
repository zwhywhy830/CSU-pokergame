package com.csu.pokergame.paodekuai;

/** 跑得快牌型。FOUR_OF_A_KIND（纯四张同点）是炸弹，可压任意非炸弹。 */
public enum PdkMoveType {
    SINGLE,
    PAIR,
    TRIPLE,
    TRIPLE_WITH_ONE,
    TRIPLE_WITH_PAIR,
    STRAIGHT,
    CONSECUTIVE_PAIRS,
    TRIPLE_STRAIGHT,
    AIRPLANE_WITH_WINGS,
    FOUR_WITH_ONE,
    FOUR_WITH_THREE,
    FOUR_OF_A_KIND
}
