package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;

import java.util.List;

/** 一次宣告的不可变描述。 */
public record LiarMove(List<Card> cards, int declaredCount) {
}
