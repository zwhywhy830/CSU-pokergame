package com.cards.ui.component;

import com.cards.bridge.CsuCardBridge;
import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;
import com.csu.pokergame.core.card.Suit;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 可视化出牌历史面板：替代右侧纯文字日志。
 *
 * <p>把游戏事件解析成结构化行：玩家头像 → 动作文字 → 迷你卡牌缩略图。
 * 支持跑得快（出牌/不出/获胜/清桌）和骗子酒馆（宣告/相信/质疑/洗牌）两种事件格式。
 * 卡牌描述格式为 {@code rank.label() + suit.symbol()}（如 "3♠"、"10♥"、"JOKER♥"），
 * 解析时按 label 长度降序匹配以正确处理多字符 label（"JOKER"、"10"）。
 */
public final class PlayHistoryPanel extends ScrollPane {

    private static final int MAX_ENTRIES = 40;
    private static final double MINI_W = 26;
    private static final double MINI_H = 36;

    /** 按标签长度降序排列的 Rank 数组（静态副本，避免修改枚举原数组）。 */
    private static final Rank[] RANKS_BY_LEN_DESC;
    static {
        Rank[] ranks = Rank.values();
        RANKS_BY_LEN_DESC = Arrays.copyOf(ranks, ranks.length);
        Arrays.sort(RANKS_BY_LEN_DESC, (a, b) -> Integer.compare(b.label().length(), a.label().length()));
    }

    private final VBox content = new VBox(4);

    public PlayHistoryPanel() {
        content.getStyleClass().add("history-panel");
        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("history-scroll");
    }

    /** 设置事件列表（时间顺序，最新在最后）。面板自动滚动到底部。 */
    public void setEvents(List<String> events) {
        content.getChildren().clear();
        if (events == null || events.isEmpty()) {
            return;
        }
        int start = Math.max(0, events.size() - MAX_ENTRIES);
        for (int i = start; i < events.size(); i++) {
            content.getChildren().add(createRow(events.get(i)));
        }
        Platform.runLater(() -> setVvalue(1.0));
    }

    private HBox createRow(String event) {
        ParsedEvent pe = parseEvent(event);

        HBox row = new HBox(6);
        row.getStyleClass().add("history-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // 玩家头像 / 动作图标
        Label avatar = new Label(pe.glyph());
        avatar.getStyleClass().addAll("history-avatar", "history-avatar-" + pe.actionStyle);
        avatar.setMinSize(24, 24);
        avatar.setMaxSize(24, 24);
        avatar.setAlignment(Pos.CENTER);
        // emoji 图标用系统彩色 emoji 字体；玩家首字走默认字体（中文回退正常）
        if (pe.glyph != null && !pe.glyph.isEmpty()) {
            avatar.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 12));
        }

        // 动作文字
        Label action = new Label(pe.actionText);
        action.getStyleClass().addAll("history-action", "history-action-" + pe.actionStyle);
        action.setWrapText(true);
        VBox textBox = new VBox(action);
        VBox.setMargin(action, new javafx.geometry.Insets(2, 0, 0, 0));

        row.getChildren().addAll(avatar, textBox);

        // 迷你卡牌（如果有）
        if (pe.cards != null && !pe.cards.isEmpty()) {
            HBox cardBox = new HBox(2);
            cardBox.getStyleClass().add("history-card-row");
            for (Card card : pe.cards) {
                ImageView iv = new ImageView(CsuCardBridge.faceImage(card));
                iv.setFitWidth(MINI_W);
                iv.setFitHeight(MINI_H);
                iv.setMouseTransparent(true);
                iv.getStyleClass().add("history-card-mini");
                cardBox.getChildren().add(iv);
            }
            row.getChildren().add(cardBox);
        }

