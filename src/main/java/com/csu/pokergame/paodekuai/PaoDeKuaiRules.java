package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 跑得快牌型识别与比较。规则基线见 docs/rules/跑得快.md。
 * 炸弹 = 纯四张同点（FOUR_OF_A_KIND）；四带一 / 四带三为普通牌型。
 */
public final class PaoDeKuaiRules {

    public Optional<PdkMove> classify(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return Optional.empty();
        }
        if (new HashSet<>(cards).size() != cards.size()) {
            return Optional.empty(); // 出现重复牌
        }

        List<Card> sorted = cards.stream()
                .sorted(Comparator.comparingInt(c -> c.rank().comparisonValue()))
                .toList();
        Map<Rank, Integer> counts = countsByRank(sorted);
        int n = sorted.size();

        // 全部同点
        if (counts.size() == 1) {
            Rank r = sorted.get(0).rank();
            return switch (n) {
                case 1 -> move(PdkMoveType.SINGLE, r, sorted);
                case 2 -> move(PdkMoveType.PAIR, r, sorted);
                case 3 -> move(PdkMoveType.TRIPLE, r, sorted); // 三条
                case 4 -> move(PdkMoveType.FOUR_OF_A_KIND, r, sorted); // 炸弹
                default -> Optional.empty();
            };
        }

        Rank max = sorted.get(n - 1).rank();
        boolean noTwo = !counts.containsKey(Rank.TWO);
        boolean allSingle = counts.values().stream().allMatch(v -> v == 1);
        boolean allPair = counts.values().stream().allMatch(v -> v == 2);
        boolean allTriple = counts.values().stream().allMatch(v -> v == 3);

        // 顺子：≥5 张、每点 1 张、连续、不含 2
        if (n >= 5 && allSingle && noTwo && isConsecutive(sorted)) {
            return move(PdkMoveType.STRAIGHT, max, sorted);
        }
        // 连对：≥2 对、每点 2 张、连续、不含 2
        if (n >= 4 && n % 2 == 0 && allPair && noTwo && isConsecutiveRanks(distinctRanks(sorted))) {
            return move(PdkMoveType.CONSECUTIVE_PAIRS, max, sorted);
        }
        // 三顺：≥2 组、每点 3 张、连续、不含 2
        if (n >= 6 && n % 3 == 0 && allTriple && noTwo && isConsecutiveRanks(distinctRanks(sorted))) {
            return move(PdkMoveType.TRIPLE_STRAIGHT, max, sorted);
        }

        // 三带二（3+2）
        if (n == 5 && hasCount(counts, 3) && hasCount(counts, 2)) {
            return move(PdkMoveType.TRIPLE_WITH_PAIR, rankWithCount(counts, 3), sorted);
        }
        // 三带一（3+1）
        if (n == 4 && hasCount(counts, 3) && hasCount(counts, 1)) {
            return move(PdkMoveType.TRIPLE_WITH_ONE, rankWithCount(counts, 3), sorted);
        }

        // 四带一（4+1）
        if (n == 5 && hasCount(counts, 4) && hasCount(counts, 1)) {
            return move(PdkMoveType.FOUR_WITH_ONE, rankWithCount(counts, 4), sorted);
        }
        // 四带三（4+3）
        if (n == 7 && hasCount(counts, 4) && hasCount(counts, 3)) {
            return move(PdkMoveType.FOUR_WITH_THREE, rankWithCount(counts, 4), sorted);
        }

        // 飞机带翅膀
        return classifyAirplane(sorted, counts);
    }

    private Optional<PdkMove> classifyAirplane(List<Card> sorted, Map<Rank, Integer> counts) {
        List<Rank> triples = counts.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(Rank::comparisonValue))
                .toList();
        if (triples.size() < 2 || !isConsecutiveRanks(triples) || triples.contains(Rank.TWO)) {
            return Optional.empty();
        }
        int groups = triples.size();
        int bodyCount = groups * 3;
        int wingCount = sorted.size() - bodyCount;
        if (wingCount <= 0) {
            return Optional.empty();
        }
        Rank maxTriple = triples.get(triples.size() - 1);
        if (wingCount == groups) {
            return move(PdkMoveType.AIRPLANE_WITH_WINGS, maxTriple, sorted); // 单翅膀
        }
        if (wingCount == 2 * groups && wingsArePairs(sorted, triples)) {
            return move(PdkMoveType.AIRPLANE_WITH_WINGS, maxTriple, sorted); // 对翅膀
        }
        return Optional.empty();
    }

    /** 判断 candidate 能否压过 table；table 为 null 表示空桌。 */
    public boolean canBeat(PdkMove candidate, PdkMove table) {
        if (table == null) {
            return true;
        }
        if (candidate.isBomb() && !table.isBomb()) {
            return true;
        }
        if (table.isBomb() && !candidate.isBomb()) {
            return false;
        }
        if (candidate.type() != table.type() || candidate.size() != table.size()) {
            return false;
        }
        return candidate.primaryRank().comparisonValue() > table.primaryRank().comparisonValue();
    }

    // ---------- 辅助 ----------

    private static Optional<PdkMove> move(PdkMoveType type, Rank primary, List<Card> sorted) {
        return Optional.of(new PdkMove(orderForDisplay(sorted), type, primary, sorted.size()));
    }

    /**
     * 出牌展示排序：主体牌（出现次数多的点数）在前，带牌在后；同点数按花色序稳定排列。
     * 例如三带二会排成「三张主体 + 两张带牌」。
     */
    private static List<Card> orderForDisplay(List<Card> sorted) {
        Map<Rank, Integer> counts = countsByRank(sorted);
        return sorted.stream()
                .sorted(Comparator
                        .comparingInt((Card c) -> counts.get(c.rank()))
                        .reversed()
                        .thenComparingInt(c -> c.rank().comparisonValue()))
                .toList();
    }

    private static Map<Rank, Integer> countsByRank(List<Card> cards) {
        Map<Rank, Integer> m = new EnumMap<>(Rank.class);
        for (Card c : cards) {
            m.merge(c.rank(), 1, Integer::sum);
        }
        return m;
    }

    private static boolean hasCount(Map<Rank, Integer> counts, int target) {
        return counts.containsValue(target);
    }

    private static Rank rankWithCount(Map<Rank, Integer> counts, int target) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() == target)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    /** 已按点数排序的牌是否构成连续点数序列。 */
    private static boolean isConsecutive(List<Card> sorted) {
        List<Rank> ranks = sorted.stream().map(Card::rank).toList();
        return isConsecutiveRanks(ranks);
    }

    private static List<Rank> distinctRanks(List<Card> sorted) {
        return sorted.stream().map(Card::rank).distinct().toList();
    }

    private static boolean isConsecutiveRanks(List<Rank> ranks) {
        for (int i = 1; i < ranks.size(); i++) {
            if (ranks.get(i).comparisonValue() != ranks.get(i - 1).comparisonValue() + 1) {
                return false;
            }
        }
        return true;
    }

    /** 对翅膀：翅膀牌（非主体三张组）恰好成对。 */
    private static boolean wingsArePairs(List<Card> sorted, List<Rank> triples) {
        Map<Rank, Integer> wingCounts = new EnumMap<>(Rank.class);
        for (Card c : sorted) {
            if (!triples.contains(c.rank())) {
                wingCounts.merge(c.rank(), 1, Integer::sum);
            }
        }
        return !wingCounts.isEmpty() && wingCounts.values().stream().allMatch(v -> v == 2);
    }
}
