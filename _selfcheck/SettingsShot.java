import com.cards.ui.SettingsView;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.settings.SettingsService;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 离屏渲染设置中心页并输出 PNG，用于界面验证（阶段 20）。 */
public class SettingsShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "settings.png";
        double height = args.length > 1 ? Double.parseDouble(args[1]) : 900;
        Platform.startup(() -> {
            try {
                SettingsView view = new SettingsView(SettingsService.getInstance(),
                        AccountService.getInstance(), null, null, null);
                Scene scene = new Scene(view, 1100, height);
                scene.getStylesheets().add(SettingsShot.class.getResource("/app.css").toExternalForm());
                view.applyCss();
                view.layout();
                WritableImage image = scene.snapshot(null);
                write(image, new File(out));
                System.out.println("[SettingsShot] 已输出 " + new File(out).getAbsolutePath()
                        + " " + (int) image.getWidth() + "x" + (int) image.getHeight());
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
