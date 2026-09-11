package com.csu.pokergame.network;

/** 联机游戏类型。每种游戏对应不同的玩家数。 */
public enum GameType {
    PAO_DE_KUAI(3),
    LIARS_POKER(4);

    private final int requiredPlayers;

    GameType(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
    }

    public int requiredPlayers() {
        return requiredPlayers;
    }
}
