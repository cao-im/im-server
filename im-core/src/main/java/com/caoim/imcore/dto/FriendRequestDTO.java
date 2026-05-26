package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private String id;
    private String userId;
    private String username;
    private String nickname;
    private String avatar;
    private String friendId;
    private String friendUsername;
    private String friendNickname;
    private String friendAvatar;
    private Integer status;
    private java.time.LocalDateTime createTime;

    public static FriendRequestDTO fromEntity(
            com.caoim.imcore.entity.Friend request,
            com.caoim.imcore.entity.User fromUser,
            com.caoim.imcore.entity.User toUser) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setId(request.getId() != null ? request.getId().toString() : null);
        dto.setUserId(request.getUserId() != null ? request.getUserId().toString() : null);
        dto.setFriendId(request.getFriendId() != null ? request.getFriendId().toString() : null);

        if (fromUser != null) {
            dto.setUsername(fromUser.getUsername());
            dto.setNickname(fromUser.getNickname());
            dto.setAvatar(fromUser.getAvatar());
        }

        if (toUser != null) {
            dto.setFriendUsername(toUser.getUsername());
            dto.setFriendNickname(toUser.getNickname());
            dto.setFriendAvatar(toUser.getAvatar());
        }

        dto.setStatus(request.getStatus());
        dto.setCreateTime(request.getCreateTime());
        return dto;
    }
}
