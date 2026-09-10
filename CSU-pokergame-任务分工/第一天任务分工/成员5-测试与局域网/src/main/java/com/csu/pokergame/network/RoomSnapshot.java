package com.csu.pokergame.network;

import java.util.List;

/**
 * 房间快照(纯数据 record,不含网络逻辑):开局前广播给所有客户端,展示当前已加入的玩家。
 *
 * @param players  玩家列表(含主机,按座位顺序)
 * @param full     房间是否已满(满员后新连接者收 ERROR(ROOM_FULL))
 * @param gameType 游戏类型(决定满员人数:跑得快 3 人、骗子酒馆 4 人)
 */
public record RoomSnapshot(List<RoomPlayer> players, boolean full, GameType gameType) {
}
