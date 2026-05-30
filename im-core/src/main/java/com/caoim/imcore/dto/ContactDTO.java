package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class ContactDTO {
    private String id;
    private String contactUserId;
    private String username;
    private String nickname;
    private String avatar;
    private String remark;
    private Integer groupId;
    private Integer isTop;
    private Integer isMute;

    public static ContactDTO fromEntity(com.caoim.imcore.entity.Contact contact, com.caoim.imcore.entity.User contactUser) {
        ContactDTO dto = new ContactDTO();
        dto.setId(contact.getId() != null ? contact.getId().toString() : null);
        dto.setContactUserId(contact.getContactUserId() != null ? contact.getContactUserId().toString() : null);
        if (contactUser != null) {
            dto.setUsername(contactUser.getUsername());
            dto.setNickname(contactUser.getNickname());
            dto.setAvatar(contactUser.getAvatar());
        }
        dto.setRemark(contact.getRemark());
        dto.setGroupId(contact.getGroupId());
        dto.setIsTop(contact.getIsTop());
        dto.setIsMute(contact.getIsMute());
        return dto;
    }
}
