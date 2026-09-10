package com.csu.pokergame.core.engine;

/**
 * 中性座位标识。本地模式把 SEAT_1 呈现为“用户”，联机模式呈现为系统分配的名称。
 * 逆时针顺序：SEAT_1 → SEAT_2 → SEAT_3 → SEAT_4 → SEAT_1（四人循环）。
 * 三人游戏（跑得快）自行定义三人循环，见各自引擎的 nextSeat。
 */
public enum PlayerId {
    SEAT_1,
    SEAT_2,
    SEAT_3,
    SEAT_4;

    /** 四人逆时针下一位。 */
    public PlayerId next() {
        return switch (this) {
            case SEAT_1 -> SEAT_2;
            case SEAT_2 -> SEAT_3;
            case SEAT_3 -> SEAT_4;
            case SEAT_4 -> SEAT_1;
        };
    }
}
