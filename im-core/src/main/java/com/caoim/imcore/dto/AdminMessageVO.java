package com.caoim.imcore.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminMessageVO {
    private Long id;
    private Long mid;
    private Long fromId;
    private String fromName;
    private Long toId;
    private Long groupId;
    private String content;
    private Integer msgType;
    private LocalDateTime createTime;
}
