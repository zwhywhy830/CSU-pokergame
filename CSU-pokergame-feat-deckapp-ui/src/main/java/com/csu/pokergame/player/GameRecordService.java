package com.csu.pokergame.player;

import com.csu.pokergame.network.JsonCodec;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 玩家战绩服务（阶段 17）：单例，战绩明细的唯一读写入口。
 *
 * <p>数据落点与账号绑定：战绩文件由<b>当前存档文件名推导</b>而来——
 * <pre>
 *   PlayerManager.getDataFile()          → data/players/playerA.json
 *   GameRecordService.getRecordsFile()   → data/players/playerA_records.json
 * </pre>
 * 因此账号切换时无需任何人通知本服务：{@link AccountService} 把
 * {@link PlayerManager} 的当前存档换成 {@code players/<username>.json} 之后，
 * 本服务在下一次读写时会发现"推导出的文件变了"，自动清空内存并按新账号的记录文件重新载入。
 * 这既做到了<b>A 账号看不到 B 战绩</b>，也保证本类不侵入账号系统（零改动、零耦合）。
 *
 * <p>文件不存在（老账号、新注册账号）时按<b>空列表</b>处理，并且<b>不主动建文件</b>：
 * 空文件要等到真正打出第一局、写入第一条记录时才落盘，避免浏览个人中心就凭空产生文件。
 *
 * <p>内存中始终保存当前账号的完整记录列表（最新一条在末尾），每次写入立刻整体落盘；
 * 记录量级是"每局一条"，无需分页与索引。
 *
 * <p>本类只负责战绩的存取与统计，不碰金币、经验、成就、账号：
 * {@code CoinService} / {@code PlayerGrowthService} / {@code AchievementService} /
 * {@code AccountService} 的接口与行为完全不变。
 */
public final class GameRecordService {

    /** 战绩文件后缀：{@code playerA.json → playerA_records.json}。 */
    public static final String RECORDS_SUFFIX = "_records.json";
    /** 存档后缀（推导时剥掉）。 */
    private static final String SAVE_SUFFIX = ".json";
    /** 界面默认展示条数：最近 20 条。 */
    public static final int DEFAULT_LIMIT = 20;
    /** 默认玩法名：跑得快。 */
    public static final String GAME_PDK = "跑得快";
    /** 默认对手信息：本地人机。 */
    public static final String OPPONENT_AI = "AI";

    private static volatile GameRecordService instance;

    private final PlayerManager players;
    /** 内存记录，按时间升序（末尾为最新）。 */
    private final List<GameRecord> records = new ArrayList<>();
    /** 当前已载入的记录文件；与推导结果不一致时说明账号变了，需要重新载入。 */
    private Path currentFile;

