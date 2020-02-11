package io.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class KafkaProxyInitializer extends ChannelInitializer<SocketChannel> {

    static final Map<String, KafkaRequest> MESSAGES = new ConcurrentHashMap<>();

    private final String remoteHost;
    private final int remotePort;

    KafkaProxyInitializer(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.INFO),
                new KafkaProxyFrontendHandler(remoteHost, remotePort));
    }
}
