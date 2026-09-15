package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.ParticleField;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.ui.AppShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Text;

/**
 * 个人信息编辑页：从 {@code DeckApp.buildProfileScene} 迁移而来。
 *
 * <p>关键适配：
 * <ul>
 *   <li>继承 {@link StackPane}，不再自建 {@code Scene}——由 {@link AppShell}
 *       统一持有 Scene 与全局样式表，本视图只负责装配节点；</li>
 *   <li>共享方法（角花色水印、头像字形字体、按钮点击音效）改走 {@link LobbyHelper}；</li>
 *   <li>背景层栈改由 {@link BackgroundManager#createLobbyBackground()} 统一构建；</li>
 *   <li>玩家档案读写改为通过 {@link PlayerManager#getInstance()} 单例，避免持有引用；</li>
 *   <li>导航改为 {@code shell.navigate("game-choice")}，由外壳负责路由与转场。</li>
 * </ul>
 *
 * <p>页面布局：大厅背景 + 四角花色水印 → 标题 / 副标题 →
 * 当前预览（大号头像 + 昵称 + 状态） → 头像选择行 → 昵称输入框 →
 * 状态选择行 → 底部「取消 / 保存并返回」按钮 → 左上角返回按钮。
 */
public final class ProfileEditView extends StackPane {

    /** 在线状态可选项（不属于档案字段，仅保留在内存）。 */
    private static final String[] STAT_CHOICES = {"在线 · 准备开局", "游戏中", "勿扰", "离线"};

    /** 头像候选字形（象棋符号 + 小丑牌 emoji）。 */
    private static final String[] AVATAR_CHOICES = {"♛", "♚", "♔", "♕", "♝", "♞", "♜", "♟", "🃏"};

    private final AppShell shell;

