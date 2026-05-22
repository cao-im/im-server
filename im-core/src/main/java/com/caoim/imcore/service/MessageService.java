package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.MessageMapper;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public Message sendMessage(Long fromId, Long toId, Long groupId, String content, Integer msgType) {
        // 1. 验证发送者是否存在
        if (!userService.existsById(fromId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "发送者用户不存在: " + fromId);
        }

        // 2. 验证消息内容
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "消息内容不能为空");
        }

        // 3. 根据消息类型验证
        if (groupId != null) {
            // 群聊消息：验证群组是否存在（后续可扩展）
            log.info("发送群聊消息: fromId={}, groupId={}", fromId, groupId);
        } else if (toId != null) {
            // 私聊消息：验证接收者是否存在
            if (!userService.existsById(toId)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "接收者用户不存在: " + toId);
            }
            
            // 不能给自己发消息
            if (fromId.equals(toId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不能给自己发送消息");
            }
            
            log.info("发送私聊消息: fromId={}, toId={}", fromId, toId);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "必须指定接收者(toId)或群组(groupId)");
        }

        // 4. 插入消息记录
        Message message = new Message(fromId, toId, groupId, content, msgType);
        messageMapper.insert(message);

        // 5. 更新会话记录（只在用户存在时更新）
        try {
            if (groupId == null && toId != null) {
                conversationService.updateConversation(fromId, toId, content, Constants.ConversationType.PRIVATE);
                conversationService.updateConversation(toId, fromId, content, Constants.ConversationType.PRIVATE);
            } else if (groupId != null) {
                conversationService.updateGroupConversation(groupId, content);
            }
        } catch (Exception e) {
            log.warn("更新会话记录失败，但不影响消息发送: {}", e.getMessage());
        }

        // 6. 发布消息事件（用于WebSocket推送）
        eventPublisher.publishEvent(new MessageSentEvent(this, message, fromId, toId, groupId));

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
