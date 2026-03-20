package com.rednote.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * WebSocket服务器
 */
@Slf4j
@Component
public class WebSocketServer {

    @Value("${websocket.port:8081}")
    private int port;

    @Value("${websocket.path:/ws}")
    private String path;

    @Autowired
    private WebSocketServerHandler webSocketServerHandler;

    @Autowired
    private WebSocketHandshakeLogger handshakeLogger;

    @Autowired
    private HttpRequestLogger httpRequestLogger;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                // 异常处理器（放在第一位，捕获所有异常）
                                pipeline.addLast(new ExceptionHandler());
                                // HTTP编解码
                                pipeline.addLast(new HttpServerCodec());
                                // HTTP消息聚合
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                // HTTP请求日志
                                pipeline.addLast(httpRequestLogger);
                                // 支持大文件传输
                                pipeline.addLast(new ChunkedWriteHandler());
                                // WebSocket协议处理 - 允许子路径匹配以支持 query string
                                pipeline.addLast(new WebSocketServerProtocolHandler(path, null, true, 65536, false, true));
                                // WebSocket握手日志
                                pipeline.addLast(handshakeLogger);
                                // 自定义业务处理器
                                pipeline.addLast(webSocketServerHandler);
                            }
                        });

                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("WebSocket server started on port: {}, path: {}", port, path);
                future.channel().closeFuture().sync();

            } catch (Exception e) {
                log.error("WebSocket server error", e);
            } finally {
                shutdown();
            }
        }, "WebSocket-Server-Thread").start();
    }

    @PreDestroy
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("WebSocket server stopped");
    }
}
