import com.csu.pokergame.player.Achievement;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerStatsService;

/** 阶段 11 成就系统自检：走一遍 6 个触发点，校验解锁与奖励只发一次。 */
public class AchievementSelfCheck {

    public static void main(String[] args) {
        PlayerManager players = PlayerManager.getInstance();
        CoinService coins = CoinService.getInstance();
        PlayerGrowthService growth = PlayerGrowthService.getInstance();
        PlayerStatsService stats = PlayerStatsService.getInstance();
        AchievementService svc = AchievementService.getInstance();

        System.out.println("成就总数=" + svc.getTotalCount() + " 已解锁=" + svc.getUnlockedCount());

        // FIRST_GAME
        System.out.println("进入游戏 -> " + names(svc.checkOnGameStart()));
        int goldAfterFirstGame = coins.getGold();

        // FIRST_WIN + WIN_STREAK_5
        players.getProfile().setLevel(1);
        for (int i = 0; i < 5; i++) {
            stats.recordWin();
            System.out.println("第 " + (i + 1) + " 胜 -> " + names(svc.checkOnWin()));
        }

        // GOLD_10000
        coins.addGold(20000, "自检");
        System.out.println("金币变化 -> " + names(svc.checkOnGoldChange()));

        // LEVEL_10 / LEVEL_20
        players.getProfile().setLevel(20);
        System.out.println("升级 -> " + names(svc.checkOnLevelUp()));

        // 幂等校验：再跑一遍不应重复解锁 / 重复发奖
        int before = svc.getUnlockedCount();
        svc.evaluateAll();
        System.out.println("重复检测后 已解锁=" + svc.getUnlockedCount() + "（应仍为 " + before + "）");

        System.out.println("已解锁列表=" + svc.getUnlockedAchievements());
        System.out.println("金币: 首局后=" + goldAfterFirstGame + " 现在=" + coins.getGold());
        System.out.println("等级=" + players.getProfile().getLevel() + " 经验=" + players.getProfile().getExp());
        System.out.println("存档 achievements=" + players.getProfile().getAchievements()
                + " achievementCount=" + players.getProfile().getAchievementCount());
    }

    private static String names(java.util.List<Achievement> list) {
        return list.isEmpty() ? "(无)" : list.toString();
    }
}
