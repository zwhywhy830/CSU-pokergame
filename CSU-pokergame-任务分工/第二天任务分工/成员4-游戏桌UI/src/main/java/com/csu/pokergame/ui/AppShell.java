package com.csu.pokergame.ui;

import com.csu.pokergame.network.GameType;
import com.csu.pokergame.network.LanClient;
import com.csu.pokergame.network.LanHost;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.ui.scene.GameModeView;
import com.csu.pokergame.ui.scene.HomeView;
import com.csu.pokergame.ui.scene.LanGameTableView;
import com.csu.pokergame.ui.scene.LanLobbyView;
import com.csu.pokergame.ui.scene.LiarTableView;
import com.csu.pokergame.ui.scene.LocalGameSelectView;
import com.csu.pokergame.ui.scene.PdkTableView;
import com.csu.pokergame.ui.scene.RulesView;
import com.csu.pokergame.ui.scene.SettingsOverlay;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

/**
 * 应用外壳：管理单一 Scene 与页面切换。
 * 后续 UI 人员可替换各 View 的实现，只需保持 {@code startPdk / startLiar / showHome} 契约。
 */
public final class AppShell {

    private final StackPane root = new StackPane();

    public AppShell(Scene scene) {
        root.getStyleClass().add("root");
        scene.setRoot(root);
        scene.getStylesheets().add(AppShell.class
                .getResource("/com/csu/pokergame/ui/theme/app.css").toExternalForm());
    }

    public void showHome() {
        root.getChildren().setAll(new HomeView(this));
    }

    public void showGameModes() {
        root.getChildren().setAll(new GameModeView(this));
    }

    public void showLocalGameSelect() {
        root.getChildren().setAll(new LocalGameSelectView(this));
    }

    public void showLanLobby() {
        root.getChildren().setAll(new LanLobbyView(this));
    }

    public void startPdk() {
        root.getChildren().setAll(new PdkTableView(this));
    }

    public void startLiar() {
        root.getChildren().setAll(new LiarTableView(this));
    }

    /** 进入联机桌面(主机视角)。 */
    public void openLanHostTable(LanHost host) {
        root.getChildren().setAll(new LanGameTableView(this, host));
    }

    /** 进入联机桌面(客户端视角)。 */
    public void openLanClientTable(LanClient client, GameType type, PlayerId seat, String hostIp) {
        root.getChildren().setAll(new LanGameTableView(this, client, type, seat, hostIp));
    }

    public void showRules() {
        root.getChildren().setAll(new RulesView(this));
    }

    /** 在当前页面之上叠加设置弹层（不破坏底层页面状态）。 */
    public void openSettings() {
        SettingsOverlay overlay = new SettingsOverlay(this);
        root.getChildren().add(overlay);
    }

    /** 关闭设置弹层（由 SettingsOverlay 调用）。 */
    public void closeSettings() {
        root.getChildren().removeIf(n -> n instanceof SettingsOverlay);
    }

    public void exit() {
        Platform.exit();
    }
}
