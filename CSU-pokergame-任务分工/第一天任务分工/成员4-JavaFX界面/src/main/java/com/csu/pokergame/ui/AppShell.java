package com.csu.pokergame.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.csu.pokergame.ui.scene.GameModeView;
import com.csu.pokergame.ui.scene.HomeView;
import com.csu.pokergame.ui.scene.SettingsOverlay;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 应用外壳:单 Scene 页面切换 + 设置浮层叠加(浮层叠在当前页之上,不销毁底层页面)。
 *
 * <p>页面约定(给 Day 2 的游戏桌立规矩):每台电脑自己的手牌始终放底部,
 * 其他玩家在上方/两侧,非当前回合时出牌按钮禁用。
 */
public final class AppShell {

    private final Scene scene;
    private final StackPane root;
    private final Stage stage;

    private SettingsOverlay settingsOverlay;

    /** 挂载 root、加载深色主题样式;调用方随后应调用 {@link #showHome()}。 */
    public AppShell(Scene scene) {
        this.scene = scene;
        this.stage = (Stage) scene.getWindow();
        this.root = new StackPane();
        scene.setRoot(root);
        var css = AppShell.class.getResource("/com/csu/pokergame/ui/theme/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    /** 首页。 */
    public void showHome() {
        showContent(new HomeView(this));
    }

    /** 模式选择页。 */
    public void showGameModes() {
        showContent(new GameModeView(this));
    }

    /** 进入跑得快:今天先显示「开发中」占位,Day 2 接成员 3 的引擎。 */
    public void startPdk() {
        showPlaceholder("跑得快", "游戏桌 Day 2 上线,今天先占位");
    }

    /** 进入骗子酒馆:同上。 */
    public void startLiar() {
        showPlaceholder("骗子酒馆", "游戏桌 Day 2 上线,今天先占位");
    }

    /** 规则页:两个 Tab 分别展示两个游戏的规则(读 resources/rules/*.md,文档缺失时显示提示)。 */
    public void showRules() {
        Tab pdkTab = new Tab("跑得快", rulesText("/rules/跑得快.md"));
        Tab liarTab = new Tab("骗子酒馆", rulesText("/rules/骗子酒馆.md"));
        pdkTab.setClosable(false);
        liarTab.setClosable(false);
        TabPane tabs = new TabPane(pdkTab, liarTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane page = new BorderPane(tabs);
        Button backButton = new Button("← 返回");
        backButton.setOnAction(e -> showHome());
        BorderPane topBar = new BorderPane();
        topBar.setLeft(backButton);
        topBar.setPadding(new Insets(10));
        page.setTop(topBar);
        showContent(page);
    }

    /** 局域网联机入口占位:联机大厅由成员 5 明后天交付。 */
    public void showLanPlaceholder() {
        showPlaceholder("局域网联机", "联机大厅 Day 2–3 上线,今天先占位");
    }

    /** 叠加设置浮层,不销毁底层页面。 */
    public void openSettings() {
        closeSettings();
        settingsOverlay = new SettingsOverlay(this);
        root.getChildren().add(settingsOverlay);
    }

    public void closeSettings() {
        if (settingsOverlay != null) {
            root.getChildren().remove(settingsOverlay);
            settingsOverlay = null;
        }
    }

    public void exit() {
        if (stage != null) {
            stage.close();
        }
        Platform.exit();
    }

    // ---------- 内部 ----------

    private void showContent(Pane content) {
        closeSettings();
        root.getChildren().setAll(content);
    }

    /** 占位页(今天所有未开工页面统一走这里)。 */
    private void showPlaceholder(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("home-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("home-subtitle");
        Button backButton = new Button("← 返回模式选择");
        backButton.getStyleClass().add("primary");
        backButton.setOnAction(e -> showGameModes());

        VBox box = new VBox(16, titleLabel, descLabel, backButton);
        box.setAlignment(Pos.CENTER);
        showContent(box);
    }

    private Node rulesText(String resourcePath) {
        String text;
        try (InputStream in = AppShell.class.getResourceAsStream(resourcePath)) {
            text = in == null ? "规则文档尚未提交:" + resourcePath
                    : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            text = "规则文档读取失败:" + e.getMessage();
        }
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        return area;
    }
}
