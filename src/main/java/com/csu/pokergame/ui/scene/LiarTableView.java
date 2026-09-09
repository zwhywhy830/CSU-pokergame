package com.csu.pokergame.ui.scene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.core.player.BotDecision;
import com.csu.pokergame.core.player.RuleBotController;
import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.GunState;
import com.csu.pokergame.liarspoker.LiarEngine;
import com.csu.pokergame.liarspoker.LiarFixedPolicy;
import com.csu.pokergame.liarspoker.LiarPhase;
import com.csu.pokergame.liarspoker.LiarRandomPolicy;
import com.csu.pokergame.liarspoker.LiarResolution;
import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.ui.AppShell;
import com.csu.pokergame.ui.component.CardView;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 骗子酒馆游戏桌（本地人机：SEAT_1 为玩家，SEAT_2/3/4 为机器人）。
 * 四个方位布局：本地玩家在底部，AI1/AI2/AI3 在顶部左/中/右三个固定位置，
 * 中央为桌面与日志，避免使用浮层卡片导致位置漂移。
 */
public final class LiarTableView extends BorderPane {

    private static final PlayerId LOCAL = PlayerId.SEAT_1;

    private final AppShell shell;
    private final LiarEngine engine;
    private final Map<PlayerId, RuleBotController> bots;
    private final Set<Card> selected = new LinkedHashSet<>();

    private final Label statusLabel = new Label();
    private final Label targetLabel = new Label();
    private final Label hintLabel = new Label();
    private final HBox seat1Info = new HBox(8);
    private final HBox seat2Info = new HBox(8);
    private final HBox seat3Info = new HBox(8);
    private final HBox seat4Info = new HBox(8);
    private final HBox tableCards = new HBox(8);
    private final HBox handBox = new HBox(8);
    private final ListView<String> log = new ListView<>();
    private final Button declareButton = new Button("宣告");
    private final Button trustButton = new Button("相信");
    private final Button challengeButton = new Button("质疑");

    public LiarTableView(AppShell shell) {
        this.shell = shell;
        this.engine = new LiarEngine(new Random());
        this.engine.start();
        // 三个 AI：AI1 一直质疑，AI2 一直相信，AI3 50% 概率质疑
        this.bots = Map.of(
                PlayerId.SEAT_2, new RuleBotController(new LiarFixedPolicy(true)),
                PlayerId.SEAT_3, new RuleBotController(new LiarFixedPolicy(false)),
                PlayerId.SEAT_4, new RuleBotController(new LiarRandomPolicy(new Random())));

        buildLayout();
        refresh();
    }

    private void buildLayout() {
        setPadding(new Insets(16));
        setStyle("-fx-background-color: -color-table;");

        // 顶部：返回 | 目标点数 | 设置
        Button back = new Button("返回游戏选择");
        back.setOnAction(e -> shell.navigate("game-modes"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> showSettings());
        targetLabel.getStyleClass().add("target-label");
        HBox topCenter = new HBox(16, targetLabel);
        topCenter.setAlignment(Pos.CENTER);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(back);
        topBar.setCenter(topCenter);
        topBar.setRight(settings);
        topBar.setPadding(new Insets(0, 0, 12, 0));
        setTop(topBar);

        // 上方 AI 行：AI1 / AI2 / AI3 三个固定卡片横排
        HBox aiRow = new HBox(24);
        aiRow.setAlignment(Pos.CENTER);
        aiRow.setPadding(new Insets(8, 8, 12, 8));
        aiRow.getChildren().addAll(
                seatCard(PlayerId.SEAT_2, seat2Info),
                seatCard(PlayerId.SEAT_3, seat3Info),
                seatCard(PlayerId.SEAT_4, seat4Info));

        // 中央：状态提示 + 桌面 + 操作引导 + 日志
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);

        tableCards.setAlignment(Pos.CENTER);
        Label tableLabel = new Label("本次宣告");
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

        VBox center = new VBox(12, aiRow, statusLabel, tableArea, hintLabel, logBox);
        center.setAlignment(Pos.TOP_CENTER);
        ScrollPane centerScroll = new ScrollPane(center);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(centerScroll);

        // 底部：本地玩家信息 + 手牌 + 操作栏
        VBox bottom = new VBox(10);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        HBox localInfoRow = new HBox(12);
        localInfoRow.setAlignment(Pos.CENTER);
        localInfoRow.getChildren().add(seatCard(PlayerId.SEAT_1, seat1Info));

        handBox.setAlignment(Pos.CENTER);
        handBox.getStyleClass().add("hand-box");
        handBox.setMinHeight(Region.USE_PREF_SIZE);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        declareButton.getStyleClass().addAll("primary", "action-button");
        declareButton.setOnAction(e -> declare());
        trustButton.getStyleClass().add("action-button");
        trustButton.setOnAction(e -> trust());
        challengeButton.getStyleClass().addAll("danger", "action-button");
        challengeButton.setOnAction(e -> challenge());
        actions.getChildren().addAll(declareButton, trustButton, challengeButton);

        bottom.getChildren().addAll(localInfoRow, handBox, actions);
        setBottom(bottom);
    }

