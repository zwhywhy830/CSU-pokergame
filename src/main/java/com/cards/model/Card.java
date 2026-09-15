package com.cards.model;

/**
 * 一张扑克牌（核心实体接口）。
 * 普通牌：suit 非空、rank 为 1..13（1=A, 11=J, 12=Q, 13=K），由 {@link RegularCard} 实现。
 * 王牌：suit 为 null、rank 为 0，由 {@link JokerCard} 实现（bigJoker 表示大王/小王）。
 * 对外统一面向本接口编程，具体实现通过工厂方法创建。
 */
public interface Card {

    int RANK_A = 1;
    int RANK_10 = 10;
    int RANK_J = 11;
    int RANK_Q = 12;
    int RANK_K = 13;

    /** 是否为王牌（大小王）。 */
    boolean isJoker();

    /** 是否为大王（仅王牌有意义；小王返回 false）。 */
    boolean isBigJoker();

    /** 花色；王牌返回 null。 */
    Suit suit();

    /** 点数 1..13；王牌返回 0。 */
    int rank();

    /** 点数字符串：A、2..10、J、Q、K。 */
    String rankText();

    /** 角标处的简写文本，如 "♠A"；王牌为 "JOKER"。 */
    String shortText();

    /** 中文名称，如 "红桃A"、"大王"。 */
    String name();

    // -------------------------------------------------------- 工厂方法

    /** 生成一张普通牌，rank ∈ [1,13]。 */
    static Card of(Suit suit, int rank) {
        if (suit == null) {
            throw new IllegalArgumentException("普通牌必须指定花色");
        }
        if (rank < RANK_A || rank > RANK_K) {
            throw new IllegalArgumentException("点数必须在 1..13 之间: " + rank);
        }
        return new RegularCard(suit, rank);
    }

    /** 小王（通常为黑白配色）。 */
    static Card blackJoker() {
        return new JokerCard(false);
    }

    /** 大王（通常为彩色/红色配色）。 */
    static Card redJoker() {
        return new JokerCard(true);
    }
}
