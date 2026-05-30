package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.FriendRequestDTO;
import com.caoim.imcore.service.FriendRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "好友申请管理")
@RestController
@RequestMapping("/friend-request")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @Operation(summary = "发送好友请求")
    @PostMapping("/request")
    public Result<Void> sendFriendRequest(
            @RequestParam("fromUserId") String fromUserId,
            @RequestParam("toUserId") String toUserId) {
        friendRequestService.sendFriendRequest(Long.parseLong(fromUserId), Long.parseLong(toUserId));
        return Result.success();
    }

    @Operation(summary = "接受好友请求")
    @PutMapping("/accept")
    public Result<Void> acceptFriendRequest(
            @RequestParam("toUserId") String toUserId,
            @RequestParam("fromUserId") String fromUserId) {
        friendRequestService.acceptFriendRequest(Long.parseLong(toUserId), Long.parseLong(fromUserId));
        return Result.success();
    }

    @Operation(summary = "拒绝好友请求")
    @PutMapping("/reject")
    public Result<Void> rejectFriendRequest(
            @RequestParam("toUserId") String toUserId,
            @RequestParam("fromUserId") String fromUserId) {
        friendRequestService.rejectFriendRequest(Long.parseLong(toUserId), Long.parseLong(fromUserId));
        return Result.success();
    }

    @Operation(summary = "获取待处理的好友请求")
    @GetMapping("/pending")
    public Result<List<FriendRequestDTO>> getPendingRequests(@RequestParam("userId") String userId) {
        return Result.success(friendRequestService.getPendingRequests(Long.parseLong(userId)));
    }
}
