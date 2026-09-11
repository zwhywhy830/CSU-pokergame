package com.cards.render;

import com.cards.model.Card;
import com.cards.model.Suit;
import javafx.geometry.Bounds;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * 扑克牌渲染器实现：不依赖任何外部图片，全部用 JavaFX Canvas 矢量绘制。
 * 采用 240x340 高清分辨率绘制，界面展示时再缩放，保证导出的 PNG 清晰。
 *
 * “其他资源”：花色符号 / 星星等 Unicode 字形与系统字体，通过字形测量精确排版。
 */
public final class CanvasCardRenderer implements CardRenderer {

    private static final double W = WIDTH;
    private static final double H = HEIGHT;
    private static final double CORNER_RADIUS = 14;

    private static final Color INK = Color.web("#191919"); // 黑牌
    private static final Color RED = Color.web("#c62828"); // 红牌

    private static final String UI_FONT = "Segoe UI";
    private static final String GLYPH_FONT = "Segoe UI Symbol";
    private static final String SERIF_FONT = "Georgia";

    /** 牌面图像。 */
    @Override
    public Image face(Card card) {
        return render(W, H, gc -> drawFace(gc, card));
    }

    /** 牌背图像（蓝色 / 红色两套可选）。 */
    @Override
    public Image back(boolean red) {
        return render(W, H, gc -> drawBack(gc, red));
    }

    // ---------------------------------------------------------------- 渲染入口

