package com.csu.pokergame.liarspoker;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.player.BotDecision;
import com.csu.pokergame.core.player.BotPolicy;

import java.util.List;

/**
 * 骗子酒馆机器人策略：在开局决定「一直质疑」或「一直不质疑」。
 * 构造时给定是否质疑；如需「开局 50% 掷硬币」，由调用方用 Random.nextBoolean() 决定后传入。
 */
public final class LiarFixedPolicy implements BotPolicy {

    private final boolean alwaysChallenge;

    public LiarFixedPolicy(boolean alwaysChallenge) {
        this.alwaysChallenge = alwaysChallenge;
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
            return alwaysChallenge
                    ? new BotDecision(challenge, "本局一直质疑")
                    : new BotDecision(trust, "本局一直相信");
        }
        if (challenge != null) {
            return new BotDecision(challenge, "质疑");
        }
        if (trust != null) {
            return new BotDecision(trust, "相信");
        }
        GameCommand declare = legal.stream()
                .filter(cmd -> cmd instanceof DeclareLiarCards)
                .findFirst()
                .orElseThrow();
        return new BotDecision(declare, "宣告出牌");
    }
}
