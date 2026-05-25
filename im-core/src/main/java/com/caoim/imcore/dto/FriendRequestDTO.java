package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long friendId;
    private String friendUsername;
    private String friendNickname;
    private String friendAvatar;
    private Integer status;
    private java.time.LocalDateTime createTime;
}
