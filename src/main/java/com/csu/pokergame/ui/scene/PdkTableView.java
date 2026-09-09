package com.csu.pokergame.ui.scene;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.core.player.BotDecision;
import com.csu.pokergame.core.player.RuleBotController;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PdkBotPolicy;
import com.csu.pokergame.paodekuai.PdkEngine;
import com.csu.pokergame.paodekuai.PdkSnapshot;
import com.csu.pokergame.paodekuai.PlayPdkCards;
import com.csu.pokergame.ui.AppShell;
import com.csu.pokergame.ui.component.CardView;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** 跑得快游戏桌（本地人机：SEAT_1 为玩家，SEAT_2/SEAT_3 为机器人）。 */
public final class PdkTableView extends BorderPane {

    private static final PlayerId LOCAL = PlayerId.SEAT_1;

    private final AppShell shell;
    private final PdkEngine engine;
    private final Map<PlayerId, RuleBotController> bots;
    private final Set<Card> selected = new LinkedHashSet<>();

    private final Label statusLabel = new Label();
    private final Label topInfo = new Label();
    private final Label leftInfo = new Label();
    private final HBox tableCards = new HBox(8);
    private final HBox handBox = new HBox(8);
    private final ListView<String> log = new ListView<>();
    private final Button playButton = new Button("出牌");
    private final Button passButton = new Button("不出");

    public PdkTableView(AppShell shell) {
        this.shell = shell;
        this.engine = new PdkEngine(new Random());
        this.engine.start();
        this.bots = Map.of(
                PlayerId.SEAT_2, new RuleBotController(new PdkBotPolicy()),
                PlayerId.SEAT_3, new RuleBotController(new PdkBotPolicy()));

        buildLayout();
        refresh();
    }

