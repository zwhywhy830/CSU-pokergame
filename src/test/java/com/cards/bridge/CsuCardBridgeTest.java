package com.cards.bridge;

import com.cards.model.Card;
import com.cards.model.Suit;
import com.cards.ui.FxTestKit;
import com.csu.pokergame.core.card.Rank;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CsuCardBridge 阶段 7 冒烟测试：验证参考引擎牌 ↔ P4 牌模型映射，
 * 以及 CanvasCardRenderer 能产出非空牌面/牌背图像。
 */
class CsuCardBridgeTest {

    @BeforeAll
    static void initFx() {
        FxTestKit.initToolkit();
    }

    @Test
    void rankMappingIsCorrect() {
        assertThat(CsuCardBridge.toP4Rank(Rank.ACE)).isEqualTo(1);
        assertThat(CsuCardBridge.toP4Rank(Rank.TWO)).isEqualTo(2);
        assertThat(CsuCardBridge.toP4Rank(Rank.THREE)).isEqualTo(3);
        assertThat(CsuCardBridge.toP4Rank(Rank.KING)).isEqualTo(13);
        assertThat(CsuCardBridge.toP4Rank(Rank.JOKER)).isZero();
    }

    @Test
    void suitMappingIsCorrect() {
        assertThat(CsuCardBridge.toP4Suit(com.csu.pokergame.core.card.Suit.SPADES))
                .isEqualTo(Suit.SPADE);
        assertThat(CsuCardBridge.toP4Suit(com.csu.pokergame.core.card.Suit.HEARTS))
                .isEqualTo(Suit.HEART);
        assertThat(CsuCardBridge.toP4Suit(com.csu.pokergame.core.card.Suit.CLUBS))
                .isEqualTo(Suit.CLUB);
        assertThat(CsuCardBridge.toP4Suit(com.csu.pokergame.core.card.Suit.DIAMONDS))
                .isEqualTo(Suit.DIAMOND);
    }

    @Test
    void regularCardMapsToP4Card() {
        com.csu.pokergame.core.card.Card aceSpades =
                new com.csu.pokergame.core.card.Card(Rank.ACE, com.csu.pokergame.core.card.Suit.SPADES);
        Card p4 = CsuCardBridge.toP4Card(aceSpades);
        assertThat(p4.suit()).isEqualTo(Suit.SPADE);
        assertThat(p4.rank()).isEqualTo(1);
        assertThat(p4.isJoker()).isFalse();
    }

    @Test
    void jokerMapsBySuitColor() {
        com.csu.pokergame.core.card.Card redJoker =
                new com.csu.pokergame.core.card.Card(Rank.JOKER, com.csu.pokergame.core.card.Suit.HEARTS);
        com.csu.pokergame.core.card.Card blackJoker =
                new com.csu.pokergame.core.card.Card(Rank.JOKER, com.csu.pokergame.core.card.Suit.SPADES);

        // redJoker() 是大王（bigJoker=true），blackJoker() 是小王（bigJoker=false）
        assertThat(CsuCardBridge.toP4Card(redJoker).isBigJoker()).isTrue();
        assertThat(CsuCardBridge.toP4Card(blackJoker).isBigJoker()).isFalse();
    }

    @Test
    void faceAndBackImagesAreRendered() {
        FxTestKit.runAndWait(() -> {
            com.csu.pokergame.core.card.Card card =
                    new com.csu.pokergame.core.card.Card(Rank.QUEEN, com.csu.pokergame.core.card.Suit.HEARTS);
            Image face = CsuCardBridge.faceImage(card);
            Image backRed = CsuCardBridge.backImage(true);
            Image backBlue = CsuCardBridge.backImage(false);

            assertThat(face).isNotNull();
            assertThat(face.getWidth()).isPositive();
            assertThat(face.getHeight()).isPositive();
            assertThat(backRed).isNotNull();
            assertThat(backBlue).isNotNull();
            assertThat(backRed).isNotSameAs(backBlue);

            // 缓存：再次取同一张牌应返回同一实例
            assertThat(CsuCardBridge.faceImage(card)).isSameAs(face);
        });
    }
}
