package com.csu.pokergame.ui.scene;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.cards.ui.background.BackgroundManager;
import com.cards.ui.liar.LiarActionBar;
import com.cards.ui.liar.LiarClaimPanel;
import com.cards.ui.liar.LiarHandView;
import com.cards.ui.liar.LiarPlayerSeat;
import com.cards.ui.liar.LiarRiskIndicator;
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

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 骗子酒馆游戏桌（本地人机：SEAT_1 为玩家，SEAT_2/3/4 为机器人）。
 *
 * <p>阶段 10：用 deckapp-ui 的 Liar 组件（LiarTableView 容器 / LiarPlayerSeat /
 * LiarClaimPanel / LiarRiskIndicator / LiarHandView / LiarActionBar）替换原先
 * 简陋的 Label + CardView 实现，引擎交互逻辑（LiarEngine / LiarSnapshot /
 * DeclareLiarCards / bot 调度）保持不变。
 *
 * <p>座位映射（与容器 {@code getSeat(index)} 一致）：
 * index 0 = SEAT_1（本人，底部），1 = SEAT_2（AI1，顶部），
 * 2 = SEAT_3（AI2，左侧），3 = SEAT_4（AI3，右侧）。
 */
public final class LiarTableView extends BorderPane {

    private static final PlayerId LOCAL = PlayerId.SEAT_1;

    /** 座位顺序与容器 index 一一对应。 */
    private static final PlayerId[] SEAT_ORDER = {
            PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4
    };

    private final AppShell shell;
    private final LiarEngine engine;
    private final Map<PlayerId, RuleBotController> bots;

    /** 本人手牌选中下标集合（与 LiarHandView 内部选中态同步）。 */
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();

