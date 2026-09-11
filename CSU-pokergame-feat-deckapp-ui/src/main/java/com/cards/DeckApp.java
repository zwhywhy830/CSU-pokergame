package com.cards;

import com.cards.deck.Deck;
import com.cards.io.CardImageWriter;
import com.cards.io.PngCardImageWriter;
import com.cards.model.Card;
import com.cards.render.CardRenderer;
import com.cards.render.CanvasCardRenderer;
import com.cards.ui.CardCell;
import com.cards.ui.LoginView;
import com.cards.ui.ParticleField;
import com.cards.ui.ProfileView;
import com.cards.ui.SettingsView;
import com.cards.ui.component.AvatarView;
import com.cards.ui.component.CoinBar;
import com.cards.ui.component.GrowthResultPanel;
import com.cards.ui.component.HandCardView;
import com.cards.ui.component.PlayedCardsView;
import com.cards.ui.component.SeatView;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.effect.WinCelebration;
import com.cards.ui.liar.LiarActionBar;
import com.cards.ui.liar.LiarHandView;
import com.cards.ui.liar.LiarPlayerSeat;
import com.cards.ui.liar.LiarTableView;
import com.cards.ui.pdk.PdkActionBar;
import com.cards.ui.pdk.PdkHandView;
import com.cards.ui.pdk.PdkPlayerSeat;
import com.cards.ui.pdk.PdkTableHeader;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.core.player.BotDecision;
import com.csu.pokergame.core.player.RuleBotController;
import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.LiarEngine;
import com.csu.pokergame.liarspoker.LiarPhase;
import com.csu.pokergame.liarspoker.LiarRandomPolicy;
import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.network.RoomSnapshot;
import com.csu.pokergame.network.RoomPlayer;
import com.csu.pokergame.paodekuai.PdkBotPolicy;
import com.csu.pokergame.paodekuai.PdkEngine;
import com.csu.pokergame.paodekuai.PdkMoveType;
import com.csu.pokergame.paodekuai.PdkSnapshot;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PlayPdkCards;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.control.ListView;

import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.theme.DesignTokens;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.CoinLogService;
import com.csu.pokergame.player.CoinRechargeService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.InventoryService;
import com.csu.pokergame.player.ItemUseService;
import com.csu.pokergame.player.LeaderboardService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.player.Achievement;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.PlayerStatsService;
import com.csu.pokergame.player.ShopService;
import com.csu.pokergame.player.StatisticsService;
import com.csu.pokergame.settings.SettingsService;

/**
 * 主程序：用 JavaFX 生成并展示整副扑克牌（54 张）。
 *
 * 功能：
 *  - 整副展示（洗牌 / 整理排序 / 全部翻开 / 全部盖牌）；
 *  - 蓝、红两种牌背切换；
 *  - 导出全部牌面 PNG；
 *  - 展示固定：鼠标悬停、点按都不会改变牌面或牌序，只有按“洗牌”才会打乱。
 */
public class DeckApp extends Application {

    private static final double CELL_WIDTH = 122;

    private final Map<Card, Image> faceCache = new HashMap<>();
    private final Map<Boolean, Image> backCache = new HashMap<>();
    private final CardRenderer renderer = new CanvasCardRenderer();
    private final CardImageWriter imageWriter = new PngCardImageWriter();

    private List<Card> order = new ArrayList<>(Deck.standard().cards());
    private boolean backRed = false;
    private final List<CardCell> cells = new ArrayList<>();

    private FlowPane grid;
    private Label status = new Label();
    /** 开始界面（主页），用于“返回主页”与“开始游戏”后续切换。 */
    private Scene homeScene;
    /** 游戏选择界面：点“开始游戏”进入，可选择湖南跑得快 / 骗子酒馆。 */
    private Scene gameChoiceScene;
    /** 模式选择界面：点任一游戏卡片进入，可选择本地人机 / 局域网联机。 */
    private Scene modeChoiceScene;
    /** 模式选择界面顶部的游戏名徽标（进入时由 enterModeChoice 更新为当前所选游戏）。 */
    private final Label modeBadge = new Label();
    /** 加载动画页：点“开始游戏”后先短暂展示，再进入“选择游戏”页。 */
    private Scene loadingScene;
    /** 个人信息编辑页：修改昵称/头像/状态（底部「编辑资料」入口）。 */
    private Scene profileScene;
    /** 玩家个人中心页（阶段 10）：点首页头像进入，展示成长 / 战绩 / 资产。 */
    private Scene profileViewScene;
    private ProfileView profileView;
    /** 登录页（阶段 16）：程序启动的第一屏；个人中心「切换账号 / 退出登录」也回到这里。 */
    private Scene loginScene;
    /** 登录视图：返回登录页时用它预填账号名。 */
    private LoginView loginView;
    /** 设置中心页（阶段 20）：底部「设置」入口进入，展示音频 / 体验 / 主题 / 账号 / 数据。 */
    private Scene settingsScene;
    /** 设置视图：进入页面时刷新一次，之后由设置服务的变更监听自动同步。 */
    private SettingsView settingsView;
    /** 账号服务（阶段 16）：注册 / 登录 / 登出 / 切换账号的唯一入口。 */
    private AccountService accountService;
    /** 「切换账号」返回登录页时预填的账号名（退出登录时清空）。 */
    private String pendingLoginName;
    /** 玩家昵称 / 头像字符 / 在线状态（在个人信息页编辑后回填到选择游戏页）。 */
    private String userNick = "玩家";
    private String userAvatarGlyph = "♛";
    private String userStat = "在线 · 准备开局";
    /** 玩家等级（首页/大厅玩家卡展示；个人信息页暂不提供编辑）。 */
    private int userLevel = 12;
    /** 选择游戏页头像卡片内的字符、昵称、状态节点（供编辑后刷新）。 */
    private final Text userAvatarText = new Text();
    private final Label userNickLabel = new Label();
    private final Label userStatLabel = new Label();
    /** 首页左上角玩家卡 / 选择游戏页玩家卡（AvatarView，个人信息保存后同步刷新）。 */
    private AvatarView homeAvatar;
    private AvatarView choiceAvatar;
    /** 音量 / 亮度偏好（后续玩法版本会真正生效）。 */
    private double volumePref = 60;
    private double brightnessPref = 100;
    /** 玩家数据管理器：昵称 / 等级 / 头像 / 经验 / 胜负场次的唯一数据来源。 */
    private PlayerManager playerManager;
    /** 统一金币服务层：金币的查询与增减全部走它，界面不直接碰 gold 字段。 */
    private CoinService coinService;
    /** 模拟充值服务（阶段 12）：唯一的充值入口，内部转发 CoinService.addGold。 */
    private CoinRechargeService coinRechargeService;
    /** 金币流水查询服务（阶段 12-2）：个人中心「金币流水」区域的数据来源。 */
    private CoinLogService coinLogService;
    /** 背包服务（阶段 13）：玩家资产的唯一入口。 */
    private InventoryService inventoryService;
    /** 商城服务（阶段 14）：金币购买道具的唯一入口。 */
    private ShopService shopService;
    /** 道具使用服务（阶段 15）：背包道具使用 / 开启的唯一入口。 */
    private ItemUseService itemUseService;
    /** 玩家成长服务：经验 / 等级 / 升级奖励的唯一入口（升级奖励金币内部走 CoinService）。 */
    private PlayerGrowthService growthService;
    /** 战绩统计服务：胜负场次 / 胜率 / 连胜的唯一入口（写入 player.json）。 */
    private PlayerStatsService statsService;
    /** 成就服务（阶段 11）：成就检测 / 解锁 / 奖励的唯一入口。 */
    private AchievementService achievementService;
    /**
     * 战绩明细服务（阶段 17）：每局一条 {@link com.csu.pokergame.player.GameRecord}
     * （玩法 / 胜负 / 金币 / 经验 / 时间），落盘 {@code data/players/<username>_records.json}。
     * 与 {@link #statsService} 的"汇总计数"是两个层次：一个记明细、一个记总数，同一个结算点写入。
     */
    private GameRecordService gameRecordService;

    /**
     * 排行榜服务（阶段 18）：汇总全部账号的金币 / 等级 / 胜率 / 胜场，供个人中心「排行榜」区展示。
     * 它只读账号表与各账号存档，用临时 {@code PlayerManager} 实例读取，不会切换或污染当前登录玩家。
     */
    private LeaderboardService leaderboardService;

    /**
     * 玩家数据统计中心服务（阶段 19）：从战绩明细 + 金币累计实时汇总出总场次 / 胜率 / 连胜 / 收支 / 最常游戏，
     * 供个人中心「数据中心」区展示。本服务只读、不落盘（不污染 {@code player.json}）。
     */
    private StatisticsService statisticsService;

    /**
     * 设置中心服务（阶段 20）：全局唯一的设置读写入口，落盘 {@code data/settings.json}。
     * 由 {@link #loadSettings()} 在 {@link #start(Stage)} 中初始化，所有页面通过它读取设置。
     */
    private SettingsService settingsService;

    // ==================== 游戏牌桌运行时上下文（每次进入桌台时重建） ====================
    /** 当前所选游戏（在进入模式选择页时记录，供"本地人机"按钮判断启动哪种桌台）。 */
    private enum SelectedGame { PDK, LIAR }
    private SelectedGame selectedGame = SelectedGame.PDK;

    private PdkEngine pdkEngine;
    private LiarEngine liarEngine;
    private RuleBotController pdkBot2, pdkBot3;
    private RuleBotController[] liarBots;
    /** 本地桌当前选中的牌（点击手牌 toggle）。 */
    private final Set<com.csu.pokergame.core.card.Card> tableSelected = new HashSet<>();
    /** bot 回合延迟计时器，用于在重刷前取消上一个等待。 */
    private PauseTransition botDelay;
    /** 跑得快桌 UI 节点（阶段 23：底部玩家区改用 {@link PdkHandView} 组件）。 */
    private PdkHandView pdkHandBox;
    /** 跑得快桌顶部比赛信息栏（阶段 23 新增）。 */
    private PdkTableHeader pdkHeader;
    private PlayedCardsView pdkCenterBox;
    private Label pdkStatus;
    private ListView<String> pdkLog;
    private Button pdkPlayBtn, pdkPassBtn;
    /** 跑得快牌桌 AI 座位条（阶段 23：{@link PdkPlayerSeat} × 2；联机为 × 3）。 */
    private HBox pdkSeatStrip;
    private PdkPlayerSeat pdkSeat1, pdkSeat2, pdkSeat3;
    /** 牌桌金币栏。 */
    private CoinBar pdkCoinBar, liarCoinBar;
    /**
     * 阶段 23：本会话跑得快局数（界面侧计数，仅用于顶部信息栏「第 N 局」展示，
     * 每次重建跑得快桌台 +1；<b>不是</b>规则字段，不写入引擎 / 快照）。
     */
    private int pdkRoundNo = 0;
    /**
     * 阶段 23：最近一次出牌的座位（仅界面侧记录，用于中央「XX 出牌」提示卡）。
     * 快照本身不含「上一手出牌人」，故由 UI 在 apply 后自行记录。
     */
    private PlayerId pdkLastMover;
    /** 阶段 24：骗子酒馆专属 UI 主容器（本地人机桌使用）。 */
    private LiarTableView liarView;
    /** 阶段 24：本人手牌的选中下标（界面侧，与快照手牌顺序一一对应）。 */
    private final Set<Integer> liarLocalSelection = new LinkedHashSet<>();
    /**
     * 本局骗子酒馆是否已完成金币结算（闸门，同 {@link #pdkSettled}）。
     * 建桌时重置为 false，保证一局只发一次奖、只扣一次入场费对应的那一局只结算一次。
     */
    private boolean liarSettled = false;
    /** 本局骗子酒馆的金币变化（供结算副标题展示）。 */
    private int liarGoldDeltaForResult = 0;
    /** 骗子酒馆桌 UI 节点（联机桌仍使用旧组件）。 */
    private HandCardView liarHandBox;
    private HBox liarOppStrip;
    private Label liarTarget, liarPhase, liarResolution;
    private ListView<String> liarLog;
    private Button liarDeclareBtn, liarTrustBtn, liarChallengeBtn;
    /** 骗子酒馆座位（SeatView × 4）。 */
    private SeatView liarSeats[];
    /** 联机运行时。 */
    private LanHost lanHost;
    private LanClient lanClient;
    private PlayerId lanLocalSeat = PlayerId.SEAT_1;
    private GameSnapshot lanLastSnapshot;
    /** 联机最近一次房间快照（座位名来源；开局后不再变化）。 */
    private RoomSnapshot lanLastRoom;
    /** 当前联机牌桌场景（用于判断是否仍停留在牌桌上）。 */
    private Scene lanTableScene;
    /** 联机座位名字气泡（SeatView 紧凑态不显示名字，用 Tooltip 承载 RoomSnapshot 中的真实玩家名）。 */
    private Tooltip[] lanSeatTips;
    /** 联机结算动画是否已展示（避免重复叠加；每次新建联机桌台时重置）。 */
    private boolean lanResultShown = false;
    /** 本局结算动画是否已展示（避免结算遮罩被重复添加；每次新建桌台时重置为 false）。 */
    private boolean resultShown = false;
    /**
     * 本局跑得快是否已完成金币 / 经验结算。
     * 作为本局结算的唯一闸门：即便 {@code refreshPdkTable} 被多次触发、结算按钮被重复点击，
     * 也只会发奖一次。每次新建跑得快桌台时重置为 false（新的一局 = 新的一次结算）。
     */
    private boolean pdkSettled = false;
    /**
     * 本局跑得快的结算成长反馈（胜负 / 金币变化 / 经验 / 等级 / 升级），
     * 由 {@link #settlePdk(boolean)} 写入，供结算卡片（{@link GrowthResultPanel}）展示。
     * 每次新建跑得快桌台时清空。
     */
    private boolean pdkWinForResult = false;
    private int pdkGoldDeltaForResult = 0;
    private int pdkExpGainForResult = 0;
    private PlayerGrowthService.LevelUpResult pdkGrowthForResult = null;
    /**
     * 本局解锁的成就（阶段 11）：进入牌桌检测 FIRST_GAME、结算检测 FIRST_WIN /
     * WIN_STREAK_5 / GOLD_10000 / LEVEL_10 / LEVEL_20，解锁结果汇集到这里，由结算卡片展示。
     */
    private final java.util.List<Achievement> pdkAchievementsForResult = new java.util.ArrayList<>();
    /** 临时场景（牌桌 / 联机大厅，会被反复重建）当前挂载的粒子监听与其粒子层。 */
    private ChangeListener<Scene> transientParticleListener;
    private ParticleField transientParticleField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        if (getParameters().getRaw().contains("--export")) {
            Path dir = Paths.get("output", "cards").toAbsolutePath();
            try {
                int n = exportTo(dir);
                System.out.println("已导出 " + n + " 张扑克牌图片 -> " + dir);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            Platform.exit();
            return;
        }

        // 阶段 20：先加载设置（不存在则自动生成默认 settings.json），再加载玩家档案，
        // 保证首帧就按用户偏好渲染；所有页面统一从 SettingsService 读取设置。
        loadSettings();
        // 阶段 21：音频服务按设置初始化（音量 / 音效 / 音乐开关），无音频资源时静默跳过
        com.csu.pokergame.audio.AudioService.getInstance().refreshSettings();
        // 先加载玩家档案，再构建各页面，保证首页/大厅首帧就显示真实昵称、等级与金币
        loadPlayerProfile();

        grid = new FlowPane();
        Scene deckScene = buildDeckScene(stage);
        homeScene = buildMenuScene(stage, deckScene);
        loadingScene = buildLoadingScene(stage);
        gameChoiceScene = buildGameChoiceScene(stage);
        profileScene = buildProfileScene(stage);
        profileViewScene = buildProfileViewScene(stage);
        modeChoiceScene = buildModeChoiceScene(stage);
        // 阶段 16：登录页最后构建（它的成功回调要导航到 homepage，所以必须在场景就绪后创建）
        loginScene = buildLoginScene(stage);
        // 阶段 20：设置中心（音频 / 体验 / 主题 / 账号 / 数据）
        settingsScene = buildSettingsScene(stage);
        stage.setTitle("中南棋牌室");
        // 首屏是登录页：没有旧场景可参考，走无动画导航（唯一接触 stage.setScene 的地方在 SceneTransition 内）
        SceneTransition.navigate(stage, loginScene, SceneTransition.Type.NONE);
        // 按屏幕可用区域自适应窗口大小。
        // JavaFX 的 Screen/VisualBounds 使用逻辑坐标，stage 尺寸也按逻辑坐标设置，
        // 正常 HiDPI（物理=逻辑×outputScale）下没有问题；但在部分多 GPU / 远程桌面 /
        // 缩放覆盖的机器上，JavaFX 报告的 outputScale 会大于系统真实 DPI（实测 1.5
        // 而面板物理分辨率只有逻辑值 1:1），此时窗口被放大渲染到超出物理屏幕，
        // 右侧与底部（含首页底部功能入口栏）被屏幕边缘裁掉。
        // 用 AWT 拿到系统级真实物理分辨率做交叉校验，得到“过渲染系数”k 后收缩逻辑尺寸。
        var primary = Screen.getPrimary();
        var visual = primary.getVisualBounds();
        double effX = visual.getMinX();
        double effY = visual.getMinY();
        double effW = visual.getWidth();
        double effH = visual.getHeight();
        try {
            var awtSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            double fxPhysW = visual.getWidth() * primary.getOutputScaleX();
            double fxPhysH = visual.getHeight() * primary.getOutputScaleY();
            if (awtSize.width > 0 && awtSize.height > 0
                    && (awtSize.width < fxPhysW * 0.95 || awtSize.height < fxPhysH * 0.95)) {
                // JavaFX 认为的物理尺寸大于 AWT 实测物理尺寸：把逻辑可视区等比缩回真实范围
                double kx = fxPhysW / awtSize.width;
                double ky = fxPhysH / awtSize.height;
                effX = visual.getMinX() / kx;
                effY = visual.getMinY() / ky;
                effW = visual.getWidth() / kx;
                effH = visual.getHeight() / ky;
            }
        } catch (Throwable ignored) {
            // AWT 不可用时退回 JavaFX 原生判断
        }
        double winW = Math.min(1580, effW - 28);
        // 高度再预留标题栏（约 31 逻辑像素），避免窗口底边连同底部入口栏被任务栏/屏幕裁掉
        double winH = Math.min(980, effH - 28 - 31);
        stage.setWidth(winW);
        stage.setHeight(winH);
        stage.setX(effX + (effW - winW) / 2);
        stage.setY(effY + (effH - winH) / 2);
        stage.show();
    }

    // ============================================================= 场景导航（转场）

    /** 返回 / 同级页面切换（返回选择游戏、返回主页）：交叉淡化。 */
    private void navFade(Stage stage, Scene target) {
        SceneTransition.fade(stage, target);
    }

    /** 进入下一流程（玩法模式、游戏大厅、加载页）：缩放进入 + 金色光环 + 轻微辉光。 */
    private void navZoom(Stage stage, Scene target) {
        SceneTransition.zoom(stage, target);
    }

    /** 进入牌桌 / 重新开始 / 联机开局（ENTER_GAME）：大厅缩小 → 光环扩散 → 花色粒子 → 巨大牌背翻转 → 牌桌展开。 */
    private void navEnterGame(Stage stage, Scene target) {
        SceneTransition.enterGame(stage, target);
    }

    /** 返回大厅（RETURN_LOBBY）：牌桌渐暗 → 卡牌碎片向中心聚合 → 大厅重新展开。 */
    private void navReturnLobby(Stage stage, Scene target) {
        SceneTransition.returnLobby(stage, target);
    }

    /** 打开个人信息（OPEN_PROFILE）：圆形光圈 + 头像放大 + 背景虚化 + 卡片滑入。 */
    private void navProfile(Stage stage, Scene target) {
        SceneTransition.openProfile(stage, target);
    }

    /** 胜利返回（WIN_TO_LOBBY）：皇冠残留 + 金币飞散 + 场景旋转淡出。 */
    private void navWinToLobby(Stage stage, Scene target) {
        SceneTransition.winToLobby(stage, target);
    }

    /** 阶段 21：通用按钮点击音效（统一走 AudioService，页面不直接接触 AudioClip）。 */
    private static void clickSound() {
        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(com.csu.pokergame.audio.SoundEffect.BUTTON_CLICK);
    }

