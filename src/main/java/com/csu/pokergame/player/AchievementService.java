package com.csu.pokergame.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 成就服务（阶段 11）：定义 / 检测 / 解锁 / 奖励的唯一入口。 */
public final class AchievementService {

    public static final String FIRST_GAME = "FIRST_GAME";
    public static final String FIRST_WIN = "FIRST_WIN";
    public static final String WIN_STREAK_5 = "WIN_STREAK_5";
    public static final String GOLD_10000 = "GOLD_10000";
    public static final String LEVEL_10 = "LEVEL_10";
    public static final String LEVEL_20 = "LEVEL_20";

    /** 连胜成就阈值。 */
    public static final int WIN_STREAK_TARGET = 5;
    /** 财富成就阈值：累计获得金币。 */
    public static final int GOLD_TARGET = 10000;

    private static volatile AchievementService instance;

    private final PlayerManager players;
    private final CoinService coins;
    private final PlayerGrowthService growth;
    private final PlayerStatsService stats;

    /** 成就定义表：id → 成就（LinkedHashMap 保持预置顺序，界面按此顺序展示）。 */
    private final Map<String, Achievement> definitions = new LinkedHashMap<>();

    /** 生产用法：单例。 */
    public static AchievementService getInstance() {
        AchievementService local = instance;
        if (local == null) {
            synchronized (AchievementService.class) {
                if (instance == null) {
                    instance = new AchievementService(PlayerManager.getInstance(), CoinService.getInstance(),
                            PlayerGrowthService.getInstance(), PlayerStatsService.getInstance());
                }
                local = instance;
            }
        }
        return local;
    }

    /** 指定依赖的构造器（测试 / 独立档位）。构造时注册定义并同步一次解锁状态。 */
    public AchievementService(PlayerManager players, CoinService coins,
                              PlayerGrowthService growth, PlayerStatsService stats) {
        this.players = Objects.requireNonNull(players, "players");
        this.coins = Objects.requireNonNull(coins, "coins");
        this.growth = Objects.requireNonNull(growth, "growth");
        this.stats = Objects.requireNonNull(stats, "stats");
        registerDefaultAchievements();
        syncFromProfile();
    }

    private void registerDefaultAchievements() {
        define(FIRST_GAME, "初出茅庐", "完成第一局游戏", 100, 20);
        define(FIRST_WIN, "首次胜利", "第一次获胜", 200, 30);
        define(WIN_STREAK_5, "连胜达人", "最高连胜达到 5 局", 500, 100);
        define(GOLD_10000, "财富积累", "累计获得金币 10000", 1000, 200);
        define(LEVEL_10, "白银玩家", "达到 Lv.10", 800, 300);
        define(LEVEL_20, "黄金玩家", "达到 Lv.20", 2000, 600);
    }

    /** 登记一条成就定义（同 id 重复登记时后者覆盖）。 */
    private void define(String id, String title, String description, int gold, int exp) {
        definitions.put(id, new Achievement(id, title, description, gold, exp));
    }

    /** 用档案里的已解锁 id 集合回填定义表的 unlocked 状态（不发奖）。 */
    private synchronized void syncFromProfile() {
        for (String id : players.getProfile().getAchievements()) {
            Achievement a = definitions.get(id);
            if (a != null) {
                a.markUnlocked();
            }
        }
    }

    /**
     * 按当前玩家档案重新加载成就解锁状态（阶段 16：多账号切换）。
     *
     * <p>解锁标记缓存在内存的定义表里，{@link PlayerManager} 换成另一个账号的存档后，
     * 上一个账号的标记会残留在内存中——既会让新账号"白拿"成就计数，又会让它的
     * {@code unlock(...)} 因为"已解锁"而拒绝发奖。因此这里先重建定义表（全部置为未解锁），
     * 再按新档案的 {@code achievements} 集合回填。
     *
     * <p>只读档案、不发奖、不改数据，<b>不影响</b>既有的检测 / 解锁 / 奖励入口。
     */
    public synchronized void reloadFromProfile() {
        definitions.clear();
        registerDefaultAchievements();
        syncFromProfile();
    }

    private Achievement unlock(String id, String reason) {
        Achievement a = definitions.get(id);
        if (a == null || !a.markUnlocked()) {
            return null;
        }
        players.getProfile().unlockAchievement(id);
        // 阶段 21：成就解锁成功 → 播放成就音效（无音频资源时 AudioService 静默跳过）
        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(com.csu.pokergame.audio.SoundEffect.ACHIEVEMENT_UNLOCK);
        if (a.getRewardGold() > 0) {
            coins.addGold(a.getRewardGold(), "成就奖励 " + a.getTitle());
        }
        if (a.getRewardExp() > 0) {
            growth.addExp(a.getRewardExp());
        }
        // 兜底落盘：成就解锁状态、奖励金币与经验都必须写进 player.json
        players.save();
        System.out.println("[Achievement] 解锁：" + a.getTitle() + "（" + reason + "）");
        return a;
    }

