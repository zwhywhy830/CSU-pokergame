package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Deck;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameEngine;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * 跑得快引擎。持有唯一不可变状态，apply 校验合法性后才推进状态。
 * 规则：有大必出（能压时 pass 非法）；连续两次 pass 清桌；首名出完获胜并结算关门。
 */
public final class PdkEngine implements GameEngine {

    /** 跑得快为三人游戏，固定三个座位。 */
    static final List<PlayerId> SEATS = List.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3);

    private final String gameId;
    private final PaoDeKuaiRules rules = new PaoDeKuaiRules();
    private final Random random;
    private PdkState state;

    /** 供测试注入已构造状态。 */
    public PdkEngine(PdkState initialState) {
        this.gameId = "pdk-" + Integer.toHexString(System.identityHashCode(initialState));
        this.random = null;
        this.state = initialState;
    }

    /** 生产用：以给定随机源发牌（保留随机源以保证同 seed 可复现）。 */
    public PdkEngine(Random random) {
        this.gameId = "pdk-" + Long.toHexString(random.nextLong());
        this.random = random;
        this.state = null;
    }

    /** 发牌并确定首出者（持有明牌的玩家）。 */
    @Override
    public void start() {
        if (state != null) {
            throw new IllegalStateException("游戏已开始");
        }
        Random r = random != null ? random : new Random(gameId.hashCode());
        Map<PlayerId, List<Card>> hands = new EnumMap<>(PlayerId.class);
        List<Card> cards = Deck.hunanPaodekuai(r).draw(48);
        hands.put(PlayerId.SEAT_1, new ArrayList<>(cards.subList(0, 16)));
        hands.put(PlayerId.SEAT_2, new ArrayList<>(cards.subList(16, 32)));
        hands.put(PlayerId.SEAT_3, new ArrayList<>(cards.subList(32, 48)));

        Card faceUp = cards.get(r.nextInt(48));
        PlayerId first = holderOf(hands, faceUp);

        Set<PlayerId> closedDoor = new LinkedHashSet<>(List.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3));
        state = new PdkState(hands, faceUp, first, first, null, 0, null, closedDoor,
                new ArrayList<>(List.of("发牌完成，明牌 " + faceUp.display() + "，首出 " + first)), GamePhase.PLAYING);
    }

    @Override
    public GameSnapshot snapshotFor(PlayerId viewer) {
        PdkState s = requireStarted();
        return new PdkSnapshot(
                gameId,
                s.phase(),
                s.currentPlayer(),
                Optional.ofNullable(s.winner()),
                List.copyOf(s.publicEvents()),
                remainingCounts(s),
                Set.copyOf(s.closedDoorPlayers()),
                List.copyOf(s.hands().get(viewer)),
                Optional.ofNullable(s.faceUpCard()),
                Optional.ofNullable(s.lastMove()));
    }

    @Override
    public List<GameCommand> legalCommands(PlayerId player) {
        PdkState s = requireStarted();
        if (s.phase() != GamePhase.PLAYING || s.winner() != null || s.currentPlayer() != player) {
            return List.of();
        }
        List<Card> hand = s.hands().get(player);
        List<GameCommand> commands = new ArrayList<>();

        // 是否存在可压牌：枚举手牌中所有能压过桌面的合法牌型
        boolean canBeat = false;
        for (List<Card> combo : candidateCombos(hand)) {
            Optional<PdkMove> move = rules.classify(combo);
            if (move.isEmpty()) {
                continue;
            }
            if (s.lastMove() == null || rules.canBeat(move.get(), s.lastMove())) {
                commands.add(new PlayPdkCards(List.copyOf(combo)));
                canBeat = true;
            }
        }

        // 有大必出：能压时不允许 pass；空桌（新一轮）也不允许 pass
        if (!canBeat && s.lastMove() != null) {
            commands.add(new PassPdkTurn());
        }
        return commands;
    }

    @Override
    public void apply(GameCommand command) {
        PdkState s = requireStarted();
        if (s.phase() != GamePhase.PLAYING || s.winner() != null) {
            throw new IllegalArgumentException("对局已结束");
        }
        PlayerId current = s.currentPlayer();

        if (command instanceof PassPdkTurn) {
            if (s.lastMove() == null) {
                throw new IllegalArgumentException("新一轮不能不出");
            }
            if (canBeatAny(s.hands().get(current), s.lastMove())) {
                throw new IllegalArgumentException("有大必出：存在可压牌时不能不出");
            }
            int passCount = s.passCount() + 1;
            if (passCount >= 2) {
                // 清桌，最后出牌者继续
                state = withEvents(s, s.lastPlayer(), null, 0, "连续两家不出，桌面清空，" + s.lastPlayer() + " 继续");
            } else {
                state = withEvents(s, nextSeat(current), s.lastMove(), passCount, current + " 不出");
            }
            return;
        }

        if (command instanceof PlayPdkCards play) {
            if (play.cards() == null || play.cards().isEmpty()) {
                throw new IllegalArgumentException("出牌不能为空");
            }
            List<Card> hand = s.hands().get(current);
            if (!new HashSet<>(hand).containsAll(play.cards())) {
                throw new IllegalArgumentException("牌不在手中");
            }
            Optional<PdkMove> move = rules.classify(play.cards());
            if (move.isEmpty()) {
                throw new IllegalArgumentException("牌型无效");
            }
            if (s.lastMove() != null && !rules.canBeat(move.get(), s.lastMove())) {
                throw new IllegalArgumentException("无法压过桌面");
            }

            List<Card> newHand = new ArrayList<>(hand);
            newHand.removeAll(play.cards());
            Map<PlayerId, List<Card>> newHands = new EnumMap<>(s.hands());
            newHands.put(current, newHand);

            Set<PlayerId> closedDoor = new LinkedHashSet<>(s.closedDoorPlayers());
            closedDoor.remove(current); // 出过牌即不算关门

            if (newHand.isEmpty()) {
                // 获胜：结算两名失败者剩牌数（关门已在 closedDoor 标记）
                state = new PdkState(newHands, s.faceUpCard(), current, current, move.get(), 0, current,
                        closedDoor, withEvent(s.publicEvents(), current + " 出完手牌，获胜"), GamePhase.FINISHED);
            } else {
                state = new PdkState(newHands, s.faceUpCard(), nextSeat(current), current, move.get(), 0, s.winner(),
                        closedDoor, withEvent(s.publicEvents(), current + " 出 " + describe(play.cards())), GamePhase.PLAYING);
            }
            return;
        }

        throw new IllegalArgumentException("未知指令");
    }

    // ---------- 辅助 ----------

    private PdkState requireStarted() {
        if (state == null) {
            throw new IllegalStateException("游戏尚未开始");
        }
        return state;
    }

    private static PlayerId holderOf(Map<PlayerId, List<Card>> hands, Card card) {
        return hands.entrySet().stream()
                .filter(e -> e.getValue().contains(card))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    /** 跑得快三人逆时针下一位：SEAT_1 → SEAT_2 → SEAT_3 → SEAT_1。 */
    static PlayerId nextSeat(PlayerId p) {
        return switch (p) {
            case SEAT_1 -> PlayerId.SEAT_2;
            case SEAT_2 -> PlayerId.SEAT_3;
            default -> PlayerId.SEAT_1;
        };
    }

    private Map<PlayerId, Integer> remainingCounts(PdkState s) {
        Map<PlayerId, Integer> m = new EnumMap<>(PlayerId.class);
        for (PlayerId p : SEATS) {
            m.put(p, s.hands().get(p).size());
        }
        return m;
    }

    private boolean canBeatAny(List<Card> hand, PdkMove table) {
        for (List<Card> combo : candidateCombos(hand)) {
            Optional<PdkMove> move = rules.classify(combo);
            if (move.isPresent() && rules.canBeat(move.get(), table)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 枚举手牌所有非空子集（按点数去重后的组合）。手牌最多 16 张，全子集 2^16 可接受。
     * 为性能，先按点数排序，用位掩码生成子集。
     */
    private List<List<Card>> candidateCombos(List<Card> hand) {
        List<Card> sorted = hand.stream()
                .sorted((a, b) -> a.rank().comparisonValue() - b.rank().comparisonValue())
                .toList();
        List<List<Card>> result = new ArrayList<>();
        int n = sorted.size();
        int total = 1 << n;
        for (int mask = 1; mask < total; mask++) {
            List<Card> combo = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    combo.add(sorted.get(i));
                }
            }
            result.add(combo);
        }
        return result;
    }

    private PdkState withEvents(PdkState s, PlayerId current, PdkMove lastMove, int passCount, String event) {
        return new PdkState(s.hands(), s.faceUpCard(), current, s.lastPlayer(), lastMove, passCount, s.winner(),
                s.closedDoorPlayers(), withEvent(s.publicEvents(), event), s.phase());
    }

    private static List<String> withEvent(List<String> events, String event) {
        List<String> out = new ArrayList<>(events);
        out.add(event);
        return out;
    }

    private static String describe(List<Card> cards) {
        return cards.stream().map(Card::display).reduce((a, b) -> a + " " + b).orElse("");
    }
}
