package com.csu.pokergame.ui.scene;

import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.network.RoomPlayer;
import com.csu.pokergame.network.RoomSnapshot;
import com.csu.pokergame.network.WireMessage;
import com.csu.pokergame.ui.AppShell;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 局域网联机大厅:两张卡片(创建房间 / 加入房间)。
 * 创建房间后显示主机地址、端口与已加入玩家列表,人数齐时启用"开始游戏"。
 * 加入房间后输入主机 IPv4,等待主机开始。
 */
public final class LanLobbyView extends BorderPane {

    private LanHost host;
    private LanClient client;

    public LanLobbyView(AppShell shell) {
        setPadding(new Insets(16));

        // 顶部:左返回模式选择,中间标题,右设置
        Button back = new Button("返回模式选择");
        back.setOnAction(e -> {
            shutdownAll();
            shell.navigate("mode-choice");
        });
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        Label title = new Label("局域网联机");
        title.getStyleClass().add("home-title");
        BorderPane topBar = new BorderPane();
        topBar.setLeft(back);
        topBar.setCenter(title);
        topBar.setRight(settings);
        BorderPane.setAlignment(title, Pos.CENTER);
        setTop(topBar);

        // 中央:两张卡片
        HBox cards = new HBox(32);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(48, 16, 16, 16));
        cards.getChildren().add(createRoomCard(shell));
        cards.getChildren().add(joinRoomCard(shell));
        setCenter(cards);
    }

    private VBox createRoomCard(AppShell shell) {
        VBox card = new VBox(10);
        card.getStyleClass().add("game-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(280);

        Label title = new Label("创建房间");
        title.getStyleClass().add("title");
        Label desc = new Label("作为主机创建房间,等待其他玩家加入");
        desc.getStyleClass().add("desc");
        desc.setWrapText(true);
        desc.setPrefWidth(240);
        desc.setAlignment(Pos.CENTER);

        // 游戏类型选择
        ToggleButton pdkBtn = new ToggleButton("跑得快 (3 人)");
        ToggleButton liarBtn = new ToggleButton("骗子酒馆 (4 人)");
        pdkBtn.setSelected(true);
        pdkBtn.setOnAction(e -> { pdkBtn.setSelected(true); liarBtn.setSelected(false); });
        liarBtn.setOnAction(e -> { liarBtn.setSelected(true); pdkBtn.setSelected(false); });
        HBox gameTypes = new HBox(8, pdkBtn, liarBtn);
        gameTypes.setAlignment(Pos.CENTER);

        Button create = new Button("创建");
        create.getStyleClass().add("primary");
        create.setOnAction(e -> {
            GameType type = pdkBtn.isSelected() ? GameType.PAO_DE_KUAI : GameType.LIARS_POKER;
            openHostRoom(shell, type);
        });

        card.getChildren().addAll(title, desc, gameTypes, create);
        return card;
    }

    private VBox joinRoomCard(AppShell shell) {
        VBox card = new VBox(10);
        card.getStyleClass().add("game-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(280);

        Label title = new Label("加入房间");
        title.getStyleClass().add("title");
        Label desc = new Label("输入主机 IPv4 地址，选择座位加入房间");
        desc.getStyleClass().add("desc");
        desc.setWrapText(true);
        desc.setPrefWidth(240);
        desc.setAlignment(Pos.CENTER);

        TextField ipField = new TextField();
        ipField.setPromptText("如 192.168.1.10");
        ipField.setPrefWidth(200);

        Label seatLabel = new Label("选择座位：");
        seatLabel.getStyleClass().add("desc");
        ToggleButton seat2 = new ToggleButton("2 号");
        ToggleButton seat3 = new ToggleButton("3 号");
        ToggleButton seat4 = new ToggleButton("4 号");
        javafx.scene.control.ToggleGroup seatGroup = new javafx.scene.control.ToggleGroup();
        seat2.setToggleGroup(seatGroup);
        seat3.setToggleGroup(seatGroup);
        seat4.setToggleGroup(seatGroup);
        seat2.setUserData(PlayerId.SEAT_2);
        seat3.setUserData(PlayerId.SEAT_3);
        seat4.setUserData(PlayerId.SEAT_4);
        HBox seatRow = new HBox(8, seat2, seat3, seat4);
        seatRow.setAlignment(Pos.CENTER);

        Button join = new Button("加入");
        join.getStyleClass().add("primary");
        join.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                return;
            }
            javafx.scene.control.Toggle selected = seatGroup.getSelectedToggle();
            if (selected == null) {
                return; // 未选座位，提示由用户重新点
            }
            PlayerId seat = (PlayerId) selected.getUserData();
            openClientRoom(shell, ip, seat);
        });

        card.getChildren().addAll(title, desc, ipField, seatLabel, seatRow, join);
        return card;
    }

    // ------ 主机房间面板 ------

    private void openHostRoom(AppShell shell, GameType type) {
        try {
            host = new LanHost(type);
        } catch (Exception ex) {
            showHostFailed(shell, "创建主机失败:" + ex.getMessage());
            return;
        }

        VBox roomBox = new VBox(12);
        roomBox.setAlignment(Pos.CENTER);
        roomBox.setPadding(new Insets(48, 16, 16, 16));

        Label title = new Label("等待玩家加入");
        title.getStyleClass().add("home-title");

        Label addr = new Label("主机地址:" + host.localAddress() + ":" + host.port());
        addr.getStyleClass().add("status-label");

        VBox playersBox = new VBox(6);
        Label playersTitle = new Label("座位状态（点空位加机器人，点机器人移除）");
        playersTitle.getStyleClass().add("section-label");

        Button startBtn = new Button("开始游戏");
        startBtn.getStyleClass().add("primary");
        startBtn.setDisable(true);
        startBtn.setOnAction(e -> {
            try {
                host.startGame();
                shell.register("lan-host-table", () -> new LanGameTableView(shell, host));
                shell.navigate("lan-host-table");
            } catch (Exception ex) {
                // 不应发生,因为按钮 disable
            }
        });

        Button cancel = new Button("取消并返回");
        cancel.setOnAction(e -> {
            shutdownAll();
            shell.navigate("lan-lobby");
        });

        roomBox.getChildren().addAll(title, addr, playersTitle, playersBox, startBtn, cancel);
        setCenter(roomBox);

        host.setOnRoomUpdate(snapshot -> Platform.runLater(() -> {
            renderHostPlayersBox(shell, snapshot, playersBox);
            startBtn.setDisable(!snapshot.full());
        }));

        host.setOnEnded(reason -> Platform.runLater(() -> {
            shutdownAll();
            shell.navigate("lan-lobby");
        }));

        // 触发首次广播
        host.broadcastRoom();
    }

    /** 渲染主机大厅的座位列表：主机/真人/机器人/空位 各有不同标签和按钮。 */
    private void renderHostPlayersBox(AppShell shell, RoomSnapshot snapshot, VBox playersBox) {
        playersBox.getChildren().clear();
        for (RoomPlayer p : snapshot.players()) {
            int seatNum = p.seat().ordinal() + 1;
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
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
            Label label = new Label(seatNum + "号 · " + (p.displayName() == null ? "空位" : p.displayName()) + " " + tag);
            label.getStyleClass().add("seat-status");
            row.getChildren().add(label);

            if (!p.connected() && !p.bot()) {
                // 空位 → 加机器人按钮
                Button addBot = new Button("加机器人");
                addBot.getStyleClass().add("primary");
                addBot.setOnAction(e -> {
                    try {
                        host.addBot(p.seat());
                    } catch (Exception ex) {
                        // 超过上限等，忽略；UI 会通过下一次 snapshot 刷新
                    }
                });
                row.getChildren().add(addBot);
            } else if (p.bot()) {
                // 机器人 → 移除按钮
                Button removeBot = new Button("移除");
                removeBot.getStyleClass().add("danger");
                removeBot.setOnAction(e -> host.removeBot(p.seat()));
                row.getChildren().add(removeBot);
            }
            playersBox.getChildren().add(row);
        }
    }

    // ------ 客户端加入面板 ------

    private void openClientRoom(AppShell shell, String hostIp, PlayerId requestedSeat) {
        client = new LanClient(hostIp);

        VBox roomBox = new VBox(12);
        roomBox.setAlignment(Pos.CENTER);
        roomBox.setPadding(new Insets(48, 16, 16, 16));

        Label title = new Label("已加入房间,等待主机开始");
        title.getStyleClass().add("home-title");

        Label status = new Label("连接中...");
        status.getStyleClass().add("status-label");

        VBox playersBox = new VBox(6);
        Label playersTitle = new Label("房间玩家");
        playersTitle.getStyleClass().add("section-label");

        Button leave = new Button("离开房间");
        leave.setOnAction(e -> {
            shutdownAll();
            shell.navigate("lan-lobby");
        });

        roomBox.getChildren().addAll(title, status, playersTitle, playersBox, leave);
        setCenter(roomBox);

        client.setOnRoomUpdate(snapshot -> Platform.runLater(() -> {
            playersBox.getChildren().clear();
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
                Label row = new Label(seatNum + "号 · " + (p.displayName() == null ? "空位" : p.displayName()) + " " + tag);
                row.getStyleClass().add("seat-status");
                playersBox.getChildren().add(row);
            }
            status.setText("等待主机开始游戏 (" + snapshot.players().stream().filter(RoomPlayer::connected).count()
                    + "/" + snapshot.gameType().requiredPlayers() + ")");
        }));
        client.setOnStartGame((type, seat) -> Platform.runLater(() -> {
            shell.register("lan-client-table", () -> new LanGameTableView(shell, client, type, seat, hostIp));
            shell.navigate("lan-client-table");
        }));
        client.setOnEnded(reason -> Platform.runLater(() -> {
            shutdownAll();
            String msg = "HOST_DISCONNECTED".equals(reason) ? "主机已关闭" : "对局结束:" + reason;
            showHostFailed(shell, msg);
        }));
        client.setOnError(code -> Platform.runLater(() -> {
            shutdownAll();
            String msg = switch (code) {
                case "SEAT_TAKEN" -> "该座位已被占用，请返回重新选择";
                case "SEAT_RESERVED_HOST" -> "1 号位是主机座位，请重新选择";
                case "SEAT_INVALID" -> "该座位在此游戏类型中无效，请重新选择";
                case "ROOM_FULL" -> "房间已满";
                default -> "错误:" + code;
            };
            showHostFailed(shell, msg);
        }));

        client.join(requestedSeat);
    }

    private void showHostFailed(AppShell shell, String message) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48, 16, 16, 16));

        Label title = new Label(message);
        title.getStyleClass().add("home-title");

        Button back = new Button("返回大厅");
        back.getStyleClass().add("primary");
        back.setOnAction(e -> shell.navigate("lan-lobby"));

        box.getChildren().addAll(title, back);
        setCenter(box);
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
    }
}
