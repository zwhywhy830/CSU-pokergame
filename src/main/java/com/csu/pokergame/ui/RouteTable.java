package com.csu.pokergame.ui;

import com.cards.ui.LoginView;
import com.cards.ui.ProfileView;
import com.cards.ui.SettingsView;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.AchievementService;
import com.csu.pokergame.player.CoinLogService;
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
import com.csu.pokergame.ui.scene.GameChoiceView;
import com.csu.pokergame.ui.scene.HomeView;
import com.csu.pokergame.ui.scene.LanLobbyView;
import com.csu.pokergame.ui.scene.LiarTableView;
import com.csu.pokergame.ui.scene.LoadingView;
import com.csu.pokergame.ui.scene.ModeChoiceView;
import com.csu.pokergame.ui.scene.PdkTableView;
import com.csu.pokergame.ui.scene.ProfileEditView;

/**
 * 路由注册表：集中注册无参数的页面路由。
 *
 * <p>带运行时参数的页面（如 LanGameTableView 需要 LanHost/LanClient）
 * 不在此注册，由调用方在跳转前临时调用 {@link AppShell#register} 注册。
 *
 * <p>新增页面时：在自己 View 类完成后，在本文件对应分区追加一行
 * {@code shell.register("xxx", () -> new XxxView(shell));} 即可，无需改动 AppShell。
 */
public final class RouteTable {

    private RouteTable() {
    }

    /**
     * 安装所有静态路由。程序启动时由 Launcher 调用一次。
     */
    public static void install(AppShell shell) {
        // ===== 登录 / 账号 =====
        shell.register("login", () -> new LoginView(
                AccountService.getInstance(),
                () -> shell.navigate("home")));

        // ===== 个人中心 / 设置中心 =====
        shell.register("profile", () -> new ProfileView(
                PlayerManager.getInstance(),
                CoinService.getInstance(),
                PlayerGrowthService.getInstance(),
                PlayerStatsService.getInstance(),
                AchievementService.getInstance(),
                CoinLogService.getInstance(),
                InventoryService.getInstance(),
                ShopService.getInstance(),
                ItemUseService.getInstance(),
                GameRecordService.getInstance(),
                LeaderboardService.getInstance(),
                StatisticsService.getInstance(),
                AccountService.getInstance(),
                () -> shell.navigate("login"),
                () -> shell.navigate("login"),
                () -> shell.navigate("home")));

        shell.register("settings", () -> new SettingsView(
                SettingsService.getInstance(),
                AccountService.getInstance(),
                () -> shell.navigate("login"),
                () -> shell.navigate("login"),
                () -> shell.navigate("home")));

        // ===== 大厅 / 入口页（deckapp-ui 移植） =====
        shell.register("home", () -> new HomeView(shell));
        shell.register("loading", () -> new LoadingView(shell));
        shell.register("game-choice", () -> new GameChoiceView(shell));
        shell.register("mode-choice", () -> new ModeChoiceView(shell));
        shell.register("profile-edit", () -> new ProfileEditView(shell));

        // ===== 出牌/桌面页 =====
        shell.register("pdk", () -> new PdkTableView(shell));
        shell.register("liar", () -> new LiarTableView(shell));

        // ===== 局域网大厅 =====
        shell.register("lan-lobby", () -> new LanLobbyView(shell));

        // 注：LanGameTableView 需要运行时参数（LanHost / LanClient），
        // 不在此注册，由 LanLobbyView 跳转前临时注册后 navigate。
    }
}
