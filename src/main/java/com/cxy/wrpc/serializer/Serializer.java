package com.cxy.wrpc.serializer;

/**
 * 序列化接口
 */
public interface Serializer {
    <T> byte[] serialize(T o);

    <T> T deserialize(byte[] data, Class<T> clazz);

    static Serializer create() {
        return new ProtoStuffSerializer();
    }
}
