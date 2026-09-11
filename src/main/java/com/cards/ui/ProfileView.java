package com.cards.ui;

import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.theme.Theme;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.Achievement;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.CoinLog;
import com.csu.pokergame.player.CoinLogService;
import com.csu.pokergame.player.CoinRechargeService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.GameRecord;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.InventoryService;
import com.csu.pokergame.player.Item;
import com.csu.pokergame.player.ItemUseService;
import com.csu.pokergame.player.LeaderboardEntry;
import com.csu.pokergame.player.LeaderboardService;
import com.csu.pokergame.player.PlayerAccount;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.player.PlayerStatistics;
import com.csu.pokergame.player.PlayerStatsService;
import com.csu.pokergame.player.ShopItem;
import com.csu.pokergame.player.ShopService;
import com.csu.pokergame.player.StatisticsService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家个人中心页面（阶段 10）：一屏展示玩家的身份、成长、战绩与资产。
 *
 * <p>内容分区：
 * <ul>
 *   <li><b>身份</b>：头像、昵称、等级、等级称号（徽章 + 青铜 / 白银 / 黄金 / 大师）；</li>
 *   <li><b>成长</b>：经验进度条 + {@code "当前 / 升级需求"}、金币余额；</li>
 *   <li><b>战绩</b>：总场次、胜利、失败、胜率、最高连胜（汇总）；</li>
 *   <li><b>最近战绩</b>（阶段 17）：最近 20 局的玩法 / 胜负 / 金币与经验变动 / 时间，
 *       数据来自 {@link GameRecordService}，账号之间完全隔离；</li>
 *   <li><b>资产</b>：累计获得金币、累计消耗金币、当前金币；</li>
 *   <li><b>数据中心</b>（阶段 19）：总场次 / 胜率 / 连胜 / 累计收支 / 最常游戏，
 *       数据来自 {@link StatisticsService}（实时汇总，不落盘）；</li>
 *   <li><b>成就</b>：解锁进度 + 最近获得成就 + 已解锁 🏆 / 未解锁 🔒 列表
 *       （数据来自 {@link AchievementService}）。</li>
 * </ul>
 *
 * <p><b>数据来源（禁止复制数据）：</b>本页面不持有任何玩家数据的副本，
 * 所有数值都在 {@link #refresh()} 里向四个服务实时读取：
 * <ul>
 *   <li>{@link PlayerManager}：昵称 / 头像 / 等级 / 经验；</li>
 *   <li>{@link PlayerGrowthService}：升级所需经验、等级称号与徽章；</li>
 *   <li>{@link CoinService}：金币余额、累计获得 / 累计消耗；</li>
 *   <li>{@link PlayerStatsService}：总场次 / 胜负 / 胜率 / 连胜（汇总口径）；</li>
 *   <li>{@link GameRecordService}：最近战绩逐局明细（阶段 17，账号级隔离）。</li>
 * </ul>
 * 因此牌局结算改动的任何数据，重新进入本页（或调用 {@link #refresh()}）都会立刻反映出来。
 *
 * <p>页面自带背景与粒子层，可直接作为 {@code Scene} 的根节点使用；「返回」行为由构造时传入的
 * {@code onBack} 决定（本项目里回到首页）。
 */
public final class ProfileView extends StackPane {

    private final PlayerManager players;
    private final CoinService coins;
    private final PlayerGrowthService growth;
    private final PlayerStatsService stats;
    private final AchievementService achievementService;
    /** 模拟充值服务（阶段 12）：充值区三个按钮统一走它。 */
    private final CoinRechargeService rechargeService;
    /** 金币流水服务（阶段 12-2）：只读 coin_log.json，供「金币流水」区域展示。 */
    private final CoinLogService coinLogService;
    /** 背包服务（阶段 13）：我的资产区域的数据来源。 */
    private final InventoryService inventoryService;
    /** 商城服务（阶段 14）：金币购买道具的唯一入口。 */
    private final ShopService shopService;
    /** 道具使用服务（阶段 15）：背包道具「使用 / 开启」的唯一入口。 */
    private final ItemUseService itemUseService;
    /**
     * 战绩明细服务（阶段 17）：每局一条对局记录（玩法 / 胜负 / 金币 / 经验 / 时间）。
     * 与 {@link #stats} 的分工：{@code stats} 给"汇总数字"（总场次 / 胜负 / 胜率 / 连胜），
     * 本服务给"逐局明细"，两者在同一个结算点写入，因此数字始终对得上。
     */
    private final GameRecordService gameRecordService;
    /** 账号服务（阶段 16）：只读当前登录账号，用于身份区展示「账号」。 */
    private final AccountService accountService;
    /** 「切换账号」动作（由 DeckApp 注入：保存进度并回到登录页）。 */
    private final Runnable onSwitchAccount;
    /** 「退出登录」动作（由 DeckApp 注入：登出并回到登录页）。 */
    private final Runnable onLogout;

    // ---------------- 身份 / 成长 ----------------
    private final Text avatarGlyph = new Text();
    private final Label nickLabel = new Label();
    /** 身份区的「账号：xxx」（阶段 16，数据来自 AccountService 当前账号）。 */
    private final Label accountValueLabel = new Label();
    private final Text badgeText = new Text();
    private final Label titleLabel = new Label();
    private final Label levelLabel = new Label();
    private final ProgressBar expBar = new ProgressBar(0);
    private final Label expValueLabel = new Label();
    private final Label goldValueLabel = new Label();
    /** 充值区当前金币（与上方金币同源，实时读 CoinService）。 */
    private final Label rechargeGoldLabel = new Label();
    /** 金币区累计获得（来源 PlayerProfile.totalGoldEarned）。 */
    private final Label panelEarnedLabel = new Label();
    /** 金币区累计消耗（来源 PlayerProfile.totalGoldSpent）。 */
    private final Label panelSpentLabel = new Label();

    // ---------------- 当前效果状态（阶段 15） ----------------
    /** 幸运状态徽标（luckyCount > 0 时显示）。 */
    private final Label luckyStatusLabel = new Label();
    /** 双倍经验状态徽标（doubleExpCount > 0 时显示）。 */
    private final Label doubleExpStatusLabel = new Label();
    /** 状态整行容器：没有任何状态时整行隐藏（不占位）。 */
    private final HBox statusRow = new HBox(8);

    // ---------------- 金币流水（阶段 12-2） ----------------
    /** 流水列表容器：每次 {@link #refresh()} 按「最近 10 条、最新在前」重建。 */
    private final VBox coinLogList = new VBox(4);

    // ---------------- 我的资产（背包，阶段 13） ----------------
    /** 资产列表容器：每次 {@link #refresh()} 按背包服务实时重建。 */
    private final VBox inventoryList = new VBox(4);

    // ---------------- 金币商城（阶段 14） ----------------
    /** 商城列表容器：每次 {@link #refresh()} 按商城服务实时重建。 */
    private final VBox shopList = new VBox(6);

    // ---------------- 战绩 ----------------
    private final Label totalGamesValue = new Label();
    private final Label winGamesValue = new Label();
    private final Label loseGamesValue = new Label();
    private final Label winRateValue = new Label();
    private final Label streakValue = new Label();

    // ---------------- 最近战绩（阶段 17） ----------------
    /** 明细口径的小结（共 N 场 / 胜 X / 负 Y / 胜率 Z%），没有任何记录时整行隐藏。 */
    private final Label recordSummaryLabel = new Label();
    /** 最近战绩列表容器：每次 {@link #refresh()} 按「最近 20 条、最新在前」重建。 */
    private final VBox recordList = new VBox(4);

    // ---------------- 排行榜（阶段 18） ----------------
    /**
     * 排行榜服务（阶段 18）：读全部账号存档生成榜单。
     * 它只用临时 {@code PlayerManager} 读别人的档，不会切换当前登录玩家。
     */
    private final LeaderboardService leaderboardService;
    /** 「我的排名」：金币榜名次（顶部身份卡内，未上榜显示"暂无排名"）。 */
    private final Label myGoldRankLabel = new Label();
    /** 「我的排名」：等级榜名次。 */
    private final Label myLevelRankLabel = new Label();
    /** 「我的排名」：胜率榜名次（局数不足时显示"暂无排名"）。 */
    private final Label myWinRateRankLabel = new Label();
    /** 当前选中的榜单（默认金币榜），由榜单切换按钮改变。 */
    private LeaderboardService.Board rankingBoard = LeaderboardService.Board.GOLD;
    /** 榜单切换按钮，刷新时同步选中态。 */
    private final EnumMap<LeaderboardService.Board, Button> rankingBoardButtons =
            new EnumMap<>(LeaderboardService.Board.class);
    /** 排行榜列表容器：每次 {@link #refresh()} 按当前榜单重建。 */
    private final VBox rankingList = new VBox(4);

    // ---------------- 数据中心（阶段 19） ----------------
    /**
     * 玩家数据统计中心服务（阶段 19）：从战绩明细 + 金币累计实时汇总，
     * <b>不落盘</b>（不污染 {@code player.json}），因此本页只读取、不写回。
     */
    private final StatisticsService statisticsService;
    /** 数据中心行容器：每次 {@link #refresh()} 按实时统计重建。 */
    private final VBox statisticsList = new VBox(4);

    // ---------------- 资产 ----------------
    private final Label earnedValue = new Label();
    private final Label spentValue = new Label();
    private final Label balanceValue = new Label();

    // ---------------- 成就 ----------------
    /** 成就进度（已解锁 / 总数）。 */
    private final Label achievementProgress = new Label();
    /** 最近获得的成就（阶段 12）。 */
    private final Label latestAchievementLabel = new Label();
    /** 成就列表容器：每次 {@link #refresh()} 按服务里的定义顺序重建。 */
    private final VBox achievementList = new VBox(6);

    /**
     * @param players 玩家档案（昵称 / 头像 / 等级 / 经验）
     * @param coins   金币服务（余额与累计收支）
     * @param growth  成长服务（升级需求 / 等级称号）
     * @param stats   战绩统计服务
     * @param coinLogService 金币流水查询服务（最近 10 条）
     * @param shopService 商城服务（阶段 14：金币购买道具）
     * @param itemUseService 道具使用服务（阶段 15：背包道具使用 / 开启）
     * @param gameRecordService 战绩明细服务（阶段 17：最近战绩列表）
     * @param leaderboardService 排行榜服务（阶段 18：全服榜单 + 我的排名）
     * @param statisticsService 数据统计中心服务（阶段 19：数据中心展示）
     * @param accountService 账号服务（阶段 16：只读当前账号用于身份区展示）
     * @param onSwitchAccount 点击「切换账号」的动作
     * @param onLogout 点击「退出登录」的动作
     * @param onBack  点击「返回主页」的动作
     */
    public ProfileView(PlayerManager players, CoinService coins, PlayerGrowthService growth,
                       PlayerStatsService stats, AchievementService achievementService,
                       CoinRechargeService rechargeService, CoinLogService coinLogService,
                       InventoryService inventoryService, ShopService shopService,
                       ItemUseService itemUseService, GameRecordService gameRecordService,
                       LeaderboardService leaderboardService,
                       StatisticsService statisticsService,
                       AccountService accountService,
                       Runnable onSwitchAccount, Runnable onLogout, Runnable onBack) {
        this.players = players;
        this.coins = coins;
        this.growth = growth;
        this.stats = stats;
        this.achievementService = achievementService;
        this.rechargeService = rechargeService;
        this.coinLogService = coinLogService;
        this.inventoryService = inventoryService;
        this.shopService = shopService;
        this.itemUseService = itemUseService;
        this.gameRecordService = gameRecordService;
        this.leaderboardService = leaderboardService;
        this.statisticsService = statisticsService;
        this.accountService = accountService;
        this.onSwitchAccount = onSwitchAccount;
        this.onLogout = onLogout;

        getStyleClass().add("profile-view");

        // ---------------- 背景：与首页/大厅同源的山水美术层 + 粒子 ----------------
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();
        addCornerSuit("♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT);
        addCornerSuit("♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT);
        addCornerSuit("♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT);
        addCornerSuit("♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT);
        // 本页作为 Scene 根节点常驻，用自身 scene 属性即可控制粒子播放
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                particles.play();
            } else {
                particles.stop();
            }
        });

        // ---------------- 中央内容 ----------------
        Label title = new Label("个人中心");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("你的成长、战绩与资产");
        sub.getStyleClass().add("game-choice-sub");

        VBox center = new VBox(18,
                title, sub,
                buildHeadCard(),
                buildRechargeSection(),
                buildInventorySection(),
                buildShopSection(),
                buildCoinLogSection(),
                section("战绩统计"),
                new HBox(12,
                        statBlock("🎮 总场次", totalGamesValue, null),
                        statBlock("🏆 胜利", winGamesValue, "player-stat-win"),
                        statBlock("❌ 失败", loseGamesValue, "player-stat-lose"),
                        statBlock("胜率", winRateValue, "player-stat-rate"),
                        statBlock("最高连胜", streakValue, "player-stat-streak")),
                // 阶段 17：逐局明细（最近 20 条，最新在前），紧跟在战绩统计下方
                buildRecordSection(),
                // 阶段 18：全服排行榜（紧随战绩之后）
                buildLeaderboardSection(),
                // 阶段 19：数据中心（排行榜下面的实时统计）
                buildStatisticsSection(),
                section("资产"),
                new HBox(12,
                        statBlock("累计获得金币", earnedValue, "player-stat-gold"),
                        statBlock("累计消耗金币", spentValue, "player-stat-lose"),
                        statBlock("当前金币", balanceValue, "player-stat-gold")),
                section("成就"),
                achievementProgress,
                latestAchievementLabel,
                achievementList);
        achievementList.getStyleClass().add("player-achievement-list");
        latestAchievementLabel.getStyleClass().add("player-achievement-latest");
        center.getStyleClass().add("player-center");
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setAlignment(center, Pos.CENTER);

        // ---------------- 左上角返回 ----------------
        Button back = new Button("← 返回主页");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        // 内容变高（成就列表）后可能超出窗口高度，用透明滚动容器承载，保证所有分区块都可访问
        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("player-scroll");
        getChildren().addAll(scroll, back);

        refresh();
    }

    // ============================================================= 刷新

    /**
     * 从各服务实时读取并刷新全部展示数值。进入页面时调用一次；牌局结算后再次进入会重新读取，
     * 所以战绩 / 金币 / 等级永远是最新值。
     */
    public void refresh() {
        PlayerProfile profile = players.getProfile();
        int level = profile.getLevel();

        // ---- 身份 ----
        String avatar = profile.getAvatar();
        avatarGlyph.setText(avatar);
        avatarGlyph.setFont(avatarGlyphFont(46, avatar));
        nickLabel.setText(profile.getName());
        badgeText.setText(growth.getLevelBadge(level));
        titleLabel.setText(growth.getLevelTitle(level));
        levelLabel.setText("Lv." + level);

        // ---- 账号（阶段 16：读当前登录账号，未登录时兜底提示）----
        PlayerAccount account = accountService == null ? null : accountService.getCurrentAccount();
        accountValueLabel.setText(account == null ? "未登录" : account.getUsername());

        // ---- 成长 ----
        int need = Math.max(1, growth.expToNextLevel(level));
        int exp = Math.max(0, profile.getExp());
        expBar.setProgress(Math.min(1.0, (double) exp / need));
        expValueLabel.setText(exp + " / " + need);
        goldValueLabel.setText(String.valueOf(coins.getGold()));
        rechargeGoldLabel.setText(String.valueOf(coins.getGold()));

        // ---- 当前效果状态（阶段 15：使用道具累积的状态层数）----
        refreshStatus(profile);

        // ---- 战绩（一次性快照，避免多次读数不一致）----
        PlayerStatsService.StatsSnapshot snapshot = stats.snapshot();
        totalGamesValue.setText(String.valueOf(snapshot.totalGames()));
        winGamesValue.setText(String.valueOf(snapshot.winGames()));
        loseGamesValue.setText(String.valueOf(snapshot.loseGames()));
        winRateValue.setText(snapshot.winRatePercent() + "%");
        streakValue.setText(String.valueOf(snapshot.maxWinStreak()));

        // ---- 资产（累计收支由 CoinService 维护，余额同样取当前值）----
        earnedValue.setText(String.valueOf(coins.getTotalEarned()));
        spentValue.setText(String.valueOf(coins.getTotalSpent()));
        balanceValue.setText(String.valueOf(coins.getGold()));
        // 金币区内的累计统计（与上方资产区同源，都是 PlayerProfile 的累计字段）
        panelEarnedLabel.setText("+" + coins.getTotalEarned());
        panelSpentLabel.setText("-" + coins.getTotalSpent());

        // ---- 金币流水（阶段 12-2：最近 10 条，最新在前）----
        refreshCoinLog();

        // ---- 最近战绩（阶段 17：最近 20 条，最新在前）----
        refreshRecords();

        // ---- 排行榜（阶段 18：我的排名 + 当前榜单）----
        refreshMyRank();
        refreshLeaderboard();

        // ---- 数据中心（阶段 19：实时汇总战绩 + 金币）----
        refreshStatistics();

        // ---- 我的资产（阶段 13：背包物品与数量）----
        refreshInventory();

        // ---- 金币商城（阶段 14：货架商品）----
        refreshShop();

        // ---- 成就（阶段 11：已解锁 🏆 / 未解锁 🔒）----
        refreshAchievements();

        // 阶段 22：为（含动态重建的）按钮安装点击缩放反馈；幂等，可重复调用
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    /**
     * 刷新身份卡的「当前效果」状态行（阶段 15）：
     * 显示 🍀 幸运状态与 ⭐ 双倍经验的状态层数，两者都为 0 时整行隐藏。
     */
    private void refreshStatus(PlayerProfile profile) {
        int lucky = profile.getLuckyCount();
        int doubleExp = profile.getDoubleExpCount();
        boolean hasLucky = lucky > 0;
        boolean hasExp = doubleExp > 0;

        luckyStatusLabel.setText("🍀 幸运状态 x" + lucky);
        doubleExpStatusLabel.setText("⭐ 双倍经验 x" + doubleExp);
        luckyStatusLabel.setVisible(hasLucky);
        luckyStatusLabel.setManaged(hasLucky);
        doubleExpStatusLabel.setVisible(hasExp);
        doubleExpStatusLabel.setManaged(hasExp);

        boolean any = hasLucky || hasExp;
        statusRow.setVisible(any);
        statusRow.setManaged(any);
    }

    // ============================================================= 构建

    /** 顶部身份卡：大号圆头像 + 昵称 / 称号等级 + 经验进度条 + 金币。 */
    private HBox buildHeadCard() {
        StackPane avatarCircle = new StackPane();
        avatarCircle.getStyleClass().addAll("user-avatar", "user-avatar-lg");
        avatarGlyph.getStyleClass().add("avatar-glyph");
        avatarGlyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.GOLD_LIGHT),
                new Stop(0.55, Theme.GOLD),
                new Stop(1, Theme.GOLD_DARK)));
        avatarCircle.getChildren().add(avatarGlyph);

        nickLabel.getStyleClass().add("player-nick");

        badgeText.getStyleClass().add("player-badge");
        badgeText.setFont(Font.font("Segoe UI Emoji", FontWeight.NORMAL, 14));
        titleLabel.getStyleClass().add("player-title");
        levelLabel.getStyleClass().add("player-level");
        HBox nameRow = new HBox(8, nickLabel, badgeText, titleLabel, levelLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        expBar.getStyleClass().add("player-exp-bar");
        expBar.setPrefWidth(240);
        Label expKey = metricKey("经验");
        expValueLabel.getStyleClass().add("player-metric-value");
        HBox expRow = new HBox(8, expKey, expBar, expValueLabel);
        expRow.setAlignment(Pos.CENTER_LEFT);

        Label goldKey = metricKey("金币");
        goldValueLabel.getStyleClass().addAll("player-metric-value", "player-gold-value");
        HBox goldRow = new HBox(8, goldKey, goldValueLabel);
        goldRow.setAlignment(Pos.CENTER_LEFT);

        // 当前效果状态（阶段 15）：没有状态时整行隐藏
        luckyStatusLabel.getStyleClass().addAll("player-status", "player-status-lucky");
        doubleExpStatusLabel.getStyleClass().addAll("player-status", "player-status-exp");
        Label statusKey = metricKey("当前效果");
        statusRow.getChildren().addAll(statusKey, luckyStatusLabel, doubleExpStatusLabel);
        statusRow.getStyleClass().add("player-status-row");
        statusRow.setAlignment(Pos.CENTER_LEFT);

        // 账号行（阶段 16）：账号 + 「切换账号」「退出登录」
        accountValueLabel.getStyleClass().add("player-account-value");
        Button switchAccountBtn = new Button("切换账号");
        switchAccountBtn.getStyleClass().add("player-account-btn");
        switchAccountBtn.setOnAction(e -> runAction(onSwitchAccount));
        Button logoutBtn = new Button("退出登录");
        logoutBtn.getStyleClass().addAll("player-account-btn", "player-account-btn-quit");
        logoutBtn.setOnAction(e -> runAction(onLogout));
        HBox accountRow = new HBox(8, metricKey("账号"), accountValueLabel, switchAccountBtn, logoutBtn);
        accountRow.getStyleClass().add("player-account-row");
        accountRow.setAlignment(Pos.CENTER_LEFT);

        HBox rankRow = buildMyRankRow();
        VBox info = new VBox(8, nameRow, accountRow, expRow, goldRow, statusRow, rankRow);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox head = new HBox(20, avatarCircle, info);
        head.getStyleClass().add("player-head");
        head.setAlignment(Pos.CENTER_LEFT);
        return head;
    }

    /** 金币充值区（阶段 12）：当前金币 + 三档模拟充值按钮，放在顶部身份卡下方。 */
    private VBox buildRechargeSection() {
        Label key = metricKey("金币");
        rechargeGoldLabel.getStyleClass().addAll("player-metric-value", "player-gold-value");
        rechargeGoldLabel.setStyle("-fx-font-size: 18px;");
        HBox goldRow = new HBox(8, key, rechargeGoldLabel);
        goldRow.setAlignment(Pos.CENTER_LEFT);

        HBox buttons = new HBox(10,
                rechargeButton("+100 金币", 100),
                rechargeButton("+500 金币", 500),
                rechargeButton("+1000 金币", 1000));
        buttons.setAlignment(Pos.CENTER_LEFT);

        // 累计统计（阶段 12-2）：数据来源 PlayerProfile.totalGoldEarned / totalGoldSpent
        panelEarnedLabel.getStyleClass().addAll("player-metric-value", "player-coin-earned");
        panelSpentLabel.getStyleClass().addAll("player-metric-value", "player-coin-spent");
        HBox totals = new HBox(18,
                metricKey("累计获得"), panelEarnedLabel,
                metricKey("累计消耗"), panelSpentLabel);
        totals.setAlignment(Pos.CENTER_LEFT);
        totals.getStyleClass().add("player-coin-totals");

        VBox box = new VBox(10, goldRow, buttons, totals);
        box.getStyleClass().add("player-recharge");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** 单档充值按钮：点击后走 {@link CoinRechargeService} 模拟充值。 */
    private Button rechargeButton(String text, int amount) {
        Button button = new Button(text);
        button.getStyleClass().add("player-recharge-btn");
        button.setOnAction(e -> {
            // 阶段 21：充值按钮点击音效
            com.csu.pokergame.audio.AudioService.getInstance()
                    .playEffect(com.csu.pokergame.audio.SoundEffect.BUTTON_CLICK);
            doRecharge(amount, text);
        });
        return button;
    }

    /** 模拟充值：成功则刷新金币与玩家信息，并用统一提示条反馈（失败仍弹窗）。 */
    private void doRecharge(int amount, String text) {
        boolean ok = rechargeService != null && rechargeService.recharge(amount);
        refresh();
        if (ok) {
            // 阶段 22：成功提示条 —— 例：＋100 金币
            GameAnimationService.getInstance().showToast(rechargeGoldLabel, text);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (getScene() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.setTitle("充值");
        alert.setHeaderText(null);
        alert.setContentText("充值失败，请重试");
        alert.showAndWait();
    }

    /** 触发外部动作（切换账号 / 退出登录），回调由 DeckApp 注入，本页不做任何账号逻辑。 */
    private static void runAction(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    // ============================================================= 我的资产（阶段 13）

    /**
     * 我的资产（背包）区：标题 + 固定高度滚动列表 + 「领取测试礼包」按钮。
     * 放在充值区下面、流水区上面；物品超出可视高度时由 ScrollPane 承接。
     */
    private VBox buildInventorySection() {
        Label title = section("我的资产");

        Button claim = new Button("领取测试礼包");
        claim.getStyleClass().add("player-recharge-btn");
        claim.setOnAction(e -> claimTestGift());
        inventoryList.getStyleClass().add("player-inventory-list");
        inventoryList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(inventoryList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(120);
        scroll.setMinHeight(80);
        scroll.setMaxHeight(130);
        scroll.getStyleClass().addAll("player-scroll", "player-inventory-scroll");

        VBox box = new VBox(8, title, scroll, claim);
        box.getStyleClass().add("player-inventory");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** 重建资产列表：逐条读 InventoryService 的数量，未拥有的显示 x0。 */
    private void refreshInventory() {
        inventoryList.getChildren().clear();
        List<Item> items = inventoryService == null
                ? List.of()
                : inventoryService.getItems();
        for (Item item : items) {
            inventoryList.getChildren().add(buildInventoryRow(item));
        }
    }

    /** 单行资产：图标 + 名称 + 说明，右侧数量 {@code xN} 与「使用 / 开启」按钮。 */
    private HBox buildInventoryRow(Item item) {
        Label icon = new Label(item.getIcon());
        icon.getStyleClass().add("player-inventory-icon");
        icon.setMinWidth(28);

        Label name = new Label(item.getName());
        name.getStyleClass().add("player-inventory-name");
        name.setMinWidth(110);

        Label desc = new Label(item.getDescription());
        desc.getStyleClass().add("player-inventory-desc");
        HBox.setHgrow(desc, Priority.ALWAYS);
        desc.setMaxWidth(Double.MAX_VALUE);

        Label count = new Label("x" + item.getCount());
        count.getStyleClass().add("player-inventory-count");

        // 使用按钮：金币宝箱文案为「开启」，其余为「使用」；点击走 ItemUseService
        Button use = new Button(InventoryService.GOLD_BOX.equals(item.getId()) ? "开启" : "使用");
        use.getStyleClass().add("player-inventory-use");
        use.setOnAction(e -> {
            // 阶段 21：使用道具 / 开启宝箱按钮点击音效
            com.csu.pokergame.audio.AudioService.getInstance()
                    .playEffect(com.csu.pokergame.audio.SoundEffect.BUTTON_CLICK);
            doUseItem(item);
        });

        HBox row = new HBox(10, icon, name, desc, count, use);
        row.getStyleClass().add("player-inventory-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * 使用道具：走 {@link ItemUseService#useItem(String)}，成功后整页刷新
     * （金币、资产数量、金币流水、状态层数一并更新），并弹窗反馈。
     */
    private void doUseItem(Item item) {
        boolean ok = itemUseService != null && itemUseService.useItem(item.getId());
        refresh();
        if (ok) {
            // 阶段 22：成功提示条
            GameAnimationService.getInstance().showToast(this, "使用成功：" + item.getName());
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (getScene() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.setTitle("使用道具");
        alert.setHeaderText(null);
        alert.setContentText("数量不足");
        alert.showAndWait();
    }

    /** 测试入口：领取测试礼包（三种道具各 +1，不消耗金币）。 */
    private void claimTestGift() {
        if (inventoryService == null) {
            return;
        }
        boolean ok = inventoryService.addItem(InventoryService.LUCKY_CARD, 1)
                & inventoryService.addItem(InventoryService.DOUBLE_EXP_CARD, 1)
                & inventoryService.addItem(InventoryService.GOLD_BOX, 1);
        refreshInventory();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("测试礼包");
        alert.setHeaderText(null);
        alert.setContentText(ok ? "测试礼包领取成功" : "领取失败，请重试");
        alert.showAndWait();
    }

    // ============================================================= 金币商城（阶段 14）

    /**
     * 金币商城区：标题 + 固定高度滚动列表。放在「我的资产」下面、「金币流水」上面；
     * 商品超出可视高度时由 {@link ScrollPane} 承接滚动，避免整页被撑高溢出窗口。
     */
    private VBox buildShopSection() {
        Label title = section("金币商城");

        shopList.getStyleClass().add("player-shop-list");
        shopList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(shopList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(170);
        scroll.setMinHeight(90);
        scroll.setMaxHeight(190);
        scroll.getStyleClass().addAll("player-scroll", "player-shop-scroll");

        VBox box = new VBox(8, title, scroll);
        box.getStyleClass().add("player-shop");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** 重建商城列表：逐条读商城服务的货架商品（名称 / 说明 / 价格 / 购买按钮）。 */
    private void refreshShop() {
        shopList.getChildren().clear();
        List<ShopItem> items = shopService == null ? List.of() : shopService.getShopItems();
        for (ShopItem item : items) {
            shopList.getChildren().add(buildShopRow(item));
        }
    }

    /** 单行商品：图标 + 名称 + 说明，右侧价格与「购买」按钮。 */
    private HBox buildShopRow(ShopItem item) {
        Label icon = new Label(item.getIcon());
        icon.getStyleClass().add("player-shop-icon");
        icon.setMinWidth(28);

        Label name = new Label(item.getName());
        name.getStyleClass().add("player-shop-name");
        name.setMinWidth(110);

        Label desc = new Label(item.getDescription());
        desc.getStyleClass().add("player-shop-desc");
        HBox.setHgrow(desc, Priority.ALWAYS);
        desc.setMaxWidth(Double.MAX_VALUE);

        Label price = new Label("价格：" + item.getPrice() + "金币");
        price.getStyleClass().add("player-shop-price");

        Button buy = new Button("购买");
        buy.getStyleClass().add("player-shop-buy");
        buy.setOnAction(e -> {
            // 阶段 21：商城购买按钮点击音效（购买成功另有 COIN_GAIN，由 CoinService 触发）
            com.csu.pokergame.audio.AudioService.getInstance()
                    .playEffect(com.csu.pokergame.audio.SoundEffect.BUTTON_CLICK);
            doBuy(item);
        });

        HBox row = new HBox(10, icon, name, desc, price, buy);
        row.getStyleClass().add("player-shop-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * 购买商品：走 {@link ShopService#buy(String)}，无论成功 / 失败都整页刷新，
     * 保证金币、资产数量、金币流水三处同步更新；随后弹窗给出购买反馈。
     */
    private void doBuy(ShopItem item) {
        boolean ok = shopService != null && shopService.buy(item.getId());
        refresh();
        if (ok) {
            // 阶段 22：购买成功用统一提示条（不再弹窗打断）；失败仍弹窗提示
            GameAnimationService.getInstance().showToast(shopList, "购买成功：" + item.getName() + " x1");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (getScene() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.setTitle("金币商城");
        alert.setHeaderText(null);
        alert.setContentText("金币不足");
        alert.showAndWait();
    }

    /**
     * 金币流水区：标题 + 固定高度的滚动列表，放在充值区下方。
     *
     * <p>最多展示最近 {@link CoinLogService#DEFAULT_LIMIT} 条（10 条）；
     * 条数超出可视高度时由 {@link ScrollPane} 承接滚动，避免把整页撑高。
     */
    private VBox buildCoinLogSection() {
        Label title = section("金币流水");

        coinLogList.getStyleClass().add("player-coin-log-list");
        coinLogList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(coinLogList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(150);
        scroll.setMinHeight(90);
        scroll.setMaxHeight(160);
        scroll.getStyleClass().addAll("player-scroll", "player-coin-log-scroll");

        VBox box = new VBox(8, title, scroll);
        box.getStyleClass().add("player-coin-log");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * 重建流水列表：从 {@link CoinLogService} 取最近 10 条（最新在前），
     * 收入绿色、支出红色；没有流水时给一行占位文案，保证区域高度稳定。
     */
    private void refreshCoinLog() {
        coinLogList.getChildren().clear();
        List<CoinLog> logs = coinLogService == null
                ? List.of()
                : coinLogService.getRecentLogs(CoinLogService.DEFAULT_LIMIT);
        if (logs.isEmpty()) {
            Label empty = new Label("暂无流水记录");
            empty.getStyleClass().add("player-coin-log-empty");
            coinLogList.getChildren().add(empty);
            return;
        }
        for (CoinLog log : logs) {
            coinLogList.getChildren().add(buildCoinLogRow(log));
        }
    }

    /** 单条流水行：左侧带符号金额（收/支配色）+ 中间原因 + 右侧时间。 */
    private static HBox buildCoinLogRow(CoinLog log) {
        Label change = new Label(log.getDisplayChange());
        change.getStyleClass().addAll("player-coin-log-change",
                log.isIncome() ? "player-coin-log-income" : "player-coin-log-expense");
        change.setMinWidth(72);

        Label reason = new Label(log.getReason() == null ? "" : log.getReason());
        reason.getStyleClass().add("player-coin-log-reason");
        HBox.setHgrow(reason, Priority.ALWAYS);
        reason.setMaxWidth(Double.MAX_VALUE);

        Label time = new Label(log.getTime() == null ? "" : log.getTime());
        time.getStyleClass().add("player-coin-log-time");

        HBox row = new HBox(10, change, reason, time);
        row.getStyleClass().add("player-coin-log-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** 单个数据格子：大号数值 + 下方说明文字，格子的配色由 {@code accentClass} 决定。 */
    private static VBox statBlock(String key, Label valueLabel, String accentClass) {
        valueLabel.getStyleClass().add("player-stat-value");
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("player-stat-key");
        VBox box = new VBox(4, valueLabel, keyLabel);
        box.getStyleClass().add("player-stat");
        if (accentClass != null) {
            box.getStyleClass().add(accentClass);
        }
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ============================================================= 最近战绩（阶段 17）

    /**
     * 最近战绩区（阶段 17）：战绩统计下方的小结 + 固定高度的滚动列表。
     *
     * <p>列表最多展示最近 {@link GameRecordService#DEFAULT_LIMIT} 条（20 条），
     * 条数超出可视高度时由内层 {@link ScrollPane} 承接滚动，不把整页撑高
     * （整页本身已在最外层 ScrollPane 里）。
     */
    private VBox buildRecordSection() {
        Label title = section("最近战绩");

        recordSummaryLabel.getStyleClass().add("player-record-summary");

        recordList.getStyleClass().add("player-record-list");
        recordList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(recordList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(180);
        scroll.setMinHeight(100);
        scroll.setMaxHeight(190);
        scroll.getStyleClass().addAll("player-scroll", "player-record-scroll");

        VBox box = new VBox(8, title, recordSummaryLabel, scroll);
        box.getStyleClass().add("player-record");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * 重建最近战绩列表：从 {@link GameRecordService} 取最近 20 条（最新在前）。
     *
     * <p>没有记录时给一行占位文案并隐藏小结，保证区域高度稳定；
     * 老账号（存档里没有 {@code records} 文件）与小节口径一致地显示"暂无战绩记录"。
     */
    private void refreshRecords() {
        recordList.getChildren().clear();

        int total = gameRecordService == null ? 0 : gameRecordService.getTotalGames();
        boolean hasRecords = total > 0;
        recordSummaryLabel.setVisible(hasRecords);
        recordSummaryLabel.setManaged(hasRecords);
        if (hasRecords) {
            recordSummaryLabel.setText("共 " + total + " 场　🏆 胜 " + gameRecordService.getWinCount()
                    + "　❌ 负 " + gameRecordService.getLossCount()
                    + "　胜率 " + gameRecordService.getWinRatePercent() + "%");
        }

        List<GameRecord> records = gameRecordService == null
                ? List.of()
                : gameRecordService.getRecentRecords(GameRecordService.DEFAULT_LIMIT);
        if (records.isEmpty()) {
            Label empty = new Label("暂无战绩记录");
            empty.getStyleClass().add("player-record-empty");
            recordList.getChildren().add(empty);
            return;
        }
        for (GameRecord record : records) {
            recordList.getChildren().add(buildRecordRow(record));
        }
    }

    /** 单条战绩行：🏆 / 💔 + 玩法与时间 + 胜负徽标 + 金币与经验变动。 */
    private static HBox buildRecordRow(GameRecord record) {
        Label icon = new Label(record.isWin() ? "🏆" : "💔");
        icon.setFont(Font.font("Segoe UI Emoji", 18));
        icon.setMinWidth(26);

        Label type = new Label(record.getGameType() == null ? "" : record.getGameType());
        type.getStyleClass().add("player-record-type");
        Label time = new Label(record.getDisplayTime());
        time.getStyleClass().add("player-record-time");
        VBox texts = new VBox(2, type, time);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);
        texts.setMaxWidth(Double.MAX_VALUE);

        Label result = new Label(record.getResultText());
        result.getStyleClass().addAll("player-record-result",
                record.isWin() ? "player-record-win" : "player-record-lose");
        result.setMinWidth(52);

        Label gold = new Label(record.getDisplayGoldChange());
        gold.getStyleClass().addAll("player-record-gold",
                record.getGoldChange() >= 0 ? "player-record-gain" : "player-record-cost");
        Label exp = new Label(record.getDisplayExpChange());
        exp.getStyleClass().add("player-record-exp");
        VBox changes = new VBox(2, gold, exp);
        changes.setAlignment(Pos.CENTER_RIGHT);
        changes.setMinWidth(76);

        HBox row = new HBox(12, icon, texts, result, changes);
        row.getStyleClass().add("player-record-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ============ 排行榜（阶段 18） ============

    /** 身份卡内的「我的排名」行：金币 / 等级 / 胜率 三个名次。 */
    private HBox buildMyRankRow() {
        HBox row = new HBox(12, metricKey("我的排名"),
                rankPair("金币", myGoldRankLabel),
                rankPair("等级", myLevelRankLabel),
                rankPair("胜率", myWinRateRankLabel));
        row.getStyleClass().add("player-rank-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** 单个名次单元：类别 + 「No.x」/「暂无排名」。 */
    private HBox rankPair(String key, Label value) {
        value.getStyleClass().add("player-rank-value");
        HBox box = new HBox(4, metricKey(key), value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** 排行榜区：标题 + 榜单切换 + 固定高度滚动列表。 */
    private VBox buildLeaderboardSection() {
        Label title = section("排行榜");
        HBox tabs = new HBox(8);
        tabs.setAlignment(Pos.CENTER_LEFT);
        for (LeaderboardService.Board board : LeaderboardService.Board.values()) {
            Button b = new Button(board.getLabel());
            b.getStyleClass().add("player-ranking-tab");
            b.setOnAction(e -> {
                rankingBoard = board;
                refreshLeaderboard();
            });
            rankingBoardButtons.put(board, b);
            tabs.getChildren().add(b);
        }

        rankingList.getStyleClass().add("player-ranking-list");
        rankingList.setFillWidth(true);
        ScrollPane scroll = new ScrollPane(rankingList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(200);
        scroll.setMinHeight(110);
        scroll.setMaxHeight(230);
        scroll.getStyleClass().addAll("player-scroll", "player-ranking-scroll");

        VBox box = new VBox(8, title, tabs, scroll);
        box.getStyleClass().add("player-ranking");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** 我的排名：金币 / 等级 / 胜率 三个名次，未上榜显示「暂无排名」。 */
    private void refreshMyRank() {
        myGoldRankLabel.setText(rankText(LeaderboardService.Board.GOLD));
        myLevelRankLabel.setText(rankText(LeaderboardService.Board.LEVEL));
        myWinRateRankLabel.setText(rankText(LeaderboardService.Board.WIN_RATE));
    }

    /** 取当前玩家在某榜单上的名次文案。 */
    private String rankText(LeaderboardService.Board board) {
        int position = leaderboardService == null ? 0 : leaderboardService.getMyRankPosition(board);
        return position > 0 ? "No." + position : "暂无排名";
    }

    /** 重建排行榜列表：按当前榜单取前 N 名，前三名加特殊样式，自己高亮。 */
    private void refreshLeaderboard() {
        rankingList.getChildren().clear();
        for (Map.Entry<LeaderboardService.Board, Button> e : rankingBoardButtons.entrySet()) {
            e.getValue().getStyleClass().remove("player-ranking-tab-active");
            if (e.getKey() == rankingBoard) {
                e.getValue().getStyleClass().add("player-ranking-tab-active");
            }
        }
        List<LeaderboardEntry> ranked = leaderboardService == null ? List.of()
                : leaderboardService.getTop(rankingBoard, LeaderboardService.DEFAULT_LIMIT);
        if (ranked.isEmpty()) {
            Label empty = new Label("暂无排行数据");
            empty.getStyleClass().add("player-ranking-empty");
            rankingList.getChildren().add(empty);
            return;
        }
        String me = leaderboardService == null ? null : leaderboardService.currentUsername();
        for (int i = 0; i < ranked.size(); i++) {
            rankingList.getChildren().add(buildRankingRow(i + 1, ranked.get(i), me));
        }
    }

    /** 单条榜单行：🥇 名次 + 玩家名 + Lv + 金币 + 胜率 + 胜场。 */
    private HBox buildRankingRow(int rank, LeaderboardEntry entry, String me) {
        Label medal = new Label(medal(rank));
        medal.getStyleClass().add("player-ranking-medal");
        medal.setMinWidth(34);

        Label name = new Label(entry.getUsername());
        name.getStyleClass().add("player-ranking-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        Label level = new Label(entry.getLevelText());
        level.getStyleClass().add("player-ranking-level");
        Label gold = new Label(entry.getGoldText());
        gold.getStyleClass().add("player-ranking-gold");
        Label rate = new Label("胜率 " + entry.getWinRateText());
        rate.getStyleClass().add("player-ranking-rate");
        Label wins = new Label(entry.getWinCountText());
        wins.getStyleClass().add("player-ranking-wins");

        HBox row = new HBox(10, medal, name, level, gold, rate, wins);
        row.getStyleClass().add("player-ranking-row");
        if (rank <= 3) {
            row.getStyleClass().add("player-ranking-top" + rank);
        }
        if (me != null && me.equals(entry.getUsername())) {
            row.getStyleClass().add("player-ranking-self");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** 前三名奖牌，其余显示序号。 */
    private static String medal(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> String.valueOf(rank);
        };
    }

    // ============ 数据中心（阶段 19） ============

    /**
     * 数据中心区（阶段 19）：位于排行榜下方，展示实时汇总的总场次 / 胜率 / 连胜 / 收支 / 最常游戏。
     *
     * <p>行数固定为 7 行，超出可视高度时由内层 {@link ScrollPane} 承接滚动，
     * 不把整页撑高（整页本身已在最外层 ScrollPane 里）。
     */
    private VBox buildStatisticsSection() {
        Label title = section("📊 数据中心");

        statisticsList.getStyleClass().add("player-statistics-list");
        statisticsList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(statisticsList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(220);
        scroll.setMinHeight(120);
        scroll.setMaxHeight(250);
        scroll.getStyleClass().addAll("player-scroll", "player-statistics-scroll");

        VBox box = new VBox(8, title, scroll);
        box.getStyleClass().add("player-statistics");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * 重建数据中心：从 {@link StatisticsService} 实时读取统计快照并渲染成 7 行，
     * 每次进入个人中心（或调用 {@link #refresh()}）都会重新计算，因此牌局结算后立刻反映最新数据。
     */
    private void refreshStatistics() {
        statisticsList.getChildren().clear();
        if (statisticsService == null) {
            return;
        }
        PlayerStatistics s = statisticsService.getStatistics();
        statisticsList.getChildren().add(statRow("总场次", s.getTotalGamesText(), false));
        statisticsList.getChildren().add(statRow("胜率", s.getWinRateText(), true));
        statisticsList.getChildren().add(statRow("最高连胜", String.valueOf(s.getMaxWinStreak()), false));
        statisticsList.getChildren().add(statRow("当前连胜", String.valueOf(s.getCurrentWinStreak()), false));
        statisticsList.getChildren().add(statRow("累计收入", String.valueOf(s.getTotalGoldEarned()), false));
        statisticsList.getChildren().add(statRow("累计消费", String.valueOf(s.getTotalGoldSpent()), false));
        statisticsList.getChildren().add(statRow("最常游戏", s.getFavoriteGameText(), true));
    }

    /** 数据中心的一行：左侧键、右侧值，值可加金色强调（{@code player-stat-highlight}）。 */
    private static HBox statRow(String key, String value, boolean highlight) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("player-stat-key");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("player-stat-value");
        if (highlight) {
            valueLabel.getStyleClass().add("player-stat-highlight");
        }

        HBox row = new HBox(10, keyLabel, spacer, valueLabel);
        row.getStyleClass().add("player-stat-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** 小节标题。 */
    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("player-section");
        return label;
    }

    // ============================================================= 成就（阶段 11）

    /**
     * 重建成就列表：顺序与 {@link AchievementService} 的定义顺序一致，
     * 已解锁显示 🏆，未解锁显示 🔒（并置灰），数据全部实时取自服务，本页不缓存副本。
     */
    private void refreshAchievements() {
        achievementList.getChildren().clear();
        if (achievementService == null) {
            return;
        }
        achievementProgress.setText("已解锁 " + achievementService.getUnlockedCount()
                + " / " + achievementService.getTotalCount());
        achievementProgress.getStyleClass().add("player-achievement-progress");

        // 最近获得成就（阶段 12）：未解锁任何成就时给一行占位文案，保证区域高度稳定
        Achievement latest = achievementService.getLatestAchievement();
        latestAchievementLabel.setText(latest == null
                ? "最近获得：暂无"
                : "最近获得：🏆 " + latest.getTitle() + "　" + latest.getDescription());

        for (Achievement a : achievementService.getAchievements()) {
            achievementList.getChildren().add(buildAchievementRow(a));
        }
    }

    /** 单条成就行：🏆 / 🔒 + 名称 + 条件与奖励说明。 */
    private static HBox buildAchievementRow(Achievement a) {
        Label icon = new Label(a.isUnlocked() ? "🏆" : "🔒");
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("player-achievement-title");
        Label desc = new Label(a.getDescription() + "　奖励 +" + a.getRewardGold() + " 金币 / +"
                + a.getRewardExp() + " 经验");
        desc.getStyleClass().add("player-achievement-text");
        VBox texts = new VBox(2, title, desc);
        HBox box = new HBox(12, icon, texts);
        box.getStyleClass().add("player-achievement");
        if (!a.isUnlocked()) {
            box.getStyleClass().add("player-achievement-locked");
        }
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Label metricKey(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("player-metric-key");
        return label;
    }

    /** 页面角落的花色暗纹（与首页 / 大厅一致的装饰语言）。 */
    private void addCornerSuit(String glyph, Color color, Pos corner) {
        Label label = new Label(glyph);
        label.setTextFill(color);
        label.setFont(Font.font("Segoe UI Symbol", 150));
        label.setMouseTransparent(true);
        StackPane.setAlignment(label, corner);
        getChildren().add(label);
    }

    /**
     * 头像字形专用字体：按码点选择字体族，避免缺字豆腐块。
     * 象棋 / 扑克符号（♛♚♝… U+2600 区段）走 Segoe UI Symbol，emoji 类（🃏🤖 U+1F000 以上）走 Segoe UI Emoji。
     */
    private static Font avatarGlyphFont(double size, String glyph) {
        boolean emoji = glyph != null && !glyph.isEmpty() && glyph.codePointAt(0) >= 0x1F000;
        return Font.font(emoji ? "Segoe UI Emoji" : "Segoe UI Symbol", FontWeight.BOLD, size);
    }
}
