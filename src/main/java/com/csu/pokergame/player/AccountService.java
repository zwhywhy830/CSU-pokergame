package com.csu.pokergame.player;

import com.csu.pokergame.network.JsonCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 账号服务（阶段 16）：<b>账号管理的唯一入口</b>。
 *
 * <p>引入本类之前，全工程只有一个 {@code player.json}，也就是"一台机器一个玩家"。
 * 现在改成"一个账号一份存档"：
 * <pre>{@code
 * data/
 *  ├── accounts.json          账号表（PlayerAccount 数组）
 *  └── players/
 *        ├── default.json           存档（PlayerProfile）
 *        ├── default_coin_log.json  金币流水
 *        ├── playerA.json
 *        ├── playerA_coin_log.json
 *        └── ...
 * }</pre>
 *
 * <p>登录 / 注册的本质是<b>把"当前存档文件"换掉</b>：
 * <ol>
 *   <li>{@link PlayerManager#switchPlayer(Path)}：保存旧档 → 换路径 → 重新载入；</li>
 *   <li>{@link CoinService#switchLogFile(Path)}：把金币流水换成该账号自己的流水文件；</li>
 *   <li>{@link AchievementService#reloadFromProfile()}：清掉上一个账号留在内存里的成就解锁标记。</li>
 * </ol>
 * 其余服务（背包 / 商城 / 成长 / 战绩 / 成就 / 充值）都只持有 {@link PlayerManager} 单例引用、
 * 并且每次调用都现读 {@code getProfile()}，所以玩家文件一换它们自动跟随，无需改动。
 *
 * <p><b>存档目录</b>：优先取系统属性 {@code -Dplayer.data.dir=...}，否则用工程根目录下的
 * {@code data/}；找不到工程根时退回当前工作目录下的 {@code data/}。本类<b>不写死任何绝对路径</b>。
 *
 * <p><b>旧存档迁移</b>：{@code accounts.json} 不存在（或账号表为空）时自动执行一次迁移，
 * 把原来的 {@code player.json} / {@code coin_log.json} 复制成 {@code default} 账号的存档，
 * 金币、等级、经验、成就、背包、道具状态全部原样保留；原文件不删除，随时可以回退。
 */
public final class AccountService {

    /** 覆盖存档目录的系统属性：{@code -Dplayer.data.dir=...}。 */
    public static final String DATA_DIR_PROPERTY = "player.data.dir";
    /** 存档目录默认名。 */
    public static final String DATA_DIR_NAME = "data";
    /** 账号表文件名。 */
    public static final String ACCOUNTS_FILE_NAME = "accounts.json";
    /** 各账号存档所在子目录名。 */
    public static final String PLAYERS_DIR_NAME = "players";
    /** 迁移出来的老玩家账号名。 */
    public static final String LEGACY_ACCOUNT_USERNAME = "default";
    /** 迁移账号的初始密码（本地演示，可在登录后自行注册新账号）。 */
    public static final String LEGACY_ACCOUNT_PASSWORD = "123456";
    /** 账号名合法字符：字母（含中文）/ 数字 / 下划线，2 ~ 16 位。 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_]{2,16}$");
    /** 密码长度上限。 */
    private static final int MAX_PASSWORD_LENGTH = 32;

    private static volatile AccountService instance;

    private final Path dataDir;
    private final Path accountsFile;
    private final Path playersDir;
    /** 账号表：username → PlayerAccount（LinkedHashMap 保持注册顺序，界面展示稳定）。 */
    private final Map<String, PlayerAccount> accounts = new LinkedHashMap<>();
    /** 当前登录账号；未登录为 null。 */
    private PlayerAccount currentAccount;

    /** 生产用法：单例，存档目录由 {@link #resolveDataDir()} 决定。 */
    public static AccountService getInstance() {
        AccountService local = instance;
        if (local == null) {
            synchronized (AccountService.class) {
                local = instance;
                if (local == null) {
                    local = new AccountService(resolveDataDir());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 指定存档目录的构造器（测试 / 多档位）。构造时创建目录、必要时迁移旧档、加载账号表。 */
    public AccountService(Path dataDir) {
        this.dataDir = dataDir;
        this.accountsFile = dataDir.resolve(ACCOUNTS_FILE_NAME);
        this.playersDir = dataDir.resolve(PLAYERS_DIR_NAME);
        init();
    }

    // ============================================================= 初始化

    private void init() {
        try {
            Files.createDirectories(playersDir);
        } catch (IOException e) {
            warn("创建存档目录失败: " + playersDir + " -> " + e.getMessage());
        }
        if (Files.isRegularFile(accountsFile)) {
            loadAccounts();
        }
        if (accounts.isEmpty()) {
            // 首次启动 / accounts.json 为空或损坏：迁移旧存档，生成 default 账号
            migrateLegacyPlayer();
            saveAccounts();
        }
    }

    /** 读取 accounts.json（解析失败只记警告，交给调用方走迁移）。 */
    private void loadAccounts() {
        try (InputStream in = Files.newInputStream(accountsFile)) {
            List<PlayerAccount> loaded = JsonCodec.mapper().readValue(in,
                    JsonCodec.mapper().getTypeFactory().constructCollectionType(List.class, PlayerAccount.class));
            if (loaded != null) {
                for (PlayerAccount account : loaded) {
                    if (account != null && account.getUsername() != null && !account.getUsername().isBlank()) {
                        accounts.put(account.getUsername(), account);
                    }
                }
            }
        } catch (Exception e) {
            warn("读取账号表失败，将按旧存档迁移: " + accountsFile + " -> " + e.getMessage());
        }
    }

    /** 写账号表（全量覆盖，保持与内存一致）。 */
    private boolean saveAccounts() {
        try {
            Files.createDirectories(dataDir);
            JsonCodec.mapper().writerWithDefaultPrettyPrinter()
                    .writeValue(accountsFile.toFile(), new ArrayList<>(accounts.values()));
            return true;
        } catch (IOException e) {
            warn("保存账号表失败: " + accountsFile + " -> " + e.getMessage());
            return false;
        }
    }

    /**
     * 旧存档迁移：把老的单档数据搬进 {@code players/default.json}，并登记 default 账号。
     *
     * <p>迁移是<b>复制</b>而不是移动——原 {@code player.json} / {@code coin_log.json} 保持不动，
     * 出问题时可以直接回退。玩家档案整体拷贝，因此金币 / 等级 / 经验 / 成就 / 背包 / 道具状态
     * 一个字段都不会丢。
     */
    private void migrateLegacyPlayer() {
        Path targetProfile = playersDir.resolve(LEGACY_ACCOUNT_USERNAME + ".json");
        Path targetCoinLog = coinLogFileFor(relativePlayerFile(LEGACY_ACCOUNT_USERNAME));

        // 1) 玩家档案：老 player.json 存在就整体复制，否则写一份全新默认档
        Path legacyProfile = legacyPlayerFile();
        boolean copied = false;
        if (legacyProfile != null && Files.isRegularFile(legacyProfile)) {
            copied = copyFile(legacyProfile, targetProfile);
        }
        if (!copied) {
            writeProfile(targetProfile, new PlayerProfile());
        }

        // 2) 金币流水：老 coin_log.json 存在就复制，否则生成空流水（避免新账号读到旧流水）
        Path legacyCoinLog = legacyProfile == null ? null : legacyProfile.resolveSibling("coin_log.json");
        if (legacyCoinLog == null || !Files.isRegularFile(legacyCoinLog) || !copyFile(legacyCoinLog, targetCoinLog)) {
            writeEmptyCoinLog(targetCoinLog);
        }

        // 3) 登记 default 账号
        long now = System.currentTimeMillis();
        PlayerAccount account = new PlayerAccount("acc-" + LEGACY_ACCOUNT_USERNAME,
                LEGACY_ACCOUNT_USERNAME, LEGACY_ACCOUNT_PASSWORD,
                relativePlayerFile(LEGACY_ACCOUNT_USERNAME), now, 0L);
        accounts.put(account.getUsername(), account);
        System.out.println("[AccountService] 旧存档已迁移 -> " + targetProfile
                + "（账号 " + LEGACY_ACCOUNT_USERNAME + " / 密码 " + LEGACY_ACCOUNT_PASSWORD + "）");
    }

    /** 老版单档存档位置（{@code src/main/resources/player/player.json}），复用 PlayerManager 的解析规则。 */
    private static Path legacyPlayerFile() {
        try {
            return PlayerManager.resolveDefaultDataFile();
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================= 注册 / 登录 / 登出

    /**
     * 注册账号。
     *
     * <p>流程：校验输入 → 查重 → 建 {@link PlayerAccount} → 生成玩家存档文件 → 保存 accounts.json。
     * 新玩家的初始档就是 {@link PlayerProfile} 的默认值：金币 1000、等级 1、经验 0、背包与成就为空。
     *
     * @param username 账号名（字母 / 数字 / 下划线 / 中文，2 ~ 16 位）
     * @param password 密码（1 ~ 32 位）
     * @return 是否注册成功（账号名非法、密码非法、账号已存在都返回 false）
     */
    public synchronized boolean register(String username, String password) {
        String user = normalizeUsername(username);
        if (!isValidUsername(user) || !isValidPassword(password)) {
            return false;
        }
        if (accounts.containsKey(user)) {
            return false;
        }
        Path playerFile = playersDir.resolve(user + ".json");
        if (Files.isRegularFile(playerFile)) {
            // 存档已存在但账号表里没有：不覆盖别人的存档，直接拒绝
            warn("存档已存在，拒绝注册同名账号: " + playerFile);
            return false;
        }

        long now = System.currentTimeMillis();
        PlayerAccount account = new PlayerAccount("acc-" + user, user, password,
                relativePlayerFile(user), now, 0L);

        // 新玩家初始档：金币 1000 / 等级 1 / 经验 0 / 背包空 / 成就空；昵称默认与账号一致，便于区分多账号
        PlayerProfile fresh = new PlayerProfile();
        fresh.setId(user);
        fresh.setName(user);
        writeProfile(playerFile, fresh);
        writeEmptyCoinLog(coinLogFileFor(account.getPlayerFile()));

        accounts.put(user, account);
        saveAccounts();
        System.out.println("[AccountService] 注册成功: " + user + " -> " + account.getPlayerFile());
        return true;
    }

    /**
     * 登录。
     *
     * <p>流程：查账号 → 校验密码 → 更新 lastLoginTime 并落盘 → 切换 {@link PlayerManager} 的玩家文件 → 载入数据。
     *
     * @return 是否登录成功（账号不存在 / 密码错误返回 false，且不改动任何数据）
     */
    public synchronized boolean login(String username, String password) {
        PlayerAccount account = accounts.get(normalizeUsername(username));
        if (account == null || account.getPassword() == null || !account.getPassword().equals(password)) {
            return false;
        }
        account.setLastLoginTime(System.currentTimeMillis());
        currentAccount = account;
        saveAccounts();
        bindPlayerSources(account);
        System.out.println("[AccountService] 登录成功: " + account.getUsername()
                + " -> " + account.getPlayerFile());
        return true;
    }

    /** 登出：保存当前玩家数据并清空当前账号（存档文件保留）。 */
    public synchronized void logout() {
        if (currentAccount == null) {
            return;
        }
        PlayerManager.getInstance().save();
        System.out.println("[AccountService] 登出: " + currentAccount.getUsername());
        currentAccount = null;
    }

    /** 当前登录账号；未登录返回 null。 */
    public synchronized PlayerAccount getCurrentAccount() {
        return currentAccount == null ? null : currentAccount.copy();
    }

    /** 当前登录账号名；未登录返回 null。 */
    public synchronized String getCurrentUsername() {
        return currentAccount == null ? null : currentAccount.getUsername();
    }

    /** 是否已登录。 */
    public synchronized boolean isLoggedIn() {
        return currentAccount != null;
    }

    /** 全部账号（按注册顺序，返回副本）。 */
    public synchronized List<PlayerAccount> getAccounts() {
        List<PlayerAccount> copies = new ArrayList<>();
        for (PlayerAccount account : accounts.values()) {
            copies.add(account.copy());
        }
        return Collections.unmodifiableList(copies);
    }

    /** 账号是否已存在。 */
    public synchronized boolean hasAccount(String username) {
        return accounts.containsKey(normalizeUsername(username));
    }

    // ============================================================= 路径解析

    /** 存档根目录（{@code data/}）。 */
    public Path getDataDir() {
        return dataDir;
    }

    /** 账号表文件（{@code data/accounts.json}）。 */
    public Path getAccountsFile() {
        return accountsFile;
    }

    /** 各账号存档目录（{@code data/players/}）。 */
    public Path getPlayersDir() {
        return playersDir;
    }

    /** 账号对应的存档文件绝对路径。 */
    public Path resolvePlayerFile(PlayerAccount account) {
        if (account == null || account.getPlayerFile() == null) {
            return null;
        }
        return dataDir.resolve(account.getPlayerFile()).normalize();
    }

    /** 当前账号的存档文件绝对路径；未登录返回 null。 */
    public synchronized Path getCurrentPlayerFile() {
        return resolvePlayerFile(currentAccount);
    }

    // ============================================================= 内部

    /** 把"当前玩家数据源"整体切到该账号：玩家档案 + 金币流水 + 成就内存状态。 */
    private void bindPlayerSources(PlayerAccount account) {
        PlayerManager.getInstance().switchPlayer(resolvePlayerFile(account));
        CoinService.getInstance().switchLogFile(coinLogFileFor(account.getPlayerFile()));
        // 成就的解锁标记缓存在 AchievementService 内存里，换账号必须重新按新档案同步
        AchievementService.getInstance().reloadFromProfile();
    }

    /** 账号存档相对路径，例如 {@code players/playerA.json}。 */
    private static String relativePlayerFile(String username) {
        return PLAYERS_DIR_NAME + "/" + username + ".json";
    }

    /** 由存档相对路径推出该账号的流水文件绝对路径：{@code players/x.json → players/x_coin_log.json}。 */
    private Path coinLogFileFor(String playerFileRelativePath) {
        if (playerFileRelativePath == null) {
            return null;
        }
        String name = Paths.get(playerFileRelativePath).getFileName().toString();
        String base = name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
        return dataDir.resolve(PLAYERS_DIR_NAME).resolve(base + "_coin_log.json");
    }

    /** 把玩家档案写到指定文件（父目录自动创建）。 */
    private static void writeProfile(Path file, PlayerProfile profile) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonCodec.mapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), profile);
        } catch (IOException e) {
            warn("写入玩家存档失败: " + file + " -> " + e.getMessage());
        }
    }

    /** 生成一个空的金币流水文件（内容为 {@code []}）。 */
    private static void writeEmptyCoinLog(Path file) {
        if (file == null) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonCodec.mapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), new ArrayList<>());
        } catch (IOException e) {
            warn("写入金币流水失败: " + file + " -> " + e.getMessage());
        }
    }

    private static boolean copyFile(Path source, Path target) {
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            warn("复制旧存档失败: " + source + " -> " + target + " " + e.getMessage());
            return false;
        }
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    /** 账号名合法性：字母（含中文）/ 数字 / 下划线，2 ~ 16 位。同时天然挡掉路径穿越字符。 */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /** 密码合法性：非空白且不超过 {@link #MAX_PASSWORD_LENGTH}。 */
    public static boolean isValidPassword(String password) {
        return password != null && !password.isBlank() && password.length() <= MAX_PASSWORD_LENGTH;
    }

    /**
     * 解析存档目录：系统属性 → 工程根下的 {@code data/} → 工作目录下的 {@code data/}。
     * 与 {@link PlayerManager#resolveDefaultDataFile()} 的工程根查找规则一致。
     */
    static Path resolveDataDir() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        Path dir = Paths.get("").toAbsolutePath();
        for (Path cursor = dir; cursor != null; cursor = cursor.getParent()) {
            if (Files.isDirectory(cursor.resolve("src").resolve("main").resolve("resources"))) {
                return cursor.resolve(DATA_DIR_NAME);
            }
        }
        return dir.resolve(DATA_DIR_NAME);
    }

    private static void warn(String message) {
        System.err.println("[AccountService] " + message);
    }
}
