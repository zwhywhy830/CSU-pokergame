package com.cards.ui.animation;

import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 花色粒子系统：整层一块 {@link Canvas}，由单个 {@link AnimationTimer} 驱动。
 *
 * <p>粒子沿二次贝塞尔曲线飞行（起点 / 控制点 / 终点），支持四种形态：
 * <ul>
 *   <li>{@link Kind#SUIT} —— ♠ ♥ ♦ ♣ 花色字形（金 / 红 / 米白三色）；</li>
 *   <li>{@link Kind#COIN} —— 圆形方孔铜钱（Canvas 绘制，非图片资源）；</li>
 *   <li>{@link Kind#CROWN} —— ♛ 皇冠字形，胜利返回时残留飘落；</li>
 *   <li>{@link Kind#DOT} —— 金色光点，用于“粒子聚合”的空气感。</li>
 * </ul>
 *
 * <p>性能约束：粒子总数硬上限 {@link #MAX_PARTICLES}（&lt; 120）；
 * 粒子全部消亡后 {@code AnimationTimer} 自动 {@code stop()}，不占用渲染帧；
 * {@link #dispose()} 可强制关闭定时器并释放画布。
 *
 * <p>必须运行在 JavaFX Application Thread。
 */
public final class SuitParticleSystem extends Canvas {

    /** 粒子数量硬上限（性能要求 &lt; 120）。 */
    public static final int MAX_PARTICLES = 110;

    /** 粒子形态。 */
    private enum Kind { SUIT, COIN, CROWN, DOT }

    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};
    private static final String CROWN = "♛";

    private static final Color GOLD = Color.web("#ffd54f");
    private static final Color GOLD_LIGHT = Color.web("#fff3c4");
    private static final Color GOLD_DARK = Color.web("#b07f1e");
    private static final Color SUIT_RED = Color.web("#e2574c");
    private static final Color SUIT_WHITE = Color.web("#fdf6e3");

    /** 铜钱渐变（比例渐变，随绘制形状自适应）。 */
    private static final Paint COIN_FILL = new RadialGradient(0, 0, 0.42, 0.36, 0.62, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#fff6d0")),
            new Stop(0.55, GOLD),
            new Stop(1.0, GOLD_DARK));

    private final List<Particle> particles = new ArrayList<>();
    private final Random rnd = new Random();
    private final AnimationTimer timer;

    private long lastNanos;
    private boolean running;
    private boolean disposed;

    public SuitParticleSystem() {
        setMouseTransparent(true);
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick(now);
            }
        };
    }

    // ============================================================= 发射接口

    /** 花色粒子自中心向四周飞散（进入牌桌）。 */
    public void burst(double cx, double cy, double span, int count, double lifeSec) {
        int n = Math.min(count, room());
        for (int i = 0; i < n; i++) {
            Particle p = base(cx + jitter(12), cy + jitter(12));
            double angle = rnd.nextDouble() * Math.PI * 2;
            double dist = span * (0.26 + rnd.nextDouble() * 0.40);
            p.x1 = cx + Math.cos(angle) * dist;
            p.y1 = cy + Math.sin(angle) * dist;
            // 控制点沿切线偏移 → 曲线飞行而非直线
            double bend = dist * (0.22 + rnd.nextDouble() * 0.36) * (rnd.nextBoolean() ? 1 : -1);
            p.cx = (p.x0 + p.x1) / 2.0 - Math.sin(angle) * bend;
            p.cy = (p.y0 + p.y1) / 2.0 + Math.cos(angle) * bend;
            p.kind = Kind.SUIT;
            p.glyph = SUITS[rnd.nextInt(SUITS.length)];
            p.color = suitColor(p.glyph);
            font(p, 16 + rnd.nextDouble() * 22);
            p.life = lifeSec * (0.78 + rnd.nextDouble() * 0.34);
            p.delay = rnd.nextDouble() * 0.18;
            p.spin = rnd.nextDouble() * 360;
            p.spinSpeed = (rnd.nextDouble() - 0.5) * 260;
            particles.add(p);
        }
        kick();
    }

    /** 卡牌碎片 / 金色粒子向中心聚合（返回大厅）。 */
    public void absorb(double cx, double cy, double span, int count, double lifeSec) {
        int n = Math.min(count, room());
        for (int i = 0; i < n; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double dist = span * (0.34 + rnd.nextDouble() * 0.36);
            Particle p = base(cx + Math.cos(angle) * dist, cy + Math.sin(angle) * dist);
            p.x1 = cx + jitter(18);
            p.y1 = cy + jitter(18);
            // 控制点切向偏移 → 螺旋吸入
            double bend = dist * (0.28 + rnd.nextDouble() * 0.34);
            p.cx = (p.x0 + p.x1) / 2.0 + Math.cos(angle + Math.PI / 2) * bend;
            p.cy = (p.y0 + p.y1) / 2.0 + Math.sin(angle + Math.PI / 2) * bend;
            boolean dot = rnd.nextInt(3) == 0;
            if (dot) {
                p.kind = Kind.DOT;
                p.color = rnd.nextBoolean() ? GOLD : GOLD_LIGHT;
                font(p, 3 + rnd.nextDouble() * 4);
            } else {
                p.kind = Kind.SUIT;
                p.glyph = SUITS[rnd.nextInt(SUITS.length)];
                p.color = suitColor(p.glyph);
                font(p, 14 + rnd.nextDouble() * 16);
            }
            p.life = lifeSec * (0.62 + rnd.nextDouble() * 0.34);
            p.delay = rnd.nextDouble() * 0.34;
            p.spin = rnd.nextDouble() * 360;
            p.spinSpeed = (rnd.nextDouble() - 0.5) * 300;
            p.fadeIn = 0.06;
            p.fadeOut = 0.45;
            particles.add(p);
        }
        kick();
    }

    /** 金币飞散 + 皇冠残留（胜利返回）。 */
    public void celebrate(double w, double h, double cx, double cy, int coins, int crowns, double lifeSec) {
        double span = Math.max(w, h);
        int nc = Math.min(coins, room());
        for (int i = 0; i < nc; i++) {
            double angle = -Math.PI * (0.15 + rnd.nextDouble() * 0.70);   // 朝上半圈抛出
            double dist = span * (0.22 + rnd.nextDouble() * 0.34);
            Particle p = base(cx + jitter(w * 0.18), cy - h * 0.06 + jitter(h * 0.05));
            p.kind = Kind.COIN;
            p.x1 = p.x0 + Math.cos(angle) * dist;
            p.y1 = p.y0 + Math.sin(angle) * dist + h * 0.30;              // 抛物线回落
            p.cx = (p.x0 + p.x1) / 2.0;
            p.cy = Math.min(p.y0, p.y1) - h * (0.16 + rnd.nextDouble() * 0.18);
            font(p, 9 + rnd.nextDouble() * 8);
            p.life = lifeSec * (0.80 + rnd.nextDouble() * 0.36);
            p.delay = rnd.nextDouble() * 0.22;
            p.spin = rnd.nextDouble() * 360;
            p.spinSpeed = (rnd.nextDouble() - 0.5) * 420;
            particles.add(p);
        }
        int nr = Math.min(crowns, room());
        for (int i = 0; i < nr; i++) {
            Particle p = base(rnd.nextDouble() * Math.max(1, w), h * (0.05 + rnd.nextDouble() * 0.16));
            p.kind = Kind.CROWN;
            p.glyph = CROWN;
            p.color = rnd.nextBoolean() ? GOLD : GOLD_LIGHT;
            font(p, 22 + rnd.nextDouble() * 18);
            p.x1 = p.x0 + jitter(w * 0.10);
            p.y1 = p.y0 + h * (0.28 + rnd.nextDouble() * 0.30);           // 缓慢飘落
            p.cx = (p.x0 + p.x1) / 2.0 + jitter(w * 0.08);
            p.cy = (p.y0 + p.y1) / 2.0;
            p.life = lifeSec * (0.88 + rnd.nextDouble() * 0.38);
            p.delay = rnd.nextDouble() * 0.3;
            p.spin = (rnd.nextDouble() - 0.5) * 30;
            p.spinSpeed = (rnd.nextDouble() - 0.5) * 60;
            p.fadeIn = 0.12;
            p.fadeOut = 0.5;
            particles.add(p);
        }
        kick();
    }

    /** 立即清空所有粒子并停止定时器。 */
    public void clear() {
        particles.clear();
        running = false;
        timer.stop();
        lastNanos = 0;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
    }

    /** 释放：关闭定时器、清空画布（转场结束时调用）。 */
    public void dispose() {
        disposed = true;
        clear();
    }

    /** 当前存活粒子数（调试 / 测试用）。 */
    public int aliveCount() {
        return particles.size();
    }

    /** 定时器是否在跑（调试 / 测试用）。 */
    public boolean isRunning() {
        return running;
    }

    // ============================================================= 内部实现

    /** 剩余可用粒子配额。 */
    private int room() {
        return Math.max(0, MAX_PARTICLES - particles.size());
    }

    private void kick() {
        if (disposed || running || particles.isEmpty()) {
            return;
        }
        running = true;
        lastNanos = 0;
        timer.start();
    }

    private Particle base(double x, double y) {
        Particle p = new Particle();
        p.x0 = x;
        p.y0 = y;
        return p;
    }

    private double jitter(double amount) {
        return (rnd.nextDouble() - 0.5) * 2 * amount;
    }

    /** 设定字号：字体在发射时创建一次并缓存到粒子上，避免逐帧 new Font。 */
    private static void font(Particle p, double size) {
        p.size = size;
        p.font = Font.font("Segoe UI Symbol", size);
    }

    private static Color suitColor(String glyph) {
        return switch (glyph) {
            case "♥", "♦" -> SUIT_RED;
            case "♣" -> SUIT_WHITE;
            default -> GOLD;
        };
    }

    private void tick(long now) {
        if (disposed) {
            return;
        }
        if (lastNanos == 0) {
            lastNanos = now;
            return;
        }
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (dt > 0.10) {
            dt = 0.10;   // 掉帧保护：单帧最多推进 100ms，避免粒子瞬移
        }

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            if (p.delay > 0) {
                p.delay -= dt;
                continue;
            }
            p.t += dt / p.life;
            if (p.t >= 1.0) {
                it.remove();
                continue;
            }
            draw(gc, p);
        }

        if (particles.isEmpty()) {
            running = false;
            timer.stop();
            lastNanos = 0;
        }
    }

    private void draw(GraphicsContext gc, Particle p) {
        double t = p.t;
        double u = 1.0 - t;
        // 二次贝塞尔：B(t) = (1-t)²P0 + 2(1-t)t·C + t²P1
        double x = u * u * p.x0 + 2 * u * t * p.cx + t * t * p.x1;
        double y = u * u * p.y0 + 2 * u * t * p.cy + t * t * p.y1;

        double alpha = Math.min(1.0, t / p.fadeIn) * Math.min(1.0, (1.0 - t) / p.fadeOut);
        if (alpha <= 0.012) {
            return;
        }

        gc.save();
        gc.setGlobalAlpha(Math.min(1.0, alpha));
        gc.translate(x, y);
        gc.rotate(p.spin + p.spinSpeed * t);
        switch (p.kind) {
            case COIN -> drawCoin(gc, p.size);
            case DOT -> drawDot(gc, p.size, p.color);
            default -> drawGlyph(gc, p);
        }
        gc.restore();
    }

    private void drawGlyph(GraphicsContext gc, Particle p) {
        gc.setFont(p.font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        // 先描一圈柔光，再压实心，暗背景上更立体
        gc.setFill(Color.web("#000000", 0.28));
        gc.fillText(p.glyph, 0, 1.5);
        gc.setFill(p.color);
        gc.fillText(p.glyph, 0, 0);
    }

    private void drawDot(GraphicsContext gc, double r, Color color) {
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.30));
        gc.fillOval(-r * 2.1, -r * 2.1, r * 4.2, r * 4.2);
        gc.setFill(color);
        gc.fillOval(-r, -r, r * 2, r * 2);
    }

    /** 圆形方孔铜钱：外圆金渐变 + 内方孔，纯 Canvas 绘制。 */
    private void drawCoin(GraphicsContext gc, double r) {
        gc.setFill(COIN_FILL);
        gc.fillOval(-r, -r, r * 2, r * 2);
        gc.setStroke(Color.web("#8a6316", 0.85));
        gc.setLineWidth(Math.max(1.0, r * 0.13));
        gc.strokeOval(-r, -r, r * 2, r * 2);
        double s = r * 0.46;
        gc.setFill(Color.web("#5a3f0c", 0.92));
        gc.fillRect(-s / 2, -s / 2, s, s);
        gc.setStroke(Color.web("#ffe9b0", 0.72));
        gc.setLineWidth(Math.max(0.7, r * 0.07));
        gc.strokeRect(-s / 2, -s / 2, s, s);
    }

    /** 单个粒子：贝塞尔轨迹 + 生命周期 + 外观。 */
    private static final class Particle {
        double x0, y0;      // 起点
        double cx, cy;      // 控制点
        double x1, y1;      // 终点
        double t;           // 0 → 1 进度
        double life = 1.0;  // 秒
        double delay;       // 秒
        double size = 18;
        double spin;
        double spinSpeed;
        double fadeIn = 0.18;
        double fadeOut = 0.38;
        Kind kind = Kind.SUIT;
        String glyph = "♠";
        Color color = GOLD;
        Font font = Font.font("Segoe UI Symbol", 18);
    }
}
