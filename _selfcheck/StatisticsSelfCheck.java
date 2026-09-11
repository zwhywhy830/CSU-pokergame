import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.GameRecord;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerStatistics;
import com.csu.pokergame.player.StatisticsService;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 阶段 19 玩家数据统计中心自检（无界面，纯数据）。
 *
 * <p>覆盖需求里的用例：
 * <ol>
 *   <li>空账号：统计全 0、胜率不是 NaN、最常游戏为「暂无」；</li>
 *   <li>玩家 A：10 局 7 胜 3 负 → totalGames=10、winRate=70%；</li>
 *   <li>连胜：按时间升序计算 currentWinStreak / maxWinStreak；</li>
 *   <li>金币：收入 5000 / 消费 1000 → 统计正确；</li>
 *   <li>不污染 player.json：存档里没有 statistics 字段；</li>
 *   <li>账号隔离：A 的统计不影响 B，切回后 A 数据不变；</li>
 *   <li>旧存档：没有统计字段的 player.json 仍能正常读取。</li>
 * </ol>
 *
 * <p>自检全程只在临时目录里造数据，不读写真实存档。
 */
public class StatisticsSelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("statchk");
        Path playerA = dir.resolve("playerA.json");
        Path playerB = dir.resolve("playerB.json");
        Files.writeString(playerA, "{}");
        Files.writeString(playerB, "{}");

        PlayerManager players = new PlayerManager(playerA);
        GameRecordService records = new GameRecordService(players);
        CoinService coins = new CoinService(players, dir.resolve("playerA_coin_log.json"));
        StatisticsService stats = new StatisticsService(records, coins, players);

        System.out.println("== 1. 空账号 ==");
        PlayerStatistics empty = stats.getStatistics();
        System.out.println("  " + empty);
        check("totalGames = 0", empty.getTotalGames() == 0);
        check("winRate = 0%（不是 NaN）", empty.getWinRatePercent() == 0);
        check("currentWinStreak = 0", empty.getCurrentWinStreak() == 0);
        check("最常游戏 = 暂无", PlayerStatistics.NO_GAME.equals(empty.getFavoriteGameText()));
        check("player.json 不含 statistics 字段",
                !Files.readString(playerA).toLowerCase().contains("statistic"));

        System.out.println();
        System.out.println("== 2. 玩家A：10 局 7 胜 3 负（模式：负负胜胜胜胜负胜胜胜）==");
        // 时间升序写入：L L W W W W L W W W
        boolean[] wins = {false, false, true, true, true, true, false, true, true, true};
        long base = 1789052910767L;
        for (int i = 0; i < wins.length; i++) {
            long t = base + i * 1000L;
            // 前 8 局跑得快、后 2 局骗子酒馆 → 最常游戏应为「跑得快」
            String type = i < 8 ? GameRecordService.GAME_PDK : "骗子酒馆";
            records.addRecord(new GameRecord(GameRecord.makeId(t), type, wins[i],
                    wins[i] ? 100 : -20, 50, t, GameRecordService.OPPONENT_AI));
        }
        PlayerStatistics a = stats.getStatistics();
        System.out.println("  " + a);
        check("totalGames = 10", a.getTotalGames() == 10);
        check("winCount = 7", a.getWinCount() == 7);
        check("loseCount = 3", a.getLoseCount() == 3);
        check("胜率 = 70%", a.getWinRatePercent() == 70);
        check("maxWinStreak = 4", a.getMaxWinStreak() == 4);
        check("currentWinStreak = 3（末尾三连胜）", a.getCurrentWinStreak() == 3);
        check("最常游戏 = 跑得快", "跑得快".equals(a.getFavoriteGameText()));
        check("getWinRate() = 0.7", Math.abs(stats.getWinRate() - 0.7) < 1e-9);
        check("getMaxWinStreak() = 4", stats.getMaxWinStreak() == 4);
        check("getFavoriteGame() = 跑得快", "跑得快".equals(stats.getFavoriteGame()));

        System.out.println();
        System.out.println("== 3. 金币：收入 5000 / 消费 1000 ==");
        coins.addGold(5000, "测试收入");
        coins.costGold(1000, "测试消费");
        PlayerStatistics a2 = stats.getStatistics();
        System.out.println("  " + a2);
        check("累计收入 = 5000", a2.getTotalGoldEarned() == 5000);
        check("累计消费 = 1000", a2.getTotalGoldSpent() == 1000);
        check("余额 = 1000 + 5000 - 1000 = 5000", coins.getGold() == 5000);

        System.out.println();
        System.out.println("== 4. 账号隔离：B 的统计不污染 A ==");
        players.switchPlayer(playerB);
        coins.switchLogFile(dir.resolve("playerB_coin_log.json"));
        check("B 初始 totalGames = 0", stats.getStatistics().getTotalGames() == 0);
        records.addRecord(GameRecordService.GAME_PDK, true, 100, 50, GameRecordService.OPPONENT_AI);
        records.addRecord("骗子酒馆", true, 100, 50, GameRecordService.OPPONENT_AI);
        coins.addGold(999, "B 收入");
        PlayerStatistics b = stats.getStatistics();
        System.out.println("  B = " + b);
        check("B totalGames = 2", b.getTotalGames() == 2);
        check("B 胜率 = 100%", b.getWinRatePercent() == 100);
        check("B 累计收入 = 999", b.getTotalGoldEarned() == 999);
        check("B 累计消费 = 0", b.getTotalGoldSpent() == 0);
        check("B 最常游戏 = 跑得快（并列取先出现）", "跑得快".equals(b.getFavoriteGameText()));

        players.switchPlayer(playerA);
        coins.switchLogFile(dir.resolve("playerA_coin_log.json"));
        PlayerStatistics aAgain = stats.getStatistics();
        System.out.println("  切回 A = " + aAgain);
        check("切回 A 后 totalGames 仍为 10", aAgain.getTotalGames() == 10);
        check("切回 A 后累计收入仍为 5000", aAgain.getTotalGoldEarned() == 5000);
        check("A / B 统计互相独立", aAgain.getTotalGames() != b.getTotalGames());

        System.out.println();
        System.out.println("== 5. 旧存档（无统计字段）正常读取 ==");
        Path oldFile = dir.resolve("oldPlayer.json");
        Files.writeString(oldFile, """
                {
                  "id": "player001",
                  "name": "旧玩家",
                  "level": 3,
                  "exp": 10,
                  "gold": 1000,
                  "diamond": 50,
                  "winCount": 30,
                  "loseCount": 13,
                  "avatar": "\u265b"
                }
                """);
        PlayerManager oldMgr = new PlayerManager(oldFile);
        GameRecordService oldRecords = new GameRecordService(oldMgr);
        CoinService oldCoins = new CoinService(oldMgr, dir.resolve("old_coin_log.json"));
        StatisticsService oldStats = new StatisticsService(oldRecords, oldCoins, oldMgr);
        PlayerStatistics old = oldStats.getStatistics();
        System.out.println("  " + old);
        check("旧档等级可读（Lv.3）", oldMgr.getProfile().getLevel() == 3);
        check("旧档战绩字段可读（winCount=30）", oldMgr.getProfile().getTotalWins() == 30);
        check("旧档无 records → 统计归零", old.getTotalGames() == 0 && old.getWinRatePercent() == 0);
        oldMgr.save();
        check("旧档保存后仍不含 statistics 字段",
                !Files.readString(oldFile).toLowerCase().contains("statistic"));

        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        if (failed > 0) {
            System.exit(1);
        }
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
}
