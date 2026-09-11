package com.csu.pokergame.network;

/** 联机游戏类型。每种游戏对应不同的玩家数(跑得快 3 人、骗子酒馆 4 人)。 */
public enum GameType {
    PAO_DE_KUAI(3),
    LIARS_POKER(4);

    private final int requiredPlayers;

    GameType(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
    }

    /** 开局所需人数;人数未齐主机不能开局。 */
    public int requiredPlayers() {
        return requiredPlayers;
    }
}