    // ============================================================= UI 构建

    /** 构建 54 张扑克牌展示页（保留作后续“游戏主界面”的页面基础）。 */
    private Scene buildDeckScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        // ---------------- 顶部工具栏 ----------------
        Label spade = glyph("♠", "glyph-ink");
        Label heart = glyph("♥", "glyph-red");
        Label club = glyph("♣", "glyph-ink");
        Label diamond = glyph("♦", "glyph-red");
        Label title = new Label("JavaFX 扑克牌生成器");
        title.getStyleClass().add("app-title");

        Button btnHome = new Button("← 返回主页");
        btnHome.getStyleClass().add("btn-home");
        btnHome.setOnAction(e -> {
            if (homeScene != null) {
                navFade(stage, homeScene);
            }
        });

        Button btnShuffle = new Button("洗牌");
        btnShuffle.setOnAction(e -> {
            order = Deck.standard().shuffled();
            rebuildGrid();
        });
        Button btnSort = new Button("整理排序");
        btnSort.setOnAction(e -> {
            order = new ArrayList<>(Deck.standard().cards());
            rebuildGrid();
        });
        Button btnAllUp = new Button("全部翻开");
        btnAllUp.setOnAction(e -> flipAll(true));
        Button btnAllDown = new Button("全部盖牌");
        btnAllDown.setOnAction(e -> flipAll(false));

        ToggleGroup backGroup = new ToggleGroup();
        ToggleButton blueBack = new ToggleButton("蓝色牌背");
        blueBack.setToggleGroup(backGroup);
        blueBack.setSelected(true);
        ToggleButton redBack = new ToggleButton("红色牌背");
        redBack.setToggleGroup(backGroup);
        backGroup.selectedToggleProperty().addListener((o, a, b) -> {
            backRed = b == redBack;
            for (CardCell cell : cells) {
                cell.applyState();
            }
        });

        Button btnExport = new Button("导出 PNG");
        btnExport.setOnAction(e -> chooseAndExport(stage));

