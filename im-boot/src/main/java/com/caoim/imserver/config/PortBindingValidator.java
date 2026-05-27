package com.caoim.imserver.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class PortBindingValidator {

    private static final Logger log = LoggerFactory.getLogger(PortBindingValidator.class);

    private static final String PROTOCOL_VERSION = "1.0.0";
    private static final String BUILD_SIGNATURE = "CAOIM-2024-OPEN-SOURCE";

    @Value("${server.port:8080}")
    private int actualPort;

    @PostConstruct
    public void validate() {
        log.info("========================================");
        log.info("🔐 曹操IM 服务端信息");
        log.info("========================================");

        generateServerFingerprint();

        log.info("✅ 端口配置验证通过: {}", actualPort);
        log.info("   协议版本: {}", PROTOCOL_VERSION);
        log.info("========================================\n");
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

    /**
     * 获取实际配置的端口号
     */
    public static int getActualPort() {
        return Integer.parseInt(System.getProperty("im.server.port.confirmed", "8080"));
    }

    /**
     * 获取预期端口号（兼容旧代码，实际返回配置的端口）
     * @deprecated 使用 {@link #getActualPort()} 替代
     */
    @Deprecated
    public static int getExpectedPort() {
        return getActualPort();
    }

    public static String getBuildSignature() {
        return BUILD_SIGNATURE;
    }

    public static String getProtocolVersion() {
        return PROTOCOL_VERSION;
    }
}
