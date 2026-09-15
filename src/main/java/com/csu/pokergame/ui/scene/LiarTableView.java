package com.csu.pokergame.ui.scene;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.CoinBar;
import com.cards.ui.component.GrowthResultPanel;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.effect.WinCelebration;
import com.csu.pokergame.player.Achievement;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerProfile;
import com.cards.ui.liar.LiarActionBar;
import com.cards.ui.liar.LiarClaimPanel;
import com.cards.ui.liar.LiarHandView;
import com.cards.ui.liar.LiarPlayerSeat;
import com.cards.ui.liar.LiarRiskIndicator;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
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
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.ui.AppShell;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * 骗子酒馆游戏桌（本地人机：SEAT_1 为玩家，SEAT_2/3/4 为机器人）。
 *
 * <p>阶段 10：用 deckapp-ui 的 Liar 组件（LiarTableView 容器 / LiarPlayerSeat /
 * LiarClaimPanel / LiarRiskIndicator / LiarHandView / LiarActionBar）替换原先
 * 简陋的 Label + CardView 实现，引擎交互逻辑（LiarEngine / LiarSnapshot /
 * DeclareLiarCards / bot 调度）保持不变。
 *
 * <p>座位映射（固定方位，上北下南、左西右东，与容器 {@code getSeat(index)} 一致）：
 * index 0 = SEAT_1（本人，南 · 底部），1 = SEAT_2（北家 · 顶部中央），
 * 2 = SEAT_3（西家 · 左侧中央），3 = SEAT_4（东家 · 右侧中央）。
 * 本人始终固定在南方（底部），不随座位 / 发牌变化。
 */
public final class LiarTableView extends BorderPane {

    private static final PlayerId LOCAL = PlayerId.SEAT_1;

    /** 入场费 / 胜负奖励（与跑得快同源，经 CoinService 写流水并落盘）。 */
    private static final int GAME_ENTRY_COST = 100;
    private static final int GAME_WIN_REWARD = 200;
    private static final int GAME_LOSS_REWARD = 50;
    private static final int LIAR_WIN_EXP = 50;
    private static final int LIAR_LOSS_EXP = 20;

    /**
     * 座位顺序与容器 index（也等于 {@link PlayerId#ordinal()}）一一对应，固定方位不可调换：
     * 0 = 南（本人，底部），1 = 北（顶部），2 = 西（左侧），3 = 东（右侧）。
     */
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

    /** 顶部金币栏（每帧与 CoinService 当前账号余额同步）。 */
    private final CoinBar liarCoinBar;

    /** 本局是否已结算（防止重复结算 / 重复叠加结算层）。 */
    private boolean settled;

    /** 已播放过分阶段演出的质疑结算（防止同一 resolution 重播）。 */
    private LiarResolution animatedResolution;
    /** 质疑结算演出是否进行中（期间锁定全部操作）。 */
    private boolean resolutionPlaying;
    /** Bot 回合调度锁：防止 refresh 多次触发重复 PauseTransition 叠加。 */
    private boolean botScheduled;
    /** 正在调度的 bot 是谁（允许不同 bot 同时排队）。 */
    private PlayerId botScheduledFor;

    // ----- 结算成长反馈字段 -----
    private boolean winForResult;
    private int goldDeltaForResult;
    private int expGainForResult;
    private PlayerGrowthService.LevelUpResult growthForResult;
    private final List<Achievement> achievementsForResult = new ArrayList<>();

