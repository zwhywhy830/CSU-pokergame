package com.csu.pokergame.ui.scene;

import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.CoinBar;
import com.cards.ui.component.GrowthResultPanel;
import com.cards.ui.component.PlayedCardsView;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.effect.WinCelebration;
import com.cards.ui.liar.LiarActionBar;
import com.cards.ui.liar.LiarClaimPanel;
import com.cards.ui.liar.LiarHandView;
import com.cards.ui.liar.LiarPlayerSeat;
import com.cards.ui.liar.LiarRiskIndicator;
import com.cards.ui.pdk.PdkHandView;
import com.cards.ui.pdk.PdkPlayerSeat;
import com.cards.ui.pdk.PdkTableHeader;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.GunState;
import com.csu.pokergame.liarspoker.LiarPhase;
import com.csu.pokergame.liarspoker.LiarResolution;
import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PdkSnapshot;
import com.csu.pokergame.paodekuai.PlayPdkCards;
import com.csu.pokergame.player.Achievement;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.player.PlayerStatsService;
import com.csu.pokergame.ui.AppShell;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 联机游戏桌：同时支持主机视角与客户端视角。
 * 根据游戏类型（跑得快/骗子酒馆）和 GameSnapshot 子类型分发渲染。
 * 使用与本地模式完全一致的 pdk/liar 组件 + 背景层 + 动画 + 结算。
 */
public final class LanGameTableView extends BorderPane {

    private static final int GAME_ENTRY_COST = 100;
    private static final int GAME_WIN_REWARD = 200;
    private static final int GAME_LOSS_REWARD = 50;
    private static final int WIN_EXP = 50;
    private static final int LOSS_EXP = 20;

    private final AppShell shell;
    private final LanHost host;
    private final LanClient client;
    private final GameType gameType;
    private PlayerId localSeat;
    private GameSnapshot lastClientSnapshot;
    private boolean settled = false;

    // ----- 结算成长反馈字段 -----
    private boolean winForResult;
    private int goldDeltaForResult;
    private int expGainForResult;
    private PlayerGrowthService.LevelUpResult growthForResult;
    private final List<Achievement> achievementsForResult = new ArrayList<>();

    // ----- 通用 UI 组件 -----
    private final Set<Card> selected = new LinkedHashSet<>();
    private final Set<Integer> selectedLiarIndices = new LinkedHashSet<>();
    private final Label statusText = new Label();
    private final ListView<String> log = new ListView<>();
    private CoinBar coinBar;

    // ----- PDK 组件 -----
    private PdkTableHeader pdkHeader;
    private PdkPlayerSeat pdkSeat2;
    private PdkPlayerSeat pdkSeat3;
    private PdkPlayerSeat pdkSeat4;
    private PlayedCardsView pdkTableCards;
    private PdkHandView pdkHandView;

    // ----- Liar 组件 -----
    private com.cards.ui.liar.LiarTableView liarView;
    private CoinBar liarCoinBar;

    // =====================================================================
    //  构造
    // =====================================================================

    public LanGameTableView(AppShell shell, LanHost host) {
        this.shell = shell;
        this.host = host;
        this.client = null;
        this.gameType = host.gameType();
        this.localSeat = PlayerId.SEAT_1;

        host.setOnCommand(pc -> Platform.runLater(() -> host.handleRemoteCommand(pc.seat(), pc.command())));
        host.setOnEnded(reason -> Platform.runLater(() -> showEnded(reason)));

        // 入场费
        CoinService.getInstance().costGold(GAME_ENTRY_COST, "联机入场");

        if (gameType == GameType.PAO_DE_KUAI) {
            buildPdkLayout();
        } else {
            buildLiarLayout();
        }

        GameAnimationService.getInstance().installButtonFeedback(this);

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

        // 入场费
        CoinService.getInstance().costGold(GAME_ENTRY_COST, "联机入场");

        if (gameType == GameType.PAO_DE_KUAI) {
            buildPdkLayout();
        } else {
            buildLiarLayout();
        }

        GameAnimationService.getInstance().installButtonFeedback(this);

        statusText.setText("等待主机广播快照...");
    }

    // =====================================================================
    //  PDK 布局
    // =====================================================================

