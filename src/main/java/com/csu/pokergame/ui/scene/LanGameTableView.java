package com.csu.pokergame.ui.scene;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PdkSnapshot;
import com.csu.pokergame.paodekuai.PlayPdkCards;
import com.csu.pokergame.ui.AppShell;
import com.csu.pokergame.ui.component.CardView;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 联机游戏桌:同时支持主机视角与客户端视角,根据 GameSnapshot 子类型分发渲染。
 * 主机视角:收到本机操作时通过 {@link LanHost#submitLocalCommand} 提交,引擎由主机持有。
 * 客户端视角:收到本机操作时通过 {@link LanClient#submitCommand} 提交,引擎在主机端。
 */
public final class LanGameTableView extends BorderPane {

    private final AppShell shell;
    private final LanHost host;          // 主机视角时非 null
    private final LanClient client;      // 客户端视角时非 null
    private final GameType gameType;
    private PlayerId localSeat;          // 主机 = SEAT_1,客户端 = snapshot.currentPlayer(开局首张快照时是自己的回合,取 currentPlayer)
    private GameSnapshot lastClientSnapshot;

    private final Set<Card> selected = new LinkedHashSet<>();

    private final Label statusLabel = new Label();
    private final Label hintLabel = new Label();
    private final HBox tableCards = new HBox(8);
    private final HBox handBox = new HBox(8);
    private final ListView<String> log = new ListView<>();
    private final Button primaryButton = new Button();
    private final Button secondaryButton = new Button();

    public LanGameTableView(AppShell shell, LanHost host) {
        this.shell = shell;
        this.host = host;
        this.client = null;
        this.gameType = host.gameType();
        this.localSeat = PlayerId.SEAT_1;

        // 主机收到客户端命令时,直接 apply(已通过 LanHost 内部 onCommand 转交)
        host.setOnCommand(pc -> Platform.runLater(() -> host.handleRemoteCommand(pc.seat(), pc.command())));
        host.setOnEnded(reason -> Platform.runLater(() -> showEnded(reason)));

        buildLayout();
        // 渲染初始状态
        GameSnapshot snap = host.localSnapshot();
        if (snap != null) {
            render(snap);
        }
    }

    public LanGameTableView(AppShell shell, LanClient client, GameType gameType, PlayerId seat, String hostIp) {
        this.shell = shell;
        this.host = null;
        this.client = client;
        this.gameType = gameType;
        this.localSeat = seat;

        client.setOnSnapshot(snap -> Platform.runLater(() -> {
            lastClientSnapshot = snap;
            render(snap);
        }));
        client.setOnEnded(reason -> Platform.runLater(() -> showEnded(reason)));

        buildLayout();
        statusLabel.setText("等待主机广播快照...");
    }

    private void buildLayout() {
        setPadding(new Insets(16));
        setStyle("-fx-background-color: -color-table;");

        Button back = new Button("离开对局");
        back.setOnAction(e -> leaveTable());
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        Label title = new Label(gameType == GameType.PAO_DE_KUAI ? "跑得快 · 联机" : "骗子酒馆 · 联机");
        title.getStyleClass().add("home-title");
        BorderPane topBar = new BorderPane();
        topBar.setLeft(back);
        topBar.setCenter(title);
        topBar.setRight(settings);
        BorderPane.setAlignment(title, Pos.CENTER);
        setTop(topBar);

        VBox center = new VBox(12);
        center.setAlignment(Pos.CENTER);
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);

        tableCards.setAlignment(Pos.CENTER);
        Label tableLabel = new Label("桌面");
        tableLabel.getStyleClass().add("section-label");
        VBox tableArea = new VBox(6, tableLabel, tableCards);
        tableArea.setAlignment(Pos.CENTER);
        tableArea.getStyleClass().add("table-area");

        hintLabel.getStyleClass().add("hint-label");
        hintLabel.setWrapText(true);
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setMaxWidth(Double.MAX_VALUE);

        log.getStyleClass().add("log-view");
        log.setPrefHeight(180);
        VBox logBox = new VBox(6, new Label("事件日志"), log);
        logBox.getStyleClass().add("log-box");

        center.getChildren().addAll(statusLabel, tableArea, hintLabel, logBox);

        ScrollPane centerScroll = new ScrollPane(center);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(centerScroll);

        handBox.setAlignment(Pos.CENTER);
        handBox.getStyleClass().add("hand-box");
        handBox.setMinHeight(Region.USE_PREF_SIZE);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        primaryButton.getStyleClass().addAll("primary", "action-button");
        primaryButton.setOnAction(e -> onPrimary());
        secondaryButton.getStyleClass().add("action-button");
        secondaryButton.setOnAction(e -> onSecondary());
        actions.getChildren().addAll(primaryButton, secondaryButton);

        VBox bottom = new VBox(10, handBox, actions);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        setBottom(bottom);
    }

    private void render(GameSnapshot snap) {
        if (snap instanceof PdkSnapshot p) {
            renderPdk(p);
        } else if (snap instanceof LiarSnapshot l) {
            renderLiar(l);
        }
    }

    // ------ 跑得快渲染 ------

    private void renderPdk(PdkSnapshot snap) {
        // 状态
        String status;
        if (snap.winner().isPresent()) {
            boolean localWon = localSeat != null && snap.winner().get() == localSeat;
            status = localWon ? "你赢了!" : "你输了 (" + name(snap.winner().get()) + " 获胜)";
        } else if (snap.currentPlayer() == localSeat) {
            status = "轮到你出牌";
        } else {
            status = "等待 " + name(snap.currentPlayer()) + " 出牌…";
        }
        statusLabel.setText(status);

        // 桌面:最近一次出牌
        tableCards.getChildren().clear();
        snap.lastMove().ifPresent(move -> {
            for (Card c : move.cards()) {
                CardView cv = new CardView(c);
                cv.setDisable(true);
                tableCards.getChildren().add(cv);
            }
        });

        // 日志
        List<String> reversed = new ArrayList<>(snap.publicEvents());
        java.util.Collections.reverse(reversed);
        log.getItems().setAll(reversed);

        // 手牌
        handBox.getChildren().clear();
        List<Card> sorted = snap.myHand().stream()
                .sorted(Comparator.comparingInt(c -> c.rank().comparisonValue()))
                .toList();
        for (Card card : sorted) {
            CardView cv = new CardView(card);
            cv.setSelected(selected.contains(card));
            cv.setOnAction(e -> {
                if (selected.contains(card)) {
                    selected.remove(card);
                } else {
                    selected.add(card);
                }
                renderPdk((PdkSnapshot) currentSnapshot());
            });
            handBox.getChildren().add(cv);
        }

        // 按钮控制
        boolean myTurn = snap.currentPlayer() == localSeat && snap.winner().isEmpty();
        if (myTurn) {
            primaryButton.setVisible(true);
            primaryButton.setText("出牌");
            primaryButton.setDisable(selected.isEmpty());
            secondaryButton.setVisible(true);
            secondaryButton.setText("不出");
            secondaryButton.setDisable(false);
            hintLabel.setText("选 1–N 张牌后点「出牌」;无牌可出点「不出」");
            hintLabel.setVisible(true);
            hintLabel.setManaged(true);
        } else {
            primaryButton.setVisible(false);
            secondaryButton.setVisible(false);
            hintLabel.setVisible(false);
            hintLabel.setManaged(false);
        }
    }

    private void onPrimary() {
        GameSnapshot snap = currentSnapshot();
        if (snap instanceof PdkSnapshot) {
            if (selected.isEmpty()) return;
            GameCommand cmd = new PlayPdkCards(List.copyOf(selected));
            submitCommand(cmd);
            selected.clear();
        } else if (snap instanceof LiarSnapshot l) {
            if (selected.isEmpty() || selected.size() > 3) return;
            GameCommand cmd = new DeclareLiarCards(List.copyOf(selected));
            submitCommand(cmd);
            selected.clear();
        }
    }

    private void onSecondary() {
        GameSnapshot snap = currentSnapshot();
        if (snap instanceof PdkSnapshot) {
            submitCommand(new PassPdkTurn());
        } else if (snap instanceof LiarSnapshot) {
            submitCommand(new TrustDeclaration());
        }
    }

    private void submitCommand(GameCommand cmd) {
        if (host != null) {
            host.submitLocalCommand(cmd);
            // 主机端 apply 后会触发广播,UI 在广播回调里更新
        } else if (client != null) {
            client.submitCommand(cmd);
        }
    }

    private GameSnapshot currentSnapshot() {
        if (host != null) {
            return host.localSnapshot();
        }
        return lastClientSnapshot;
    }

    // ------ 骗子酒馆渲染 ------

    private void renderLiar(LiarSnapshot snap) {
        String status;
        if (snap.winner().isPresent()) {
            boolean localWon = localSeat != null && snap.winner().get() == localSeat;
            status = localWon ? "你赢了!" : "你输了 (" + name(snap.winner().get()) + " 获胜)";
        } else if (snap.lastResolution().isPresent()) {
            var res = snap.lastResolution().get();
            status = (res.truthful() ? "宣告属实," : "宣告被拆穿,")
                    + name(res.shooter()) + " 扣扳机"
                    + (res.hit() ? " 中弹淘汰" : " 空仓存活");
        } else if (snap.currentPlayer() == localSeat) {
            status = "轮到你";
        } else {
            status = "等待 " + name(snap.currentPlayer()) + "…";
        }
        statusLabel.setText(status);

        // 桌面
        tableCards.getChildren().clear();
        snap.lastResolution().ifPresent(res -> {
            for (Card c : res.actualCards()) {
                CardView cv = new CardView(c);
                cv.setDisable(true);
                tableCards.getChildren().add(cv);
            }
        });
        if (tableCards.getChildren().isEmpty() && snap.pendingDeclaredCount() > 0) {
            for (int i = 0; i < snap.pendingDeclaredCount(); i++) {
                Label back = new Label("?");
                back.getStyleClass().add("card-back");
                tableCards.getChildren().add(back);
            }
        }

        // 日志
        List<String> reversed = new ArrayList<>(snap.publicEvents());
        java.util.Collections.reverse(reversed);
        log.getItems().setAll(reversed);

        // 手牌
        handBox.getChildren().clear();
        List<Card> sorted = snap.myHand().stream()
                .sorted(Comparator.comparingInt(c -> c.rank().comparisonValue()))
                .toList();
        for (Card card : sorted) {
            CardView cv = new CardView(card);
            cv.setSelected(selected.contains(card));
            cv.setOnAction(e -> {
                if (selected.contains(card)) {
                    selected.remove(card);
                } else {
                    selected.add(card);
                }
                renderLiar((LiarSnapshot) currentSnapshot());
            });
            handBox.getChildren().add(cv);
        }

        // 按钮
        boolean myTurn = snap.currentPlayer() == localSeat && snap.winner().isEmpty();
        if (myTurn) {
            primaryButton.setVisible(true);
            secondaryButton.setVisible(true);
            hintLabel.setVisible(true);
            hintLabel.setManaged(true);
            switch (snap.liarPhase()) {
                case DECLARE -> {
                    primaryButton.setText("宣告");
                    primaryButton.setDisable(selected.isEmpty() || selected.size() > 3);
                    secondaryButton.setVisible(false);
                    int n = selected.size();
                    if (n == 0) hintLabel.setText("请选择 1–3 张牌后点「宣告」");
                    else if (n <= 3) hintLabel.setText("已选 " + n + " 张 · 点击「宣告」提交");
                    else hintLabel.setText("已选 " + n + " 张 · 最多只能选 3 张");
                }
                case RESPOND -> {
                    primaryButton.setText("相信");
                    primaryButton.setDisable(false);
                    secondaryButton.setText("质疑");
                    secondaryButton.setDisable(false);
                    hintLabel.setText("相信:不质疑,轮到你出牌 ｜ 质疑:怀疑对方假宣告");
                }
                default -> {
                    primaryButton.setVisible(false);
                    secondaryButton.setVisible(false);
                    hintLabel.setVisible(false);
                    hintLabel.setManaged(false);
                }
            }
        } else {
            primaryButton.setVisible(false);
            secondaryButton.setVisible(false);
            hintLabel.setVisible(false);
            hintLabel.setManaged(false);
        }
    }

    private void showEnded(String reason) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("result-overlay");
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("result-box");

        String text = switch (reason) {
            case "GAME_OVER" -> {
                GameSnapshot snap = currentSnapshot();
                if (snap != null && snap.winner().isPresent() && localSeat != null
                        && snap.winner().get() == localSeat) {
                    yield "你赢了!";
                }
                yield "对局结束";
            }
            case "PLAYER_DISCONNECTED" -> "玩家连接已断开,本局结束";
            case "HOST_SHUTDOWN" -> "主机已关闭";
            default -> "对局已结束 (" + reason + ")";
        };
        Label title = new Label(text);
        title.getStyleClass().add("result-title-lose");

        Button back = new Button("返回大厅");
        back.getStyleClass().add("primary");
        back.setOnAction(e -> leaveTable());

        box.getChildren().addAll(title, back);
        overlay.getChildren().add(box);
        setCenter(overlay);
    }

    private void leaveTable() {
        if (host != null) {
            host.shutdown();
        }
        if (client != null) {
            client.shutdown();
        }
        shell.navigate("lan-lobby");
    }

    private String name(PlayerId p) {
        String base = switch (p) {
            case SEAT_1 -> "玩家 1";
            case SEAT_2 -> "玩家 2";
            case SEAT_3 -> "玩家 3";
            case SEAT_4 -> "玩家 4";
        };
        // 主机视角：若该座位是机器人，加 [AI] 后缀
        if (host != null && host.botSeats().contains(p)) {
            return base + " [AI]";
        }
        return base;
    }
}
