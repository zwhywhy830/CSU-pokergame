package com.csu.pokergame.interaction;

import com.csu.pokergame.audio.SoundEffect;
import javafx.scene.paint.Color;

/**
 * 玩家互动类型（阶段 25）。
 *
 * <p>每项绑定：emoji 字符、对应音效、受击 Toast 文案、视觉色调。
 * <ul>
 *   <li>{@link #EGG} / {@link #FLOWER} / {@link #TOMATO} 投掷类——需选择目标座位；</li>
 *   <li>{@link #PHRASE_POSITIVE} / {@link #PHRASE_NEUTRAL} / {@link #PHRASE_TAUNT} 短语类——
 *       由 {@link QuickPhrase} 提供具体文案，本枚举只决定配色与音效。</li>
 * </ul>
 */
public enum InteractionType {
    /** 砸鸡蛋。 */
    EGG("\uD83E\uDD5A", SoundEffect.EGG_THROW, "被砸了鸡蛋！", Color.web("#ffd54f")),
    /** 送花。 */
    FLOWER("\uD83C\uDF40", SoundEffect.FLOWER_SEND, "收到一束花！", Color.web("#ff8fb1")),
    /** 砸番茄。 */
    TOMATO("\uD83C\uDF45", SoundEffect.TOMATO_THROW, "被砸了番茄！", Color.web("#ff6347")),
    /** 正面短语。 */
    PHRASE_POSITIVE(null, SoundEffect.PHRASE_POSITIVE, null, Color.web("#ffd54f")),
    /** 中性短语。 */
    PHRASE_NEUTRAL(null, SoundEffect.PHRASE_NEUTRAL, null, Color.web("#cfd8dc")),
    /** 挑衅短语。 */
    PHRASE_TAUNT(null, SoundEffect.PHRASE_TAUNT, null, Color.web("#ff8a80"));

    private final String emoji;
    private final SoundEffect sound;
    private final String hitText;
    private final Color accent;

    InteractionType(String emoji, SoundEffect sound, String hitText, Color accent) {
        this.emoji = emoji;
        this.sound = sound;
        this.hitText = hitText;
        this.accent = accent;
    }

    /** 互动 emoji（短语类返回 null）。 */
    public String getEmoji() {
        return emoji;
    }

    /** 对应音效。 */
    public SoundEffect getSound() {
        return sound;
    }

    /** 受击 Toast 文案（短语类返回 null，调用方应改用短语原文）。 */
    public String getHitText() {
        return hitText;
    }

    /** 视觉强调色。 */
    public Color getAccent() {
        return accent;
    }

    /** 是否为投掷类（需选择目标）。 */
    public boolean isThrow() {
        return this == EGG || this == FLOWER || this == TOMATO;
    }

    /** 是否为短语类。 */
    public boolean isPhrase() {
        return !isThrow();
    }
}
