package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_group_mute")
public class GroupMute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
