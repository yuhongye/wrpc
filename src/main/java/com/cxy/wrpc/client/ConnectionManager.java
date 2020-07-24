package com.cxy.wrpc.client;

import com.cxy.wrpc.utils.NetUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 管理和server的连接，server有可能会定期更新
 * 注意并发的正确性
 */
@Slf4j
public class ConnectionManager {
    private static ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(4, 4, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private List<RpcClientHandler> connectedServerNodes = new ArrayList<>();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private Condition hasConnection = writeLock.newCondition();
    private volatile boolean isRunning = true;

    private long connectedTimeoutMS = 6000;
    private AtomicLong roundRobin = new AtomicLong(0);

    private ConnectionManager() { }

    public void updateConnectedServer(List<String> allServerAddress) {
        writeLock.lock();
        try {
            updateConnectedServer0(allServerAddress);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 传进来最新的服务地址:
     *  - 不在该地址中的server全部删掉
     *  - 对于尚未建立连接的server建立连接
     *
     * @param allServerAddress 最新的服务地址
     */
    private void updateConnectedServer0(List<String> allServerAddress) {
        if (allServerAddress == null) {
            return;
        }

        log.info("update connected server: {}", allServerAddress);
        if (allServerAddress.isEmpty()) {
            clearConnectedInfo();
            return;
        }

        // 解析成规范的地址表示形式
        Set<InetSocketAddress> newAllServerNodeSet = allServerAddress.stream().map(NetUtil::toInetSocketAddress)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        Set<InetSocketAddress> existAddresses = connectedServerNodes.stream()
                .map(RpcClientHandler::getRemotePeer)
                .collect(Collectors.toSet());

        // 对于尚未连接的server 建立连接
        newAllServerNodeSet.stream().filter(addr -> !existAddresses.contains(addr))
                .forEach(this::connectServerNode);

        // 删除过期的server
        Iterator<RpcClientHandler> it = connectedServerNodes.iterator();
        while (it.hasNext()) {
            RpcClientHandler handler = it.next();
            if (!newAllServerNodeSet.contains(handler.getRemotePeer())) {
                it.remove();
                log.info("remove invalid server: {}", handler.getRemotePeer());
                handler.close();
            }
        }
    }

    /**
     * 没有可用的server节点，所有的server节点都down了，清楚connectedHandler 和 connectedServerNodes
     */
    private void clearConnectedInfo() {
        log.error("No available server node. All server nodes are down!");
        connectedServerNodes.forEach(RpcClientHandler::close);
        connectedServerNodes.clear();
    }

    private void connectServerNode(final InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());

            ChannelFuture channelFuture = b.connect(remotePeer);
            channelFuture.addListener(future -> {
                if (channelFuture.isSuccess()) {
                    log.info("Successfully connect to remote server: {}", remotePeer);
                    RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                    addHandler(handler);
                }
            });
        });
    }

    private void addHandler(RpcClientHandler handler) {
        writeLock.lock();
        try {
            connectedServerNodes.add(handler);
            signalAailableHandler();
        } finally {
            writeLock.unlock();
        }
    }

    private void signalAailableHandler() {
        writeLock.lock();
        try {
            hasConnection.signalAll();
        } finally {
            writeLock.unlock();
        }
    }

    // todo: 优化
    public RpcClientHandler chooseHandler() {
        while (true) {
            while (isRunning && connectedServerNodes.isEmpty()) {
                boolean hasHandler = waitingForHandler();
                log.info("wait for handler finish, has handler? {}", hasHandler);
            }

            readLock.lock();
            try {
                int size = connectedServerNodes.size();
                if (size > 0) {
                    int index = (int) ((roundRobin.getAndIncrement() + size) % size);
                    log.info("choose server: {}", connectedServerNodes.get(index));
                    return connectedServerNodes.get(index);
                }
            } finally {
                readLock.unlock();
            }
        }
    }

    /**
     * 阻塞直到有可用的连接
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForHandler() {
        writeLock.lock();
        try {
            log.info("time wait for available server.");
            return hasConnection.await(connectedTimeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Waiting for available node is interrupted!", e);
            throw new RuntimeException("Can not connect any server!", e);
        } finally {
            writeLock.unlock();
        }
    }

    public void stop() {
        isRunning = false;
        writeLock.lock();
        try {
            connectedServerNodes.forEach(RpcClientHandler::close);
            connectedServerNodes.clear();
        } finally {
            writeLock.unlock();
        }

        signalAailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }


    public static ConnectionManager getInstance() {
        return ConnectionManageHolder.INSTANCE;
    }

    private static class ConnectionManageHolder {
        final static ConnectionManager INSTANCE = new ConnectionManager();
    }
}
