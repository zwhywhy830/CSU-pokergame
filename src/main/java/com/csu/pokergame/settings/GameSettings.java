package com.csu.pokergame.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * 棋牌平台设置数据对象（阶段 20）：一整套玩家偏好，独立落盘到 {@code data/settings.json}。
 *
 * <p><b>与玩家档案的关系：</b>设置是"跟机器 / 跟客户端走"的偏好，<b>不属于某个账号</b>，
 * 因此单独存 {@code settings.json}，<b>绝不写进 {@code player.json}</b>。
 * 切换账号不会重置设置，也不会把设置带进玩家存档（账号隔离）。
 *
 * <p>落盘格式（字段与顺序固定）：
 * <pre>{@code
 * {
 *   "masterVolume" : 80,
 *   "musicEnabled" : true,
 *   "soundEnabled" : true,
 *   "animationEnabled" : true,
 *   "aiHintEnabled" : true,
 *   "theme" : "dark"
 * }
 * }</pre>
 *
 * <p>字段含义：
 * <ul>
 *   <li>{@code masterVolume}：主音量，0 ~ 100；</li>
 *   <li>{@code musicEnabled}：背景音乐开关；</li>
 *   <li>{@code soundEnabled}：音效开关；</li>
 *   <li>{@code animationEnabled}：出牌动画开关；</li>
 *   <li>{@code aiHintEnabled}：AI 提示（出牌 / 操作提示）开关；</li>
 *   <li>{@code theme}：界面主题，{@code "dark"}（深色主题）/ {@code "default"}（默认主题）。</li>
 * </ul>
 *
 * <p><b>容错：</b>{@link #setMasterVolume(int)} 把音量夹到 {@code [0,100]}；
 * {@link #setTheme(String)} 把非法主题（含空白）归一为默认 {@link #DEFAULT_THEME}。
 * 因此无论来自界面、JSON 还是旧文件，对象里都不会出现越界值。
 *
 * <p>展示辅助方法（{@code getVolumeText} / {@code getMusicText} …）全部 {@link JsonIgnore}，
 * 只服务界面文案，不参与序列化。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"masterVolume", "musicEnabled", "soundEnabled", "animationEnabled",
        "aiHintEnabled", "theme"})
public class GameSettings {

    /** 默认主题：深色。 */
    public static final String THEME_DARK = "dark";
    /** 默认主题（浅色 / 经典）：{@code default}。 */
    public static final String THEME_DEFAULT = "default";
    /** 出厂默认主题。 */
    public static final String DEFAULT_THEME = THEME_DARK;
    /** 音量下限。 */
    public static final int MIN_VOLUME = 0;
    /** 音量上限。 */
    public static final int MAX_VOLUME = 100;
    /** 出场默认音量。 */
    public static final int DEFAULT_VOLUME = 80;

    /** 主音量 0 ~ 100。 */
    private int masterVolume = DEFAULT_VOLUME;
    /** 背景音乐开关。 */
    private boolean musicEnabled = true;
    /** 音效开关。 */
    private boolean soundEnabled = true;
    /** 出牌动画开关。 */
    private boolean animationEnabled = true;
    /** AI 提示（出牌 / 操作提示）开关。 */
    private boolean aiHintEnabled = true;
    /** 界面主题：dark / default。 */
    private String theme = DEFAULT_THEME;

    /** 出厂默认设置：全部字段取默认值。 */
    public GameSettings() {
    }

    public GameSettings(int masterVolume, boolean musicEnabled, boolean soundEnabled,
                        boolean animationEnabled, boolean aiHintEnabled, String theme) {
        setMasterVolume(masterVolume);
        this.musicEnabled = musicEnabled;
        this.soundEnabled = soundEnabled;
        this.animationEnabled = animationEnabled;
        this.aiHintEnabled = aiHintEnabled;
        setTheme(theme);
    }

    // ============================================================= getter / setter

    public int getMasterVolume() {
        return masterVolume;
    }

    /** 写主音量；越界值夹到 {@link #MIN_VOLUME} ~ {@link #MAX_VOLUME}。 */
    public void setMasterVolume(int masterVolume) {
        this.masterVolume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, masterVolume));
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    public boolean isAiHintEnabled() {
        return aiHintEnabled;
    }

    public void setAiHintEnabled(boolean aiHintEnabled) {
        this.aiHintEnabled = aiHintEnabled;
    }

    public String getTheme() {
        return theme;
    }

    /** 写主题；空白 / 非 {@code dark} / {@code default} 的值统一归一到 {@link #DEFAULT_THEME}。 */
    public void setTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            this.theme = DEFAULT_THEME;
            return;
        }
        String normalized = theme.trim().toLowerCase();
        this.theme = (THEME_DARK.equals(normalized) || THEME_DEFAULT.equals(normalized))
                ? normalized : DEFAULT_THEME;
    }

    // ============================================================= 展示辅助（只读，不参与序列化）

    /** 音量文案：{@code 80%}。 */
    @JsonIgnore
    public String getVolumeText() {
        return masterVolume + "%";
    }

    /** 音乐开关文案：{@code 开} / {@code 关}。 */
    @JsonIgnore
    public String getMusicText() {
        return musicEnabled ? "开" : "关";
    }

    /** 音效开关文案：{@code 开} / {@code 关}。 */
    @JsonIgnore
    public String getSoundText() {
        return soundEnabled ? "开" : "关";
    }

    /** 出牌动画开关文案：{@code 开} / {@code 关}。 */
    @JsonIgnore
    public String getAnimationText() {
        return animationEnabled ? "开" : "关";
    }

    /** AI 提示开关文案：{@code 开} / {@code 关}。 */
    @JsonIgnore
    public String getAiHintText() {
        return aiHintEnabled ? "开" : "关";
    }

    /** 主题文案：{@code 深色主题} / {@code 默认主题}。 */
    @JsonIgnore
    public String getThemeText() {
        return isDarkTheme() ? "深色主题" : "默认主题";
    }

    /** 当前是否深色主题。 */
    @JsonIgnore
    public boolean isDarkTheme() {
        return !THEME_DEFAULT.equals(theme);
    }

    // ============================================================= 工具

    /** 深拷贝：对外给快照，避免调用方就地改写服务内部状态。 */
    public GameSettings copy() {
        return new GameSettings(masterVolume, musicEnabled, soundEnabled,
                animationEnabled, aiHintEnabled, theme);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GameSettings other)) {
            return false;
        }
        return masterVolume == other.masterVolume
                && musicEnabled == other.musicEnabled
                && soundEnabled == other.soundEnabled
                && animationEnabled == other.animationEnabled
                && aiHintEnabled == other.aiHintEnabled
                && Objects.equals(theme, other.theme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterVolume, musicEnabled, soundEnabled,
                animationEnabled, aiHintEnabled, theme);
    }

    @Override
    public String toString() {
        return "GameSettings{volume=" + masterVolume + " music=" + musicEnabled
                + " sound=" + soundEnabled + " animation=" + animationEnabled
                + " aiHint=" + aiHintEnabled + " theme=" + theme + "}";
    }
}
