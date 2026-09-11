import com.cards.ui.SettingsView;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.settings.SettingsService;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 离屏渲染设置中心页（含音频区「🔊 测试按钮音效」）并输出 PNG，用于界面验证。 */
public class AudioShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "stage21_settings.png";
        double height = args.length > 1 ? Double.parseDouble(args[1]) : 980;
        Platform.startup(() -> {
            try {
                AudioService.getInstance().refreshSettings();
                SettingsView view = new SettingsView(SettingsService.getInstance(),
                        AccountService.getInstance(), null, null, null);
                Scene scene = new Scene(view, 1100, height);
                scene.getStylesheets().add(AudioShot.class.getResource("/app.css").toExternalForm());
                view.applyCss();
                view.layout();
                WritableImage image = scene.snapshot(null);
                write(image, new File(out));
                System.out.println("[AudioShot] 已输出 " + new File(out).getAbsolutePath()
                        + " " + (int) image.getWidth() + "x" + (int) image.getHeight());
                System.out.println("音效开关=" + AudioService.getInstance().isSoundEnabled()
                        + " 音乐开关=" + AudioService.getInstance().isMusicEnabled()
                        + " 音量=" + AudioService.getInstance().getVolume());
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
