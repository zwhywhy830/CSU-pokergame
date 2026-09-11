package com.csu.pokergame;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LauncherTest {

    @Test
    void applicationTitleIsStable() {
        assertThat(Launcher.APP_TITLE).isEqualTo("CSU Poker Game");
    }
}
