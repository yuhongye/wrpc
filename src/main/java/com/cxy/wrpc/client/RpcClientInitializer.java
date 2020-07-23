package com.cxy.wrpc.client;

import com.cxy.wrpc.protocol.RpcDecoder;
import com.cxy.wrpc.protocol.RpcEncoder;
import com.cxy.wrpc.protocol.RpcResponse;
import com.cxy.wrpc.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        Serializer serializer = Serializer.create();
        pipeline.addLast(new RpcEncoder(serializer));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        pipeline.addLast(new RpcDecoder<>(RpcResponse.class, serializer));
        pipeline.addLast(new RpcClientHandler());
    }
}
