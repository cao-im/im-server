package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.SystemConfigMapper;
import com.caoim.imcore.entity.SystemConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemConfigService {

    public static final String REGISTER_ENABLED = "register_enabled";
    public static final String SENSITIVE_FILTER_ENABLED = "sensitive_filter_enabled";
    public static final String MAX_GROUP_MEMBERS = "max_group_members";
    public static final String RECALL_MINUTES = "recall_minutes";

    private static final Set<String> KEYS = Set.of(
            REGISTER_ENABLED, SENSITIVE_FILTER_ENABLED, MAX_GROUP_MEMBERS, RECALL_MINUTES);

    private final SystemConfigMapper systemConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public SystemConfigService(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getRaw(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public int getInt(String key, int defaultValue) {
        String value = getRaw(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public List<SystemConfig> listAll() {
        ensureLoaded();
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getId));
    }

    public void updateValues(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "配置不能为空");
        }
        ensureLoaded();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!KEYS.contains(key)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "未知配置: " + key);
            }
            validate(key, value);
            SystemConfig existing = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigKey, key));
            if (existing == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "配置不存在: " + key);
            }
            existing.setConfigValue(value);
            systemConfigMapper.updateById(existing);
            cache.put(key, value);
        }
    }

    public void reload() {
        synchronized (this) {
            cache.clear();
            List<SystemConfig> rows = systemConfigMapper.selectList(null);
            for (SystemConfig row : rows) {
                cache.put(row.getConfigKey(), row.getConfigValue());
            }
            loaded = true;
        }
    }

    public static Map<String, String> defaults() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(REGISTER_ENABLED, "1");
        values.put(SENSITIVE_FILTER_ENABLED, "1");
        values.put(MAX_GROUP_MEMBERS, "500");
        values.put(RECALL_MINUTES, "2");
        return values;
    }

    private String getRaw(String key) {
        ensureLoaded();
        return cache.get(key);
    }

    private void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    private void validate(String key, String value) {
        if (REGISTER_ENABLED.equals(key) || SENSITIVE_FILTER_ENABLED.equals(key)) {
            if (!"0".equals(value) && !"1".equals(value)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), key + " 只能是 0 或 1");
            }
            return;
        }
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), key + " 必须是整数");
        }
        if (MAX_GROUP_MEMBERS.equals(key) && (number < 2 || number > 10000)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "群成员上限范围是 2 到 10000");
        }
        if (RECALL_MINUTES.equals(key) && (number < 1 || number > 10080)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "撤回时限范围是 1 到 10080 分钟");
        }
    }
}
