package com.csu.pokergame.core.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 受控牌堆:构造时传入随机源,保证同 seed 同顺序(成员 5 的确定性测试依赖这一点)。
 *
 * <ul>
 *   <li>{@link #standard52(Random)}:标准 52 张(无王)。</li>
 *   <li>{@link #paoDeKuai(Random)}:跑得快 48 张,组成以 src/main/resources/rules/跑得快.md 为准。</li>
 * </ul>
 */
public final class Deck {

    private final List<Card> cards;
    private final Random random;

    /** 以给定初始牌与随机源建堆;随机源会在 {@link #shuffle()} 时复用。 */
    public Deck(List<Card> cards, Random random) {
        if (cards == null || random == null) {
            throw new IllegalArgumentException("cards 与 random 均不可为空");
        }
        this.cards = new ArrayList<>(cards);
        this.random = random;
    }

    /** 标准 52 张(不含王),构造时即用给定随机源洗牌。 */
    public static Deck standard52(Random random) {
        List<Card> all = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (rank == Rank.JOKER) {
                    continue; // 标准牌堆不含王
                }
                all.add(new Card(rank, suit));
            }
        }
        Deck deck = new Deck(all, random);
        deck.shuffle();
        return deck;
    }

    /**
     * 跑得快 48 张:标准 52 张移除红桃/梅花/方块 2(非黑桃 2)与黑桃 A,不含王。
     * 构造时即用给定随机源洗牌。
     */
    public static Deck paoDeKuai(Random random) {
        List<Card> all = new ArrayList<>(48);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (rank == Rank.JOKER) {
                    continue; // 跑得快不含王
                }
                if (rank == Rank.TWO && suit != Suit.SPADES) {
                    continue; // 移除非黑桃 2
                }
                if (rank == Rank.ACE && suit == Suit.SPADES) {
                    continue; // 移除黑桃 A
                }
                all.add(new Card(rank, suit));
            }
        }
        Deck deck = new Deck(all, random);
        deck.shuffle();
        return deck;
    }

    /** 用构造时传入的 Random 重新洗牌;同 seed 同顺序,保证测试可复现。 */
    public void shuffle() {
        Collections.shuffle(cards, random);
    }

    /** 从顶部摸 n 张;剩余不足时抛 IllegalArgumentException。 */
    public List<Card> deal(int n) {
        if (n < 0 || n > cards.size()) {
            throw new IllegalArgumentException(
                    "牌堆不足:需要 " + n + " 张,仅剩 " + cards.size() + " 张");
        }
        List<Card> dealt = new ArrayList<>(cards.subList(0, n));
        cards.subList(0, n).clear();
        return dealt;
    }

    public int remaining() {
        return cards.size();
    }
}
