package com.cards.model;

import java.util.Objects;

/**
 * 王牌实现：大王（bigJoker=true，彩色/红色）或小王（bigJoker=false，黑白）。
 * 不可变值对象，请通过 {@link Card#redJoker()} / {@link Card#blackJoker()} 工厂方法创建。
 */
public final class JokerCard implements Card {

    private final boolean bigJoker;

    JokerCard(boolean bigJoker) {
        this.bigJoker = bigJoker;
    }

    @Override
    public boolean isJoker() {
        return true;
    }

    @Override
    public boolean isBigJoker() {
        return bigJoker;
    }

    @Override
    public Suit suit() {
        return null;
    }

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public String rankText() {
        return "JOKER";
    }

    @Override
    public String shortText() {
        return "JOKER";
    }

    @Override
    public String name() {
        return bigJoker ? "大王" : "小王";
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JokerCard c)) return false;
        return bigJoker == c.bigJoker;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bigJoker);
    }
}
