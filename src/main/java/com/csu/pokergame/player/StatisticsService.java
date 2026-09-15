package com.csu.pokergame.player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家数据统计中心服务（阶段 19）：把"战绩明细 + 金币累计"实时汇总成 {@link PlayerStatistics}。
 *
 * <p><b>职责（只读、不落盘）：</b>本类<b>不新增任何持久化字段</b>，也不保存统计结果，
 * 每次调用都从已有数据现算，因此永远不会出现"统计数字和真实数据对不上"的情况：
 * <pre>
 *   GameRecordService  →  records 文件：总场次 / 胜 / 负 / 连胜 / 最常玩玩法
 *   CoinService        →  累计获得 / 累计消耗金币
 *   PlayerProfile      →  服务缺失时的兜底口径（见下）
 * </pre>
 *
 * <p><b>统计口径：</b>
 * <ul>
 *   <li>总场次 / 胜 / 负：直接取 {@link GameRecordService#getTotalGames()} 等；</li>
 *   <li>胜率：{@code winCount / totalGames}，无对局时为 {@code 0}（不是 {@code NaN}）；</li>
 *   <li>连胜：把记录按时间升序（最旧 → 最新）遍历，遇到"胜"累加、遇到"负"清零，
 *       过程中的最大值即 {@code maxWinStreak}，遍历结束时的值即 {@code currentWinStreak}；</li>
 *   <li>最常玩玩法：按 {@code gameType} 计数取最多的那个（并列时取先出现的），返回展示名；</li>
 *   <li>金币：累计收入 / 支出取 {@link CoinService#getTotalEarned()} / {@link CoinService#getTotalSpent()}，
 *       它们是 {@link PlayerProfile} 的累计字段，本类<b>不新增金币字段</b>。</li>
 * </ul>
 *
 * <p><b>兜底：</b>{@link GameRecordService} / {@link CoinService} 为 {@code null} 时（极端装配场景），
 * 对应维度退回 {@link PlayerProfile} 的汇总字段，保证界面不崩、数字不空。
 * 正常情况下（战绩文件存在）以 records 为唯一权威口径。
 *
 * <p>本类不修改金币 / 战绩 / 等级，也不写任何文件；纯粹是"读取 + 汇总"。
 */
public final class StatisticsService {

    /** 玩法代码 → 展示名：兼容未来把 {@code gameType} 写成枚举名的情况。 */
    private static final Map<String, String> GAME_LABELS = Map.of(
            "PDK", "跑得快",
            "PAO_DE_KUAI", "跑得快",
            "LIAR", "骗子酒馆",
            "LIARS_POKER", "骗子酒馆");

    private static volatile StatisticsService instance;

    private final GameRecordService records;
    private final CoinService coins;
    /** 玩家档案（兜底口径）：服务缺失时用它的汇总字段。 */
    private final PlayerManager players;

    /** 生产用法：单例，复用三个全局单例服务。 */
    public static StatisticsService getInstance() {
        StatisticsService local = instance;
        if (local == null) {
            synchronized (StatisticsService.class) {
                local = instance;
                if (local == null) {
                    local = new StatisticsService(GameRecordService.getInstance(),
                            CoinService.getInstance(), PlayerManager.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * 指定依赖的构造器（测试 / 多档位）。
     *
     * @param records 战绩明细服务（可为 null，走档案兜底）
     * @param coins   金币服务（可为 null，走档案兜底）
     * @param players 玩家档案管理器（兜底口径来源，可为 null）
     */
    public StatisticsService(GameRecordService records, CoinService coins, PlayerManager players) {
        this.records = records;
        this.coins = coins;
        this.players = players;
    }

    // ============================================================= 对外接口

    /**
     * 实时汇总当前玩家的统计数据。
     *
     * <p>每次调用都重新读取 + 计算，不做任何缓存，调用方拿到的永远是当前值。
     *
     * @return 统计数据快照（不可变语义，调用方修改不到内部状态）
     */
    public PlayerStatistics getStatistics() {
        List<GameRecord> all = records == null ? List.of() : records.getRecords();

        int total;
        int win;
        int maxStreak;
        int currentStreak;
        String favorite;

        if (records == null) {
            // 兜底：没有战绩服务时退回档案里的汇总胜负（连胜取档案值）
            PlayerProfile profile = profileOrNull();
            win = profile == null ? 0 : profile.getTotalWins();
            int lose = profile == null ? 0 : profile.getTotalLosses();
            total = win + lose;
            maxStreak = profile == null ? 0 : profile.getMaxWinStreak();
            currentStreak = profile == null ? 0 : profile.getCurrentWinStreak();
            favorite = null;
        } else {
            total = all.size();
            win = 0;
            int run = 0;
            maxStreak = 0;
            Map<String, Integer> typeCount = new LinkedHashMap<>();
            // 记录按时间升序：遍历过程中的"连胜游程"即口径
            for (GameRecord record : all) {
                if (record.isWin()) {
                    win++;
                    run++;
                    if (run > maxStreak) {
                        maxStreak = run;
                    }
                } else {
                    run = 0;
                }
                String label = normalizeGame(record.getGameType());
                if (label != null) {
                    typeCount.merge(label, 1, Integer::sum);
                }
            }
            currentStreak = run;
            favorite = favorite(typeCount);
        }

        int lose = Math.max(0, total - win);
        double winRate = total == 0 ? 0.0 : (double) win / total;
        int earned = coins == null ? fallbackEarned() : coins.getTotalEarned();
        int spent = coins == null ? fallbackSpent() : coins.getTotalSpent();

        return new PlayerStatistics(total, win, lose, winRate, maxStreak, currentStreak,
                earned, spent, favorite);
    }

    /** 胜率，取值 [0,1]；未打过任何一局时返回 0。 */
    public double getWinRate() {
        return getStatistics().getWinRate();
    }

    /** 历史最高连胜。 */
    public int getMaxWinStreak() {
        return getStatistics().getMaxWinStreak();
    }

    /** 最常玩玩的玩法展示名；无记录时返回 {@link PlayerStatistics#NO_GAME}。 */
    public String getFavoriteGame() {
        return getStatistics().getFavoriteGameText();
    }

    /** 当前连胜（界面直接展示用）。 */
    public int getCurrentWinStreak() {
        return getStatistics().getCurrentWinStreak();
    }

    /** 胜率百分数（四舍五入取整），便于界面直接拼 {@code "%"}。 */
    public int getWinRatePercent() {
        return getStatistics().getWinRatePercent();
    }

    // ============================================================= 内部

    /** 玩法名归一：已知代码映射为展示名，其余原样返回（空白返回 null）。 */
    private static String normalizeGame(String gameType) {
        if (gameType == null || gameType.isBlank()) {
            return null;
        }
        String trimmed = gameType.trim();
        return GAME_LABELS.getOrDefault(trimmed, trimmed);
    }

    /** 计数最多的玩法；并列时取先出现的一个；空表返回 null。 */
    private static String favorite(Map<String, Integer> typeCount) {
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private PlayerProfile profileOrNull() {
        return players == null ? null : players.getProfile();
    }

    private int fallbackEarned() {
        PlayerProfile profile = profileOrNull();
        return profile == null ? 0 : profile.getTotalGoldEarned();
    }

    private int fallbackSpent() {
        PlayerProfile profile = profileOrNull();
        return profile == null ? 0 : profile.getTotalGoldSpent();
    }
}
