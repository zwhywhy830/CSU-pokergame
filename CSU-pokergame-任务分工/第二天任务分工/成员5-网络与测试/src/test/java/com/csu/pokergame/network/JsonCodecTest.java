package com.csu.pokergame.network;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PdkSnapshot;
import com.csu.pokergame.paodekuai.PlayPdkCards;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 WireMessage、GameCommand、GameSnapshot 的 Jackson 序列化往返。 */
class JsonCodecTest {

    @Test
    void joinRoundTrip() {
        byte[] json = JsonCodec.writeBytes(new WireMessage.Join());
        WireMessage msg = JsonCodec.readMessage(json);
        assertThat(msg).isInstanceOf(WireMessage.Join.class);
    }

    @Test
    void roomSnapshotRoundTrip() {
        RoomSnapshot snap = new RoomSnapshot(
                List.of(
                        new RoomPlayer(PlayerId.SEAT_1, "玩家 1（主机）", true, true),
                        new RoomPlayer(PlayerId.SEAT_2, "玩家 2", false, true)),
                false,
                GameType.PAO_DE_KUAI);
        byte[] json = JsonCodec.writeBytes(new WireMessage.RoomSnapshotMsg(snap));
        WireMessage.RoomSnapshotMsg msg = (WireMessage.RoomSnapshotMsg) JsonCodec.readMessage(json);
        assertThat(msg.snapshot().players()).hasSize(2);
        assertThat(msg.snapshot().players().get(0).displayName()).isEqualTo("玩家 1（主机）");
        assertThat(msg.snapshot().gameType()).isEqualTo(GameType.PAO_DE_KUAI);
    }

    @Test
    void errorRoundTrip() {
        byte[] json = JsonCodec.writeBytes(new WireMessage.Error("ROOM_FULL"));
        WireMessage.Error msg = (WireMessage.Error) JsonCodec.readMessage(json);
        assertThat(msg.code()).isEqualTo("ROOM_FULL");
    }

    @Test
    void gameEndedRoundTrip() {
        byte[] json = JsonCodec.writeBytes(new WireMessage.GameEnded("PLAYER_DISCONNECTED"));
        WireMessage.GameEnded msg = (WireMessage.GameEnded) JsonCodec.readMessage(json);
        assertThat(msg.reason()).isEqualTo("PLAYER_DISCONNECTED");
    }

    @Test
    void submitCommandPlayPdkCardsRoundTrip() {
        List<Card> cards = List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.SPADES));
        GameCommand cmd = new PlayPdkCards(cards);
        byte[] json = JsonCodec.writeBytes(new WireMessage.SubmitCommand(cmd));
        WireMessage.SubmitCommand msg = (WireMessage.SubmitCommand) JsonCodec.readMessage(json);
        assertThat(msg.command()).isInstanceOf(PlayPdkCards.class);
        assertThat(((PlayPdkCards) msg.command()).cards()).containsExactlyElementsOf(cards);
    }

    @Test
    void submitCommandPassPdkTurnRoundTrip() {
        byte[] json = JsonCodec.writeBytes(new WireMessage.SubmitCommand(new PassPdkTurn()));
        WireMessage.SubmitCommand msg = (WireMessage.SubmitCommand) JsonCodec.readMessage(json);
        assertThat(msg.command()).isInstanceOf(PassPdkTurn.class);
    }

    @Test
    void submitCommandDeclareLiarCardsRoundTrip() {
        List<Card> cards = List.of(new Card(Rank.ACE, Suit.HEARTS));
        byte[] json = JsonCodec.writeBytes(new WireMessage.SubmitCommand(new DeclareLiarCards(cards)));
        WireMessage.SubmitCommand msg = (WireMessage.SubmitCommand) JsonCodec.readMessage(json);
        assertThat(msg.command()).isInstanceOf(DeclareLiarCards.class);
        assertThat(((DeclareLiarCards) msg.command()).cards()).containsExactlyElementsOf(cards);
    }

    @Test
    void submitCommandTrustAndChallengeRoundTrip() {
        byte[] jsonT = JsonCodec.writeBytes(new WireMessage.SubmitCommand(new TrustDeclaration()));
        WireMessage.SubmitCommand msgT = (WireMessage.SubmitCommand) JsonCodec.readMessage(jsonT);
        assertThat(msgT.command()).isInstanceOf(TrustDeclaration.class);

        byte[] jsonC = JsonCodec.writeBytes(new WireMessage.SubmitCommand(new ChallengeDeclaration()));
        WireMessage.SubmitCommand msgC = (WireMessage.SubmitCommand) JsonCodec.readMessage(jsonC);
        assertThat(msgC.command()).isInstanceOf(ChallengeDeclaration.class);
    }

    @Test
    void pdkSnapshotRoundTrip() {
        PdkSnapshot snap = new PdkSnapshot(
                "game-1",
                com.csu.pokergame.core.engine.GamePhase.PLAYING,
                PlayerId.SEAT_1,
                Optional.empty(),
                List.of("用户 出 A♠"),
                Map.of(PlayerId.SEAT_1, 15, PlayerId.SEAT_2, 16, PlayerId.SEAT_3, 17),
                Set.of(),
                List.of(new Card(Rank.ACE, Suit.SPADES)),
                Optional.empty(),
                Optional.empty());
        byte[] json = JsonCodec.writeBytes(new WireMessage.GameSnapshotMsg(snap));
        WireMessage.GameSnapshotMsg msg = (WireMessage.GameSnapshotMsg) JsonCodec.readMessage(json);
        GameSnapshot parsed = msg.snapshot();
        assertThat(parsed).isInstanceOf(PdkSnapshot.class);
        PdkSnapshot p = (PdkSnapshot) parsed;
        assertThat(p.gameId()).isEqualTo("game-1");
        assertThat(p.currentPlayer()).isEqualTo(PlayerId.SEAT_1);
        assertThat(p.myHand()).hasSize(1);
        assertThat(p.remainingCardCounts()).containsEntry(PlayerId.SEAT_2, 16);
    }

    @Test
    void startGameRoundTrip() {
        byte[] json = JsonCodec.writeBytes(new WireMessage.StartGame(GameType.LIARS_POKER, PlayerId.SEAT_2));
        WireMessage.StartGame msg = (WireMessage.StartGame) JsonCodec.readMessage(json);
        assertThat(msg.gameType()).isEqualTo(GameType.LIARS_POKER);
        assertThat(msg.gameType().requiredPlayers()).isEqualTo(4);
        assertThat(msg.seat()).isEqualTo(PlayerId.SEAT_2);
    }
}
