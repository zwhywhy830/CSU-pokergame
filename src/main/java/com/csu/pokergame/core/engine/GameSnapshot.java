package com.csu.pokergame.core.engine;

import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.paodekuai.PdkSnapshot;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Optional;

/**
 * 不可变快照的公共视图。{@code snapshotFor(viewer)} 只包含 viewer 自己的手牌,
 * 其他玩家仅公开剩余牌数、生命和公开事件,以支持局域网客户端而不泄露手牌。
 * 具体游戏快照(如跑得快快照)用 record 实现本接口,另带各自公开状态与本地玩家可见手牌。
 *
 * <p>联机序列化:Jackson 多态,通过 {@code @type} 字段区分子类型。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PdkSnapshot.class, name = "PdkSnapshot"),
        @JsonSubTypes.Type(value = LiarSnapshot.class, name = "LiarSnapshot"),
})
public interface GameSnapshot {

    String gameId();

    GamePhase phase();

    PlayerId currentPlayer();

    Optional<PlayerId> winner();

    List<String> publicEvents();
}
