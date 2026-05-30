package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.FriendDTO;
import com.caoim.imcore.dto.FriendRequestDTO;
import com.caoim.imcore.dto.UserSearchDTO;
import com.caoim.imserver.common.UserContext;
import com.caoim.imcore.entity.Friend;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.service.FriendService;
import com.caoim.imcore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "好友管理")
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserService userService;

    @Operation(summary = "发送好友请求")
    @PostMapping("/request")
    public Result<Void> sendFriendRequest(
            @RequestParam("userId") String userId,
            @RequestParam("friendId") String friendId) {
        friendService.sendFriendRequest(Long.parseLong(userId), Long.parseLong(friendId));
        return Result.success();
    }

    @Operation(summary = "接受好友请求")
    @PutMapping("/accept")
    public Result<Void> acceptFriendRequest(
            @RequestParam("userId") String userId,
            @RequestParam("friendId") String friendId) {
        friendService.acceptFriendRequest(Long.parseLong(userId), Long.parseLong(friendId));
        return Result.success();
    }

    @Operation(summary = "拒绝好友请求")
    @PutMapping("/reject")
    public Result<Void> rejectFriendRequest(
            @RequestParam("userId") String userId,
            @RequestParam("friendId") String friendId) {
        friendService.rejectFriendRequest(Long.parseLong(userId), Long.parseLong(friendId));
        return Result.success();
    }

    @Operation(summary = "获取好友列表")
    @GetMapping("/list")
    public Result<List<FriendDTO>> getFriends(@RequestParam("userId") String userId) {
        return Result.success(friendService.getFriends(Long.parseLong(userId)));
    }

    @Operation(summary = "获取待处理的好友请求")
    @GetMapping("/requests")
    public Result<List<FriendRequestDTO>> getPendingRequests(@RequestParam("userId") String userId) {
        return Result.success(friendService.getPendingRequests(Long.parseLong(userId)));
    }

    @Operation(summary = "检查好友状态")
    @GetMapping("/check-status")
    public Result<Integer> checkFriendStatus(
            @RequestParam("userId") String userId,
            @RequestParam("friendId") String friendId) {
        int status = friendService.checkFriendStatus(Long.parseLong(userId), Long.parseLong(friendId));
        return Result.success(status);
    }

    @Operation(summary = "删除好友")
    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(
            @RequestParam("userId") String userId,
            @PathVariable String friendId) {
        friendService.deleteFriend(Long.parseLong(userId), Long.parseLong(friendId));
        return Result.success();
    }

    @Operation(summary = "搜索用户（用于添加好友）")
    @GetMapping("/search-users")
    public Result<List<UserSearchDTO>> searchUsers(
            @RequestParam("keyword") String keyword,
            HttpServletRequest request) {
        Long currentUserId = UserContext.getCurrentUserId(request);
        String currentUsername = UserContext.getCurrentUsername(request);

        List<User> users = userService.searchUsers(keyword, currentUserId, currentUsername);
        List<UserSearchDTO> result = new ArrayList<>();
        for (User user : users) {
            UserSearchDTO dto = UserSearchDTO.fromEntity(user);
            if (currentUserId != null) {
                int friendStatus = friendService.checkFriendStatus(currentUserId, user.getId());
                dto.setFriendStatus(friendStatus);
            }
            result.add(dto);
        }
        return Result.success(result);
    }
}
