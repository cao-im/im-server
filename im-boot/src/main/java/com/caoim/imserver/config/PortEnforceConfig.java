package com.caoim.imserver.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortEnforceConfig {

    private static final Logger log = LoggerFactory.getLogger(PortEnforceConfig.class);

    @Value("${server.port:8080}")
    private int configuredPort;

    @PostConstruct
    public void printPortInfo() {
        log.info("========================================");
        log.info("✅ 曹操IM (Cao-IM) 服务端启动配置");
        log.info("========================================");
        log.info("服务端监听端口: {}", configuredPort);
        log.info("WebSocket 地址: ws://host:{}/api/ws", configuredPort);
        log.info("REST API 地址: http://host:{}/api", configuredPort);
        log.info("");
        log.info("💡 提示: 端口可通过以下方式修改:");
        log.info("   - application.yml 中的 server.port");
        log.info("   - 启动参数 --server.port=xxxx");
        log.info("   - 环境变量 SERVER_PORT");
        log.info("========================================");
    }
}
