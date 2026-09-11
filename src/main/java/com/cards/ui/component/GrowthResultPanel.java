package com.cards.ui.component;

import com.cards.ui.effect.GameAnimationService;
import com.cards.ui.theme.Theme;
import com.csu.pokergame.player.PlayerGrowthService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * 对局结算成长反馈面板：嵌在结算卡片（{@link com.cards.ui.effect.WinCelebration}）副标题下方，
 * 集中展示本局的成长结果。
 *
 * <p>固定展示：
 * <ol>
 *   <li>本局胜负；</li>
 *   <li>金币变化（本局奖励）；</li>
 *   <li>获得经验；</li>
 *   <li>当前等级（等级徽章 + 称号 + 等级）；</li>
 *   <li>当前经验 / 升级需求。</li>
 * </ol>
 *
 * <p>升级时追加高亮块：{@code "升级成功 Lv.12 → Lv.13"} 与 {@code "获得金币奖励 +650"}。
 *
 * <p>本组件只做展示，不读写任何玩家数据；数值由调用方（{@code DeckApp}）从
 * {@link com.csu.pokergame.player.PlayerManager} / {@link com.csu.pokergame.player.CoinService} /
 * {@link PlayerGrowthService} 查得后传入。
 */
public final class GrowthResultPanel extends VBox {

    /** 金币变化数值标签（阶段 22：数字从 0 滚动到目标值）。 */
    private final Label goldValueLabel;
    /** 获得经验数值标签（阶段 22：数字从 0 滚动到目标值）。 */
    private final Label expValueLabel;

    /** 金币滚动前缀 / 后缀。 */
    private static final String GOLD_PREFIX = "＋";
    private static final String GOLD_SUFFIX = " 金币";
    /** 经验滚动前缀 / 后缀。 */
    private static final String EXP_PREFIX = "＋";
    private static final String EXP_SUFFIX = " 经验";

    /**
     * @param win       本局是否获胜
     * @param goldDelta 本局获得金币（正数）
     * @param expGain   本局获得经验（正数）
     * @param level     结算后的当前等级
     * @param levelTitle 结算后的等级称号（青铜 / 白银 / 黄金 / 大师）
     * @param levelBadge 结算后的等级徽章 emoji
     * @param exp       结算后的当前经验
     * @param expNeed   当前等级升到下一级所需经验
     * @param growth    升级结果（可为 null；{@code upgraded()} 为 true 时展示升级高亮块）
     */
    public GrowthResultPanel(boolean win,
                             int goldDelta,
                             int expGain,
                             int level,
                             String levelTitle,
                             String levelBadge,
                             int exp,
                             int expNeed,
                             PlayerGrowthService.LevelUpResult growth) {
        getStyleClass().add("growth-result");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);

        int safeGold = Math.max(0, goldDelta);
        int safeExpGain = Math.max(0, expGain);
        int safeLevel = Math.max(1, level);
        int safeNeed = Math.max(1, expNeed);
        int safeExp = Math.max(0, exp);

        // 1. 本局胜负（阶段 22：胜利 🏆 / 失败 💪 再接再厉）
        Label resultLabel = value(win ? "🏆 胜利" : "💪 再接再厉",
                win ? "growth-value-win" : "growth-value-lose");
        if (win) {
            resultLabel.getStyleClass().add("win-effect");
        }
        getChildren().add(row("本局胜负", resultLabel));
        // 2. 金币变化（阶段 22：数字从 0 滚动到目标值）
        goldValueLabel = value(GOLD_PREFIX + safeGold + GOLD_SUFFIX, "growth-value-gold");
        getChildren().add(row("金币变化", goldValueLabel));
        // 3. 获得经验（阶段 22：数字从 0 滚动到目标值）
        expValueLabel = value(EXP_PREFIX + safeExpGain + EXP_SUFFIX, "growth-value-exp");
        getChildren().add(row("获得经验", expValueLabel));
        // 4. 当前等级（徽章 + 称号 + 等级）
        getChildren().add(row("当前等级",
                levelValue(levelBadge, levelTitle, safeLevel)));
        // 5. 当前经验 / 升级需求
        getChildren().add(row("经验进度",
                value(safeExp + " / " + safeNeed, "growth-value-exp")));

