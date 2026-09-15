package com.csu.pokergame.core.player;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;

import java.util.List;

/** 机器人策略：根据快照与合法指令列表产出确定性决策。 */
@FunctionalInterface
public interface BotPolicy {

    BotDecision decide(GameSnapshot snapshot, List<GameCommand> legal);
}
