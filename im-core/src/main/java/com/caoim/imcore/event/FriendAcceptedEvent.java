package com.caoim.imcore.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FriendAcceptedEvent extends ApplicationEvent {

    private final Long userId;
    private final Long friendId;

    public FriendAcceptedEvent(Object source, Long userId, Long friendId) {
        super(source);
        this.userId = userId;
        this.friendId = friendId;
    }
}
