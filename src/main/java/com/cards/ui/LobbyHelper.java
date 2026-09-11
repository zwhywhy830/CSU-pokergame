package com.cards.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.Random;

/**
 * 大厅页面共享辅助工具：从 DeckApp 提取的静态方法集合。
 *
 * <p>所有大厅系页面（首页 / 选择游戏 / 玩法模式 / 个人信息编辑）共用的
 * 视觉辅助、卡片构建、规则渲染、动画效果均集中在此。
 */
public final class LobbyHelper {

    private LobbyHelper() {
    }

    // ============================================================= 花色 / 装饰

    /** 在 StackPane 四角放花色装饰；watermark=true 加微模糊作暗纹底。 */
    public static void addCornerSuit(StackPane root, String g, Color color, Pos corner, boolean watermark) {
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

    public static void addCornerSuit(StackPane root, String g, Color color, Pos corner) {
        addCornerSuit(root, g, color, corner, false);
    }

    /** 在 StackPane 角落放一张半透明旋转的牌背装饰。 */
    public static void addCardDeco(StackPane root, Image img, double height,
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
    public static void addGradientSuit(HBox strip, String glyph, Color top, Color bottom) {
        Text t = new Text(glyph);
        t.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 54));
        t.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        t.setMouseTransparent(true);
        strip.getChildren().add(t);
    }

    // ============================================================= 动画

