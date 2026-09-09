package com.cards.deck;

import com.cards.model.Card;
import com.cards.model.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 标准扑克牌组实现：四种花色 × 13 张 + 小王 + 大王，共 54 张。
 * 牌组在构造时生成且不可变；{@link #shuffled()} 返回打乱后的副本。
 */
public final class StandardDeck implements Deck {

    private final List<Card> cards;

    public StandardDeck() {
        List<Card> deck = new ArrayList<>(54);
        for (Suit suit : Suit.values()) {
            for (int rank = Card.RANK_A; rank <= Card.RANK_K; rank++) {
                deck.add(Card.of(suit, rank));
            }
        }
        deck.add(Card.blackJoker()); // 小王
        deck.add(Card.redJoker()); // 大王
        this.cards = List.copyOf(deck);
    }

    @Override
    public List<Card> cards() {
        return cards;
    }

    @Override
    public int size() {
        return cards.size();
    }

    @Override
    public List<Card> shuffled() {
        List<Card> copy = new ArrayList<>(cards);
        Collections.shuffle(copy);
        return copy;
    }
}
