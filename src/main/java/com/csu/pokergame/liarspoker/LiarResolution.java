package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.List;

/**
 * 一次质疑结算的公开结果。
 * shooter 为扣扳机的玩家（输方）；hit 表示下次扣扳机是否中弹；killed 表示是否被击杀淘汰。
 */
public record LiarResolution(
        PlayerId declarer,
        int declaredCount,
        List<Card> actualCards,
        PlayerId challenger,
        boolean truthful,
        PlayerId shooter,
        boolean hit,
        boolean killed) {
}
