package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.FriendMapper;
import com.caoim.imcore.entity.Friend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;

    public void sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不能添加自己为好友");
        }

        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId);
        wrapper.eq(Friend::getFriendId, friendId);
        if (friendMapper.selectCount(wrapper) > 0) {
            Friend existing = friendMapper.selectOne(wrapper);
            if (existing.getStatus() == Constants.FriendStatus.PENDING) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);
            }
            if (existing.getStatus() == Constants.FriendStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
            }
            if (existing.getStatus() == Constants.FriendStatus.DELETED) {
                existing.setStatus(Constants.FriendStatus.PENDING);
                friendMapper.updateById(existing);
                return;
            }
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);
        }

        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(Constants.FriendStatus.PENDING);
        friendMapper.insert(friend);
    }

    public void sendRequest(Long userId, Long friendId) {
        sendFriendRequest(userId, friendId);
    }

    public void acceptFriendRequest(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, friendId);
        wrapper.eq(Friend::getFriendId, userId);
        wrapper.eq(Friend::getStatus, Constants.FriendStatus.PENDING);
        Friend request = friendMapper.selectOne(wrapper);

        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS);
        }

        request.setStatus(Constants.FriendStatus.ACCEPTED);
        friendMapper.updateById(request);

        LambdaQueryWrapper<Friend> reverseWrapper = new LambdaQueryWrapper<>();
        reverseWrapper.eq(Friend::getUserId, userId);
        reverseWrapper.eq(Friend::getFriendId, friendId);
        Friend reverse = friendMapper.selectOne(reverseWrapper);
        if (reverse != null) {
            reverse.setStatus(Constants.FriendStatus.ACCEPTED);
            friendMapper.updateById(reverse);
        } else {
            Friend newFriend = new Friend();
            newFriend.setUserId(userId);
            newFriend.setFriendId(friendId);
            newFriend.setStatus(Constants.FriendStatus.ACCEPTED);
            friendMapper.insert(newFriend);
        }
    }

    public void acceptRequest(Long userId, Long friendId) {
        acceptFriendRequest(userId, friendId);
    }

    public void rejectFriendRequest(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, friendId);
        wrapper.eq(Friend::getFriendId, userId);
        wrapper.eq(Friend::getStatus, Constants.FriendStatus.PENDING);
        Friend request = friendMapper.selectOne(wrapper);

        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS);
        }

        request.setStatus(Constants.FriendStatus.REJECTED);
        friendMapper.updateById(request);
    }

    public void rejectRequest(Long userId, Long friendId) {
        rejectFriendRequest(userId, friendId);
    }

    public List<Friend> getFriends(Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId);
        wrapper.eq(Friend::getStatus, Constants.FriendStatus.ACCEPTED);
        return friendMapper.selectList(wrapper);
    }

    public List<Friend> getPendingRequests(Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getFriendId, userId);
        wrapper.eq(Friend::getStatus, Constants.FriendStatus.PENDING);
        return friendMapper.selectList(wrapper);
    }

    public void deleteFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friend::getUserId, userId);
        wrapper1.eq(Friend::getFriendId, friendId);
        Friend friend1 = friendMapper.selectOne(wrapper1);
        if (friend1 != null) {
            friend1.setStatus(Constants.FriendStatus.DELETED);
            friendMapper.updateById(friend1);
        }

        LambdaQueryWrapper<Friend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friend::getUserId, friendId);
        wrapper2.eq(Friend::getFriendId, userId);
        Friend friend2 = friendMapper.selectOne(wrapper2);
        if (friend2 != null) {
            friend2.setStatus(Constants.FriendStatus.DELETED);
            friendMapper.updateById(friend2);
        }
    }
}
