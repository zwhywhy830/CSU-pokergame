package com.cards.ui.background;

import com.cards.ui.ParticleField;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 背景系统：统一构建各场景的背景层栈，替代原先散落在 DeckApp 中的
 * {@code sceneBackgroundImage / drawLandscape / fabricTexture} 样板代码。
 *
 * <p>手游级分层背景（自下而上，全部为节点叠加，不逐帧重绘位图）：
 * <ol>
 *   <li><b>美术层</b>：照片 cover 模式等比铺满（{@code preserveRatio}，禁止拉伸），
 *       并做 8 秒一轮的缓慢呼吸（{@code ImageView} 的 scaleX/Y 1.0 → 1.03 → 1.0）；
 *       资源缺失时回退到 Canvas 国风山水；</li>
 *   <li><b>主题色调层</b>：设置页「界面主题」的轻度色偏（不再压暗全屏）；</li>
 *   <li><b>明暗渐变层</b>：顶部 0.10 / 中部 0.15 / 底部 0.35，替代原先的全屏黑罩；</li>
 *   <li><b>底部牌桌增强层</b>：只占下方 30%，由透明渐变到 0.45 黑；</li>
 *   <li><b>金色环境光</b>：标题区径向光晕 rgba(255,215,100,0.18) → 透明；</li>
 *   <li>{@link ParticleField} 低强度粒子层（星光 + 纸牌碎片，由场景按需 play/stop）。</li>
 * </ol>
 *
 * <p>各场景仅需提供背景资源路径（大厅 / 牌桌 / 加载页三套）；设置页可通过
 * {@link #setBackground(String)} 统一切换全局背景图，已创建与后续创建的美术层都会跟随刷新。
 */
public final class BackgroundManager {

    /** 大厅 / 选游戏 / 选模式 / 个人信息 / 联机大厅默认背景。 */
    public static final String RES_LOBBY = "/assets/background/lobby_dark.jpg";
    /** 跑得快 / 骗子酒馆牌桌默认背景。 */
    public static final String RES_GAME = "/assets/background/poker_gold.jpg";
    /** 加载页默认背景。 */
    public static final String RES_LOADING = "/assets/background/fantasy_dragon.jpg";

    /** 可选背景图：设置页「背景图片」统一切换用。 */
    public static final List<BackgroundOption> OPTIONS = List.of(
            new BackgroundOption(RES_LOBBY, "墨绿大厅"),
            new BackgroundOption(RES_GAME, "鎏金牌桌"),
            new BackgroundOption(RES_LOADING, "幻境巨龙"));

    /** 一张可选背景图：资源路径 + 显示名。 */
    public record BackgroundOption(String resource, String label) {
    }

    /**
     * 全局统一背景：{@code null} 表示各页面仍用各自默认图；
     * 设置页选中某张后，所有已创建与后续创建的美术层统一使用该图。
     */
    private static String currentResource;

    /** 存活美术层的弱引用：切换背景时逐个刷新，避免持有已卸载场景的节点。 */
    private static final List<WeakReference<ArtLayer>> LIVE_ART = new ArrayList<>(8);

    /**
     * 山水背景纹理缓存：按画布尺寸缓存，尺寸不变时直接 blit。
     * 整幅山水（三层远山各 80 段折线 + 水面 + 三朵荷花各 6 片花瓣 + 72 个光点 + 双层八角金框）
     * 有数百次绘制调用，只应在尺寸变化时重跑一次。
     */
    private static WritableImage landscapeTex;
    private static int landscapeTexW = -1;
    private static int landscapeTexH = -1;

    /** 织物噪点纹理缓存：固定种子生成一次，各场景共用同一张 WritableImage。 */
    private static WritableImage fabricTex;

    /** 照片背景缓存：资源路径 → Image，各页面各加载一次，之后复用同一实例。 */
    private static final Map<String, Image> PHOTO_CACHE = new HashMap<>(4);

    /** 底部牌桌增强层的占比（只压暗下方 30%）。 */
    private static final double TABLE_BOOST_RATIO = 0.30;
    /** 金色环境光的中心位置（相对宽/高）与半径。 */
    private static final double GLOW_CENTER_X = 0.50;
    private static final double GLOW_CENTER_Y = 0.42;
    private static final double GLOW_RADIUS = 0.72;

    private BackgroundManager() {
    }

    /** 一组背景层：根节点 + 主题遮罩 + 粒子层。 */
    public static final class Background {
        private final StackPane root;
        private final Region shade;
        private final ParticleField particles;

        Background(StackPane root, Region shade, ParticleField particles) {
            this.root = root;
            this.shade = shade;
            this.particles = particles;
        }

        /** 背景根节点（鼠标穿透），直接加入场景根容器即可。 */
        public StackPane root() {
            return root;
        }

        /**
         * 主题色调层：设置页切换经典绿桌/星夜蓝时改变其背景色。
         * 只是一层很轻的色偏（alpha ≈ 0.1），不再是把画面压平的半透明黑罩。
         */
        public Region shade() {
            return shade;
        }

        /** 粒子层：场景显示时 play()、切走时 stop()。 */
        public ParticleField particles() {
            return particles;
        }
    }

    /** 首页/大厅类背景（主页、选游戏、选模式、个人信息、联机大厅）。 */
    public static Background createLobbyBackground() {
        return create(effective(RES_LOBBY));
    }

    /** 游戏牌桌背景（牌桌透明叠放其上）。 */
    public static Background createGameBackground() {
        return create(effective(RES_GAME));
    }

    /** 加载页背景。 */
    public static Background createLoadingBackground() {
        return create(effective(RES_LOADING));
    }

    /** 统一切换后的背景优先，未切换过时回退到该页面的默认图。 */
    private static String effective(String fallback) {
        return currentResource != null ? currentResource : fallback;
    }

    // ============================================================= 全局背景切换

    /** 当前统一背景资源；未切换过返回 {@code null}（各页面使用各自默认图）。 */
    public static String currentBackground() {
        return currentResource;
    }

    /** 已缓存/加载的背景图（供设置页做缩略图）；资源缺失返回 {@code null}。 */
    public static Image backgroundImage(String resource) {
        return photo(resource);
    }

    /**
     * 统一切换全局背景图：所有已创建的美术层立即刷新，之后新建的页面也使用该图。
     *
     * @param resource 背景资源路径，见 {@link #OPTIONS}
     */
    public static void setBackground(String resource) {
        if (resource == null || resource.equals(currentResource)) {
            return;
        }
        currentResource = resource;
        for (Iterator<WeakReference<ArtLayer>> it = LIVE_ART.iterator(); it.hasNext(); ) {
            ArtLayer layer = it.next().get();
            if (layer == null) {
                it.remove();
            } else {
                layer.setResource(resource);
            }
        }
    }

    // ============================================================= 背景层构建

    /**
     * 构建全屏背景层（自下而上）：美术层 → 主题色调 → 明暗渐变 → 底部牌桌增强 → 金色环境光 → 粒子层。
     * 全部为节点叠加（节点变换 / 静态渐变填充），没有任何逐帧重绘位图的逻辑。
     */
    private static Background create(String resource) {
        StackPane holder = new StackPane();

        // 1) 美术层：cover 铺满 + 缓慢呼吸
        ArtLayer art = new ArtLayer(resource);

        // 2) 主题色调层：很轻的色偏（设置页「界面主题」作用于此层），不再压暗全屏
        Region tint = new Region();
        tint.getStyleClass().add("bg-theme-shade");
        tint.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tint.setMouseTransparent(true);

        // 3) 明暗渐变：顶部 0.10 → 中部 0.15 → 底部 0.35
        Region vignette = fillLayer(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(0, 0, 0, 0.10)),
                new Stop(0.45, Color.rgb(0, 0, 0, 0.15)),
                new Stop(1.00, Color.rgb(0, 0, 0, 0.35))));

        // 4) 底部牌桌增强：只占下方 30%，由透明渐变到 0.45 黑，压住牌桌区的杂乱底纹
        Region tableBoost = fillLayer(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(1.00, Color.rgb(0, 0, 0, 0.45))));
        tableBoost.maxHeightProperty().bind(holder.heightProperty().multiply(TABLE_BOOST_RATIO));
        StackPane.setAlignment(tableBoost, Pos.BOTTOM_CENTER);

        // 5) 标题区金色环境光：中心 rgba(255,215,100,0.18) → 边缘透明
        Region glow = fillLayer(new RadialGradient(0, 0, GLOW_CENTER_X, GLOW_CENTER_Y,
                GLOW_RADIUS, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 215, 100, 0.18)),
                new Stop(0.45, Color.rgb(255, 205, 90, 0.06)),
                new Stop(1.00, Color.TRANSPARENT)));

        ParticleField particles = new ParticleField();

        holder.getChildren().addAll(art, tint, vignette, tableBoost, glow, particles);
        holder.getStyleClass().add("bg-theme-root");
        holder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        holder.setMouseTransparent(true);
        return new Background(holder, tint, particles);
    }

    /** 构建一层铺满的纯色/渐变叠加层（鼠标穿透）。 */
    private static Region fillLayer(Paint paint) {
        Region region = new Region();
        region.setBackground(new javafx.scene.layout.Background(
                new BackgroundFill(paint, CornerRadii.EMPTY, Insets.EMPTY)));
        region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        region.setMouseTransparent(true);
        return region;
    }

    // ============================================================= 美术层

    /**
     * 美术层：照片以 cover 模式等比铺满（{@code preserveRatio=true}，绝不拉伸），
     * 超出部分居中裁剪；并用 {@code ImageView} 的 scaleX/Y 做 8 秒一轮的缓慢呼吸。
     * 呼吸只是节点变换（GPU 合成），不做位图重绘；资源缺失时内嵌矢量山水 Canvas 兜底。
     * 支持运行时切换背景图，并登记到 {@link #LIVE_ART} 供全局统一切换刷新。
     */
    private static final class ArtLayer extends Region {

        /** 呼吸放大上限与半程时长：1.0 → 1.03 → 1.0 共 8 秒。 */
        private static final double BREATH_TO = 1.03;
        private static final double BREATH_HALF_SECONDS = 4.0;

        private final ImageView view = new ImageView();
        private final Rectangle clip = new Rectangle();
        private String resource;
        private Canvas fallback;
        private Timeline breath;
        private boolean coverHookInstalled;

        ArtLayer(String resource) {
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            setMouseTransparent(true);

            // 裁剪到本层范围：呼吸放大时不会溢出到相邻节点
            clip.widthProperty().bind(widthProperty());
            clip.heightProperty().bind(heightProperty());
            setClip(clip);

            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setManaged(false);
            view.setMouseTransparent(true);

            LIVE_ART.add(new WeakReference<>(this));
            apply(resource);
        }

        /** cover 布局：等比放大到完全覆盖本层，居中裁剪，不做任何拉伸。 */
        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            Image img = view.getImage();
            if (img == null || w <= 0 || h <= 0) {
                return;
            }
            double iw = img.getWidth();
            double ih = img.getHeight();
            if (iw <= 0 || ih <= 0) {
                return;
            }
            double cover = Math.max(w / iw, h / ih);
            double vw = iw * cover;
            double vh = ih * cover;
            view.setFitWidth(vw);
            view.setFitHeight(vh);
            view.relocate((w - vw) / 2.0, (h - vh) / 2.0);
        }

        /** 切换到指定背景图（同一张直接跳过）。 */
        void setResource(String resource) {
            if (resource == null || resource.equals(this.resource)) {
                return;
            }
            apply(resource);
        }

        private void apply(String resource) {
            this.resource = resource;
            Image img = photo(resource);
            if (img != null) {
                view.setImage(img);
                if (!getChildren().contains(view)) {
                    getChildren().setAll(view);
                }
                if (!coverHookInstalled) {
                    // 图片是异步解码的，尺寸就绪后需要补一次 cover 排版
                    coverHookInstalled = true;
                    img.widthProperty().addListener(o -> requestLayout());
                    img.heightProperty().addListener(o -> requestLayout());
                }
                requestLayout();
                startBreath();
                return;
            }
            // 兜底：矢量山水 Canvas，尺寸跟随本层，尺寸变化时按缓存纹理重绘
            stopBreath();
            if (fallback == null) {
                fallback = new Canvas();
                fallback.widthProperty().bind(widthProperty());
                fallback.heightProperty().bind(heightProperty());
                widthProperty().addListener(o -> drawLandscape(fallback));
                heightProperty().addListener(o -> drawLandscape(fallback));
                fallback.sceneProperty().addListener((o, prev, next) -> {
                    if (next != null) {
                        // 场景挂载后补画一次（画布尺寸要等布局阶段才确定）；画完立即注销该 pulse 监听。
                        // 若保留，它会随 post-layout pulse 每帧回调，把整幅山水反复重绘一遍。
                        Runnable[] once = new Runnable[1];
                        once[0] = () -> {
                            drawLandscape(fallback);
                            next.removePostLayoutPulseListener(once[0]);
                        };
                        next.addPostLayoutPulseListener(once[0]);
                    }
                });
            }
            if (!getChildren().contains(fallback)) {
                getChildren().setAll(fallback);
            }
        }

        /** 启动（或保持）呼吸动画：只动画 ImageView 的 scaleX/Y。 */
        private void startBreath() {
            if (breath == null) {
                breath = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(view.scaleXProperty(), 1.0),
                                new KeyValue(view.scaleYProperty(), 1.0)),
                        new KeyFrame(Duration.seconds(BREATH_HALF_SECONDS),
                                new KeyValue(view.scaleXProperty(), BREATH_TO, Interpolator.EASE_BOTH),
                                new KeyValue(view.scaleYProperty(), BREATH_TO, Interpolator.EASE_BOTH)),
                        new KeyFrame(Duration.seconds(BREATH_HALF_SECONDS * 2),
                                new KeyValue(view.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                                new KeyValue(view.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)));
                breath.setCycleCount(Timeline.INDEFINITE);
            }
            if (breath.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                breath.play();
            }
        }

        private void stopBreath() {
            if (breath != null) {
                breath.stop();
            }
            view.setScaleX(1.0);
            view.setScaleY(1.0);
        }
    }

    /** 按资源路径加载照片背景（缓存复用；加载失败或资源缺失返回 null）。 */
    private static Image photo(String resource) {
        if (PHOTO_CACHE.containsKey(resource)) {
            return PHOTO_CACHE.get(resource);
        }
        Image image = null;
        try {
            if (BackgroundManager.class.getResource(resource) == null) {
                System.err.println("[background] 资源不存在，回退矢量山水: " + resource);
            } else {
                image = new Image(resource, true);
                if (image.isError()) {
                    System.err.println("[background] 图片加载失败，回退矢量山水: " + resource);
                    image = null;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("[background] 图片加载异常，回退矢量山水: " + resource + " -> " + ex);
            image = null;
        }
        PHOTO_CACHE.put(resource, image);
        return image;
    }

    /**
     * 生成一张低透明织物噪点纹理 Region，叠加在背景之上增加绒布质感。
     * （原 DeckApp.fabricTexture，固定种子保证每次启动纹理一致。）
     */
    public static Region fabricTexture() {
        int size = 72;
        if (fabricTex == null) {
            // 72×72 逐像素写入（5184 次 setColor）只需生成一次，之后复用同一张纹理
            WritableImage img = new WritableImage(size, size);
            PixelWriter pw = img.getPixelWriter();
            Random rnd = new Random(20240908L);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double v = rnd.nextDouble();
                    Color c;
                    if (v < 0.14) {
                        c = Color.rgb(255, 255, 255, 0.08 + rnd.nextDouble() * 0.04);
                    } else if (v < 0.22) {
                        c = Color.rgb(20, 12, 4, 0.08 + rnd.nextDouble() * 0.04);
                    } else {
                        c = Color.TRANSPARENT;
                    }
                    pw.setColor(x, y, c);
                }
            }
            fabricTex = img;
        }
        ImagePattern pattern = new ImagePattern(fabricTex, 0, 0, size, size, false);
        Region region = new Region();
        region.setBackground(new javafx.scene.layout.Background(
                new BackgroundFill(pattern, CornerRadii.EMPTY, Insets.EMPTY)));
        region.setMouseTransparent(true);
        region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return region;
    }

    // ============================================================= Canvas 山水绘制

    /**
     * 把缓存的山水纹理贴到画布上。
     * 纹理按画布尺寸缓存：尺寸不变时只做一次 drawImage，不再重跑整幅矢量绘制。
     */
    private static void drawLandscape(Canvas canvas) {
        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W < 10 || H < 10) {
            return;
        }
        int iw = (int) Math.ceil(W);
        int ih = (int) Math.ceil(H);
        if (landscapeTex == null || iw != landscapeTexW || ih != landscapeTexH) {
            landscapeTex = renderLandscape(iw, ih);
            landscapeTexW = iw;
            landscapeTexH = ih;
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        g.drawImage(landscapeTex, 0, 0, W, H);
    }

    /** 离屏渲染一张山水纹理：按目标尺寸绘制一次，保证与原逐帧绘制像素级一致。 */
    private static WritableImage renderLandscape(int w, int h) {
        Canvas off = new Canvas(w, h);
        paintLandscape(off.getGraphicsContext2D(), w, h);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return off.snapshot(params, new WritableImage(w, h));
    }

    /** 国风墨绿山水的实际绘制：底色 / 远山 / 水面 / 荷花 / 金色光点 / 八角金框。 */
    private static void paintLandscape(GraphicsContext g, double W, double H) {
        // 1. 墨绿渐变底色（上深下浅）
        LinearGradient bg = new LinearGradient(0, 0, 0, H, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#0c1f14")),
                new Stop(0.45, Color.web("#1a3d27")),
                new Stop(1.00, Color.web("#2d5a3a")));
        g.setFill(bg);
        g.fillRect(0, 0, W, H);

        // 2. 远山剪影（三层 misty mountains，透明度递增）
        drawMountainLayer(g, W, H * 0.55, H * 0.14, 0.22, Color.web("#0a1a10"));
        drawMountainLayer(g, W, H * 0.65, H * 0.10, 0.35, Color.web("#102a1c"));
        drawMountainLayer(g, W, H * 0.78, H * 0.08, 0.55, Color.web("#163624"));

        // 3. 底部水面渐变（更深的墨绿，带点反光）
        LinearGradient water = new LinearGradient(0, H * 0.78, 0, H, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(12, 30, 20, 0.85)),
                new Stop(1.0, Color.rgb(6, 18, 12, 0.95)));
        g.setFill(water);
        g.fillRect(0, H * 0.78, W, H * 0.22);

        // 4. 荷花剪影（底部，左右各一朵）
        drawLotus(g, W * 0.12, H * 0.86, 0.9);
        drawLotus(g, W * 0.88, H * 0.88, 1.0);
        drawLotus(g, W * 0.28, H * 0.92, 0.7);

        // 5. 金色光点（sparkles）
        Random spark = new Random(42L);
        g.setFill(Color.rgb(255, 220, 120, 0.85));
        for (int i = 0; i < 60; i++) {
            double sx = spark.nextDouble() * W;
            double sy = spark.nextDouble() * H * 0.9;
            double r = 0.5 + spark.nextDouble() * 1.8;
            g.beginPath();
            g.arc(sx, sy, r, r, 0, 360);
            g.fill();
        }
        // 几颗稍大的亮点
        g.setFill(Color.rgb(255, 230, 140, 0.95));
        for (int i = 0; i < 12; i++) {
            double sx = spark.nextDouble() * W;
            double sy = spark.nextDouble() * H * 0.85;
            double r = 2.0 + spark.nextDouble() * 3.5;
            g.beginPath();
            g.arc(sx, sy, r, r, 0, 360);
            g.fill();
            // 光晕
            g.setFill(Color.rgb(255, 220, 120, 0.25));
            g.beginPath();
            g.arc(sx, sy, r * 3, r * 3, 0, 360);
            g.fill();
            g.setFill(Color.rgb(255, 230, 140, 0.95));
        }

        // 6. 八角金色边框
        drawGoldenOctagon(g, W, H);
    }

    /** 绘制一层远山剪影。 */
    private static void drawMountainLayer(GraphicsContext g, double W, double baseY,
                                          double amplitude, double seed, Color color) {
        g.setFill(color);
        g.beginPath();
        g.moveTo(0, baseY);
        // 用三角函数叠加生成自然山形
        Random rnd = new Random((long) (seed * 1000));
        double step = W / 80;
        for (double x = 0; x <= W; x += step) {
            double y = baseY - Math.sin(x * 0.006 + seed * 3.1) * amplitude * 0.6
                    - Math.sin(x * 0.013 + seed * 1.7) * amplitude * 0.3
                    - rnd.nextDouble() * amplitude * 0.15;
            g.lineTo(x, y);
        }
        g.lineTo(W, baseY);
        g.lineTo(W, baseY + amplitude);
        g.lineTo(0, baseY + amplitude);
        g.closePath();
        g.fill();
    }

    /** 绘制一朵荷花剪影。 */
    private static void drawLotus(GraphicsContext g, double cx, double cy, double scale) {
        double petalR = 18 * scale;
        // 荷叶（圆形）
        g.setFill(Color.rgb(10, 20, 14, 0.9));
        g.beginPath();
        g.arc(cx, cy, petalR * 2.2, petalR * 1.2, 0, 360);
        g.fill();
        // 花瓣（用 arc 拼出 6 片）
        g.setFill(Color.rgb(8, 18, 12, 0.95));
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3 - Math.PI / 2;
            double px = cx + Math.cos(ang) * petalR * 0.6;
            double py = cy - petalR * 0.8 + Math.sin(ang) * petalR * 0.3;
            g.save();
            g.translate(px, py);
            g.rotate(Math.toDegrees(ang));
            g.beginPath();
            g.arc(0, 0, petalR * 0.4, petalR * 0.7, 0, 360);
            g.fill();
            g.restore();
        }
    }

    /** 绘制八角金色边框。 */
    private static void drawGoldenOctagon(GraphicsContext g, double W, double H) {
        double padX = W * 0.05;
        double padY = H * 0.07;
        double topW = W - padX * 2;
        double botW = W - padX * 2;
        double topInset = topW * 0.04;
        double botInset = botW * 0.04;

        g.setStroke(Color.rgb(200, 165, 55, 0.85));
        g.setLineWidth(2.5);
        g.beginPath();
        // 顶边左斜 → 顶边 → 顶边右斜
        g.moveTo(padX + topInset, padY);
        g.lineTo(padX + topW - topInset, padY);
        // 右上角斜切
        g.lineTo(padX + topW, padY + topW * 0.02);
        g.lineTo(padX + topW, padY + topW * 0.08);
        // 右边
        g.lineTo(padX + topW, H - padY - botW * 0.08);
        g.lineTo(padX + topW, H - padY - botW * 0.02);
        // 右下角斜切
        g.lineTo(padX + topW - botInset, H - padY);
        g.lineTo(padX + botInset, H - padY);
        // 左下角斜切
        g.lineTo(padX, H - padY - botW * 0.02);
        g.lineTo(padX, H - padY - botW * 0.08);
        // 左边
        g.lineTo(padX, padY + topW * 0.08);
        g.lineTo(padX, padY + topW * 0.02);
        // 左上角斜切
        g.closePath();
        g.stroke();

        // 内边框（稍细稍暗）
        g.setStroke(Color.rgb(180, 145, 45, 0.55));
        g.setLineWidth(1.2);
        double padX2 = W * 0.058;
        double padY2 = H * 0.078;
        double inset2 = (W - padX2 * 2) * 0.038;
        g.beginPath();
        g.moveTo(padX2 + inset2, padY2);
        g.lineTo(padX2 + (W - padX2 * 2) - inset2, padY2);
        g.lineTo(padX2 + (W - padX2 * 2), padY2 + (W - padX2 * 2) * 0.018);
        g.lineTo(padX2 + (W - padX2 * 2), padY2 + (W - padX2 * 2) * 0.075);
        g.lineTo(padX2 + (W - padX2 * 2), H - padY2 - (W - padX2 * 2) * 0.075);
        g.lineTo(padX2 + (W - padX2 * 2), H - padY2 - (W - padX2 * 2) * 0.018);
        g.lineTo(padX2 + (W - padX2 * 2) - inset2, H - padY2);
        g.lineTo(padX2 + inset2, H - padY2);
        g.lineTo(padX2, H - padY2 - (W - padX2 * 2) * 0.018);
        g.lineTo(padX2, H - padY2 - (W - padX2 * 2) * 0.075);
        g.lineTo(padX2, padY2 + (W - padX2 * 2) * 0.075);
        g.lineTo(padX2, padY2 + (W - padX2 * 2) * 0.018);
        g.closePath();
        g.stroke();
    }
}
