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
        int expectedPort = PortBindingValidator.getExpectedPort();

        log.info("🤝 WebSocket 握手请求 - 本地端口: {}, 预期端口: {}", localPort, expectedPort);

        if (localPort != expectedPort) {
            log.error("❌ 握手拒绝: 端口不匹配! 客户端连接到端口 {}, 但服务端锁定为 {}",
                localPort, expectedPort);

            response.getHeaders().add("X-IM-Error", "PORT_MISMATCH");
            response.getHeaders().add("X-Expected-Port", String.valueOf(expectedPort));
            response.getHeaders().add("X-Actual-Port", String.valueOf(localPort));

            return false;
        }

        attributes.put("serverFingerprint", System.getProperty("im.server.fingerprint", "unknown"));
        attributes.put("buildSignature", PortBindingValidator.getBuildSignature());
        attributes.put("protocolVersion", PortBindingValidator.getProtocolVersion());
        attributes.put("confirmedPort", expectedPort);

        log.info("✅ WebSocket 握手通过 - 端口验证成功: {}", localPort);
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
