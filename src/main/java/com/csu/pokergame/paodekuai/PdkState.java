package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 跑得快不可变局状态。
 * hands 三家手牌；faceUpCard 明牌（首出者持有，可空）；
 * currentPlayer 当前出牌者；lastPlayer 最后出牌者；lastMove 桌面当前待压牌（可空，空桌）；
 * passCount 连续不出次数（累计 2 次清桌）；winner 赢家（可空）；closedDoorPlayers 一张未出者。
 */
public record PdkState(
        Map<PlayerId, List<Card>> hands,
        Card faceUpCard,
        PlayerId currentPlayer,
        PlayerId lastPlayer,
        PdkMove lastMove,
        int passCount,
        PlayerId winner,
        Set<PlayerId> closedDoorPlayers,
        List<String> publicEvents,
        GamePhase phase) {
}
