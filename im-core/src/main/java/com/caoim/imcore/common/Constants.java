package com.caoim.imcore.common;

public class Constants {
    public static final String TOKEN_PREFIX = "Bearer ";

    public static class UserStatus {
        public static final int OFFLINE = 0;
        public static final int ONLINE = 1;
        public static final int BUSY = 2;
    }

    public static class MessageType {
        public static final int TEXT = 0;
        public static final int IMAGE = 1;
        public static final int FILE = 2;
        public static final int VOICE = 3;
        public static final int VIDEO = 4;
    }

    public static class MessageStatus {
        public static final int UNREAD = 0;
        public static final int READ = 1;
    }

    public static class FriendStatus {
        public static final int PENDING = 0;
        public static final int ACCEPTED = 1;
        public static final int REJECTED = 2;
        public static final int DELETED = 3;
    }

    public static class GroupRole {
        public static final int MEMBER = 0;
        public static final int ADMIN = 1;
        public static final int OWNER = 2;
    }
}
