package com.caoim.imserver.service;

import com.caoim.imcore.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisWebSocketService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void userOnline(Long userId, String sessionId) {
        try {
            String userKey = Constants.USER_KEY + userId;
            String sessionKey = Constants.WS_SESSIONS + sessionId;

            redisTemplate.opsForSet().add(Constants.ONLINE_USERS, userId.toString());

            redisTemplate.opsForHash().put(userKey, "userId", userId.toString());
            redisTemplate.opsForHash().put(userKey, "sessionId", sessionId);
            redisTemplate.opsForHash().put(userKey, "connectTime", LocalDateTime.now().format(FORMATTER));
            redisTemplate.opsForHash().put(userKey, "status", String.valueOf(Constants.UserStatus.ONLINE));

            redisTemplate.opsForHash().put(sessionKey, "userId", userId.toString());
            redisTemplate.opsForHash().put(sessionKey, "connectTime", LocalDateTime.now().format(FORMATTER));

            redisTemplate.expire(userKey, 24, TimeUnit.HOURS);
            redisTemplate.expire(sessionKey, 24, TimeUnit.HOURS);

            log.info("用户上线: userId={}, sessionId={}, 当前在线人数: {}", userId, sessionId, getOnlineCount());
        } catch (Exception e) {
            log.error("Redis 记录用户上线失败: userId={}", userId, e);
        }
    }

    public void userOffline(Long userId, String sessionId) {
        try {
            String userKey = Constants.USER_KEY + userId;
            String sessionKey = Constants.WS_SESSIONS + sessionId;

            redisTemplate.opsForSet().remove(Constants.ONLINE_USERS, userId.toString());

            redisTemplate.delete(userKey);
            redisTemplate.delete(sessionKey);

            log.info("用户下线: userId={}, sessionId={}, 当前在线人数: {}", userId, sessionId, getOnlineCount());
        } catch (Exception e) {
            log.error("Redis 记录用户下线失败: userId={}", userId, e);
        }
    }

    public boolean isUserOnline(Long userId) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(Constants.ONLINE_USERS, userId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Redis 查询用户在线状态失败: userId={}", userId, e);
            return false;
        }
    }

    public int getOnlineCount() {
        try {
            Long size = redisTemplate.opsForSet().size(Constants.ONLINE_USERS);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.error("Redis 获取在线人数失败", e);
            return 0;
        }
    }

    public Set<Object> getOnlineUsers() {
        try {
            return redisTemplate.opsForSet().members(Constants.ONLINE_USERS);
        } catch (Exception e) {
            log.error("Redis 获取在线用户列表失败", e);
            return Set.of();
        }
    }

    public Object getUserSessionInfo(Long userId) {
        try {
            String userKey = Constants.USER_KEY + userId;
            return redisTemplate.opsForHash().entries(userKey);
        } catch (Exception e) {
            log.error("Redis 获取用户会话信息失败: userId={}", userId, e);
            return null;
        }
    }
}
