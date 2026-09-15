package com.csu.pokergame.ui.component;

import com.csu.pokergame.core.card.Card;
import javafx.scene.control.Button;

/** 文字牌视图:以文字展示一张牌,可切换选中态;Day 2 游戏桌直接复用,后续可替换为贴图。 */
public final class CardView extends Button {

    private boolean selected;

    public CardView(Card card) {
        super(card.display());
        getStyleClass().setAll("card");
        setOnAction(e -> setSelected(!selected));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
        } else {
            getStyleClass().remove("selected");
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
