package com.cards.ui.pdk;

import com.cards.ui.effect.GameAnimationService;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * 阶段 23：跑得快操作按钮条。
 *
 * <p>两个按钮：
 * <ul>
 *   <li>「不出」——灰色玻璃次按钮（{@code .pdk-action-pass}）；</li>
 *   <li>「出牌」——金色主按钮（{@code .pdk-action-play}）。</li>
 * </ul>
 *
 * <p>按钮状态：不可用时半透明（{@code :disabled} → 透明度 50%），可用时发光。
 * 点击时接入 {@link GameAnimationService#playButtonFeedback} 的轻微缩放（0.95 → 1.05 → 1，150ms），
 * 只改缩放不影响 {@code setOnAction} 既有功能。
 */
public final class PdkActionBar extends HBox {

    private final Button passButton;
    private final Button playButton;

    public PdkActionBar() {
        super(26);
        getStyleClass().add("pdk-action-bar");
        setAlignment(Pos.CENTER);

        passButton = new Button("不出");
        passButton.getStyleClass().addAll("pdk-action-button", "pdk-action-pass");
        passButton.setMinWidth(132);

        playButton = new Button("出牌");
        playButton.getStyleClass().addAll("pdk-action-button", "pdk-action-play");
        playButton.setMinWidth(160);

        for (Button b : List.of(passButton, playButton)) {
            b.addEventHandler(ActionEvent.ACTION,
                    e -> GameAnimationService.getInstance().playButtonFeedback(b));
        }

        getChildren().addAll(passButton, playButton);
    }

    /** 「不出」按钮。 */
    public Button getPassButton() {
        return passButton;
    }

    /** 「出牌」按钮。 */
    public Button getPlayButton() {
        return playButton;
    }

    /** 设置「出牌」可用性（不可用时半透明）。 */
    public void setPlayEnabled(boolean enabled) {
        playButton.setDisable(!enabled);
    }

    /** 设置「不出」可用性（不可用时半透明）。 */
    public void setPassEnabled(boolean enabled) {
        passButton.setDisable(!enabled);
    }
}
