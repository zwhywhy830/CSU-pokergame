package com.cards.ui.effect;

import com.cards.ui.animation.CardFlyAnimation;
import com.cards.ui.component.GameToast;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
import com.csu.pokergame.settings.SettingsService;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/**
 * 阶段 22 游戏表现增强：统一动画服务。
 *
 * <p>表现层（{@code com.cards.ui.*}）播放任何游戏动画时只调用本服务，不直接 new 动画对象：
 * <pre>{@code
 * GameAnimationService anim = GameAnimationService.getInstance();
 * anim.playCardAnimation(cardNode, fromX, fromY, () -> { ... });
 * anim.playWinAnimation(root, celebration);
 * anim.playLoseAnimation(celebration);
 * anim.playCoinAnimation(panel, coinBar, null);
 * anim.playLevelUpAnimation(avatar);
 * anim.playButtonFeedback(button);
 * anim.showToast(anchor, "＋100 金币");
 * }</pre>
 *
 * <p><b>开关联动：</b>所有动画都受 {@code GameSettings.animationEnabled} 控制；
 * 关闭后各 {@code playXxx} 直接返回 {@code false} 且不做任何视觉效果（只保留必要的回调，
 * 保证业务逻辑不被动画阻塞）。音效不受本开关影响，只受 {@code soundEnabled} 控制。
 *
 * <p><b>容错：</b>节点为 null、节点未入场景、非 JavaFX 线程等情况一律安全降级；任何动画异常都不抛出。
 */
public final class GameAnimationService {

    /** 动画事件类型（同时用于统计与自检）。 */
    public enum Kind {
        /** 出牌动画。 */
        CARD,
        /** 胜利动画。 */
        WIN,
        /** 失败动画。 */
        LOSE,
        /** 金币飞入动画。 */
        COIN,
        /** 升级动画。 */
        LEVEL_UP,
        /** 按钮反馈动画。 */
        BUTTON,
        /** 提示条。 */
        TOAST
    }

    /** 按钮反馈时长：0.95 → 1.05 → 1。 */
    public static final Duration BUTTON_FEEDBACK = Duration.millis(150);

    private static volatile GameAnimationService instance;

    private final Map<Kind, Integer> playCounts = new EnumMap<>(Kind.class);
    private Kind lastPlayed;
    /** 测试用开关覆盖：null 表示跟随设置。 */
    private Boolean enabledOverride;

    private static final String BUTTON_MARKER = "gameAnimationButtonBound";

    private GameAnimationService() {
    }

    /** 单例入口。 */
    public static GameAnimationService getInstance() {
        GameAnimationService local = instance;
        if (local == null) {
            synchronized (GameAnimationService.class) {
                local = instance;
                if (local == null) {
                    local = new GameAnimationService();
                    instance = local;
                }
            }
        }
        return local;
    }

    // ============================================================= 开关

    /**
     * 当前是否允许播放动画：优先取测试覆盖值，否则实时读
     * {@code GameSettings.animationEnabled}；读取失败按默认 {@code true} 处理（与旧配置兼容）。
     */
    public boolean isEnabled() {
        if (enabledOverride != null) {
            return enabledOverride;
        }
        try {
            return SettingsService.getInstance().getSettings().isAnimationEnabled();
        } catch (Throwable t) {
            return true;
        }
    }

    /** 测试 / 调试用：覆盖开关（传 null 恢复跟随设置）。 */
    public void setEnabledOverride(Boolean enabled) {
        this.enabledOverride = enabled;
    }

    // ============================================================= 统计（自检用）

    /** 某类动画已播放的次数（关闭动画时不计数）。 */
    public synchronized int getPlayCount(Kind kind) {
        Integer v = playCounts.get(kind);
        return v == null ? 0 : v;
    }

    /** 最近一次播放的动画类型（未播放过为 null）。 */
    public synchronized Kind getLastPlayed() {
        return lastPlayed;
    }

    /** 清空统计。 */
    public synchronized void resetStats() {
        playCounts.clear();
        lastPlayed = null;
    }

    private synchronized boolean begin(Kind kind) {
        if (!isEnabled()) {
            return false;
        }
        playCounts.merge(kind, 1, Integer::sum);
        lastPlayed = kind;
        return true;
    }

