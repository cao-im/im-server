package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetId;
    private Integer conversationType;
    private Long lastMsgId;
    private String lastMessage;
    private Integer lastMsgType;
    private LocalDateTime lastMsgTime;
    private Integer unreadCount;
    private Integer isTop;
    private Integer isMute;
    private Integer isDeleted;
    private String draftContent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
