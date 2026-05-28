package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_blacklist")
public class Blacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long blockedUserId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
