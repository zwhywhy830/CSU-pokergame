package com.csu.pokergame.player;

import com.csu.pokergame.network.JsonCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 玩家数据管理器：统一负责玩家档案的<b>加载 / 保存</b>，以及<b>经验、等级、胜负场次</b>的唯一写入口。
 *
 * <p>职责边界：
 * <ul>
 *   <li>加载：优先读当前存档文件 {@link #getDataFile()}（阶段 16 起即 {@code data/players/<username>.json}），
 *       读不到再退回 classpath 上的 {@code /player/player.json}（打包运行），都读不到则用全新默认档；</li>
 *   <li>切换：{@link #switchPlayer(Path)} 在多账号登录时把当前存档整体换掉（先保存、再换、后重载）；</li>
 *   <li>保存：写回同一个 JSON 文件（不存在则自动建目录），格式与 {@link PlayerProfile} 字段一一对应；</li>
 *   <li>业务动作：{@link #addExp}、{@link #recordWin(int)}、{@link #recordLoss(int)}。</li>
 * </ul>
 *
 * <p><b>金币不在这里。</b>金币的唯一入口是 {@link CoinService}：{@link PlayerProfile#setGold(int)}
 * 已降为包内可见，本类也<b>不再提供</b>任何加/扣金币的方法，
 * 保证全工程只有 CoinService 一个地方能改余额、并且改余额时一定会写流水、一定落盘。
 *
 * <p>所有改动都在同一个实例上加锁，并且默认在改动后立即落盘（{@link #setAutoSave} 可关）。
 * 这样任何界面只需要调一次方法，就同时完成了"改内存"和"持久化"，不会出现两处数据不一致。
 *
 * <p>单例通过 {@link #getInstance()} 获取；如需独立实例（测试隔离）用 {@link #PlayerManager(Path)}。
 */
public final class PlayerManager {

    /** 覆盖存档路径的系统属性，便于测试/多档切换：{@code -Dplayer.data.file=...}。 */
    public static final String DATA_FILE_PROPERTY = "player.data.file";

    private static final String RESOURCE_DIR = "player";
    private static final String DATA_FILE_NAME = "player.json";
    private static final String CLASSPATH_LOCATION = "/player/" + DATA_FILE_NAME;

    private static volatile PlayerManager instance;

    /**
     * 当前存档文件。阶段 16 起不再是常量：多账号登录时由
     * {@link AccountService} 通过 {@link #switchPlayer(Path)} 换成
     * {@code data/players/<username>.json}。
     */
    private Path currentPlayerFile;
    /** 当前玩家档案。切换存档时整体替换，因此不是 final。 */
    private PlayerProfile profile;
    private boolean autoSave = true;

    /** 生产用法：单例，指向工程内的 {@code src/main/resources/player/player.json}。 */
    public static PlayerManager getInstance() {
        PlayerManager local = instance;
        if (local == null) {
            synchronized (PlayerManager.class) {
                local = instance;
                if (local == null) {
                    local = new PlayerManager(resolveDefaultDataFile());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定存档路径的构造器（测试 / 多档位）。构造时即完成一次加载。 */
    public PlayerManager(Path dataFile) {
        this.currentPlayerFile = dataFile;
        this.profile = load();
    }

    // ============================================================= 加载 / 保存

    /** 当前玩家档案（可变对象，直接读即可；改请走本类的方法）。 */
    public PlayerProfile getProfile() {
        return profile;
    }

    /** 当前存档文件路径（随账号切换而变化）。 */
    public Path getDataFile() {
        return currentPlayerFile;
    }

    /**
     * 切换当前玩家存档文件（阶段 16：多账号）。
     *
     * <p>顺序严格是"先保存、再换路径、最后重新载入"：
     * <ol>
     *   <li>{@link #save()}：把旧账号的改动落盘，绝不丢数据；</li>
     *   <li>换 {@link #currentPlayerFile}；</li>
     *   <li>重新 {@link #load()}，并把新档写回内存；</li>
     *   <li>目标文件不存在时<b>直接建一份全新默认档</b>（不做 classpath 回退，
     *       否则新账号会读到打包进 resources 的老存档）。</li>
     * </ol>
     *
     * <p>所有服务都只持有本单例、并实时调用 {@link #getProfile()}，
     * 因此换档后它们自动跟随，无需逐个重启。
     *
     * @param file 目标存档文件；null 或与当前相同则不做任何事
     */
    public synchronized void switchPlayer(Path file) {
        if (file == null || file.equals(currentPlayerFile)) {
            return;
        }
        save();
        this.currentPlayerFile = file;
        if (Files.isRegularFile(file)) {
            this.profile = load();
        } else {
            this.profile = new PlayerProfile();
            save();
        }
    }

    /** 是否在每次改动后自动落盘，默认 true。 */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    /**
     * 加载玩家数据：文件系统存档 → classpath 存档 → 全新默认档。
     * 解析失败不抛出，只记一条警告并回退默认档，保证界面永远能起来。
     */
    private PlayerProfile load() {
        if (currentPlayerFile != null && Files.isRegularFile(currentPlayerFile)) {
            try (InputStream in = Files.newInputStream(currentPlayerFile)) {
                PlayerProfile loaded = JsonCodec.mapper().readValue(in, PlayerProfile.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                warn("读取存档失败，改用 classpath / 默认档: " + currentPlayerFile + " -> " + e.getMessage());
            }
        }

        try (InputStream in = PlayerManager.class.getResourceAsStream(CLASSPATH_LOCATION)) {
            if (in != null) {
                PlayerProfile loaded = JsonCodec.mapper().readValue(in, PlayerProfile.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            warn("读取 classpath 存档失败: " + CLASSPATH_LOCATION + " -> " + e.getMessage());
        }

        return new PlayerProfile();
    }

    /** 保存玩家数据。返回是否写入成功（失败只记警告，不打断玩法）。 */
    public synchronized boolean save() {
        if (currentPlayerFile == null) {
            return false;
        }
        try {
            Path parent = currentPlayerFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonCodec.mapper().writerWithDefaultPrettyPrinter().writeValue(currentPlayerFile.toFile(), profile);
            return true;
        } catch (IOException e) {
            warn("保存存档失败: " + currentPlayerFile + " -> " + e.getMessage());
            return false;
        }
    }

    /** 关掉 autoSave 时手动落盘用的别名，语义更直白。 */
    public synchronized boolean flush() {
        return save();
    }

    // ============================================================= 钻石
    // 这里<b>故意没有</b>金币方法：金币的唯一入口是 CoinService（见 {@link CoinService}），
    // 本类不再提供任何"发金币 / 扣金币"的入口，避免出现两个能改余额的地方。

    /** 增加钻石（金币之外的第二货币，先留接口）。 */
    public synchronized int addDiamond(int amount) {
        if (amount <= 0) {
            return profile.getDiamond();
        }
        profile.setDiamond(saturatingAdd(profile.getDiamond(), amount));
        afterChange();
        return profile.getDiamond();
    }

    // ============================================================= 经验 / 等级

    /**
     * 升到下一级所需经验：{@code 100 + level * 100}（Lv.1 需 200，Lv.12 需 1300）。
     * 经验是"当前等级已累积的经验"，升级时扣除对应额度，不做跨级累加。
     */
    public static int expToNextLevel(int level) {
        return 100 + Math.max(PlayerProfile.MIN_LEVEL, level) * 100;
    }

    /**
     * 增加经验，按 {@link #expToNextLevel(int)} 自动连升多级。
     *
     * @return 本次提升的等级数（0 表示没升级）
     */
    public synchronized int addExp(int amount) {
        if (amount <= 0) {
            return 0;
        }
        profile.setExp(saturatingAdd(profile.getExp(), amount));
        int gained = 0;
        while (profile.getExp() >= expToNextLevel(profile.getLevel())) {
            profile.setExp(profile.getExp() - expToNextLevel(profile.getLevel()));
            profile.setLevel(profile.getLevel() + 1);
            gained++;
        }
        afterChange();
        return gained;
    }

    // ============================================================= 胜负统计

    /** 记一胜（不含金币结算）。返回当前胜场。 */
    public synchronized int recordWin() {
        profile.setTotalWins(profile.getTotalWins() + 1);
        afterChange();
        return profile.getTotalWins();
    }

    /** 记一负（不含金币结算）。返回当前负场。 */
    public synchronized int recordLoss() {
        profile.setTotalLosses(profile.getTotalLosses() + 1);
        afterChange();
        return profile.getTotalLosses();
    }

    /**
     * 结算一局的"胜"：胜场 +1、加经验。
     *
     * <p>金币不含在内——输赢的金币请另外调用
     * {@link CoinService#addGold(int, String)} / {@link CoinService#costGold(int, String)}。
     *
     * @param rewardExp 经验，&lt;=0 表示不加
     * @return 本次提升的等级数
     */
    public synchronized int recordWin(int rewardExp) {
        profile.setTotalWins(profile.getTotalWins() + 1);
        int levels = applyRewards(rewardExp);
        afterChange();
        return levels;
    }

    /**
     * 结算一局的"负"：负场 +1、加经验（经验通常给少一些）。
     *
     * <p>金币不含在内——扣的金币请另外调用 {@link CoinService#costGold(int, String)}，
     * 由它负责"余额不足则拒绝"和写流水。
     *
     * @param rewardExp 经验，&lt;=0 表示不加
     * @return 本次提升的等级数
     */
    public synchronized int recordLoss(int rewardExp) {
        profile.setTotalLosses(profile.getTotalLosses() + 1);
        int levels = applyRewards(rewardExp);
        afterChange();
        return levels;
    }

    // ============================================================= 内部工具

    /** 加经验并结算升级，不加锁，由调用方持锁。 */
    private int applyRewards(int exp) {
        if (exp <= 0) {
            return 0;
        }
        profile.setExp(saturatingAdd(profile.getExp(), exp));
        int levels = 0;
        while (profile.getExp() >= expToNextLevel(profile.getLevel())) {
            profile.setExp(profile.getExp() - expToNextLevel(profile.getLevel()));
            profile.setLevel(profile.getLevel() + 1);
            levels++;
        }
        return levels;
    }

    private void afterChange() {
        if (autoSave) {
            save();
        }
    }

    /** 防止 int 溢出成负数。 */
    private static int saturatingAdd(int current, int delta) {
        long sum = (long) current + delta;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    /**
     * 解析默认存档路径：
     * <ol>
     *   <li>系统属性 {@code -Dplayer.data.file=...}；</li>
     *   <li>从当前工作目录逐级向上找含 {@code src/main/resources} 的工程根，取其下 {@code player/player.json}；</li>
     *   <li>都找不到时退回工作目录下的 {@code player.json}。</li>
     * </ol>
     */
    static Path resolveDefaultDataFile() {
        String override = System.getProperty(DATA_FILE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        Path dir = Paths.get("").toAbsolutePath();
        for (Path cursor = dir; cursor != null; cursor = cursor.getParent()) {
            if (Files.isDirectory(cursor.resolve("src").resolve("main").resolve("resources"))) {
                return cursor.resolve("src").resolve("main").resolve("resources")
                        .resolve(RESOURCE_DIR).resolve(DATA_FILE_NAME);
            }
        }
        return dir.resolve(DATA_FILE_NAME);
    }

    private static void warn(String message) {
        System.err.println("[PlayerManager] " + message);
    }
}
