package com.caoim.imcore.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.api.ImService;
import com.caoim.imcore.dto.ContactDTO;
import com.caoim.imcore.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImServiceImpl implements ImService {

    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final FriendRequestService friendRequestService;
    private final ContactService contactService;

    @Override
    public User getUser(Long userId) {
        return userService.getUserInfo(userId);
    }

    @Override
    public User findByUsername(String username) {
        return userService.findByUsername(username);
    }

    @Override
    public Message sendMessage(Long fromId, Long toId, Long groupId, String content, Integer msgType) {
        return messageService.sendMessage(fromId, toId, groupId, content, msgType);
    }

    @Override
    public List<Message> getPrivateHistory(Long userId, Long targetId, int page, int size) {
        Page<Message> result = messageService.getPrivateMessages(userId, targetId, page, size);
        return result.getRecords();
    }

    @Override
    public List<Message> getGroupHistory(Long groupId, int page, int size) {
        Page<Message> result = messageService.getGroupMessages(groupId, page, size);
        return result.getRecords();
    }

    @Override
    public long getUnreadCount(Long userId) {
        return messageService.getUnreadCount(userId);
    }

    @Override
    public Group createGroup(String name, Long ownerId, List<Long> memberIds) {
        return groupService.createGroup(name, ownerId, memberIds);
    }

    @Override
    public List<Group> getUserGroups(Long userId) {
        return groupService.getUserGroups(userId);
    }

    @Override
    public void addGroupMembers(Long groupId, List<Long> userIds) {
        groupService.addGroupMembers(groupId, userIds);
    }

    @Override
    public void removeGroupMember(Long groupId, Long userId) {
        groupService.removeGroupMember(groupId, userId);
    }

    @Override
    public void sendFriendRequest(Long userId, Long friendId) {
        friendRequestService.sendFriendRequest(userId, friendId);
    }

    @Override
    public void acceptFriendRequest(Long userId, Long friendId) {
        friendRequestService.acceptFriendRequest(userId, friendId);
    }

    @Override
    public void rejectFriendRequest(Long userId, Long friendId) {
        friendRequestService.rejectFriendRequest(userId, friendId);
    }

    @Override
    public List<ContactDTO> getContacts(Long userId) {
        return contactService.getContacts(userId);
    }

    @Override
    public void deleteContact(Long userId, Long contactId) {
        contactService.deleteContact(userId, contactId);
    }
}
