package com.csu.pokergame.ui.scene;

import com.cards.render.CanvasCardRenderer;
import com.cards.render.CardRenderer;
import com.cards.ui.LobbyHelper;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.AvatarView;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.player.PlayerStatsService;
import com.csu.pokergame.ui.AppShell;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * 大厅首页：扑克大厅式主菜单 UI。
 *
 * <p>移植自 DeckApp.buildMenuScene 的富大厅界面，作为 {@link StackPane} 直接挂在
 * {@link AppShell} 的根容器上；CSS 由 AppShell 全局注入，本类不重复添加。
 *
 * <p>布局（自下而上叠加）：
 * <ol>
 *   <li>背景层：{@link BackgroundManager#createLobbyBackground()} 美术层 + 明暗渐变 + 金色环境光 + 粒子层</li>
 *   <li>四角花色装饰（♠♥♣♦，左上 / 右上 / 左下 / 右下）</li>
 *   <li>左右两侧半透明蓝色牌背剪影（纯装饰，鼠标穿透）</li>
 *   <li>中央：花色条 → 「中南棋牌室」主标题 → 开始游戏 / 游戏规则 / 退出游戏 三按钮</li>
 *   <li>右上角：设置齿轮按钮</li>
 *   <li>左上角：玩家头像卡（{@link AvatarView}，含等级称号 + 经验进度 + 金币 + 胜率）</li>
 *   <li>底部：游戏大厅 / 编辑资料 / 设置 三个药丸入口</li>
 *   <li>规则弹窗浮层（默认隐藏，「游戏规则」按钮触发显示）</li>
 * </ol>
 *
 * <p>动画：主标题金色呼吸、花色条错峰浮动、菜单内容依次淡入。
 */
public final class HomeView extends StackPane {

    private final AppShell shell;

    /** 牌背图像渲染器：用于左右两侧的牌背剪影装饰。 */
    private final CardRenderer renderer = new CanvasCardRenderer();
    /** 牌背图像缓存：red→Image，避免重复 snapshot。 */
    private final Map<Boolean, Image> backCache = new HashMap<>();

    /** 左上角玩家头像卡（点击进入个人主页）。 */
    private AvatarView homeAvatar;
    /** 规则弹窗浮层（默认隐藏）。 */
    private StackPane rulesOverlay;
    /** 入场动画节点：花色条 / 主标题 / 三按钮。 */
    private Node[] introNodes;

    public HomeView(AppShell shell) {
        this.shell = shell;
        getStyleClass().add("home-view");
        buildLobby();
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    // ============================================================= 大厅构建

    private void buildLobby() {
        // 进入首页 → 循环播放大厅背景音乐（资源缺失 / 音乐关闭时静默跳过）
        com.csu.pokergame.audio.AudioService.getInstance()
                .playMusic(com.csu.pokergame.audio.AudioService.BGM_LOBBY);

        // ================= 背景：BackgroundManager 统一构建（美术层 + 明暗渐变 + 底部增强 + 金色环境光 + 粒子层） =================
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        getChildren().add(BackgroundManager.fabricTexture());

        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源
        var particles = bgLayers.particles();
        sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) {
                particles.play();
            } else {
                particles.stop();
            }
        });

        // ================= 四角花色装饰 =================
        LobbyHelper.addCornerSuit(this, "♠", Color.rgb(255, 255, 255, 0.10), Pos.TOP_LEFT);
        LobbyHelper.addCornerSuit(this, "♥", Color.rgb(255, 140, 130, 0.10), Pos.TOP_RIGHT);
        LobbyHelper.addCornerSuit(this, "♣", Color.rgb(255, 255, 255, 0.10), Pos.BOTTOM_LEFT);
        LobbyHelper.addCornerSuit(this, "♦", Color.rgb(255, 140, 130, 0.10), Pos.BOTTOM_RIGHT);

        // ================= 左右两侧半透明蓝色牌背剪影（纯装饰，鼠标穿透） =================
        // 渲染器不可用时静默跳过——背景与中央内容仍正常显示
        try {
            Image backImg = backCache.computeIfAbsent(false, renderer::back);
            LobbyHelper.addCardDeco(this, backImg, 300, -14, Pos.CENTER_LEFT, new Insets(0, 0, 70, 44));
            LobbyHelper.addCardDeco(this, backImg, 300, 14, Pos.CENTER_RIGHT, new Insets(0, 44, 70, 0));
        } catch (RuntimeException ignored) {
            // 牌背渲染失败不影响主页主流程
        }

        // ================= 规则弹窗浮层（先建好供"游戏规则"按钮引用） =================
        rulesOverlay = new StackPane();
        rulesOverlay.setVisible(false);
        Region rShade = new Region();
        rShade.getStyleClass().add("modal-shade");
        rShade.setOnMouseClicked(ev -> rulesOverlay.setVisible(false));
        VBox rulesCard = LobbyHelper.buildRulesModalCard(rulesOverlay);
        StackPane.setAlignment(rulesCard, Pos.CENTER);
        StackPane.setMargin(rulesCard, new Insets(40));
        rulesOverlay.getChildren().addAll(rShade, rulesCard);

        // ================= 中央内容：花色条 → 主标题 → 三按钮 =================
        HBox suitStrip = new HBox(26);
        suitStrip.setAlignment(Pos.CENTER);
        LobbyHelper.addGradientSuit(suitStrip, "♠", Color.web("#f7e6b0"), Color.web("#b8942a"));
        LobbyHelper.addGradientSuit(suitStrip, "♥", Color.web("#ffc1ae"), Color.web("#c62b2b"));
        LobbyHelper.addGradientSuit(suitStrip, "♣", Color.web("#f7e6b0"), Color.web("#b8942a"));
        LobbyHelper.addGradientSuit(suitStrip, "♦", Color.web("#ffc1ae"), Color.web("#c62b2b"));
        suitStrip.setMouseTransparent(true);

        Text title = new Text("中南棋牌室");
        title.getStyleClass().add("menu-title");
        title.setMouseTransparent(true);

        Button start = new Button("开始游戏");
        start.getStyleClass().addAll("menu-btn", "menu-btn-start");
        start.setTooltip(new Tooltip("挑选一款游戏开始对局"));
        start.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("loading", SceneTransition.Type.ENTER_GAME);
        });
        // 主按钮辉光挂在外层 holder（避免与按钮 CSS 内阴影互相覆盖）+ hover 放大反馈
        StackPane startHolder = new StackPane(start);
        LobbyHelper.addStartButtonGlow(startHolder, start);

        Button rules = new Button("游戏规则");
        rules.getStyleClass().addAll("menu-btn", "menu-btn-rules", "menu-btn-sub");
        rules.setTooltip(new Tooltip("查看玩法与规则说明"));
        rules.setOnAction(e -> {
            LobbyHelper.clickSound();
            rulesOverlay.setVisible(true);
        });

        Button exit = new Button("退出游戏");
        exit.getStyleClass().addAll("menu-btn", "menu-btn-quit", "menu-btn-sub");
        exit.setTooltip(new Tooltip("退出中南棋牌室"));
        exit.setOnAction(e -> Platform.exit());

        HBox subActions = new HBox(22, rules, exit);
        subActions.setAlignment(Pos.CENTER);

        VBox buttons = new VBox(26, startHolder, subActions);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(30, suitStrip, title, buttons);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);
        StackPane.setMargin(center, new Insets(0, 0, 96, 0));
        getChildren().add(center);

        // 主标题金色呼吸 + 花色条错峰浮动（手游大厅式"活"标题）
        LobbyHelper.playTitleBreath(title);
        LobbyHelper.playSuitFloat(suitStrip);

        // 右下角技术备注（原副标题降噪后移到底角，不参与主流程）
        Label tech = new Label("基于 JavaFX 的桌面棋牌小游戏 · 课程项目");
        tech.getStyleClass().add("tech-note");
        tech.setMouseTransparent(true);
        StackPane.setAlignment(tech, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(tech, new Insets(0, 36, 18, 0));
        getChildren().add(tech);

        // ================= 右上角：设置齿轮 =================
        Path gear = LobbyHelper.gearShape();
        gear.getStyleClass().add("gear-icon");
        gear.setMouseTransparent(true);

        Button gearBtn = new Button();
        gearBtn.setGraphic(gear);
        gearBtn.getStyleClass().add("settings-btn");
        gearBtn.setTooltip(new Tooltip("设置"));
        gearBtn.setPickOnBounds(true);
        gear.fillProperty().bind(Bindings.when(gearBtn.hoverProperty())
                .then(Color.web("#ffd54f"))
                .otherwise(Color.web("#f2f6ee")));
        StackPane.setAlignment(gearBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(gearBtn, new Insets(20, 26, 0, 0));
        gearBtn.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("settings", SceneTransition.Type.FADE);
        });
        getChildren().add(gearBtn);

        // ================= 左上角：玩家区域（AvatarView 圆头像 + 昵称 + 称号等级 + 成长信息，点击进入个人中心） =================
        PlayerProfile profile = PlayerManager.getInstance().getProfile();
        homeAvatar = new AvatarView(profile.getAvatar(), profile.getName(), "", profile.getLevel());
        // 首页玩家卡启用成长信息：等级称号徽章 + 经验进度 + 金币 + 胜率（在线状态只在选择游戏页展示）
        applyGrowthInfo(homeAvatar);
        homeAvatar.getStyleClass().add("home-player-card");
        homeAvatar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        homeAvatar.setPickOnBounds(true);
        homeAvatar.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(homeAvatar, new Tooltip("查看个人主页"));
        homeAvatar.setOnMouseClicked(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("profile", SceneTransition.Type.OPEN_PROFILE);
        });
        StackPane.setAlignment(homeAvatar, Pos.TOP_LEFT);
        StackPane.setMargin(homeAvatar, new Insets(22, 0, 0, 24));
        getChildren().add(homeAvatar);

        // ================= 底部功能入口：游戏大厅 / 编辑资料 / 设置 =================
        Button hallEntry = LobbyHelper.lobbyEntry("🏛", "游戏大厅", () -> shell.transitionTo("game-choice", SceneTransition.Type.ZOOM));
        // 「个人信息」的名字已归个人中心页所有，这里明确为"编辑资料"，避免两个入口语义混淆
        Button profileEntry = LobbyHelper.lobbyEntry("👤", "编辑资料", () -> shell.transitionTo("profile-edit", SceneTransition.Type.OPEN_PROFILE));
        Button settingsEntry = LobbyHelper.lobbyEntry("⚙", "设置", () -> shell.transitionTo("settings", SceneTransition.Type.FADE));
        HBox bottomBar = new HBox(18, hallEntry, profileEntry, settingsEntry);
        bottomBar.getStyleClass().add("lobby-bottom-bar");
        bottomBar.setAlignment(Pos.CENTER);
        // StackPane 会拉伸可缩放子节点铺满场景；锁定为内容尺寸后 BOTTOM_CENTER 才能把入口栏压到底部
        bottomBar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(bottomBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bottomBar, new Insets(0, 0, 24, 0));
        getChildren().add(bottomBar);

        // 规则浮层最后加入，覆盖所有底层节点
        getChildren().add(rulesOverlay);

        // ================= 入场动画：首次显示与每次回到主页时依次淡入 =================
        introNodes = new Node[]{suitStrip, title, start, rules, exit};
        LobbyHelper.playMenuIntro(introNodes);
    }

    // ============================================================= 玩家卡成长信息

    /**
     * 把「等级称号 + 成长信息（经验进度 / 金币 / 胜率）」回填到首页玩家卡，全部实时查询当前值：
     * <pre>
     * 玩家  🥈 白银 Lv.12      ← 等级徽章 + 称号 + 等级
     * 经验  ▓▓▓░░░░░  350/1300 ← 经验进度条 + 当前/需求
     * 金币  1000               ← 金币数量
     * 胜率  65%                ← 战绩胜率（0 场时显示 0%）
     * </pre>
     *
     * <p>等级 / 经验取 {@link PlayerManager} 档案，升级需求与称号由 {@link PlayerGrowthService} 给出，
     * 金币由 {@link CoinService} 给出，胜率由 {@link PlayerStatsService} 给出，
     * 当前登录账号由 {@link AccountService} 给出（昵称兜底）。首页不展示在线状态行。
     */
    private void applyGrowthInfo(AvatarView card) {
        if (card == null) {
            return;
        }
        var players = PlayerManager.getInstance();
        var growth = PlayerGrowthService.getInstance();
        var coins = CoinService.getInstance();
        var stats = PlayerStatsService.getInstance();
        var accounts = AccountService.getInstance();
        var profile = players.getProfile();
        int level = profile.getLevel();
        card.setLevel(level);
        card.setLevelTitle(growth.getLevelBadge(level), growth.getLevelTitle(level));
        // 昵称兜底：profile.name 缺失时退回当前登录账号用户名
        String displayName = profile.getName();
        if ((displayName == null || displayName.isBlank()) && accounts.isLoggedIn()) {
            displayName = accounts.getCurrentUsername();
        }
        card.setAvatarName(displayName);
        // 头像字形也跟随存档（玩家可能在个人信息页换了头像）
        card.setGlyph(profile.getAvatar());
        // 首页不展示在线状态行
        card.setStatusText("");
        card.setGrowthInfo(profile.getExp(), growth.expToNextLevel(level),
                coins.getGold(), stats.getWinRatePercent());
    }

    // ============================================================= 回主页刷新

    /**
     * 回到首页时刷新玩家卡：经验 / 等级 / 金币 / 胜率 / 头像 / 昵称可能已在牌局结算里增长，
     * 全部从单例服务实时读取覆盖。同时重放菜单入场动画，让回到主页有"页面切换"的呼吸感。
     */
    public void refreshUserCard() {
        if (homeAvatar != null) {
            applyGrowthInfo(homeAvatar);
        }
        if (introNodes != null && introNodes.length > 0) {
            LobbyHelper.playMenuIntro(introNodes);
        }
    }
}
