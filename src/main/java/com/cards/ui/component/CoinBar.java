package com.cards.ui.component;

import com.cards.ui.theme.DesignTokens;
import com.cards.ui.theme.Theme;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 金币栏：牌桌顶部显示金币图标 + 积分数量。
 *
 * <p>视觉：玻璃卡片 + 金色数字 + 金币图标 + 轻微呼吸动画。
 * 数据为展示数据（不修改玩家数据模型），通过 {@link #setCoins(int)} 更新。
 */
public final class CoinBar extends HBox {

    private final Label coinLabel;
    private int coins = 1000;

    public CoinBar() {
        getStyleClass().add("coin-bar");
        setAlignment(Pos.CENTER);
        setSpacing(DesignTokens.SPACING_XS);

        // 金币图标（圆形渐变）
        Circle coinIcon = new Circle(12);
        coinIcon.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.GOLD_LIGHT),
                new Stop(0.5, Theme.GOLD),
                new Stop(1, Theme.GOLD_DARK)));
        coinIcon.setStroke(Theme.GOLD_BRIGHT);
        coinIcon.setStrokeWidth(1);
        StackPane iconWrap = new StackPane(coinIcon);
        iconWrap.getStyleClass().add("coin-icon-wrap");

        // 金币数字
        coinLabel = new Label();
        coinLabel.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 15));
        coinLabel.setTextFill(Theme.GOLD_TEXT);
        coinLabel.getStyleClass().add("coin-amount");

        getChildren().addAll(iconWrap, coinLabel);
        setCoins(1000);
        startBreath();
    }

    /** 设置金币数量。 */
    public void setCoins(int amount) {
        this.coins = Math.max(0, amount);
        coinLabel.setText("💰 " + this.coins);
    }

    /** 获取当前金币数量。 */
    public int getCoins() {
        return coins;
    }

    /** 增加金币（可负数减少）。 */
    public void addCoins(int delta) {
        setCoins(coins + delta);
    }

    private void startBreath() {
        Timeline pulse = new Timeline(
                new KeyFrame(javafx.util.Duration.ZERO,
                        new KeyValue(coinLabel.scaleXProperty(), 1.0),
                        new KeyValue(coinLabel.scaleYProperty(), 1.0)),
                new KeyFrame(javafx.util.Duration.millis(DesignTokens.ANIM_PULSE_SLOW.toMillis() / 2),
                        new KeyValue(coinLabel.scaleXProperty(), 1.06),
                        new KeyValue(coinLabel.scaleYProperty(), 1.06)),
                new KeyFrame(javafx.util.Duration.millis(DesignTokens.ANIM_PULSE_SLOW.toMillis()),
                        new KeyValue(coinLabel.scaleXProperty(), 1.0),
                        new KeyValue(coinLabel.scaleYProperty(), 1.0)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }
}
