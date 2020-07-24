package com.cxy.wrpc.protocol;

import com.cxy.wrpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class RpcDecoder<T> extends ByteToMessageDecoder {
    private Class<T> cls;
    private Serializer serializer;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            log.debug("receive bytes length[{}] too small.", in.readableBytes());
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        // 不够，需要继续累积
        if (in.readableBytes() < dataLength) {
            log.debug("receive bytes length[{}] too small.", in.readableBytes());
            in.resetReaderIndex();
            return;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);
        Object obj = serializer.deserialize(data, cls);
        log.info("decode message: {}", obj);

        out.add(obj);
    }
}
