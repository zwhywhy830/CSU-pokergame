package com.cards.ui;

import com.cards.ui.background.BackgroundManager;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.player.AccountService;
import com.csu.pokergame.player.PlayerAccount;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 登录界面（阶段 16）：程序启动的第一屏。
 *
 * <p>布局：
 * <pre>
 *              中南棋牌室
 *          登录后进入你自己的存档
 *
 *        账号 [                     ]
 *        密码 [                     ]
 *        [   登 录   ]  [ 注册账号 ]
 * </pre>
 *
 * <p>行为：
 * <ul>
 *   <li>「登录」→ {@link AccountService#login(String, String)}；成功后回调 {@code onLoginSuccess}
 *       （由 {@code DeckApp} 负责载入玩家数据并切到首页），失败时在卡片内提示"账号或密码错误"；</li>
 *   <li>「注册账号」→ 弹出注册窗口（账号 / 密码 / 确认密码 / 注册），校验通过后调用
 *       {@link AccountService#register(String, String)}，并<b>自动登录</b>；</li>
 *   <li>回车等价于点「登录」。</li>
 * </ul>
 *
 * <p>本类只做界面与输入校验，账号数据（{@code accounts.json}）与存档切换全部由
 * {@link AccountService} 负责，界面层不会出现第二份账号数据。
 */
public final class LoginView extends StackPane {

    private final AccountService accounts;
    /** 登录成功后的动作（由 DeckApp 注入：载入玩家档案 → 进入首页）。 */
    private final Runnable onLoginSuccess;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    /** 卡片内的即时反馈（登录失败原因），不弹窗，避免打断输入。 */
    private final Label feedback = new Label();
    /** 旧存档迁移提示（本机存在 default 账号时才显示）。 */
    private final Label hint = new Label();

    public LoginView(AccountService accounts, Runnable onLoginSuccess) {
        this.accounts = accounts;
        this.onLoginSuccess = onLoginSuccess;

        getStyleClass().add("login-view");

        // ---------------- 背景：与首页同源的山水美术层 + 粒子 ----------------
        BackgroundManager.Background bgLayers = BackgroundManager.createLobbyBackground();
        getChildren().add(bgLayers.root());
        ParticleField particles = bgLayers.particles();
        addCornerSuit("♠", Color.rgb(255, 255, 255, 0.05), Pos.TOP_LEFT);
        addCornerSuit("♥", Color.rgb(255, 170, 160, 0.05), Pos.TOP_RIGHT);
        addCornerSuit("♣", Color.rgb(255, 255, 255, 0.05), Pos.BOTTOM_LEFT);
        addCornerSuit("♦", Color.rgb(255, 170, 160, 0.05), Pos.BOTTOM_RIGHT);
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                particles.play();
                refreshHint();
            } else {
                particles.stop();
            }
        });

        // ---------------- 标题 ----------------
        Label title = new Label("中南棋牌室");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("登录后进入你自己的存档");
        sub.getStyleClass().add("game-choice-sub");

        // ---------------- 账号 / 密码 ----------------
        usernameField.getStyleClass().add("login-field");
        usernameField.setPromptText("请输入账号");
        usernameField.setPrefWidth(240);
        passwordField.getStyleClass().add("login-field");
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefWidth(240);

        // ---------------- 按钮 ----------------
        Button loginBtn = new Button("登 录");
        // 阶段 21：登录按钮点击音效（统一走 AudioService）
        loginBtn.addEventHandler(javafx.event.ActionEvent.ACTION, e ->
                com.csu.pokergame.audio.AudioService.getInstance()
                        .playEffect(com.csu.pokergame.audio.SoundEffect.BUTTON_CLICK));
        loginBtn.getStyleClass().addAll("menu-btn", "menu-btn-start", "login-btn-login");
        loginBtn.setOnAction(e -> doLogin());

        Button registerBtn = new Button("注册账号");
        registerBtn.getStyleClass().addAll("menu-btn", "login-btn-register");
        registerBtn.setOnAction(e -> showRegisterDialog());

        HBox buttons = new HBox(14, loginBtn, registerBtn);
        buttons.getStyleClass().add("login-actions");
        buttons.setAlignment(Pos.CENTER);

        feedback.getStyleClass().add("login-feedback");
        hide(feedback);
        hint.getStyleClass().add("login-hint");
        hide(hint);

        VBox card = new VBox(14, title, sub,
                fieldRow("账号", usernameField), fieldRow("密码", passwordField),
                buttons, feedback, hint);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(430);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(card, Pos.CENTER);
        getChildren().add(card);

        // 回车 = 登录
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doLogin();
            }
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        // 阶段 22：主按钮点击缩放反馈（统一走 GameAnimationService，通过事件过滤器实现，不影响功能）
        GameAnimationService.getInstance().installButtonFeedback(this);
    }

    // ============================================================= 外部接口

    /** 预填账号（「切换账号」返回登录页时使用，方便快速切回上一个账号）。 */
    public void setUsernamePrefill(String username) {
        usernameField.setText(username == null ? "" : username);
        passwordField.clear();
        hide(feedback);
        if (username != null && !username.isBlank()) {
            passwordField.requestFocus();
        } else {
            usernameField.requestFocus();
        }
    }

    // ============================================================= 登录 / 注册

    /** 登录：校验非空 → 交给 {@link AccountService#login}；成功即回调，失败在卡片内提示。 */
    private void doLogin() {
        String username = text(usernameField).trim();
        String password = text(passwordField);
        if (username.isEmpty()) {
            showError("请输入账号");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("请输入密码");
            passwordField.requestFocus();
            return;
        }
        if (accounts == null || !accounts.login(username, password)) {
            showError("账号或密码错误");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }
        hide(feedback);
        if (onLoginSuccess != null) {
            onLoginSuccess.run();
        }
    }

    /** 弹出注册窗口：账号 / 密码 / 确认密码 / 注册；注册成功后自动登录。 */
    private void showRegisterDialog() {
        Stage dialog = new Stage();
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("注册账号");

        Label title = new Label("注册新账号");
        title.getStyleClass().add("game-choice-title");
        Label sub = new Label("注册成功自动登录 · 初始金币 1000 · Lv.1");
        sub.getStyleClass().add("game-choice-sub");

        TextField newUser = new TextField();
        newUser.getStyleClass().add("login-field");
        newUser.setPromptText("2 ~ 16 位字母 / 数字 / 下划线");
        newUser.setPrefWidth(240);
        PasswordField newPass = new PasswordField();
        newPass.getStyleClass().add("login-field");
        newPass.setPromptText("请输入密码");
        newPass.setPrefWidth(240);
        PasswordField confirmPass = new PasswordField();
        confirmPass.getStyleClass().add("login-field");
        confirmPass.setPromptText("请再次输入密码");
        confirmPass.setPrefWidth(240);

        Label error = new Label();
        error.getStyleClass().add("login-feedback");
        hide(error);

        Button cancel = new Button("取消");
        cancel.getStyleClass().addAll("menu-btn", "login-btn-register");
        cancel.setOnAction(e -> dialog.close());

        Button confirm = new Button("注 册");
        confirm.getStyleClass().addAll("menu-btn", "menu-btn-start", "login-btn-login");

        Runnable doRegister = () -> {
            String u = text(newUser).trim();
            String p = text(newPass);
            String c = text(confirmPass);
            if (!AccountService.isValidUsername(u)) {
                showError(error, "账号需 2 ~ 16 位字母 / 数字 / 下划线");
                return;
            }
            if (!AccountService.isValidPassword(p)) {
                showError(error, "密码不能为空，且不超过 32 位");
                return;
            }
            if (!p.equals(c)) {
                showError(error, "两次输入的密码不一致");
                return;
            }
            if (accounts == null || accounts.hasAccount(u)) {
                showError(error, "该账号已存在，请换一个");
                return;
            }
            if (!accounts.register(u, p)) {
                showError(error, "注册失败：输入不合法或存档已存在");
                return;
            }
            dialog.close();
            // 注册成功 → 自动登录（复用同一条登录路径，保证"切换玩家文件"逻辑只有一处实现）
            usernameField.setText(u);
            passwordField.setText(p);
            doLogin();
        };
        confirm.setOnAction(e -> doRegister.run());
        confirmPass.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doRegister.run();
            }
        });

        HBox buttons = new HBox(14, cancel, confirm);
        buttons.getStyleClass().add("login-actions");
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(12, title, sub,
                fieldRow("账号", newUser), fieldRow("密码", newPass), fieldRow("确认密码", confirmPass),
                buttons, error);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(430);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-view");
        // 阶段 22：注册窗口按钮同样接入点击缩放反馈
        GameAnimationService.getInstance().installButtonFeedback(root);

        Scene scene = new Scene(root, 560, 480);
        var css = getClass().getResource("/com/csu/pokergame/ui/theme/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        dialog.setScene(scene);
        dialog.show();
        newUser.requestFocus();
    }

    // ============================================================= 提示

    private void showError(String message) {
        showError(feedback, message);
    }

    private static void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void hide(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    /** 旧存档迁移提示：本机存在 {@code default} 账号时展示，帮助老玩家直接登录原有进度。 */
    private void refreshHint() {
        if (accounts == null || !accounts.hasAccount(AccountService.LEGACY_ACCOUNT_USERNAME)) {
            hide(hint);
            return;
        }
        PlayerAccount account = accounts.getAccounts().stream()
                .filter(a -> AccountService.LEGACY_ACCOUNT_USERNAME.equals(a.getUsername()))
                .findFirst().orElse(null);
        String created = account == null || account.getCreateTime() <= 0
                ? ""
                : " · 建档 " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(account.getCreateTime()));
        hint.setText("旧存档已迁移为账号 " + AccountService.LEGACY_ACCOUNT_USERNAME
                + " / 密码 " + AccountService.LEGACY_ACCOUNT_PASSWORD + created);
        hint.setVisible(true);
        hint.setManaged(true);
    }

    // ============================================================= 小组件

    /** 一行输入：左侧固定宽度的标签 + 右侧输入框。 */
    private static VBox fieldRow(String label, TextInputControl input) {
        Label key = new Label(label);
        key.getStyleClass().add("login-field-key");
        key.setMinWidth(56);
        HBox row = new HBox(10, key, input);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(row);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static String text(TextInputControl input) {
        return input.getText() == null ? "" : input.getText();
    }

    /** 页面角落的花色暗纹（与首页 / 个人中心一致的装饰语言）。 */
    private void addCornerSuit(String glyph, Color color, Pos corner) {
        Label label = new Label(glyph);
        label.setTextFill(color);
        label.setFont(Font.font("Segoe UI Symbol", 150));
        label.setMouseTransparent(true);
        StackPane.setAlignment(label, corner);
        getChildren().add(label);
    }
}
