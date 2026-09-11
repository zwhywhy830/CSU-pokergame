package com.cards.ui.theme;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThemeTest {

    @Test
    void primaryPaletteIsOpaque() {
        assertThat(Theme.PRIMARY).isNotNull();
        assertThat(Theme.PRIMARY.getOpacity()).isEqualTo(1.0);
        assertThat(Theme.PRIMARY_LIGHT.getOpacity()).isEqualTo(1.0);
        assertThat(Theme.PRIMARY_DARK.getOpacity()).isEqualTo(1.0);
    }

    @Test
    void goldPaletteIsOpaque() {
        assertThat(Theme.GOLD).isNotNull();
        assertThat(Theme.GOLD_LIGHT).isNotNull();
        assertThat(Theme.GOLD_DARK).isNotNull();
        assertThat(Theme.GOLD_BRIGHT).isNotNull();
        assertThat(Theme.GOLD_TEXT).isNotNull();
        assertThat(Theme.GOLD.getOpacity()).isEqualTo(1.0);
    }

    @Test
    void textAndAccentColorsArePresent() {
        assertThat(Theme.ACCENT_RED).isNotNull();
        assertThat(Theme.TEXT_LIGHT).isNotNull();
        assertThat(Theme.TEXT_DIM).isNotNull();
    }

    @Test
    void shadeColorsAreSemiTransparent() {
        // 主题遮罩必须是半透明色偏，不能把画面完全压平
        assertThat(Theme.SHADE_GREEN.getOpacity()).isBetween(0.0, 1.0);
        assertThat(Theme.SHADE_NIGHT.getOpacity()).isBetween(0.0, 1.0);
        assertThat(Theme.TEXT_DIM.getOpacity()).isLessThan(1.0);
    }

    @Test
    void statusColorsAreDistinctOpaqueColors() {
        assertThat(Theme.STATUS_ONLINE).isNotNull();
        assertThat(Theme.STATUS_IN_GAME).isNotNull();
        assertThat(Theme.STATUS_BUSY).isNotNull();
        assertThat(Theme.STATUS_OFFLINE).isNotNull();
        assertThat(new Color[]{Theme.STATUS_ONLINE, Theme.STATUS_IN_GAME,
                Theme.STATUS_BUSY, Theme.STATUS_OFFLINE})
                .doesNotHaveDuplicates();
    }
}
