package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 骗子酒馆不可变局状态。
 * hands 各家手牌；alive 存活玩家；guns 各家手枪状态（子弹位置 + 已扣次数）；
 * targetRank 当前小局目标点数（每次重新洗牌发牌时从 K/Q/A 重选）；phase 内部阶段；
 * declarer 当前出牌者；responder 当前回应者；
 * pendingDeclaration 本次宣告的牌；lastResolution 最近一次结算；winner 赢家（可空）。
 * 重新洗牌时保留 guns、alive；hands 重新生成、targetRank 重选。
 */
public record LiarState(
        Map<PlayerId, List<Card>> hands,
        Set<PlayerId> alive,
        Map<PlayerId, GunState> guns,
        Rank targetRank,
        LiarPhase phase,
        PlayerId declarer,
        PlayerId responder,
        List<Card> pendingDeclaration,
        LiarResolution lastResolution,
        PlayerId winner,
        List<String> publicEvents) {
}
