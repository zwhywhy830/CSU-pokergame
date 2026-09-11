package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;

import java.util.List;

/**
 * 一手已识别的跑得快牌型。
 *
 * @param type     牌型
 * @param cards    组成这手牌的牌(按牌型语义排序:主体在前、带牌在后)
 * @param mainRank 比较用的主点数(三带一/三带二取三条的点数,顺子取最大点数)
 */
public record PdkMove(PdkMoveType type, List<Card> cards, Rank mainRank) {

    public PdkMove {
        cards = List.copyOf(cards); // 防御性不可变
    }

    /** 牌的张数。 */
    public int size() {
        return cards.size();
    }
}
