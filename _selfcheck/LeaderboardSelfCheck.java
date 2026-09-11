import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.LeaderboardEntry;
import com.csu.pokergame.player.LeaderboardService;
import com.csu.pokergame.player.PlayerManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阶段 18 排行榜系统自检（无界面，纯数据）。
 *
 * <p>覆盖需求里的四组用例：
 * <ol>
 *   <li>金币榜 / 等级榜排序：B(5000,Lv10) &gt; C(3000,Lv8) &gt; A(1000,Lv5)；</li>
 *   <li>胜率榜门槛：不足 10 局的账号不进入胜率榜；</li>
 *   <li>账号隔离：切换账号后「我的排名」跟着变；</li>
 *   <li>旧存档兼容：没有排行榜字段的 player json 仍能正常读取。</li>
 * </ol>
 *
 * <p>自检全程只在临时目录里造数据，并把三个数据源系统属性都指向临时目录，
 * 保证不会读写真实存档（游戏可能正在运行）。
 */
public class LeaderboardSelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("lbcheck");
        // 把全局单例的数据源也锁进临时目录：自检绝不触碰真实存档
        System.setProperty(PlayerManager.DATA_FILE_PROPERTY, dir.resolve("sandbox.json").toString());
        System.setProperty("coin.log.file", dir.resolve("sandbox_coin.json").toString());

        // 旧存档：只有等级 / 金币 / 胜负，完全没有排行榜字段
        writeProfile(dir, "A", 5, 1000, 12, 8);    // 20 局 60%
        writeProfile(dir, "B", 10, 5000, 11, 5);   // 16 局 68.75%
        writeProfile(dir, "C", 8, 3000, 6, 10);    // 16 局 37.5%
        writeProfile(dir, "D", 3, 600, 3, 2);      // 5 局 60% —— 局数不足，胜率榜应排除
        writeAccounts(dir);

        AccountService accounts = new AccountService(dir);
        LeaderboardService lb = new LeaderboardService(accounts);

        System.out.println("== 1. 金币榜 / 等级榜排序 ==");
        List<LeaderboardEntry> gold = lb.getRankByGold();
        System.out.println("  金币榜 = " + names(gold));
        check("金币榜顺序 B > C > A > D", names(gold).equals("B,C,A,D"));
        List<LeaderboardEntry> level = lb.getRankByLevel();
        System.out.println("  等级榜 = " + names(level));
        check("等级榜顺序 B > C > A > D", names(level).equals("B,C,A,D"));

        System.out.println();
        System.out.println("== 2. 胜率榜：不足 10 局不入榜 ==");
        List<LeaderboardEntry> rate = lb.getRankByWinRate();
        System.out.println("  胜率榜 = " + names(rate));
        check("胜率榜顺序 B > A > C", names(rate).equals("B,A,C"));
        check("D 只有 5 局，被挡在胜率榜外", !names(rate).contains("D"));

        System.out.println();
        System.out.println("== 3. 账号隔离：切换账号后排名正确 ==");
        accounts.login("A", PASSWORD);
        check("A 金币榜 No.3", lb.getMyRankPosition(LeaderboardService.Board.GOLD) == 3);
        check("A 胜率榜 No.2", lb.getMyRankPosition(LeaderboardService.Board.WIN_RATE) == 2);
        check("A 看到的是自己", lb.getMyRank() != null && "A".equals(lb.getMyRank().getUsername()));
        accounts.login("B", PASSWORD);
        check("B 金币榜 No.1", lb.getMyRankPosition(LeaderboardService.Board.GOLD) == 1);
        check("B 看到的是自己", "B".equals(lb.getMyRank().getUsername()));
        accounts.login("C", PASSWORD);
        check("C 金币榜 No.2", lb.getMyRankPosition(LeaderboardService.Board.GOLD) == 2);
        check("C 胜率榜 No.3", lb.getMyRankPosition(LeaderboardService.Board.WIN_RATE) == 3);
        accounts.login("D", PASSWORD);
        check("D 胜率榜暂无排名（返回 0）",
                lb.getMyRankPosition(LeaderboardService.Board.WIN_RATE) == 0);
        check("D 金币榜 No.4", lb.getMyRankPosition(LeaderboardService.Board.GOLD) == 4);
        accounts.logout();
        check("登出后无排名（getMyRank = null）", lb.getMyRank() == null);

        System.out.println();
        System.out.println("== 4. 旧存档兼容 / 不污染当前玩家 ==");
        check("缺排行榜字段的旧档仍读出正确等级", levelOf(level, "B") == 10);
        check("旧档金币正确", goldOf(gold, "B") == 5000);
        check("旧档胜率正确（11/16 = 69%）", percentOf(rate, "B") == 69);
        check("没有存档的账号不入榜", gold.size() == 4);
        check("排行榜不切换当前玩家（仍是最后登录的 D）",
                "D".equals(PlayerManager.getInstance().getProfile().getId()));
        check("E 没有存档文件 → 不出现在榜单", !names(gold).contains("E"));

        summary();
    }

    private static final String PASSWORD = "123456";

    /** 写一份"旧版"玩家存档：只有 id / name / 等级 / 金币 / 胜负，没有任何排行榜字段。 */
    private static void writeProfile(Path dir, String user, int level, int gold, int wins, int losses)
            throws Exception {
        Path players = Files.createDirectories(dir.resolve("players"));
        String json = "{\n"
                + "  \"id\" : \"" + user + "\",\n"
                + "  \"name\" : \"" + user + "\",\n"
                + "  \"level\" : " + level + ",\n"
                + "  \"exp\" : 0,\n"
                + "  \"gold\" : " + gold + ",\n"
                + "  \"totalWins\" : " + wins + ",\n"
                + "  \"totalLosses\" : " + losses + "\n"
                + "}\n";
        Files.writeString(players.resolve(user + ".json"), json);
    }

    /** 写 accounts.json：A ~ D 有存档，E 故意不建存档（验证"无档账号不入榜"）。 */
    private static void writeAccounts(Path dir) throws Exception {
        List<String> users = new ArrayList<>(List.of("A", "B", "C", "D", "E"));
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < users.size(); i++) {
            String u = users.get(i);
            sb.append("  {\n")
                    .append("    \"accountId\" : \"acc-").append(u).append("\",\n")
                    .append("    \"username\" : \"").append(u).append("\",\n")
                    .append("    \"password\" : \"").append(PASSWORD).append("\",\n")
                    .append("    \"playerFile\" : \"players/").append(u).append(".json\",\n")
                    .append("    \"createTime\" : 1789052910767,\n")
                    .append("    \"lastLoginTime\" : 0\n")
                    .append("  }").append(i == users.size() - 1 ? "\n" : ",\n");
        }
        sb.append("]\n");
        Files.writeString(dir.resolve("accounts.json"), sb.toString());
    }

    /** 榜单用户名序列，例如 {@code "B,C,A,D"}。 */
    private static String names(List<LeaderboardEntry> list) {
        return list.stream().map(LeaderboardEntry::getUsername).collect(Collectors.joining(","));
    }

    private static int levelOf(List<LeaderboardEntry> list, String user) {
        for (LeaderboardEntry e : list) {
            if (user.equals(e.getUsername())) {
                return e.getLevel();
            }
        }
        return -1;
    }

    private static int goldOf(List<LeaderboardEntry> list, String user) {
        for (LeaderboardEntry e : list) {
            if (user.equals(e.getUsername())) {
                return e.getGold();
            }
        }
        return -1;
    }

    private static int percentOf(List<LeaderboardEntry> list, String user) {
        for (LeaderboardEntry e : list) {
            if (user.equals(e.getUsername())) {
                return e.getWinRatePercent();
            }
        }
        return -1;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    private static void summary() {
        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
