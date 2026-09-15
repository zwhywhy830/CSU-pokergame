package com.csu.pokergame.core.player;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.GunState;
import com.csu.pokergame.liarspoker.LiarFixedPolicy;
import com.csu.pokergame.liarspoker.LiarPhase;
import com.csu.pokergame.liarspoker.LiarRandomPolicy;
import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.paodekuai.PaoDeKuaiRules;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PdkBotPolicy;
import com.csu.pokergame.paodekuai.PdkMove;
import com.csu.pokergame.paodekuai.PdkSnapshot;
import com.csu.pokergame.paodekuai.PlayPdkCards;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBotControllerTest {

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

    private PdkSnapshot pdkSnapshot(Optional<PdkMove> lastMove) {
        return new PdkSnapshot("id", GamePhase.PLAYING, PlayerId.SEAT_1, Optional.empty(), List.of(),
                Map.of(PlayerId.SEAT_1, 5, PlayerId.SEAT_2, 6, PlayerId.SEAT_3, 6),
                Set.of(), List.of(), Optional.empty(), lastMove);
    }

    private PdkMove move(List<Card> cards) {
        return rules.classify(cards).orElseThrow();
    }

    // ---------- 跑得快 ----------

    @Test
    void pdkBotPrefersMostCards() {
        PdkBotPolicy policy = new PdkBotPolicy();
        List<GameCommand> legal = List.of(
                new PlayPdkCards(run(Rank.NINE)),                                        // 单张 1 张
                new PlayPdkCards(of(2, Rank.FIVE)),                                      // 对子 2 张
                new PlayPdkCards(run(Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN))); // 顺子 5 张
        BotDecision d = policy.decide(pdkSnapshot(Optional.empty()), legal);
        assertThat(d.command()).isEqualTo(new PlayPdkCards(run(Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN)));
    }

    @Test
    void pdkBotUsesBombWhenTableHasCards() {
        PdkBotPolicy policy = new PdkBotPolicy();
        PdkMove table = move(run(Rank.THREE)); // 桌面单张 3
        List<GameCommand> legal = List.of(
                new PlayPdkCards(of(4, Rank.SEVEN)), // 炸弹
                new PlayPdkCards(run(Rank.NINE)));   // 单张 9 也能压
        BotDecision d = policy.decide(pdkSnapshot(Optional.of(table)), legal);
        assertThat(d.command()).isEqualTo(new PlayPdkCards(of(4, Rank.SEVEN)));
    }

    @Test
    void pdkBotDoesNotLeadWithBomb() {
        PdkBotPolicy policy = new PdkBotPolicy();
        List<GameCommand> legal = List.of(
                new PlayPdkCards(of(4, Rank.EIGHT)), // 炸弹
                new PlayPdkCards(of(2, Rank.FIVE)),  // 对子 2 张
                new PlayPdkCards(run(Rank.NINE)));   // 单张 1 张
        BotDecision d = policy.decide(pdkSnapshot(Optional.empty()), legal);
        assertThat(d.command()).isEqualTo(new PlayPdkCards(of(2, Rank.FIVE))); // 不先出炸弹，出对子
    }

    @Test
    void pdkBotPassesWhenOnlyPassAvailable() {
        PdkBotPolicy policy = new PdkBotPolicy();
        BotDecision d = policy.decide(pdkSnapshot(Optional.empty()), List.of(new PassPdkTurn()));
        assertThat(d.command()).isInstanceOf(PassPdkTurn.class);
    }

    // ---------- 骗子酒馆 ----------

    private LiarSnapshot respondSnapshot() {
        return new LiarSnapshot("id", GamePhase.PLAYING, PlayerId.SEAT_2, Optional.empty(), List.of(),
                LiarPhase.RESPOND, Rank.ACE,
                Set.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3),
                Map.of(PlayerId.SEAT_1, new GunState(6, 0),
                        PlayerId.SEAT_2, new GunState(6, 0),
                        PlayerId.SEAT_3, new GunState(6, 0)),
                List.of(), PlayerId.SEAT_1, 1, Set.of(), Optional.empty());
    }

    @Test
    void liarRandomPolicyReturnsLegalCommand() {
        LiarRandomPolicy policy = new LiarRandomPolicy(new Random(42));
        List<GameCommand> legal = List.of(new TrustDeclaration(), new ChallengeDeclaration());
        for (int i = 0; i < 30; i++) {
            BotDecision d = policy.decide(respondSnapshot(), legal);
            assertThat(legal).contains(d.command());
        }
    }

    @Test
    void liarFixedPolicyAlwaysChallenges() {
        LiarFixedPolicy policy = new LiarFixedPolicy(true);
        List<GameCommand> legal = List.of(new TrustDeclaration(), new ChallengeDeclaration());
        assertThat(policy.decide(respondSnapshot(), legal).command()).isInstanceOf(ChallengeDeclaration.class);
    }

    @Test
    void liarFixedPolicyAlwaysTrusts() {
        LiarFixedPolicy policy = new LiarFixedPolicy(false);
        List<GameCommand> legal = List.of(new TrustDeclaration(), new ChallengeDeclaration());
        assertThat(policy.decide(respondSnapshot(), legal).command()).isInstanceOf(TrustDeclaration.class);
    }

    // ---------- 控制器 ----------

    @Test
    void controllerReturnsCommandWithinLegal() {
        PdkBotPolicy policy = new PdkBotPolicy();
        RuleBotController controller = new RuleBotController(policy);
        List<GameCommand> legal = List.of(new PassPdkTurn());
        GameSnapshot snap = pdkSnapshot(Optional.empty());
        GameCommand cmd = controller.choose(snap, legal).join();
        assertThat(legal).contains(cmd);
    }
}
