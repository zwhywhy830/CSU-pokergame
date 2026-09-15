package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.CoinBar;
import com.cards.ui.component.GrowthResultPanel;
import com.cards.ui.component.PlayedCardsView;
import com.cards.ui.component.PlayHistoryPanel;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.effect.WinCelebration;
import com.csu.pokergame.player.Achievement;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.player.PlayerStatsService;
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
import com.csu.pokergame.paodekuai.PdkMoveType;
import com.csu.pokergame.paodekuai.PlayPdkCards;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.ui.AppShell;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
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
    /** 胜利获得经验。 */
    private static final int PDK_WIN_EXP = 50;
    /** 失败获得经验。 */
    private static final int PDK_LOSS_EXP = 20;

    private final AppShell shell;
    private final PdkEngine engine;
    private final Map<PlayerId, RuleBotController> bots;
    private final Set<Card> selected = new LinkedHashSet<>();
    private final CoinService coinService = CoinService.getInstance();

    private final PdkTableHeader header = new PdkTableHeader();
    private final CoinBar coinBar = header.getCoinBar();
    // AI 座位等级固定 8（与骗子酒馆 LiarTableView 的 AI 口径一致）
    private final PdkPlayerSeat seat2 = new PdkPlayerSeat("♞", "AI1", 8, false);
    private final PdkPlayerSeat seat3 = new PdkPlayerSeat("♝", "AI2", 8, false);
    private final PlayedCardsView tableCards = new PlayedCardsView();
    private final Label statusText = new Label();
    private final PlayHistoryPanel log = new PlayHistoryPanel();
    private final PdkHandView handView = new PdkHandView(selected);

    /** 本局结算闸门：防止 refresh 重入导致重复加金币 / 播放结算动画。 */
    private boolean settled = false;
    /** Bot 回合调度锁：防止 refresh 多次触发重复 PauseTransition 叠加。 */
    private boolean botScheduled = false;

    /** 出牌倒计时（15 秒）。轮到任何玩家时启动，超时自动出牌。 */
    private static final int TURN_TIME_LIMIT = 15;
    /** 本地玩家倒计时时间线（超时会触发自动出牌）。 */
    private Timeline turnTimer;
    /** AI 回合显示用倒计时时间线（只更新秒数，不触发超时动作）。 */
    private Timeline botDisplayTimer;
    /** 当前剩余秒数。 */
    private int turnSecondsLeft;

    // ----- 结算成长反馈字段（对应 DeckApp 的 pdkWinForResult 等） -----
    private boolean winForResult;
    private int goldDeltaForResult;
    private int expGainForResult;
    private PlayerGrowthService.LevelUpResult growthForResult;
    private final List<Achievement> achievementsForResult = new ArrayList<>();

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
        // 进入跑得快牌桌：切换为斗地主经典风格 BGM（音乐关闭时静默跳过）
        // 开局不播语音，"要不起"仅在点击"不出"时触发
        AudioService.getInstance().playMusic(AudioService.BGM_PDK);
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

        // 右侧：出牌历史面板
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
            startTurnTimer();
        } else {
            stopTurnTimer();
            stopBotDisplayTimer();
            handView.getActionBar().setPlayEnabled(false);
            handView.getActionBar().setPassEnabled(false);
            handView.setInteractive(false);
            // AI 回合用显示倒计时（不触发超时自动出牌）
            startBotDisplayTimer();
            scheduleBotTurn(current);
        }
    }

    private void renderHeader(PdkSnapshot snap) {
        // 模式 / 底分 / 局数 / 金币栏在 buildLayout 已初始化，刷新只更新模式文案
        header.setMode("跑得快");
    }

    /** 状态提示卡：展示当前轮次 / 上一手出牌张数 + 倒计时（对应 DeckApp 的 pdkStatusText）。 */
    private void renderStatus(PdkSnapshot snap) {
        StringBuilder text = new StringBuilder();
        if (snap.currentPlayer() == LOCAL) {
            text.append("轮到你出牌");
            if (turnSecondsLeft > 0) {
                text.append("  ·  ⏱ ").append(turnSecondsLeft).append("s");
            }
        } else {
            text.append("等待 ").append(name(snap.currentPlayer())).append(" 出牌");
            if (turnSecondsLeft > 0) {
                text.append("  ·  ⏱ ").append(turnSecondsLeft).append("s");
            }
        }
        snap.lastMove().ifPresent(move -> text.append("  ·  上一手 ").append(move.cards().size()).append(" 张"));
        statusText.setText(text.toString());
        // 最后 5 秒红色警告样式
        statusText.getStyleClass().removeAll("timer-warning");
        if (turnSecondsLeft > 0 && turnSecondsLeft <= 5) {
            statusText.getStyleClass().add("timer-warning");
        }
    }

    private void renderSeats(PdkSnapshot snap) {
        // 本人座位实时同步真实等级（PdkHandView 构造时为占位 Lv.1，这里必须覆盖，
        // 否则牌桌内一直显示 Lv.1，与大厅 / 结算面板的真实等级不一致）
        handView.getSeat().setLevel(PlayerManager.getInstance().getProfile().getLevel());
        seat2.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_2));
        seat3.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_3));
        seat2.setState(snap.currentPlayer() == PlayerId.SEAT_2
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
        seat3.setState(snap.currentPlayer() == PlayerId.SEAT_3
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
        // 所有座位同步倒计时：当前出牌方显示秒数，其他座位隐藏
        int secs = turnSecondsLeft;
        handView.getSeat().setTurnTimer(snap.currentPlayer() == LOCAL ? secs : 0);
        seat2.setTurnTimer(snap.currentPlayer() == PlayerId.SEAT_2 ? secs : 0);
        seat3.setTurnTimer(snap.currentPlayer() == PlayerId.SEAT_3 ? secs : 0);
    }

    private void renderTable(PdkSnapshot snap) {
        String playerName = snap.lastPlayer().map(PdkTableView::name).orElse(null);
        snap.lastMove().ifPresentOrElse(
                move -> tableCards.setCards(move.cards(), playerName),
                () -> tableCards.setCards(List.of(), null));
    }

    private void renderLog(PdkSnapshot snap) {
        log.setEvents(snap.publicEvents());
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
        stopTurnTimer();
        stopBotDisplayTimer();
        // 记录飞牌起点坐标（手牌节点场景坐标）
        List<double[]> flyFrom = new ArrayList<>();
        for (var card : selected) {
            var node = handView.getCardNode(card);
            if (node != null) {
                var b = node.localToScene(node.getBoundsInLocal());
                flyFrom.add(new double[]{b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2});
            }
        }

        List<Card> playedCards = List.copyOf(selected);
        engine.apply(new PlayPdkCards(playedCards));
        selected.clear();
        refresh();

        // 飞牌动画：从手牌位置飞向桌面中央（统一走 GameAnimationService，受动画开关控制）
        if (!flyFrom.isEmpty()) {
            Platform.runLater(() -> GameAnimationService.getInstance()
                    .playCardAnimation(() -> tableCards.playFlyIn(playedCards, "你",
                            flyFrom.get(0)[0], flyFrom.get(0)[1], null), null));
        }
        // 出牌成功的小提示，显示牌型（顺子 / 三带一 / 炸弹 …）
        PdkSnapshot after = (PdkSnapshot) engine.snapshotFor(LOCAL);
        after.lastMove().ifPresent(move ->
                GameAnimationService.getInstance().showToast(handView, moveTypeLabel(move.type())));
    }

    private void pass() {
        stopTurnTimer();
        stopBotDisplayTimer();
        // "要不起"语音包（斗地主经典体验）
        AudioService.getInstance().playEffect(SoundEffect.PDK_CANNOT_PLAY);
        engine.apply(new PassPdkTurn());
        selected.clear();
        refresh();
    }

    private void scheduleBotTurn(PlayerId bot) {
        // 重入保护：上一个 bot 定时器还没跑完就不要叠新的，否则 legal 空 → refresh → 再 schedule → 无限循环
        if (botScheduled) {
            return;
        }
        botScheduled = true;
        PauseTransition pause = new PauseTransition(Duration.millis(3000));
        pause.setOnFinished(e -> {
            try {
                GameSnapshot snap = engine.snapshotFor(bot);
                List<GameCommand> legal = engine.legalCommands(bot);
                // 状态可能已推进（重入 / 异常重置）：此时不应再替 bot 出牌，直接刷新
                if (legal.isEmpty()) {
                    // 先释放调度锁再 refresh：refresh 会重新调度当前 bot，持锁会导致调度被跳过
                    botScheduled = false;
                    refresh();
                    return;
                }
                BotDecision decision = bots.get(bot).decide(snap, legal);
                boolean played = decision.command() instanceof PlayPdkCards;
                boolean passed = decision.command() instanceof PassPdkTurn;
                engine.apply(decision.command());
                // 关键：先释放调度锁再 refresh()。refresh 会链式调度下一位 bot，
                // 若等 finally 才释放，下一位 bot（如 AI2）的调度会被重入锁跳过，造成永不出牌
                botScheduled = false;
                refresh();
                if (passed) {
                    // AI 过牌："要不起"语音包
                    AudioService.getInstance().playEffect(SoundEffect.PDK_CANNOT_PLAY);
                }
                if (played) {
                    PdkSnapshot after = (PdkSnapshot) engine.snapshotFor(LOCAL);
                    after.lastMove().ifPresent(move -> {
                        double botX = getScene().getWidth() / 2;
                        double botY = 100;
                        Platform.runLater(() -> GameAnimationService.getInstance()
                                .playCardAnimation(() -> tableCards.playFlyIn(move.cards(), name(bot),
                                        botX, botY, null), null));
                        GameAnimationService.getInstance().showToast(handView, moveTypeLabel(move.type()));
                    });
                }
            } catch (Throwable t) {
                // 异常不能吞：打印堆栈方便诊断，并主动刷新避免"AI 不出牌"卡死
                t.printStackTrace();
                botScheduled = false;
                refresh();
            } finally {
                botScheduled = false;
            }
        });
        pause.play();
    }

    // ============================================================= 出牌倒计时

    /** 启动 15 秒出牌倒计时，每秒刷新状态提示，超时自动出牌（最小单张或 pass）。 */
    private void startTurnTimer() {
        // 互斥不变式：本地倒计时与 AI 显示倒计时任一时刻只能有一个在跑，
        // 否则两个 Timeline 同时递减共享的 turnSecondsLeft，倒计时会双倍速
        stopBotDisplayTimer();
        stopTurnTimer();
        turnSecondsLeft = TURN_TIME_LIMIT;
        updateTimerDisplay();
        turnTimer = new Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),
                        e -> {
                            turnSecondsLeft--;
                            updateTimerDisplay();
                            if (turnSecondsLeft <= 0) {
                                stopTurnTimer();
                                onTurnTimeout();
                            }
                        }));
        turnTimer.setCycleCount(TURN_TIME_LIMIT);
        turnTimer.play();
    }

    /** 停止倒计时并清零显示。 */
    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
            turnTimer = null;
        }
        turnSecondsLeft = 0;
        statusText.getStyleClass().removeAll("timer-warning");
    }

    /** 启动 AI 回合显示用倒计时：只更新秒数显示，超时自动结束（不触发出牌动作）。 */
    private void startBotDisplayTimer() {
        // 互斥不变式：见 startTurnTimer
        stopTurnTimer();
        stopBotDisplayTimer();
        turnSecondsLeft = TURN_TIME_LIMIT;
        updateTimerDisplay();
        botDisplayTimer = new Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),
                        e -> {
                            turnSecondsLeft--;
                            updateTimerDisplay();
                        }));
        botDisplayTimer.setCycleCount(TURN_TIME_LIMIT);
        botDisplayTimer.play();
    }

    /** 停止 AI 回合显示倒计时。 */
    private void stopBotDisplayTimer() {
        if (botDisplayTimer != null) {
            botDisplayTimer.stop();
            botDisplayTimer = null;
        }
    }

    /** 刷新状态提示卡上的倒计时显示。 */
    private void updateTimerDisplay() {
        PdkSnapshot snap = (PdkSnapshot) engine.snapshotFor(LOCAL);
        renderStatus(snap);
    }

    /**
     * 倒计时超时：自动为本地玩家出牌。
     * <ul>
     *   <li>有合法出牌 → 出最小单张（避免错过回合）</li>
     *   <li>无合法出牌 → pass</li>
     * </ul>
     */
    private void onTurnTimeout() {
        if (settled) {
            return;
        }
        // 防御：超时自动出牌只作用于本地玩家回合。AI 回合由 scheduleBotTurn 驱动，
        // 若倒计时在 AI 回合异常归零，不能替本地玩家（实际会被引擎按当前 AI 执行）误操作
        PdkSnapshot current = (PdkSnapshot) engine.snapshotFor(LOCAL);
        if (current.winner().isPresent() || current.currentPlayer() != LOCAL) {
            return;
        }
        List<GameCommand> legal = engine.legalCommands(LOCAL);
        if (legal.isEmpty()) {
            pass();
            return;
        }
        // 优先选最小单张出牌，避免超时后出大牌
        GameCommand auto = legal.stream()
                .filter(c -> c instanceof PlayPdkCards)
                .map(c -> (PlayPdkCards) c)
                .min(Comparator.comparingInt(c -> c.cards().size()))
                .map(c -> (GameCommand) c)
                .orElseGet(() -> legal.stream()
                        .filter(c -> c instanceof PassPdkTurn)
                        .findFirst()
                        .orElse(legal.get(0)));
        selected.clear();
        if (auto instanceof PlayPdkCards ppc) {
            selected.addAll(ppc.cards());
        }
        playSelected();
    }

    private void renderResult(PdkSnapshot snap) {
        // 防重复结算闸门（对应 DeckApp 的 pdkSettled）：第一个进入者完成结算，
        // 之后 refresh 再被触发也不会重复加金币 / 播放结算动画
        if (settled) {
            return;
        }
        settled = true;
        stopTurnTimer();
        stopBotDisplayTimer();

        boolean localWon = snap.winner().orElseThrow() == LOCAL;
        // 结算金币（发放胜负奖励），并刷新顶部金币栏
        settlePdk(localWon);
        // 结算反馈音：胜利 / 失败语音包（跑得快专属）
        AudioService.getInstance().playEffect(localWon ? SoundEffect.PDK_WIN : SoundEffect.PDK_LOSE);

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

        // 胜负庆祝层（胜利金色星光 / 失败灰化遮罩）+ 成长反馈面板
        StackPane root = (StackPane) getCenter();
        javafx.scene.Node growthPanel = buildGrowthPanel();

        final WinCelebration[] celebrationRef = new WinCelebration[1];

        // 操作按钮行：重新开始 / 返回选择游戏（整合进结算卡片底部）
        Button again = new Button("重新开始");
        again.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-restart");
        again.setOnAction(e -> {
            celebrationRef[0].stop();
            root.getChildren().remove(celebrationRef[0]);
            shell.transitionTo("pdk", SceneTransition.Type.ENTER_GAME);
        });
        Button back = new Button("返回选择游戏");
        back.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-back");
        back.setOnAction(e -> {
            celebrationRef[0].stop();
            root.getChildren().remove(celebrationRef[0]);
            shell.transitionTo("mode-choice", SceneTransition.Type.RETURN_LOBBY);
        });
        HBox btnRow = new HBox(22, again, back);
        btnRow.setAlignment(Pos.CENTER);

        celebrationRef[0] = new WinCelebration(localWon, subtitle.toString().trim(), growthPanel, btnRow);

        root.getChildren().add(celebrationRef[0]);

        // 结算表现动画：胜负动画 + 金币飞入 + 升级光环 + 提示条
        GameAnimationService anim = GameAnimationService.getInstance();
        if (localWon) {
            anim.playWinAnimation(root, celebrationRef[0]);
        } else {
            anim.playLoseAnimation(celebrationRef[0]);
        }
        boolean upgraded = growthForResult != null && growthForResult.upgraded();
        PauseTransition settleDelay = new PauseTransition(Duration.millis(140));
        settleDelay.setOnFinished(e -> {
            javafx.scene.Node coinTarget = coinBar != null ? coinBar : celebrationRef[0];
            anim.playCoinAnimation(growthPanel, coinTarget, null);
            if (upgraded) {
                anim.playLevelUpAnimation(growthPanel);
            }
            anim.showToast(celebrationRef[0], settleToastText(upgraded));
        });
        settleDelay.play();
    }

    /** 结算提示条文案：金币 / 经验 / 升级 / 成就。 */
    private String settleToastText(boolean upgraded) {
        StringBuilder msg = new StringBuilder();
        msg.append("＋").append(goldDeltaForResult).append(" 金币")
                .append(" · ＋").append(expGainForResult).append(" 经验");
        if (upgraded && growthForResult != null) {
            msg.append(" · ✨ Level UP Lv.").append(growthForResult.getOldLevel())
                    .append(" → Lv.").append(growthForResult.getNewLevel());
        }
        if (!achievementsForResult.isEmpty()) {
            msg.append(" · 🏆 ").append(achievementsForResult.get(0).getTitle());
        }
        return msg.toString();
    }

    /**
     * 跑得快本局结算：发放胜负奖励并刷新顶部金币栏（对应 DeckApp 的 settlePdk）。
     *
     * <p>入场费已在构造时扣完，这里只发奖励：胜利 {@link #GAME_WIN_REWARD}、
     * 失败 {@link #GAME_LOSS_REWARD}，全部经 {@link CoinService} 写流水并落盘；
     * 完成后同步顶部金币栏显示最新余额。
     */
    private void settlePdk(boolean win) {
        int goldDelta = win ? GAME_WIN_REWARD : GAME_LOSS_REWARD;
        int expGain = win ? PDK_WIN_EXP : PDK_LOSS_EXP;
        String reason = win ? "跑得快胜利奖励" : "跑得快参与奖励";
        coinService.addGold(goldDelta, reason);

        // 加经验并自动升级
        PlayerGrowthService growthService = PlayerGrowthService.getInstance();
        growthForResult = growthService.addExp(expGain);

        // 战绩统计
        PlayerStatsService.getInstance().recordGameResult(win);

        // 战绩明细
        GameRecordService.getInstance().addRecord(
                GameRecordService.GAME_PDK, win, goldDelta, expGain, GameRecordService.OPPONENT_AI);

        // 成就检测
        if (win) {
            achievementsForResult.addAll(AchievementService.getInstance().checkOnWin());
        }
        achievementsForResult.addAll(AchievementService.getInstance().checkOnGoldChange());
        achievementsForResult.addAll(AchievementService.getInstance().checkOnLevelUp());

        // 记录本局成长反馈
        winForResult = win;
        goldDeltaForResult = goldDelta;
        expGainForResult = expGain;

        if (coinBar != null) {
            coinBar.setCoins(coinService.getGold());
        }
    }

    /**
     * 构建跑得快结算成长反馈面板（对应 DeckApp.buildPdkGrowthPanel）。
     */
    private javafx.scene.Node buildGrowthPanel() {
        PlayerProfile profile = PlayerManager.getInstance().getProfile();
        PlayerGrowthService growthService = PlayerGrowthService.getInstance();
        int level = profile.getLevel();
        GrowthResultPanel panel = new GrowthResultPanel(
                winForResult,
                goldDeltaForResult,
                expGainForResult,
                level,
                growthService.getLevelTitle(level),
                growthService.getLevelBadge(level),
                profile.getExp(),
                growthService.expToNextLevel(level),
                growthForResult);
        if (achievementsForResult.isEmpty()) {
            return panel;
        }
        VBox box = new VBox(10, panel, buildAchievementNotice());
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** 本局解锁的成就提示条。 */
    private VBox buildAchievementNotice() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        for (Achievement a : achievementsForResult) {
            Label label = new Label("🏆 成就解锁：" + a.getTitle()
                    + "　奖励 +" + a.getRewardGold() + " 金币 / +" + a.getRewardExp() + " 经验");
            label.getStyleClass().add("growth-result-achievement");
            box.getChildren().add(label);
        }
        return box;
    }

    private static String name(PlayerId p) {
        return switch (p) {
            case SEAT_1 -> "用户";
            case SEAT_2 -> "AI1";
            case SEAT_3 -> "AI2";
            case SEAT_4 -> "AI3";
        };
    }

    /** 跑得快牌型的中文展示名（表现层映射，不改动规则枚举）。 */
    private static String moveTypeLabel(PdkMoveType type) {
        if (type == null) {
            return "出牌";
        }
        return switch (type) {
            case SINGLE -> "单张";
            case PAIR -> "对子";
            case TRIPLE -> "三张";
            case TRIPLE_WITH_ONE -> "三带一";
            case TRIPLE_WITH_PAIR -> "三带二";
            case STRAIGHT -> "顺子";
            case CONSECUTIVE_PAIRS -> "连对";
            case TRIPLE_STRAIGHT -> "三顺";
            case AIRPLANE_WITH_WINGS -> "飞机";
            case FOUR_WITH_ONE -> "四带一";
            case FOUR_WITH_THREE -> "四带三";
            case FOUR_OF_A_KIND -> "💣 炸弹";
        };
    }
}
