package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 消息全局唯一ID(雪花算法生成, 0表示待分配) */
    private Long mid = 0L;
    private Long fromId;
    private Long toId;
    private Long groupId;
    private String content;
    private Integer msgType;
    /** 消息阅读状态: 0-未读, 1-已读 */
    private Integer msgStatus;
    /** 送达状态: 0-未送达, 1-已送达（独立于msg_status，发送方视角） */
    private Integer delivered = 0;
    private Long replyMsgId;
    private String atUserIds;
    private String extra;
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
        this.msgStatus = 0;
    }
}
