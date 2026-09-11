package com.csu.pokergame.core.engine;

import java.util.List;
import java.util.Optional;

/**
 * 只读快照接口。
 *
 * <p><b>关键约定(联机不泄露手牌的基础)</b>:实现方必须保证
 * {@code snapshotFor(viewer)} 只含 viewer 自己的手牌,其他玩家只给
 * 剩余牌数 / 状态 / 公开事件 —— 局域网主机按座位给各客户端各发各的快照,
 * 任何他人的手牌都不得出现在快照里。
 *
 * <p>具体游戏快照(Day 2 的 PdkSnapshot / LiarSnapshot)用 record 实现本接口,只加各自公开状态。
 */
public interface GameSnapshot {

    String gameId();

    GamePhase phase();

    PlayerId currentPlayer();

    /** 无胜者时为 empty。 */
    Optional<PlayerId> winner();

    /** 所有玩家都可见的公开事件(如「玩家 2 出了 33」「玩家 3 被质疑」)。 */
    List<String> publicEvents();
}
