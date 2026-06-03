package com.caoim.imcore.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.MessageMapper;
import com.caoim.imcore.dao.MessageReadMapper;
import com.caoim.imcore.dao.GroupMemberMapper;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.GroupMember;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.entity.MessageRead;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.event.MessageSentEvent;
import com.caoim.imcore.dto.SenderInfo;
import com.caoim.imcore.dto.GroupInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final MessageReadMapper messageReadMapper;
    private final UserService userService;
    private final GroupService groupService;
    private final GroupMemberMapper groupMemberMapper;
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

        // 构建发送者信息快照并注入消息的extra字段
        String extraJson = buildMessageExtra(fromId, groupId);
        message.setExtra(extraJson);

        log.info("📤 [消息发送] fromId={}, toId={}, groupId={}, extra={}", fromId, toId, groupId, extraJson);

        messageMapper.insert(message);

        eventPublisher.publishEvent(new MessageSentEvent(this, message, fromId, toId, groupId));

        return message;
    }

    /**
     * 私聊消息查询（直接通过 from_id + to_id 查询双方消息）
     */
    public Page<Message> getPrivateMessages(Long userId, Long targetId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w ->
            w.eq(Message::getFromId, userId).eq(Message::getToId, targetId)
             .or()
             .eq(Message::getFromId, targetId).eq(Message::getToId, userId)
        );
        wrapper.orderByAsc(Message::getCreateTime);

        log.debug("查询私聊消息: userId={}, targetId={}, page={}, size={}", userId, targetId, page, size);

        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 群聊消息查询（直接通过 group_id 查询）
     */
    public Page<Message> getGroupMessages(Long groupId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getGroupId, groupId);
        wrapper.orderByAsc(Message::getCreateTime);

        log.debug("查询群聊消息: groupId={}, page={}, size={}", groupId, page, size);

        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<Message> getRecentMessages(Long userId, int limit) {
        // 直接查询发给用户或用户发出的最近消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w ->
            w.eq(Message::getToId, userId)
             .or()
             .eq(Message::getFromId, userId)
        );
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
        // 直接查询发给该用户且未读的消息数量（不依赖会话表）
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getToId, userId);
        wrapper.eq(Message::getMsgStatus, Constants.MessageStatus.UNREAD);
        return messageMapper.selectCount(wrapper);
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

    // ==================== 发送者信息快照构建 ====================

    /**
     * 构建消息的 extra 字段（JSON格式）
     * 包含发送者的昵称、头像等快照信息，以及群聊时的群组信息
     * 这样接收方即使没有本地缓存也能正常显示发送者身份
     */
    private String buildMessageExtra(Long fromId, Long groupId) {
        try {
            Map<String, Object> extraData = new HashMap<>();

            // 1. 构建发送者信息
            SenderInfo senderInfo = buildSenderInfo(fromId, groupId);
            extraData.put("senderInfo", senderInfo);

            // 2. 如果是群聊，额外添加群组信息
            if (groupId != null) {
                GroupInfoDTO groupInfo = buildGroupInfo(groupId);
                extraData.put("groupInfo", groupInfo);
            }

            return JSON.toJSONString(extraData);
        } catch (Exception e) {
            log.warn("构建消息extra字段失败, fromId={}, groupId={}, error={}", fromId, groupId, e.getMessage());
            return null;
        }
    }

    /**
     * 构建发送者信息快照
     */
    private SenderInfo buildSenderInfo(Long fromId, Long groupId) {
        User user = userService.getUserInfo(fromId);

        String nickname = user != null ? user.getNickname() : null;
        String avatar = user != null ? user.getAvatar() : null;

        // 如果是群聊，尝试获取群昵称
        String groupNickname = null;
        if (groupId != null) {
            groupNickname = getGroupMemberNickname(fromId, groupId);
        }

        return new SenderInfo(fromId, nickname, avatar, groupNickname);
    }

    /**
     * 构建群组信息快照
     */
    private GroupInfoDTO buildGroupInfo(Long groupId) {
        try {
            Group group = groupService.getGroupInfo(groupId);
            if (group != null) {
                return new GroupInfoDTO(group.getId(), group.getName(), group.getAvatar());
            }
        } catch (Exception e) {
            log.warn("获取群组信息失败: groupId={}, error={}", groupId, e.getMessage());
        }
        return new GroupInfoDTO(groupId, null, null);
    }

    /**
     * 获取用户在指定群组的群昵称
     */
    private String getGroupMemberNickname(Long userId, Long groupId) {
        try {
            LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GroupMember::getUserId, userId)
                   .eq(GroupMember::getGroupId, groupId)
                   .select(GroupMember::getNickname);
            
            GroupMember member = groupMemberMapper.selectOne(wrapper);
            if (member != null && member.getNickname() != null && !member.getNickname().trim().isEmpty()) {
                return member.getNickname().trim();
            }
        } catch (Exception e) {
            log.debug("查询群成员昵称失败: userId={}, groupId={}", userId, groupId);
        }
        return null;
    }

    // ==================== 消息extra字段解析工具方法 ====================

    /**
     * 从消息的extra字段中解析出发送者信息
     *
     * @param message 消息对象
     * @return 发送者信息，解析失败返回null
     */
    public static SenderInfo parseSenderInfo(Message message) {
        if (message == null || message.getExtra() == null || message.getExtra().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> extraMap = JSON.parseObject(message.getExtra(), Map.class);
            if (extraMap != null && extraMap.containsKey("senderInfo")) {
                Object senderInfoObj = extraMap.get("senderInfo");
                if (senderInfoObj instanceof Map) {
                    Map<String, Object> senderMap = (Map<String, Object>) senderInfoObj;
                    SenderInfo info = new SenderInfo();
                    if (senderMap.get("userId") instanceof Number) {
                        info.setUserId(((Number) senderMap.get("userId")).longValue());
                    }
                    info.setNickname((String) senderMap.get("nickname"));
                    info.setAvatar((String) senderMap.get("avatar"));
                    info.setGroupNickname((String) senderMap.get("groupNickname"));
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("解析senderInfo失败: messageId={}, error={}", message.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * 从消息的extra字段中解析出群组信息
     *
     * @param message 消息对象
     * @return 群组信息，解析失败返回null
     */
    public static GroupInfoDTO parseGroupInfo(Message message) {
        if (message == null || message.getExtra() == null || message.getExtra().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> extraMap = JSON.parseObject(message.getExtra(), Map.class);
            if (extraMap != null && extraMap.containsKey("groupInfo")) {
                Object groupInfoObj = extraMap.get("groupInfo");
                if (groupInfoObj instanceof Map) {
                    Map<String, Object> groupMap = (Map<String, Object>) groupInfoObj;
                    GroupInfoDTO info = new GroupInfoDTO();
                    if (groupMap.get("groupId") instanceof Number) {
                        info.setGroupId(((Number) groupMap.get("groupId")).longValue());
                    }
                    info.setGroupName((String) groupMap.get("groupName"));
                    info.setGroupAvatar((String) groupMap.get("groupAvatar"));
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("解析groupInfo失败: messageId={}, error={}", message.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * 将消息转换为包含完整信息的Map（用于API返回和WebSocket推送）
     * 自动从extra字段解析出senderInfo和groupInfo并放入返回数据
     */
    public static Map<String, Object> messageToMap(Message msg) {
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("id", msg.getId());
        msgData.put("mid", msg.getMid());
        msgData.put("fromId", msg.getFromId());
        msgData.put("toId", msg.getToId());
        msgData.put("groupId", msg.getGroupId());

        String content = msg.getContent();
        msgData.put("content", content != null ? content : "");

        msgData.put("msgType", msg.getMsgType());
        msgData.put("msgStatus", msg.getMsgStatus());

        if (msg.getCreateTime() != null) {
            long timestamp = msg.getCreateTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            msgData.put("timestamp", timestamp);
        } else {
            msgData.put("timestamp", System.currentTimeMillis());
        }

        // 解析并附加发送者信息和群组信息
        SenderInfo senderInfo = parseSenderInfo(msg);
        if (senderInfo != null) {
            msgData.put("senderInfo", senderInfo);
        }

        GroupInfoDTO groupInfo = parseGroupInfo(msg);
        if (groupInfo != null) {
            msgData.put("groupInfo", groupInfo);
        }

        return msgData;
    }
}