    /** deckapp-ui 骗子酒馆容器（包名同类名，用全限定名区分）。 */
    private final com.cards.ui.liar.LiarTableView liarView =
            new com.cards.ui.liar.LiarTableView();

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
        wireActions();
        refresh();
    }

    private void buildLayout() {
        // 背景层
        BackgroundManager.Background bg = BackgroundManager.createGameBackground();

        // 顶部：返回 + 设置
        Button back = new Button("返回");
        back.setOnAction(e -> shell.navigate("game-modes"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        HBox topBar = new HBox(12, back, settings);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 12, 8, 12));

        // 组装：背景 + 顶栏 + 骗子酒馆容器
        StackPane root = new StackPane(bg.root(), new BorderPane(topBar, liarView, null, null, null));
        setCenter(root);
    }

    private void wireActions() {
        LiarHandView hand = liarView.getHandView();
        hand.setMaxSelect(3);
        hand.setOnSelectionChange(sel -> {
            selectedIndices.clear();
            selectedIndices.addAll(sel);
            refreshActions();
        });

        LiarActionBar bar = liarView.getActionBar();
        bar.getDeclareButton().setOnAction(e -> declare());
        bar.getContinueButton().setOnAction(e -> trust());
        bar.getChallengeButton().setOnAction(e -> challenge());
    }

    private void refresh() {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);

        // 顶部：阶段 / 存活 / 目标点数
        liarView.setPhaseText(phaseName(snap.liarPhase()));
        liarView.setAliveText(snap.alivePlayers().size());
        liarView.setTipText("目标点数：" + snap.targetRank().label());

        // 四家座位：生命值 + 状态
        renderSeats(snap);

        // 中央声明卡 + 顶部紧凑声明
        renderClaim(snap);

        // 怀疑度（纯展示，按已宣告张数粗略估算）
        LiarRiskIndicator risk = liarView.getRiskIndicator();
        risk.setValue(snap.pendingDeclaredCount() > 0
                ? Math.min(1.0, snap.pendingDeclaredCount() / 3.0 * 0.7 + 0.1)
                : 0.0);

        // 本人手牌
        LiarHandView hand = liarView.getHandView();
        hand.setCards(snap.myHand());
        selectedIndices.removeIf(i -> i >= snap.myHand().size());
        hand.refreshSelection();

        // 日志（最新在上，与原实现一致）
        List<String> events = new ArrayList<>(snap.publicEvents());
        java.util.Collections.reverse(events);
        liarView.setLogEntries(events);

        if (snap.winner().isPresent()) {
            renderResult(snap);
            return;
        }

        refreshActions();
        scheduleBotIfNeeded(snap);
    }

    private void renderSeats(LiarSnapshot snap) {
        for (int i = 0; i < SEAT_ORDER.length; i++) {
            PlayerId p = SEAT_ORDER[i];
            LiarPlayerSeat seat = liarView.getSeat(i);
            if (seat == null) {
                continue;
            }
            seat.setPlayerName(name(p));

            GunState gun = snap.guns().get(p);
            int pulled = gun == null ? 0 : gun.shotsFired();
            // 生命值按「剩余机会」折算成 0~3 颗心
            seat.setLife(Math.max(0, 3 - pulled / 2));

            if (snap.eliminatedPlayers().contains(p)) {
                seat.setDead();
            } else if (snap.winner().isPresent()) {
                seat.setNormal();
                if (snap.winner().get() == p) {
                    seat.playWinBurst();
                }
            } else if (p == snap.currentPlayer()) {
                seat.setThinking();
            } else {
                seat.setNormal();
            }
        }
    }

    private void renderClaim(LiarSnapshot snap) {
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

    private void refreshActions() {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        LiarActionBar bar = liarView.getActionBar();
        LiarHandView hand = liarView.getHandView();

        boolean myTurn = snap.currentPlayer() == LOCAL;
        // 手牌只在「本人 · 宣告阶段」可点击选牌
        hand.setInteractive(myTurn && snap.liarPhase() == LiarPhase.DECLARE);

        if (!myTurn) {
            bar.setAllEnabled(false);
            return;
        }
        if (snap.liarPhase() == LiarPhase.DECLARE) {
            bar.setDeclareEnabled(!selectedCards(snap).isEmpty());
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

    /** 把选中的手牌下标映射为实际牌（顺序与快照手牌一致）。 */
    private List<Card> selectedCards(LiarSnapshot snap) {
        List<Card> hand = snap.myHand();
        List<Card> picked = new ArrayList<>();
        for (int i : selectedIndices) {
            if (i >= 0 && i < hand.size()) {
                picked.add(hand.get(i));
            }
        }
        return picked;
    }

    private void declare() {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        List<Card> picked = selectedCards(snap);
        if (picked.isEmpty() || picked.size() > 3) {
            return;
        }
        engine.apply(new DeclareLiarCards(picked));
        selectedIndices.clear();
        liarView.getHandView().clearSelection();
        liarView.getClaimPanel().playClaimIn();
        refresh();
    }

    private void trust() {
        engine.apply(new TrustDeclaration());
        selectedIndices.clear();
        liarView.getHandView().clearSelection();
        refresh();
    }

    private void challenge() {
        engine.apply(new ChallengeDeclaration());
        selectedIndices.clear();
        liarView.getHandView().clearSelection();
        liarView.playChallengeEffect();
        refresh();
    }

    private void scheduleBotIfNeeded(LiarSnapshot snap) {
        if (snap.currentPlayer() == LOCAL) {
            return;
        }
        PlayerId bot = snap.currentPlayer();
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            GameSnapshot botSnap = engine.snapshotFor(bot);
            List<GameCommand> legal = engine.legalCommands(bot);
            if (legal.isEmpty()) {
                refresh();
                return;
            }
            BotDecision decision = bots.get(bot).decide(botSnap, legal);
            engine.apply(decision.command());
            refresh();
        });
        pause.play();
    }

    private void renderResult(LiarSnapshot snap) {
        boolean localWon = snap.winner().orElseThrow() == LOCAL;
        if (localWon) {
            liarView.playVictoryGlow();
        } else {
            liarView.playDefeatEffect();
        }

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("result-overlay");
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("result-box");

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

        StackPane root = (StackPane) getCenter();
        root.getChildren().add(overlay);
    }

    private static String phaseName(LiarPhase phase) {
        return switch (phase) {
            case DECLARE -> "宣告";
            case RESPOND -> "回应";
            case RESOLVE -> "结算";
            case FINISHED -> "结束";
        };
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
