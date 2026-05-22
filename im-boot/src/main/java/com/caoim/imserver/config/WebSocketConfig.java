package com.caoim.imserver.config;

import com.caoim.imserver.websocket.IMWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final IMWebSocketHandler imWebSocketHandler;
    private final PortHandshakeInterceptor portHandshakeInterceptor;

    public WebSocketConfig(IMWebSocketHandler imWebSocketHandler,
                          PortHandshakeInterceptor portHandshakeInterceptor) {
        this.imWebSocketHandler = imWebSocketHandler;
        this.portHandshakeInterceptor = portHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(imWebSocketHandler, "/ws")
                .addInterceptors(portHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
