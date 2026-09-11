package com.csu.pokergame.core.card;

/** 花色。跑得快只比点数不比花色，花色仅用于展示与构造牌。 */
public enum Suit {
    CLUBS("♣"),
    DIAMONDS("♦"),
    HEARTS("♥"),
    SPADES("♠");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
