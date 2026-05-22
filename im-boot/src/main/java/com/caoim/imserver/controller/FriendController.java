package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.entity.Friend;
import com.caoim.imcore.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "好友管理")
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "发送好友请求")
    @PostMapping("/request")
    public Result<Void> sendFriendRequest(@RequestParam Long userId, @RequestParam Long friendId) {
        friendService.sendFriendRequest(userId, friendId);
        return Result.success();
    }

    @Operation(summary = "接受好友请求")
    @PutMapping("/accept")
    public Result<Void> acceptFriendRequest(@RequestParam Long userId, @RequestParam Long friendId) {
        friendService.acceptFriendRequest(userId, friendId);
        return Result.success();
    }

    @Operation(summary = "拒绝好友请求")
    @PutMapping("/reject")
    public Result<Void> rejectFriendRequest(@RequestParam Long userId, @RequestParam Long friendId) {
        friendService.rejectFriendRequest(userId, friendId);
        return Result.success();
    }

    @Operation(summary = "获取好友列表")
    @GetMapping("/list")
    public Result<List<Friend>> getFriends(@RequestParam Long userId) {
        return Result.success(friendService.getFriends(userId));
    }

    @Operation(summary = "获取待处理的好友请求")
    @GetMapping("/requests")
    public Result<List<Friend>> getPendingRequests(@RequestParam Long userId) {
        return Result.success(friendService.getPendingRequests(userId));
    }

    @Operation(summary = "删除好友")
    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@RequestParam Long userId, @PathVariable Long friendId) {
        friendService.deleteFriend(userId, friendId);
        return Result.success();
    }
}
