package com.csu.pokergame.core.card;

/** 不可变的一张牌。 */
public record Card(Rank rank, Suit suit) {

    /** 展示文字，例如 "A♠"、"10♥"。 */
    public String display() {
        return rank.label() + suit.symbol();
    }

    @Override
    public String toString() {
        return display();
    }
}
