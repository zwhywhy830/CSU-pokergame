package com.cards.ui.component;

import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.theme.DesignTokens;
import com.cards.ui.theme.Theme;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * 玩家头像组件：圆形头像 + 昵称 + 在线状态点 + 等级徽章。
 *
 * <p>第一阶段支持 Unicode 字形头像（♛ ♚ 🃏 🤖 等，与个人信息页候选一致）；
 * 图片头像通过 {@link #setAvatarResource(String)} / {@link #setAvatarImage(Image)}
 * 预留（未来读取 {@code resources/assets/avatar/} 下的 PNG），图片缺失时自动回退字形。
 *
 * <p>两种形态：
 * <ul>
 *   <li>完整形态（默认）：圆头像 + 昵称/等级 + 状态文本，用于首页玩家区、大厅玩家卡；</li>
 *   <li>紧凑形态（{@link #setCompact(boolean)}）：仅圆头像 + 状态点，用于牌桌 AI 座位。</li>
 * </ul>
 */
public final class AvatarView extends HBox {

    /** 在线状态，决定右下角状态点颜色。 */
    public enum Status {
        ONLINE, IN_GAME, BUSY, OFFLINE
    }

    private final StackPane circle;
    private final Text glyph;
    private final ImageView imageView;
    private final Circle dot;
    private final Label nameLabel;
    private final Label levelLabel;
    private final Label statusLabel;
    private final VBox textCol;
    /** 等级称号：徽章（emoji）+ 称号文字（青铜/白银/黄金/大师），默认隐藏。 */
    private final Text badgeText;
    private final Label titleLabel;
    private final HBox titleBox;
    /** 成长信息块：经验进度条 + 当前/需求经验 + 金币数量，默认隐藏（首页玩家卡开启）。 */
    private VBox growthBox;
    private ProgressBar expBar;
    private Label expTextLabel;
    private Label goldValueLabel;
    /** 胜率行（阶段 17）：无胜率可展示（传入负数）时整行隐藏。 */
    private HBox winRateRow;
    private Label winRateValueLabel;

    private double circleSize = DesignTokens.AVATAR_SIZE;

    public AvatarView(String avatarGlyph, String name, String statusText, int level) {
        getStyleClass().add("avatar-view");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(DesignTokens.SPACING_SM);

        // ---------------- 圆形头像 ----------------
        circle = new StackPane();
        circle.getStyleClass().add("avatar-circle");
        circle.setMinSize(circleSize, circleSize);
        circle.setPrefSize(circleSize, circleSize);
        circle.setMaxSize(circleSize, circleSize);

        glyph = new Text(avatarGlyph == null ? "♛" : avatarGlyph);
        glyph.getStyleClass().add("avatar-glyph");
        applyGlyphFont(DesignTokens.FONT_AVATAR_GLYPH);
        glyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.GOLD_LIGHT),
                new Stop(0.55, Theme.GOLD),
                new Stop(1, Theme.GOLD_DARK)));

        imageView = new ImageView();
        imageView.setFitWidth(circleSize);
        imageView.setFitHeight(circleSize);
        imageView.setClip(new Circle(circleSize / 2, circleSize / 2, circleSize / 2));
        imageView.setVisible(false);
        imageView.setMouseTransparent(true);

        // 右下角在线状态点
        dot = new Circle(7);
        dot.getStyleClass().add("avatar-dot");
        dot.setFill(Theme.STATUS_ONLINE);
        dot.setStroke(Color.web("#0e2418"));
        dot.setStrokeWidth(2);
        StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);

        circle.getChildren().addAll(imageView, glyph, dot);

        // ---------------- 文本列：昵称 + 称号徽章 + 等级 / 成长信息 / 状态 ----------------
        nameLabel = new Label(name == null ? "玩家" : name);
        nameLabel.getStyleClass().add("avatar-name");

        // 等级称号（徽章 emoji + 称号文字），默认隐藏，首页玩家卡通过 setLevelTitle 开启
        badgeText = new Text();
        badgeText.getStyleClass().add("avatar-badge");
        badgeText.setFont(Font.font("Segoe UI Emoji", FontWeight.NORMAL, 13));
        titleLabel = new Label();
        titleLabel.getStyleClass().add("avatar-title");
        titleBox = new HBox(4, badgeText, titleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        setTitleVisible(false);

        levelLabel = new Label();
        levelLabel.getStyleClass().add("avatar-level");

        HBox nameRow = new HBox(DesignTokens.SPACING_XS, nameLabel, titleBox, levelLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label(statusText == null ? "" : statusText);
        statusLabel.getStyleClass().add("avatar-status");

        // 成长信息块（经验进度 + 金币），默认隐藏，首页玩家卡通过 setGrowthInfo 开启
        growthBox = buildGrowthBox();

        textCol = new VBox(3, nameRow, growthBox, statusLabel);
        textCol.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(circle, textCol);

        setLevel(level);
        setStatusText(statusText);
    }

    /** 构建成长信息块（经验进度条 + 当前/需求经验 + 金币），初始隐藏。 */
    private VBox buildGrowthBox() {
        expBar = new ProgressBar(0);
        expBar.getStyleClass().add("avatar-exp-bar");
        expBar.setPrefWidth(96);
        expBar.setMinWidth(96);
        expBar.setMaxWidth(96);

        expTextLabel = new Label("0/0");
        expTextLabel.getStyleClass().add("avatar-metric-value");

        Label expKey = new Label("经验");
        expKey.getStyleClass().add("avatar-metric-key");
        HBox expRow = new HBox(6, expKey, expBar, expTextLabel);
        expRow.setAlignment(Pos.CENTER_LEFT);

        Label goldKey = new Label("金币");
        goldKey.getStyleClass().add("avatar-metric-key");
        goldValueLabel = new Label("0");
        goldValueLabel.getStyleClass().addAll("avatar-metric-value", "avatar-gold-value");
        HBox goldRow = new HBox(6, goldKey, goldValueLabel);
        goldRow.setAlignment(Pos.CENTER_LEFT);

        // 胜率行（阶段 17）：默认隐藏，由 setGrowthInfo(..., winRatePercent) 按需开启
        Label winRateKey = new Label("胜率");
        winRateKey.getStyleClass().add("avatar-metric-key");
        winRateValueLabel = new Label("0%");
        winRateValueLabel.getStyleClass().addAll("avatar-metric-value", "avatar-winrate-value");
        winRateRow = new HBox(6, winRateKey, winRateValueLabel);
        winRateRow.setAlignment(Pos.CENTER_LEFT);
        winRateRow.setVisible(false);
        winRateRow.setManaged(false);

        VBox box = new VBox(3, expRow, goldRow, winRateRow);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setVisible(false);
        box.setManaged(false);
        return box;
    }

    // ============================================================= 文本内容

    /** 设置头像字形（Unicode）。设置图片头像后字形自动隐藏。 */
    public void setGlyph(String value) {
        glyph.setText(value == null ? "♛" : value);
        applyGlyphFont(circleSize * 0.44);
    }

    /** 设置昵称。 */
    public void setAvatarName(String value) {
        nameLabel.setText(value == null || value.isBlank() ? "玩家" : value);
    }

    /** 设置等级（显示为 "Lv.N"）。 */
    public void setLevel(int level) {
        levelLabel.setText("Lv." + Math.max(1, level));
    }

    /**
     * 阶段 22：升级光环 —— 头像外围出现金色动画边框，持续 2 秒后自动消失。
     *
     * <p>动画总开关（{@code GameSettings.animationEnabled}）关闭时不做任何效果。
     * 光环是纯装饰节点，鼠标穿透、不参与布局，不影响头像既有功能。
     */
    public void playLevelUpGlow() {
        if (!GameAnimationService.getInstance().isEnabled()) {
            return;
        }
        final Circle ring = new Circle(circleSize / 2 + 3);
        ring.getStyleClass().add("avatar-glow");
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Theme.GOLD_BRIGHT);
        ring.setStrokeWidth(3);
        ring.setMouseTransparent(true);
        ring.setOpacity(0.0);
        circle.getChildren().add(ring);

        Timeline glow = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ring.opacityProperty(), 0.0),
                        new KeyValue(ring.scaleXProperty(), 0.72),
                        new KeyValue(ring.scaleYProperty(), 0.72)),
                new KeyFrame(Duration.millis(240),
                        new KeyValue(ring.opacityProperty(), 0.95),
                        new KeyValue(ring.scaleXProperty(), 1.0),
                        new KeyValue(ring.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(1500),
                        new KeyValue(ring.opacityProperty(), 0.85)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(ring.opacityProperty(), 0.0),
                        new KeyValue(ring.scaleXProperty(), 1.35),
                        new KeyValue(ring.scaleYProperty(), 1.35)));
        glow.setOnFinished(e -> circle.getChildren().remove(ring));
        glow.play();
    }

    /**
     * 阶段 22：单独更新胜率徽章（如 {@code 65%}）；传负数表示隐藏该行。
     * 与 {@link #setGrowthInfo(int, int, int, int)} 共用同一行，不改变成长块的显隐。
     */
    public void setWinRatePercent(int winRatePercent) {
        boolean show = winRatePercent >= 0;
        if (show) {
            winRateValueLabel.setText(Math.min(100, winRatePercent) + "%");
        }
        winRateRow.setVisible(show);
        winRateRow.setManaged(show);
    }

    // ============================================================= 等级称号 / 成长信息

    /**
     * 设置等级称号，显示为 {@code "🥈 白银 Lv.12"}（徽章 + 称号 + 等级）。
     * 徽章与称号都为空时隐藏称号区，仅保留 {@code "Lv.N"}。
     *
     * @param badge 等级徽章 emoji（青铜 🥉 / 白银 🥈 / 黄金 🥇 / 大师 👑）
     * @param title 等级称号文字（青铜 / 白银 / 黄金 / 大师）
     */
    public void setLevelTitle(String badge, String title) {
        boolean hasBadge = badge != null && !badge.isBlank();
        boolean hasTitle = title != null && !title.isBlank();
        badgeText.setText(hasBadge ? badge : "");
        titleLabel.setText(hasTitle ? title : "");
        setTitleVisible(hasBadge || hasTitle);
    }

    private void setTitleVisible(boolean visible) {
        badgeText.setVisible(visible);
        titleLabel.setVisible(visible);
        titleBox.setVisible(visible);
        titleBox.setManaged(visible);
    }

    /**
     * 展示 / 更新成长信息：经验进度条 + {@code "当前/需求"} + 金币数量（不展示胜率行）。
     * 调用后成长块自动显示（首页玩家卡使用）。
     *
     * @param exp     当前经验
     * @param expNeed 升到下一级所需经验
     * @param gold    金币余额
     */
    public void setGrowthInfo(int exp, int expNeed, int gold) {
        setGrowthInfo(exp, expNeed, gold, -1);
    }

    /**
     * 展示 / 更新成长信息：经验进度条 + {@code "当前/需求"} + 金币数量 + 胜率（阶段 17）。
     * 调用后成长块自动显示（首页玩家卡使用）。
     *
     * @param exp            当前经验
     * @param expNeed        升到下一级所需经验
     * @param gold           金币余额
     * @param winRatePercent 胜率百分数；传负数表示不展示胜率行
     */
    public void setGrowthInfo(int exp, int expNeed, int gold, int winRatePercent) {
        int safeNeed = Math.max(1, expNeed);
        int safeExp = Math.max(0, exp);
        expBar.setProgress(Math.min(1.0, (double) safeExp / safeNeed));
        expTextLabel.setText(safeExp + "/" + safeNeed);
        goldValueLabel.setText(String.valueOf(Math.max(0, gold)));
        boolean showWinRate = winRatePercent >= 0;
        if (showWinRate) {
            winRateValueLabel.setText(Math.min(100, winRatePercent) + "%");
        }
        winRateRow.setVisible(showWinRate);
        winRateRow.setManaged(showWinRate);
        growthBox.setVisible(true);
        growthBox.setManaged(true);
    }

    /** 设置状态文本，并按文案推断状态点颜色；空文本时隐藏该行。 */
    public void setStatusText(String text) {
        String value = text == null ? "" : text;
        statusLabel.setText(value);
        boolean show = !value.isBlank();
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
        setStatusKind(inferStatus(value));
    }

    /** 显式设置状态点颜色。 */
    public void setStatusKind(Status status) {
        dot.setFill(switch (status == null ? Status.ONLINE : status) {
            case IN_GAME -> Theme.STATUS_IN_GAME;
            case BUSY -> Theme.STATUS_BUSY;
            case OFFLINE -> Theme.STATUS_OFFLINE;
            case ONLINE -> Theme.STATUS_ONLINE;
        });
    }

    /** 紧凑形态：隐藏文本列，仅保留圆头像（牌桌 AI 座位使用）。 */
    public void setCompact(boolean compact) {
        textCol.setVisible(!compact);
        textCol.setManaged(!compact);
    }

    /** 调整圆头像尺寸（牌桌大头像 / 小座位复用）。 */
    public void setCircleSize(double size) {
        this.circleSize = size;
        circle.setMinSize(size, size);
        circle.setPrefSize(size, size);
        circle.setMaxSize(size, size);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        ((Circle) imageView.getClip()).setCenterX(size / 2);
        ((Circle) imageView.getClip()).setCenterY(size / 2);
        ((Circle) imageView.getClip()).setRadius(size / 2);
        double glyphSize = size * 0.44;
        applyGlyphFont(glyphSize);
    }

    /**
     * 按字形码点选择字体：象棋/扑克符号（♛♚♝… U+2600 区段）走 Segoe UI Symbol，
     * emoji 类头像（🃏🤖 U+1F000 以上）走 Segoe UI Emoji，避免缺字豆腐块。
     */
    private void applyGlyphFont(double sizePx) {
        String g = glyph.getText();
        boolean emoji = g != null && !g.isEmpty() && g.codePointAt(0) >= 0x1F000;
        glyph.setFont(Font.font(emoji ? "Segoe UI Emoji" : "Segoe UI Symbol", FontWeight.BOLD, sizePx));
    }

    // ============================================================= 图片头像（预留）

    /**
     * 直接设置图片头像（图片已加载）。传 null 回退为字形头像。
     * 图片会按圆形裁剪铺满圆徽。
     */
    public void setAvatarImage(Image image) {
        if (image == null) {
            imageView.setImage(null);
            imageView.setVisible(false);
            glyph.setVisible(true);
        } else {
            imageView.setImage(image);
            imageView.setVisible(true);
            glyph.setVisible(false);
        }
    }

    /**
     * 按资源路径加载图片头像（如 {@code "/assets/avatar/bot_1.png"}）。
     * 异步加载；加载失败自动回退字形头像。第一阶段无图片资源时不会被调用。
     */
    public void setAvatarResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            setAvatarImage(null);
            return;
        }
        Image image = new Image(resourcePath, true);
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (isError) {
                Platform.runLater(() -> setAvatarImage(null));
            }
        });
        image.progressProperty().addListener((obs, oldP, newP) -> {
            if (!image.isError() && newP.doubleValue() >= 1.0) {
                Platform.runLater(() -> setAvatarImage(image));
            }
        });
    }

    /** 按中文状态文案推断状态点。 */
    private static Status inferStatus(String text) {
        if (text == null) {
            return Status.ONLINE;
        }
        if (text.contains("游戏中")) {
            return Status.IN_GAME;
        }
        if (text.contains("勿扰")) {
            return Status.BUSY;
        }
        if (text.contains("离线")) {
            return Status.OFFLINE;
        }
        return Status.ONLINE;
    }
}
