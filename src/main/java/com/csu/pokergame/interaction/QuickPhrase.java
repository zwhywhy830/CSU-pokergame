package com.csu.pokergame.interaction;

/**
 * 玩家快捷短语（阶段 25）。
 *
 * <p>6 条混合风格短语（国风 + 现代口语），每条绑定一个 {@link InteractionType}
 * 决定配色与音效。点击短语按钮时直接广播到目标（默认随机 AI）。
 */
public enum QuickPhrase {
    /** 国风正面。 */
    NICE_MOVE("承让了！", InteractionType.PHRASE_POSITIVE),
    /** 国风正面。 */
    GREAT_HAND("妙手！", InteractionType.PHRASE_POSITIVE),
    /** 现代正面。 */
    GG_666("666", InteractionType.PHRASE_POSITIVE),
    /** 现代中性。 */
    BAD_LUCK("运气真差", InteractionType.PHRASE_NEUTRAL),
    /** 现代中性。 */
    PLAY_AGAIN("再来一局", InteractionType.PHRASE_NEUTRAL),
    /** 国风挑衅。 */
    FAREWELL("告辞", InteractionType.PHRASE_TAUNT);

    private final String text;
    private final InteractionType type;

    QuickPhrase(String text, InteractionType type) {
        this.text = text;
        this.type = type;
    }

    /** 短语文本。 */
    public String getText() {
        return text;
    }

    /** 对应的互动类型（决定音效与配色）。 */
    public InteractionType getType() {
        return type;
    }
}
