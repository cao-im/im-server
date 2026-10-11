package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.FriendRequestDTO;
import com.caoim.imcore.service.FriendRequestService;
import com.caoim.imserver.common.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
            HttpServletRequest request,
            @RequestParam(value = "fromUserId", required = false) String fromUserId,
            @RequestParam("toUserId") String toUserId) {
        Long actorId = UserContext.resolveActor(request, fromUserId == null ? null : Long.parseLong(fromUserId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        friendRequestService.sendFriendRequest(actorId, Long.parseLong(toUserId));
        return Result.success();
    }

    @PutMapping("/accept")
    public Result<Void> acceptFriendRequest(
            HttpServletRequest request,
            @RequestParam(value = "toUserId", required = false) String toUserId,
            @RequestParam("fromUserId") String fromUserId) {
        Long actorId = UserContext.resolveActor(request, toUserId == null ? null : Long.parseLong(toUserId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        friendRequestService.acceptFriendRequest(actorId, Long.parseLong(fromUserId));
        return Result.success();
    }

    @PutMapping("/reject")
    public Result<Void> rejectFriendRequest(
            HttpServletRequest request,
            @RequestParam(value = "toUserId", required = false) String toUserId,
            @RequestParam("fromUserId") String fromUserId) {
        Long actorId = UserContext.resolveActor(request, toUserId == null ? null : Long.parseLong(toUserId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        friendRequestService.rejectFriendRequest(actorId, Long.parseLong(fromUserId));
        return Result.success();
    }

    @GetMapping("/pending")
    public Result<List<FriendRequestDTO>> getPendingRequests(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        return Result.success(friendRequestService.getPendingRequests(actorId));
    }
}
