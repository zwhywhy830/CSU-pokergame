package com.cards.ui.theme;

import javafx.util.Duration;

/**
 * 设计令牌（Design Tokens）：圆角、字号、间距、动画时长的单一事实来源。
 *
 * <p>数值与现有 app.css 中已稳定使用的规格保持一致，新增组件/动效统一引用，
 * 避免魔法数字在代码中漂移。
 */
public final class DesignTokens {

    private DesignTokens() {
    }

    // ==================== 圆角 ====================

    /** 大卡片圆角（游戏入口卡、结算卡）。 */
    public static final double RADIUS_CARD = 22.0;
    /** 主按钮大圆角（开始游戏）。 */
    public static final double RADIUS_BUTTON = 34.0;
    /** 药丸入口圆角（底部功能入口、返回按钮）。 */
    public static final double RADIUS_PILL = 24.0;
    /** 圆形头像直径。 */
    public static final double AVATAR_SIZE = 54.0;

    // ==================== 字号 ====================

    /** 头像字形字号。 */
    public static final double FONT_AVATAR_GLYPH = 24.0;
    /** 头像昵称字号。 */
    public static final double FONT_AVATAR_NAME = 15.0;
    /** 底部入口/状态条字号。 */
    public static final double FONT_ENTRY = 15.0;

    // ==================== 间距 ====================

    public static final double SPACING_XS = 8.0;
    public static final double SPACING_SM = 12.0;
    public static final double SPACING_MD = 18.0;
    public static final double SPACING_LG = 28.0;

    // ==================== 动画时长 ====================

    /** 场景淡入淡出。 */
    public static final Duration ANIM_FADE = Duration.millis(320);
    /** 场景缩放进入。 */
    public static final Duration ANIM_ZOOM = Duration.millis(380);
    /** 发牌式卡片进入。 */
    public static final Duration ANIM_CARD = Duration.millis(420);

    // ==================== 转场（SceneTransition Pro） ====================

    /** 进入牌桌（ENTER_GAME）：大厅缩小 → 金色光环扩散 → 花色粒子四散 → 巨大牌背翻转 → 牌桌展开。 */
    public static final Duration ANIM_TRANSITION_ENTER = Duration.millis(1000);
    /** 返回大厅（RETURN_LOBBY）：牌桌渐暗 → 碎片向中心聚合 → 大厅重新展开。 */
    public static final Duration ANIM_TRANSITION_RETURN = Duration.millis(850);
    /** 中央巨大牌背（CardPortal）出现并回弹的时长。 */
    public static final Duration ANIM_CARD_PORTAL = Duration.millis(650);
    /** 花色粒子生命周期（♠♥♦♣ 飞散 / 聚合 / 金币皇冠）。 */
    public static final Duration ANIM_PARTICLE = Duration.millis(1200);
    /** 横向金色扫光（LightSweep）。 */
    public static final Duration ANIM_LIGHT_SWEEP = Duration.millis(700);
    /** 按钮 hover/pressed 反馈（即时但不打扰）。 */
    public static final Duration ANIM_HOVER = Duration.millis(180);
    /** 慢速呼吸循环（标题、光环）。 */
    public static final Duration ANIM_PULSE_SLOW = Duration.millis(1600);
    /** 中速呼吸循环（主按钮辉光）。 */
    public static final Duration ANIM_GLOW = Duration.millis(900);
}
