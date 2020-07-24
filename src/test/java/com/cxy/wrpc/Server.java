package com.cxy.wrpc;

import com.cxy.wrpc.registry.ServiceRegistry;
import com.cxy.wrpc.server.RpcServer;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        ServiceRegistry registry = new ServiceRegistry(Confs.ZK);
        RpcServer server = new RpcServer(Confs.SERVER_ADDR, registry);
        server.start("com.cxy.wrpc");
    }
}
