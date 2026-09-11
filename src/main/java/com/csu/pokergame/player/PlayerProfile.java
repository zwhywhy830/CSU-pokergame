package com.csu.pokergame.player;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 玩家档案（V1）：玩家经济系统的最小数据模型。
 *
 * <p>字段与持久化 JSON 一一对应，可直接由 Jackson 序列化 / 反序列化：
 * <pre>{@code
 * {
 *   "id": "player001",
 *   "name": "玩家",
 *   "level": 12,
 *   "exp": 350,
 *   "gold": 1000,
 *   "diamond": 50,
 *   "totalGames": 38,
 *   "totalWins": 25,
 *   "totalLosses": 13,
 *   "currentWinStreak": 2,
 *   "maxWinStreak": 5,
 *   "avatar": "♛",
 *   "totalGoldEarned": 0,
 *   "totalGoldSpent": 0,
 *   "achievementCount": 0,
 *   "vipLevel": 0
 * }
 * }</pre>
 *
 * <p>本类只负责<b>持有数据并做越界归一</b>（负数归零、空昵称回退默认值）。
 * 业务动作按数据种类分工，各自只有一个入口：
 * <ul>
 *   <li><b>金币</b> → {@link CoinService}（{@code setGold} 已降为包内可见，类外无法绕过）；</li>
 *   <li><b>经验 / 等级 / 升级奖励</b> → {@link PlayerGrowthService}；</li>
 *   <li><b>胜负场次</b> → {@link PlayerManager}（场次计数）/ {@link PlayerStatsService}（含连胜的战绩统计）。</li>
 * </ul>
 *
 * <p><b>旧存档兼容：</b>新增字段（{@code totalGames} / {@code totalWins} / {@code totalLosses} /
 * {@code currentWinStreak} / {@code maxWinStreak} / {@code totalGoldEarned} / {@code totalGoldSpent} /
 * {@code achievementCount} / {@code vipLevel}）在旧版 JSON 中不存在时，Jackson 不会调用对应 setter，
 * 字段保持无参构造里设定的 0，因此老存档可以无缝升级，不会因为缺字段而读档失败。
 * {@code avatar} 同理：缺失时反序列化为 null，由 {@link #getAvatar()} 回退为默认字形。
 *
 * <p><b>战绩字段改名（阶段 12）：</b>旧存档里的 {@code winCount} / {@code loseCount} 通过
 * {@link JsonAlias} 映射到 {@link #setTotalWins(int)} / {@link #setTotalLosses(int)}，
 * 因此"胜场 / 负场"在旧存档里不会丢失，新存档统一写成 {@code totalWins} / {@code totalLosses}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "name", "level", "exp", "gold", "diamond",
        "totalGames", "totalWins", "totalLosses", "currentWinStreak", "maxWinStreak", "avatar",
        "totalGoldEarned", "totalGoldSpent", "achievementCount", "vipLevel", "achievements",
        "inventory", "luckyCount", "doubleExpCount"})
public class PlayerProfile {

    /** 默认玩家 id。 */
    public static final String DEFAULT_ID = "player001";
    /** 默认昵称。 */
    public static final String DEFAULT_NAME = "玩家";
    /** 默认头像字形（与 AvatarView / 个人信息页候选一致）。 */
    public static final String DEFAULT_AVATAR = "♛";
    /** 新档初始金币。 */
    public static final int DEFAULT_GOLD = 1000;
    /** 新档初始钻石。 */
    public static final int DEFAULT_DIAMOND = 50;
    /** 等级下限。 */
    public static final int MIN_LEVEL = 1;

