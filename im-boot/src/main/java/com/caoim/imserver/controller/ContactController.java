package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.ContactDTO;
import com.caoim.imcore.dto.UserSearchDTO;
import com.caoim.imserver.common.UserContext;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.service.ContactService;
import com.caoim.imcore.service.FriendRequestService;
import com.caoim.imcore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "联系人管理")
@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final FriendRequestService friendRequestService;
    private final UserService userService;

    @Operation(summary = "获取联系人列表")
    @GetMapping("/list")
    public Result<List<ContactDTO>> getContacts(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        return Result.success(contactService.getContacts(actorId));
    }

    @DeleteMapping("/{contactId}")
    public Result<Void> deleteContact(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId,
            @PathVariable String contactId) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        contactService.deleteContact(actorId, Long.parseLong(contactId));
        return Result.success();
    }

    @PutMapping("/remark")
    public Result<Void> updateRemark(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam("contactId") String contactId,
            @RequestParam("remark") String remark) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        contactService.updateRemark(actorId, Long.parseLong(contactId), remark);
        return Result.success();
    }

    @PutMapping("/top")
    public Result<Void> setTop(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam("contactId") String contactId,
            @RequestParam("isTop") boolean isTop) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        contactService.setTop(actorId, Long.parseLong(contactId), isTop);
        return Result.success();
    }

    @PutMapping("/mute")
    public Result<Void> setMute(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam("contactId") String contactId,
            @RequestParam("isMute") boolean isMute) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        contactService.setMute(actorId, Long.parseLong(contactId), isMute);
        return Result.success();
    }

    @GetMapping("/check-status")
    public Result<Integer> checkFriendStatus(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam("targetUserId") String targetUserId) {
        Long actorId = UserContext.resolveActor(request, userId == null ? null : Long.parseLong(userId));
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        int status = friendRequestService.checkRequestStatus(actorId, Long.parseLong(targetUserId));
        return Result.success(status);
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
                int friendStatus = friendRequestService.checkRequestStatus(currentUserId, user.getId());
                dto.setFriendStatus(friendStatus);
            }
            result.add(dto);
        }
        return Result.success(result);
    }
}
