package com.cards.ui.pdk;

import com.cards.bridge.CsuCardBridge;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.core.card.Card;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 阶段 23：跑得快正式棋牌卡视图。
 *
 * <p>在现有牌面渲染（{@link CsuCardBridge#faceImage}）之上做「表现层」包装：
 * <ul>
 *   <li>圆角 + 层次阴影（app.css 的 {@code .pdk-card}）；</li>
 *   <li>选中：向上移动 20px + 金色边框 + 阴影增强（{@code .pdk-card-selected}）；</li>
 *   <li>hover：轻微上浮 + 阴影加深（{@code .pdk-card-hover}）。</li>
 * </ul>
 *
 * <p>只承载一张牌的显示与选中视觉，不持有任何引擎 / 规则引用；点击后的选中集合增删由
 * {@link PdkHandView} 统一管理，通过 {@link #setOnToggle(Runnable)} 回调。
 *
 * <p>位移动画受 {@code GameSettings.animationEnabled} 控制：关闭时直接跳到目标位置。
 */
public final class PdkCardView extends StackPane {

    /** 卡牌显示尺寸（与手牌行保持一致的 64×90）。 */
    public static final double CARD_W = 64;
    public static final double CARD_H = 90;

    /** 选中上浮距离（需求：向上移动 20px）。 */
    public static final double SELECT_LIFT = 20;
    /** hover 上浮距离。 */
    private static final double HOVER_LIFT = 6;
    /** 上浮动画时长。 */
    private static final Duration LIFT_TIME = Duration.millis(140);

    private final Card card;
    private final ImageView face;

    private boolean selected;
    private boolean interactive;
    private Runnable onToggle;

    public PdkCardView(Card card) {
        this.card = card;
        getStyleClass().add("pdk-card");
        setMinSize(CARD_W, CARD_H);
        setPrefSize(CARD_W, CARD_H);
        setMaxSize(CARD_W, CARD_H);
        setPickOnBounds(true);

        face = new ImageView(CsuCardBridge.faceImage(card));
        face.setFitWidth(CARD_W);
        face.setFitHeight(CARD_H);
        face.setSmooth(true);
        face.setMouseTransparent(true);
        getChildren().add(face);

        setOnMouseEntered(e -> {
            if (!selected) {
                if (!getStyleClass().contains("pdk-card-hover")) {
                    getStyleClass().add("pdk-card-hover");
                }
                animateLift(-HOVER_LIFT);
            }
        });
        setOnMouseExited(e -> {
            getStyleClass().remove("pdk-card-hover");
            if (!selected) {
                animateLift(0);
            }
        });
        setOnMouseClicked(e -> {
            if (interactive && onToggle != null) {
                onToggle.run();
            }
        });
    }

    /** 该视图承载的牌。 */
    public Card card() {
        return card;
    }

    /** 当前是否选中。 */
    public boolean isSelected() {
        return selected;
    }

    /** 设置是否响应点击。 */
    public void setInteractive(boolean value) {
        this.interactive = value;
        if (!value) {
            getStyleClass().remove("pdk-card-hover");
        }
    }

    /** 点击回调（由 {@link PdkHandView} 注入选中切换逻辑）。 */
    public void setOnToggle(Runnable callback) {
        this.onToggle = callback;
    }

    /** 切换选中视觉：上浮 20px + 金色边框 + 阴影增强。 */
    public void setSelected(boolean value) {
        if (selected == value) {
            return;
        }
        selected = value;
        getStyleClass().remove("pdk-card-selected");
        if (selected) {
            getStyleClass().add("pdk-card-selected");
            getStyleClass().remove("pdk-card-hover");
        }
        animateLift(selected ? -SELECT_LIFT : 0);
    }

    /** 立即复位（清空手牌 / 重建时用，不播动画）。 */
    public void resetSelectionVisual() {
        selected = false;
        getStyleClass().removeAll("pdk-card-selected", "pdk-card-hover");
        setTranslateY(0);
    }

    /** 飞牌动画需要牌面节点本身作为目标。 */
    public ImageView getFaceNode() {
        return face;
    }

    private void animateLift(double targetY) {
        if (!GameAnimationService.getInstance().isEnabled()) {
            setTranslateY(targetY);
            return;
        }
        Timeline lift = new Timeline(new KeyFrame(LIFT_TIME,
                new KeyValue(translateYProperty(), targetY, Interpolator.EASE_OUT)));
        lift.play();
    }
}
