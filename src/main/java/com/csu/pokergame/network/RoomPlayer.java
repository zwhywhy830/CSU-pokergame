package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.PlayerId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 房间中的一个玩家。由主机按加入顺序分配座位与显示名。
 *
 * @param seat        座位（SEAT_1 为主机）
 * @param displayName 系统分配的显示名（如"玩家 2"或"机器人 3"）
 * @param host        是否主机
 * @param connected   当前是否在线（机器人视为已就位 true）
 * @param bot         是否机器人座位（由主机本地添加，不占 TCP 连接）
 */
public record RoomPlayer(
        PlayerId seat,
        String displayName,
        boolean host,
        boolean connected,
        boolean bot) {

    /** 5 参构造（新客户端/主机使用）。 */
    @JsonCreator
    public RoomPlayer(
            @JsonProperty("seat") PlayerId seat,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("host") boolean host,
            @JsonProperty("connected") boolean connected,
            @JsonProperty(value = "bot", defaultValue = "false") boolean bot) {
        this.seat = seat;
        this.displayName = displayName;
        this.host = host;
        this.connected = connected;
        this.bot = bot;
    }

    /** 4 参构造兼容：bot 默认 false（兼容旧版 JSON 与未关心 bot 的调用方）。 */
    public RoomPlayer(PlayerId seat, String displayName, boolean host, boolean connected) {
        this(seat, displayName, host, connected, false);
    }
}