    /** 构造一个座位信息卡片：名称 + 存活状态 + 手枪指示器。 */
    private VBox seatCard(PlayerId seat, HBox gunBox) {
        VBox card = new VBox(6);
        card.getStyleClass().add("seat-card");
        card.setAlignment(Pos.CENTER);

        Label name = new Label(name(seat));
        name.getStyleClass().add("seat-name");
        Label status = new Label();
        status.getStyleClass().add("seat-status");

        gunBox.setAlignment(Pos.CENTER);
        gunBox.getStyleClass().add("gun-row");
        gunBox.getChildren().clear();
        for (int i = 0; i < 6; i++) {
            Label dot = new Label("•");
            dot.getStyleClass().add("chamber");
            gunBox.getChildren().add(dot);
        }

        card.getChildren().addAll(name, status, gunBox);
        return card;
    }

    private void showSettings() {
        shell.openSettings();
    }

    private void refresh() {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        renderStatus(snap);
        renderTarget(snap);
        renderSeats(snap);
        renderTable(snap);
        renderLog(snap);
        renderHand(snap);
        renderHint(snap);

        if (snap.winner().isPresent()) {
            renderResult(snap);
            return;
        }

        switch (snap.liarPhase()) {
            case DECLARE -> {
                if (snap.currentPlayer() == LOCAL) {
                    declareButton.setVisible(true);
                    trustButton.setVisible(false);
                    challengeButton.setVisible(false);
                    boolean valid = !selected.isEmpty() && selected.size() <= 3;
                    declareButton.setDisable(!valid);
                } else {
                    setBotWaiting();
                }
            }
            case RESPOND -> {
                if (snap.currentPlayer() == LOCAL) {
                    declareButton.setVisible(false);
                    trustButton.setVisible(true);
                    challengeButton.setVisible(true);
                    trustButton.setDisable(false);
                    challengeButton.setDisable(false);
                } else {
                    setBotWaiting();
                }
            }
            case RESOLVE, FINISHED -> setBotWaiting();
        }
    }

    private void setBotWaiting() {
        declareButton.setDisable(true);
        trustButton.setDisable(true);
        challengeButton.setDisable(true);
        scheduleBotTurn(engine.snapshotFor(LOCAL).currentPlayer());
    }

    private void renderStatus(LiarSnapshot snap) {
        String text;
        if (snap.lastResolution().isPresent() && snap.liarPhase() != LiarPhase.FINISHED) {
            LiarResolution res = snap.lastResolution().get();
            text = (res.truthful() ? "宣告属实，" : "宣告被拆穿，")
                    + name(res.shooter()) + " 扣扳机"
                    + (res.hit() ? "，中弹淘汰" : "，空仓存活");
        } else if (snap.liarPhase() == LiarPhase.DECLARE) {
            text = snap.currentPlayer() == LOCAL
                    ? "轮到你宣告"
                    : "等待 " + name(snap.currentPlayer()) + " 宣告…";
        } else if (snap.liarPhase() == LiarPhase.RESPOND) {
            text = snap.currentPlayer() == LOCAL
                    ? "轮到你回应"
                    : "等待 " + name(snap.currentPlayer()) + " 回应…";
        } else {
            text = "对局结束";
        }
        statusLabel.setText(text);
    }

    /** 渲染操作引导提示，根据当前阶段动态显示玩家该做什么。 */
    private void renderHint(LiarSnapshot snap) {
        String hint;
        if (snap.winner().isPresent()) {
            hint = "";
        } else if (snap.liarPhase() == LiarPhase.DECLARE && snap.currentPlayer() == LOCAL) {
            int n = selected.size();
            if (n == 0) {
                hint = "请选择 1–3 张牌后点击「宣告」";
            } else if (n <= 3) {
                hint = "已选 " + n + " 张 · 点击「宣告」提交";
            } else {
                hint = "已选 " + n + " 张 · 最多只能选 3 张";
            }
        } else if (snap.liarPhase() == LiarPhase.RESPOND && snap.currentPlayer() == LOCAL) {
            hint = "相信：不质疑，轮到你出牌 ｜ 质疑：怀疑对方假宣告";
        } else {
            hint = "";
        }
        hintLabel.setText(hint);
        hintLabel.setVisible(!hint.isEmpty());
        hintLabel.setManaged(!hint.isEmpty());
    }

