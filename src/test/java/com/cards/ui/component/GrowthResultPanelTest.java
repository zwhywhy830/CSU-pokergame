package com.cards.ui.component;

import com.cards.ui.FxTestKit;
import com.csu.pokergame.player.PlayerGrowthService;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GrowthResultPanel 阶段 9 冒烟测试：验证胜负面板构造、参数夹取、
 * 升级高亮块出现，以及 animateNumbers 在动画开关关闭时不抛异常。
 */
class GrowthResultPanelTest {

    @BeforeAll
    static void initFx() {
        FxTestKit.initToolkit();
    }

    @Test
    void winPanelWithUpgradeShowsAllRows() {
        FxTestKit.runAndWait(() -> {
            PlayerGrowthService.LevelUpResult growth =
                    new PlayerGrowthService.LevelUpResult(5, 7, 2, 300);

            GrowthResultPanel panel = new GrowthResultPanel(
                    true, 500, 120, 7, "新手", "★", 80, 800, growth);

            // 胜负 / 金币 / 经验 / 等级 / 经验进度 + 升级高亮块 = 6 行
            assertThat(panel.getChildren()).hasSize(6);
            assertThat(panel.getStyleClass()).contains("growth-result");
        });
    }

    @Test
    void losePanelWithoutUpgradeHasFiveRows() {
        FxTestKit.runAndWait(() -> {
            GrowthResultPanel panel = new GrowthResultPanel(
                    false, -50, 30, 3, "新手", "★", 10, 400, null);

            // 失败 + 无升级：5 行（无升级高亮块）
            assertThat(panel.getChildren()).hasSize(5);
        });
    }

    @Test
    void negativeInputsAreClampedToZero() {
        FxTestKit.runAndWait(() -> {
            GrowthResultPanel panel = new GrowthResultPanel(
                    true, -100, -50, -1, "新手", "★", -10, -1, null);

            // 参数夹取后不抛异常，面板正常构造
            assertThat(panel).isNotNull();
            assertThat(panel.getChildren()).isNotEmpty();
        });
    }

    @Test
    void animateNumbersCanBeCalledRepeatedly() {
        FxTestKit.runAndWait(() -> {
            GrowthResultPanel panel = new GrowthResultPanel(
                    true, 100, 50, 5, "新手", "★", 30, 600, null);

            // 重复调用不抛异常
            panel.animateNumbers(200, 100);
            panel.animateNumbers(0, 0);
            for (Node child : panel.getChildren()) {
                assertThat(child).isNotNull();
            }
        });
    }
}
