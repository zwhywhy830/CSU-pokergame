package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.player.BotDecision;
import com.csu.pokergame.core.player.BotPolicy;

import java.util.List;
import java.util.Random;

/**
 * 骗子酒馆机器人策略：每个回应回合 50% 概率质疑，否则相信。
 * 随机源可注入，便于测试复现。
 */
public final class LiarRandomPolicy implements BotPolicy {

    private final Random random;

    public LiarRandomPolicy(Random random) {
        this.random = random;
    }

    @Override
    public BotDecision decide(GameSnapshot snapshot, List<GameCommand> legal) {
        GameCommand challenge = legal.stream()
                .filter(cmd -> cmd instanceof ChallengeDeclaration)
                .findFirst()
                .orElse(null);
        GameCommand trust = legal.stream()
                .filter(cmd -> cmd instanceof TrustDeclaration)
                .findFirst()
                .orElse(null);

        if (challenge != null && trust != null) {
            if (random.nextBoolean()) {
                return new BotDecision(challenge, "掷硬币决定质疑");
            }
            return new BotDecision(trust, "掷硬币决定相信");
        }
        if (challenge != null) {
            return new BotDecision(challenge, "质疑");
        }
        if (trust != null) {
            return new BotDecision(trust, "相信");
        }
        return declare(legal);
    }

    private BotDecision declare(List<GameCommand> legal) {
        GameCommand declare = legal.stream()
                .filter(cmd -> cmd instanceof DeclareLiarCards)
                .findFirst()
                .orElseThrow();
        return new BotDecision(declare, "宣告出牌");
    }
}
