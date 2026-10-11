package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.SensitiveWordMapper;
import com.caoim.imcore.dto.PageResult;
import com.caoim.imcore.entity.SensitiveWord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SystemConfigService systemConfigService;
    private volatile List<String> cachedWords = List.of();
    private volatile boolean wordsLoaded;

    public SensitiveWordService(SensitiveWordMapper sensitiveWordMapper, SystemConfigService systemConfigService) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.systemConfigService = systemConfigService;
    }

    public void assertClean(String content) {
        if (!systemConfigService.getBoolean(SystemConfigService.SENSITIVE_FILTER_ENABLED, true)) {
            return;
        }
        if (content == null || content.isBlank()) {
            return;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        for (String word : currentWords()) {
            if (!word.isBlank() && lower.contains(word.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.SENSITIVE_WORD);
            }
        }
    }

    public PageResult<SensitiveWord> page(String keyword, int page, int size) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SensitiveWord::getWord, keyword.trim());
        }
        wrapper.orderByDesc(SensitiveWord::getId);
        return PageResult.of(sensitiveWordMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public SensitiveWord create(String word) {
        String value = normalize(word);
        Long count = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getWord, value));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "敏感词已存在");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(value);
        entity.setEnabled(1);
        sensitiveWordMapper.insert(entity);
        reload();
        return entity;
    }

    public void setEnabled(Long id, Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SensitiveWord entity = require(id);
        entity.setEnabled(enabled);
        sensitiveWordMapper.updateById(entity);
        reload();
    }

    public void delete(Long id) {
        require(id);
        sensitiveWordMapper.deleteById(id);
        reload();
    }

    public void reload() {
        List<SensitiveWord> rows = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getEnabled, 1));
        cachedWords = rows.stream().map(SensitiveWord::getWord).toList();
        wordsLoaded = true;
    }

    private List<String> currentWords() {
        if (!wordsLoaded) {
            reload();
        }
        return cachedWords;
    }

    private SensitiveWord require(Long id) {
        SensitiveWord entity = sensitiveWordMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "敏感词不存在");
        }
        return entity;
    }

    private String normalize(String word) {
        if (word == null || word.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "敏感词不能为空");
        }
        String value = word.trim();
        if (value.length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "敏感词最长 50 个字符");
        }
        return value;
    }
}
