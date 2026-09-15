package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.card.Card;
import com.csu.pokergame.core.engine.GameCommand;

import java.util.List;

/** 宣告指令：打出 1–3 张牌并宣告均为当前目标点数。 */
public record DeclareLiarCards(List<Card> cards) implements GameCommand {
}
