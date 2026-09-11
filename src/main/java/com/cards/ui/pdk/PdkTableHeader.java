package com.cards.ui.pdk;

import com.cards.ui.component.CoinBar;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;

/**
 * 阶段 23：跑得快牌桌顶部比赛信息栏。
 *
 * <p>展示：模式（本地人机 / 局域网联机）、底分、本局局数、剩余牌数、金币栏。
 *
 * <p>数据全部由调用方从现有 {@code PdkSnapshot} 与界面侧只读值填充，
 * 不新增任何规则字段（本组件只是展示容器）。
 */
public final class PdkTableHeader extends HBox {

    private final Label modeLabel;
    private final Label baseLabel;
    private final Label roundLabel;
    private final HBox remainBox;
    private final Label auxLabel;
    private final CoinBar coinBar;

    public PdkTableHeader() {
        super(10);
        getStyleClass().add("pdk-info-panel");
        setAlignment(Pos.CENTER_LEFT);

        modeLabel = chip("模式：本地人机", "pdk-info-mode");
        baseLabel = chip("底分：50 金币", "pdk-info-base");
        roundLabel = chip("第 1 局", "pdk-info-round");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        remainBox = new HBox(8);
        remainBox.setAlignment(Pos.CENTER_LEFT);

        auxLabel = chip("", "pdk-info-aux");
        auxLabel.setVisible(false);
        auxLabel.setManaged(false);

        coinBar = new CoinBar();

        getChildren().addAll(modeLabel, baseLabel, roundLabel, spacer, remainBox, auxLabel, coinBar);
    }

    /** 模式文案（本地人机 / 局域网联机）。 */
    public void setMode(String mode) {
        modeLabel.setText("模式：" + (mode == null ? "本地人机" : mode));
    }

    /** 底分（金币）。 */
    public void setBaseScore(int baseScore) {
        baseLabel.setText("底分：" + Math.max(0, baseScore) + " 金币");
    }

    /** 本局局数（界面侧计数，非规则字段）。 */
    public void setRound(int round) {
        setRoundText("第 " + Math.max(1, round) + " 局");
    }

    /** 自定义局数文案；传空字符串隐藏该药丸（联机桌等无局数概念的场景）。 */
    public void setRoundText(String text) {
        String value = text == null ? "" : text.trim();
        roundLabel.setText(value);
        boolean show = !value.isBlank();
        roundLabel.setVisible(show);
        roundLabel.setManaged(show);
    }

    /** 剩余牌数药丸，例如 {@code ["玩家 13", "西家 8", "北家 10"]}。传空清空。 */
    public void setRemaining(List<String> entries) {
        remainBox.getChildren().clear();
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            remainBox.getChildren().add(chip(entry, "pdk-info-remain"));
        }
    }

    /** 辅助信息（关门 / 明牌），空文本隐藏。 */
    public void setAuxText(String text) {
        String value = text == null ? "" : text.trim();
        auxLabel.setText(value);
        boolean show = !value.isBlank();
        auxLabel.setVisible(show);
        auxLabel.setManaged(show);
    }

    /** 顶部金币栏（余额由 {@link CoinBar#setCoins(int)} 更新）。 */
    public CoinBar getCoinBar() {
        return coinBar;
    }

    private static Label chip(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("pdk-info-chip", styleClass);
        return label;
    }
}
