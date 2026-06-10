package com.caoim.imserver.websocket;

import com.alibaba.fastjson2.JSON;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.entity.Message;
import com.caoim.imcore.service.MessageService;
import com.caoim.imcore.service.UserService;
import com.caoim.imcore.util.JwtUtil;
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

    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<WebSocketSession, Long> SESSION_USERS = new ConcurrentHashMap<>();

    public IMWebSocketHandler(MessageService messageService, UserService userService, JwtUtil jwtUtil) {
        this.messageService = messageService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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
                    long iatTime = e.getClaims().getIssuedAt() != null ? e.getClaims().getIssuedAt().getTime() : 0;
                    long nowTime = System.currentTimeMillis();
                    long expiredMs = nowTime - expTime;
                    String expiredInfo = expiredMs > 0 ?
                            String.format("已过期 %d 分钟 (%d秒)", expiredMs / 60000, (expiredMs % 60000) / 1000) :
                            String.format("还未生效 (%d 分钟后生效)", -expiredMs / 60000);
                    String tokenType = e.getClaims().get("type", String.class);
                    Long userIdInToken = e.getClaims().get("userId", Long.class);
                    log.warn("❌ WebSocket 认证失败 - Token已过期: sessionId={}, 当前时间={}, {}",
                            session.getId(), new Date(nowTime), expiredInfo);
                    log.warn("   🔍 Token详情: type={}, userId={}, 签发时间={}, 过期时间={}",
                            tokenType, userIdInToken,
                            iatTime > 0 ? new Date(iatTime) : "未知",
                            e.getClaims().getExpiration());
                    if ("refresh".equals(tokenType)) {
                        log.warn("   ⚠️ 注意: 客户端误将 RefreshToken 当作 AccessToken 使用于 WebSocket 连接!");
                    }
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
                case "group_get":
                    handleGetGroupHistoryMessages(userId, session, msgMap);
                    break;
                case "private_get":
                    handleGetPrivateHistoryMessages(userId, session, msgMap);
                    break;
                case "get_offline_messages":
                    handleGetOfflineMessages(userId, session, msgMap);
                    break;
                case "group_offline_sync":
                    handleGroupOfflineSync(userId, session, msgMap);
                    break;
                case "read_receipt":
                    handleReadReceipt(userId, session, msgMap);
                    break;
                case "delivery_ack":
                    handleDeliveryAck(userId, session, msgMap);
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
            // 提取客户端生成的 mid（雪花算法）
            Long clientMid = null;
            if (msgMap.get("mid") != null) {
                clientMid = Long.valueOf(msgMap.get("mid").toString());
            }

            log.info("处理私聊消息: fromId={}, toId={}, content={}, mid={}", fromId, toId, content, clientMid);

            // 调用消息服务（会保存消息 + 发布事件，由 Listener 负责推送）
            Message message = messageService.sendMessage(fromId, toId, null, content, msgType, clientMid);

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
            // 优先取 groupId，兼容旧版客户端可能将群ID放在 toId 中的情况
            Object groupIdObj = msgMap.get("groupId");
            if (groupIdObj == null) {
                groupIdObj = msgMap.get("toId");
            }
            if (groupIdObj == null) {
                throw new BusinessException("群消息缺少 groupId 参数");
            }
            Long groupId = Long.valueOf(groupIdObj.toString());
            String content = (String) msgMap.get("content");
            Integer msgType = msgMap.get("msgType") != null ?
                    Integer.valueOf(msgMap.get("msgType").toString()) : Constants.MessageType.TEXT;
            // 提取客户端生成的 mid
            Long clientMid = null;
            if (msgMap.get("mid") != null) {
                clientMid = Long.valueOf(msgMap.get("mid").toString());
            }

            log.info("处理群聊消息: fromId={}, groupId={}, content={}, mid={}", fromId, groupId, content, clientMid);

            Message message = messageService.sendMessage(fromId, null, groupId, content, msgType, clientMid);

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
     * 按送达状态判断：delivered=0 的消息为离线消息（用户未收到）
     * 支持基于时间戳和消息ID的增量同步，分页返回
     *
     * @param userId 请求用户ID
     * @param session WebSocket会话
     * @param msgMap 请求参数
     */
    /**
     * 处理获取群聊历史消息请求
     */
    private void handleGetGroupHistoryMessages(Long userId, WebSocketSession session, Map<String, Object> msgMap) {
        try {
            Object groupIdObj = msgMap.get("groupId");
            if (groupIdObj == null) {
                sendError(session, "缺少 groupId 参数");
                return;
            }
            Long groupId = Long.valueOf(groupIdObj.toString());
            int page = msgMap.get("page") != null ? Integer.valueOf(msgMap.get("page").toString()) : 1;
            int size = msgMap.get("size") != null ? Integer.valueOf(msgMap.get("size").toString()) : 20;

            List<Map<String, Object>> messages = messageService.getGroupHistoryMessages(groupId, userId, page, size);
            sendToUser(session, JSON.toJSONString(Map.of(
                "type", "group_history",
                "groupId", groupId,
                "page", page,
                "messages", messages
            )));
        } catch (Exception e) {
            log.error("获取群聊历史消息失败: ", e);
            sendError(session, "获取群聊历史消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取私聊历史消息请求
     */
    private void handleGetPrivateHistoryMessages(Long userId, WebSocketSession session, Map<String, Object> msgMap) {
        try {
            Object targetIdObj = msgMap.get("targetId");
            if (targetIdObj == null) {
                targetIdObj = msgMap.get("toId");
            }
            if (targetIdObj == null) {
                sendError(session, "缺少 targetId 参数");
                return;
            }
            Long targetId = Long.valueOf(targetIdObj.toString());
            int page = msgMap.get("page") != null ? Integer.valueOf(msgMap.get("page").toString()) : 1;
            int size = msgMap.get("size") != null ? Integer.valueOf(msgMap.get("size").toString()) : 20;

            List<Map<String, Object>> messages = messageService.getPrivateHistoryMessages(userId, targetId, page, size);
            sendToUser(session, JSON.toJSONString(Map.of(
                "type", "private_history",
                "targetId", targetId,
                "page", page,
                "messages", messages
            )));
        } catch (Exception e) {
            log.error("获取私聊历史消息失败: ", e);
            sendError(session, "获取私聊历史消息失败: " + e.getMessage());
        }
    }

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
                messagesData.add(messageService.messageToMap(msg));
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
     * 处理群聊离线消息同步请求
     * 客户端传入 groupId + sinceMid，服务端返回该群中 mid > sinceMid 的所有消息
     */
    private void handleGroupOfflineSync(Long userId, WebSocketSession session, Map<String, Object> msgMap) {
        try {
            Object groupIdObj = msgMap.get("groupId");
            if (groupIdObj == null) {
                sendError(session, "缺少 groupId 参数");
                return;
            }

            Long groupId = Long.valueOf(groupIdObj.toString());
            Long sinceMid = 0L;
            if (msgMap.get("sinceMid") != null) {
                sinceMid = Long.valueOf(msgMap.get("sinceMid").toString());
            }

            int limit = 50;
            if (msgMap.get("limit") != null) {
                limit = Integer.valueOf(msgMap.get("limit").toString());
                if (limit <= 0 || limit > 200) limit = 50;
            }

            log.info("处理群聊离线消息同步: userId={}, groupId={}, sinceMid={}, limit={}",
                    userId, groupId, sinceMid, limit);

            // 查询该群的离线消息（mid > sinceMid）
            List<Message> groupOfflineMessages = messageService.getGroupOfflineMessages(
                    groupId, sinceMid, limit);

            // 构建响应数据
            List<Map<String, Object>> messagesData = new ArrayList<>();
            for (Message msg : groupOfflineMessages) {
                messagesData.add(messageService.messageToMap(msg));
            }

            boolean hasMore = groupOfflineMessages.size() >= limit;

            Map<String, Object> response = new HashMap<>();
            response.put("type", "group_offline_sync");
            response.put("groupId", groupId);
            response.put("messages", messagesData);
            response.put("count", groupOfflineMessages.size());
            response.put("hasMore", hasMore);
            response.put("sinceMid", sinceMid);

            String jsonResponse = JSON.toJSONString(response);

            log.info("群聊离线消息同步结果: userId={}, groupId={}, 返回数量={}, hasMore={}",
                    userId, groupId, groupOfflineMessages.size(), hasMore);

            sendToUser(session, jsonResponse);

        } catch (NumberFormatException e) {
            log.error("群聊离线消息参数格式错误: ", e);
            sendError(session, "参数格式错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理群聊离线消息请求异常: ", e);
            sendError(session, "群聊离线消息同步失败: " + e.getMessage());
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
            // 优先使用 mids（客户端生成的全局唯一ID），兼容旧的 messageIds
            Object idsObj = msgMap.get("mids");
            if (idsObj == null) {
                idsObj = msgMap.get("messageIds");
            }

            if (idsObj == null) {
                log.warn("已读回执缺少mids/messageIds参数: userId={}", userId);
                sendError(session, "已读回执缺少mids参数");
                return;
            }

            List<Long> messageIds = new ArrayList<>();

            // 支持两种格式：List 或 单个Long
            if (idsObj instanceof List) {
                for (Object id : (List<?>) idsObj) {
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
            } else if (idsObj instanceof Number) {
                messageIds.add(((Number) idsObj).longValue());
            }

            if (messageIds.isEmpty()) {
                log.warn("已读回执的mids为空: userId={}", userId);
                sendError(session, "消息ID列表不能为空");
                return;
            }

            log.info("收到WebSocket已读回执: userId={}, 消息数={}, 使用mid={}",
                    userId, messageIds.size(), msgMap.containsKey("mids"));

            // 调用完整的markAsRead方法（包含回执记录）
            // 注意：这里传入的可能是 mid 也可能是服务端 id，由 markAsRead 统一处理
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

    /**
     * 处理送达回执（delivery_ack）
     * 接收方收到消息后自动发送，服务端更新 delivered 状态并通知发送方
     *
     * @param userId 发送回执的用户ID（即消息接收方）
     * @param session WebSocket会话
     * @param msgMap 请求数据
     */
    private void handleDeliveryAck(Long userId, WebSocketSession session, Map<String, Object> msgMap) {
        try {
            // 解析 mids 列表
            Object midsObj = msgMap.get("mids");
            if (midsObj == null) {
                log.warn("送达回执缺少mids参数: userId={}", userId);
                return;
            }

            List<Long> mids = new ArrayList<>();
            if (midsObj instanceof List) {
                for (Object id : (List<?>) midsObj) {
                    if (id instanceof Number) {
                        mids.add(((Number) id).longValue());
                    } else if (id != null) {
                        try {
                            mids.add(Long.parseLong(id.toString()));
                        } catch (NumberFormatException e) {
                            log.warn("无效的mid: {}", id);
                        }
                    }
                }
            } else if (midsObj instanceof Number) {
                mids.add(((Number) midsObj).longValue());
            }

            if (mids.isEmpty()) {
                log.warn("送达回执的mids为空: userId={}", userId);
                return;
            }

            log.info("收到送达回执: userId={}, 消息数={}", userId, mids.size());

            // 调用 MessageService 标记已送达
            List<Map<String, Object>> results = messageService.markAsDelivered(mids);

            // 通知发送方：消息已送达
            for (Map<String, Object> result : results) {
                Long fromId = ((Number) result.get("fromId")).longValue();
                Long mid = ((Number) result.get("mid")).longValue();

                Map<String, Object> confirmation = Map.of(
                        "type", "delivery_confirmation",
                        "mid", mid,
                        "status", "delivered",
                        "timestamp", System.currentTimeMillis()
                );

                pushMessageToUser(fromId, JSON.toJSONString(confirmation));
                log.info("已发送送达确认给发送方: fromId={}, mid={}", fromId, mid);
            }

            // 发送确认响应给接收方
            Map<String, Object> response = new HashMap<>();
            response.put("type", "delivery_ack_response");
            response.put("success", true);
            response.put("messageCount", mids.size());

            sendToUser(session, JSON.toJSONString(response));
            log.info("送达回执处理完成: userId={}, 确认消息数={}", userId, mids.size());

        } catch (Exception e) {
            log.error("处理送达回执异常: ", e);
            sendError(session, "处理送达回执失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = SESSION_USERS.remove(session);
        if (userId != null) {
            USER_SESSIONS.remove(userId);
            userService.updateStatus(userId, Constants.UserStatus.OFFLINE);

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
            data = messageService.messageToMap((Message) messageData);
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
            data = messageService.messageToMap((Message) messageData);
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
            // 优先使用客户端 mid，如果没有则用服务端 id
            Long confirmId = msg.getMid() != null && msg.getMid() > 0 ? msg.getMid() : msg.getId();
            confirmation = Map.of(
                    "type", "send_confirmation",
                    "mid", confirmId,
                    "id", msg.getId(),
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
        return session != null && session.isOpen();
    }

    public int getOnlineCount() {
        return USER_SESSIONS.size();
    }

    public int getOnlineUserCount() {
        return USER_SESSIONS.size();
    }

    public Set<Long> getOnlineUserIds() {
        return USER_SESSIONS.keySet();
    }
}
