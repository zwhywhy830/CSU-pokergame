package com.csu.pokergame.core.player;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 规则机器人控制器：立即返回策略选定的指令。 */
public final class RuleBotController implements PlayerController {

    private final BotPolicy policy;

    public RuleBotController(BotPolicy policy) {
        this.policy = policy;
    }

    @Override
    public CompletableFuture<GameCommand> choose(GameSnapshot snapshot, List<GameCommand> legal) {
        BotDecision decision = policy.decide(snapshot, legal);
        return CompletableFuture.completedFuture(decision.command());
    }

    /** 供 UI 展示机器人行动理由。 */
    public BotDecision decide(GameSnapshot snapshot, List<GameCommand> legal) {
        return policy.decide(snapshot, legal);
    }
}
