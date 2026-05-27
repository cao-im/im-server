package com.caoim.imserver;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@MapperScan("com.caoim.imcore.dao")
@ComponentScan(basePackages = {"com.caoim.imserver", "com.caoim.imcore"})
public class ImServerApplication implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ImServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ImServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        org.springframework.core.env.Environment env = event.getApplicationContext().getEnvironment();
        int port = env.getProperty("server.port", Integer.class, 8080);
        String contextPath = normalizeContextPath(env.getProperty("server.servlet.context-path", ""));

        log.info("");
        log.info("========================================");
        log.info("🚀 曹操IM (Cao-IM) Server 启动成功!");
        log.info("========================================");
        log.info("  服务端口: {}", port);
        log.info("  API地址: http://localhost:{}{}", port, contextPath);
        log.info("  WebSocket: ws://localhost:{}{}/ws", port, contextPath);
        log.info("  健康检查: http://localhost:{}{}/health/check", port, contextPath);
        if (isSwaggerEnabled(env)) {
            String swaggerPath = env.getProperty("springdoc.swagger-ui.path", "/swagger-ui/index.html");
            log.info("  Swagger文档: http://localhost:{}{}{}", port, contextPath, swaggerPath);
            log.info("  API文档(JSON): http://localhost:{}{}/api-docs", port, contextPath);
        }
        log.info("========================================");
        log.info("");
        log.info("💡 配置文件加载优先级:");
        log.info("   1. 启动参数（--server.port=xxx）");
        log.info("   2. 环境变量（SERVER_PORT）");
        log.info("   3. 外部配置文件（./config/application.yml）");
        log.info("   4. JAR包内配置（默认值）");
        log.info("");
    }

    /**
     * 规范化 context-path，避免产生双斜杠
     * - "" → ""
     * - "/" → ""
     * - "/api" → "/api"
     */
    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
            return "";
        }
        return contextPath;
    }

    private boolean isSwaggerEnabled(org.springframework.core.env.Environment env) {
        return env.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true);
    }

    private boolean isDevProfile(ApplicationReadyEvent event) {
        String[] activeProfiles = event.getApplicationContext().getEnvironment().getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return true;
    }
}
