package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.GroupCreateDTO;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.GroupMember;
import com.caoim.imcore.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public Result<Group> createGroup(@RequestParam Long ownerId, @RequestBody GroupCreateDTO dto) {
        return Result.success(groupService.createGroup(ownerId, dto));
    }

    @Operation(summary = "获取用户的群组列表")
    @GetMapping("/list")
    public Result<List<Group>> getUserGroups(@RequestParam Long userId) {
        return Result.success(groupService.getUserGroups(userId));
    }

    @Operation(summary = "获取群组信息")
    @GetMapping("/{groupId}")
    public Result<Group> getGroupInfo(@PathVariable Long groupId) {
        return Result.success(groupService.getGroupInfo(groupId));
    }

    @Operation(summary = "添加群成员")
    @PostMapping("/{groupId}/member")
    public Result<Void> addMember(
            @PathVariable Long groupId,
            @RequestParam Long userId,
            @RequestParam Long operatorId) {
        groupService.addMember(groupId, userId, operatorId);
        return Result.success();
    }

    @Operation(summary = "移除群成员")
    @DeleteMapping("/{groupId}/member/{userId}")
    public Result<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestParam Long operatorId) {
        groupService.removeMember(groupId, userId, operatorId);
        return Result.success();
    }

    @Operation(summary = "获取群成员列表")
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        return Result.success(groupService.getGroupMembers(groupId));
    }

    @Operation(summary = "更新群组信息")
    @PutMapping("/{groupId}")
    public Result<Void> updateGroupInfo(
            @PathVariable Long groupId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String avatar) {
        groupService.updateGroupInfo(groupId, name, avatar);
        return Result.success();
    }
}
