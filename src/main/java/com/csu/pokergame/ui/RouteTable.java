package com.csu.pokergame.ui;

import com.csu.pokergame.ui.scene.GameModeView;
import com.csu.pokergame.ui.scene.HomeView;
import com.csu.pokergame.ui.scene.LanLobbyView;
import com.csu.pokergame.ui.scene.LiarTableView;
import com.csu.pokergame.ui.scene.LocalGameSelectView;
import com.csu.pokergame.ui.scene.PdkTableView;
import com.csu.pokergame.ui.scene.RulesView;

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
        // ===== 加载/入口页（feat/loading 分支负责） =====
        shell.register("home", () -> new HomeView(shell));
        shell.register("game-modes", () -> new GameModeView(shell));
        shell.register("local-select", () -> new LocalGameSelectView(shell));
        shell.register("rules", () -> new RulesView(shell));

        // ===== 出牌/桌面页（feat/gameplay 分支负责） =====
        shell.register("pdk", () -> new PdkTableView(shell));
        shell.register("liar", () -> new LiarTableView(shell));

        // ===== 局域网大厅（feat/ai-network 分支负责） =====
        shell.register("lan-lobby", () -> new LanLobbyView(shell));

        // 注：LanGameTableView 需要运行时参数（LanHost / LanClient），
        // 不在此注册，由 LanLobbyView 跳转前临时注册后 navigate。
    }
}
