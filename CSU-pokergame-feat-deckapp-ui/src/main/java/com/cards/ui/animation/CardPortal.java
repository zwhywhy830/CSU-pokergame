package com.cards.ui.animation;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * 中央巨大牌背（Card Portal）：进入牌桌转场的视觉主体。
 *
 * <p>整张牌完全由 {@link Canvas} 绘制，不依赖任何图片资源：
 * 深绿绒面渐变牌身 + 双层金色描边 + 四角回纹菱饰 + 中央径向辉光 + 金环 + 黑桃。
 *
 * <p>动画三段式（由 {@link #playIn} / {@link #playBurst} 提供）：
 * <ol>
 *   <li>出现：{@code scale 0.1 → 1.2}，{@code opacity 0 → 1}，{@code rotate 0 → 180}；</li>
 *   <li>回弹：{@code scale 1.2 → 1}（轻微过冲，手游手感）；</li>
 *   <li>炸开：{@code scale 1 → 1.7}，{@code opacity 1 → 0}，{@code rotate 180 → 360}，
 *       新场景从牌面背后展开。</li>
 * </ol>
 *
 * <p>必须运行在 JavaFX Application Thread。
 */
public final class CardPortal extends Canvas {

    /** 牌背逻辑尺寸（动画通过 scaleX / scaleY 放大，画布本身不重绘）。 */
    public static final double CARD_W = 196.0;
    public static final double CARD_H = 274.0;

    private static final Color GOLD_LIGHT = Color.web("#fff3c4");
    private static final Color GOLD = Color.web("#e8c25e");
    private static final Color GOLD_DEEP = Color.web("#dcb860");
    private static final Color FELT_LIGHT = Color.web("#1d5c43");
    private static final Color FELT_MID = Color.web("#123f2e");
    private static final Color FELT_DARK = Color.web("#08251a");

    public CardPortal() {
        this(CARD_W, CARD_H);
    }

    public CardPortal(double width, double height) {
        super(width, height);
        setMouseTransparent(true);
        paint();
        reset();
    }

    /** 复位到“未出现”状态。 */
    public void reset() {
        setOpacity(0.0);
        setScaleX(0.1);
        setScaleY(0.1);
        setRotate(0.0);
    }

    /**
     * 出现：放大 0.1 → 1.2 → 1，同时旋转 0 → 180、透明度 0 → 1。
     *
     * @param delay    延迟
     * @param duration 总时长
     */
    public Timeline playIn(Duration delay, Duration duration) {
        reset();
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(opacityProperty(), 0.0),
                        new KeyValue(scaleXProperty(), 0.1),
                        new KeyValue(scaleYProperty(), 0.1),
                        new KeyValue(rotateProperty(), 0.0)),
                new KeyFrame(duration.multiply(0.72),
                        new KeyValue(opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(scaleXProperty(), 1.2, Interpolator.EASE_OUT),
                        new KeyValue(scaleYProperty(), 1.2, Interpolator.EASE_OUT),
                        new KeyValue(rotateProperty(), 150.0, Interpolator.EASE_OUT)),
                new KeyFrame(duration,
                        new KeyValue(scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(rotateProperty(), 180.0, Interpolator.EASE_OUT)));
        tl.setDelay(delay);
        return tl;
    }

    /**
     * 炸开：继续放大并淡出，旋转补满一圈（180 → 360）。
     *
     * @param delay    延迟（通常紧接 {@link #playIn} 结束）
     * @param duration 总时长
     */
    public Timeline playBurst(Duration delay, Duration duration) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(opacityProperty(), getOpacity()),
                        new KeyValue(scaleXProperty(), 1.0),
                        new KeyValue(scaleYProperty(), 1.0),
                        new KeyValue(rotateProperty(), 180.0)),
                new KeyFrame(duration,
                        new KeyValue(opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(scaleXProperty(), 1.7, Interpolator.EASE_IN),
                        new KeyValue(scaleYProperty(), 1.7, Interpolator.EASE_IN),
                        new KeyValue(rotateProperty(), 360.0, Interpolator.EASE_IN)));
        tl.setDelay(delay);
        return tl;
    }

    // ============================================================= 绘制

    /** 一次性绘制牌背（动画只改 transform，不重绘画布）。 */
    private void paint() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        double arc = 20;
        double inset = 3;

        gc.clearRect(0, 0, w, h);

        // ---- 牌身：深绿绒面渐变 ----
        gc.setFill(new LinearGradient(0, 0, w * 0.35, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, FELT_LIGHT),
                new Stop(0.55, FELT_MID),
                new Stop(1.0, FELT_DARK)));
        gc.fillRoundRect(inset, inset, w - inset * 2, h - inset * 2, arc, arc);

        // ---- 斜向暗纹（手工织纹感）----
        gc.save();
        gc.beginPath();
        gc.rect(inset, inset, w - inset * 2, h - inset * 2);
        gc.clip();
        gc.setStroke(Color.web("#ffffff", 0.045));
        gc.setLineWidth(1.0);
        for (double x = -h; x < w + h; x += 12) {
            gc.strokeLine(x, inset, x + h, h - inset);
        }
        gc.restore();

        // ---- 双层金边 ----
        gc.setStroke(GOLD_DEEP);
        gc.setLineWidth(2.6);
        gc.strokeRoundRect(inset, inset, w - inset * 2, h - inset * 2, arc, arc);
        gc.setStroke(Color.web("#dcb860", 0.42));
        gc.setLineWidth(1.1);
        gc.strokeRoundRect(inset + 9, inset + 9, w - (inset + 9) * 2, h - (inset + 9) * 2, arc - 7, arc - 7);

        // ---- 四角回纹菱饰 ----
        double d = 7.0;
        double[][] corners = {{inset + 22, inset + 22}, {w - inset - 22, inset + 22},
                {inset + 22, h - inset - 22}, {w - inset - 22, h - inset - 22}};
        gc.setFill(Color.web("#dcb860", 0.85));
        for (double[] c : corners) {
            gc.save();
            gc.translate(c[0], c[1]);
            gc.rotate(45);
            gc.fillRect(-d / 2, -d / 2, d, d);
            gc.restore();
        }

        // ---- 中央径向辉光 ----
        double cx = w / 2.0;
        double cy = h / 2.0;
        double glowR = Math.min(w, h) * 0.52;
        gc.setFill(new RadialGradient(0, 0, cx, cy, glowR, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ffd54f", 0.34)),
                new Stop(0.55, Color.web("#dcb860", 0.12)),
                new Stop(1.0, Color.web("#ffd54f", 0.0))));
        gc.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

        // ---- 中央金环 ----
        double ringR = Math.min(w, h) * 0.29;
        gc.setStroke(Color.web("#ffe9b0", 0.55));
        gc.setLineWidth(1.6);
        gc.strokeOval(cx - ringR, cy - ringR, ringR * 2, ringR * 2);
        gc.setStroke(Color.web("#dcb860", 0.30));
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - ringR * 1.18, cy - ringR * 1.18, ringR * 2.36, ringR * 2.36);

        // ---- 中央黑桃（金色渐变）----
        gc.setFont(Font.font("Segoe UI Symbol", Math.min(w, h) * 0.40));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(Color.web("#000000", 0.35));
        gc.fillText("♠", cx, cy + 2.5);
        gc.setFill(new LinearGradient(0, cy - ringR, 0, cy + ringR, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, GOLD_LIGHT),
                new Stop(0.55, GOLD),
                new Stop(1.0, GOLD_DEEP)));
        gc.fillText("♠", cx, cy);
    }
}
