package com.cxy.wrpc;

import com.cxy.wrpc.client.RpcClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {
    public static void main(String[] args) {
        RpcClient client = new RpcClient(Confs.ZK);
        HelloWorldService service = client.create(HelloWorldService.class);
        log.info("HelloWorldService class: {}, bean: {}", service.getClass().getName(), service);
        for (int i = 0; i < 20; i++) {
            String response = service.hello("world-" + i);
            log.info("response: {}", response);
        }

        client.stop();
    }
}
