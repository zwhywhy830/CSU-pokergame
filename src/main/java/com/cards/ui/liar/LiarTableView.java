package com.cards.ui.liar;

import com.cards.ui.component.CoinBar;
import com.cards.ui.component.PlayHistoryPanel;
import com.cards.ui.effect.GameAnimationService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * 阶段 24：骗子酒馆主桌面容器（酒馆玻璃 + 暗红金风格）。
 *
 * <p>整体为 {@link StackPane}，内部 {@link BorderPane}：
 * <pre>
 *   Top    : [ 当前阶段 ] [ LiarClaimPanel(紧凑) ] [ 剩余生命 ] ...... CoinBar
 *   Center : 环形座位的酒馆桌面（固定方位，上北下南、左西右东）
 *              (0,1) 北家 AI              ← 上（seats[1]）
 *              (1,0) 西家 AI   (1,1) 中央声明卡   (1,2) 东家 AI
 *              (2,1) 玩家（南家，本人）    ← 下（seats[0]）
 *   Bottom : LiarHandView(隐藏牌背) + LiarActionBar(继续 / 质疑)
 *   Right  : 游戏日志
 * </pre>
 *
 * <p>本组件只负责装配与展示，<b>不持有引擎引用</b>，也不新增任何规则字段；
 * 所有数据由调用方（{@code DeckApp}）从现有 {@code LiarSnapshot} 填入。
 * 动画层（{@link #getFxLayer()}）叠加在桌面之上，用于 Particles / 胜负光效。
 */
public final class LiarTableView extends StackPane {

    /** 座位数。 */
    public static final int SEAT_COUNT = 4;

    private final Label phaseLabel = new Label("阶段：—");
    private final Label lifeLabel = new Label("存活：4");
    private final Label tipLabel = new Label("");

    private final LiarClaimPanel claimPanel = new LiarClaimPanel(false);
    private final LiarClaimPanel headerClaim = new LiarClaimPanel(true);
    private final LiarRiskIndicator riskIndicator = new LiarRiskIndicator();
    private final LiarHandView handView = new LiarHandView();
    private final LiarActionBar actionBar = new LiarActionBar();
    private final CoinBar coinBar = new CoinBar();
    private final PlayHistoryPanel log = new PlayHistoryPanel();

    private final LiarPlayerSeat[] seats = new LiarPlayerSeat[SEAT_COUNT];
    private final Pane fxLayer = new Pane();
    private final Region dangerFlash = new Region();

    /** 质疑结算分阶段演出横幅（中央大字 + 副标题）。 */
    private final StackPane bannerLayer = new StackPane();
    private final VBox bannerCard = new VBox(6);
    private final Label bannerTitle = new Label();
    private final Label bannerSubtitle = new Label();

    private final BorderPane root = new BorderPane();
    private final GridPane table = new GridPane();

    public LiarTableView() {
        getStyleClass().add("liar-root");
        setPrefSize(1000, 660);

        // ---------------- Top：阶段 / 声明 / 生命 + 金币 ----------------
        phaseLabel.getStyleClass().add("liar-chip-phase");
        lifeLabel.getStyleClass().add("liar-chip-life");
        tipLabel.getStyleClass().add("liar-chip-tip");

        HBox topChips = new HBox(10, phaseLabel, lifeLabel, tipLabel);
        topChips.setAlignment(Pos.CENTER_LEFT);

        VBox topLeft = new VBox(4, topChips, headerClaim);
        topLeft.setAlignment(Pos.CENTER_LEFT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox top = new HBox(12, topLeft, topSpacer, coinBar);
        top.getStyleClass().add("liar-header");
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(coinBar, new Insets(0, 4, 0, 0));

        root.setTop(top);

        // ---------------- Center：环形座位 + 中央声明卡 ----------------
        table.getStyleClass().add("liar-table");
        table.setAlignment(Pos.CENTER);
        table.setHgap(26);
        table.setVgap(18);

        for (int i = 0; i < SEAT_COUNT; i++) {
            seats[i] = new LiarPlayerSeat(defaultGlyph(i), "玩家 " + (i + 1), 8, i == 0);
        }

        VBox centerBox = new VBox(8, claimPanel, riskIndicator);
        centerBox.setAlignment(Pos.CENTER);

        table.add(seats[1], 1, 0);   // 北：顶部中央
        table.add(seats[2], 0, 1);   // 西：左侧中央
        table.add(centerBox, 1, 1);  // 中：声明卡 + 怀疑度
        table.add(seats[3], 2, 1);   // 东：右侧中央
        table.add(seats[0], 1, 2);   // 南：底部（玩家本人，固定）

        // 动画层：粒子 / 光效 / 暗红屏幕
        fxLayer.setMouseTransparent(true);
        fxLayer.getStyleClass().add("liar-fx-layer");
        dangerFlash.getStyleClass().add("liar-danger-flash");
        dangerFlash.setOpacity(0.0);
        dangerFlash.setMouseTransparent(true);
        dangerFlash.setManaged(false);
        fxLayer.getChildren().add(dangerFlash);

        StackPane tableWrap = new StackPane(table, fxLayer);
        tableWrap.setPadding(new Insets(8, 14, 8, 14));
        fxLayer.prefWidthProperty().bind(tableWrap.widthProperty());
        fxLayer.prefHeightProperty().bind(tableWrap.heightProperty());
        dangerFlash.prefWidthProperty().bind(tableWrap.widthProperty());
        dangerFlash.prefHeightProperty().bind(tableWrap.heightProperty());

        root.setCenter(tableWrap);

        // ---------------- Bottom：隐藏手牌 + 操作条 ----------------
        VBox bottom = new VBox(8, handView, actionBar);
        bottom.setAlignment(Pos.CENTER);
        bottom.getStyleClass().add("liar-bottom");
        root.setBottom(bottom);

        // ---------------- Right：出牌历史面板 ----------------
        log.setPrefWidth(232);
        log.setMinWidth(180);
        root.setRight(log);

        // ---------------- 结算演出横幅（最上层，默认隐藏） ----------------
        bannerTitle.getStyleClass().add("liar-banner-title");
        bannerSubtitle.getStyleClass().add("liar-banner-subtitle");
        bannerCard.getStyleClass().add("liar-banner-card");
        bannerCard.setAlignment(Pos.CENTER);
        bannerCard.getChildren().addAll(bannerTitle, bannerSubtitle);
        bannerLayer.setMouseTransparent(true);
        bannerLayer.setVisible(false);
        bannerLayer.getChildren().add(bannerCard);
        StackPane.setAlignment(bannerLayer, Pos.CENTER);

        getChildren().addAll(root, bannerLayer);
    }

    // ============================================================= 访问器

    /**
     * 按固定方位取座位：0 = 南（本人，底部），1 = 北（顶部），
     * 2 = 西（左侧），3 = 东（右侧）。index 与 {@code PlayerId.ordinal()} 对齐。
     */
    public LiarPlayerSeat getSeat(int index) {
        return index >= 0 && index < SEAT_COUNT ? seats[index] : null;
    }

    /** 全部座位（只读）。 */
    public List<LiarPlayerSeat> getSeats() {
        return List.of(seats);
    }

    /** 中央声明卡（大号）。 */
    public LiarClaimPanel getClaimPanel() {
        return claimPanel;
    }

    /** 顶部紧凑声明面板。 */
    public LiarClaimPanel getHeaderClaim() {
        return headerClaim;
    }

    /** 怀疑度指示器。 */
    public LiarRiskIndicator getRiskIndicator() {
        return riskIndicator;
    }

    /** 隐藏手牌区域。 */
    public LiarHandView getHandView() {
        return handView;
    }

    /** 操作按钮条。 */
    public LiarActionBar getActionBar() {
        return actionBar;
    }

    /** 顶部金币栏。 */
    public CoinBar getCoinBar() {
        return coinBar;
    }

    /** 游戏日志。 */
    public PlayHistoryPanel getLog() {
        return log;
    }

    /** 桌面容器（粒子 / 光效挂载点）。 */
    public Pane getFxLayer() {
        return fxLayer;
    }

    /** 根节点（{@code GameAnimationService.playWinAnimation} 需要 Pane）。 */
    public Pane getRootPane() {
        return fxLayer;
    }

    /** 供外部定位使用的节点（飞牌 / Toast 锚点）。 */
    public Node getCenterAnchor() {
        return claimPanel;
    }

    // ============================================================= 文本

    /** 顶部阶段文案。 */
    public void setPhaseText(String text) {
        phaseLabel.setText("阶段：" + (text == null || text.isBlank() ? "—" : text));
    }

    /** 顶部存活人数。 */
    public void setAliveText(int alive) {
        lifeLabel.setText("存活：" + Math.max(0, alive) + "/" + SEAT_COUNT);
    }

    /** 顶部辅助提示（空文本隐藏）。 */
    public void setTipText(String text) {
        String value = text == null ? "" : text.trim();
        tipLabel.setText(value);
        boolean show = !value.isBlank();
        tipLabel.setVisible(show);
        tipLabel.setManaged(show);
    }

    /** 日志内容（时间顺序，最新在最后）。 */
    public void setLogEntries(List<String> entries) {
        log.setEvents(entries);
    }

    // ============================================================= 动画

    /**
     * 显示中央结算横幅（质疑分阶段演出用）。
     *
     * @param title    主标题，如「西家 选择质疑！」
     * @param subtitle 副标题（可 null），如「翻牌验证中…」
     * @param tone     色调：challenge / success / danger / gun-hit / gun-miss
     */
    public void showBanner(String title, String subtitle, String tone) {
        bannerTitle.setText(title == null ? "" : title);
        bannerSubtitle.setText(subtitle == null ? "" : subtitle);
        bannerSubtitle.setVisible(subtitle != null && !subtitle.isBlank());
        bannerSubtitle.setManaged(subtitle != null && !subtitle.isBlank());
        bannerCard.getStyleClass().removeAll(
                "liar-banner-challenge", "liar-banner-success",
                "liar-banner-danger", "liar-banner-gun-hit", "liar-banner-gun-miss");
        bannerCard.getStyleClass().add("liar-banner-" + (tone == null ? "challenge" : tone));
        bannerLayer.setVisible(true);
        if (!GameAnimationService.getInstance().isEnabled()) {
            bannerCard.setOpacity(1.0);
            bannerCard.setScaleX(1.0);
            bannerCard.setScaleY(1.0);
            return;
        }
        bannerCard.setOpacity(0.0);
        bannerCard.setScaleX(0.82);
        bannerCard.setScaleY(0.82);
        FadeTransition fade = new FadeTransition(Duration.millis(220), bannerCard);
        fade.setToValue(1.0);
        fade.play();
        ScaleTransition pop = new ScaleTransition(Duration.millis(260), bannerCard);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    /** 隐藏中央结算横幅。 */
    public void hideBanner() {
        bannerLayer.setVisible(false);
    }

    /** 质疑结算：暗红屏幕闪烁 + 声明卡震动。 */
    public void playChallengeEffect() {
        claimPanel.playChallengeShake();
        dangerFlash.setOpacity(0.0);
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(dangerFlash.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(90), new KeyValue(dangerFlash.opacityProperty(), 0.45)),
                new KeyFrame(Duration.millis(420), new KeyValue(dangerFlash.opacityProperty(), 0.0)));
        flash.play();
    }

    /** 失败：更重的暗红屏幕（终局输家）。 */
    public void playDefeatEffect() {
        dangerFlash.setOpacity(0.0);
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(dangerFlash.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(160), new KeyValue(dangerFlash.opacityProperty(), 0.62)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(dangerFlash.opacityProperty(), 0.28, Interpolator.EASE_BOTH)));
        flash.play();
    }

    /** 胜利：金色光晕扫过整桌。 */
    public void playVictoryGlow() {
        Region gold = new Region();
        gold.getStyleClass().add("liar-victory-glow");
        gold.setOpacity(0.0);
        gold.setManaged(false);
        gold.prefWidthProperty().bind(fxLayer.prefWidthProperty());
        gold.prefHeightProperty().bind(fxLayer.prefHeightProperty());
        fxLayer.getChildren().add(gold);
        FadeTransition fade = new FadeTransition(Duration.millis(320), gold);
        fade.setFromValue(0.0);
        fade.setToValue(0.72);
        fade.setAutoReverse(true);
        fade.setCycleCount(4);
        fade.setOnFinished(e -> fxLayer.getChildren().remove(gold));
        fade.play();
    }

    /** 默认头像字形（本人 / 三家 AI）。 */
    private static String defaultGlyph(int index) {
        return switch (index) {
            case 0 -> "♛";
            case 1 -> "♚";
            case 2 -> "♝";
            default -> "♞";
        };
    }
}
