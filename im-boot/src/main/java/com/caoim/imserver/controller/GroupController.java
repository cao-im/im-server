package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.GroupCreateDTO;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.GroupMember;
import com.caoim.imcore.service.GroupService;
import com.caoim.imserver.common.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "群组管理")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "创建群组")
    @PostMapping("/create")
    public Result<Group> createGroup(
            HttpServletRequest request,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            @RequestBody GroupCreateDTO dto) {
        Long actorId = UserContext.resolveActor(request, ownerId);
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        return Result.success(groupService.createGroup(actorId, dto));
    }

    @GetMapping("/list")
    public Result<List<Group>> getUserGroups(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) Long userId) {
        Long actorId = UserContext.resolveActor(request, userId);
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        return Result.success(groupService.getUserGroups(actorId));
    }

    @Operation(summary = "获取群组信息")
    @GetMapping("/{groupId}")
    public Result<Group> getGroupInfo(HttpServletRequest request, @PathVariable Long groupId) {
        if (!UserContext.isInternalCall(request)) {
            Long actorId = UserContext.getCurrentUserId(request);
            if (actorId == null) {
                return Result.error(401, "未认证或Token无效");
            }
            groupService.requireMember(groupId, actorId);
        }
        return Result.success(groupService.getGroupInfo(groupId));
    }

    @Operation(summary = "添加群成员")
    @PostMapping("/{groupId}/member")
    public Result<Void> addMember(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "operatorId", required = false) Long operatorId) {
        Long actorId = UserContext.resolveActor(request, operatorId);
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        groupService.addMember(groupId, userId, actorId);
        return Result.success();
    }

    @DeleteMapping("/{groupId}/member/{userId}")
    public Result<Void> removeMember(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestParam(value = "operatorId", required = false) Long operatorId) {
        Long actorId = UserContext.resolveActor(request, operatorId);
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        groupService.removeMember(groupId, userId, actorId);
        return Result.success();
    }

    @Operation(summary = "获取群成员列表")
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMember>> getGroupMembers(HttpServletRequest request, @PathVariable Long groupId) {
        Long actorId = UserContext.resolveActor(request, null);
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        groupService.requireMember(groupId, actorId);
        return Result.success(groupService.getGroupMembers(groupId));
    }

    @Operation(summary = "更新群组信息")
    @PutMapping("/{groupId}")
    public Result<Void> updateGroupInfo(
            HttpServletRequest request,
            @PathVariable Long groupId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "avatar", required = false) String avatar,
            @RequestParam(value = "operatorId", required = false) Long operatorId) {
        Long actorId = UserContext.resolveActor(request, operatorId);
        if (actorId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        groupService.updateGroupInfo(groupId, name, avatar, actorId);
        return Result.success();
    }
}
