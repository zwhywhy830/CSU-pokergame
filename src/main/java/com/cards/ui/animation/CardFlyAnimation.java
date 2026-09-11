package com.cards.ui.animation;

import com.cards.ui.theme.DesignTokens;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * 卡牌飞行动画：手牌位置 → 桌面中央。
 *
 * <p>组合 TranslateTransition（位移）+ ScaleTransition（缩放）+ FadeTransition（淡入），
 * 时长统一使用 {@link DesignTokens#ANIM_CARD}（420ms）。
 */
public final class CardFlyAnimation {

    private CardFlyAnimation() {
    }

    /**
     * 让目标节点从指定起始坐标飞到其当前布局位置，同时缩放+淡入。
     *
     * <p>调用前提：目标节点已加入场景图且已完成布局（layout），
     * 这样 {@link Node#getBoundsInParent} / scene 坐标转换才有效。
     *
     * @param target    飞入的目标节点（已加到场景图）
     * @param fromX     起始 x（场景坐标系）
     * @param fromY     起始 y（场景坐标系）
     * @param onComplete 动画结束回调（可 null）
     */
    public static void flyTo(Node target, double fromX, double fromY, Runnable onComplete) {
        if (target == null || target.getScene() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // 目标在场景坐标系中的位置
        var bounds = target.localToScene(target.getBoundsInLocal());
        double toX = bounds.getMinX() + bounds.getWidth() / 2;
        double toY = bounds.getMinY() + bounds.getHeight() / 2;

        // 计算位移偏移量（从起点到目标）
        double dx = fromX - toX;
        double dy = fromY - toY;

        Duration duration = DesignTokens.ANIM_CARD;

        // 位移：从起始偏移位置平移到 0,0（即回到布局位置）
        TranslateTransition translate = new TranslateTransition(duration, target);
        translate.setFromX(dx);
        translate.setFromY(dy);
        translate.setToX(0);
        translate.setToY(0);

        // 缩放：从 1.3x 缩到 1.0x（飞入时略大，落定时恢复正常）
        ScaleTransition scale = new ScaleTransition(duration, target);
        scale.setFromX(1.3);
        scale.setFromY(1.3);
        scale.setToX(1.0);
        scale.setToY(1.0);

        // 淡入：从 0.3 到 1.0
        FadeTransition fade = new FadeTransition(duration, target);
        fade.setFromValue(0.3);
        fade.setToValue(1.0);

        ParallelTransition parallel = new ParallelTransition(translate, scale, fade);
        if (onComplete != null) {
            parallel.setOnFinished(e -> onComplete.run());
        }
        parallel.play();
    }

    /**
     * 简化版：只做位移 + 淡入（用于非卡牌节点飞入）。
     */
    public static void flyToSimple(Node target, double fromX, double fromY, Runnable onComplete) {
        if (target == null || target.getScene() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        var bounds = target.localToScene(target.getBoundsInLocal());
        double toX = bounds.getMinX() + bounds.getWidth() / 2;
        double toY = bounds.getMinY() + bounds.getHeight() / 2;
        double dx = fromX - toX;
        double dy = fromY - toY;

        Duration duration = DesignTokens.ANIM_CARD;
        TranslateTransition translate = new TranslateTransition(duration, target);
        translate.setFromX(dx);
        translate.setFromY(dy);
        translate.setToX(0);
        translate.setToY(0);

        FadeTransition fade = new FadeTransition(duration, target);
        fade.setFromValue(0.3);
        fade.setToValue(1.0);

        ParallelTransition parallel = new ParallelTransition(translate, fade);
        if (onComplete != null) {
            parallel.setOnFinished(e -> onComplete.run());
        }
        parallel.play();
    }
}
