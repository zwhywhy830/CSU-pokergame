package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdkEngineTest {

    private final PaoDeKuaiRules rules = new PaoDeKuaiRules();

    private static Card c(Rank r, Suit s) {
        return new Card(r, s);
    }

    private static List<Card> of(int n, Rank r) {
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(c(r, Suit.values()[i]));
        }
        return out;
    }

    private static List<Card> run(Rank... ranks) {
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < ranks.length; i++) {
            out.add(c(ranks[i], Suit.values()[i % 4]));
        }
        return out;
    }

    private static PdkEngine engine(Map<PlayerId, List<Card>> hands, PlayerId current, PdkMove lastMove, PlayerId lastPlayer) {
        PdkState state = new PdkState(hands, null, current, lastPlayer, lastMove, 0, null,
                new HashSet<>(hands.keySet()), new ArrayList<>(), GamePhase.PLAYING);
        return new PdkEngine(state);
    }

    @Test
    void startDeals16EachAndFaceUpHolderStarts() {
        PdkEngine engine = new PdkEngine(new Random(42));
        engine.start();
        PdkSnapshot s = (PdkSnapshot) engine.snapshotFor(PlayerId.SEAT_1);
        assertThat(s.remainingCardCounts()).containsValues(16, 16, 16);

        Card faceUp = s.faceUpCard().orElseThrow();
        PlayerId first = s.currentPlayer();
        PdkSnapshot firstSnap = (PdkSnapshot) engine.snapshotFor(first);
        assertThat(firstSnap.myHand()).contains(faceUp);
    }

    @Test
    void turnAdvancesCounterClockwiseAfterPlay() {
        Map<PlayerId, List<Card>> hands = Map.of(
                PlayerId.SEAT_1, List.of(c(Rank.SEVEN, Suit.CLUBS), c(Rank.SEVEN, Suit.DIAMONDS)),
                PlayerId.SEAT_2, List.of(c(Rank.EIGHT, Suit.CLUBS)),
                PlayerId.SEAT_3, List.of(c(Rank.NINE, Suit.CLUBS)));
        PdkEngine engine = engine(hands, PlayerId.SEAT_1, null, PlayerId.SEAT_1);
        engine.apply(new PlayPdkCards(List.of(c(Rank.SEVEN, Suit.CLUBS))));
        assertThat(engine.snapshotFor(PlayerId.SEAT_1).currentPlayer()).isEqualTo(PlayerId.SEAT_2);
    }

    @Test
    void passIsIllegalWhenCanBeat() {
        PdkMove table = rules.classify(run(Rank.SEVEN)).orElseThrow();
        Map<PlayerId, List<Card>> hands = Map.of(
                PlayerId.SEAT_1, List.of(c(Rank.ACE, Suit.CLUBS), c(Rank.THREE, Suit.CLUBS)),
                PlayerId.SEAT_2, List.of(c(Rank.THREE, Suit.DIAMONDS)),
                PlayerId.SEAT_3, List.of(c(Rank.THREE, Suit.HEARTS)));
        PdkEngine engine = engine(hands, PlayerId.SEAT_1, table, PlayerId.SEAT_3);
        assertThat(engine.legalCommands(PlayerId.SEAT_1)).noneMatch(cmd -> cmd instanceof PassPdkTurn);
        assertThatThrownBy(() -> engine.apply(new PassPdkTurn()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void passIsIllegalOnNewRound() {
        Map<PlayerId, List<Card>> hands = Map.of(
                PlayerId.SEAT_1, List.of(c(Rank.SEVEN, Suit.CLUBS)),
                PlayerId.SEAT_2, List.of(c(Rank.EIGHT, Suit.CLUBS)),
                PlayerId.SEAT_3, List.of(c(Rank.NINE, Suit.CLUBS)));
        PdkEngine engine = engine(hands, PlayerId.SEAT_1, null, PlayerId.SEAT_1);
        assertThat(engine.legalCommands(PlayerId.SEAT_1)).noneMatch(cmd -> cmd instanceof PassPdkTurn);
        assertThatThrownBy(() -> engine.apply(new PassPdkTurn()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void twoPassesClearTableAndLastPlayerContinues() {
        Map<PlayerId, List<Card>> hands = Map.of(
                PlayerId.SEAT_1, List.of(c(Rank.SEVEN, Suit.CLUBS), c(Rank.EIGHT, Suit.CLUBS)),
                PlayerId.SEAT_2, List.of(c(Rank.THREE, Suit.DIAMONDS)),
                PlayerId.SEAT_3, List.of(c(Rank.FOUR, Suit.HEARTS)));
        PdkEngine engine = engine(hands, PlayerId.SEAT_1, null, PlayerId.SEAT_1);
        engine.apply(new PlayPdkCards(List.of(c(Rank.SEVEN, Suit.CLUBS)))); // SEAT_1 出 7
        engine.apply(new PassPdkTurn());                                     // SEAT_2 不能压
        engine.apply(new PassPdkTurn());                                     // SEAT_3 不能压，清桌
        PdkSnapshot s = (PdkSnapshot) engine.snapshotFor(PlayerId.SEAT_1);
        assertThat(s.currentPlayer()).isEqualTo(PlayerId.SEAT_1);
        assertThat(s.lastMove()).isEmpty();
    }

    @Test
    void firstOutWinsAndRecordsRemainingAndClosedDoor() {
        Map<PlayerId, List<Card>> hands = Map.of(
                PlayerId.SEAT_1, List.of(c(Rank.SEVEN, Suit.CLUBS)),
                PlayerId.SEAT_2, List.of(c(Rank.EIGHT, Suit.CLUBS), c(Rank.NINE, Suit.CLUBS)),
                PlayerId.SEAT_3, List.of(c(Rank.THREE, Suit.CLUBS), c(Rank.FOUR, Suit.CLUBS)));
        PdkEngine engine = engine(hands, PlayerId.SEAT_1, null, PlayerId.SEAT_1);
        engine.apply(new PlayPdkCards(List.of(c(Rank.SEVEN, Suit.CLUBS))));
        PdkSnapshot s = (PdkSnapshot) engine.snapshotFor(PlayerId.SEAT_1);
        assertThat(s.phase()).isEqualTo(GamePhase.FINISHED);
        assertThat(s.winner()).contains(PlayerId.SEAT_1);
        assertThat(s.remainingCardCounts().get(PlayerId.SEAT_2)).isEqualTo(2);
        assertThat(s.remainingCardCounts().get(PlayerId.SEAT_3)).isEqualTo(2);
        assertThat(s.closedDoorPlayers()).containsExactlyInAnyOrder(PlayerId.SEAT_2, PlayerId.SEAT_3);
    }

    @Test
    void illegalCommandsDoNotMutateState() {
        Map<PlayerId, List<Card>> hands = Map.of(
                PlayerId.SEAT_1, List.of(c(Rank.SEVEN, Suit.CLUBS), c(Rank.SEVEN, Suit.DIAMONDS)),
                PlayerId.SEAT_2, List.of(c(Rank.EIGHT, Suit.CLUBS)),
                PlayerId.SEAT_3, List.of(c(Rank.NINE, Suit.CLUBS)));
        PdkEngine engine = engine(hands, PlayerId.SEAT_1, null, PlayerId.SEAT_1);
        var before = engine.snapshotFor(PlayerId.SEAT_1);

        // 非当前玩家出牌
        assertThatThrownBy(() -> engine.apply(new PlayPdkCards(List.of(c(Rank.EIGHT, Suit.CLUBS)))))
                .isInstanceOf(IllegalArgumentException.class);
        // 牌不在手中
        assertThatThrownBy(() -> engine.apply(new PlayPdkCards(List.of(c(Rank.ACE, Suit.CLUBS)))))
                .isInstanceOf(IllegalArgumentException.class);
        // 牌型无效（两张不同点）
        assertThatThrownBy(() -> engine.apply(new PlayPdkCards(run(Rank.SEVEN, Rank.EIGHT))))
                .isInstanceOf(IllegalArgumentException.class);
        // 新一轮 pass
        assertThatThrownBy(() -> engine.apply(new PassPdkTurn()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(engine.snapshotFor(PlayerId.SEAT_1)).isEqualTo(before);
    }
}
