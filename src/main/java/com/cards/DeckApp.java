package com.cards;

import com.cards.deck.Deck;
import com.cards.io.CardImageWriter;
import com.cards.io.PngCardImageWriter;
import com.cards.model.Card;
import com.cards.render.CardRenderer;
import com.cards.render.CanvasCardRenderer;
import com.cards.ui.CardCell;
import com.cards.ui.ParticleField;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 主程序：用 JavaFX 生成并展示整副扑克牌（54 张）。
 *
 * 功能：
 *  - 整副展示（洗牌 / 整理排序 / 全部翻开 / 全部盖牌）；
 *  - 蓝、红两种牌背切换；
 *  - 导出全部牌面 PNG；
 *  - 展示固定：鼠标悬停、点按都不会改变牌面或牌序，只有按“洗牌”才会打乱。
 */
public class DeckApp extends Application {

    private static final double CELL_WIDTH = 122;

    private final Map<Card, Image> faceCache = new HashMap<>();
    private final Map<Boolean, Image> backCache = new HashMap<>();
    private final CardRenderer renderer = new CanvasCardRenderer();
    private final CardImageWriter imageWriter = new PngCardImageWriter();

    private List<Card> order = new ArrayList<>(Deck.standard().cards());
    private boolean backRed = false;
    private final List<CardCell> cells = new ArrayList<>();

    private FlowPane grid;
    private Label status = new Label();
    /** 开始界面（主页），用于“返回主页”与“开始游戏”后续切换。 */
    private Scene homeScene;
    /** 游戏选择界面：点“开始游戏”进入，可选择湖南跑得快 / 骗子酒馆。 */
    private Scene gameChoiceScene;
    /** 模式选择界面：点任一游戏卡片进入，可选择本地人机 / 局域网联机。 */
    private Scene modeChoiceScene;
    /** 模式选择界面顶部的游戏名徽标（进入时更新为当前所选游戏）。 */
    private final Label modeBadge = new Label();
    /** 音量 / 亮度偏好（后续玩法版本会真正生效）。 */
    private double volumePref = 60;
    private double brightnessPref = 100;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        if (getParameters().getRaw().contains("--export")) {
            Path dir = Paths.get("output", "cards").toAbsolutePath();
            try {
                int n = exportTo(dir);
                System.out.println("已导出 " + n + " 张扑克牌图片 -> " + dir);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            Platform.exit();
            return;
        }

        grid = new FlowPane();
        Scene deckScene = buildDeckScene(stage);
        homeScene = buildMenuScene(stage, deckScene);
        gameChoiceScene = buildGameChoiceScene(stage);
        modeChoiceScene = buildModeChoiceScene(stage);
        stage.setTitle("扑克牌小游戏");
        stage.setScene(homeScene);
        // 按屏幕可用区域自适应窗口大小：高 DPI 缩放（如 150%/175%）下，
        // 写死的 1580x980 逻辑像素会被放大到超出物理屏幕，导致右侧/底部内容被屏幕边缘截断
        var visual = Screen.getPrimary().getVisualBounds();
        double winW = Math.min(1580, visual.getWidth() - 28);
        double winH = Math.min(980, visual.getHeight() - 28);
        stage.setWidth(winW);
        stage.setHeight(winH);
        stage.setX(visual.getMinX() + (visual.getWidth() - winW) / 2);
        stage.setY(visual.getMinY() + (visual.getHeight() - winH) / 2);
        stage.show();
    }

    // ============================================================= UI 构建

    /** 构建 54 张扑克牌展示页（保留作后续“游戏主界面”的页面基础）。 */
    private Scene buildDeckScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        // ---------------- 顶部工具栏 ----------------
        Label spade = glyph("♠", "glyph-ink");
        Label heart = glyph("♥", "glyph-red");
        Label club = glyph("♣", "glyph-ink");
        Label diamond = glyph("♦", "glyph-red");
        Label title = new Label("JavaFX 扑克牌生成器");
        title.getStyleClass().add("app-title");

        Button btnHome = new Button("← 返回主页");
        btnHome.getStyleClass().add("btn-home");
        btnHome.setOnAction(e -> {
            if (homeScene != null) {
                stage.setScene(homeScene);
            }
        });

        Button btnShuffle = new Button("洗牌");
        btnShuffle.setOnAction(e -> {
            order = Deck.standard().shuffled();
            rebuildGrid();
        });
        Button btnSort = new Button("整理排序");
        btnSort.setOnAction(e -> {
            order = new ArrayList<>(Deck.standard().cards());
            rebuildGrid();
        });
        Button btnAllUp = new Button("全部翻开");
        btnAllUp.setOnAction(e -> flipAll(true));
        Button btnAllDown = new Button("全部盖牌");
        btnAllDown.setOnAction(e -> flipAll(false));

        ToggleGroup backGroup = new ToggleGroup();
        ToggleButton blueBack = new ToggleButton("蓝色牌背");
        blueBack.setToggleGroup(backGroup);
        blueBack.setSelected(true);
        ToggleButton redBack = new ToggleButton("红色牌背");
        redBack.setToggleGroup(backGroup);
        backGroup.selectedToggleProperty().addListener((o, a, b) -> {
            backRed = b == redBack;
            for (CardCell cell : cells) {
                cell.applyState();
            }
        });

        Button btnExport = new Button("导出 PNG");
        btnExport.setOnAction(e -> chooseAndExport(stage));

