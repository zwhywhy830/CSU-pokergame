package com.csu.pokergame.player;

import java.util.Objects;

/**
 * 玩家成长服务（阶段 9）：经验、等级、升级奖励的唯一入口。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link #addExp(int)}：加经验，并按阈值自动升级；</li>
 *   <li>{@link #checkLevelUp()}：结算"经验达到阈值"的升级（等级 +1、扣除对应经验、发放升级金币奖励）；</li>
 *   <li>{@link #expToNextLevel(int)}：升到下一级所需经验（与 {@link PlayerManager} 同源，保证曲线一致）；</li>
 *   <li>{@link #upgradeReward(int)}：升到指定等级时发放的金币奖励；</li>
 *   <li>{@link #getLevelTitle(int)} / {@link #getLevelBadge(int)}：等级称号与徽章（青铜 / 白银 / 黄金 / 大师）。</li>
 * </ul>
 *
 * <p>规则：经验达到阈值即自动升级；每升一级 {@code level + 1}、从经验中扣除该级所需额度，
 * 并通过 {@link CoinService#addGold(int, String)} 发放升级金币奖励（同时写流水、落盘）。
 *
 * <p><b>升级结果反馈：</b>{@link #addExp(int)} / {@link #checkLevelUp()} 返回 {@link LevelUpResult}，
 * 记录 {@code oldLevel / newLevel / upgradeCount / upgradeReward}，供表现层展示"升级成功"反馈。
 *
 * <p><b>分工边界：</b>
 * <ul>
 *   <li>经验 / 等级的内存修改与落盘由本服务编排，底层读写复用 {@link PlayerManager}；
 *       升级金币奖励是金币，唯一合法出口仍是 {@link CoinService}（本服务不直接碰 gold 字段）；</li>
 *   <li>{@link PlayerManager#addExp(int)} 保留为不带奖励的底层能力，成长路径统一走本服务，
 *       以免出现"升了级但没发奖励"的入口。</li>
 * </ul>
 *
 * <p>所有会改数据的公开方法都加锁，并且改动后立即落盘（沿用 {@link PlayerManager} 的持久化），
 * 保证内存与 {@code player.json} 一致。
 *
 * <p>单例通过 {@link #getInstance()} 获取；测试 / 多档位可用
 * {@link #PlayerGrowthService(PlayerManager, CoinService)} 注入独立实例。
 */
public final class PlayerGrowthService {

    /** 每升一级的金币奖励基数：升到 Lv.N 奖励 {@code N × 50} 金币（Lv.2→100，Lv.12→600）。 */
    public static final int UPGRADE_GOLD_PER_LEVEL = 50;

    /** 升级奖励流水的固定前缀，最终写为 {@code "升级奖励 Lv.13"}。 */
    public static final String UPGRADE_REASON_PREFIX = "升级奖励 Lv.";

    /** 称号分段：白银起始等级（Lv.10 ~ Lv.19）。 */
    public static final int TIER_SILVER_LEVEL = 10;
    /** 称号分段：黄金起始等级（Lv.20 ~ Lv.29）。 */
    public static final int TIER_GOLD_LEVEL = 20;
    /** 称号分段：大师起始等级（Lv.30 及以上）。 */
    public static final int TIER_MASTER_LEVEL = 30;

    private static volatile PlayerGrowthService instance;

    private final PlayerManager players;
    private final CoinService coins;

