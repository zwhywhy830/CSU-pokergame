package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaoDeKuaiRulesTest {

    private final PaoDeKuaiRules rules = new PaoDeKuaiRules();

    // ---------- 构造辅助 ----------

    private static Card c(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }

    /** count 张同点（用不同花色）。 */
    private static List<Card> of(int count, Rank rank) {
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(c(rank, Suit.values()[i]));
        }
        return out;
    }

    /** 连续单张（点数由低到高）。 */
    private static List<Card> run(Rank... ranks) {
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < ranks.length; i++) {
            out.add(c(ranks[i], Suit.values()[i % 4]));
        }
        return out;
    }

    private static List<Card> concat(List<Card> a, List<Card> b) {
        List<Card> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    // ---------- 牌型识别 ----------

    @Test
    void classifiesSinglePairAndTripleFamily() {
        assertThat(rules.classify(run(Rank.SEVEN)).orElseThrow().type()).isEqualTo(PdkMoveType.SINGLE);
        assertThat(rules.classify(of(2, Rank.SEVEN)).orElseThrow().type()).isEqualTo(PdkMoveType.PAIR);
        assertThat(rules.classify(of(3, Rank.NINE)).orElseThrow().type()).isEqualTo(PdkMoveType.TRIPLE);
        assertThat(rules.classify(concat(of(3, Rank.NINE), run(Rank.FIVE))).orElseThrow().type())
                .isEqualTo(PdkMoveType.TRIPLE_WITH_ONE);
        assertThat(rules.classify(concat(of(3, Rank.NINE), of(2, Rank.FIVE))).orElseThrow().type())
                .isEqualTo(PdkMoveType.TRIPLE_WITH_PAIR);
    }

    @Test
    void classifiesStraightConsecutivePairsAndTripleStraight() {
        assertThat(rules.classify(run(Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN)).orElseThrow().type())
                .isEqualTo(PdkMoveType.STRAIGHT);
        assertThat(rules.classify(concat(of(2, Rank.FOUR), of(2, Rank.FIVE))).orElseThrow().type())
                .isEqualTo(PdkMoveType.CONSECUTIVE_PAIRS);
        assertThat(rules.classify(concat(of(3, Rank.FOUR), of(3, Rank.FIVE))).orElseThrow().type())
                .isEqualTo(PdkMoveType.TRIPLE_STRAIGHT);
    }

    @Test
    void classifiesAirplaneWithWings() {
        assertThat(rules.classify(concat(concat(of(3, Rank.THREE), of(3, Rank.FOUR)), run(Rank.SEVEN, Rank.EIGHT)))
                .orElseThrow().type()).isEqualTo(PdkMoveType.AIRPLANE_WITH_WINGS);
    }

    @Test
    void classifiesFourWithOneAndFourWithThree() {
        assertThat(rules.classify(concat(of(4, Rank.KING), run(Rank.THREE))).orElseThrow().type())
                .isEqualTo(PdkMoveType.FOUR_WITH_ONE);
        assertThat(rules.classify(concat(of(4, Rank.KING), of(3, Rank.FIVE))).orElseThrow().type())
                .isEqualTo(PdkMoveType.FOUR_WITH_THREE);
    }

    @Test
    void pureFourOfAKindIsBomb() {
        assertThat(rules.classify(of(4, Rank.KING)).orElseThrow().type()).isEqualTo(PdkMoveType.FOUR_OF_A_KIND);
    }

    // ---------- 反例 ----------

    static Stream<List<Card>> invalidRuns() {
        return Stream.of(
                run(Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE, Rank.TWO),          // 顺子含 2
                concat(of(2, Rank.ACE), of(2, Rank.TWO)),                           // 连对含 2
                concat(of(3, Rank.ACE), of(3, Rank.TWO))                            // 三顺含 2
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRuns")
    void runsCannotContainTwo(List<Card> cards) {
        assertThat(rules.classify(cards)).isEmpty();
    }

    @Test
    void tripleWithOneIsValidAlways() {
        List<Card> tripleWithOne = concat(of(3, Rank.NINE), run(Rank.FIVE));
        assertThat(rules.classify(tripleWithOne).orElseThrow().type()).isEqualTo(PdkMoveType.TRIPLE_WITH_ONE);
    }

    @Test
    void bareTripleIsValidAlways() {
        assertThat(rules.classify(of(3, Rank.NINE)).orElseThrow().type()).isEqualTo(PdkMoveType.TRIPLE);
    }

    // ---------- 比较 ----------

    @Test
    void bombBeatsAnyNonBomb() {
        PdkMove bomb = rules.classify(of(4, Rank.SEVEN)).orElseThrow();
        PdkMove straight = rules.classify(run(Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN)).orElseThrow();
        assertThat(rules.canBeat(bomb, straight)).isTrue();
    }

    @Test
    void smallerBombCannotBeatLargerBomb() {
        PdkMove small = rules.classify(of(4, Rank.FIVE)).orElseThrow();
        PdkMove large = rules.classify(of(4, Rank.NINE)).orElseThrow();
        assertThat(rules.canBeat(small, large)).isFalse();
    }

    @Test
    void sameRankDifferentSuitCannotBeat() {
        PdkMove a = rules.classify(run(Rank.SEVEN)).orElseThrow();
        PdkMove b = rules.classify(List.of(c(Rank.SEVEN, Suit.DIAMONDS))).orElseThrow();
        assertThat(rules.canBeat(a, b)).isFalse();
    }

    @Test
    void differentLengthTripleStraightCannotBeat() {
        PdkMove two = rules.classify(concat(of(3, Rank.FOUR), of(3, Rank.FIVE))).orElseThrow();
        PdkMove three = rules.classify(concat(concat(of(3, Rank.THREE), of(3, Rank.FOUR)), of(3, Rank.FIVE))).orElseThrow();
        assertThat(rules.canBeat(two, three)).isFalse();
    }

    @Test
    void fourWithThreeIsNotABombAndCannotBeatStraight() {
        PdkMove fourWithThree = rules.classify(concat(of(4, Rank.KING), of(3, Rank.FIVE))).orElseThrow();
        PdkMove straight = rules.classify(run(Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN)).orElseThrow();
        assertThat(rules.canBeat(fourWithThree, straight)).isFalse();
    }
}
