package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.card.Rank;

import java.util.List;

/** 一次出牌的不可变描述：按点数排序的牌、牌型、主比较点数与张数。 */
public record PdkMove(List<Card> cards, PdkMoveType type, Rank primaryRank, int size) {

    public boolean isBomb() {
        return type == PdkMoveType.FOUR_OF_A_KIND;
    }
}
