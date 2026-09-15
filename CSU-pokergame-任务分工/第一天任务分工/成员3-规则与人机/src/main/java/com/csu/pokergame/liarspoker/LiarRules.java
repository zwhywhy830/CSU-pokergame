package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.List;

/**
 * 骗子酒馆静态规则类(新版规则:四人、6 仓左轮、质疑失败扣扳机;旧的「6 点生命」已作废)。
 * 扣扳机后中不中弹由引擎掷仓位,不属于本类职责。
 */
public final class LiarRules {

    private LiarRules() {
    }

    /** 一次宣告的张数上限为 1–3 张。 */
    public static boolean isValidDeclarationCount(int count) {
        return count >= 1 && count <= 3;
    }

    /**
     * 质疑结算:宣告属实 → 质疑者扣扳机;宣告被拆穿 → 宣告者扣扳机。
     */
    public static PlayerId whoShoots(boolean truthful, PlayerId declarer, PlayerId challenger) {
        return truthful ? challenger : declarer;
    }

    /** 按实际牌判定宣告真伪;JOKER 是万能牌,可当作任何目标点数。 */
    public static boolean isTruthful(List<Card> cards, Rank targetRank) {
        return cards != null && !cards.isEmpty()
                && cards.stream().allMatch(c -> c.rank() == targetRank || c.rank() == Rank.JOKER);
    }
}
