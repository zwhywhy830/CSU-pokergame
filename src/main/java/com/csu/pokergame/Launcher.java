package com.csu.pokergame;

import com.csu.pokergame.ui.AppShell;
import com.csu.pokergame.ui.RouteTable;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * 程序入口。
 */
public class Launcher extends Application {

    public static final String APP_TITLE = "CSU Poker Game";

    @Override
    public void start(Stage stage) {
        stage.setTitle(APP_TITLE);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);

        Scene scene = new Scene(new StackPane(), 1100, 720);
        AppShell shell = new AppShell(scene);
        RouteTable.install(shell);
        shell.navigate("login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
