package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_group")
public class Group {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private String introduction;
    private String notice;
    private Long ownerId;
    private Integer maxMembers;
    private Integer joinType;
    private Integer muteAll;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
