package com.cards.ui.component;

import com.cards.bridge.CsuCardBridge;
import com.csu.pokergame.core.card.Card;
import com.cards.ui.theme.DesignTokens;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 手牌视图：统一管理玩家手牌的排列、选中、hover 效果。
 *
 * <p>替代 DeckApp 中直接创建 ImageView + 手动 toggle 的逻辑。
 * 数据继续使用现有 {@link Card} + {@link CsuCardBridge}，不修改卡牌模型。
 *
 * <p>效果：
 * <ul>
 *   <li>点击牌：上浮 + 金色边框 + 阴影增强</li>
 *   <li>hover：轻微上浮 + 阴影</li>
 *   <li>按点数自动排序排列</li>
 * </ul>
 */
public final class HandCardView extends HBox {

    /** 卡牌尺寸（跑得快/骗子酒馆共用）。 */
    public static final double CARD_W = 64;
    public static final double CARD_H = 90;

    /** 选中时上浮偏移。 */
    private static final double SELECT_LIFT = 16;
    /** hover 时上浮偏移。 */
    private static final double HOVER_LIFT = 6;

    private final List<Card> cards = new ArrayList<>();
    private final Set<Card> selected;
    private boolean interactive = false;
    private int maxSelect = Integer.MAX_VALUE;
    private Consumer<Set<Card>> onSelectionChange;

    /** 内部映射：Card → ImageView，便于刷新选中态。 */
    private final java.util.Map<Card, ImageView> cardMap = new java.util.HashMap<>();

    public HandCardView(Set<Card> selectedSet) {
        this.selected = selectedSet;
        getStyleClass().add("hand-card-view");
        setAlignment(Pos.CENTER);
        setSpacing(DesignTokens.SPACING_XS);
        setMinHeight(CARD_H + 16);
    }

    /**
     * 设置手牌并重绘。传入 null 或空列表清空。
     *
     * <p>增量更新：牌桌每次刷新（出牌 / bot 回合）都会调用本方法，而手牌通常只有一两张变化。
     * 这里按牌复用已有 {@link ImageView}（{@link Card} 是 record，equals 由点数 + 花色决定），
     * 只创建新增的牌、丢弃已离手的牌，避免每次清空重建全部节点 ——
     * 重建节点会连带为每个新节点重算 CSS 阴影（最多 17 张），是刷新时的主要开销。
     */
    public void setCards(List<Card> newCards) {
        if (newCards == null || newCards.isEmpty()) {
            cards.clear();
            cardMap.clear();
            getChildren().clear();
            return;
        }

        // 按点数排序
        List<Card> sorted = new ArrayList<>(newCards);
        sorted.sort(Comparator.comparingInt(c -> c.rank().comparisonValue()));
        cards.clear();
        cards.addAll(sorted);
        cardMap.keySet().retainAll(sorted); // 丢弃已不在手牌中的牌

        List<ImageView> ordered = new ArrayList<>(sorted.size());
        for (Card card : sorted) {
            ImageView iv = cardMap.get(card);
            if (iv == null) {
                iv = createCardNode(card);
                cardMap.put(card, iv);
            }
            ordered.add(iv);
        }
        if (!getChildren().equals(ordered)) {
            getChildren().setAll(ordered);
        }
        applySelectionVisuals();
    }

    /** 是否允许点击选中。 */
    public void setInteractive(boolean value) {
        this.interactive = value;
    }

    /** 最大选中数（骗子酒馆最多 3 张）。 */
    public void setMaxSelect(int max) {
        this.maxSelect = max;
    }

    /** 选中集合变化回调。 */
    public void setOnSelectionChange(Consumer<Set<Card>> callback) {
        this.onSelectionChange = callback;
    }

    /** 刷新选中态的视觉效果（外部修改 selected 后调用）。 */
    public void refreshSelection() {
        applySelectionVisuals();
    }

    // ============================================================= 内部方法

    private ImageView createCardNode(Card card) {
        ImageView iv = new ImageView(CsuCardBridge.faceImage(card));
        iv.setFitWidth(CARD_W);
        iv.setFitHeight(CARD_H);
        iv.getStyleClass().add("hand-card");
        iv.setPickOnBounds(true);

        // hover 效果
        iv.setOnMouseEntered(e -> {
            if (!selected.contains(card)) {
                iv.setTranslateY(-HOVER_LIFT);
                iv.getStyleClass().add("hand-card-hover");
            }
        });
        iv.setOnMouseExited(e -> {
            if (!selected.contains(card)) {
                iv.setTranslateY(0);
                iv.getStyleClass().remove("hand-card-hover");
            }
        });

        // 点击 toggle
        iv.setOnMouseClicked(e -> {
            if (!interactive) return;
            if (selected.contains(card)) {
                selected.remove(card);
            } else if (selected.size() < maxSelect) {
                selected.add(card);
            }
            applySelectionVisuals();
            if (onSelectionChange != null) {
                onSelectionChange.accept(selected);
            }
        });

        return iv;
    }

    private void applySelectionVisuals() {
        for (var entry : cardMap.entrySet()) {
            Card card = entry.getKey();
            ImageView iv = entry.getValue();
            boolean sel = selected.contains(card);
            iv.setTranslateY(sel ? -SELECT_LIFT : 0);
            // 切换 CSS class
            iv.getStyleClass().removeAll("hand-card-selected", "hand-card-hover");
            if (sel) {
                iv.getStyleClass().add("hand-card-selected");
            }
        }
    }

    /** 获取某张牌的 ImageView（飞牌动画需要起点坐标）。 */
    public ImageView getCardNode(Card card) {
        return cardMap.get(card);
    }
}
