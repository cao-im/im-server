package com.caoim.imcore.event;

import com.caoim.imcore.entity.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessageSentEvent extends ApplicationEvent {

    private final Message message;
    private final Long fromId;
    private final Long toId;
    private final Long groupId;

    public MessageSentEvent(Object source, Message message, Long fromId, Long toId, Long groupId) {
        super(source);
        this.message = message;
        this.fromId = fromId;
        this.toId = toId;
        this.groupId = groupId;
    }
}
