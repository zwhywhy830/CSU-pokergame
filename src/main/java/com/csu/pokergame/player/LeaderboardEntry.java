package com.csu.pokergame.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * 排行榜条目（阶段 18）：某个账号在当前榜单里的"一行数据"。
 *
 * <p>落盘 / 传输形态（{@code accounts.json} 与 {@code players/*.json} 之外的本类只读快照）：
 * <pre>{@code
 * {
 *   "accountId" : "acc-playerA",
 *   "username" : "playerA",
 *   "level" : 10,
 *   "gold" : 5000,
 *   "winCount" : 32,
 *   "totalGames" : 50,
 *   "winRate" : 0.64,
 *   "achievementCount" : 6
 * }
 * }</pre>
 *
 * <p>字段含义：
 * <ul>
 *   <li>{@code accountId}：账号唯一标识（来自 {@link PlayerAccount#getAccountId()}）；</li>
 *   <li>{@code username}：账号名（登录名，同时是排行榜展示名）；</li>
 *   <li>{@code level}：等级；</li>
 *   <li>{@code gold}：当前金币余额；</li>
 *   <li>{@code winCount}：胜场数（= {@link PlayerProfile#getTotalWins()}）；</li>
 *   <li>{@code totalGames}：总场次（= 胜 + 负）；</li>
 *   <li>{@code winRate}：胜率，取值 {@code 0.0 ~ 1.0}（派生值，不落盘到玩家存档）；</li>
 *   <li>{@code achievementCount}：已解锁成就数。</li>
 * </ul>
 *
 * <p><b>本类不持有任何可变业务逻辑</b>：它只是 {@link LeaderboardService} 从各账号存档里
 * "摘出来"的只读快照，写入由服务负责、排序由服务负责。字段集中在排序与展示上，
 * 因此所有 {@code getXxx()} 展示辅助方法都标了 {@link JsonIgnore}，保证序列化时只有上面 8 个字段。
 *
 * <p>{@code winRate} 直接以 {@code [0,1]} 的小数落盘（而不是百分数），
 * 排序时可直接比较，展示时用 {@link #getWinRatePercent()} 转成整数百分比。
 * 这样"排序口径"和"展示口径"不会因为四舍五入而互相矛盾。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"accountId", "username", "level", "gold", "winCount", "totalGames",
        "winRate", "achievementCount"})
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {

    /** 账号唯一标识。 */
    private String accountId;
    /** 账号名（排行榜展示名）。 */
    private String username;
    /** 等级。 */
    private int level;
    /** 当前金币。 */
    private int gold;
    /** 胜场数。 */
    private int winCount;
    /** 总场次。 */
    private int totalGames;
    /** 胜率（0.0 ~ 1.0）。 */
    private double winRate;
    /** 已解锁成就数。 */
    private int achievementCount;

    /** Jackson 反序列化用的无参构造。 */
    public LeaderboardEntry() {
    }

    public LeaderboardEntry(String accountId, String username, int level, int gold,
                            int winCount, int totalGames, double winRate, int achievementCount) {
        this.accountId = accountId;
        this.username = username;
        setLevel(level);
        setGold(gold);
        setWinCount(winCount);
        setTotalGames(totalGames);
        setWinRate(winRate);
        setAchievementCount(achievementCount);
    }

    /**
     * 从账号 + 玩家档案摘一条榜单数据。
     *
     * <p>这是本类唯一的"生产"入口：{@link LeaderboardService} 读一个账号存档后就调它，
     * 保证账号名 / 账号 id 来自 {@link PlayerAccount}、数值来自 {@link PlayerProfile}，两边不会错位。
     *
     * @param account 账号；null 时返回 null
     * @param profile 玩家档案；null 时数值按 0 / 等级 1 处理
     */
    public static LeaderboardEntry of(PlayerAccount account, PlayerProfile profile) {
        if (account == null) {
            return null;
        }
        PlayerProfile p = profile == null ? new PlayerProfile() : profile;
        return new LeaderboardEntry(account.getAccountId(), account.getUsername(),
                p.getLevel(), p.getGold(), p.getTotalWins(), p.getTotalGames(),
                p.getWinRate(), p.getAchievementCount());
    }

    // ============================================================= getter / setter

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(PlayerProfile.MIN_LEVEL, level);
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = Math.max(0, winCount);
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = Math.max(0, totalGames);
    }

    public double getWinRate() {
        return winRate;
    }

    /** 写胜率；越界自动夹到 {@code [0,1]}，脏 JSON 不会让榜单出现 {@code 150%} 这种值。 */
    public void setWinRate(double winRate) {
        if (Double.isNaN(winRate) || winRate < 0.0) {
            this.winRate = 0.0;
        } else {
            this.winRate = Math.min(1.0, winRate);
        }
    }

    public int getAchievementCount() {
        return achievementCount;
    }

    public void setAchievementCount(int achievementCount) {
        this.achievementCount = Math.max(0, achievementCount);
    }

    // ============================================================= 展示辅助（只读，不参与落盘）

    /** 胜率百分数（四舍五入取整），例如 {@code 64}。 */
    @JsonIgnore
    public int getWinRatePercent() {
        return (int) Math.round(winRate * 100);
    }

    /** 胜率展示文案：{@code 64%}。 */
    @JsonIgnore
    public String getWinRateText() {
        return getWinRatePercent() + "%";
    }

    /** 等级展示文案：{@code Lv.10}。 */
    @JsonIgnore
    public String getLevelText() {
        return "Lv." + level;
    }

    /** 金币展示文案：{@code 金币 5000}。 */
    @JsonIgnore
    public String getGoldText() {
        return "金币 " + gold;
    }

    /** 胜场展示文案：{@code 胜场 32}。 */
    @JsonIgnore
    public String getWinCountText() {
        return "胜场 " + winCount;
    }

    // ============================================================= 工具

    /** 深拷贝：排行榜对外只给快照，避免调用方就地改写内存条目。 */
    public LeaderboardEntry copy() {
        return new LeaderboardEntry(accountId, username, level, gold, winCount, totalGames,
                winRate, achievementCount);
    }

    /**
     * 默认比较：按金币降序（与金币榜一致），金币相同则等级高者在前，再相同按账号名升序。
     * 排序的"权威口径"在 {@link LeaderboardService}，这里只是给本类一个稳定默认序。
     */
    @Override
    public int compareTo(LeaderboardEntry other) {
        int byGold = Integer.compare(other.gold, this.gold);
        if (byGold != 0) {
            return byGold;
        }
        int byLevel = Integer.compare(other.level, this.level);
        if (byLevel != 0) {
            return byLevel;
        }
        return String.valueOf(this.username).compareTo(String.valueOf(other.username));
    }

    /** 同一账号视为同一条（账号 id 相同即相等）。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LeaderboardEntry other)) {
            return false;
        }
        if (accountId != null || other.accountId != null) {
            return Objects.equals(accountId, other.accountId);
        }
        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId != null ? accountId : username);
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{" + username + " Lv." + level + " gold=" + gold
                + " win=" + winCount + "/" + totalGames + " rate=" + getWinRatePercent() + "%}";
    }
}
