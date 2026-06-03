package com.caoim.imcore.dto;

import lombok.Data;

/**
 * 群组信息快照
 * 在群聊消息中携带群组的名称、头像等信息
 * 用于在会话列表和聊天界面中快速显示群组身份
 */
@Data
public class GroupInfoDTO {
    private Long groupId;
    private String groupName;
    private String groupAvatar;

    public GroupInfoDTO() {
    }

    public GroupInfoDTO(Long groupId, String groupName, String groupAvatar) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupAvatar = groupAvatar;
    }

    /**
     * 获取群组显示名称
     */
    public String getDisplayName() {
        if (groupName != null && !groupName.trim().isEmpty()) {
            return groupName.trim();
        }
        return "群组" + groupId;
    }
}
