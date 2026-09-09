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
            shell.navigate("game-modes");
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
        Label desc = new Label("输入主机 IPv4 地址加入已创建的房间");
        desc.getStyleClass().add("desc");
        desc.setWrapText(true);
        desc.setPrefWidth(240);
        desc.setAlignment(Pos.CENTER);

        TextField ipField = new TextField();
        ipField.setPromptText("如 192.168.1.10");
        ipField.setPrefWidth(200);

        Button join = new Button("加入");
        join.getStyleClass().add("primary");
        join.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                return;
            }
            openClientRoom(shell, ip);
        });

        card.getChildren().addAll(title, desc, ipField, join);
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
        Label playersTitle = new Label("已加入玩家");
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
            playersBox.getChildren().clear();
            for (RoomPlayer p : snapshot.players()) {
                String tag = p.host() ? "（主机）" : (p.connected() ? "" : "（等待中）");
                Label row = new Label(p.displayName() + tag);
                row.getStyleClass().add("seat-status");
                playersBox.getChildren().add(row);
            }
            startBtn.setDisable(!snapshot.full());
        }));

        host.setOnEnded(reason -> Platform.runLater(() -> {
            shutdownAll();
            shell.navigate("lan-lobby");
        }));

        // 触发首次广播
        host.broadcastRoom();
    }

    // ------ 客户端加入面板 ------

    private void openClientRoom(AppShell shell, String hostIp) {
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
                String tag = p.host() ? "（主机）" : (p.connected() ? "" : "（等待中）");
                Label row = new Label(p.displayName() + tag);
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
            showHostFailed(shell, "错误:" + code);
        }));

        client.join();
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