    // ============================================================= 检测入口

    /** 进入一局游戏时检测：FIRST_GAME（第一局完成后解锁）。 */
    public synchronized List<Achievement> checkOnGameStart() {
        return collect(unlock(FIRST_GAME, "完成第一局游戏"));
    }

    /**
     * 一局获胜结算时检测：FIRST_WIN、WIN_STREAK_5。
     *
     * <p><b>阶段 12：</b>连胜成就改为读取 {@link PlayerStatsService#getMaxWinStreak()}（历史最高连胜），
     * 因此"曾经连胜过 5 局"就会被永久判定为达成，不会因为之后输一局清零当前连胜而漏解锁。
     */
    public synchronized List<Achievement> checkOnWin() {
        List<Achievement> unlocked = new ArrayList<>();
        unlocked.addAll(collect(unlock(FIRST_WIN, "第一次获胜")));
        if (stats.getMaxWinStreak() >= WIN_STREAK_TARGET) {
            unlocked.addAll(collect(unlock(WIN_STREAK_5, "最高连胜 " + WIN_STREAK_TARGET + " 局")));
        }
        return unlocked;
    }

    /** 金币变化时检测：GOLD_10000（累计获得金币 ≥ 10000）。 */
    public synchronized List<Achievement> checkOnGoldChange() {
        if (coins.getTotalEarned() >= GOLD_TARGET) {
            return collect(unlock(GOLD_10000, "累计获得金币 " + GOLD_TARGET));
        }
        return new ArrayList<>();
    }

    /** 升级时检测：LEVEL_10、LEVEL_20。 */
    public synchronized List<Achievement> checkOnLevelUp() {
        int level = players.getProfile().getLevel();
        List<Achievement> unlocked = new ArrayList<>();
        if (level >= 10) {
            unlocked.addAll(collect(unlock(LEVEL_10, "达到 Lv.10")));
        }
        if (level >= 20) {
            unlocked.addAll(collect(unlock(LEVEL_20, "达到 Lv.20")));
        }
        return unlocked;
    }

    /** 全量检测一遍（牌局结算后的兜底，任何来源的数据变化都能补解锁）。 */
    public synchronized List<Achievement> evaluateAll() {
        List<Achievement> all = new ArrayList<>();
        all.addAll(checkOnGameStart());
        all.addAll(checkOnWin());
        all.addAll(checkOnGoldChange());
        all.addAll(checkOnLevelUp());
        return all;
    }

    /** 把单个解锁结果包成列表（未解锁时返回空列表）。 */
    private static List<Achievement> collect(Achievement a) {
        List<Achievement> list = new ArrayList<>();
        if (a != null) {
            list.add(a);
        }
        return list;
    }

    // ============================================================= 查询

    /** 全部成就（按定义顺序，含未解锁），界面列表直接遍历。 */
    public synchronized List<Achievement> getAchievements() {
        return new ArrayList<>(definitions.values());
    }

    /** 已解锁成就。 */
    public synchronized List<Achievement> getUnlockedAchievements() {
        List<Achievement> list = new ArrayList<>();
        for (Achievement a : definitions.values()) {
            if (a.isUnlocked()) {
                list.add(a);
            }
        }
        return list;
    }

    /** 已解锁数量。 */
    public synchronized int getUnlockedCount() {
        return getUnlockedAchievements().size();
    }

    /** 成就总数。 */
    public synchronized int getTotalCount() {
        return definitions.size();
    }

    /** 单条成就是否已解锁。 */
    public synchronized boolean isUnlocked(String id) {
        Achievement a = definitions.get(id);
        return a != null && a.isUnlocked();
    }

    /**
     * 最近获得的成就（阶段 12）：按解锁顺序取最后一条，未解锁任何成就时返回 {@code null}。
     *
     * <p>解锁顺序保存在 {@link PlayerProfile#getAchievements()} 这个 {@code LinkedHashSet} 里
     * （写档时按插入顺序输出为数组，读档时按同样顺序回填），因此跨存档也能拿到"最近一条"。
     */
    public synchronized Achievement getLatestAchievement() {
        Achievement latest = null;
        for (String id : players.getProfile().getAchievements()) {
            Achievement a = definitions.get(id);
            if (a != null && a.isUnlocked()) {
                latest = a;
            }
        }
        return latest;
    }
}



