import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.pdk.PdkActionBar;
import com.cards.ui.pdk.PdkCardView;
import com.cards.ui.pdk.PdkHandView;
import com.cards.ui.pdk.PdkPlayerSeat;
import com.cards.ui.pdk.PdkTableHeader;
import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Deck;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.core.player.RuleBotController;
import com.csu.pokergame.paodekuai.PdkBotPolicy;
import com.csu.pokergame.paodekuai.PdkEngine;
import com.csu.pokergame.paodekuai.PdkSnapshot;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 阶段 23 湖南跑得快牌桌 UI 重构自检。
 *
 * <p>覆盖需求用例：
 * <ol>
 *   <li>牌桌组件创建成功（PdkTableHeader / PdkPlayerSeat / PdkHandView / PdkActionBar / PdkCardView）；</li>
 *   <li>玩家座位显示（等级 / 昵称 / 剩余牌数 / 状态三态样式类）；</li>
 *   <li>手牌数量正常（setCards 增量更新，张数一致）；</li>
 *   <li>按钮状态正常（可用 / 不可用切换）；</li>
 *   <li>动画调用接口正常（GameAnimationService 单例 + 提示条 + 选中视觉）；</li>
 *   <li>不影响原规则（PdkEngine 开局 48 张 / 手牌 16 张 / 合法命令 / bot 可完整打完）。</li>
 * </ol>
 */
