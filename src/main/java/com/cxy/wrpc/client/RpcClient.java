package com.cxy.wrpc.client;

import com.cxy.wrpc.protocol.RpcRequest;
import com.cxy.wrpc.protocol.RpcResponse;
import com.cxy.wrpc.registry.ServiceDiscovery;
import com.cxy.wrpc.utils.ClassTypes;
import com.cxy.wrpc.utils.GlobleExecutor;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 创建rpc proxy的入口
 */
@Slf4j
@AllArgsConstructor
public class RpcClient {
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
    public CompletableFuture<RpcResponse> call(Class<?> service, String methodName, Object... args) {
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

    public void stop() {
        GlobleExecutor.stop();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }
}

