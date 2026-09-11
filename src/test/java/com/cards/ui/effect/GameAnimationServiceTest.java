package com.cards.ui.effect;

import com.cards.ui.FxTestKit;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GameAnimationService 阶段 8 冒烟测试：验证单例、动画开关覆盖、
 * 统计计数，以及按钮反馈/提示条等轻量动画在 FX 线程上不抛异常。
 */
class GameAnimationServiceTest {

    private static GameAnimationService service;

    @BeforeAll
    static void init() {
        FxTestKit.initToolkit();
        service = GameAnimationService.getInstance();
        service.resetStats();
    }

    @AfterAll
    static void cleanup() {
        // 还原开关覆盖，避免污染其他测试类
        service.setEnabledOverride(null);
        service.resetStats();
    }

    @Test
    void singletonIsSameInstance() {
        assertThat(GameAnimationService.getInstance()).isSameAs(service);
    }

    @Test
    void enabledOverrideControlsPlayback() {
        service.setEnabledOverride(false);
        assertThat(service.isEnabled()).isFalse();

        service.setEnabledOverride(true);
        assertThat(service.isEnabled()).isTrue();

        // null 表示恢复到 SettingsService 的实际配置
        service.setEnabledOverride(null);
        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void playButtonFeedbackIncrementsStatsWithoutThrowing() {
        FxTestKit.runAndWait(() -> {
            service.setEnabledOverride(true);
            Button button = new Button("test");
            StackPane root = new StackPane(button);
            new javafx.scene.Scene(root, 200, 200);

            int before = service.getPlayCount(GameAnimationService.Kind.BUTTON);
            boolean played = service.playButtonFeedback(button);

            assertThat(played).isTrue();
            assertThat(service.getPlayCount(GameAnimationService.Kind.BUTTON)).isEqualTo(before + 1);
            assertThat(service.getLastPlayed()).isEqualTo(GameAnimationService.Kind.BUTTON);
        });
    }

    @Test
    void disabledServiceSkipsAnimationAndDoesNotIncrement() {
        FxTestKit.runAndWait(() -> {
            service.setEnabledOverride(false);
            Button button = new Button("x");
            int before = service.getPlayCount(GameAnimationService.Kind.BUTTON);

            boolean played = service.playButtonFeedback(button);

            assertThat(played).isFalse();
            assertThat(service.getPlayCount(GameAnimationService.Kind.BUTTON)).isEqualTo(before);
        });
    }

    @Test
    void showToastDoesNotThrow() {
        FxTestKit.runAndWait(() -> {
            service.setEnabledOverride(true);
            Button anchor = new Button("anchor");
            StackPane root = new StackPane(anchor);
            new javafx.scene.Scene(root, 300, 300);

            int before = service.getPlayCount(GameAnimationService.Kind.TOAST);
            boolean shown = service.showToast(anchor, "hello");

            assertThat(shown).isTrue();
            assertThat(service.getPlayCount(GameAnimationService.Kind.TOAST)).isEqualTo(before + 1);
        });
    }

    @Test
    void resetStatsClearsAllCounters() {
        FxTestKit.runAndWait(() -> {
            service.setEnabledOverride(true);
            service.playButtonFeedback(new Button("a"));
            service.showToast(new Button("b"), "msg");
            assertThat(service.getLastPlayed()).isNotNull();

            service.resetStats();

            for (GameAnimationService.Kind kind : GameAnimationService.Kind.values()) {
                assertThat(service.getPlayCount(kind)).isZero();
            }
            assertThat(service.getLastPlayed()).isNull();
        });
    }

    @Test
    void nullNodeDoesNotThrow() {
        service.setEnabledOverride(true);
        // showToast 对 null anchor 明确返回 false
        assertThat(service.showToast(null, "text")).isFalse();
        // playButtonFeedback / playCardAnimation 对 null 节点只跳过动画、不抛异常
        // （begin 已计入统计，返回 true）
        service.playButtonFeedback(null);
        service.playCardAnimation((javafx.scene.Node) null, 0, 0, null);
        service.playCardAnimation((Runnable) null, null);
    }
}
