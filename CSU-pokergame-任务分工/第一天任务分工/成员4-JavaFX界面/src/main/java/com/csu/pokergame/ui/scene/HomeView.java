package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/** 首页:游戏名称 + 「开始游戏」 + 右上角设置按钮。 */
public final class HomeView extends BorderPane {

    public HomeView(AppShell shell) {
        Text title = new Text("CSU Poker Game");
        title.getStyleClass().add("home-title");
        Text subtitle = new Text("中 南 棋 牌 室");
        subtitle.getStyleClass().add("home-subtitle");

        Button startButton = new Button("开始游戏");
        startButton.getStyleClass().addAll("primary", "start-button");
        startButton.setOnAction(e -> shell.showGameModes());

        VBox center = new VBox(18, title, subtitle, startButton);
        center.setAlignment(Pos.CENTER);
        setCenter(center);

        Button settingsButton = new Button("⚙ 设置");
        settingsButton.setOnAction(e -> shell.openSettings());
        BorderPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        BorderPane.setMargin(settingsButton, new Insets(16));
        setTop(settingsButton);
    }
}
