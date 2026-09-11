package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 线缆消息:主机与客户端之间所有 TCP 帧的 JSON 顶层结构。
 * 使用 Jackson 多态:type 字段区分 7 种消息。
 *
 * <p>客户端只能发:JOIN、SUBMIT_COMMAND。
 * <p>主机只能发:ROOM_SNAPSHOT、START_GAME、GAME_SNAPSHOT、ERROR、GAME_ENDED。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WireMessage.Join.class, name = "JOIN"),
        @JsonSubTypes.Type(value = WireMessage.RoomSnapshotMsg.class, name = "ROOM_SNAPSHOT"),
        @JsonSubTypes.Type(value = WireMessage.StartGame.class, name = "START_GAME"),
        @JsonSubTypes.Type(value = WireMessage.GameSnapshotMsg.class, name = "GAME_SNAPSHOT"),
        @JsonSubTypes.Type(value = WireMessage.SubmitCommand.class, name = "SUBMIT_COMMAND"),
        @JsonSubTypes.Type(value = WireMessage.Error.class, name = "ERROR"),
        @JsonSubTypes.Type(value = WireMessage.GameEnded.class, name = "GAME_ENDED"),
})
public sealed interface WireMessage
        permits WireMessage.Join,
                WireMessage.RoomSnapshotMsg,
                WireMessage.StartGame,
                WireMessage.GameSnapshotMsg,
                WireMessage.SubmitCommand,
                WireMessage.Error,
                WireMessage.GameEnded {

    /**
     * 客户端 → 主机:请求加入房间。
     *
     * @param requestedSeat 客户端请求的座位；null 表示任意空位（兼容旧版不传字段）
     *                     非空时主机校验该座位是否空闲且非 SEAT_1，
     *                     冲突返回 SEAT_TAKEN / SEAT_RESERVED_HOST 错误
     */
    record Join(PlayerId requestedSeat) implements WireMessage {
        /** 无参构造兼容旧版客户端，等价于 requestedSeat = null。 */
        @JsonCreator
        public Join(
                @JsonProperty(value = "requestedSeat", required = false) PlayerId requestedSeat) {
            this.requestedSeat = requestedSeat;
        }

        /** 旧版无参构造：requestedSeat = null，让客户端可以 {@code new WireMessage.Join()}。 */
        public Join() {
            this(null);
        }
    }

    /** 主机 → 客户端:广播当前房间玩家状态。 */
    record RoomSnapshotMsg(RoomSnapshot snapshot) implements WireMessage {}

    /** 主机 → 客户端:宣布开始游戏,并告知客户端被分配的座位。 */
    record StartGame(GameType gameType, PlayerId seat) implements WireMessage {}

    /** 主机 → 客户端:发送该 viewer 视角的 GameSnapshot。 */
    record GameSnapshotMsg(GameSnapshot snapshot) implements WireMessage {}

    /** 客户端 → 主机:提交一条命令。 */
    record SubmitCommand(GameCommand command) implements WireMessage {}

    /** 主机 → 客户端:错误码(如 ROOM_FULL、INVALID_COMMAND)。 */
    record Error(String code) implements WireMessage {}

    /** 主机 → 客户端:对局结束原因(如 PLAYER_DISCONNECTED、GAME_OVER)。 */
    record GameEnded(String reason) implements WireMessage {}
}
