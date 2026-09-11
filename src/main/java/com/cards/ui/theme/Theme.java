package com.cards.ui.theme;

import javafx.scene.paint.Color;

/**
 * 全局视觉主题：集中保存 Java 代码侧使用的颜色常量。
 *
 * <p>取值与现有 app.css / DeckApp 中硬编码的色板完全一致（国风墨绿 + 鎏金），
 * 新增组件统一引用此处，替代散落各处的 {@code Color.web(...)}。
 * CSS 侧颜色仍在 app.css 中维护，本类只服务 JavaFX 代码内绘图/动效。
 */
public final class Theme {

    private Theme() {
    }

    // ==================== 主色（墨绿牌桌系） ====================

    /** 主色：深墨绿（次按钮、桌台底色）。 */
    public static final Color PRIMARY = Color.web("#1f4528");
    /** 主色亮：悬浮态墨绿。 */
    public static final Color PRIMARY_LIGHT = Color.web("#2f6140");
    /** 主色暗：山水背景最深处。 */
    public static final Color PRIMARY_DARK = Color.web("#0c1f14");

    // ==================== 辅色 ====================

    /** 扑克红（红桃/方块/警示）。 */
    public static final Color ACCENT_RED = Color.web("#c62828");
    /** 亮色正文（暗底上的米白文字）。 */
    public static final Color TEXT_LIGHT = Color.web("#f5f8f3");
    /** 次级正文（70% 米白）。 */
    public static final Color TEXT_DIM = Color.rgb(245, 248, 243, 0.65);

    // ==================== 金色系（大厅主视觉） ====================

    /** 金色高光（字形渐变顶端）。 */
    public static final Color GOLD_LIGHT = Color.web("#fff3c4");
    /** 金色主色（字形渐变中段、描边）。 */
    public static final Color GOLD = Color.web("#e8c25e");
    /** 金色暗色（字形渐变底端）。 */
    public static final Color GOLD_DARK = Color.web("#b07f1e");
    /** 亮金（按钮、高亮描边）。 */
    public static final Color GOLD_BRIGHT = Color.web("#ffd54f");
    /** 暖金文字（次级标题）。 */
    public static final Color GOLD_TEXT = Color.web("#ffe9b0");

    // ==================== 背景遮罩 ====================

    /** 经典绿桌主题：半透明墨绿遮罩。 */
    public static final Color SHADE_GREEN = Color.rgb(8, 22, 14, 0.58);
    /** 星夜蓝主题：半透明深蓝遮罩。 */
    public static final Color SHADE_NIGHT = Color.rgb(10, 20, 40, 0.68);

    // ==================== 头像在线状态点 ====================

    /** 在线：绿色。 */
    public static final Color STATUS_ONLINE = Color.web("#4cd964");
    /** 游戏中：金色。 */
    public static final Color STATUS_IN_GAME = Color.web("#ffd54f");
    /** 勿扰：红色。 */
    public static final Color STATUS_BUSY = Color.web("#ff6b5e");
    /** 离线：灰色。 */
    public static final Color STATUS_OFFLINE = Color.web("#8a929a");
}
