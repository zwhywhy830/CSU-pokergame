package com.csu.pokergame.core.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 用例清单来自「成员5-测试与局域网任务.md」。 */
class PlayerIdTest {

    @Test
    void next方法按四人循环_SEAT1到2到3到4再回到1() {
        assertThat(PlayerId.SEAT_1.next()).isEqualTo(PlayerId.SEAT_2);
        assertThat(PlayerId.SEAT_2.next()).isEqualTo(PlayerId.SEAT_3);
        assertThat(PlayerId.SEAT_3.next()).isEqualTo(PlayerId.SEAT_4);
        assertThat(PlayerId.SEAT_4.next())
                .as("四人循环:末位绕回首座")
                .isEqualTo(PlayerId.SEAT_1);
    }

    @Test
    void 四个座位的valueOf与声明顺序正确() {
        assertThat(PlayerId.values()).hasSize(4);
        assertThat(PlayerId.valueOf("SEAT_1")).isEqualTo(PlayerId.SEAT_1);
        assertThat(PlayerId.valueOf("SEAT_2")).isEqualTo(PlayerId.SEAT_2);
        assertThat(PlayerId.valueOf("SEAT_3")).isEqualTo(PlayerId.SEAT_3);
        assertThat(PlayerId.valueOf("SEAT_4")).isEqualTo(PlayerId.SEAT_4);
        assertThat(PlayerId.SEAT_1.ordinal())
                .as("SEAT_1 即首座,联机时分配给主机")
                .isZero();
    }
}
