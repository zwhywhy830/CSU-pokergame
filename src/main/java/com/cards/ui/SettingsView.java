package com.cards.ui;

import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.settings.GameSettings;
import com.csu.pokergame.settings.SettingsService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * 设置中心页面（阶段 20）：中南棋牌室风格（深色玻璃卡片 + 圆角 + 金色强调）的完整设置页。
 *
 * <p>内容分区：
 * <ul>
 *   <li><b>音频设置</b>：总音量滑块（0 ~ 100）+ 音乐开/关 + 音效开/关；</li>
 *   <li><b>游戏体验</b>：出牌动画 / AI 提示（操作提示）开关；</li>
 *   <li><b>主题</b>：深色主题 / 默认主题；</li>
 *   <li><b>账号</b>：当前账号 + 切换账号 + 退出登录；</li>
 *   <li><b>数据</b>：恢复默认设置。</li>
 * </ul>
 *
 * <p><b>数据来源（禁止复制数据）：</b>所有读数都实时向 {@link SettingsService} 要，
 * 本页不缓存任何偏好值。任何一处改动都由服务立即落盘 {@code data/settings.json}
 * 并通过 {@link SettingsService#addChangeListener} 回调本页 {@link #refresh()}，
 * 所以界面永远和服务一致，无需重启。
 *
 * <p><b>账号隔离：</b>设置属于"客户端偏好"而非玩家存档——本页读的是 {@link AccountService}
 * 的当前账号名（只读展示），从不把设置写进 {@code player.json}，切换账号也不会影响设置。
 */
public class SettingsView extends StackPane {

    private final SettingsService settingsService;
    private final AccountService accountService;
    private final Runnable onSwitchAccount;
    private final Runnable onLogout;

    // ---------------- 音频 ----------------
    private final Slider volumeSlider = new Slider(GameSettings.MIN_VOLUME, GameSettings.MAX_VOLUME,
            GameSettings.DEFAULT_VOLUME);
    private final Label volumeValue = new Label();
    private final Button musicButton = new Button();
    private final Button soundButton = new Button();

    // ---------------- 游戏体验 ----------------
    private final ToggleButton animationToggle = pillToggle();
    private final ToggleButton aiHintToggle = pillToggle();

    // ---------------- 主题 ----------------
    private final Button darkThemeButton = new Button("深色主题");
    private final Button defaultThemeButton = new Button("默认主题");

    // ---------------- 账号 ----------------
    private final Label accountValue = new Label();

    /** 刷新期间置真：避免"回填控件值"反过来触发写设置（防回环）。 */
    private boolean syncing = false;

    /**
     * @param settingsService 设置服务（单例，设置读写的唯一入口）
     * @param accountService  账号服务（只读当前账号名用于展示）
     * @param onSwitchAccount 点击「切换账号」的动作
     * @param onLogout        点击「退出登录」的动作
     * @param onBack          点击「返回主页」的动作
     */
    public SettingsView(SettingsService settingsService, AccountService accountService,
                        Runnable onSwitchAccount, Runnable onLogout, Runnable onBack) {
        this.settingsService = settingsService;
        this.accountService = accountService;
        this.onSwitchAccount = onSwitchAccount;
        this.onLogout = onLogout;

        getStyleClass().add("settings-view");

        // ---------------- 背景：与首页 / 个人中心同源的山水美术层 + 粒子 ----------------
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        addCornerSuit("♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT);
        addCornerSuit("♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT);
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                bgLayers.particles().play();
            } else {
                bgLayers.particles().stop();
            }
        });

        // ---------------- 中央内容 ----------------
        Label title = new Label("设置中心");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("音量、体验、主题与账号");
        sub.getStyleClass().add("game-choice-sub");

        VBox center = new VBox(16,
                title, sub,
                buildAudioCard(),
                buildExperienceCard(),
                buildThemeCard(),
                buildAccountCard(),
                buildDataCard());
        center.getStyleClass().add("settings-center");
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setAlignment(center, Pos.CENTER);

        // ---------------- 左上角返回 ----------------
        Button back = new Button("← 返回主页");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("player-scroll");
        getChildren().addAll(scroll, back);

        // 设置变化 → 立即回填界面（免重启实时同步）
        settingsService.addChangeListener(s -> {
            // 阶段 21：设置变化时同步音频音量 / 开关（免重启实时生效）
            AudioService.getInstance().refreshSettings();
            refresh();
        });
        refresh();
    }

    // ============================================================= 分区

    /** 音频设置：总音量滑块 + 音乐 / 音效开关按钮。 */
    private VBox buildAudioCard() {
        volumeSlider.getStyleClass().add("settings-slider");
        HBox.setHgrow(volumeSlider, Priority.ALWAYS);
        volumeSlider.setMaxWidth(Double.MAX_VALUE);
        volumeValue.getStyleClass().add("value-chip");
        volumeSlider.valueProperty().addListener((o, a, b) -> {
            volumeValue.setText(Math.round(b.doubleValue()) + "%");
            // 阶段 21：音量实时作用于 AudioService（音效 + 背景音乐），无需重启
            AudioService.getInstance().setVolume(b.doubleValue() / 100.0);
            if (!syncing) {
                settingsService.setVolume((int) Math.round(b.doubleValue()));
            }
        });
        HBox volumeRow = settingsRow("总音量", volumeSlider, volumeValue);

        musicButton.getStyleClass().add("settings-pill");
        musicButton.setOnAction(e -> settingsService.toggleMusic());
        soundButton.getStyleClass().add("settings-pill");
        soundButton.setOnAction(e -> settingsService.toggleSound());

        HBox musicRow = settingsRow("音乐", musicButton);
        HBox soundRow = settingsRow("音效", soundButton);

        // 阶段 21：音效测试按钮，点击立即播放 BUTTON_CLICK 验证音效链路
        Button testSoundBtn = new Button("🔊 测试按钮音效");
        testSoundBtn.getStyleClass().add("settings-pill");
        testSoundBtn.setOnAction(e -> AudioService.getInstance().playEffect(SoundEffect.BUTTON_CLICK));
        HBox testRow = settingsRow("音效测试", testSoundBtn);

        return card("🎵 音频设置", volumeRow, musicRow, soundRow, testRow);
    }

    /** 游戏体验：出牌动画 / AI 提示（操作提示）开关。 */
    private VBox buildExperienceCard() {
        animationToggle.setOnAction(e -> settingsService.toggleAnimation());
        aiHintToggle.setOnAction(e -> settingsService.toggleAiHint());
        return card("🎮 游戏体验",
                settingsRow("出牌动画", animationToggle),
                settingsRow("AI 提示 · 操作提示", aiHintToggle));
    }

    /** 主题：深色主题 / 默认主题。 */
    private VBox buildThemeCard() {
        darkThemeButton.getStyleClass().add("settings-pill");
        defaultThemeButton.getStyleClass().add("settings-pill");
        darkThemeButton.setOnAction(e -> settingsService.changeTheme(GameSettings.THEME_DARK));
        defaultThemeButton.setOnAction(e -> settingsService.changeTheme(GameSettings.THEME_DEFAULT));
        HBox row = new HBox(10, darkThemeButton, defaultThemeButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return card("🎨 界面主题", row);
    }

    /** 账号区域：当前账号 + 切换账号 / 退出登录。 */
    private VBox buildAccountCard() {
        accountValue.getStyleClass().add("settings-account-value");

        Button switchBtn = new Button("切换账号");
        switchBtn.getStyleClass().add("settings-pill");
        switchBtn.setOnAction(e -> {
            if (onSwitchAccount != null) {
                onSwitchAccount.run();
            }
        });

        Button logoutBtn = new Button("退出登录");
        logoutBtn.getStyleClass().addAll("settings-pill", "settings-pill-danger");
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        HBox accountRow = settingsRow("当前账号", accountValue);
        HBox actionRow = new HBox(10, switchBtn, logoutBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        return card("👤 账号", accountRow, actionRow);
    }

    /** 数据：恢复默认设置。 */
    private VBox buildDataCard() {
        Button resetBtn = new Button("恢复默认设置");
        resetBtn.getStyleClass().add("settings-pill");
        resetBtn.setOnAction(e -> settingsService.reset());
        Label note = new Label("将音量、开关与主题全部恢复为出厂默认，并立即写入 settings.json。");
        note.getStyleClass().add("settings-card-note");
        return card("🧹 数据", resetBtn, note);
    }

    // ============================================================= 刷新

    /**
     * 从 {@link SettingsService} 实时读取当前设置并回填到各控件。
     * 进入页面时调用一次；此后设置变化会通过监听自动回调本方法。
     */
    public void refresh() {
        GameSettings settings = settingsService.getSettings();

        syncing = true;
        try {
            volumeSlider.setValue(settings.getMasterVolume());
            volumeValue.setText(settings.getVolumeText());

            // 开关按钮：文字用"开 / 关"，颜色交给 CSS 的 :selected 态
            musicButton.setText("音乐 · " + settings.getMusicText());
            soundButton.setText("音效 · " + settings.getSoundText());

            applyPill(animationToggle, settings.isAnimationEnabled());
            applyPill(aiHintToggle, settings.isAiHintEnabled());

            markActive(darkThemeButton, settings.isDarkTheme());
            markActive(defaultThemeButton, !settings.isDarkTheme());

            String username = accountService == null ? null : accountService.getCurrentUsername();
            accountValue.setText(username == null ? "未登录" : username);
        } finally {
            syncing = false;
        }

        // 阶段 22：为设置页所有按钮 / 开关安装点击缩放反馈（幂等）
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    /** 把开关的选中态与文字同步到设置值。 */
    private static void applyPill(ToggleButton toggle, boolean on) {
        toggle.setSelected(on);
        toggle.setText(on ? "开" : "关");
    }

    /** 给当前生效的主题按钮加高亮样式类。 */
    private static void markActive(Button button, boolean active) {
        if (active) {
            if (!button.getStyleClass().contains("settings-pill-active")) {
                button.getStyleClass().add("settings-pill-active");
            }
        } else {
            button.getStyleClass().remove("settings-pill-active");
        }
    }

    // ============================================================= 构件

    /** 一张设置卡片：标题 + 若干行。 */
    private static VBox card(String heading, Node... rows) {
        Label head = new Label(heading);
        head.getStyleClass().add("settings-title");
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-card");
        card.getChildren().add(head);
        card.getChildren().addAll(rows);
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    /** 一行设置：左侧标签 + 弹性空隙 + 右侧控件（可附带值标签）。 */
    private static HBox settingsRow(String label, Node control, Node... trailing) {
        Label key = new Label(label);
        key.getStyleClass().add("settings-row-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, key, spacer, control);
        row.getChildren().addAll(trailing);
        row.getStyleClass().add("settings-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** 一个胶囊开关控件（"开 / 关"）。 */
    private static ToggleButton pillToggle() {
        ToggleButton toggle = new ToggleButton("开");
        toggle.getStyleClass().add("settings-toggle");
        toggle.setFocusTraversable(false);
        toggle.setTooltip(new Tooltip("开 / 关"));
        // 用户点击后由 onAction 写入设置；取消其自身 toggle 的中间态由 refresh 统一校正
        return toggle;
    }

    /** 角落花色水印（与个人中心一致的装饰）。 */
    private void addCornerSuit(String suit, Color color, Pos pos) {
        Text glyph = new Text(suit);
        glyph.setFill(color);
        glyph.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 120));
        glyph.setMouseTransparent(true);
        StackPane.setAlignment(glyph, pos);
        StackPane.setMargin(glyph, new Insets(10, 18, 10, 18));
        getChildren().add(glyph);
    }
}
