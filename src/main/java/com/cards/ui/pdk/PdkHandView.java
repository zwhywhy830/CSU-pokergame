package com.cards.ui.pdk;

import com.cards.ui.component.AvatarView;
import com.csu.pokergame.core.card.Card;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 阶段 23：跑得快底部玩家区域。
 *
 * <p>纵向三段（需求「二、牌桌布局重构 → 底部」）：
 * <pre>
 *   [头像 + 昵称 / Lv 等级 / 剩余牌数]   ← {@link PdkPlayerSeat}
 *   [手牌区域]                          ← {@link PdkCardView} × N
 *   [ 不出 ]  [ 出牌 ]                  ← {@link PdkActionBar}
 * </pre>
 *
 * <p>手牌增量更新：按牌复用已有 {@link PdkCardView}（{@link Card} 是 record），
 * 只创建新增的牌、丢弃已离手的牌，避免每次刷新重建全部节点。
 *
 * <p>选中集合由外部传入（与牌桌共用同一个 {@link Set}），本组件只负责视觉与交互。
 */
public final class PdkHandView extends VBox {

    private static final double CARD_SPACING = 6;

    private final PdkPlayerSeat seat;
    private final HBox cardRow;
    private final PdkActionBar actionBar;

    private final List<Card> cards = new ArrayList<>();
    private final Map<Card, PdkCardView> cardMap = new HashMap<>();
    private final Set<Card> selected;

    private boolean interactive;
    private int maxSelect = Integer.MAX_VALUE;
    private Consumer<Set<Card>> onSelectionChange;

    public PdkHandView(Set<Card> selectedSet) {
        super(10);
        this.selected = selectedSet;
        getStyleClass().add("pdk-hand-view");
        setAlignment(Pos.CENTER);

        seat = new PdkPlayerSeat("♛", "你", 1, true);
        seat.getStyleClass().add("pdk-seat-bottom");

        cardRow = new HBox(CARD_SPACING);
        cardRow.getStyleClass().add("pdk-hand-row");
        cardRow.setAlignment(Pos.CENTER);
        cardRow.setMinHeight(PdkCardView.CARD_H + PdkCardView.SELECT_LIFT + 8);

        actionBar = new PdkActionBar();

        getChildren().addAll(seat, cardRow, actionBar);
    }

    // ============================================================= 内容

    /** 本人座位（头像 / 等级 / 剩余牌数 / 状态）。 */
    public PdkPlayerSeat getSeat() {
        return seat;
    }

    /** 操作按钮条。 */
    public PdkActionBar getActionBar() {
        return actionBar;
    }

    /** 本人头像（结算升级光环等复用）。 */
    public AvatarView getAvatar() {
        return seat.getAvatar();
    }

    /** 是否显示顶部本人座位行（联机桌把座位统一放在顶部座位条时关闭）。 */
    public void setSeatVisible(boolean visible) {
        seat.setVisible(visible);
        seat.setManaged(visible);
    }

    /**
     * 设置手牌并重绘。传 null / 空列表清空。
     *
     * <p>增量更新：按牌复用已有 {@link PdkCardView}，只增删差异部分。
     */
    public void setCards(List<Card> newCards) {
        if (newCards == null || newCards.isEmpty()) {
            cards.clear();
            cardMap.clear();
            cardRow.getChildren().clear();
            return;
        }

        List<Card> sorted = new ArrayList<>(newCards);
        sorted.sort(Comparator.comparingInt(c -> c.rank().comparisonValue()));
        cards.clear();
        cards.addAll(sorted);
        cardMap.keySet().retainAll(sorted);

        List<Node> ordered = new ArrayList<>(sorted.size());
        for (Card card : sorted) {
            PdkCardView view = cardMap.get(card);
            if (view == null) {
                view = createCardView(card);
                cardMap.put(card, view);
            }
            // 选中集合以外部为准（重开 / 出牌后同步）
            view.setSelected(selected.contains(card));
            ordered.add(view);
        }
        if (!cardRow.getChildren().equals(ordered)) {
            cardRow.getChildren().setAll(ordered);
        }
    }

    /** 是否允许点击选中。 */
    public void setInteractive(boolean value) {
        this.interactive = value;
        for (PdkCardView view : cardMap.values()) {
            view.setInteractive(value);
        }
    }

    /** 最大选中数。 */
    public void setMaxSelect(int max) {
        this.maxSelect = max;
    }

    /** 选中集合变化回调。 */
    public void setOnSelectionChange(Consumer<Set<Card>> callback) {
        this.onSelectionChange = callback;
    }

    /** 只刷新选中态（外部修改 selected 后调用）。 */
    public void refreshSelection() {
        for (Map.Entry<Card, PdkCardView> entry : cardMap.entrySet()) {
            entry.getValue().setSelected(selected.contains(entry.getKey()));
        }
    }

    /** 获取某张牌的节点（飞牌动画起点坐标）。 */
    public Node getCardNode(Card card) {
        return cardMap.get(card);
    }

    /** 当前展示的手牌（只读副本）。 */
    public List<Card> getCards() {
        return List.copyOf(cards);
    }

    // ============================================================= 内部

    private PdkCardView createCardView(Card card) {
        PdkCardView view = new PdkCardView(card);
        view.setInteractive(interactive);
        view.setOnToggle(() -> {
            if (!interactive) {
                return;
            }
            if (selected.contains(card)) {
                selected.remove(card);
            } else if (selected.size() < maxSelect) {
                selected.add(card);
            }
            view.setSelected(selected.contains(card));
            if (onSelectionChange != null) {
                onSelectionChange.accept(selected);
            }
        });
        return view;
    }
}
