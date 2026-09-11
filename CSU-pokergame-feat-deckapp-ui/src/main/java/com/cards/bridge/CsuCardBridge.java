package com.cards.bridge;

import com.cards.model.Card;
import com.cards.model.Suit;
import com.cards.render.CanvasCardRenderer;
import com.cards.render.CardRenderer;
import com.csu.pokergame.core.card.Rank;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * 参考引擎牌模型（com.csu.pokergame.core.card.Card）与 P4 牌模型/渲染器之间的桥接。
 * 把跑得快/骗子酒馆引擎产出的牌映射成 P4 的 Card，再用 CanvasCardRenderer 渲染牌面 Image。
 *
 * <p>牌面 / 牌背图像在此做全局缓存：{@link CanvasCardRenderer} 每次渲染都要
 * {@code new Canvas(240,340)} + 全量矢量绘制 + {@code snapshot()}，单次开销远高于一次查表；
 * 而牌桌每刷新一次就要取全部手牌 + 桌面牌的图像（一次最多 20 余张，bot 回合每 0.7–0.9s 触发一次）。
 * 牌是不可变值对象（{@code equals/hashCode} 由花色 + 点数决定），缓存条目上限为 54 张牌面 + 2 张牌背，
 * 不会无限增长。
 */
public final class CsuCardBridge {

    private static final CardRenderer RENDERER = new CanvasCardRenderer();

    /** 牌面缓存（键为 P4 值对象牌，最多 54 项）。 */
    private static final Map<Card, Image> FACE_CACHE = new HashMap<>(64);
    /** 牌背缓存：red=true 红色牌背，false 蓝色牌背。 */
    private static final Map<Boolean, Image> BACK_CACHE = new HashMap<>(2);

    /** 参考 Rank → P4 点数 1..13。ACE→1（P4 的 A=1），TWO→2，THREE..KING 用其 comparisonValue()。 */
    public static int toP4Rank(Rank rank) {
        return switch (rank) {
            case ACE -> 1;
            case TWO -> 2;
            case JOKER -> 0;
            default -> rank.comparisonValue(); // THREE=3 .. KING=13
        };
    }

    /** 参考 Suit → P4 Suit。 */
    public static Suit toP4Suit(com.csu.pokergame.core.card.Suit s) {
        return switch (s) {
            case SPADES -> Suit.SPADE;
            case HEARTS -> Suit.HEART;
            case CLUBS -> Suit.CLUB;
            case DIAMONDS -> Suit.DIAMOND;
        };
    }

    /**
     * 参考 Card → P4 Card。
     * JOKER 按花色区分大小王：HEARTS（红）→大王，SPADES（黑）→小王；
     * Deck.liarPoker() 的两张 JOKER 正是 HEARTS 与 SPADES，视觉上一大一小可区分。
     */
    public static Card toP4Card(com.csu.pokergame.core.card.Card c) {
        if (c.rank() == Rank.JOKER) {
            return c.suit() == com.csu.pokergame.core.card.Suit.HEARTS
                    ? Card.redJoker()
                    : Card.blackJoker();
        }
        return Card.of(toP4Suit(c.suit()), toP4Rank(c.rank()));
    }

    /** 取参考引擎牌的正面 Image（首次渲染后缓存复用，同一张牌只渲染一次）。 */
    public static Image faceImage(com.csu.pokergame.core.card.Card c) {
        return FACE_CACHE.computeIfAbsent(toP4Card(c), RENDERER::face);
    }

    /** 取牌背 Image（红 / 蓝各缓存一张）。red=true 红色牌背，false 蓝色牌背。 */
    public static Image backImage(boolean red) {
        return BACK_CACHE.computeIfAbsent(red, RENDERER::back);
    }

    private CsuCardBridge() {
    }
}
