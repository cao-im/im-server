package com.caoim.imserver.controller;

import com.caoim.imserver.websocket.IMWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final IMWebSocketHandler webSocketHandler;

    @GetMapping("/online-users")
    public Map<String, Object> getOnlineUsers() {
        return Map.of(
                "onlineCount", webSocketHandler.getOnlineUserCount(),
                "onlineUsers", webSocketHandler.getOnlineUserIds()
        );
    }

    @GetMapping("/check-user/{userId}")
    public Map<String, Object> checkUserOnline(@PathVariable Long userId) {
        return Map.of(
                "userId", userId,
                "isOnline", webSocketHandler.isUserOnline(userId)
        );
    }
}
