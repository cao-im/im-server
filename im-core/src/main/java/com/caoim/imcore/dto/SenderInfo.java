package com.caoim.imcore.dto;

import lombok.Data;

/**
 * 发送者信息快照
 * 在消息发送时，将发送者的昵称、头像等信息快照存入消息体
 * 这样接收方即使没有本地缓存也能正常显示发送者的身份信息
 */
@Data
public class SenderInfo {
    private Long userId;
    private String nickname;
    private String avatar;
    /**
     * 群昵称（仅在群聊消息中有效）
     * 用户在特定群组中设置的群内显示名称
     */
    private String groupNickname;

    public SenderInfo() {
    }

    public SenderInfo(Long userId, String nickname, String avatar) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
    }

    public SenderInfo(Long userId, String nickname, String avatar, String groupNickname) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.groupNickname = groupNickname;
    }

    /**
     * 检查是否有有效的显示名称
     */
    public boolean hasDisplayName() {
        return nickname != null && !nickname.trim().isEmpty();
    }

    /**
     * 获取最佳显示名称：优先使用群昵称，其次使用昵称
     */
    public String getBestDisplayName() {
        if (groupNickname != null && !groupNickname.trim().isEmpty()) {
            return groupNickname.trim();
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname.trim();
        }
        return "用户" + userId;
    }
}