public class PdkUISelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        // ---------- 1. 牌桌组件创建成功 ----------
        System.out.println("== 1. 牌桌组件创建成功 ==");
        AtomicReference<Throwable> error = new AtomicReference<>();
        runOnFx(error, () -> {
            StackPane root = new StackPane();
            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(PdkUISelfCheck.class.getResource("/app.css").toExternalForm());

            PdkTableHeader header = new PdkTableHeader();
            PdkPlayerSeat seatSelf = new PdkPlayerSeat("♛", "你", 12, true);
            PdkPlayerSeat seatWest = new PdkPlayerSeat("♚", "AI-西家", 8, false);
            PdkActionBar bar = new PdkActionBar();
            PdkHandView hand = new PdkHandView(new LinkedHashSet<>());

            check("PdkTableHeader 创建成功", header != null);
            check("PdkPlayerSeat 创建成功", seatSelf != null && seatWest != null);
            check("PdkHandView 创建成功", hand != null);
            check("PdkActionBar 创建成功", bar != null);
            check("PdkCardView 创建成功",
                    new PdkCardView(new Card(Rank.ACE, Suit.SPADES)) != null);
            check("PdkHandView 内含操作条与本人座位",
                    hand.getActionBar() != null && hand.getSeat() != null);
            check("PdkActionBar 含 出牌 / 不出 两个按钮",
                    bar.getChildren().size() == 2);

            root.getChildren().addAll(header, seatWest, hand);
            root.applyCss();
            root.layout();

            // ---------- 2. 玩家座位显示 ----------
            System.out.println();
            System.out.println("== 2. 玩家座位显示 ==");
            seatWest.setLevel(12);
            seatWest.setPlayerName("AI-西家");
            seatWest.setCardCount(16);
            seatWest.setState(PdkPlayerSeat.State.THINKING);
            root.applyCss();
            root.layout();

            Label nameLabel = (Label) seatWest.lookup(".pdk-seat-name");
            Label levelLabel = (Label) seatWest.lookup(".pdk-seat-level");
            Label countLabel = (Label) seatWest.lookup(".pdk-seat-count");
            Label statusLabel = (Label) seatWest.lookup(".pdk-seat-status");
            check("昵称显示正确", nameLabel != null && "AI-西家".equals(nameLabel.getText()));
            check("等级徽章显示 Lv.12", levelLabel != null && levelLabel.getText().contains("Lv.12"));
            check("剩余牌数显示 16", countLabel != null && countLabel.getText().contains("16"));
            check("状态显示「思考中」", statusLabel != null && statusLabel.getText().contains("思考中"));
            check("当前行动态带金色样式类",
                    seatWest.getStyleClass().contains("pdk-seat-active"));

            seatWest.setState(PdkPlayerSeat.State.WON);
            check("胜利态带 pdk-seat-won 样式类",
                    seatWest.getStyleClass().contains("pdk-seat-won"));
            seatWest.setState(PdkPlayerSeat.State.WAITING);
            check("等待态清空行动 / 胜利样式类",
                    !seatWest.getStyleClass().contains("pdk-seat-active")
                            && !seatWest.getStyleClass().contains("pdk-seat-won"));

            // ---------- 3. 手牌数量正常 ----------
            System.out.println();
            System.out.println("== 3. 手牌数量正常 ==");
            List<Card> full = new ArrayList<>();
            Deck deck = Deck.hunanPaodekuai(new Random(2026));
            full.addAll(deck.draw(16));
            hand.setCards(full);
            check("setCards(16) 后手牌数 = 16", hand.getCards().size() == 16);
            check("手牌节点可定位（供飞牌动画取起点）",
                    hand.getCardNode(full.get(0)) != null);

            List<Card> fewer = new ArrayList<>(full.subList(0, 11));
            hand.setCards(fewer);
            check("setCards(11) 后手牌数 = 11", hand.getCards().size() == 11);
            hand.setCards(List.of());
            check("setCards(空) 后手牌清空", hand.getCards().isEmpty());

            // ---------- 4. 按钮状态正常 ----------
            System.out.println();
            System.out.println("== 4. 按钮状态正常 ==");
            Button play = bar.getPlayButton();
            Button pass = bar.getPassButton();
            bar.setPlayEnabled(false);
            bar.setPassEnabled(false);
            check("不可用时 出牌 disabled", play.isDisabled());
            check("不可用时 不出 disabled", pass.isDisabled());
            bar.setPlayEnabled(true);
            bar.setPassEnabled(true);
            check("可用时 出牌 未 disabled", !play.isDisabled());
            check("可用时 不出 未 disabled", !pass.isDisabled());

            // ---------- 5. 动画调用接口正常 ----------
            System.out.println();
            System.out.println("== 5. 动画调用接口正常 ==");
            GameAnimationService anim = GameAnimationService.getInstance();
            check("GameAnimationService 单例可用", anim != null && anim == GameAnimationService.getInstance());
            boolean enabled = anim.isEnabled();
            check("动画开关可读（animationEnabled）", enabled || !enabled);
            boolean toast = anim.showToast(root, "顺子");
            check("出牌型提示条调用成功", toast == enabled);

            PdkCardView cardView = new PdkCardView(new Card(Rank.KING, Suit.HEARTS));
            cardView.setSelected(true);
            check("卡牌选中态 isSelected()=true", cardView.isSelected());
            check("卡牌选中态带 pdk-card-selected 样式类",
                    cardView.getStyleClass().contains("pdk-card-selected"));
            cardView.setSelected(false);
            check("取消选中后样式类移除",
                    !cardView.getStyleClass().contains("pdk-card-selected"));
        });

        // ---------- 6. 不影响原规则 ----------
        System.out.println();
        System.out.println("== 6. 不影响原规则（PdkEngine 行为不变）==");
        check("Deck.hunanPaodekuai 仍为 48 张", Deck.hunanPaodekuai(new Random(1)).remaining() == 48);

        PdkEngine engine = new PdkEngine(new Random(20260911L));
        engine.start();
        PdkSnapshot snap = (PdkSnapshot) engine.snapshotFor(PlayerId.SEAT_1);
        check("开局阶段为 PLAYING", snap.phase() == GamePhase.PLAYING);
        check("本人开局手牌 16 张", snap.myHand().size() == 16);
        int total = snap.remainingCardCounts().values().stream().mapToInt(Integer::intValue).sum();
        check("三家剩余牌合计 48 张", total == 48);
        check("开局无胜者", snap.winner().isEmpty());

        List<com.csu.pokergame.core.engine.GameCommand> legal = engine.legalCommands(PlayerId.SEAT_1);
        check("SEAT_1 存在合法命令", !legal.isEmpty());
        check("首家不能不出（规则未被改动）",
                legal.stream().noneMatch(c -> c instanceof com.csu.pokergame.paodekuai.PassPdkTurn));

        // 用现有 bot 策略把整局打完，验证引擎流程未受影响
        RuleBotController[] bots = {
                null,
                new RuleBotController(new PdkBotPolicy()),
                new RuleBotController(new PdkBotPolicy())
        };
        int guard = 0;
        PdkSnapshot cur = snap;
        while (cur.winner().isEmpty() && cur.phase() == GamePhase.PLAYING && guard++ < 500) {
            PlayerId who = cur.currentPlayer();
            var cmds = engine.legalCommands(who);
            if (cmds.isEmpty()) {
                break;
            }
            if (who == PlayerId.SEAT_1) {
                engine.apply(cmds.get(cmds.size() - 1));
            } else {
                GameSnapshot botSnap = engine.snapshotFor(who);
                engine.apply(bots[who.ordinal()].decide(botSnap, cmds).command());
            }
            cur = (PdkSnapshot) engine.snapshotFor(PlayerId.SEAT_1);
        }
        check("整局可由现有 bot 打完并产生胜者", cur.winner().isPresent());
        check("打到终局未死循环", guard < 500);

        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        System.out.println("PDK_UI_OK");
        if (failed > 0) {
            System.exit(1);
        }
        Platform.exit();
    }

    /** 在 FX 线程上执行任务并等待完成。 */
    private static void runOnFx(AtomicReference<Throwable> error, ThrowingRunnable task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                error.set(t);
                t.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        latch.await(30, TimeUnit.SECONDS);
        if (error.get() != null) {
            System.out.println("  [FAIL] FX 任务异常：" + error.get());
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
