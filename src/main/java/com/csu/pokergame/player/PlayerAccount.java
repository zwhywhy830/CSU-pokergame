package com.csu.pokergame.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 玩家账号模型（阶段 16）：{@code accounts.json} 里的一条记录。
 *
 * <p>一条账号 = 一套登录凭据 + 一个存档文件位置，因此同一个账号每次登录都会读到同一份
 * {@link PlayerProfile}，不同账号之间的金币 / 等级 / 经验 / 成就 / 背包完全隔离。
 *
 * <p>落盘形态（{@code data/accounts.json} 是它的数组）：
 * <pre>{@code
 * {
 *   "accountId": "acc-player001",
 *   "username": "player001",
 *   "password": "123456",
 *   "playerFile": "players/player001.json",
 *   "createTime": 1757000000000,
 *   "lastLoginTime": 1757003600000
 * }
 * }</pre>
 *
 * <p><b>playerFile 存的是相对路径</b>（相对 {@code data/} 目录），因此整个 {@code data/}
 * 目录可以整体拷贝 / 迁移到别的机器或目录，不会出现写死的绝对路径。
 *
 * <p>本类只负责持有数据。账号的新增 / 校验 / 切换全部由 {@link AccountService} 负责，
 * 布局可以类比 {@link PlayerProfile} 与 {@link PlayerManager} 的分工。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"accountId", "username", "password", "playerFile", "createTime", "lastLoginTime"})
public class PlayerAccount {

    /** 账号唯一标识（本阶段用 {@code acc-<username>} 生成，保证可读且稳定）。 */
    private String accountId;
    /** 登录名，同时决定存档文件名（{@code players/<username>.json}）。 */
    private String username;
    /** 登录密码（本阶段为本地演示，明文保存）。 */
    private String password;
    /** 存档文件相对路径（相对 data 目录），例如 {@code players/player001.json}。 */
    private String playerFile;
    /** 注册时间（epoch 毫秒）。 */
    private long createTime;
    /** 最近一次登录时间（epoch 毫秒）；从未登录过为 0。 */
    private long lastLoginTime;

    /** Jackson 反序列化用的无参构造。 */
    public PlayerAccount() {
    }

    public PlayerAccount(String accountId, String username, String password, String playerFile,
                         long createTime, long lastLoginTime) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.playerFile = playerFile;
        this.createTime = Math.max(0L, createTime);
        this.lastLoginTime = Math.max(0L, lastLoginTime);
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
        this.username = username == null ? null : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlayerFile() {
        return playerFile;
    }

    public void setPlayerFile(String playerFile) {
        this.playerFile = playerFile == null ? null : playerFile.trim();
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = Math.max(0L, createTime);
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = Math.max(0L, lastLoginTime);
    }

    // ============================================================= 工具

    /** 深拷贝：避免把内存里的账号对象交给外部后被就地改写。 */
    public PlayerAccount copy() {
        return new PlayerAccount(accountId, username, password, playerFile, createTime, lastLoginTime);
    }

    @Override
    public String toString() {
        return "PlayerAccount{" + username + " file=" + playerFile + "}";
    }
}
