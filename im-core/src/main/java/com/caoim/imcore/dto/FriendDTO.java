package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class FriendDTO {
    private Long id;
    private Long friendId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
}
