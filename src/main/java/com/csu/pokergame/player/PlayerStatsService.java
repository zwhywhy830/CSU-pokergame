package com.csu.pokergame.player;

import java.util.Objects;

/**
 * 玩家战绩统计服务（阶段 10 / 12）：胜场 / 负场 / 胜率 / 连胜的唯一入口。
 *
 * <p>统计口径（全部从 {@link PlayerProfile} 实时读出，本类<b>不缓存副本</b>）：
 * <ul>
 *   <li>{@code winGames} = {@link PlayerProfile#getTotalWins()}；</li>
 *   <li>{@code loseGames} = {@link PlayerProfile#getTotalLosses()}；</li>
 *   <li>{@code totalGames} = 胜 + 负（派生）；</li>
 *   <li>{@code winRate} = 胜 / 总场次，取值 [0,1]（派生）；</li>
 *   <li>{@code maxWinStreak} = 历史最高连胜（落盘字段）。</li>
 * </ul>
 *
 * <p><b>数据写入 {@code player.json}</b>：胜场 / 负场沿用 {@link PlayerManager} 的场次入口，
 * 连胜（{@code currentWinStreak} / {@code maxWinStreak}）写 {@link PlayerProfile} 的包内 setter，
 * 每次记录后立即落盘，因此存档里始终有完整的战绩数据。
 *
 * <p><b>分工边界：</b>{@link PlayerManager} 只管"胜场 +1 / 负场 +1"这一个原子动作；
 * "连胜怎么算、什么算一局"由本服务编排，全工程只有这里能改写连胜，
 * 避免出现第二条战绩计数路径导致数据漂移。
 *
 * <p>单例通过 {@link #getInstance()} 获取；测试 / 多档位可用 {@link #PlayerStatsService(PlayerManager)}。
 */
public final class PlayerStatsService {

    private static volatile PlayerStatsService instance;

    private final PlayerManager players;

    /** 生产用法：单例，复用 {@link PlayerManager} 的单例。 */
    public static PlayerStatsService getInstance() {
        PlayerStatsService local = instance;
        if (local == null) {
            synchronized (PlayerStatsService.class) {
                local = instance;
                if (local == null) {
                    local = new PlayerStatsService(PlayerManager.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定依赖的构造器（测试 / 独立档位）。 */
    public PlayerStatsService(PlayerManager players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    // ============================================================= 记录

    /**
     * 记录一局结果（阶段 12 新增，供牌局结算统一调用）：{@code win} 为 true 记一胜，否则记一负。
     *
     * <p>这是结算层调用的<b>唯一入口</b>，内部转发给 {@link #recordWin()} / {@link #recordLoss()}，
     * 无论胜负都会立即落盘 {@code player.json}，保证统计不丢。
     *
     * @param win 本局是否获胜
     * @return 记录后的战绩快照
     */
    public synchronized StatsSnapshot recordGameResult(boolean win) {
        return win ? recordWin() : recordLoss();
    }

    /**
     * 记一胜：胜场 +1、当前连胜 +1，并刷新历史最高连胜，随后落盘 {@code player.json}。
     *
     * @return 记录后的战绩快照
     */
    public synchronized StatsSnapshot recordWin() {
        // 胜场计数的唯一入口是 PlayerManager（它自己也会落盘一次）
        players.recordWin();
        PlayerProfile profile = players.getProfile();
        int streak = saturatingAdd(profile.getCurrentWinStreak(), 1);
        profile.setCurrentWinStreak(streak);
        if (streak > profile.getMaxWinStreak()) {
            profile.setMaxWinStreak(streak);
        }
        players.save();
        return snapshot();
    }

    /**
     * 记一负：负场 +1、当前连胜清零（历史最高连胜保留），随后落盘 {@code player.json}。
     *
     * @return 记录后的战绩快照
     */
    public synchronized StatsSnapshot recordLoss() {
        players.recordLoss();
        players.getProfile().setCurrentWinStreak(0);
        players.save();
        return snapshot();
    }

    // ============================================================= 查询

    /** 总场次 = 胜 + 负。 */
    public synchronized int getTotalGames() {
        PlayerProfile profile = players.getProfile();
        return profile.getTotalWins() + profile.getTotalLosses();
    }

    /** 胜场。 */
    public synchronized int getWinGames() {
        return players.getProfile().getTotalWins();
    }

    /** 负场。 */
    public synchronized int getLoseGames() {
        return players.getProfile().getTotalLosses();
    }

    /** 胜率，取值 [0,1]；未打过任何一局时返回 0。 */
    public synchronized double getWinRate() {
        int total = getTotalGames();
        return total == 0 ? 0.0 : (double) getWinGames() / total;
    }

    /** 胜率百分数（四舍五入取整，0 ~ 100），便于界面直接显示 {@code "63%"}。 */
    public synchronized int getWinRatePercent() {
        return (int) Math.round(getWinRate() * 100);
    }

    /** 当前连胜场次。 */
    public synchronized int getCurrentWinStreak() {
        return players.getProfile().getCurrentWinStreak();
    }

    /** 历史最高连胜场次。 */
    public synchronized int getMaxWinStreak() {
        return players.getProfile().getMaxWinStreak();
    }

    /** 一次性取出全部战绩（界面刷新用，避免多次加锁读数不一致）。 */
    public synchronized StatsSnapshot snapshot() {
        PlayerProfile profile = players.getProfile();
        int win = profile.getTotalWins();
        int lose = profile.getTotalLosses();
        int total = win + lose;
        return new StatsSnapshot(total, win, lose,
                total == 0 ? 0.0 : (double) win / total,
                profile.getCurrentWinStreak(), profile.getMaxWinStreak());
    }

    /** 防止 int 溢出成负数。 */
    private static int saturatingAdd(int current, int delta) {
        long sum = (long) current + delta;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    // ============================================================= 战绩快照

    /**
     * 一次读取到的战绩快照（不可变），供界面一次性刷新。
     *
     * @param totalGames      总场次
     * @param winGames        胜场
     * @param loseGames       负场
     * @param winRate         胜率，[0,1]
     * @param currentWinStreak 当前连胜
     * @param maxWinStreak    历史最高连胜
     */
    public record StatsSnapshot(int totalGames, int winGames, int loseGames,
                                double winRate, int currentWinStreak, int maxWinStreak) {

        /** 胜率百分数（四舍五入取整）。 */
        public int winRatePercent() {
            return (int) Math.round(winRate * 100);
        }
    }
}
