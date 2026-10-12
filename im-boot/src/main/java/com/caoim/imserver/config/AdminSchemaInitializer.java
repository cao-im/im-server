package com.caoim.imserver.config;

import com.caoim.imcore.service.SensitiveWordService;
import com.caoim.imcore.service.SystemConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(0)
public class AdminSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigService systemConfigService;
    private final SensitiveWordService sensitiveWordService;

    public AdminSchemaInitializer(JdbcTemplate jdbcTemplate,
                                  SystemConfigService systemConfigService,
                                  SensitiveWordService sensitiveWordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.systemConfigService = systemConfigService;
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer enabledColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'im_user' AND COLUMN_NAME = 'enabled'",
                Integer.class);
        if (enabledColumn == null || enabledColumn == 0) {
            jdbcTemplate.execute("ALTER TABLE im_user ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '账号启停: 0-禁用, 1-启用' AFTER status");
        }
        jdbcTemplate.execute("ALTER TABLE im_user MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '在线状态: 0-离线, 1-在线, 2-忙碌'");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS im_sensitive_word (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '敏感词ID',
                    word VARCHAR(50) NOT NULL COMMENT '敏感词',
                    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1-启用, 0-停用',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    UNIQUE KEY uk_word (word)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS im_system_config (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
                    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
                    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
                    remark VARCHAR(200) DEFAULT '' COMMENT '说明',
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    UNIQUE KEY uk_config_key (config_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置'
                """);

        Map<String, String> remarks = Map.of(
                SystemConfigService.REGISTER_ENABLED, "是否允许注册：1-允许，0-关闭",
                SystemConfigService.SENSITIVE_FILTER_ENABLED, "敏感词过滤：1-开启，0-关闭",
                SystemConfigService.MAX_GROUP_MEMBERS, "新建群的成员上限",
                SystemConfigService.RECALL_MINUTES, "消息撤回时限（分钟）"
        );
        for (Map.Entry<String, String> entry : SystemConfigService.defaults().entrySet()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM im_system_config WHERE config_key = ?",
                    Integer.class,
                    entry.getKey());
            if (count == null || count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO im_system_config (config_key, config_value, remark) VALUES (?, ?, ?)",
                        entry.getKey(), entry.getValue(), remarks.getOrDefault(entry.getKey(), ""));
            }
        }
        systemConfigService.reload();
        sensitiveWordService.reload();
    }
}
