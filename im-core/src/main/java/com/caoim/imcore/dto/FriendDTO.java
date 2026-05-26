package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class FriendDTO {
    private String id;
    private String friendId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;

    public static FriendDTO fromEntity(com.caoim.imcore.entity.Friend friend, com.caoim.imcore.entity.User friendUser) {
        FriendDTO dto = new FriendDTO();
        dto.setId(friend.getId() != null ? friend.getId().toString() : null);
        dto.setFriendId(friend.getFriendId() != null ? friend.getFriendId().toString() : null);
        if (friendUser != null) {
            dto.setUsername(friendUser.getUsername());
            dto.setNickname(friendUser.getNickname());
            dto.setAvatar(friendUser.getAvatar());
        }
        dto.setStatus(friend.getStatus());
        return dto;
    }
}