    /** 花色图标轻微上下浮动，错峰延迟避免整齐划一。 */
    public static void playSuitFloat(HBox strip) {
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

    /** 菜单内容依次淡入。 */
    public static void playMenuIntro(javafx.scene.Node... nodes) {
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

    /** 页面内容错峰淡入并轻微上浮。 */
    public static void playRiseIn(javafx.scene.Node... nodes) {
        double delay = 0;
        for (javafx.scene.Node n : nodes) {
            n.setOpacity(0.0);
            n.setTranslateY(16);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(n.opacityProperty(), 0.0),
                            new KeyValue(n.translateYProperty(), 16, Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.millis(460),
                            new KeyValue(n.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                            new KeyValue(n.translateYProperty(), 0, Interpolator.EASE_OUT)));
            tl.setDelay(Duration.millis(delay));
            tl.play();
            delay += 110;
        }
    }

    /** 主标题金色呼吸：极轻微缩放。 */
    public static void playTitleBreath(Text title) {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(title.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(title.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(com.cards.ui.theme.DesignTokens.ANIM_PULSE_SLOW,
                        new KeyValue(title.scaleXProperty(), 1.03, Interpolator.EASE_BOTH),
                        new KeyValue(title.scaleYProperty(), 1.03, Interpolator.EASE_BOTH)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    /** "开始游戏"主按钮辉光 + hover 放大。 */
    public static void addStartButtonGlow(StackPane holder, Button start) {
        var glow = new DropShadow(javafx.scene.effect.BlurType.GAUSSIAN,
                Color.rgb(255, 200, 80, 0.42), 24, 0.30, 0, 0);
        holder.setEffect(glow);
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 22, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(), Color.rgb(255, 200, 80, 0.40), Interpolator.EASE_BOTH)),
                new KeyFrame(com.cards.ui.theme.DesignTokens.ANIM_GLOW,
                        new KeyValue(glow.radiusProperty(), 40, Interpolator.EASE_BOTH),
                        new KeyValue(glow.colorProperty(), Color.rgb(255, 214, 110, 0.78), Interpolator.EASE_BOTH)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        ScaleTransition hoverIn = new ScaleTransition(com.cards.ui.theme.DesignTokens.ANIM_HOVER, start);
        hoverIn.setToX(1.05);
        hoverIn.setToY(1.05);
        ScaleTransition hoverOut = new ScaleTransition(com.cards.ui.theme.DesignTokens.ANIM_HOVER, start);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);
        start.hoverProperty().addListener((o, wasHover, nowHover) -> {
            if (nowHover) {
                hoverOut.stop();
                hoverIn.playFromStart();
            } else {
                hoverIn.stop();
                hoverOut.playFromStart();
            }
        });
    }

    // ============================================================= UI 组件

    /** 首页底部功能入口：字形图标 + 文案的药丸按钮。 */
    public static Button lobbyEntry(String icon, String text, Runnable action) {
        Text glyphIcon = new Text(icon);
        glyphIcon.getStyleClass().add("entry-glyph");
        glyphIcon.setFont(Font.font("Segoe UI Emoji", 18));
        Button btn = new Button(text);
        btn.setGraphic(glyphIcon);
        btn.getStyleClass().add("lobby-entry");
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setOnAction(e -> {
            clickSound();
            action.run();
        });
        return btn;
    }

    /** 绘制镂空八齿齿轮图形。 */
    public static Path gearShape() {
        Path gear = new Path();
        gear.setFillRule(FillRule.EVEN_ODD);
        int teeth = 8;
        double outerR = 13;
        double innerR = 9;
        double holeR = 4.5;
        for (int i = 0; i < teeth; i++) {
            double a0 = i * (Math.PI * 2 / teeth);
            double a1 = (i + 0.3) * (Math.PI * 2 / teeth);
            double a2 = (i + 0.7) * (Math.PI * 2 / teeth);
            double a3 = (i + 1) * (Math.PI * 2 / teeth);
            if (i == 0) {
                gear.getElements().add(new MoveTo(Math.cos(a0) * outerR, Math.sin(a0) * outerR));
            }
            gear.getElements().add(new LineTo(Math.cos(a1) * outerR, Math.sin(a1) * outerR));
            gear.getElements().add(new LineTo(Math.cos(a1) * innerR, Math.sin(a1) * innerR));
            gear.getElements().add(new LineTo(Math.cos(a2) * innerR, Math.sin(a2) * innerR));
            gear.getElements().add(new LineTo(Math.cos(a2) * outerR, Math.sin(a2) * outerR));
            gear.getElements().add(new LineTo(Math.cos(a3) * outerR, Math.sin(a3) * outerR));
        }
        gear.getElements().add(new ClosePath());
        for (int i = 0; i < 16; i++) {
            double a = i * (Math.PI / 8);
            if (i == 0) {
                gear.getElements().add(new MoveTo(Math.cos(a) * holeR, Math.sin(a) * holeR));
            } else {
                gear.getElements().add(new LineTo(Math.cos(a) * holeR, Math.sin(a) * holeR));
            }
        }
        gear.getElements().add(new ClosePath());
        return gear;
    }

    // ============================================================= 游戏卡片

    /** 生成一个游戏入口卡片按钮。 */
    public static Button gameOption(String icon, String name, String desc, String tag,
                                    boolean featured, Runnable onPlay) {
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
        box.setPadding(new Insets(26, 34, 26, 34));

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

    /** 配置游戏入口卡片的底色 / 边框 / 阴影与悬浮动效。 */
    public static void styleGameCard(Button card, boolean featured) {
        CornerRadii radii = new CornerRadii(22);
        BorderWidths strokeWidth = new BorderWidths(2);

        Color topIdle = Color.rgb(255, 255, 255, 0.06);
        Color botIdle = Color.rgb(255, 255, 255, 0.025);
        Color borderIdle = Color.rgb(225, 230, 236, 0.30);
        Color glowIdle = Color.rgb(0, 0, 0, 0.35);
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
        card.setBorder(new Border(new BorderStroke(borderIdle, BorderStrokeStyle.SOLID, radii, strokeWidth)));

        javafx.beans.property.DoubleProperty frac = new javafx.beans.property.SimpleDoubleProperty(0);
        frac.addListener((o, oldV, v) -> {
            double t = v.doubleValue();
            Color top = (Color) Interpolator.EASE_BOTH.interpolate(topIdle, topHover, t);
            Color bot = (Color) Interpolator.EASE_BOTH.interpolate(botIdle, botHover, t);
            Color border = (Color) Interpolator.EASE_BOTH.interpolate(borderIdle, borderHover, t);
            Color glow = (Color) Interpolator.EASE_BOTH.interpolate(glowIdle, glowHover, t);
            card.setBackground(cardBackground(top, bot, radii));
            card.setBorder(new Border(new BorderStroke(border, BorderStrokeStyle.SOLID, radii, strokeWidth)));
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
    public static Background cardBackground(Color top, Color bottom, CornerRadii radii) {
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
        return new Background(new BackgroundFill(grad, radii, Insets.EMPTY));
    }

    private static ImagePattern FROST_PATTERN;

    /** 卡片磨砂质感的细密噪点纹理（懒加载，全局复用）。 */
    public static ImagePattern frostPattern() {
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

    // ============================================================= 规则文案

    public static final String RUN_RULES_MD = """
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

            1. **有大必出（必压）**：上家出牌，你手里有能大过上家的牌，就必须打出来，不能过。有牌不出叫 "放走"，要包赔。
            2. 一轮全部要不起，出牌人继续出牌。
            3. **报单**：手里只剩 1 张牌，必须口头报单提醒其他人。
            4. 胜负：**最先出完手牌为头家获胜**；剩下两家按手里剩余牌数算分，剩几张算几分。剩 1 张保本不扣分。

            ## 放走包赔

            > 如果你手里有大牌可以压住上家，却选择要不起，直接放走对手跑完全部手牌，那么由你一个人承担另外两家的全部输分。
            """;

    public static final String LIAR_RULES_MD = """
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

    /** 把一段规则 Markdown 渲染成规则正文容器。 */
    public static VBox renderRulesBody(String md) {
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

    /** 构建主页"游戏规则"弹窗卡片。 */
    public static VBox buildRulesModalCard(StackPane overlay) {
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

        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane();
        scroller.getStyleClass().add("rules-scroll");
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setContent(renderRulesBody(RUN_RULES_MD));
        VBox.setVgrow(scroller, Priority.ALWAYS);

        tabs.selectedToggleProperty().addListener((o, oldT, newT) ->
                scroller.setContent(renderRulesBody(newT == liarTab ? LIAR_RULES_MD : RUN_RULES_MD)));

        card.getChildren().addAll(topRow, tabRow, scroller);
        return card;
    }

    // ============================================================= 其他

    /** 通用按钮点击音效。 */
    public static void clickSound() {
        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(com.csu.pokergame.audio.SoundEffect.BUTTON_CLICK);
    }

    /** 头像字形专用字体。 */
    public static Font avatarGlyphFont(double size, String glyph) {
        boolean emoji = glyph != null && !glyph.isEmpty() && glyph.codePointAt(0) >= 0x1F000;
        return Font.font(emoji ? "Segoe UI Emoji" : "Segoe UI Symbol", FontWeight.BOLD, size);
    }
}
