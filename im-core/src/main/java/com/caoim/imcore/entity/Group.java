package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_group")
public class Group {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
