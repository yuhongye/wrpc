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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供两个方法：
 * 1. 通过netty channel将请求发送给server端
 * 2. 实现inbound接口, 处理response
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private ConcurrentHashMap<String, CompletableFuture<RpcResponse>> pendingRpc = new ConcurrentHashMap<>();

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
        String requestId = response.getRequestId();
        CompletableFuture<RpcResponse> future = pendingRpc.get(requestId);
        if (future != null) {
            pendingRpc.remove(requestId);
            future.complete(response);
            log.info("complete requestId[{}], response success ? ", requestId, response.isOK());
        } else {
            log.warn("requestId[{}] rpc call finished, but can not found future. the response detail: {}",
                    requestId, response);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel Inactive. still wait result request number: {}", pendingRpc.size());
        pendingRpc.forEach((reqId, f) -> f.complete(new RpcResponse(reqId, "连接断开")));
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

    public CompletableFuture<RpcResponse> sendRequest(RpcRequest request) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRpc.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future;
    }

    @Override
    public String toString() {
        return "RpcClientHandler remote peer: " + remotePeer;
    }

}
