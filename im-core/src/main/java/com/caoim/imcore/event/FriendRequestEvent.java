package com.caoim.imcore.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FriendRequestEvent extends ApplicationEvent {

    private final Long fromId;
    private final Long toId;

    public FriendRequestEvent(Object source, Long fromId, Long toId) {
        super(source);
        this.fromId = fromId;
        this.toId = toId;
    }
}