        HBox titleBox = new HBox(2, spade, heart, club, diamond, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar bar = new ToolBar(btnHome, new Separator(), titleBox, new Separator(), btnShuffle, btnSort,
                new Separator(), btnAllUp, btnAllDown,
                new Separator(), blueBack, redBack, spacer, btnExport);
        bar.getStyleClass().add("app-toolbar");
        root.setTop(bar);

        // ---------------- 中央牌区 ----------------
        grid.setPadding(new Insets(16, 12, 18, 12));
        grid.setHgap(8);
        grid.setVgap(12);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPrefWrapLength(CELL_WIDTH * 13 - 6);
        grid.getStyleClass().add("cards-panel");

        StackPane content = new StackPane();
        content.getChildren().add(grid);

        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToHeight(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(scroller);

        // ---------------- 状态栏 ----------------
        status.getStyleClass().add("status-label");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6, 14, 6, 14));
        root.setBottom(status);
        refreshStatus();

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        return scene;
    }

    // ============================================================= 开始界面（主页）

    /** 构建开始界面（主页）：国风山水背景 + 书法主标题 + 权重化按钮 + 设置/规则浮层。 */
    private Scene buildMenuScene(Stage stage, Scene deckScene) {
        // 阶段 21：进入首页 → 循环播放大厅背景音乐（资源缺失 / 音乐关闭时静默跳过）
        com.csu.pokergame.audio.AudioService.getInstance()
                .playMusic(com.csu.pokergame.audio.AudioService.BGM_LOBBY);
        StackPane root = new StackPane();

        // ================= 背景：BackgroundManager 统一构建（照片美术层 + 明暗渐变 + 底部增强 + 金色环境光 + 粒子层） =================
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        StackPane bgLayer = bgLayers.root();
        Region themeShade = bgLayers.shade();
        root.getChildren().add(bgLayer);

        root.getChildren().add(BackgroundManager.fabricTexture());

        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.10), Pos.TOP_LEFT);
        addCornerSuit(root, "♥", Color.rgb(255, 140, 130, 0.10), Pos.TOP_RIGHT);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.10), Pos.BOTTOM_LEFT);
        addCornerSuit(root, "♦", Color.rgb(255, 140, 130, 0.10), Pos.BOTTOM_RIGHT);

        // 左右两侧半透明蓝色牌背剪影（纯装饰，鼠标穿透）
        Image backImg = backCache.computeIfAbsent(false, renderer::back);
        addCardDeco(root, backImg, 300, -14, Pos.CENTER_LEFT, new Insets(0, 0, 70, 44));
        addCardDeco(root, backImg, 300, 14, Pos.CENTER_RIGHT, new Insets(0, 44, 70, 0));

        // ================= 规则弹窗浮层（先建好供“游戏规则”按钮引用） =================
        StackPane rulesOverlay = new StackPane();
        rulesOverlay.setVisible(false);
        Region rShade = new Region();
        rShade.getStyleClass().add("modal-shade");
        rShade.setOnMouseClicked(ev -> rulesOverlay.setVisible(false));
        VBox rulesCard = buildRulesModalCard(rulesOverlay);
        StackPane.setAlignment(rulesCard, Pos.CENTER);
        StackPane.setMargin(rulesCard, new Insets(40));
        rulesOverlay.getChildren().addAll(rShade, rulesCard);

        // ================= 中央内容：花色条 → 主标题 → 三按钮 =================
        HBox suitStrip = new HBox(26);
        suitStrip.setAlignment(Pos.CENTER);
        addGradientSuit(suitStrip, "♠", Color.web("#f7e6b0"), Color.web("#b8942a"));
        addGradientSuit(suitStrip, "♥", Color.web("#ffc1ae"), Color.web("#c62b2b"));
        addGradientSuit(suitStrip, "♣", Color.web("#f7e6b0"), Color.web("#b8942a"));
        addGradientSuit(suitStrip, "♦", Color.web("#ffc1ae"), Color.web("#c62b2b"));
        suitStrip.setMouseTransparent(true);

        Text title = new Text("中南棋牌室");
        title.getStyleClass().add("menu-title");
        title.setMouseTransparent(true);

        Button start = new Button("开始游戏");
        start.getStyleClass().addAll("menu-btn", "menu-btn-start");
        start.setTooltip(new Tooltip("挑选一款游戏开始对局"));
        start.setOnAction(e -> {
            clickSound();
            if (loadingScene != null) {
                navEnterGame(stage, loadingScene);
            }
        });
        // 主按钮辉光挂在外层 holder（避免与按钮 CSS 内阴影互相覆盖）+ hover 放大反馈
        StackPane startHolder = new StackPane(start);
        addStartButtonGlow(startHolder, start);

        Button rules = new Button("游戏规则");
        rules.getStyleClass().addAll("menu-btn", "menu-btn-rules", "menu-btn-sub");
        rules.setTooltip(new Tooltip("查看玩法与规则说明"));
        rules.setOnAction(e -> rulesOverlay.setVisible(true));

        Button exit = new Button("退出游戏");
        exit.getStyleClass().addAll("menu-btn", "menu-btn-quit", "menu-btn-sub");
        exit.setTooltip(new Tooltip("退出中南棋牌室"));
        exit.setOnAction(e -> confirmExit(stage));

        HBox subActions = new HBox(22, rules, exit);
        subActions.setAlignment(Pos.CENTER);

        VBox buttons = new VBox(26, startHolder, subActions);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(30, suitStrip, title, buttons);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);
        StackPane.setMargin(center, new Insets(0, 0, 96, 0));
        root.getChildren().add(center);

        // 主标题金色呼吸 + 花色条错峰浮动（手游大厅式“活”标题）
        playTitleBreath(title);
        playSuitFloat(suitStrip);

        // 右下角技术备注（原副标题降噪后移到底角，不参与主流程）
        Label tech = new Label("基于 JavaFX 的桌面棋牌小游戏 · 课程项目");
        tech.getStyleClass().add("tech-note");
        tech.setMouseTransparent(true);
        StackPane.setAlignment(tech, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(tech, new Insets(0, 36, 18, 0));
        root.getChildren().add(tech);

        // ================= 右上角：设置齿轮 =================
        javafx.scene.shape.Path gear = gearShape();
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
        root.getChildren().add(gearBtn);

        // ================= 左上角：玩家区域（AvatarView 圆头像 + 昵称 + 称号等级 + 成长信息，点击进入个人中心） =================
        homeAvatar = new AvatarView(userAvatarGlyph, userNick, "", userLevel);
        // 首页玩家卡启用成长信息：等级称号徽章 + 经验进度 + 金币（在线状态只在选择游戏页展示）
        applyHomeGrowthInfo(homeAvatar);
        homeAvatar.getStyleClass().add("home-player-card");
        homeAvatar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        homeAvatar.setPickOnBounds(true);
        homeAvatar.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(homeAvatar, new Tooltip("查看个人主页"));
        homeAvatar.setOnMouseClicked(e -> {
            if (profileViewScene != null) {
                // 个人中心页在进入时自行 refresh()，这里无需额外同步数据
                navProfile(stage, profileViewScene);
            }
        });
        StackPane.setAlignment(homeAvatar, Pos.TOP_LEFT);
        StackPane.setMargin(homeAvatar, new Insets(22, 0, 0, 24));
        root.getChildren().add(homeAvatar);

        // ================= 设置浮层（主题 / 音量 / 亮度 / 开发者预览） =================
        StackPane overlay = new StackPane();
        overlay.setVisible(false);
        Region shade = new Region();
        shade.getStyleClass().add("modal-shade");
        shade.setOnMouseClicked(ev -> overlay.setVisible(false));

        // 亮度调节层：铺满但不挡鼠标，透明度由设置里的“亮度”控制
        Region dim = new Region();
        dim.getStyleClass().add("brightness-shade");
        dim.setOpacity(0.0);
        dim.setMouseTransparent(true);

        HBox holder = new HBox();
        holder.setAlignment(Pos.TOP_RIGHT);
        holder.setPadding(new Insets(84, 26, 26, 26));
        holder.getChildren().add(buildSettingsPanel(themeShade, deckScene, stage, overlay, dim));

        overlay.getChildren().addAll(shade, holder);
        gearBtn.setOnAction(e -> {
            clickSound();
            overlay.setVisible(!overlay.isVisible());
        });

        // ================= 底部功能入口：游戏大厅 / 个人信息 / 设置 =================
        Button hallEntry = lobbyEntry("🏛", "游戏大厅", () -> {
            if (gameChoiceScene != null) {
                navZoom(stage, gameChoiceScene);
            }
        });
        // 「个人信息」的名字已归个人中心页所有，这里明确为"编辑资料"，避免两个入口语义混淆
        Button profileEntry = lobbyEntry("👤", "编辑资料", () -> {
            if (profileScene != null) {
                navProfile(stage, profileScene);
            }
        });
        // 阶段 20：「设置」进入完整的设置中心页（齿轮浮层保留为快捷面板）
        Button settingsEntry = lobbyEntry("⚙", "设置", () -> {
            if (settingsScene != null) {
                navFade(stage, settingsScene);
            }
        });
        HBox bottomBar = new HBox(18, hallEntry, profileEntry, settingsEntry);
        bottomBar.getStyleClass().add("lobby-bottom-bar");
        bottomBar.setAlignment(Pos.CENTER);
        // StackPane 会拉伸可缩放子节点铺满场景；锁定为内容尺寸后 BOTTOM_CENTER 才能把入口栏压到底部
        bottomBar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(bottomBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bottomBar, new Insets(0, 0, 24, 0));
        root.getChildren().add(bottomBar);

        root.getChildren().addAll(overlay, dim, rulesOverlay);

        // ================= 入场动画：首次显示与每次回到主页时依次淡入 =================
        final javafx.scene.Node[] intro = {suitStrip, title, start, rules, exit};
        stage.showingProperty().addListener((o, a, now) -> {
            if (now) {
                playMenuIntro(intro);
            }
        });
        stage.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == homeScene && stage.isShowing()) {
                // 回到首页时刷新玩家卡：经验 / 等级可能已在牌局结算里增长
                refreshUserCard();
                playMenuIntro(intro);
            }
        });

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        return scene;
    }

    // ============================================================= 大厅视觉辅助

    /** 首页底部功能入口：字形图标 + 文案的药丸按钮。 */
    private static Button lobbyEntry(String icon, String text, Runnable action) {
        Text glyphIcon = new Text(icon);
        glyphIcon.getStyleClass().add("entry-glyph");
        glyphIcon.setFont(Font.font("Segoe UI Emoji", 18));
        Button btn = new Button(text);
        btn.setGraphic(glyphIcon);
        btn.getStyleClass().add("lobby-entry");
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setOnAction(e -> {
            clickSound();
            action.run();
        });
        return btn;
    }

    /**
     * “开始游戏”主按钮的手游化反馈：
     * 外层 holder 承载金色呼吸辉光（不动按钮自身的 CSS 内阴影），
     * 按钮自身在 hover 时 1.05 倍放大、移出时回弹（180ms，即时但不打扰）。
     */
    private static void addStartButtonGlow(StackPane holder, Button start) {
        var glow = new javafx.scene.effect.DropShadow(javafx.scene.effect.BlurType.GAUSSIAN,
                Color.rgb(255, 200, 80, 0.42), 24, 0.30, 0, 0);
        holder.setEffect(glow);
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 22, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(), Color.rgb(255, 200, 80, 0.40), Interpolator.EASE_BOTH)),
                new KeyFrame(DesignTokens.ANIM_GLOW,
                        new KeyValue(glow.radiusProperty(), 40, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(), Color.rgb(255, 214, 110, 0.78), Interpolator.EASE_BOTH)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        ScaleTransition hoverIn = new ScaleTransition(DesignTokens.ANIM_HOVER, start);
        hoverIn.setToX(1.05);
        hoverIn.setToY(1.05);
        ScaleTransition hoverOut = new ScaleTransition(DesignTokens.ANIM_HOVER, start);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);
        start.hoverProperty().addListener((o, wasHover, nowHover) -> {
            if (nowHover) {
                hoverOut.stop();
                hoverIn.playFromStart();
            } else {
                hoverIn.stop();
                hoverOut.playFromStart();
            }
        });
    }

    /** 主标题金色呼吸：极轻微缩放（1.0 ↔ 1.03），模拟金字光晕脉动。 */
    private static void playTitleBreath(Text title) {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(title.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(title.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(DesignTokens.ANIM_PULSE_SLOW,
                        new KeyValue(title.scaleXProperty(), 1.03, Interpolator.EASE_BOTH),
                        new KeyValue(title.scaleYProperty(), 1.03, Interpolator.EASE_BOTH)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ============================================================= 主页辅助组件

    /** 在 StackPane 角落放一张半透明旋转的牌背装饰。 */
    private static void addCardDeco(StackPane root, Image img, double height,
                                    double rotate, Pos pos, Insets margin) {
        ImageView iv = new ImageView(img);
        iv.setFitHeight(height);
        iv.setPreserveRatio(true);
        iv.setRotate(rotate);
        iv.setOpacity(0.10);
        iv.setMouseTransparent(true);
        iv.setSmooth(true);
        StackPane.setAlignment(iv, pos);
        StackPane.setMargin(iv, margin);
        root.getChildren().add(iv);
    }

    /** 向容器追加一个带柔和纵向渐变的扑克花色字符。 */
    private static void addGradientSuit(HBox strip, String glyph, Color top, Color bottom) {
        Text t = new Text(glyph);
        t.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 54));
        t.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        t.setMouseTransparent(true);
        strip.getChildren().add(t);
    }

    /** 花色图标轻微上下浮动，错峰延迟避免整齐划一。 */
    private static void playSuitFloat(HBox strip) {
        double delay = 0;
        for (javafx.scene.Node n : strip.getChildren()) {
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(n.translateYProperty(), 0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(1800), new KeyValue(n.translateYProperty(), -6, Interpolator.EASE_BOTH)));
            tl.setDelay(Duration.millis(delay));
            tl.setAutoReverse(true);
            tl.setCycleCount(Timeline.INDEFINITE);
            tl.play();
            delay += 320;
        }
    }

    /** 菜单内容依次淡入：标题、按钮错峰出现。 */
    private static void playMenuIntro(javafx.scene.Node... nodes) {
        double delay = 0;
        for (javafx.scene.Node n : nodes) {
            n.setOpacity(0.0);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(n.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(420), new KeyValue(n.opacityProperty(), 1.0, Interpolator.EASE_OUT)));
            tl.setDelay(Duration.millis(delay));
            tl.play();
            delay += 130;
        }
    }

    /** 页面内容错峰淡入并轻微上浮：切换到新页面后自然入场，避免硬切后内容瞬间弹出。 */
    private static void playRiseIn(javafx.scene.Node... nodes) {
        double delay = 0;
        for (javafx.scene.Node n : nodes) {
            n.setOpacity(0.0);
            n.setTranslateY(16);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(n.opacityProperty(), 0.0),
                            new KeyValue(n.translateYProperty(), 16, Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.millis(460),
                            new KeyValue(n.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                            new KeyValue(n.translateYProperty(), 0, Interpolator.EASE_OUT)));
            tl.setDelay(Duration.millis(delay));
            tl.play();
            delay += 110;
        }
    }

    /** 构建主页“游戏规则”弹窗卡片：页签切换跑得快 / 骗子酒馆，正文可滚动。 */
    private static VBox buildRulesModalCard(StackPane overlay) {
        VBox card = new VBox(12);
        card.getStyleClass().add("rules-modal-card");
        card.setPrefWidth(980);
        card.setMaxWidth(1040);
        card.setMaxHeight(920);

        Label head = new Label("游戏规则");
        head.getStyleClass().add("rules-modal-title");
        Button close = new Button("×");
        close.getStyleClass().add("icon-close");
        close.setTooltip(new Tooltip("关闭"));
        close.setOnAction(e -> overlay.setVisible(false));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox topRow = new HBox(head, gap, close);
        topRow.setAlignment(Pos.CENTER_LEFT);

        ToggleButton runTab = new ToggleButton("湖南跑得快");
        ToggleButton liarTab = new ToggleButton("骗子酒馆");
        runTab.getStyleClass().add("rules-tab");
        liarTab.getStyleClass().add("rules-tab");
        ToggleGroup tabs = new ToggleGroup();
        runTab.setToggleGroup(tabs);
        liarTab.setToggleGroup(tabs);
        runTab.setSelected(true);

        Label tabHint = new Label("点击弹窗外部或 × 关闭");
        tabHint.getStyleClass().add("settings-note");
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        HBox tabRow = new HBox(10, runTab, liarTab, gap2, tabHint);
        tabRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroller = new ScrollPane();
        scroller.getStyleClass().add("rules-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setContent(renderRulesBody(RUN_RULES_MD));
        VBox.setVgrow(scroller, Priority.ALWAYS);

        tabs.selectedToggleProperty().addListener((o, oldT, newT) ->
                scroller.setContent(renderRulesBody(newT == liarTab ? LIAR_RULES_MD : RUN_RULES_MD)));

        card.getChildren().addAll(topRow, tabRow, scroller);
        return card;
    }

    /** 退出前二次确认，防止误触。 */
    private static void confirmExit(Stage stage) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(stage);
        a.setTitle("退出确认");
        a.setHeaderText("确定要退出游戏吗？");
        a.setContentText("离开牌桌后，本局进度不会被保存。");
        ButtonType ok = new ButtonType("确定退出");
        ButtonType cancel = new ButtonType("再玩一会");
        a.getButtonTypes().setAll(ok, cancel);
        a.showAndWait().filter(b -> b == ok).ifPresent(b -> Platform.exit());
    }

    // ============================================================= 游戏选择界面

    /**
     * 构建加载动画页：点“开始游戏”后短暂展示，再进入“选择游戏”页。
     * 动画 = 三点错峰波浪跳动 + 金色进度条从左填满到右（确定式）+ “正在进入牌桌”文案，
     * 节奏为：卡片淡入 → 进度条满格 → 短暂停留 → 淡出切场（约 1.65 秒）；
     * 切到“选择游戏”页后由该页内容错峰淡入承接，避免硬切后内容瞬间弹出。
     */
    private Scene buildLoadingScene(Stage stage) {
        StackPane root = new StackPane();

        // 背景：与其它页一致的国风山水层（BackgroundManager 统一构建）
        root.getChildren().add(BackgroundManager.createLoadingBackground().root());

        // 中央加载卡片：玻璃质感深色圆角面板
        StackPane card = new StackPane();
        card.getStyleClass().add("loading-card");
        card.setMouseTransparent(true);

        // 跳动的三个金点
        HBox dots = new HBox(14);
        dots.setAlignment(Pos.CENTER);
        dots.setMouseTransparent(true);
        List<Label> dotList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Label d = new Label("●");
            d.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 18));
            d.setTextFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fff3c4")),
                    new Stop(0.55, Color.web("#e8c25e")),
                    new Stop(1, Color.web("#b07f1e"))));
            d.setMouseTransparent(true);
            dotList.add(d);
            dots.getChildren().add(d);
        }

        // 进度条容器：固定宽高，确定式填充（高亮条从左填满到右）
        double barW = 280;
        double barH = 8;
        StackPane barTrack = new StackPane();
        barTrack.setPrefSize(barW, barH);
        barTrack.setMaxSize(barW, barH);
        barTrack.setMinSize(barW, barH);
        barTrack.getStyleClass().add("loading-bar-track");
        barTrack.setMouseTransparent(true);
        // 圆角裁剪：填充条与光晕只在轨道内显示，不溢出两端
        Rectangle barClip = new Rectangle(barW, barH);
        barClip.setArcWidth(9);
        barClip.setArcHeight(9);
        barTrack.setClip(barClip);
        Region barFill = new Region();
        barFill.getStyleClass().add("loading-bar-fill");
        barFill.setMouseTransparent(true);
        barFill.setMinHeight(6);
        barFill.setPrefHeight(6);
        barFill.setMaxHeight(6);
        // 宽度由入场动画 0 -> barW 驱动，呈“加载到满格”的确定式进度
        barFill.setMinWidth(0);
        barFill.setPrefWidth(barW);
        barFill.setMaxWidth(0);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        barTrack.getChildren().add(barFill);

        Label tip = new Label("正在进入牌桌");
        tip.getStyleClass().add("loading-tip");
        tip.setMouseTransparent(true);

        VBox box = new VBox(22, dots, barTrack, tip);
        box.setAlignment(Pos.CENTER);
        card.getChildren().add(box);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        // 三点跳动：合并到单条时间轴，统一 540ms 周期、峰值间隔 180ms，
        // 形成稳定连续的波浪节奏（原来三条独立时间轴周期不同，相位会漂移、节奏忽快忽慢）
        Timeline dotsTl = new Timeline();
        double wavePeriod = 540;
        for (int i = 0; i < dotList.size(); i++) {
            Label d = dotList.get(i);
            double base = i * 180;
            dotsTl.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(base), new KeyValue(d.translateYProperty(), 0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(base + 90), new KeyValue(d.translateYProperty(), -8, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(base + 180), new KeyValue(d.translateYProperty(), 0, Interpolator.EASE_BOTH)));
        }
        // 末尾锚点：把周期拉齐到 wavePeriod，末点回落处即下一循环起点，首尾无缝
        dotsTl.getKeyFrames().add(new KeyFrame(Duration.millis(wavePeriod),
                new KeyValue(dotList.get(dotList.size() - 1).translateYProperty(), 0)));
        dotsTl.setCycleCount(Timeline.INDEFINITE);
        dotsTl.play();

        // 主动画：卡片淡入 → 进度条填满（确定式）→ 满格短暂停留 → 淡出切场，约 1.65s
        card.setOpacity(0.0);
        Timeline intro = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(card.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(300), new KeyValue(card.opacityProperty(), 1.0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(350), new KeyValue(barFill.maxWidthProperty(), 0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1150), new KeyValue(barFill.maxWidthProperty(), barW, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1300), new KeyValue(card.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1650), new KeyValue(card.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        intro.setOnFinished(e -> {
            dotsTl.stop();
            if (gameChoiceScene != null) {
                // 加载页演完 → 进入选择游戏：与大厅入口保持同一套“进入下一流程”的缩放转场
                navZoom(stage, gameChoiceScene);
            }
        });

        // 仅在本页显示时启动动画，切走后停止；再次进入时复位并从头播放
        stage.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS == scene) {
                card.setOpacity(0.0);
                barFill.setMaxWidth(0);
                for (Label d : dotList) d.setTranslateY(0);
                dotsTl.playFromStart();
                intro.playFromStart();
            } else if (oldS == scene) {
                intro.stop();
                dotsTl.stop();
            }
        });
        return scene;
    }

    /** 构建“选择游戏”页：提供湖南跑得快 / 骗子酒馆两种玩法入口。 */
    private Scene buildGameChoiceScene(Stage stage) {
        StackPane root = new StackPane();

        // 背景层（山水 Canvas + 主题遮罩）与粒子层均由 BackgroundManager 统一构建
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        root.getChildren().add(bgLayers.root());

        // 低强度动态粒子：星光缓慢浮动 + 纸牌碎片轻轻飘落（鼠标穿透，离开本页自动暂停）
        ParticleField particles = bgLayers.particles();

        // 四角花色改成淡淡的暗纹底（低透明度 + 微模糊），营造氛围但不抢焦点
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        Label title = new Label("选择游戏");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("请选择要游玩的模式");
        sub.getStyleClass().add("game-choice-sub");

        Button runFast = gameOption("♠", "湖南跑得快",
                "三人 16 张经典玩法\n先出完手牌者获胜",
                "扑克 · 竞速出牌", true, () -> enterModeChoice(stage, "♠ 湖南跑得快", SelectedGame.PDK));
        Button liarBar = gameOption("🃏", "骗子酒馆",
                "扑克与骰子模式\n谎言与质疑并存，活到最后即胜",
                "聚会 · 心理博弈", false, () -> enterModeChoice(stage, "🃏 骗子酒馆", SelectedGame.LIAR));

        HBox options = new HBox(28, runFast, liarBar);
        options.setAlignment(Pos.CENTER);
        options.setPadding(new Insets(0, 48, 0, 48));
        // 两张卡片平分可用宽度（宽度上下限由 CSS min/pref/max-width 约束），窗口缩小时自动收窄不溢出
        HBox.setHgrow(runFast, Priority.ALWAYS);
        HBox.setHgrow(liarBar, Priority.ALWAYS);

        Button back = new Button("返回主页 →");
        back.addEventHandler(javafx.event.ActionEvent.ACTION, e -> clickSound());
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (homeScene != null) {
                navFade(stage, homeScene);
            }
        });
        StackPane.setAlignment(back, Pos.TOP_RIGHT);
        StackPane.setMargin(back, new Insets(22, 24, 0, 0));

        // 左上角玩家卡片：复用 AvatarView（圆头像 + 昵称 + 等级 + 在线状态点）
        choiceAvatar = new AvatarView(userAvatarGlyph, userNick, userStat, userLevel);
        choiceAvatar.getStyleClass().add("user-card");
        choiceAvatar.setAlignment(Pos.CENTER_LEFT);
        // 让 HBox 按内容计算尺寸，否则默认会撑满 StackPane，
        // .user-card 的半透明深绿背景就会铺满整个屏幕，造成“亮度下降”
        choiceAvatar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(choiceAvatar, Pos.TOP_LEFT);
        StackPane.setMargin(choiceAvatar, new Insets(22, 0, 0, 24));
        // 点击头像/昵称卡片进入个人信息编辑页
        choiceAvatar.setPickOnBounds(true);
        choiceAvatar.setCursor(javafx.scene.Cursor.HAND);
        choiceAvatar.setOnMouseClicked(e -> {
            if (profileScene != null) {
                navProfile(stage, profileScene);
            }
        });
        refreshUserCard();

        VBox center = new VBox(34, title, sub, options);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        root.getChildren().addAll(center, back, choiceAvatar);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源；
        // 进入本页时让头像/返回/标题/卡片错峰淡入上浮，承接加载页淡出，避免内容瞬间弹出
        final javafx.scene.Node[] choiceIntro = { choiceAvatar, back, title, sub, options };
        stage.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS == scene) {
                particles.play();
                playRiseIn(choiceIntro);
            } else if (oldS == scene) {
                particles.stop();
            }
        });
        return scene;
    }

    /** 生成一个游戏入口卡片按钮：金属圆徽图标 + 名称 + 简介 + 圆角标签；featured 为推荐选中态。 */
    private Button gameOption(String icon, String name, String desc, String tag,
                              boolean featured, Runnable onPlay) {
        // 图标：深色金属圆徽 + 内高光描边；花色字符用金色渐变，emoji 保留本色加暖光
        StackPane badge = new StackPane();
        badge.getStyleClass().add("game-icon-badge");
        Text glyph = new Text(icon);
        glyph.getStyleClass().add("game-icon-glyph");
        boolean suitGlyph = icon.length() == 1 && "♠♥♣♦".indexOf(icon) >= 0;
        if (suitGlyph) {
            glyph.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 48));
            glyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fff3c4")),
                    new Stop(0.55, Color.web("#e8c25e")),
                    new Stop(1, Color.web("#b07f1e"))));
        } else {
            glyph.setFont(Font.font("Segoe UI Emoji", 46));
            glyph.setEffect(new DropShadow(8, Color.rgb(255, 210, 90, 0.35)));
        }
        badge.getChildren().add(glyph);

        Label nameL = new Label(name);
        nameL.getStyleClass().add("game-option-title");
        Label descL = new Label(desc);
        descL.getStyleClass().add("game-option-desc");
        descL.setTextAlignment(TextAlignment.CENTER);
        Label tagL = new Label(tag);
        tagL.getStyleClass().add("game-option-tag");

        VBox box = new VBox(14, badge, nameL, descL, tagL);
        box.setAlignment(Pos.CENTER);
        // 内边距与 CSS 卡片高度（min 296）匹配：内容总高 ≈ 283，小于卡片内容区 292，
        // 保证圆徽/标题/简介/标签都完整落在圆角卡片内，不贴边、不溢出
        box.setPadding(new Insets(26, 34, 26, 34));

        // 细微磨砂噪点叠层，给卡片增加牌面磨砂质感
        Region frost = new Region();
        frost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        frost.setMouseTransparent(true);
        frost.setBackground(new Background(new BackgroundFill(
                frostPattern(), new CornerRadii(20), new Insets(2))));

        StackPane content = new StackPane(frost, box);

        Button card = new Button();
        card.getStyleClass().add("game-option");
        card.setGraphic(content);
        card.setOnAction(e -> onPlay.run());
        styleGameCard(card, featured);
        return card;
    }

    /**
     * 配置游戏入口卡片的底色 / 边框 / 阴影与悬浮动效。
     * 两张卡片常态完全一致（浅灰边框、压暗、投影低调）；
     * 仅当鼠标悬浮时，被选中的卡片才缓慢过渡为金色高亮（金边、泛金底色、暖光、轻微上浮）。
     */
    private static void styleGameCard(Button card, boolean featured) {
        CornerRadii radii = new CornerRadii(22);
        javafx.scene.layout.BorderWidths strokeWidth =
                new javafx.scene.layout.BorderWidths(2);

        // 常态：低调深色（两张卡片一致）
        Color topIdle = Color.rgb(255, 255, 255, 0.06);
        Color botIdle = Color.rgb(255, 255, 255, 0.025);
        Color borderIdle = Color.rgb(225, 230, 236, 0.30);
        Color glowIdle = Color.rgb(0, 0, 0, 0.35);
        // 悬浮：金色高亮
        Color topHover = Color.rgb(255, 246, 205, 0.30);
        Color botHover = Color.rgb(255, 213, 79, 0.17);
        Color borderHover = Color.rgb(255, 230, 160, 0.98);
        Color glowHover = Color.rgb(255, 206, 90, 0.60);

        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(6);
        shadow.setRadius(18);
        shadow.setColor(glowIdle);
        card.setEffect(shadow);
        card.setBackground(cardBackground(topIdle, botIdle, radii));
        card.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(borderIdle, javafx.scene.layout.BorderStrokeStyle.SOLID, radii, strokeWidth)));

        // 用 0→1 的进度量驱动颜色插值，实现“缓慢变亮 / 上浮”的平滑 hover 动效
        javafx.beans.property.DoubleProperty frac = new javafx.beans.property.SimpleDoubleProperty(0);
        frac.addListener((o, oldV, v) -> {
            double t = v.doubleValue();
            Color top = (Color) Interpolator.EASE_BOTH.interpolate(topIdle, topHover, t);
            Color bot = (Color) Interpolator.EASE_BOTH.interpolate(botIdle, botHover, t);
            Color border = (Color) Interpolator.EASE_BOTH.interpolate(borderIdle, borderHover, t);
            Color glow = (Color) Interpolator.EASE_BOTH.interpolate(glowIdle, glowHover, t);
            card.setBackground(cardBackground(top, bot, radii));
            card.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(border, javafx.scene.layout.BorderStrokeStyle.SOLID, radii, strokeWidth)));
            shadow.setColor(glow);
            shadow.setRadius(18 + t * 14);
            card.setTranslateY(-7 * t);
        });

        Timeline hoverIn = new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(frac, 1.0, Interpolator.EASE_OUT)));
        Timeline hoverOut = new Timeline(new KeyFrame(Duration.millis(320),
                new KeyValue(frac, 0.0, Interpolator.EASE_IN)));
        card.hoverProperty().addListener((o, wasHover, nowHover) -> {
            if (nowHover) {
                hoverOut.stop();
                hoverIn.playFromStart();
            } else {
                hoverIn.stop();
                hoverOut.playFromStart();
            }
        });
    }

    /** 按上下两色生成卡片纵向渐变背景。 */
    private static Background cardBackground(Color top, Color bottom, CornerRadii radii) {
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
        return new Background(new BackgroundFill(grad, radii, Insets.EMPTY));
    }

    /** 卡片磨砂质感的细密噪点纹理（懒加载，全局复用）。 */
    private static ImagePattern FROST_PATTERN;

    private static ImagePattern frostPattern() {
        if (FROST_PATTERN != null) {
            return FROST_PATTERN;
        }
        int size = 64;
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        Random rnd = new Random(777L);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double v = rnd.nextDouble();
                Color c;
                if (v < 0.10) {
                    c = Color.rgb(255, 255, 255, 0.05 + rnd.nextDouble() * 0.06);
                } else if (v < 0.16) {
                    c = Color.rgb(20, 24, 20, 0.05 + rnd.nextDouble() * 0.05);
                } else {
                    c = Color.TRANSPARENT;
                }
                pw.setColor(x, y, c);
            }
        }
        FROST_PATTERN = new ImagePattern(img, 0, 0, size, size, false);
        return FROST_PATTERN;
    }

    // ============================================================= 玩法模式选择界面

    /** 从“选择游戏”进入“玩法模式”页，并把顶部徽标更新为所选游戏。 */
    private void enterModeChoice(Stage stage, String game, SelectedGame which) {
        this.selectedGame = which;
        modeBadge.setText(game);
        if (modeChoiceScene != null) {
            navZoom(stage, modeChoiceScene);
        }
    }

    /** 构建“玩法模式”页：针对所选游戏提供本地人机 / 局域网联机两种模式。 */
    private Scene buildModeChoiceScene(Stage stage) {
        StackPane root = new StackPane();

        // 背景层与粒子层由 BackgroundManager 统一构建（与“选择游戏”页一致）
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        root.getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();

        // 四角花色改成淡淡的暗纹底（低透明度 + 微模糊），与“选择游戏”页保持一致
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        // 顶部徽标：进入时由 enterModeChoice 更新为当前游戏名
        modeBadge.getStyleClass().add("game-choice-title");
        modeBadge.setMinHeight(76);
        Label sub = new Label("请选择玩法模式");
        sub.getStyleClass().add("game-choice-sub");

        Button vsCpu = gameOption("🤖", "本地人机",
                "与电脑 AI 同台对战\n无需联网，随时开局",
                "单人 · 离线", false, () -> {
                    if (botDelay != null) { botDelay.stop(); botDelay = null; }
                    tableSelected.clear();
                    if (selectedGame == SelectedGame.PDK) {
                        enterPdkTable(stage);
                    } else {
                        enterLiarTable(stage);
                    }
                });
        Button lanPlay = gameOption("🌐", "局域网联机",
                "创建或加入局域网房间\n与身边好友同台竞技",
                "多人 · 联机", false, () -> navZoom(stage, buildLanLobbyScene(stage)));

        HBox options = new HBox(28, vsCpu, lanPlay);
        options.setAlignment(Pos.CENTER);
        options.setPadding(new Insets(0, 48, 0, 48));
        HBox.setHgrow(vsCpu, Priority.ALWAYS);
        HBox.setHgrow(lanPlay, Priority.ALWAYS);

        Button back = new Button("← 返回选择游戏");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (gameChoiceScene != null) {
                navFade(stage, gameChoiceScene);
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        VBox center = new VBox(30, modeBadge, sub, options);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        root.getChildren().addAll(center, back);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源
        stage.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS == scene) {
                particles.play();
            } else if (oldS == scene) {
                particles.stop();
            }
        });
        return scene;
    }

    // ============================================================= 游戏牌桌（本地人机 + 局域网联机）

    /**
     * 为“会被反复重建的临时场景”（牌桌 / 联机大厅）挂粒子层启停监听。
     *
     * <p>大厅各页只构建一次，其监听器数量恒定；但牌桌与联机大厅每次进入 / 重新开始都会重建。
     * 若直接 {@code stage.sceneProperty().addListener(...)}，每次重建都会多留一个监听器，
     * 而监听器强引用整张旧场景（含背景 Canvas、粒子层与全部牌节点），
     * 使旧牌桌无法被 GC —— 长局数下监听器与场景会持续累积。
     * 因此这里只保留“最新一张临时场景”的监听：挂新的之前先注销旧的，
     * 并停掉旧场景的粒子层，避免遗留一直在跑的 AnimationTimer。
     */
    private void bindTransientParticles(Stage stage, Scene scene, ParticleField particles) {
        if (transientParticleListener != null) {
            if (transientParticleField != null) {
                transientParticleField.stop();
            }
            stage.sceneProperty().removeListener(transientParticleListener);
        }
        ChangeListener<Scene> listener = (o, oldS, newS) -> {
            if (newS == scene) {
                particles.play();
            } else if (oldS == scene) {
                particles.stop();
            }
        };
        transientParticleField = particles;
        transientParticleListener = listener;
        stage.sceneProperty().addListener(listener);
    }

    /** 把牌桌主体包成 P4 国风场景：山水背景 + 粒子 + 四角花色水印 + 返回按钮，挂 app.css。 */
    private Scene wrapTableScene(Stage stage, javafx.scene.layout.Pane table, String backLabel) {
        StackPane root = new StackPane();
        // 牌桌背景（当前与大厅同源，后续可替换为绒布主题）与粒子层由 BackgroundManager 统一构建
        BackgroundManager.Background bgLayers = BackgroundManager.createGameBackground();
        root.getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        StackPane.setAlignment(table, Pos.CENTER);
        StackPane.setMargin(table, new Insets(28));
        root.getChildren().add(table);

        Button back = new Button(backLabel);
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (botDelay != null) { botDelay.stop(); botDelay = null; }
            shutdownLan();
            navReturnLobby(stage, modeChoiceScene);
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));
        root.getChildren().add(back);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        bindTransientParticles(stage, scene, particles);
        return scene;
    }

    /**
     * 对局结束统一结算入口：在当前牌桌上覆盖一层结算遮罩，按胜/负播放不同主题的退场动画。
     * 节奏：遮罩淡入 → 结算卡片弹入（缩放+上浮，徽标轻微回弹）→ “重新开始 / 返回选择游戏”按钮淡入；
     * 胜利时标题带轻微呼吸光感，失败时保持沉稳。按钮在入场动画结束后才可点击。
     *
     * @param onRestart “重新开始”动作（由调用方传入重建对应桌台的逻辑）
     */
    private void showGameResult(Stage stage, boolean win, String subtitle, Runnable onRestart) {
        showGameResult(stage, win, subtitle, onRestart, null);
    }

    /**
     * 对局结束统一结算入口（可携带附加展示内容）。
     *
     * @param extra 结算卡片内的附加节点（跑得快传成长反馈面板），null 表示无
     */
    private void showGameResult(Stage stage, boolean win, String subtitle, Runnable onRestart,
                                javafx.scene.Node extra) {
        if (resultShown) return;
        resultShown = true;
        StackPane root = (StackPane) stage.getScene().getRoot();

        // 胜利/失败全屏庆祝效果（附加内容随副标题之后淡入）
        WinCelebration celebration = new WinCelebration(win, subtitle, extra);

        // 操作按钮行（延迟显示）
        Button restart = new Button("重新开始");
        restart.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-restart");
        restart.setDisable(true);
        Button back = new Button("返回选择游戏");
        back.addEventHandler(javafx.event.ActionEvent.ACTION, e -> clickSound());
        back.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-back");
        back.setDisable(true);
        HBox btnRow = new HBox(22, restart, back);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setOpacity(0.0);
        btnRow.setTranslateY(10);
        StackPane.setAlignment(btnRow, Pos.BOTTOM_CENTER);
        StackPane.setMargin(btnRow, new Insets(0, 0, 60, 0));
        celebration.getChildren().add(btnRow);

        root.getChildren().add(celebration);

        // 阶段 22：结算表现动画（胜负动画 / 金币飞入 / 升级光环 / 提示条），统一走 GameAnimationService
        playSettlementAnimations(root, celebration, win, extra);
        // 阶段 22：结算按钮同样接入点击缩放反馈
        GameAnimationService.getInstance().installButtonFeedback(root);

        // 按钮延迟 1.1s 后淡入可用
        Timeline btnIn = new Timeline(
                new KeyFrame(Duration.millis(1100),
                        new KeyValue(btnRow.opacityProperty(), 0.0),
                        new KeyValue(btnRow.translateYProperty(), 10, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(btnRow.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(btnRow.translateYProperty(), 0, Interpolator.EASE_OUT)));
        btnIn.setOnFinished(e -> {
            restart.setDisable(false);
            back.setDisable(false);
        });
        btnIn.play();

        restart.setOnAction(e -> {
            if (botDelay != null) { botDelay.stop(); botDelay = null; }
            celebration.stop();
            root.getChildren().remove(celebration);
            onRestart.run();
        });
        back.setOnAction(e -> {
            if (botDelay != null) { botDelay.stop(); botDelay = null; }
            celebration.stop();
            root.getChildren().remove(celebration);
            navWinToLobby(stage, gameChoiceScene);
        });
    }

    /**
     * 阶段 22：结算表现动画集合，统一由 {@link GameAnimationService} 驱动。
     *
     * <p>只做表现，不涉及任何结算数据：
     * <ul>
     *   <li>胜利 / 失败动画（星光、辉光脉冲 / 抖动）；</li>
     *   <li>金币飞入（从成长面板飞向顶部金币栏）；</li>
     *   <li>升级光环（本局发生升级时）；</li>
     *   <li>合并提示条（金币 / 经验 / 升级 / 成就）。</li>
     * </ul>
     *
     * <p>动画总开关关闭时，服务内部全部静默跳过，结算卡片仍以静态结果展示。
     *
     * @param root        当前场景根（StackPane）
     * @param celebration 结算庆祝层
     * @param win         本局是否获胜
     * @param extra       附加内容（跑得快的成长反馈面板），可为 null（如暗牌玩法）
     */
    private void playSettlementAnimations(StackPane root, WinCelebration celebration,
                                          boolean win, javafx.scene.Node extra) {
        final GameAnimationService anim = GameAnimationService.getInstance();
        if (win) {
            anim.playWinAnimation(root, celebration);
        } else {
            anim.playLoseAnimation(celebration);
        }
        if (extra == null) {
            return;
        }
        final boolean upgraded = pdkGrowthForResult != null && pdkGrowthForResult.upgraded();
        // 等结算卡片完成一次布局，再播金币飞入 / 升级光环 / 提示条（避免拿到 0 尺寸坐标）
        PauseTransition settleDelay = new PauseTransition(Duration.millis(140));
        settleDelay.setOnFinished(e -> {
            javafx.scene.Node coinTarget = pdkCoinBar != null ? pdkCoinBar : celebration;
            anim.playCoinAnimation(extra, coinTarget, null);
            if (upgraded) {
                anim.playLevelUpAnimation(extra);
                // 阶段 22：升级 → 牌桌本人头像外围出现金色光环（持续 2 秒）
                if (pdkSeat1 != null) {
                    pdkSeat1.getAvatar().playLevelUpGlow();
                }
            }
            anim.showToast(celebration, settleToastText(upgraded));
        });
        settleDelay.play();
    }

    /** 组装结算提示条文案：金币 / 经验 / 升级 / 首个成就，合并为一条避免重叠。 */
    private String settleToastText(boolean upgraded) {
        StringBuilder msg = new StringBuilder();
        msg.append("＋").append(pdkGoldDeltaForResult).append(" 金币")
                .append(" · ＋").append(pdkExpGainForResult).append(" 经验");
        if (upgraded && pdkGrowthForResult != null) {
            msg.append(" · ✨ Level UP Lv.").append(pdkGrowthForResult.getOldLevel())
                    .append(" → Lv.").append(pdkGrowthForResult.getNewLevel());
        }
        if (!pdkAchievementsForResult.isEmpty()) {
            msg.append(" · 🏆 ").append(pdkAchievementsForResult.get(0).getTitle());
        }
        return msg.toString();
    }

    // ----------------------------- 跑得快（本地人机） -----------------------------

    /**
     * 统一金币规则：跑得快与骗子酒馆共用同一套金额（入场费 / 胜负奖励），
     * 且全部经 {@link CoinService} 读写 —— 余额即当前登录账号的金币数，
     * 切换账号后两个玩法看到与扣发的都是同一个数字。
     */
    private static final int GAME_ENTRY_COST = 50;
    /** 胜利奖励（金币）。 */
    private static final int GAME_WIN_REWARD = 100;
    /** 失败参与奖励（金币）。 */
    private static final int GAME_LOSS_REWARD = 20;
    /** 跑得快胜利经验（骗子酒馆不发经验）。 */
    private static final int PDK_WIN_EXP = 50;
    /** 跑得快失败经验。 */
    private static final int PDK_LOSS_EXP = 20;

    /**
     * 进入跑得快牌桌的统一起点：先扣入场费，再开局。
     *
     * <p>入场费是本局唯一的"进入成本"，经 {@link CoinService#costGold(int, String)} 扣除，
     * 扣款成功时余额与 {@code coin_log.json} 流水已同步落盘；余额不足时<b>不扣款、不建桌</b>，
     * 仅提示"金币不足"并留在原页面（返回 false 由调用方感知）。
     *
     * <p>首局与"重新开始"都走这里，因此每一局都会重新收取入场费，不会出现免费连开。
     *
     * @return 是否成功进入牌桌
     */
    private boolean enterPdkTable(Stage stage) {
        if (!coinService.costGold(GAME_ENTRY_COST, "跑得快入场")) {
            showInfo(stage, "金币不足，跑得快入场需 " + GAME_ENTRY_COST + " 金币");
            return false;
        }
        Scene table = buildPdkTableScene(stage);
        // 阶段 11：进入游戏时检测 FIRST_GAME（建桌时成就列表已清空，这里重新收集本局结果）
        pdkAchievementsForResult.addAll(achievementService.checkOnGameStart());
        navEnterGame(stage, table);
        return true;
    }

    /**
     * 跑得快本局结算：发放金币与经验、记录战绩，并保存成长反馈供结算卡片展示。
     *
     * <p><b>防重复结算：</b>{@link #pdkSettled} 是本局结算的唯一闸门 —— 第一个进入者置位并完成发放，
     * 之后无论 {@code refreshPdkTable} 再被触发多少次、结算界面被重复打开，都会直接返回，
     * 绝不重复加金币 / 加经验 / 记战绩。闸门在 {@link #buildPdkTableScene} 建桌时重置，只作用于"当前这一局"。
     *
     * <p>三类数据各走各的唯一入口：
     * <ul>
     *   <li>金币 → {@link CoinService}（写流水 + 落盘）；</li>
     *   <li>经验 / 等级 → {@link PlayerGrowthService#addExp(int)}，返回
     *       {@link PlayerGrowthService.LevelUpResult}（oldLevel / newLevel / upgradeCount /
     *       upgradeReward），存入本局反馈字段，由 {@link #showGameResult} 组装成结算信息展示；</li>
     *   <li>战绩 → {@link PlayerStatsService#recordGameResult(boolean)}
     *       （内部按胜负转发到 {@code recordWin()} / {@code recordLoss()}，更新胜场 / 负场 / 连胜，
     *       并落盘 {@code player.json}）。</li>
     * </ul>
     */
    private void settlePdk(boolean win) {
        if (pdkSettled) {
            return;
        }
        pdkSettled = true;

        int goldDelta;
        int expGain;
        if (win) {
            goldDelta = GAME_WIN_REWARD;
            expGain = PDK_WIN_EXP;
            coinService.addGold(goldDelta, "跑得快胜利奖励");
        } else {
            goldDelta = GAME_LOSS_REWARD;
            expGain = PDK_LOSS_EXP;
            coinService.addGold(goldDelta, "跑得快参与奖励");
        }
        // 加经验并按阈值自动升级；LevelUpResult 记录本次是否升级、升了几级、发了多少金币奖励
        PlayerGrowthService.LevelUpResult growth = growthService.addExp(expGain);

        // 战绩统计：一次调用按胜负记一局（胜场 / 负场 / 当前连胜 / 最高连胜），立即落盘 player.json
        statsService.recordGameResult(win);

        // 阶段 17：战绩明细。金币 / 经验都已结算完毕，把这一局原样记一条（胜 +100 / +50，负 +20 / +20），
        // 落盘 players/<username>_records.json；账号隔离由 GameRecordService 自行推导文件路径完成
        if (gameRecordService != null) {
            gameRecordService.addRecord(GameRecordService.GAME_PDK, win, goldDelta, expGain,
                    GameRecordService.OPPONENT_AI);
        }

        // 阶段 11：成就检测（胜利 → FIRST_WIN / WIN_STREAK_5；金币变化 → GOLD_10000；升级 → LEVEL_10/20）
        if (win) {
            pdkAchievementsForResult.addAll(achievementService.checkOnWin());
        }
        pdkAchievementsForResult.addAll(achievementService.checkOnGoldChange());
        pdkAchievementsForResult.addAll(achievementService.checkOnLevelUp());

        // 记录本局成长反馈（结算卡片读取；等级 / 经验在展示时实时取最新档案）
        pdkWinForResult = win;
        pdkGoldDeltaForResult = goldDelta;
        pdkExpGainForResult = expGain;
        pdkGrowthForResult = growth;

        if (pdkCoinBar != null) {
            pdkCoinBar.setCoins(coinService.getGold());
        }

        // 阶段 21：结算反馈音（胜利 → WIN，失败 → LOSE）
        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(win ? com.csu.pokergame.audio.SoundEffect.WIN
                        : com.csu.pokergame.audio.SoundEffect.LOSE);
    }

    /**
     * 组装跑得快结算成长反馈面板：读取本局反馈字段 + 结算后的最新档案（等级 / 经验）
     * 与等级称号，生成 {@link GrowthResultPanel} 交给结算卡片展示。
     *
     * <p>等级 / 经验 / 升级需求都在这里实时取最新值，因此面板展示的一定是结算后的状态。
     */
    private javafx.scene.Node buildPdkGrowthPanel() {
        PlayerProfile profile = playerManager.getProfile();
        int level = profile.getLevel();
        GrowthResultPanel growth = new GrowthResultPanel(
                pdkWinForResult,
                pdkGoldDeltaForResult,
                pdkExpGainForResult,
                level,
                growthService.getLevelTitle(level),
                growthService.getLevelBadge(level),
                profile.getExp(),
                growthService.expToNextLevel(level),
                pdkGrowthForResult);
        javafx.scene.Node achievements = buildAchievementUnlockNotice();
        if (achievements == null) {
            return growth;
        }
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, growth, achievements);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    /** 本局解锁的成就提示条（没有解锁任何成就时返回 null）。 */
    private javafx.scene.Node buildAchievementUnlockNotice() {
        if (pdkAchievementsForResult.isEmpty()) {
            return null;
        }
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(6);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        for (Achievement a : pdkAchievementsForResult) {
            Label label = new Label("🏆 成就解锁：" + a.getTitle() + "　奖励 +" + a.getRewardGold()
                    + " 金币 / +" + a.getRewardExp() + " 经验");
            label.getStyleClass().add("growth-result-achievement");
            box.getChildren().add(label);
        }
        return box;
    }

    /** 构建跑得快牌桌：SEAT_1 为玩家，SEAT_2/3 为 AI。 */
    private Scene buildPdkTableScene(Stage stage) {
        resultShown = false;
        pdkSettled = false;
        // 清空上一局的成长反馈
        pdkWinForResult = false;
        pdkGoldDeltaForResult = 0;
        pdkExpGainForResult = 0;
        pdkGrowthForResult = null;
        pdkAchievementsForResult.clear();
        tableSelected.clear();
        // 阶段 23：顶部信息栏的「第 N 局」为界面侧计数，每建一张新桌 +1（不入引擎/快照）
        pdkRoundNo++;
        pdkLastMover = null;
        pdkEngine = new PdkEngine(new Random(System.currentTimeMillis()));
        pdkEngine.start();
        pdkBot2 = new RuleBotController(new PdkBotPolicy());
        pdkBot3 = new RuleBotController(new PdkBotPolicy());

        BorderPane table = new BorderPane();
        table.getStyleClass().add("table-root");
        table.setPrefSize(960, 600);

        // ---------- 顶部：比赛信息栏 + AI 玩家区域（阶段 23） ----------
        pdkHeader = new PdkTableHeader();
        pdkHeader.setMode("本地人机");
        pdkHeader.setBaseScore(GAME_ENTRY_COST);
        pdkHeader.setRound(pdkRoundNo);
        pdkCoinBar = pdkHeader.getCoinBar();
        // 余额直接取自 CoinService（入场费扣完后显示实时余额）
        pdkCoinBar.setCoins(coinService.getGold());

        pdkSeat2 = new PdkPlayerSeat("♚", "西家", 8, false);
        pdkSeat3 = new PdkPlayerSeat("♝", "北家", 8, false);
        pdkSeatStrip = new HBox(30, pdkSeat2, pdkSeat3);
        pdkSeatStrip.getStyleClass().add("pdk-seat-strip");
        pdkSeatStrip.setAlignment(Pos.CENTER);

        VBox topBar = new VBox(10, pdkHeader, pdkSeatStrip);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 14, 8, 14));
        table.setTop(topBar);

        // ---------- 中央：玻璃牌桌 + 出牌区 + 状态提示卡（阶段 23） ----------
        pdkCenterBox = new PlayedCardsView();
        pdkCenterBox.getStyleClass().add("pdk-center");

        pdkStatus = new Label();
        pdkStatus.getStyleClass().addAll("table-hint", "pdk-status-card");
        pdkStatus.setMaxWidth(Double.MAX_VALUE);
        pdkStatus.setAlignment(Pos.CENTER);
        pdkStatus.setMinHeight(Region.USE_PREF_SIZE);

        VBox centerCol = new VBox(14, pdkCenterBox, pdkStatus);
        centerCol.setAlignment(Pos.CENTER);
        StackPane centerWrap = new StackPane(centerCol);
        centerWrap.getStyleClass().addAll("table-center-wrap", "pdk-table");
        table.setCenter(centerWrap);

        // ---------- 底部：玩家区域（头像 + 手牌 + 操作按钮，阶段 23） ----------
        pdkHandBox = new PdkHandView(tableSelected);
        pdkHandBox.getStyleClass().add("pdk-bottom");
        PdkActionBar actionBar = pdkHandBox.getActionBar();
        pdkPlayBtn = actionBar.getPlayButton();
        pdkPassBtn = actionBar.getPassButton();
        pdkPlayBtn.setOnAction(e -> onPdkPlay(stage));
        pdkPassBtn.setOnAction(e -> onPdkPass(stage));
        pdkSeat1 = pdkHandBox.getSeat();
        pdkSeat1.setLevel(userLevel);
        pdkSeat1.setPlayerName("你");
        table.setBottom(pdkHandBox);

        pdkLog = new ListView<>();
        pdkLog.getStyleClass().add("table-log");
        pdkLog.setPrefWidth(220);
        pdkLog.setMaxHeight(Double.MAX_VALUE);
        table.setRight(pdkLog);

        Scene scene = wrapTableScene(stage, table, "← 返回模式选择");
        refreshPdkTable(stage);
        return scene;
    }

    /** 刷新跑得快桌：手牌 / 桌面 / 状态 / 日志 / 按钮启用 / bot 调度。 */
    private void refreshPdkTable(Stage stage) {
        PdkSnapshot snap = (PdkSnapshot) pdkEngine.snapshotFor(PlayerId.SEAT_1);

        // 手牌：PdkHandView 统一管理（排序+选中+hover，卡牌为 PdkCardView）
        boolean myTurn = snap.currentPlayer() == PlayerId.SEAT_1
                && snap.phase() == GamePhase.PLAYING && snap.winner().isEmpty();
        pdkHandBox.setInteractive(myTurn);
        pdkHandBox.setMaxSelect(Integer.MAX_VALUE);
        pdkHandBox.setOnSelectionChange(sel -> updatePdkButtons(snap));
        pdkHandBox.setCards(snap.myHand());

        // 桌面：PlayedCardsView 显示最近一次出牌（标明出牌人）
        if (snap.lastMove().isPresent()) {
            var move = snap.lastMove().get();
            PlayerId mover = pdkLastMover == null ? snap.currentPlayer() : pdkLastMover;
            pdkCenterBox.setCards(move.cards(), seatName(mover));
        } else {
            pdkCenterBox.setCards(null, null);
        }

        // 顶部信息栏：关门 / 明牌辅助信息 + 各座位剩余牌数（阶段 23）
        if (pdkHeader != null) {
            StringBuilder aux = new StringBuilder();
            snap.closedDoorPlayers().forEach(p -> aux.append(seatName(p)).append("关门 "));
            if (snap.faceUpCard().isPresent()) {
                aux.append(" 明牌 ").append(snap.faceUpCard().get().display());
            }
            pdkHeader.setAuxText(aux.toString().trim());
            List<String> remaining = new ArrayList<>();
            for (PlayerId seat : PlayerId.values()) {
                Integer remain = snap.remainingCardCounts().get(seat);
                if (remain != null) {
                    remaining.add(seatName(seat) + " " + remain);
                }
            }
            pdkHeader.setRemaining(remaining);
        }

        // 状态提示卡：轮到谁出牌 / 已出牌型（半透明提示卡，不直接贴裸文本）
        pdkStatus.setText(pdkStatusText(snap));

        // 座位状态刷新
        java.util.Optional<PlayerId> winner = snap.winner();
        PdkPlayerSeat[] row = {pdkSeat1, pdkSeat2, pdkSeat3};
        for (int i = 0; i < row.length; i++) {
            PlayerId seat = i == 0 ? PlayerId.SEAT_1 : i == 1 ? PlayerId.SEAT_2 : PlayerId.SEAT_3;
            PdkPlayerSeat sv = row[i];
            Integer remain = snap.remainingCardCounts().get(seat);
            sv.setCardCount(remain == null ? -1 : remain);
            if (winner.isPresent()) {
                sv.setState(winner.get() == seat ? PdkPlayerSeat.State.WON : PdkPlayerSeat.State.LOST);
            } else if (seat == snap.currentPlayer() && snap.phase() == GamePhase.PLAYING) {
                sv.setState(seat == PlayerId.SEAT_1 ? PdkPlayerSeat.State.ACTIVE : PdkPlayerSeat.State.THINKING);
            } else {
                sv.setState(PdkPlayerSeat.State.WAITING);
            }
        }

        // 日志
        pdkLog.getItems().setAll(snap.publicEvents().stream()
                .skip(Math.max(0, snap.publicEvents().size() - 40))
                .toList());

        updatePdkButtons(snap);

        // 胜负：结算金币 / 经验（settlePdk 内部防重复），再播放战败/胜利退场动画，
        // 动画结束后提供“重新开始 / 返回选择游戏”；重新开始也走带入场费的 enterPdkTable。
        if (snap.winner().isPresent()) {
            boolean win = snap.winner().get() == PlayerId.SEAT_1;
            if (botDelay != null) { botDelay.stop(); botDelay = null; }
            String sub = win
                    ? "你率先出完手牌，本局获胜！"
                    : seatName(snap.winner().get()) + " 先出完手牌，本局惜败。";
            settlePdk(win);
            // 结算成长反馈：胜负 / 金币变化 / 获得经验 / 当前等级 / 经验进度 / 升级提示
            showGameResult(stage, win, sub, () -> enterPdkTable(stage), buildPdkGrowthPanel());
            return;
        }

        // bot 回合调度
        schedulePdkBot(stage);
    }

    private void updatePdkButtons(PdkSnapshot snap) {
        boolean myTurn = snap.currentPlayer() == PlayerId.SEAT_1
                && snap.phase() == GamePhase.PLAYING && snap.winner().isEmpty();
        if (!myTurn) {
            pdkPlayBtn.setDisable(true);
            pdkPassBtn.setDisable(true);
            return;
        }
        // 出牌：选中牌构成一个合法的 PlayPdkCards
        boolean canPlay = !tableSelected.isEmpty()
                && pdkEngine.legalCommands(PlayerId.SEAT_1).stream()
                        .anyMatch(c -> c instanceof PlayPdkCards p
                                && new HashSet<>(p.cards()).containsAll(tableSelected)
                                && tableSelected.containsAll(p.cards()));
        pdkPlayBtn.setDisable(!canPlay);
        // 不出：合法命令含 PassPdkTurn（能压时不可不出）
        pdkPassBtn.setDisable(pdkEngine.legalCommands(PlayerId.SEAT_1).stream()
                .noneMatch(c -> c instanceof PassPdkTurn));
    }

    private void onPdkPlay(Stage stage) {
        if (tableSelected.isEmpty()) {
            showInfo(stage, "请先选择要出的牌");
            return;
        }
        // 记录飞牌起点坐标（手牌节点场景坐标）
        List<double[]> flyFrom = new ArrayList<>();
        for (var card : tableSelected) {
            var node = pdkHandBox.getCardNode(card);
            if (node != null) {
                var b = node.localToScene(node.getBoundsInLocal());
                flyFrom.add(new double[]{b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2});
            }
        }
        PlayPdkCards cmd = new PlayPdkCards(List.copyOf(tableSelected));
        try {
            pdkEngine.apply(cmd);
        } catch (IllegalArgumentException ex) {
            showInfo(stage, "出牌不合法：" + ex.getMessage());
            return;
        }
        var playedCards = List.copyOf(tableSelected);
        tableSelected.clear();
        pdkLastMover = PlayerId.SEAT_1;
        refreshPdkTable(stage);
        // 飞牌动画：从手牌位置飞向桌面中央（阶段 22：统一走 GameAnimationService，受动画开关控制）
        if (!flyFrom.isEmpty()) {
            javafx.application.Platform.runLater(() -> GameAnimationService.getInstance()
                    .playCardAnimation(() -> pdkCenterBox.playFlyIn(playedCards, "你",
                            flyFrom.get(0)[0], flyFrom.get(0)[1], null), null));
        }
        // 阶段 23：出牌成功的小提示，显示牌型（顺子 / 三带一 / 炸弹 …）
        PdkSnapshot after = (PdkSnapshot) pdkEngine.snapshotFor(PlayerId.SEAT_1);
        after.lastMove().ifPresent(move ->
                GameAnimationService.getInstance().showToast(pdkHandBox, pdkMoveTypeLabel(move.type())));
    }

    private void onPdkPass(Stage stage) {
        try {
            pdkEngine.apply(new PassPdkTurn());
        } catch (IllegalArgumentException ex) {
            showInfo(stage, "不能不出：" + ex.getMessage());
            return;
        }
        refreshPdkTable(stage);
    }

    /**
     * 阶段 23：中央状态提示卡的文案。
     *
     * <p>不新增规则字段，全部由现有 {@link PdkSnapshot} 推导：
     * 当前回合座位、上一手出牌人 + 牌型。
     */
    private String pdkStatusText(PdkSnapshot snap) {
        if (snap.winner().isPresent()) {
            return snap.winner().get() == PlayerId.SEAT_1 ? "🏆 本局胜利" : "💪 再接再厉";
        }
        if (snap.phase() != GamePhase.PLAYING) {
            return "等待开局…";
        }
        if (snap.lastMove().isPresent() && pdkLastMover != null) {
            var move = snap.lastMove().get();
            return seatName(pdkLastMover) + " 出牌 · " + pdkMoveTypeLabel(move.type());
        }
        return seatName(snap.currentPlayer()) + " 先手出牌";
    }

    /** 跑得快牌型的中文展示名（表现层映射，不改动规则枚举）。 */
    private static String pdkMoveTypeLabel(PdkMoveType type) {
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

    /** 若当前是 bot 回合，延迟后让 bot 决策并 apply，再刷新（链式推进）。 */
    private void schedulePdkBot(Stage stage) {
        PdkSnapshot snap = (PdkSnapshot) pdkEngine.snapshotFor(PlayerId.SEAT_1);
        if (snap.phase() != GamePhase.PLAYING || snap.winner().isPresent()) return;
        PlayerId cur = snap.currentPlayer();
        if (cur == PlayerId.SEAT_1) return;
        RuleBotController bot = cur == PlayerId.SEAT_2 ? pdkBot2 : pdkBot3;
        if (botDelay != null) botDelay.stop();
        botDelay = new PauseTransition(Duration.millis(700));
        botDelay.setOnFinished(e -> {
            boolean bomb = false;
            try {
                GameSnapshot botSnap = pdkEngine.snapshotFor(cur);
                var legal = pdkEngine.legalCommands(cur);
                if (legal.isEmpty()) {
                    refreshPdkTable(stage);
                    return;
                }
                BotDecision d = bot.decide(botSnap, legal);
                pdkEngine.apply(d.command());
                pdkLastMover = cur;
                // 阶段 23：AI 打出炸弹时给一次高亮提示（普通出牌不打扰）
                PdkSnapshot after = (PdkSnapshot) pdkEngine.snapshotFor(PlayerId.SEAT_1);
                bomb = after.lastMove().map(m -> m.type() == PdkMoveType.FOUR_OF_A_KIND).orElse(false);
            } catch (IllegalArgumentException ignored) {
                // 容错：bot 决策偶尔不合法时直接刷新，避免卡死
            }
            refreshPdkTable(stage);
            if (bomb) {
                GameAnimationService.getInstance().showToast(pdkHandBox, "💣 炸弹！");
            }
        });
        botDelay.play();
    }

    // ----------------------------- 骗子酒馆（本地人机） -----------------------------

    /**
     * 进入骗子酒馆牌桌的统一起点：与跑得快共用同一套金币规则 —— 先按
     * {@link #GAME_ENTRY_COST} 扣入场费，扣款失败则不入桌。
     *
     * <p>首局与"再来一局"都走这里，因此每局都会重新收取入场费，不会出现免费连开。
     *
     * @return 是否成功进入牌桌
     */
    private boolean enterLiarTable(Stage stage) {
        if (!coinService.costGold(GAME_ENTRY_COST, "骗子酒馆入场")) {
            showInfo(stage, "金币不足，骗子酒馆入场需 " + GAME_ENTRY_COST + " 金币");
            return false;
        }
        navEnterGame(stage, buildLiarTableScene(stage));
        return true;
    }

    /**
     * 骗子酒馆本局结算：与跑得快共用同一套奖励金额（{@link #GAME_WIN_REWARD} /
     * {@link #GAME_LOSS_REWARD}），全部经 {@link CoinService} 写入当前账号并落盘，
     * 因此结算后的余额与个人中心、金币流水永远一致。
     *
     * <p><b>防重复：</b>{@link #liarSettled} 是本局唯一闸门，建桌时重置。
     */
    private void settleLiar(boolean win) {
        if (liarSettled) {
            return;
        }
        liarSettled = true;

        int goldDelta = win ? GAME_WIN_REWARD : GAME_LOSS_REWARD;
        coinService.addGold(goldDelta, win ? "骗子酒馆胜利奖励" : "骗子酒馆参与奖励");
        liarGoldDeltaForResult = goldDelta;

        if (liarCoinBar != null) {
            liarCoinBar.setCoins(coinService.getGold());
        }

        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(win ? com.csu.pokergame.audio.SoundEffect.WIN
                        : com.csu.pokergame.audio.SoundEffect.LOSE);
    }

    /** 构建骗子酒馆牌桌：SEAT_1 为玩家，SEAT_2/3/4 为不同随机策略 AI。 */
    private Scene buildLiarTableScene(Stage stage) {
        resultShown = false;
        tableSelected.clear();
        liarSettled = false;          // 新的一局 = 新的一次结算
        liarGoldDeltaForResult = 0;
        liarSeats = null;  // 重置座位，让 refreshLiarTable 重新创建
        liarEngine = new LiarEngine(new Random(System.currentTimeMillis()));
        liarEngine.start();
        liarBots = new RuleBotController[3];
        for (int i = 0; i < 3; i++) {
            liarBots[i] = new RuleBotController(new LiarRandomPolicy(new Random(System.currentTimeMillis() + i)));
        }

        // 阶段 24：改用骗子酒馆专属容器 LiarTableView（旧 SeatView / HandCardView 仅联机桌保留）
        liarView = new LiarTableView();
        liarCoinBar = liarView.getCoinBar();
        // 金币栏与跑得快同源：直接读当前账号余额（入场费已扣完，显示实时值）
        liarCoinBar.setCoins(coinService.getGold());
        liarLocalSelection.clear();

        String[] names = {"你", "西家", "北家", "东家"};
        int[] levels = {userLevel, 8, 8, 8};
        for (int i = 0; i < LiarTableView.SEAT_COUNT; i++) {
            LiarPlayerSeat seat = liarView.getSeat(i);
            if (seat == null) {
                continue;
            }
            seat.setPlayerName(names[i]);
            seat.setLevel(levels[i]);
        }

        // 本人手牌正面可见；选中用「下标集合」表示，与快照手牌顺序一一对应
        LiarHandView hand = liarView.getHandView();
        hand.setMaxSelect(3);
        hand.setOnSelectionChange(sel -> {
            liarLocalSelection.clear();
            liarLocalSelection.addAll(sel);
            updateLiarLocalButtons(stage);
        });

        LiarActionBar bar = liarView.getActionBar();
        bar.getDeclareButton().setOnAction(e -> onLiarDeclare(stage));
        bar.getContinueButton().setOnAction(e -> onLiarTrust(stage));
        bar.getChallengeButton().setOnAction(e -> onLiarChallenge(stage));

        Scene scene = wrapTableScene(stage, liarView, "← 返回模式选择");
        refreshLiarTable(stage);
        return scene;
    }

    /** 阶段 24：刷新骗子酒馆桌（LiarTableView）。 */
    private void refreshLiarTable(Stage stage) {
        if (liarView == null) {
            return;
        }
        LiarSnapshot snap = (LiarSnapshot) liarEngine.snapshotFor(PlayerId.SEAT_1);

        // 金币栏：每帧与当前账号余额对齐（充值 / 商城 / 换账号后不会残留旧数字）
        if (liarCoinBar != null) {
            liarCoinBar.setCoins(coinService.getGold());
        }

        // 顶部：阶段 / 存活 / 目标点数
        liarView.setPhaseText(phaseName(snap.liarPhase()));
        liarView.setAliveText(snap.alivePlayers().size());
        liarView.setTipText("目标点数：" + snap.targetRank().label() + " · 入场 " + GAME_ENTRY_COST);

        // 四家座位：生命值（展示值 = 3 - 已扣扳机次数）/ 状态
        PlayerId[] seats = {PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4};
        String[] names = {"你", "西家", "北家", "东家"};
        for (int i = 0; i < seats.length; i++) {
            PlayerId p = seats[i];
            LiarPlayerSeat seat = liarView.getSeat(i);
            if (seat == null) {
                continue;
            }
            seat.setPlayerName(names[i]);
            var gs = snap.guns().get(p);
            int pulled = gs == null ? 0 : gs.shotsFired();
            // 手枪仓：6 个弹巢，已扣次数越多越危险；生命值按「剩余机会」折算成 0~3 颗心
            seat.setLife(Math.max(0, 3 - pulled / 2));
            if (snap.eliminatedPlayers().contains(p)) {
                seat.setDead();
            } else if (snap.winner().isPresent()) {
                seat.setNormal();
                if (snap.winner().get() == p) {
                    seat.playWinBurst();
                }
            } else if (p == snap.currentPlayer() && snap.phase() == GamePhase.PLAYING) {
                seat.setThinking();
            } else {
                seat.setNormal();
            }
        }

        // 中央声明卡 + 顶部紧凑声明
        String declarerName = snap.declarer() == null ? "" : names[indexOfSeat(seats, snap.declarer())];
        String claimText = snap.declarer() == null
                ? "等待首位宣告者"
                : snap.pendingDeclaredCount() + " 张 " + snap.targetRank().label();
        applyLiarClaim(declarerName, claimText, snap);

        // 本人手牌：正面同步牌面（对手手牌不在快照中，无从泄漏）
        LiarHandView hand = liarView.getHandView();
        hand.setCards(snap.myHand());
        liarLocalSelection.removeIf(i -> i >= snap.myHand().size());
        hand.refreshSelection();

        // 心理压力 / 怀疑度：引擎无该字段，由界面侧按公开信息临时推算（纯展示）
        liarView.getRiskIndicator().setValue(estimateLiarRisk(snap));

        // 日志
        liarView.setLogEntries(snap.publicEvents());

        updateLiarLocalButtons(stage);

        // 本地中弹/终局：播放胜利/失败退场动画，结束后可重新开始或返回选择游戏
        if (snap.winner().isPresent() || snap.eliminatedPlayers().contains(PlayerId.SEAT_1)) {
            boolean win = snap.winner().isPresent() && snap.winner().get() == PlayerId.SEAT_1;
            if (botDelay != null) { botDelay.stop(); botDelay = null; }
            // 统一金币结算：与跑得快同额（胜 +100 / 负 +20），唯一入口是 CoinService
            settleLiar(win);
            String sub = (win
                    ? "你活到了最后，赢得酒馆对决！"
                    : "你中弹被淘汰，本局结束。")
                    + " 金币 ＋" + liarGoldDeltaForResult + "，余额 " + coinService.getGold();
            showGameResult(stage, win, sub, () -> enterLiarTable(stage));
            return;
        }

        scheduleLiarBot(stage);
    }

    /** 阶段 24：本地桌按钮可用性（基于隐藏手牌的下标选中集合）。 */
    private void updateLiarLocalButtons(Stage stage) {
        if (liarView == null) {
            return;
        }
        LiarSnapshot snap = (LiarSnapshot) liarEngine.snapshotFor(PlayerId.SEAT_1);
        LiarActionBar bar = liarView.getActionBar();
        boolean myTurn = snap.currentPlayer() == PlayerId.SEAT_1 && snap.phase() == GamePhase.PLAYING;
        // 手牌只在「本人 · 宣告阶段」可点击选牌；其余时刻不可交互（牌背变暗）
        liarView.getHandView().setInteractive(myTurn && snap.liarPhase() == LiarPhase.DECLARE);
        if (!myTurn) {
            bar.setAllEnabled(false);
            return;
        }
        if (snap.liarPhase() == LiarPhase.DECLARE) {
            bar.setDeclareEnabled(!liarSelectedCards(snap).isEmpty());
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

    /** 把选中的手牌下标映射为实际牌（顺序与快照手牌一致，用于提交命令）。 */
    private List<com.csu.pokergame.core.card.Card> liarSelectedCards(LiarSnapshot snap) {
        List<com.csu.pokergame.core.card.Card> hand = snap.myHand();
        List<com.csu.pokergame.core.card.Card> picked = new ArrayList<>();
        for (int i : liarLocalSelection) {
            if (i >= 0 && i < hand.size()) {
                picked.add(hand.get(i));
            }
        }
        return picked;
    }

    /** 宣告：提交选中的 1~3 张牌，并播放声明文字淡入。 */
    private void onLiarDeclare(Stage stage) {
        LiarSnapshot snap = (LiarSnapshot) liarEngine.snapshotFor(PlayerId.SEAT_1);
        List<com.csu.pokergame.core.card.Card> picked = liarSelectedCards(snap);
        if (picked.isEmpty() || picked.size() > 3) {
            showInfo(stage, "请选择 1–3 张手牌宣告");
            return;
        }
        try {
            liarEngine.apply(new DeclareLiarCards(picked));
        } catch (IllegalArgumentException ex) {
            showInfo(stage, "宣告不合法：" + ex.getMessage());
            return;
        }
        liarLocalSelection.clear();
        liarView.getHandView().clearSelection();
        liarView.getClaimPanel().playClaimIn();
        refreshLiarTable(stage);
    }

    /** 继续（相信）：沿用 TrustDeclaration。 */
    private void onLiarTrust(Stage stage) {
        try {
            liarEngine.apply(new TrustDeclaration());
        } catch (IllegalArgumentException ex) {
            showInfo(stage, "操作不合法：" + ex.getMessage());
            return;
        }
        liarLocalSelection.clear();
        liarView.getHandView().clearSelection();
        refreshLiarTable(stage);
    }

    /** 质疑：沿用 ChallengeDeclaration + 红色震动 / 暗红屏幕闪烁。 */
    private void onLiarChallenge(Stage stage) {
        try {
            liarEngine.apply(new ChallengeDeclaration());
        } catch (IllegalArgumentException ex) {
            showInfo(stage, "操作不合法：" + ex.getMessage());
            return;
        }
        liarLocalSelection.clear();
        liarView.getHandView().clearSelection();
        liarView.getActionBar().playChallengeFeedback();
        liarView.playChallengeEffect();
        refreshLiarTable(stage);
    }

    /** 座位下标（找不到回退到本人）。 */
    private static int indexOfSeat(PlayerId[] seats, PlayerId p) {
        for (int i = 0; i < seats.length; i++) {
            if (seats[i] == p) {
                return i;
            }
        }
        return 0;
    }

    /** 同步声明卡（中央大字 + 顶部紧凑）。 */
    private void applyLiarClaim(String declarerName, String claimText, LiarSnapshot snap) {
        String cred = snap.lastResolution()
                .map(r -> r.truthful() ? "偏高" : "偏低")
                .orElse("未知");
        if (liarView.getClaimPanel().getClaim().equals(claimText)) {
            // 文案未变则不重复播放动画
            liarView.getClaimPanel().setDeclarer(declarerName);
            liarView.getClaimPanel().setCredibility(cred);
        } else {
            liarView.getClaimPanel().setDeclarer(declarerName);
            liarView.getClaimPanel().setClaim(claimText);
            liarView.getClaimPanel().setCredibility(cred);
        }
        liarView.getHeaderClaim().setDeclarer(declarerName);
        liarView.getHeaderClaim().setClaim(claimText);
        liarView.getHeaderClaim().setCredibility(cred);
    }

    /**
     * 界面侧推算的「怀疑度」（0.0 ~ 1.0，纯展示）。
     *
     * <p>引擎没有该字段，因此只用公开信息估算：是否存在待决声明、上轮结算是否说谎、
     * 公开事件数量。不参与任何规则判定，也不写回引擎。
     */
    private static double estimateLiarRisk(LiarSnapshot snap) {
        double pending = snap.declarer() == null ? 0 : 0.25;
        double lastLie = snap.lastResolution().map(r -> r.truthful() ? 0.0 : 0.35).orElse(0.0);
        double events = Math.min(4, snap.publicEvents().size() / 6) * 0.08;
        return Math.min(1.0, pending + lastLie + events);
    }

    /** 骗子酒馆 bot 调度。 */
    private void scheduleLiarBot(Stage stage) {
        LiarSnapshot snap = (LiarSnapshot) liarEngine.snapshotFor(PlayerId.SEAT_1);
        if (snap.phase() != GamePhase.PLAYING || snap.winner().isPresent()) return;
        PlayerId cur = snap.currentPlayer();
        if (cur == PlayerId.SEAT_1) return;
        int idx = cur == PlayerId.SEAT_2 ? 0 : cur == PlayerId.SEAT_3 ? 1 : 2;
        RuleBotController bot = liarBots[idx];
        if (botDelay != null) botDelay.stop();
        botDelay = new PauseTransition(Duration.millis(900));
        botDelay.setOnFinished(e -> {
            try {
                GameSnapshot botSnap = liarEngine.snapshotFor(cur);
                var legal = liarEngine.legalCommands(cur);
                if (legal.isEmpty()) {
                    refreshLiarTable(stage);
                    return;
                }
                BotDecision d = bot.decide(botSnap, legal);
                liarEngine.apply(d.command());
            } catch (IllegalArgumentException ignored) {
            }
            refreshLiarTable(stage);
        });
        botDelay.play();
    }

    // ----------------------------- 局域网联机 -----------------------------

    /** 关闭联机主机/客户端，释放后台线程。 */
    private void shutdownLan() {
        if (lanHost != null) { lanHost.shutdown(); lanHost = null; }
        if (lanClient != null) { lanClient.shutdown(); lanClient = null; }
        lanLastSnapshot = null;
        lanLastRoom = null;
        lanTableScene = null;
    }

    /** 构建联机大厅：创建或加入房间。 */
    private Scene buildLanLobbyScene(Stage stage) {
        shutdownLan();
        StackPane root = new StackPane();
        // 背景层与粒子层由 BackgroundManager 统一构建
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        root.getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        String gameName = selectedGame == SelectedGame.PDK ? "湖南跑得快" : "骗子酒馆";
        Label title = new Label("局域网联机 · " + gameName);
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("创建房间成为主机，或输入主机 IP 加入");
        sub.getStyleClass().add("game-choice-sub");

        Button createBtn = new Button("创建房间");
        createBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");
        TextField ipField = new TextField();
        ipField.setPromptText("主机 IP，如 192.168.1.100");
        ipField.getStyleClass().add("lan-ip-field");
        ipField.setPrefWidth(220);
        Button joinBtn = new Button("加入房间");
        joinBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");

        Label ipDisplay = new Label();
        ipDisplay.getStyleClass().add("game-choice-sub");

        ListView<String> playerList = new ListView<>();
        playerList.getStyleClass().add("table-log");
        playerList.setPrefHeight(170);
        playerList.setMaxWidth(420);

        Button startBtn = new Button("开始游戏");
        startBtn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> clickSound());
        startBtn.getStyleClass().addAll("menu-btn", "menu-btn-start");
        startBtn.setVisible(false);

        GameType gt = selectedGame == SelectedGame.PDK ? GameType.PAO_DE_KUAI : GameType.LIARS_POKER;
        int required = gt.requiredPlayers();

        createBtn.setOnAction(e -> {
            try {
                lanHost = new LanHost(gt);
                lanHost.setOnRoomUpdate(rm -> Platform.runLater(() -> updateLobbyRoom(rm, playerList, startBtn, required)));
                lanHost.setOnStartGame(() -> Platform.runLater(() -> navEnterGame(stage, buildLanTableSceneHost(stage, lanHost))));
                lanHost.setOnEnded(reason -> Platform.runLater(() -> {
                    // 牌桌上已播放过结算动画时不再跳转，由“返回大厅”按钮收尾
                    if (lanResultShown) return;
                    showInfo(stage, "对局结束：" + reason);
                    shutdownLan();
                    navReturnLobby(stage, modeChoiceScene);
                }));
                ipDisplay.setText("本机 " + lanHost.localAddress() + ":" + lanHost.port() + "，等待 " + (required - 1) + " 人加入");
                lanHost.broadcastRoom();
                createBtn.setDisable(true);
            } catch (IOException ex) {
                showInfo(stage, "创建房间失败：" + ex.getMessage());
            }
        });

        joinBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) { showInfo(stage, "请输入主机 IP"); return; }
            lanClient = new LanClient(ip);
            lanClient.setOnRoomUpdate(rm -> Platform.runLater(() -> updateLobbyRoom(rm, playerList, startBtn, required)));
            lanClient.setOnStartGame((gameType, seat) -> Platform.runLater(() -> {
                lanLocalSeat = seat;
                navEnterGame(stage, buildLanTableSceneClient(stage, lanClient, gameType, seat));
            }));
            lanClient.setOnSnapshot(snap -> Platform.runLater(() -> {
                lanLastSnapshot = snap;
                applyLanSnapshot(stage, snap);
            }));
            lanClient.setOnEnded(reason -> Platform.runLater(() -> {
                // 牌桌上已播放过结算动画时不再跳转，由“返回大厅”按钮收尾
                if (lanResultShown) return;
                showInfo(stage, "对局结束：" + reason);
                shutdownLan();
                navReturnLobby(stage, modeChoiceScene);
            }));
            lanClient.setOnError(err -> Platform.runLater(() -> showInfo(stage, "错误：" + err)));
            lanClient.join();
            joinBtn.setDisable(true);
            ipDisplay.setText("正在连接 " + ip + " …");
        });

        startBtn.setOnAction(e -> {
            try { if (lanHost != null) lanHost.startGame(); }
            catch (IllegalStateException ex) { showInfo(stage, ex.getMessage()); }
        });

        Button back = new Button("← 返回模式选择");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> { shutdownLan(); navFade(stage, modeChoiceScene); });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        HBox row = new HBox(16, createBtn, ipField, joinBtn);
        row.setAlignment(Pos.CENTER);
        VBox center = new VBox(26, title, sub, row, ipDisplay, playerList, startBtn);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);
        root.getChildren().addAll(center, back);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        bindTransientParticles(stage, scene, particles);
        return scene;
    }

    private void updateLobbyRoom(RoomSnapshot rm, ListView<String> playerList, Button startBtn, int required) {
        lanLastRoom = rm;
        var items = rm.players().stream()
                .map(p -> p.displayName() + (p.host() ? "（主机）" : "")
                        + (p.connected() ? "  ●" : "  ○"))
                .toList();
        playerList.getItems().setAll(items);
        boolean isHost = lanHost != null;
        startBtn.setVisible(isHost);
        startBtn.setDisable(!rm.full());
        startBtn.setText(rm.full() ? "开始游戏" : "等待 (" + required + "人)");
    }

    /** 联机桌（主机视角）。 */
    private Scene buildLanTableSceneHost(Stage stage, LanHost host) {
        lanHost = host;
        lanLocalSeat = PlayerId.SEAT_1;
        // 收到远程客户端命令：切回 JavaFX 线程后由主机 apply
        host.setOnCommand(pc -> Platform.runLater(() -> {
            try { host.handleRemoteCommand(pc.seat(), pc.command()); }
            catch (IllegalArgumentException ignored) { }
            applyLanSnapshot(stage, host.localSnapshot());
        }));
        Scene scene = (selectedGame == SelectedGame.PDK)
                ? buildPdkLanTableShell(stage, cmd -> host.submitLocalCommand(cmd), () -> host.localSnapshot())
                : buildLiarLanTableShell(stage, cmd -> host.submitLocalCommand(cmd), () -> host.localSnapshot());
        lanTableScene = scene;
        applyLanSnapshot(stage, host.localSnapshot());
        return scene;
    }

    /** 联机桌（客户端视角）。 */
    private Scene buildLanTableSceneClient(Stage stage, LanClient client, GameType gt, PlayerId seat) {
        lanClient = client;
        lanLocalSeat = seat;
        Scene scene = (gt == GameType.PAO_DE_KUAI)
                ? buildPdkLanTableShell(stage, cmd -> client.submitCommand(cmd), () -> lanLastSnapshot)
                : buildLiarLanTableShell(stage, cmd -> client.submitCommand(cmd), () -> lanLastSnapshot);
        lanTableScene = scene;
        if (lanLastSnapshot != null) applyLanSnapshot(stage, lanLastSnapshot);
        return scene;
    }

    /** 联机跑得快桌壳：按钮提交走 submitter，刷新源 supplier。无 bot。复用本地桌视觉组件。 */
    private Scene buildPdkLanTableShell(Stage stage, java.util.function.Consumer<GameCommand> submitter,
                                        java.util.function.Supplier<GameSnapshot> snapshotSupplier) {
        lanResultShown = false;
        tableSelected.clear();
        BorderPane table = new BorderPane();
        table.getStyleClass().add("table-root");
        table.setPrefSize(960, 600);

        // 顶部：比赛信息栏 + PdkPlayerSeat × 3 座位条（与本地桌同款）
        pdkHeader = new PdkTableHeader();
        pdkHeader.setMode("局域网联机");
        pdkHeader.setBaseScore(GAME_ENTRY_COST);
        pdkHeader.setRoundText("");   // 联机桌无「第 N 局」概念
        pdkCoinBar = pdkHeader.getCoinBar();
        pdkCoinBar.setCoins(coinService.getGold());

        pdkSeat1 = new PdkPlayerSeat(userAvatarGlyph, "你", userLevel, true);
        pdkSeat2 = new PdkPlayerSeat("♚", "玩家 2", 8, false);
        pdkSeat3 = new PdkPlayerSeat("♝", "玩家 3", 8, false);
        pdkSeatStrip = new HBox(26, pdkSeat1, pdkSeat2, pdkSeat3);
        pdkSeatStrip.getStyleClass().add("pdk-seat-strip");
        pdkSeatStrip.setAlignment(Pos.CENTER);
        lanSeatTips = installLanSeatTips(pdkSeat1, pdkSeat2, pdkSeat3);

        VBox topBar = new VBox(10, pdkHeader, pdkSeatStrip);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 14, 8, 14));
        table.setTop(topBar);

        // 中央：玻璃牌桌 + PlayedCardsView（最近一次出牌）+ 状态提示卡
        pdkStatus = new Label();
        pdkStatus.getStyleClass().addAll("table-hint", "pdk-status-card");
        pdkStatus.setMaxWidth(Double.MAX_VALUE);
        pdkStatus.setAlignment(Pos.CENTER);
        pdkStatus.setMinHeight(Region.USE_PREF_SIZE);

        pdkCenterBox = new PlayedCardsView();
        pdkCenterBox.getStyleClass().add("pdk-center");
        VBox centerCol = new VBox(14, pdkCenterBox, pdkStatus);
        centerCol.setAlignment(Pos.CENTER);
        StackPane centerWrap = new StackPane(centerCol);
        centerWrap.getStyleClass().addAll("table-center-wrap", "pdk-table");
        table.setCenter(centerWrap);

        // 底部：PdkHandView（手牌 + 出牌 / 不出）；本人座位已在顶部座位条，故隐藏底部座位行
        pdkHandBox = new PdkHandView(tableSelected);
        pdkHandBox.getStyleClass().add("pdk-bottom");
        pdkHandBox.setSeatVisible(false);
        PdkActionBar actionBar = pdkHandBox.getActionBar();
        pdkPlayBtn = actionBar.getPlayButton();
        pdkPassBtn = actionBar.getPassButton();
        pdkPlayBtn.setOnAction(e -> {
            if (tableSelected.isEmpty()) { showInfo(stage, "请先选择要出的牌"); return; }
            var played = List.copyOf(tableSelected);
            tableSelected.clear();
            submitLanCommand(stage, submitter, new PlayPdkCards(played), snapshotSupplier);
        });
        pdkPassBtn.setOnAction(e -> submitLanCommand(stage, submitter, new PassPdkTurn(), snapshotSupplier));
        table.setBottom(pdkHandBox);

        pdkLog = new ListView<>();
        pdkLog.getStyleClass().add("table-log");
        pdkLog.setPrefWidth(220);
        table.setRight(pdkLog);
        return wrapTableScene(stage, table, "← 返回大厅");
    }

    private Scene buildLiarLanTableShell(Stage stage, java.util.function.Consumer<GameCommand> submitter,
                                          java.util.function.Supplier<GameSnapshot> snapshotSupplier) {
        lanResultShown = false;
        tableSelected.clear();
        BorderPane table = new BorderPane();
        table.getStyleClass().add("table-root");
        table.setPrefSize(960, 600);

        // 顶部：目标点数 + 阶段，右侧 CoinBar
        liarTarget = new Label();
        liarTarget.getStyleClass().add("table-liar-target");
        liarPhase = new Label();
        liarPhase.getStyleClass().add("table-liar-phase");
        HBox topBar = new HBox(30, liarTarget, liarPhase);
        topBar.setAlignment(Pos.CENTER);
        topBar.getStyleClass().add("table-status-bar");
        liarCoinBar = new CoinBar();
        // 与跑得快联机桌一致：金币栏显示当前账号余额
        liarCoinBar.setCoins(coinService.getGold());
        BorderPane topLayout = new BorderPane();
        topLayout.setCenter(topBar);
        topLayout.setRight(liarCoinBar);
        BorderPane.setMargin(liarCoinBar, new Insets(6, 14, 6, 0));
        table.setTop(topLayout);

        // 中部：SeatView × 4（与本地桌同款，承载存活 + 手枪仓），下方显示上轮结算
        liarOppStrip = new HBox(24);
        liarOppStrip.getStyleClass().add("table-opponent-strip");
        liarOppStrip.setAlignment(Pos.CENTER);
        liarSeats = new SeatView[]{
                new SeatView(userAvatarGlyph, "你", userLevel, true),
                new SeatView("♚", "玩家 2", 8, false),
                new SeatView("♝", "玩家 3", 8, false),
                new SeatView("♞", "玩家 4", 8, false)
        };
        liarOppStrip.getChildren().addAll(liarSeats);
        lanSeatTips = installLanSeatTips(liarSeats);
        liarResolution = new Label();
        liarResolution.getStyleClass().add("table-hint");
        liarResolution.setWrapText(true);
        liarResolution.setMaxWidth(640);
        liarResolution.setAlignment(Pos.CENTER);
        VBox center = new VBox(16, liarOppStrip, liarResolution);
        center.setAlignment(Pos.CENTER);
        table.setCenter(center);

        // 底部：HandCardView + 宣告 / 相信 / 质疑
        liarHandBox = new HandCardView(tableSelected);
        liarHandBox.getStyleClass().add("table-hand-row");
        liarHandBox.setAlignment(Pos.CENTER);
        liarHandBox.setMinHeight(110);
        liarDeclareBtn = new Button("宣告");
        liarTrustBtn = new Button("相信");
        liarChallengeBtn = new Button("质疑");
        for (Button b : List.of(liarDeclareBtn, liarTrustBtn, liarChallengeBtn)) b.getStyleClass().addAll("menu-btn", "table-action-btn");
        liarDeclareBtn.setOnAction(e -> {
            if (tableSelected.isEmpty() || tableSelected.size() > 3) { showInfo(stage, "请选择 1–3 张牌"); return; }
            var declared = List.copyOf(tableSelected);
            tableSelected.clear();
            submitLanCommand(stage, submitter, new DeclareLiarCards(declared), snapshotSupplier);
        });
        liarTrustBtn.setOnAction(e -> submitLanCommand(stage, submitter, new TrustDeclaration(), snapshotSupplier));
        liarChallengeBtn.setOnAction(e -> submitLanCommand(stage, submitter, new ChallengeDeclaration(), snapshotSupplier));
        HBox actions = new HBox(24, liarDeclareBtn, liarTrustBtn, liarChallengeBtn);
        actions.setAlignment(Pos.CENTER);
        VBox bottom = new VBox(14, liarHandBox, actions);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10));
        table.setBottom(bottom);

        liarLog = new ListView<>();
        liarLog.getStyleClass().add("table-log");
        liarLog.setPrefWidth(220);
        table.setRight(liarLog);
        return wrapTableScene(stage, table, "← 返回大厅");
    }

    /** 联机快照分发：按快照类型渲染对应桌（桌台节点未就绪时忽略，避免跨场景空指针）。 */
    private void applyLanSnapshot(Stage stage, GameSnapshot snap) {
        if (snap == null) return;
        if (snap instanceof PdkSnapshot p) {
            if (pdkHandBox != null && pdkSeat1 != null) renderPdkLan(stage, p);
        } else if (snap instanceof LiarSnapshot l) {
            if (liarHandBox != null && liarSeats != null) renderLiarLan(stage, l);
        }
    }

    /**
     * 联机跑得快渲染：直接复用本地桌视觉组件——
     * {@link SeatView} × 3（牌数徽章 + 当前回合光环 / 思考 / 胜负）、
     * {@link HandCardView}（手牌）、{@link PlayedCardsView}（桌面出牌）、
     * {@link CoinBar}（顶部金币栏）与 {@link WinCelebration}（终局庆祝）。
     * 数据全部来自 {@link PdkSnapshot} 与 {@link RoomSnapshot}。
     */
    private void renderPdkLan(Stage stage, PdkSnapshot snap) {
        boolean myTurn = snap.currentPlayer() == lanLocalSeat
                && snap.phase() == GamePhase.PLAYING && snap.winner().isEmpty();

        // 手牌：HandCardView 统一管理（排序 + 选中 + hover）
        pdkHandBox.setInteractive(myTurn);
        pdkHandBox.setMaxSelect(Integer.MAX_VALUE);
        pdkHandBox.setOnSelectionChange(sel -> updatePdkLanButtons(snap));
        pdkHandBox.setCards(snap.myHand());

        // 桌面：PlayedCardsView 显示最近一次出牌（标明出牌人）
        if (snap.lastMove().isPresent()) {
            pdkCenterBox.setCards(snap.lastMove().get().cards(), "上家");
        } else {
            pdkCenterBox.setCards(null, null);
        }

        // 座位条：PdkPlayerSeat × 3，剩余牌数 + 回合光环 / 胜负态（阶段 23）
        PlayerId[] seats = {PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3};
        PdkPlayerSeat[] views = {pdkSeat1, pdkSeat2, pdkSeat3};
        for (int i = 0; i < views.length; i++) {
            PlayerId seat = seats[i];
            PdkPlayerSeat sv = views[i];
            applyLanSeatName(sv, i, seat);
            Integer remain = snap.remainingCardCounts().get(seat);
            sv.setCardCount(remain == null ? -1 : remain);
            if (snap.winner().isPresent()) {
                sv.setState(snap.winner().get() == seat ? PdkPlayerSeat.State.WON : PdkPlayerSeat.State.LOST);
            } else if (seat == snap.currentPlayer() && snap.phase() == GamePhase.PLAYING) {
                sv.setState(seat == lanLocalSeat ? PdkPlayerSeat.State.ACTIVE : PdkPlayerSeat.State.THINKING);
            } else {
                sv.setState(PdkPlayerSeat.State.WAITING);
            }
        }

        // 顶部信息栏：关门 / 明牌辅助信息 + 各座位剩余牌数（阶段 23）
        if (pdkHeader != null) {
            StringBuilder aux = new StringBuilder();
            snap.closedDoorPlayers().forEach(p -> aux.append(lanPlayerName(p)).append("关门 "));
            if (snap.faceUpCard().isPresent()) {
                aux.append(" 明牌 ").append(snap.faceUpCard().get().display());
            }
            pdkHeader.setAuxText(aux.toString().trim());
            List<String> remaining = new ArrayList<>();
            for (PlayerId p : seats) {
                Integer remain = snap.remainingCardCounts().get(p);
                if (remain != null) {
                    remaining.add(lanPlayerName(p) + " " + remain);
                }
            }
            pdkHeader.setRemaining(remaining);
        }

        // 状态提示卡：当前回合 / 上一手出牌
        pdkStatus.setText(snap.winner().isPresent()
                ? (snap.winner().get() == lanLocalSeat ? "🏆 本局胜利" : "💪 再接再厉")
                : (snap.lastMove().isPresent()
                        ? "上家出牌 · " + pdkMoveTypeLabel(snap.lastMove().get().type())
                        : lanPlayerName(snap.currentPlayer()) + " 先手出牌"));

        // 日志
        pdkLog.getItems().setAll(snap.publicEvents().stream()
                .skip(Math.max(0, snap.publicEvents().size() - 40)).toList());

        updatePdkLanButtons(snap);

        // 终局：与本地桌同一套胜利 / 失败庆祝效果
        if (snap.winner().isPresent()) {
            boolean win = snap.winner().get() == lanLocalSeat;
            String sub = win
                    ? "你率先出完手牌，本局获胜！"
                    : lanPlayerName(snap.winner().get()) + " 先出完手牌，本局惜败。";
            pdkPlayBtn.setDisable(true);
            pdkPassBtn.setDisable(true);
            showLanResult(stage, win, sub);
        }
    }

    private void updatePdkLanButtons(PdkSnapshot snap) {
        boolean myTurn = snap.currentPlayer() == lanLocalSeat
                && snap.phase() == GamePhase.PLAYING && snap.winner().isEmpty();
        pdkPlayBtn.setDisable(!(myTurn && !tableSelected.isEmpty()));
        pdkPassBtn.setDisable(!myTurn);
    }

    /**
     * 联机骗子酒馆渲染：直接复用本地桌视觉组件——
     * {@link SeatView} × 4（手枪仓 + 存活 / 当前回合光环 / 胜负）、
     * {@link HandCardView}（手牌）、{@link CoinBar}（顶部金币栏）与
     * {@link WinCelebration}（终局庆祝）。数据来自 {@link LiarSnapshot} 与 {@link RoomSnapshot}。
     */
    private void renderLiarLan(Stage stage, LiarSnapshot snap) {
        liarTarget.setText("目标点数：" + snap.targetRank().label());
        liarPhase.setText("阶段：" + phaseName(snap.liarPhase()));

        // 座位条：SeatView × 4，手枪仓信息 + 存活 / 回合 / 胜负态
        PlayerId[] seats = {PlayerId.SEAT_1, PlayerId.SEAT_2, PlayerId.SEAT_3, PlayerId.SEAT_4};
        for (int i = 0; i < seats.length && liarSeats != null && i < liarSeats.length; i++) {
            PlayerId p = seats[i];
            SeatView sv = liarSeats[i];
            applyLanSeatName(sv, i, p);
            var gs = snap.guns().get(p);
            int pulled = gs == null ? 0 : gs.shotsFired();
            sv.setExtraInfo("🔫 " + pulled + "/6");
            boolean eliminated = snap.eliminatedPlayers().contains(p);
            if (snap.winner().isPresent()) {
                sv.setState(snap.winner().get() == p ? SeatView.State.WON : SeatView.State.LOST);
            } else if (eliminated) {
                sv.setState(SeatView.State.LOST);
            } else if (p == snap.currentPlayer() && snap.phase() == GamePhase.PLAYING) {
                sv.setState(p == lanLocalSeat ? SeatView.State.ACTIVE : SeatView.State.THINKING);
            } else {
                sv.setState(SeatView.State.WAITING);
            }
        }

        // 上轮结算
        liarResolution.setText(snap.lastResolution()
                .map(r -> "上轮：" + r)
                .orElse(snap.declarer() == null ? "等待首位宣告者" : ""));

        // 手牌：HandCardView（最多选 3 张）
        boolean myTurn = snap.currentPlayer() == lanLocalSeat && snap.phase() == GamePhase.PLAYING;
        liarHandBox.setInteractive(myTurn && snap.liarPhase() == LiarPhase.DECLARE);
        liarHandBox.setMaxSelect(3);
        liarHandBox.setOnSelectionChange(sel -> updateLiarLanButtons(snap));
        liarHandBox.setCards(snap.myHand());

        // 日志
        liarLog.getItems().setAll(snap.publicEvents().stream()
                .skip(Math.max(0, snap.publicEvents().size() - 40)).toList());

        updateLiarLanButtons(snap);

        // 终局：与本地桌同一套胜利 / 失败庆祝效果
        if (snap.winner().isPresent()) {
            boolean win = snap.winner().get() == lanLocalSeat;
            String sub = win
                    ? "你活到了最后，赢得酒馆对决！"
                    : lanPlayerName(snap.winner().get()) + " 活到了最后。";
            liarDeclareBtn.setDisable(true);
            liarTrustBtn.setDisable(true);
            liarChallengeBtn.setDisable(true);
            showLanResult(stage, win, sub);
        }
    }

    private void updateLiarLanButtons(LiarSnapshot snap) {
        boolean myTurn = snap.currentPlayer() == lanLocalSeat && snap.phase() == GamePhase.PLAYING;
        liarDeclareBtn.setDisable(!(myTurn && snap.liarPhase() == LiarPhase.DECLARE));
        liarTrustBtn.setDisable(!(myTurn && snap.liarPhase() == LiarPhase.RESPOND));
        liarChallengeBtn.setDisable(!(myTurn && snap.liarPhase() == LiarPhase.RESPOND));
    }

    /**
     * 联机命令提交：交给主机 / 客户端通道，随后按最新快照重绘。
     * 主机本地出牌不会自动回流快照，因此必须主动刷新；引擎校验失败时弹窗提示，不中断对局。
     */
    private void submitLanCommand(Stage stage, java.util.function.Consumer<GameCommand> submitter,
                                  GameCommand command, java.util.function.Supplier<GameSnapshot> snapshotSupplier) {
        try {
            submitter.accept(command);
        } catch (IllegalArgumentException ex) {
            showInfo(stage, "操作不合法：" + ex.getMessage());
        }
        applyLanSnapshot(stage, snapshotSupplier.get());
    }

    /** 联机玩家显示名：优先取 {@link RoomSnapshot} 中主机分配的座位名，回退到本地座位名。 */
    private String lanPlayerName(PlayerId seat) {
        if (lanLastRoom != null) {
            for (RoomPlayer rp : lanLastRoom.players()) {
                if (rp.seat() == seat) {
                    return seat == lanLocalSeat ? "你" : rp.displayName();
                }
            }
        }
        return seatName(seat);
    }

    /** 把房间里的真实玩家名写入座位（头像名 + 悬停气泡，SeatView 紧凑态不显示名字）。 */
    private void applyLanSeatName(SeatView sv, int index, PlayerId seat) {
        applyLanSeatNameInternal(sv, sv.getAvatar(), null, index, seat);
    }

    /** 阶段 23：跑得快联机座位（PdkPlayerSeat 会直接显示昵称）。 */
    private void applyLanSeatName(PdkPlayerSeat sv, int index, PlayerId seat) {
        applyLanSeatNameInternal(sv, sv.getAvatar(), sv, index, seat);
    }

    private void applyLanSeatNameInternal(Node node, AvatarView avatar, PdkPlayerSeat pdkSeat,
                                          int index, PlayerId seat) {
        String name = lanPlayerName(seat);
        avatar.setAvatarName(name);
        if (pdkSeat != null) {
            pdkSeat.setPlayerName(name);
        }
        if (lanSeatTips != null && index < lanSeatTips.length) {
            lanSeatTips[index].setText(name);
        }
    }

    /** 给联机座位安装名字气泡，返回可增量更新文本的 Tooltip 数组。 */
    private static Tooltip[] installLanSeatTips(SeatView... seats) {
        Tooltip[] tips = new Tooltip[seats.length];
        for (int i = 0; i < seats.length; i++) {
            tips[i] = new Tooltip("");
            Tooltip.install(seats[i], tips[i]);
        }
        return tips;
    }

    /** 阶段 23：跑得快联机座位（PdkPlayerSeat）同样安装名字气泡。 */
    private static Tooltip[] installLanSeatTips(PdkPlayerSeat... seats) {
        Tooltip[] tips = new Tooltip[seats.length];
        for (int i = 0; i < seats.length; i++) {
            tips[i] = new Tooltip("");
            Tooltip.install(seats[i], tips[i]);
        }
        return tips;
    }

    /**
     * 联机结算：在当前牌桌上叠加 {@link WinCelebration}（胜利金色星光 / 失败灰化）。
     * 联机无法本地重开，因此只提供“返回大厅”，由按钮负责关闭网络并返回模式选择页。
     */
    private void showLanResult(Stage stage, boolean win, String subtitle) {
        if (lanResultShown || lanTableScene == null || stage.getScene() != lanTableScene) return;
        lanResultShown = true;
        StackPane root = (StackPane) stage.getScene().getRoot();

        WinCelebration celebration = new WinCelebration(win, subtitle);
        Button back = new Button("返回大厅");
        back.addEventHandler(javafx.event.ActionEvent.ACTION, e -> clickSound());
        back.getStyleClass().addAll("menu-btn", "result-btn", "result-btn-back");
        back.setDisable(true);
        back.setOpacity(0.0);
        back.setTranslateY(10);
        StackPane.setAlignment(back, Pos.BOTTOM_CENTER);
        StackPane.setMargin(back, new Insets(0, 0, 60, 0));
        celebration.getChildren().add(back);
        root.getChildren().add(celebration);

        Timeline btnIn = new Timeline(
                new KeyFrame(Duration.millis(1100),
                        new KeyValue(back.opacityProperty(), 0.0),
                        new KeyValue(back.translateYProperty(), 10, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(back.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(back.translateYProperty(), 0, Interpolator.EASE_OUT)));
        btnIn.setOnFinished(e -> back.setDisable(false));
        btnIn.play();

        back.setOnAction(e -> {
            celebration.stop();
            root.getChildren().remove(celebration);
            shutdownLan();
            navWinToLobby(stage, modeChoiceScene);
        });
    }

    // ----------------------------- 牌桌小工具 -----------------------------

    private static String seatName(PlayerId p) {
        return switch (p) {
            case SEAT_1 -> "你";
            case SEAT_2 -> "西家";
            case SEAT_3 -> "北家";
            case SEAT_4 -> "东家";
        };
    }

    private static String phaseName(LiarPhase ph) {
        return switch (ph) {
            case DECLARE -> "宣告";
            case RESPOND -> "应答";
            case RESOLVE -> "结算";
            case FINISHED -> "结束";
        };
    }


    /**
     * 加载玩家档案，并把昵称 / 等级 / 头像回填到首页与大厅的展示字段。
     * 金币与经验都不存字段，需要时由 {@link #applyHomeGrowthInfo(AvatarView)} 直接向
     * {@link CoinService} / {@link PlayerProfile} 查询，避免"内存里两份数据"。
     */
    private void loadPlayerProfile() {
        playerManager = PlayerManager.getInstance();
        // 阶段 16：账号服务在建单例时完成 data/ 目录创建与旧存档迁移（player.json → players/default.json）
        accountService = AccountService.getInstance();
        coinService = CoinService.getInstance();
        coinRechargeService = CoinRechargeService.getInstance();
        coinLogService = CoinLogService.getInstance();
        inventoryService = InventoryService.getInstance();
        shopService = ShopService.getInstance();
        itemUseService = ItemUseService.getInstance();
        growthService = PlayerGrowthService.getInstance();
        statsService = PlayerStatsService.getInstance();
        achievementService = AchievementService.getInstance();
        // 阶段 17：战绩明细服务。它按当前存档名推导自己的记录文件，因此换账号无需额外通知
        gameRecordService = GameRecordService.getInstance();
        // 阶段 18：排行榜服务。绑定 AccountService 读取全部账号存档，只读、不切换当前玩家
        leaderboardService = LeaderboardService.getInstance();
        // 阶段 19：数据统计中心服务。绑定战绩明细 + 金币服务，实时汇总、只读、不落盘
        statisticsService = StatisticsService.getInstance();
        PlayerProfile profile = playerManager.getProfile();
        userNick = profile.getName();
        userLevel = profile.getLevel();
        userAvatarGlyph = profile.getAvatar();
    }

    /**
     * 把「等级称号 + 成长信息（经验进度 / 金币）」回填到首页玩家卡，全部实时查询当前值：
     * <pre>
     * 玩家  🥈 白银 Lv.12      ← 等级徽章 + 称号 + 等级
     * 经验  ▓▓▓░░░░░  350/1300 ← 经验进度条 + 当前/需求
     * 金币  1000               ← 金币数量
     * </pre>
     * 等级 / 经验取 {@link PlayerManager} 档案，升级需求与称号由 {@link PlayerGrowthService} 给出，
     * 金币由 {@link CoinService} 给出；首页不展示在线状态（该信息只在选择游戏页展示）。
     */
    private void applyHomeGrowthInfo(AvatarView card) {
        if (card == null) {
            return;
        }
        if (playerManager == null || growthService == null) {
            return;
        }
        PlayerProfile profile = playerManager.getProfile();
        int level = profile.getLevel();
        card.setLevel(level);
        card.setLevelTitle(growthService.getLevelBadge(level), growthService.getLevelTitle(level));
        card.setStatusText("");
        card.setGrowthInfo(profile.getExp(), growthService.expToNextLevel(level),
                coinService == null ? 0 : coinService.getGold(),
                // 阶段 17：首页玩家卡补一行胜率（0 场时显示 0%，由 PlayerStatsService 汇总口径给出）
                statsService == null ? -1 : statsService.getWinRatePercent());
    }

    /** 把当前 userNick/userAvatarGlyph/userStat 回填到首页与选择游戏页的头像卡片节点。 */
    private void refreshUserCard() {
        // 旧文本节点已不在场景中（保留字段兼容），仅维护新 AvatarView
        userAvatarText.setText(userAvatarGlyph);
        userNickLabel.setText(userNick);
        userStatLabel.setText(userStat);
        if (homeAvatar != null) {
            homeAvatar.setGlyph(userAvatarGlyph);
            homeAvatar.setAvatarName(userNick);
            // 首页卡实时读取档案：经验 / 等级可能在牌局结算里已增长，这里用当前值覆盖
            applyHomeGrowthInfo(homeAvatar);
        }
        if (choiceAvatar != null) {
            choiceAvatar.setGlyph(userAvatarGlyph);
            choiceAvatar.setAvatarName(userNick);
            choiceAvatar.setStatusText(userStat);
            choiceAvatar.setLevel(userLevel);
        }
    }

    /**
     * 头像字形专用字体：按码点选择字体族，避免不同控件路径下的字体回退差异。
     * 补充符号（象棋♛♚♝ 等 U+2600 区段）走 Segoe UI Symbol；
     * emoji 类（🃏🤖 U+1F000 以上）走 Segoe UI Emoji。
     */
    private static Font avatarGlyphFont(double size, String glyph) {
        boolean emoji = glyph != null && !glyph.isEmpty() && glyph.codePointAt(0) >= 0x1F000;
        return Font.font(emoji ? "Segoe UI Emoji" : "Segoe UI Symbol", FontWeight.BOLD, size);
    }

    /** 构建“个人信息”编辑页：选择头像字符、编辑昵称、切换在线状态；保存后回填到选择游戏页。 */
    private Scene buildProfileScene(Stage stage) {
        StackPane root = new StackPane();

        // 背景层与粒子层由 BackgroundManager 统一构建
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        root.getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();

        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        Label title = new Label("个人信息");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("自定义你的头像与昵称");
        sub.getStyleClass().add("game-choice-sub");

        // ---------------- 当前预览：大号头像 + 昵称预览 ----------------
        StackPane previewBadge = new StackPane();
        previewBadge.getStyleClass().addAll("user-avatar", "user-avatar-lg");
        Text previewGlyph = new Text(userAvatarGlyph);
        // 按字形码点选择字体族（象棋符号 / emoji），避免预览出现缺字豆腐块
        previewGlyph.setFont(avatarGlyphFont(46, userAvatarGlyph));
        previewGlyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff3c4")),
                new Stop(0.55, Color.web("#e8c25e")),
                new Stop(1, Color.web("#b07f1e"))));
        previewBadge.getChildren().add(previewGlyph);

        Label previewNick = new Label(userNick);
        previewNick.getStyleClass().add("profile-preview-nick");
        Label previewStat = new Label(userStat);
        previewStat.getStyleClass().add("user-stat");
        VBox previewText = new VBox(4, previewNick, previewStat);
        previewText.setAlignment(Pos.CENTER_LEFT);

        HBox preview = new HBox(18, previewBadge, previewText);
        preview.getStyleClass().add("profile-preview");
        preview.setAlignment(Pos.CENTER_LEFT);

        // ---------------- 头像选择：一排候选字符 ----------------
        Label avatarHead = new Label("选择头像");
        avatarHead.getStyleClass().add("settings-section");
        String[] avatarChoices = {"♛", "♚", "♔", "♕", "♝", "♞", "♜", "♟", "🃏"};
        ToggleGroup avatarGroup = new ToggleGroup();
        HBox avatarRow = new HBox(12);
        avatarRow.getStyleClass().add("profile-avatar-row");
        for (String g : avatarChoices) {
            // 用带显式字体的 Text 作为图标，绕开按钮文字在 CSS 字体回退链下的缺字问题
            Text glyphNode = new Text(g);
            glyphNode.setFont(avatarGlyphFont(24, g));
            glyphNode.setFill(Color.web("#fff3c4"));
            ToggleButton btn = new ToggleButton();
            btn.setGraphic(glyphNode);
            btn.getStyleClass().add("profile-avatar-btn");
            btn.setToggleGroup(avatarGroup);
            btn.setUserData(g);
            if (g.equals(userAvatarGlyph)) {
                btn.setSelected(true);
            }
            btn.selectedProperty().addListener((o, a, sel) -> {
                if (sel) {
                    previewGlyph.setText(g);
                    previewGlyph.setFont(avatarGlyphFont(46, g));
                }
            });
            avatarRow.getChildren().add(btn);
        }
        // 如果当前头像不在候选里，默认选中第一个，并把预览同步为当前
        if (avatarGroup.getSelectedToggle() == null && !avatarRow.getChildren().isEmpty()) {
            ((ToggleButton) avatarRow.getChildren().get(0)).setSelected(true);
        }

        // ---------------- 昵称编辑 ----------------
        Label nickHead = new Label("昵称");
        nickHead.getStyleClass().add("settings-section");
        TextField nickField = new TextField(userNick);
        nickField.getStyleClass().add("profile-nick-field");
        nickField.setPromptText("输入你的昵称");
        nickField.setPrefColumnCount(16);
        nickField.textProperty().addListener((o, a, t) -> previewNick.setText(t.isEmpty() ? "玩家" : t));

        // ---------------- 状态选择 ----------------
        Label statHead = new Label("在线状态");
        statHead.getStyleClass().add("settings-section");
        String[] statChoices = {"在线 · 准备开局", "游戏中", "勿扰", "离线"};
        ToggleGroup statGroup = new ToggleGroup();
        HBox statRow = new HBox(10);
        for (String s : statChoices) {
            ToggleButton btn = new ToggleButton(s);
            btn.getStyleClass().add("seg-toggle");
            btn.setToggleGroup(statGroup);
            btn.setUserData(s);
            if (s.equals(userStat)) {
                btn.setSelected(true);
            }
            btn.selectedProperty().addListener((o, a, sel) -> {
                if (sel) {
                    previewStat.setText(s);
                }
            });
            statRow.getChildren().add(btn);
        }
        if (statGroup.getSelectedToggle() == null && !statRow.getChildren().isEmpty()) {
            ((ToggleButton) statRow.getChildren().get(0)).setSelected(true);
        }

        // ---------------- 底部按钮：取消 / 保存并返回 ----------------
        Button cancel = new Button("取消");
        cancel.getStyleClass().add("settings-link-btn");
        cancel.setOnAction(e -> {
            if (gameChoiceScene != null) {
                navFade(stage, gameChoiceScene);
            }
        });
        Button save = new Button("保存并返回");
        save.getStyleClass().addAll("menu-btn", "menu-btn-start", "profile-save-btn");
        save.setOnAction(e -> {
            String nick = nickField.getText().trim();
            userNick = nick.isEmpty() ? "玩家" : nick;
            ToggleButton avSel = (ToggleButton) avatarGroup.getSelectedToggle();
            if (avSel != null) {
                userAvatarGlyph = (String) avSel.getUserData();
            }
            ToggleButton stSel = (ToggleButton) statGroup.getSelectedToggle();
            if (stSel != null) {
                userStat = (String) stSel.getUserData();
            }
            // 昵称 / 头像写回玩家档案并落盘（在线状态不属于档案字段，仅保留在内存）
            if (playerManager != null) {
                PlayerProfile profile = playerManager.getProfile();
                profile.setName(userNick);
                profile.setAvatar(userAvatarGlyph);
                playerManager.save();
            }
            refreshUserCard();
            if (gameChoiceScene != null) {
                navFade(stage, gameChoiceScene);
            }
        });
        HBox actionRow = new HBox(14, cancel, save);
        actionRow.getStyleClass().add("profile-action-row");
        actionRow.setAlignment(Pos.CENTER);

        // ---------------- 中央整体 ----------------
        VBox center = new VBox(22, title, sub, preview, avatarHead, avatarRow,
                nickHead, nickField, statHead, statRow, actionRow);
        center.getStyleClass().add("profile-center");
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        // 左上角返回按钮：回到选择游戏页（不保存）
        Button back = new Button("← 返回选择游戏");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (gameChoiceScene != null) {
                navFade(stage, gameChoiceScene);
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        root.getChildren().addAll(center, back);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS == scene) {
                particles.play();
            } else if (oldS == scene) {
                particles.stop();
            }
        });
        return scene;
    }

    // ============================================================= 玩家个人中心

    /**
     * 构建玩家个人中心页（阶段 10）：页面结构、背景、数据读取全部封装在 {@link ProfileView} 内，
     * 这里只负责装配四个数据服务的单例、注入「返回主页」动作，并在每次进入时刷新一次数值。
     *
     * <p>返回路径：个人中心 → {@link #homeScene}（交叉淡化）。
     */
    private Scene buildProfileViewScene(Stage stage) {
        profileView = new ProfileView(playerManager, coinService, growthService, statsService,
                achievementService, coinRechargeService, coinLogService, inventoryService,
                shopService, itemUseService, gameRecordService,
                leaderboardService, statisticsService, accountService,
                () -> switchAccount(stage),
                () -> logout(stage),
                () -> {
                    if (homeScene != null) {
                        navFade(stage, homeScene);
                    }
                });
        Scene scene = new Scene(profileView);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // 每次从首页进入个人中心都重新读数：牌局结算后的金币 / 经验 / 战绩立即可见
        stage.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == scene) {
                profileView.refresh();
            }
        });
        return scene;
    }

    // ============================================================= 设置中心（阶段 20）

    /**
     * 加载全局设置（阶段 20）：{@link SettingsService#getInstance()} 构造时即完成
     * "读 {@code data/settings.json} → 缺字段补默认 → 回写"，因此这里只需触发一次初始化。
     * 之后所有页面都从同一个单例读取设置，天然免重启实时同步。
     */
    private void loadSettings() {
        try {
            settingsService = SettingsService.getInstance();
        } catch (RuntimeException e) {
            // 设置加载失败不应阻塞启动：退化为内存态出厂默认
            System.err.println("[DeckApp] 加载设置失败，使用默认设置: " + e.getMessage());
            settingsService = new SettingsService(null);
        }
    }

    /**
     * 设置中心页（阶段 20）：底部「设置」入口进入，内容见 {@link SettingsView}。
     *
     * <p>账号区只读展示当前账号；「切换账号 / 退出登录」复用个人中心同一套导航链路，
     * 因此切换账号不会读写 / 重置设置（账号隔离）。
     */
    private Scene buildSettingsScene(Stage stage) {
        settingsView = new SettingsView(settingsService, accountService,
                () -> switchAccount(stage),
                () -> logout(stage),
                () -> {
                    if (homeScene != null) {
                        navFade(stage, homeScene);
                    }
                });
        Scene scene = new Scene(settingsView);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // 每次进入设置中心都重新读一遍设置（例如从账号页切回来）
        stage.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == scene) {
                settingsView.refresh();
            }
        });
        return scene;
    }

    // ============================================================= 登录 / 账号（阶段 16）

    /**
     * 登录页（阶段 16）：程序启动后的第一屏。
     *
     * <p>启动链路由原来的 {@code start() → loadPlayerProfile() → homeScene} 改为
     * {@code start() → LoginView → 登录成功 → 载入玩家数据 → homeScene}。
     * 账号校验与存档切换全部在 {@link AccountService} 内部完成，这里只负责：
     * <ol>
     *   <li>登录成功→回调 {@link #onLoginSucceeded(Stage)}：重新读一遍玩家档案，
     *       并把账号数据回填到首页玩家卡；</li>
     *   <li>把 {@link #loginScene} 交给 {@link SceneTransition} 导航。</li>
     * </ol>
     */
    private Scene buildLoginScene(Stage stage) {
        loginView = new LoginView(accountService, () -> onLoginSucceeded(stage));
        Scene scene = new Scene(loginView);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        return scene;
    }

    /**
     * 登录成功：把当前账号的玩家档案重新读进内存展示字段，刷新首页/大厅玩家卡与个人中心，
     * 再淡入首页。金币、经验、战绩、背包、成就都由各自服务实时读取，这里只同步界面缓存字段。
     */
    private void onLoginSucceeded(Stage stage) {
        loadPlayerProfile();
        refreshUserCard();
        if (profileView != null) {
            profileView.refresh();
        }
        String username = accountService == null ? null : accountService.getCurrentUsername();
        System.out.println("[DeckApp] 进入首页，当前账号: " + username);
        navFade(stage, homeScene);
    }

    /** 「切换账号」：保存当前进度后回到登录页，并预填刚用过的账号名，方便切回。 */
    private void switchAccount(Stage stage) {
        pendingLoginName = accountService == null ? null : accountService.getCurrentUsername();
        if (accountService != null) {
            accountService.logout();
        }
        gotoLogin(stage);
    }

    /** 「退出登录」：保存当前进度并清空当前账号，回到登录页（不预填账号）。 */
    private void logout(Stage stage) {
        if (accountService != null) {
            accountService.logout();
        }
        pendingLoginName = null;
        gotoLogin(stage);
    }

    /** 回到登录页（重置上一次的输入与提示）。 */
    private void gotoLogin(Stage stage) {
        if (loginScene == null) {
            return;
        }
        if (loginView != null) {
            loginView.setUsernamePrefill(pendingLoginName);
        }
        if (stage.getScene() == loginScene) {
            return;
        }
        navFade(stage, loginScene);
    }

    // ============================================================= 规则界面

    /** 湖南跑得快规则文案（轻量 Markdown：# 大节、## 小节、> 引用、- 列表、数字列表、--- 分隔线）。 */
    private static final String RUN_RULES_MD = """
            # 湖南跑得快（16 张经典，3 人局）

            > 注意：棋牌仅娱乐，禁止赌博。

            ## 基础配置

            - 人数：3 人，各自为战
            - 牌：一副去掉大小王、3 张 2、1 张 A，共 48 张，每人 **16 张**
            - 牌大小：**2＞A＞K＞Q＞J＞10＞9＞8＞7＞6＞5＞4＞3**，不比花色
            - 首局：拿到**黑桃 3** 的玩家必须先出黑桃 3；上局赢家下局先手

            ## 可用牌型

            1. 单张：一张牌
            2. 对子：两张点数一样
            3. 连对：≥ 2 对连续对子，例：3344、556677
            4. 顺子：≥ 5 张连续单牌，**不能包含 2、A**，34567 最小
            5. 三张：三张同点；三带二（三张 + 任意对子）
            6. 飞机：两组及以上连续三张，可以带对子
            7. 炸弹：四张同点，最大牌型，可以炸一切；炸弹之间比点数大小

            ## 核心规则

            1. **有大必出（必压）**：上家出牌，你手里有能大过上家的牌，就必须打出来，不能过。有牌不出叫 “放走”，要包赔。
            2. 一轮全部要不起，出牌人继续出牌。
            3. **报单**：手里只剩 1 张牌，必须口头报单提醒其他人。
            4. 胜负：**最先出完手牌为头家获胜**；剩下两家按手里剩余牌数算分，剩几张算几分。剩 1 张保本不扣分。

            ## 放走包赔

            > 如果你手里有大牌可以压住上家，却选择要不起，直接放走对手跑完全部手牌，那么由你一个人承担另外两家的全部输分。
            """;

    /** 骗子酒馆规则文案。 */
    private static final String LIAR_RULES_MD = """
            # 骗子酒馆（Liar's Bar，骗子酒吧）

            > 聚会桌游，分**扑克模式、骰子模式**，最多 4 人，各自为战。

            ## 🃏 扑克模式（主流）

            1. 牌库：20 张，6Q、6K、6A、2 张 Joker（万能牌）。每小局随机选 Q/K/A 其中一个作为**主牌**，Joker 直接当作主牌使用。每人发 5 张手牌。
            2. 顺时针轮流出牌，每回合打出 **1-3 张牌，牌背朝下**，口头报出打出几张主牌，可以撒谎。
            3. 下家二选一：
               - **跟牌**：相信，轮到你，你继续打 1-3 张宣称主牌；
               - **质疑（开他）**：翻开上家打出的牌。
            4. 质疑判定：
               - ✘ 上家撒谎（不全是主牌）：**上家受罚俄罗斯轮盘**，左轮 6 个弹槽只有 1 颗子弹，中子弹直接出局。
               - ✔ 上家没撒谎：**质疑的人受罚轮盘**。
            5. 有人轮盘出局后，小局结束，重新发牌、重选主牌继续。活到最后一人胜利。
            """;

    /** 把一段规则 Markdown 渲染成规则正文容器（供主页规则弹窗使用）。 */
    private static VBox renderRulesBody(String md) {
        VBox body = new VBox(10);
        body.getStyleClass().add("rules-body");
        for (String raw : md.split("\n")) {
            javafx.scene.Node node = renderRulesLine(raw);
            if (node != null) {
                body.getChildren().add(node);
            }
        }
        return body;
    }

    /** 把一行轻量 Markdown 渲染成结点；空行返回 null。 */
    private static javafx.scene.Node renderRulesLine(String raw) {
        if (raw == null) {
            return null;
        }
        String line = raw.trim();
        if (line.isEmpty()) {
            return null;
        }
        if (line.startsWith("# ")) {
            return rulesHeading(line.substring(2), "rules-h1");
        }
        if (line.startsWith("## ")) {
            return rulesHeading(line.substring(3), "rules-h2");
        }
        if (line.matches("-{3,}")) {
            return new Separator();
        }
        if (line.startsWith("> ")) {
            return rulesFlow(line.substring(2), true);
        }
        // “- / * ”列表前缀统一转圆点；数字列表与普通行原样保留
        String text = line.matches("(?i)(-|\\*)\\s+.*")
                ? "•  " + line.replaceFirst("(?i)(-|\\*)\\s+", "")
                : line;
        return rulesFlow(text, false);
    }

    private static Label rulesHeading(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    /** 把正文解析成 TextFlow：支持 **加粗**，且逐字符生成以保证中文任意位置可换行。 */
    private static TextFlow rulesFlow(String text, boolean quote) {
        TextFlow flow = new TextFlow();
        String[] parts = text.split("\\*\\*");
        for (int i = 0; i < parts.length; i++) {
            boolean bold = i % 2 == 1;
            parts[i].codePoints().forEach(cp -> {
                Text run = new Text(new String(Character.toChars(cp)));
                run.getStyleClass().add("rules-run");
                if (bold) {
                    run.getStyleClass().add("rules-strong");
                }
                if (quote) {
                    run.getStyleClass().add("rules-quote");
                }
                flow.getChildren().add(run);
            });
        }
        return flow;
    }

    private void addCornerSuit(StackPane root, String g, Color color, Pos corner) {
        addCornerSuit(root, g, color, corner, false);
    }

    /** watermark=true 时花色额外加微模糊，作为淡淡的暗纹底使用。 */
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

    /** 设置浮层面板：界面主题 + 背景图片 + 音量/亮度 + 开发者预览。 */
    private VBox buildSettingsPanel(Region bg, Scene deckScene, Stage stage,
                                    StackPane overlay, Region dim) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("settings-panel");
        panel.setPrefWidth(340);

        Label head = new Label("设置");
        head.getStyleClass().add("settings-title");
        Button close = new Button("×");
        close.getStyleClass().add("icon-close");
        close.setOnAction(e -> overlay.setVisible(false));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(head, spacer, close);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label sec1 = new Label("界面主题");
        sec1.getStyleClass().add("settings-section");
        ToggleButton felt = new ToggleButton("经典绿桌");
        ToggleButton night = new ToggleButton("星夜蓝");
        felt.getStyleClass().add("seg-toggle");
        night.getStyleClass().add("seg-toggle");
        ToggleGroup themeGroup = new ToggleGroup();
        felt.setToggleGroup(themeGroup);
        night.setToggleGroup(themeGroup);
        felt.setSelected(true);
        themeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            // 只做轻度色偏：明暗层次交给 BackgroundManager 的渐变层，避免把背景压成一张暗壁纸
            if (b == night) {
                bg.setStyle("-fx-background-color: rgba(12, 24, 52, 0.20);");
            } else {
                bg.setStyle("-fx-background-color: rgba(8, 22, 14, 0.10);");
            }
        });
        HBox segRow = new HBox(10, felt, night);
        Label note1 = new Label("切换开始界面背景配色。");
        note1.getStyleClass().add("settings-note");

        // ---- 背景图片：统一切换三张内置背景 ----
        Label secBg = new Label("背景图片");
        secBg.getStyleClass().add("settings-section");
        HBox bgRow = new HBox(10);
        bgRow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup bgGroup = new ToggleGroup();
        for (BackgroundManager.BackgroundOption opt : BackgroundManager.OPTIONS) {
            ToggleButton bgOpt = new ToggleButton(opt.label());
            bgOpt.getStyleClass().add("bg-option");
            bgOpt.setToggleGroup(bgGroup);
            bgOpt.setUserData(opt.resource());
            bgOpt.setContentDisplay(ContentDisplay.TOP);
            bgOpt.setGraphic(backgroundThumb(opt.resource()));
            bgOpt.setSelected(opt.resource().equals(BackgroundManager.currentBackground()));
            bgOpt.setTooltip(new Tooltip(opt.label()));
            bgRow.getChildren().add(bgOpt);
        }
        bgGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b != null && b.getUserData() instanceof String res) {
                BackgroundManager.setBackground(res);
            }
        });
        Label noteBg = new Label("切换后所有页面统一使用所选背景图。");
        noteBg.getStyleClass().add("settings-note");

        // ---- 声音与显示：音量 / 亮度 ----
        Label sec2 = new Label("声音与显示");
        sec2.getStyleClass().add("settings-section");

        Label volLbl = new Label("音量");
        volLbl.getStyleClass().add("settings-row-label");
        Slider vol = new Slider(0, 100, volumePref);
        vol.getStyleClass().add("settings-slider");
        HBox.setHgrow(vol, Priority.ALWAYS);
        vol.setMaxWidth(Double.MAX_VALUE);
        Label volVal = new Label(valueText(volumePref));
        volVal.getStyleClass().add("value-chip");
        vol.valueProperty().addListener((o, a, b) -> {
            volumePref = b.doubleValue();
            volVal.setText(valueText(b.doubleValue()));
        });
        HBox volRow = new HBox(12, volLbl, vol, volVal);
        volRow.setAlignment(Pos.CENTER_LEFT);
        Label volNote = new Label("声音效果将在游戏玩法版本接入。");
        volNote.getStyleClass().add("settings-note");

        Label briLbl = new Label("亮度");
        briLbl.getStyleClass().add("settings-row-label");
        Slider bri = new Slider(20, 100, brightnessPref);
        bri.getStyleClass().add("settings-slider");
        HBox.setHgrow(bri, Priority.ALWAYS);
        bri.setMaxWidth(Double.MAX_VALUE);
        Label briVal = new Label(valueText(brightnessPref));
        briVal.getStyleClass().add("value-chip");
        bri.valueProperty().addListener((o, a, b) -> {
            brightnessPref = b.doubleValue();
            briVal.setText(valueText(b.doubleValue()));
            dim.setOpacity((100 - b.doubleValue()) / 100.0 * 0.85);
        });
        HBox briRow = new HBox(12, briLbl, bri, briVal);
        briRow.setAlignment(Pos.CENTER_LEFT);
        Label briNote = new Label("调低亮度时开始界面会整体变暗（后续全局生效）。");
        briNote.getStyleClass().add("settings-note");

        // ---- 开发者预览 ----
        Label sec3 = new Label("开发者预览");
        sec3.getStyleClass().add("settings-section");
        Button preview = new Button("查看 54 张扑克牌展示页");
        preview.getStyleClass().add("settings-link-btn");
        preview.setMaxWidth(Double.MAX_VALUE);
        preview.setOnAction(e -> {
            overlay.setVisible(false);
            navFade(stage, deckScene);
        });
        Label note3 = new Label("该页面是“游戏主界面”的素材基础，之后会接进“开始游戏”。");
        note3.getStyleClass().add("settings-note");

        Label foot = new Label("中南棋牌室 v0.2 · 开发中");
        foot.getStyleClass().add("settings-foot");

        panel.getChildren().addAll(topRow, new Separator(), sec1, segRow, note1,
                new Separator(), secBg, bgRow, noteBg,
                new Separator(), sec2, volRow, volNote, briRow, briNote,
                new Separator(), sec3, preview, note3, new Separator(), foot);
        return panel;
    }

    /** 设置页背景选项缩略图（等比拉伸铺满 64×40，资源缺失时留空）。 */
    private static ImageView backgroundThumb(String resource) {
        ImageView iv = new ImageView(BackgroundManager.backgroundImage(resource));
        iv.setFitWidth(64);
        iv.setFitHeight(40);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        return iv;
    }

    private static String valueText(double v) {
        return "%.0f%%".formatted(v);
    }

    private static void showInfo(Stage owner, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(owner);
        a.setTitle("提示");
        a.setHeaderText(null);
        a.setContentText(text);
        a.showAndWait();
    }

    /** 画一枚镂空齿轮（八齿）。 */
    private static javafx.scene.shape.Path gearShape() {
        javafx.scene.shape.Path p = new javafx.scene.shape.Path();
        p.setFillRule(FillRule.EVEN_ODD);
        p.setSmooth(true);
        double outer = 15.5;
        double rootR = outer * 0.80;
        double hole = outer * 0.40;
        ring(p, outer, rootR, 16);          // 齿圈
        ring(p, hole, hole, 40);            // 中心镂空
        return p;
    }

    /** 向路径追加一段点圈，偶数下标在外径、奇数在内径，形成锯齿。 */
    private static void ring(javafx.scene.shape.Path p, double rOuter, double rInner, int sides) {
        double step = Math.PI * 2 / sides;
        for (int i = 0; i < sides; i++) {
            double a = -Math.PI / 2 + i * step;
            double r = (i % 2 == 0) ? rOuter : rInner;
            double x = Math.cos(a) * r;
            double y = Math.sin(a) * r;
            if (i == 0) {
                p.getElements().add(new MoveTo(x, y));
            } else {
                p.getElements().add(new LineTo(x, y));
            }
        }
        p.getElements().add(new ClosePath());
    }

    private static Label glyph(String g, String colorClass) {
        Label l = new Label(g);
        l.getStyleClass().addAll("suit-glyph", colorClass);
        return l;
    }

    private void rebuildGrid() {
        cells.clear();
        grid.getChildren().clear();
        for (Card card : order) {
            CardCell cell = new CardCell(card, this::imageFor);
            cells.add(cell);
            grid.getChildren().add(cell);
        }
        refreshStatus();
    }

    private void flipAll(boolean up) {
        for (CardCell cell : cells) {
            cell.setFaceUp(up);
        }
    }

    // ============================================================= 状态栏

    private void refreshStatus() {
        status.setText("共 " + order.size() + " 张牌 · " + (backRed ? "红色牌背" : "蓝色牌背")
                + " · 展示固定：鼠标悬停/点按不改变牌面，仅按“洗牌”会打乱牌序，按“整理排序”恢复。");
    }

    // ============================================================= 图像 / 导出

    private Image imageFor(Card card, boolean faceUp) {
        if (faceUp) {
            return faceCache.computeIfAbsent(card, renderer::face);
        }
        return backCache.computeIfAbsent(backRed, renderer::back);
    }

    private void chooseAndExport(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择导出目录");
        File def = new File("output");
        def.mkdirs();
        chooser.setInitialDirectory(def);
        File chosen = chooser.showDialog(stage);
        if (chosen == null) {
            return;
        }
        try {
            int n = exportTo(chosen.toPath());
            status.setText("已导出 " + n + " 张牌面 PNG 到：" + chosen.getAbsolutePath());
        } catch (IOException ex) {
            status.setText("导出失败：" + ex.getMessage());
        }
    }

    private int exportTo(Path dir) throws IOException {
        Files.createDirectories(dir);
        List<Card> deck = Deck.standard().cards();
        int count = 0;
        for (Card card : deck) {
            Image img = faceCache.computeIfAbsent(card, renderer::face);
            imageWriter.write(img, dir.resolve(fileName(card)));
            count++;
        }
        imageWriter.write(backCache.computeIfAbsent(backRed, renderer::back),
                dir.resolve("00_BACK.png"));
        count++;
        return count;
    }

    private static String fileName(Card card) {
        if (card.isJoker()) {
            return card.isBigJoker() ? "54_RED_JOKER.png" : "53_BLACK_JOKER.png";
        }
        String suit = switch (card.suit()) {
            case SPADE -> "S";
            case HEART -> "H";
            case CLUB -> "C";
            case DIAMOND -> "D";
        };
        String idx = "%02d".formatted(13 * card.suit().ordinal() + card.rank());
        return idx + "_" + suit + card.rankText() + ".png";
    }

}
