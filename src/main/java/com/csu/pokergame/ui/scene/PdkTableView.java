package com.csu.pokergame.ui.scene;

import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.PlayedCardsView;
import com.cards.ui.pdk.PdkHandView;
import com.cards.ui.pdk.PdkPlayerSeat;
import com.cards.ui.pdk.PdkTableHeader;
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
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 跑得快游戏桌（本地人机：SEAT_1 为玩家，SEAT_2/SEAT_3 为机器人）。
 *
 * <p>阶段 10：用 deckapp-ui 的 Pdk 组件（PdkTableHeader / PdkPlayerSeat /
 * PlayedCardsView / PdkHandView）替换原先简陋的 Label + CardView 实现，
 * 引擎交互逻辑（PdkEngine / PdkSnapshot / PlayPdkCards / bot 调度）保持不变。
 */
public final class PdkTableView extends BorderPane {

    private static final PlayerId LOCAL = PlayerId.SEAT_1;

    private final AppShell shell;
    private final PdkEngine engine;
    private final Map<PlayerId, RuleBotController> bots;
    private final Set<Card> selected = new LinkedHashSet<>();

    private final PdkTableHeader header = new PdkTableHeader();
    private final PdkPlayerSeat seat2 = new PdkPlayerSeat("♞", "AI1", 1, false);
    private final PdkPlayerSeat seat3 = new PdkPlayerSeat("♝", "AI2", 1, false);
    private final PlayedCardsView tableCards = new PlayedCardsView();
    private final ListView<String> log = new ListView<>();
    private final PdkHandView handView = new PdkHandView(selected);

    public PdkTableView(AppShell shell) {
        this.shell = shell;
        this.engine = new PdkEngine(new Random());
        this.engine.start();
        this.bots = Map.of(
                PlayerId.SEAT_2, new RuleBotController(new PdkBotPolicy()),
                PlayerId.SEAT_3, new RuleBotController(new PdkBotPolicy()));

        buildLayout();
        wireActions();
        refresh();
    }

    private void buildLayout() {
        // 背景层
        BackgroundManager.Background bg = BackgroundManager.createGameBackground();

        // 顶部：返回 + 牌桌信息条 + 设置
        Button back = new Button("返回");
        back.setOnAction(e -> shell.navigate("game-modes"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        HBox topLeft = new HBox(12, back);
        HBox topRight = new HBox(12, settings);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(topLeft);
        topBar.setCenter(header);
        topBar.setRight(topRight);

        // 中央：两家座位 + 桌面牌 + 日志
        seat2.setAlignment(Pos.CENTER);
        seat3.setAlignment(Pos.CENTER);
        HBox seats = new HBox(60, seat2, seat3);
        seats.setAlignment(Pos.CENTER);

        log.getStyleClass().add("log-view");
        log.setPrefHeight(120);

        VBox center = new VBox(16, seats, tableCards, log);
        center.setAlignment(Pos.CENTER);

        // 组装到背景根
        StackPane root = new StackPane(bg.root(), new BorderPane(topBar, center, null, handView, null));
        setCenter(root);
    }

    private void wireActions() {
        handView.getActionBar().getPlayButton().setOnAction(e -> playSelected());
        handView.getActionBar().getPassButton().setOnAction(e -> pass());
        handView.setOnSelectionChange(s -> refreshActions());
    }

    private void refresh() {
        PdkSnapshot snap = (PdkSnapshot) engine.snapshotFor(LOCAL);
        renderHeader(snap);
        renderSeats(snap);
        renderTable(snap);
        renderLog(snap);
        renderHand(snap);

        if (snap.winner().isPresent()) {
            renderResult(snap);
            return;
        }

        PlayerId current = snap.currentPlayer();
        if (current == LOCAL) {
            refreshActions();
        } else {
            handView.getActionBar().setPlayEnabled(false);
            handView.getActionBar().setPassEnabled(false);
            handView.setInteractive(false);
            scheduleBotTurn(current);
        }
    }

    private void renderHeader(PdkSnapshot snap) {
        header.setMode("跑得快");
        header.setRoundText(snap.currentPlayer() == LOCAL ? "轮到你出牌" : "等待 " + name(snap.currentPlayer()));
    }

    private void renderSeats(PdkSnapshot snap) {
        seat2.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_2));
        seat3.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_3));
        seat2.setState(snap.currentPlayer() == PlayerId.SEAT_2
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.IDLE);
        seat3.setState(snap.currentPlayer() == PlayerId.SEAT_3
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.IDLE);
    }

    private void renderTable(PdkSnapshot snap) {
        // PdkMove 不含出牌者信息，快照也未暴露 lastMover，仅展示牌面
        snap.lastMove().ifPresentOrElse(
                move -> tableCards.setCards(move.cards(), null),
                () -> tableCards.setCards(List.of(), null));
    }

    private void renderLog(PdkSnapshot snap) {
        log.getItems().setAll(snap.publicEvents());
        if (!snap.publicEvents().isEmpty()) {
            log.scrollTo(snap.publicEvents().size() - 1);
        }
    }

    private void renderHand(PdkSnapshot snap) {
        handView.setCards(snap.myHand());
        handView.getSeat().setCardCount(snap.myHand().size());
        if (snap.myHand().size() == 1) {
            header.setAuxText("报单！你只剩 1 张牌");
        } else {
            header.setAuxText("");
        }
    }

    private void refreshActions() {
        List<GameCommand> legal = engine.legalCommands(LOCAL);
        boolean hasPlay = legal.stream()
                .filter(cmd -> cmd instanceof PlayPdkCards)
                .map(cmd -> (PlayPdkCards) cmd)
                .anyMatch(p -> new LinkedHashSet<>(p.cards()).equals(selected));
        boolean hasPass = legal.stream().anyMatch(cmd -> cmd instanceof PassPdkTurn);
        handView.getActionBar().setPlayEnabled(hasPlay);
        handView.getActionBar().setPassEnabled(hasPass);
        handView.setInteractive(true);
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
        overlay.getChildren().add(box);
        setCenter(new StackPane(BackgroundManager.createGameBackground().root(), overlay));
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
