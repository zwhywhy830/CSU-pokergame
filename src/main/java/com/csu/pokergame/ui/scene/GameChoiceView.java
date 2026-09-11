package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.component.AvatarView;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.player.PlayerManager;
import com.csu.pokergame.player.PlayerProfile;
import com.csu.pokergame.ui.AppShell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 选择游戏页：两张游戏入口卡片——湖南跑得快 / 骗子酒馆。
 *
 * <p>由 {@code DeckApp.buildGameChoiceScene} 改造而来：不再自建
 * {@link javafx.scene.Scene}，而是直接继承 {@link StackPane}，由
 * {@link AppShell} 统一装进单一 Scene；背景、四角花色、卡片、动画
 * 全部复用 {@link LobbyHelper} 与 {@link BackgroundManager}，CSS 由全局样式表负责。
 *
 * <p>所选游戏通过静态字段 {@link #SELECTED_GAME} 传递给下一页（玩法模式选择）。
 */
public final class GameChoiceView extends StackPane {

    /**
     * 本次选择的游戏：{@code "PDK"} 湖南跑得快 / {@code "LIAR"} 骗子酒馆。
     * 跳转到 mode-choice 前写入，玩法模式页读取后即可区分构建哪种游戏的模式卡。
     */
    public static String SELECTED_GAME;

    public GameChoiceView(AppShell shell) {
        // 背景层（山水 Canvas + 主题遮罩）与粒子层均由 BackgroundManager 统一构建
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());

        // 低强度动态粒子：星光缓慢浮动 + 纸牌碎片轻轻飘落（鼠标穿透，离开本页自动暂停）
        var particles = bgLayers.particles();

        // 四角花色改成淡淡的暗纹底（低透明度 + 微模糊），营造氛围但不抢焦点
        LobbyHelper.addCornerSuit(this, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        LobbyHelper.addCornerSuit(this, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        Label title = new Label("选择游戏");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("请选择要游玩的模式");
        sub.getStyleClass().add("game-choice-sub");

        Button runFast = LobbyHelper.gameOption("♠", "湖南跑得快",
                "三人 16 张经典玩法\n先出完手牌者获胜",
                "扑克 · 竞速出牌", true, () -> {
                    SELECTED_GAME = "PDK";
                    shell.transitionTo("mode-choice", SceneTransition.Type.ZOOM);
                });
        Button liarBar = LobbyHelper.gameOption("🃏", "骗子酒馆",
                "扑克与骰子模式\n谎言与质疑并存，活到最后即胜",
                "聚会 · 心理博弈", false, () -> {
                    SELECTED_GAME = "LIAR";
                    shell.transitionTo("mode-choice", SceneTransition.Type.ZOOM);
                });

        HBox options = new HBox(28, runFast, liarBar);
        options.setAlignment(Pos.CENTER);
        options.setPadding(new Insets(0, 48, 0, 48));
        // 两张卡片平分可用宽度（宽度上下限由 CSS min/pref/max-width 约束），窗口缩小时自动收窄不溢出
        HBox.setHgrow(runFast, Priority.ALWAYS);
        HBox.setHgrow(liarBar, Priority.ALWAYS);

        Button back = new Button("返回主页 →");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("home", SceneTransition.Type.FADE);
        });
        StackPane.setAlignment(back, Pos.TOP_RIGHT);
        StackPane.setMargin(back, new Insets(22, 24, 0, 0));

        // 左上角玩家卡片：复用 AvatarView（圆头像 + 昵称 + 等级 + 在线状态点）
        PlayerProfile profile = PlayerManager.getInstance().getProfile();
        String userAvatarGlyph = profile.getAvatar();
        String userNick = profile.getName();
        int userLevel = profile.getLevel();
        String userStat = "在线 · 准备开局";
        AvatarView choiceAvatar = new AvatarView(userAvatarGlyph, userNick, userStat, userLevel);
        choiceAvatar.getStyleClass().add("user-card");
        choiceAvatar.setAlignment(Pos.CENTER_LEFT);
        // 让 HBox 按内容计算尺寸，否则默认会撑满 StackPane，
        // .user-card 的半透明深绿背景就会铺满整个屏幕，造成“亮度下降”
        choiceAvatar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(choiceAvatar, Pos.TOP_LEFT);
        StackPane.setMargin(choiceAvatar, new Insets(22, 0, 0, 24));
        // 点击头像/昵称卡片进入个人信息编辑页
        choiceAvatar.setPickOnBounds(true);
        choiceAvatar.setCursor(Cursor.HAND);
        choiceAvatar.setOnMouseClicked(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("profile-edit", SceneTransition.Type.OPEN_PROFILE);
        });

        VBox center = new VBox(34, title, sub, options);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        getChildren().addAll(center, back, choiceAvatar);

        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源；
        // 进入本页时让头像/返回/标题/卡片错峰淡入上浮，承接加载页淡出，避免内容瞬间弹出
        final Node[] choiceIntro = { choiceAvatar, back, title, sub, options };
        sceneProperty().addListener((o, oldS, newS) -> {
            if (newS != null) {
                particles.play();
                LobbyHelper.playRiseIn(choiceIntro);
            } else if (oldS != null) {
                particles.stop();
            }
        });

        GameAnimationService.getInstance().installButtonFeedback(this);
    }
}
