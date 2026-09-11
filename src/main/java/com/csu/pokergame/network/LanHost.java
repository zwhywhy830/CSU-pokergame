package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameEngine;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.core.player.BotPolicy;
import com.csu.pokergame.core.player.RuleBotController;
import com.csu.pokergame.liarspoker.LiarEngine;
import com.csu.pokergame.liarspoker.LiarRandomPolicy;
import com.csu.pokergame.paodekuai.PdkBotPolicy;
import com.csu.pokergame.paodekuai.PdkEngine;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 主机权威:在单线程中持有唯一 {@link GameEngine},接受客户端命令、广播快照。
 *
 * <p>生命周期:创建 → 接受客户端加入 → {@link #startGame()} 开始对局 → 客户端/主机提交命令 →
 * 出现 winner 或任一玩家断开时广播 {@code GAME_ENDED} → {@link #shutdown()} 关闭。
 *
 * <p>线程规则:本类内部的 I/O 在 {@link ExecutorService} 中执行;回调由调用者决定线程。
 * 引擎 {@code apply} 永远在调用方线程(由 UI 通过 {@code submitLocalCommand} 在 JavaFX 线程触发)。
 */
public final class LanHost {

    public static final int FIXED_PORT = 46888;

    private final ServerSocket serverSocket;
    private final GameType gameType;
    private final ExecutorService acceptExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lan-host-accept");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "lan-host-client");
        t.setDaemon(true);
        return t;
    });

    /** 已接入客户端,按座位索引。null 表示座位空闲。访问需持锁(this)。 */
    private final Map<PlayerId, ClientConn> clients = new LinkedHashMap<>();
    private final Map<PlayerId, String> displayNames = new EnumMap<>(PlayerId.class);

    /** 机器人座位 → 控制器。访问需持锁(this)。机器人不占 TCP 连接，由主机本地驱动。 */
    private final Map<PlayerId, RuleBotController> bots = new EnumMap<>(PlayerId.class);
    /** 机器人思考延迟下限/上限（毫秒）。 */
    private static final int BOT_DELAY_MIN_MS = 500;
    private static final int BOT_DELAY_MAX_MS = 1500;
    private final Random botDelayRng = new Random();

    /**
     * 机器人任务调度器。默认实现用 JavaFX {@link PauseTransition}（生产环境）。
     * 测试环境可注入同步实现（直接 {@code r.run()}）以避免启动 JavaFX Toolkit。
     */
    @FunctionalInterface
    public interface BotScheduler {
        void schedule(Runnable task, int delayMs);
    }

    private BotScheduler botScheduler = this::defaultScheduleBot;

    /** 默认调度器：JavaFX PauseTransition，必须在 JavaFX Application Thread 调用。 */
    private void defaultScheduleBot(Runnable task, int delayMs) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> task.run());
        pause.play();
    }

    /** 注入调度器（主要供测试用）。必须在 startGame 之前调用。 */
    public void setBotScheduler(BotScheduler scheduler) {
        this.botScheduler = Objects.requireNonNull(scheduler);
    }

    private GameEngine engine;
    private final AtomicBoolean gameStarted = new AtomicBoolean(false);
    private final AtomicBoolean ended = new AtomicBoolean(false);

    /** 房间状态变化回调(给 UI 用,UI 应在 JavaFX 线程处理)。 */
    private Consumer<RoomSnapshot> onRoomUpdate;
    /** 对局开始回调。 */
    private Runnable onStartGame;
    /** 收到客户端命令回调(给 UI 用,UI 应在 JavaFX 线程处理)。 */
    private Consumer<PlayerCommand> onCommand;
    /** 对局结束回调。 */
    private Consumer<String> onEnded;

    public LanHost(GameType gameType) throws IOException {
        this(gameType, FIXED_PORT);
    }

    /** 用于测试:port=0 让系统分配端口。 */
    public LanHost(GameType gameType, int port) throws IOException {
        this.gameType = gameType;
        this.serverSocket = new ServerSocket(port);
        // 主机自己 = SEAT_1
        displayNames.put(PlayerId.SEAT_1, "玩家 1（主机）");
        // 启动接受连接循环
        acceptExecutor.submit(this::acceptLoop);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public GameType gameType() {
        return gameType;
    }

    public String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public void setOnRoomUpdate(Consumer<RoomSnapshot> cb) {
        this.onRoomUpdate = cb;
    }

    public void setOnStartGame(Runnable cb) {
        this.onStartGame = cb;
    }

    public void setOnCommand(Consumer<PlayerCommand> cb) {
        this.onCommand = cb;
    }

    public void setOnEnded(Consumer<String> cb) {
        this.onEnded = cb;
    }

    /** 当前房间快照。线程安全。 */
    public synchronized RoomSnapshot currentRoom() {
        List<RoomPlayer> players = new ArrayList<>();
        for (PlayerId seat : orderedSeats()) {
            boolean isBot = bots.containsKey(seat);
            boolean connected = seat == PlayerId.SEAT_1 || clients.get(seat) != null || isBot;
            String name = displayNames.get(seat);
            players.add(new RoomPlayer(seat, name, seat == PlayerId.SEAT_1, connected, isBot));
        }
        int presentCount = (int) players.stream().filter(RoomPlayer::connected).count();
        return new RoomSnapshot(players, presentCount >= gameType.requiredPlayers(), gameType);
    }

    /** 调用者应在 JavaFX 线程调用,广播房间状态给所有客户端。 */
    public synchronized void broadcastRoom() {
        RoomSnapshot snap = currentRoom();
        if (onRoomUpdate != null) {
            onRoomUpdate.accept(snap);
        }
        WireMessage.RoomSnapshotMsg msg = new WireMessage.RoomSnapshotMsg(snap);
        broadcast(msg);
    }

    /** 开始对局。真人数 + 机器人数 < requiredPlayers 时抛 IllegalStateException。 */
    public synchronized void startGame() {
        if (gameStarted.get()) {
            return;
        }
        RoomSnapshot room = currentRoom();
        int present = (int) room.players().stream().filter(RoomPlayer::connected).count();
        if (present < gameType.requiredPlayers()) {
            throw new IllegalStateException("人数未齐 (" + present + "/" + gameType.requiredPlayers() + ")");
        }
        long seed = System.currentTimeMillis();
        engine = newEngine(gameType, seed);
        engine.start();
        gameStarted.set(true);

        // 给每个客户端发其专属的 START_GAME(带其被分配的座位)
        for (Map.Entry<PlayerId, ClientConn> e : clients.entrySet()) {
            PlayerId seat = e.getKey();
            ClientConn c = e.getValue();
            if (c == null) continue;
            try {
                send(c, new WireMessage.StartGame(gameType, seat));
            } catch (IOException ex) {
                handleDisconnect(seat);
            }
        }
        // 广播每个玩家视角的快照
        broadcastSnapshots();
        if (onStartGame != null) {
            onStartGame.run();
        }
        // 启动机器人循环（若首回合是机器人）
        maybeScheduleBotTurn();
    }

    /** 主机提交自己的命令(在 JavaFX 线程调用)。 */
    public synchronized void submitLocalCommand(GameCommand command) {
        applyCommand(PlayerId.SEAT_1, command);
        maybeScheduleBotTurn();
    }

    /** 收到客户端命令(由 ioExecutor 通过 onCommand 回调驱动,UI 应在 JavaFX 线程处理后再 apply)。 */
    public synchronized void handleRemoteCommand(PlayerId seat, GameCommand command) {
        applyCommand(seat, command);
        maybeScheduleBotTurn();
    }

    private void applyCommand(PlayerId seat, GameCommand command) {
        if (engine == null || ended.get()) {
            return;
        }
        // 引擎会校验当前玩家与合法性,非法抛 IllegalArgumentException
        engine.apply(command);
        broadcastSnapshots();

        // 检查 winner
        GameSnapshot any = engine.snapshotFor(seat);
        if (any.winner().isPresent()) {
            endGame("GAME_OVER");
        }
    }

    /** 广播每个座位视角的 GameSnapshot 给对应客户端(主机用本地 onCommand 已收到变化)。 */
    public synchronized void broadcastSnapshots() {
        if (engine == null) {
            return;
        }
        for (Map.Entry<PlayerId, ClientConn> e : clients.entrySet()) {
            PlayerId seat = e.getKey();
            ClientConn conn = e.getValue();
            if (conn == null) continue;
            GameSnapshot snap = engine.snapshotFor(seat);
            try {
                send(conn, new WireMessage.GameSnapshotMsg(snap));
            } catch (IOException ex) {
                handleDisconnect(seat);
            }
        }
    }

    /** 主机自己获取自己的快照(供主机 UI 直接渲染)。 */
    public synchronized GameSnapshot localSnapshot() {
        if (engine == null) {
            return null;
        }
        return engine.snapshotFor(PlayerId.SEAT_1);
    }

    /** 结束对局。 */
    public synchronized void endGame(String reason) {
        if (!ended.compareAndSet(false, true)) {
            return;
        }
        broadcast(new WireMessage.GameEnded(reason));
        if (onEnded != null) {
            onEnded.accept(reason);
        }
    }

    // ------ 机器人管理 ------

    /**
     * 给指定座位挂机器人。必须在未开局时调用。
     * 约束：seat 非 SEAT_1、未被真人/机器人占用、当前机器人数 < requiredPlayers - 1。
     * 成功后更新 displayName、广播 RoomSnapshot。
     */
    public synchronized void addBot(PlayerId seat) {
        if (gameStarted.get()) {
            throw new IllegalStateException("开局后不能加机器人");
        }
        if (seat == PlayerId.SEAT_1) {
            throw new IllegalArgumentException("主机座位不能加机器人");
        }
        if (!orderedSeats().contains(seat)) {
            throw new IllegalArgumentException("非法座位:" + seat);
        }
        if (clients.get(seat) != null) {
            throw new IllegalStateException("座位已被真人占用:" + seat);
        }
        if (bots.containsKey(seat)) {
            throw new IllegalStateException("座位已有机器人:" + seat);
        }
        if (bots.size() >= gameType.requiredPlayers() - 1) {
            throw new IllegalStateException("机器人数已达上限 (" + (gameType.requiredPlayers() - 1) + ")");
        }
        RuleBotController controller = new RuleBotController(createBotPolicy(gameType));
        bots.put(seat, controller);
        displayNames.put(seat, "机器人 " + (seatIndex(seat) + 1));
        broadcastRoom();
    }

    /** 移除指定座位的机器人。未开局时调用。 */
    public synchronized void removeBot(PlayerId seat) {
        if (gameStarted.get()) {
            throw new IllegalStateException("开局后不能移除机器人");
        }
        if (!bots.containsKey(seat)) {
            return;
        }
        bots.remove(seat);
        displayNames.remove(seat);
        broadcastRoom();
    }

    /** 当前机器人座位集合（不可变快照）。 */
    public synchronized java.util.Set<PlayerId> botSeats() {
        return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(bots.keySet()));
    }

    /** 构造机器人策略：跑得快用 PdkBotPolicy，骗子酒馆用 LiarRandomPolicy。 */
    private BotPolicy createBotPolicy(GameType type) {
        return switch (type) {
            case PAO_DE_KUAI -> new PdkBotPolicy();
            case LIARS_POKER -> new LiarRandomPolicy(new Random());
        };
    }

    /**
     * 若当前引擎回合是机器人座位，安排一次延迟后的机器人出牌。
     * 通过 while 循环跳过连续机器人回合的链式递归（同步调度器场景）；
     * 默认 JavaFX 调度器异步触发，每轮 apply 后会再次调用本方法推进。
     * 必须在 JavaFX 线程被调用（startGame/submitLocalCommand/handleRemoteCommand 均满足）。
     */
    private void maybeScheduleBotTurn() {
        while (true) {
            if (engine == null || ended.get()) {
                return;
            }
            GameSnapshot snap = engine.snapshotFor(PlayerId.SEAT_1);
            PlayerId current = snap.currentPlayer();
            if (current == null) {
                return;
            }
            RuleBotController bot = bots.get(current);
            if (bot == null) {
                return; // 当前是真人，等用户输入
            }
            final PlayerId botSeat = current;
            int delay = BOT_DELAY_MIN_MS + botDelayRng.nextInt(BOT_DELAY_MAX_MS - BOT_DELAY_MIN_MS);
            // 调度器决定同步还是异步执行本轮机器人出牌
            botScheduler.schedule(() -> {
                GameSnapshot botSnap;
                List<GameCommand> legal;
                synchronized (this) {
                    if (engine == null || ended.get()) {
                        return;
                    }
                    botSnap = engine.snapshotFor(botSeat);
                    legal = engine.legalCommands(botSeat);
                }
                // choose 立即返回（RuleBotController 同步决策）
                GameCommand cmd = bot.choose(botSnap, legal).join();
                applyCommand(botSeat, cmd);
                maybeScheduleBotTurn();
            }, delay);
            // 异步调度器：return 等回调；同步调度器：回调已执行完且若 still 同步则会再次进入 while
            return;
        }
    }

    public void shutdown() {
        endGame("HOST_SHUTDOWN");
        acceptExecutor.shutdownNow();
        clientExecutor.shutdownNow();
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
        // 关闭所有客户端 socket（先复制列表，避免 socket.close 触发 handleDisconnect 并发修改 clients）
        List<ClientConn> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(clients.values());
        }
        for (ClientConn c : snapshot) {
            if (c != null) {
                try { c.socket.close(); } catch (IOException ignored) {}
            }
        }
        clients.clear();
    }

    // ------ 内部:接受连接循环 ------

    private void acceptLoop() {
        while (!ended.get()) {
            try {
                Socket socket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(socket));
            } catch (IOException e) {
                if (!ended.get()) {
                    // server socket 关闭或异常
                    break;
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        DataInputStream in = null;
        DataOutputStream out = null;
        PlayerId assignedSeat = null;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // 读首条消息,必须是 JOIN
            WireMessage first = readMessage(in);
            if (!(first instanceof WireMessage.Join join)) {
                send(out, new WireMessage.Error("EXPECTED_JOIN"));
                socket.close();
                return;
            }

            // 分配座位
            synchronized (this) {
                PlayerId requested = join.requestedSeat();
                PlayerId seat = null;
                String errorCode = null;
                if (requested == null) {
                    // 兼容旧客户端：自动分配
                    seat = nextFreeSeat();
                    if (seat == null) {
                        errorCode = "ROOM_FULL";
                    }
                } else if (requested == PlayerId.SEAT_1) {
                    errorCode = "SEAT_RESERVED_HOST";
                } else if (!orderedSeats().contains(requested)) {
                    errorCode = "SEAT_INVALID";
                } else if (clients.get(requested) != null || bots.containsKey(requested)) {
                    errorCode = "SEAT_TAKEN";
                } else {
                    seat = requested;
                }
                if (errorCode != null) {
                    try {
                        send(out, new WireMessage.Error(errorCode));
                    } catch (IOException ignored) {}
                    socket.close();
                    return;
                }
                String name = "玩家 " + (seatIndex(seat) + 1);
                clients.put(seat, new ClientConn(socket, in, out));
                displayNames.put(seat, name);
                assignedSeat = seat;
            }

            // 广播房间更新
            broadcastRoom();

            // 进入消息循环
            while (!ended.get()) {
                WireMessage msg = readMessage(in);
                if (msg instanceof WireMessage.SubmitCommand sc) {
                    GameCommand cmd = sc.command();
                    if (onCommand != null) {
                        onCommand.accept(new PlayerCommand(assignedSeat, cmd));
                    } else {
                        applyCommand(assignedSeat, cmd);
                    }
                } else if (msg instanceof WireMessage.Join) {
                    send(out, new WireMessage.Error("ALREADY_JOINED"));
                } else {
                    send(out, new WireMessage.Error("UNEXPECTED_MESSAGE"));
                }
            }
        } catch (IOException e) {
            // 客户端断线
            handleDisconnect(assignedSeat);
        }
    }

    private synchronized void handleDisconnect(PlayerId seat) {
        if (seat == null) return;
        ClientConn c = clients.remove(seat);
        displayNames.remove(seat);
        if (c != null) {
            try { c.socket.close(); } catch (IOException ignored) {}
        }
        if (gameStarted.get()) {
            // 开局后断线直接结束
            endGame("PLAYER_DISCONNECTED");
        } else {
            broadcastRoom();
        }
    }

    private synchronized void broadcast(WireMessage msg) {
        for (Map.Entry<PlayerId, ClientConn> e : clients.entrySet()) {
            ClientConn c = e.getValue();
            if (c == null) continue;
            try {
                send(c, msg);
            } catch (IOException ex) {
                handleDisconnect(e.getKey());
            }
        }
    }

    private void send(ClientConn c, WireMessage msg) throws IOException {
        send(c.out, msg);
    }

    private void send(DataOutputStream out, WireMessage msg) throws IOException {
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

    private WireMessage readMessage(DataInputStream in) throws IOException {
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

    private List<PlayerId> orderedSeats() {
        // 按座位顺序列出所有可能的玩家位
        List<PlayerId> seats = new ArrayList<>();
        seats.add(PlayerId.SEAT_1);
        for (int i = 1; i < gameType.requiredPlayers(); i++) {
            seats.add(PlayerId.valueOf("SEAT_" + (i + 1)));
        }
        return seats;
    }

    private PlayerId nextFreeSeat() {
        for (PlayerId seat : orderedSeats()) {
            if (seat == PlayerId.SEAT_1) continue; // 主机
            if (bots.containsKey(seat)) continue;    // 机器人占用
            if (!clients.containsKey(seat) || clients.get(seat) == null) {
                return seat;
            }
        }
        return null;
    }

    private static int seatIndex(PlayerId seat) {
        return switch (seat) {
            case SEAT_1 -> 0;
            case SEAT_2 -> 1;
            case SEAT_3 -> 2;
            case SEAT_4 -> 3;
        };
    }

    private static GameEngine newEngine(GameType type, long seed) {
        java.util.Random r = new java.util.Random(seed);
        return switch (type) {
            case PAO_DE_KUAI -> new PdkEngine(r);
            case LIARS_POKER -> new LiarEngine(r);
        };
    }

    /** 收到客户端命令回调的载体。 */
    public record PlayerCommand(PlayerId seat, GameCommand command) {}

    private static final class ClientConn {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;

        ClientConn(Socket socket, DataInputStream in, DataOutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }
    }
}
