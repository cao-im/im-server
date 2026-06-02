package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.MessageMapper;
import com.caoim.imcore.dao.MessageReadMapper;
import com.caoim.imcore.dao.ConversationMapper;
import com.caoim.imcore.entity.Conversation;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.entity.MessageRead;
import com.caoim.imcore.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final MessageReadMapper messageReadMapper;
    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public Message sendMessage(Long fromId, Long toId, Long groupId, String content, Integer msgType) {
        if (!userService.existsById(fromId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "发送者用户不存在: " + fromId);
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "消息内容不能为空");
        }

        if (groupId != null) {
            log.info("发送群聊消息: fromId={}, groupId={}", fromId, groupId);
        } else if (toId != null) {
            if (!userService.existsById(toId)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "接收者用户不存在: " + toId);
            }
            
            if (fromId.equals(toId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不能给自己发送消息");
            }
            
            log.info("发送私聊消息: fromId={}, toId={}", fromId, toId);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "必须指定接收者(toId)或群组(groupId)");
        }

        Long conversationId = null;

        try {
            if (groupId == null && toId != null) {
                conversationId = getOrCreatePrivateConversation(fromId, toId, content);

                Long receiverConvId = getOrCreatePrivateConversation(toId, fromId, content);

                if (receiverConvId != null && !receiverConvId.equals(conversationId)) {
                    updateUnreadCount(receiverConvId);
                }
            } else if (groupId != null) {
                conversationId = getOrCreateGroupConversation(groupId, content);
            }
        } catch (Exception e) {
            log.warn("获取/更新会话失败，但不影响消息发送: {}", e.getMessage());
        }

        Message message = new Message(fromId, toId, groupId, conversationId, content, msgType);
        messageMapper.insert(message);

        eventPublisher.publishEvent(new MessageSentEvent(this, message, fromId, toId, groupId));

        return message;
    }

    /**
     * 根据会话ID查询消息（包含完整对话：双方发送的所有消息）
     * 
     * 设计说明：
     * - 虽然会话表有2条记录（A视角和B视角），但消息是共享的
     * - 查询时根据 conversation_id 找到对应的 user_pair
     * - 然后查询该 user_pair 的所有消息（from→to 和 to→from）
     */
    public Page<Message> getMessagesByConversationId(Long conversationId, int page, int size) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            log.warn("会话不存在: conversationId={}", conversationId);
            return new Page<>(page, size);
        }
        
        Long userId = conv.getUserId();
        Long targetId = conv.getTargetId();
        
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> 
            w.eq(Message::getFromId, userId).eq(Message::getToId, targetId)
             .or()
             .eq(Message::getFromId, targetId).eq(Message::getToId, userId)
        );
        wrapper.orderByAsc(Message::getCreateTime);
        
        log.debug("查询会话完整消息: conversationId={}, userId={}, targetId={}, page={}, size={}", 
                conversationId, userId, targetId, page, size);
        
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 私聊消息查询（内部转换为新方式）
     */
    public Page<Message> getPrivateMessages(Long userId, Long targetId, int page, int size) {
        Long conversationId = findConversationId(userId, targetId, Constants.ConversationType.PRIVATE);
        
        if (conversationId != null) {
            return getMessagesByConversationId(conversationId, page, size);
        } else {
            log.warn("未找到会话记录: userId={}, targetId={}, 使用兼容模式", userId, targetId);
            return getPrivateMessagesLegacy(userId, targetId, page, size);
        }
    }

    /**
     * 群聊消息查询
     */
    public Page<Message> getGroupMessages(Long groupId, int page, int size) {
        Long conversationId = findGroupConversationId(groupId);
        
        if (conversationId != null) {
            return getMessagesByConversationId(conversationId, page, size);
        } else {
            log.warn("未找到群会话记录: groupId={}, 使用兼容模式", groupId);
            return getGroupMessagesLegacy(groupId, page, size);
        }
    }

    public List<Message> getRecentMessages(Long userId, int limit) {
        List<Long> conversationIds = getUserConversationIds(userId);
        
        if (conversationIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Message::getConversationId, conversationIds);
        wrapper.orderByDesc(Message::getCreateTime);
        wrapper.last("LIMIT " + limit);
        
        return messageMapper.selectList(wrapper);
    }

    @Transactional
    public void markAsRead(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty() || userId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<MessageRead> readRecords = new ArrayList<>();

        for (Long messageId : messageIds) {
            Message message = new Message();
            message.setId(messageId);
            message.setMsgStatus(Constants.MessageStatus.READ);
            messageMapper.updateById(message);

            LambdaQueryWrapper<MessageRead> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(MessageRead::getMsgId, messageId)
                     .eq(MessageRead::getUserId, userId);
            
            Long count = messageReadMapper.selectCount(existCheck);
            if (count == 0) {
                MessageRead readRecord = new MessageRead();
                readRecord.setMsgId(messageId);
                readRecord.setUserId(userId);
                readRecord.setReadTime(now);
                readRecords.add(readRecord);
            }
        }

        if (!readRecords.isEmpty()) {
            for (MessageRead record : readRecords) {
                messageReadMapper.insert(record);
            }
            log.info("标记消息已读: userId={}, 消息数={}, 回执数={}", 
                    userId, messageIds.size(), readRecords.size());
        }
    }

    public Long getUnreadCount(Long userId) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) FROM im_message m ");
        sqlBuilder.append("WHERE m.conversation_id IN (");
        sqlBuilder.append("  SELECT c.id FROM im_conversation c WHERE c.user_id = {0}");
        sqlBuilder.append(")");
        sqlBuilder.append("AND m.msg_status = {1} ");

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(Constants.MessageStatus.UNREAD);

        return messageMapper.selectCount(
            new LambdaQueryWrapper<Message>().apply(sqlBuilder.toString(), params.toArray())
        );
    }

    /**
     * 查询离线消息数量（用于分页判断是否还有更多）
     */
    public Long getOfflineMessagesCount(Long userId, Long sinceTimestamp, Long sinceMessageId) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) FROM im_message m ");
        sqlBuilder.append("WHERE m.to_id = {0} ");
        sqlBuilder.append("AND m.msg_status = {1} ");

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(Constants.MessageStatus.UNREAD);

        if (sinceTimestamp != null && sinceTimestamp > 0) {
            LocalDateTime sinceTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(sinceTimestamp),
                java.time.ZoneId.systemDefault()
            );
            sqlBuilder.append(" AND m.create_time >= {2} ");
            params.add(sinceTime);
        }

        if (sinceMessageId != null && sinceMessageId > 0) {
            sqlBuilder.append(" AND m.id > {3} ");
            params.add(sinceMessageId);
        }

        return messageMapper.selectCount(
            new LambdaQueryWrapper<Message>().apply(sqlBuilder.toString(), params.toArray())
        );
    }

    public List<Message> getOfflineMessages(Long userId, Long sinceTimestamp, Long sinceMessageId, 
                                           int offset, int limit) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT m.* FROM im_message m ");
        sqlBuilder.append("WHERE m.to_id = {0} ");
        sqlBuilder.append("AND m.msg_status = {1} ");
        
        sqlBuilder.append("AND m.id NOT IN (");
        sqlBuilder.append("  SELECT mr.msg_id FROM im_message_read mr ");
        sqlBuilder.append("  WHERE mr.user_id = {0}");
        sqlBuilder.append(")");
        
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(Constants.MessageStatus.UNREAD);

        if (sinceTimestamp != null && sinceTimestamp > 0) {
            LocalDateTime sinceTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(sinceTimestamp),
                java.time.ZoneId.systemDefault()
            );
            sqlBuilder.append(" AND m.create_time >= {1} ");
            params.add(sinceTime);
        }

        if (sinceMessageId != null && sinceMessageId > 0) {
            sqlBuilder.append(" AND m.id > {2} ");
            params.add(sinceMessageId);
        }

        sqlBuilder.append(" ORDER BY m.id ASC ");
        sqlBuilder.append(" LIMIT {3}, {4} ");
        params.add(offset);
        params.add(limit);

        List<Message> messages = messageMapper.selectList(
            new LambdaQueryWrapper<Message>().apply(sqlBuilder.toString(), params.toArray())
        );

        log.info("查询离线消息: userId={}, 结果数={}", userId, messages.size());

        return messages;
    }

    // ==================== 私有辅助方法 ====================

    private Long getOrCreatePrivateConversation(Long userId, Long targetId, String lastMessage) {
        Conversation conv = findOrCreateConversation(userId, targetId, 
            Constants.ConversationType.PRIVATE, lastMessage);
        return conv != null ? conv.getId() : null;
    }

    private Long getOrCreateGroupConversation(Long groupId, String lastMessage) {
        conversationService.updateGroupConversation(groupId, lastMessage);
        
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getTargetId, groupId)
               .eq(Conversation::getConversationType, Constants.ConversationType.GROUP)
               .last("LIMIT 1");
        
        Conversation conv = conversationMapper.selectOne(wrapper);
        
        return conv != null ? conv.getId() : null;
    }

    private Conversation findOrCreateConversation(Long userId, Long targetId, Integer type, 
                                                String lastMessage) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId);
        wrapper.eq(Conversation::getTargetId, targetId);
        wrapper.eq(Conversation::getConversationType, type);

        Conversation conversation = conversationMapper.selectOne(wrapper);
        
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setConversationType(type);
            conversation.setLastMessage(lastMessage);
            conversation.setUnreadCount(0);
            conversationMapper.insert(conversation);
            log.info("创建新会话: id={}, userId={}, targetId={}, type={}", 
                    conversation.getId(), userId, targetId, type);
        } else {
            conversation.setLastMessage(lastMessage);
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
        
        return conversation;
    }

    private void updateUnreadCount(Long conversationId) {
        if (conversationId == null) return;
        
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conv.setUnreadCount(conv.getUnreadCount() + 1);
            conversationMapper.updateById(conv);
        }
    }

    private Long findConversationId(Long userId, Long targetId, Integer type) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .eq(Conversation::getTargetId, targetId)
               .eq(Conversation::getConversationType, type)
               .select(Conversation::getId);
        
        Conversation conv = conversationMapper.selectOne(wrapper);
        return conv != null ? conv.getId() : null;
    }

    private Long findGroupConversationId(Long groupId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getTargetId, groupId)
               .eq(Conversation::getConversationType, Constants.ConversationType.GROUP)
               .select(Conversation::getId)
               .last("LIMIT 1");
        
        Conversation conv = conversationMapper.selectOne(wrapper);
        return conv != null ? conv.getId() : null;
    }

    private List<Long> getUserConversationIds(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
               .select(Conversation::getId);
        
        return conversationMapper.selectList(wrapper).stream()
            .map(Conversation::getId)
            .toList();
    }

    // ==================== 兼容旧逻辑的方法（降级方案）====================

    private Page<Message> getPrivateMessagesLegacy(Long userId, Long targetId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getFromId, userId).eq(Message::getToId, targetId)
                .or().eq(Message::getFromId, targetId).eq(Message::getToId, userId));
        wrapper.orderByAsc(Message::getCreateTime);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private Page<Message> getGroupMessagesLegacy(Long groupId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getGroupId, groupId);
        wrapper.orderByAsc(Message::getCreateTime);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