    private static Image render(double w, double h, Consumer<GraphicsContext> painter) {
        Canvas canvas = new Canvas(w, h);
        painter.accept(canvas.getGraphicsContext2D());
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, new WritableImage((int) w, (int) h));
    }

    private static void drawFace(GraphicsContext gc, Card card) {
        Color backColor = Color.web("#fbfaf6");
        roundedPath(gc, 0, 0, W, H, CORNER_RADIUS);
        gc.clip();

        // 纸面底色（微渐变模拟纸张质感）
        LinearGradient paper = new LinearGradient(0, 0, 0, H, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffffff")), new Stop(1, backColor));
        gc.setFill(paper);
        gc.fillRect(0, 0, W, H);

        // 外描边 + 内衬边框
        gc.setStroke(Color.web("#262626"));
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(1.5, 1.5, W - 3, H - 3, CORNER_RADIUS, CORNER_RADIUS);
        gc.setStroke(Color.web("#b9b3a6"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(9, 9, W - 18, H - 18, CORNER_RADIUS - 4, CORNER_RADIUS - 4);

        if (card.isJoker()) {
            drawJokerFace(gc, card);
            return;
        }

        Color cc = suitColor(card.suit());
        int rank = card.rank();

        // 右上角标 = 左上角标旋转 180°（轴对称）
        drawCorner(gc, card, cc);
        gc.save();
        gc.translate(W, H);
        gc.rotate(180);
        drawCorner(gc, card, cc);
        gc.restore();

        if (rank >= Card.RANK_J) {
            drawCourt(gc, card, cc); // J Q K 花牌
        } else if (rank == Card.RANK_A) {
            gc.setFill(cc);
            centerGlyph(gc, card.suit().glyph(), W * 0.5, H * 0.53, H * 0.42); // A：居中大花色
        } else {
            double size = pipSize(rank); // 2..10：经典点数布局
            for (double[] p : PIPS(rank)) {
                gc.setFill(cc);
                centerGlyph(gc, card.suit().glyph(), p[0] * W, p[1] * H, size);
            }
        }
    }

    // ---------------------------------------------------------------- 角标

    private static void drawCorner(GraphicsContext gc, Card card, Color cc) {
        double x = W * 0.062;
        double y = H * 0.032;

        gc.setFill(cc);
        gc.setFont(Font.font(UI_FONT, FontWeight.BOLD, 31));
        double boxH = textAt(gc, card.rankText(), x, y); // 点数

        gc.setFont(Font.font(GLYPH_FONT, 25));
        textAt(gc, card.suit().glyph(), x + 2, y + boxH + 3); // 花色小图标
    }

    // ---------------------------------------------------------------- 点数布局

    /** 2..10 各点数的花色坐标（相对牌面的比例），按经典扑克对称布局。 */
    private static double[][] PIPS(int rank) {
        return switch (rank) {
            case 2 -> new double[][]{{0.5, 0.27}, {0.5, 0.73}};
            case 3 -> new double[][]{{0.5, 0.27}, {0.5, 0.5}, {0.5, 0.73}};
            case 4 -> new double[][]{{0.3, 0.31}, {0.7, 0.31}, {0.3, 0.69}, {0.7, 0.69}};
            case 5 -> new double[][]{{0.3, 0.31}, {0.7, 0.31}, {0.5, 0.5}, {0.3, 0.69}, {0.7, 0.69}};
            case 6 -> new double[][]{
                    {0.3, 0.27}, {0.7, 0.27}, {0.3, 0.5}, {0.7, 0.5}, {0.3, 0.73}, {0.7, 0.73}};
            case 7 -> new double[][]{
                    {0.3, 0.26}, {0.7, 0.26}, {0.3, 0.48}, {0.7, 0.48},
                    {0.3, 0.7}, {0.7, 0.7}, {0.5, 0.385}};
            case 8 -> new double[][]{
                    {0.3, 0.2}, {0.7, 0.2}, {0.3, 0.39}, {0.7, 0.39},
                    {0.3, 0.61}, {0.7, 0.61}, {0.3, 0.8}, {0.7, 0.8}};
            case 9 -> new double[][]{
                    {0.3, 0.23}, {0.5, 0.23}, {0.7, 0.23},
                    {0.3, 0.5}, {0.5, 0.5}, {0.7, 0.5},
                    {0.3, 0.77}, {0.5, 0.77}, {0.7, 0.77}};
            case 10 -> new double[][]{
                    {0.3, 0.2}, {0.7, 0.2}, {0.3, 0.35}, {0.7, 0.35}, {0.3, 0.5},
                    {0.7, 0.5}, {0.3, 0.65}, {0.7, 0.65}, {0.3, 0.8}, {0.7, 0.8}};
            default -> new double[0][0];
        };
    }

    private static double pipSize(int rank) {
        if (rank <= 3) return 56;
        if (rank <= 5) return 50;
        if (rank <= 7) return 44;
        return 38;
    }

    // ---------------------------------------------------------------- J Q K 花牌

    /** 花牌：大号衬线字母叠在淡花色底纹上，上下各一枚对称小图标。 */
    private static void drawCourt(GraphicsContext gc, Card card, Color cc) {
        String letter = card.rankText();
        double cx = W * 0.5;
        double cy = H * 0.5;

        // 背景大花色（半透明）
        gc.setGlobalAlpha(0.13);
        gc.setFill(cc);
        centerGlyph(gc, card.suit().glyph(), cx, cy + 2, H * 0.46);
        gc.setGlobalAlpha(1.0);

        gc.setFont(Font.font(SERIF_FONT, FontWeight.BOLD, H * 0.42));

        // 立体字：先深色错位一层，再叠主色渐变
        gc.setFill(cc.interpolate(Color.BLACK, 0.35));
        centerGlyph(gc, letter, cx, cy + 4, 0);
        gc.setFill(new LinearGradient(0, cy - 50, 0, cy + 50, false, CycleMethod.NO_CYCLE,
                new Stop(0, brighten(cc)), new Stop(1, cc)));
        centerGlyph(gc, letter, cx, cy, 0);

        // 上下对称小花色
        gc.setFill(cc);
        gc.setFont(Font.font(GLYPH_FONT, 34));
        centerGlyph(gc, card.suit().glyph(), cx, H * 0.14, 0);
        centerGlyph(gc, card.suit().glyph(), cx, H * 0.86, 0);
    }

    private static Color brighten(Color c) {
        return c.interpolate(Color.WHITE, 0.35);
    }

    /** 以 (x, y) 为中心的菱形（旋转 45° 的圆角方块），用作戏服菱形纹样。 */
    private static void diamond(GraphicsContext gc, double x, double y, double dx, double dy) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(45);
        gc.fillRoundRect(-dx, -dy, dx * 2, dy * 2, 3, 3);
        gc.restore();
    }

    // ---------------------------------------------------------------- 大小王（小丑造型）

    /**
     * 大小王牌面：小丑丑角造型。大王为红×金彩色小丑，小王为灰蓝单色小丑。
     * 构图：三尖小丑帽（带铃铛）＋白色大圆眼＋圆鼻头＋咧嘴白牙＋褶皱颈圈＋菱形戏服。
     */
    private static void drawJokerFace(GraphicsContext gc, Card card) {
        boolean big = card.isBigJoker();
        Color accent, deep, light, pale, cheek;
        if (big) { // 大王：红 × 金的彩色小丑
            accent = Color.web("#e53935");
            deep = Color.web("#7f1d1d");
            light = Color.web("#ffd54f");
            pale = Color.web("#ffe9c9");
            cheek = Color.web("#ff8a80", 0.5);
        } else { // 小王：灰蓝的单色小丑
            accent = Color.web("#546e7a");
            deep = Color.web("#263238");
            light = Color.web("#eceff1");
            pale = Color.web("#f2f4f6");
            cheek = Color.web("#90a4ae", 0.45);
        }
        Color mouthDark = big ? Color.web("#4e1510") : Color.web("#30363c");
        double cx = W / 2.0;

        // ---- 1. 身体（菱形戏服）
        LinearGradient body = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, light),
                new Stop(1, big ? Color.web("#e0a800") : Color.web("#cfd8dc")));
        gc.setFill(body);
        gc.fillRoundRect(cx - 58, 244, 116, 58, 28, 28);
        double[][] pat = {
                {cx - 30, 266}, {cx + 30, 266},
                {cx, 266}, {cx - 15, 286}, {cx + 15, 286}};
        for (int i = 0; i < pat.length; i++) {
            gc.setFill(i % 2 == 0 ? accent : deep);
            diamond(gc, pat[i][0], pat[i][1], 13, 8);
        }

        // ---- 2. 脖子 + 圆脸
        gc.setFill(pale.interpolate(deep, 0.12));
        gc.fillRoundRect(cx - 18, 206, 36, 44, 15, 15);
        gc.setFill(new RadialGradient(0, 0, 0.42, 0.38, 1.05, true, CycleMethod.NO_CYCLE,
                new Stop(0, pale.interpolate(Color.WHITE, 0.35)), new Stop(1, pale)));
        gc.fillOval(cx - 57, 102, 114, 122);

        // ---- 3. 五官
        // 眼白 + 瞳孔 + 高光
        for (double e : new double[]{cx - 25, cx + 25}) {
            gc.setFill(Color.WHITE);
            gc.fillOval(e - 8, 139, 16, 18);
            gc.setFill(deep);
            gc.fillOval(e - 4.5, 144.5, 9, 9);
            gc.setFill(Color.WHITE);
            gc.fillOval(e - 1.5, 146, 3.2, 3.2);
        }
        // 上扬眉毛（快乐的表情）
        gc.setStroke(deep);
        gc.setLineWidth(4);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(cx - 35, 137, cx - 21, 128);
        gc.strokeLine(cx + 35, 137, cx + 21, 128);
        // 腮红
        gc.setFill(cheek);
        gc.fillOval(cx - 40, 169, 22, 18);
        gc.fillOval(cx + 18, 169, 22, 18);
        // 圆鼻头
        RadialGradient nose = new RadialGradient(0, 0, 0.38, 0.3, 1.15, true, CycleMethod.NO_CYCLE,
                new Stop(0, big ? Color.web("#ffab91") : Color.web("#b0bec5")),
                new Stop(0.55, accent),
                new Stop(1, deep));
        gc.setFill(nose);
        gc.fillOval(cx - 14, 156, 28, 28);
        // 大笑的嘴：张开的下半圆 + 白牙
        double mx = cx - 23, my = 188, mw = 46, mh = 30;
        gc.setFill(mouthDark);
        gc.fillArc(mx, my, mw, mh, 180, 180, ArcType.ROUND);
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(mx + 8, my + mh / 2.0, 13, 8, 2, 2);
        gc.fillRoundRect(mx + 25, my + mh / 2.0, 13, 8, 2, 2);
        gc.setStroke(deep);
        gc.setLineWidth(3);
        gc.strokeArc(mx, my, mw, mh, 180, 180, ArcType.OPEN);

        // ---- 4. 褶皱颈圈
        for (int i = 0; i <= 8; i++) {
            double a = Math.toRadians(-90 + i * 22.5);
            double bx = cx + Math.sin(a) * 52;
            double by = 232 + Math.cos(a) * 18;
            gc.setFill(light);
            gc.fillOval(bx - 13, by - 13, 26, 26);
            gc.setStroke(big ? Color.web("#e0b300") : Color.web("#90a4ae"));
            gc.setLineWidth(1.4);
            gc.strokeOval(bx - 13, by - 13, 26, 26);
        }

        // ---- 5. 三尖小丑帽 + 铃铛
        gc.setFill(accent);
        gc.beginPath();
        gc.moveTo(58, 128);
        gc.lineTo(64, 56);
        gc.lineTo(96, 96);
        gc.lineTo(120, 32);
        gc.lineTo(144, 96);
        gc.lineTo(176, 56);
        gc.lineTo(182, 128);
        gc.closePath();
        gc.fill();
        // 帽体竖条纹（对帽型轮廓裁剪）
        gc.save();
        gc.beginPath();
        gc.moveTo(58, 128);
        gc.lineTo(64, 56);
        gc.lineTo(96, 96);
        gc.lineTo(120, 32);
        gc.lineTo(144, 96);
        gc.lineTo(176, 56);
        gc.lineTo(182, 128);
        gc.closePath();
        gc.clip();
        gc.setFill(Color.color(1, 1, 1, 0.22));
        for (int i = 0; i < 4; i++) {
            gc.fillRoundRect(66 + i * 30, 22, 9, 112, 4, 4);
        }
        gc.setStroke(deep);
        gc.setLineWidth(2.2);
        gc.stroke();
        gc.restore();
        // 铃铛
        for (double[] t : new double[][]{{64, 56}, {120, 32}, {176, 56}}) {
            gc.setFill(light);
            gc.fillOval(t[0] - 6.5, t[1] - 6.5, 13, 13);
            gc.setStroke(deep);
            gc.setLineWidth(1.6);
            gc.strokeOval(t[0] - 6.5, t[1] - 6.5, 13, 13);
            gc.setFill(deep);
            gc.fillOval(t[0] - 1.6, t[1] + 2.2, 3.2, 3.2);
        }

        // ---- 6. 下沿 JOKER 字样
        gc.setFill(big ? Color.web("#7a5200") : Color.web("#455a64"));
        gc.setFont(Font.font(UI_FONT, FontWeight.BOLD, 24));
        centerGlyph(gc, "JOKER", cx, 314, 0);
    }

    // ---------------------------------------------------------------- 牌背

    /** 蓝 / 红双色牌背：斜纹网底 + 金边徽章。 */
    private static void drawBack(GraphicsContext gc, boolean red) {
        roundedPath(gc, 0, 0, W, H, CORNER_RADIUS);
        gc.clip();

        Color baseA, baseB;
        if (red) {
            baseA = Color.web("#8c1f1f");
            baseB = Color.web("#4a0c0c");
        } else {
            baseA = Color.web("#1f4b77");
            baseB = Color.web("#0b2340");
        }
        Color cream = Color.web("#ecd9a0");

        LinearGradient bg = new LinearGradient(0, 0, 0, H, false, CycleMethod.NO_CYCLE,
                new Stop(0, baseA), new Stop(1, baseB));
        gc.setFill(bg);
        gc.fillRect(0, 0, W, H);

        // 斜纹网
        gc.setStroke(Color.color(1, 1, 1, 0.06));
        gc.setLineWidth(2);
        double step = 26;
        for (double i = -H; i < W + H; i += step) {
            gc.strokeLine(i, 0, i + H, H);
            gc.strokeLine(i + H, 0, i, H);
        }

        // 细点阵（布纹质感）
        gc.setFill(Color.color(1, 1, 1, 0.08));
        double dot = 26;
        for (double y = 20; y < H; y += dot) {
            for (double x = 20; x < W; x += dot) {
                gc.fillRect(x - 0.6, y - 0.6, 1.6, 1.6);
            }
        }

        // 描边
        gc.setStroke(cream);
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(8, 8, W - 16, H - 16, 10, 10);
        gc.setStroke(Color.color(1, 1, 1, 0.45));
        gc.setLineWidth(1);
        gc.strokeRoundRect(14, 14, W - 28, H - 28, 7, 7);

        // 中央椭圆徽章
        double bw = W * 0.62, bh = H * 0.30;
        double bx = (W - bw) / 2, by = (H - bh) / 2;
        RadialGradient badge = new RadialGradient(0, 0, 0.5, 0.35, 1.2, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f6e7bd")), new Stop(0.6, cream), new Stop(1, Color.web("#c9a65f")));
        gc.setFill(badge);
        gc.fillOval(bx, by, bw, bh);
        gc.setStroke(baseB);
        gc.setLineWidth(3);
        gc.strokeOval(bx, by, bw, bh);
        gc.setStroke(Color.color(1, 1, 1, 0.55));
        gc.setLineWidth(1.2);
        gc.strokeOval(bx + 6, by + 6, bw - 12, bh - 12);

        // POKER 字样
        gc.setFill(baseB);
        gc.setFont(Font.font(SERIF_FONT, FontWeight.BOLD, H * 0.105));
        centerGlyph(gc, "POKER", W * 0.5, H * 0.5, 0);
    }

    // ---------------------------------------------------------------- 排版工具

    private static Color suitColor(Suit suit) {
        return suit.isRed() ? RED : INK;
    }

    private static void roundedPath(GraphicsContext gc, double x, double y, double w, double h, double r) {
        gc.beginPath();
        gc.moveTo(x + r, y);
        gc.lineTo(x + w - r, y);
        gc.arcTo(x + w, y, x + w, y + r, r);
        gc.lineTo(x + w, y + h - r);
        gc.arcTo(x + w, y + h, x + w - r, y + h, r);
        gc.lineTo(x + r, y + h);
        gc.arcTo(x, y + h, x, y + h - r, r);
        gc.lineTo(x, y + r);
        gc.arcTo(x, y, x + r, y, r);
        gc.closePath();
    }

    /**
     * 以 (x, topY) 为文字排版框左上角绘制文本，返回文本高度，便于上下文字垂直衔接。
     * 文本方向使用 gc 当前字体与填充色。
     */
    private static double textAt(GraphicsContext gc, String s, double x, double topY) {
        Text probe = new Text(s);
        probe.setFont(gc.getFont());
        Bounds b = probe.getLayoutBounds();
        double baseline = topY - b.getMinY();
        gc.fillText(s, x, baseline);
        return b.getHeight();
    }

    /**
     * 以 (cx, cy) 为水平 / 垂直中心绘制文本。
     * size &gt; 0 时按 size 设置字形字体（适合图形符号），否则使用 gc 当前字体。
     */
    private static void centerGlyph(GraphicsContext gc, String s, double cx, double cy, double size) {
        if (size > 0) {
            gc.setFont(Font.font(GLYPH_FONT, size));
        }
        Text probe = new Text(s);
        probe.setFont(gc.getFont());
        Bounds b = probe.getLayoutBounds();
        double w = b.getWidth();
        double baseline = cy - (b.getMinY() + b.getHeight() / 2.0);
        gc.fillText(s, cx - w / 2.0, baseline);
    }
}
