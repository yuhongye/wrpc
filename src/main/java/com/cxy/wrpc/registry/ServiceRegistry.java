package com.cxy.wrpc.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * 服务注册
 */
@Slf4j
public class ServiceRegistry {
    private String registryAddress;
    private ZooKeeper zk;

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
        zk = connectZookeeper();
    }

    public void register(String data) {
        if (data != null) {
            addRootNodeIfAbsent(zk);
            createNode(zk, data);
        }
    }

    public void close() {
        try {
            zk.close();
        } catch (InterruptedException e) {
            log.error("close zk failed, skip it.", e);
        }
    }

    private ZooKeeper connectZookeeper() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            log.info("will connect zookeeper[{}] server.", registryAddress);
            ZooKeeper zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                    log.info("connect zookeeper[{}] success.", registryAddress);
                }
            });
            latch.await();
            return zk;
        } catch (IOException | InterruptedException e) {
            log.error("connect zookeeper failed.", e);
            throw new RuntimeException("connect zookeeper failed.", e);
        }
    }

    private void addRootNodeIfAbsent(ZooKeeper zk) {
        try {
            Stat s = zk.exists(Constant.ZK_REGISTRY_PATH, false);
            log.info("zookeeper root node: {}", s);
            if (s == null) {
                log.info("registry root node does not exist. will create it......");
                zk.create(Constant.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                log.info("create root node success.");
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("add registry root node failed.", e);
        }
    }

    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("create zookeeper node ({} => {})", path, data);
        } catch (KeeperException | InterruptedException e) {
            log.error("create zookeeper node failed.", e);
            throw new RuntimeException("create zookeeper node failed.", e);
        }
    }


}
