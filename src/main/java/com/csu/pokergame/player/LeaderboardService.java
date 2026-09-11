package com.csu.pokergame.player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 排行榜服务（阶段 18）：把"全部账号的玩家数据"汇总成可排序的榜单。
 *
 * <p><b>数据来源</b>（只读，不写任何文件）：
 * <pre>
 *   AccountService.getAccounts()          → accounts.json 里的账号表
 *   AccountService.resolvePlayerFile(acc) → data/players/&lt;username&gt;.json
 *   new PlayerManager(playerFile)         → 临时实例读出该账号的 PlayerProfile
 * </pre>
 *
 * <p><b>为什么用临时 {@link PlayerManager}：</b>排行榜要读"别人"的存档，但当前登录玩家正被
 * 全局单例 {@link PlayerManager#getInstance()} 持有。<b>绝不能</b>用单例去 {@code switchPlayer}
 * 别人的档——那会把当前玩家踢下线、还会把内存里的数据写错文件。因此这里对每个账号
 * {@code new PlayerManager(file)} 建一个一次性实例，读完即弃：<b>全局单例分毫不动</b>，
 * 当前玩家的金币 / 等级 / 战绩在排行榜刷新前后完全一致。
 *
 * <p><b>不缓存：</b>每次调用都重新读盘。数据量是"一个账号一个文件"，很小；换来的是
 * "切换账号后榜单立刻正确""打完一局立刻反映最新战绩"，不会出现缓存过期导致的排名错乱。
 *
 * <p><b>榜单口径</b>：
 * <ul>
 *   <li>{@link #getRankByGold()}：{@code gold} 降序；</li>
 *   <li>{@link #getRankByLevel()}：{@code level} 降序；</li>
 *   <li>{@link #getRankByWinRate()}：{@code winRate} 降序，<b>且只在
 *       {@link #MIN_GAMES_FOR_WIN_RATE} 局以上的账号之间比较</b>（局数太少胜率没有参考价值）；</li>
 *   <li>{@link #getRankByWins()}：{@code winCount} 降序。</li>
 * </ul>
 * 每个榜单都带稳定的次级排序（同分时比较另一个维度，再同分按账号名升序），
 * 保证同样的数据每次刷新顺序一致、不会在界面上"跳来跳去"。
 *
 * <p>本类不修改金币 / 等级 / 战绩 / 账号，也不写盘，纯粹是"读取 + 排序"。
 */
public final class LeaderboardService {

    /** 进入胜率榜的最低场次要求（含）。局数不足的账号不出现在胜率榜。 */
    public static final int MIN_GAMES_FOR_WIN_RATE = 10;
    /** 界面默认展示条数。 */
    public static final int DEFAULT_LIMIT = 50;

    /** 榜单种类：金币 / 等级 / 胜率 / 胜场。 */
    public enum Board {
        /** 金币榜：{@code gold} 降序。 */
        GOLD("金币榜"),
        /** 等级榜：{@code level} 降序。 */
        LEVEL("等级榜"),
        /** 胜率榜：{@code winRate} 降序，需 {@link #MIN_GAMES_FOR_WIN_RATE} 局以上。 */
        WIN_RATE("胜率榜"),
        /** 胜场榜：{@code winCount} 降序。 */
        WINS("胜场榜");

        private final String label;

        Board(String label) {
            this.label = label;
        }

        /** 界面展示名。 */
        public String getLabel() {
            return label;
        }
    }

    // ---------------- 各榜单排序器（降序 + 稳定次级排序） ----------------

    /** 金币榜：金币 ↓，同分等级 ↓，再同分账号名 ↑。 */
    private static final Comparator<LeaderboardEntry> BY_GOLD =
            Comparator.comparingInt(LeaderboardEntry::getGold).reversed()
                    .thenComparing(Comparator.comparingInt(LeaderboardEntry::getLevel).reversed())
                    .thenComparing(LeaderboardEntry::getUsername, Comparator.nullsLast(String::compareTo));

    /** 等级榜：等级 ↓，同分金币 ↓，再同分账号名 ↑。 */
    private static final Comparator<LeaderboardEntry> BY_LEVEL =
            Comparator.comparingInt(LeaderboardEntry::getLevel).reversed()
                    .thenComparing(Comparator.comparingInt(LeaderboardEntry::getGold).reversed())
                    .thenComparing(LeaderboardEntry::getUsername, Comparator.nullsLast(String::compareTo));

    /** 胜率榜：胜率 ↓，同分场次多者在前（样本更可信），再同分账号名 ↑。 */
    private static final Comparator<LeaderboardEntry> BY_WIN_RATE =
            Comparator.comparingDouble(LeaderboardEntry::getWinRate).reversed()
                    .thenComparing(Comparator.comparingInt(LeaderboardEntry::getTotalGames).reversed())
                    .thenComparing(LeaderboardEntry::getUsername, Comparator.nullsLast(String::compareTo));

    /** 胜场榜：胜场 ↓，同分胜率 ↓，再同分账号名 ↑。 */
    private static final Comparator<LeaderboardEntry> BY_WINS =
            Comparator.comparingInt(LeaderboardEntry::getWinCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LeaderboardEntry::getWinRate).reversed())
                    .thenComparing(LeaderboardEntry::getUsername, Comparator.nullsLast(String::compareTo));

    private static volatile LeaderboardService instance;

    private final AccountService accounts;

    /** 生产用法：单例，绑定全局 {@link AccountService}。 */
    public static LeaderboardService getInstance() {
        LeaderboardService local = instance;
        if (local == null) {
            synchronized (LeaderboardService.class) {
                local = instance;
                if (local == null) {
                    local = new LeaderboardService(AccountService.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定账号服务的构造器（测试 / 多档位）。 */
    public LeaderboardService(AccountService accounts) {
        this.accounts = accounts;
    }

    // ============================================================= 榜单

    /** 金币榜（gold 降序）。 */
    public List<LeaderboardEntry> getRankByGold() {
        return sorted(BY_GOLD);
    }

    /** 等级榜（level 降序）。 */
    public List<LeaderboardEntry> getRankByLevel() {
        return sorted(BY_LEVEL);
    }

    /**
     * 胜率榜（winRate 降序）。
     *
     * <p>只保留总场次 {@code >= }{@link #MIN_GAMES_FOR_WIN_RATE} 的账号：
     * 打一局赢一局的 100% 胜率不能算数，否则榜单会被新号刷满。
     */
    public List<LeaderboardEntry> getRankByWinRate() {
        List<LeaderboardEntry> eligible = new ArrayList<>();
        for (LeaderboardEntry entry : buildEntries()) {
            if (entry.getTotalGames() >= MIN_GAMES_FOR_WIN_RATE) {
                eligible.add(entry);
            }
        }
        return sortCopy(eligible, BY_WIN_RATE);
    }

    /** 胜场榜（winCount 降序）。 */
    public List<LeaderboardEntry> getRankByWins() {
        return sorted(BY_WINS);
    }

    /** 按榜单种类取榜单，供界面统一切换使用。 */
    public List<LeaderboardEntry> getRank(Board board) {
        if (board == null) {
            return List.of();
        }
        return switch (board) {
            case GOLD -> getRankByGold();
            case LEVEL -> getRankByLevel();
            case WIN_RATE -> getRankByWinRate();
            case WINS -> getRankByWins();
        };
    }

    /** 取榜单前 {@code limit} 条；{@code limit <= 0} 时返回空表。 */
    public List<LeaderboardEntry> getTop(Board board, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<LeaderboardEntry> ranked = getRank(board);
        if (ranked.size() <= limit) {
            return ranked;
        }
        return Collections.unmodifiableList(new ArrayList<>(ranked.subList(0, limit)));
    }

    // ============================================================= 我的排名

    /**
     * 当前登录玩家的榜单条目（未登录 / 账号无存档时返回 {@code null}）。
     *
     * <p>通过"当前账号名"匹配，因此玩家 A 登录看到的一定是 A 自己的条目，
     * 切换到 B 后同一个调用返回 B 的条目——这就是账号隔离的全部实现。
     */
    public synchronized LeaderboardEntry getMyRank() {
        String me = currentUsername();
        if (me == null) {
            return null;
        }
        for (LeaderboardEntry entry : buildEntries()) {
            if (me.equals(entry.getUsername())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 当前登录玩家在指定榜单上的名次。
     *
     * @return {@code 1} 起的名次；未登录 / 上不了榜（例如胜率榜局数不足）返回 {@code 0}
     */
    public int getMyRankPosition(Board board) {
        String me = currentUsername();
        if (me == null) {
            return 0;
        }
        List<LeaderboardEntry> ranked = getRank(board);
        for (int i = 0; i < ranked.size(); i++) {
            if (me.equals(ranked.get(i).getUsername())) {
                return i + 1;
            }
        }
        return 0;
    }

    /** 当前登录账号名；未登录返回 null。 */
    public String currentUsername() {
        return accounts == null ? null : accounts.getCurrentUsername();
    }

    // ============================================================= 内部

    /** 取榜单时统一走"构建 + 拷贝 + 排序"，对外是不可修改列表。 */
    private List<LeaderboardEntry> sorted(Comparator<LeaderboardEntry> comparator) {
        return sortCopy(buildEntries(), comparator);
    }

    private static List<LeaderboardEntry> sortCopy(List<LeaderboardEntry> entries,
                                                   Comparator<LeaderboardEntry> comparator) {
        List<LeaderboardEntry> copy = new ArrayList<>(entries);
        copy.sort(comparator);
        return Collections.unmodifiableList(copy);
    }

    /**
     * 汇总全部账号的榜单条目（未排序）。
     *
     * <p>跳过两类账号：账号表里没有玩家名的；以及还没有存档文件的。
     * 前者是脏数据，后者（理论上不该出现）如果硬读会落进 {@link PlayerManager} 的
     * "找不到就用默认档"分支，凭空给榜单塞一个 Lv.1 / 1000 金币的幽灵条目。
     */
    private List<LeaderboardEntry> buildEntries() {
        if (accounts == null) {
            return List.of();
        }
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (PlayerAccount account : accounts.getAccounts()) {
            if (account == null || account.getUsername() == null || account.getUsername().isBlank()) {
                continue;
            }
            Path file = accounts.resolvePlayerFile(account);
            if (file == null || !Files.isRegularFile(file)) {
                continue;
            }
            PlayerProfile profile = loadProfile(file);
            if (profile == null) {
                continue;
            }
            LeaderboardEntry entry = LeaderboardEntry.of(account, profile);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * 用"一次性 {@link PlayerManager}"读某个账号的存档。
     *
     * <p>关键点：<b>不碰全局单例</b>。{@code new PlayerManager(file)} 只把该文件读进一个
     * 局部对象，读完就没用了，当前登录玩家的内存状态与文件指针都不会被改动。
     * 返回副本，避免调用方持有可写对象。
     */
    private static PlayerProfile loadProfile(Path file) {
        try {
            return new PlayerManager(file).getProfile().copy();
        } catch (Exception e) {
            warn("读取玩家存档失败，跳过该账号: " + file + " -> " + e.getMessage());
            return null;
        }
    }

    private static void warn(String message) {
        System.out.println("[Leaderboard] " + message);
    }
}
