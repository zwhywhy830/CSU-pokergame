package com.csu.pokergame.ui.scene;

import com.cards.ui.background.BackgroundManager;
import com.csu.pokergame.ui.AppShell;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载动画页：三个跳动金点 + 进度条 + "正在进入牌桌"。
 *
 * <p>改编自 {@code DeckApp.buildLoadingScene}：本类直接继承 {@link StackPane}
 * （而非创建 {@code Scene}），由 {@link AppShell} 路由系统挂载到根容器。
 * 加载动画演完（约 1.65s）后自动 {@code shell.navigate("game-choice")}。
 *
 * <p>样式由全局 {@code app.css} 统一管理（loading-card / loading-bar-track /
 * loading-bar-fill / loading-tip），本类不单独加载 stylesheet。
 */
public final class LoadingView extends StackPane {

    public LoadingView(AppShell shell) {
        // 背景：与其它页一致的国风山水层（BackgroundManager 统一构建）
        getChildren().add(BackgroundManager.createLoadingBackground().root());

        // 中央加载卡片：玻璃质感深色圆角面板
        StackPane card = new StackPane();
        card.getStyleClass().add("loading-card");
        card.setMouseTransparent(true);

        // 跳动的三个金点
        HBox dots = new HBox(14);
        dots.setAlignment(Pos.CENTER);
        dots.setMouseTransparent(true);
        List<Label> dotList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Label d = new Label("●");
            d.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 18));
            d.setTextFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fff3c4")),
                    new Stop(0.55, Color.web("#e8c25e")),
                    new Stop(1, Color.web("#b07f1e"))));
            d.setMouseTransparent(true);
            dotList.add(d);
            dots.getChildren().add(d);
        }

        // 进度条容器：固定宽高，确定式填充（高亮条从左填满到右）
        double barW = 280;
        double barH = 8;
        StackPane barTrack = new StackPane();
        barTrack.setPrefSize(barW, barH);
        barTrack.setMaxSize(barW, barH);
        barTrack.setMinSize(barW, barH);
        barTrack.getStyleClass().add("loading-bar-track");
        barTrack.setMouseTransparent(true);
        // 圆角裁剪：填充条与光晕只在轨道内显示，不溢出两端
        Rectangle barClip = new Rectangle(barW, barH);
        barClip.setArcWidth(9);
        barClip.setArcHeight(9);
        barTrack.setClip(barClip);
        Region barFill = new Region();
        barFill.getStyleClass().add("loading-bar-fill");
        barFill.setMouseTransparent(true);
        barFill.setMinHeight(6);
        barFill.setPrefHeight(6);
        barFill.setMaxHeight(6);
        // 宽度由入场动画 0 -> barW 驱动，呈“加载到满格”的确定式进度
        barFill.setMinWidth(0);
        barFill.setPrefWidth(barW);
        barFill.setMaxWidth(0);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        barTrack.getChildren().add(barFill);

        Label tip = new Label("正在进入牌桌");
        tip.getStyleClass().add("loading-tip");
        tip.setMouseTransparent(true);

        VBox box = new VBox(22, dots, barTrack, tip);
        box.setAlignment(Pos.CENTER);
        card.getChildren().add(box);

        StackPane.setAlignment(card, Pos.CENTER);
        getChildren().add(card);

        // 三点跳动：合并到单条时间轴，统一 540ms 周期、峰值间隔 180ms，
        // 形成稳定连续的波浪节奏（原来三条独立时间轴周期不同，相位会漂移、节奏忽快忽慢）
        Timeline dotsTl = new Timeline();
        double wavePeriod = 540;
        for (int i = 0; i < dotList.size(); i++) {
            Label d = dotList.get(i);
            double base = i * 180;
            dotsTl.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(base), new KeyValue(d.translateYProperty(), 0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(base + 90), new KeyValue(d.translateYProperty(), -8, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(base + 180), new KeyValue(d.translateYProperty(), 0, Interpolator.EASE_BOTH)));
        }
        // 末尾锚点：把周期拉齐到 wavePeriod，末点回落处即下一循环起点，首尾无缝
        dotsTl.getKeyFrames().add(new KeyFrame(Duration.millis(wavePeriod),
                new KeyValue(dotList.get(dotList.size() - 1).translateYProperty(), 0)));
        dotsTl.setCycleCount(Timeline.INDEFINITE);
        dotsTl.play();

        // 主动画：卡片淡入 → 进度条填满（确定式）→ 满格短暂停留 → 淡出切场，约 1.65s
        card.setOpacity(0.0);
        Timeline intro = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(card.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(300), new KeyValue(card.opacityProperty(), 1.0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(350), new KeyValue(barFill.maxWidthProperty(), 0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1150), new KeyValue(barFill.maxWidthProperty(), barW, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1300), new KeyValue(card.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1650), new KeyValue(card.opacityProperty(), 0.0, Interpolator.EASE_IN)));
        intro.setOnFinished(e -> {
            dotsTl.stop();
            shell.navigate("game-choice");
        });
        intro.play();
    }
}
