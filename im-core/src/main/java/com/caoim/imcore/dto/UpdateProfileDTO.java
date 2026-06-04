package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String nickname;
    private String avatar;
    private String signature;
    private Integer gender;
    private String birthday;  // 格式: yyyy-MM-dd
    private String location;
    private String phone;
    private String email;
}
