package com.csu.pokergame.core.engine;

import com.csu.pokergame.liarspoker.ChallengeDeclaration;
import com.csu.pokergame.liarspoker.DeclareLiarCards;
import com.csu.pokergame.liarspoker.TrustDeclaration;
import com.csu.pokergame.paodekuai.PassPdkTurn;
import com.csu.pokergame.paodekuai.PlayPdkCards;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 玩家指令的标记接口。具体指令由各游戏定义(如跑得快的 PlayPdkCards / PassPdkTurn)。
 * 引擎通过 {@code apply} 校验当前玩家、阶段与指令合法性,非法时抛 IllegalArgumentException。
 *
 * <p>联机序列化:Jackson 多态,通过 {@code @type} 字段区分子类型。
 * 引入 Jackson 注解不违反 {@code core} 零 JavaFX 依赖规则。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlayPdkCards.class, name = "PlayPdkCards"),
        @JsonSubTypes.Type(value = PassPdkTurn.class, name = "PassPdkTurn"),
        @JsonSubTypes.Type(value = DeclareLiarCards.class, name = "DeclareLiarCards"),
        @JsonSubTypes.Type(value = TrustDeclaration.class, name = "TrustDeclaration"),
        @JsonSubTypes.Type(value = ChallengeDeclaration.class, name = "ChallengeDeclaration"),
})
public interface GameCommand {
}
