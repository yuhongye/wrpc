package com.cxy.wrpc.registry;

import com.cxy.wrpc.client.ConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 服务发现, 从zk指定的路径中读取服务器地址
 */
@Slf4j
public class ServiceDiscovery {
    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zookeeper;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        zookeeper = connectZookeeper();
        if (zookeeper != null) {
            watchNode();
        }
    }

    private ZooKeeper connectZookeeper() {
        try {
            log.info("will connect zookeeper server: {}", registryAddress);
            ZooKeeper zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                    log.info("connect zookeeper server success.");
                }
            });
            latch.await();
            return zk;
        } catch (IOException | InterruptedException e) {
            log.error("connect zookeeper failed.", e);
            throw new RuntimeException("connect zookeeper failed.", e);
        }
    }

    private void watchNode() {
        try {
            List<String> nodeList = zookeeper.getChildren(Constant.ZK_REGISTRY_PATH, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    watchNode();
                }
            });

            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zookeeper.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                String data = new String(bytes, StandardCharsets.UTF_8);
                log.info("zookeeper node: {}, data: {}", node, data);
                dataList.add(data);
            }

            this.dataList = dataList;
            log.info("Service discovery triggered updating connected server node.");
            updateConnectedServer();
        } catch (InterruptedException | KeeperException e) {
            log.info("zookeeper wath node failed.", e);
        }
    }

    private void updateConnectedServer() {
        ConnectionManager.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop() {
        if (zookeeper != null) {
            try {
                zookeeper.close();
                log.info("zookeeper close success.");
            } catch (InterruptedException e) {
                log.info("close zookeeper failed.", e);
            }
        }
    }
}
