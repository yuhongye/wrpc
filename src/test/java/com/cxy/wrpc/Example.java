package com.cxy.wrpc;

import com.cxy.wrpc.client.RpcClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {
    public static void main(String[] args) {
        RpcClient client = new RpcClient(Confs.ZK);
        HelloWorldService service = client.create(HelloWorldService.class);
        String response = service.hello("world");
        log.info("response: {}", response);
    }
}
