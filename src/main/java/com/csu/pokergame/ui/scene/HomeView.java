package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** 主加载页：标题 + 副标题 + 「开始游戏」按钮，右上角个人中心 / 设置入口。 */
public final class HomeView extends BorderPane {

    public HomeView(AppShell shell) {
        setPadding(new Insets(16));

        // 右上角：个人中心 + 设置中心（全屏页面，非弹层）
        Button profile = new Button("个人中心");
        profile.setOnAction(e -> shell.navigate("profile"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.navigate("settings"));
        HBox topRight = new HBox(10, profile, settings);
        BorderPane.setAlignment(topRight, Pos.TOP_RIGHT);
        setTop(topRight);

        VBox center = new VBox(24);
        center.setAlignment(Pos.CENTER);

        Label title = new Label("CSU Poker Game");
        title.getStyleClass().add("home-title");

        Label subtitle = new Label("中南棋牌室");
        subtitle.getStyleClass().add("home-subtitle");

        Button start = new Button("开始游戏");
        start.getStyleClass().add("primary");
        start.setPrefWidth(200);
        start.setOnAction(e -> shell.navigate("game-modes"));

        center.getChildren().addAll(title, subtitle, start);
        setCenter(center);
    }
}