        // 6. 升级高亮块（可选，阶段 22：✨ Level UP）
        if (growth != null && growth.upgraded()) {
            getChildren().add(upgradeBox(growth));
        }

        // 7. 数字递增动画（阶段 22：金币 / 经验从 0 滚动到目标值；关闭动画时直接显示终值）
        animateNumbers(safeGold, safeExpGain);
    }

    /**
     * 阶段 22：金币 / 经验数值从 0 滚动到目标值。
     *
     * <p>动画总开关关闭时直接显示终值，不做滚动。
     */
    public void animateNumbers(int goldTarget, int expTarget) {
        if (!GameAnimationService.getInstance().isEnabled()) {
            goldValueLabel.setText(GOLD_PREFIX + Math.max(0, goldTarget) + GOLD_SUFFIX);
            expValueLabel.setText(EXP_PREFIX + Math.max(0, expTarget) + EXP_SUFFIX);
            return;
        }
        rollNumber(goldValueLabel, GOLD_PREFIX, GOLD_SUFFIX, Math.max(0, goldTarget), 900);
        rollNumber(expValueLabel, EXP_PREFIX, EXP_SUFFIX, Math.max(0, expTarget), 1080);
    }

    /** 单个数值滚动：从 0 递增到 target，共 {@code steps} 帧。 */
    private static void rollNumber(Label label, String prefix, String suffix, int target, long delayMs) {
        final int steps = 22;
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(delayMs),
                e -> label.setText(prefix + 0 + suffix)));
        for (int i = 1; i <= steps; i++) {
            final int value = (int) Math.round(target * (i / (double) steps));
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(delayMs + i * 24L),
                    e -> label.setText(prefix + value + suffix)));
        }
        timeline.setOnFinished(e -> label.setText(prefix + target + suffix));
        timeline.play();
    }

    // ============================================================= 行构建

    /** 一行：左侧定宽标题 + 右侧值节点。 */
    private static HBox row(String key, Node value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("growth-key");
        keyLabel.setMinWidth(60);
        HBox line = new HBox(10, keyLabel, value);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    /** 普通值：文本标签 + 可选附加样式类。 */
    private static Label value(String text, String extraStyleClass) {
        Label label = new Label(text);
        label.getStyleClass().add("growth-value");
        if (extraStyleClass != null) {
            label.getStyleClass().add(extraStyleClass);
        }
        return label;
    }

    /** 当前等级值：徽章 emoji（可选）+ "称号 Lv.N"。 */
    private static HBox levelValue(String badge, String title, int level) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        if (badge != null && !badge.isBlank()) {
            Text badgeText = new Text(badge);
            // emoji 徽章单独用 Emoji 字体，避免与中文字体混排时出现豆腐块
            badgeText.setFont(Font.font("Segoe UI Emoji", FontWeight.NORMAL, 14));
            badgeText.setFill(Theme.GOLD_BRIGHT);
            box.getChildren().add(badgeText);
        }
        String titleText = title == null || title.isBlank() ? "" : title + " ";
        box.getChildren().add(value(titleText + "Lv." + level, "growth-value-level"));
        return box;
    }

    /** 升级高亮块：两行文案（升级成功 / 升级奖励）。 */
    private static VBox upgradeBox(PlayerGrowthService.LevelUpResult growth) {
        Label title = new Label("✨ Level UP  Lv." + growth.getOldLevel() + " → Lv." + growth.getNewLevel());
        title.getStyleClass().addAll("growth-value", "growth-upgrade-title", "level-up-effect");

        Label reward = new Label("获得金币奖励  +" + Math.max(0, growth.getUpgradeReward()));
        reward.getStyleClass().addAll("growth-value", "growth-upgrade-reward");

        VBox box = new VBox(2, title, reward);
        box.getStyleClass().add("growth-upgrade");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
