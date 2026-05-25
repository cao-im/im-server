package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_friend")
public class Friend {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long friendId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
