package com.cards.ui.effect;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 阶段 22 轻量粒子效果（纯 JavaFX，无第三方依赖）。
 *
 * <p>三类预设效果，通过静态工厂创建：
 * <ul>
 *   <li>{@link #coinBurst()}：金币粒子 —— 从原点向外抛出、受重力回落、逐渐淡出；</li>
 *   <li>{@link #winStars()}：胜利星光 —— 顶部金色星光缓缓飘落、呼吸闪烁；</li>
 *   <li>{@link #levelHalo()}：升级光环 —— 原点向外扩散的金色光环，循环数波后淡出。</li>
 * </ul>
 *
 * <p>本层是一个透明的 {@link Pane}（内部画布随尺寸自适应），鼠标事件完全穿透，
 * 既可铺满整屏（{@link #winStars()}），也可用 {@link #setOrigin(double, double)}
 * 定位到某个节点的场景坐标（金币 / 光环）。
 *
 * <p>粒子为一次性短动效：{@link #play()} 后经过 {@link #setLifetime(double)} 指定的时长
 * 自动 {@link #stop()}，并回调 {@link #setOnFinished(Runnable)}。
 * 调用方把它加入场景后无需手动回收（宿主可在回调里移除）。
 */
public final class ParticleEffect extends Pane {

    /** 预设效果类型。 */
    public enum Kind {
        /** 金币粒子。 */
        COIN,
        /** 胜利星光。 */
        WIN_STAR,
        /** 升级光环。 */
        LEVEL_HALO
    }

    private final Kind kind;
    private final Canvas canvas = new Canvas();
    private final List<P> particles = new ArrayList<>();
    private final Random rnd = new Random();

    private AnimationTimer timer;
    private long lastNanos;
    private double elapsed;
    private double lifetime = 1.4;
    private double originX = Double.NaN;
    private double originY = Double.NaN;
    private Runnable onFinished;
    private boolean running;

    private ParticleEffect(Kind kind) {
        this.kind = kind;
        getStyleClass().add("game-particle");
        setMouseTransparent(true);
        setPickOnBounds(false);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        canvas.setMouseTransparent(true);
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener(o -> seed());
        heightProperty().addListener(o -> seed());
    }

    /** 金币粒子：从原点抛出的金币。 */
    public static ParticleEffect coinBurst() {
        return new ParticleEffect(Kind.COIN);
    }

    /** 胜利星光：铺满整屏的金色星光。 */
    public static ParticleEffect winStars() {
        return new ParticleEffect(Kind.WIN_STAR);
    }

    /** 升级光环：从原点扩散的金色光环。 */
    public static ParticleEffect levelHalo() {
        return new ParticleEffect(Kind.LEVEL_HALO);
    }

    /** 效果类型。 */
    public Kind getKind() {
        return kind;
    }

    /**
     * 设置发射原点（本层坐标系，通常是节点在本层内的坐标）。
     * 不设置时对 COIN / LEVEL_HALO 取层中心。
     */
    public void setOrigin(double x, double y) {
        this.originX = x;
        this.originY = y;
    }

    /** 设置动效存活时长（秒），到点自动停止。 */
    public void setLifetime(double seconds) {
        this.lifetime = Math.max(0.2, seconds);
    }

    /** 结束回调（自动停止时触发一次，可 null）。 */
    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    /** 立即播放（重复调用会重新开始计时）。 */
    public void play() {
        seed();
        elapsed = 0;
        if (timer == null) {
            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double dt = lastNanos == 0 ? 0.016 : (now - lastNanos) / 1e9;
                    lastNanos = now;
                    if (dt > 0.05) {
                        dt = 0.05;
                    }
                    elapsed += dt;
                    tick(dt);
                    if (elapsed >= lifetime) {
                        stop();
                        if (onFinished != null) {
                            onFinished.run();
                        }
                    }
                }
            };
        }
        lastNanos = 0;
        running = true;
        timer.start();
    }

    /** 停止并清屏。 */
    public void stop() {
        running = false;
        if (timer != null) {
            timer.stop();
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /** 是否正在播放。 */
    public boolean isRunning() {
        return running;
    }

    // ============================================================= 内部

    private double originX() {
        if (!Double.isNaN(originX)) {
            return originX;
        }
        double w = getWidth();
        return w > 0 ? w / 2 : 450;
    }

    private double originY() {
        if (!Double.isNaN(originY)) {
            return originY;
        }
        double h = getHeight();
        return h > 0 ? h / 2 : 300;
    }

    private void seed() {
        particles.clear();
        double w = getWidth();
        double h = getHeight();
        if (w < 10 || h < 10) {
            // 尺寸未确定时用兜底尺寸播种，等待 resize 再重播
            w = 900;
            h = 600;
        }
        switch (kind) {
            case COIN -> seedCoins(w, h);
            case WIN_STAR -> seedStars(w, h);
            case LEVEL_HALO -> seedHalo(w, h);
        }
    }

    private void seedCoins(double w, double h) {
        double cx = originX();
        double cy = originY();
        for (int i = 0; i < 16; i++) {
            P p = new P();
            double angle = (Math.PI * 2 * i / 16) + rnd.nextDouble() * 0.3;
            double speed = 90 + rnd.nextDouble() * 160;
            p.x = cx;
            p.y = cy;
            p.vx = Math.cos(angle) * speed;
            p.vy = Math.sin(angle) * speed - 120;
            p.size = 5 + rnd.nextDouble() * 5;
            p.alpha = 1.0;
            particles.add(p);
        }
    }

    private void seedStars(double w, double h) {
        for (int i = 0; i < 44; i++) {
            P p = new P();
            p.x = rnd.nextDouble() * w;
            p.y = -rnd.nextDouble() * h;
            p.vy = 26 + rnd.nextDouble() * 60;
            p.vx = (rnd.nextDouble() - 0.5) * 16;
            p.size = 2 + rnd.nextDouble() * 4.5;
            p.alpha = 0.35 + rnd.nextDouble() * 0.55;
            p.phase = rnd.nextDouble() * Math.PI * 2;
            p.twinkle = 0.8 + rnd.nextDouble() * 1.6;
            particles.add(p);
        }
    }

    private void seedHalo(double w, double h) {
        for (int i = 0; i < 4; i++) {
            P p = new P();
            p.alpha = 0.85;
            p.size = i * 0.26; // 发射延迟
            p.radius = 6;
            p.maxRadius = 70 + rnd.nextDouble() * 26;
            particles.add(p);
        }
    }

    private void tick(double dt) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        switch (kind) {
            case COIN -> tickCoins(g, dt, h);
            case WIN_STAR -> tickStars(g, dt, w, h);
            case LEVEL_HALO -> tickHalo(g, dt);
        }
    }

    private void tickCoins(GraphicsContext g, double dt, double h) {
        for (P p : particles) {
            p.vy += 420 * dt; // 重力
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.alpha -= dt * 0.85;
            if (p.alpha <= 0 || p.y > h + 40) {
                continue;
            }
            double a = clamp(p.alpha);
            double r = p.size;
            g.save();
            g.translate(p.x, p.y);
            // 金币：外圈金环 + 内芯亮金
            g.setFill(Color.rgb(232, 194, 94, a));
            g.beginPath();
            g.arc(0, 0, r, r, 0, 360);
            g.fill();
            g.setFill(Color.rgb(255, 240, 170, a));
            g.beginPath();
            g.arc(0, 0, r * 0.6, r * 0.6, 0, 360);
            g.fill();
            g.restore();
        }
    }

    private void tickStars(GraphicsContext g, double dt, double w, double h) {
        double t = elapsed;
        for (P p : particles) {
            p.y += p.vy * dt;
            p.x += p.vx * dt;
            if (p.y > h + 10) {
                p.y = -10;
                p.x = rnd.nextDouble() * w;
            }
            if (p.x < -10) {
                p.x = w + 10;
            } else if (p.x > w + 10) {
                p.x = -10;
            }
            double tw = 0.55 + 0.45 * Math.sin(t * p.twinkle + (p.phase == 0 ? 0 : p.phase));
            double a = clamp(p.alpha * tw);
            drawStar(g, p.x, p.y, p.size, a);
        }
    }

    private void drawStar(GraphicsContext g, double cx, double cy, double r, double a) {
        // 用两条交叉的线段拼出四角星光
        g.setStroke(Color.rgb(255, 226, 140, a));
        g.setLineWidth(Math.max(1, r * 0.35));
        g.strokeLine(cx - r, cy, cx + r, cy);
        g.strokeLine(cx, cy - r, cx, cy + r);
        g.setFill(Color.rgb(255, 245, 200, clamp(a * 0.9)));
        g.beginPath();
        g.arc(cx, cy, r * 0.42, r * 0.42, 0, 360);
        g.fill();
    }

    private void tickHalo(GraphicsContext g, double dt) {
        double cx = originX();
        double cy = originY();
        for (P p : particles) {
            if (p.size > 0) {
                p.size -= dt; // 发射延迟倒计时
                continue;
            }
            p.radius += (p.maxRadius - p.radius) * Math.min(1, dt * 2.6 + 0.02);
            p.alpha -= dt * 0.9;
            if (p.alpha <= 0) {
                p.alpha = 0.85;
                p.radius = 6;
            }
            double a = clamp(p.alpha);
            g.setStroke(Color.rgb(255, 213, 120, a));
            g.setLineWidth(3);
            g.strokeOval(cx - p.radius, cy - p.radius, p.radius * 2, p.radius * 2);
            g.setStroke(Color.rgb(255, 243, 196, clamp(a * 0.5)));
            g.setLineWidth(1.5);
            g.strokeOval(cx - p.radius * 0.82, cy - p.radius * 0.82, p.radius * 1.64, p.radius * 1.64);
        }
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    /** 单个粒子。 */
    private static final class P {
        double x;
        double y;
        double vx;
        double vy;
        double size;
        double alpha;
        double phase;
        double twinkle;
        double radius;
        double maxRadius;
    }
}
