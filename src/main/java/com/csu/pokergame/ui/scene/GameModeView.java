package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 游戏模式选择页：两张大卡片——本地对战 / 局域网联机。
 * 顶部右上角统一设置入口；底部留返回主页按钮。
 */
public final class GameModeView extends BorderPane {

    public GameModeView(AppShell shell) {
        setPadding(new Insets(16));

        // 顶部：左返回主页，中间标题，右设置
        Button home = new Button("返回主页");
        home.setOnAction(e -> shell.navigate("home"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        Label title = new Label("选择游戏模式");
        title.getStyleClass().add("home-title");
        BorderPane topBar = new BorderPane();
        topBar.setLeft(home);
        topBar.setCenter(title);
        topBar.setRight(settings);
        BorderPane.setAlignment(title, Pos.CENTER);
        setTop(topBar);

        // 中央：两张模式卡片
        HBox cards = new HBox(32);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(48, 16, 16, 16));
        cards.getChildren().add(modeCard("本地对战",
                "单机人机对战，无需联网，立刻开始一局", () -> shell.navigate("local-select")));
        cards.getChildren().add(modeCard("局域网联机",
                "和同局域网的好友联机对战（功能开发中）", () -> shell.navigate("lan-lobby")));
        setCenter(cards);
    }

    private VBox modeCard(String name, String desc, Runnable onClick) {
        VBox card = new VBox(10);
        card.getStyleClass().add("game-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(280);

        Label title = new Label(name);
        title.getStyleClass().add("title");
        Label description = new Label(desc);
        description.getStyleClass().add("desc");
        description.setWrapText(true);
        description.setPrefWidth(240);
        description.setAlignment(Pos.CENTER);

        Button choose = new Button("进入");
        choose.getStyleClass().add("primary");
        choose.setOnAction(e -> onClick.run());

        card.getChildren().addAll(title, description, choose);
        return card;
    }
}
