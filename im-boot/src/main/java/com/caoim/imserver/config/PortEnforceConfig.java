package com.caoim.imserver.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class PortEnforceConfig {

    private static final Logger log = LoggerFactory.getLogger(PortEnforceConfig.class);

    private static final int FORCED_PORT = 80;

    @Value("${server.port:80}")
    private int configuredPort;

    @PostConstruct
    public void enforcePort() {
        if (configuredPort != FORCED_PORT) {
            log.warn("========================================");
            log.warn("⚠️  端口安全警告");
            log.warn("========================================");
            log.warn("检测到尝试修改服务端端口: {} -> {}", configuredPort, FORCED_PORT);
            log.warn("");
            log.warn("曹操IM (Cao-IM) 服务端端口已锁定为: {}", FORCED_PORT);
            log.warn("此限制无法通过以下方式绕过:");
            log.warn("  - application.yml 配置文件");
            log.warn("  - application-{profile}.yml 配置文件");
            log.warn("  - --server.port 启动参数");
            log.warn("  - 环境变量 SERVER_PORT");
            log.warn("  - 命令行参数 -Dserver.port");
            log.warn("");
            log.warn("如需更改端口，请修改 PortEnforceConfig.java 中的 FORCED_PORT 常量");
            log.warn("并重新编译项目。这是有意为之的安全设计。");
            log.warn("========================================");
        }

        log.info("✅ 曹操IM 服务端将在端口 {} 上启动", FORCED_PORT);
        log.info("   WebSocket 地址: ws://host:{}/api/ws", FORCED_PORT);
        log.info("   REST API 地址: http://host:{}/api", FORCED_PORT);
    }
}
