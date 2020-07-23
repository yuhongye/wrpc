package com.cxy.wrpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * server的结果都转化成该类传递过来
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RpcResponse {
    private String requestId;
    private String error;
    private Object result;

    public RpcResponse(String requestId) {
        this.requestId = requestId;
    }

    public boolean isOK() {
        return !isError();
    }

    public boolean isError() {
        return error != null;
    }
}
