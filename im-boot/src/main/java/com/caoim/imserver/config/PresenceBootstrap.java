package com.caoim.imserver.config;

import com.caoim.imcore.service.UserService;
import com.caoim.imserver.websocket.IMWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 进程重启后内存里的连接表是空的，库里残留的在线标记需要立刻纠正。
 */
@Component
@Order(10)
public class PresenceBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PresenceBootstrap.class);

    private final UserService userService;
    private final IMWebSocketHandler webSocketHandler;

    public PresenceBootstrap(UserService userService, IMWebSocketHandler webSocketHandler) {
        this.userService = userService;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void run(ApplicationArguments args) {
        int corrected = userService.markOfflineExcept(webSocketHandler.liveOnlineUserIds());
        log.info("已将 {} 个没有实时连接的用户标为离线", corrected);
    }
}
