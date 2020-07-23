package com.cxy.wrpc.server;

import com.cxy.wrpc.annotations.RpcService;
import com.cxy.wrpc.protocol.RpcDecoder;
import com.cxy.wrpc.protocol.RpcEncoder;
import com.cxy.wrpc.protocol.RpcRequest;
import com.cxy.wrpc.registry.ServiceRegistry;
import com.cxy.wrpc.serializer.Serializer;
import com.cxy.wrpc.utils.BeanFactory;
import com.cxy.wrpc.utils.GlobleExecutor;
import com.cxy.wrpc.utils.NetUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 当调用{@link #start(String)}方法是会扫描所有使用{@link RpcService}注解的类，并注册成service
 * 也可以使用{@link #registerServiceManual(String, Object)} ()}来手动添加service
 */
@Slf4j
public class RpcServer {

    private InetSocketAddress serverAddress;
    private ServiceRegistry registry;

    private Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
    private AtomicBoolean started = new AtomicBoolean(false);

    public RpcServer(String serverAddress, ServiceRegistry registry) {
        this.serverAddress = NetUtil.toInetSocketAddress(serverAddress).get();
        this.registry = Objects.requireNonNull(registry);
    }

    public void start(String scanServiceBase) throws InterruptedException {
        if (! started.compareAndSet(false, true)) {
            throw new RuntimeException("Server already started.");
        }

        scanServiceThenRegister(scanServiceBase);
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                .addLast(new RpcDecoder<>(RpcRequest.class, Serializer.create()))
                                .addLast(new RpcHandler(serviceMap))
                                .addLast(new RpcEncoder(Serializer.create()));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = bootstrap.bind(serverAddress).sync();
        log.info("Server start on port: {}", serverAddress.getPort());

        registry.register(NetUtil.hostPost(serverAddress));
        log.info("register {} success.", serverAddress);

        future.channel().closeFuture().sync();
    }

    private void scanServiceThenRegister(String scanServiceBase) {
        List<Object> services = BeanFactory.getBeanWithAnnotation(scanServiceBase, RpcService.class);
        services.forEach(service -> {
            String serviceName = service.getClass().getAnnotation(RpcService.class).value().getName();
            log.info("Loading service: {}", serviceName);
            serviceMap.put(serviceName, service);
        });
    }

    public RpcServer registerServiceManual(String serviceName, Object service) {
        serviceMap.computeIfAbsent(serviceName, name -> {
            log.info("Loading service: {}", name);
            return service;
        });
        return this;
    }

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        GlobleExecutor.stop();
    }
}
