package com.cards.ui.component;

import com.cards.bridge.CsuCardBridge;
import com.csu.pokergame.core.card.Card;
import com.cards.ui.theme.DesignTokens;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 桌面中央出牌区视图：显示最近一次出牌 + 出牌玩家标签。
 *
 * <p>替代 DeckApp 中的 pdkCenterBox / liarCenterBox 手工 HBox。
 * 不持有引擎引用，由调用方在每次刷新时 setCards。
 *
 * <p>出牌动画入口：{@link #playFlyIn(List, String)} 接收新出的牌和玩家名，
 * 内部调用 {@link com.cards.ui.animation.CardFlyAnimation} 完成飞入动画。
 */
public final class PlayedCardsView extends VBox {

    private static final double PLAYED_W = 56;
    private static final double PLAYED_H = 78;

    private final HBox cardRow;
    private final Label playerLabel;
    private final Label emptyHint;

    public PlayedCardsView() {
        super(DesignTokens.SPACING_XS);
        getStyleClass().add("played-cards-view");
        setAlignment(Pos.CENTER);
        setMinHeight(120);
        setFillWidth(true);

        playerLabel = new Label();
        playerLabel.getStyleClass().add("played-player-label");

        cardRow = new HBox(DesignTokens.SPACING_XS);
        cardRow.getStyleClass().add("played-card-row");
        cardRow.setAlignment(Pos.CENTER);

        emptyHint = new Label("（桌面为空，自由出牌）");
        emptyHint.getStyleClass().add("table-hint");
        emptyHint.setVisible(false);

        StackPane rowWrap = new StackPane(cardRow, emptyHint);
        rowWrap.setAlignment(Pos.CENTER);
        getChildren().addAll(playerLabel, rowWrap);
    }

    /** 设置显示的牌和出牌玩家名。传 null/空清空。 */
    public void setCards(List<Card> cards, String playerName) {
        if (cards == null || cards.isEmpty()) {
            cardRow.getChildren().clear();
            emptyHint.setVisible(true);
            playerLabel.setText("");
            return;
        }
        emptyHint.setVisible(false);
        playerLabel.setText(playerName == null ? "" : playerName + " 出牌");
        syncCardNodes(cards);
    }

    /**
     * 增量同步出牌行：复用已有 {@link ImageView}，只更新牌面图像 / 增删差异部分。
     * 牌桌每次刷新都会调用 {@link #setCards}，清空重建会让每个新节点重算一次 CSS 阴影。
     */
    private void syncCardNodes(List<Card> cards) {
        var children = cardRow.getChildren();
        while (children.size() > cards.size()) {
            children.remove(children.size() - 1);
        }
        for (int i = 0; i < cards.size(); i++) {
            Image image = CsuCardBridge.faceImage(cards.get(i));
            if (i < children.size()) {
                ((ImageView) children.get(i)).setImage(image);
            } else {
                ImageView iv = new ImageView(image);
                iv.setFitWidth(PLAYED_W);
                iv.setFitHeight(PLAYED_H);
                iv.setMouseTransparent(true);
                iv.getStyleClass().add("played-card");
                children.add(iv);
            }
        }
    }

    /**
     * 出牌飞入动画入口：从指定起始坐标飞入桌面中央。
     *
     * @param cards       要飞的牌
     * @param playerName  出牌玩家名
     * @param fromX       起始 x（场景坐标）
     * @param fromY       起始 y（场景坐标）
     * @param onComplete  动画结束回调（可 null）
     */
    public void playFlyIn(List<Card> cards, String playerName,
                          double fromX, double fromY, Runnable onComplete) {
        emptyHint.setVisible(false);
        playerLabel.setText(playerName == null ? "" : playerName + " 出牌");

        if (cards == null || cards.isEmpty()) {
            cardRow.getChildren().clear();
            if (onComplete != null) onComplete.run();
            return;
        }

        // 复用 / 补齐目标 ImageView（飞入后留在桌面），飞入前统一隐藏
        syncCardNodes(cards);
        var targets = new java.util.ArrayList<ImageView>();
        for (var node : cardRow.getChildren()) {
            ImageView iv = (ImageView) node;
            iv.setVisible(false); // 飞入前隐藏，动画中显示
            targets.add(iv);
        }

        // 延迟到 layout 后获取目标坐标，然后飞牌
        javafx.application.Platform.runLater(() -> {
            int total = targets.size();
            for (int idx = 0; idx < total; idx++) {
                ImageView target = targets.get(idx);
                double delay = idx * 80; // 每张牌错开 80ms
                boolean isLast = idx == total - 1;
                javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(delay));
                wait.setOnFinished(e -> {
                    target.setVisible(true);
                    com.cards.ui.animation.CardFlyAnimation.flyTo(
                            target, fromX, fromY, isLast ? onComplete : null);
                });
                wait.play();
            }
        });
    }
}
