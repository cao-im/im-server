package com.caoim.imserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class PortHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PortHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        int localPort = request.getLocalAddress().getPort();
        int configuredPort = PortBindingValidator.getActualPort();

        log.info("🤝 WebSocket 握手请求 - 本地端口: {}, 配置端口: {}", localPort, configuredPort);

        // 记录端口信息到属性中，不再强制拒绝连接
        attributes.put("serverFingerprint", System.getProperty("im.server.fingerprint", "unknown"));
        attributes.put("buildSignature", PortBindingValidator.getBuildSignature());
        attributes.put("protocolVersion", PortBindingValidator.getProtocolVersion());
        attributes.put("confirmedPort", configuredPort);
        attributes.put("actualPort", localPort);

        log.info("✅ WebSocket 握手通过 - 端口: {}", localPort);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("⚠️ WebSocket 握手后发生异常: {}", exception.getMessage());
        }
    }
}