    private void renderTarget(LiarSnapshot snap) {
        targetLabel.setText("目标点数：" + snap.targetRank().label());
    }

    private void renderSeats(LiarSnapshot snap) {
        renderSeat(seat1Info, PlayerId.SEAT_1, snap);
        renderSeat(seat2Info, PlayerId.SEAT_2, snap);
        renderSeat(seat3Info, PlayerId.SEAT_3, snap);
        renderSeat(seat4Info, PlayerId.SEAT_4, snap);
    }

    private void renderSeat(HBox gunBox, PlayerId seat, LiarSnapshot snap) {
        VBox card = (VBox) gunBox.getParent();
        Label statusLabel = (Label) card.getChildren().get(1);
        HBox dots = (HBox) card.getChildren().get(2);

        boolean alive = snap.alivePlayers().contains(seat);
        GunState gun = snap.guns().get(seat);
        if (gun == null) {
            return;
        }
        statusLabel.setText(alive
                ? "存活 · 已扣 " + gun.shotsFired() + "/6"
                : "已淘汰");

        for (int i = 0; i < 6; i++) {
            Label dot = (Label) dots.getChildren().get(i);
            dot.getStyleClass().setAll("chamber");
            if (!alive) {
                dot.getStyleClass().add("chamber-dead");
            } else if (i < gun.shotsFired()) {
                dot.getStyleClass().add("chamber-fired");
            }
        }

        card.getStyleClass().setAll("seat-card");
        if (!alive) {
            card.getStyleClass().add("seat-dead");
        } else if (seat == snap.currentPlayer()) {
            card.getStyleClass().add("seat-active");
        }
    }

    private void renderTable(LiarSnapshot snap) {
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
    }

    private void renderLog(LiarSnapshot snap) {
        List<String> events = snap.publicEvents();
        List<String> reversed = new ArrayList<>(events);
        java.util.Collections.reverse(reversed);
        log.getItems().setAll(reversed);
    }

    private void renderHand(LiarSnapshot snap) {
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
                refresh();
            });
            handBox.getChildren().add(cv);
        }
    }

    private void declare() {
        if (selected.isEmpty() || selected.size() > 3) {
            return;
        }
        engine.apply(new DeclareLiarCards(List.copyOf(selected)));
        selected.clear();
        refresh();
    }

    private void trust() {
        engine.apply(new TrustDeclaration());
        refresh();
    }

    private void challenge() {
        engine.apply(new ChallengeDeclaration());
        refresh();
    }

    private void scheduleBotTurn(PlayerId bot) {
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            GameSnapshot snap = engine.snapshotFor(bot);
            List<GameCommand> legal = engine.legalCommands(bot);
            if (legal.isEmpty()) {
                refresh();
                return;
            }
            BotDecision decision = bots.get(bot).decide(snap, legal);
            engine.apply(decision.command());
            refresh();
        });
        pause.play();
    }

    private void renderResult(LiarSnapshot snap) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("result-overlay");
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("result-box");

        boolean localWon = snap.winner().orElseThrow() == LOCAL;
        Label title = new Label(localWon ? "你赢了！" : "你输了！");
        title.getStyleClass().add(localWon ? "result-title-win" : "result-title-lose");

        String reason = "";
        if (snap.lastResolution().isPresent()) {
            LiarResolution res = snap.lastResolution().get();
            if (res.shooter() == LOCAL && res.killed()) {
                reason = "你扣扳机打中子弹仓，被淘汰";
            } else if (!localWon) {
                reason = name(snap.winner().orElseThrow()) + " 是最后存活者";
            }
        }
        Label detail = new Label(reason);
        detail.getStyleClass().add("result-detail");

        Button again = new Button("重新开始");
        again.getStyleClass().add("primary");
        again.setOnAction(e -> shell.navigate("liar"));
        Button back = new Button("返回游戏选择");
        back.setOnAction(e -> shell.navigate("game-modes"));

        HBox buttons = new HBox(12, again, back);
        buttons.setAlignment(Pos.CENTER);

        box.getChildren().addAll(title, detail, buttons);
        overlay.getChildren().add(box);
        setCenter(overlay);
    }

    private static String name(PlayerId p) {
        return switch (p) {
            case SEAT_1 -> "用户";
            case SEAT_2 -> "AI1";
            case SEAT_3 -> "AI2";
            case SEAT_4 -> "AI3";
        };
    }
}
