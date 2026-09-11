package com.cards.ui.component;

import com.cards.ui.theme.DesignTokens;
import com.cards.ui.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 牌桌座位视图：复用 {@link AvatarView} 展示玩家头像，叠加牌数徽章、
 * 当前回合金色光环、AI 思考提示、胜利皇冠 / 失败灰化等效果。
 *
 * <p>数据来源仅依赖现有快照字段（remainingCardCounts / currentPlayer / winner 等），
 * 不持有任何引擎引用，状态由调用方在每次刷新时设置。
 *
 * <p>适用于跑得快牌桌（3 座）与骗子酒馆牌桌（4 座）。
 */
public final class SeatView extends VBox {

    /** 座位运行态，决定光环 / 文案 / 透明度。 */
    public enum State {
        /** 等待中（非当前回合、未淘汰）。 */
        WAITING,
        /** 当前回合（玩家自己轮到）。 */
        ACTIVE,
        /** AI 正在思考（bot 延迟窗口内）。 */
        THINKING,
        /** 已获胜。 */
        WON,
        /** 已失败 / 已淘汰。 */
        LOST
    }

    private static final double RING_SIZE = DesignTokens.AVATAR_SIZE + 6;

    private final AvatarView avatar;
    private final Label cardCountLabel;
    private final Label statusLabel;
    private final Circle goldRing;
    private final Label crownLabel;
    private final StackPane avatarWrap;

    private Timeline ringPulse;
    private boolean compactExtra = false;

    /**
     * 构造座位。
     *
     * @param glyph   头像字形（♛ ♚ 🃏 🤖 等）
     * @param name    玩家名
     * @param level   等级
     * @param isSelf  是否为玩家自己（影响头像大小与紧凑模式）
     */
    public SeatView(String glyph, String name, int level, boolean isSelf) {
        super(DesignTokens.SPACING_XS);
        getStyleClass().add("seat-view");
        setAlignment(Pos.CENTER);

        // 头像（紧凑模式 + 尺寸）
        avatar = new AvatarView(glyph, name, null, level);
        avatar.setCompact(true);
        double size = isSelf ? DesignTokens.AVATAR_SIZE : DesignTokens.AVATAR_SIZE * 0.86;
        avatar.setCircleSize(size);
        avatar.getStyleClass().add("seat-avatar");

        // 金色光环（当前回合）
        goldRing = new Circle(size / 2 + 3);
        goldRing.getStyleClass().add("seat-ring");
        goldRing.setFill(Color.TRANSPARENT);
        goldRing.setStroke(Theme.GOLD_BRIGHT);
        goldRing.setStrokeWidth(2.5);
        goldRing.setStrokeType(StrokeType.OUTSIDE);
        goldRing.setMouseTransparent(true);
        goldRing.setVisible(false);
        StackPane.setAlignment(goldRing, Pos.CENTER);

        // 皇冠（胜利时显示）
        crownLabel = new Label("♛");
        crownLabel.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 16));
        crownLabel.setTextFill(Theme.GOLD_BRIGHT);
        crownLabel.setVisible(false);
        crownLabel.setMouseTransparent(true);
        StackPane.setAlignment(crownLabel, Pos.TOP_CENTER);
        StackPane.setMargin(crownLabel, new Insets(-8, 0, 0, 0));

        avatarWrap = new StackPane(avatar, goldRing, crownLabel);
        avatarWrap.setAlignment(Pos.CENTER);

        // 牌数徽章
        cardCountLabel = new Label();
        cardCountLabel.getStyleClass().add("seat-card-badge");
        cardCountLabel.setVisible(false);

        // 状态文本（思考中…/已出完/已淘汰 等）
        statusLabel = new Label();
        statusLabel.getStyleClass().add("seat-status");

        getChildren().addAll(avatarWrap, cardCountLabel, statusLabel);
    }

    // ============================================================= 内容设置

    /** 设置剩余牌数（跑得快用）。传 -1 隐藏徽章。 */
    public void setCardCount(int count) {
        if (count < 0) {
            cardCountLabel.setVisible(false);
            return;
        }
        cardCountLabel.setText("剩 " + count + " 张");
        cardCountLabel.setVisible(true);
    }

    /** 设置额外信息文案（骗子酒馆手枪仓等），显示在徽章位置。 */
    public void setExtraInfo(String text) {
        if (text == null || text.isBlank()) {
            cardCountLabel.setVisible(false);
            return;
        }
        cardCountLabel.setText(text);
        cardCountLabel.setVisible(true);
    }

    /** 设置状态文案。 */
    public void setStatusText(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    // ============================================================= 状态切换

    /** 切换座位运行态，自动管理光环、皇冠、透明度、思考提示。 */
    public void setState(State state) {
        switch (state) {
            case ACTIVE -> {
                goldRing.setVisible(true);
                crownLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("");
                startRingPulse();
            }
            case THINKING -> {
                goldRing.setVisible(true);
                crownLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("思考中…");
                startRingPulse();
            }
            case WAITING -> {
                stopRingPulse();
                goldRing.setVisible(false);
                crownLabel.setVisible(false);
                setOpacity(1.0);
                setStatusText("");
            }
            case WON -> {
                stopRingPulse();
                goldRing.setVisible(false);
                crownLabel.setVisible(true);
                setOpacity(1.0);
                setStatusText("获胜");
            }
            case LOST -> {
                stopRingPulse();
                goldRing.setVisible(false);
                crownLabel.setVisible(false);
                setOpacity(0.40);
                setStatusText("出局");
            }
        }
    }

    // ============================================================= 光环呼吸动画

    private void startRingPulse() {
        if (ringPulse != null && ringPulse.getStatus() == Animation.Status.RUNNING) return;
        ringPulse = new Timeline(
                new KeyFrame(DesignTokens.ANIM_PULSE_SLOW.divide(2),
                        new KeyValue(goldRing.strokeProperty(),
                                Color.color(Theme.GOLD_BRIGHT.getRed(),
                                        Theme.GOLD_BRIGHT.getGreen(),
                                        Theme.GOLD_BRIGHT.getBlue(), 0.35))),
                new KeyFrame(DesignTokens.ANIM_PULSE_SLOW,
                        new KeyValue(goldRing.strokeProperty(), Theme.GOLD_BRIGHT)));
        ringPulse.setAutoReverse(true);
        ringPulse.setCycleCount(Animation.INDEFINITE);
        ringPulse.play();
    }

    private void stopRingPulse() {
        if (ringPulse != null) {
            ringPulse.stop();
            ringPulse = null;
        }
        goldRing.setStroke(Theme.GOLD_BRIGHT);
    }

    // ============================================================= Getter

    public AvatarView getAvatar() { return avatar; }
}
