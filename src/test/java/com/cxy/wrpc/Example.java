package com.cxy.wrpc;

import com.cxy.wrpc.client.RpcClient;

public class Example {
    public static void main(String[] args) {
        RpcClient client = new RpcClient();
        HelloWorldService service = client.create(HelloWorldService.class);
        String response = service.hello("world");
        System.out.println(response);
    }
}
