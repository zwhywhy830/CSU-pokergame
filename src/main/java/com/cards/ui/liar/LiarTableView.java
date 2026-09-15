package com.cards.ui.liar;

import com.cards.ui.component.CoinBar;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
 *   Center : 环形座位的酒馆桌面
 *              (0,1) AI 1        ← 上
 *              (1,0) AI 2   (1,1) 中央声明卡   (1,2) AI 3
 *              (2,1) 玩家        ← 下
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
    private final ListView<String> log = new ListView<>();

    private final LiarPlayerSeat[] seats = new LiarPlayerSeat[SEAT_COUNT];
    private final Pane fxLayer = new Pane();
    private final Region dangerFlash = new Region();

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

        table.add(seats[1], 1, 0);   // 上：AI 1
        table.add(seats[2], 0, 1);   // 左：AI 2
        table.add(centerBox, 1, 1);  // 中：声明卡 + 怀疑度
        table.add(seats[3], 2, 1);   // 右：AI 3
        table.add(seats[0], 1, 2);   // 下：本人

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

        // ---------------- Right：日志 ----------------
        log.getStyleClass().add("liar-log");
        log.setPrefWidth(232);
        log.setMinWidth(180);
        root.setRight(log);

        getChildren().add(root);
    }

    // ============================================================= 访问器

    /** 第 index 个座位（0 = 本人，1~3 = AI）。 */
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
    public ListView<String> getLog() {
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

    /** 日志内容（保留最近 60 条）。 */
    public void setLogEntries(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            log.getItems().clear();
            return;
        }
        log.getItems().setAll(entries.subList(Math.max(0, entries.size() - 60), entries.size()));
    }

    // ============================================================= 动画

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