    // ============================================================= 出牌动画

    /**
     * 出牌动画：卡牌从 {@code (fromX, fromY)}（场景坐标）飞到其布局位置，带缩放 + 淡入。
     *
     * @param card       目标卡牌节点
     * @param fromX      起始 x（场景坐标）
     * @param fromY      起始 y（场景坐标）
     * @param onComplete 动画结束回调（关闭动画 / 节点不可用时立即回调，保证业务不阻塞）
     * @return 是否真的播放了动画
     */
    public boolean playCardAnimation(Node card, double fromX, double fromY, Runnable onComplete) {
        if (!begin(Kind.CARD)) {
            if (onComplete != null) {
                onComplete.run();
            }
            return false;
        }
        try {
            if (card != null && onFx()) {
                if (!card.getStyleClass().contains("card-animation")) {
                    card.getStyleClass().add("card-animation");
                }
                CardFlyAnimation.flyTo(card, fromX, fromY, onComplete);
            } else if (onComplete != null) {
                onComplete.run();
            }
        } catch (Throwable t) {
            if (onComplete != null) {
                onComplete.run();
            }
        }
        return true;
    }

    /**
     * 出牌动画（动作式重载）：把一段现成的动画动作（如桌上多张牌的集体飞入）
     * 纳入统一开关管理。动画关闭时不执行 {@code animation}，只执行 {@code onComplete}。
     *
     * @return 是否真的执行了动画动作
     */
    public boolean playCardAnimation(Runnable animation, Runnable onComplete) {
        if (!begin(Kind.CARD)) {
            if (onComplete != null) {
                onComplete.run();
            }
            return false;
        }
        try {
            if (animation != null && onFx()) {
                animation.run();
            } else if (onComplete != null) {
                onComplete.run();
                return true;
            }
        } catch (Throwable t) {
            if (onComplete != null) {
                onComplete.run();
            }
        }
        return true;
    }

    // ============================================================= 胜利 / 失败

