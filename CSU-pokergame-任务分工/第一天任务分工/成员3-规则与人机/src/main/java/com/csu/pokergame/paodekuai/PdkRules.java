package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 跑得快静态规则类(第一天范围):牌型识别 + 牌型比较,不依赖引擎。
 * 规则基线见 src/main/resources/rules/跑得快.md;「有大必出」由 Day 2 的引擎 legalCommands 落实。
 */
public final class PdkRules {

    private PdkRules() {
    }

    /** 识别一手牌的牌型;不是合法牌型返回 Optional.empty()。 */
    public static Optional<PdkMove> parse(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return Optional.empty();
        }
        if (new HashSet<>(cards).size() != cards.size()) {
            return Optional.empty(); // 出现重复牌
        }

        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(c -> c.rank().comparisonValue()));
        Map<Rank, Integer> counts = countsByRank(sorted);
        int n = sorted.size();

        // 全部同点:单张 / 对子 / 三条(纯 4 张同点 = 炸弹,Day 2 扩展,今天返回 empty)
        if (counts.size() == 1) {
            Rank r = sorted.get(0).rank();
            return switch (n) {
                case 1 -> Optional.of(new PdkMove(PdkMoveType.SINGLE, order(sorted), r));
                case 2 -> Optional.of(new PdkMove(PdkMoveType.PAIR, order(sorted), r));
                case 3 -> Optional.of(new PdkMove(PdkMoveType.TRIPLE, order(sorted), r));
                default -> Optional.empty();
            };
        }

        // 三带一:3 张同点 + 1 张不同点散牌
        if (n == 4 && counts.containsValue(3) && counts.containsValue(1)) {
            return Optional.of(new PdkMove(PdkMoveType.TRIPLE_WITH_SINGLE, order(sorted), rankWithCount(counts, 3)));
        }
        // 三带二:3 张同点 + 1 对
        if (n == 5 && counts.containsValue(3) && counts.containsValue(2)) {
            return Optional.of(new PdkMove(PdkMoveType.TRIPLE_WITH_PAIR, order(sorted), rankWithCount(counts, 3)));
        }
        // 顺子:≥5 张、每点 1 张、连续、范围 3–A(不得包含 2)
        if (n >= 5 && counts.values().stream().allMatch(v -> v == 1)
                && !counts.containsKey(Rank.TWO) && isConsecutive(sorted)) {
            return Optional.of(new PdkMove(PdkMoveType.STRAIGHT, order(sorted), sorted.get(n - 1).rank()));
        }
        return Optional.empty();
    }

    /**
     * a 是否大过 b(仅同型且同张数可比,按主点数比)。
     * b 为 null 表示自由出牌(新一轮),任何合法牌型都算大。
     */
    public static boolean beats(PdkMove a, PdkMove b) {
        if (a == null) {
            return false;
        }
        if (b == null) {
            return true;
        }
        if (a.type() != b.type() || a.size() != b.size()) {
            return false;
        }
        return a.mainRank().comparisonValue() > b.mainRank().comparisonValue();
    }

    // ---------- 辅助 ----------

    /** 展示排序:主体点数在前(张数多优先),同点数按比较值稳定排列。 */
    private static List<Card> order(List<Card> sorted) {
        Map<Rank, Integer> counts = countsByRank(sorted);
        List<Card> ordered = new ArrayList<>(sorted);
        ordered.sort(Comparator
                .comparingInt((Card c) -> counts.get(c.rank())).reversed()
                .thenComparingInt(c -> c.rank().comparisonValue()));
        return ordered;
    }

    private static Map<Rank, Integer> countsByRank(List<Card> cards) {
        Map<Rank, Integer> m = new EnumMap<>(Rank.class);
        for (Card c : cards) {
            m.merge(c.rank(), 1, Integer::sum);
        }
        return m;
    }

    private static Rank rankWithCount(Map<Rank, Integer> counts, int target) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() == target)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    /** 已按点数升序排列的牌是否构成连续点数序列。 */
    private static boolean isConsecutive(List<Card> sorted) {
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).rank().comparisonValue() != sorted.get(i - 1).rank().comparisonValue() + 1) {
                return false;
            }
        }
        return true;
    }
}
