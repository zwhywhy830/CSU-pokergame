package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.PlayerId;

/**
 * 房间中的一个玩家(纯数据 record,不含网络逻辑)。
 * 主机按加入顺序分配座位与显示名:主机 = SEAT_1 =「玩家 1(主机)」,加入者依次「玩家 2」「玩家 3」「玩家 4」。
 *
 * @param seat        座位
 * @param displayName 系统分配的显示名
 * @param host        是否主机
 * @param connected   当前是否在线
 */
public record RoomPlayer(PlayerId seat, String displayName, boolean host, boolean connected) {
}
