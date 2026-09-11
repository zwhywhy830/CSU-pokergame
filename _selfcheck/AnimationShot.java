import com.cards.ui.component.AvatarView;
import com.cards.ui.component.GameToast;
import com.cards.ui.component.GrowthResultPanel;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.player.PlayerGrowthService;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 阶段 22 表现增强离屏截图：
 * 渲染「结算界面（🏆 胜利 / 金币 / 经验 / ✨ Level UP）」+「头像升级光环」+「统一提示条」，
 * 停留约 1.7 秒让数字滚动到终值、光环点亮，再快照输出 PNG，用于界面验证。
 */
public class AnimationShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "stage22_animation.png";
        double height = args.length > 1 ? Double.parseDouble(args[1]) : 760;
        Platform.startup(() -> {
            try {
                StackPane root = new StackPane();
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #2a3d50, #1a2733);");
                root.setPadding(new Insets(28));

                Label title = new Label("阶段 22 · 结算表现增强");
                title.getStyleClass().add("game-choice-title");
                Label sub = new Label("胜利动画 · 金币飞入 · 升级光环 · 统一提示条");
                sub.getStyleClass().add("game-choice-sub");

                AvatarView avatar = new AvatarView("🃏", "玩家一号", "在线", 13);
                avatar.setGrowthInfo(350, 1300, 1288, 65);

                PlayerGrowthService.LevelUpResult levelUp =
                        new PlayerGrowthService.LevelUpResult(12, 13, 1, 650);
                GrowthResultPanel panel = new GrowthResultPanel(
                        true, 100, 50, 13, "白银", "🥈", 350, 1300, levelUp);

                VBox center = new VBox(18, title, sub, avatar, panel);
                center.setAlignment(Pos.CENTER);
                center.setMaxWidth(520);
                root.getChildren().add(center);

                Scene scene = new Scene(root, 1100, height);
                scene.getStylesheets().add(AnimationShot.class.getResource("/app.css").toExternalForm());
                root.applyCss();
                root.layout();

                // 头像升级光环（持续 2 秒）
                avatar.playLevelUpGlow();
                // 统一提示条（加长停留，便于截图捕捉）
                GameToast.show(root, "＋100 金币 · ＋50 经验 · ✨ Level UP Lv.12 → Lv.13",
                        Duration.millis(6000), true);

                // 等数字滚动到终值 + 光环点亮后快照
                PauseTransition wait = new PauseTransition(Duration.millis(1700));
                wait.setOnFinished(e -> {
                    try {
                        WritableImage image = scene.snapshot(null);
                        write(image, new File(out));
                        System.out.println("[AnimationShot] 已输出 " + new File(out).getAbsolutePath()
                                + " " + (int) image.getWidth() + "x" + (int) image.getHeight()
                                + " 动画开关=" + GameAnimationService.getInstance().isEnabled());
                        System.exit(0);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.exit(1);
                    }
                });
                wait.play();
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
