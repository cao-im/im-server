package com.caoim.imserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.MessageSendDTO;
import com.caoim.imcore.dto.OfflineMessageResponseDTO;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.service.MessageService;
import com.caoim.imserver.common.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "消息管理")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<Message> sendMessage(HttpServletRequest request, @RequestBody MessageSendDTO dto) {
        // 从JWT中提取当前用户ID（安全性）
        Long currentUserId = UserContext.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error(401, "未认证或Token无效");
        }

        // 强制使用Token中的userId作为发送者，忽略前端传入的值
        Message message = messageService.sendMessage(
                currentUserId,  // ✅ 使用JWT中的用户ID
                dto.getToId(),
                dto.getGroupId(),
                dto.getContent(),
                dto.getMsgType() != null ? dto.getMsgType() : Constants.MessageType.TEXT
        );
        return Result.success(message);
    }

    @Operation(summary = "获取私聊历史消息")
    @GetMapping("/private/{targetId}")
    public Result<Page<Message>> getPrivateMessages(
            HttpServletRequest request,
            @PathVariable Long targetId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        Long userId = UserContext.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        
        return Result.success(messageService.getPrivateMessages(userId, targetId, page, size));
    }

    @Operation(summary = "获取群聊历史消息")
    @GetMapping("/group/{groupId}")
    public Result<Page<Message>> getGroupMessages(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        // 群聊消息也需要认证（后续可检查用户是否在群中）
        Long userId = UserContext.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        
        return Result.success(messageService.getGroupMessages(groupId, page, size));
    }

    @Operation(summary = "标记消息已读（发送回执）")
    @PutMapping("/read")
    public Result<Void> markAsRead(HttpServletRequest request, @RequestBody List<Long> messageIds) {
        // 从JWT中提取当前用户ID
        Long userId = UserContext.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未认证或Token无效");
        }

        if (messageIds == null || messageIds.isEmpty()) {
            return Result.error(400, "消息ID列表不能为空");
        }

        log.info("收到已读回执: userId={}, 消息数={}", userId, messageIds.size());
        
        // 调用完整的markAsRead方法（包含回执记录）
        messageService.markAsRead(messageIds, userId);
        
        return Result.success();
    }

    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        // 从JWT中提取当前用户ID
        Long userId = UserContext.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        
        return Result.success(messageService.getUnreadCount(userId));
    }

    @Operation(summary = "获取离线消息（增量同步）- 只返回真正的未读消息")
    @GetMapping("/offline")
    public Result<OfflineMessageResponseDTO> getOfflineMessages(
            HttpServletRequest request,
            @RequestParam(value = "since", defaultValue = "0") Long sinceTimestamp,
            @RequestParam(value = "sinceMessageId", defaultValue = "0") Long sinceMessageId,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        // 🔒 安全性：从JWT Token中强制提取当前登录用户的ID
        Long currentUserId = UserContext.getCurrentUserId(request);
        if (currentUserId == null) {
            log.warn("离线消息请求失败: 未提供有效的认证Token");
            return Result.error(401, "未认证或Token无效，请先登录");
        }

        log.info("处理离线消息请求(安全版): userId={}, sinceTimestamp={}, sinceMessageId={}, offset={}, limit={}",
                currentUserId, sinceTimestamp, sinceMessageId, offset, limit);

        // 参数校验
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        if (offset < 0) {
            offset = 0;
        }

        // 查询真正的离线消息（未读 + 未确认）
        List<Message> offlineMessages = messageService.getOfflineMessages(
                currentUserId,  // ✅ 使用从JWT提取的用户ID
                sinceTimestamp,
                sinceMessageId,
                offset,
                limit
        );

        // 查询总数
        long totalCount = messageService.getOfflineMessagesCount(
                currentUserId,
                sinceTimestamp,
                sinceMessageId
        );

        // 构建响应数据
        List<Map<String, Object>> messagesData = new ArrayList<>();
        for (Message msg : offlineMessages) {
            Map<String, Object> msgData = new HashMap<>();
            msgData.put("id", msg.getId());
            msgData.put("fromId", msg.getFromId());
            msgData.put("toId", msg.getToId());
            msgData.put("groupId", msg.getGroupId());

            String content = msg.getContent();
            if (content == null) {
                content = "";
            }
            msgData.put("content", content);

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

            messagesData.add(msgData);
        }

        boolean hasMore = (offset + offlineMessages.size()) < totalCount;

        OfflineMessageResponseDTO response = new OfflineMessageResponseDTO();
        response.setMessages(messagesData);
        response.setCount(offlineMessages.size());
        response.setTotalCount(totalCount);
        response.setHasMore(hasMore);
        response.setOffset(offset);
        response.setLimit(limit);

        log.info("离线消息查询结果(安全版): userId={}, 返回数量={}, 总数={}, hasMore={}",
                currentUserId, offlineMessages.size(), totalCount, hasMore);

        return Result.success(response);
    }
}
