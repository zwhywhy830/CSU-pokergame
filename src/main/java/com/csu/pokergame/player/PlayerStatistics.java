package com.csu.pokergame.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * 玩家统计数据对象（阶段 19）：把"战绩 + 金币"汇总成一份只读快照，供数据中心展示。
 *
 * <p>落盘形态（<b>仅作为可序列化对象存在，绝不写进 {@code player.json}</b>）：
 * <pre>{@code
 * {
 *   "totalGames" : 128,
 *   "winCount" : 87,
 *   "loseCount" : 41,
 *   "winRate" : 0.68,
 *   "maxWinStreak" : 12,
 *   "currentWinStreak" : 3,
 *   "totalGoldEarned" : 35000,
 *   "totalGoldSpent" : 22000,
 *   "favoriteGame" : "跑得快"
 * }
 * }</pre>
 *
 * <p>字段含义：
 * <ul>
 *   <li>{@code totalGames}：总场次（= 胜 + 负）；</li>
 *   <li>{@code winCount} / {@code loseCount}：胜场 / 负场；</li>
 *   <li>{@code winRate}：胜率，取值 {@code [0,1]}（派生值，展示时用 {@link #getWinRateText()}）；</li>
 *   <li>{@code maxWinStreak} / {@code currentWinStreak}：历史最高连胜 / 当前连胜；</li>
 *   <li>{@code totalGoldEarned} / {@code totalGoldSpent}：累计获得 / 累计消耗金币；</li>
 *   <li>{@code favoriteGame}：最常玩玩的玩法名（无记录时为空）。</li>
 * </ul>
 *
 * <p><b>为什么单独一个类：</b>统计数据是"从已有数据算出来的派生结果"，不属于玩家档案。
 * 本类只承载一次计算结果，由 {@link StatisticsService} 实时组装；
 * 它<b>不会被塞进</b> {@link PlayerProfile}，因此 {@code player.json} 不会多出任何统计字段
 * （旧存档读档不受影响）。
 *
 * <p><b>展示辅助方法全部 {@link JsonIgnore}：</b>{@link #getWinRateText()} 等只是界面文案，
 * 不参与序列化，保证对象字段与上面的 9 项严格一一对应。
 *
 * <p>{@link #setWinRate(double)} 对越界 / {@code NaN} 做归一，脏数据不会出现 {@code 150%}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"totalGames", "winCount", "loseCount", "winRate",
        "maxWinStreak", "currentWinStreak", "totalGoldEarned", "totalGoldSpent", "favoriteGame"})
public class PlayerStatistics {

    /** 无数据时"最常游戏"的占位文案。 */
    public static final String NO_GAME = "暂无";

    /** 总场次。 */
    private int totalGames;
    /** 胜场。 */
    private int winCount;
    /** 负场。 */
    private int loseCount;
    /** 胜率，取值 [0,1]。 */
    private double winRate;
    /** 历史最高连胜。 */
    private int maxWinStreak;
    /** 当前连胜。 */
    private int currentWinStreak;
    /** 累计获得金币。 */
    private int totalGoldEarned;
    /** 累计消耗金币。 */
    private int totalGoldSpent;
    /** 最常玩玩的玩法名。 */
    private String favoriteGame;

    /** Jackson 反序列化用无参构造：全部字段先落到 0 / 空。 */
    public PlayerStatistics() {
        this(0, 0, 0, 0.0, 0, 0, 0, 0, null);
    }

    public PlayerStatistics(int totalGames, int winCount, int loseCount, double winRate,
                            int maxWinStreak, int currentWinStreak,
                            int totalGoldEarned, int totalGoldSpent, String favoriteGame) {
        setTotalGames(totalGames);
        setWinCount(winCount);
        setLoseCount(loseCount);
        setWinRate(winRate);
        setMaxWinStreak(maxWinStreak);
        setCurrentWinStreak(currentWinStreak);
        setTotalGoldEarned(totalGoldEarned);
        setTotalGoldSpent(totalGoldSpent);
        setFavoriteGame(favoriteGame);
    }

    // ============================================================= getter / setter
    // setter 一律归一：任何来源（计算 / JSON / 脏数据）都不会产生非法值。

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = Math.max(0, totalGames);
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = Math.max(0, winCount);
    }

    public int getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(int loseCount) {
        this.loseCount = Math.max(0, loseCount);
    }

    public double getWinRate() {
        return winRate;
    }

    /** 写胜率；{@code NaN} / 负数归 0，超过 1 夹到 1。 */
    public void setWinRate(double winRate) {
        if (Double.isNaN(winRate) || winRate < 0.0) {
            this.winRate = 0.0;
        } else {
            this.winRate = Math.min(1.0, winRate);
        }
    }

    public int getMaxWinStreak() {
        return maxWinStreak;
    }

    public void setMaxWinStreak(int maxWinStreak) {
        this.maxWinStreak = Math.max(0, maxWinStreak);
    }

    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    public void setCurrentWinStreak(int currentWinStreak) {
        this.currentWinStreak = Math.max(0, currentWinStreak);
    }

    public int getTotalGoldEarned() {
        return totalGoldEarned;
    }

    public void setTotalGoldEarned(int totalGoldEarned) {
        this.totalGoldEarned = Math.max(0, totalGoldEarned);
    }

    public int getTotalGoldSpent() {
        return totalGoldSpent;
    }

    public void setTotalGoldSpent(int totalGoldSpent) {
        this.totalGoldSpent = Math.max(0, totalGoldSpent);
    }

    public String getFavoriteGame() {
        return favoriteGame;
    }

    public void setFavoriteGame(String favoriteGame) {
        this.favoriteGame = favoriteGame == null || favoriteGame.isBlank() ? null : favoriteGame.trim();
    }

    // ============================================================= 展示辅助（只读，不参与序列化）

    /** 胜率百分数（四舍五入取整），例如 {@code 68}。 */
    @JsonIgnore
    public int getWinRatePercent() {
        return (int) Math.round(winRate * 100);
    }

    /** 胜率展示文案：{@code 68%}。 */
    @JsonIgnore
    public String getWinRateText() {
        return getWinRatePercent() + "%";
    }

    /** 最常游戏展示文案：有记录时给玩法名，无记录时给 {@link #NO_GAME}。 */
    @JsonIgnore
    public String getFavoriteGameText() {
        return favoriteGame == null ? NO_GAME : favoriteGame;
    }

    /** 是否打过至少一局。 */
    @JsonIgnore
    public boolean hasGames() {
        return totalGames > 0;
    }

    /** 总场次展示文案。 */
    @JsonIgnore
    public String getTotalGamesText() {
        return String.valueOf(totalGames);
    }

    // ============================================================= 工具

    /** 深拷贝：对外只给快照，避免调用方就地改写。 */
    public PlayerStatistics copy() {
        return new PlayerStatistics(totalGames, winCount, loseCount, winRate,
                maxWinStreak, currentWinStreak, totalGoldEarned, totalGoldSpent, favoriteGame);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerStatistics other)) {
            return false;
        }
        return totalGames == other.totalGames
                && winCount == other.winCount
                && loseCount == other.loseCount
                && Double.compare(winRate, other.winRate) == 0
                && maxWinStreak == other.maxWinStreak
                && currentWinStreak == other.currentWinStreak
                && totalGoldEarned == other.totalGoldEarned
                && totalGoldSpent == other.totalGoldSpent
                && Objects.equals(favoriteGame, other.favoriteGame);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalGames, winCount, loseCount, winRate,
                maxWinStreak, currentWinStreak, totalGoldEarned, totalGoldSpent, favoriteGame);
    }

    @Override
    public String toString() {
        return "PlayerStatistics{total=" + totalGames + " win=" + winCount + " lose=" + loseCount
                + " rate=" + getWinRateText() + " maxStreak=" + maxWinStreak
                + " curStreak=" + currentWinStreak
                + " earned=" + totalGoldEarned + " spent=" + totalGoldSpent
                + " favorite=" + getFavoriteGameText() + "}";
    }
}
