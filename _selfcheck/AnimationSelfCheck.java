import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.settings.GameSettings;
import com.csu.pokergame.settings.SettingsService;

import javafx.application.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 阶段 22 游戏表现增强系统自检（无界面，纯逻辑）。
 *
 * <p>覆盖需求用例：
 * <ol>
 *   <li>动画服务可以创建（单例）；</li>
 *   <li>关闭 {@code animationEnabled} 后不播放（计数不增长）；</li>
 *   <li>升级事件触发（LEVEL_UP 计数 +1）；</li>
 *   <li>金币动画触发（COIN 计数 +1）；</li>
 *   <li>旧设置文件兼容（缺 {@code animationEnabled} 字段默认 true）。</li>
 * </ol>
 *
 * <p>说明：{@link GameAnimationService} 的播放方法在「开关打开」时先记录事件、
 * 再做视觉动画；自检传 {@code null} 节点，只验证「事件是否触发 / 开关是否生效」，
 * 不依赖真实场景，因此无需在 FX 线程构造节点。
 */
public class AnimationSelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        initToolkit();

        System.out.println("== 1. 动画服务可以创建 ==");
        GameAnimationService anim = GameAnimationService.getInstance();
        check("getInstance() 非空", anim != null);
        check("两次 getInstance() 同一实例",
                GameAnimationService.getInstance() == GameAnimationService.getInstance());

        System.out.println();
        System.out.println("== 2. 关闭 animationEnabled 后不播放 ==");
        SettingsService svc = SettingsService.getInstance();
        boolean original = svc.getSettings().isAnimationEnabled();
        try {
            if (original) {
                svc.toggleAnimation();
            }
            check("设置已关闭 animationEnabled", !svc.getSettings().isAnimationEnabled());
            check("关闭后 isEnabled() = false", !anim.isEnabled());

            anim.resetStats();
            boolean playedLevelUp = anim.playLevelUpAnimation(null);
            boolean playedCoin = anim.playCoinAnimation(null, null, null);
            boolean playedWin = anim.playWinAnimation(null, null);
            boolean playedButton = anim.playButtonFeedback(null);
            check("关闭后 playLevelUpAnimation 返回 false", !playedLevelUp);
            check("关闭后 playCoinAnimation 返回 false", !playedCoin);
            check("关闭后 playWinAnimation 返回 false", !playedWin);
            check("关闭后 playButtonFeedback 返回 false", !playedButton);
            check("关闭后 LEVEL_UP 计数仍为 0", anim.getPlayCount(GameAnimationService.Kind.LEVEL_UP) == 0);
            check("关闭后 COIN 计数仍为 0", anim.getPlayCount(GameAnimationService.Kind.COIN) == 0);
        } finally {
            if (original != svc.getSettings().isAnimationEnabled()) {
                svc.toggleAnimation();
            }
        }
        check("恢复后 animationEnabled 已还原", svc.getSettings().isAnimationEnabled() == original);
        check("恢复后 isEnabled() = true", anim.isEnabled());

        System.out.println();
        System.out.println("== 3. 升级事件触发 ==");
        anim.resetStats();
        boolean levelUpPlayed = anim.playLevelUpAnimation(null);
        check("playLevelUpAnimation 返回 true", levelUpPlayed);
        check("LEVEL_UP 计数 = 1", anim.getPlayCount(GameAnimationService.Kind.LEVEL_UP) == 1);
        check("lastPlayed = LEVEL_UP", anim.getLastPlayed() == GameAnimationService.Kind.LEVEL_UP);

        System.out.println();
        System.out.println("== 4. 金币动画触发 ==");
        anim.resetStats();
        final boolean[] completed = {false};
        boolean coinPlayed = anim.playCoinAnimation(null, null, () -> completed[0] = true);
        check("playCoinAnimation 返回 true", coinPlayed);
        check("COIN 计数 = 1", anim.getPlayCount(GameAnimationService.Kind.COIN) == 1);
        check("无节点时立即回调（业务不被动画阻塞）", completed[0]);
        check("lastPlayed = COIN", anim.getLastPlayed() == GameAnimationService.Kind.COIN);

        System.out.println();
        System.out.println("== 5. 旧设置文件兼容（缺 animationEnabled 默认 true）==");
        Path base = Files.createTempDirectory("animchk");
        Path legacyFile = base.resolve("legacy").resolve("settings.json");
        Files.createDirectories(legacyFile.getParent());
        Files.writeString(legacyFile, "{ \"masterVolume\" : 44 }");
        SettingsService legacy = new SettingsService(legacyFile);
        GameSettings lg = legacy.getSettings();
        check("旧字段保留（volume=44）", lg.getMasterVolume() == 44);
        check("缺字段补默认 animationEnabled=true", lg.isAnimationEnabled());
        String upgraded = Files.readString(legacyFile);
        check("旧文件被回写补齐（含 animationEnabled）", upgraded.contains("animationEnabled"));

        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        System.out.println("ANIMATION_SYSTEM_OK");
        if (failed > 0) {
            System.exit(1);
        }
        Platform.exit();
    }

    /** 初始化 JavaFX 工具链（服务内部对音效 / 时间线有兜底）；失败则忽略。 */
    private static void initToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(10, TimeUnit.SECONDS);
        } catch (Throwable t) {
            System.out.println("  [INFO] JavaFX 工具链未初始化（" + t.getMessage() + "），动画调用将走静默兜底");
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
}
