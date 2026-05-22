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

    private static final int FORCED_PORT = 80;

    public static void main(String[] args) {
        enforcePortBeforeStartup(args);
        SpringApplication.run(ImServerApplication.class, args);
    }

    private static void enforcePortBeforeStartup(String[] args) {
        System.setProperty("server.port", String.valueOf(FORCED_PORT));

        boolean portOverrideDetected = false;
        for (String arg : args) {
            if (arg.contains("server.port") || arg.contains("PORT=")) {
                portOverrideDetected = true;
                break;
            }
        }

        if (portOverrideDetected || System.getenv("SERVER_PORT") != null) {
            log.warn("");
            log.warn("╔════════════════════════════════════════════════════════════╗");
            log.warn("║           ⚠️  曹操IM 端口安全锁定警告                      ║");
            log.warn("╠════════════════════════════════════════════════════════════╣");
            log.warn("║                                                            ║");
            log.warn("║  检测到尝试通过以下方式修改服务端端口:                     ║");
            if (portOverrideDetected) {
                log.warn("║  ✗ 启动参数 --server.port 或 -Dserver.port              ║");
            }
            if (System.getenv("SERVER_PORT") != null) {
                log.warn("║  ✗ 环境变量 SERVER_PORT={}", System.getenv("SERVER_PORT"));
            }
            log.warn("║                                                            ║");
            log.warn("║  → 端口已强制锁定为: {}                                  ", FORCED_PORT);
            log.warn("║                                                            ║");
            log.warn("║  此为安全设计，防止意外更改导致客户端连接失败。          ║");
            log.warn("║                                                            ║");
            log.warn("║  如需修改端口（不推荐）:                                 ║");
            log.warn("║  1. 编辑 ImServerApplication.java                        ║");
            log.warn("║  2. 修改 FORCED_PORT 常量值                               ║");
            log.warn("║  3. 同步修改 SDK 中的 _fixedPort 常量                    ║");
            log.warn("║  4. 重新编译整个项目                                      ║");
            log.warn("║                                                            ║");
            log.warn("╚════════════════════════════════════════════════════════════╝");
            log.warn("");
        }

        log.info("🔒 曹操IM 服务端端口已锁定: {}", FORCED_PORT);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("");
        log.info("========================================");
        log.info("🚀 曹操IM (Cao-IM) Server 启动成功!");
        log.info("========================================");
        log.info("  服务端口: {}", FORCED_PORT);
        log.info("  API地址: http://localhost:{}/api", FORCED_PORT);
        log.info("  WebSocket: ws://localhost:{}/api/ws", FORCED_PORT);
        log.info("  健康检查: http://localhost:{}/api/health/check", FORCED_PORT);
        if (isDevProfile(event)) {
            log.info("  Swagger文档: http://localhost:{}/api/swagger-ui.html", FORCED_PORT);
        }
        log.info("========================================");
        log.info("");
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
