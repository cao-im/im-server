package com.caoim.imserver.websocket;

import com.alibaba.fastjson2.JSON;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.service.MessageService;
import com.caoim.imcore.service.UserService;
import com.caoim.imcore.util.JwtUtil;
import com.caoim.imserver.service.RedisWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IMWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(IMWebSocketHandler.class);

    private final MessageService messageService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisWebSocketService redisWebSocketService;

    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<WebSocketSession, Long> SESSION_USERS = new ConcurrentHashMap<>();

    public IMWebSocketHandler(MessageService messageService, UserService userService, JwtUtil jwtUtil, RedisWebSocketService redisWebSocketService) {
        this.messageService = messageService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisWebSocketService = redisWebSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getTokenFromSession(session);
        if (token == null || !jwtUtil.validateToken(token)) {
            String uri = session.getUri() != null ? session.getUri().toString() : "null";
            if (token == null) {
                log.warn("❌ WebSocket 认证失败 - Token缺失: sessionId={}, uri={}, 提示: 请在URL中添加token参数(?token=xxx)",
                        session.getId(), uri);
            } else {
                try {
                    jwtUtil.parseToken(token);
                    log.warn("❌ WebSocket 认证失败 - 未知错误: sessionId={}", session.getId());
                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    long expTime = e.getClaims().getExpiration().getTime();
                    long nowTime = System.currentTimeMillis();
                    long expiredMs = nowTime - expTime;
                    String expiredInfo = expiredMs > 0 ?
                            String.format("已过期 %d 分钟", expiredMs / 60000) :
                            String.format("还未生效 (%d 分钟后生效)", -expiredMs / 60000);
                    log.warn("❌ WebSocket 认证失败 - Token已过期: sessionId={}, 过期时间={}, 当前时间={}, {}",
                            session.getId(), e.getClaims().getExpiration(), new Date(nowTime), expiredInfo);
                } catch (io.jsonwebtoken.security.SignatureException e) {
                    log.warn("❌ WebSocket 认证失败 - 签名无效: sessionId={}, 原因: Token签名与服务端secret不匹配，请重新登录",
                            session.getId());
                } catch (io.jsonwebtoken.MalformedJwtException e) {
                    log.warn("❌ WebSocket 认证失败 - Token格式错误: sessionId={}, 原因: {}",
                            session.getId(), e.getMessage());
                } catch (Exception e) {
                    log.warn("❌ WebSocket 认证失败 - 验证异常: sessionId={}, 异常类型={}, 原因: {}",
                            session.getId(), e.getClass().getSimpleName(), e.getMessage());
                }
            }
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        USER_SESSIONS.put(userId, session);
        SESSION_USERS.put(session, userId);
        userService.updateStatus(userId, Constants.UserStatus.ONLINE);

        String sessionId = session.getId();
        redisWebSocketService.userOnline(userId, sessionId);

        log.info("用户 {} 已连接 WebSocket, 当前在线人数: {}", userId, USER_SESSIONS.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> msgMap = JSON.parseObject(message.getPayload(), Map.class);
            Long userId = SESSION_USERS.get(session);
            if (userId == null) return;

            String type = (String) msgMap.get("type");
            switch (type) {
                case "private":
                    handlePrivateMessage(userId, msgMap);
                    break;
                case "group":
                    handleGroupMessage(userId, msgMap);
                    break;
                case "get_offline_messages":
                    handleGetOfflineMessages(userId, session, msgMap);
                    break;
                case "read_receipt":
                    handleReadReceipt(userId, session, msgMap);
                    break;
                case "ping":
                    sendToUser(session, JSON.toJSONString(Map.of("type", "pong")));
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: ", e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    private void handlePrivateMessage(Long fromId, Map<String, Object> msgMap) {
        try {
            Long toId = Long.valueOf(msgMap.get("toId").toString());
            String content = (String) msgMap.get("content");
            Integer msgType = msgMap.get("msgType") != null ?
                    Integer.valueOf(msgMap.get("msgType").toString()) : Constants.MessageType.TEXT;

            log.info("处理私聊消息: fromId={}, toId={}, content={}", fromId, toId, content);

            // 调用消息服务（会保存消息 + 发布事件，由 Listener 负责推送）
            Message message = messageService.sendMessage(fromId, toId, null, content, msgType);

            log.info("消息已保存并发布推送事件: messageId={}, fromId={}, toId={}", 
                    message.getId(), fromId, toId);
            
            // 注意：不再在这里直接推送！
            // 推送逻辑统一由 WebSocketMessageListener (MessageSentEvent) 处理
            // 这样可以避免重复推送问题
            
        } catch (BusinessException e) {
            log.error("私聊消息发送失败: {}", e.getMessage());
            WebSocketSession fromSession = USER_SESSIONS.get(fromId);
            if (fromSession != null && fromSession.isOpen()) {
                sendError(fromSession, e.getMessage());
            }
        } catch (Exception e) {
            log.error("处理私聊消息异常: ", e);
            WebSocketSession fromSession = USER_SESSIONS.get(fromId);
            if (fromSession != null && fromSession.isOpen()) {
                sendError(fromSession, "消息发送失败: " + e.getMessage());
            }
        }
    }

    private void handleGroupMessage(Long fromId, Map<String, Object> msgMap) {
        try {
            Long groupId = Long.valueOf(msgMap.get("groupId").toString());
            String content = (String) msgMap.get("content");
            Integer msgType = msgMap.get("msgType") != null ?
                    Integer.valueOf(msgMap.get("msgType").toString()) : Constants.MessageType.TEXT;

            log.info("处理群聊消息: fromId={}, groupId={}, content={}", fromId, groupId, content);

            Message message = messageService.sendMessage(fromId, null, groupId, content, msgType);

            log.info("群消息已保存并发布推送事件: messageId={}, groupId={}",
                    message.getId(), groupId);

        } catch (BusinessException e) {
            log.error("群聊消息发送失败: {}", e.getMessage());
            WebSocketSession fromSession = USER_SESSIONS.get(fromId);
            if (fromSession != null && fromSession.isOpen()) {
                sendError(fromSession, e.getMessage());
            }
        } catch (Exception e) {
            log.error("处理群聊消息异常: ", e);
            WebSocketSession fromSession = USER_SESSIONS.get(fromId);
            if (fromSession != null && fromSession.isOpen()) {
                sendError(fromSession, "消息发送失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理离线消息拉取请求
     * 支持基于时间戳和消息ID的增量同步，分页返回
     *
     * @param userId 请求用户ID
     * @param session WebSocket会话
     * @param msgMap 请求参数
     */
    private void handleGetOfflineMessages(Long userId, WebSocketSession session, Map<String, Object> msgMap) {
        try {
            // 解析请求参数
            Long sinceTimestamp = 0L;
            Long sinceMessageId = 0L;
            int offset = 0;
            int limit = 50; // 默认每页50条

            if (msgMap.get("since") != null) {
                sinceTimestamp = Long.valueOf(msgMap.get("since").toString());
            }

            if (msgMap.get("sinceMessageId") != null) {
                sinceMessageId = Long.valueOf(msgMap.get("sinceMessageId").toString());
            }

            if (msgMap.get("offset") != null) {
                offset = Integer.valueOf(msgMap.get("offset").toString());
            }

            if (msgMap.get("limit") != null) {
                limit = Integer.valueOf(msgMap.get("limit").toString());
            }

            // 参数校验：限制最大每页数量，防止一次拉取过多
            if (limit <= 0 || limit > 200) {
                log.warn("离线消息请求limit参数异常: {}, 使用默认值50", limit);
                limit = 50;
            }

            if (offset < 0) {
                log.warn("离线消息请求offset参数异常: {}, 使用默认值0", offset);
                offset = 0;
            }

            log.info("处理离线消息拉取请求: userId={}, sinceTimestamp={}, sinceMessageId={}, offset={}, limit={}",
                    userId, sinceTimestamp, sinceMessageId, offset, limit);

            // 查询离线消息
            List<Message> offlineMessages = messageService.getOfflineMessages(
                    userId,
                    sinceTimestamp,
                    sinceMessageId,
                    offset,
                    limit
            );

            // 查询总数（用于判断是否还有更多消息）
            long totalCount = messageService.getOfflineMessagesCount(
                    userId,
                    sinceTimestamp,
                    sinceMessageId
            );

            // 构建响应数据（使用 messageToMap 自动包含 senderInfo/groupInfo）
            List<Map<String, Object>> messagesData = new ArrayList<>();
            for (Message msg : offlineMessages) {
                messagesData.add(MessageService.messageToMap(msg));
            }

            // 判断是否还有更多数据
            boolean hasMore = (offset + offlineMessages.size()) < totalCount;

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "offline_messages");
            response.put("messages", messagesData);
            response.put("count", offlineMessages.size());
            response.put("totalCount", totalCount);
            response.put("hasMore", hasMore);
            response.put("offset", offset);
            response.put("limit", limit);

            String jsonResponse = JSON.toJSONString(response);

            log.info("离线消息查询结果: userId={}, 返回数量={}, 总数={}, hasMore={}",
                    userId, offlineMessages.size(), totalCount, hasMore);

            // 发送响应给客户端
            sendToUser(session, jsonResponse);

        } catch (NumberFormatException e) {
            log.error("离线消息参数格式错误: ", e);
            sendError(session, "参数格式错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理离线消息请求异常: ", e);
            sendError(session, "获取离线消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理已读回执（通过WebSocket发送）
     * 客户端阅读消息后，发送此消息通知服务器
     *
     * @param userId 发送回执的用户ID
     * @param session WebSocket会话
     * @param msgMap 请求数据
     */
    private void handleReadReceipt(Long userId, WebSocketSession session, Map<String, Object> msgMap) {
        try {
            // 解析消息ID列表
            Object messageIdsObj = msgMap.get("messageIds");
            
            if (messageIdsObj == null) {
                log.warn("已读回执缺少messageIds参数: userId={}", userId);
                sendError(session, "已读回执缺少messageIds参数");
                return;
            }

            List<Long> messageIds = new ArrayList<>();
            
            // 支持两种格式：List 或 单个Long
            if (messageIdsObj instanceof List) {
                for (Object id : (List<?>) messageIdsObj) {
                    if (id instanceof Number) {
                        messageIds.add(((Number) id).longValue());
                    } else if (id != null) {
                        try {
                            messageIds.add(Long.parseLong(id.toString()));
                        } catch (NumberFormatException e) {
                            log.warn("无效的消息ID: {}", id);
                        }
                    }
                }
            } else if (messageIdsObj instanceof Number) {
                messageIds.add(((Number) messageIdsObj).longValue());
            }

            if (messageIds.isEmpty()) {
                log.warn("已读回执的messageIds为空: userId={}", userId);
                sendError(session, "消息ID列表不能为空");
                return;
            }

            log.info("收到WebSocket已读回执: userId={}, 消息数={}", userId, messageIds.size());

            // 调用完整的markAsRead方法（包含回执记录）
            messageService.markAsRead(messageIds, userId);

            // 发送确认响应给客户端
            Map<String, Object> response = new HashMap<>();
            response.put("type", "read_receipt_ack");
            response.put("success", true);
            response.put("messageCount", messageIds.size());
            response.put("timestamp", System.currentTimeMillis());

            sendToUser(session, JSON.toJSONString(response));

            log.info("已读回执处理完成: userId={}, 确认消息数={}", userId, messageIds.size());

        } catch (ClassCastException e) {
            log.error("已读回执参数格式错误: ", e);
            sendError(session, "参数格式错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理已读回执异常: ", e);
            sendError(session, "处理已读回执失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = SESSION_USERS.remove(session);
        if (userId != null) {
            USER_SESSIONS.remove(userId);
            userService.updateStatus(userId, Constants.UserStatus.OFFLINE);

            String sessionId = session.getId();
            redisWebSocketService.userOffline(userId, sessionId);

            log.info("用户 {} 断开连接, 原因: {}, 当前在线人数: {}", userId, status, USER_SESSIONS.size());
        } else {
            log.debug("未知会话断开: sessionId={}, 原因: {}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = SESSION_USERS.get(session);

        if (exception instanceof java.net.SocketException) {
            String errorMsg = exception.getMessage();
            if (errorMsg != null && (errorMsg.contains("Connection reset") ||
                errorMsg.contains("Broken pipe") ||
                errorMsg.contains("Connection aborted"))) {
                log.warn("WebSocket 连接异常断开: userId={}, sessionId={}, 原因={}",
                        userId, session.getId(), exception.getMessage());
            } else {
                log.error("WebSocket Socket异常: userId={}, sessionId={}", userId, session.getId(), exception);
            }
        } else {
            log.error("WebSocket 传输错误: userId={}, sessionId={}", userId, session.getId(), exception);
        }

        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.debug("关闭异常会话失败: {}", e.getMessage());
        }

        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    private void sendToUser(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("发送消息失败: ", e);
                }
            }
        }
    }

    public void pushMessageToUser(Long userId, String message) {
        WebSocketSession session = USER_SESSIONS.get(userId);

        log.debug("尝试推送消息给用户 {}: 在线={}, session={}",
                userId,
                session != null && session.isOpen(),
                session != null ? session.getId() : "null");

        if (session != null && session.isOpen()) {
            sendToUser(session, message);
            log.info("✅ 消息已成功推送给用户 {}", userId);
        } else {
            log.warn("⚠️ 用户 {} 不在线或连接已断开，消息无法实时推送 (当前在线用户数: {})",
                    userId, USER_SESSIONS.size());
            log.warn("当前在线用户列表: {}", USER_SESSIONS.keySet());
        }
    }

    public void pushPrivateMessage(Long fromId, Long toId, Object messageData) {
        // 构建包含完整sender信息的推送数据
        Object data;
        if (messageData instanceof Message) {
            data = MessageService.messageToMap((Message) messageData);
        } else {
            data = messageData;
        }

        String message = JSON.toJSONString(Map.of(
                "type", "message",
                "data", data
        ));

        log.info("📡 [WS推送-私聊] fromId={}, toId={}, 推送数据={}", fromId, toId, message);

        pushMessageToUser(toId, message);

        sendSendConfirmation(fromId, messageData);
    }

    public void pushFriendRequest(Long fromId, Long toId) {
        String message = JSON.toJSONString(Map.of(
                "type", "friend_request",
                "data", Map.of(
                        "fromId", fromId,
                        "toId", toId,
                        "timestamp", System.currentTimeMillis()
                )
        ));

        log.info("推送好友请求通知: fromId={}, toId={}", fromId, toId);
        pushMessageToUser(toId, message);
    }

    public void pushFriendAccepted(Long userId, Long friendId) {
        String message = JSON.toJSONString(Map.of(
                "type", "friend_accepted",
                "data", Map.of(
                        "fromId", userId,
                        "toId", friendId,
                        "timestamp", System.currentTimeMillis()
                )
        ));

        log.info("推送好友接受通知: userId={}, friendId={}", userId, friendId);

        pushMessageToUser(friendId, message);
    }

    public void pushFriendRejected(Long userId, Long friendId) {
        String message = JSON.toJSONString(Map.of(
                "type", "friend_rejected",
                "data", Map.of(
                        "fromId", userId,
                        "toId", friendId,
                        "timestamp", System.currentTimeMillis()
                )
        ));

        log.info("推送好友拒绝通知: userId={}, friendId={}", userId, friendId);

        pushMessageToUser(friendId, message);
    }

    public void pushGroupMessage(Long fromId, Long groupId, Object messageData) {
        // 构建包含完整sender信息和group信息的推送数据
        Object data;
        if (messageData instanceof Message) {
            data = MessageService.messageToMap((Message) messageData);
        } else {
            data = messageData;
        }

        String message = JSON.toJSONString(Map.of(
                "type", "group_message",
                "data", data,
                "groupId", groupId
        ));

        log.info("📡 [WS推送-群聊] fromId={}, groupId={}, 推送数据={}", fromId, groupId, message);

        for (Map.Entry<Long, WebSocketSession> entry : USER_SESSIONS.entrySet()) {
            if (!entry.getKey().equals(fromId)) {
                pushMessageToUser(entry.getKey(), message);
            }
        }
        
        sendSendConfirmation(fromId, messageData);
    }
    
    private void sendSendConfirmation(Long fromId, Object messageData) {
        Map<String, Object> confirmation;
        if (messageData instanceof Message) {
            Message msg = (Message) messageData;
            confirmation = Map.of(
                    "type", "send_confirmation",
                    "messageId", msg.getId(),
                    "status", "sent",
                    "timestamp", System.currentTimeMillis()
            );
        } else {
            confirmation = Map.of(
                    "type", "send_confirmation",
                    "status", "sent",
                    "timestamp", System.currentTimeMillis()
            );
        }
        
        pushMessageToUser(fromId, JSON.toJSONString(confirmation));
    }

    private void sendError(WebSocketSession session, String error) {
        Map<String, Object> errorMsg = Map.of(
                "type", "error",
                "message", error
        );
        sendToUser(session, JSON.toJSONString(errorMsg));
    }

    private String getTokenFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    public boolean isUserOnline(Long userId) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            return true;
        }
        return redisWebSocketService.isUserOnline(userId);
    }

    public int getOnlineCount() {
        return redisWebSocketService.getOnlineCount();
    }

    public int getOnlineUserCount() {
        return USER_SESSIONS.size();
    }

    public Set<Long> getOnlineUserIds() {
        return USER_SESSIONS.keySet();
    }
}
