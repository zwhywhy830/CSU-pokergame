package com.csu.pokergame.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SettingsService 阶段 5 测试：全部使用 {@code @TempDir} 下的独立文件，
 * 不触碰单例与工程根 {@code data/} 目录。
 */
class SettingsServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void missingFileIsCreatedWithFactoryDefaults(@TempDir Path dir) {
        Path file = dir.resolve("settings.json");
        SettingsService service = new SettingsService(file);

        assertThat(Files.isRegularFile(file)).isTrue();
        GameSettings s = service.getSettings();
        assertThat(s.getMasterVolume()).isEqualTo(GameSettings.DEFAULT_VOLUME);
        assertThat(s.isMusicEnabled()).isTrue();
        assertThat(s.isSoundEnabled()).isTrue();
        assertThat(s.isAnimationEnabled()).isTrue();
        assertThat(s.isAiHintEnabled()).isTrue();
        assertThat(s.getTheme()).isEqualTo(GameSettings.DEFAULT_THEME);
        assertThat(service.getSettingsFile()).isEqualTo(file);
    }

    @Test
    void volumeIsClampedAndRoundTripsThroughDisk(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("settings.json");
        SettingsService service = new SettingsService(file);

        service.setVolume(150);
        assertThat(service.getSettings().getMasterVolume()).isEqualTo(GameSettings.MAX_VOLUME);
        service.setVolume(-9);
        assertThat(service.getSettings().getMasterVolume()).isEqualTo(GameSettings.MIN_VOLUME);
        service.setVolume(42);

        // 重新加载：验证真正落盘而不是只改内存
        SettingsService reopened = new SettingsService(file);
        assertThat(reopened.getSettings().getMasterVolume()).isEqualTo(42);

        JsonNode json = mapper.readTree(Files.readString(file));
        assertThat(json.get("masterVolume").asInt()).isEqualTo(42);
    }

    @Test
    void togglesPersist(@TempDir Path dir) {
        Path file = dir.resolve("settings.json");
        SettingsService service = new SettingsService(file);

        service.toggleMusic();
        service.toggleSound();
        service.toggleAnimation();
        service.toggleAiHint();

        GameSettings reloaded = new SettingsService(file).getSettings();
        assertThat(reloaded.isMusicEnabled()).isFalse();
        assertThat(reloaded.isSoundEnabled()).isFalse();
        assertThat(reloaded.isAnimationEnabled()).isFalse();
        assertThat(reloaded.isAiHintEnabled()).isFalse();
    }

    @Test
    void invalidThemeNormalizesToDark(@TempDir Path dir) {
        Path file = dir.resolve("settings.json");
        SettingsService service = new SettingsService(file);

        service.changeTheme("  NEON  ");
        assertThat(service.getSettings().getTheme()).isEqualTo(GameSettings.THEME_DARK);
        service.changeTheme(null);
        assertThat(service.getSettings().getTheme()).isEqualTo(GameSettings.THEME_DARK);
        service.changeTheme("DEFAULT");
        assertThat(service.getSettings().getTheme()).isEqualTo(GameSettings.THEME_DEFAULT);
        assertThat(service.getSettings().isDarkTheme()).isFalse();
    }

    @Test
    void corruptedFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{ 这不是合法 JSON ");

        SettingsService service = new SettingsService(file);
        GameSettings s = service.getSettings();
        assertThat(s.getMasterVolume()).isEqualTo(GameSettings.DEFAULT_VOLUME);
        assertThat(s.getTheme()).isEqualTo(GameSettings.DEFAULT_THEME);
        // 构造时的回写已把损坏文件修复成完整配置
        assertThat(Files.readString(file)).contains("masterVolume");
    }

    @Test
    void unknownJsonFieldsAreIgnoredAndMissingFieldsKeepDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{\"masterVolume\":33,\"futureFlag\":\"x\"}");

        GameSettings s = new SettingsService(file).getSettings();
        assertThat(s.getMasterVolume()).isEqualTo(33);
        assertThat(s.isSoundEnabled()).isTrue();
        assertThat(s.getTheme()).isEqualTo(GameSettings.DEFAULT_THEME);
    }

    @Test
    void getterReturnsDefensiveCopy(@TempDir Path dir) {
        SettingsService service = new SettingsService(dir.resolve("settings.json"));

        GameSettings snapshot = service.getSettings();
        snapshot.setMasterVolume(0);
        snapshot.setTheme("bogus");

        assertThat(service.getSettings().getMasterVolume()).isEqualTo(GameSettings.DEFAULT_VOLUME);
        assertThat(service.getSettings().getTheme()).isEqualTo(GameSettings.DEFAULT_THEME);
    }

    @Test
    void changeListenerFiresWithSnapshotAndCanBeRemoved(@TempDir Path dir) {
        SettingsService service = new SettingsService(dir.resolve("settings.json"));
        AtomicInteger calls = new AtomicInteger();
        java.util.function.Consumer<GameSettings> listener = s -> calls.incrementAndGet();

        service.addChangeListener(listener);
        service.setVolume(10);
        service.toggleMusic();
        assertThat(calls.get()).isEqualTo(2);

        service.removeChangeListener(listener);
        service.setVolume(20);
        assertThat(calls.get()).isEqualTo(2);

        // 空监听防御
        service.addChangeListener(null);
        service.setVolume(30);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void resetRestoresDefaults(@TempDir Path dir) {
        Path file = dir.resolve("settings.json");
        SettingsService service = new SettingsService(file);
        service.setVolume(0);
        service.toggleSound();

        service.reset();

        GameSettings reloaded = new SettingsService(file).getSettings();
        assertThat(reloaded.getMasterVolume()).isEqualTo(GameSettings.DEFAULT_VOLUME);
        assertThat(reloaded.isSoundEnabled()).isTrue();
    }
}