        return row;
    }

    // ============================================================= 事件解析

    /**
     * 解析事件字符串，提取玩家名、动作类型和卡牌列表。
     *
     * <p>跑得快事件格式：
     * <ul>
     *   <li>"SEAT_2 出 3♠ 4♠ 5♠" → 出牌</li>
     *   <li>"SEAT_1 不出" → 过牌</li>
     *   <li>"SEAT_1 出完手牌，获胜" → 获胜</li>
     *   <li>"连续两家不出，桌面清空，SEAT_1 继续" → 清桌</li>
     * </ul>
     *
     * <p>骗子酒馆事件格式（质疑拆为多步）：
     * <ul>
     *   <li>"发牌完成，目标点数 K，轮到 用户" → 发牌</li>
     *   <li>"用户 宣告 2 张 K" → 宣告</li>
     *   <li>"AI1 选择相信，轮到其出牌" → 相信</li>
     *   <li>"AI1 选择质疑！" → 发起质疑</li>
     *   <li>"质疑失败：宣告属实" / "质疑成功：拆穿谎言！" → 质疑结果（系统行）</li>
     *   <li>"AI1 扣扳机——咔哒，空仓存活" → 空仓</li>
     *   <li>"用户 扣扳机——中弹！被淘汰" → 中弹</li>
     *   <li>"用户 获胜，对局结束" → 获胜</li>
     *   <li>"重新洗牌发牌，轮到 用户" → 洗牌</li>
     * </ul>
     */
    static ParsedEvent parseEvent(String event) {
        if (event == null || event.isBlank()) {
            return new ParsedEvent("?", "—", "system", null, null);
        }

        // 跑得快：出牌（不含"出完手牌"，也不含骗子酒馆的"出牌"）
        int playIdx = event.indexOf(" 出 ");
        if (playIdx > 0 && !event.contains("出完手牌")) {
            String player = event.substring(0, playIdx);
            String cardsPart = event.substring(playIdx + 3);
            List<Card> cards = parseCards(cardsPart);
            return new ParsedEvent(player, "出牌", "play", cards, null);
        }

        // 跑得快：不出
        int passIdx = event.indexOf(" 不出");
        if (passIdx > 0 && !event.contains("连续")) {
            String player = event.substring(0, passIdx);
            return new ParsedEvent(player, "不出", "pass", null, null);
        }

        // 跑得快：获胜
        if (event.contains("出完手牌，获胜")) {
            int idx = event.indexOf(" 出完手牌");
            String player = idx > 0 ? event.substring(0, idx) : "?";
            return new ParsedEvent(player, "获胜！", "win", null, null);
        }

        // 跑得快：清桌
        if (event.contains("连续两家不出") || event.contains("桌面清空")) {
            return new ParsedEvent("—", "桌面清空", "clear", null, "♻");
        }

        // 骗子酒馆：发起质疑
        int challengeIdx = event.indexOf(" 选择质疑");
        if (challengeIdx > 0) {
            String player = event.substring(0, challengeIdx);
            return new ParsedEvent(player, "选择质疑！", "challenge", null, "🔍");
        }

        // 骗子酒馆：质疑结果（无玩家前缀的系统行）
        if (event.startsWith("质疑成功")) {
            return new ParsedEvent("—", event, "challenge-ok", null, "✓");
        }
        if (event.startsWith("质疑失败")) {
            return new ParsedEvent("—", event, "challenge-bad", null, "✗");
        }

        // 骗子酒馆：扣扳机——中弹
        int hitIdx = event.indexOf(" 扣扳机——中弹");
        if (hitIdx > 0) {
            String player = event.substring(0, hitIdx);
            return new ParsedEvent(player, "扣扳机 · 中弹淘汰！", "gun-hit", null, "💥");
        }

        // 骗子酒馆：扣扳机——空仓
        int clickIdx = event.indexOf(" 扣扳机——咔哒");
        if (clickIdx > 0) {
            String player = event.substring(0, clickIdx);
            return new ParsedEvent(player, "扣扳机 · 咔哒空仓", "gun-miss", null, "🔫");
        }

        // 骗子酒馆：宣告
        int declareIdx = event.indexOf(" 宣告 ");
        if (declareIdx > 0) {
            String player = event.substring(0, declareIdx);
            String rest = event.substring(declareIdx + 3);
            return new ParsedEvent(player, "宣告 " + rest, "declare", null, null);
        }

        // 骗子酒馆：相信
        int trustIdx = event.indexOf(" 选择相信");
        if (trustIdx > 0) {
            String player = event.substring(0, trustIdx);
            return new ParsedEvent(player, "选择相信", "trust", null, null);
        }

        // 两种模式：获胜
        if (event.contains("获胜，对局结束") || event.contains("出完手牌，获胜")) {
            int idx = event.indexOf(" 获胜");
            if (idx < 0) idx = event.indexOf(" 出完手牌");
            String player = idx > 0 ? event.substring(0, idx) : "?";
            return new ParsedEvent(player, "获胜！", "win", null, null);
        }

        // 骗子酒馆：重新洗牌
        if (event.contains("重新洗牌")) {
            if (event.contains("出完手牌")) {
                int idx = event.indexOf(" 出完手牌");
                String player = idx > 0 ? event.substring(0, idx) : "?";
                return new ParsedEvent(player, "出完手牌 · 重新洗牌", "reshuffle", null, "🔄");
            }
            return new ParsedEvent("—", "重新洗牌发牌", "reshuffle", null, "🔄");
        }

        // 骗子酒馆：开局发牌
        if (event.startsWith("发牌完成")) {
            return new ParsedEvent("—", event, "deal", null, "🎴");
        }

        // 兜底：系统事件
        return new ParsedEvent("—", event, "system", null, "•");
    }

    /** 从空格分隔的牌面文字中解析出 Card 列表。 */
    private static List<Card> parseCards(String text) {
        List<Card> cards = new ArrayList<>();
        for (String token : text.split(" ")) {
            Card c = parseCard(token);
            if (c != null) {
                cards.add(c);
            }
        }
        return cards;
    }

    /**
     * 把单张牌面文字（如 "3♠"、"10♥"、"JOKER♥"）解析回 Card。
     * 按 rank 标签长度降序匹配，确保 "JOKER" 不会被误匹配为 "J"。
     */
    private static Card parseCard(String text) {
        for (Rank rank : RANKS_BY_LEN_DESC) {
            if (text.startsWith(rank.label())) {
                String rest = text.substring(rank.label().length());
                for (Suit suit : Suit.values()) {
                    if (suit.symbol().equals(rest)) {
                        return new Card(rank, suit);
                    }
                }
            }
        }
        return null;
    }

    /** 解析结果。glyph 非空时用 emoji 图标，否则取玩家名首字。 */
    static final class ParsedEvent {
        final String playerName;
        final String actionText;
        final String actionStyle;
        final List<Card> cards;
        final String glyph;

        ParsedEvent(String playerName, String actionText, String actionStyle, List<Card> cards, String glyph) {
            this.playerName = playerName;
            this.actionText = actionText;
            this.actionStyle = actionStyle;
            this.cards = cards;
            this.glyph = glyph;
        }

        String glyph() {
            if (glyph != null && !glyph.isEmpty()) {
                return glyph;
            }
            if (playerName == null || playerName.isEmpty()) {
                return "?";
            }
            return playerName.substring(0, 1);
        }
    }
}