    public ProfileEditView(AppShell shell) {
        this.shell = shell;
        buildLayout();
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    private void buildLayout() {
        // ===== 当前档案：读昵称 / 头像作为表单初始值（等级等其余字段随 profile 一并读入） =====
        PlayerProfile profile = PlayerManager.getInstance().getProfile();
        String currentNick = profile.getName();
        String currentAvatar = profile.getAvatar();

        // 状态默认取第一项（不在档案里持久化）
        String currentStat = STAT_CHOICES[0];

        // ===== 背景层与粒子层（统一由 BackgroundManager 构建） =====
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();

        // 四角花色水印
        LobbyHelper.addCornerSuit(this, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        LobbyHelper.addCornerSuit(this, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        // ===== 标题 / 副标题 =====
        Label title = new Label("个人信息");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("自定义你的头像与昵称");
        sub.getStyleClass().add("game-choice-sub");

        // ===== 当前预览：大号头像 + 昵称预览 + 状态预览 =====
        StackPane previewBadge = new StackPane();
        previewBadge.getStyleClass().addAll("user-avatar", "user-avatar-lg");
        Text previewGlyph = new Text(currentAvatar);
        // 按字形码点选择字体族（象棋符号 / emoji），避免缺字豆腐块
        previewGlyph.setFont(LobbyHelper.avatarGlyphFont(46, currentAvatar));
        previewGlyph.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff3c4")),
                new Stop(0.55, Color.web("#e8c25e")),
                new Stop(1, Color.web("#b07f1e"))));
        previewBadge.getChildren().add(previewGlyph);

        Label previewNick = new Label(currentNick);
        previewNick.getStyleClass().add("profile-preview-nick");
        Label previewStat = new Label(currentStat);
        previewStat.getStyleClass().add("user-stat");
        VBox previewText = new VBox(4, previewNick, previewStat);
        previewText.setAlignment(Pos.CENTER_LEFT);

        HBox preview = new HBox(18, previewBadge, previewText);
        preview.getStyleClass().add("profile-preview");
        preview.setAlignment(Pos.CENTER_LEFT);

        // ===== 头像选择：一排候选字符 =====
        Label avatarHead = new Label("选择头像");
        avatarHead.getStyleClass().add("settings-section");
        ToggleGroup avatarGroup = new ToggleGroup();
        HBox avatarRow = new HBox(12);
        avatarRow.getStyleClass().add("profile-avatar-row");
        for (String g : AVATAR_CHOICES) {
            // 用带显式字体的 Text 作为图标，绕开按钮文字在 CSS 字体回退链下的缺字问题
            Text glyphNode = new Text(g);
            glyphNode.setFont(LobbyHelper.avatarGlyphFont(24, g));
            glyphNode.setFill(Color.web("#fff3c4"));
            ToggleButton btn = new ToggleButton();
            btn.setGraphic(glyphNode);
            btn.getStyleClass().add("profile-avatar-btn");
            btn.setToggleGroup(avatarGroup);
            btn.setUserData(g);
            if (g.equals(currentAvatar)) {
                btn.setSelected(true);
            }
            btn.selectedProperty().addListener((o, a, sel) -> {
                if (sel) {
                    previewGlyph.setText(g);
                    previewGlyph.setFont(LobbyHelper.avatarGlyphFont(46, g));
                }
            });
            avatarRow.getChildren().add(btn);
        }
        // 如果当前头像不在候选里，默认选中第一个，并把预览同步为当前
        if (avatarGroup.getSelectedToggle() == null && !avatarRow.getChildren().isEmpty()) {
            ((ToggleButton) avatarRow.getChildren().get(0)).setSelected(true);
        }

        // ===== 昵称编辑 =====
        Label nickHead = new Label("昵称");
        nickHead.getStyleClass().add("settings-section");
        TextField nickField = new TextField(currentNick);
        nickField.getStyleClass().add("profile-nick-field");
        nickField.setPromptText("输入你的昵称");
        nickField.setPrefColumnCount(16);
        nickField.textProperty().addListener((o, a, t) ->
                previewNick.setText(t.isEmpty() ? "玩家" : t));

        // ===== 状态选择 =====
        Label statHead = new Label("在线状态");
        statHead.getStyleClass().add("settings-section");
        ToggleGroup statGroup = new ToggleGroup();
        HBox statRow = new HBox(10);
        for (String s : STAT_CHOICES) {
            ToggleButton btn = new ToggleButton(s);
            btn.getStyleClass().add("seg-toggle");
            btn.setToggleGroup(statGroup);
            btn.setUserData(s);
            if (s.equals(currentStat)) {
                btn.setSelected(true);
            }
            btn.selectedProperty().addListener((o, a, sel) -> {
                if (sel) {
                    previewStat.setText(s);
                }
            });
            statRow.getChildren().add(btn);
        }
        if (statGroup.getSelectedToggle() == null && !statRow.getChildren().isEmpty()) {
            ((ToggleButton) statRow.getChildren().get(0)).setSelected(true);
        }

        // ===== 底部按钮：取消 / 保存并返回 =====
        Button cancel = new Button("取消");
        cancel.getStyleClass().add("settings-link-btn");
        cancel.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("game-choice", SceneTransition.Type.FADE);
        });
        Button save = new Button("保存并返回");
        save.getStyleClass().addAll("menu-btn", "menu-btn-start", "profile-save-btn");
        save.setOnAction(e -> {
            LobbyHelper.clickSound();
            String nick = nickField.getText().trim();
            String finalNick = nick.isEmpty() ? "玩家" : nick;
            String finalAvatar = currentAvatar;
            ToggleButton avSel = (ToggleButton) avatarGroup.getSelectedToggle();
            if (avSel != null) {
                finalAvatar = (String) avSel.getUserData();
            }
            // 在线状态不属于档案字段，仅保留在内存，因此不写入档案
            // 昵称 / 头像写回玩家档案并落盘
            PlayerProfile prof = PlayerManager.getInstance().getProfile();
            prof.setName(finalNick);
            prof.setAvatar(finalAvatar);
            PlayerManager.getInstance().save();
            shell.transitionTo("game-choice", SceneTransition.Type.FADE);
        });
        HBox actionRow = new HBox(14, cancel, save);
        actionRow.getStyleClass().add("profile-action-row");
        actionRow.setAlignment(Pos.CENTER);

        // ===== 中央整体 =====
        VBox center = new VBox(22, title, sub, preview, avatarHead, avatarRow,
                nickHead, nickField, statHead, statRow, actionRow);
        center.getStyleClass().add("profile-center");
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        // ===== 左上角返回按钮：回到选择游戏页（不保存） =====
        Button back = new Button("← 返回选择游戏");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("game-choice", SceneTransition.Type.FADE);
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        getChildren().addAll(center, back);

        // 粒子动画随挂载 / 卸载播放 / 停止：原 DeckApp 监听 stage.sceneProperty()，
        // 改为 AppShell 单 Scene 模型后监听本视图的 sceneProperty 即可。
        sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) {
                particles.play();
            } else {
                particles.stop();
            }
        });
    }
}
