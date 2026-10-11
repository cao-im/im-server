package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_sensitive_word")
public class SensitiveWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
