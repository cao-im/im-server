package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.ContactMapper;
import com.caoim.imcore.dao.FriendRequestMapper;
import com.caoim.imcore.dao.UserMapper;
import com.caoim.imcore.dto.FriendRequestDTO;
import com.caoim.imcore.entity.Contact;
import com.caoim.imcore.entity.FriendRequest;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.event.FriendAcceptedEvent;
import com.caoim.imcore.event.FriendRejectedEvent;
import com.caoim.imcore.event.FriendRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestMapper friendRequestMapper;
    private final ContactMapper contactMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void sendFriendRequest(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不能添加自己为好友");
        }

        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, fromUserId);
        wrapper.eq(FriendRequest::getToUserId, toUserId);
        if (friendRequestMapper.selectCount(wrapper) > 0) {
            FriendRequest existing = friendRequestMapper.selectOne(wrapper);
            if (existing.getStatus() == Constants.FriendStatus.PENDING) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);
            }
            if (existing.getStatus() == Constants.FriendStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
            }
            if (existing.getStatus() == Constants.FriendStatus.REJECTED) {
                existing.setStatus(Constants.FriendStatus.PENDING);
                friendRequestMapper.updateById(existing);
                eventPublisher.publishEvent(new FriendRequestEvent(this, fromUserId, toUserId));
                return;
            }
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);
        }

        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setStatus(Constants.FriendStatus.PENDING);
        friendRequestMapper.insert(request);

        eventPublisher.publishEvent(new FriendRequestEvent(this, fromUserId, toUserId));
    }

    @Transactional
    public void acceptFriendRequest(Long toUserId, Long fromUserId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, fromUserId);
        wrapper.eq(FriendRequest::getToUserId, toUserId);
        wrapper.eq(FriendRequest::getStatus, Constants.FriendStatus.PENDING);
        FriendRequest request = friendRequestMapper.selectOne(wrapper);

        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS);
        }

        request.setStatus(Constants.FriendStatus.ACCEPTED);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        Contact contact1 = new Contact();
        contact1.setUserId(fromUserId);
        contact1.setContactUserId(toUserId);
        contactMapper.insert(contact1);

        Contact contact2 = new Contact();
        contact2.setUserId(toUserId);
        contact2.setContactUserId(fromUserId);
        contactMapper.insert(contact2);

        eventPublisher.publishEvent(new FriendAcceptedEvent(this, toUserId, fromUserId));
    }

    @Transactional
    public void rejectFriendRequest(Long toUserId, Long fromUserId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, fromUserId);
        wrapper.eq(FriendRequest::getToUserId, toUserId);
        wrapper.eq(FriendRequest::getStatus, Constants.FriendStatus.PENDING);
        FriendRequest request = friendRequestMapper.selectOne(wrapper);

        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS);
        }

        request.setStatus(Constants.FriendStatus.REJECTED);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        eventPublisher.publishEvent(new FriendRejectedEvent(this, toUserId, fromUserId));
    }

    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId);
        wrapper.eq(FriendRequest::getStatus, Constants.FriendStatus.PENDING);
        wrapper.orderByDesc(FriendRequest::getCreateTime);
        List<FriendRequest> requests = friendRequestMapper.selectList(wrapper);

        return requests.stream().map(request -> {
            User fromUser = userMapper.selectById(request.getFromUserId());
            User toUser = userMapper.selectById(request.getToUserId());
            return FriendRequestDTO.fromEntity(request, fromUser, toUser);
        }).toList();
    }

    public int checkRequestStatus(Long userId, Long targetUserId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .eq(FriendRequest::getFromUserId, userId)
                        .eq(FriendRequest::getToUserId, targetUserId)
                        .or()
                        .eq(FriendRequest::getFromUserId, targetUserId)
                        .eq(FriendRequest::getToUserId, userId)
        );

        List<FriendRequest> requests = friendRequestMapper.selectList(wrapper);

        for (FriendRequest request : requests) {
            if (request.getStatus() == Constants.FriendStatus.ACCEPTED) {
                return 2;
            }
            if (request.getStatus() == Constants.FriendStatus.PENDING) {
                if (request.getFromUserId().equals(userId)) {
                    return 1;
                }
            }
        }

        return 0;
    }
}
