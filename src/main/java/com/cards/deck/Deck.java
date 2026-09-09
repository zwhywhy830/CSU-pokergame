package com.cards.deck;

import com.cards.model.Card;

import java.util.List;

/**
 * 一副扑克牌（牌组接口）。
 * 具体牌组构成（张数、是否含王牌等）由实现类决定，如 {@link StandardDeck} 为标准 54 张。
 */
public interface Deck {

    /** 牌组包含的全部牌（按既定顺序，不可变）。 */
    List<Card> cards();

    /** 牌的张数。 */
    int size();

    /** 洗牌：返回一个随机打乱的新列表，不影响原牌组。 */
    List<Card> shuffled();

    // -------------------------------------------------------- 工厂方法

    /** 生成一副标准牌：四种花色 × 13 张 + 小王 + 大王，共 54 张。 */
    static Deck standard() {
        return new StandardDeck();
    }
}
