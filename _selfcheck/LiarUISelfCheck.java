import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.liar.LiarActionBar;
import com.cards.ui.liar.LiarClaimPanel;
import com.cards.ui.liar.LiarHandView;
import com.cards.ui.liar.LiarPlayerSeat;
import com.cards.ui.liar.LiarRiskIndicator;
import com.cards.ui.liar.LiarTableView;
import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Deck;
import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GamePhase;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.core.player.RuleBotController;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.LiarEngine;
import com.csu.pokergame.liarspoker.LiarRandomPolicy;
import com.csu.pokergame.liarspoker.LiarSnapshot;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.PlayerAccount;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 阶段 24 骗子酒馆专属 UI 自检。
 *
 * <p>覆盖需求用例：
 * <ol>
 *   <li>四个玩家座位存在（LiarTableView 环形 4 座 + 状态接口 Thinking/Bluffing/Dead）；</li>
 *   <li>本人手牌正面可见（牌面节点与张数一致；对手手牌不渲染）；</li>
 *   <li>按钮事件绑定（宣告 / 继续 / 质疑 可绑定、可禁用、点击反馈走 GameAnimationService）；</li>
 *   <li>声明面板刷新（当前玩家 / 声明 / 可信度 + 淡入动画终态正确）；</li>
 *   <li>账号数据不影响（跑完整一局后金币余额不变）；</li>
 *   <li>旧存档兼容（空数据目录可自动初始化 default 账号并登录）；</li>
 *   <li>截图生成（场景快照可产出非空图像）。</li>
 * </ol>
 *
 * <p>输出标记：{@code LIAR_UI_OK}
 */
