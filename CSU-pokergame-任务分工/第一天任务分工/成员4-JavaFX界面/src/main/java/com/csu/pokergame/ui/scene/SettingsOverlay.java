package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 设置浮层:全屏遮罩 + 居中设置面板,叠在当前页之上(StackPane 顶层),点空白处关闭。
 * 音量 / 亮度今天做占位,不接真实逻辑。
 */
public final class SettingsOverlay extends StackPane {

    public SettingsOverlay(AppShell shell) {
        getStyleClass().add("settings-overlay");

        Label title = new Label("设置");
        title.getStyleClass().add("settings-title");

        Label volumeLabel = new Label("音量");
        Slider volume = new Slider(0, 100, 50);
        Label brightnessLabel = new Label("亮度");
        Slider brightness = new Slider(0, 100, 100);

        Button rulesButton = new Button("查看规则");
        rulesButton.getStyleClass().add("settings-button");
        rulesButton.setOnAction(e -> {
            shell.closeSettings();
            shell.showRules();
        });

        Button homeButton = new Button("返回主页");
        homeButton.getStyleClass().add("settings-button");
        homeButton.setOnAction(e -> {
            shell.closeSettings();
            shell.showHome();
        });

        Button exitButton = new Button("退出游戏");
        exitButton.getStyleClass().addAll("settings-button", "danger");
        exitButton.setOnAction(e -> shell.exit());

        VBox box = new VBox(12,
                title,
                volumeLabel, volume,
                brightnessLabel, brightness,
                rulesButton, homeButton, exitButton);
        box.getStyleClass().add("settings-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(24));
        box.setPrefSize(320, 420);
        box.setMaxSize(320, 420);

        getChildren().add(box);
        // 点空白处(遮罩)关闭;点面板本身不关闭
        setOnMouseClicked(e -> shell.closeSettings());
        box.setOnMouseClicked(e -> e.consume());
    }
}
