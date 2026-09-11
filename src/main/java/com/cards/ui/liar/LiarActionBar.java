package com.cards.ui.liar;

import com.cards.ui.effect.GameAnimationService;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * 阶段 24：骗子酒馆「诈唬操作」按钮条。
 *
 * <p>三个按钮：
 * <ul>
 *   <li>「宣告」——金色主按钮（{@code .liar-action-primary}），选择牌背后宣告；</li>
 *   <li>「继续」——灰色玻璃按钮（{@code .liar-action}）；</li>
 *   <li>「质疑」——红色危险按钮（{@code .liar-danger-btn}）。</li>
 * </ul>
 *
 * <p>点击反馈：统一走 {@link GameAnimationService#playButtonFeedback}（0.95 → 1.05 → 1，150ms），
 * 只改缩放，不影响 {@code setOnAction} 既有功能；动画总开关关闭时自动跳过。
 */
public final class LiarActionBar extends HBox {

    private final Button declareButton;
    private final Button continueButton;
    private final Button challengeButton;

    public LiarActionBar() {
        super(14);
        getStyleClass().add("liar-action-bar");
        setAlignment(Pos.CENTER);

        declareButton = new Button("宣告");
        declareButton.getStyleClass().addAll("liar-action-btn", "liar-action-primary");
        declareButton.setMinWidth(138);

        continueButton = new Button("继续");
        continueButton.getStyleClass().addAll("liar-action-btn", "liar-action");
        continueButton.setMinWidth(138);

        challengeButton = new Button("质疑");
        challengeButton.getStyleClass().addAll("liar-action-btn", "liar-danger-btn");
        challengeButton.setMinWidth(158);

        for (Button b : List.of(declareButton, continueButton, challengeButton)) {
            b.addEventHandler(ActionEvent.ACTION,
                    e -> GameAnimationService.getInstance().playButtonFeedback(b));
        }

        getChildren().addAll(declareButton, continueButton, challengeButton);
    }

    /** 「宣告」按钮。 */
    public Button getDeclareButton() {
        return declareButton;
    }

    /** 「继续」（相信）按钮。 */
    public Button getContinueButton() {
        return continueButton;
    }

    /** 「质疑」按钮。 */
    public Button getChallengeButton() {
        return challengeButton;
    }

    /** 启用 / 禁用 三个按钮。 */
    public void setAllEnabled(boolean enabled) {
        for (Button b : List.of(declareButton, continueButton, challengeButton)) {
            b.setDisable(!enabled);
        }
    }

    /** 单独控制「宣告」。 */
    public void setDeclareEnabled(boolean enabled) {
        declareButton.setDisable(!enabled);
    }

    /** 单独控制「继续」。 */
    public void setContinueEnabled(boolean enabled) {
        continueButton.setDisable(!enabled);
    }

    /** 单独控制「质疑」。 */
    public void setChallengeEnabled(boolean enabled) {
        challengeButton.setDisable(!enabled);
    }

    /** 在「质疑」按钮上播放一次红色震动（质疑结算时用）。 */
    public void playChallengeFeedback() {
        GameAnimationService.getInstance().playButtonFeedback(challengeButton);
    }
}
