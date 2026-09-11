package com.csu.pokergame.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlayerManager 阶段 6 冒烟测试：验证存档加载（classpath 种子回退）、
 * 经验升级曲线、胜负计数与自动落盘重读一致。
 */
class PlayerManagerTest {

    @Test
    void classpathSeedLoadsWhenFileMissing(@TempDir Path dir) {
        // 指向一个不存在的文件：load() 回退到 classpath /player/player.json
        PlayerManager pm = new PlayerManager(dir.resolve("missing.json"));
        PlayerProfile p = pm.getProfile();

        assertThat(p.getId()).isEqualTo("player001");
        assertThat(p.getName()).isEqualTo("玩家");
        assertThat(p.getLevel()).isEqualTo(12);
        assertThat(p.getGold()).isEqualTo(1800);
        assertThat(p.getDiamond()).isEqualTo(50);
    }

    @Test
    void expToNextLevelFormula() {
        // Lv.1 需 200，Lv.12 需 1300
        assertThat(PlayerManager.expToNextLevel(1)).isEqualTo(200);
        assertThat(PlayerManager.expToNextLevel(12)).isEqualTo(1300);
        assertThat(PlayerManager.expToNextLevel(0)).isEqualTo(200);
    }

    @Test
    void addExpLevelsUpAndCarriesRemainder(@TempDir Path dir) {
        PlayerManager pm = new PlayerManager(dir.resolve("p.json"));
        PlayerProfile p = pm.getProfile();
        // 种子档 level=12 exp=370，升级需 1300
        int startLevel = p.getLevel();
        int startExp = p.getExp();

        int gained = pm.addExp(1300 - startExp + 50);
        assertThat(gained).isEqualTo(1);
        assertThat(p.getLevel()).isEqualTo(startLevel + 1);
        // 升级后 exp 应为 50（扣掉 1300 后剩余）
        assertThat(p.getExp()).isEqualTo(50);
    }

    @Test
    void addExpRejectsNonPositive(@TempDir Path dir) {
        PlayerManager pm = new PlayerManager(dir.resolve("p.json"));
        int expBefore = pm.getProfile().getExp();
        assertThat(pm.addExp(0)).isZero();
        assertThat(pm.addExp(-5)).isZero();
        assertThat(pm.getProfile().getExp()).isEqualTo(expBefore);
    }

    @Test
    void recordWinAndLossPersistOnReload(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("p.json");
        PlayerManager pm = new PlayerManager(file);
        int winsBefore = pm.getProfile().getTotalWins();
        int lossesBefore = pm.getProfile().getTotalLosses();

        assertThat(pm.recordWin()).isEqualTo(winsBefore + 1);
        assertThat(pm.recordLoss()).isEqualTo(lossesBefore + 1);

        // 重新构造：验证 autoSave 已落盘
        PlayerManager reloaded = new PlayerManager(file);
        assertThat(reloaded.getProfile().getTotalWins()).isEqualTo(winsBefore + 1);
        assertThat(reloaded.getProfile().getTotalLosses()).isEqualTo(lossesBefore + 1);
    }

    @Test
    void autoSaveOffKeepsChangesInMemoryUntilFlush(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("p.json");
        PlayerManager pm = new PlayerManager(file);
        // 先手动落盘一次，保证磁盘上有初始内容
        pm.save();
        String onDisk = Files.readString(file);
        pm.setAutoSave(false);

        pm.recordWin();
        // 关闭自动保存：磁盘内容应保持不变
        assertThat(Files.readString(file)).isEqualTo(onDisk);

        pm.flush();
        String afterFlush = Files.readString(file);
        assertThat(afterFlush).isNotEqualTo(onDisk);
    }

    @Test
    void switchPlayerCreatesDefaultProfileForNewFile(@TempDir Path dir) {
        PlayerManager pm = new PlayerManager(dir.resolve("a.json"));
        Path bFile = dir.resolve("b.json");
        assertThat(Files.exists(bFile)).isFalse();

        pm.switchPlayer(bFile);
        assertThat(Files.isRegularFile(bFile)).isTrue();
        // 新账号是全新默认档，不应该读到 classpath 种子
        assertThat(pm.getProfile().getGold()).isEqualTo(PlayerProfile.DEFAULT_GOLD);
        assertThat(pm.getProfile().getLevel()).isEqualTo(PlayerProfile.MIN_LEVEL);
    }

    @Test
    void profileNormalizesNegativeFields() {
        PlayerProfile p = new PlayerProfile();
        p.setGold(-100);
        p.setExp(-10);
        p.setLevel(0);
        p.setName(null);

        assertThat(p.getGold()).isGreaterThanOrEqualTo(0);
        assertThat(p.getExp()).isGreaterThanOrEqualTo(0);
        assertThat(p.getLevel()).isGreaterThanOrEqualTo(PlayerProfile.MIN_LEVEL);
        assertThat(p.getName()).isEqualTo(PlayerProfile.DEFAULT_NAME);
    }
}
