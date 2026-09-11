package com.cards.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 阶段 22 统一提示条（Toast）：轻量、居中顶部浮层，用于展示
 * 「+100金币」「升级成功」「获得成就」「购买成功」等一次性反馈。
 *
 * <p>用法：
 * <pre>{@code
 * GameToast.show(anchorNode, "+100 金币");
 * GameToast.show(anchorNode, "✨ Level UP", Duration.millis(1800));
 * }</pre>
 *
 * <p>提示条会挂到 {@code anchorNode} 所在场景的根节点上，自动淡入 / 停留 / 淡出后移除，
 * 鼠标事件完全穿透，不打断下方交互。
 *
 * <p>{@code animated=false}（动画总开关关闭时）只做静态显示 + 定时移除，不做位移 / 淡入淡出。
 */
public final class GameToast extends HBox {

    /** 默认停留时长。 */
    public static final Duration DEFAULT_DURATION = Duration.millis(1600);

    private GameToast(String text) {
        getStyleClass().add("game-toast");
        setAlignment(Pos.CENTER);
        setMouseTransparent(true);
        setPickOnBounds(false);
        // 关键：提示条必须保持自身首选尺寸，否则作为 StackPane 子节点会被拉伸铺满整屏，
        // 其半透明背景会像遮罩一样压暗整个界面。
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label label = new Label(text);
        label.getStyleClass().add("game-toast-label");
        getChildren().add(label);
    }

    /** 以默认时长显示一条提示（带动画）。 */
    public static void show(Node anchor, String text) {
        show(anchor, text, DEFAULT_DURATION);
    }

    /** 以指定时长显示一条提示（带动画）。 */
    public static void show(Node anchor, String text, Duration duration) {
        show(anchor, text, duration, true);
    }

    /**
     * 显示一条提示。
     *
     * @param anchor   场景中的任意节点（用于定位场景根）
     * @param text     提示文案，空白时忽略
     * @param duration 停留时长（null 用默认）
     * @param animated 是否播放淡入淡出动画
     */
    public static void show(Node anchor, String text, Duration duration, boolean animated) {
        if (anchor == null || text == null || text.isBlank()) {
            return;
        }
        Scene scene = anchor.getScene();
        if (scene == null) {
            return;
        }
        Parent root = scene.getRoot();
        if (!(root instanceof Pane pane)) {
            return;
        }

        Duration hold = duration == null ? DEFAULT_DURATION : duration;
        GameToast toast = new GameToast(text);
        toast.setOpacity(animated ? 0.0 : 1.0);
        toast.setTranslateY(animated ? -14 : 0);

        if (pane instanceof StackPane stack) {
            StackPane.setAlignment(toast, Pos.TOP_CENTER);
            StackPane.setMargin(toast, new Insets(72, 0, 0, 0));
            stack.getChildren().add(toast);
        } else {
            pane.getChildren().add(toast);
            toast.setManaged(false);
            // 根节点非 StackPane 时手动居中到顶部
            pane.applyCss();
            pane.layout();
            double w = toast.prefWidth(-1);
            toast.resize(w, toast.prefHeight(-1));
            toast.setLayoutX(Math.max(0, (pane.getWidth() - toast.getWidth()) / 2));
            toast.setLayoutY(72);
        }

        Runnable remove = () -> {
            if (pane instanceof StackPane stack) {
                stack.getChildren().remove(toast);
            } else {
                pane.getChildren().remove(toast);
            }
        };

        if (!animated) {
            PauseTransition stay = new PauseTransition(hold);
            stay.setOnFinished(e -> remove.run());
            stay.play();
            return;
        }

        FadeTransition in = new FadeTransition(Duration.millis(220), toast);
        in.setFromValue(0.0);
        in.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), toast);
        slide.setFromY(-14);
        slide.setToY(0);

        PauseTransition stay = new PauseTransition(hold);

        FadeTransition out = new FadeTransition(Duration.millis(260), toast);
        out.setFromValue(1.0);
        out.setToValue(0.0);

        SequentialTransition seq = new SequentialTransition(
                new javafx.animation.ParallelTransition(in, slide), stay, out);
        seq.setOnFinished(e -> remove.run());
        seq.play();
    }
}
