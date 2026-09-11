package com.cards.ui;

import com.cards.model.Card;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.function.BiFunction;

/**
 * 一张牌的卡片组件：展示牌面或牌背，交互上保持固定不动。
 * 牌面/牌背图像由外部注入的 imageProvider 提供（便于缓存与牌背配色切换）。
 */
public final class CardCell extends VBox {

    /** 界面展示时的牌面宽度（高清图按比例缩放）。 */
    public static final double DISPLAY_WIDTH = 106;

    private final Card card;
    private final BiFunction<Card, Boolean, Image> imageProvider;
    private boolean up = true;
    private final ImageView imageView;
    private final Label caption;

    public CardCell(Card card, BiFunction<Card, Boolean, Image> imageProvider) {
        this.card = card;
        this.imageProvider = imageProvider;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(5, 5, 3, 5));
        getStyleClass().add("card-cell");

        imageView = new ImageView(imageProvider.apply(card, true));
        imageView.setFitWidth(DISPLAY_WIDTH);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setEffect(baseShadow());

        caption = new Label(card.name());
        caption.getStyleClass().add("card-caption");
        getChildren().addAll(imageView, caption);
    }

    public Card card() {
        return card;
    }

    public void setFaceUp(boolean value) {
        if (up == value) {
            return;
        }
        up = value;
        applyState();
    }

    public void applyState() {
        imageView.setImage(imageProvider.apply(card, up));
        caption.setText(up ? card.name() : "背面");
    }

    private static DropShadow baseShadow() {
        return new DropShadow(10, Color.rgb(0, 0, 0, 0.35));
    }
}
