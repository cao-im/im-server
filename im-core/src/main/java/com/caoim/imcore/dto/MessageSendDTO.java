package com.caoim.imcore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageSendDTO {
    @NotNull(message = "发送者ID不能为空")
    private Long fromId;
    private Long toId;
    private Long groupId;
    @NotBlank(message = "消息内容不能为空")
    private String content;
    private Integer msgType;
}
