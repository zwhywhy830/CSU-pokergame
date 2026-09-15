package com.cards.ui.liar;

import com.cards.ui.component.AvatarView;
import com.cards.ui.effect.GameAnimationService;
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
 * 阶段 24：骗子酒馆玩家座位（酒馆玻璃 + 暗红金风格）。
 *
 * <p>布局：[圆形头像 + 状态光环] + [Lv 徽章 / 昵称 / 生命值 / 状态文案]。
 *
 * <p>四种状态（<b>仅视觉</b>，不参与任何规则判定）：
 * <ul>
 *   <li>{@link State#NORMAL} 正常 — 😐</li>
 *   <li>{@link State#THINKING} 思考 — 🤔（金色呼吸光环）</li>
 *   <li>{@link State#BLUFFING} 骗人 — 😈（暗红闪光）</li>
 *   <li>{@link State#DEAD} 失败 — 💀（灰化下沉）</li>
 * </ul>
 *
 * <p>数据全部由调用方传入（昵称 / 等级 / 生命值 / 状态），本组件不持有引擎引用，
 * 也不新增任何规则字段。
 */
public final class LiarPlayerSeat extends HBox {

    /** 座位运行态（纯视觉）。 */
    public enum State {
        /** 正常待机。 */
        NORMAL,
        /** 正在思考。 */
        THINKING,
        /** 正在诈唬（视觉标记，非规则判定）。 */
        BLUFFING,
        /** 已失败 / 出局。 */
        DEAD
    }

    /** 头像直径。 */
    private static final double AVATAR_SIZE = 48;
    /** 最大生命值。 */
    private static final int MAX_LIFE = 3;

    private final AvatarView avatar;
    private final Circle ring;
    private final Label skullLabel;
    private final StackPane avatarWrap;
    private final Label levelLabel;
    private final Label nameLabel;
    private final Label lifeLabel;
    private final Label statusLabel;

    private Timeline ringPulse;
    private State state = State.NORMAL;
    private int life = MAX_LIFE;

    /**
     * @param glyph  头像字形
     * @param name   昵称
     * @param level  等级
     * @param isSelf 是否本人（影响亮度与描边）
     */
    public LiarPlayerSeat(String glyph, String name, int level, boolean isSelf) {
        getStyleClass().add("liar-seat");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(9);
        setMinWidth(172);
        if (isSelf) {
            getStyleClass().add("liar-seat-self");
        }

        // ---------------- 圆形头像 + 光环 ----------------
        avatar = new AvatarView(glyph, name, null, level);
        avatar.setCompact(true);
        avatar.setCircleSize(AVATAR_SIZE);
        avatar.getStyleClass().add("liar-seat-avatar");

        ring = new Circle(AVATAR_SIZE / 2 + 4);
        ring.getStyleClass().add("liar-seat-ring");
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#ffd54f"));
        ring.setStrokeWidth(2.5);
        ring.setStrokeType(StrokeType.OUTSIDE);
        ring.setMouseTransparent(true);
        ring.setVisible(false);
        StackPane.setAlignment(ring, Pos.CENTER);

        skullLabel = new Label("💀");
        skullLabel.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 14));
        skullLabel.setMouseTransparent(true);
        skullLabel.setVisible(false);
        StackPane.setAlignment(skullLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(skullLabel, new Insets(0, -4, -2, 0));

        avatarWrap = new StackPane(avatar, ring, skullLabel);
        avatarWrap.setAlignment(Pos.CENTER);

        // ---------------- 文字信息列 ----------------
        levelLabel = new Label();
        levelLabel.getStyleClass().add("liar-seat-level");

        nameLabel = new Label(name == null ? "玩家" : name);
        nameLabel.getStyleClass().add("liar-seat-name");

        lifeLabel = new Label();
        lifeLabel.getStyleClass().add("liar-seat-life");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("liar-seat-status");

        VBox info = new VBox(1, levelLabel, nameLabel, lifeLabel, statusLabel);
        info.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(avatarWrap, info);
        setLevel(level);
        setLife(MAX_LIFE);
        setState(State.NORMAL);
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

    /**
     * 设置生命值（0 ~ 3），渲染为 {@code ❤️❤️🖤}。
     * 传负数隐藏该行（联机桌等无生命值概念的场景）。
     */
    public void setLife(int value) {
        if (value < 0) {
            lifeLabel.setVisible(false);
            lifeLabel.setManaged(false);
            return;
        }
        this.life = Math.max(0, Math.min(MAX_LIFE, value));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_LIFE; i++) {
            sb.append(i < life ? "❤️" : "🖤");
        }
        lifeLabel.setText(sb.toString());
        lifeLabel.setVisible(true);
        lifeLabel.setManaged(true);
        getStyleClass().remove("liar-seat-lowlife");
        if (life == 1) {
            getStyleClass().add("liar-seat-lowlife");
        }
    }

    /** 当前生命值。 */
    public int getLife() {
        return life;
    }

    /** 设置状态文案（空文本隐藏该行）。 */
    public void setStatusText(String text) {
        String value = text == null ? "" : text;
        statusLabel.setText(value);
        boolean show = !value.isBlank();
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
    }

    /** 头像节点（结算 / 胜利光效复用）。 */
    public AvatarView getAvatar() {
        return avatar;
    }

    // ============================================================= 状态切换

    /** 当前状态。 */
    public State getState() {
        return state;
    }

    /** 正常待机：😐。 */
    public void setNormal() {
        setState(State.NORMAL);
    }

    /** 思考中：🤔（金色呼吸光）。 */
    public void setThinking() {
        setState(State.THINKING);
    }

    /** 诈唬中：😈（暗红闪光，仅视觉标记）。 */
    public void setBluffing() {
        setState(State.BLUFFING);
    }

    /** 失败 / 出局：💀（灰化下沉）。 */
    public void setDead() {
        setState(State.DEAD);
    }

    /** 切换座位运行态，自动管理光环 / 骷髅 / 透明度 / 状态文案。 */
    public void setState(State next) {
        this.state = next == null ? State.NORMAL : next;
        getStyleClass().removeAll("liar-seat-active", "liar-seat-bluff", "liar-seat-dead", "liar-seat-live");
        switch (this.state) {
            case NORMAL -> {
                stopRingPulse();
                showRing(false);
                skullLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("😐 正常");
            }
            case THINKING -> {
                getStyleClass().add("liar-seat-live");
                showRing(true);
                ring.setStroke(Color.web("#ffd54f"));
                skullLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("🤔 思考中");
                startRingPulse();
            }
            case BLUFFING -> {
                getStyleClass().add("liar-seat-bluff");
                showRing(true);
                ring.setStroke(Color.web("#e0574f"));
                skullLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("😈 骗人中");
                startRingPulse();
            }
            case DEAD -> {
                getStyleClass().add("liar-seat-dead");
                stopRingPulse();
                showRing(false);
                skullLabel.setVisible(true);
                setOpacity(0.45);
                setStatusText("💀 失败");
            }
        }
    }

    /** 金色胜利爆闪（终局使用）。 */
    public void playWinBurst() {
        ring.setStroke(Color.web("#ffd54f"));
        showRing(true);
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
    }

    /** 红色震动（被质疑 / 扣血时使用）。 */
    public void playShake() {
        if (!GameAnimationService.getInstance().isEnabled()) {
            return;
        }
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(translateXProperty(), 0),
                        new KeyValue(scaleXProperty(), 1.0),
                        new KeyValue(scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(60),
                        new KeyValue(translateXProperty(), -9),
                        new KeyValue(scaleXProperty(), 1.06),
                        new KeyValue(scaleYProperty(), 1.06)),
                new KeyFrame(Duration.millis(130), new KeyValue(translateXProperty(), 9)),
                new KeyFrame(Duration.millis(200), new KeyValue(translateXProperty(), -5)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(translateXProperty(), 0),
                        new KeyValue(scaleXProperty(), 1.0),
                        new KeyValue(scaleYProperty(), 1.0)));
        shake.play();
    }

    // ============================================================= 内部动效

    private void showRing(boolean visible) {
        ring.setVisible(visible);
        ring.setOpacity(visible ? 1.0 : 0.0);
    }

    private void startRingPulse() {
        if (!GameAnimationService.getInstance().isEnabled()) {
            ring.setOpacity(1.0);
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
}
