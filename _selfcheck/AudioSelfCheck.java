import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
import com.csu.pokergame.settings.GameSettings;
import com.csu.pokergame.settings.SettingsService;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 阶段 21 音频系统自检（无界面，纯逻辑）。
 *
 * <p>覆盖需求用例：
 * <ol>
 *   <li>AudioService 单例正常；</li>
 *   <li>Settings 读取正常（音量 / 音效 / 音乐开关）；</li>
 *   <li>不存在音频文件不会异常；</li>
 *   <li>音量范围限制正常；</li>
 *   <li>关闭 soundEnabled 后不播放。</li>
 * </ol>
 */
public class AudioSelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        // 音频依赖 JavaFX 工具链；初始化失败也不影响后面的纯逻辑校验
        initToolkit();

        AudioService audio = AudioService.getInstance();

        System.out.println("== 1. AudioService 单例正常 ==");
        check("getInstance() 非空", audio != null);
        check("两次 getInstance() 同一实例", AudioService.getInstance() == AudioService.getInstance());

        System.out.println();
        System.out.println("== 2. Settings 读取正常 ==");
        GameSettings s = SettingsService.getInstance().getSettings();
        check("settings 非空", s != null);
        check("masterVolume 在 0~100", s.getMasterVolume() >= 0 && s.getMasterVolume() <= 100);
        audio.refreshSettings();
        double expect = AudioService.clamp(s.getMasterVolume() / 100.0);
        check("refreshSettings 后音量 = masterVolume/100 (=" + expect + ")",
                Math.abs(audio.getVolume() - expect) < 1e-9);
        check("soundEnabled 已同步", audio.isSoundEnabled() == s.isSoundEnabled());
        check("musicEnabled 已同步", audio.isMusicEnabled() == s.isMusicEnabled());

        System.out.println();
        System.out.println("== 3. 不存在音频文件不会异常 ==");
        check("playMusic(不存在的 mp3) 不抛异常", noThrow(() -> audio.playMusic("/audio/bgm/not_exist.mp3")));
        check("playMusic(null) 不抛异常", noThrow(() -> audio.playMusic(null)));
        check("playMusic(空串) 不抛异常", noThrow(() -> audio.playMusic("   ")));
        check("stopMusic() 不抛异常", noThrow(audio::stopMusic));
        check("playEffect(null) 不抛异常", noThrow(() -> audio.playEffect(null)));
        for (SoundEffect e : SoundEffect.values()) {
            check("playEffect(" + e.name() + ") 不抛异常", noThrow(() -> audio.playEffect(e)));
        }

        System.out.println();
        System.out.println("== 4. 音量范围限制正常 ==");
        check("clamp(-1) = 0", AudioService.clamp(-1) == 0);
        check("clamp(0.35) = 0.35", Math.abs(AudioService.clamp(0.35) - 0.35) < 1e-9);
        check("clamp(1) = 1", AudioService.clamp(1) == 1);
        check("clamp(999) = 1", AudioService.clamp(999) == 1);
        audio.setVolume(5);
        check("setVolume(5) 被夹取为 1", audio.getVolume() == 1);
        audio.setVolume(-3);
        check("setVolume(-3) 被夹取为 0", audio.getVolume() == 0);
        audio.setVolume(0.6);
        check("setVolume(0.6) 生效", Math.abs(audio.getVolume() - 0.6) < 1e-9);

        System.out.println();
        System.out.println("== 5. 关闭 soundEnabled 后不播放 ==");
        SettingsService svc = SettingsService.getInstance();
        boolean original = svc.getSettings().isSoundEnabled();
        try {
            if (original) {
                svc.toggleSound();
            }
            audio.refreshSettings();
            check("关闭后 isSoundEnabled() = false", !audio.isSoundEnabled());
            check("关闭后 playEffect(BUTTON_CLICK) 静默跳过且不抛异常",
                    noThrow(() -> audio.playEffect(SoundEffect.BUTTON_CLICK)));
        } finally {
            if (original != svc.getSettings().isSoundEnabled()) {
                svc.toggleSound();
            }
            audio.refreshSettings();
        }
        check("恢复后 soundEnabled 已还原", audio.isSoundEnabled() == original);

        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        System.out.println("AUDIO_SYSTEM_OK");
        if (failed > 0) {
            System.exit(1);
        }
        Platform.exit();
    }

    /** 初始化 JavaFX 工具链（MediaPlayer 需要）；失败则忽略。 */
    private static void initToolkit() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(10, TimeUnit.SECONDS);
        } catch (Throwable t) {
            System.out.println("  [INFO] JavaFX 工具链未初始化（" + t.getMessage() + "），音频调用将走静默兜底");
        }
    }

    private interface Action {
        void run();
    }

    private static boolean noThrow(Action action) {
        try {
            action.run();
            return true;
        } catch (Throwable t) {
            System.out.println("      异常：" + t);
            return false;
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
