package com.rednote.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP请求日志处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class HttpRequestLogger extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            log.info("HTTP Request received:");
            log.info("  Method: {}", request.method());
            log.info("  URI: {}", request.uri());
            log.info("  Protocol: {}", request.protocolVersion());
            log.info("  Headers: {}", request.headers());
        }

        super.channelRead(ctx, msg);
    }
}
