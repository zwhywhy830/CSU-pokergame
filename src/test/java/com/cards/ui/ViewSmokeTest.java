package com.cards.ui;

import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.CoinLogService;
import com.csu.pokergame.player.CoinRechargeService;
import com.csu.pokergame.player.CoinService;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.InventoryService;
import com.csu.pokergame.player.ItemUseService;
import com.csu.pokergame.player.LeaderboardService;
import com.csu.pokergame.player.PlayerGrowthService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerStatsService;
import com.csu.pokergame.player.ShopService;
import com.csu.pokergame.player.StatisticsService;
import com.csu.pokergame.settings.SettingsService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 阶段 12：新增页面（LoginView / ProfileView / SettingsView）冒烟测试。
 *
 * <p>验证三个页面在 JavaFX Application Thread 上能正常构造，
 * 不抛异常且根节点非空。服务均走 getInstance() 单例，与 RouteTable 装配一致。
 */
class ViewSmokeTest {

    @BeforeAll
    static void initFx() {
        FxTestKit.initToolkit();
    }

    @Test
    void loginViewConstructsWithoutError() {
        FxTestKit.runAndWait(() -> {
            LoginView view = new LoginView(AccountService.getInstance(), () -> { });
            assertThat(view).isNotNull();
            assertThat(view.getStyleClass()).contains("login-view");
        });
    }

    @Test
    void settingsViewConstructsWithoutError() {
        FxTestKit.runAndWait(() -> {
            SettingsView view = new SettingsView(
                    SettingsService.getInstance(),
                    AccountService.getInstance(),
                    () -> { }, () -> { }, () -> { });
            assertThat(view).isNotNull();
            assertThat(view.getStyleClass()).contains("settings-view");
        });
    }

    @Test
    void profileViewConstructsWithoutError() {
        FxTestKit.runAndWait(() -> {
            ProfileView view = new ProfileView(
                    PlayerManager.getInstance(),
                    CoinService.getInstance(),
                    PlayerGrowthService.getInstance(),
                    PlayerStatsService.getInstance(),
                    AchievementService.getInstance(),
                    CoinRechargeService.getInstance(),
                    CoinLogService.getInstance(),
                    InventoryService.getInstance(),
                    ShopService.getInstance(),
                    ItemUseService.getInstance(),
                    GameRecordService.getInstance(),
                    LeaderboardService.getInstance(),
                    StatisticsService.getInstance(),
                    AccountService.getInstance(),
                    () -> { }, () -> { }, () -> { });
            assertThat(view).isNotNull();
            assertThat(view.getStyleClass()).contains("profile-view");
        });
    }
}
