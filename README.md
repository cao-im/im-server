# IM Server - 曹操IM核心服务

> **Maven 多模块项目**：`im-core`（核心库JAR）+ `im-boot`（Spring Boot启动器）

---

## 架构设计

```
im-server/
├── pom.xml              # 父POM
├── im-core/             # 核心库 → 编译为 JAR，供外部依赖
│   └── (entity/service/dao/api/ImService接口)
│
└── im-boot/             # 启动器 → Spring Boot应用，可独立运行
    └── (controller/websocket/config/启动类)
```

### 模块说明

| 模块 | artifactId | 打包方式 | 用途 |
|------|-----------|---------|------|
| **im-core** | `com.caoim:im-core:1.0.0` | `jar` | **IM核心SDK**，包含全部业务逻辑，可被任何Java项目依赖 |
| **im-boot** | `com.caoim:im-boot:1.0.0` | `jar` | Spring Boot启动器，提供REST API + WebSocket，依赖im-core |

### 依赖关系

```
app-server / 第三方项目
       │
       │  Maven依赖
       ▼
   im-core (JAR)  ←── 你引用这个
       │
       │  内部依赖
       ▼
   im-boot (可独立运行的IM服务)
```

---

## 快速开始

### 前置条件

1. 先安装 im-core 到本地Maven仓库：
```bash
cd im-server
mvn clean install -DskipTests
```

2. 确保MySQL和Redis已启动

### 方式一：独立运行 im-boot（完整IM服务）

```bash
cd im-server/im-boot
# 初始化数据库
mysql -u root -p < src/main/resources/schema.sql

# 启动
mvn spring-boot:run
```

访问：http://localhost:8080

- REST API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html
- WebSocket: `ws://localhost:8080/api/ws?token=xxx`

### 方式二：在项目中引入 im-core JAR

在其他项目的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.caoim</groupId>
    <artifactId>im-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

然后通过 `ImService` 接口调用IM能力：

```java
@Autowired
private ImService imService;

// 发送私聊消息
Message msg = imService.sendMessage(fromId, toId, null, "你好", 0);

// 获取会话列表
List<Conversation> list = imService.getConversations(userId);

// 创建群组
Group group = imService.createGroup("技术群", ownerId, memberIds);
```

---

## ImService 公共API

```java
public interface ImService {
    // ===== 用户 =====
    User getUser(Long userId);
    User findByUsername(String username);
    
    // ===== 消息 =====
    Message sendMessage(Long fromId, Long toId, Long groupId, String content, Integer msgType);
    List<Message> getPrivateHistory(Long userId, Long targetId, int page, int size);
    List<Message> getGroupHistory(Long groupId, int page, int size);
    void markAsRead(Long userId, Long conversationId);
    long getUnreadCount(Long userId);
    
    // ===== 会话 =====
    List<Conversation> getConversations(Long userId);
    void clearUnread(Long conversationId);
    void deleteConversation(Long userId, Long conversationId);
    
    // ===== 群组 =====
    Group createGroup(String name, Long ownerId, List<Long> memberIds);
    List<Group> getUserGroups(Long userId);
    void addGroupMembers(Long groupId, List<Long> userIds);
    void removeGroupMember(Long groupId, Long userId);
    
    // ===== 好友 =====
    void sendFriendRequest(Long userId, Long friendId);
    void acceptFriendRequest(Long userId, Long friendId);
    void rejectFriendRequest(Long userId, Long friendId);
    List<Friend> getFriends(Long userId);
    void deleteFriend(Long userId, Long friendId);
}
```

---

## im-boot 提供的REST API

### 用户模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 注册 |
| POST | `/api/user/login` | 登录 |
| GET | `/api/user/{id}` | 用户信息 |
| PUT | `/api/user/status` | 更新在线状态 |

### 消息模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/message/send` | 发送消息 |
| GET | `/api/message/private` | 私聊历史 |
| GET | `/api/message/group` | 群聊历史 |
| PUT | `/api/message/read` | 标记已读 |
| GET | `/api/message/unread` | 未读数 |

### 会话模块
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/conversation/list` | 会话列表 |
| PUT | `/api/conversation/read` | 清除未读 |
| DELETE | `/api/conversation/delete` | 删除会话 |

### 群组模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/group/create` | 创建群组 |
| GET | `/api/group/list` | 我的群组 |
| GET | `/api/group/info` | 群组详情 |
| POST | `/api/group/member/add` | 添加成员 |
| DELETE | `/api/group/member/remove` | 移除成员 |

### 好友模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/friend/request` | 发送好友请求 |
| POST | `/api/friend/accept` | 接受请求 |
| POST | `/api/friend/reject` | 拒绝请求 |
| GET | `/api/friend/list` | 好友列表 |
| GET | `/api/friend/pending` | 待处理请求 |
| DELETE | `/api/friend/delete` | 删除好友 |

### WebSocket
```
端点: ws://host:{port}/api/ws?token=xxx  （{port}为配置的端口号，默认8080）
消息类型:
  - {type:"private", toId, content, msgType}  私聊
  - {type:"group", groupId, content, msgType}   群聊
  - {type:"ping"}                            心跳
```

---

## 数据库表

| 表名 | 说明 |
|------|------|
| `im_user` | IM用户 |
| `im_message` | 消息记录 |
| `im_conversation` | 会话 |
| `im_group` | 群组 |
| `im_group_member` | 群成员 |
| `im_friend` | 好友关系 |

---

## 技术栈

| 技术 | 版本 | 用于 |
|------|------|------|
| Java | 17+ | 运行时 |
| Spring Boot | 3.2.5 | im-boot 启动器 |
| MyBatis-Plus | 3.5.6 | ORM |
| MySQL | 8.0+ | 存储 |
| Redis | 6.0+ | 缓存 |
| JWT | 0.12.5 | Token |
| Lombok | 1.18.34 | 简化代码 |

---

## ⭐ 求 Star

如果这个项目对你有帮助，请帮忙点个 Star 支持一下！你的支持是我持续更新的动力！

## 🤝 联系作者

如果你有任何问题、建议，或者有开发类需求想要合作，欢迎随时联系我：

- **微信**：dxmcpjl
- **QQ**：1529097251

## 📄 License

MIT License
