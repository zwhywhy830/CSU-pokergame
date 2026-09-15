package com.cards.ui.animation;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 转场覆盖层（Scene Transition Overlay）：铺满整个 Stage 的 {@link StackPane}，
 * 负责转场期间“旧场景之外”的全部视觉，是 {@link SceneTransition} 的演出舞台。
 *
 * <p><b>五层结构</b>（自下而上）：
 * <ol>
 *   <li><b>旧场景截图层</b> {@code ghost} —— 切换前抓取的旧场景截图（{@link ImageView}），
 *       负责“旧场景淡出 / 缩小 / 旋转 / 景深虚化”（{@link GaussianBlur}）；</li>
 *   <li><b>金色光幕层</b> {@code curtain} + {@code sweep} —— 金色径向光幕 / 暗幕呼吸，
 *       以及手游按钮式的横向金色扫光（LightSweep）；</li>
 *   <li><b>花色粒子层</b> {@code particles} —— {@link SuitParticleSystem}，
 *       ♠♥♦♣ 沿贝塞尔曲线飞散 / 聚合，金币与皇冠粒子；</li>
 *   <li><b>扑克牌动画层</b> {@code ring} + {@code portal} —— 中心扩散的金色光环，
 *       以及中央巨大牌背 {@link CardPortal}；</li>
 *   <li><b>新场景揭示层</b> {@link #revealLayer()} —— 新场景原有内容被临时收进这一层，
 *       由 {@link SceneTransition} 做淡入 / 缩放，位于覆盖层之下、旧场景残影之上。</li>
 * </ol>
 *
 * <p>因为残影、光幕、粒子都留在覆盖层里独立演出，新场景的淡入不会把残影“带暗”，
 * 二者互不叠加。动画结束调用 {@link #finish()}：把揭示层里的节点<b>按原索引</b>搬回场景根、
 * 释放截图与定时器、还原被单独动过的节点；若转场被下一次导航打断也会先 {@link #finish()} 收干净，
 * 保证场景结构零残留。
 *
 * <p>全部视觉元素均由 Canvas / Shape 现场绘制，不依赖任何图片资源。
 * 必须运行在 JavaFX Application Thread。
 */
public final class SceneTransitionOverlay extends StackPane {

    private static final Color GOLD = Color.web("#ffd54f");
    private static final Color GOLD_LIGHT = Color.web("#fff3c4");
    private static final Color GOLD_DEEP = Color.web("#dcb860");
    private static final Color NIGHT = Color.web("#04150d");

    /** 金色光环画布边长（动画靠 scale 放大，画布不重绘）。 */
    private static final double RING_SIZE = 520.0;

    /** 当前仍在播放的覆盖层：同一时刻只允许一个，新的导航会立刻结束旧的。 */
    private static SceneTransitionOverlay active;

    private final ImageView ghost;
    private final GaussianBlur ghostBlur = new GaussianBlur(0);
    private final Canvas curtain;
    private final Canvas sweep;
    private final Canvas ring;
    private final SuitParticleSystem particles;
    private final CardPortal portal;

    private final DoubleProperty goldVeil = new SimpleDoubleProperty(0.0);
    private final DoubleProperty darkVeil = new SimpleDoubleProperty(0.0);
    private final DoubleProperty sweepProgress = new SimpleDoubleProperty(-0.45);

    private final List<Animation> running = new ArrayList<>();
    private final List<Touch> touched = new ArrayList<>();

    private StackPane outerRoot;
    private StackPane revealLayer;
    private List<Node> content;
    private Scene wrappedScene;
    private Parent wrappedOriginalRoot;
    private boolean finished;

    private SceneTransitionOverlay(Scene oldScene) {
        // 转场期间吞掉点击，避免用户在动画中重复触发导航
        setPickOnBounds(true);
        setFocusTraversable(false);

        // ---------- ① 旧场景截图层 ----------
        ghost = snapshot(oldScene);
        if (ghost != null) {
            ghost.fitWidthProperty().bind(widthProperty());
            ghost.fitHeightProperty().bind(heightProperty());
            ghost.setEffect(ghostBlur);
            getChildren().add(ghost);
        }

        // ---------- ② 金色光幕层 ----------
        curtain = new Canvas();
        curtain.setMouseTransparent(true);
        curtain.widthProperty().bind(widthProperty());
        curtain.heightProperty().bind(heightProperty());
        goldVeil.addListener((o, a, b) -> paintCurtain());
        darkVeil.addListener((o, a, b) -> paintCurtain());
        getChildren().add(curtain);

        sweep = new Canvas();
        sweep.setMouseTransparent(true);
        sweep.widthProperty().bind(widthProperty());
        sweep.heightProperty().bind(heightProperty());
        sweepProgress.addListener((o, a, b) -> paintSweep());
        getChildren().add(sweep);

        // ---------- ④ 扑克牌动画层：金色光环 ----------
        ring = buildRing(RING_SIZE);
        ring.setOpacity(0.0);
        getChildren().add(ring);

        // ---------- ③ 花色粒子层 ----------
        particles = new SuitParticleSystem();
        particles.widthProperty().bind(widthProperty());
        particles.heightProperty().bind(heightProperty());
        getChildren().add(particles);

        // ---------- ④ 扑克牌动画层：中央巨大牌背 ----------
        portal = new CardPortal();
        getChildren().add(portal);
    }

    /** 抓取旧场景快照并创建覆盖层；旧场景为空或抓取失败时返回“无残影”的可用覆盖层。 */
    public static SceneTransitionOverlay capture(Scene oldScene) {
        return new SceneTransitionOverlay(oldScene);
    }

    /** 结束上一个仍在播放的转场（幂等）。 */
    public static void abortActive() {
        SceneTransitionOverlay current = active;
        if (current != null) {
            current.finish();
        }
    }

    /**
     * 把覆盖层挂到新场景最顶层。
     *
     * <p>新场景原有内容会临时包进“新场景揭示层”；非 {@link StackPane} 根
     * （如扑克牌展示页的 BorderPane）会先包一层 StackPane，保证覆盖层能铺满整屏。
     */
    public void install(Scene next) {
        Parent root = next.getRoot();
        StackPane outer;
        if (root instanceof StackPane sp) {
            outer = sp;
        } else {
            // 临时包一层 StackPane（转场结束后会在 finish() 里换回原根节点）
            StackPane wrapper = new StackPane(root);
            next.setRoot(wrapper);
            wrappedScene = next;
            wrappedOriginalRoot = root;
            outer = wrapper;
        }

        outerRoot = outer;
        content = new ArrayList<>(outer.getChildren());
        outer.getChildren().clear();

        revealLayer = new StackPane();
        revealLayer.setPickOnBounds(false);
        revealLayer.getChildren().setAll(content);

        outer.getChildren().addAll(revealLayer, this);
        active = this;
    }

    /**
     * 把覆盖层挂到 AppShell 的单一 Scene 根 StackPane 上（单 Scene 模式）。
     *
     * <p>与 {@link #install(Scene)} 不同，本方法不切 Scene：调用前应由 AppShell.navigate
     * 把 root 的 children 替换成新页面，本方法把新页面内容包进揭示层，再叠 overlay。
     * 旧场景残影已在 {@link #capture(Scene)} 时抓取，独立于 root 的 children。
     */
    public void installOnStackPane(StackPane root) {
        outerRoot = root;
        content = new ArrayList<>(root.getChildren());
        root.getChildren().clear();

        revealLayer = new StackPane();
        revealLayer.setPickOnBounds(false);
        revealLayer.getChildren().setAll(content);

        root.getChildren().addAll(revealLayer, this);
        active = this;
    }

    /** 新场景揭示层：新场景原有内容的容器，由 {@link SceneTransition} 做淡入 / 缩放。 */
    public StackPane revealLayer() {
        return revealLayer;
    }

    /** 登记外部动画（如揭示层淡入），便于转场被打断时统一停止。 */
    public void track(Animation animation) {
        if (animation != null) {
            running.add(animation);
        }
    }

    // ============================================================= ① 旧场景截图

    /**
     * 旧场景残影退场。
     *
     * @param scaleTo  缩放目标（&lt;1 缩小，&gt;1 推近）
     * @param blurTo   景深虚化半径（0 表示不虚化）
     * @param rotateTo 旋转角度（度）
     */
    public void ghostOut(Duration delay, Duration duration, double scaleTo, double blurTo, double rotateTo) {
        if (ghost == null) {
            return;
        }
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ghost.opacityProperty(), ghost.getOpacity()),
                        new KeyValue(ghost.scaleXProperty(), ghost.getScaleX()),
                        new KeyValue(ghost.scaleYProperty(), ghost.getScaleY()),
                        new KeyValue(ghost.rotateProperty(), ghost.getRotate()),
                        new KeyValue(ghostBlur.radiusProperty(), ghostBlur.getRadius())),
                new KeyFrame(duration,
                        new KeyValue(ghost.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(ghost.scaleXProperty(), scaleTo, Interpolator.EASE_IN),
                        new KeyValue(ghost.scaleYProperty(), scaleTo, Interpolator.EASE_IN),
                        new KeyValue(ghost.rotateProperty(), rotateTo, Interpolator.EASE_IN),
                        new KeyValue(ghostBlur.radiusProperty(), blurTo, Interpolator.EASE_IN)));
        tl.setDelay(delay);
        play(tl);
    }

    /** 旧场景残影提前“定格”（用于只渐暗不消失的返回流程）。 */
    public void ghostDarken(Duration delay, Duration duration, double opacityTo) {
        if (ghost == null) {
            return;
        }
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(ghost.opacityProperty(), ghost.getOpacity())),
                new KeyFrame(duration, new KeyValue(ghost.opacityProperty(), opacityTo, Interpolator.EASE_IN)));
        tl.setDelay(delay);
        play(tl);
    }

    // ============================================================= ② 金色光幕

    /** 金色光幕呼吸：0 → peak → 0。 */
    public void curtainGlow(Duration delay, Duration duration, double peak) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(goldVeil, goldVeil.get())),
                new KeyFrame(duration.multiply(0.34), new KeyValue(goldVeil, peak, Interpolator.EASE_OUT)),
                new KeyFrame(duration, new KeyValue(goldVeil, 0.0, Interpolator.EASE_IN)));
        tl.setDelay(delay);
        play(tl);
    }

    /** 暗幕渐暗（返回大厅：牌桌先暗下去）。 */
    public void curtainDarken(Duration delay, Duration duration, double peak) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(darkVeil, darkVeil.get())),
                new KeyFrame(duration, new KeyValue(darkVeil, peak, Interpolator.EASE_OUT)));
        tl.setDelay(delay);
        play(tl);
    }

    /** 横向金色扫光（LightSweep）：像手游按钮高光一样掠过整屏。 */
    public void lightSweep(Duration delay, Duration duration) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(sweepProgress, -0.45)),
                new KeyFrame(duration, new KeyValue(sweepProgress, 1.45, Interpolator.EASE_BOTH)));
        tl.setDelay(delay);
        tl.setOnFinished(e -> {
            sweepProgress.set(-0.45);
            paintSweep();
        });
        play(tl);
    }

    // ============================================================= ④ 金色光环

    /** 金色光环自中心扩散。 */
    public void ringExpand(Duration delay, Duration duration, double maxScale) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ring.opacityProperty(), 0.0),
                        new KeyValue(ring.scaleXProperty(), 0.16),
                        new KeyValue(ring.scaleYProperty(), 0.16),
                        new KeyValue(ring.rotateProperty(), 0.0)),
                new KeyFrame(duration.multiply(0.30),
                        new KeyValue(ring.opacityProperty(), 0.92, Interpolator.EASE_OUT)),
                new KeyFrame(duration,
                        new KeyValue(ring.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(ring.scaleXProperty(), maxScale, Interpolator.EASE_OUT),
                        new KeyValue(ring.scaleYProperty(), maxScale, Interpolator.EASE_OUT),
                        new KeyValue(ring.rotateProperty(), 90.0, Interpolator.EASE_OUT)));
        tl.setDelay(delay);
        play(tl);
    }

    /** 金色光环向中心收拢（返回大厅：粒子聚合）。 */
    public void ringCollapse(Duration delay, Duration duration, double fromScale) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ring.opacityProperty(), 0.0),
                        new KeyValue(ring.scaleXProperty(), fromScale),
                        new KeyValue(ring.scaleYProperty(), fromScale),
                        new KeyValue(ring.rotateProperty(), 0.0)),
                new KeyFrame(duration.multiply(0.22),
                        new KeyValue(ring.opacityProperty(), 0.75, Interpolator.EASE_OUT)),
                new KeyFrame(duration,
                        new KeyValue(ring.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(ring.scaleXProperty(), 0.20, Interpolator.EASE_IN),
                        new KeyValue(ring.scaleYProperty(), 0.20, Interpolator.EASE_IN),
                        new KeyValue(ring.rotateProperty(), -70.0, Interpolator.EASE_IN)));
        tl.setDelay(delay);
        play(tl);
    }

    // ============================================================= ③ 花色粒子

    /** ♠♥♦♣ 自中心向四周飞散（进入牌桌）。 */
    public void particlesBurst(Duration delay, Duration life, int count) {
        double sec = life.toSeconds();
        emit(delay, () -> {
            double[] c = center();
            particles.burst(c[0], c[1], span(), count, sec);
        });
    }

    /** 卡牌碎片 + 金色粒子向中心聚合（返回大厅）。 */
    public void particlesAbsorb(Duration delay, Duration life, int count) {
        double sec = life.toSeconds();
        emit(delay, () -> {
            double[] c = center();
            particles.absorb(c[0], c[1], span(), count, sec);
        });
    }

    /** 金币飞散 + 皇冠残留（胜利返回）。 */
    public void particlesCelebrate(Duration delay, Duration life, int coins, int crowns) {
        double sec = life.toSeconds();
        emit(delay, () -> {
            double[] c = center();
            particles.celebrate(size()[0], size()[1], c[0], c[1], coins, crowns, sec);
        });
    }

    // ============================================================= ④ 中央牌背

    /** 中央巨大牌背出现（scale 0.1 → 1.2 → 1，rotate 0 → 180，opacity 0 → 1）。 */
    public void portalIn(Duration delay, Duration duration) {
        play(portal.playIn(delay, duration));
    }

    /** 牌背炸开并淡出（rotate 180 → 360），新场景自牌面背后展开。 */
    public void portalBurst(Duration delay, Duration duration) {
        play(portal.playBurst(delay, duration));
    }

    // ============================================================= 角色节点

    /**
     * 让新场景里的头像组件放大入场（打开个人信息）。
     *
     * <p>按样式类 {@code .avatar-view} 查找（{@link com.cards.ui.component.AvatarView} 构造时注册）；
     * 查不到就静默跳过，不影响转场其余部分。被改动过的节点会登记下来，
     * {@link #finish()} 时精确还原，避免转场被打断后残留缩放。
     */
    public void avatarZoom(Scene next, Duration delay, Duration duration) {
        Node avatar = lookupAvatar(next);
        if (avatar == null) {
            return;
        }
        touched.add(new Touch(avatar));
        avatar.setOpacity(0.0);
        avatar.setScaleX(0.55);
        avatar.setScaleY(0.55);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(avatar.opacityProperty(), 0.0),
                        new KeyValue(avatar.scaleXProperty(), 0.55),
                        new KeyValue(avatar.scaleYProperty(), 0.55)),
                new KeyFrame(duration.multiply(0.74),
                        new KeyValue(avatar.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(avatar.scaleXProperty(), 1.08, Interpolator.EASE_OUT),
                        new KeyValue(avatar.scaleYProperty(), 1.08, Interpolator.EASE_OUT)),
                new KeyFrame(duration,
                        new KeyValue(avatar.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(avatar.scaleYProperty(), 1.0, Interpolator.EASE_OUT)));
        tl.setDelay(delay);
        play(tl);
    }

    // ============================================================= 收尾

    /** 还原场景结构、释放截图与定时器、移除覆盖层（幂等，可在动画结束或被打断时调用）。 */
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;

        for (Animation a : new ArrayList<>(running)) {
            if (a.getStatus() == Animation.Status.RUNNING) {
                a.stop();
            }
        }
        running.clear();

        // 还原被单独动过的节点（头像缩放等）
        for (Touch t : touched) {
            t.restore();
        }
        touched.clear();

        // 场景截图及时释放
        if (ghost != null) {
            ghost.setEffect(null);
            ghost.setImage(null);
        }
        particles.dispose();

        if (revealLayer != null && outerRoot != null && content != null) {
            int index = outerRoot.getChildren().indexOf(revealLayer);
            if (index < 0) {
                index = 0;
            }
            revealLayer.getChildren().clear();
            outerRoot.getChildren().remove(revealLayer);
            outerRoot.getChildren().addAll(index, content);
        }
        if (outerRoot != null) {
            outerRoot.getChildren().remove(this);
        }
        getChildren().clear();

        // 非 StackPane 根：把临时包装层撤掉，场景根恢复成原节点
        if (wrappedScene != null && wrappedOriginalRoot != null) {
            if (outerRoot != null) {
                outerRoot.getChildren().remove(wrappedOriginalRoot);
            }
            wrappedScene.setRoot(wrappedOriginalRoot);
            wrappedScene = null;
            wrappedOriginalRoot = null;
        }

        if (active == this) {
            active = null;
        }
    }

    // ============================================================= 内部实现

    /** 播放并登记动画（不覆盖调用方自己设置的 onFinished）。 */
    private void play(Animation animation) {
        running.add(animation);
        animation.play();
    }

    /**
     * 延迟发射粒子：额外等 16ms（一帧布局）再发射，
     * 否则覆盖层还没拿到尺寸，粒子会挤在左上角。
     */
    private void emit(Duration delay, Runnable action) {
        PauseTransition kick = new PauseTransition(delay.add(Duration.millis(16)));
        kick.setOnFinished(e -> action.run());
        play(kick);
    }

    private double[] size() {
        double w = getWidth() > 1 ? getWidth() : (getScene() != null ? getScene().getWidth() : 960);
        double h = getHeight() > 1 ? getHeight() : (getScene() != null ? getScene().getHeight() : 640);
        return new double[]{Math.max(1, w), Math.max(1, h)};
    }

    private double[] center() {
        double[] s = size();
        return new double[]{s[0] / 2.0, s[1] / 2.0};
    }

    private double span() {
        double[] s = size();
        return Math.max(s[0], s[1]);
    }

    private static Node lookupAvatar(Scene next) {
        if (next == null || next.getRoot() == null) {
            return null;
        }
        try {
            return next.getRoot().lookup(".avatar-view");
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 金色光幕 + 暗幕重绘。 */
    private void paintCurtain() {
        double w = curtain.getWidth();
        double h = curtain.getHeight();
        GraphicsContext gc = curtain.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        if (w <= 1 || h <= 1) {
            return;
        }

        double gold = clamp01(goldVeil.get());
        if (gold > 0.002) {
            double r = Math.max(w, h) * 0.80;
            gc.setGlobalAlpha(gold);
            gc.setFill(new RadialGradient(0, 0, w / 2.0, h / 2.0, r, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, alpha(GOLD, 0.58)),
                    new Stop(0.45, alpha(GOLD_DEEP, 0.24)),
                    new Stop(1.0, Color.web("#000000", 0.0))));
            gc.fillRect(0, 0, w, h);
        }

        double dark = clamp01(darkVeil.get());
        if (dark > 0.002) {
            gc.setGlobalAlpha(dark);
            gc.setFill(NIGHT);
            gc.fillRect(0, 0, w, h);
        }
        gc.setGlobalAlpha(1.0);
    }

    /** 横向金色扫光重绘：一条斜切的金色渐变光带掠过整屏。 */
    private void paintSweep() {
        double w = sweep.getWidth();
        double h = sweep.getHeight();
        GraphicsContext gc = sweep.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        if (w <= 1 || h <= 1) {
            return;
        }

        double p = sweepProgress.get();
        if (p < -0.30 || p > 1.30) {
            return;
        }
        double band = w * 0.26;
        double x = -band + p * (w + band * 2.0);
        double skew = h * 0.34;

        gc.save();
        gc.setGlobalAlpha(0.55 * Math.sin(Math.PI * clamp01(p)));
        gc.setFill(new LinearGradient(x, 0, x + band, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, alpha(GOLD, 0.0)),
                new Stop(0.5, alpha(GOLD_LIGHT, 0.60)),
                new Stop(1.0, alpha(GOLD, 0.0))));
        gc.beginPath();
        gc.moveTo(x - skew, 0);
        gc.lineTo(x + band - skew, 0);
        gc.lineTo(x + band, h);
        gc.lineTo(x, h);
        gc.closePath();
        gc.fill();
        gc.restore();
    }

    /** 金色光环画布：柔光外圈 + 两道金环（Canvas 绘制，非图片）。 */
    private static Canvas buildRing(double diameter) {
        Canvas c = new Canvas(diameter, diameter);
        c.setMouseTransparent(true);
        GraphicsContext gc = c.getGraphicsContext2D();
        double r = diameter / 2.0;

        gc.setFill(new RadialGradient(0, 0, r, r, r * 0.96, false, CycleMethod.NO_CYCLE,
                new Stop(0.60, alpha(GOLD, 0.0)),
                new Stop(0.86, alpha(GOLD, 0.42)),
                new Stop(1.0, alpha(GOLD_LIGHT, 0.0))));
        gc.fillOval(0, 0, diameter, diameter);

        gc.setStroke(Color.web("#ffe9b0", 0.90));
        gc.setLineWidth(3.0);
        gc.strokeOval(r * 0.10, r * 0.10, r * 1.80, r * 1.80);

        gc.setStroke(alpha(GOLD_DEEP, 0.55));
        gc.setLineWidth(1.4);
        gc.strokeOval(r * 0.26, r * 0.26, r * 1.48, r * 1.48);
        return c;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /** 同色不同透明度（避免散落的 rgba 字面量）。 */
    private static Color alpha(Color c, double a) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** 抓取场景根节点快照（失败返回 null，转场退化为无残影）。 */
    private static ImageView snapshot(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return null;
        }
        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage shot = scene.getRoot().snapshot(params, null);
            if (shot == null) {
                return null;
            }
            ImageView view = new ImageView(shot);
            view.setMouseTransparent(true);
            view.setPreserveRatio(false);
            return view;
        } catch (Throwable ignored) {
            // 快照失败（如节点未上屏）不阻断切换
            return null;
        }
    }

    /** 被转场单独改动过的节点：记录原值，收尾时精确还原。 */
    private static final class Touch {
        private final Node node;
        private final double scaleX;
        private final double scaleY;
        private final double opacity;
        private final double translateY;
        private final double rotate;

        Touch(Node node) {
            this.node = node;
            this.scaleX = node.getScaleX();
            this.scaleY = node.getScaleY();
            this.opacity = node.getOpacity();
            this.translateY = node.getTranslateY();
            this.rotate = node.getRotate();
        }

        void restore() {
            node.setScaleX(scaleX);
            node.setScaleY(scaleY);
            node.setOpacity(opacity);
            node.setTranslateY(translateY);
            node.setRotate(rotate);
        }
    }
}