    private String id;
    private String name;
    private int level;
    private int exp;
    private int gold;
    private int diamond;
    /** 总场次（= {@code totalWins + totalLosses}，落盘镜像；读 / 写由胜负场次保证一致）。 */
    private int totalGames;
    /** 胜利次数（旧存档键名 {@code winCount}，见 {@link #setTotalWins(int)} 上的别名）。 */
    private int totalWins;
    /** 失败次数（旧存档键名 {@code loseCount}，见 {@link #setTotalLosses(int)} 上的别名）。 */
    private int totalLosses;
    /** 当前连胜场次（输一局清零）。战绩统计由 {@link PlayerStatsService} 维护。 */
    private int currentWinStreak;
    /** 历史最高连胜场次（只增不减）。战绩统计由 {@link PlayerStatsService} 维护。 */
    private int maxWinStreak;
    private String avatar;
    /** 累计获得金币总量（每次 CoinService 加金币时按金额累加；初始金币不计入）。 */
    private int totalGoldEarned;
    /** 累计消耗金币总量（每次 CoinService 扣金币成功时按金额累加）。 */
    private int totalGoldSpent;
    /** 已解锁成就数（成就系统尚未实现，先留字段，默认 0）。 */
    private int achievementCount;
    /**
     * 已解锁成就 id 集合（阶段 11 新增）。
     *
     * <p><b>旧存档兼容：</b>旧版 {@code player.json} 没有 {@code achievements} 键时，
     * Jackson 不会调用 setter，集合保持空集，读档不会报错（配合
     * {@code @JsonIgnoreProperties(ignoreUnknown = true)} 反向也安全）。
     */
    private final Set<String> achievements = new LinkedHashSet<>();
    /** VIP 等级（VIP 系统尚未实现，先留字段，0 表示非 VIP）。 */
    private int vipLevel;
    /** 背包（阶段 13）：旧存档没有该键时保持空表，自动初始化为 []。 */
    private final List<Item> inventory = new ArrayList<>();
    /**
     * 幸运状态层数（阶段 15）：使用「幸运牌」累积，供未来游戏奖励倍率使用。
     * 本阶段只记录状态，不参与结算。旧存档缺该键时保持 0。
     */
    private int luckyCount;
    /**
     * 双倍经验状态层数（阶段 15）：使用「双倍经验卡」累积，未来获得经验时按层数翻倍。
     * 本阶段只记录状态，不参与结算。旧存档缺该键时保持 0。
     */
    private int doubleExpCount;

    /** Jackson 反序列化用的无参构造：全部字段先落到默认值，避免 JSON 缺字段时出现 0 值。 */
    public PlayerProfile() {
        this(DEFAULT_ID, DEFAULT_NAME, MIN_LEVEL, 0, DEFAULT_GOLD, DEFAULT_DIAMOND, 0, 0, 0, 0,
                DEFAULT_AVATAR, 0, 0, 0, 0);
    }

    public PlayerProfile(String id, String name, int level, int exp, int gold, int diamond,
                         int totalWins, int totalLosses, int currentWinStreak, int maxWinStreak, String avatar,
                         int totalGoldEarned, int totalGoldSpent, int achievementCount, int vipLevel) {
        this.id = blankTo(id, DEFAULT_ID);
        this.name = blankTo(name, DEFAULT_NAME);
        this.level = Math.max(MIN_LEVEL, level);
        this.exp = Math.max(0, exp);
        this.gold = Math.max(0, gold);
        this.diamond = Math.max(0, diamond);
        this.totalWins = Math.max(0, totalWins);
        this.totalLosses = Math.max(0, totalLosses);
        this.totalGames = this.totalWins + this.totalLosses;
        this.currentWinStreak = Math.max(0, currentWinStreak);
        this.maxWinStreak = Math.max(0, maxWinStreak);
        this.avatar = blankTo(avatar, DEFAULT_AVATAR);
        this.totalGoldEarned = Math.max(0, totalGoldEarned);
        this.totalGoldSpent = Math.max(0, totalGoldSpent);
        this.achievementCount = Math.max(0, achievementCount);
        this.vipLevel = Math.max(0, vipLevel);
    }

    // ============================================================= 只读派生值

    /**
     * 总场次 = 胜 + 负，落盘为 {@code "totalGames"}。
     *
     * <p>读写都保证与胜负场次一致：{@link #setTotalWins(int)} / {@link #setTotalLosses(int)}
     * 会即时重算该镜像值；{@link #setTotalGames(int)}（Jackson 读档用）只做越界归一。
     */
    public int getTotalGames() {
        return totalGames;
    }

    /** 写"总场次"（派生镜像）。包内可见，只允许 Jackson 读档时写入。 */
    @JsonSetter("totalGames")
    void setTotalGames(int totalGames) {
        this.totalGames = Math.max(0, totalGames);
    }

