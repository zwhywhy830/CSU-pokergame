package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.PlayerId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 LanHost 的机器人补位与座位选择功能：
 * <ul>
 *   <li>addBot 成功挂机器人，超过 N-1 拒绝</li>
 *   <li>removeBot 移除后座位空</li>
 *   <li>startGame 在 1 主机 + N-1 机器人时成功</li>
 *   <li>客户端请求指定座位时成功分配</li>
 *   <li>客户端请求 SEAT_1 时收到 SEAT_RESERVED_HOST</li>
 *   <li>客户端请求被机器人占用的座位时收到 SEAT_TAKEN</li>
 * </ul>
 */
class LanHostBotTest {

    private LanHost host;

    @AfterEach
    void tearDown() {
        if (host != null) {
            host.shutdown();
        }
    }

    @Test
    void addBotSucceedsAndShowsInRoom() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        host.addBot(PlayerId.SEAT_2);

        RoomSnapshot room = host.currentRoom();
        assertThat(room.players()).hasSize(3);
        // SEAT_2 现在是机器人
        RoomPlayer seat2 = room.players().get(1);
        assertThat(seat2.seat()).isEqualTo(PlayerId.SEAT_2);
        assertThat(seat2.bot()).isTrue();
        assertThat(seat2.connected()).isTrue();
        assertThat(seat2.displayName()).isEqualTo("机器人 2");
        // SEAT_3 仍空
        assertThat(room.players().get(2).connected()).isFalse();
        assertThat(room.players().get(2).bot()).isFalse();
    }

    @Test
    void addBotRejectsHostSeat() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        assertThatThrownBy(() -> host.addBot(PlayerId.SEAT_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("主机座位");
    }

    @Test
    void addBotRejectsExceedingNMinusOne() throws Exception {
        // 跑得快 3 人 → 最多 2 个机器人
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        host.addBot(PlayerId.SEAT_2);
        host.addBot(PlayerId.SEAT_3);

        // 已经 N-1=2 个机器人，不能再加（也没空位了，但校验顺序先检查上限）
        assertThatThrownBy(() -> host.addBot(PlayerId.SEAT_2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeBotFreesSeat() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        host.addBot(PlayerId.SEAT_2);
        assertThat(host.currentRoom().players().get(1).bot()).isTrue();

        host.removeBot(PlayerId.SEAT_2);
        assertThat(host.currentRoom().players().get(1).bot()).isFalse();
        assertThat(host.currentRoom().players().get(1).connected()).isFalse();
    }

    @Test
    void startGameSucceedsWithOneHostPlusBots() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        host.addBot(PlayerId.SEAT_2);
        host.addBot(PlayerId.SEAT_3);

        // 房间已满，1 主机 + 2 机器人
        assertThat(host.currentRoom().full()).isTrue();
        // 注入同步调度器（测试环境无 JavaFX Toolkit）
        host.setBotScheduler((task, delayMs) -> task.run());
        // startGame 不抛异常，机器人会同步链式推进直到游戏结束或回到真人
        host.startGame();
    }

    @Test
    void clientRequestedSeatIsAssigned() throws Exception {
        host = new LanHost(GameType.LIARS_POKER, 0); // 4 座位
        Thread.sleep(100);

        ClientStub c = new ClientStub("127.0.0.1", host.port(), PlayerId.SEAT_3);
        RoomSnapshot room = c.waitForRoom();
        // SEAT_3 应已连接，displayName = "玩家 3"
        RoomPlayer seat3 = room.players().get(2);
        assertThat(seat3.seat()).isEqualTo(PlayerId.SEAT_3);
        assertThat(seat3.connected()).isTrue();
        assertThat(seat3.displayName()).isEqualTo("玩家 3");
        assertThat(seat3.bot()).isFalse();
        c.close();
    }

    @Test
    void clientRequestingHostSeatGetsError() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        ClientStub c = new ClientStub("127.0.0.1", host.port(), PlayerId.SEAT_1);
        WireMessage msg = c.waitForMessage();
        assertThat(msg).isInstanceOf(WireMessage.Error.class);
        assertThat(((WireMessage.Error) msg).code()).isEqualTo("SEAT_RESERVED_HOST");
        c.close();
    }

    @Test
    void clientRequestingBotSeatGetsSeatTaken() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        // 主机先把 SEAT_2 占为机器人
        host.addBot(PlayerId.SEAT_2);

        ClientStub c = new ClientStub("127.0.0.1", host.port(), PlayerId.SEAT_2);
        WireMessage msg = c.waitForMessage();
        assertThat(msg).isInstanceOf(WireMessage.Error.class);
        assertThat(((WireMessage.Error) msg).code()).isEqualTo("SEAT_TAKEN");
        c.close();
    }

    /** 简易客户端，连接后发送 JOIN(指定座位)，接收消息。 */
    private static final class ClientStub {
        private final java.net.Socket socket;
        private final java.io.DataInputStream in;
        private final java.io.DataOutputStream out;

        ClientStub(String hostIp, int port, PlayerId requestedSeat) throws Exception {
            socket = new java.net.Socket(hostIp, port);
            socket.setSoTimeout(5000);
            in = new java.io.DataInputStream(socket.getInputStream());
            out = new java.io.DataOutputStream(socket.getOutputStream());
            send(new WireMessage.Join(requestedSeat));
        }

        WireMessage waitForMessage() throws Exception {
            int len = in.readInt();
            byte[] json = in.readNBytes(len);
            return JsonCodec.readMessage(json);
        }

        RoomSnapshot waitForRoom() throws Exception {
            for (int i = 0; i < 10; i++) {
                WireMessage msg = waitForMessage();
                if (msg instanceof WireMessage.RoomSnapshotMsg rs) {
                    return rs.snapshot();
                }
            }
            throw new AssertionError("未收到 ROOM_SNAPSHOT");
        }

        void send(WireMessage msg) throws Exception {
            byte[] json = JsonCodec.writeBytes(msg);
            out.writeInt(json.length);
            out.write(json);
            out.flush();
        }

        void close() {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
