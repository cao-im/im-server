package com.caoim.imcore.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_INVALID(1005, "Token无效"),

    FRIEND_REQUEST_EXISTS(2001, "好友请求已存在"),
    ALREADY_FRIENDS(2002, "已经是好友关系"),
    NOT_FRIENDS(2003, "不是好友关系"),

    GROUP_NOT_FOUND(3001, "群组不存在"),
    NOT_GROUP_MEMBER(3002, "不是群成员"),
    ALREADY_IN_GROUP(3003, "已经在群组中"),

    MESSAGE_SEND_FAILED(4001, "消息发送失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
