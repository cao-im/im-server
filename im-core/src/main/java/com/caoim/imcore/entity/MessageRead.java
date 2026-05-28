package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_message_read")
public class MessageRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long msgId;
    private Long userId;
    private Long groupId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime readTime;
}
