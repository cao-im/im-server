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
    private final SystemConfigService systemConfigService;

    @Transactional
    public Group createGroup(Long ownerId, GroupCreateDTO dto) {
        Group group = new Group();
        group.setName(dto.getName());
        group.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : "");
        group.setOwnerId(ownerId);
        int maxMembers = systemConfigService.getInt(SystemConfigService.MAX_GROUP_MEMBERS, 500);
        int requested = 1 + (dto.getMemberIds() == null ? 0 : dto.getMemberIds().size());
        if (maxMembers > 0 && requested > maxMembers) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "超过群成员上限");
        }
        group.setMaxMembers(maxMembers);
        group.setStatus(1);
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

        Group group = groupMapper.selectById(groupId);
        int maxMembers = group != null && group.getMaxMembers() != null && group.getMaxMembers() > 0
                ? group.getMaxMembers()
                : systemConfigService.getInt(SystemConfigService.MAX_GROUP_MEMBERS, 500);
        if (maxMembers > 0) {
            LambdaQueryWrapper<GroupMember> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(GroupMember::getGroupId, groupId);
            Long count = groupMemberMapper.selectCount(countWrapper);
            if (count != null && count >= maxMembers) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "超过群成员上限");
            }
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
        if (!isMember(groupId, userId)) {
            throw new BusinessException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }

    public boolean isMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        return groupMemberMapper.selectCount(wrapper) > 0;
    }

    public void requireMember(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        if (group.getStatus() != null && group.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "群组已解散");
        }
        checkIsMember(groupId, userId);
    }

    public void adminDismiss(Long groupId) {
        Group group = getGroupInfo(groupId);
        if (group.getStatus() != null && group.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "群组已解散");
        }
        group.setStatus(0);
        groupMapper.updateById(group);
    }

    public List<Long> listMemberIds(Long groupId) {
        return getGroupMembers(groupId).stream().map(GroupMember::getUserId).toList();
    }

    @Transactional
    public void dismissGroup(Long groupId, Long operatorId) {
        Group group = getGroupInfo(groupId);
        if (!operatorId.equals(group.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "只有群主才能解散群组");
        }
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId);
        groupMemberMapper.delete(wrapper);
        groupMapper.deleteById(groupId);
    }

    @Transactional
    public void transferOwner(Long groupId, Long operatorId, Long newOwnerId) {
        Group group = getGroupInfo(groupId);
        if (!operatorId.equals(group.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "只有群主才能转让群主");
        }
        if (operatorId.equals(newOwnerId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该用户已是群主");
        }
        checkIsMember(groupId, newOwnerId);

        LambdaQueryWrapper<GroupMember> ownerQuery = new LambdaQueryWrapper<>();
        ownerQuery.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, operatorId);
        GroupMember ownerMember = groupMemberMapper.selectOne(ownerQuery);
        if (ownerMember != null) {
            ownerMember.setRole(Constants.GroupRole.MEMBER);
            groupMemberMapper.updateById(ownerMember);
        }

        LambdaQueryWrapper<GroupMember> newOwnerQuery = new LambdaQueryWrapper<>();
        newOwnerQuery.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getUserId, newOwnerId);
        GroupMember newOwner = groupMemberMapper.selectOne(newOwnerQuery);
        newOwner.setRole(Constants.GroupRole.OWNER);
        groupMemberMapper.updateById(newOwner);

        group.setOwnerId(newOwnerId);
        groupMapper.updateById(group);
    }

    public void updateGroupInfo(Long groupId, String name, String avatar, Long operatorId) {
        checkIsOwnerOrAdmin(groupId, operatorId);
        updateGroupInfo(groupId, name, avatar);
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
