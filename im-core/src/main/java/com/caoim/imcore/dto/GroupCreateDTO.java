package com.caoim.imcore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class GroupCreateDTO {
    @NotBlank(message = "群组名称不能为空")
    private String name;
    private String avatar;
    private List<Long> memberIds;
}
