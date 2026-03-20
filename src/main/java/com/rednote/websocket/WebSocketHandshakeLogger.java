package com.rednote.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket握手日志处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketHandshakeLogger extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocketHandshakeLogger: channelActive - {}", ctx.channel().id().asShortText());
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("WebSocketHandshakeLogger: channelRead - msg type: {}", msg.getClass().getName());
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("WebSocketHandshakeLogger: userEventTriggered - evt type: {}", evt.getClass().getName());

        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshake =
                (WebSocketServerProtocolHandler.HandshakeComplete) evt;

            log.info("WebSocket handshake completed successfully!");
            log.info("  Channel: {}", ctx.channel().id().asShortText());
            log.info("  Request URI: {}", handshake.requestUri());
            log.info("  Request Headers: {}", handshake.requestHeaders());
            log.info("  Selected Subprotocol: {}", handshake.selectedSubprotocol());
        } else {
            log.info("WebSocketHandshakeLogger: Other event - {}", evt);
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket handshake error: ", cause);
        super.exceptionCaught(ctx, cause);
    }
}