    public LiarTableView(AppShell shell) {
        this.shell = shell;
        this.engine = new LiarEngine(new Random());
        this.engine.start();
        // 三个 AI：AI1 一直质疑，AI2 一直相信，AI3 50% 概率质疑
        this.bots = Map.of(
                PlayerId.SEAT_2, new RuleBotController(new LiarFixedPolicy(true)),
                PlayerId.SEAT_3, new RuleBotController(new LiarFixedPolicy(false)),
                PlayerId.SEAT_4, new RuleBotController(new LiarRandomPolicy(new Random())));

        this.liarCoinBar = liarView.getCoinBar();
        // 入场费：每局重新收取（与跑得快同源），扣完同步金币栏显示实时余额
        CoinService.getInstance().costGold(GAME_ENTRY_COST, "骗子酒馆入场");
        liarCoinBar.setCoins(CoinService.getInstance().getGold());

        // 座位名 / 等级：本人用真实等级，三家 AI 固定 8 级
        setupSeatProfiles();

        buildLayout();
        wireActions();
        refresh();
        GameAnimationService.getInstance().installButtonFeedback(this);
        // 进入骗子酒馆：切换为悬疑风格 BGM（音乐关闭时静默跳过）
        AudioService.getInstance().playMusic(AudioService.BGM_LIAR);
    }

