package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** 用例清单来自「成员3-规则与人机任务.md」,先写用例后写实现。 */
class PdkRulesTest {

    private static Card card(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }

    @Test
    void 两张同点识别为对子_主点数为A() {
        Optional<PdkMove> move = PdkRules.parse(List.of(
                card(Rank.ACE, Suit.SPADES), card(Rank.ACE, Suit.HEARTS)));

        assertThat(move).isPresent();
        assertThat(move.get().type()).isEqualTo(PdkMoveType.PAIR);
        assertThat(move.get().mainRank()).isEqualTo(Rank.ACE);
    }

    @Test
    void 五张连续同花色识别为顺子() {
        Optional<PdkMove> move = PdkRules.parse(List.of(
                card(Rank.THREE, Suit.SPADES), card(Rank.FOUR, Suit.SPADES),
                card(Rank.FIVE, Suit.SPADES), card(Rank.SIX, Suit.SPADES),
                card(Rank.SEVEN, Suit.SPADES)));

        assertThat(move).isPresent();
        assertThat(move.get().type()).isEqualTo(PdkMoveType.STRAIGHT);
        assertThat(move.get().mainRank()).isEqualTo(Rank.SEVEN);
    }

    @Test
    void 顺子包含2不是合法牌型_顺子不能带2() {
        Optional<PdkMove> move = PdkRules.parse(List.of(
                card(Rank.TEN, Suit.SPADES), card(Rank.JACK, Suit.HEARTS),
                card(Rank.QUEEN, Suit.CLUBS), card(Rank.KING, Suit.DIAMONDS),
                card(Rank.TWO, Suit.SPADES)));

        assertThat(move).as("顺子范围 3–A,不得包含 2").isEmpty();
    }

    @Test
    void 三张同点加一张散牌识别为三带一() {
        Optional<PdkMove> move = PdkRules.parse(List.of(
                card(Rank.FIVE, Suit.SPADES), card(Rank.FIVE, Suit.HEARTS),
                card(Rank.FIVE, Suit.DIAMONDS), card(Rank.EIGHT, Suit.SPADES)));

        assertThat(move).isPresent();
        assertThat(move.get().type()).isEqualTo(PdkMoveType.TRIPLE_WITH_SINGLE);
        assertThat(move.get().mainRank()).isEqualTo(Rank.FIVE);
    }

    @Test
    void 同为对子_AA大于99() {
        Optional<PdkMove> aa = PdkRules.parse(List.of(
                card(Rank.ACE, Suit.SPADES), card(Rank.ACE, Suit.HEARTS)));
        Optional<PdkMove> nines = PdkRules.parse(List.of(
                card(Rank.NINE, Suit.SPADES), card(Rank.NINE, Suit.HEARTS)));

        assertThat(aa).isPresent();
        assertThat(nines).isPresent();
        assertThat(PdkRules.beats(aa.get(), nines.get()))
                .as("同为对子按主点数比,A > 9")
                .isTrue();
    }

    @Test
    void 不同牌型不可比较_顺子压不了三张() {
        Optional<PdkMove> straight = PdkRules.parse(List.of(
                card(Rank.THREE, Suit.SPADES), card(Rank.FOUR, Suit.SPADES),
                card(Rank.FIVE, Suit.SPADES), card(Rank.SIX, Suit.SPADES),
                card(Rank.SEVEN, Suit.SPADES)));
        Optional<PdkMove> triple = PdkRules.parse(List.of(
                card(Rank.TWO, Suit.SPADES), card(Rank.TWO, Suit.HEARTS),
                card(Rank.TWO, Suit.DIAMONDS)));

        assertThat(straight).isPresent();
        assertThat(triple).isPresent();
        assertThat(PdkRules.beats(straight.get(), triple.get()))
                .as("仅同型可比,顺子与三张互不压")
                .isFalse();
    }

    @Test
    void 单张比较_2大于A() {
        Optional<PdkMove> two = PdkRules.parse(List.of(card(Rank.TWO, Suit.SPADES)));
        Optional<PdkMove> ace = PdkRules.parse(List.of(card(Rank.ACE, Suit.HEARTS)));

        assertThat(two).isPresent();
        assertThat(ace).isPresent();
        assertThat(PdkRules.beats(two.get(), ace.get()))
                .as("跑得快里 2 是最大的单张")
                .isTrue();
    }

    @Test
    void 张数不符的同型不是合法牌型_四张当对子返回empty() {
        Optional<PdkMove> move = PdkRules.parse(List.of(
                card(Rank.FIVE, Suit.SPADES), card(Rank.FIVE, Suit.HEARTS),
                card(Rank.FIVE, Suit.DIAMONDS), card(Rank.FIVE, Suit.CLUBS)));

        assertThat(move).as("对子只允许 2 张;纯 4 张同点是炸弹,属 Day 2 扩展").isEmpty();
    }

    @Test
    void 自由出牌时_桌面为null任何合法牌型都算大() {
        Optional<PdkMove> single = PdkRules.parse(List.of(card(Rank.THREE, Suit.SPADES)));

        assertThat(single).isPresent();
        assertThat(PdkRules.beats(single.get(), null))
                .as("新一轮桌面为空,任何合法牌型都可以出")
                .isTrue();
    }

    @Test
    void 空列表与null不是合法牌型() {
        assertThat(PdkRules.parse(List.of())).isEmpty();
        assertThat(PdkRules.parse(null)).isEmpty();
    }

    @Test
    void 同为顺子_张数相同才可比较主点数() {
        Optional<PdkMove> low = PdkRules.parse(List.of(
                card(Rank.THREE, Suit.SPADES), card(Rank.FOUR, Suit.HEARTS),
                card(Rank.FIVE, Suit.CLUBS), card(Rank.SIX, Suit.DIAMONDS),
                card(Rank.SEVEN, Suit.SPADES)));
        Optional<PdkMove> high = PdkRules.parse(List.of(
                card(Rank.NINE, Suit.SPADES), card(Rank.TEN, Suit.HEARTS),
                card(Rank.JACK, Suit.CLUBS), card(Rank.QUEEN, Suit.DIAMONDS),
                card(Rank.KING, Suit.SPADES)));

        assertThat(low).isPresent();
        assertThat(high).isPresent();
        assertThat(PdkRules.beats(high.get(), low.get())).isTrue();
        assertThat(PdkRules.beats(low.get(), high.get())).isFalse();
    }

    @Test
    void 三张同点加一对识别为三带二_主点数取三条() {
        Optional<PdkMove> move = PdkRules.parse(List.of(
                card(Rank.KING, Suit.SPADES), card(Rank.KING, Suit.HEARTS),
                card(Rank.KING, Suit.DIAMONDS), card(Rank.NINE, Suit.CLUBS),
                card(Rank.NINE, Suit.SPADES)));

        assertThat(move).isPresent();
        assertThat(move.get().type()).isEqualTo(PdkMoveType.TRIPLE_WITH_PAIR);
        assertThat(move.get().mainRank()).as("三带二按三条的点数比较").isEqualTo(Rank.KING);
    }
}
