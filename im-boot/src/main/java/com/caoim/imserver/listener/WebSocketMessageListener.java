package com.caoim.imserver.listener;

import com.caoim.imcore.event.FriendAcceptedEvent;
import com.caoim.imcore.event.FriendRejectedEvent;
import com.caoim.imcore.event.FriendRequestEvent;
import com.caoim.imcore.event.MessageSentEvent;
import com.caoim.imserver.websocket.IMWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageListener {

    private final IMWebSocketHandler webSocketHandler;

    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        log.info("收到消息发送事件: fromId={}, toId={}, groupId={}",
                event.getFromId(), event.getToId(), event.getGroupId());

        if (event.getToId() != null) {
            log.info("推送私聊消息给用户: toId={}", event.getToId());
            webSocketHandler.pushPrivateMessage(
                    event.getFromId(),
                    event.getToId(),
                    event.getMessage()
            );
        }

        if (event.getGroupId() != null) {
            log.info("广播群消息到群组: groupId={}", event.getGroupId());
            webSocketHandler.pushGroupMessage(
                    event.getFromId(),
                    event.getGroupId(),
                    event.getMessage()
            );
        }
    }

    @EventListener
    public void onFriendRequest(FriendRequestEvent event) {
        log.info("收到好友请求事件: fromId={}, toId={}", event.getFromId(), event.getToId());

        webSocketHandler.pushFriendRequest(event.getFromId(), event.getToId());
    }

    @EventListener
    public void onFriendAccepted(FriendAcceptedEvent event) {
        log.info("好友请求已被接受: userId={}, friendId={}", event.getUserId(), event.getFriendId());

        webSocketHandler.pushFriendAccepted(event.getUserId(), event.getFriendId());
    }

    @EventListener
    public void onFriendRejected(FriendRejectedEvent event) {
        log.info("好友请求已被拒绝: userId={}, friendId={}", event.getUserId(), event.getFriendId());

        webSocketHandler.pushFriendRejected(event.getUserId(), event.getFriendId());
    }
}
