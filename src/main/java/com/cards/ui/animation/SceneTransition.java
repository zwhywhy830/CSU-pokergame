package com.cards.ui.animation;

import com.cards.ui.theme.DesignTokens;
import com.csu.pokergame.ui.AppShell;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 场景转场（SceneTransition Pro）：替代裸 {@code stage.setScene(...)} 的硬切，
 * 提供国风棋牌手游级转场。
 *
 * <p>提供两套入口：
 * <ul>
 *   <li>{@link #navigate(Stage, Scene, Type)} —— 原版多 Scene 模式，通过 {@link Stage#setScene(Scene)} 切换；</li>
 *   <li>{@link #transition(AppShell, String, Type, Runnable)} —— AppShell 单 Scene 模式，
 *       在同一 Scene 的 StackPane 根上叠 Overlay + 切 root 内容。</li>
 * </ul>
 *
 * <p>实现要点：切换前把旧场景根节点抓成截图，连同光幕 / 扫光 / 花色粒子 / 金色光环 /
 * 巨大牌背一起放进 {@link SceneTransitionOverlay}（铺满 Stage 的 StackPane，压在新场景最顶层）；
 * 新场景的原有内容被临时收进“新场景揭示层”，只对它做淡入 / 缩放。
 * 因此旧场景的残影与新场景的淡入互不干扰，也不会把 transform / opacity 残留到场景根上
 * （转场被打断时场景依然干净）。
 *
 * <p><b>四种高级转场</b>：
 * <ul>
 *   <li>{@link Type#ENTER_GAME} 进入牌桌：大厅缩小 → 金色光环扩散 → ♠♥♦♣ 四散 →
 *       巨大牌背旋转 180° 出现 → 炸开，牌桌自牌面背后展开（1000ms）；</li>
 *   <li>{@link Type#RETURN_LOBBY} 返回大厅：牌桌渐暗 → 卡牌碎片与金色粒子向中心聚合 →
 *       大厅重新展开（850ms）；</li>
 *   <li>{@link Type#OPEN_PROFILE} 个人信息：圆形光圈展开 → AvatarView 放大 →
 *       背景虚化 → 信息卡片滑入；</li>
 *   <li>{@link Type#WIN_TO_LOBBY} 胜利返回：皇冠粒子残留 + 金币飞散 → 场景旋转淡出。</li>
 * </ul>
 *
 * <p>必须运行在 JavaFX Application Thread（按钮回调等）。
 */
public final class SceneTransition {

    /** 转场类型。 */
    public enum Type {
        /** 无动画：仅切换场景（启动首屏，此时没有可参考的旧场景）。 */
        NONE,
        /** 交叉淡化：旧场景 1 → 0，新场景 0 → 1。用于返回 / 同级页面切换。 */
        FADE,
        /** 缩放进入：旧场景 1 → 0 并缩到 0.96，新场景 0.96 → 1，叠一层轻微金色辉光。 */
        ZOOM,
        /** 进入牌桌：金色光环 + 花色粒子 + 巨大牌背翻转，牌桌自牌面背后展开。 */
        ENTER_GAME,
        /** 返回大厅：牌桌渐暗 + 碎片向中心聚合，大厅重新展开。 */
        RETURN_LOBBY,
        /** 打开个人信息：圆形光圈 + 头像放大 + 背景虚化 + 卡片滑入。 */
        OPEN_PROFILE,
        /** 胜利返回：皇冠残留 + 金币飞散 + 场景旋转淡出。 */
        WIN_TO_LOBBY
    }

    private SceneTransition() {
    }

    // ============================================================= AppShell 单 Scene 入口

    /**
     * AppShell 单 Scene 模式的转场入口：在同一 Scene 的 StackPane 根上叠 Overlay，
     * 切换 root 内容并播放对应转场。
     *
     * <p>流程：抓取旧场景快照 → {@link AppShell#navigate(String)} 切换 root 内容 →
     * 把新内容包进揭示层 + 叠 Overlay → 播放转场 → 动画结束移除 Overlay。
     *
     * @param shell          AppShell（提供 getRoot 与 navigate）
     * @param routeName      目标路由名（已注册）
     * @param type           转场类型，{@code null} 视为 {@link Type#FADE}
     * @param afterTransition 转场结束回调（可 null）
     */
    public static void transition(AppShell shell, String routeName, Type type, Runnable afterTransition) {
        if (shell == null || routeName == null) {
            return;
        }
        Type kind = type == null ? Type.FADE : type;
        StackPane root = shell.getRoot();
        Scene scene = root.getScene();

        // 首屏 / 无旧场景：直接切换，不演转场
        if (kind == Type.NONE || scene == null) {
            SceneTransitionOverlay.abortActive();
            shell.navigate(routeName);
            if (afterTransition != null) afterTransition.run();
            return;
        }

        // 上一次转场没演完就再次导航：先收干净
        SceneTransitionOverlay.abortActive();

        // 抓取旧场景快照（在切换之前，此时 root 还是旧页面）
        SceneTransitionOverlay overlay = SceneTransitionOverlay.capture(scene);

        // 切换 root 内容（此时 root.getChildren() 变成新页面）
        shell.navigate(routeName);

        // 把新页面内容包进揭示层，叠 Overlay 到 root 上
        overlay.installOnStackPane(root);
        StackPane reveal = overlay.revealLayer();

        double fromScale = switch (kind) {
            case ENTER_GAME, OPEN_PROFILE -> 0.94;
            case WIN_TO_LOBBY -> 1.04;
            case ZOOM -> 0.96;
            default -> 1.0;
        };
        double fromY = switch (kind) {
            case ENTER_GAME -> 22.0;
            case OPEN_PROFILE -> 18.0;
            default -> 0.0;
        };
        reveal.setOpacity(0.0);
        reveal.setScaleX(fromScale);
        reveal.setScaleY(fromScale);
        reveal.setTranslateY(fromY);

        Timeline revealIn = switch (kind) {
            case ENTER_GAME -> enterGame(overlay, reveal, fromScale, fromY);
            case RETURN_LOBBY -> returnLobby(overlay, reveal, fromScale, fromY);
            case OPEN_PROFILE -> openProfile(overlay, scene, reveal, fromScale, fromY);
            case WIN_TO_LOBBY -> winToLobby(overlay, reveal, fromScale, fromY);
            case ZOOM -> zoomIn(overlay, reveal, fromScale, fromY);
            default -> fadeIn(overlay, reveal, fromScale, fromY);
        };

        overlay.track(revealIn);
        revealIn.setOnFinished(e -> {
            overlay.finish();
            if (afterTransition != null) afterTransition.run();
        });
        revealIn.play();
    }

    /** AppShell 模式的语义化入口（无结束回调）。 */
    public static void transition(AppShell shell, String routeName, Type type) {
        transition(shell, routeName, type, null);
    }

    // ============================================================= 原版多 Scene 语义化入口

    /** 淡入淡出：最克制的转场，用于返回 / 同级页面切换。 */
    public static void fade(Stage stage, Scene next) {
        navigate(stage, next, Type.FADE);
    }

    /** 缩放进入：新场景由 0.96 放大归位 + 淡入，用于进入下一流程（玩法模式 / 游戏大厅）。 */
    public static void zoom(Stage stage, Scene next) {
        navigate(stage, next, Type.ZOOM);
    }

    /** 进入牌桌 / 开始游戏：光环 + 花色粒子 + 巨大牌背翻转。 */
    public static void enterGame(Stage stage, Scene next) {
        navigate(stage, next, Type.ENTER_GAME);
    }

    /** 返回大厅：牌桌渐暗 + 碎片聚合。 */
    public static void returnLobby(Stage stage, Scene next) {
        navigate(stage, next, Type.RETURN_LOBBY);
    }

    /** 打开个人信息：圆形光圈 + 头像放大。 */
    public static void openProfile(Stage stage, Scene next) {
        navigate(stage, next, Type.OPEN_PROFILE);
    }

    /** 胜利返回：皇冠 + 金币粒子 + 场景旋转淡出。 */
    public static void winToLobby(Stage stage, Scene next) {
        navigate(stage, next, Type.WIN_TO_LOBBY);
    }

    /** 兼容旧调用：发牌式进入等价于 {@link #enterGame}。 */
    public static void card(Stage stage, Scene next) {
        enterGame(stage, next);
    }

    // ============================================================= 原版统一入口

    /**
     * 统一导航入口：切换场景并播放对应转场。
     *
     * @param stage 目标窗口
     * @param next  新场景
     * @param type  转场类型，{@code null} 视为 {@link Type#FADE}
     */
    public static void navigate(Stage stage, Scene next, Type type) {
        if (stage == null || next == null) {
            return;
        }
        Type kind = type == null ? Type.FADE : type;
        Scene old = stage.getScene();

        // 首屏 / 同场景重进 / 无旧场景：直接切换，不演转场
        if (kind == Type.NONE || old == null || old == next || next.getRoot() == null) {
            SceneTransitionOverlay.abortActive();
            stage.setScene(next);
            return;
        }

        // 上一次转场没演完就再次导航：先收干净，避免残影被快照进新画面
        SceneTransitionOverlay.abortActive();

        SceneTransitionOverlay overlay = SceneTransitionOverlay.capture(old);
        overlay.install(next);
        StackPane reveal = overlay.revealLayer();

        double fromScale = switch (kind) {
            case ENTER_GAME, OPEN_PROFILE -> 0.94;
            case WIN_TO_LOBBY -> 1.04;
            case ZOOM -> 0.96;
            default -> 1.0;
        };
        double fromY = switch (kind) {
            case ENTER_GAME -> 22.0;
            case OPEN_PROFILE -> 18.0;
            default -> 0.0;
        };
        reveal.setOpacity(0.0);
        reveal.setScaleX(fromScale);
        reveal.setScaleY(fromScale);
        reveal.setTranslateY(fromY);

        stage.setScene(next);   // 旧场景已切走，残影继续在覆盖层里演完

        Timeline revealIn = switch (kind) {
            case ENTER_GAME -> enterGame(overlay, reveal, fromScale, fromY);
            case RETURN_LOBBY -> returnLobby(overlay, reveal, fromScale, fromY);
            case OPEN_PROFILE -> openProfile(overlay, next, reveal, fromScale, fromY);
            case WIN_TO_LOBBY -> winToLobby(overlay, reveal, fromScale, fromY);
            case ZOOM -> zoomIn(overlay, reveal, fromScale, fromY);
            default -> fadeIn(overlay, reveal, fromScale, fromY);
        };

        overlay.track(revealIn);
        revealIn.setOnFinished(e -> overlay.finish());
        revealIn.play();
    }

    // ============================================================= 转场编排

    /**
     * ① 进入牌桌（ENTER_GAME，1000ms）：
     * 大厅缩小 + 渐暗虚化 → 金色扫光 → 金色光环自中心扩散 + ♠♥♦♣ 四散 →
     * 中央巨大牌背出现并旋转 180° → 牌背炸开，牌桌自牌面背后展开。
     */
    private static Timeline enterGame(SceneTransitionOverlay overlay, StackPane reveal,
                                      double fromScale, double fromY) {
        Duration total = DesignTokens.ANIM_TRANSITION_ENTER;              // 1000ms
        Duration burst = total.multiply(0.20);                            // 200ms
        Duration portal = DesignTokens.ANIM_CARD_PORTAL;                  // 650ms
        Duration portalAt = total.subtract(portal).subtract(burst);       // 150ms

        // ① 当前大厅缩小 1 → 0.92（同时渐暗 + 景深虚化）
        overlay.ghostOut(Duration.ZERO, total.multiply(0.58), 0.92, 16, 0);
        // ② 横向金色扫光 + 金色光幕呼吸
        overlay.lightSweep(Duration.ZERO, DesignTokens.ANIM_LIGHT_SWEEP);
        overlay.curtainGlow(Duration.ZERO, total.multiply(0.72), 0.55);
        // ② 金色光环自中心扩散
        overlay.ringExpand(total.multiply(0.10), total.multiply(0.62), 3.4);
        // ③ 大量 ♠♥♦♣ 花色粒子向四周飞散
        overlay.particlesBurst(total.multiply(0.10), DesignTokens.ANIM_PARTICLE, 72);
        // ④⑤ 中央巨大牌背出现（0.1 → 1.2 → 1）并旋转 180°
        overlay.portalIn(portalAt, portal);
        // ⑥ 牌背炸开淡出（旋转补满 360°），新牌桌自牌面背后展开
        overlay.portalBurst(portalAt.add(portal), burst);

        Duration start = total.multiply(0.72);
        return revealIn(reveal, start, total.subtract(start), fromScale, fromY);
    }

    /**
     * ② 返回大厅（RETURN_LOBBY，850ms）：
     * 牌桌渐暗 → 卡牌碎片与金色粒子向中心聚合 → 大厅重新展开。
     */
    private static Timeline returnLobby(SceneTransitionOverlay overlay, StackPane reveal,
                                        double fromScale, double fromY) {
        Duration total = DesignTokens.ANIM_TRANSITION_RETURN;             // 850ms

        // ① 牌桌渐暗（暗幕 + 景深虚化，随后整张残影淡出）
        overlay.curtainDarken(Duration.ZERO, total.multiply(0.42), 0.58);
        overlay.ghostOut(total.multiply(0.28), total.multiply(0.72), 0.96, 12, 0);
        // ② 卡牌碎片向中心吸收
        overlay.particlesAbsorb(Duration.ZERO, DesignTokens.ANIM_PARTICLE, 64);
        // ③ 金色粒子聚合：光环收拢 + 光幕峰值 + 一次扫光
        overlay.ringCollapse(total.multiply(0.10), total.multiply(0.62), 3.0);
        overlay.curtainGlow(total.multiply(0.12), total.multiply(0.62), 0.46);
        overlay.lightSweep(total.multiply(0.22), DesignTokens.ANIM_LIGHT_SWEEP);
        // ④ 大厅重新展开
        Duration start = total.multiply(0.46);
        return revealIn(reveal, start, total.subtract(start), fromScale, fromY);
    }

    /**
     * ③ 打开个人信息（OPEN_PROFILE）：
     * 圆形光圈展开 → AvatarView 放大 → 背景虚化 → 信息卡片滑入。
     */
    private static Timeline openProfile(SceneTransitionOverlay overlay, Scene next, StackPane reveal,
                                        double fromScale, double fromY) {
        Duration total = DesignTokens.ANIM_ZOOM.multiply(1.5);            // 570ms

        // ③ 背景虚化：旧页面失焦并轻微推近
        overlay.ghostOut(Duration.ZERO, total.multiply(0.62), 1.06, 18, 0);
        // ① 圆形光圈自中心展开
        overlay.ringExpand(Duration.ZERO, total.multiply(0.82), 2.6);
        overlay.curtainGlow(Duration.ZERO, total.multiply(0.70), 0.42);
        overlay.lightSweep(Duration.ZERO, DesignTokens.ANIM_LIGHT_SWEEP);
        // ② 新场景里的 AvatarView 放大入场
        overlay.avatarZoom(next, total.multiply(0.16), total.multiply(0.84));
        // ④ 信息卡片滑入
        Duration start = total.multiply(0.30);
        return revealIn(reveal, start, total.subtract(start), fromScale, fromY);
    }

    /**
     * ④ 胜利返回（WIN_TO_LOBBY，850ms）：
     * 皇冠粒子残留 + 金币飞散 → 场景旋转淡出 → 大厅展开。
     */
    private static Timeline winToLobby(SceneTransitionOverlay overlay, StackPane reveal,
                                       double fromScale, double fromY) {
        Duration total = DesignTokens.ANIM_TRANSITION_RETURN;             // 850ms

        // ③ 场景旋转淡出（轻微放大 + 逆向旋转，像被抽走）
        overlay.ghostOut(Duration.ZERO, total.multiply(0.74), 1.08, 8, -4.0);
        // ① 皇冠粒子残留 + ② 金币飞散
        overlay.particlesCelebrate(Duration.ZERO, DesignTokens.ANIM_PARTICLE, 26, 6);
        overlay.curtainGlow(Duration.ZERO, total.multiply(0.60), 0.50);
        overlay.lightSweep(total.multiply(0.14), DesignTokens.ANIM_LIGHT_SWEEP);
        // 大厅展开
        Duration start = total.multiply(0.48);
        return revealIn(reveal, start, total.subtract(start), fromScale, fromY);
    }

    /** 缩放进入：残影淡出缩到 0.96，叠金色辉光 + 光环 + 少量粒子。 */
    private static Timeline zoomIn(SceneTransitionOverlay overlay, StackPane reveal,
                                   double fromScale, double fromY) {
        Duration d = DesignTokens.ANIM_ZOOM;
        overlay.ghostOut(Duration.ZERO, d, 0.96, 0, 0);
        overlay.curtainGlow(Duration.ZERO, d, 0.50);
        overlay.lightSweep(Duration.ZERO, DesignTokens.ANIM_LIGHT_SWEEP);
        overlay.ringExpand(Duration.ZERO, d.multiply(1.1), 2.2);
        overlay.particlesBurst(Duration.ZERO, DesignTokens.ANIM_PARTICLE, 18);
        return revealIn(reveal, Duration.ZERO, d, fromScale, fromY);
    }

    /** 交叉淡化：最克制的转场。 */
    private static Timeline fadeIn(SceneTransitionOverlay overlay, StackPane reveal,
                                   double fromScale, double fromY) {
        Duration d = DesignTokens.ANIM_FADE;
        overlay.ghostOut(Duration.ZERO, d, 1.0, 0, 0);
        return revealIn(reveal, Duration.ZERO, d, fromScale, fromY);
    }

    /**
     * 新场景揭示层：透明度 0 → 1（同时缩放 / 位移归位）。
     *
     * @param delay    延迟（转场前段留给残影 / 光环 / 牌背演出）
     * @param duration 揭示层自身的时长，{@code delay + duration} 即整段转场总时长
     */
    private static Timeline revealIn(StackPane reveal, Duration delay, Duration duration,
                                     double fromScale, double fromY) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(reveal.opacityProperty(), 0.0),
                        new KeyValue(reveal.scaleXProperty(), fromScale),
                        new KeyValue(reveal.scaleYProperty(), fromScale),
                        new KeyValue(reveal.translateYProperty(), fromY)),
                new KeyFrame(duration,
                        new KeyValue(reveal.opacityProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(reveal.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(reveal.scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(reveal.translateYProperty(), 0.0, Interpolator.EASE_OUT)));
        tl.setDelay(delay);
        return tl;
    }
}
