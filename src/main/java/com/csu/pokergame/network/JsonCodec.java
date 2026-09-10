package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Jackson 单例。负责 {@link WireMessage}、{@link GameCommand}、{@link GameSnapshot} 的
 * 序列化与反序列化。多态类型由各接口上的 {@code @JsonSubTypes} 注解自动注册。
 *
 * <p>所有 record 的反序列化通过 Jackson 的 record 支持(JDK 21 编译,record 反射可用)。
 * {@link PlayerId} 是枚举,Jackson 按名称序列化。
 *
 * <p>关闭 {@code FAIL_ON_UNKNOWN_PROPERTIES}:多态注解的 {@code visible=true} 让 type 字段
 * 同时出现在 JSON 与目标 record 中,record 没有对应字段时会被忽略,不报错。
 */
public final class JsonCodec {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** 把对象写成 UTF-8 JSON 字节。 */
    public static byte[] writeBytes(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化失败:" + e.getMessage(), e);
        }
    }

    /** 把 JSON 字节反序列化为 WireMessage。 */
    public static WireMessage readMessage(byte[] json) {
        try {
            return MAPPER.readValue(json, WireMessage.class);
        } catch (Exception e) {
            throw new IllegalStateException("反序列化 WireMessage 失败:" + e.getMessage(), e);
        }
    }

    /** 把 JSON 字节反序列化为 GameCommand。 */
    public static GameCommand readCommand(byte[] json) {
        try {
            return MAPPER.readValue(json, GameCommand.class);
        } catch (Exception e) {
            throw new IllegalStateException("反序列化 GameCommand 失败:" + e.getMessage(), e);
        }
    }

    /** 把 JSON 字节反序列化为 GameSnapshot。 */
    public static GameSnapshot readSnapshot(byte[] json) {
        try {
            return MAPPER.readValue(json, GameSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("反序列化 GameSnapshot 失败:" + e.getMessage(), e);
        }
    }

    private JsonCodec() {}
}
