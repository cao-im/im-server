package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer mute;
    private Long lastReadMsgId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;
    private Long inviteUserId;
}