    /**
     * 胜利动画：在 {@code root} 上铺一层金色星光粒子，并对 {@code target} 做金色辉光脉冲。
     * 同时播放 {@code win.wav}（受音效开关控制）。
     */
    public boolean playWinAnimation(Pane root, Node target) {
        sound(SoundEffect.WIN);
        if (!begin(Kind.WIN)) {
            return false;
        }
        try {
            if (onFx() && root != null) {
                ParticleEffect stars = ParticleEffect.winStars();
                attachOverlay(root, stars, null, 2.4);
            }
            if (onFx() && target != null) {
                glowPulse(target, Color.web("#ffd54f"), 1.06);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    /**
     * 失败动画：对 {@code target} 做一次下沉 + 灰化抖动。同时播放 {@code lose.wav}。
     */
    public boolean playLoseAnimation(Node target) {
        sound(SoundEffect.LOSE);
        if (!begin(Kind.LOSE)) {
            return false;
        }
        try {
            if (onFx() && target != null) {
                Timeline shake = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(target.translateXProperty(), 0),
                                new KeyValue(target.opacityProperty(), 1.0)),
                        new KeyFrame(Duration.millis(60), new KeyValue(target.translateXProperty(), -8)),
                        new KeyFrame(Duration.millis(120), new KeyValue(target.translateXProperty(), 8)),
                        new KeyFrame(Duration.millis(180), new KeyValue(target.translateXProperty(), -5)),
                        new KeyFrame(Duration.millis(240), new KeyValue(target.translateXProperty(), 0)));
                shake.play();
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    // ============================================================= 金币

    /**
     * 金币飞入动画：从 {@code from} 飞向 {@code to}（两者都需已入场景）。
     * 同时播放 {@code coin.wav}。
     *
     * @param onComplete 动画结束回调（可为 null）
     */
    public boolean playCoinAnimation(Node from, Node to, Runnable onComplete) {
        sound(SoundEffect.COIN_GAIN);
        if (!begin(Kind.COIN)) {
            if (onComplete != null) {
                onComplete.run();
            }
            return false;
        }
        boolean visual = false;
        try {
            if (onFx() && from != null && to != null && from.getScene() != null
                    && from.getScene().getRoot() instanceof Pane pane) {
                visual = spawnFlyingCoins(pane, from, to);
            }
        } catch (Throwable ignored) {
        }
        if (!visual && onComplete != null) {
            onComplete.run();
        } else if (visual && onComplete != null) {
            // 飞行动画约 620ms 后回调
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(620), e -> onComplete.run()));
            delay.play();
        }
        return true;
    }

    /** 在 {@code pane} 上生成一批从 from 飞向 to 的金币，飞完后自动移除。 */
    private boolean spawnFlyingCoins(Pane pane, Node from, Node to) {
        Bounds fb = from.localToScene(from.getBoundsInLocal());
        Bounds tb = to.localToScene(to.getBoundsInLocal());
        Point2D start = pane.sceneToLocal(fb.getMinX() + fb.getWidth() / 2,
                fb.getMinY() + fb.getHeight() / 2);
        Point2D end = pane.sceneToLocal(tb.getMinX() + tb.getWidth() / 2,
                tb.getMinY() + tb.getHeight() / 2);
        if (start == null || end == null) {
            return false;
        }
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        int count = 8;
        for (int i = 0; i < count; i++) {
            Circle coin = new Circle(6.5);
            coin.getStyleClass().add("coin-fly");
            coin.setFill(Color.web("#ffd54f"));
            coin.setStroke(Color.web("#b07f1e"));
            coin.setStrokeWidth(1.4);
            coin.setManaged(false);
            coin.setMouseTransparent(true);
            coin.relocate(start.getX() - 6.5, start.getY() - 6.5);
            coin.setOpacity(0.0);
            pane.getChildren().add(coin);

            double delay = i * 45.0;
            double jitterY = (i - count / 2.0) * 6;
            Timeline fly = new Timeline(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(coin.opacityProperty(), 0.0),
                            new KeyValue(coin.translateXProperty(), 0),
                            new KeyValue(coin.translateYProperty(), 0)),
                    new KeyFrame(Duration.millis(delay + 120),
                            new KeyValue(coin.opacityProperty(), 1.0),
                            new KeyValue(coin.translateXProperty(), dx * 0.35),
                            new KeyValue(coin.translateYProperty(), dy * 0.35 + jitterY),
                            new KeyValue(coin.scaleXProperty(), 1.15),
                            new KeyValue(coin.scaleYProperty(), 1.15)),
                    new KeyFrame(Duration.millis(delay + 520),
                            new KeyValue(coin.opacityProperty(), 0.0),
                            new KeyValue(coin.translateXProperty(), dx),
                            new KeyValue(coin.translateYProperty(), dy),
                            new KeyValue(coin.scaleXProperty(), 0.5),
                            new KeyValue(coin.scaleYProperty(), 0.5)));
            fly.setOnFinished(e -> pane.getChildren().remove(coin));
            fly.play();
        }
        return true;
    }

    // ============================================================= 升级

    /**
     * 升级动画：目标外围出现金色光环（粒子）+ 缩放脉冲。同时播放 {@code level_up.wav}。
     */
    public boolean playLevelUpAnimation(Node target) {
        sound(SoundEffect.LEVEL_UP);
        if (!begin(Kind.LEVEL_UP)) {
            return false;
        }
        try {
            if (onFx() && target != null) {
                scalePulse(target, 1.12);
                glowPulse(target, Color.web("#ffe9b0"), 1.0);
                if (target.getScene() != null && target.getScene().getRoot() instanceof Pane pane) {
                    ParticleEffect halo = ParticleEffect.levelHalo();
                    halo.setLifetime(2.0);
                    attachOverlay(pane, halo, target, 2.0);
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    // ============================================================= 按钮反馈

    /**
     * 按钮点击反馈：轻微缩放 0.95 → 1.05 → 1，总时长 {@link #BUTTON_FEEDBACK}（150ms）。
     * 只改 scale，不影响布局与功能。
     */
    public boolean playButtonFeedback(Node node) {
        if (!begin(Kind.BUTTON)) {
            return false;
        }
        try {
            if (onFx() && node != null) {
                Timeline t = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(node.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                                new KeyValue(node.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                        new KeyFrame(Duration.millis(75),
                                new KeyValue(node.scaleXProperty(), 1.05, Interpolator.EASE_BOTH),
                                new KeyValue(node.scaleYProperty(), 1.05, Interpolator.EASE_BOTH)),
                        new KeyFrame(BUTTON_FEEDBACK,
                                new KeyValue(node.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                                new KeyValue(node.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)));
                t.play();
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    /**
     * 为整棵界面树里的所有 {@link Button} 安装点击缩放反馈（幂等，可重复调用）。
     *
     * <p>通过事件过滤器实现，不覆盖按钮原有的 {@code setOnAction}，因此不影响任何功能。
     */
    public void installButtonFeedback(Parent root) {
        if (root == null) {
            return;
        }
        walkInstall(root);
    }

    private void walkInstall(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof ButtonBase button && button.getProperties().get(BUTTON_MARKER) == null) {
            button.getProperties().put(BUTTON_MARKER, Boolean.TRUE);
            button.addEventFilter(javafx.event.ActionEvent.ACTION, e -> playButtonFeedback(button));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                walkInstall(child);
            }
        }
    }

    // ============================================================= 提示条

    /**
     * 统一提示条。动画总开关关闭时仍展示静态提示（只去掉淡入淡出）。
     *
     * @return 是否已展示
     */
    public boolean showToast(Node anchor, String text) {
        if (anchor == null || text == null || text.isBlank()) {
            return false;
        }
        boolean animated = isEnabled();
        synchronized (this) {
            playCounts.merge(Kind.TOAST, 1, Integer::sum);
            lastPlayed = Kind.TOAST;
        }
        try {
            if (!onFx()) {
                Platform.runLater(() -> GameToast.show(anchor, text, GameToast.DEFAULT_DURATION, animated));
            } else {
                GameToast.show(anchor, text, GameToast.DEFAULT_DURATION, animated);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ============================================================= 内部

    /** 把粒子层铺满 {@code pane}，可选对齐到 {@code target} 中心。 */
    private void attachOverlay(Pane pane, ParticleEffect effect, Node target, double lifetimeSec) {
        if (pane == null || effect == null) {
            return;
        }
        double w = pane.getWidth() > 0 ? pane.getWidth() : 900;
        double h = pane.getHeight() > 0 ? pane.getHeight() : 600;
        effect.setManaged(false);
        effect.resize(w, h);
        effect.relocate(0, 0);
        if (target != null) {
            Bounds b = target.localToScene(target.getBoundsInLocal());
            Point2D p = pane.sceneToLocal(b.getMinX() + b.getWidth() / 2,
                    b.getMinY() + b.getHeight() / 2);
            if (p != null) {
                effect.setOrigin(p.getX(), p.getY());
            }
        }
        effect.setLifetime(lifetimeSec);
        effect.setOnFinished(() -> pane.getChildren().remove(effect));
        pane.getChildren().add(effect);
        effect.play();
    }

    /** 缩放脉冲（1.0 → peak → 1.0）。 */
    private void scalePulse(Node node, double peak) {
        ScaleTransition st = new ScaleTransition(Duration.millis(320), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(peak);
        st.setToY(peak);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    /** 金色辉光脉冲（DropShadow 半径呼吸），结束后还原原有效果。 */
    private void glowPulse(Node node, Color color, double scalePeak) {
        Effect previous = node.getEffect();
        DropShadow glow = new DropShadow();
        glow.setColor(color);
        glow.setRadius(6);
        glow.setSpread(0.25);
        node.setEffect(glow);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 6)),
                new KeyFrame(Duration.millis(260), new KeyValue(glow.radiusProperty(), 30)),
                new KeyFrame(Duration.millis(900), new KeyValue(glow.radiusProperty(), 4)));
        t.setOnFinished(e -> node.setEffect(previous));
        t.play();
    }

    /** 播放音效（只受音效开关控制，与动画开关无关）。 */
    private static void sound(SoundEffect effect) {
        try {
            AudioService.getInstance().playEffect(effect);
        } catch (Throwable ignored) {
        }
    }

    private static boolean onFx() {
        try {
            return Platform.isFxApplicationThread();
        } catch (Throwable t) {
            return false;
        }
    }
}
