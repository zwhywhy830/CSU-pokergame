package com.csu.pokergame.core.card;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 全队第一批 JUnit 示范:用例清单来自「成员5-测试与局域网任务.md」。 */
class DeckTest {

    @Test
    void 新建标准牌堆剩余52张_跑得快牌堆剩余48张() {
        assertThat(Deck.standard52(new Random(1)).remaining())
                .as("标准 52 张(不含王)")
                .isEqualTo(52);
        assertThat(Deck.paoDeKuai(new Random(1)).remaining())
                .as("跑得快 48 张:移除 3 张非黑桃 2 与黑桃 A")
                .isEqualTo(48);
    }

    @Test
    void 摸16张后剩余数减少16() {
        Deck deck = Deck.standard52(new Random(2));
        deck.deal(16);
        assertThat(deck.remaining()).isEqualTo(52 - 16);
    }

    @Test
    void 摸空整副52张不重不漏() {
        Deck deck = Deck.standard52(new Random(3));
        List<Card> all = new ArrayList<>();
        all.addAll(deck.deal(16));
        all.addAll(deck.deal(16));
        all.addAll(deck.deal(16));
        all.addAll(deck.deal(4));

        assertThat(all).hasSize(52).doesNotHaveDuplicates();
        assertThat(deck.remaining()).isZero();
        assertThat(new HashSet<>(all)).hasSize(52);
    }

    @Test
    void 摸牌数超过剩余张数时抛IllegalArgumentException() {
        Deck deck = Deck.standard52(new Random(4));
        deck.deal(50);

        assertThatThrownBy(() -> deck.deal(3))
                .as("剩余 2 张却要摸 3 张,必须失败且不部分出牌")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Deck.paoDeKuai(new Random(4)).deal(49))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 相同seed构造两次_shuffle后摸牌顺序完全一致() {
        Deck first = Deck.standard52(new Random(42));
        Deck second = Deck.standard52(new Random(42));
        first.shuffle();
        second.shuffle();

        assertThat(first.deal(52))
                .as("确定性:同 seed 同顺序,是可测规则与联机对账的关键")
                .containsExactlyElementsOf(second.deal(52));
    }

    @Test
    void 洗牌前后牌的多重集合相等_不丢牌不造牌() {
        Deck deck = Deck.standard52(new Random(5));
        deck.shuffle();
        List<Card> afterShuffle = deck.deal(52);

        List<Card> expected = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (rank == Rank.JOKER) {
                    continue;
                }
                expected.add(new Card(rank, suit));
            }
        }
        assertThat(afterShuffle)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void 跑得快牌堆只含黑桃2_且无黑桃A() {
        List<Card> cards = Deck.paoDeKuai(new Random(6)).deal(48);

        assertThat(cards)
                .noneMatch(c -> c.rank() == Rank.TWO && c.suit() != Suit.SPADES)
                .noneMatch(c -> c.rank() == Rank.ACE && c.suit() == Suit.SPADES)
                .noneMatch(c -> c.rank() == Rank.JOKER);
    }
}
