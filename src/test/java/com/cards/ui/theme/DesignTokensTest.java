package com.cards.ui.theme;

import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesignTokensTest {

    @Test
    void radiusTokensArePositive() {
        assertThat(DesignTokens.RADIUS_CARD).isPositive();
        assertThat(DesignTokens.RADIUS_BUTTON).isPositive();
        assertThat(DesignTokens.RADIUS_PILL).isPositive();
        assertThat(DesignTokens.AVATAR_SIZE).isPositive();
    }

    @Test
    void fontTokensArePositive() {
        assertThat(DesignTokens.FONT_AVATAR_GLYPH).isPositive();
        assertThat(DesignTokens.FONT_AVATAR_NAME).isPositive();
        assertThat(DesignTokens.FONT_ENTRY).isPositive();
    }

    @Test
    void spacingTokensAreStrictlyOrdered() {
        assertThat(DesignTokens.SPACING_XS).isPositive();
        assertThat(DesignTokens.SPACING_SM).isGreaterThan(DesignTokens.SPACING_XS);
        assertThat(DesignTokens.SPACING_MD).isGreaterThan(DesignTokens.SPACING_SM);
        assertThat(DesignTokens.SPACING_LG).isGreaterThan(DesignTokens.SPACING_MD);
    }

    @Test
    void animationDurationsArePositive() {
        assertThat(positive(DesignTokens.ANIM_FADE)).isTrue();
        assertThat(positive(DesignTokens.ANIM_ZOOM)).isTrue();
        assertThat(positive(DesignTokens.ANIM_CARD)).isTrue();
        assertThat(positive(DesignTokens.ANIM_HOVER)).isTrue();
    }

    @Test
    void transitionDurationsArePositive() {
        assertThat(positive(DesignTokens.ANIM_TRANSITION_ENTER)).isTrue();
        assertThat(positive(DesignTokens.ANIM_TRANSITION_RETURN)).isTrue();
        assertThat(positive(DesignTokens.ANIM_CARD_PORTAL)).isTrue();
        assertThat(positive(DesignTokens.ANIM_PARTICLE)).isTrue();
        assertThat(positive(DesignTokens.ANIM_LIGHT_SWEEP)).isTrue();
        assertThat(positive(DesignTokens.ANIM_PULSE_SLOW)).isTrue();
        assertThat(positive(DesignTokens.ANIM_GLOW)).isTrue();
    }

    @Test
    void fadeIsQuickerThanZoomWhichIsQuickerThanEnterGame() {
        assertThat(DesignTokens.ANIM_FADE.toMillis())
                .isLessThan(DesignTokens.ANIM_ZOOM.toMillis());
        assertThat(DesignTokens.ANIM_ZOOM.toMillis())
                .isLessThan(DesignTokens.ANIM_TRANSITION_ENTER.toMillis());
    }

    private static boolean positive(Duration duration) {
        return duration != null && duration.toMillis() > 0;
    }
}
