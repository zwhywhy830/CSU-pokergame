package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.CoinBar;
import com.cards.ui.component.PlayedCardsView;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.effect.WinCelebration;
import com.cards.ui.pdk.PdkHandView;
import com.cards.ui.pdk.PdkPlayerSeat;
import com.cards.ui.pdk.PdkTableHeader;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
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
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.ui.AppShell;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    /**
     * 统一金币规则（与 DeckApp 跑得快一致）：入场费扣在进入牌桌时，
     * 胜负奖励在结算时发放；全部经 {@link CoinService} 写入当前账号并落盘。
     */
    private static final int GAME_ENTRY_COST = 100;
    /** 胜利奖励（金币）。 */
    private static final int GAME_WIN_REWARD = 200;
    /** 失败参与奖励（金币）。 */
    private static final int GAME_LOSS_REWARD = 50;

    private final AppShell shell;
    private final PdkEngine engine;
    private final Map<PlayerId, RuleBotController> bots;
    private final Set<Card> selected = new LinkedHashSet<>();
    private final CoinService coinService = CoinService.getInstance();

    private final PdkTableHeader header = new PdkTableHeader();
    private final CoinBar coinBar = header.getCoinBar();
    private final PdkPlayerSeat seat2 = new PdkPlayerSeat("♞", "AI1", 1, false);
    private final PdkPlayerSeat seat3 = new PdkPlayerSeat("♝", "AI2", 1, false);
    private final PlayedCardsView tableCards = new PlayedCardsView();
    private final Label statusText = new Label();
    private final ListView<String> log = new ListView<>();
    private final PdkHandView handView = new PdkHandView(selected);

    /** 本局结算闸门：防止 refresh 重入导致重复加金币 / 播放结算动画。 */
    private boolean settled = false;

    public PdkTableView(AppShell shell) {
        this.shell = shell;
        this.engine = new PdkEngine(new Random());
        this.engine.start();
        this.bots = Map.of(
                PlayerId.SEAT_2, new RuleBotController(new PdkBotPolicy()),
                PlayerId.SEAT_3, new RuleBotController(new PdkBotPolicy()));

        // 入场费：扣款失败则不入桌，由大厅提示余额不足
        if (!coinService.costGold(GAME_ENTRY_COST, "跑得快入场")) {
            throw new IllegalStateException("金币不足，跑得快入场需 " + GAME_ENTRY_COST + " 金币");
        }

        buildLayout();
        wireActions();
        refresh();
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    private void buildLayout() {
        // 背景层（与 wrapTableScene 同源：背景 + 四角花色暗纹 + 返回 / 设置角标）
        BackgroundManager.Background bg = BackgroundManager.createGameBackground();
        StackPane root = new StackPane();
        root.getChildren().add(bg.root());
        LobbyHelper.addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        LobbyHelper.addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        LobbyHelper.addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        LobbyHelper.addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        // ---------- 牌桌本体 ----------
        BorderPane table = new BorderPane();
        table.getStyleClass().add("table-root");
        table.setPrefSize(960, 600);

        // 顶部：比赛信息栏 + AI 玩家座位条（与 buildPdkTableScene 一致）
        header.setMode("跑得快");
        header.setBaseScore(GAME_ENTRY_COST);
        coinBar.setCoins(coinService.getGold());

        seat2.setAlignment(Pos.CENTER);
        seat3.setAlignment(Pos.CENTER);
        HBox seatStrip = new HBox(60, seat2, seat3);
        seatStrip.getStyleClass().add("pdk-seat-strip");
        seatStrip.setAlignment(Pos.CENTER);

        VBox topBar = new VBox(10, header, seatStrip);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 14, 8, 14));
        table.setTop(topBar);

        // 中央：玻璃牌桌 + 出牌区 + 状态提示卡
        statusText.getStyleClass().addAll("table-hint", "pdk-status-card");
        statusText.setMaxWidth(Double.MAX_VALUE);
        statusText.setAlignment(Pos.CENTER);
        statusText.setMinHeight(Region.USE_PREF_SIZE);

        VBox centerCol = new VBox(14, tableCards, statusText);
        centerCol.setAlignment(Pos.CENTER);
        StackPane centerWrap = new StackPane(centerCol);
        centerWrap.getStyleClass().addAll("table-center-wrap", "pdk-table");
        table.setCenter(centerWrap);

        // 底部：玩家手牌
        table.setBottom(handView);

        // 右侧：日志
        log.getStyleClass().add("table-log");
        log.setPrefWidth(220);
        log.setMaxHeight(Double.MAX_VALUE);
        table.setRight(log);

        StackPane.setAlignment(table, Pos.CENTER);
        StackPane.setMargin(table, new Insets(28));
        root.getChildren().add(table);

        // 左上角返回按钮
        Button back = new Button("← 返回模式选择");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> shell.transitionTo("mode-choice", SceneTransition.Type.RETURN_LOBBY));
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));
        root.getChildren().add(back);

        // 右上角设置按钮
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        StackPane.setAlignment(settings, Pos.TOP_RIGHT);
        StackPane.setMargin(settings, new Insets(22, 24, 0, 0));
        root.getChildren().add(settings);

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
        renderStatus(snap);

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
        // 模式 / 底分 / 局数 / 金币栏在 buildLayout 已初始化，刷新只更新模式文案
        header.setMode("跑得快");
    }

    /** 状态提示卡：展示当前轮次 / 上一手出牌张数（对应 DeckApp 的 pdkStatusText）。 */
    private void renderStatus(PdkSnapshot snap) {
        StringBuilder text = new StringBuilder();
        if (snap.currentPlayer() == LOCAL) {
            text.append("轮到你出牌");
        } else {
            text.append("等待 ").append(name(snap.currentPlayer())).append(" 出牌");
        }
        snap.lastMove().ifPresent(move -> text.append("  ·  上一手 ").append(move.cards().size()).append(" 张"));
        statusText.setText(text.toString());
    }

    private void renderSeats(PdkSnapshot snap) {
        seat2.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_2));
        seat3.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_3));
        seat2.setState(snap.currentPlayer() == PlayerId.SEAT_2
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
        seat3.setState(snap.currentPlayer() == PlayerId.SEAT_3
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
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
        // 防重复结算闸门（对应 DeckApp 的 pdkSettled）：第一个进入者完成结算，
        // 之后 refresh 再被触发也不会重复加金币 / 播放结算动画
        if (settled) {
            return;
        }
        settled = true;

        boolean localWon = snap.winner().orElseThrow() == LOCAL;
        // 结算金币（发放胜负奖励），并刷新顶部金币栏
        settlePdk(localWon);
        // 结算反馈音：胜利 → WIN，失败 → LOSE
        AudioService.getInstance().playEffect(localWon ? SoundEffect.WIN : SoundEffect.LOSE);

        // 副标题：其余玩家剩余牌数 + 关门倍率
        StringBuilder subtitle = new StringBuilder();
        for (PlayerId p : List.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3)) {
            if (p == snap.winner().orElseThrow()) {
                continue;
            }
            subtitle.append(name(p)).append(" 剩 ").append(snap.remainingCardCounts().get(p)).append(" 张");
            if (snap.closedDoorPlayers().contains(p)) {
                subtitle.append("（关门×2）");
            }
            subtitle.append("    ");
        }

        // 胜负庆祝层（胜利金色星光 / 失败灰化遮罩）
        StackPane root = (StackPane) getCenter();
        WinCelebration celebration = new WinCelebration(localWon, subtitle.toString().trim(), null);

        // 操作按钮行：重新开始 / 返回选择游戏（叠在 celebration 之上）
        Button again = new Button("重新开始");
        again.getStyleClass().add("primary");
        again.setOnAction(e -> {
            celebration.stop();
            root.getChildren().remove(celebration);
            shell.transitionTo("pdk", SceneTransition.Type.ENTER_GAME);
        });
        Button back = new Button("返回选择游戏");
        back.setOnAction(e -> {
            celebration.stop();
            root.getChildren().remove(celebration);
            shell.transitionTo("mode-choice", SceneTransition.Type.RETURN_LOBBY);
        });
        HBox btnRow = new HBox(22, again, back);
        btnRow.setAlignment(Pos.CENTER);
        StackPane.setAlignment(btnRow, Pos.BOTTOM_CENTER);
        StackPane.setMargin(btnRow, new Insets(0, 0, 60, 0));
        celebration.getChildren().add(btnRow);

        root.getChildren().add(celebration);
    }

    /**
     * 跑得快本局结算：发放胜负奖励并刷新顶部金币栏（对应 DeckApp 的 settlePdk）。
     *
     * <p>入场费已在构造时扣完，这里只发奖励：胜利 {@link #GAME_WIN_REWARD}、
     * 失败 {@link #GAME_LOSS_REWARD}，全部经 {@link CoinService} 写流水并落盘；
     * 完成后同步顶部金币栏显示最新余额。
     */
    private void settlePdk(boolean win) {
        int reward = win ? GAME_WIN_REWARD : GAME_LOSS_REWARD;
        String reason = win ? "跑得快胜利奖励" : "跑得快参与奖励";
        coinService.addGold(reward, reason);
        if (coinBar != null) {
            coinBar.setCoins(coinService.getGold());
        }
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
