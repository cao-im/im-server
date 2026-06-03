package com.caoim.imcore.api;

import com.caoim.imcore.dto.ContactDTO;
import com.caoim.imcore.entity.*;
import java.util.List;

public interface ImService {
    User getUser(Long userId);
    User findByUsername(String username);
    Message sendMessage(Long fromId, Long toId, Long groupId, String content, Integer msgType);
    List<Message> getPrivateHistory(Long userId, Long targetId, int page, int size);
    List<Message> getGroupHistory(Long groupId, int page, int size);
    long getUnreadCount(Long userId);
    Group createGroup(String name, Long ownerId, List<Long> memberIds);
    List<Group> getUserGroups(Long userId);
    void addGroupMembers(Long groupId, List<Long> userIds);
    void removeGroupMember(Long groupId, Long userId);
    void sendFriendRequest(Long userId, Long friendId);
    void acceptFriendRequest(Long userId, Long friendId);
    void rejectFriendRequest(Long userId, Long friendId);
    List<ContactDTO> getContacts(Long userId);
    void deleteContact(Long userId, Long contactId);
}
