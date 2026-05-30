package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.dao.ContactMapper;
import com.caoim.imcore.dao.UserMapper;
import com.caoim.imcore.dto.ContactDTO;
import com.caoim.imcore.entity.Contact;
import com.caoim.imcore.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMapper contactMapper;
    private final UserMapper userMapper;

    public List<ContactDTO> getContacts(Long userId) {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.orderByDesc(Contact::getIsTop);
        wrapper.orderByDesc(Contact::getUpdateTime);
        List<Contact> contacts = contactMapper.selectList(wrapper);

        return contacts.stream().map(contact -> {
            User contactUser = userMapper.selectById(contact.getContactUserId());
            return ContactDTO.fromEntity(contact, contactUser);
        }).toList();
    }

    public boolean isContact(Long userId, Long contactUserId) {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.eq(Contact::getContactUserId, contactUserId);
        return contactMapper.selectCount(wrapper) > 0;
    }

    public void updateRemark(Long userId, Long contactUserId, String remark) {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.eq(Contact::getContactUserId, contactUserId);
        Contact contact = contactMapper.selectOne(wrapper);

        if (contact != null) {
            contact.setRemark(remark);
            contactMapper.updateById(contact);
        }
    }

    public void deleteContact(Long userId, Long contactUserId) {
        LambdaQueryWrapper<Contact> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Contact::getUserId, userId);
        wrapper1.eq(Contact::getContactUserId, contactUserId);
        Contact contact1 = contactMapper.selectOne(wrapper1);
        if (contact1 != null) {
            contactMapper.deleteById(contact1.getId());
        }

        LambdaQueryWrapper<Contact> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Contact::getUserId, contactUserId);
        wrapper2.eq(Contact::getContactUserId, userId);
        Contact contact2 = contactMapper.selectOne(wrapper2);
        if (contact2 != null) {
            contactMapper.deleteById(contact2.getId());
        }
    }

    public void setTop(Long userId, Long contactUserId, boolean isTop) {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.eq(Contact::getContactUserId, contactUserId);
        Contact contact = contactMapper.selectOne(wrapper);

        if (contact != null) {
            contact.setIsTop(isTop ? 1 : 0);
            contactMapper.updateById(contact);
        }
    }

    public void setMute(Long userId, Long contactUserId, boolean isMute) {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.eq(Contact::getContactUserId, contactUserId);
        Contact contact = contactMapper.selectOne(wrapper);

        if (contact != null) {
            contact.setIsMute(isMute ? 1 : 0);
            contactMapper.updateById(contact);
        }
    }
}
