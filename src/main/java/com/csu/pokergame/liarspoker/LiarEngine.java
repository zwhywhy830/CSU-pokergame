package com.csu.pokergame.liarspoker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Deck;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameEngine;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

/**
 * 骗子酒馆引擎。四人（SEAT_1 本地 + SEAT_2/3/4 机器人），每人 1 滴血（中弹淘汰）。
 * 每人一把 6 仓手枪，开局随机决定子弹所在仓；被质疑失败后扣扳机一次，打到子弹仓淘汰。
 * 质疑结算或玩家出完手牌后小局结束，重新洗牌发牌并重选主牌（目标点数），仅保留手枪与淘汰状态。
 * 本地玩家中弹即结束对局（提示输了）；其他玩家淘汰后继续，直到只剩一名存活者。
 */
public final class LiarEngine implements GameEngine {

    /** 骗子酒馆为四人游戏，固定四个座位。 */
    static final List<PlayerId> SEATS =
            List.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4);

    /** 本地玩家。 */
    static final PlayerId LOCAL = PlayerId.SEAT_1;

    private final String gameId;
    private final LiarRules rules = new LiarRules();
    private final Random random;
    private LiarState state;

    /** 供测试注入已构造状态。 */
    public LiarEngine(LiarState initialState) {
        this.gameId = "liar-" + Integer.toHexString(System.identityHashCode(initialState));
        this.random = null;
        this.state = initialState;
    }

    /** 生产用：以给定随机源发牌与分配手枪。 */
    public LiarEngine(Random random) {
        this.gameId = "liar-" + Long.toHexString(random.nextLong());
        this.random = random;
        this.state = null;
    }

    /** 发牌：四人各 5 张，正好使用 20 张牌组。 */
    private Map<PlayerId, List<Card>> dealHands(Random r) {
        Deck deck = Deck.liarPoker(r);
        List<Card> cards = deck.draw(20);
        Map<PlayerId, List<Card>> hands = new EnumMap<>(PlayerId.class);
        hands.put(PlayerId.SEAT_1, new ArrayList<>(cards.subList(0, 5)));
        hands.put(PlayerId.SEAT_2, new ArrayList<>(cards.subList(5, 10)));
        hands.put(PlayerId.SEAT_3, new ArrayList<>(cards.subList(10, 15)));
        hands.put(PlayerId.SEAT_4, new ArrayList<>(cards.subList(15, 20)));
        return hands;
    }

    /** 从 K/Q/A 随机抽一张作为整局目标点数。 */
    private static Rank randomTarget(Random r) {
        return switch (r.nextInt(3)) {
            case 0 -> Rank.KING;
            case 1 -> Rank.QUEEN;
            default -> Rank.ACE;
        };
    }

    @Override
    public void start() {
        if (state != null) {
            throw new IllegalStateException("游戏已开始");
        }
        Random r = random != null ? random : new Random(gameId.hashCode());

        // 每个玩家一把手枪：子弹随机装入 1–6 仓之一
        Map<PlayerId, GunState> guns = new EnumMap<>(PlayerId.class);
        for (PlayerId p : SEATS) {
            guns.put(p, new GunState(r.nextInt(6) + 1, 0));
        }

        Map<PlayerId, List<Card>> hands = dealHands(r);
        Rank target = randomTarget(r);

        Set<PlayerId> alive = new LinkedHashSet<>(SEATS);
        state = new LiarState(hands, alive, guns, target, LiarPhase.DECLARE, PlayerId.SEAT_1, null,
                List.of(), null, null,
                new ArrayList<>(List.of("发牌完成，目标点数 " + target.label() + "，轮到 " + name(PlayerId.SEAT_1))));
    }

    @Override
    public GameSnapshot snapshotFor(PlayerId viewer) {
        LiarState s = requireStarted();
        Set<PlayerId> eliminated = eliminatedPlayers(s);
        PlayerId current = switch (s.phase()) {
            case DECLARE -> s.declarer();
            case RESPOND -> s.responder();
            case RESOLVE, FINISHED -> s.winner() != null ? s.winner() : s.declarer();
        };
        return new LiarSnapshot(
                gameId,
                mapPhase(s.phase()),
                current,
                Optional.ofNullable(s.winner()),
                List.copyOf(s.publicEvents()),
                s.phase(),
                s.targetRank(),
                Set.copyOf(s.alive()),
                new EnumMap<>(s.guns()),
                List.copyOf(s.hands().get(viewer)),
                s.declarer(),
                s.pendingDeclaration().size(),
                Set.copyOf(eliminated),
                Optional.ofNullable(s.lastResolution()));
    }

    @Override
    public List<GameCommand> legalCommands(PlayerId player) {
        LiarState s = requireStarted();
        if (s.winner() != null) {
            return List.of();
        }
        return switch (s.phase()) {
            case DECLARE -> s.declarer() == player ? declareCommands(s.hands().get(player)) : List.of();
            case RESPOND -> s.responder() == player
                    ? List.of(new TrustDeclaration(), new ChallengeDeclaration())
                    : List.of();
            case RESOLVE, FINISHED -> List.of();
        };
    }

    @Override
    public void apply(GameCommand command) {
        LiarState s = requireStarted();
        if (s.winner() != null) {
            throw new IllegalArgumentException("对局已结束");
        }

        if (command instanceof DeclareLiarCards declare) {
            applyDeclare(s, declare);
            return;
        }
        if (command instanceof TrustDeclaration) {
            applyTrust(s);
            return;
        }
        if (command instanceof ChallengeDeclaration) {
            applyChallenge(s);
            return;
        }
        throw new IllegalArgumentException("未知指令");
    }

    // ---------- 状态推进 ----------

    private void applyDeclare(LiarState s, DeclareLiarCards declare) {
        if (s.phase() != LiarPhase.DECLARE) {
            throw new IllegalArgumentException("当前不是宣告阶段");
        }
        if (declare.cards() == null || !rules.isValidCount(declare.cards().size())) {
            throw new IllegalArgumentException("宣告张数必须为 1–3");
        }
        List<Card> hand = s.hands().get(s.declarer());
        List<Card> newHand = new ArrayList<>(hand);
        for (Card c : declare.cards()) {
            if (!newHand.remove(c)) {
                throw new IllegalArgumentException("牌不在手中");
            }
        }
        Map<PlayerId, List<Card>> newHands = new EnumMap<>(s.hands());
        newHands.put(s.declarer(), newHand);

        PlayerId responder = nextAlive(s.declarer(), s.alive());
        state = new LiarState(newHands, s.alive(), s.guns(), s.targetRank(),
                LiarPhase.RESPOND, s.declarer(), responder,
                List.copyOf(declare.cards()), s.lastResolution(), s.winner(),
                withEvent(s.publicEvents(),
                        name(s.declarer()) + " 宣告 " + declare.cards().size() + " 张 " + s.targetRank().label()));

        // 出完手牌 → 重新洗牌（保留手枪与淘汰状态），responder 接下来宣告
        if (newHand.isEmpty()) {
            reshuffleAndDeal(name(s.declarer()) + " 出完手牌，重新洗牌发牌", responder,
                    LiarPhase.DECLARE, null, null);
        }
    }

    private void applyTrust(LiarState s) {
        if (s.phase() != LiarPhase.RESPOND) {
            throw new IllegalArgumentException("当前不是回应阶段");
        }
        PlayerId next = s.responder();
        state = new LiarState(s.hands(), s.alive(), s.guns(), s.targetRank(),
                LiarPhase.DECLARE, next, null,
                List.of(), s.lastResolution(), s.winner(),
                withEvent(s.publicEvents(), name(s.responder()) + " 相信，轮到其出牌"));
    }

    private void applyChallenge(LiarState s) {
        if (s.phase() != LiarPhase.RESPOND) {
            throw new IllegalArgumentException("当前不是回应阶段");
        }
        boolean truthful = rules.isTruthful(s.pendingDeclaration(), s.targetRank());
        // 输的一方扣扳机
        PlayerId shooter = truthful ? s.responder() : s.declarer();
        GunState gunBefore = s.guns().get(shooter);
        boolean hit = gunBefore.wouldHit();
        boolean killed = hit; // 中弹即淘汰

        Map<PlayerId, GunState> newGuns = new EnumMap<>(s.guns());
        newGuns.put(shooter, gunBefore.fire());

        Set<PlayerId> newAlive = new LinkedHashSet<>(s.alive());
        if (killed) {
            newAlive.remove(shooter);
        }

        LiarResolution resolution = new LiarResolution(s.declarer(), s.pendingDeclaration().size(),
                List.copyOf(s.pendingDeclaration()), s.responder(), truthful, shooter, hit, killed);

        // 胜负判定
        PlayerId winner = null;
        if (killed && shooter == LOCAL) {
            // 本地玩家死亡 → 立即结束，winner 设为某存活者（UI 根据 winner!=LOCAL 显示「你输了」）
            winner = newAlive.isEmpty() ? null : newAlive.iterator().next();
        } else if (newAlive.size() == 1) {
            // 只剩一名存活者 → 该玩家获胜
            winner = newAlive.iterator().next();
        }

        LiarPhase phase = winner != null ? LiarPhase.FINISHED : LiarPhase.DECLARE;
        PlayerId nextDeclarer = winner != null ? null : nextAlive(shooter, newAlive);

        // 不论中弹与否，都先记录事件
        List<String> events = withEvent(s.publicEvents(),
                (truthful ? "宣告属实，" : "宣告被拆穿，") + name(shooter) + " 扣扳机"
                        + (hit ? "，中弹被淘汰" : "，空仓存活"));

        if (winner != null) {
            events = appendEvent(events, name(winner) + " 获胜，对局结束");
            // 不再洗牌，仅推进到 FINISHED
            state = new LiarState(s.hands(), newAlive, newGuns, s.targetRank(),
                    phase, nextDeclarer, null, List.of(), resolution, winner, events);
            return;
        }

        // 小局结束：重新洗牌发牌并重选主牌（目标点数），仅保留手枪与淘汰状态
        Random r = random != null ? random : new Random();
        Rank newTarget = randomTarget(r);
        Map<PlayerId, List<Card>> newHands = dealHands(r);
        events = appendEvent(events, "重新洗牌发牌，新目标点数 " + newTarget.label()
                + "，轮到 " + name(nextDeclarer));
        state = new LiarState(newHands, newAlive, newGuns, newTarget,
                phase, nextDeclarer, null, List.of(), resolution, winner, events);
    }

    // ---------- 辅助 ----------

    /** 出完手牌后的重新洗牌：进入新小局，保留 alive/guns，重新发 hands 并重选目标点数。 */
    private void reshuffleAndDeal(String event, PlayerId nextDeclarer, LiarPhase phase, LiarResolution resolution,
                                 PlayerId winner) {
        LiarState s = state;
        Random r = random != null ? random : new Random();
        Rank newTarget = randomTarget(r);
        Map<PlayerId, List<Card>> newHands = dealHands(r);
        List<String> events = withEvent(s.publicEvents(), event);
        events = appendEvent(events, "新小局目标点数 " + newTarget.label());
        state = new LiarState(newHands, s.alive(), s.guns(), newTarget,
                phase, nextDeclarer, null, List.of(),
                resolution != null ? resolution : s.lastResolution(), winner, events);
    }

    private LiarState requireStarted() {
        if (state == null) {
            throw new IllegalStateException("游戏尚未开始");
        }
        return state;
    }

    private static GamePhase mapPhase(LiarPhase p) {
        return p == LiarPhase.FINISHED ? GamePhase.FINISHED : GamePhase.PLAYING;
    }

    private static Set<PlayerId> eliminatedPlayers(LiarState s) {
        Set<PlayerId> out = new LinkedHashSet<>();
        for (PlayerId p : SEATS) {
            if (!s.alive().contains(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** 四人逆时针下一位存活者。 */
    private static PlayerId nextAlive(PlayerId from, Set<PlayerId> alive) {
        PlayerId p = nextSeat(from);
        for (int i = 0; i < 4; i++) {
            if (alive.contains(p)) {
                return p;
            }
            p = nextSeat(p);
        }
        return from;
    }

    /** 四人逆时针：SEAT_1 → SEAT_2 → SEAT_3 → SEAT_4 → SEAT_1。 */
    static PlayerId nextSeat(PlayerId p) {
        return p.next();
    }

    private static String name(PlayerId p) {
        return switch (p) {
            case SEAT_1 -> "用户";
            case SEAT_2 -> "AI1";
            case SEAT_3 -> "AI2";
            case SEAT_4 -> "AI3";
        };
    }

    private List<GameCommand> declareCommands(List<Card> hand) {
        List<GameCommand> out = new ArrayList<>();
        List<Card> sorted = hand.stream()
                .sorted((a, b) -> a.rank().comparisonValue() - b.rank().comparisonValue())
                .toList();
        for (int len = 1; len <= 3 && len <= sorted.size(); len++) {
            for (List<Card> combo : combinations(sorted, len)) {
                out.add(new DeclareLiarCards(combo));
            }
        }
        return out;
    }

    private static List<List<Card>> combinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        combine(cards, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void combine(List<Card> cards, int k, int start, List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(List.copyOf(current));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            combine(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static List<String> withEvent(List<String> events, String event) {
        List<String> out = new ArrayList<>(events);
        out.add(event);
        return out;
    }

    private static List<String> appendEvent(List<String> events, String event) {
        List<String> out = new ArrayList<>(events);
        out.add(event);
        return out;
    }
}
