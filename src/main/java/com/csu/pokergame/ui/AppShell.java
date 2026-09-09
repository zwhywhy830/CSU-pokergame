package com.csu.pokergame.ui;

import com.csu.pokergame.ui.scene.SettingsOverlay;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 应用外壳：管理单一 Scene 与页面切换。
 *
 * <p>采用注册表模式：每个页面通过 {@link #register(String, Supplier)} 注册路由名，
 * 切换时调用 {@link #navigate(String)} 即可。新增页面不再修改本类，
 * 只在自己的 View 内或 {@code RouteTable} 中注册一行。
 */
public final class AppShell {

    private final StackPane root = new StackPane();
    private final Map<String, Supplier<Parent>> routes = new HashMap<>();

    public AppShell(Scene scene) {
        root.getStyleClass().add("root");
        scene.setRoot(root);
        scene.getStylesheets().add(AppShell.class
                .getResource("/com/csu/pokergame/ui/theme/app.css").toExternalForm());
    }

    /**
     * 注册页面路由。
     *
     * @param name    路由名（如 "home"、"pdk"）
     * @param factory 造页面的工厂，每次 navigate 时调用
     */
    public void register(String name, Supplier<Parent> factory) {
        routes.put(name, factory);
    }

    /**
     * 切换到指定路由页面。未注册的路由名会抛出异常。
     */
    public void navigate(String name) {
        Supplier<Parent> factory = routes.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("未注册的路由: " + name);
        }
        root.getChildren().setAll(factory.get());
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
