package com.caoim.imserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.MessageSendDTO;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息管理")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestBody MessageSendDTO dto) {
        Message message = messageService.sendMessage(
                dto.getFromId(),
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
            @RequestParam("userId") Long userId,
            @PathVariable Long targetId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(messageService.getPrivateMessages(userId, targetId, page, size));
    }

    @Operation(summary = "获取群聊历史消息")
    @GetMapping("/group/{groupId}")
    public Result<Page<Message>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(messageService.getGroupMessages(groupId, page, size));
    }

    @Operation(summary = "标记消息已读")
    @PutMapping("/read")
    public Result<Void> markAsRead(@RequestBody List<Long> messageIds) {
        messageService.markAsRead(messageIds);
        return Result.success();
    }

    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(@RequestParam("userId") Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }
}
