package com.cxy.wrpc.server;

import com.cxy.wrpc.protocol.RpcRequest;
import com.cxy.wrpc.protocol.RpcResponse;
import com.cxy.wrpc.utils.GlobleExecutor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Slf4j
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static Map<Class<?>, FastClass> fastClassMap = new ConcurrentHashMap<>();

    private final Map<String, Object> handlerMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        Runnable task = () -> {
            log.info("receive request: {}", request);
            RpcResponse response = new RpcResponse(request.getRequestId());
            try {
                Object result = handle(request);
                response.setResult(result);
            } catch (Throwable e) {
                response.setError(e.toString());
                log.error("Rpc Server handle request error.", e);
            }
            ctx.writeAndFlush(response).addListener(future ->
                    log.info("Send response for request[{}] success.", request.getRequestId()));
        };
        GlobleExecutor.submit(task);
    }

    /**
     * 进行方法调用
     * @param request
     * @return service调用结果
     * @throws InvocationTargetException
     */
    private Object handle(RpcRequest request) throws InvocationTargetException {
        String className = request.getClassName();
        Object service = handlerMap.get(className);
        Class<?> serviceClass = service.getClass();

        // 使用cglib来代理方法调用，避免使用反射带来的性能开销
        FastClass serviceFastClass = fastClassMap.computeIfAbsent(serviceClass, cls -> FastClass.create(cls));
        int methodIndex = serviceFastClass.getIndex(request.getMethodName(), request.getParameterTypes());
        return serviceFastClass.invoke(methodIndex, service, request.getParameters());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server caught exception", cause);
        ctx.close();
    }
}
