package com.caoim.imserver.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;

@Component
public class PortBindingValidator {

    private static final Logger log = LoggerFactory.getLogger(PortBindingValidator.class);

    private static final int EXPECTED_PORT = 80;
    private static final String PROTOCOL_VERSION = "1.0.0";
    private static final String BUILD_SIGNATURE = "CAOIM-2024-80-LOCKED";

    @Value("${server.port:80}")
    private int actualPort;

    @PostConstruct
    public void validate() {
        log.info("========================================");
        log.info("🔒 曹操IM 端口绑定验证器启动");
        log.info("========================================");

        boolean isValid = performValidation();

        if (!isValid) {
            log.error("");
            log.error("╔══════════════════════════════════════════╗");
            log.error("║  ❌ 致命错误: 端口绑定验证失败！          ║");
            log.error("╠══════════════════════════════════════════╣");
            log.error("║                                          ║");
            log.error("║  检测到以下问题:                         ║");
            log.error("║  - 服务端端口被修改为: {}              ", actualPort);
            log.error("║  - 预期端口应为: {}                    ", EXPECTED_PORT);
            log.error("║                                          ║");
            log.error("║  后果:                                   ║");
            log.error("║  - SDK 客户端将无法连接                  ║");
            log.error("║  - WebSocket 握手会失败                  ║");
            log.error("║  - 所有 IM 功能不可用                    ║");
            log.error("║                                          ║");
            log.error("║  解决方案:                               ║");
            log.error("║  1. 恢复端口配置为 {}                   ", EXPECTED_PORT);
            log.error("║  2. 或同步修改 SDK 源码并重新编译        ║");
            log.error("║  3. 或使用官方预编译版本                 ║");
            log.error("║                                          ║");
            log.error("╚══════════════════════════════════════════╝");
            log.error("");

            throw new IllegalStateException(
                String.format(
                    "端口绑定验证失败! 实际端口: %d, 预期端口: %d. " +
                    "服务将无法正常工作，因为 SDK 客户端期望连接到端口 %d",
                    actualPort, EXPECTED_PORT, EXPECTED_PORT
                )
            );
        }

        generateServerFingerprint();
        log.info("✅ 端口绑定验证通过: {}", actualPort);
        log.info("========================================\n");
    }

    private boolean performValidation() {
        return actualPort == EXPECTED_PORT;
    }

    private void generateServerFingerprint() {
        try {
            String rawData = String.format("%s:%d:%s:%s",
                BUILD_SIGNATURE,
                actualPort,
                PROTOCOL_VERSION,
                Instant.now().toString()
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes());
            String fingerprint = Base64.getEncoder().encodeToString(hash);

            System.setProperty("im.server.fingerprint", fingerprint);
            System.setProperty("im.server.port.confirmed", String.valueOf(actualPort));

            log.info("🔐 服务端指纹已生成: {}...", fingerprint.substring(0, 16));
        } catch (Exception e) {
            log.warn("⚠️ 无法生成服务器指纹: {}", e.getMessage());
        }
    }

    public static int getExpectedPort() {
        return EXPECTED_PORT;
    }

    public static String getBuildSignature() {
        return BUILD_SIGNATURE;
    }

    public static String getProtocolVersion() {
        return PROTOCOL_VERSION;
    }
}
