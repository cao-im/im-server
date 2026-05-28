package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.MessageMapper;
import com.caoim.imcore.dao.MessageReadMapper;
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

        Message message = new Message(fromId, toId, groupId, content, msgType);
        messageMapper.insert(message);

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

    /**
     * 标记消息为已读（完整的回执处理）
     * 同时执行两个操作：
     * 1. 更新 im_message 表的 msg_status 为 READ
     * 2. 写入 im_message_read 表记录回执
     *
     * @param messageIds 消息ID列表
     * @param userId 已读用户ID（谁读了这些消息）
     */
    @Transactional
    public void markAsRead(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty() || userId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<MessageRead> readRecords = new ArrayList<>();

        for (Long messageId : messageIds) {
            // 1. 更新消息状态为已读
            Message message = new Message();
            message.setId(messageId);
            message.setMsgStatus(Constants.MessageStatus.READ);
            messageMapper.updateById(message);

            // 2. 创建已读回执记录（幂等性检查）
            LambdaQueryWrapper<MessageRead> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(MessageRead::getMsgId, messageId)
                     .eq(MessageRead::getUserId, userId);
            
            Long count = messageReadMapper.selectCount(existCheck);
            if (count == 0) {
                // 不存在才插入，防止重复回执
                MessageRead readRecord = new MessageRead();
                readRecord.setMsgId(messageId);
                readRecord.setUserId(userId);
                readRecord.setReadTime(now);
                readRecords.add(readRecord);
            }
        }

        // 批量插入已读回执记录
        if (!readRecords.isEmpty()) {
            for (MessageRead record : readRecords) {
                messageReadMapper.insert(record);
            }
            log.info("标记消息已读: userId={}, 消息数={}, 回执数={}", 
                    userId, messageIds.size(), readRecords.size());
        }
    }

    public Long getUnreadCount(Long userId) {
        // 方式1：基于msg_status统计（简单但可能不准确）
        // LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        // wrapper.eq(Message::getToId, userId);
        // wrapper.eq(Message::getMsgStatus, Constants.MessageStatus.UNREAD);
        // return messageMapper.selectCount(wrapper);

        // 方式2：基于已读回执表统计（更准确）✅ 推荐
        // 未读消息 = 总消息 - 已读回执的消息
        String sql = "SELECT COUNT(*) FROM im_message m " +
                     "WHERE m.to_id = ? " +
                     "AND m.id NOT IN (" +
                     "  SELECT mr.msg_id FROM im_message_read mr WHERE mr.user_id = ?" +
                     ")";

        // 使用原生SQL查询（与getOfflineMessages保持一致）
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) FROM im_message m ");
        sqlBuilder.append("WHERE m.to_id = {0} ");
        sqlBuilder.append("AND m.msg_status = 1 ");
        sqlBuilder.append("AND m.id NOT IN (");
        sqlBuilder.append("  SELECT mr.msg_id FROM im_message_read mr ");
        sqlBuilder.append("  WHERE mr.user_id = {0}");
        sqlBuilder.append(")");

        List<Object> params = new ArrayList<>();
        params.add(userId);

        return messageMapper.selectCount(
            new LambdaQueryWrapper<Message>().apply(sqlBuilder.toString(), params.toArray())
        );
    }

    /**
     * 获取用户的真正离线消息（未读且未确认收到）
     * 
     * 离线消息定义：
     * - 接收方是当前用户 (to_id = userId)
     * - 消息状态为未读 (msg_status = 0/UNREAD)
     * - 且不在已读回执表中 (id NOT IN message_read)
     *
     * @param userId 用户ID
     * @param sinceTimestamp 起始时间戳（毫秒），0表示不限制
     * @param sinceMessageId 起始消息ID，0表示不限制
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 离线消息列表（真正的未读消息）
     */
    public List<Message> getOfflineMessages(Long userId, Long sinceTimestamp, Long sinceMessageId, int offset, int limit) {
        // 构建查询条件
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT m.* FROM im_message m ");
        sqlBuilder.append("WHERE m.to_id = {0} ");                          // 条件1：接收方
        sqlBuilder.append("AND m.msg_status = 1 ");                         // 条件2：未读状态
        
        // 条件3：排除已确认收到的消息（在回执表中的）
        sqlBuilder.append("AND m.id NOT IN (");
        sqlBuilder.append("  SELECT mr.msg_id FROM im_message_read mr ");
        sqlBuilder.append("  WHERE mr.user_id = {0}");
        sqlBuilder.append(")");
        
        List<Object> params = new ArrayList<>();
        params.add(userId);

        // 时间戳过滤
        if (sinceTimestamp != null && sinceTimestamp > 0) {
            LocalDateTime sinceTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(sinceTimestamp),
                java.time.ZoneId.systemDefault()
            );
            sqlBuilder.append(" AND m.create_time >= {1} ");
            params.add(sinceTime);
        }

        // 消息ID过滤（增量同步）
        if (sinceMessageId != null && sinceMessageId > 0) {
            sqlBuilder.append(" AND m.id > {2} ");
            params.add(sinceMessageId);
        }

        // 排序和分页
        sqlBuilder.append(" ORDER BY m.id ASC ");
        sqlBuilder.append(" LIMIT {3}, {4} ");
        params.add(offset);
        params.add(limit);

        // 执行原生SQL查询
        List<Message> messages = messageMapper.selectList(
            new LambdaQueryWrapper<Message>().apply(sqlBuilder.toString(), params.toArray())
        );

        log.info("查询离线消息(真正的未读): userId={}, sinceTimestamp={}, sinceMessageId={}, offset={}, limit={}, 结果数={}",
                userId, sinceTimestamp, sinceMessageId, offset, limit, messages.size());

        return messages;
    }

    /**
     * 获取用户真正的离线消息总数
     */
    public Long getOfflineMessagesCount(Long userId, Long sinceTimestamp, Long sinceMessageId) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT COUNT(*) FROM im_message m ");
        sqlBuilder.append("WHERE m.to_id = {0} ");
        sqlBuilder.append("AND m.msg_status = 1 ");
        sqlBuilder.append("AND m.id NOT IN (");
        sqlBuilder.append("  SELECT mr.msg_id FROM im_message_read mr ");
        sqlBuilder.append("  WHERE mr.user_id = {0}");
        sqlBuilder.append(")");
        
        List<Object> params = new ArrayList<>();
        params.add(userId);

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

        return messageMapper.selectCount(
            new LambdaQueryWrapper<Message>().apply(sqlBuilder.toString(), params.toArray())
        );
    }
}