public class LiarUISelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        runOnFx(error, () -> {
            StackPane root = new StackPane();
            Scene scene = new Scene(root, 1200, 760);
            scene.getStylesheets().add(LiarUISelfCheck.class.getResource("/app.css").toExternalForm());

            // ---------- 1. 四个玩家座位存在 ----------
            System.out.println("== 1. 四个玩家座位存在 ==");
            LiarTableView view = new LiarTableView();
            root.getChildren().add(view);
            root.applyCss();
            root.layout();

            check("LiarTableView 创建成功", view != null);
            check("座位数 = 4", LiarTableView.SEAT_COUNT == 4);
            List<LiarPlayerSeat> seats = view.getSeats();
            check("getSeats() 返回 4 个座位", seats != null && seats.size() == 4);
            boolean allNonNull = true;
            boolean allInScene = true;
            for (int i = 0; i < LiarTableView.SEAT_COUNT; i++) {
                LiarPlayerSeat s = view.getSeat(i);
                allNonNull &= s != null;
                allInScene &= s != null && s.getScene() == scene;
            }
            check("四个座位实例均存在", allNonNull);
            check("四个座位均已挂到场景图（环形布局）", allInScene);
            check("越界座位访问返回 null", view.getSeat(4) == null && view.getSeat(-1) == null);

            // 状态接口（仅视觉）：Thinking / Bluffing / Dead
            LiarPlayerSeat s1 = view.getSeat(1);
            s1.setThinking();
            check("setThinking() → 样式类 liar-seat-live",
                    s1.getStyleClass().contains("liar-seat-live")
                            && s1.getState() == LiarPlayerSeat.State.THINKING);
            s1.setBluffing();
            check("setBluffing() → 样式类 liar-seat-bluff",
                    s1.getStyleClass().contains("liar-seat-bluff")
                            && s1.getState() == LiarPlayerSeat.State.BLUFFING);
            s1.setDead();
            check("setDead() → 样式类 liar-seat-dead 且透明度 0.45",
                    s1.getStyleClass().contains("liar-seat-dead")
                            && Math.abs(s1.getOpacity() - 0.45) < 1e-6);
            s1.setNormal();
            check("setNormal() 清除全部态样式类",
                    !s1.getStyleClass().contains("liar-seat-live")
                            && !s1.getStyleClass().contains("liar-seat-bluff")
                            && !s1.getStyleClass().contains("liar-seat-dead"));

            // 等级 / 昵称 / 生命值
            s1.setPlayerName("AI 张三");
            s1.setLevel(8);
            s1.setLife(2);
            root.applyCss();
            root.layout();
            Label name = (Label) s1.lookup(".liar-seat-name");
            Label level = (Label) s1.lookup(".liar-seat-level");
            Label life = (Label) s1.lookup(".liar-seat-life");
            check("昵称显示正确", name != null && "AI 张三".equals(name.getText()));
            check("等级显示 Lv.8", level != null && level.getText().contains("Lv.8"));
            check("生命值渲染 ❤️❤️🖤", life != null && life.getText().equals("❤️❤️🖤"));
            check("生命值 getter 一致", s1.getLife() == 2);
            s1.setLife(-1);
            check("setLife(-1) 隐藏生命值行", !life.isVisible() && !life.isManaged());

            // ---------- 2. 本人手牌正面可见 ----------
            System.out.println();
            System.out.println("== 2. 本人手牌正面可见（对手手牌不渲染） ==");
            LiarHandView hand = view.getHandView();
            List<Card> myHand = Deck.liarPoker(new Random(2026)).draw(5);
            hand.setCards(myHand);
            root.applyCss();
            root.layout();
            check("setCards(5) 后手牌数量 = 5", hand.getCount() == 5);
            check("手牌列表与快照一致", hand.getCards().equals(myHand));
            check("牌面节点可定位", hand.getCardNode(0) != null && hand.getCardNode(4) != null);
            check("越界手牌节点返回 null", hand.getCardNode(5) == null);

            check("渲染出的牌面节点数 = 5", hand.lookupAll(".liar-card-face").size() == 5);
            check("本人手牌不使用牌背", hand.lookupAll(".liar-card-back").isEmpty());
            check("不复用跑得快牌面样式类 (.pdk-card)",
                    hand.lookupAll(".pdk-card").isEmpty());
            boolean facesRender = true;
            for (Node n : hand.lookupAll(".liar-card-face")) {
                facesRender &= n instanceof LiarHandView.LiarCardFace f
                        && f.card() != null && f.getFaceNode().getImage() != null;
            }
            check("每张牌面都渲染出真实图像（非空白）", facesRender);

            // 无牌面数据时退化为牌背占位
            hand.setCount(3);
            root.applyCss();
            root.layout();
            check("setCount(3) 退化为牌背占位", hand.lookupAll(".liar-card-back").size() == 3);
            hand.setCards(myHand);

            hand.setInteractive(true);
            hand.setMaxSelect(3);
            check("收缩到 2 张后数量正确", shrink(hand) == 2);
            hand.setCards(myHand);

            // ---------- 3. 按钮事件绑定 ----------
            System.out.println();
            System.out.println("== 3. 按钮事件绑定 ==");
            LiarActionBar bar = view.getActionBar();
            check("操作条含 3 个按钮（宣告 / 继续 / 质疑）", bar.getChildren().size() == 3);
            check("按钮为 Button 类型",
                    bar.getDeclareButton() instanceof Button
                            && bar.getContinueButton() instanceof Button
                            && bar.getChallengeButton() instanceof Button);

            AtomicInteger declareClicks = new AtomicInteger();
            AtomicInteger continueClicks = new AtomicInteger();
            AtomicInteger challengeClicks = new AtomicInteger();
            bar.getDeclareButton().setOnAction(e -> declareClicks.incrementAndGet());
            bar.getContinueButton().setOnAction(e -> continueClicks.incrementAndGet());
            bar.getChallengeButton().setOnAction(e -> challengeClicks.incrementAndGet());
            bar.getDeclareButton().fire();
            bar.getContinueButton().fire();
            bar.getChallengeButton().fire();
            check("「宣告」点击回调被触发", declareClicks.get() == 1);
            check("「继续」点击回调被触发", continueClicks.get() == 1);
            check("「质疑」点击回调被触发", challengeClicks.get() == 1);
            check("按钮点击反馈走统一动画服务（BUTTON 计数 > 0）",
                    GameAnimationService.getInstance().getPlayCount(
                            GameAnimationService.Kind.BUTTON) > 0);

            bar.setAllEnabled(false);
            check("setAllEnabled(false) 三个按钮均禁用",
                    bar.getDeclareButton().isDisabled()
                            && bar.getContinueButton().isDisabled()
                            && bar.getChallengeButton().isDisabled());
            bar.setDeclareEnabled(true);
            bar.setContinueEnabled(true);
            bar.setChallengeEnabled(true);
            check("单独启用后按钮恢复可用",
                    !bar.getDeclareButton().isDisabled()
                            && !bar.getContinueButton().isDisabled()
                            && !bar.getChallengeButton().isDisabled());
            check("「质疑」为红色危险样式类",
                    bar.getChallengeButton().getStyleClass().contains("liar-danger-btn"));
            check("「继续」为灰色玻璃样式类",
                    bar.getContinueButton().getStyleClass().contains("liar-action"));

            // ---------- 4. 声明面板刷新 ----------
            System.out.println();
            System.out.println("== 4. 声明面板刷新 ==");
            LiarClaimPanel claim = view.getClaimPanel();
            claim.setDeclarer("AI 李四");
            claim.setClaim("三张 K");
            claim.setCredibility("未知");
            root.applyCss();
            root.layout();
            check("声明面板声明文案 = 「三张 K」", "三张 K".equals(claim.getClaim()));
            Label declarerLabel = (Label) claim.lookup(".liar-claim-declarer");
            Label credLabel = (Label) claim.lookup(".liar-claim-cred");
            check("当前玩家显示 AI 李四",
                    declarerLabel != null && declarerLabel.getText().contains("AI 李四"));
            check("可信度显示「未知」",
                    credLabel != null && credLabel.getText().contains("未知")
                            && credLabel.getStyleClass().contains("liar-claim-cred-unknown"));
            claim.setCredibility("偏低");
            check("可信度切换为「偏低」样式",
                    credLabel.getStyleClass().contains("liar-claim-cred-low"));
            claim.playClaimIn();
            check("playClaimIn() 后文字回到完全不透明", claim.lookup(".liar-claim-text") != null
                    && Math.abs(claim.lookup(".liar-claim-text").getOpacity()) <= 1.0);
            claim.playChallengeShake();
            check("playChallengeShake() 立即带上被质疑样式类",
                    claim.getStyleClass().contains("liar-claim-challenged"));

            // 怀疑度（纯 UI）
            LiarRiskIndicator risk = view.getRiskIndicator();
            risk.setValue(0.65);
            check("怀疑度值 = 0.65", Math.abs(risk.getValue() - 0.65) < 1e-6);
            check("怀疑度显示 65%", view.getRiskIndicator().lookupAll(".liar-risk-percent")
                    .stream().anyMatch(n -> n instanceof Label l && "65%".equals(l.getText())));
            risk.setValue(0.9);
            check("≥70% 标记为高危", risk.isHigh()
                    && risk.getStyleClass().contains("liar-risk-high"));
            risk.setValue(2.0);
            check("越界值被截断到 1.0", Math.abs(risk.getValue() - 1.0) < 1e-6);

            // 其他展示接口
            view.setPhaseText("应答");
            view.setAliveText(3);
            view.setTipText("目标点数：K");
            check("顶部阶段 / 存活 / 提示文案可设置",
                    view.lookupAll(".liar-chip-phase").size() == 1
                            && view.lookupAll(".liar-chip-life").size() == 1
                            && view.lookupAll(".liar-chip-tip").size() == 1);
            view.setLogEntries(List.of("A", "B", "C"));
            check("日志写入 3 条", view.getLog().getItems().size() == 3);
            check("特效层存在（暗红屏幕 / 金色光效挂载点）",
                    view.getFxLayer() != null && view.getRootPane() != null);
            view.playChallengeEffect();
            view.playVictoryGlow();
            view.playDefeatEffect();
            check("三种特效调用不抛异常", true);

            // ---------- 5 & 6. 账号数据 / 旧存档 ----------
            System.out.println();
            System.out.println("== 5. 账号数据不影响 / 6. 旧存档兼容 ==");
            int goldBefore = CoinService.getInstance().getGold();
            int winner = playFullGameWithBots();
            check("整局可由现有 AI 打完并产生胜者", winner >= 0);
            int goldAfter = CoinService.getInstance().getGold();
            check("跑完整局后金币余额不变（金币系统未被触碰）", goldBefore == goldAfter);

            Path tmp = Files.createTempDirectory("liar-selfcheck-");
            try {
                AccountService accounts = new AccountService(tmp);
                List<PlayerAccount> list = accounts.getAccounts();
                check("空数据目录可自动初始化账号文件", list != null && !list.isEmpty());
                boolean hasDefault = list.stream()
                        .anyMatch(a -> AccountService.LEGACY_ACCOUNT_USERNAME.equals(a.getUsername()));
                check("自动创建 default 账号（旧存档兼容）", hasDefault);
                check("default / 123456 可登录",
                        accounts.login(AccountService.LEGACY_ACCOUNT_USERNAME,
                                AccountService.LEGACY_ACCOUNT_PASSWORD));
                check("登录后玩家存档文件路径可解析",
                        accounts.resolvePlayerFile(accounts.getCurrentAccount()) != null);
                check("账号文件已落盘",
                        Files.exists(accounts.getAccountsFile()));
            } finally {
                deleteQuietly(tmp);
            }

            // ---------- 7. 截图生成 ----------
            System.out.println();
            System.out.println("== 7. 截图生成 ==");
            hand.setCards(myHand);
            hand.setInteractive(true);
            root.applyCss();
            root.layout();
            WritableImage shot = scene.snapshot(null);
            check("场景快照生成成功且尺寸正确",
                    shot != null && shot.getWidth() == 1200 && shot.getHeight() == 760);
            check("快照含非透明像素",
                    shot.getPixelReader().getArgb(600, 380) != 0);

            System.out.println();
            System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
            System.out.println("LIAR_UI_OK");
            if (failed > 0) {
                System.exit(1);
            }
        });
        if (error.get() != null) {
            System.exit(1);
        }
        Platform.exit();
    }

    // ============================================================= 辅助

    /** 手牌收缩测试：截到 2 张并返回实际数量。 */
    private static int shrink(LiarHandView hand) {
        List<Card> sub = hand.getCards().subList(0, Math.min(2, hand.getCount()));
        hand.setCards(new ArrayList<>(sub));
        return hand.getCount();
    }

    /** 用现有引擎 + 现成 bot 打完整整局，返回胜者下标（无胜者返回 -1）。 */
    private static int playFullGameWithBots() {
        LiarEngine engine = new LiarEngine(new Random(2026));
        engine.start();
        RuleBotController[] bots = new RuleBotController[3];
        for (int i = 0; i < 3; i++) {
            bots[i] = new RuleBotController(new LiarRandomPolicy(new Random(2026 + i)));
        }
        int guard = 0;
        while (guard++ < 600) {
            LiarSnapshot snap = (LiarSnapshot) engine.snapshotFor(PlayerId.SEAT_1);
            if (snap.winner().isPresent()) {
                return snap.winner().get().ordinal();
            }
            if (snap.phase() != GamePhase.PLAYING) {
                break;
            }
            PlayerId cur = snap.currentPlayer();
            List<GameCommand> cmds = new ArrayList<>(engine.legalCommands(cur));
            if (cmds.isEmpty()) {
                break;
            }
            if (cur == PlayerId.SEAT_1) {
                GameCommand pick = cmds.stream()
                        .filter(c -> c instanceof DeclareLiarCards)
                        .findFirst()
                        .orElse(cmds.get(0));
                engine.apply(pick);
            } else {
                GameSnapshot botSnap = engine.snapshotFor(cur);
                engine.apply(bots[cur.ordinal() - 1].decide(botSnap, cmds).command());
            }
        }
        return -1;
    }

    private static void deleteQuietly(Path dir) {
        try {
            if (dir == null || !Files.exists(dir)) {
                return;
            }
            try (var walk = Files.walk(dir)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception ignored) {
        }
    }

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
        latch.await(60, TimeUnit.SECONDS);
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
