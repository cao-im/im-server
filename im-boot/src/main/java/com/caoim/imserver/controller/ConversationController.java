package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.entity.Conversation;
import com.caoim.imcore.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会话管理")
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "获取会话列表")
    @GetMapping("/list")
    public Result<List<Conversation>> getConversations(@RequestParam Long userId) {
        return Result.success(conversationService.getConversations(userId));
    }

    @Operation(summary = "清除未读数")
    @PutMapping("/read")
    public Result<Void> clearUnreadCount(@RequestParam Long userId, @RequestParam Long targetId) {
        conversationService.clearUnreadCount(userId, targetId);
        return Result.success();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{targetId}")
    public Result<Void> deleteConversation(@RequestParam Long userId, @PathVariable Long targetId) {
        conversationService.deleteConversation(userId, targetId);
        return Result.success();
    }
}
