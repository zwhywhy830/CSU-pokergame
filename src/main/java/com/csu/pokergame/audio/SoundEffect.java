package com.csu.pokergame.audio;

/** 音效枚举（阶段 21）：每项绑定 classpath 资源路径，缺失时静默跳过。 */
public enum SoundEffect {
    BUTTON_CLICK("/audio/effect/button_click.wav"),
    CARD_PICK("/audio/effect/card_pick.wav"),
    CARD_PLAY("/audio/effect/card_play.wav"),
    WIN("/audio/effect/win.wav"),
    LOSE("/audio/effect/lose.wav"),
    COIN_GAIN("/audio/effect/coin.wav"),
    LEVEL_UP("/audio/effect/level_up.wav"),
    ACHIEVEMENT_UNLOCK("/audio/effect/achievement.wav"),
    /** 骗子酒馆：左轮空仓扣扳机（金属咔哒）。 */
    GUN_CLICK("/audio/effect/gun_click.wav"),
    /** 骗子酒馆：中弹枪响。 */
    GUN_FIRE("/audio/effect/gun_fire.wav"),
    /** 骗子酒馆：发起质疑时的紧张揭示音。 */
    CHALLENGE_REVEAL("/audio/effect/challenge_reveal.wav"),
    /** 跑得快语音包：要不起（本地玩家 / AI 过牌时）。 */
    PDK_CANNOT_PLAY("/audio/effect/pdk_cannot_play.wav"),
    /** 跑得快语音包：洗牌发牌。 */
    PDK_DEAL("/audio/effect/pdk_deal.wav"),
    /** 跑得快语音包：本局失败。 */
    PDK_LOSE("/audio/effect/pdk_lose.wav"),
    /** 跑得快语音包：本局胜利。 */
    PDK_WIN("/audio/effect/pdk_win.wav");

    private final String path;

    SoundEffect(String path) {
        this.path = path;
    }

    /** classpath 资源路径，例如 {@code /audio/effect/button_click.wav}。 */
    public String getPath() {
        return path;
    }
}