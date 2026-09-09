package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 通用设置弹层：覆盖在当前页面之上，不破坏底层页面状态。
 * 提供多项功能：查看规则、回到主页、返回游戏选择、返回当前对局重开、退出游戏、关闭。
 */
public final class SettingsOverlay extends StackPane {

    public SettingsOverlay(AppShell shell) {
        getStyleClass().add("result-overlay");
        setPickOnBounds(false); // 点空白处也能透到下面

        VBox box = new VBox(14);
        box.getStyleClass().add("settings-box");
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(360);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("设置");
        title.getStyleClass().add("settings-title");

        Label subtitle = new Label("暂停 / 退出 / 查看规则");
        subtitle.getStyleClass().add("settings-subtitle");

        Button viewRules = new Button("查看规则");
        viewRules.getStyleClass().addAll("primary", "settings-button");
        viewRules.setOnAction(e -> {
            shell.closeSettings();
            shell.navigate("rules");
        });

        Button home = new Button("返回主页");
        home.getStyleClass().add("settings-button");
        home.setOnAction(e -> {
            shell.closeSettings();
            shell.navigate("home");
        });

        Button gameModes = new Button("返回游戏选择");
        gameModes.getStyleClass().add("settings-button");
        gameModes.setOnAction(e -> {
            shell.closeSettings();
            shell.navigate("game-modes");
        });

        Separator sep = new Separator();
        sep.getStyleClass().add("settings-separator");

        Button exit = new Button("退出游戏");
        exit.getStyleClass().addAll("danger", "settings-button");
        exit.setOnAction(e -> shell.exit());

        Button close = new Button("关闭");
        close.getStyleClass().add("settings-button");
        close.setOnAction(e -> shell.closeSettings());

        box.getChildren().addAll(title, subtitle, viewRules, home, gameModes, sep, exit, close);

        // 点击空白区域关闭
        setOnMousePressed(e -> {
            if (e.getTarget() == this) {
                shell.closeSettings();
            }
        });

        getChildren().add(box);
    }
}
