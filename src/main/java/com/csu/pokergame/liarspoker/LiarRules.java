package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;

import java.util.List;

/** 骗子酒馆规则：宣告张数 1–3，宣告"均为目标点数"时按实际牌判定真伪。 */
public final class LiarRules {

    public boolean isValidCount(int count) {
        return count >= 1 && count <= 3;
    }

    public boolean isTruthful(List<Card> cards, Rank targetRank) {
        // JOKER 是万能牌，可当作任何牌（包括目标点数）
        return !cards.isEmpty() && cards.stream().allMatch(c -> c.rank() == targetRank || c.rank() == Rank.JOKER);
    }
}
