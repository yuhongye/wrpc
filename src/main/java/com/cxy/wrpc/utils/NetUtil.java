package com.cxy.wrpc.utils;

import java.net.InetSocketAddress;
import java.util.Optional;

public final class NetUtil {
    /**
     * @param serverAddress ip:port
     */
    public static Optional<InetSocketAddress> toInetSocketAddress(String serverAddress) {
        String[] parts = serverAddress.split(":");
        if (parts.length == 2) {
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            return Optional.of(new InetSocketAddress(host, port));
        }

        return Optional.empty();
    }

    public static String hostPost(InetSocketAddress address) {
        return address.getHostName() + ":" + address.getPort();
    }
}
