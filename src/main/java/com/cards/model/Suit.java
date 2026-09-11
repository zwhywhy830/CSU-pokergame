package com.cards.model;

/**
 * 扑克牌花色：黑桃 / 红桃 / 梅花 / 方块。
 */
public enum Suit {
    SPADE("♠", "黑桃", false),
    HEART("♥", "红桃", true),
    CLUB("♣", "梅花", false),
    DIAMOND("♦", "方块", true);

    private final String glyph; // 花色图形符号
    private final String chinese; // 中文名
    private final boolean red; // 是否为红色花色

    Suit(String glyph, String chinese, boolean red) {
        this.glyph = glyph;
        this.chinese = chinese;
        this.red = red;
    }

    public String glyph() {
        return glyph;
    }

    public String chinese() {
        return chinese;
    }

    public boolean isRed() {
        return red;
    }
}
