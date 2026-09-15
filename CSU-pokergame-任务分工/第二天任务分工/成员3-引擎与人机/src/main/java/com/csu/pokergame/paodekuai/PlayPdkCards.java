package com.csu.pokergame.paodekuai;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GameCommand;

import java.util.List;

/** 出牌指令。 */
public record PlayPdkCards(List<Card> cards) implements GameCommand {
}
