package com.csu.pokergame.audio;

import com.cards.ui.FxTestKit;
import com.csu.pokergame.settings.GameSettings;
import com.csu.pokergame.settings.SettingsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AudioService 阶段 5 容错测试。
 *
 * <p>单例初始化前把 settings.data.file 指到临时目录下的“静音配置”
 * （音效/音乐均关、音量 50），保证测试不实际驱动音频设备，
 * 同时覆盖“资源缺失 / 非法路径 / 越界音量”一律静默不抛的契约。
 */
class AudioServiceTest {

    @TempDir
    static Path tempData;

    @BeforeAll
    static void initSilentSettings() throws Exception {
        FxTestKit.initToolkit();

        // 手写一份静音 settings.json，供 SettingsService 单例加载
        String json = """
                {
                  "masterVolume" : 50,
                  "musicEnabled" : false,
                  "soundEnabled" : false,
                  "animationEnabled" : true,
                  "aiHintEnabled" : true,
                  "theme" : "dark"
                }
                """;
        Path settingsFile = tempData.resolve("settings.json");
        Files.writeString(settingsFile, json);
        System.setProperty(SettingsService.SETTINGS_FILE_PROPERTY, settingsFile.toString());

        // 触发单例初始化（构造时读取静音设置）
        AudioService.getInstance();
    }

    @AfterAll
    static void clearOverride() {
        System.clearProperty(SettingsService.SETTINGS_FILE_PROPERTY);
    }

    @Test
    void singletonLoadsSilentSettings() {
        AudioService audio = AudioService.getInstance();
        assertThat(audio.isSoundEnabled()).isFalse();
        assertThat(audio.isMusicEnabled()).isFalse();
        assertThat(audio.getVolume()).isEqualTo(0.5);
    }

    @Test
    void playEffectNeverThrowsForAnyEnumOrNullWhenDisabled() {
        AudioService audio = AudioService.getInstance();
        for (SoundEffect effect : SoundEffect.values()) {
            audio.playEffect(effect);
            audio.playEffect(effect);
        }
        audio.playEffect(null);
    }

    @Test
    void musicApiToleratesNullBlankAndMissingResources() {
        AudioService audio = AudioService.getInstance();
        audio.playMusic(null);
        audio.playMusic("");
        audio.playMusic("   ");
        audio.playMusic("/audio/bgm/does-not-exist.wav");
        audio.playMusic("/audio/bgm/does-not-exist.mp3");
        assertThat(audio.hasMusic()).isFalse();
        audio.stopMusic();
        audio.stopMusic();
    }

    @Test
    void setVolumeIsClampedAndRefreshSettingsKeepsSilent() {
        AudioService audio = AudioService.getInstance();
        audio.setVolume(-1.0);
        assertThat(audio.getVolume()).isZero();
        audio.setVolume(2.0);
        assertThat(audio.getVolume()).isEqualTo(1.0);
        audio.setVolume(0.3);
        assertThat(audio.getVolume()).isEqualTo(0.3);
        // 恢复成静音配置里的 0.5，不应抛异常
        audio.refreshSettings();
        assertThat(audio.getVolume()).isEqualTo(0.5);
    }

    @Test
    void clampHelperBounds() {
        assertThat(AudioService.clamp(-0.2)).isZero();
        assertThat(AudioService.clamp(1.4)).isEqualTo(1.0);
        assertThat(AudioService.clamp(0.77)).isEqualTo(0.77);
    }

    @Test
    void everySoundEffectIsBackedByAClasspathResource() {
        for (SoundEffect effect : SoundEffect.values()) {
            assertThat(effect.getPath())
                    .as("音效 %s 资源路径", effect)
                    .startsWith("/audio/effect/")
                    .endsWith(".wav");
            assertThat(AudioService.class.getResource(effect.getPath()))
                    .as("音效资源文件存在: %s", effect.getPath())
                    .isNotNull();
        }
    }

    @Test
    void lobbyBgmWavResourceExistsForMp3Fallback() {
        // AudioService.BGM_LOBBY 写的是 .mp3，缺失时按同名 .wav 容错
        assertThat(AudioService.class.getResource("/audio/bgm/lobby.wav")).isNotNull();
        assertThat(GameSettings.DEFAULT_THEME).isEqualTo("dark");
    }
}
