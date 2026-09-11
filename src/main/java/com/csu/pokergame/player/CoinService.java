package com.csu.pokergame.player;

import com.csu.pokergame.network.JsonCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 棋牌大厅统一金币服务层（阶段 2）。
 *
 * <p>本类是<b>全工程唯一允许改动金币的地方</b>。{@link PlayerProfile#setGold(int)} 已降为包内可见，
 * 其他包（例如 {@code com.cards.*} 的界面层）在编译期就无法直接改金币，只能走：
 * <pre>{@code
 * CoinService coins = CoinService.getInstance();
 * coins.getGold();                       // 查询
 * coins.addGold(100, "跑得快胜利奖励");    // 增加
 * coins.costGold(50, "骗子酒馆入场费");    // 消耗
 * }</pre>
 *
 * <p>每次改动都会做三件事，缺一不可：
 * <ol>
 *   <li>改内存中的 {@link PlayerProfile#getGold()}；</li>
 *   <li>追加一条流水到 {@code coin_log.json}；</li>
 *   <li>落盘 {@code player.json}。</li>
 * </ol>
 * 因此流水和余额永远对得上：把流水里所有 {@code change} 累加，再加上初始金币就是当前余额。
 *
 * <p>流水文件与 {@code player.json} 同目录（默认都在 {@code src/main/resources/player/} 下），
 * 可用系统属性 {@code -Dcoin.log.file=...} 覆盖，便于测试隔离。
 *
 * <p>本类<b>不涉及</b>任何跑得快 / 骗子酒馆的规则，只提供金币原子操作；
 * 具体"赢了加多少、输了扣多少"由各游戏的结算逻辑在调用时传入。
 */
public final class CoinService {

    /** 覆盖流水文件路径的系统属性：{@code -Dcoin.log.file=...}。 */
    public static final String LOG_FILE_PROPERTY = "coin.log.file";

    private static final String LOG_FILE_NAME = "coin_log.json";
    private static final String CLASSPATH_LOCATION = "/player/" + LOG_FILE_NAME;
    private static final String DEFAULT_REASON = "未注明";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile CoinService instance;

    private final PlayerManager players;
    /** 当前流水文件。阶段 16 起不再是常量：多账号登录时由 {@link AccountService} 切换。 */
    private Path logFile;
    private final List<CoinLogEntry> log = new ArrayList<>();

    /**
     * 一条金币流水。
     *
     * @param time   发生时间，格式 {@code yyyy-MM-dd HH:mm:ss}
     * @param change 变动值，正数为收入、负数为支出（永远不带 0）
     * @param reason 变动原因
     */
    public record CoinLogEntry(String time, int change, String reason) {
    }

    /** 生产用法：单例，流动文件与 {@code player.json} 同目录。 */
    public static CoinService getInstance() {
        CoinService local = instance;
        if (local == null) {
            synchronized (CoinService.class) {
                local = instance;
                if (local == null) {
                    local = new CoinService(PlayerManager.getInstance(), resolveDefaultLogFile());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定流水文件（测试 / 多档位）。构造时即完成一次流水加载。 */
    public CoinService(PlayerManager players, Path logFile) {
        this.players = players;
        this.logFile = logFile;
        this.log.addAll(loadLog());
    }

    // ============================================================= 查询

    /** 当前金币余额。 */
    public synchronized int getGold() {
        return players.getProfile().getGold();
    }

    /**
     * 累计获得金币总量：历史上所有 {@link #addGold(int, String)} 的收入之和（初始金币不计入）。
     * 写入方同样是本服务（{@code addGold} 内累加），因此"读取"也统一从这里出，
     * 界面不需要直接访问 {@link PlayerProfile} 的累计字段。
     */
    public synchronized int getTotalEarned() {
        return players.getProfile().getTotalGoldEarned();
    }

    /** 累计消耗金币总量：历史上所有 {@link #costGold(int, String)} 的支出之和。 */
    public synchronized int getTotalSpent() {
        return players.getProfile().getTotalGoldSpent();
    }

    /** 金币是否够付 {@code amount}；{@code amount <= 0} 视为够（无需支付）。 */
    public synchronized boolean canAfford(int amount) {
        return amount <= 0 || players.getProfile().getGold() >= amount;
    }

    /** 流水文件的只读快照（按发生顺序，最早在前）。 */
    public synchronized List<CoinLogEntry> getLog() {
        return Collections.unmodifiableList(new ArrayList<>(log));
    }

    /** 流水文件路径。 */
    public Path getLogFile() {
        return logFile;
    }

    /**
     * 切换流水文件（阶段 16：多账号）。
     *
     * <p>每个账号有自己的存档，也就必须有自己的流水；否则 B 账号会看到 A 账号的收支记录。
     * 切换时把内存里的流水整体换成目标文件的内容：
     * <ul>
     *   <li>目标文件已存在：直接读入；</li>
     *   <li>目标文件不存在：<b>立即生成一个空流水文件</b>再开始记账——
     *       这一步不能省，否则读流水时会落到 classpath 上打包的老 {@code coin_log.json}，
     *       新账号会莫名其妙看到旧流水。</li>
     * </ul>
     * 本方法只影响"流水读写的落点"，{@link #addGold(int, String)} / {@link #costGold(int, String)}
     * 的语义与调用方式完全不变。
     *
     * @param file 目标流水文件；null 表示只清空内存流水
     */
    public synchronized void switchLogFile(Path file) {
        this.logFile = file;
        log.clear();
        if (file == null) {
            return;
        }
        if (Files.isRegularFile(file)) {
            log.addAll(loadLog());
        } else {
            saveLog();
        }
    }

    // ============================================================= 增加 / 消耗

    /**
     * 增加金币。
     *
     * @param amount 增加量，必须 &gt; 0，否则拒绝
     * @param reason 原因，写入流水
     * @return 是否成功（{@code amount <= 0} 返回 false）
     */
    public synchronized boolean addGold(int amount, String reason) {
        if (amount <= 0) {
            return false;
        }
        PlayerProfile profile = players.getProfile();
        profile.setGold(saturatingAdd(profile.getGold(), amount));
        // 统计：累计获得金币（含游戏奖励、升级奖励等一切收入），随 persist 一起落盘
        profile.setTotalGoldEarned(saturatingAdd(profile.getTotalGoldEarned(), amount));
        persist(amount, reason);
        // 阶段 21：金币增加成功 → 播放金币音效（无音频资源时 AudioService 静默跳过）
        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(com.csu.pokergame.audio.SoundEffect.COIN_GAIN);
        return true;
    }

    /**
     * 消耗金币。余额不足时<b>不改动任何数据、不写流水</b>并返回 false。
     *
     * @param amount 消耗量，必须 &gt; 0，否则拒绝
     * @param reason 原因，写入流水
     * @return 是否扣款成功
     */
    public synchronized boolean costGold(int amount, String reason) {
        if (amount <= 0) {
            return false;
        }
        PlayerProfile profile = players.getProfile();
        if (profile.getGold() < amount) {
            return false;
        }
        profile.setGold(profile.getGold() - amount);
        // 统计：累计消耗金币（入场费等一切支出），随 persist 一起落盘
        profile.setTotalGoldSpent(saturatingAdd(profile.getTotalGoldSpent(), amount));
        persist(-amount, reason);
        return true;
    }

    // ============================================================= 内部

    /** 追加流水 + 保存 player.json。调用方必须已持锁。 */
    private void persist(int change, String reason) {
        log.add(new CoinLogEntry(LocalDateTime.now().format(TIME_FORMAT), change,
                reason == null || reason.isBlank() ? DEFAULT_REASON : reason.trim()));
        saveLog();
        // setGold 绕过了 PlayerManager 的自动保存，这里必须显式落盘余额
        players.save();
    }

    /** 读流水：文件 → classpath → 空表。解析失败不抛出，避免打不开大厅。 */
    private List<CoinLogEntry> loadLog() {
        if (logFile != null && Files.isRegularFile(logFile)) {
            try (InputStream in = Files.newInputStream(logFile)) {
                List<CoinLogEntry> loaded = readEntries(in);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                warn("读取流水失败，改从 classpath / 空表开始: " + logFile + " -> " + e.getMessage());
            }
        }
        try (InputStream in = CoinService.class.getResourceAsStream(CLASSPATH_LOCATION)) {
            if (in != null) {
                List<CoinLogEntry> loaded = readEntries(in);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            warn("读取 classpath 流水失败: " + CLASSPATH_LOCATION + " -> " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private static List<CoinLogEntry> readEntries(InputStream in) throws IOException {
        return JsonCodec.mapper().readValue(in, JsonCodec.mapper()
                .getTypeFactory().constructCollectionType(List.class, CoinLogEntry.class));
    }

    /** 写流水（全量覆盖，保持与内存一致）。 */
    private void saveLog() {
        if (logFile == null) {
            return;
        }
        try {
            Path parent = logFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonCodec.mapper().writerWithDefaultPrettyPrinter().writeValue(logFile.toFile(), log);
        } catch (IOException e) {
            warn("保存流水失败: " + logFile + " -> " + e.getMessage());
        }
    }

    /** 防止 int 溢出成负数。 */
    private static int saturatingAdd(int current, int delta) {
        long sum = (long) current + delta;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    /**
     * 解析默认流水路径：系统属性 → {@code player.json} 同目录 → 工作目录。
     * 与 {@link PlayerManager} 的存档放在一起，方便整个「玩家数据」一起备份 / 迁移。
     */
    static Path resolveDefaultLogFile() {
        String override = System.getProperty(LOG_FILE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        Path dataFile = PlayerManager.getInstance().getDataFile();
        if (dataFile != null && dataFile.getParent() != null) {
            return dataFile.getParent().resolve(LOG_FILE_NAME);
        }
        return Paths.get("").toAbsolutePath().resolve(LOG_FILE_NAME);
    }

    private static void warn(String message) {
        System.err.println("[CoinService] " + message);
    }
}
