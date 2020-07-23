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
    private CountDownLatch latch = new CountDownLatch(1);

    private String registryAddress;

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void register(String data) {
        if (data != null) {
            ZooKeeper zk = connectZookeeper();
            addRootNodeIfAbsent(zk);
            createNode(zk, data);
            try {
                zk.close();
            } catch (InterruptedException e) {
                log.error("close zk failed, skip it.", e);
            }
        }
    }

    private ZooKeeper connectZookeeper() {
        try {
            ZooKeeper zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            latch.wait();
            return zk;
        } catch (IOException | InterruptedException e) {
            log.error("connect zookeeper failed.", e);
            throw new RuntimeException("connect zookeeper failed.", e);
        }
    }

    private void addRootNodeIfAbsent(ZooKeeper zk) {
        try {
            Stat s = zk.exists(Constant.ZK_REGISTRY_PATH, false);
            log.info("registry root node does not exist. will create it......");
            if (s == null) {
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
