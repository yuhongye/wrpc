package com.cxy.wrpc.protocol;

import com.cxy.wrpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class RpcEncoder extends MessageToByteEncoder<Object> {
    private Serializer serializer;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf byteBuf) throws Exception {
        byte[] data = serializer.serialize(in);
        log.debug("write data length: {}", data.length);
        byteBuf.writeInt(data.length);
        byteBuf.writeBytes(data);
    }
}
