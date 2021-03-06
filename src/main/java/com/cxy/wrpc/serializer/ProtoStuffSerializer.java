package com.cxy.wrpc.serializer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * 使用protostuff作为序列化工具
 * thread safe
 */
@Slf4j
public class ProtoStuffSerializer implements Serializer {
    private static Objenesis objenesis = new ObjenesisStd(true);

    public <T> byte[] serialize(T o) {
        log.debug("serilize object: {}", o);
        Class<T> cls = (Class<T>) o.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = RuntimeSchema.getSchema(cls);
            return ProtostuffIOUtil.toByteArray(o, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    public <T> T deserialize(byte[] data, Class<T> clazz) {
        log.info("deserialize class[{}], object size: {}", clazz.getName(), data.length);
        Schema<T> schema  = RuntimeSchema.getSchema(clazz);
        T message = objenesis.newInstance(clazz);
        ProtostuffIOUtil.mergeFrom(data, message, schema);
        return message;
    }
}
