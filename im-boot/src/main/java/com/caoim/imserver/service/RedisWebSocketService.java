package com.caoim.imserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 会话管理服务（基于内存 Map）
 * 不再使用 Redis 存储在线状态，直接通过内存 Map 判断用户在线/推送消息
 */
@Slf4j
@Service
public class RedisWebSocketService {
    // 已废弃，保留类以避免编译错误（后续可完全移除）
}
