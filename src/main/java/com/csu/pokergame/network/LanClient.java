package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 局域网客户端:连接主机,接收房间快照、游戏快照、对局结束事件。
 *
 * <p>线程规则:本类的 I/O 在 {@link ExecutorService} 中执行;收到消息后通过回调,
 * UI 层应在回调中通过 {@code Platform.runLater} 转到 JavaFX 线程处理。
 *
 * <p>使用流程:{@link #join} → 等待 {@code onRoomUpdate} → 等待 {@code onStartGame} →
 * 收到 GameSnapshot 通过 {@code onSnapshot} → 玩家操作调 {@link #submitCommand} →
 * 收到 GameEnded 通过 {@code onEnded} → {@link #shutdown}。
 */
public final class LanClient {

    private final String hostIp;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lan-client-io");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean joined = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** 收到房间快照回调。 */
    private Consumer<RoomSnapshot> onRoomUpdate;
    /** 收到 START_GAME 回调,带 GameType 与客户端被分配的座位。 */
    private BiConsumer<GameType, PlayerId> onStartGame;
    /** 收到游戏快照回调。 */
    private Consumer<GameSnapshot> onSnapshot;
    /** 收到对局结束回调。 */
    private Consumer<String> onEnded;
    /** 收到错误回调。 */
    private Consumer<String> onError;

    public LanClient(String hostIp) {
        this(hostIp, LanHost.FIXED_PORT);
    }

    public LanClient(String hostIp, int port) {
        this.hostIp = hostIp;
        this.port = port;
    }

    public void setOnRoomUpdate(Consumer<RoomSnapshot> cb) { this.onRoomUpdate = cb; }
    public void setOnStartGame(BiConsumer<GameType, PlayerId> cb) { this.onStartGame = cb; }
    public void setOnSnapshot(Consumer<GameSnapshot> cb) { this.onSnapshot = cb; }
    public void setOnEnded(Consumer<String> cb) { this.onEnded = cb; }
    public void setOnError(Consumer<String> cb) { this.onError = cb; }

    /** 连接并发送 JOIN（不指定座位，由主机自动分配；兼容旧版）。 */
    public void join() {
        join(null);
    }

    /**
     * 连接并请求指定座位。
     *
     * @param requestedSeat 请求的座位；null 表示任意空位（让主机自动分配）
     *                      非 null 时主机校验，冲突会回调 onError(SEAT_TAKEN / SEAT_RESERVED_HOST / SEAT_INVALID)
     */
    public void join(PlayerId requestedSeat) {
        ioExecutor.submit(() -> {
            try {
                socket = new Socket(hostIp, port);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                send(new WireMessage.Join(requestedSeat));
                joined.set(true);
                // 进入消息循环
                receiveLoop();
            } catch (IOException e) {
                if (onError != null) {
                    onError.accept("JOIN_FAILED: " + e.getMessage());
                }
                shutdownInternal();
            }
        });
    }

    /** 提交一条命令到主机。 */
    public void submitCommand(GameCommand command) {
        if (!joined.get() || closed.get()) {
            return;
        }
        ioExecutor.submit(() -> {
            try {
                send(new WireMessage.SubmitCommand(command));
            } catch (IOException e) {
                if (onError != null) {
                    onError.accept("SEND_FAILED: " + e.getMessage());
                }
                shutdownInternal();
            }
        });
    }

    public void shutdown() {
        shutdownInternal();
    }

    private void shutdownInternal() {
        if (closed.compareAndSet(false, true)) {
            ioExecutor.shutdownNow();
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    private void receiveLoop() {
        while (!closed.get()) {
            try {
                WireMessage msg = readMessage();
                dispatch(msg);
            } catch (IOException e) {
                // 主机断开或网络错误
                if (!closed.get()) {
                    if (onEnded != null) {
                        onEnded.accept("HOST_DISCONNECTED");
                    }
                }
                shutdownInternal();
                return;
            }
        }
    }

    private void dispatch(WireMessage msg) {
        if (msg instanceof WireMessage.RoomSnapshotMsg rs) {
            if (onRoomUpdate != null) onRoomUpdate.accept(rs.snapshot());
        } else if (msg instanceof WireMessage.StartGame sg) {
            if (onStartGame != null) onStartGame.accept(sg.gameType(), sg.seat());
        } else if (msg instanceof WireMessage.GameSnapshotMsg gs) {
            if (onSnapshot != null) onSnapshot.accept(gs.snapshot());
        } else if (msg instanceof WireMessage.GameEnded ge) {
            if (onEnded != null) onEnded.accept(ge.reason());
        } else if (msg instanceof WireMessage.Error er) {
            if (onError != null) onError.accept(er.code());
        }
        // 客户端不应收到 JOIN / SUBMIT_COMMAND,忽略
    }

    private void send(WireMessage msg) throws IOException {
        byte[] json = JsonCodec.writeBytes(msg);
        if (json.length < 1 || json.length > 1_048_576) {
            throw new IOException("帧长度非法:" + json.length);
        }
        synchronized (out) {
            out.writeInt(json.length);
            out.write(json);
            out.flush();
        }
    }

    private WireMessage readMessage() throws IOException {
        int len = in.readInt();
        if (len < 1 || len > 1_048_576) {
            throw new IOException("帧长度非法:" + len);
        }
        byte[] json = in.readNBytes(len);
        if (json.length != len) {
            throw new IOException("帧不完整");
        }
        return JsonCodec.readMessage(json);
    }
}
