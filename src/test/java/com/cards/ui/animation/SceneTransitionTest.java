package com.cards.ui.animation;

import com.cards.ui.FxTestKit;
import com.csu.pokergame.ui.AppShell;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SceneTransition 阶段 3 改造测试：验证 AppShell 单 Scene 模式下
 * “navigate 被调用 + Overlay 演完移除 + 场景结构零残留”的核心契约。
 */
class SceneTransitionTest {

    /** 两个路由页 + 一个 AppShell，全部挂在带尺寸的 Scene 上。 */
    private record Fixture(AppShell shell, Region pageA, Region pageB) {
        StackPane root() {
            return shell.getRoot();
        }
    }

    @BeforeAll
    static void initFx() {
        FxTestKit.initToolkit();
    }

    private Fixture newFixture() {
        Region pageA = markerPage("page-a");
        Region pageB = markerPage("page-b");
        Scene scene = new Scene(new StackPane(), 800, 600);
        AppShell shell = new AppShell(scene);
        shell.register("a", () -> pageA);
        shell.register("b", () -> pageB);
        return new Fixture(shell, pageA, pageB);
    }

    private static Region markerPage(String styleClass) {
        Region page = new Region();
        page.getStyleClass().add(styleClass);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }

    @Test
    void fadeTransitionSwapsContentAndRemovesOverlayWhenFinished() {
        AtomicReference<Fixture> ref = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        FxTestKit.runAndWait(() -> {
            Fixture fx = newFixture();
            ref.set(fx);
            fx.shell().navigate("a");
            assertThat(fx.root().getChildren()).containsExactly(fx.pageA());

            fx.shell().transitionTo("b", SceneTransition.Type.FADE, done::countDown);

            // 转场进行中：新页面被包进揭示层，Overlay 叠在最顶层
            assertThat(fx.root().getChildren()).hasSize(2);
            assertThat(fx.root().getChildren().get(1)).isInstanceOf(SceneTransitionOverlay.class);
        });

        // 等 320ms FADE 动画结束回调
        FxTestKit.waitForLatch(done);

        FxTestKit.runAndWait(() -> {
            Fixture fx = ref.get();
            // 契约一：navigate 已被调用，root 内容是目标页
            assertThat(fx.root().getChildren()).containsExactly(fx.pageB());
            // 契约二：Overlay 已移除，结构零残留
            assertThat(fx.root().getChildren())
                    .noneMatch(SceneTransitionOverlay.class::isInstance);
            // 契约三：目标页自身不带 transform / opacity 残留
            assertThat(fx.pageB().getOpacity()).isEqualTo(1.0);
            assertThat(fx.pageB().getScaleX()).isEqualTo(1.0);
            assertThat(fx.pageB().getScaleY()).isEqualTo(1.0);
            assertThat(fx.pageB().getTranslateY()).isEqualTo(0.0);
        });
    }

    @Test
    void noneTypeNavigatesImmediatelyAndRunsCallbackSynchronously() {
        FxTestKit.runAndWait(() -> {
            Fixture fx = newFixture();
            fx.shell().navigate("a");

            CountDownLatch done = new CountDownLatch(1);
            fx.shell().transitionTo("b", SceneTransition.Type.NONE, done::countDown);

            // NONE 不演动画：调用返回前回调已同步执行，root 直接是目标页，无 Overlay
            assertThat(done.getCount()).isZero();
            assertThat(fx.root().getChildren()).containsExactly(fx.pageB());
            assertThat(fx.root().getChildren())
                    .noneMatch(SceneTransitionOverlay.class::isInstance);
        });
    }

    @Test
    void nullTypeIsTreatedAsFadeAndStillCleansUp() {
        AtomicReference<Fixture> ref = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        FxTestKit.runAndWait(() -> {
            Fixture fx = newFixture();
            ref.set(fx);
            fx.shell().navigate("a");
            fx.shell().transitionTo("b", null, done::countDown);
        });

        FxTestKit.waitForLatch(done);

        FxTestKit.runAndWait(() -> {
            Fixture fx = ref.get();
            assertThat(fx.root().getChildren()).containsExactly(fx.pageB());
            assertThat(fx.root().getChildren())
                    .noneMatch(SceneTransitionOverlay.class::isInstance);
        });
    }

    @Test
    void interruptingLongTransitionFinishesOldOverlayBeforeNewOne() {
        AtomicReference<Fixture> ref = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        FxTestKit.runAndWait(() -> {
            Fixture fx = newFixture();
            ref.set(fx);
            fx.shell().navigate("a");

            // ENTER_GAME 时长 1000ms，起播后立刻再次导航（FADE 320ms）
            fx.shell().transitionTo("b", SceneTransition.Type.ENTER_GAME);
            fx.shell().transitionTo("a", SceneTransition.Type.FADE, done::countDown);

            // 旧 Overlay 已被 abort 收掉，只剩新转场的 揭示层 + Overlay
            long overlayCount = fx.root().getChildren().stream()
                    .filter(SceneTransitionOverlay.class::isInstance)
                    .count();
            assertThat(overlayCount).isEqualTo(1);
        });

        FxTestKit.waitForLatch(done);

        FxTestKit.runAndWait(() -> {
            Fixture fx = ref.get();
            // 第二次转场目标是 a，Overlay 同样必须被移除
            assertThat(fx.root().getChildren()).containsExactly(fx.pageA());
            assertThat(fx.root().getChildren())
                    .noneMatch(SceneTransitionOverlay.class::isInstance);
        });
    }

    @Test
    void unregisteredRouteStillFailsFast() {
        FxTestKit.runAndWait(() -> {
            Fixture fx = newFixture();
            fx.shell().navigate("a");
            assertThatThrownBy(() -> fx.shell().transitionTo("missing", SceneTransition.Type.NONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing");
        });
    }

    @Test
    void nullArgumentsAreIgnored() {
        FxTestKit.runAndWait(() -> {
            Fixture fx = newFixture();
            fx.shell().navigate("a");
            // 空 shell / 空路由直接 return，不抛异常、不改变页面
            SceneTransition.transition(null, "b", SceneTransition.Type.FADE);
            SceneTransition.transition(fx.shell(), null, SceneTransition.Type.FADE);
            assertThat(fx.root().getChildren()).containsExactly(fx.pageA());
        });
    }
}
