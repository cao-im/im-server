package com.caoim.imserver.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 用于自动填充 createTime 和 updateTime 字段
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充 createTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

        // 插入时也自动填充 updateTime（如果有该字段）
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充 updateTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
