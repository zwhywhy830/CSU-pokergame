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
 * 本地对战游戏选择页：跑得快 / 骗子酒馆 两张卡片。
 * 顶部右上角统一设置入口。
 */
public final class LocalGameSelectView extends BorderPane {

    public LocalGameSelectView(AppShell shell) {
        setPadding(new Insets(16));

        // 顶部：左返回模式选择，中间标题，右设置
        Button back = new Button("返回模式选择");
        back.setOnAction(e -> shell.navigate("game-modes"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        Label title = new Label("本地对战 · 选择游戏");
        title.getStyleClass().add("home-title");
        BorderPane topBar = new BorderPane();
        topBar.setLeft(back);
        topBar.setCenter(title);
        topBar.setRight(settings);
        BorderPane.setAlignment(title, Pos.CENTER);
        setTop(topBar);

        // 中央：两张游戏卡片
        HBox cards = new HBox(24);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(48, 16, 16, 16));
        cards.getChildren().add(gameCard("湖南跑得快",
                "三人各自为战，有大必出，最先出完获胜", () -> shell.navigate("pdk")));
        cards.getChildren().add(gameCard("骗子酒馆",
                "四人淘汰制，每人一把 6 仓手枪，质疑失败扣扳机",
                () -> shell.navigate("liar")));
        setCenter(cards);
    }

    private VBox gameCard(String name, String desc, Runnable onClick) {
        VBox card = new VBox(10);
        card.getStyleClass().add("game-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(260);

        Label title = new Label(name);
        title.getStyleClass().add("title");
        Label description = new Label(desc);
        description.getStyleClass().add("desc");
        description.setWrapText(true);
        description.setPrefWidth(220);
        description.setAlignment(Pos.CENTER);

        Button choose = new Button("开始");
        choose.getStyleClass().add("primary");
        choose.setOnAction(e -> onClick.run());

        card.getChildren().addAll(title, description, choose);
        return card;
    }
}
