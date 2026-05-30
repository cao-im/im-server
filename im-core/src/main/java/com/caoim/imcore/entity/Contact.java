package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_contact")
public class Contact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long contactUserId;
    private String remark;
    private Integer groupId;
    private Integer isTop;
    private Integer isMute;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
