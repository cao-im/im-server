package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private String id;
    private String fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private String toUserId;
    private String toUsername;
    private String toNickname;
    private String toAvatar;
    private Integer status;
    private String applyMessage;
    private Integer source;
    private java.time.LocalDateTime createTime;

    public static FriendRequestDTO fromEntity(
            com.caoim.imcore.entity.FriendRequest request,
            com.caoim.imcore.entity.User fromUser,
            com.caoim.imcore.entity.User toUser) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setId(request.getId() != null ? request.getId().toString() : null);
        dto.setFromUserId(request.getFromUserId() != null ? request.getFromUserId().toString() : null);
        dto.setToUserId(request.getToUserId() != null ? request.getToUserId().toString() : null);
        dto.setApplyMessage(request.getApplyMessage());
        dto.setSource(request.getSource());

        if (fromUser != null) {
            dto.setFromUsername(fromUser.getUsername());
            dto.setFromNickname(fromUser.getNickname());
            dto.setFromAvatar(fromUser.getAvatar());
        }

        if (toUser != null) {
            dto.setToUsername(toUser.getUsername());
            dto.setToNickname(toUser.getNickname());
            dto.setToAvatar(toUser.getAvatar());
        }

        dto.setStatus(request.getStatus());
        dto.setCreateTime(request.getCreateTime());
        return dto;
    }
}
