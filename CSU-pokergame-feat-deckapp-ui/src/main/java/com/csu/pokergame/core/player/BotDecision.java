package com.csu.pokergame.core.player;

import com.csu.pokergame.core.engine.GameCommand;

/** 机器人决策：选定的指令 + 展示用理由。 */
public record BotDecision(GameCommand command, String reason) {
}
