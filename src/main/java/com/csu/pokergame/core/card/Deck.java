package com.csu.pokergame.core.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** 受控牌堆：构造时洗牌，按序 draw。不可变的使用方式（draw 后返回剩余的新状态）。 */
public final class Deck {

    private final List<Card> cards;

    private Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    /** 标准 52 张牌（不含 JOKER），按给定随机源洗牌。 */
    public static Deck standard(Random random) {
        List<Card> all = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (rank == Rank.JOKER) {
                    continue;
                }
                all.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(all, random);
        return new Deck(all);
    }

    /**
     * 湖南跑得快 48 张：从标准 52 张移除红桃/梅花/方块 2 与黑桃 A（不含 JOKER）。
     */
    public static Deck hunanPaodekuai(Random random) {
        List<Card> all = new ArrayList<>(48);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (rank == Rank.JOKER) {
                    continue;
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
        Collections.shuffle(all, random);
        return new Deck(all);
    }

    /**
     * 骗子酒馆 20 张：K、Q、A 各 6 张 + 2 张 JOKER。
     * K/Q/A 每点 6 张用 4 花色循环分配（花色无比较意义，仅区分张数）。
     */
    public static Deck liarPoker(Random random) {
        List<Card> all = new ArrayList<>(20);
        Rank[] ranks = {Rank.KING, Rank.QUEEN, Rank.ACE};
        Suit[] suits = Suit.values();
        for (Rank rank : ranks) {
            for (int i = 0; i < 6; i++) {
                all.add(new Card(rank, suits[i % suits.length]));
            }
        }
        all.add(new Card(Rank.JOKER, Suit.HEARTS));
        all.add(new Card(Rank.JOKER, Suit.SPADES));
        Collections.shuffle(all, random);
        return new Deck(all);
    }

    /** 抽取 n 张；不足时抛异常。 */
    public List<Card> draw(int n) {
        if (n < 0 || n > cards.size()) {
            throw new IllegalStateException("牌堆不足：需要 " + n + " 张，仅剩 " + cards.size() + " 张");
        }
        List<Card> drawn = new ArrayList<>(cards.subList(0, n));
        cards.subList(0, n).clear();
        return drawn;
    }

    public int remaining() {
        return cards.size();
    }
}
