import com.cards.ui.component.GameToast;
import com.cards.ui.component.PlayedCardsView;
import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.pdk.PdkHandView;
import com.cards.ui.pdk.PdkPlayerSeat;
import com.cards.ui.pdk.PdkTableHeader;
import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 阶段 23 湖南跑得快牌桌离屏截图。
 *
 * <p>用与 {@code DeckApp} 完全相同的 {@code ui/pdk} 组件与样式类拼出牌桌：
 * 顶部比赛信息栏 + AI 玩家座位、中央玻璃出牌区 + 状态提示卡、
 * 底部本人座位 + 手牌 + 操作按钮，右侧日志栏。
 *
 * <p>可选第二参数 {@code toast} 会额外触发一次「💣 炸弹！」提示条，用于验证状态提示动画。
 */
public class PdkUIShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "stage23_pdk_table.png";
        boolean withToast = args.length > 1 && "toast".equalsIgnoreCase(args[1]);

        Platform.startup(() -> {
            try {
                BorderPane table = new BorderPane();
                table.getStyleClass().add("table-root");
                table.setPrefSize(1180, 720);

                // ---------------- 顶部：比赛信息栏 + AI 玩家区域 ----------------
                PdkTableHeader header = new PdkTableHeader();
                header.setMode("本地人机");
                header.setBaseScore(50);
                header.setRound(3);
                header.setRemaining(List.of("你 13", "西家 8", "北家 10"));
                header.getCoinBar().setCoins(2680);

                PdkPlayerSeat seatWest = new PdkPlayerSeat("♚", "AI-西家", 8, false);
                seatWest.setCardCount(8);
                seatWest.setState(PdkPlayerSeat.State.THINKING);

                PdkPlayerSeat seatNorth = new PdkPlayerSeat("♝", "AI-北家", 9, false);
                seatNorth.setCardCount(10);
                seatNorth.setState(PdkPlayerSeat.State.WAITING);

                HBox seatStrip = new HBox(30, seatWest, seatNorth);
                seatStrip.getStyleClass().add("pdk-seat-strip");
                seatStrip.setAlignment(Pos.CENTER);

                VBox topBar = new VBox(10, header, seatStrip);
                topBar.setAlignment(Pos.CENTER);
                topBar.setPadding(new Insets(10, 14, 8, 14));
                table.setTop(topBar);

                // ---------------- 中央：玻璃牌桌 + 出牌区 + 状态提示卡 ----------------
                PlayedCardsView center = new PlayedCardsView();
                center.getStyleClass().add("pdk-center");
                center.setCards(List.of(
                        new Card(Rank.NINE, Suit.SPADES),
                        new Card(Rank.NINE, Suit.HEARTS),
                        new Card(Rank.NINE, Suit.CLUBS),
                        new Card(Rank.FIVE, Suit.DIAMONDS)), "西家");

                Label status = new Label("西家 出牌 · 三带一");
                status.getStyleClass().addAll("table-hint", "pdk-status-card");
                status.setMaxWidth(Double.MAX_VALUE);
                status.setAlignment(Pos.CENTER);

                VBox centerCol = new VBox(14, center, status);
                centerCol.setAlignment(Pos.CENTER);
                StackPane centerWrap = new StackPane(centerCol);
                centerWrap.getStyleClass().addAll("table-center-wrap", "pdk-table");
                table.setCenter(centerWrap);

                // ---------------- 底部：本人座位 + 手牌 + 操作按钮 ----------------
                Set<Card> selected = new LinkedHashSet<>();
                PdkHandView hand = new PdkHandView(selected);
                hand.getStyleClass().add("pdk-bottom");
                hand.getSeat().setLevel(12);
                hand.getSeat().setPlayerName("你");
                hand.getSeat().setCardCount(13);
                hand.getSeat().setState(PdkPlayerSeat.State.ACTIVE);

                List<Card> hole = sortedHand();
                selected.add(hole.get(0));
                selected.add(hole.get(1));
                selected.add(hole.get(2));
                hand.setCards(hole);
                hand.setInteractive(true);
                hand.getActionBar().setPlayEnabled(true);
                hand.getActionBar().setPassEnabled(true);
                table.setBottom(hand);

                // ---------------- 右侧：对局日志 ----------------
                ListView<String> log = new ListView<>();
                log.getStyleClass().add("table-log");
                log.setPrefWidth(220);
                log.getItems().addAll(
                        "第 3 局开始",
                        "你 先手出牌",
                        "你 出牌 顺子 3-4-5-6-7",
                        "西家 出牌 对子 K",
                        "北家 不要",
                        "你 出牌 三带一 999+5",
                        "西家 出牌 三带一 999+5",
                        "北家 不要");
                table.setRight(log);

                StackPane root = new StackPane(table);
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #1d2b26, #0b1210);");
                root.setPadding(new Insets(20));

                Scene scene = new Scene(root, 1220, 760);
                scene.getStylesheets().add(PdkUIShot.class.getResource("/app.css").toExternalForm());
                root.applyCss();
                root.layout();

                if (withToast) {
                    GameToast.show(table, "💣 炸弹！", Duration.millis(6000));
                }

                PauseTransition wait = new PauseTransition(Duration.millis(700));
                wait.setOnFinished(e -> {
                    try {
                        WritableImage image = scene.snapshot(null);
                        write(image, new File(out));
                        System.out.println("[PdkUIShot] 已输出 " + new File(out).getAbsolutePath()
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

    /** 构造一副 13 张的手牌（含一组可出牌型的对子与单张）。 */
    private static List<Card> sortedHand() {
        List<Card> hole = new ArrayList<>();
        hole.add(new Card(Rank.KING, Suit.SPADES));
        hole.add(new Card(Rank.KING, Suit.HEARTS));
        hole.add(new Card(Rank.JACK, Suit.CLUBS));
        hole.add(new Card(Rank.TEN, Suit.SPADES));
        hole.add(new Card(Rank.TEN, Suit.HEARTS));
        hole.add(new Card(Rank.EIGHT, Suit.DIAMONDS));
        hole.add(new Card(Rank.SEVEN, Suit.CLUBS));
        hole.add(new Card(Rank.SIX, Suit.SPADES));
        hole.add(new Card(Rank.SIX, Suit.HEARTS));
        hole.add(new Card(Rank.FIVE, Suit.CLUBS));
        hole.add(new Card(Rank.FOUR, Suit.DIAMONDS));
        hole.add(new Card(Rank.FOUR, Suit.CLUBS));
        hole.add(new Card(Rank.THREE, Suit.SPADES));
        return hole;
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
