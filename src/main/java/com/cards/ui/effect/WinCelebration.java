package com.cards.ui.effect;

import com.cards.ui.theme.DesignTokens;
import com.cards.ui.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 胜利/失败全屏庆祝效果。
 *
 * <p>胜利：金色星光粒子（AnimationTimer 驱动下落）+ 皇冠放大弹跳 + 胜利文字淡入 + 呼吸循环。
 * 失败：灰化遮罩 + ✕ 图标 + 失败文字。
 *
 * <p>动画时长使用 {@link DesignTokens#ANIM_GLOW}（900ms）作为主入场，
 * 粒子持续到 {@link #stop()} 被调用。
 */
public final class WinCelebration extends StackPane {

    private final List<Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private Timeline entryTimeline;
    private Timeline breathPulse;

    private static final int PARTICLE_COUNT = 48;

    private static final class Particle {
        double x, y, vx, vy, size, life;
        boolean gold;
        Circle node;
    }

    /** 构造胜利/失败庆祝层。win=true 胜利，win=false 失败。 */
    public WinCelebration(boolean win, String subtitle) {
        this(win, subtitle, null);
    }

    /**
     * 构造胜利/失败庆祝层，并在副标题下方插入附加内容（如结算成长反馈面板）。
     *
     * @param extra 附加节点，null 表示不插入；非空时随副标题之后淡入
     */
    public WinCelebration(boolean win, String subtitle, Node extra) {
        getStyleClass().add("win-overlay");
        setOpacity(0.0);
        setMouseTransparent(false);

        // 遮罩
        Region shade = new Region();
        shade.setStyle(win
                ? "-fx-background-color: rgba(8,22,14,0.55);"
                : "-fx-background-color: rgba(20,20,24,0.65);");
        shade.setMouseTransparent(true);

        // 内容卡
        VBox card = new VBox(14);
        card.getStyleClass().addAll("win-card", win ? "win-victory" : "win-defeat");
        card.setAlignment(Pos.CENTER);
        card.setOpacity(0.0);
        card.setScaleX(0.85);
        card.setScaleY(0.85);
        card.setTranslateY(24);

        // 皇冠 / ✕
        Text glyph = new Text(win ? "♛" : "✕");
        glyph.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 64));
        glyph.setFill(win ? Theme.GOLD_BRIGHT : Color.web("#9aa6b2"));
        glyph.setOpacity(0.0);
        glyph.setScaleX(0.4);
        glyph.setScaleY(0.4);
        glyph.setMouseTransparent(true);

        // 胜利/失败标题
        Label title = new Label(win ? "胜利" : "失败");
        title.setFont(Font.font(win ? "Segoe UI Symbol" : "System", FontWeight.BOLD, 40));
        title.setTextFill(win ? Theme.GOLD_TEXT : Color.web("#c7ced6"));
        title.setOpacity(0.0);

        // 副标题
        Label sub = new Label(subtitle == null ? "" : subtitle);
        sub.setFont(Font.font(16));
        sub.setTextFill(Color.web("rgba(235,240,235,0.85)"));
        sub.setWrapText(true);
        sub.setMaxWidth(420);
        sub.setOpacity(0.0);

        // 粒子层（胜利时才生成）
        Pane particleLayer = new Pane();
        particleLayer.setMouseTransparent(true);
        if (win) {
            Random rng = new Random();
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                Particle p = new Particle();
                p.x = rng.nextDouble() * 900;
                p.y = -rng.nextDouble() * 300;
                p.vx = (rng.nextDouble() - 0.5) * 1.2;
                p.vy = 1.0 + rng.nextDouble() * 2.5;
                p.size = 2 + rng.nextDouble() * 5;
                p.life = 1.0;
                p.gold = rng.nextBoolean();
                p.node = new Circle(p.size);
                p.node.setFill(p.gold ? Theme.GOLD_BRIGHT : Color.web("#fff3c4"));
                p.node.setOpacity(0.85);
                p.node.setLayoutX(p.x);
                p.node.setLayoutY(p.y);
                particleLayer.getChildren().add(p.node);
                particles.add(p);
            }
        }

        card.getChildren().addAll(glyph, title, sub);
        if (extra != null) {
            // 附加内容（成长反馈）从透明起步，在副标题之后淡入
            extra.setOpacity(0.0);
            card.getChildren().add(extra);
        }
        getChildren().addAll(particleLayer, shade, card);
        StackPane.setAlignment(card, Pos.CENTER);

        // ---- 入场动画 ----
        entryTimeline = new Timeline(
                // 遮罩淡入
                new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(300), new KeyValue(opacityProperty(), 1.0)),
                // 卡片弹入
                new KeyFrame(Duration.millis(DesignTokens.ANIM_GLOW.toMillis()),
                        new KeyValue(card.opacityProperty(), 1.0),
                        new KeyValue(card.scaleXProperty(), 1.0),
                        new KeyValue(card.scaleYProperty(), 1.0),
                        new KeyValue(card.translateYProperty(), 0)),
                // 皇冠弹跳
                new KeyFrame(Duration.millis(300), new KeyValue(glyph.opacityProperty(), 0.0),
                        new KeyValue(glyph.scaleXProperty(), 0.4),
                        new KeyValue(glyph.scaleYProperty(), 0.4)),
                new KeyFrame(Duration.millis(500), new KeyValue(glyph.opacityProperty(), 1.0),
                        new KeyValue(glyph.scaleXProperty(), 1.15),
                        new KeyValue(glyph.scaleYProperty(), 1.15)),
                new KeyFrame(Duration.millis(700), new KeyValue(glyph.scaleXProperty(), 1.0),
                        new KeyValue(glyph.scaleYProperty(), 1.0)),
                // 标题淡入
                new KeyFrame(Duration.millis(500), new KeyValue(title.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(900), new KeyValue(title.opacityProperty(), 1.0)),
                // 副标题淡入
                new KeyFrame(Duration.millis(700), new KeyValue(sub.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(1100), new KeyValue(sub.opacityProperty(), 1.0))
        );
        entryTimeline.setOnFinished(e -> {
            if (win) {
                startParticles();
                startBreathPulse(title);
            }
        });
        if (extra != null) {
            // 附加内容在副标题淡入之后接续淡入
            entryTimeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(900), new KeyValue(extra.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(1350), new KeyValue(extra.opacityProperty(), 1.0)));
        }
        entryTimeline.play();
    }

    private void startParticles() {
        particleTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double w = getWidth();
                double h = getHeight();
                for (Particle p : particles) {
                    p.x += p.vx;
                    p.y += p.vy;
                    p.vy += 0.02; // 重力
                    p.life -= 0.003;
                    if (p.y > h + 20 || p.life <= 0) {
                        // 重置到顶部
                        p.x = Math.random() * (w > 0 ? w : 900);
                        p.y = -20;
                        p.vy = 1.0 + Math.random() * 2.5;
                        p.vx = (Math.random() - 0.5) * 1.2;
                        p.life = 1.0;
                    }
                    p.node.setLayoutX(p.x);
                    p.node.setLayoutY(p.y);
                    p.node.setOpacity(Math.max(0, p.life * 0.85));
                }
            }
        };
        particleTimer.start();
    }

    private void startBreathPulse(Label title) {
        breathPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(title.scaleXProperty(), 1.0),
                        new KeyValue(title.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(1100),
                        new KeyValue(title.scaleXProperty(), 1.045),
                        new KeyValue(title.scaleYProperty(), 1.045)));
        breathPulse.setAutoReverse(true);
        breathPulse.setCycleCount(Timeline.INDEFINITE);
        breathPulse.play();
    }

    /** 停止所有动画并清理。从父节点移除后调用。 */
    public void stop() {
        if (particleTimer != null) { particleTimer.stop(); particleTimer = null; }
        if (breathPulse != null) { breathPulse.stop(); breathPulse = null; }
        if (entryTimeline != null) { entryTimeline.stop(); entryTimeline = null; }
        particles.clear();
    }
}
