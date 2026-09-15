package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 跑得快快照。viewer 之外的玩家只暴露剩余牌数（remainingCardCounts），不暴露手牌。
 */
public record PdkSnapshot(
        String gameId,
        GamePhase phase,
        PlayerId currentPlayer,
        Optional<PlayerId> winner,
        List<String> publicEvents,
        Map<PlayerId, Integer> remainingCardCounts,
        Set<PlayerId> closedDoorPlayers,
        List<Card> myHand,
        Optional<Card> faceUpCard,
        Optional<PdkMove> lastMove) implements GameSnapshot {
}