    private void buildLayout() {
        // 背景层
        BackgroundManager.Background bg = BackgroundManager.createGameBackground();

        // 顶部：返回 + 设置
        Button back = new Button("← 返回模式选择");
        back.setOnAction(e -> shell.transitionTo("mode-choice", SceneTransition.Type.RETURN_LOBBY));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        HBox topBar = new HBox(12, back, settings);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 12, 8, 12));

        // 组装：背景 + 角落花色水印 + 顶栏 + 骗子酒馆容器
        StackPane root = new StackPane(bg.root());
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);
        root.getChildren().add(new BorderPane(topBar, liarView, null, null, null));
        setCenter(root);
    }

    /** 座位名 / 等级：本人用真实等级，三家 AI 固定 8 级（与 deckapp-ui 一致）。 */
    private void setupSeatProfiles() {
        int userLevel = PlayerManager.getInstance().getProfile().getLevel();
        // 与容器固定方位一致：0 南（本人）/ 1 北 / 2 西 / 3 东
        String[] names = {"你", "北家", "西家", "东家"};
        int[] levels = {userLevel, 8, 8, 8};
        for (int i = 0; i < SEAT_ORDER.length; i++) {
            LiarPlayerSeat seat = liarView.getSeat(i);
            if (seat == null) {
                continue;
            }
            seat.setPlayerName(names[i]);
            seat.setLevel(levels[i]);
        }
    }

    /** 在背景四角叠加花色水印（watermark=true 时加微模糊作为暗纹底）。 */
    private void addCornerSuit(StackPane root, String g, Color color, Pos corner, boolean watermark) {
        Label l = new Label(g);
        l.setTextFill(color);
        l.setFont(Font.font("Segoe UI Symbol", 150));
        l.getStyleClass().add("menu-corner");
        l.setMouseTransparent(true);
        if (watermark) {
            l.setEffect(new BoxBlur(4, 4, 3));
        }
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
        // 质疑结算演出期间禁止普通刷新抢画面（演出结束时会显式刷新一次）
        if (resolutionPlaying) {
            System.out.println("[LiarRefresh] SKIP — resolutionPlaying=true");
            return;
        }
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        System.out.println("[LiarRefresh] phase=" + snap.liarPhase()
                + " current=" + snap.currentPlayer()
                + " declarer=" + snap.declarer()
                + " winner=" + snap.winner().orElse(null)
                + " alive=" + snap.alivePlayers());

        // 金币栏：每帧与当前账号余额对齐（充值 / 商城 / 换账号后不残留旧数字）
        liarCoinBar.setCoins(CoinService.getInstance().getGold());

        // 顶部：阶段 / 存活 / 目标点数 + 入场费
        liarView.setPhaseText(phaseName(snap.liarPhase()));
        liarView.setAliveText(snap.alivePlayers().size());
        liarView.setTipText("目标点数：" + snap.targetRank().label() + " · 入场 " + GAME_ENTRY_COST);

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

        // 日志（时间顺序，PlayHistoryPanel 自动滚动到底部）
        liarView.setLogEntries(snap.publicEvents());

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
            // 左轮枪仓：已扣次数直观显示（子弹位置保密，中弹即淘汰，不再用心数折算）
            seat.setLife(-1);
            seat.setChambers(pulled);

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
        int eventsBefore = snap.publicEvents().size();
        engine.apply(new DeclareLiarCards(picked));
        selectedIndices.clear();
        liarView.getHandView().clearSelection();
        liarView.getClaimPanel().playClaimIn();
        AudioService.getInstance().playEffect(SoundEffect.CARD_PLAY);
        postCommand(eventsBefore);
    }

    private void trust() {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        int eventsBefore = snap.publicEvents().size();
        engine.apply(new TrustDeclaration());
        selectedIndices.clear();
        liarView.getHandView().clearSelection();
        AudioService.getInstance().playEffect(SoundEffect.BUTTON_CLICK);
        postCommand(eventsBefore);
    }

    private void challenge() {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        int eventsBefore = snap.publicEvents().size();
        engine.apply(new ChallengeDeclaration());
        selectedIndices.clear();
        liarView.getHandView().clearSelection();
        AudioService.getInstance().playEffect(SoundEffect.CHALLENGE_REVEAL);
        postCommand(eventsBefore);
    }

    /**
     * 指令落盘后的统一出口：若本次产生了新的质疑结算，走三阶段戏剧演出；
     * 否则立即刷新。演出期间锁定操作，结束后再由 {@link #refresh()} 推进机器人 / 结算。
     */
    private void postCommand(int eventsBefore) {
        LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(LOCAL);
        LiarResolution res = snap.lastResolution().orElse(null);
        if (res != null && res != animatedResolution) {
            animatedResolution = res;
            playResolutionSequence(snap, res, eventsBefore);
        } else {
            refresh();
        }
    }

    /**
     * 质疑结算三阶段演出：
     * <ol>
     *   <li>0ms — 「XX 选择质疑！」+ 红屏闪烁 / 声明卡震动</li>
     *   <li>950ms — 翻牌结果：质疑成功（拆穿谎言）/ 质疑失败（宣告属实）</li>
     *   <li>1900ms — 输方座位震动 + 枪响（中弹淘汰）/ 金属咔哒（空仓存活）</li>
     *   <li>3050ms — 收横幅，完整刷新（洗牌 / 胜负庆祝层随后出现）</li>
     * </ol>
     * 右侧历史同步逐阶段揭开，而不是一次性刷出全部结果。
     */
    private void playResolutionSequence(LiarSnapshot snap, LiarResolution res, int eventsBefore) {
        resolutionPlaying = true;
        liarView.getActionBar().setAllEnabled(false);
        liarView.getHandView().setInteractive(false);

        List<String> events = snap.publicEvents();
        String challenger = name(res.challenger());
        String shooter = name(res.shooter());
        int shooterIndex = res.shooter().ordinal();

        // —— 阶段 1：发起质疑 ——
        liarView.playChallengeEffect();
        liarView.showBanner(challenger + " 选择质疑！", "翻牌验证中…", "challenge");
        revealHistory(events, eventsBefore + 1);

        PauseTransition p1 = new PauseTransition(Duration.millis(950));
        p1.setOnFinished(e1 -> {
            if (!isSceneAlive()) {
                return;
            }
            // —— 阶段 2：翻牌结果 ——
            if (res.truthful()) {
                liarView.showBanner("质疑失败", "宣告属实，" + shooter + " 接受惩罚", "danger");
            } else {
                liarView.showBanner("质疑成功！", "拆穿谎言，" + shooter + " 接受惩罚", "success");
            }
            revealHistory(events, eventsBefore + 2);

            PauseTransition p2 = new PauseTransition(Duration.millis(950));
            p2.setOnFinished(e2 -> {
                if (!isSceneAlive()) {
                    return;
                }
                // —— 阶段 3：扣扳机 ——
                LiarPlayerSeat shooterSeat = liarView.getSeat(shooterIndex);
                if (shooterSeat != null) {
                    shooterSeat.playShake();
                }
                if (res.hit()) {
                    liarView.playChallengeEffect();
                    AudioService.getInstance().playEffect(SoundEffect.GUN_FIRE);
                    liarView.showBanner("💥 " + shooter + " 中弹！", "子弹上膛，淘汰出局", "gun-hit");
                } else {
                    AudioService.getInstance().playEffect(SoundEffect.GUN_CLICK);
                    liarView.showBanner("咔哒——空仓！", shooter + " 这一发逃过一劫", "gun-miss");
                }
                // 揭开扣扳机事件；若已分出胜负，同时揭开获胜行
                int reveal = eventsBefore + 3 + (snap.winner().isPresent() ? 1 : 0);
                revealHistory(events, reveal);

                PauseTransition p3 = new PauseTransition(Duration.millis(1150));
                p3.setOnFinished(e3 -> {
                    if (!isSceneAlive()) {
                        return;
                    }
                    // —— 阶段 4：收横幅，进入洗牌 / 结算 ——
                    liarView.hideBanner();
                    resolutionPlaying = false;
                    refresh();
                });
                p3.play();
            });
            p2.play();
        });
        p1.play();
    }

    /** 逐阶段揭开右侧历史（只显示事件列表的前 upTo 条）。 */
    private void revealHistory(List<String> events, int upTo) {
        int end = Math.min(Math.max(0, upTo), events.size());
        liarView.setLogEntries(events.subList(0, end));
    }

    /** 演出定时器回调时确认场景未被销毁（玩家可能中途返回大厅）。 */
    private boolean isSceneAlive() {
        return liarView.getScene() != null;
    }

    private void scheduleBotIfNeeded(LiarSnapshot snap) {
        if (snap.currentPlayer() == LOCAL) {
            return;
        }
        PlayerId bot = snap.currentPlayer();
        // 防止同一 bot 被重复调度
        if (botScheduled && botScheduledFor == bot) {
            System.out.println("[LiarBot] " + bot + " 已在定时器中，跳过");
            return;
        }
        botScheduled = true;
        botScheduledFor = bot;
        System.out.println("[LiarBot] 调度 " + bot + "（" + snap.liarPhase() + "）");

        PauseTransition pause = new PauseTransition(Duration.millis(3000));
        pause.setOnFinished(e -> {
            botScheduled = false;
            botScheduledFor = null;
            if (!isSceneAlive()) {
                return;
            }
            try {
                GameSnapshot botSnap = engine.snapshotFor(bot);
                List<GameCommand> legal = engine.legalCommands(bot);
                System.out.println("[LiarBot] " + bot + " legal=" + legal.size()
                        + " phase=" + botSnap.phase());
                if (legal.isEmpty()) {
                    System.out.println("[LiarBot] ⚠️ legal 空！刷新后退出");
                    refresh();
                    return;
                }
                int eventsBefore = snap.publicEvents().size();
                BotDecision decision = bots.get(bot).decide(botSnap, legal);
                System.out.println("[LiarBot] " + bot + " → " + decision.reason()
                        + " cmd=" + decision.command().getClass().getSimpleName());
                engine.apply(decision.command());
                if (decision.command() instanceof ChallengeDeclaration) {
                    AudioService.getInstance().playEffect(SoundEffect.CHALLENGE_REVEAL);
                } else if (decision.command() instanceof DeclareLiarCards) {
                    AudioService.getInstance().playEffect(SoundEffect.CARD_PLAY);
                }
                postCommand(eventsBefore);
            } catch (Throwable t) {
                System.err.println("[LiarBot] bot " + bot + " 异常: " + t.getMessage());
                t.printStackTrace();
                refresh();
            }
        });
        pause.play();
    }

    private void renderResult(LiarSnapshot snap) {
        if (settled) {
            return;
        }
        settled = true;

        boolean localWon = snap.winner().orElseThrow() == LOCAL;
        if (localWon) {
            liarView.playVictoryGlow();
        } else {
            liarView.playDefeatEffect();
        }

        // 金币结算：胜/负奖励 + 音效（与跑得快同源，唯一入口 CoinService）
        settleLiar(localWon);

        // 副标题：复用原结算原因文案
        String subtitle;
        if (snap.lastResolution().isPresent()) {
            LiarResolution res = snap.lastResolution().get();
            if (res.shooter() == LOCAL && res.killed()) {
                subtitle = "你扣扳机打中子弹仓，被淘汰";
            } else if (!localWon) {
                subtitle = name(snap.winner().orElseThrow()) + " 是最后存活者";
            } else {
                subtitle = "你是最后存活者";
            }
        } else {
            subtitle = localWon ? "你是最后存活者" : name(snap.winner().orElseThrow()) + " 是最后存活者";
        }

        // 终局庆祝层：胜利金色星光 / 失败灰化 + 成长反馈面板
        javafx.scene.Node growthPanel = buildGrowthPanel();

        final WinCelebration[] celebrationRef = new WinCelebration[1];

        Button again = new Button("重新开始");
        again.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-restart");
        again.setOnAction(e -> {
            celebrationRef[0].stop();
            shell.transitionTo("liar", SceneTransition.Type.ENTER_GAME);
        });
        Button back = new Button("返回游戏选择");
        back.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-back");
        back.setOnAction(e -> {
            celebrationRef[0].stop();
            shell.transitionTo("mode-choice", SceneTransition.Type.RETURN_LOBBY);
        });
        HBox buttons = new HBox(22, again, back);
        buttons.setAlignment(Pos.CENTER);

        celebrationRef[0] = new WinCelebration(localWon, subtitle, growthPanel, buttons);

        StackPane root = (StackPane) getCenter();
        root.getChildren().add(celebrationRef[0]);

        // 结算表现动画
        GameAnimationService anim = GameAnimationService.getInstance();
        if (localWon) {
            anim.playWinAnimation(root, celebrationRef[0]);
        } else {
            anim.playLoseAnimation(celebrationRef[0]);
        }
        boolean upgraded = growthForResult != null && growthForResult.upgraded();
        PauseTransition settleDelay = new PauseTransition(Duration.millis(140));
        settleDelay.setOnFinished(e -> {
            javafx.scene.Node coinTarget = liarCoinBar != null ? liarCoinBar : celebrationRef[0];
            anim.playCoinAnimation(growthPanel, coinTarget, null);
            if (upgraded) {
                anim.playLevelUpAnimation(growthPanel);
            }
            anim.showToast(celebrationRef[0], settleToastText(upgraded));
        });
        settleDelay.play();
    }

    /**
     * 本局结算：胜/负奖励经 {@link CoinService} 写入当前账号并落盘，并播放对应音效。
     */
    private void settleLiar(boolean win) {
        CoinService coinService = CoinService.getInstance();
        int goldDelta = win ? GAME_WIN_REWARD : GAME_LOSS_REWARD;
        int expGain = win ? LIAR_WIN_EXP : LIAR_LOSS_EXP;
        coinService.addGold(goldDelta, win ? "骗子酒馆胜利奖励" : "骗子酒馆参与奖励");

        // 加经验并自动升级
        PlayerGrowthService growthService = PlayerGrowthService.getInstance();
        growthForResult = growthService.addExp(expGain);

        // 战绩统计
        com.csu.pokergame.player.PlayerStatsService.getInstance().recordGameResult(win);

        // 战绩明细
        GameRecordService.getInstance().addRecord(
                "骗子酒馆", win, goldDelta, expGain, GameRecordService.OPPONENT_AI);

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

        liarCoinBar.setCoins(coinService.getGold());
        AudioService.getInstance()
                .playEffect(win ? SoundEffect.WIN : SoundEffect.LOSE);
    }

    /** 构建骗子酒馆结算成长反馈面板。 */
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

    /** 结算提示条文案。 */
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

    private static String phaseName(LiarPhase phase) {
        return switch (phase) {
            case DECLARE -> "宣告";
            case RESPOND -> "回应";
            case RESOLVE -> "结算";
            case FINISHED -> "结束";
        };
    }

    /**
     * 座位显示名：固定方位（上北下南、左西右东）。
     * SEAT_1 本人居南（底部），SEAT_2 北，SEAT_3 西，SEAT_4 东。
     */
    private static String name(PlayerId p) {
        return switch (p) {
            case SEAT_1 -> "你";
            case SEAT_2 -> "北家";
            case SEAT_3 -> "西家";
            case SEAT_4 -> "东家";
        };
    }
}
