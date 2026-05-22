package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromId;
    private Long toId;
    private Long groupId;
    private String content;
    private Integer msgType;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Message() {
    }

    public Message(Long fromId, Long toId, Long groupId, String content, Integer msgType) {
        this.fromId = fromId;
        this.toId = toId;
        this.groupId = groupId;
        this.content = content;
        this.msgType = msgType;
        this.status = 0;
    }
}
