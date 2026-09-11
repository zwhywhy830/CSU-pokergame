package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LiarRulesTest {

    private final LiarRules rules = new LiarRules();

    @Test
    void declarationIsTruthfulOnlyWhenAllMatchTarget() {
        assertThat(rules.isTruthful(List.of(new Card(Rank.ACE, Suit.CLUBS)), Rank.ACE)).isTrue();
        assertThat(rules.isTruthful(
                List.of(new Card(Rank.ACE, Suit.CLUBS), new Card(Rank.ACE, Suit.SPADES)), Rank.ACE)).isTrue();
        assertThat(rules.isTruthful(
                List.of(new Card(Rank.ACE, Suit.CLUBS), new Card(Rank.THREE, Suit.CLUBS)), Rank.ACE)).isFalse();
    }

    @Test
    void declarationCountMustBeOneToThree() {
        assertThat(rules.isValidCount(1)).isTrue();
        assertThat(rules.isValidCount(2)).isTrue();
        assertThat(rules.isValidCount(3)).isTrue();
        assertThat(rules.isValidCount(0)).isFalse();
        assertThat(rules.isValidCount(4)).isFalse();
    }
}
