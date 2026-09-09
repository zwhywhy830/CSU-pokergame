package com.csu.pokergame.ui.scene;

import com.csu.pokergame.ui.AppShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 规则页：以标签页展示两款游戏的规则（UTF-8 从资源文件读取），顶部含设置入口。 */
public final class RulesView extends BorderPane {

    public RulesView(AppShell shell) {
        setPadding(new Insets(16));

        Button back = new Button("返回");
        back.setOnAction(e -> shell.navigate("game-modes"));
        Button settings = new Button("设置");
        settings.setOnAction(e -> shell.openSettings());
        Label heading = new Label("游戏规则");
        heading.getStyleClass().add("home-title");
        BorderPane topBar = new BorderPane();
        topBar.setLeft(back);
        topBar.setCenter(heading);
        topBar.setRight(settings);
        BorderPane.setAlignment(heading, Pos.CENTER);
        setTop(topBar);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(ruleTab("湖南跑得快", "/rules/跑得快.md"));
        tabs.getTabs().add(ruleTab("骗子酒馆", "/rules/骗子酒馆.md"));
        setCenter(tabs);
    }

    private Tab ruleTab(String title, String resourcePath) {
        TextArea text = new TextArea(readResource(resourcePath));
        text.setEditable(false);
        text.setWrapText(true);
        text.getStyleClass().add("log-view");
        Tab tab = new Tab(title, text);
        tab.setClosable(false);
        return tab;
    }

    private String readResource(String path) {
        try (InputStream in = RulesView.class.getResourceAsStream(path)) {
            if (in == null) {
                return "规则文件未找到：" + path;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "读取规则失败：" + e.getMessage();
        }
    }
}