    /** 胜率，取值 [0,1]；未打过任何一局时返回 0。同样是派生值，不落盘。 */
    @JsonIgnore
    public double getWinRate() {
        int total = getTotalGames();
        return total == 0 ? 0.0 : (double) totalWins / total;
    }

    // ============================================================= getter / setter
    // setter 一律做越界归一，任何来源（JSON / 网络 / UI 输入）都不会把数据写成非法值。

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = blankTo(id, DEFAULT_ID);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = blankTo(name, DEFAULT_NAME);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(MIN_LEVEL, level);
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = Math.max(0, exp);
    }

    public int getGold() {
        return gold;
    }

    /**
     * 写金币。<b>故意做成包内可见</b>：金币的唯一合法入口是 {@link CoinService}，
     * 界面层 / 游戏层在编译期就无法直接改金币，只能通过
     * {@link CoinService#addGold(int, String)} / {@link CoinService#costGold(int, String)}。
     *
     * <p>{@code @JsonSetter} 让 Jackson 在反序列化 {@code player.json} 时仍然认这个方法，
     * 因此"类外不可见"不影响读档，同时保留负数归零的保护。
     */
    @JsonSetter("gold")
    void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public int getDiamond() {
        return diamond;
    }

    public void setDiamond(int diamond) {
        this.diamond = Math.max(0, diamond);
    }

    /** 胜利次数。 */
    public int getTotalWins() {
        return totalWins;
    }

    /**
     * 写"胜利次数"。{@code @JsonAlias("winCount")} 兼容阶段 12 之前存档使用的键名，
     * 老 {@code player.json} 里的 {@code winCount} 仍会正确读入；每次写入同步重算总场次。
     */
    @JsonAlias("winCount")
    public void setTotalWins(int totalWins) {
        this.totalWins = Math.max(0, totalWins);
        this.totalGames = this.totalWins + this.totalLosses;
    }

    /** 失败次数。 */
    public int getTotalLosses() {
        return totalLosses;
    }

    /** 写"失败次数"，{@code @JsonAlias("loseCount")} 兼容旧存档键名；每次写入同步重算总场次。 */
    @JsonAlias("loseCount")
    public void setTotalLosses(int totalLosses) {
        this.totalLosses = Math.max(0, totalLosses);
        this.totalGames = this.totalWins + this.totalLosses;
    }

    public String getAvatar() {
        return avatar == null || avatar.isBlank() ? DEFAULT_AVATAR : avatar;
    }

    // ============================================================= 连胜（战绩统计）

    /** 当前连胜场次。 */
    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    /**
     * 写"当前连胜"。包内可见：战绩统计的唯一入口是 {@link PlayerStatsService}，
     * 保证连胜与胜负场次同源、不会出现两处计数漂移。{@code @JsonSetter} 保证读档正常。
     */
    @JsonSetter("currentWinStreak")
    void setCurrentWinStreak(int currentWinStreak) {
        this.currentWinStreak = Math.max(0, currentWinStreak);
    }

    /** 历史最高连胜场次。 */
    public int getMaxWinStreak() {
        return maxWinStreak;
    }

    /** 写"历史最高连胜"。包内可见，只允许 {@link PlayerStatsService} 更新。 */
    @JsonSetter("maxWinStreak")
    void setMaxWinStreak(int maxWinStreak) {
        this.maxWinStreak = Math.max(0, maxWinStreak);
    }

    public void setAvatar(String avatar) {
        this.avatar = blankTo(avatar, DEFAULT_AVATAR);
    }

    // ============================================================= 成长 / 统计字段

    public int getTotalGoldEarned() {
        return totalGoldEarned;
    }

    /**
     * 写"累计获得金币"。与 {@link #setGold(int)} 同样是<b>包内可见</b>：
     * 只允许 {@link CoinService} 在成功加金币时更新，保证累计值永远跟随真实流水。
     * {@code @JsonSetter} 保证旧 / 新存档读档正常。
     */
    @JsonSetter("totalGoldEarned")
    void setTotalGoldEarned(int totalGoldEarned) {
        this.totalGoldEarned = Math.max(0, totalGoldEarned);
    }

    public int getTotalGoldSpent() {
        return totalGoldSpent;
    }

    /** 写"累计消耗金币"。包内可见，只允许 {@link CoinService} 在成功扣金币时更新。 */
    @JsonSetter("totalGoldSpent")
    void setTotalGoldSpent(int totalGoldSpent) {
        this.totalGoldSpent = Math.max(0, totalGoldSpent);
    }

    public int getAchievementCount() {
        return achievements.size();
    }

    public void setAchievementCount(int achievementCount) {
        this.achievementCount = Math.max(0, achievementCount);
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = Math.max(0, vipLevel);
    }

    // ============================================================= 成就（阶段 11）

    /** 已解锁成就 id 集合（只读视图，解锁唯一入口是 {@link AchievementService}）。 */
    public Set<String> getAchievements() {
        return java.util.Collections.unmodifiableSet(achievements);
    }

    /** Jackson 读档入口：兼容 null / 重复 / 空白 id，乱数据也不会写坏档案。 */
    @JsonSetter("achievements")
    void setAchievements(Collection<String> ids) {
        achievements.clear();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    achievements.add(id.trim());
                }
            }
        }
        achievementCount = achievements.size();
    }

    /** 解锁一条成就；返回本次是否新增（防重复发奖）。只允许 {@link AchievementService} 调用。 */
    boolean unlockAchievement(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        boolean added = achievements.add(id.trim());
        achievementCount = achievements.size();
        return added;
    }

    /** 背包内容（只读视图）。 */
    public List<Item> getInventory() {
        return java.util.Collections.unmodifiableList(inventory);
    }

    /** 按 id 查背包物品；不存在返回 null。 */
    public Item findItem(String id) {
        if (id == null) {
            return null;
        }
        for (Item item : inventory) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    /** 查找或按模板新建背包物品（数量从 0 开始）。 */
    Item getOrCreateItem(Item template) {
        Item owned = findItem(template.getId());
        if (owned == null) {
            owned = template.copy();
            owned.setCount(0);
            inventory.add(owned);
        }
        return owned;
    }

    /** Jackson 读档入口：兼容 null / 脏数据，旧存档无该键时保持空表。 */
    @JsonSetter("inventory")
    void setInventory(Collection<Item> items) {
        inventory.clear();
        if (items != null) {
            for (Item item : items) {
                if (item != null && item.getId() != null && !item.getId().isBlank()) {
                    inventory.add(item);
                }
            }
        }
    }

    // ============================================================= 道具状态（阶段 15）

    /** 幸运状态层数（幸运牌累计）。 */
    public int getLuckyCount() {
        return luckyCount;
    }

    /**
     * 写"幸运状态层数"。包内可见：唯一入口是 {@link ItemUseService}，
     * 保证状态只随道具使用增长。{@code @JsonSetter} 保证新旧存档读档正常（缺键时保持 0）。
     */
    @JsonSetter("luckyCount")
    void setLuckyCount(int luckyCount) {
        this.luckyCount = Math.max(0, luckyCount);
    }

    /** 双倍经验状态层数（双倍经验卡累计）。 */
    public int getDoubleExpCount() {
        return doubleExpCount;
    }

    /** 写"双倍经验状态层数"。包内可见，只允许 {@link ItemUseService} 更新。 */
    @JsonSetter("doubleExpCount")
    void setDoubleExpCount(int doubleExpCount) {
        this.doubleExpCount = Math.max(0, doubleExpCount);
    }

    /** 深拷贝：用于"加载失败回退默认档"时避免共享可变实例。 */
    public PlayerProfile copy() {
        PlayerProfile clone = new PlayerProfile(id, name, level, exp, gold, diamond, totalWins, totalLosses,
                currentWinStreak, maxWinStreak, avatar,
                totalGoldEarned, totalGoldSpent, achievementCount, vipLevel);
        clone.setAchievements(achievements);
        // 背包深拷贝：两个档案不共享同一个 Item 实例
        for (Item item : inventory) {
            clone.inventory.add(item.copy());
        }
        // 道具状态层数同步（阶段 15）
        clone.luckyCount = luckyCount;
        clone.doubleExpCount = doubleExpCount;
        return clone;
    }

    @Override
    public String toString() {
        return "PlayerProfile{" + name + " Lv." + level + " gold=" + gold + " diamond=" + diamond
                + " exp=" + exp + " win=" + totalWins + " lose=" + totalLosses + "}";
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
