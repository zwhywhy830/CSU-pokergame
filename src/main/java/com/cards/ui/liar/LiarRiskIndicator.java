package com.cards.ui.liar;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 阶段 24：骗子酒馆「心理压力 / 怀疑度」指示器（纯展示，不参与规则判定）。
 *
 * <p>显示为方块进度条 + 百分比，例如 {@code ██████░░░░ 65%}。
 *
 * <p>数据来源：引擎快照没有该字段，由调用方（界面侧）按公开信息临时推算后传入，
 * 本组件不读取引擎、不新增任何规则字段。
 *
 * <p>样式见 app.css 的 {@code .liar-risk*} 段（暗红 → 金色渐变）。
 */
public final class LiarRiskIndicator extends VBox {

    /** 方块总数。 */
    private static final int BLOCKS = 10;

    private final Label[] blocks = new Label[BLOCKS];
    private final Label title = new Label("怀疑度");
    private final Label percent = new Label("0%");
    private final HBox bar = new HBox(2);

    /** 当前值 0.0 ~ 1.0。 */
    private double value;

    public LiarRiskIndicator() {
        super(3);
        getStyleClass().add("liar-risk");
        setAlignment(Pos.CENTER_LEFT);

        title.getStyleClass().add("liar-risk-title");
        percent.getStyleClass().add("liar-risk-percent");

        bar.getStyleClass().add("liar-risk-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < BLOCKS; i++) {
            Label b = new Label("█");
            b.getStyleClass().add("liar-risk-block");
            blocks[i] = b;
            bar.getChildren().add(b);
        }

        HBox head = new HBox(6, title, percent);
        head.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(head, bar);
        setValue(0);
    }

    /** 设置怀疑度（0.0 ~ 1.0，越界自动截断）。 */
    public void setValue(double v) {
        double clamped = Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
        this.value = clamped;
        int filled = (int) Math.round(clamped * BLOCKS);
        for (int i = 0; i < BLOCKS; i++) {
            Label b = blocks[i];
            b.getStyleClass().removeAll("liar-risk-block-on", "liar-risk-block-high");
            if (i < filled) {
                b.getStyleClass().add(clamped >= 0.7 ? "liar-risk-block-high" : "liar-risk-block-on");
            }
        }
        percent.setText(Math.round(clamped * 100) + "%");
        getStyleClass().removeAll("liar-risk-mid", "liar-risk-high");
        if (clamped >= 0.7) {
            getStyleClass().add("liar-risk-high");
        } else if (clamped >= 0.4) {
            getStyleClass().add("liar-risk-mid");
        }
    }

    /** 当前值（0.0 ~ 1.0）。 */
    public double getValue() {
        return value;
    }

    /** 文案前缀（如「怀疑度」/「压力」）。 */
    public void setTitleText(String text) {
        title.setText(text == null ? "怀疑度" : text);
    }

    /** 是否处于高危（≥ 70%）。 */
    public boolean isHigh() {
        return value >= 0.7;
    }
}
