import com.cards.ui.ProfileView;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.CoinLogService;
import com.csu.pokergame.player.InventoryService;
import com.csu.pokergame.player.CoinRechargeService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.ItemUseService;
import com.csu.pokergame.player.LeaderboardService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerStatsService;
import com.csu.pokergame.player.ShopService;
import com.csu.pokergame.player.StatisticsService;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 离屏渲染个人中心页（含成就区域）并输出 PNG，用于界面验证。 */
public class ProfileShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "profile_achievements.png";
        // 可选第二个参数：画布高度。整页很高时（例如要看到最下方的排行榜）传个更大的值
        double height = args.length > 1 ? Double.parseDouble(args[1]) : 900;
        Platform.startup(() -> {
            try {
                ProfileView view = new ProfileView(PlayerManager.getInstance(), CoinService.getInstance(),
                        PlayerGrowthService.getInstance(), PlayerStatsService.getInstance(),
                        AchievementService.getInstance(), CoinRechargeService.getInstance(),
                        CoinLogService.getInstance(), InventoryService.getInstance(),
                        ShopService.getInstance(), ItemUseService.getInstance(),
                        GameRecordService.getInstance(), LeaderboardService.getInstance(),
                        StatisticsService.getInstance(),
                        AccountService.getInstance(),
                        null, null, null);
                Scene scene = new Scene(view, 1000, height);
                scene.getStylesheets().add(ProfileShot.class.getResource("/app.css").toExternalForm());
                view.applyCss();
                view.layout();
                WritableImage image = scene.snapshot(null);
                write(image, new File(out));
                System.out.println("[ProfileShot] 已输出 " + new File(out).getAbsolutePath()
                        + " " + (int) image.getWidth() + "x" + (int) image.getHeight());
                // 直接退出：Platform.exit() 会在工具包 runLoop 里触发一次已知的退出竞态告警，出图后已无必要
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static void write(WritableImage image, File file) throws Exception {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buf.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        ImageIO.write(buf, "png", file);
    }
}
