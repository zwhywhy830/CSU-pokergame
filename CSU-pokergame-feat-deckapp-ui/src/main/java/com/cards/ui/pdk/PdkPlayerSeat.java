package com.cards.ui.pdk;

import com.cards.ui.component.AvatarView;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * 阶段 23：跑得快玩家座位（手机棋牌 App 风格）。
 *
 * <p>横向布局：[圆形头像 + 状态光环] + [等级徽章 / 昵称 / 剩余牌数 / 状态]。
 *
 * <p>头像外圈三态（需求「二、牌桌布局重构 → 顶部」）：
 * <ul>
 *   <li>正常：灰色描边（{@code .pdk-seat} 下 {@code .avatar-circle} 覆写）；</li>
 *   <li>当前行动：金色呼吸光（{@link State#ACTIVE} / {@link State#THINKING}）；</li>
 *   <li>胜利：金色爆闪（{@link State#WON}）。</li>
 * </ul>
 *
 * <p>数据全部来自调用方传入的展示值（剩余牌数 / 当前回合 / 胜负），本组件不持有引擎引用，
 * 也不新增任何规则字段。
 */
public final class PdkPlayerSeat extends HBox {

    /** 座位运行态。 */
    public enum State {
        /** 等待中（非当前回合）。 */
        WAITING,
        /** 当前回合（玩家自己）。 */
        ACTIVE,
        /** AI 正在思考。 */
        THINKING,
        /** 已获胜。 */
        WON,
        /** 已失败 / 已出局。 */
        LOST
    }

    /** 头像直径。 */
    private static final double AVATAR_SIZE = 46;
    /** 当前回合的绿色状态点颜色（由 AvatarView 内部管理，这里只做展示）。 */

    private final AvatarView avatar;
    private final Circle ring;
    private final Label crownLabel;
    private final StackPane avatarWrap;
    private final Label levelLabel;
    private final Label nameLabel;
    private final Label countLabel;
    private final Label statusLabel;

    private Timeline ringPulse;

    /**
     * @param glyph  头像字形
     * @param name   昵称
     * @param level  等级
     * @param isSelf 是否本人（影响外边距与等级配色）
     */
    public PdkPlayerSeat(String glyph, String name, int level, boolean isSelf) {
        getStyleClass().add("pdk-seat");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(9);
        setMinWidth(168);
        if (isSelf) {
            getStyleClass().add("pdk-seat-self");
        }

        // ---------------- 圆形头像 + 光环 + 皇冠 ----------------
        avatar = new AvatarView(glyph, name, null, level);
        avatar.setCompact(true);
        avatar.setCircleSize(AVATAR_SIZE);
        avatar.getStyleClass().add("pdk-seat-avatar");

        ring = new Circle(AVATAR_SIZE / 2 + 4);
        ring.getStyleClass().add("pdk-seat-ring");
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Theme.GOLD_BRIGHT);
        ring.setStrokeWidth(2.5);
        ring.setStrokeType(StrokeType.OUTSIDE);
        ring.setMouseTransparent(true);
        ring.setVisible(false);
        StackPane.setAlignment(ring, Pos.CENTER);

        crownLabel = new Label("♛");
        crownLabel.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 15));
        crownLabel.setTextFill(Theme.GOLD_BRIGHT);
        crownLabel.setMouseTransparent(true);
        crownLabel.setVisible(false);
        StackPane.setAlignment(crownLabel, Pos.TOP_CENTER);
        StackPane.setMargin(crownLabel, new Insets(-10, 0, 0, 0));

        avatarWrap = new StackPane(avatar, ring, crownLabel);
        avatarWrap.setAlignment(Pos.CENTER);

        // ---------------- 文字信息列 ----------------
        levelLabel = new Label();
        levelLabel.getStyleClass().add("pdk-seat-level");

        nameLabel = new Label(name == null ? "玩家" : name);
        nameLabel.getStyleClass().add("pdk-seat-name");

        countLabel = new Label();
        countLabel.getStyleClass().add("pdk-seat-count");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("pdk-seat-status");

        VBox info = new VBox(1, levelLabel, nameLabel, countLabel, statusLabel);
        info.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(avatarWrap, info);
        setLevel(level);
        setCardCount(-1);
    }

    // ============================================================= 内容

    /** 设置等级徽章，显示为 {@code "● Lv.N"}。 */
    public void setLevel(int level) {
        levelLabel.setText("● Lv." + Math.max(1, level));
    }

    /** 设置昵称。 */
    public void setPlayerName(String name) {
        nameLabel.setText(name == null || name.isBlank() ? "玩家" : name);
    }

    /** 设置剩余牌数；传 -1 隐藏。 */
    public void setCardCount(int count) {
        boolean show = count >= 0;
        if (show) {
            countLabel.setText("剩余 " + count + " 张");
        }
        countLabel.setVisible(show);
        countLabel.setManaged(show);
    }

    /** 设置状态文案（空文本隐藏该行）。 */
    public void setStatusText(String text) {
        String value = text == null ? "" : text;
        statusLabel.setText(value);
        boolean show = !value.isBlank();
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
    }

    // ============================================================= 状态切换

    /** 切换座位运行态，自动管理光环 / 皇冠 / 透明度 / 状态文案。 */
    public void setState(State state) {
        getStyleClass().removeAll("pdk-seat-active", "pdk-seat-won", "pdk-seat-lost");
        switch (state == null ? State.WAITING : state) {
            case ACTIVE -> {
                getStyleClass().add("pdk-seat-active");
                showRing(true);
                crownLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("轮到你出牌");
                startRingPulse();
            }
            case THINKING -> {
                getStyleClass().add("pdk-seat-active");
                showRing(true);
                crownLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("思考中…");
                startRingPulse();
            }
            case WON -> {
                getStyleClass().add("pdk-seat-won");
                stopRingPulse();
                showRing(true);
                crownLabel.setVisible(true);
                setOpacity(1.0);
                setStatusText("获胜");
                playWinBurst();
            }
            case LOST -> {
                getStyleClass().add("pdk-seat-lost");
                stopRingPulse();
                showRing(false);
                crownLabel.setVisible(false);
                setOpacity(0.45);
                setStatusText("出局");
            }
            case WAITING -> {
                stopRingPulse();
                showRing(false);
                crownLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("");
            }
        }
    }

    /** 头像节点（结算升级光环等复用）。 */
    public AvatarView getAvatar() {
        return avatar;
    }

    // ============================================================= 内部动效

    private void showRing(boolean visible) {
        ring.setVisible(visible);
        ring.setOpacity(visible ? 1.0 : 0.0);
    }

    /** 金色呼吸光（当前行动）。 */
    private void startRingPulse() {
        if (!GameAnimationService.getInstance().isEnabled()) {
            ring.setStroke(Theme.GOLD_BRIGHT);
            return;
        }
        if (ringPulse != null && ringPulse.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        ringPulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(ring.opacityProperty(), 0.30)),
                new KeyFrame(Duration.millis(800), new KeyValue(ring.opacityProperty(), 1.0)));
        ringPulse.setAutoReverse(true);
        ringPulse.setCycleCount(Animation.INDEFINITE);
        ringPulse.play();
    }

    private void stopRingPulse() {
        if (ringPulse != null) {
            ringPulse.stop();
            ringPulse = null;
        }
        ring.setOpacity(1.0);
    }

    /** 胜利金色爆闪：光环放大 + 高亮后收敛。 */
    private void playWinBurst() {
        ring.setStroke(Theme.GOLD_BRIGHT);
        ring.setOpacity(1.0);
        if (!GameAnimationService.getInstance().isEnabled()) {
            return;
        }
        ScaleTransition burst = new ScaleTransition(Duration.millis(260), ring);
        burst.setFromX(0.8);
        burst.setFromY(0.8);
        burst.setToX(1.35);
        burst.setToY(1.35);
        burst.setAutoReverse(true);
        burst.setCycleCount(2);
        burst.setInterpolator(Interpolator.EASE_BOTH);
        burst.play();
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(ring.strokeWidthProperty(), 2.5)),
                new KeyFrame(Duration.millis(130), new KeyValue(ring.strokeWidthProperty(), 6.0)),
                new KeyFrame(Duration.millis(320), new KeyValue(ring.strokeWidthProperty(), 2.5)));
        flash.play();
    }
}
