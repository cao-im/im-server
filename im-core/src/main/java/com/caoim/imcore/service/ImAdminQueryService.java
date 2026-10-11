package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.GroupMapper;
import com.caoim.imcore.dao.MessageMapper;
import com.caoim.imcore.dao.UserMapper;
import com.caoim.imcore.dto.AdminMessageVO;
import com.caoim.imcore.dto.AdminOverviewDTO;
import com.caoim.imcore.dto.PageResult;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ImAdminQueryService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final MessageMapper messageMapper;

    public ImAdminQueryService(UserMapper userMapper, GroupMapper groupMapper, MessageMapper messageMapper) {
        this.userMapper = userMapper;
        this.groupMapper = groupMapper;
        this.messageMapper = messageMapper;
    }

    public PageResult<User> pageUsers(String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            wrapper.and(query -> query.like(User::getUsername, value).or().like(User::getNickname, value));
        }
        wrapper.orderByDesc(User::getId);
        return PageResult.of(userMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), wrapper));
    }

    public PageResult<Group> pageGroups(String keyword, int page, int size) {
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Group::getName, keyword.trim());
        }
        wrapper.orderByDesc(Group::getId);
        return PageResult.of(groupMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), wrapper));
    }

    public PageResult<AdminMessageVO> pageMessages(Long userId, Long groupId, String keyword,
                                                   String startTime, String endTime, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.and(query -> query.eq(Message::getFromId, userId).or().eq(Message::getToId, userId));
        }
        if (groupId != null) {
            wrapper.eq(Message::getGroupId, groupId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Message::getContent, keyword.trim());
        }
        LocalDateTime start = parseTime(startTime, false);
        LocalDateTime end = parseTime(endTime, true);
        if (start != null) {
            wrapper.ge(Message::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(Message::getCreateTime, end);
        }
        wrapper.orderByDesc(Message::getId);
        Page<Message> result = messageMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), wrapper);

        Set<Long> fromIds = result.getRecords().stream()
                .map(Message::getFromId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        if (!fromIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(fromIds)) {
                String name = user.getNickname() != null && !user.getNickname().isBlank()
                        ? user.getNickname() : user.getUsername();
                names.put(user.getId(), name);
            }
        }
        List<AdminMessageVO> records = result.getRecords().stream().map(message -> {
            AdminMessageVO vo = new AdminMessageVO();
            vo.setId(message.getId());
            vo.setMid(message.getMid());
            vo.setFromId(message.getFromId());
            vo.setFromName(names.get(message.getFromId()));
            vo.setToId(message.getToId());
            vo.setGroupId(message.getGroupId());
            vo.setContent(message.getContent());
            vo.setMsgType(message.getMsgType());
            vo.setCreateTime(message.getCreateTime());
            return vo;
        }).toList();

        PageResult<AdminMessageVO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setRecords(records);
        return pageResult;
    }

    public AdminOverviewDTO overview() {
        AdminOverviewDTO dto = new AdminOverviewDTO();
        dto.setUserCount(userMapper.selectCount(null));
        dto.setGroupCount(groupMapper.selectCount(new LambdaQueryWrapper<Group>().eq(Group::getStatus, 1)));
        dto.setTodayMessageCount(messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .ge(Message::getCreateTime, LocalDate.now().atStartOfDay())));
        return dto;
    }

    private LocalDateTime parseTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text, DATE);
                return endOfDay ? date.plusDays(1).atStartOfDay().minusSeconds(1) : date.atStartOfDay();
            }
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private long normalizePage(int page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
