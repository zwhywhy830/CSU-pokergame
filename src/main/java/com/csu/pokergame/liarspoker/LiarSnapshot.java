package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 骗子酒馆快照。viewer 之外玩家只暴露存活状态与已扣扳机次数（guns），不暴露手牌与子弹位置。
 */
public record LiarSnapshot(
        String gameId,
        GamePhase phase,
        PlayerId currentPlayer,
        Optional<PlayerId> winner,
        List<String> publicEvents,
        LiarPhase liarPhase,
        Rank targetRank,
        Set<PlayerId> alivePlayers,
        Map<PlayerId, GunState> guns,
        List<Card> myHand,
        PlayerId declarer,
        int pendingDeclaredCount,
        Set<PlayerId> eliminatedPlayers,
        Optional<LiarResolution> lastResolution) implements GameSnapshot {
}
