package com.cards.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 低强度背景粒子层：金色星光缓慢浮动、呼吸闪烁；少量纸牌碎片轻轻飘落、左右摇摆并缓慢旋转。
 * 密度刻意压低、透明度弱，只做氛围点缀，不干扰文字。鼠标事件完全穿透。
 */
public final class ParticleField extends Pane {

    private final Canvas canvas = new Canvas();
    private final List<P> particles = new ArrayList<>();
    private final Random rnd = new Random(20260909L);
    private AnimationTimer timer;
    private long lastNanos;

    public ParticleField() {
        setMouseTransparent(true);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener(o -> seed());
        heightProperty().addListener(o -> seed());
        seed();
    }

    /** 启动粒子动画。 */
    public void play() {
        if (timer == null) {
            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double dt = lastNanos == 0 ? 0.016 : (now - lastNanos) / 1e9;
                    lastNanos = now;
                    tick(Math.min(dt, 0.05));
                }
            };
        }
        lastNanos = 0L;
        timer.start();
    }

    /** 暂停粒子动画。 */
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    /** 按当前尺寸撒粒子（星光 26 颗 + 碎片 9 片，低密度）。 */
    private void seed() {
        particles.clear();
        double w = getWidth();
        double h = getHeight();
        if (w < 10 || h < 10) {
            return;
        }
        for (int i = 0; i < 26; i++) {
            P p = new P();
            p.kind = 0;
            p.x = rnd.nextDouble() * w;
            p.y = rnd.nextDouble() * h;
            p.size = 0.8 + rnd.nextDouble() * 1.8;
            p.vx = (rnd.nextDouble() - 0.5) * 6.0;
            p.vy = -(3.0 + rnd.nextDouble() * 6.0);
            p.alpha = 0.12 + rnd.nextDouble() * 0.38;
            p.phase = rnd.nextDouble() * Math.PI * 2;
            p.twinkle = 0.6 + rnd.nextDouble() * 1.2;
            particles.add(p);
        }
        for (int i = 0; i < 9; i++) {
            P p = new P();
            p.kind = 1;
            p.x = rnd.nextDouble() * w;
            p.y = rnd.nextDouble() * h;
            p.w = 5 + rnd.nextDouble() * 6;
            p.h = 7 + rnd.nextDouble() * 8;
            p.vy = 10 + rnd.nextDouble() * 14;
            p.sway = 14 + rnd.nextDouble() * 18;
            p.swaySpeed = 0.5 + rnd.nextDouble() * 0.7;
            p.rot = rnd.nextDouble() * 360;
            p.rotSpeed = (rnd.nextDouble() - 0.5) * 22;
            p.alpha = 0.10 + rnd.nextDouble() * 0.14;
            p.phase = rnd.nextDouble() * Math.PI * 2;
            particles.add(p);
        }
    }

    private void tick(double dt) {
        double w = getWidth();
        double h = getHeight();
        if (w < 10 || h < 10) {
            return;
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        double t = System.nanoTime() / 1e9;

        for (P p : particles) {
            if (p.kind == 0) {
                // 星光：缓慢漂移 + 呼吸闪烁
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                wrapSpark(p, w, h);
                double tw = 0.55 + 0.45 * Math.sin(t * p.twinkle + p.phase);
                double a = clamp(p.alpha * tw);
                g.setFill(Color.rgb(255, 226, 140, a));
                g.beginPath();
                g.arc(p.x, p.y, p.size, p.size, 0, 360);
                g.fill();
                g.setFill(Color.rgb(255, 220, 120, clamp(a * 0.18)));
                g.beginPath();
                g.arc(p.x, p.y, p.size * 3.0, p.size * 3.0, 0, 360);
                g.fill();
            } else {
                // 纸牌碎片：以 p.x 为中轴左右摇摆、缓慢飘落旋转
                p.phase += dt * p.swaySpeed;
                p.y += p.vy * dt;
                double x = p.x + Math.sin(p.phase) * p.sway;
                if (p.y - p.h > h) {
                    p.y = -p.h;
                    p.x = rnd.nextDouble() * w;
                }
                if (p.x < -40) {
                    p.x = w + 40;
                } else if (p.x > w + 40) {
                    p.x = -40;
                }
                p.rot += p.rotSpeed * dt;
                g.save();
                g.translate(x, p.y);
                g.rotate(p.rot);
                g.setFill(Color.rgb(245, 238, 214, clamp(p.alpha)));
                g.fillRoundRect(-p.w / 2, -p.h / 2, p.w, p.h, 3, 3);
                g.setFill(Color.rgb(200, 70, 70, clamp(p.alpha * 0.9)));
                g.fillRect(-1.0, -p.h * 0.28, 2.0, p.h * 0.56);
                g.restore();
            }
        }
    }

    private void wrapSpark(P p, double w, double h) {
        if (p.y < -6) {
            p.y = h + 6;
            p.x = rnd.nextDouble() * w;
        } else if (p.y > h + 6) {
            p.y = -6;
            p.x = rnd.nextDouble() * w;
        }
        if (p.x < -6) {
            p.x = w + 6;
        } else if (p.x > w + 6) {
            p.x = -6;
        }
    }

    private static double clamp(double v) {
        return v < 0 ? 0.0 : Math.min(v, 1.0);
    }

    /** 一个粒子（星光 kind=0 / 纸牌碎片 kind=1）。 */
    private static final class P {
        int kind;
        double x;
        double y;
        double size;
        double vx;
        double vy;
        double alpha;
        double phase;
        double twinkle;
        double w;
        double h;
        double sway;
        double swaySpeed;
        double rot;
        double rotSpeed;
    }
}
