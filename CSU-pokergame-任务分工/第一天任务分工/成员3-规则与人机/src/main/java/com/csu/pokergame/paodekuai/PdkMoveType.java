package com.csu.pokergame.paodekuai;

/**
 * 跑得快牌型(第一天范围,基线 src/main/resources/rules/跑得快.md)。
 * 炸弹(纯 4 张同点)、连对、三顺、飞机、四带一/四带三为 Day 2 扩展,今天不实现。
 */
public enum PdkMoveType {
    /** 单张。 */
    SINGLE,
    /** 对子:2 张同点。 */
    PAIR,
    /** 三条:3 张同点。 */
    TRIPLE,
    /** 三带一:3 张同点 + 1 张散牌。 */
    TRIPLE_WITH_SINGLE,
    /** 三带二:3 张同点 + 1 对。 */
    TRIPLE_WITH_PAIR,
    /** 顺子:≥5 张连续点数,范围 3–A,不得包含 2。 */
    STRAIGHT
}
