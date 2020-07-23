package com.cxy.wrpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 所有的请求都通过转化成该类发送给server
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RpcRequest {
    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
