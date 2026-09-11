package com.cards.ui.liar;

import com.cards.ui.effect.GameAnimationService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 阶段 24：骗子酒馆「声明信息面板」。
 *
 * <p>三段信息：
 * <pre>
 *   当前玩家：玩家A
 *   声明：三张 Q          ← 大字，带淡入动画
 *   可信度：未知
 * </pre>
 *
 * <p>同一组件既用于顶部信息栏，也用于桌面中央的「当前声明卡」，
 * 由 {@code compact} 构造参数控制排版；{@link #playClaimIn()} 播放文字淡入。
 *
 * <p>纯展示：文案由调用方从 {@code LiarSnapshot} 或界面侧只读值生成，不新增规则字段。
 */
public final class LiarClaimPanel extends VBox {

    private final Label declarerLabel;
    private final Label claimLabel;
    private final Label credibilityLabel;

    private final boolean compact;

    public LiarClaimPanel() {
        this(false);
    }

    /**
     * @param compact true 时缩小字号与内边距（用于顶部信息栏）
     */
    public LiarClaimPanel(boolean compact) {
        super(3);
        this.compact = compact;
        getStyleClass().add("liar-claim");
        if (compact) {
            getStyleClass().add("liar-claim-compact");
        }
        setAlignment(Pos.CENTER);

        declarerLabel = new Label("当前玩家：—");
        declarerLabel.getStyleClass().add("liar-claim-declarer");

        claimLabel = new Label("等待首位宣告者");
        claimLabel.getStyleClass().add("liar-claim-text");

        credibilityLabel = new Label("可信度：未知");
        credibilityLabel.getStyleClass().add("liar-claim-cred");

        getChildren().addAll(declarerLabel, claimLabel, credibilityLabel);
    }

    /** 横排子项工厂：把两个面板并成一排（顶部栏用）。 */
    public static HBox row(LiarClaimPanel... panels) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(panels);
        return box;
    }

    // ============================================================= 内容

    /** 当前宣告者昵称，传空隐藏该行。 */
    public void setDeclarer(String name) {
        String value = name == null ? "" : name.trim();
        declarerLabel.setText(value.isBlank() ? "当前玩家：—" : "当前玩家：" + value);
        boolean show = !value.isBlank();
        declarerLabel.setVisible(show);
        declarerLabel.setManaged(show);
    }

    /** 声明文案，例如 {@code "三张 Q"}。 */
    public void setClaim(String text) {
        claimLabel.setText(text == null || text.isBlank() ? "等待首位宣告者" : text);
    }

    /** 当前声明文案。 */
    public String getClaim() {
        return claimLabel.getText();
    }

    /** 可信度文案，例如 {@code "未知"} / {@code "偏高"} / {@code "偏低"}。 */
    public void setCredibility(String text) {
        String value = text == null || text.isBlank() ? "未知" : text.trim();
        credibilityLabel.setText("可信度：" + value);
        credibilityLabel.getStyleClass().removeAll(
                "liar-claim-cred-unknown", "liar-claim-cred-high", "liar-claim-cred-low");
        credibilityLabel.getStyleClass().add(switch (value) {
            case "偏高" -> "liar-claim-cred-high";
            case "偏低" -> "liar-claim-cred-low";
            default -> "liar-claim-cred-unknown";
        });
    }

    /** 未知可信度（默认态）。 */
    public void setCredibilityUnknown() {
        setCredibility("未知");
    }

    // ============================================================= 动画

    /** 声明文字淡入（+ 轻微放大）。动画开关关闭时直接置为终态。 */
    public void playClaimIn() {
        if (!GameAnimationService.getInstance().isEnabled()) {
            claimLabel.setOpacity(1.0);
            claimLabel.setScaleX(1.0);
            claimLabel.setScaleY(1.0);
            return;
        }
        claimLabel.setOpacity(0.0);
        claimLabel.setScaleX(0.86);
        claimLabel.setScaleY(0.86);
        FadeTransition fade = new FadeTransition(Duration.millis(260), claimLabel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
        ScaleTransition pop = new ScaleTransition(Duration.millis(260), claimLabel);
        pop.setFromX(0.86);
        pop.setFromY(0.86);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    /** 声明被质疑：面板红色震动。动画关闭时只保留一次红边闪烁。 */
    public void playChallengeShake() {
        getStyleClass().add("liar-claim-challenged");
        if (!GameAnimationService.getInstance().isEnabled()) {
            Timeline off = new Timeline(new KeyFrame(Duration.millis(320),
                    e -> getStyleClass().remove("liar-claim-challenged")));
            off.play();
            return;
        }
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60), new KeyValue(translateXProperty(), -10)),
                new KeyFrame(Duration.millis(130), new KeyValue(translateXProperty(), 10)),
                new KeyFrame(Duration.millis(210), new KeyValue(translateXProperty(), -6)),
                new KeyFrame(Duration.millis(300), new KeyValue(translateXProperty(), 0)));
        shake.setOnFinished(e -> getStyleClass().remove("liar-claim-challenged"));
        shake.play();
    }

    /** 是否紧凑排版。 */
    public boolean isCompact() {
        return compact;
    }
}
