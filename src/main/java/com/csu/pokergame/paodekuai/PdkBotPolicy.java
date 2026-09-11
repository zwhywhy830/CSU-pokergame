package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.player.BotDecision;
import com.csu.pokergame.core.player.BotPolicy;

import java.util.Comparator;
import java.util.List;

/**
 * 跑得快机器人策略：
 * <ul>
 *   <li>炸弹：能炸别人就炸，但不会自己先出（作为新一轮首出时不出炸弹）。</li>
 *   <li>其余：能打多牌优先打多牌（张数最多，其次主点最小）。</li>
 * </ul>
 */
public final class PdkBotPolicy implements BotPolicy {

    private final PaoDeKuaiRules rules = new PaoDeKuaiRules();

    @Override
    public BotDecision decide(GameSnapshot snapshot, List<GameCommand> legal) {
        List<PlayPdkCards> plays = legal.stream()
                .filter(cmd -> cmd instanceof PlayPdkCards)
                .map(cmd -> (PlayPdkCards) cmd)
                .toList();

        if (plays.isEmpty()) {
            GameCommand pass = legal.stream()
                    .filter(cmd -> cmd instanceof PassPdkTurn)
                    .findFirst()
                    .orElseThrow();
            return new BotDecision(pass, "无可压牌，选择不出");
        }

        boolean isLead = snapshot instanceof PdkSnapshot s && s.lastMove().isEmpty();

        // 炸弹：桌面有牌时，能炸就炸（但不会自己先出）
        if (!isLead) {
            List<PlayPdkCards> bombs = plays.stream()
                    .filter(p -> move(p).isBomb())
                    .sorted(Comparator.comparingInt(p -> move(p).primaryRank().comparisonValue()))
                    .toList();
            if (!bombs.isEmpty()) {
                return new BotDecision(bombs.get(0), "有炸弹，炸掉对方");
            }
        }

        // 候选：作为首出时排除炸弹（除非只剩炸弹）
        List<PlayPdkCards> candidates = plays;
        if (isLead) {
            List<PlayPdkCards> nonBombs = plays.stream()
                    .filter(p -> !move(p).isBomb())
                    .toList();
            if (!nonBombs.isEmpty()) {
                candidates = nonBombs;
            }
        }

        // 能打多牌优先打多牌：张数最多，其次主点最小
        PlayPdkCards chosen = candidates.stream()
                .sorted(Comparator
                        .comparingInt((PlayPdkCards p) -> move(p).size()).reversed()
                        .thenComparingInt(p -> move(p).primaryRank().comparisonValue()))
                .findFirst()
                .orElseThrow();
        return new BotDecision(chosen, "优先出张数最多的牌");
    }

    private PdkMove move(PlayPdkCards play) {
        return rules.classify(play.cards()).orElseThrow();
    }
}
