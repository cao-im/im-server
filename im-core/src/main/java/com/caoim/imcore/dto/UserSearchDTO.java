package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class UserSearchDTO {
    private String id;           // 使用String避免前端精度丢失
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private String imUserId;     // IM系统用户ID

    public static UserSearchDTO fromEntity(com.caoim.imcore.entity.User user) {
        UserSearchDTO dto = new UserSearchDTO();
        dto.setId(user.getId() != null ? user.getId().toString() : null);
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
