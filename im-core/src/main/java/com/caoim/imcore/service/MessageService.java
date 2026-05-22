package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.dao.MessageMapper;
import com.caoim.imcore.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final ConversationService conversationService;

    public Message sendMessage(Long fromId, Long toId, Long groupId, String content, Integer msgType) {
        Message message = new Message(fromId, toId, groupId, content, msgType);
        messageMapper.insert(message);

        if (groupId == null) {
            conversationService.updateConversation(fromId, toId, content, Constants.ConversationType.PRIVATE);
            conversationService.updateConversation(toId, fromId, content, Constants.ConversationType.PRIVATE);
        } else {
            conversationService.updateGroupConversation(groupId, content);
        }

        return message;
    }

    public Page<Message> getPrivateMessages(Long userId, Long targetId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getFromId, userId).eq(Message::getToId, targetId)
                .or().eq(Message::getFromId, targetId).eq(Message::getToId, userId));
        wrapper.orderByAsc(Message::getCreateTime);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<Message> getGroupMessages(Long groupId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getGroupId, groupId);
        wrapper.orderByAsc(Message::getCreateTime);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<Message> getRecentMessages(Long userId, int limit) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getFromId, userId).or().eq(Message::getToId, userId));
        wrapper.orderByDesc(Message::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return messageMapper.selectList(wrapper);
    }

    public void markAsRead(List<Long> messageIds) {
        if (messageIds != null && !messageIds.isEmpty()) {
            for (Long messageId : messageIds) {
                Message message = new Message();
                message.setId(messageId);
                message.setStatus(Constants.MessageStatus.READ);
                messageMapper.updateById(message);
            }
        }
    }

    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getToId, userId);
        wrapper.eq(Message::getStatus, Constants.MessageStatus.UNREAD);
        return messageMapper.selectCount(wrapper);
    }
}
