package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.GroupMapper;
import com.caoim.imcore.dao.GroupMemberMapper;
import com.caoim.imcore.dto.GroupCreateDTO;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.GroupMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;

    @Transactional
    public Group createGroup(Long ownerId, GroupCreateDTO dto) {
        Group group = new Group();
        group.setName(dto.getName());
        group.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : "");
        group.setOwnerId(ownerId);
        groupMapper.insert(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setRole(Constants.GroupRole.OWNER);
        groupMemberMapper.insert(ownerMember);

        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            for (Long memberId : dto.getMemberIds()) {
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setUserId(memberId);
                member.setRole(Constants.GroupRole.MEMBER);
                groupMemberMapper.insert(member);
            }
        }

        return group;
    }

    public Group createGroup(String name, Long ownerId, List<Long> memberIds) {
        GroupCreateDTO dto = new GroupCreateDTO();
        dto.setName(name);
        dto.setMemberIds(memberIds);
        return createGroup(ownerId, dto);
    }

    public void addMembers(Long groupId, List<Long> userIds) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        for (Long userId : userIds) {
            addMember(groupId, userId, group.getOwnerId());
        }
    }

    public void addGroupMembers(Long groupId, List<Long> userIds) {
        addMembers(groupId, userIds);
    }

    public void removeGroupMember(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        removeMember(groupId, userId, group.getOwnerId());
    }

    public List<Group> getUserGroups(Long userId) {
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getUserId, userId);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);

        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = members.stream().map(GroupMember::getGroupId).toList();
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Group::getId, groupIds);
        return groupMapper.selectList(wrapper);
    }

    public Group getGroupInfo(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        return group;
    }

    @Transactional
    public void addMember(Long groupId, Long userId, Long operatorId) {
        checkIsMember(groupId, operatorId);

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_IN_GROUP);
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(Constants.GroupRole.MEMBER);
        groupMemberMapper.insert(member);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        checkIsOwnerOrAdmin(groupId, operatorId);

        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        groupMemberMapper.delete(wrapper);
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        return groupMemberMapper.selectList(wrapper);
    }

    public void updateGroupInfo(Long groupId, String name, String avatar) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        if (name != null) {
            group.setName(name);
        }
        if (avatar != null) {
            group.setAvatar(avatar);
        }
        groupMapper.updateById(group);
    }

    private void checkIsMember(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        if (groupMemberMapper.selectCount(wrapper) == 0) {
            throw new BusinessException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }

    private void checkIsOwnerOrAdmin(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.in(GroupMember::getRole, Constants.GroupRole.ADMIN, Constants.GroupRole.OWNER);
        if (groupMemberMapper.selectCount(wrapper) == 0) {
            throw new BusinessException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }
}
