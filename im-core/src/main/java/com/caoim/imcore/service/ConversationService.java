package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.dao.ConversationMapper;
import com.caoim.imcore.dao.GroupMemberMapper;
import com.caoim.imcore.entity.Conversation;
import com.caoim.imcore.entity.GroupMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final GroupMemberMapper groupMemberMapper;

    public List<Conversation> getConversations(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId);
        wrapper.orderByDesc(Conversation::getUpdateTime);
        return conversationMapper.selectList(wrapper);
    }

    public Conversation getConversation(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    public void updateConversation(Long userId, Long targetId, String lastMessage, Integer type) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId);
        wrapper.eq(Conversation::getTargetId, targetId);

        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation != null) {
            conversation.setLastMessage(lastMessage);
            if (!userId.equals(targetId)) {
                conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            }
            conversationMapper.updateById(conversation);
        } else {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setConversationType(type);
            conversation.setLastMessage(lastMessage);
            conversation.setUnreadCount(userId.equals(targetId) ? 0 : 1);
            conversationMapper.insert(conversation);
        }
    }

    public void updateGroupConversation(Long groupId, String lastMessage) {
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);

        for (GroupMember member : members) {
            updateConversation(member.getUserId(), groupId, lastMessage, Constants.ConversationType.GROUP);
        }
    }

    public void clearUnreadCount(Long userId, Long targetId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId);
        wrapper.eq(Conversation::getTargetId, targetId);

        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation != null) {
            conversation.setUnreadCount(0);
            conversationMapper.updateById(conversation);
        }
    }

    public void deleteConversation(Long userId, Long targetId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId);
        wrapper.eq(Conversation::getTargetId, targetId);
        conversationMapper.delete(wrapper);
    }
}