    /** 生产用法：单例，复用 {@link PlayerManager} / {@link CoinService} 的单例。 */
    public static PlayerGrowthService getInstance() {
        PlayerGrowthService local = instance;
        if (local == null) {
            synchronized (PlayerGrowthService.class) {
                local = instance;
                if (local == null) {
                    local = new PlayerGrowthService(PlayerManager.getInstance(), CoinService.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定依赖的构造器（测试 / 独立档位）。 */
    public PlayerGrowthService(PlayerManager players, CoinService coins) {
        this.players = Objects.requireNonNull(players, "players");
        this.coins = Objects.requireNonNull(coins, "coins");
    }

    // ============================================================= 经验曲线

    /**
     * 升到下一级所需经验：{@code 100 + level * 100}（Lv.1 需 200，Lv.12 需 1300）。
     * 与 {@link PlayerManager#expToNextLevel(int)} 同源，避免两条公式漂移。
     */
    public int expToNextLevel(int level) {
        return PlayerManager.expToNextLevel(level);
    }

    /** 当前等级升到下一级所需经验。 */
    public int expToNextLevel() {
        return expToNextLevel(players.getProfile().getLevel());
    }

    /**
     * 升到指定等级（{@code level}）时发放的金币奖励：{@code level × 50}。
     * 例如升到 Lv.2 奖励 100，升到 Lv.13 奖励 650。
     */
    public int upgradeReward(int level) {
        return Math.max(PlayerProfile.MIN_LEVEL, level) * UPGRADE_GOLD_PER_LEVEL;
    }

    // ============================================================= 等级称号

    /**
     * 等级称号：
     * <ul>
     *   <li>Lv.1 ~ Lv.9 → 青铜</li>
     *   <li>Lv.10 ~ Lv.19 → 白银</li>
     *   <li>Lv.20 ~ Lv.29 → 黄金</li>
     *   <li>Lv.30 及以上 → 大师</li>
     * </ul>
     *
     * @param level 等级，低于 1 按 1 处理
     */
    public String getLevelTitle(int level) {
        int lv = Math.max(PlayerProfile.MIN_LEVEL, level);
        if (lv >= TIER_MASTER_LEVEL) {
            return "大师";
        }
        if (lv >= TIER_GOLD_LEVEL) {
            return "黄金";
        }
        if (lv >= TIER_SILVER_LEVEL) {
            return "白银";
        }
        return "青铜";
    }

    /**
     * 等级称号徽章（与 {@link #getLevelTitle(int)} 分段一致）：
     * 青铜 🥉、白银 🥈、黄金 🥇、大师 👑。
     */
    public String getLevelBadge(int level) {
        int lv = Math.max(PlayerProfile.MIN_LEVEL, level);
        if (lv >= TIER_MASTER_LEVEL) {
            return "👑";
        }
        if (lv >= TIER_GOLD_LEVEL) {
            return "🥇";
        }
        if (lv >= TIER_SILVER_LEVEL) {
            return "🥈";
        }
        return "🥉";
    }

    // ============================================================= 加经验 / 升级

    /**
     * 增加经验，并按阈值自动升级（可一次连升多级）。
     *
     * <p>流程：经验入账 → {@link #checkLevelUp()} 结算升级（逐级扣经验、升级、发奖励）→ 落盘。
     *
     * @param amount 增加的经验，必须 &gt; 0，否则不做任何改动
     * @return 本次升级结果快照（{@code upgradeCount == 0} 表示没升级）
     */
    public synchronized LevelUpResult addExp(int amount) {
        PlayerProfile profile = players.getProfile();
        if (amount <= 0) {
            int level = profile.getLevel();
            return new LevelUpResult(level, level, 0, 0);
        }
        profile.setExp(saturatingAdd(profile.getExp(), amount));
        LevelUpResult result = checkLevelUp();
        if (!result.upgraded()) {
            // 只有经验变化、没升级：checkLevelUp 不会写盘，这里补一次持久化
            players.save();
        }
        return result;
    }

    /**
     * 结算"经验达到阈值"的升级：经验够一级就升一级，等级 +1、扣除该级所需经验，
     * 并通过 {@link CoinService} 发放对应升级金币奖励，直到经验不足以再升一级为止。
     *
     * <p>常规升级已由 {@link #addExp(int)} 流程内调用本方法即时完成；本方法也对外暴露，
     * 用于"经验被外部直接写入、需要补结算"的场景。
     *
     * @return 本次结算的升级结果快照（{@code upgradeCount == 0} 表示未升级）
     */
    public synchronized LevelUpResult checkLevelUp() {
        PlayerProfile profile = players.getProfile();
        int oldLevel = profile.getLevel();
        int totalReward = 0;
        while (profile.getExp() >= expToNextLevel(profile.getLevel())) {
            profile.setExp(profile.getExp() - expToNextLevel(profile.getLevel()));
            profile.setLevel(profile.getLevel() + 1);
            int reward = upgradeReward(profile.getLevel());
            // 升级奖励是金币：唯一出口是 CoinService（内部会同时写流水并落盘等级 / 经验）
            coins.addGold(reward, UPGRADE_REASON_PREFIX + profile.getLevel());
            totalReward += reward;
        }
        int newLevel = profile.getLevel();
        // 阶段 21：升级成功 → 播放升级音效（无音频资源时 AudioService 静默跳过）
        if (newLevel > oldLevel) {
            com.csu.pokergame.audio.AudioService.getInstance()
                    .playEffect(com.csu.pokergame.audio.SoundEffect.LEVEL_UP);
        }
        return new LevelUpResult(oldLevel, newLevel, newLevel - oldLevel, totalReward);
    }

    /** 防止 int 溢出成负数。 */
    private static int saturatingAdd(int current, int delta) {
        long sum = (long) current + delta;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    // ============================================================= 升级结果

    /**
     * 一次加经验 / 升级结算的结果快照（不可变），供表现层展示成长反馈。
     *
     * <ul>
     *   <li>{@link #getOldLevel()}：结算前等级；</li>
     *   <li>{@link #getNewLevel()}：结算后等级；</li>
     *   <li>{@link #getUpgradeCount()}：本次提升等级数（0 表示未升级，可一次连升多级）；</li>
     *   <li>{@link #getUpgradeReward()}：本次升级累计发放的金币奖励。</li>
     * </ul>
     */
    public static final class LevelUpResult {

        private final int oldLevel;
        private final int newLevel;
        private final int upgradeCount;
        private final int upgradeReward;

        public LevelUpResult(int oldLevel, int newLevel, int upgradeCount, int upgradeReward) {
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.upgradeCount = upgradeCount;
            this.upgradeReward = upgradeReward;
        }

        public int getOldLevel() {
            return oldLevel;
        }

        public int getNewLevel() {
            return newLevel;
        }

        public int getUpgradeCount() {
            return upgradeCount;
        }

        public int getUpgradeReward() {
            return upgradeReward;
        }

        /** 本次是否发生了升级。 */
        public boolean upgraded() {
            return upgradeCount > 0;
        }

        @Override
        public String toString() {
            return upgraded()
                    ? "LevelUpResult[Lv." + oldLevel + " → Lv." + newLevel + ", +" + upgradeReward + " 金币]"
                    : "LevelUpResult[Lv." + newLevel + " 未升级]";
        }
    }
}
