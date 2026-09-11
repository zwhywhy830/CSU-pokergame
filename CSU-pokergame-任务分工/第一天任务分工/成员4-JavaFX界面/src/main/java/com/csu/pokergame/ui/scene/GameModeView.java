package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 模式选择页:两张卡片 ——「本地对战」「局域网联机」。
 * 局域网今天只做入口占位(联机大厅由成员 5 明后天交付)。
 */
public final class GameModeView extends BorderPane {

    public GameModeView(AppShell shell) {
        Label title = new Label("选择模式");
        title.getStyleClass().add("home-title");

        VBox localCard = card("本地对战", "1 人 + 机器人,跑得快 / 骗子酒馆", () -> shell.startPdk());
        VBox lanCard = card("局域网联机", "创建房间或输入主机 IPv4 加入", () -> shell.showLanPlaceholder());

        HBox cards = new HBox(32, localCard, lanCard);
        cards.setAlignment(Pos.CENTER);

        Button backButton = new Button("← 返回首页");
        backButton.setOnAction(e -> shell.showHome());

        VBox center = new VBox(28, title, cards, backButton);
        center.setAlignment(Pos.CENTER);
        setCenter(center);
    }

    private VBox card(String name, String desc, Runnable onClick) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("title");
        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("desc");

        VBox box = new VBox(10, nameLabel, descLabel);
        box.getStyleClass().add("game-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefSize(260, 160);
        box.setOnMouseClicked(e -> onClick.run());
        return box;
    }
}
