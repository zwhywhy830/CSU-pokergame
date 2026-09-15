package com.cards.model;

import java.util.Objects;

/**
 * 普通牌实现：确定花色 + 确定点数（A..K）。
 * 不可变值对象，请通过 {@link Card#of(Suit, int)} 工厂方法创建。
 */
public final class RegularCard implements Card {

    private final Suit suit;
    private final int rank;

    RegularCard(Suit suit, int rank) {
        this.suit = suit;
        this.rank = rank;
    }

    @Override
    public boolean isJoker() {
        return false;
    }

    @Override
    public boolean isBigJoker() {
        return false;
    }

    @Override
    public Suit suit() {
        return suit;
    }

    @Override
    public int rank() {
        return rank;
    }

    @Override
    public String rankText() {
        return switch (rank) {
            case RANK_A -> "A";
            case RANK_J -> "J";
            case RANK_Q -> "Q";
            case RANK_K -> "K";
            default -> String.valueOf(rank);
        };
    }

    @Override
    public String shortText() {
        return suit.glyph() + rankText();
    }

    @Override
    public String name() {
        return suit.chinese() + rankText();
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegularCard c)) return false;
        return rank == c.rank && suit == c.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }
}
