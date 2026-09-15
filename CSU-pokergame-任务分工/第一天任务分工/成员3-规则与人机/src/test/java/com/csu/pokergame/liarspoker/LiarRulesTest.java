package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import com.csu.pokergame.core.engine.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 用例清单来自「成员3-规则与人机任务.md」,按新版规则(6 仓左轮)编写。 */
class LiarRulesTest {

    @Test
    void 宣告张数_1到3张合法() {
        assertThat(LiarRules.isValidDeclarationCount(1)).isTrue();
        assertThat(LiarRules.isValidDeclarationCount(2)).isTrue();
        assertThat(LiarRules.isValidDeclarationCount(3)).isTrue();
    }

    @Test
    void 宣告张数_0张与4张不合法() {
        assertThat(LiarRules.isValidDeclarationCount(0)).isFalse();
        assertThat(LiarRules.isValidDeclarationCount(4)).isFalse();
    }

    @Test
    void 宣告属实时_质疑者扣扳机() {
        PlayerId shooter = LiarRules.whoShoots(true, PlayerId.SEAT_1, PlayerId.SEAT_2);

        assertThat(shooter)
                .as("质疑失败(宣告属实)由质疑者承担后果")
                .isEqualTo(PlayerId.SEAT_2);
    }

    @Test
    void 宣告被拆穿时_宣告者扣扳机() {
        PlayerId shooter = LiarRules.whoShoots(false, PlayerId.SEAT_1, PlayerId.SEAT_2);

        assertThat(shooter)
                .as("撒谎被拆穿由宣告者承担后果")
                .isEqualTo(PlayerId.SEAT_1);
    }

    @Test
    void 真伪判定_全部是目标点数即属实() {
        List<Card> cards = List.of(
                new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.KING, Suit.SPADES));

        assertThat(LiarRules.isTruthful(cards, Rank.KING)).isTrue();
    }

    @Test
    void 真伪判定_JOKER是万能牌_可以顶替目标点数() {
        List<Card> queenPlusJoker = List.of(
                new Card(Rank.QUEEN, Suit.HEARTS),
                new Card(Rank.JOKER, Suit.SPADES));
        List<Card> allJokers = List.of(
                new Card(Rank.JOKER, Suit.HEARTS),
                new Card(Rank.JOKER, Suit.SPADES));

        assertThat(LiarRules.isTruthful(queenPlusJoker, Rank.QUEEN)).isTrue();
        assertThat(LiarRules.isTruthful(allJokers, Rank.KING)).isTrue();
    }

    @Test
    void 真伪判定_JOKER只顶替一张_其余牌仍需匹配目标点数() {
        List<Card> kingPlusJoker = List.of(
                new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.JOKER, Suit.SPADES));

        assertThat(LiarRules.isTruthful(kingPlusJoker, Rank.QUEEN))
                .as("KING 不是 QUEEN,JOKER 万能牌不能把整手牌都变成目标点数")
                .isFalse();
    }

    @Test
    void 真伪判定_混入无关点数即被拆穿() {
        List<Card> cards = List.of(
                new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.QUEEN, Suit.SPADES));

        assertThat(LiarRules.isTruthful(cards, Rank.KING)).isFalse();
    }
}
