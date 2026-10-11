package com.caoim.imcore.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("im_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    @JSONField(serialize = false)
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer gender;
    private LocalDate birthday;
    private String location;
    private String phone;
    private String email;
    private Integer onlineStatus;
    private LocalDateTime lastOnlineTime;
    /** 在线状态，沿用既有写入：0 离线，1 在线，2 忙碌 */
    private Integer status;
    /** 账号启停：0 禁用，1 启用。空值视为启用。 */
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    public User() {
    }
}
