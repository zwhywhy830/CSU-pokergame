package com.cards.ui.background;

import com.cards.ui.FxTestKit;
import com.cards.ui.ParticleField;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BackgroundManager 阶段 4 冒烟测试：
 * 资源文件尚未复制（阶段 5 才到位），当前所有背景都走矢量山水兜底路径，
 * 正好验证“资源缺失不阻断、六层结构完整、粒子可控、全局切换不抛”。
 */
class BackgroundManagerTest {

    @BeforeAll
    static void initFx() {
        FxTestKit.initToolkit();
    }

    @AfterAll
    static void resetGlobalBackground() throws Exception {
        // currentResource 是进程级静态状态，反射还原避免污染后续测试类
        Field field = BackgroundManager.class.getDeclaredField("currentResource");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void lobbyBackgroundHasSixLayersAndIsMouseTransparent() {
        FxTestKit.runAndWait(() -> {
            BackgroundManager.Background bg = BackgroundManager.createLobbyBackground();

            StackPane root = bg.root();
            assertThat(root).isNotNull();
            assertThat(root.getStyleClass()).contains("bg-theme-root");
            assertThat(root.isMouseTransparent()).isTrue();
            // 美术层 / 主题色调 / 明暗渐变 / 牌桌增强 / 金色环境光 / 粒子层
            assertThat(root.getChildren()).hasSize(6);
            assertThat(root.getChildren()).contains(bg.shade());
            assertThat(root.getChildren().get(5)).isInstanceOf(ParticleField.class);
            assertThat(bg.shade().isMouseTransparent()).isTrue();
            assertThat(bg.particles()).isInstanceOf(ParticleField.class);
        });
    }

    @Test
    void gameAndLoadingBackgroundsAreConstructedWithoutAssets() {
        FxTestKit.runAndWait(() -> {
            BackgroundManager.Background game = BackgroundManager.createGameBackground();
            BackgroundManager.Background loading = BackgroundManager.createLoadingBackground();

            assertThat(game.root().getChildren()).hasSize(6);
            assertThat(loading.root().getChildren()).hasSize(6);

            // 粒子层 play/stop 幂等不抛
            game.particles().play();
            game.particles().play();
            game.particles().stop();
            loading.particles().play();
            loading.particles().stop();
        });
    }

    @Test
    void fallbackLandscapeRendersWhenMountedInScene() {
        FxTestKit.runAndWait(() -> {
            BackgroundManager.Background bg = BackgroundManager.createLoadingBackground();
            // 不挂 Stage，仅挂 Scene 并强制一次 layout/render pass，
            // 触发 ArtLayer 的“挂载后补画一次山水”监听
            new javafx.scene.Scene(bg.root(), 800, 600);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            bg.root().snapshot(params, null);

            // 兜底绘制后背景结构依旧完整
            assertThat(bg.root().getChildren()).hasSize(6);
        });
    }

    @Test
    void optionsExposeThreeKnownBackgrounds() {
        assertThat(BackgroundManager.OPTIONS)
                .extracting(BackgroundManager.BackgroundOption::resource)
                .containsExactly(
                        BackgroundManager.RES_LOBBY,
                        BackgroundManager.RES_GAME,
                        BackgroundManager.RES_LOADING);
        assertThat(BackgroundManager.OPTIONS)
                .extracting(BackgroundManager.BackgroundOption::label)
                .containsExactly("墨绿大厅", "鎏金牌桌", "幻境巨龙");
    }

    @Test
    void globalBackgroundSwitchUpdatesCurrentAndLiveLayers() {
        FxTestKit.runAndWait(() -> {
            BackgroundManager.Background live = BackgroundManager.createLobbyBackground();

            assertThat(BackgroundManager.currentBackground()).isNull();
            BackgroundManager.setBackground(BackgroundManager.RES_GAME);
            assertThat(BackgroundManager.currentBackground()).isEqualTo(BackgroundManager.RES_GAME);

            // 重复设置同一张直接跳过，不抛
            BackgroundManager.setBackground(BackgroundManager.RES_GAME);
            // null 入参防御性忽略，不抛
            BackgroundManager.setBackground(null);

            // 切换后新建背景仍可正常构造（资源不存在时继续走山水兜底）
            BackgroundManager.Background after = BackgroundManager.createGameBackground();
            assertThat(after.root().getChildren()).hasSize(6);
            assertThat(live.root().getChildren()).hasSize(6);
        });
    }

    @Test
    void fabricTextureReturnsReusableRegion() {
        FxTestKit.runAndWait(() -> {
            Region first = BackgroundManager.fabricTexture();
            Region second = BackgroundManager.fabricTexture();
            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(first.isMouseTransparent()).isTrue();
            assertThat(first.getBackground()).isNotNull();
            assertThat(second.getBackground()).isNotNull();
        });
    }
}
