package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.PlayerId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 房间中的一个玩家。由主机按加入顺序分配座位与显示名。
 *
 * @param seat        座位（SEAT_1 为主机）
 * @param displayName 系统分配的显示名（如"玩家 2"）
 * @param host        是否主机
 * @param connected   当前是否在线
 */
public record RoomPlayer(
        PlayerId seat,
        String displayName,
        boolean host,
        boolean connected) {

    @JsonCreator
    public RoomPlayer(
            @JsonProperty("seat") PlayerId seat,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("host") boolean host,
            @JsonProperty("connected") boolean connected) {
        this.seat = seat;
        this.displayName = displayName;
        this.host = host;
        this.connected = connected;
    }
}
