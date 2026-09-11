package com.csu.pokergame.settings;

import com.csu.pokergame.network.JsonCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 棋牌平台设置服务（阶段 20）：{@link GameSettings} 的<b>唯一读写入口</b>。
 *
 * <p><b>落盘位置</b>：{@code data/settings.json}，与玩家存档（{@code data/players/*.json}）平级、
 * 但<b>完全独立</b>——设置不会写进 {@code player.json}，也不随账号切换而改变。
 *
 * <p><b>单例</b>：{@link #getInstance()} 全局唯一，所有页面都用它读取当前设置，
 * 因此任何一处改动都会立刻被其它页面看到（配合 {@link #addChangeListener} 可做到免重启实时同步）。
 *
 * <p><b>启动自动创建默认配置</b>：构造时先加载，再无条件回写一次。
 * 因此：
 * <ul>
 *   <li>文件不存在 → 生成一份出厂默认 {@code settings.json}；</li>
 *   <li>文件存在但<b>缺字段</b>（旧版本写的）→ Jackson 保留字段初始默认值，
 *       再由回写补齐成完整 6 字段，旧文件自动升级；</li>
 *   <li>文件内容损坏 → 退回出厂默认并重建。</li>
 * </ul>
 *
 * <p><b>改即存</b>：{@link #setVolume}/{@link #toggleMusic} 等写方法改完内存立即 {@link #save()}，
 * 不丢设置、无需重启。
 *
 * <p><b>路径解析</b>（优先级从高到低，均不写死绝对路径）：
 * <ol>
 *   <li>系统属性 {@code -Dsettings.data.file=...}（测试 / 多档位）；</li>
 *   <li>系统属性 {@code -Dplayer.data.dir=...} 下的 {@code settings.json}；</li>
 *   <li>从工作目录逐级向上找到含 {@code src/main/resources} 的工程根，取其下 {@code data/settings.json}；</li>
 *   <li>都找不到时退回工作目录下的 {@code data/settings.json}。</li>
 * </ol>
 * 与前 19 阶段的 {@code AccountService} / {@code PlayerManager} 使用同一套工程根查找规则。
 */
public final class SettingsService {

    /** 覆盖设置文件路径的系统属性：{@code -Dsettings.data.file=...}。 */
    public static final String SETTINGS_FILE_PROPERTY = "settings.data.file";
    /** 覆盖数据目录的系统属性（与 {@code AccountService} 一致）：{@code -Dplayer.data.dir=...}。 */
    public static final String DATA_DIR_PROPERTY = "player.data.dir";
    /** 数据目录名。 */
    public static final String DATA_DIR_NAME = "data";
    /** 设置文件名。 */
    public static final String SETTINGS_FILE_NAME = "settings.json";

    private static volatile SettingsService instance;

    private final Path settingsFile;
    private GameSettings settings;
    /** 设置变化监听：界面可订阅以做免重启的实时同步。 */
    private final List<Consumer<GameSettings>> listeners = new CopyOnWriteArrayList<>();

    /** 生产用法：单例，设置文件由 {@link #resolveSettingsFile()} 决定。 */
    public static SettingsService getInstance() {
        SettingsService local = instance;
        if (local == null) {
            synchronized (SettingsService.class) {
                local = instance;
                if (local == null) {
                    local = new SettingsService(resolveSettingsFile());
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * 指定设置文件路径的构造器（测试 / 多档位）。
     * 构造时完成一次加载，并把（补全默认值后的）设置回写落盘。
     */
    public SettingsService(Path settingsFile) {
        this.settingsFile = settingsFile;
        this.settings = load();
        // 启动自动创建 / 补全：保证磁盘上始终是一份完整、合法的配置
        save();
    }

    // ============================================================= 读取

    /**
     * 当前设置的<b>快照</b>（拷贝）。界面读值用它即可；
     * 写值请走 {@link #setVolume} 等写方法，保证"改内存 + 落盘"一起完成。
     */
    public synchronized GameSettings getSettings() {
        return settings.copy();
    }

    /** 设置文件路径（测试 / 展示用）。 */
    public Path getSettingsFile() {
        return settingsFile;
    }

    // ============================================================= 写方法（改完立即落盘）

    /** 保存当前设置到 {@code settings.json}（父目录自动创建）。返回是否成功。 */
    public synchronized boolean save() {
        if (settingsFile == null) {
            return false;
        }
        try {
            Path parent = settingsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonCodec.mapper().writerWithDefaultPrettyPrinter()
                    .writeValue(settingsFile.toFile(), settings);
            return true;
        } catch (IOException e) {
            warn("保存设置失败: " + settingsFile + " -> " + e.getMessage());
            return false;
        }
    }

    /** 恢复默认设置并立即保存。 */
    public synchronized void reset() {
        settings = new GameSettings();
        save();
        fireChanged();
    }

    /** 设置主音量（0 ~ 100，越界自动夹取），并立即保存。 */
    public synchronized void setVolume(int volume) {
        settings.setMasterVolume(volume);
        save();
        fireChanged();
    }

    /** 切换背景音乐开关，并立即保存。 */
    public synchronized void toggleMusic() {
        settings.setMusicEnabled(!settings.isMusicEnabled());
        save();
        fireChanged();
    }

    /** 切换音效开关，并立即保存。 */
    public synchronized void toggleSound() {
        settings.setSoundEnabled(!settings.isSoundEnabled());
        save();
        fireChanged();
    }

    /** 切换出牌动画开关，并立即保存。 */
    public synchronized void toggleAnimation() {
        settings.setAnimationEnabled(!settings.isAnimationEnabled());
        save();
        fireChanged();
    }

    /** 切换 AI 提示开关，并立即保存。 */
    public synchronized void toggleAiHint() {
        settings.setAiHintEnabled(!settings.isAiHintEnabled());
        save();
        fireChanged();
    }

    /** 切换主题（非法值归一为默认主题），并立即保存。 */
    public synchronized void changeTheme(String theme) {
        settings.setTheme(theme);
        save();
        fireChanged();
    }

    // ============================================================= 监听

    /**
     * 订阅设置变化（每次写方法成功后触发）。
     *
     * @param listener 回调，参数是最新设置快照
     */
    public void addChangeListener(Consumer<GameSettings> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** 取消订阅。 */
    public void removeChangeListener(Consumer<GameSettings> listener) {
        listeners.remove(listener);
    }

    private void fireChanged() {
        GameSettings snapshot = settings.copy();
        for (Consumer<GameSettings> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (RuntimeException e) {
                warn("设置监听回调异常: " + e.getMessage());
            }
        }
    }

    // ============================================================= 内部

    /**
     * 读取设置：文件存在则读（缺字段由字段默认值补齐），否则给出厂默认。
     * 解析失败只记警告并回退出厂默认，保证界面永远能起来。
     */
    private GameSettings load() {
        if (settingsFile != null && Files.isRegularFile(settingsFile)) {
            try (InputStream in = Files.newInputStream(settingsFile)) {
                GameSettings loaded = JsonCodec.mapper().readValue(in, GameSettings.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                warn("读取设置失败，改用默认设置: " + settingsFile + " -> " + e.getMessage());
            }
        }
        return new GameSettings();
    }

    /**
     * 解析设置文件路径：{@code -Dsettings.data.file} → {@code -Dplayer.data.dir}/settings.json
     * → 工程根 {@code data/settings.json} → 工作目录 {@code data/settings.json}。
     */
    static Path resolveSettingsFile() {
        String fileOverride = System.getProperty(SETTINGS_FILE_PROPERTY);
        if (fileOverride != null && !fileOverride.isBlank()) {
            return Paths.get(fileOverride);
        }
        String dirOverride = System.getProperty(DATA_DIR_PROPERTY);
        if (dirOverride != null && !dirOverride.isBlank()) {
            return Paths.get(dirOverride).resolve(SETTINGS_FILE_NAME);
        }
        Path dir = Paths.get("").toAbsolutePath();
        for (Path cursor = dir; cursor != null; cursor = cursor.getParent()) {
            if (Files.isDirectory(cursor.resolve("src").resolve("main").resolve("resources"))) {
                return cursor.resolve(DATA_DIR_NAME).resolve(SETTINGS_FILE_NAME);
            }
        }
        return dir.resolve(DATA_DIR_NAME).resolve(SETTINGS_FILE_NAME);
    }

    private static void warn(String message) {
        System.err.println("[SettingsService] " + message);
    }
}
