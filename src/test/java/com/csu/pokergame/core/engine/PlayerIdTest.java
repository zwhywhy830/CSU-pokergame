package com.csu.pokergame.core.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerIdTest {

    @Test
    void turnsRotateCounterClockwise() {
        assertThat(PlayerId.SEAT_1.next()).isEqualTo(PlayerId.SEAT_2);
        assertThat(PlayerId.SEAT_2.next()).isEqualTo(PlayerId.SEAT_3);
        assertThat(PlayerId.SEAT_3.next()).isEqualTo(PlayerId.SEAT_4);
        assertThat(PlayerId.SEAT_4.next()).isEqualTo(PlayerId.SEAT_1);
    }
}