    private void buildLayout() {
        setPadding(new Insets(16));

        // 顶部：左侧返回，中间两家信息，右上角设置
        leftInfo.getStyleClass().add("player-label");
        topInfo.getStyleClass().add("player-label");
        Button back = new Button("返回游戏选择");
        back.setOnAction(e -> shell.navigate("game-modes"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> showSettings());
        HBox info = new HBox(20, leftInfo, topInfo);
        info.setAlignment(Pos.CENTER);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(back);
        topBar.setCenter(info);
        topBar.setRight(settings);
        setTop(topBar);

        // 中央：桌面牌 + 状态
        VBox center = new VBox(12);
        center.setAlignment(Pos.CENTER);
        statusLabel.getStyleClass().add("status-label");
        tableCards.setAlignment(Pos.CENTER);
        Label tableLabel = new Label("桌面");
        tableLabel.getStyleClass().add("section-label");
        center.getChildren().addAll(statusLabel, tableLabel, tableCards);

        // 日志
        log.getStyleClass().add("log-view");
        log.setPrefWidth(260);
        center.getChildren().add(log);
        ScrollPane centerScroll = new ScrollPane(center);
        centerScroll.setFitToWidth(true);
        setCenter(centerScroll);

        // 底部：手牌 + 操作栏
        VBox bottom = new VBox(10);
        handBox.setAlignment(Pos.CENTER);
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        playButton.getStyleClass().add("primary");
        playButton.setOnAction(e -> playSelected());
        passButton.setOnAction(e -> pass());
        Button autoButton = new Button("自动出牌");
        autoButton.setOnAction(e -> autoPlay());
        actions.getChildren().addAll(playButton, passButton, autoButton);
        bottom.getChildren().addAll(handBox, actions);
        setBottom(bottom);
    }

    private void showSettings() {
        shell.openSettings();
    }

    /** 自动出牌：选择能出的最小牌（张数最少、主点最小）并打出。 */
    private void autoPlay() {
        List<GameCommand> legal = engine.legalCommands(LOCAL);
        List<PlayPdkCards> plays = legal.stream()
                .filter(cmd -> cmd instanceof PlayPdkCards)
                .map(cmd -> (PlayPdkCards) cmd)
                .sorted(Comparator
                        .comparingInt((PlayPdkCards p) -> p.cards().size())
                        .thenComparingInt(p -> primaryOf(p)))
                .toList();
        if (plays.isEmpty()) {
            return;
        }
        engine.apply(plays.get(0));
        selected.clear();
        refresh();
    }

    private int primaryOf(PlayPdkCards p) {
        return p.cards().stream()
                .mapToInt(c -> c.rank().comparisonValue())
                .max()
                .orElse(0);
    }

    private void refresh() {
        PdkSnapshot snap = (PdkSnapshot) engine.snapshotFor(LOCAL);
        renderStatus(snap);
        renderOthers(snap);
        renderTable(snap);
        renderLog(snap);
        renderHand(snap);

        if (snap.winner().isPresent()) {
            renderResult(snap);
            return;
        }

        PlayerId current = snap.currentPlayer();
        if (current == LOCAL) {
            refreshActions(snap);
        } else {
            playButton.setDisable(true);
            passButton.setDisable(true);
            scheduleBotTurn(current);
        }
    }

    private void renderStatus(PdkSnapshot snap) {
        statusLabel.setText(snap.currentPlayer() == LOCAL ? "轮到你出牌" : "等待 " + name(snap.currentPlayer()) + " 出牌…");
    }

    private void renderOthers(PdkSnapshot snap) {
        leftInfo.setText(name(PlayerId.SEAT_2) + " 剩 " + snap.remainingCardCounts().get(PlayerId.SEAT_2) + " 张");
        topInfo.setText(name(PlayerId.SEAT_3) + " 剩 " + snap.remainingCardCounts().get(PlayerId.SEAT_3) + " 张");
    }

    private void renderTable(PdkSnapshot snap) {
        tableCards.getChildren().clear();
        snap.lastMove().ifPresent(move -> {
            for (Card c : move.cards()) {
                CardView cv = new CardView(c);
                cv.setDisable(true);
                tableCards.getChildren().add(cv);
            }
        });
    }

    private void renderLog(PdkSnapshot snap) {
        log.getItems().setAll(snap.publicEvents());
        log.scrollTo(snap.publicEvents().size() - 1);
    }

    private void renderHand(PdkSnapshot snap) {
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
        if (snap.myHand().size() == 1) {
            statusLabel.setText("报单！你只剩 1 张牌");
        }
    }

    private void refreshActions(PdkSnapshot snap) {
        List<GameCommand> legal = engine.legalCommands(LOCAL);
        boolean hasPlay = legal.stream()
                .filter(cmd -> cmd instanceof PlayPdkCards)
                .map(cmd -> (PlayPdkCards) cmd)
                .anyMatch(p -> new LinkedHashSet<>(p.cards()).equals(selected));
        playButton.setDisable(!hasPlay);
        boolean hasPass = legal.stream().anyMatch(cmd -> cmd instanceof PassPdkTurn);
        passButton.setDisable(!hasPass);
    }

    private void playSelected() {
        if (selected.isEmpty()) {
            return;
        }
        engine.apply(new PlayPdkCards(List.copyOf(selected)));
        selected.clear();
        refresh();
    }

    private void pass() {
        engine.apply(new PassPdkTurn());
        selected.clear();
        refresh();
    }

    private void scheduleBotTurn(PlayerId bot) {
        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> {
            GameSnapshot snap = engine.snapshotFor(bot);
            List<GameCommand> legal = engine.legalCommands(bot);
            BotDecision decision = bots.get(bot).decide(snap, legal);
            engine.apply(decision.command());
            refresh();
        });
        pause.play();
    }

    private void renderResult(PdkSnapshot snap) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("result-overlay");
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);

        boolean localWon = snap.winner().orElseThrow() == LOCAL;
        Label title = new Label(localWon ? "你赢了！" : name(snap.winner().orElseThrow()) + " 赢了");
        title.getStyleClass().add("result-title");

        StringBuilder detail = new StringBuilder();
        for (PlayerId p : List.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3)) {
            if (p == snap.winner().orElseThrow()) {
                continue;
            }
            detail.append(name(p)).append(" 剩 ").append(snap.remainingCardCounts().get(p)).append(" 张");
            if (snap.closedDoorPlayers().contains(p)) {
                detail.append("（关门×2）");
            }
            detail.append("    ");
        }
        Label detailLabel = new Label(detail.toString());
        detailLabel.getStyleClass().add("result-detail");

        Button again = new Button("重新开始");
        again.getStyleClass().add("primary");
        again.setOnAction(e -> shell.navigate("pdk"));
        Button back = new Button("返回游戏选择");
        back.setOnAction(e -> shell.navigate("game-modes"));

        box.getChildren().addAll(title, detailLabel, new HBox(12, again, back));
        box.setAlignment(Pos.CENTER);
        overlay.getChildren().add(box);
        setCenter(new StackPane(overlay));
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
