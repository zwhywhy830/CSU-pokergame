package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.ParticleField;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.network.RoomPlayer;
import com.csu.pokergame.network.RoomSnapshot;
import com.csu.pokergame.ui.AppShell;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 局域网联机大厅：单列布局（标题 + 创建/IP/座位/加入 + 状态 + 玩家列表 + 开始）。
 *
 * <p>主机端功能：
 * <ul>
 *   <li>创建房间后显示本机地址，等待真人加入</li>
 *   <li>可对空座位点击「加机器人」补全人数</li>
 *   <li>可对机器人座位点击「移除」</li>
 *   <li>房间满员后「开始游戏」可用</li>
 * </ul>
 *
 * <p>客户端端功能：
 * <ul>
 *   <li>输入主机 IP + 选择座位号 → 加入房间</li>
 *   <li>座位冲突时提示错误，可重试</li>
 *   <li>等待主机开始游戏</li>
 * </ul>
 */
public final class LanLobbyView extends StackPane {

    private LanHost host;
    private LanClient client;
    private ParticleField particles;

    private final GameType gameType;
    private final int requiredPlayers;

    /** 座位选择按钮组（客户端加入时用）。 */
    private ToggleGroup seatGroup;
    private ToggleButton seat2Btn;
    private ToggleButton seat3Btn;
    private ToggleButton seat4Btn;

    /** 复用控件。 */
    private final Label ipDisplay = new Label();
    private final VBox playersBox = new VBox(6);
    private final Button startBtn = new Button("开始游戏");
    private final Button createBtn = new Button("创建房间");
    private final Button joinBtn = new Button("加入房间");
    private final TextField ipField = new TextField();

    public LanLobbyView(AppShell shell) {
        // ---------------- 背景 ----------------
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        particles = bgLayers.particles();

        LobbyHelper.addCornerSuit(this, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        LobbyHelper.addCornerSuit(this, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                particles.play();
            } else if (oldScene != null) {
                particles.stop();
            }
        });

        // ---------------- 游戏类型 ----------------
        boolean isPdk = "PDK".equals(GameChoiceView.SELECTED_GAME);
        gameType = isPdk ? GameType.PAO_DE_KUAI : GameType.LIARS_POKER;
        requiredPlayers = gameType.requiredPlayers();
        String gameName = isPdk ? "湖南跑得快" : "骗子酒馆";

        // ---------------- 顶部条 ----------------
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

        // ---------------- 中央内容 ----------------
        Label title = new Label("局域网联机 · " + gameName);
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("创建房间成为主机，或选择座位加入");
        sub.getStyleClass().add("game-choice-sub");

        createBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");
        ipField.setPromptText("主机 IP，如 192.168.1.100");
        ipField.getStyleClass().add("lan-ip-field");
        ipField.setPrefWidth(200);
        joinBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");

        // 座位选择按钮
        seat2Btn = new ToggleButton("2 号");
        seat3Btn = new ToggleButton("3 号");
        seat4Btn = new ToggleButton("4 号");
        seatGroup = new ToggleGroup();
        seat2Btn.setToggleGroup(seatGroup);
        seat3Btn.setToggleGroup(seatGroup);
        seat4Btn.setToggleGroup(seatGroup);
        seat2Btn.setUserData(PlayerId.SEAT_2);
        seat3Btn.setUserData(PlayerId.SEAT_3);
        seat4Btn.setUserData(PlayerId.SEAT_4);
        // 默认选 2 号
        seat2Btn.setSelected(true);

        // 骗子酒馆 4 人才显示 4 号座位
        seat4Btn.setVisible(!isPdk);
        seat4Btn.setManaged(!isPdk);

        for (ToggleButton btn : new ToggleButton[]{seat2Btn, seat3Btn, seat4Btn}) {
            btn.setOnAction(e -> LobbyHelper.clickSound());
        }

        Label seatLabel = new Label("座位：");
        seatLabel.getStyleClass().add("game-choice-sub");
        HBox seatRow = new HBox(8, seatLabel, seat2Btn, seat3Btn, seat4Btn);
        seatRow.setAlignment(Pos.CENTER);

        HBox row = new HBox(16, createBtn, ipField, seatRow, joinBtn);
        row.setAlignment(Pos.CENTER);

        ipDisplay.getStyleClass().add("game-choice-sub");

        playersBox.setAlignment(Pos.CENTER);

        startBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");
        startBtn.setVisible(false);

        VBox center = new VBox(26, title, sub, row, ipDisplay, playersBox, startBtn);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        getChildren().addAll(center, back, settings);

        // ---------------- 行为 ----------------

        createBtn.setOnAction(e -> {
            LobbyHelper.clickSound();
            try {
                host = new LanHost(gameType);
                host.setOnRoomUpdate(snapshot -> Platform.runLater(() -> updateLobbyRoom(shell, snapshot)));
                host.setOnEnded(reason -> Platform.runLater(() -> {
                    shutdownAll();
                    ipDisplay.setText("对局结束：" + reason);
                }));
                ipDisplay.setText("本机 " + host.localAddress() + ":" + host.port()
                        + "，等待 " + (requiredPlayers - 1) + " 人加入（或加机器人补全）");
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
            // 读取选中的座位
            PlayerId requestedSeat = null;
            ToggleButton selected = (ToggleButton) seatGroup.getSelectedToggle();
            if (selected != null) {
                requestedSeat = (PlayerId) selected.getUserData();
            }
            client = new LanClient(ip);
            client.setOnRoomUpdate(snapshot -> Platform.runLater(() -> updateLobbyRoom(shell, snapshot)));
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
                    case "SEAT_TAKEN" -> "该座位已被占用，请重新选择";
                    case "SEAT_RESERVED_HOST" -> "1 号位是主机座位，请重新选择";
                    case "SEAT_INVALID" -> "该座位在此游戏类型中无效，请重新选择";
                    case "ROOM_FULL" -> "房间已满";
                    default -> "错误：" + code;
                };
                ipDisplay.setText(msg);
            }));
            client.join(requestedSeat); // 指定座位加入
            joinBtn.setDisable(true);
            ipDisplay.setText("正在连接 " + ip + "（座位 " + seatName(requestedSeat) + "）…");
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
                ipDisplay.setText(ex.getMessage());
            }
        });

        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    /**
     * 刷新玩家列表 + 开始按钮状态。
     * 主机端：空座位显示「加机器人」按钮，机器人座位显示「移除」按钮。
     */
    private void updateLobbyRoom(AppShell shell, RoomSnapshot snapshot) {
        playersBox.getChildren().clear();
        boolean isHost = host != null;

        for (RoomPlayer p : snapshot.players()) {
            int seatNum = p.seat().ordinal() + 1;
            String tag;
            if (p.host()) {
                tag = "（主机）";
            } else if (p.bot()) {
                tag = "[AI]";
            } else if (p.connected()) {
                tag = "（已连接）";
            } else {
                tag = "（空位）";
            }
            String name = p.displayName() == null ? "空位" : p.displayName();
            Label label = new Label(seatNum + "号 · " + name + " " + tag + (p.connected() ? "  ●" : "  ○"));
            label.getStyleClass().add("seat-status");

            HBox row = new HBox(8, label);
            row.setAlignment(Pos.CENTER_LEFT);

            // 主机端：空位可加机器人，机器人可移除
            if (isHost && !p.host()) {
                if (!p.connected() && !p.bot()) {
                    Button addBot = new Button("加机器人");
                    addBot.getStyleClass().add("primary");
                    addBot.setOnAction(e -> {
                        LobbyHelper.clickSound();
                        try {
                            host.addBot(p.seat());
                        } catch (Exception ex) {
                            ipDisplay.setText(ex.getMessage());
                        }
                    });
                    row.getChildren().add(addBot);
                } else if (p.bot()) {
                    Button removeBot = new Button("移除");
                    removeBot.getStyleClass().add("danger");
                    removeBot.setOnAction(e -> {
                        LobbyHelper.clickSound();
                        host.removeBot(p.seat());
                    });
                    row.getChildren().add(removeBot);
                }
            }

            playersBox.getChildren().add(row);
        }

        startBtn.setVisible(isHost);
        startBtn.setDisable(!snapshot.full());
        startBtn.setText(snapshot.full() ? "开始游戏" : "等待 (" + requiredPlayers + "人)");
    }

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
        playersBox.getChildren().clear();
    }

    private static String seatName(PlayerId seat) {
        return switch (seat) {
            case SEAT_1 -> "1";
            case SEAT_2 -> "2";
            case SEAT_3 -> "3";
            case SEAT_4 -> "4";
        };
    }
}
