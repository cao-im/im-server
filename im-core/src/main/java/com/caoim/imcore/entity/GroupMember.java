package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_group_member")
public class GroupMember {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;
}
