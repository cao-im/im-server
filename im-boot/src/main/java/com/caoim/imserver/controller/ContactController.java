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
    public Result<List<ContactDTO>> getContacts(@RequestParam("userId") String userId) {
        return Result.success(contactService.getContacts(Long.parseLong(userId)));
    }

    @Operation(summary = "删除联系人（删除好友）")
    @DeleteMapping("/{contactId}")
    public Result<Void> deleteContact(
            @RequestParam("userId") String userId,
            @PathVariable String contactId) {
        contactService.deleteContact(Long.parseLong(userId), Long.parseLong(contactId));
        return Result.success();
    }

    @Operation(summary = "修改备注名")
    @PutMapping("/remark")
    public Result<Void> updateRemark(
            @RequestParam("userId") String userId,
            @RequestParam("contactId") String contactId,
            @RequestParam("remark") String remark) {
        contactService.updateRemark(Long.parseLong(userId), Long.parseLong(contactId), remark);
        return Result.success();
    }

    @Operation(summary = "设置置顶")
    @PutMapping("/top")
    public Result<Void> setTop(
            @RequestParam("userId") String userId,
            @RequestParam("contactId") String contactId,
            @RequestParam("isTop") boolean isTop) {
        contactService.setTop(Long.parseLong(userId), Long.parseLong(contactId), isTop);
        return Result.success();
    }

    @Operation(summary = "设置免打扰")
    @PutMapping("/mute")
    public Result<Void> setMute(
            @RequestParam("userId") String userId,
            @RequestParam("contactId") String contactId,
            @RequestParam("isMute") boolean isMute) {
        contactService.setMute(Long.parseLong(userId), Long.parseLong(contactId), isMute);
        return Result.success();
    }

    @Operation(summary = "检查好友关系状态")
    @GetMapping("/check-status")
    public Result<Integer> checkFriendStatus(
            @RequestParam("userId") String userId,
            @RequestParam("targetUserId") String targetUserId) {
        int status = friendRequestService.checkRequestStatus(Long.parseLong(userId), Long.parseLong(targetUserId));
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
