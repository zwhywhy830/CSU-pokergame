import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.player.PlayerStatsService;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 阶段 12 战绩系统自检：
 * <ol>
 *   <li>旧存档兼容：只有 {@code winCount} / {@code loseCount} 的 player.json 能否正确读出战绩；</li>
 *   <li>{@link PlayerStatsService#recordGameResult(boolean)} 的胜负 / 连胜 / 落盘行为。</li>
 * </ol>
 */
public class StatsSelfCheck {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("statscheck");
        Path playerFile = dir.resolve("player.json");
        Path logFile = dir.resolve("coin_log.json");

        // 旧格式存档：只有 winCount / loseCount，没有 totalGames / totalWins / totalLosses
        Files.writeString(playerFile, """
                {
                  "id": "player001",
                  "name": "玩家",
                  "level": 3,
                  "exp": 10,
                  "gold": 1000,
                  "diamond": 50,
                  "winCount": 30,
                  "loseCount": 13,
                  "avatar": "\u265b"
                }
                """);

        PlayerManager players = new PlayerManager(playerFile);
        CoinService coins = new CoinService(players, logFile);
        PlayerStatsService stats = new PlayerStatsService(players);

        System.out.println("== 旧存档兼容（winCount=30, loseCount=13，无新字段）==");
        PlayerProfile p = players.getProfile();
        System.out.println("totalGames=" + p.getTotalGames() + " totalWins=" + p.getTotalWins()
                + " totalLosses=" + p.getTotalLosses());
        System.out.println("snapshot=" + stats.snapshot());

        System.out.println();
        System.out.println("== recordGameResult(true) x6（验证连胜）==");
        for (int i = 0; i < 6; i++) {
            stats.recordGameResult(true);
        }
        System.out.println("胜场=" + stats.getWinGames() + " 当前连胜=" + stats.getCurrentWinStreak()
                + " 最高连胜=" + stats.getMaxWinStreak() + " 胜率=" + stats.getWinRatePercent() + "%");

        System.out.println();
        System.out.println("== recordGameResult(false)（验证连胜清零 / 最高连胜保留）==");
        stats.recordGameResult(false);
        System.out.println("负场=" + stats.getLoseGames() + " 当前连胜=" + stats.getCurrentWinStreak()
                + "（应为 0） 最高连胜=" + stats.getMaxWinStreak() + "（应为 6）");

        System.out.println();
        System.out.println("== 落盘后的 player.json ==");
        System.out.println(Files.readString(playerFile));
    }
}
