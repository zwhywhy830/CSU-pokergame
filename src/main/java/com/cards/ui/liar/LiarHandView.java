package com.cards.ui.liar;

import com.cards.bridge.CsuCardBridge;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.core.card.Card;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 阶段 24：骗子酒馆「本人手牌」区域。
 *
 * <p><b>只有本地玩家自己的手牌正面可见</b>（与联机桌 {@code HandCardView} 行为一致）：
 * 用 {@link #setCards(List)} 传入 {@code LiarSnapshot.myHand()} 即渲染真实牌面。
 * 没有牌面数据时可用 {@link #setCount(int)} 退化为牌背占位。
 *
 * <p><b>对手手牌永不经过本组件</b>（引擎只给查看者自己的手牌），因此不存在泄漏风险。
 * 手牌「打出去的牌」在桌面上牌背朝下，也由 {@code LiarTableView} 另行表达。
 *
 * <pre>
 *   [ A♠ K♥ K♣ Q♦ Joker ]   ← {@link LiarCardFace} × N
 * </pre>
 */
public final class LiarHandView extends VBox {

    /** 牌面显示尺寸。 */
    public static final double CARD_W = 62;
    public static final double CARD_H = 88;
    /** 选中上浮距离（轻微上浮）。 */
    public static final double SELECT_LIFT = 12;
    /** 牌间距。 */
    private static final double SPACING = 8;

    private final HBox cardRow = new HBox(SPACING);
    private final List<LiarCardBase> cards = new ArrayList<>();
    /** 当前手牌；元素为 {@code null} 表示该位用牌背占位。 */
    private final List<Card> current = new ArrayList<>();
    private final Set<Integer> selected = new LinkedHashSet<>();

    private boolean interactive;
    private int maxSelect = 3;
    private Consumer<Set<Integer>> onSelectionChange;

    public LiarHandView() {
        super(6);
        getStyleClass().add("liar-hand");
        setAlignment(Pos.CENTER);

        cardRow.getStyleClass().add("liar-hand-row");
        cardRow.setAlignment(Pos.CENTER);
        cardRow.setMinHeight(CARD_H + SELECT_LIFT + 8);
        getChildren().add(cardRow);
    }

    // ============================================================= 内容

    /** 设置手牌并正面渲染（本地玩家自己的牌）。 */
    public void setCards(List<Card> hand) {
        List<Card> next = hand == null ? List.of() : List.copyOf(hand);
        if (current.equals(next)) {
            refreshSelection();
            return;
        }
        current.clear();
        current.addAll(next);
        rebuildNodes();
    }

    /** 设置手牌张数（无牌面数据时退化为牌背占位）。 */
    public void setCount(int count) {
        int n = Math.max(0, count);
        if (current.size() == n && current.stream().allMatch(Objects::isNull)) {
            refreshSelection();
            return;
        }
        current.clear();
        for (int i = 0; i < n; i++) {
            current.add(null);
        }
        rebuildNodes();
    }

    /** 当前手牌张数。 */
    public int getCount() {
        return cards.size();
    }

    /** 当前手牌（只读副本；牌背占位位为 {@code null}）。 */
    public List<Card> getCards() {
        return java.util.Collections.unmodifiableList(new ArrayList<>(current));
    }

    /** 是否允许点击选中。 */
    public void setInteractive(boolean value) {
        this.interactive = value;
        for (LiarCardBase card : cards) {
            card.setInteractive(value);
        }
    }

    /** 最大选中数。 */
    public void setMaxSelect(int max) {
        this.maxSelect = Math.max(1, max);
        if (selected.size() > this.maxSelect) {
            List<Integer> keep = new ArrayList<>(selected).subList(0, this.maxSelect);
            selected.clear();
            selected.addAll(keep);
            refreshSelection();
        }
    }

    /** 当前最大选中数。 */
    public int getMaxSelect() {
        return maxSelect;
    }

    /** 选中下标变化回调（回调中被修改的集合为只读副本）。 */
    public void setOnSelectionChange(Consumer<Set<Integer>> callback) {
        this.onSelectionChange = callback;
    }

    /** 当前选中下标（只读副本）。 */
    public Set<Integer> getSelectedIndices() {
        return Set.copyOf(selected);
    }

    /** 清空选中。 */
    public void clearSelection() {
        selected.clear();
        refreshSelection();
        fireChange();
    }

    /** 只刷新选中态。 */
    public void refreshSelection() {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setSelected(selected.contains(i));
        }
    }

    /** 第 index 张手牌节点（飞牌动画 / 定位用）。 */
    public Node getCardNode(int index) {
        return index >= 0 && index < cards.size() ? cards.get(index) : null;
    }

    // ============================================================= 内部

    private void rebuildNodes() {
        cardRow.getChildren().clear();
        cards.clear();
        for (int i = 0; i < current.size(); i++) {
            final int index = i;
            Card card = current.get(i);
            LiarCardBase node = card == null ? new LiarCardBack() : new LiarCardFace(card);
            node.setInteractive(interactive);
            node.setOnToggle(() -> toggle(index));
            cards.add(node);
            cardRow.getChildren().add(node);
        }
        selected.removeIf(i -> i >= cards.size());
        refreshSelection();
    }

    private void toggle(int index) {
        if (!interactive || index < 0 || index >= cards.size()) {
            return;
        }
        if (selected.contains(index)) {
            selected.remove(index);
        } else if (selected.size() < maxSelect) {
            selected.add(index);
        } else {
            return;
        }
        refreshSelection();
        fireChange();
    }

    private void fireChange() {
        if (onSelectionChange != null) {
            onSelectionChange.accept(Set.copyOf(selected));
        }
    }

    // ============================================================= 单张手牌

    /**
     * 单张手牌节点基类：统一「选中金边 + 上浮 / hover / 点击回调」行为。
     *
     * <p>子类只负责挂载各自的内容与样式类（{@link #hoverClass()} / {@link #selectedClass()}），
     * 不新增任何规则字段。
     */
    public abstract static class LiarCardBase extends StackPane {

        /** 上浮动画时长。 */
        private static final Duration LIFT_TIME = Duration.millis(140);
        /** 静止时上浮量（轻微抬升，避免贴底）。 */
        private static final double HOVER_LIFT = 5;

        private boolean selected;
        private boolean interactive;
        private Runnable onToggle;

        LiarCardBase() {
            setMinSize(CARD_W, CARD_H);
            setPrefSize(CARD_W, CARD_H);
            setMaxSize(CARD_W, CARD_H);
            setPickOnBounds(true);

            setOnMouseEntered(e -> {
                if (interactive && !selected) {
                    if (!getStyleClass().contains(hoverClass())) {
                        getStyleClass().add(hoverClass());
                    }
                    animateLift(-HOVER_LIFT);
                }
            });
            setOnMouseExited(e -> {
                getStyleClass().remove(hoverClass());
                if (!selected) {
                    animateLift(0);
                }
            });
            setOnMouseClicked(e -> {
                if (interactive && onToggle != null) {
                    onToggle.run();
                }
            });
        }

        /** hover 样式类名。 */
        protected abstract String hoverClass();

        /** 选中样式类名。 */
        protected abstract String selectedClass();

        /** 是否选中。 */
        public boolean isSelected() {
            return selected;
        }

        /** 是否响应点击。 */
        public void setInteractive(boolean value) {
            this.interactive = value;
            if (!value) {
                getStyleClass().remove(hoverClass());
            }
            setOpacity(value ? 1.0 : 0.72);
        }

        /** 点击回调。 */
        public void setOnToggle(Runnable callback) {
            this.onToggle = callback;
        }

        /** 选中视觉：金色边框 + 上浮 12px。 */
        public void setSelected(boolean value) {
            if (selected == value) {
                return;
            }
            selected = value;
            getStyleClass().remove(selectedClass());
            if (selected) {
                getStyleClass().add(selectedClass());
                getStyleClass().remove(hoverClass());
            }
            animateLift(selected ? -SELECT_LIFT : 0);
        }

        private void animateLift(double targetY) {
            if (!GameAnimationService.getInstance().isEnabled()) {
                setTranslateY(targetY);
                return;
            }
            Timeline lift = new Timeline(new KeyFrame(LIFT_TIME,
                    new KeyValue(translateYProperty(), targetY, Interpolator.EASE_OUT)));
            lift.play();
        }
    }

    /**
     * 牌背：无牌面数据时的占位（酒馆暗纹卡背 + {@code 🂠}），不含任何牌值信息。
     */
    public static final class LiarCardBack extends LiarCardBase {

        public LiarCardBack() {
            getStyleClass().add("liar-card-back");

            Label glyph = new Label("🂠");
            glyph.getStyleClass().add("liar-card-back-glyph");
            glyph.setMouseTransparent(true);

            Label mark = new Label("?");
            mark.getStyleClass().add("liar-card-back-mark");
            mark.setMouseTransparent(true);
            StackPane.setAlignment(mark, Pos.BOTTOM_CENTER);

            getChildren().addAll(glyph, mark);
        }

        @Override
        protected String hoverClass() {
            return "liar-card-back-hover";
        }

        @Override
        protected String selectedClass() {
            return "liar-card-back-selected";
        }
    }

    /**
     * 牌面：本地玩家自己的手牌，用与跑得快 / 联机桌同一套渲染（{@link CsuCardBridge#faceImage}）。
     */
    public static final class LiarCardFace extends LiarCardBase {

        private final Card card;
        private final ImageView face;

        public LiarCardFace(Card card) {
            this.card = card;
            getStyleClass().add("liar-card-face");

            face = new ImageView(CsuCardBridge.faceImage(card));
            face.setFitWidth(CARD_W);
            face.setFitHeight(CARD_H);
            face.setSmooth(true);
            face.setMouseTransparent(true);
            getChildren().add(face);
        }

        /** 该节点承载的牌。 */
        public Card card() {
            return card;
        }

        /** 牌面节点（飞牌动画需要）。 */
        public ImageView getFaceNode() {
            return face;
        }

        @Override
        protected String hoverClass() {
            return "liar-card-face-hover";
        }

        @Override
        protected String selectedClass() {
            return "liar-card-face-selected";
        }
    }
}
