package com.caoim.imserver.websocket;

import com.alibaba.fastjson2.JSON;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.service.MessageService;
import com.caoim.imcore.service.UserService;
import com.caoim.imcore.util.JwtUtil;
import com.caoim.imserver.service.RedisWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class IMWebSocketHandler extends TextWebSocketHandler {

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
        Long toId = Long.valueOf(msgMap.get("toId").toString());
        String content = (String) msgMap.get("content");
        Integer msgType = msgMap.get("msgType") != null ?
                Integer.valueOf(msgMap.get("msgType").toString()) : Constants.MessageType.TEXT;

        Message message = messageService.sendMessage(fromId, toId, null, content, msgType);

        Map<String, Object> response = Map.of(
                "type", "message",
                "data", message
        );

        WebSocketSession toSession = USER_SESSIONS.get(toId);
        if (toSession != null && toSession.isOpen()) {
            sendToUser(toSession, JSON.toJSONString(response));
        }

        WebSocketSession fromSession = USER_SESSIONS.get(fromId);
        if (fromSession != null && fromSession.isOpen()) {
            sendToUser(fromSession, JSON.toJSONString(response));
        }
    }

    private void handleGroupMessage(Long fromId, Map<String, Object> msgMap) {
        Long groupId = Long.valueOf(msgMap.get("groupId").toString());
        String content = (String) msgMap.get("content");
        Integer msgType = msgMap.get("msgType") != null ?
                Integer.valueOf(msgMap.get("msgType").toString()) : Constants.MessageType.TEXT;

        Message message = messageService.sendMessage(fromId, null, groupId, content, msgType);

        Map<String, Object> response = Map.of(
                "type", "group_message",
                "data", message,
                "groupId", groupId
        );

        for (Map.Entry<Long, WebSocketSession> entry : USER_SESSIONS.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen() && !entry.getKey().equals(fromId)) {
                sendToUser(session, JSON.toJSONString(response));
            }
        }

        WebSocketSession fromSession = USER_SESSIONS.get(fromId);
        if (fromSession != null && fromSession.isOpen()) {
            sendToUser(fromSession, JSON.toJSONString(response));
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

            log.info("用户 {} 断开连接, 当前在线人数: {}", userId, USER_SESSIONS.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: ", exception);
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
}