    private void buildPdkLayout() {
        BackgroundManager.Background bg = BackgroundManager.createGameBackground();
        StackPane root = new StackPane();
        root.getChildren().add(bg.root());
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        BorderPane table = new BorderPane();
        table.getStyleClass().add("table-root");
        table.setPrefSize(960, 600);

        pdkHeader = new PdkTableHeader();
        coinBar = pdkHeader.getCoinBar();
        pdkHeader.setMode("跑得快 · 联机");
        pdkHeader.setBaseScore(GAME_ENTRY_COST);
        coinBar.setCoins(CoinService.getInstance().getGold());

        pdkSeat2 = new PdkPlayerSeat("2", "玩家2", 1, false);
        pdkSeat3 = new PdkPlayerSeat("3", "玩家3", 1, false);
        HBox seatStrip;
        if (gameType.requiredPlayers() >= 4) {
            pdkSeat4 = new PdkPlayerSeat("4", "玩家4", 1, false);
            pdkSeat4.setAlignment(Pos.CENTER);
            seatStrip = new HBox(60, pdkSeat2, pdkSeat3, pdkSeat4);
        } else {
            seatStrip = new HBox(60, pdkSeat2, pdkSeat3);
        }
        pdkSeat2.setAlignment(Pos.CENTER);
        pdkSeat3.setAlignment(Pos.CENTER);
        seatStrip.getStyleClass().add("pdk-seat-strip");
        seatStrip.setAlignment(Pos.CENTER);

        VBox topBar = new VBox(10, pdkHeader, seatStrip);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 14, 8, 14));
        table.setTop(topBar);

        pdkTableCards = new PlayedCardsView();
        statusText.getStyleClass().addAll("table-hint", "pdk-status-card");
        statusText.setMaxWidth(Double.MAX_VALUE);
        statusText.setAlignment(Pos.CENTER);
        statusText.setMinHeight(Region.USE_PREF_SIZE);
        statusText.setTextOverrun(OverrunStyle.ELLIPSIS);

        VBox centerCol = new VBox(14, pdkTableCards, statusText);
        centerCol.setAlignment(Pos.CENTER);
        StackPane centerWrap = new StackPane(centerCol);
        centerWrap.getStyleClass().addAll("table-center-wrap", "pdk-table");
        table.setCenter(centerWrap);

        pdkHandView = new PdkHandView(selected);
        table.setBottom(pdkHandView);

        log.getStyleClass().add("table-log");
        log.setPrefWidth(220);
        log.setMaxHeight(Double.MAX_VALUE);
        table.setRight(log);

        StackPane.setAlignment(table, Pos.CENTER);
        StackPane.setMargin(table, new Insets(28));
        root.getChildren().add(table);

        // 返回 + 设置
        Button back = new Button("← 离开对局");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> leaveTable());
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));
        root.getChildren().add(back);

        Button settings = new Button("⚙ 设置");
        settings.setOnAction(e -> shell.openSettings());
        StackPane.setAlignment(settings, Pos.TOP_RIGHT);
        StackPane.setMargin(settings, new Insets(22, 24, 0, 0));
        root.getChildren().add(settings);

        setCenter(root);

        // 绑定动作
        pdkHandView.getActionBar().getPlayButton().setOnAction(e -> pdkPlay());
        pdkHandView.getActionBar().getPassButton().setOnAction(e -> pdkPass());
        pdkHandView.setOnSelectionChange(s -> refreshPdkActions());
    }

    // =====================================================================
    //  Liar 布局
    // =====================================================================

    private void buildLiarLayout() {
        BackgroundManager.Background bg = BackgroundManager.createGameBackground();

        Button back = new Button("← 离开对局");
        back.setOnAction(e -> leaveTable());
        Button settings = new Button("⚙ 设置");
        settings.setOnAction(e -> shell.openSettings());
        HBox topBar = new HBox(12, back, settings);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 12, 8, 12));

        liarView = new com.cards.ui.liar.LiarTableView();
        liarCoinBar = liarView.getCoinBar();
        liarCoinBar.setCoins(CoinService.getInstance().getGold());

        setupLiarSeatProfiles();

        StackPane root = new StackPane(bg.root());
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);
        root.getChildren().add(new BorderPane(topBar, liarView, null, null, null));
        setCenter(root);

        // 绑定动作
        LiarHandView hand = liarView.getHandView();
        hand.setMaxSelect(3);
        hand.setOnSelectionChange(sel -> {
            selectedLiarIndices.clear();
            selectedLiarIndices.addAll(sel);
            refreshLiarActions();
        });

        LiarActionBar bar = liarView.getActionBar();
        bar.getDeclareButton().setOnAction(e -> liarDeclare());
        bar.getContinueButton().setOnAction(e -> liarTrust());
        bar.getChallengeButton().setOnAction(e -> liarChallenge());
    }

    private void setupLiarSeatProfiles() {
        int userLevel = PlayerManager.getInstance().getProfile().getLevel();
        String[] names = {"你", "西家", "北家", "东家"};
        int[] levels = {userLevel, 8, 8, 8};
        PlayerId[] seats = {PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4};
        for (int i = 0; i < seats.length; i++) {
            LiarPlayerSeat seat = liarView.getSeat(i);
            if (seat == null) continue;
            seat.setPlayerName(names[i]);
            seat.setLevel(levels[i]);
        }
    }

    // =====================================================================
    //  渲染分发
    // =====================================================================

    private void render(GameSnapshot snap) {
        if (snap instanceof PdkSnapshot p) {
            renderPdk(p);
        } else if (snap instanceof LiarSnapshot l) {
            renderLiar(l);
        }
    }

    private GameSnapshot currentSnapshot() {
        if (host != null) return host.localSnapshot();
        return lastClientSnapshot;
    }

    // =====================================================================
    //  PDK 渲染
    // =====================================================================

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
        statusText.setText(status);

        // 座位
        pdkSeat2.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_2));
        pdkSeat3.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_3));
        pdkSeat2.setState(snap.currentPlayer() == PlayerId.SEAT_2
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
        pdkSeat3.setState(snap.currentPlayer() == PlayerId.SEAT_3
                ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
        if (pdkSeat4 != null) {
            pdkSeat4.setCardCount(snap.remainingCardCounts().get(PlayerId.SEAT_4));
            pdkSeat4.setState(snap.currentPlayer() == PlayerId.SEAT_4
                    ? PdkPlayerSeat.State.THINKING : PdkPlayerSeat.State.WAITING);
        }

        // 桌面
        snap.lastMove().ifPresentOrElse(
                move -> pdkTableCards.setCards(move.cards(), null),
                () -> pdkTableCards.setCards(List.of(), null));

        // 日志
        log.getItems().setAll(snap.publicEvents());
        if (!snap.publicEvents().isEmpty()) {
            log.scrollTo(snap.publicEvents().size() - 1);
        }

        // 手牌
        pdkHandView.setCards(snap.myHand());
        pdkHandView.getSeat().setCardCount(snap.myHand().size());
        if (snap.myHand().size() == 1) {
            pdkHeader.setAuxText("报单！你只剩 1 张牌");
        } else {
            pdkHeader.setAuxText("");
        }

        if (snap.winner().isPresent()) {
            renderPdkResult(snap);
            return;
        }

        if (snap.currentPlayer() == localSeat) {
            refreshPdkActions();
        } else {
            pdkHandView.getActionBar().setPlayEnabled(false);
            pdkHandView.getActionBar().setPassEnabled(false);
            pdkHandView.setInteractive(false);
        }
    }

    private void refreshPdkActions() {
        PdkSnapshot snap = (PdkSnapshot) currentSnapshot();
        if (snap == null) return;
        // 客户端不能直接查 legalCommands（引擎在主机端），只按选中状态启用
        boolean myTurn = snap.currentPlayer() == localSeat && snap.winner().isEmpty();
        pdkHandView.getActionBar().setPlayEnabled(myTurn && !selected.isEmpty());
        pdkHandView.getActionBar().setPassEnabled(myTurn);
        pdkHandView.setInteractive(myTurn);
    }

    private void pdkPlay() {
        if (selected.isEmpty()) return;
        List<Card> played = List.copyOf(selected);
        GameCommand cmd = new PlayPdkCards(played);
        selected.clear();
        submitCommand(cmd);

        // 飞牌动画
        Platform.runLater(() -> GameAnimationService.getInstance()
                .playCardAnimation(() -> pdkTableCards.playFlyIn(played, "你", 0, 0, null), null));
    }

    private void pdkPass() {
        submitCommand(new PassPdkTurn());
        selected.clear();
    }

    // =====================================================================
    //  Liar 渲染
    // =====================================================================

    private void renderLiar(LiarSnapshot snap) {
        liarCoinBar.setCoins(CoinService.getInstance().getGold());

        liarView.setPhaseText(phaseName(snap.liarPhase()));
        liarView.setAliveText(snap.alivePlayers().size());
        liarView.setTipText("目标点数：" + snap.targetRank().label() + " · 入场 " + GAME_ENTRY_COST);

        renderLiarSeats(snap);
        renderLiarClaim(snap);

        LiarRiskIndicator risk = liarView.getRiskIndicator();
        risk.setValue(snap.pendingDeclaredCount() > 0
                ? Math.min(1.0, snap.pendingDeclaredCount() / 3.0 * 0.7 + 0.1) : 0.0);

        LiarHandView hand = liarView.getHandView();
        hand.setCards(snap.myHand());
        selectedLiarIndices.removeIf(i -> i >= snap.myHand().size());
        hand.refreshSelection();

        List<String> events = new ArrayList<>(snap.publicEvents());
        java.util.Collections.reverse(events);
        liarView.setLogEntries(events);

        if (snap.winner().isPresent()) {
            renderLiarResult(snap);
            return;
        }
        refreshLiarActions();
    }

    private void renderLiarSeats(LiarSnapshot snap) {
        PlayerId[] order = {PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4};
        for (int i = 0; i < order.length; i++) {
            PlayerId p = order[i];
            LiarPlayerSeat seat = liarView.getSeat(i);
            if (seat == null) continue;
            seat.setPlayerName(name(p));

            GunState gun = snap.guns().get(p);
            int pulled = gun == null ? 0 : gun.shotsFired();
            seat.setLife(Math.max(0, 3 - pulled / 2));

            if (snap.eliminatedPlayers().contains(p)) {
                seat.setDead();
            } else if (snap.winner().isPresent()) {
                seat.setNormal();
                if (snap.winner().get() == p) seat.playWinBurst();
            } else if (p == snap.currentPlayer()) {
                seat.setThinking();
            } else {
                seat.setNormal();
            }
        }
    }

    private void renderLiarClaim(LiarSnapshot snap) {
        LiarClaimPanel center = liarView.getClaimPanel();
        LiarClaimPanel header = liarView.getHeaderClaim();
        String declarerName = snap.declarer() == null ? "" : name(snap.declarer());
        String claimText = snap.declarer() == null
                ? "等待首位宣告者"
                : snap.pendingDeclaredCount() + " 张 " + snap.targetRank().label();
        center.setDeclarer(declarerName);
        center.setClaim(claimText);
        center.setCredibilityUnknown();
        header.setDeclarer(declarerName);
        header.setClaim(claimText);
        header.setCredibilityUnknown();
    }

    private void refreshLiarActions() {
        LiarSnapshot snap = (LiarSnapshot) currentSnapshot();
        if (snap == null) return;
        LiarActionBar bar = liarView.getActionBar();
        LiarHandView hand = liarView.getHandView();

        boolean myTurn = snap.currentPlayer() == localSeat;
        hand.setInteractive(myTurn && snap.liarPhase() == LiarPhase.DECLARE);

        if (!myTurn) {
            bar.setAllEnabled(false);
            return;
        }
        if (snap.liarPhase() == LiarPhase.DECLARE) {
            bar.setDeclareEnabled(!selectedLiarCards(snap).isEmpty());
            bar.setContinueEnabled(false);
            bar.setChallengeEnabled(false);
        } else if (snap.liarPhase() == LiarPhase.RESPOND) {
            bar.setDeclareEnabled(false);
            bar.setContinueEnabled(true);
            bar.setChallengeEnabled(true);
        } else {
            bar.setAllEnabled(false);
        }
    }

    private List<Card> selectedLiarCards(LiarSnapshot snap) {
        List<Card> hand = snap.myHand();
        List<Card> picked = new ArrayList<>();
        for (int i : selectedLiarIndices) {
            if (i >= 0 && i < hand.size()) picked.add(hand.get(i));
        }
        return picked;
    }

    private void liarDeclare() {
        LiarSnapshot snap = (LiarSnapshot) currentSnapshot();
        if (snap == null) return;
        List<Card> picked = selectedLiarCards(snap);
        if (picked.isEmpty() || picked.size() > 3) return;
        submitCommand(new DeclareLiarCards(picked));
        selectedLiarIndices.clear();
        liarView.getHandView().clearSelection();
        liarView.getClaimPanel().playClaimIn();
    }

    private void liarTrust() {
        submitCommand(new TrustDeclaration());
        selectedLiarIndices.clear();
        liarView.getHandView().clearSelection();
    }

    private void liarChallenge() {
        submitCommand(new ChallengeDeclaration());
        selectedLiarIndices.clear();
        liarView.getHandView().clearSelection();
        liarView.playChallengeEffect();
    }

    // =====================================================================
    //  结算
    // =====================================================================

    private void renderPdkResult(PdkSnapshot snap) {
        if (settled) return;
        settled = true;

        boolean localWon = snap.winner().orElseThrow() == localSeat;
        settleGame(localWon, "跑得快联机");
        AudioService.getInstance().playEffect(localWon ? SoundEffect.WIN : SoundEffect.LOSE);

        StringBuilder subtitle = new StringBuilder();
        for (PlayerId p : List.of(PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4)) {
            if (snap.winner().orElseThrow() == p) continue;
            Integer remaining = snap.remainingCardCounts().get(p);
            if (remaining == null) continue;
            subtitle.append(name(p)).append(" 剩 ").append(remaining).append(" 张    ");
        }

        showCelebration(localWon, subtitle.toString().trim());
    }

    private void renderLiarResult(LiarSnapshot snap) {
        if (settled) return;
        settled = true;

        boolean localWon = snap.winner().orElseThrow() == localSeat;
        if (localWon) liarView.playVictoryGlow();
        else liarView.playDefeatEffect();

        settleGame(localWon, "骗子酒馆联机");
        AudioService.getInstance().playEffect(localWon ? SoundEffect.WIN : SoundEffect.LOSE);

        String subtitle;
        if (snap.lastResolution().isPresent()) {
            LiarResolution res = snap.lastResolution().get();
            if (res.shooter() == localSeat && res.killed()) {
                subtitle = "你扣扳机打中子弹仓，被淘汰";
            } else if (!localWon) {
                subtitle = name(snap.winner().orElseThrow()) + " 是最后存活者";
            } else {
                subtitle = "你是最后存活者";
            }
        } else {
            subtitle = localWon ? "你是最后存活者" : name(snap.winner().orElseThrow()) + " 是最后存活者";
        }

        showCelebration(localWon, subtitle);
    }

    private void settleGame(boolean win, String reason) {
        int goldDelta = win ? GAME_WIN_REWARD : GAME_LOSS_REWARD;
        int expGain = win ? WIN_EXP : LOSS_EXP;
        CoinService.getInstance().addGold(goldDelta, reason + (win ? "胜利" : "参与") + "奖励");

        PlayerGrowthService growthService = PlayerGrowthService.getInstance();
        growthForResult = growthService.addExp(expGain);
        PlayerStatsService.getInstance().recordGameResult(win);
        GameRecordService.getInstance().addRecord(
                reason, win, goldDelta, expGain, "LAN");

        if (win) achievementsForResult.addAll(AchievementService.getInstance().checkOnWin());
        achievementsForResult.addAll(AchievementService.getInstance().checkOnGoldChange());
        achievementsForResult.addAll(AchievementService.getInstance().checkOnLevelUp());

        winForResult = win;
        goldDeltaForResult = goldDelta;
        expGainForResult = expGain;

        if (coinBar != null) coinBar.setCoins(CoinService.getInstance().getGold());
        if (liarCoinBar != null) liarCoinBar.setCoins(CoinService.getInstance().getGold());
    }

    private void showCelebration(boolean localWon, String subtitle) {
        StackPane root = (StackPane) getCenter();
        javafx.scene.Node growthPanel = buildGrowthPanel();
        WinCelebration celebration = new WinCelebration(localWon, subtitle, growthPanel);

        Button again = new Button("重新开始");
        again.getStyleClass().add("primary");
        again.setOnAction(e -> {
            celebration.stop();
            root.getChildren().remove(celebration);
            leaveTable();
        });
        Button back = new Button("返回大厅");
        back.setOnAction(e -> {
            celebration.stop();
            root.getChildren().remove(celebration);
            leaveTable();
        });
        HBox btnRow = new HBox(22, again, back);
        btnRow.setAlignment(Pos.CENTER);
        StackPane.setAlignment(btnRow, Pos.BOTTOM_CENTER);
        StackPane.setMargin(btnRow, new Insets(0, 0, 60, 0));
        celebration.getChildren().add(btnRow);

        root.getChildren().add(celebration);

        GameAnimationService anim = GameAnimationService.getInstance();
        if (localWon) anim.playWinAnimation(root, celebration);
        else anim.playLoseAnimation(celebration);

        boolean upgraded = growthForResult != null && growthForResult.upgraded();
        PauseTransition delay = new PauseTransition(Duration.millis(140));
        delay.setOnFinished(e -> {
            javafx.scene.Node coinTarget = coinBar != null ? coinBar
                    : (liarCoinBar != null ? liarCoinBar : celebration);
            anim.playCoinAnimation(growthPanel, coinTarget, null);
            if (upgraded) anim.playLevelUpAnimation(growthPanel);
            anim.showToast(celebration, settleToastText(upgraded));
        });
        delay.play();
    }

    private javafx.scene.Node buildGrowthPanel() {
        PlayerProfile profile = PlayerManager.getInstance().getProfile();
        PlayerGrowthService growthService = PlayerGrowthService.getInstance();
        int level = profile.getLevel();
        GrowthResultPanel panel = new GrowthResultPanel(
                winForResult, goldDeltaForResult, expGainForResult, level,
                growthService.getLevelTitle(level), growthService.getLevelBadge(level),
                profile.getExp(), growthService.expToNextLevel(level), growthForResult);
        if (achievementsForResult.isEmpty()) return panel;
        VBox box = new VBox(10, panel, buildAchievementNotice());
        box.setAlignment(Pos.CENTER);
        return box;
    }

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

    // =====================================================================
    //  通用工具
    // =====================================================================

    private void submitCommand(GameCommand cmd) {
        if (host != null) {
            host.submitLocalCommand(cmd);
        } else if (client != null) {
            client.submitCommand(cmd);
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
                        && snap.winner().get() == localSeat) yield "你赢了!";
                yield "对局结束";
            }
            case "PLAYER_DISCONNECTED" -> "玩家连接已断开，本局结束";
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
        if (host != null) host.shutdown();
        if (client != null) client.shutdown();
        shell.transitionTo("lan-lobby", SceneTransition.Type.RETURN_LOBBY);
    }

    private void addCornerSuit(StackPane root, String g, Color color, Pos corner, boolean watermark) {
        Label l = new Label(g);
        l.setTextFill(color);
        l.setFont(Font.font("Segoe UI Symbol", 150));
        l.getStyleClass().add("menu-corner");
        l.setMouseTransparent(true);
        if (watermark) l.setEffect(new BoxBlur(4, 4, 3));
        StackPane.setAlignment(l, corner);
        Insets m = switch (corner) {
            case TOP_LEFT -> new Insets(8, 0, 0, 26);
            case TOP_RIGHT -> new Insets(8, 26, 0, 0);
            case BOTTOM_LEFT -> new Insets(0, 0, 10, 26);
            default -> new Insets(0, 26, 10, 0);
        };
        StackPane.setMargin(l, m);
        root.getChildren().add(l);
    }

    private String name(PlayerId p) {
        String base = switch (p) {
            case SEAT_1 -> "玩家1";
            case SEAT_2 -> "玩家2";
            case SEAT_3 -> "玩家3";
            case SEAT_4 -> "玩家4";
        };
        if (host != null && host.botSeats().contains(p)) return base + " [AI]";
        return base;
    }

    private static String phaseName(LiarPhase phase) {
        return switch (phase) {
            case DECLARE -> "宣告";
            case RESPOND -> "回应";
            case RESOLVE -> "结算";
            case FINISHED -> "结束";
        };
    }
}
