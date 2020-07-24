package com.cxy.wrpc.client;

import com.cxy.wrpc.protocol.RpcRequest;
import com.cxy.wrpc.protocol.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供两个方法：
 * 1. 通过netty channel将请求发送给server端
 * 2. 实现inbound接口, 处理response
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private ConcurrentHashMap<String, RPCFuture> pendingRpc = new ConcurrentHashMap<>();

    @Getter
    private volatile Channel channel;
    @Getter
    private InetSocketAddress remotePeer;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
        log.info("channel registered.");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = (InetSocketAddress) this.channel.remoteAddress();
        log.info("channel active, remote peer: {}", remotePeer);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        log.info("===========receive response: {}", response);
        String requestId = response.getRequestId();
        RPCFuture future = pendingRpc.get(requestId);
        if (future != null) {
            pendingRpc.remove(requestId);
            future.done(response);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel Inactive. still wait result request number: {}", pendingRpc.size());
        pendingRpc.forEach((reqId, f) -> f.done(new RpcResponse(reqId, "连接断开")));
        pendingRpc.clear();
        ctx.fireChannelActive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        log.error("client caught exception", throwable);
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        log.info("channel closed.");
    }

    public RPCFuture sendRequest(RpcRequest request) {
        RPCFuture future = new RPCFuture(request);
        pendingRpc.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future;
    }

    @Override
    public String toString() {
        return "RpcClientHandler remote peer: " + remotePeer;
    }

}
