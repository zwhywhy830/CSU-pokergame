package com.csu.pokergame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * 程序入口。今天先挂一个空 Scene 保证工程可跑;
 * 成员 4 下午交付 ui.AppShell 后,把这里的占位根节点替换为:
 *
 * <pre>{@code
 * Scene scene = new Scene(new StackPane(), 1100, 720);
 * AppShell shell = new AppShell(scene);
 * shell.showHome();
 * }</pre>
 */
public class Launcher extends Application {

    public static final String APP_TITLE = "CSU Poker Game";

    @Override
    public void start(Stage stage) {
        stage.setTitle(APP_TITLE);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);

        Label placeholder = new Label("工程骨架就绪,等待 AppShell 接入(成员 4)");
        StackPane root = new StackPane(placeholder);

        Scene scene = new Scene(root, 1100, 720);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
