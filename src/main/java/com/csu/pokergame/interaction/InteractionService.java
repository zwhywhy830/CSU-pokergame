package com.csu.pokergame.interaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import com.cards.ui.component.GameToast;
import com.cards.ui.effect.GameAnimationService;
import com.csu.pokergame.audio.AudioService;
import com.csu.pokergame.audio.SoundEffect;
import com.csu.pokergame.core.engine.PlayerId;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * 玩家互动服务（阶段 25）。
 *
 * <p>单例，负责把 {@link InteractionType} / {@link QuickPhrase} 的视觉与音效落到牌桌上：
 * <ul>
 *   <li>投掷类（鸡蛋 / 花 / 番茄）：emoji 从源座位头像飞向目标座位头像，落地播声音 + 抖动 + Toast；</li>
 *   <li>短语类：直接在源座位头像上方弹气泡 + 播 ping 音。</li>
 * </ul>
 *
 * <p>AI 自动互动：调用 {@link #maybeAiInteract}，约 15% 概率随机发起一次。
 *
 * <p>目标选择模式：调用 {@link #enterTargetingMode} 高亮可选座位，
 * 点击座位后回调目标 PlayerId，由调用方再调 {@link #fireInteraction}。
 *
 * <p>线程：所有 UI 操作经 {@code Platform.runLater} 包装，可在任意线程调用。
 */
public final class InteractionService {

    private static volatile InteractionService instance;

    /** 牌桌动画层（emoji 飞行 / 粒子挂载点）。 */
    private Pane fxLayer;
    /** 座位头像节点映射。 */
    private final Map<PlayerId, Node> seatAvatars = new HashMap<>();
    /** 座位节点（targeting 高亮加在座位根上，而非头像本身）。 */
    private final Map<PlayerId, Node> seatRoots = new HashMap<>();
    /** 座位显示名。 */
    private final Map<PlayerId, String> seatNames = new HashMap<>();
    /** AI 自动互动概率（0~1）。 */
    private static final double AI_INTERACT_PROBABILITY = 0.15;

    private final Random rng = new Random();

    private InteractionService() {
    }

    /** 单例入口。 */
    public static InteractionService getInstance() {
        InteractionService local = instance;
        if (local == null) {
            synchronized (InteractionService.class) {
                local = instance;
                if (local == null) {
                    local = new InteractionService();
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * 注入牌桌资源：动画层 + 座位头像 + 座位根节点 + 座位名。
     *
     * @param fxLayer 牌桌动画层（透明 Pane，铺满桌面）
     * @param seatAvatars 各 PlayerId 座位的头像 Node（用于飞行起点 / 终点 / Toast 锚点）
     * @param seatRoots 各 PlayerId 座位根 Node（用于 targeting 高亮样式）
     * @param seatNames 各 PlayerId 座位显示名
     */
    public void bind(Pane fxLayer,
                     Map<PlayerId, Node> seatAvatars,
                     Map<PlayerId, Node> seatRoots,
                     Map<PlayerId, String> seatNames) {
        this.fxLayer = fxLayer;
        this.seatAvatars.clear();
        this.seatAvatars.putAll(seatAvatars);
        this.seatRoots.clear();
        this.seatRoots.putAll(seatRoots);
        this.seatNames.clear();
        this.seatNames.putAll(seatNames);
    }

    /**
     * 发起一次互动。
     *
     * @param source 发起者
     * @param target 目标（短语类可与 source 相同——表示"说话人"）
     * @param type   互动类型
     * @param text   短语文本（投掷类可为 null，使用 type.hitText）
     */
    public void fireInteraction(PlayerId source, PlayerId target,
                                InteractionType type, String text) {
        if (type == null) {
            return;
        }
        Platform.runLater(() -> doFire(source, target, type, text));
    }

    /** 进入目标选择模式：高亮 validTargets 中的座位，点击后回调。 */
    public void enterTargetingMode(InteractionType type,
                                   Set<PlayerId> validTargets,
                                   Consumer<PlayerId> onTargetChosen) {
        Platform.runLater(() -> {
            clearTargetingMode();
            if (validTargets == null || validTargets.isEmpty() || onTargetChosen == null) {
                return;
            }
            for (PlayerId p : validTargets) {
                Node seat = seatRoots.get(p);
                if (seat == null) {
                    continue;
                }
                seat.getStyleClass().add("seat-targetable");
                seat.setUserData(new TargetingContext(type, p, onTargetChosen));
                seat.setOnMouseClicked(e -> {
                    e.consume();
                    Object data = seat.getUserData();
                    if (data instanceof TargetingContext ctx) {
                        clearTargetingMode();
                        ctx.onTargetChosen().accept(ctx.target());
                    }
                });
            }
        });
    }

    /** 退出目标选择模式：移除高亮与点击处理。 */
    public void clearTargetingMode() {
        for (Node seat : seatRoots.values()) {
            seat.getStyleClass().removeAll("seat-targetable");
            seat.setUserData(null);
            seat.setOnMouseClicked(null);
        }
    }

    /**
     * AI 在自己回合结束时按概率主动发起互动。
     *
     * @param ai AI 玩家
     * @param human 人类玩家
     * @param allOpponents AI 视角的其他玩家（用于短语也可向其说话，但默认 target=human 让玩家看见）
     */
    public void maybeAiInteract(PlayerId ai, PlayerId human, List<PlayerId> allOpponents) {
        if (rng.nextDouble() > AI_INTERACT_PROBABILITY) {
            return;
        }
        // 9 选 1：3 投掷 + 6 短语
        int pick = rng.nextInt(9);
        if (pick < 3) {
            InteractionType type = switch (pick) {
                case 0 -> InteractionType.EGG;
                case 1 -> InteractionType.FLOWER;
                default -> InteractionType.TOMATO;
            };
            fireInteraction(ai, human, type, null);
        } else {
            QuickPhrase phrase = QuickPhrase.values()[pick - 3];
            fireInteraction(ai, ai, phrase.getType(), phrase.getText());
        }
    }

    // ============================================================= 内部

    private void doFire(PlayerId source, PlayerId target,
                       InteractionType type, String text) {
        if (fxLayer == null) {
            // 没绑定时仅播声音 + toast（toast 在座位锚点上）
            playSound(type.getSound());
            return;
        }
        Node sourceAvatar = seatAvatars.get(source);
        Node targetAvatar = seatAvatars.get(target);

        if (type.isThrow()) {
            String hitText = buildHitText(target, type, text);
            if (sourceAvatar == null || targetAvatar == null) {
                playSound(type.getSound());
                if (targetAvatar != null) {
                    GameToast.show(targetAvatar, hitText);
                }
                return;
            }
            flyEmoji(sourceAvatar, targetAvatar, type, () -> {
                playSound(type.getSound());
                shakeNode(targetAvatar);
                GameToast.show(targetAvatar, hitText);
            });
        } else {
            // 短语类：在源座位上方弹气泡
            Node anchor = sourceAvatar != null ? sourceAvatar : targetAvatar;
            if (anchor == null) {
                playSound(type.getSound());
                return;
            }
            playSound(type.getSound());
            GameToast.show(anchor, text == null ? "" : text);
        }
    }

    /** 飞行 emoji：源→目标，弧线 + 旋转 + 缩放，落地回调。 */
    private void flyEmoji(Node fromAvatar, Node toAvatar,
                          InteractionType type, Runnable onArrived) {
        if (!GameAnimationService.getInstance().isEnabled()) {
            onArrived.run();
            return;
        }
        Point2D fromScene = centerScene(fromAvatar);
        Point2D toScene = centerScene(toAvatar);
        Point2D from = fxLayer.sceneToLocal(fromScene);
        Point2D to = fxLayer.sceneToLocal(toScene);

        Label flyer = new Label(type.getEmoji());
        flyer.getStyleClass().add("interaction-flyer");
        flyer.setMouseTransparent(true);
        flyer.setManaged(false);
        flyer.setLayoutX(from.getX() - 14);
        flyer.setLayoutY(from.getY() - 14);
        fxLayer.getChildren().add(flyer);

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double peakHeight = 60; // 弧线峰值

        // 位置：用 Timeline 三关键帧形成弧线
        Timeline arc = new Timeline();
        KeyFrame k0 = new KeyFrame(Duration.ZERO,
                new KeyValue(flyer.layoutXProperty(), from.getX() - 14),
                new KeyValue(flyer.layoutYProperty(), from.getY() - 14),
                new KeyValue(flyer.scaleXProperty(), 1.0),
                new KeyValue(flyer.scaleYProperty(), 1.0),
                new KeyValue(flyer.rotateProperty(), 0));
        KeyFrame kMid = new KeyFrame(Duration.millis(300),
                new KeyValue(flyer.layoutXProperty(), from.getX() - 14 + dx * 0.5),
                new KeyValue(flyer.layoutYProperty(),
                        from.getY() - 14 + dy * 0.5 - peakHeight,
                        Interpolator.EASE_BOTH),
                new KeyValue(flyer.scaleXProperty(), 1.15),
                new KeyValue(flyer.scaleYProperty(), 1.15),
                new KeyValue(flyer.rotateProperty(), 180));
        KeyFrame kEnd = new KeyFrame(Duration.millis(600),
                new KeyValue(flyer.layoutXProperty(), to.getX() - 14),
                new KeyValue(flyer.layoutYProperty(), to.getY() - 14),
                new KeyValue(flyer.scaleXProperty(), 0.85),
                new KeyValue(flyer.scaleYProperty(), 0.85),
                new KeyValue(flyer.rotateProperty(), 360));
        arc.getKeyFrames().addAll(k0, kMid, kEnd);

        PauseTransition remove = new PauseTransition(Duration.millis(80));
        remove.setOnFinished(e -> {
            fxLayer.getChildren().remove(flyer);
            // 落地撞击粒子：按互动类型差异化
            playImpactBurst(to, type);
            onArrived.run();
        });
        SequentialTransition seq = new SequentialTransition(arc, remove);
        seq.play();
    }

    /**
     * 落地撞击爆裂：按互动类型差异化。
     * <ul>
     *   <li>{@code EGG}：黄色蛋壳碎片四溅 + 中央"💥"破裂爆闪；</li>
     *   <li>{@code TOMATO}：红色飞溅点 + 中央"🍅"压扁放大；</li>
     *   <li>{@code FLOWER}：花瓣"🌸"四向飘落 + 中央"✨"金色光晕。</li>
     * </ul>
     */
    private void playImpactBurst(Point2D atLocal, InteractionType type) {
        try {
            // 中央主爆点
            String coreGlyph = switch (type) {
                case EGG -> "💥";
                case TOMATO -> "🍅";
                case FLOWER -> "✨";
                default -> "✦";
            };
            Label impact = new Label(coreGlyph);
            impact.getStyleClass().add("interaction-impact");
            impact.setMouseTransparent(true);
            impact.setManaged(false);
            impact.setLayoutX(atLocal.getX() - 16);
            impact.setLayoutY(atLocal.getY() - 16);
            impact.setOpacity(0.0);
            fxLayer.getChildren().add(impact);

            // 中央动画：破裂 / 压扁 / 闪现
            double coreMaxScale = type == InteractionType.TOMATO ? 1.8 : 1.5;
            Timeline flash = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(impact.opacityProperty(), 0.0),
                            new KeyValue(impact.scaleXProperty(), 0.3),
                            new KeyValue(impact.scaleYProperty(), 0.3)),
                    new KeyFrame(Duration.millis(100),
                            new KeyValue(impact.opacityProperty(), 1.0),
                            new KeyValue(impact.scaleXProperty(), coreMaxScale),
                            new KeyValue(impact.scaleYProperty(),
                                    type == InteractionType.TOMATO ? 0.6 : coreMaxScale)),
                    new KeyFrame(Duration.millis(500),
                            new KeyValue(impact.opacityProperty(), 0.0),
                            new KeyValue(impact.scaleXProperty(), coreMaxScale + 0.8),
                            new KeyValue(impact.scaleYProperty(), coreMaxScale + 0.8)));
            flash.setOnFinished(e -> fxLayer.getChildren().remove(impact));
            flash.play();

            // 飞溅碎片：6~8 个 emoji 从中心向外散开
            String shardGlyph;
            int shardCount;
            double spread;
            double lifetimeMs;
            switch (type) {
                case EGG -> {
                    shardGlyph = "🥚";
                    shardCount = 7;
                    spread = 90;
                    lifetimeMs = 600;
                }
                case TOMATO -> {
                    shardGlyph = "🔴";
                    shardCount = 8;
                    spread = 110;
                    lifetimeMs = 550;
                }
                case FLOWER -> {
                    shardGlyph = "🌸";
                    shardCount = 6;
                    spread = 80;
                    lifetimeMs = 1200; // 花瓣飘落更慢
                }
                default -> {
                    return;
                }
            }
            for (int i = 0; i < shardCount; i++) {
                spawnShard(shardGlyph, atLocal, spread, lifetimeMs,
                        type == InteractionType.FLOWER, i, shardCount);
            }
        } catch (Throwable ignored) {
            // 视觉占位，不阻断主流程
        }
    }

    /**
     * 生成一片飞溅碎片：从中心向外散开并淡出。
     * 花瓣类会缓慢飘落（带弧线 + 旋转）。
     */
    private void spawnShard(String glyph, Point2D origin, double spread, double lifetimeMs,
                            boolean drifting, int index, int total) {
        Label shard = new Label(glyph);
        shard.getStyleClass().add("interaction-shard");
        shard.setMouseTransparent(true);
        shard.setManaged(false);
        double sx = 14 + (index % 2 == 0 ? 0 : 4);
        shard.setLayoutX(origin.getX() - sx);
        shard.setLayoutY(origin.getY() - sx);
        shard.setOpacity(1.0);
        fxLayer.getChildren().add(shard);

        // 角度均匀分布 + 随机扰动
        double baseAngle = 2 * Math.PI * index / total;
        double angle = baseAngle + (rng.nextDouble() - 0.5) * 0.6;
        double dist = spread * (0.7 + rng.nextDouble() * 0.6);
        double endX = origin.getX() - sx + Math.cos(angle) * dist;
        double endY = origin.getY() - sx + Math.sin(angle) * dist
                + (drifting ? 40 + rng.nextDouble() * 30 : 0); // 花瓣向下飘

        Timeline move = new Timeline();
        KeyFrame k0 = new KeyFrame(Duration.ZERO,
                new KeyValue(shard.layoutXProperty(), origin.getX() - sx),
                new KeyValue(shard.layoutYProperty(), origin.getY() - sx),
                new KeyValue(shard.opacityProperty(), 1.0),
                new KeyValue(shard.rotateProperty(), 0.0),
                new KeyValue(shard.scaleXProperty(), 1.0),
                new KeyValue(shard.scaleYProperty(), 1.0));
        KeyFrame kMid = new KeyFrame(Duration.millis(lifetimeMs * 0.4),
                new KeyValue(shard.opacityProperty(), 1.0),
                new KeyValue(shard.scaleXProperty(), 1.15),
                new KeyValue(shard.scaleYProperty(), 1.15),
                new KeyValue(shard.rotateProperty(), 180 * (index % 2 == 0 ? 1 : -1)));
        KeyFrame kEnd = new KeyFrame(Duration.millis(lifetimeMs),
                new KeyValue(shard.layoutXProperty(), endX),
                new KeyValue(shard.layoutYProperty(), endY),
                new KeyValue(shard.opacityProperty(), 0.0),
                new KeyValue(shard.rotateProperty(), 360 * (index % 2 == 0 ? 1 : -1)),
                new KeyValue(shard.scaleXProperty(), 0.5),
                new KeyValue(shard.scaleYProperty(), 0.5));
        move.getKeyFrames().addAll(k0, kMid, kEnd);
        move.setOnFinished(e -> fxLayer.getChildren().remove(shard));
        move.play();
    }

    /** 抖动节点（受击反馈）。 */
    private void shakeNode(Node node) {
        if (!GameAnimationService.getInstance().isEnabled() || node == null) {
            return;
        }
        try {
            double origX = node.getTranslateX();
            TranslateTransition s1 = new TranslateTransition(Duration.millis(40), node);
            s1.setFromX(origX);
            s1.setToX(origX + 5);
            TranslateTransition s2 = new TranslateTransition(Duration.millis(40), node);
            s2.setFromX(origX + 5);
            s2.setToX(origX - 5);
            TranslateTransition s3 = new TranslateTransition(Duration.millis(40), node);
            s3.setFromX(origX - 5);
            s3.setToX(origX);
            SequentialTransition seq = new SequentialTransition(s1, s2, s3, s2, s3);
            seq.play();
        } catch (Throwable ignored) {
        }
    }

    private Point2D centerScene(Node node) {
        var b = node.localToScene(node.getBoundsInLocal());
        return new Point2D(b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2);
    }

    private String buildHitText(PlayerId target, InteractionType type, String override) {
        String name = seatNames.getOrDefault(target, "");
        String body = override != null ? override : type.getHitText();
        if (body == null) {
            return name;
        }
        return name.isBlank() ? body : name + " " + body;
    }

    private void playSound(SoundEffect effect) {
        try {
            AudioService.getInstance().playEffect(effect);
        } catch (Throwable ignored) {
        }
    }

    private record TargetingContext(InteractionType type, PlayerId target,
                                    Consumer<PlayerId> onTargetChosen) {
    }
}
