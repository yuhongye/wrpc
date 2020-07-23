package com.cxy.wrpc.client;

import com.cxy.wrpc.protocol.RpcRequest;
import com.cxy.wrpc.registry.ServiceDiscovery;
import com.cxy.wrpc.utils.ClassTypes;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 创建rpc proxy的入口
 */
@Slf4j
@AllArgsConstructor
public class RpcClient {
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    private ServiceDiscovery serviceDiscovery;

    public RpcClient(String registryServerAddress) {
        serviceDiscovery = new ServiceDiscovery(registryServerAddress);
    }

    /**
     * 创建service的代理，调用方通过代理进行方法调用
     * @param service
     * @param <T>
     * @return
     */
    public <T> T create(Class<T> service) {
        Preconditions.checkArgument(service.isInterface(), "rpc service must be an interface.");
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(service);
        enhancer.setCallback(new RpcStub());
        return (T) enhancer.create();
    }

    /**
     * 网络调用，返回future
     */
    public RPCFuture call(Class<?> service, String methodName, Object... args) {
        RpcRequest request = RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .className(service.getName())
                .methodName(methodName)
                .parameterTypes(ClassTypes.getParameterTypes(args))
                .parameters(args)
                .build();
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler();
        log.info("call request: {} send to server: {}", request, handler);
        return handler.sendRequest(request);
    }

    public static void submit(Runnable task) {
        executor.submit(task);
    }

    public void stop() {
        executor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }
}

