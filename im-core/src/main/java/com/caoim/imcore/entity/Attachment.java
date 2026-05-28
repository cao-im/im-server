package com.caoim.imcore.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_attachment")
public class Attachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long msgId;
    private String fileName;
    private String fileUrl;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileExt;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String extra;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