        HBox titleBox = new HBox(2, spade, heart, club, diamond, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar bar = new ToolBar(btnHome, new Separator(), titleBox, new Separator(), btnShuffle, btnSort,
                new Separator(), btnAllUp, btnAllDown,
                new Separator(), blueBack, redBack, spacer, btnExport);
        bar.getStyleClass().add("app-toolbar");
        root.setTop(bar);

        // ---------------- 中央牌区 ----------------
        grid.setPadding(new Insets(16, 12, 18, 12));
        grid.setHgap(8);
        grid.setVgap(12);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPrefWrapLength(CELL_WIDTH * 13 - 6);
        grid.getStyleClass().add("cards-panel");

        StackPane content = new StackPane();
        content.getChildren().add(grid);

        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToHeight(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(scroller);

        // ---------------- 状态栏 ----------------
        status.getStyleClass().add("status-label");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6, 14, 6, 14));
        root.setBottom(status);
        refreshStatus();

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        return scene;
    }

    // ============================================================= 开始界面（主页）

    /** 构建开始界面（主页）：国风山水背景 + 书法主标题 + 权重化按钮 + 设置/规则浮层。 */
    private Scene buildMenuScene(Stage stage, Scene deckScene) {
        StackPane root = new StackPane();

        // ================= 背景：国风山水图 + 半透明墨绿遮罩 + 织物噪点 + 极淡水印 =================
        StackPane bgLayer = sceneBackgroundImage("/bg_menu.png");
        Region themeShade = (Region) bgLayer.getChildren().get(1);
        root.getChildren().add(bgLayer);

        root.getChildren().add(fabricTexture());

        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.10), Pos.TOP_LEFT);
        addCornerSuit(root, "♥", Color.rgb(255, 140, 130, 0.10), Pos.TOP_RIGHT);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.10), Pos.BOTTOM_LEFT);
        addCornerSuit(root, "♦", Color.rgb(255, 140, 130, 0.10), Pos.BOTTOM_RIGHT);

        // 左右两侧半透明蓝色牌背剪影（纯装饰，鼠标穿透）
        Image backImg = backCache.computeIfAbsent(false, renderer::back);
        addCardDeco(root, backImg, 300, -14, Pos.CENTER_LEFT, new Insets(0, 0, 70, 44));
        addCardDeco(root, backImg, 300, 14, Pos.CENTER_RIGHT, new Insets(0, 44, 70, 0));

        // ================= 规则弹窗浮层（先建好供“游戏规则”按钮引用） =================
        StackPane rulesOverlay = new StackPane();
        rulesOverlay.setVisible(false);
        Region rShade = new Region();
        rShade.getStyleClass().add("modal-shade");
        rShade.setOnMouseClicked(ev -> rulesOverlay.setVisible(false));
        VBox rulesCard = buildRulesModalCard(rulesOverlay);
        StackPane.setAlignment(rulesCard, Pos.CENTER);
        StackPane.setMargin(rulesCard, new Insets(40));
        rulesOverlay.getChildren().addAll(rShade, rulesCard);

        // ================= 中央内容：花色条 → 主标题 → 三按钮 =================
        HBox suitStrip = new HBox(26);
        suitStrip.setAlignment(Pos.CENTER);
        addGradientSuit(suitStrip, "♠", Color.web("#f7e6b0"), Color.web("#b8942a"));
        addGradientSuit(suitStrip, "♥", Color.web("#ffc1ae"), Color.web("#c62b2b"));
        addGradientSuit(suitStrip, "♣", Color.web("#f7e6b0"), Color.web("#b8942a"));
        addGradientSuit(suitStrip, "♦", Color.web("#ffc1ae"), Color.web("#c62b2b"));
        suitStrip.setMouseTransparent(true);

        Text title = new Text("扑克牌小游戏");
        title.getStyleClass().add("menu-title");
        title.setMouseTransparent(true);

        Button start = new Button("开始游戏");
        start.getStyleClass().addAll("menu-btn", "menu-btn-start");
        start.setTooltip(new Tooltip("挑选一款游戏开始对局"));
        start.setOnAction(e -> {
            if (gameChoiceScene != null) {
                stage.setScene(gameChoiceScene);
            }
        });

        Button rules = new Button("游戏规则");
        rules.getStyleClass().addAll("menu-btn", "menu-btn-rules");
        rules.setTooltip(new Tooltip("查看玩法与规则说明"));
        rules.setOnAction(e -> rulesOverlay.setVisible(true));

        Button exit = new Button("退出游戏");
        exit.getStyleClass().addAll("menu-btn", "menu-btn-quit");
        exit.setTooltip(new Tooltip("退出扑克牌小游戏"));
        exit.setOnAction(e -> confirmExit(stage));

        VBox buttons = new VBox(32, start, rules, exit);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(34, suitStrip, title, buttons);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);
        StackPane.setMargin(center, new Insets(0, 0, 70, 0));
        root.getChildren().add(center);

        // 右下角技术备注（原副标题降噪后移到底角，不参与主流程）
        Label tech = new Label("基于 JavaFX 的桌面棋牌小游戏 · 课程项目");
        tech.getStyleClass().add("tech-note");
        tech.setMouseTransparent(true);
        StackPane.setAlignment(tech, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(tech, new Insets(0, 36, 18, 0));
        root.getChildren().add(tech);

        // ================= 右上角：设置齿轮 =================
        javafx.scene.shape.Path gear = gearShape();
        gear.getStyleClass().add("gear-icon");
        gear.setMouseTransparent(true);

        Button gearBtn = new Button();
        gearBtn.setGraphic(gear);
        gearBtn.getStyleClass().add("settings-btn");
        gearBtn.setTooltip(new Tooltip("设置"));
        gearBtn.setPickOnBounds(true);
        gear.fillProperty().bind(Bindings.when(gearBtn.hoverProperty())
                .then(Color.web("#ffd54f"))
                .otherwise(Color.web("#f2f6ee")));
        StackPane.setAlignment(gearBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(gearBtn, new Insets(20, 26, 0, 0));
        root.getChildren().add(gearBtn);

        // ================= 设置浮层（主题 / 音量 / 亮度 / 开发者预览） =================
        StackPane overlay = new StackPane();
        overlay.setVisible(false);
        Region shade = new Region();
        shade.getStyleClass().add("modal-shade");
        shade.setOnMouseClicked(ev -> overlay.setVisible(false));

        // 亮度调节层：铺满但不挡鼠标，透明度由设置里的“亮度”控制
        Region dim = new Region();
        dim.getStyleClass().add("brightness-shade");
        dim.setOpacity(0.0);
        dim.setMouseTransparent(true);

        HBox holder = new HBox();
        holder.setAlignment(Pos.TOP_RIGHT);
        holder.setPadding(new Insets(84, 26, 26, 26));
        holder.getChildren().add(buildSettingsPanel(themeShade, deckScene, stage, overlay, dim));

        overlay.getChildren().addAll(shade, holder);
        gearBtn.setOnAction(e -> overlay.setVisible(!overlay.isVisible()));

        root.getChildren().addAll(overlay, dim, rulesOverlay);

        // ================= 入场动画：首次显示与每次回到主页时依次淡入 =================
        final javafx.scene.Node[] intro = {suitStrip, title, start, rules, exit};
        stage.showingProperty().addListener((o, a, now) -> {
            if (now) {
                playMenuIntro(intro);
            }
        });
        stage.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == homeScene && stage.isShowing()) {
                playMenuIntro(intro);
            }
        });

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        return scene;
    }

    // ============================================================= 主页辅助组件

    /** 构建全屏背景层：Canvas 绘制国风山水 + 半透明墨绿遮罩。 */
    private static StackPane sceneBackgroundImage(String resource) {
        Canvas canvas = new Canvas();
        Region shade = new Region();
        shade.getStyleClass().add("bg-theme-shade");
        shade.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane holder = new StackPane(canvas, shade);
        holder.getStyleClass().add("bg-theme-root");
        holder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Canvas 尺寸跟随容器
        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());

        // 尺寸变化时重绘
        holder.widthProperty().addListener(o -> drawLandscape(canvas));
        holder.heightProperty().addListener(o -> drawLandscape(canvas));
        canvas.sceneProperty().addListener((o, prev, next) -> {
            if (next != null) next.addPostLayoutPulseListener(() -> drawLandscape(canvas));
        });

        holder.setMouseTransparent(true);
        return holder;
    }

    /** 在 Canvas 上绘制国风墨绿山水背景。 */
    private static void drawLandscape(Canvas canvas) {
        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W < 10 || H < 10) return;

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);

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
        Random rnd = new Random((long)(seed * 1000));
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

    /** 生成一张低透明织物噪点纹理 Region，叠加在背景之上增加绒布质感。 */
    private static Region fabricTexture() {
        int size = 72;
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        Random rnd = new Random(20240908L); // 固定种子，保证每次启动纹理一致
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double v = rnd.nextDouble();
                Color c;
                if (v < 0.14) {
                    c = Color.rgb(255, 255, 255, 0.08 + rnd.nextDouble() * 0.04); // 亮噪点 8%-12%
                } else if (v < 0.22) {
                    c = Color.rgb(20, 12, 4, 0.08 + rnd.nextDouble() * 0.04);    // 暗噪点 8%-12%
                } else {
                    c = Color.TRANSPARENT;
                }
                pw.setColor(x, y, c);
            }
        }
        ImagePattern pattern = new ImagePattern(img, 0, 0, size, size, false);
        Region region = new Region();
        region.setBackground(new Background(new BackgroundFill(pattern, CornerRadii.EMPTY, Insets.EMPTY)));
        region.setMouseTransparent(true);
        region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return region;
    }

    /** 在 StackPane 角落放一张半透明旋转的牌背装饰。 */
    private static void addCardDeco(StackPane root, Image img, double height,
                                    double rotate, Pos pos, Insets margin) {
        ImageView iv = new ImageView(img);
        iv.setFitHeight(height);
        iv.setPreserveRatio(true);
        iv.setRotate(rotate);
        iv.setOpacity(0.10);
        iv.setMouseTransparent(true);
        iv.setSmooth(true);
        StackPane.setAlignment(iv, pos);
        StackPane.setMargin(iv, margin);
        root.getChildren().add(iv);
    }

    /** 向容器追加一个带柔和纵向渐变的扑克花色字符。 */
    private static void addGradientSuit(HBox strip, String glyph, Color top, Color bottom) {
        Text t = new Text(glyph);
        t.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 54));
        t.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        t.setMouseTransparent(true);
        strip.getChildren().add(t);
    }

    /** 花色图标轻微上下浮动，错峰延迟避免整齐划一。 */
    private static void playSuitFloat(HBox strip) {
        double delay = 0;
        for (javafx.scene.Node n : strip.getChildren()) {
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(n.translateYProperty(), 0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(1800), new KeyValue(n.translateYProperty(), -6, Interpolator.EASE_BOTH)));
            tl.setDelay(Duration.millis(delay));
            tl.setAutoReverse(true);
            tl.setCycleCount(Timeline.INDEFINITE);
            tl.play();
            delay += 320;
        }
    }

    /** 菜单内容依次淡入：标题、按钮错峰出现。 */
    private static void playMenuIntro(javafx.scene.Node... nodes) {
        double delay = 0;
        for (javafx.scene.Node n : nodes) {
            n.setOpacity(0.0);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(n.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(420), new KeyValue(n.opacityProperty(), 1.0, Interpolator.EASE_OUT)));
            tl.setDelay(Duration.millis(delay));
            tl.play();
            delay += 130;
        }
    }

    /** 构建主页“游戏规则”弹窗卡片：页签切换跑得快 / 骗子酒馆，正文可滚动。 */
    private static VBox buildRulesModalCard(StackPane overlay) {
        VBox card = new VBox(12);
        card.getStyleClass().add("rules-modal-card");
        card.setPrefWidth(980);
        card.setMaxWidth(1040);
        card.setMaxHeight(920);

        Label head = new Label("游戏规则");
        head.getStyleClass().add("rules-modal-title");
        Button close = new Button("×");
        close.getStyleClass().add("icon-close");
        close.setTooltip(new Tooltip("关闭"));
        close.setOnAction(e -> overlay.setVisible(false));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox topRow = new HBox(head, gap, close);
        topRow.setAlignment(Pos.CENTER_LEFT);

        ToggleButton runTab = new ToggleButton("湖南跑得快");
        ToggleButton liarTab = new ToggleButton("骗子酒馆");
        runTab.getStyleClass().add("rules-tab");
        liarTab.getStyleClass().add("rules-tab");
        ToggleGroup tabs = new ToggleGroup();
        runTab.setToggleGroup(tabs);
        liarTab.setToggleGroup(tabs);
        runTab.setSelected(true);

        Label tabHint = new Label("点击弹窗外部或 × 关闭");
        tabHint.getStyleClass().add("settings-note");
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        HBox tabRow = new HBox(10, runTab, liarTab, gap2, tabHint);
        tabRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroller = new ScrollPane();
        scroller.getStyleClass().add("rules-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setContent(renderRulesBody(RUN_RULES_MD));
        VBox.setVgrow(scroller, Priority.ALWAYS);

        tabs.selectedToggleProperty().addListener((o, oldT, newT) ->
                scroller.setContent(renderRulesBody(newT == liarTab ? LIAR_RULES_MD : RUN_RULES_MD)));

        card.getChildren().addAll(topRow, tabRow, scroller);
        return card;
    }

    /** 退出前二次确认，防止误触。 */
    private static void confirmExit(Stage stage) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(stage);
        a.setTitle("退出确认");
        a.setHeaderText("确定要退出游戏吗？");
        a.setContentText("离开牌桌后，本局进度不会被保存。");
        ButtonType ok = new ButtonType("确定退出");
        ButtonType cancel = new ButtonType("再玩一会");
        a.getButtonTypes().setAll(ok, cancel);
        a.showAndWait().filter(b -> b == ok).ifPresent(b -> Platform.exit());
    }

    // ============================================================= 游戏选择界面

    /** 构建“选择游戏”页：提供湖南跑得快 / 骗子酒馆两种玩法入口。 */
    private Scene buildGameChoiceScene(Stage stage) {
        StackPane root = new StackPane();

        root.getChildren().add(sceneBackgroundImage("/bg_menu.png"));

        // 低强度动态粒子：星光缓慢浮动 + 纸牌碎片轻轻飘落（鼠标穿透，离开本页自动暂停）
        ParticleField particles = new ParticleField();
        root.getChildren().add(particles);

        // 四角花色改成淡淡的暗纹底（低透明度 + 微模糊），营造氛围但不抢焦点
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        Label title = new Label("选择游戏");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("请选择要游玩的模式");
        sub.getStyleClass().add("game-choice-sub");

        Button runFast = gameOption("♠", "湖南跑得快",
                "三人 16 张经典玩法\n先出完手牌者获胜",
                "扑克 · 竞速出牌", true, () -> enterModeChoice(stage, "♠ 湖南跑得快"));
        Button liarBar = gameOption("🃏", "骗子酒馆",
                "扑克与骰子模式\n谎言与质疑并存，活到最后即胜",
                "聚会 · 心理博弈", false, () -> enterModeChoice(stage, "🃏 骗子酒馆"));

        HBox options = new HBox(28, runFast, liarBar);
        options.setAlignment(Pos.CENTER);
        options.setPadding(new Insets(0, 48, 0, 48));
        // 两张卡片平分可用宽度（宽度上下限由 CSS min/pref/max-width 约束），窗口缩小时自动收窄不溢出
        HBox.setHgrow(runFast, Priority.ALWAYS);
        HBox.setHgrow(liarBar, Priority.ALWAYS);

        Button back = new Button("← 返回主页");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (homeScene != null) {
                stage.setScene(homeScene);
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        // 右上角玩家头像 + 昵称卡片：与左上角“返回”按钮对称呼应页内金属圆徽风格
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-avatar");
        Text avatarGlyph = new Text("♛");
        avatarGlyph.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 24));
        avatarGlyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff3c4")),
                new Stop(0.55, Color.web("#e8c25e")),
                new Stop(1, Color.web("#b07f1e"))));
        avatar.getChildren().add(avatarGlyph);

        Label nick = new Label("玩家");
        nick.getStyleClass().add("user-nick");
        Label stat = new Label("在线 · 准备开局");
        stat.getStyleClass().add("user-stat");
        VBox textBox = new VBox(2, nick, stat);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox userCard = new HBox(12, avatar, textBox);
        userCard.getStyleClass().add("user-card");
        userCard.setAlignment(Pos.CENTER_LEFT);
        // 让 HBox 按内容计算尺寸，否则默认会撑满 StackPane，
        // .user-card 的半透明深绿背景就会铺满整个屏幕，造成“亮度下降”
        userCard.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(userCard, Pos.TOP_RIGHT);
        StackPane.setMargin(userCard, new Insets(22, 24, 0, 0));

        VBox center = new VBox(34, title, sub, options);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        root.getChildren().addAll(center, back, userCard);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源
        stage.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS == scene) {
                particles.play();
            } else if (oldS == scene) {
                particles.stop();
            }
        });
        return scene;
    }

    /** 生成一个游戏入口卡片按钮：金属圆徽图标 + 名称 + 简介 + 圆角标签；featured 为推荐选中态。 */
    private Button gameOption(String icon, String name, String desc, String tag,
                              boolean featured, Runnable onPlay) {
        // 图标：深色金属圆徽 + 内高光描边；花色字符用金色渐变，emoji 保留本色加暖光
        StackPane badge = new StackPane();
        badge.getStyleClass().add("game-icon-badge");
        Text glyph = new Text(icon);
        glyph.getStyleClass().add("game-icon-glyph");
        boolean suitGlyph = icon.length() == 1 && "♠♥♣♦".indexOf(icon) >= 0;
        if (suitGlyph) {
            glyph.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 48));
            glyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fff3c4")),
                    new Stop(0.55, Color.web("#e8c25e")),
                    new Stop(1, Color.web("#b07f1e"))));
        } else {
            glyph.setFont(Font.font("Segoe UI Emoji", 46));
            glyph.setEffect(new DropShadow(8, Color.rgb(255, 210, 90, 0.35)));
        }
        badge.getChildren().add(glyph);

        Label nameL = new Label(name);
        nameL.getStyleClass().add("game-option-title");
        Label descL = new Label(desc);
        descL.getStyleClass().add("game-option-desc");
        descL.setTextAlignment(TextAlignment.CENTER);
        Label tagL = new Label(tag);
        tagL.getStyleClass().add("game-option-tag");

        VBox box = new VBox(14, badge, nameL, descL, tagL);
        box.setAlignment(Pos.CENTER);
        // 内边距与 CSS 卡片高度（min 296）匹配：内容总高 ≈ 283，小于卡片内容区 292，
        // 保证圆徽/标题/简介/标签都完整落在圆角卡片内，不贴边、不溢出
        box.setPadding(new Insets(26, 34, 26, 34));

        // 细微磨砂噪点叠层，给卡片增加牌面磨砂质感
        Region frost = new Region();
        frost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        frost.setMouseTransparent(true);
        frost.setBackground(new Background(new BackgroundFill(
                frostPattern(), new CornerRadii(20), new Insets(2))));

        StackPane content = new StackPane(frost, box);

        Button card = new Button();
        card.getStyleClass().add("game-option");
        card.setGraphic(content);
        card.setOnAction(e -> onPlay.run());
        styleGameCard(card, featured);
        return card;
    }

    /**
     * 配置游戏入口卡片的底色 / 边框 / 阴影与悬浮动效。
     * 两张卡片常态完全一致（浅灰边框、压暗、投影低调）；
     * 仅当鼠标悬浮时，被选中的卡片才缓慢过渡为金色高亮（金边、泛金底色、暖光、轻微上浮）。
     */
    private static void styleGameCard(Button card, boolean featured) {
        CornerRadii radii = new CornerRadii(22);
        javafx.scene.layout.BorderWidths strokeWidth =
                new javafx.scene.layout.BorderWidths(2);

        // 常态：低调深色（两张卡片一致）
        Color topIdle = Color.rgb(255, 255, 255, 0.06);
        Color botIdle = Color.rgb(255, 255, 255, 0.025);
        Color borderIdle = Color.rgb(225, 230, 236, 0.30);
        Color glowIdle = Color.rgb(0, 0, 0, 0.35);
        // 悬浮：金色高亮
        Color topHover = Color.rgb(255, 246, 205, 0.30);
        Color botHover = Color.rgb(255, 213, 79, 0.17);
        Color borderHover = Color.rgb(255, 230, 160, 0.98);
        Color glowHover = Color.rgb(255, 206, 90, 0.60);

        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(6);
        shadow.setRadius(18);
        shadow.setColor(glowIdle);
        card.setEffect(shadow);
        card.setBackground(cardBackground(topIdle, botIdle, radii));
        card.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(borderIdle, javafx.scene.layout.BorderStrokeStyle.SOLID, radii, strokeWidth)));

        // 用 0→1 的进度量驱动颜色插值，实现“缓慢变亮 / 上浮”的平滑 hover 动效
        javafx.beans.property.DoubleProperty frac = new javafx.beans.property.SimpleDoubleProperty(0);
        frac.addListener((o, oldV, v) -> {
            double t = v.doubleValue();
            Color top = (Color) Interpolator.EASE_BOTH.interpolate(topIdle, topHover, t);
            Color bot = (Color) Interpolator.EASE_BOTH.interpolate(botIdle, botHover, t);
            Color border = (Color) Interpolator.EASE_BOTH.interpolate(borderIdle, borderHover, t);
            Color glow = (Color) Interpolator.EASE_BOTH.interpolate(glowIdle, glowHover, t);
            card.setBackground(cardBackground(top, bot, radii));
            card.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(border, javafx.scene.layout.BorderStrokeStyle.SOLID, radii, strokeWidth)));
            shadow.setColor(glow);
            shadow.setRadius(18 + t * 14);
            card.setTranslateY(-7 * t);
        });

        Timeline hoverIn = new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(frac, 1.0, Interpolator.EASE_OUT)));
        Timeline hoverOut = new Timeline(new KeyFrame(Duration.millis(320),
                new KeyValue(frac, 0.0, Interpolator.EASE_IN)));
        card.hoverProperty().addListener((o, wasHover, nowHover) -> {
            if (nowHover) {
                hoverOut.stop();
                hoverIn.playFromStart();
            } else {
                hoverIn.stop();
                hoverOut.playFromStart();
            }
        });
    }

    /** 按上下两色生成卡片纵向渐变背景。 */
    private static Background cardBackground(Color top, Color bottom, CornerRadii radii) {
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
        return new Background(new BackgroundFill(grad, radii, Insets.EMPTY));
    }

    /** 卡片磨砂质感的细密噪点纹理（懒加载，全局复用）。 */
    private static ImagePattern FROST_PATTERN;

    private static ImagePattern frostPattern() {
        if (FROST_PATTERN != null) {
            return FROST_PATTERN;
        }
        int size = 64;
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        Random rnd = new Random(777L);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double v = rnd.nextDouble();
                Color c;
                if (v < 0.10) {
                    c = Color.rgb(255, 255, 255, 0.05 + rnd.nextDouble() * 0.06);
                } else if (v < 0.16) {
                    c = Color.rgb(20, 24, 20, 0.05 + rnd.nextDouble() * 0.05);
                } else {
                    c = Color.TRANSPARENT;
                }
                pw.setColor(x, y, c);
            }
        }
        FROST_PATTERN = new ImagePattern(img, 0, 0, size, size, false);
        return FROST_PATTERN;
    }

    // ============================================================= 玩法模式选择界面

    /** 从“选择游戏”进入“玩法模式”页，并把顶部徽标更新为所选游戏。 */
    private void enterModeChoice(Stage stage, String game) {
        modeBadge.setText(game);
        if (modeChoiceScene != null) {
            stage.setScene(modeChoiceScene);
        }
    }

    /** 构建“玩法模式”页：针对所选游戏提供本地人机 / 局域网联机两种模式。 */
    private Scene buildModeChoiceScene(Stage stage) {
        StackPane root = new StackPane();

        root.getChildren().add(sceneBackgroundImage("/bg_menu.png"));

        // 低强度动态粒子：与“选择游戏”页一致，星光缓慢浮动 + 纸牌碎片轻轻飘落（鼠标穿透，离开本页自动暂停）
        ParticleField particles = new ParticleField();
        root.getChildren().add(particles);

        // 四角花色改成淡淡的暗纹底（低透明度 + 微模糊），与“选择游戏”页保持一致
        addCornerSuit(root, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        addCornerSuit(root, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        addCornerSuit(root, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        addCornerSuit(root, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        // 顶部徽标：进入时由 enterModeChoice 更新为当前游戏名
        modeBadge.getStyleClass().add("game-choice-title");
        modeBadge.setMinHeight(76);
        Label sub = new Label("请选择玩法模式");
        sub.getStyleClass().add("game-choice-sub");

        Button vsCpu = gameOption("🤖", "本地人机",
                "与电脑 AI 同台对战\n无需联网，随时开局",
                "单人 · 离线", false, () -> showInfo(stage,
                        "本地人机模式正在开发中，敬请期待！"));
        Button lanPlay = gameOption("🌐", "局域网联机",
                "创建或加入局域网房间\n与身边好友同场竞技",
                "多人 · 联机", false, () -> showInfo(stage,
                        "局域网联机模式正在开发中，敬请期待！"));

        HBox options = new HBox(28, vsCpu, lanPlay);
        options.setAlignment(Pos.CENTER);
        options.setPadding(new Insets(0, 48, 0, 48));
        HBox.setHgrow(vsCpu, Priority.ALWAYS);
        HBox.setHgrow(lanPlay, Priority.ALWAYS);

        Button back = new Button("← 返回选择游戏");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (gameChoiceScene != null) {
                stage.setScene(gameChoiceScene);
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        VBox center = new VBox(30, modeBadge, sub, options);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        root.getChildren().addAll(center, back);

        Scene scene = new Scene(root);
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源
        stage.sceneProperty().addListener((o, oldS, newS) -> {
            if (newS == scene) {
                particles.play();
            } else if (oldS == scene) {
                particles.stop();
            }
        });
        return scene;
    }

    // ============================================================= 规则界面

    /** 湖南跑得快规则文案（轻量 Markdown：# 大节、## 小节、> 引用、- 列表、数字列表、--- 分隔线）。 */
    private static final String RUN_RULES_MD = """
            # 湖南跑得快（16 张经典，3 人局）

            > 注意：棋牌仅娱乐，禁止赌博。

            ## 基础配置

            - 人数：3 人，各自为战
            - 牌：一副去掉大小王、3 张 2、1 张 A，共 48 张，每人 **16 张**
            - 牌大小：**2＞A＞K＞Q＞J＞10＞9＞8＞7＞6＞5＞4＞3**，不比花色
            - 首局：拿到**黑桃 3** 的玩家必须先出黑桃 3；上局赢家下局先手

            ## 可用牌型

            1. 单张：一张牌
            2. 对子：两张点数一样
            3. 连对：≥ 2 对连续对子，例：3344、556677
            4. 顺子：≥ 5 张连续单牌，**不能包含 2、A**，34567 最小
            5. 三张：三张同点；三带二（三张 + 任意对子）
            6. 飞机：两组及以上连续三张，可以带对子
            7. 炸弹：四张同点，最大牌型，可以炸一切；炸弹之间比点数大小

            ## 核心规则

            1. **有大必出（必压）**：上家出牌，你手里有能大过上家的牌，就必须打出来，不能过。有牌不出叫 “放走”，要包赔。
            2. 一轮全部要不起，出牌人继续出牌。
            3. **报单**：手里只剩 1 张牌，必须口头报单提醒其他人。
            4. 胜负：**最先出完手牌为头家获胜**；剩下两家按手里剩余牌数算分，剩几张算几分。剩 1 张保本不扣分。

            ## 放走包赔

            > 如果你手里有大牌可以压住上家，却选择要不起，直接放走对手跑完全部手牌，那么由你一个人承担另外两家的全部输分。
            """;

    /** 骗子酒馆规则文案。 */
    private static final String LIAR_RULES_MD = """
            # 骗子酒馆（Liar's Bar，骗子酒吧）

            > 聚会桌游，分**扑克模式、骰子模式**，最多 4 人，各自为战。

            ## 🃏 扑克模式（主流）

            1. 牌库：20 张，6Q、6K、6A、2 张 Joker（万能牌）。每小局随机选 Q/K/A 其中一个作为**主牌**，Joker 直接当作主牌使用。每人发 5 张手牌。
            2. 顺时针轮流出牌，每回合打出 **1-3 张牌，牌背朝下**，口头报出打出几张主牌，可以撒谎。
            3. 下家二选一：
               - **跟牌**：相信，轮到你，你继续打 1-3 张宣称主牌；
               - **质疑（开他）**：翻开上家打出的牌。
            4. 质疑判定：
               - ✘ 上家撒谎（不全是主牌）：**上家受罚俄罗斯轮盘**，左轮 6 个弹槽只有 1 颗子弹，中子弹直接出局。
               - ✔ 上家没撒谎：**质疑的人受罚轮盘**。
            5. 有人轮盘出局后，小局结束，重新发牌、重选主牌继续。活到最后一人胜利。
            """;

    /** 把一段规则 Markdown 渲染成规则正文容器（供主页规则弹窗使用）。 */
    private static VBox renderRulesBody(String md) {
        VBox body = new VBox(10);
        body.getStyleClass().add("rules-body");
        for (String raw : md.split("\n")) {
            javafx.scene.Node node = renderRulesLine(raw);
            if (node != null) {
                body.getChildren().add(node);
            }
        }
        return body;
    }

    /** 把一行轻量 Markdown 渲染成结点；空行返回 null。 */
    private static javafx.scene.Node renderRulesLine(String raw) {
        if (raw == null) {
            return null;
        }
        String line = raw.trim();
        if (line.isEmpty()) {
            return null;
        }
        if (line.startsWith("# ")) {
            return rulesHeading(line.substring(2), "rules-h1");
        }
        if (line.startsWith("## ")) {
            return rulesHeading(line.substring(3), "rules-h2");
        }
        if (line.matches("-{3,}")) {
            return new Separator();
        }
        if (line.startsWith("> ")) {
            return rulesFlow(line.substring(2), true);
        }
        // “- / * ”列表前缀统一转圆点；数字列表与普通行原样保留
        String text = line.matches("(?i)(-|\\*)\\s+.*")
                ? "•  " + line.replaceFirst("(?i)(-|\\*)\\s+", "")
                : line;
        return rulesFlow(text, false);
    }

    private static Label rulesHeading(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    /** 把正文解析成 TextFlow：支持 **加粗**，且逐字符生成以保证中文任意位置可换行。 */
    private static TextFlow rulesFlow(String text, boolean quote) {
        TextFlow flow = new TextFlow();
        String[] parts = text.split("\\*\\*");
        for (int i = 0; i < parts.length; i++) {
            boolean bold = i % 2 == 1;
            parts[i].codePoints().forEach(cp -> {
                Text run = new Text(new String(Character.toChars(cp)));
                run.getStyleClass().add("rules-run");
                if (bold) {
                    run.getStyleClass().add("rules-strong");
                }
                if (quote) {
                    run.getStyleClass().add("rules-quote");
                }
                flow.getChildren().add(run);
            });
        }
        return flow;
    }

    private void addCornerSuit(StackPane root, String g, Color color, Pos corner) {
        addCornerSuit(root, g, color, corner, false);
    }

    /** watermark=true 时花色额外加微模糊，作为淡淡的暗纹底使用。 */
    private void addCornerSuit(StackPane root, String g, Color color, Pos corner, boolean watermark) {
        Label l = new Label(g);
        l.setTextFill(color);
        l.setFont(Font.font("Segoe UI Symbol", 150));
        l.getStyleClass().add("menu-corner");
        l.setMouseTransparent(true);
        if (watermark) {
            l.setEffect(new BoxBlur(4, 4, 3));
        }
        StackPane.setAlignment(l, corner);
        Insets m = switch (corner) {
            case TOP_LEFT -> new Insets(8, 0, 0, 26);
            case TOP_RIGHT -> new Insets(8, 26, 0, 0);
            case BOTTOM_LEFT -> new Insets(0, 0, 10, 26);
            default -> new Insets(0, 26, 10, 0);
        };
        StackPane.setMargin(l, m);
        root.getChildren().add(l);
    }

    /** 设置浮层面板：界面主题 + 音量/亮度 + 开发者预览。 */
    private VBox buildSettingsPanel(Region bg, Scene deckScene, Stage stage,
                                    StackPane overlay, Region dim) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("settings-panel");
        panel.setPrefWidth(340);

        Label head = new Label("设置");
        head.getStyleClass().add("settings-title");
        Button close = new Button("×");
        close.getStyleClass().add("icon-close");
        close.setOnAction(e -> overlay.setVisible(false));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(head, spacer, close);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label sec1 = new Label("界面主题");
        sec1.getStyleClass().add("settings-section");
        ToggleButton felt = new ToggleButton("经典绿桌");
        ToggleButton night = new ToggleButton("星夜蓝");
        felt.getStyleClass().add("seg-toggle");
        night.getStyleClass().add("seg-toggle");
        ToggleGroup themeGroup = new ToggleGroup();
        felt.setToggleGroup(themeGroup);
        night.setToggleGroup(themeGroup);
        felt.setSelected(true);
        themeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == night) {
                bg.setStyle("-fx-background-color: rgba(10, 20, 40, 0.68);");
            } else {
                bg.setStyle("-fx-background-color: rgba(8, 22, 14, 0.58);");
            }
        });
        HBox segRow = new HBox(10, felt, night);
        Label note1 = new Label("切换开始界面背景配色。");
        note1.getStyleClass().add("settings-note");

        // ---- 声音与显示：音量 / 亮度 ----
        Label sec2 = new Label("声音与显示");
        sec2.getStyleClass().add("settings-section");

        Label volLbl = new Label("音量");
        volLbl.getStyleClass().add("settings-row-label");
        Slider vol = new Slider(0, 100, volumePref);
        vol.getStyleClass().add("settings-slider");
        HBox.setHgrow(vol, Priority.ALWAYS);
        vol.setMaxWidth(Double.MAX_VALUE);
        Label volVal = new Label(valueText(volumePref));
        volVal.getStyleClass().add("value-chip");
        vol.valueProperty().addListener((o, a, b) -> {
            volumePref = b.doubleValue();
            volVal.setText(valueText(b.doubleValue()));
        });
        HBox volRow = new HBox(12, volLbl, vol, volVal);
        volRow.setAlignment(Pos.CENTER_LEFT);
        Label volNote = new Label("声音效果将在游戏玩法版本接入。");
        volNote.getStyleClass().add("settings-note");

        Label briLbl = new Label("亮度");
        briLbl.getStyleClass().add("settings-row-label");
        Slider bri = new Slider(20, 100, brightnessPref);
        bri.getStyleClass().add("settings-slider");
        HBox.setHgrow(bri, Priority.ALWAYS);
        bri.setMaxWidth(Double.MAX_VALUE);
        Label briVal = new Label(valueText(brightnessPref));
        briVal.getStyleClass().add("value-chip");
        bri.valueProperty().addListener((o, a, b) -> {
            brightnessPref = b.doubleValue();
            briVal.setText(valueText(b.doubleValue()));
            dim.setOpacity((100 - b.doubleValue()) / 100.0 * 0.85);
        });
        HBox briRow = new HBox(12, briLbl, bri, briVal);
        briRow.setAlignment(Pos.CENTER_LEFT);
        Label briNote = new Label("调低亮度时开始界面会整体变暗（后续全局生效）。");
        briNote.getStyleClass().add("settings-note");

        // ---- 开发者预览 ----
        Label sec3 = new Label("开发者预览");
        sec3.getStyleClass().add("settings-section");
        Button preview = new Button("查看 54 张扑克牌展示页");
        preview.getStyleClass().add("settings-link-btn");
        preview.setMaxWidth(Double.MAX_VALUE);
        preview.setOnAction(e -> {
            overlay.setVisible(false);
            stage.setScene(deckScene);
        });
        Label note3 = new Label("该页面是“游戏主界面”的素材基础，之后会接进“开始游戏”。");
        note3.getStyleClass().add("settings-note");

        Label foot = new Label("扑克牌小游戏 v0.2 · 开发中");
        foot.getStyleClass().add("settings-foot");

        panel.getChildren().addAll(topRow, new Separator(), sec1, segRow, note1,
                new Separator(), sec2, volRow, volNote, briRow, briNote,
                new Separator(), sec3, preview, note3, new Separator(), foot);
        return panel;
    }

    private static String valueText(double v) {
        return "%.0f%%".formatted(v);
    }

    private static void showInfo(Stage owner, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(owner);
        a.setTitle("提示");
        a.setHeaderText(null);
        a.setContentText(text);
        a.showAndWait();
    }

    /** 画一枚镂空齿轮（八齿）。 */
    private static javafx.scene.shape.Path gearShape() {
        javafx.scene.shape.Path p = new javafx.scene.shape.Path();
        p.setFillRule(FillRule.EVEN_ODD);
        p.setSmooth(true);
        double outer = 15.5;
        double rootR = outer * 0.80;
        double hole = outer * 0.40;
        ring(p, outer, rootR, 16);          // 齿圈
        ring(p, hole, hole, 40);            // 中心镂空
        return p;
    }

    /** 向路径追加一段点圈，偶数下标在外径、奇数在内径，形成锯齿。 */
    private static void ring(javafx.scene.shape.Path p, double rOuter, double rInner, int sides) {
        double step = Math.PI * 2 / sides;
        for (int i = 0; i < sides; i++) {
            double a = -Math.PI / 2 + i * step;
            double r = (i % 2 == 0) ? rOuter : rInner;
            double x = Math.cos(a) * r;
            double y = Math.sin(a) * r;
            if (i == 0) {
                p.getElements().add(new MoveTo(x, y));
            } else {
                p.getElements().add(new LineTo(x, y));
            }
        }
        p.getElements().add(new ClosePath());
    }

    private static Label glyph(String g, String colorClass) {
        Label l = new Label(g);
        l.getStyleClass().addAll("suit-glyph", colorClass);
        return l;
    }

    private void rebuildGrid() {
        cells.clear();
        grid.getChildren().clear();
        for (Card card : order) {
            CardCell cell = new CardCell(card, this::imageFor);
            cells.add(cell);
            grid.getChildren().add(cell);
        }
        refreshStatus();
    }

    private void flipAll(boolean up) {
        for (CardCell cell : cells) {
            cell.setFaceUp(up);
        }
    }

    // ============================================================= 状态栏

    private void refreshStatus() {
        status.setText("共 " + order.size() + " 张牌 · " + (backRed ? "红色牌背" : "蓝色牌背")
                + " · 展示固定：鼠标悬停/点按不改变牌面，仅按“洗牌”会打乱牌序，按“整理排序”恢复。");
    }

    // ============================================================= 图像 / 导出

    private Image imageFor(Card card, boolean faceUp) {
        if (faceUp) {
            return faceCache.computeIfAbsent(card, renderer::face);
        }
        return backCache.computeIfAbsent(backRed, renderer::back);
    }

    private void chooseAndExport(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择导出目录");
        File def = new File("output");
        def.mkdirs();
        chooser.setInitialDirectory(def);
        File chosen = chooser.showDialog(stage);
        if (chosen == null) {
            return;
        }
        try {
            int n = exportTo(chosen.toPath());
            status.setText("已导出 " + n + " 张牌面 PNG 到：" + chosen.getAbsolutePath());
        } catch (IOException ex) {
            status.setText("导出失败：" + ex.getMessage());
        }
    }

    private int exportTo(Path dir) throws IOException {
        Files.createDirectories(dir);
        List<Card> deck = Deck.standard().cards();
        int count = 0;
        for (Card card : deck) {
            Image img = faceCache.computeIfAbsent(card, renderer::face);
            imageWriter.write(img, dir.resolve(fileName(card)));
            count++;
        }
        imageWriter.write(backCache.computeIfAbsent(backRed, renderer::back),
                dir.resolve("00_BACK.png"));
        count++;
        return count;
    }

    private static String fileName(Card card) {
        if (card.isJoker()) {
            return card.isBigJoker() ? "54_RED_JOKER.png" : "53_BLACK_JOKER.png";
        }
        String suit = switch (card.suit()) {
            case SPADE -> "S";
            case HEART -> "H";
            case CLUB -> "C";
            case DIAMOND -> "D";
        };
        String idx = "%02d".formatted(13 * card.suit().ordinal() + card.rank());
        return idx + "_" + suit + card.rankText() + ".png";
    }

}
