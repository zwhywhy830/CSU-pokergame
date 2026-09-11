import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.settings.GameSettings;
import com.csu.pokergame.settings.SettingsService;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 阶段 20 设置中心自检（无界面，纯数据）。
 *
 * <p>覆盖需求用例：
 * <ol>
 *   <li>首次启动：自动生成 settings.json（6 个默认字段）；</li>
 *   <li>修改 volume=50，保存后重新读取仍为 50；</li>
 *   <li>旧 settings 缺字段：自动补默认值并回写升级；</li>
 *   <li>reset()：恢复出厂默认；</li>
 *   <li>账号切换：设置不影响玩家存档，也不被玩家存档影响。</li>
 * </ol>
 *
 * <p>自检全程只在临时目录里造数据，不读写真实存档 / 真实配置。
 */
public class SettingsSelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory("settingschk");

        System.out.println("== 1. 首次启动：自动生成 settings.json ==");
        Path firstFile = base.resolve("first").resolve("settings.json");
        SettingsService first = new SettingsService(firstFile);
        check("settings.json 已自动创建", Files.isRegularFile(firstFile));
        String firstJson = Files.readString(firstFile);
        check("包含 masterVolume", firstJson.contains("masterVolume"));
        check("包含 theme", firstJson.contains("theme"));
        GameSettings d = first.getSettings();
        check("默认 masterVolume = 80", d.getMasterVolume() == 80);
        check("默认 musicEnabled = true", d.isMusicEnabled());
        check("默认 soundEnabled = true", d.isSoundEnabled());
        check("默认 animationEnabled = true", d.isAnimationEnabled());
        check("默认 aiHintEnabled = true", d.isAiHintEnabled());
        check("默认 theme = dark", GameSettings.THEME_DARK.equals(d.getTheme()));
        check("默认主题文案 = 深色主题", "深色主题".equals(d.getThemeText()));

        System.out.println();
        System.out.println("== 2. 修改 volume=50：保存后重新读取仍为 50 ==");
        Path persistFile = base.resolve("persist").resolve("settings.json");
        SettingsService persist = new SettingsService(persistFile);
        persist.setVolume(50);
        SettingsService reload = new SettingsService(persistFile);
        check("重读 masterVolume = 50", reload.getSettings().getMasterVolume() == 50);
        check("磁盘文件写入 50", Files.readString(persistFile).contains("50"));
        // 开关切换也要"改即存"
        persist.toggleMusic();
        check("toggleMusic 后 musicEnabled = false", !persist.getSettings().isMusicEnabled());
        check("重读 musicEnabled = false", !new SettingsService(persistFile).getSettings().isMusicEnabled());

        System.out.println();
        System.out.println("== 3. 旧 settings 缺字段：自动补默认值 ==");
        Path legacyFile = base.resolve("legacy").resolve("settings.json");
        Files.createDirectories(legacyFile.getParent());
        // 只有音量一个字段，其余缺失（模拟旧版本配置文件）
        Files.writeString(legacyFile, "{ \"masterVolume\" : 33 }");
        SettingsService legacy = new SettingsService(legacyFile);
        GameSettings lg = legacy.getSettings();
        check("旧文件已有字段保留（volume=33）", lg.getMasterVolume() == 33);
        check("缺字段补默认 musicEnabled=true", lg.isMusicEnabled());
        check("缺字段补默认 soundEnabled=true", lg.isSoundEnabled());
        check("缺字段补默认 animationEnabled=true", lg.isAnimationEnabled());
        check("缺字段补默认 aiHintEnabled=true", lg.isAiHintEnabled());
        check("缺字段补默认 theme=dark", GameSettings.THEME_DARK.equals(lg.getTheme()));
        String upgraded = Files.readString(legacyFile);
        check("旧文件被回写补齐（含 theme）", upgraded.contains("theme"));
        check("旧文件被回写补齐（含 aiHintEnabled）", upgraded.contains("aiHintEnabled"));

        System.out.println();
        System.out.println("== 4. reset()：恢复默认 ==");
        Path resetFile = base.resolve("reset").resolve("settings.json");
        SettingsService resetSvc = new SettingsService(resetFile);
        resetSvc.setVolume(10);
        resetSvc.toggleMusic();          // false
        resetSvc.toggleAnimation();      // false
        resetSvc.changeTheme(GameSettings.THEME_DEFAULT);
        resetSvc.reset();
        GameSettings r = resetSvc.getSettings();
        check("reset 后 volume = 80", r.getMasterVolume() == 80);
        check("reset 后 musicEnabled = true", r.isMusicEnabled());
        check("reset 后 animationEnabled = true", r.isAnimationEnabled());
        check("reset 后 theme = dark", GameSettings.THEME_DARK.equals(r.getTheme()));
        check("reset 已落盘", new SettingsService(resetFile).getSettings().getMasterVolume() == 80);

        System.out.println();
        System.out.println("== 5. 账号切换：设置不影响玩家存档 ==");
        Path settingsFile = base.resolve("iso").resolve("settings.json");
        SettingsService svc = new SettingsService(settingsFile);
        Path playerA = base.resolve("playerA.json");
        Path playerB = base.resolve("playerB.json");
        Files.writeString(playerA, "{}");
        Files.writeString(playerB, "{}");
        PlayerManager players = new PlayerManager(playerA);
        players.save();
        check("playerA.json 已写入存档", Files.isRegularFile(playerA));
        String playerJson = Files.readString(playerA).toLowerCase();
        check("player.json 不含 masterVolume", !playerJson.contains("mastervolume"));
        check("player.json 不含 theme", !playerJson.contains("\"theme\""));
        svc.setVolume(42);
        check("改设置后 player.json 未变", !Files.readString(playerA).toLowerCase().contains("mastervolume"));
        players.switchPlayer(playerB);
        players.switchPlayer(playerA);
        check("切账号后设置仍为 42", new SettingsService(settingsFile).getSettings().getMasterVolume() == 42);

        System.out.println();
        System.out.println("== 6. 单例 ==");
        check("getInstance() 两次同一实例",
                SettingsService.getInstance() == SettingsService.getInstance());

        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        if (failed > 0) {
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
}