    /** 单例：绑定全局 {@link PlayerManager}。 */
    public static GameRecordService getInstance() {
        GameRecordService local = instance;
        if (local == null) {
            synchronized (GameRecordService.class) {
                local = instance;
                if (local == null) {
                    local = new GameRecordService(PlayerManager.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定玩家管理器的构造器（测试 / 多档位）。 */
    public GameRecordService(PlayerManager players) {
        this.players = players;
    }

    // ============================================================= 写入

    /**
     * 增加一条战绩（阶段 17 的唯一写入口）。
     *
     * <p>调用方是结算流程：金币结算 → 经验结算 → 本方法，因此传进来的
     * {@code goldChange} / {@code expChange} 就是这一局实际到账的数字。
     *
     * @param gameType     玩法名
     * @param win          是否获胜
     * @param goldChange   本局金币变动
     * @param expChange    本局经验
     * @param opponentInfo 对手信息
     * @return 已落盘的记录（含生成的 id 与时间）
     */
    public synchronized GameRecord addRecord(String gameType, boolean win, int goldChange,
                                            int expChange, String opponentInfo) {
        return addRecord(GameRecord.of(gameType, win, goldChange, expChange, opponentInfo));
    }

    /**
     * 增加一条战绩（外部已构造好 {@link GameRecord} 时使用；缺失的 id / time 自动补齐）。
     *
     * @param record 待写入记录；null 直接忽略
     * @return 已落盘的记录；入参为 null 时返回 null
     */
    public synchronized GameRecord addRecord(GameRecord record) {
        if (record == null) {
            return null;
        }
        ensureLoaded();
        if (record.getTime() <= 0) {
            record.setTime(System.currentTimeMillis());
        }
        if (record.getId() == null || record.getId().isBlank()) {
            record.setId(GameRecord.makeId(record.getTime()));
        }
        record.setId(uniqueId(record.getId()));
        records.add(record);
        save();
        return record;
    }

    /** id 冲突时补 {@code -2 / -3 …} 序号，保证同一秒内的多局也能区分。 */
    private String uniqueId(String base) {
        String id = base;
        int seq = 2;
        while (containsId(id)) {
            id = base + "-" + seq++;
        }
        return id;
    }

    private boolean containsId(String id) {
        for (GameRecord r : records) {
            if (id.equals(r.getId())) {
                return true;
            }
        }
        return false;
    }

    // ============================================================= 查询

    /** 最近 {@code limit} 条记录，<b>最新的在最前面</b>（界面直接用）。 */
    public synchronized List<GameRecord> getRecentRecords(int limit) {
        ensureLoaded();
        if (limit <= 0 || records.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, records.size() - limit);
        List<GameRecord> recent = new ArrayList<>(records.subList(from, records.size()));
        Collections.reverse(recent);
        return Collections.unmodifiableList(recent);
    }

    /** 最近 {@link #DEFAULT_LIMIT} 条记录，最新在前。 */
    public synchronized List<GameRecord> getRecentRecords() {
        return getRecentRecords(DEFAULT_LIMIT);
    }

    /** 当前账号的全部记录（时间升序，返回副本，改不到内存数据）。 */
    public synchronized List<GameRecord> getRecords() {
        ensureLoaded();
        List<GameRecord> copies = new ArrayList<>(records.size());
        for (GameRecord r : records) {
            copies.add(r.copy());
        }
        return Collections.unmodifiableList(copies);
    }

    /** 最近一条记录；没有则返回 null。 */
    public synchronized GameRecord getLatestRecord() {
        ensureLoaded();
        return records.isEmpty() ? null : records.get(records.size() - 1).copy();
    }

    // ============================================================= 统计

    /** 总场次。 */
    public synchronized int getTotalGames() {
        ensureLoaded();
        return records.size();
    }

    /** 胜利场次。 */
    public synchronized int getWinCount() {
        ensureLoaded();
        int count = 0;
        for (GameRecord r : records) {
            if (r.isWin()) {
                count++;
            }
        }
        return count;
    }

    /** 失败场次。 */
    public synchronized int getLossCount() {
        return getTotalGames() - getWinCount();
    }

    /**
     * 胜率，取值 {@code 0.0 ~ 1.0}。
     * 没有对局时返回 {@code 0.0}（而不是 NaN），避免界面出现 {@code NaN%}。
     */
    public synchronized double getWinRate() {
        int total = getTotalGames();
        return total == 0 ? 0.0 : (double) getWinCount() / total;
    }

    /** 胜率百分数（四舍五入取整），例如 7 胜 3 败 → {@code 70}。 */
    public synchronized int getWinRatePercent() {
        return (int) Math.round(getWinRate() * 100);
    }

    /** 当前账号的战绩文件路径（账号切换后自动指向新账号的文件）。 */
    public synchronized Path getRecordsFile() {
        ensureLoaded();
        return currentFile;
    }

    // ============================================================= 载入 / 落盘

    /**
     * 保证内存记录与"当前账号的记录文件"一致。
     *
     * <p>每次读写前调用：推导路径与上次不同 → 说明换了账号（登录 / 切换账号），
     * 清内存后按新文件重新载入。文件不存在时保持空列表，不做 classpath 回退，
     * 否则新账号会读到打包进 resources 的老战绩。
     */
    private void ensureLoaded() {
        Path file = resolveRecordsFile();
        if (Objects.equals(file, currentFile)) {
            return;
        }
        currentFile = file;
        records.clear();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        records.addAll(load(file));
    }

    /**
     * 由当前存档路径推导战绩文件：{@code players/playerA.json → players/playerA_records.json}。
     *
     * <p>这就是"战绩跟账号绑定"的全部实现——不新增任何路径配置，
     * 也不依赖账号系统主动通知。
     */
    private Path resolveRecordsFile() {
        Path playerFile = players == null ? null : players.getDataFile();
        if (playerFile == null) {
            return null;
        }
        Path fileName = playerFile.getFileName();
        if (fileName == null) {
            return null;
        }
        String name = fileName.toString();
        String base = name.endsWith(SAVE_SUFFIX)
                ? name.substring(0, name.length() - SAVE_SUFFIX.length())
                : name;
        return playerFile.resolveSibling(base + RECORDS_SUFFIX);
    }

    /** 读取战绩文件；解析失败只记日志并当作空列表，绝不影响登录与游戏。 */
    private List<GameRecord> load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            List<GameRecord> loaded = JsonCodec.mapper().readValue(in, JsonCodec.mapper()
                    .getTypeFactory().constructCollectionType(List.class, GameRecord.class));
            return loaded == null ? List.of() : loaded;
        } catch (Exception e) {
            warn("读取战绩失败，按空列表处理: " + file + " -> " + e.getMessage());
            return List.of();
        }
    }

    /** 整体落盘；返回是否成功。 */
    private boolean save() {
        if (currentFile == null) {
            return false;
        }
        try {
            Path parent = currentFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonCodec.mapper().writerWithDefaultPrettyPrinter().writeValue(currentFile.toFile(), records);
            return true;
        } catch (Exception e) {
            warn("保存战绩失败: " + currentFile + " -> " + e.getMessage());
            return false;
        }
    }

    private static void warn(String message) {
        System.out.println("[GameRecord] " + message);
    }
}
