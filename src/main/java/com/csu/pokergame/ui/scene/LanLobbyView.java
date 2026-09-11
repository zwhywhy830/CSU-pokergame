package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.ParticleField;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.network.RoomSnapshot;
import com.csu.pokergame.ui.AppShell;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 局域网联机大厅：单列布局（标题 + 副标题 + 创建/IP/加入一行 + IP 提示 + 玩家列表 + 开始按钮）。
 *
 * <p>对齐 DeckApp.buildLanLobbyScene 的简化版式：不再使用两张卡片，
 * 而是把「创建房间」按钮、IP 输入框、「加入房间」按钮放进同一个 HBox；
 * 游戏类型由 {@link GameChoiceView#SELECTED_GAME} 决定（在更上一页已选定），
 * 加入流程不再让用户选座位，由主机自动分配（{@link LanClient#join()} 无参重载）。
 *
 * <p>主机：点击「创建房间」后显示本机地址、等待玩家加入；房间满员后「开始游戏」可用。
 * 客户端：输入主机 IP 点击「加入房间」后等待主机开始；开始按钮对客户端不可见。
 *
 * <p>背景、四角花色水印、点击音效、按钮反馈全部复用 {@link LobbyHelper} 与
 * {@link BackgroundManager}，CSS 由 {@link AppShell} 全局样式表负责。
 */
public final class LanLobbyView extends StackPane {

    private LanHost host;
    private LanClient client;
    private ParticleField particles;

    /** 复用控件：同一组 UI 同时承担主机/客户端两路流程。 */
    private final Label ipDisplay = new Label();
    private final ListView<String> playerList = new ListView<>();
    private final Button startBtn = new Button("开始游戏");
    private final Button createBtn = new Button("创建房间");
    private final Button joinBtn = new Button("加入房间");
    private final TextField ipField = new TextField();

    private final GameType gameType;
    private final int requiredPlayers;

    public LanLobbyView(AppShell shell) {
        // ---------------- 背景：大厅同源美术层 + 粒子 ----------------
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        particles = bgLayers.particles();

        // 四角花色水印：低透明度 + 微模糊，作暗纹底（与其他大厅页一致）
        LobbyHelper.addCornerSuit(this, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        LobbyHelper.addCornerSuit(this, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                particles.play();
            } else if (oldScene != null) {
                particles.stop();
            }
        });

        // ---------------- 游戏类型：由更上一页 GameChoiceView 选定 ----------------
        boolean isPdk = "PDK".equals(GameChoiceView.SELECTED_GAME);
        gameType = isPdk ? GameType.PAO_DE_KUAI : GameType.LIARS_POKER;
        requiredPlayers = gameType.requiredPlayers();
        String gameName = isPdk ? "湖南跑得快" : "骗子酒馆";

        // ---------------- 顶部条：左返回 / 右设置 ----------------
        Button back = new Button("← 返回模式选择");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            LobbyHelper.clickSound();
            shutdownAll();
            shell.transitionTo("mode-choice", SceneTransition.Type.RETURN_LOBBY);
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        Button settings = new Button("⚙ 设置");
        settings.getStyleClass().add("game-back");
        settings.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.openSettings();
        });
        StackPane.setAlignment(settings, Pos.TOP_RIGHT);
        StackPane.setMargin(settings, new Insets(22, 24, 0, 0));

        // ---------------- 中央内容：单列布局 ----------------
        Label title = new Label("局域网联机 · " + gameName);
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("创建房间成为主机，或输入主机 IP 加入");
        sub.getStyleClass().add("game-choice-sub");

        createBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");
        ipField.setPromptText("主机 IP，如 192.168.1.100");
        ipField.getStyleClass().add("lan-ip-field");
        ipField.setPrefWidth(220);
        joinBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");

        HBox row = new HBox(16, createBtn, ipField, joinBtn);
        row.setAlignment(Pos.CENTER);

        ipDisplay.getStyleClass().add("game-choice-sub");

        playerList.getStyleClass().add("table-log");
        playerList.setPrefHeight(170);
        playerList.setMaxWidth(420);

        startBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");
        startBtn.setVisible(false);

        VBox center = new VBox(26, title, sub, row, ipDisplay, playerList, startBtn);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        getChildren().addAll(center, back, settings);

        // ---------------- 行为：创建 / 加入 / 开始 ----------------
        createBtn.setOnAction(e -> {
            LobbyHelper.clickSound();
            try {
                host = new LanHost(gameType);
                host.setOnRoomUpdate(snapshot -> Platform.runLater(() -> updateLobbyRoom(snapshot)));
                host.setOnEnded(reason -> Platform.runLater(() -> {
                    shutdownAll();
                    ipDisplay.setText("对局结束：" + reason);
                }));
                ipDisplay.setText("本机 " + host.localAddress() + ":" + host.port()
                        + "，等待 " + (requiredPlayers - 1) + " 人加入");
                host.broadcastRoom();
                createBtn.setDisable(true);
            } catch (Exception ex) {
                ipDisplay.setText("创建房间失败：" + ex.getMessage());
            }
        });

        joinBtn.setOnAction(e -> {
            LobbyHelper.clickSound();
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                ipDisplay.setText("请输入主机 IP");
                return;
            }
            client = new LanClient(ip);
            client.setOnRoomUpdate(snapshot -> Platform.runLater(() -> updateLobbyRoom(snapshot)));
            client.setOnStartGame((type, seat) -> Platform.runLater(() -> {
                shell.register("lan-client-table", () -> new LanGameTableView(shell, client, type, seat, ip));
                shell.navigate("lan-client-table");
            }));
            client.setOnEnded(reason -> Platform.runLater(() -> {
                shutdownAll();
                String msg = "HOST_DISCONNECTED".equals(reason) ? "主机已关闭" : "对局结束：" + reason;
                ipDisplay.setText(msg);
            }));
            client.setOnError(code -> Platform.runLater(() -> {
                shutdownAll();
                String msg = switch (code) {
                    case "SEAT_TAKEN" -> "该座位已被占用，请重新加入";
                    case "SEAT_RESERVED_HOST" -> "1 号位是主机座位，请重新加入";
                    case "SEAT_INVALID" -> "该座位在此游戏类型中无效，请重新加入";
                    case "ROOM_FULL" -> "房间已满";
                    default -> "错误：" + code;
                };
                ipDisplay.setText(msg);
            }));
            client.join(); // 不指定座位，由主机自动分配
            joinBtn.setDisable(true);
            ipDisplay.setText("正在连接 " + ip + " …");
        });

        startBtn.setOnAction(e -> {
            LobbyHelper.clickSound();
            try {
                if (host != null) {
                    host.startGame();
                    shell.register("lan-host-table", () -> new LanGameTableView(shell, host));
                    shell.navigate("lan-host-table");
                }
            } catch (Exception ex) {
                // 不应发生：按钮在房间未满时禁用
                ipDisplay.setText(ex.getMessage());
            }
        });

        // 统一为按钮叠加悬浮反馈（不覆盖各自 setOnAction）
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    /** 主机/客户端共用：刷新玩家列表 + 同步开始按钮可见性/可用性。 */
    private void updateLobbyRoom(RoomSnapshot snapshot) {
        var items = snapshot.players().stream()
                .map(p -> p.displayName() + (p.host() ? "（主机）" : "")
                        + (p.connected() ? "  ●" : "  ○"))
                .toList();
        playerList.setItems(FXCollections.observableArrayList(items));
        boolean isHost = host != null;
        startBtn.setVisible(isHost);
        startBtn.setDisable(!snapshot.full());
        startBtn.setText(snapshot.full() ? "开始游戏" : "等待 (" + requiredPlayers + "人)");
    }

    /** 关闭主机/客户端连接，并把 UI 恢复到可再次尝试的状态。 */
    private void shutdownAll() {
        if (host != null) {
            host.shutdown();
            host = null;
        }
        if (client != null) {
            client.shutdown();
            client = null;
        }
        createBtn.setDisable(false);
        joinBtn.setDisable(false);
        startBtn.setVisible(false);
        startBtn.setDisable(true);
        startBtn.setText("开始游戏");
    }
}
