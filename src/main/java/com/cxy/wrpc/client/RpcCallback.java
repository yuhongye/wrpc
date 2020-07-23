package com.cxy.wrpc.client;

public interface RpcCallback {
    void success(Object result);

    void fail(Exception e);
}
