package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.FriendMapper;
import com.caoim.imcore.dao.UserMapper;
import com.caoim.imcore.dto.FriendDTO;
import com.caoim.imcore.dto.FriendRequestDTO;
import com.caoim.imcore.entity.Friend;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.event.FriendRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

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
                eventPublisher.publishEvent(new FriendRequestEvent(this, userId, friendId));
                return;
            }
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);
        }

        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(Constants.FriendStatus.PENDING);
        friendMapper.insert(friend);

        eventPublisher.publishEvent(new FriendRequestEvent(this, userId, friendId));
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

    public List<FriendDTO> getFriends(Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId);
        wrapper.eq(Friend::getStatus, Constants.FriendStatus.ACCEPTED);
        List<Friend> friends = friendMapper.selectList(wrapper);

        List<FriendDTO> result = new ArrayList<>();
        for (Friend friend : friends) {
            FriendDTO dto = new FriendDTO();
            dto.setId(friend.getId());
            dto.setFriendId(friend.getFriendId());
            dto.setStatus(friend.getStatus());

            User friendUser = userMapper.selectById(friend.getFriendId());
            if (friendUser != null) {
                dto.setUsername(friendUser.getUsername());
                dto.setNickname(friendUser.getNickname());
                dto.setAvatar(friendUser.getAvatar());
            }

            result.add(dto);
        }
        return result;
    }

    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(Friend::getFriendId, userId)
                .or()
                .eq(Friend::getUserId, userId)
        );
        wrapper.in(Friend::getStatus,
                Constants.FriendStatus.PENDING,
                Constants.FriendStatus.ACCEPTED
        );
        wrapper.orderByDesc(Friend::getCreateTime);
        List<Friend> friends = friendMapper.selectList(wrapper);

        List<FriendRequestDTO> result = new ArrayList<>();
        Set<String> acceptedPairs = new HashSet<>();

        for (Friend friend : friends) {
            boolean isAccepted = friend.getStatus() == Constants.FriendStatus.ACCEPTED;
            String pairKey = Math.min(friend.getUserId(), friend.getFriendId()) + "-" +
                              Math.max(friend.getUserId(), friend.getFriendId());

            if (isAccepted && acceptedPairs.contains(pairKey)) {
                continue;
            }
            if (isAccepted) {
                acceptedPairs.add(pairKey);
            }

            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setId(friend.getId());
            dto.setUserId(friend.getUserId());
            dto.setFriendId(friend.getFriendId());
            dto.setStatus(friend.getStatus());
            dto.setCreateTime(friend.getCreateTime());

            User user = userMapper.selectById(friend.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setNickname(user.getNickname());
                dto.setAvatar(user.getAvatar());
            }

            User friendUser = userMapper.selectById(friend.getFriendId());
            if (friendUser != null) {
                dto.setFriendUsername(friendUser.getUsername());
                dto.setFriendNickname(friendUser.getNickname());
                dto.setFriendAvatar(friendUser.getAvatar());
            }

            result.add(dto);
        }

        return result;
    }

    public int checkFriendStatus(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .or()
                        .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId)
        );

        List<Friend> friends = friendMapper.selectList(wrapper);

        for (Friend friend : friends) {
            if (friend.getStatus() == Constants.FriendStatus.ACCEPTED) {
                return 2;
            }
            if (friend.getStatus() == Constants.FriendStatus.PENDING) {
                if (friend.getUserId().equals(userId)) {
                    return 1;
                }
            }
        }

        return 0;
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
