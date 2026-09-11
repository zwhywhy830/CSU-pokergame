import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.liar.LiarActionBar;
import com.cards.ui.liar.LiarHandView;
import com.cards.ui.liar.LiarPlayerSeat;
import com.cards.ui.liar.LiarTableView;
import com.csu.pokergame.core.card.Deck;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * 阶段 24 骗子酒馆牌桌离屏截图。
 *
 * <p>用与 {@code DeckApp} 完全相同的 {@code com.cards.ui.liar} 组件与样式类拼出牌桌：
 * 顶部信息栏（阶段 / 声明 / 存活 / 金币）、环形四玩家座位、中央声明卡 + 怀疑度、
 * 底部本人手牌（正面可见）+ 操作按钮、右侧日志。
 *
 * <p>参数：{@code [输出路径] [fx]}，第二参数为 {@code fx} 时额外触发一次质疑特效
 * （暗红屏幕闪烁），用于验证动画层。
 */
public class LiarUIShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "stage24_liar_table.png";
        boolean withFx = args.length > 1 && "fx".equalsIgnoreCase(args[1]);

        Platform.startup(() -> {
            try {
                LiarTableView view = new LiarTableView();
                view.setPrefSize(1160, 700);

                // 顶部信息
                view.setPhaseText("应答");
                view.setAliveText(4);
                view.setTipText("目标点数：K");
                view.getCoinBar().setCoins(2680);

                // 四玩家座位（0 本人 / 1~3 AI）
                String[] names = {"你", "AI 张三", "AI 李四", "AI 王五"};
                int[] levels = {12, 8, 8, 9};
                int[] lifes = {3, 2, 3, 1};
                for (int i = 0; i < LiarTableView.SEAT_COUNT; i++) {
                    LiarPlayerSeat seat = view.getSeat(i);
                    seat.setPlayerName(names[i]);
                    seat.setLevel(levels[i]);
                    seat.setLife(lifes[i]);
                }
                view.getSeat(0).setNormal();
                view.getSeat(1).setThinking();
                view.getSeat(2).setBluffing();
                view.getSeat(3).setDead();

                // 中央声明卡 + 怀疑度
                view.getClaimPanel().setDeclarer("AI 李四");
                view.getClaimPanel().setClaim("三张 K");
                view.getClaimPanel().setCredibility("未知");
                view.getHeaderClaim().setDeclarer("AI 李四");
                view.getHeaderClaim().setClaim("三张 K");
                view.getHeaderClaim().setCredibility("未知");
                view.getRiskIndicator().setValue(0.65);

                // 本人手牌：正面可见
                LiarHandView hand = view.getHandView();
                hand.setCards(Deck.liarPoker(new Random(2026)).draw(5));
                hand.setInteractive(true);
                hand.setMaxSelect(3);

                LiarActionBar bar = view.getActionBar();
                bar.setDeclareEnabled(true);
                bar.setContinueEnabled(true);
                bar.setChallengeEnabled(true);

                view.setLogEntries(List.of(
                        "=== 第 1 局 ===",
                        "AI 张三 宣告 三张 K",
                        "你 选择相信",
                        "AI 李四 宣告 三张 K",
                        "AI 王五 质疑 → 结果：说谎（AI 李四 扣扳机）",
                        "AI 李四 被淘汰",
                        "AI 王五 宣告 两张 K",
                        "轮到你：继续（相信）或质疑"));

                StackPane root = new StackPane(view);
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1211, #0a0708);");
                root.setPadding(new Insets(18));

                Scene scene = new Scene(root, 1220, 760);
                scene.getStylesheets().add(LiarUIShot.class.getResource("/app.css").toExternalForm());
                root.applyCss();
                root.layout();

                if (withFx) {
                    view.playChallengeEffect();
                }

                PauseTransition wait = new PauseTransition(Duration.millis(700));
                wait.setOnFinished(e -> {
                    try {
                        WritableImage image = scene.snapshot(null);
                        write(image, new File(out));
                        System.out.println("[LiarUIShot] 已输出 " + new File(out).getAbsolutePath()
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
