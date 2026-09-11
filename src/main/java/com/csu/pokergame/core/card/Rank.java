package com.csu.pokergame.core.card;

/** 点数。THREE 至 TWO，带展示文字和跑得快比较值（3 < 4 < … < A < 2）。JOKER 为骗子酒馆万能牌。 */
public enum Rank {
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14),
    TWO("2", 15),
    JOKER("JOKER", 16);

    private final String label;
    private final int comparisonValue;

    Rank(String label, int comparisonValue) {
        this.label = label;
        this.comparisonValue = comparisonValue;
    }

    public String label() {
        return label;
    }

    /** 跑得快比较值：3 最小、2 最大。 */
    public int comparisonValue() {
        return comparisonValue;
    }
}
