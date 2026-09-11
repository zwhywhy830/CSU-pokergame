package com.csu.pokergame.core.card;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DeckTest {

    @Test
    void standardDeckHas52UniqueCards() {
        Deck deck = Deck.standard(new Random(7));
        assertThat(deck.draw(52)).hasSize(52).doesNotHaveDuplicates();
        assertThat(deck.remaining()).isZero();
    }

    @Test
    void sameSeedProducesSameOrder() {
        assertThat(Deck.standard(new Random(9)).draw(5))
                .containsExactlyElementsOf(Deck.standard(new Random(9)).draw(5));
    }

    @Test
    void hunanPaodekuaiDeckHas48ConfiguredCards() {
        assertThat(Deck.hunanPaodekuai(new Random(3)).draw(48))
                .hasSize(48)
                .noneMatch(card -> card.rank() == Rank.TWO && card.suit() != Suit.SPADES)
                .noneMatch(card -> card.rank() == Rank.ACE && card.suit() == Suit.SPADES);
    }
}
