package com.cxy.wrpc;

import com.cxy.wrpc.annotations.RpcService;
import lombok.extern.slf4j.Slf4j;

@RpcService(HelloWorldService.class)
@Slf4j
public class HelloWorldImpl implements HelloWorldService {
    @Override
    public String hello(String msg) {
        log.info("recevie message: {}", msg);
        return "Hello, " + msg + "!";
    }
}
