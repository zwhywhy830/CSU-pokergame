package com.csu.pokergame.audio;

import com.csu.pokergame.settings.GameSettings;
import com.csu.pokergame.settings.SettingsService;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一音频服务（阶段 21）：音效与背景音乐的唯一出口。
 *
 * <p>页面禁止直接使用 {@code AudioClip} / {@code MediaPlayer}，一律通过本服务：
 * <pre>{@code
 * AudioService.getInstance().playEffect(SoundEffect.BUTTON_CLICK);
 * AudioService.getInstance().playMusic("/audio/bgm/lobby.mp3");
 * AudioService.getInstance().setVolume(60);
 * AudioService.getInstance().refreshSettings();
 * }</pre>
 *
 * <p>所有播放路径都做了容错：资源不存在、无声音设备、播放异常一律静默跳过，绝不抛出到调用方。
 */
public final class AudioService {

    /** 背景音乐资源路径。 */
    public static final String BGM_LOBBY = "/audio/bgm/lobby.mp3";

    private static volatile AudioService instance;

    /** 音效缓存：首次播放时加载，之后复用。 */
    private final Map<SoundEffect, MediaPlayer> effectCache = new HashMap<>();
    private MediaPlayer musicPlayer;
    private String currentMusicPath;
    private String musicSourcePath;
    private double volume = 1.0;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private boolean ready;

    private AudioService() {
        refreshSettings();
    }

    /** 单例入口。 */
    public static AudioService getInstance() {
        AudioService local = instance;
        if (local == null) {
            synchronized (AudioService.class) {
                local = instance;
                if (local == null) {
                    local = new AudioService();
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 从设置服务读取音量 / 音效 / 音乐开关，并立即生效。 */
    public synchronized void refreshSettings() {
        try {
            GameSettings s = SettingsService.getInstance().getSettings();
            this.volume = clamp(s.getMasterVolume() / 100.0);
            this.soundEnabled = s.isSoundEnabled();
            this.musicEnabled = s.isMusicEnabled();
            applyMusicState();
        } catch (Throwable t) {
            warn("refreshSettings 失败: " + t.getMessage());
        }
        ready = true;
    }

    /** 播放一个音效（音效开关关闭时静默跳过）。 */
    public synchronized void playEffect(SoundEffect effect) {
        if (effect == null || !soundEnabled || volume <= 0) {
            return;
        }
        try {
            MediaPlayer p = effectCache.computeIfAbsent(effect, this::createPlayer);
            if (p == null) {
                return;
            }
            p.stop();
            p.setVolume(volume);
            p.seek(Duration.ZERO);
            p.play();
        } catch (Throwable t) {
            warn("playEffect 失败 " + effect + ": " + t.getMessage());
        }
    }

    /** 循环播放背景音乐；音乐开关关闭或音量为 0 时静默跳过。 */
    public synchronized void playMusic(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        currentMusicPath = path;
        if (!musicEnabled || volume <= 0) {
            return;
        }
        try {
            if (musicPlayer == null || !path.equals(musicSourcePath)) {
                disposeMusic();
                musicPlayer = createMusicPlayer(path);
                musicSourcePath = path;
            }
            if (musicPlayer == null) {
                return;
            }
            musicPlayer.setVolume(volume);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.play();
        } catch (Throwable t) {
            warn("playMusic 失败 " + path + ": " + t.getMessage());
        }
    }

    /** 停止背景音乐（不影响音效）。 */
    public synchronized void stopMusic() {
        try {
            if (musicPlayer != null) {
                musicPlayer.stop();
            }
        } catch (Throwable t) {
            warn("stopMusic 失败: " + t.getMessage());
        }
    }

    /** 设置音量（0.0 ~ 1.0，越界夹取），实时生效、无需重启。 */
    public synchronized void setVolume(double v) {
        this.volume = clamp(v);
        try {
            for (MediaPlayer p : effectCache.values()) {
                if (p != null) {
                    p.setVolume(this.volume);
                }
            }
            if (musicPlayer != null) {
                musicPlayer.setVolume(this.volume);
                if (this.volume <= 0) {
                    musicPlayer.pause();
                } else if (musicEnabled) {
                    musicPlayer.play();
                }
            }
        } catch (Throwable t) {
            warn("setVolume 失败: " + t.getMessage());
        }
    }

    // ============================= 内部

    /** 加载音效播放器；资源不存在 / 格式不支持时返回 null（静默跳过）。 */
    private MediaPlayer createPlayer(SoundEffect effect) {
        try {
            String path = effect.getPath();
            if (AudioService.class.getResource(path) == null) {
                return null;
            }
            MediaPlayer p = new MediaPlayer(new Media(resolve(path)));
            p.setVolume(volume);
            return p;
        } catch (Throwable t) {
            warn("加载音效失败 " + effect + ": " + t.getMessage());
            return null;
        }
    }

    /** 加载背景音乐播放器；资源不存在时为 null。 */
    private MediaPlayer createMusicPlayer(String path) {
        try {
            String real = path;
            if (AudioService.class.getResource(real) == null && real.toLowerCase().endsWith(".mp3")) {
                // 容错：mp3 缺失时尝试同名 wav（例如 lobby.mp3 → lobby.wav）
                String alt = real.substring(0, real.length() - 4) + ".wav";
                if (AudioService.class.getResource(alt) != null) {
                    real = alt;
                }
            }
            if (AudioService.class.getResource(real) == null) {
                return null;
            }
            MediaPlayer p = new MediaPlayer(new Media(resolve(real)));
            p.setVolume(volume);
            return p;
        } catch (Throwable t) {
            warn("加载音乐失败 " + path + ": " + t.getMessage());
            return null;
        }
    }

    /** 按当前音乐开关 / 音量同步背景音乐播放状态。 */
    private void applyMusicState() {
        if (musicPlayer == null) {
            return;
        }
        try {
            if (!musicEnabled || volume <= 0) {
                musicPlayer.pause();
            } else {
                musicPlayer.setVolume(volume);
                musicPlayer.play();
            }
        } catch (Throwable t) {
            warn("applyMusicState 失败: " + t.getMessage());
        }
    }

    /** 释放背景音乐播放器。 */
    private void disposeMusic() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
                musicPlayer.dispose();
            } catch (Throwable ignored) {
            }
            musicPlayer = null;
        }
        musicSourcePath = null;
    }

    /** classpath 路径 → Media 可加载的 URL 字符串。 */
    private static String resolve(String path) {
        URL url = AudioService.class.getResource(path);
        return url == null ? path : url.toExternalForm();
    }

    /** 音量夹取到 [0,1]。 */
    public static double clamp(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /** 当前音量 0.0 ~ 1.0（只读，供自检 / 界面展示）。 */
    public double getVolume() {
        return volume;
    }

    /** 音效开关当前状态（只读）。 */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /** 背景音乐开关当前状态（只读）。 */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    /** 是否有已加载的背景音乐播放器（只读，供自检使用）。 */
    public boolean hasMusic() {
        return musicPlayer != null;
    }

    private static void warn(String message) {
        System.err.println("[AudioService] " + message);
    }
}

