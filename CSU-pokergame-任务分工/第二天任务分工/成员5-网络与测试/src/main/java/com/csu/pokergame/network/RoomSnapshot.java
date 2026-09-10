package com.csu.pokergame.network;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 房间快照:开局前广播给所有客户端,显示当前已加入的玩家。
 *
 * @param players    玩家列表(含主机)
 * @param full       房间是否已满
 * @param gameType   游戏类型
 */
public record RoomSnapshot(
        List<RoomPlayer> players,
        boolean full,
        GameType gameType) {

    @JsonCreator
    public RoomSnapshot(
            @JsonProperty("players") List<RoomPlayer> players,
            @JsonProperty("full") boolean full,
            @JsonProperty("gameType") GameType gameType) {
        this.players = players;
        this.full = full;
        this.gameType = gameType;
    }
}
