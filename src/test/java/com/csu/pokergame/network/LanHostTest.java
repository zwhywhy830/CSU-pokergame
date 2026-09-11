package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.PlayerId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证 LanHost 加入顺序、座位分配、ROOM_FULL 拒绝、未齐无法 start。 */
class LanHostTest {

    private LanHost host;

    @AfterEach
    void tearDown() {
        if (host != null) {
            host.shutdown();
        }
    }

    @Test
    void assignsNamesByJoinOrderAndRejectsFourthPlayer() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        // 等待 accept 线程就绪
        Thread.sleep(200);

        // 房间初始:仅主机 SEAT_1
        RoomSnapshot initial = host.currentRoom();
        assertThat(initial.players()).hasSize(3); // 跑得快 3 座位
        assertThat(initial.players().get(0).seat()).isEqualTo(PlayerId.SEAT_1);
        assertThat(initial.players().get(0).displayName()).isEqualTo("玩家 1（主机）");
        assertThat(initial.players().get(0).host()).isTrue();
        assertThat(initial.players().get(1).connected()).isFalse();
        assertThat(initial.full()).isFalse();

        // 第 1 个客户端加入 → SEAT_2 / 玩家 2
        ClientStub c1 = new ClientStub("127.0.0.1", host.port());
        RoomSnapshot after1 = c1.waitForRoom();
        assertThat(after1.players().get(1).seat()).isEqualTo(PlayerId.SEAT_2);
        assertThat(after1.players().get(1).displayName()).isEqualTo("玩家 2");
        assertThat(after1.players().get(1).connected()).isTrue();

        // 第 2 个客户端加入 → SEAT_3 / 玩家 3
        ClientStub c2 = new ClientStub("127.0.0.1", host.port());
        RoomSnapshot after2 = c2.waitForRoom();
        assertThat(after2.players().get(2).seat()).isEqualTo(PlayerId.SEAT_3);
        assertThat(after2.players().get(2).displayName()).isEqualTo("玩家 3");
        assertThat(after2.full()).isTrue();

        // 第 3 个客户端 → ROOM_FULL
        ClientStub c3 = new ClientStub("127.0.0.1", host.port());
        WireMessage msg = c3.waitForMessage();
        assertThat(msg).isInstanceOf(WireMessage.Error.class);
        assertThat(((WireMessage.Error) msg).code()).isEqualTo("ROOM_FULL");
        c3.close();
        c2.close();
        c1.close();
    }

    @Test
    void cannotStartBeforeRoomFull() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        // 房间只有主机一人,未齐
        assertThatThrownBy(host::startGame)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("人数未齐");
    }

    @Test
    void liarsPokerRequiresFourPlayers() throws Exception {
        host = new LanHost(GameType.LIARS_POKER, 0);
        Thread.sleep(100);

        RoomSnapshot initial = host.currentRoom();
        assertThat(initial.players()).hasSize(4);
        assertThat(initial.full()).isFalse();

        // 加 3 个客户端
        ClientStub c1 = new ClientStub("127.0.0.1", host.port());
        c1.waitForRoom();
        ClientStub c2 = new ClientStub("127.0.0.1", host.port());
        c2.waitForRoom();
        ClientStub c3 = new ClientStub("127.0.0.1", host.port());
        RoomSnapshot full = c3.waitForRoom();
        assertThat(full.full()).isTrue();
        assertThat(full.players()).allSatisfy(p -> assertThat(p.connected()).isTrue());
    }

    /** 简易客户端,连接后发送 JOIN,接收消息。 */
    private static final class ClientStub {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;

        ClientStub(String hostIp, int port) throws Exception {
            socket = new Socket(hostIp, port);
            socket.setSoTimeout(5000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            send(new WireMessage.Join());
        }

        WireMessage waitForMessage() throws Exception {
            int len = in.readInt();
            byte[] json = in.readNBytes(len);
            return JsonCodec.readMessage(json);
        }

        RoomSnapshot waitForRoom() throws Exception {
            // 可能先收到一个 RoomSnapshot,也可能多次广播,循环到 ROOM_SNAPSHOT
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
