package com.cxy.wrpc.client;

import com.cxy.wrpc.protocol.RpcRequest;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 将client的方法调用转成rpc request；将rpc response转成方法返回
 */
@Slf4j
public class RpcStub implements MethodInterceptor {

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            log.info("Object.{} invoke, do not send request.", method.getName());
            return methodProxy.invokeSuper(obj, args);
        }
        // 1. 构造request
        RpcRequest request = buildRequest(method, args);

        // 2. send request
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler();
        log.info("request will send to {}", handler);
        RPCFuture future = handler.sendRequest(request);

        // 3. receive response
        return future.get();
    }

    private RpcRequest buildRequest(Method method, Object[] args) {
        RpcRequest request = RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .className(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(args)
                .build();
        log.info("build rpc request: {}", request);
        return request;
    }


}
