package com.cards.ui.component;

import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.interaction.InteractionType;
import com.csu.pokergame.interaction.QuickPhrase;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * 玩家互动按钮条（阶段 25）。
 *
 * <p>一个"💬 互动"主按钮；点击弹出 popover 浮层：
 * <ul>
 *   <li>第一行：🥚 砸鸡蛋 / 🌸 送花 / 🍅 砸番茄（投掷类，需要目标选择）；</li>
 *   <li>第二行：6 条混合风格快捷短语（点击直接发起广播）。</li>
 * </ul>
 *
 * <p>调用方通过 {@link #setOnInteractionChosen} / {@link #setOnPhraseChosen} 接收回调，
 * 自行调用 {@link com.csu.pokergame.interaction.InteractionService#enterTargetingMode}
 * 进入目标选择，或直接发起短语广播。
 */
public final class PlayerInteractionBar extends HBox {

    private final Button triggerButton;
    private final StackPane ownerRoot;
    private VBox popover;
    private boolean popoverVisible = false;

    private Consumer<InteractionType> onInteractionChosen;
    private Consumer<QuickPhrase> onPhraseChosen;

    public PlayerInteractionBar(StackPane ownerRoot) {
        super(14);
        getStyleClass().add("interaction-bar");
        setAlignment(Pos.CENTER);
        this.ownerRoot = ownerRoot;

        triggerButton = new Button("💬 互动");
        triggerButton.getStyleClass().addAll("interaction-trigger");
        triggerButton.setOnAction(e -> {
            GameAnimationService.getInstance().playButtonFeedback(triggerButton);
            togglePopover();
        });
        getChildren().add(triggerButton);
    }

    /** 「💬 互动」主按钮。 */
    public Button getTriggerButton() {
        return triggerButton;
    }

    /** 设置投掷类互动被选中的回调（调用方负责进入目标选择模式）。 */
    public void setOnInteractionChosen(Consumer<InteractionType> callback) {
        this.onInteractionChosen = callback;
    }

    /** 设置快捷短语被选中的回调。 */
    public void setOnPhraseChosen(Consumer<QuickPhrase> callback) {
        this.onPhraseChosen = callback;
    }

    /** 关闭 popover（如外部进入目标选择模式后调用）。 */
    public void hidePopover() {
        if (popoverVisible && popover != null && ownerRoot != null) {
            popoverVisible = false;
            ownerRoot.getChildren().remove(popover);
            popover = null;
        }
    }

    private void togglePopover() {
        if (popoverVisible) {
            hidePopover();
            return;
        }
        showPopover();
    }

    private void showPopover() {
        if (ownerRoot == null) {
            return;
        }
        popover = buildPopover();
        popover.setOpacity(0.0);
        StackPane.setAlignment(popover, Pos.BOTTOM_CENTER);
        StackPane.setMargin(popover, new Insets(0, 0, 110, 0));
        ownerRoot.getChildren().add(popover);

        FadeTransition in = new FadeTransition(Duration.millis(150), popover);
        in.setFromValue(0.0);
        in.setToValue(1.0);
        in.play();
        popoverVisible = true;
    }

    private VBox buildPopover() {
        Button egg = new Button("🥚 砸鸡蛋");
        egg.getStyleClass().addAll("interaction-btn", "interaction-btn-egg");
        Button flower = new Button("🌸 送花");
        flower.getStyleClass().addAll("interaction-btn", "interaction-btn-flower");
        Button tomato = new Button("🍅 砸番茄");
        tomato.getStyleClass().addAll("interaction-btn", "interaction-btn-tomato");

        for (Button b : List.of(egg, flower, tomato)) {
            b.addEventHandler(ActionEvent.ACTION,
                    e -> GameAnimationService.getInstance().playButtonFeedback(b));
        }
        egg.setOnAction(e -> {
            hidePopover();
            if (onInteractionChosen != null) {
                onInteractionChosen.accept(InteractionType.EGG);
            }
        });
        flower.setOnAction(e -> {
            hidePopover();
            if (onInteractionChosen != null) {
                onInteractionChosen.accept(InteractionType.FLOWER);
            }
        });
        tomato.setOnAction(e -> {
            hidePopover();
            if (onInteractionChosen != null) {
                onInteractionChosen.accept(InteractionType.TOMATO);
            }
        });

        HBox throwRow = new HBox(10, egg, flower, tomato);
        throwRow.getStyleClass().add("interaction-row");
        throwRow.setAlignment(Pos.CENTER);

        HBox phraseRow = new HBox(8);
        phraseRow.getStyleClass().add("interaction-row");
        phraseRow.setAlignment(Pos.CENTER);
        for (QuickPhrase phrase : QuickPhrase.values()) {
            Button pb = new Button(phrase.getText());
            pb.getStyleClass().addAll("interaction-btn", "interaction-phrase");
            pb.addEventHandler(ActionEvent.ACTION,
                    ev -> GameAnimationService.getInstance().playButtonFeedback(pb));
            pb.setOnAction(e -> {
                hidePopover();
                if (onPhraseChosen != null) {
                    onPhraseChosen.accept(phrase);
                }
            });
            phraseRow.getChildren().add(pb);
        }

        VBox box = new VBox(10, throwRow, phraseRow);
        box.getStyleClass().add("interaction-popover");
        box.setMouseTransparent(false);
        // 点击 popover 自身不冒泡到桌面
        box.setOnMousePressed(e -> e.consume());
        return box;
    }
}
