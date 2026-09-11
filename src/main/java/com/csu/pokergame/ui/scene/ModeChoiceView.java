package com.csu.pokergame.ui.scene;

import com.cards.ui.LobbyHelper;
import com.cards.ui.ParticleField;
import com.cards.ui.animation.SceneTransition;
import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.ui.AppShell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 玩法模式选择页：针对所选游戏提供「本地人机 / 局域网联机」两种模式。
 *
 * <p>从 DeckApp.buildModeChoiceScene 改造而来：
 * <ul>
 *   <li>继承 StackPane 而非构造 Scene（由 AppShell 统一管理路由与样式表）；</li>
 *   <li>背景层 + 粒子层由 {@link BackgroundManager#createLobbyBackground()} 构建；</li>
 *   <li>四角花色水印、模式卡片、点击音效统一走 {@link LobbyHelper}；</li>
 *   <li>CSS 由 AppShell 全局注入，本类不再加载样式表。</li>
 * </ul>
 */
public final class ModeChoiceView extends StackPane {

    public ModeChoiceView(AppShell shell) {
        // ---------------- 背景：大厅同源美术层 + 粒子 ----------------
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();

        // 四角花色水印：低透明度 + 微模糊，作暗纹底（与“选择游戏”页一致）
        LobbyHelper.addCornerSuit(this, "♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT, true);
        LobbyHelper.addCornerSuit(this, "♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT, true);
        LobbyHelper.addCornerSuit(this, "♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT, true);

        // 粒子动画仅在本页显示时运行，切走后暂停以节省资源
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                particles.play();
            } else {
                particles.stop();
            }
        });

        // ---------------- 顶部徽标：当前所选游戏名 ----------------
        Label badge = new Label(gameDisplayName(GameChoiceView.SELECTED_GAME));
        badge.getStyleClass().add("game-choice-title");
        badge.setMinHeight(76);
        Label sub = new Label("请选择玩法模式");
        sub.getStyleClass().add("game-choice-sub");

        // ---------------- 两张模式卡片 ----------------
        Button vsCpu = LobbyHelper.gameOption("🤖", "本地人机",
                "与电脑 AI 同台对战\n无需联网，随时开局",
                "单人 · 离线", false, () -> {
                    LobbyHelper.clickSound();
                    if ("PDK".equals(GameChoiceView.SELECTED_GAME)) {
                        shell.transitionTo("pdk", SceneTransition.Type.ENTER_GAME);
                    } else {
                        shell.transitionTo("liar", SceneTransition.Type.ENTER_GAME);
                    }
                });
        Button lanPlay = LobbyHelper.gameOption("🌐", "局域网联机",
                "创建或加入局域网房间\n与身边好友同台竞技",
                "多人 · 联机", false, () -> {
                    LobbyHelper.clickSound();
                    shell.transitionTo("lan-lobby", SceneTransition.Type.ZOOM);
                });

        HBox options = new HBox(28, vsCpu, lanPlay);
        options.setAlignment(Pos.CENTER);
        options.setPadding(new Insets(0, 48, 0, 48));
        HBox.setHgrow(vsCpu, Priority.ALWAYS);
        HBox.setHgrow(lanPlay, Priority.ALWAYS);

        // ---------------- 返回按钮 ----------------
        Button back = new Button("← 返回选择游戏");
        back.getStyleClass().add("game-back");
        back.setOnAction(e -> {
            LobbyHelper.clickSound();
            shell.transitionTo("game-choice", SceneTransition.Type.FADE);
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(22, 0, 0, 24));

        // ---------------- 中央内容 ----------------
        VBox center = new VBox(30, badge, sub, options);
        center.setAlignment(Pos.CENTER);
        StackPane.setAlignment(center, Pos.CENTER);

        getChildren().addAll(center, back);

        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    /** 把 GameChoiceView.SELECTED_GAME 映射为顶部徽标显示的游戏名。 */
    private static String gameDisplayName(String selectedGame) {
        if ("PDK".equals(selectedGame)) {
            return "♠ 湖南跑得快";
        }
        return "🃏 骗子酒馆";
    }
}
