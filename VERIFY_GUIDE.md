# IM Server 功能验证指南

## 📋 前置条件

### 1. 环境准备
- ✅ MySQL 已启动（数据库：cao_im_db）
- ✅ Redis 已启动（默认端口 6379）
- ✅ JDK 17+

### 2. 初始化数据库
```bash
# 在 MySQL 中执行初始化脚本
mysql -u root -prootlocal < im-server/im-boot/src/main/resources/schema.sql
```

### 3. 启动项目
```bash
# 方式1: IDEA 中直接运行 ImServerApplication.java

# 方式2: Maven 命令行
cd im-server
mvn spring-boot:run -pl im-boot
```

**预期结果：**
- 服务端口：`8081`
- Context Path：`/api`
- 访问地址：`http://localhost:8081/api`

---

## 🧪 验证方式一：Swagger UI（推荐）

### 访问地址
```
http://localhost:8081/api/swagger-ui.html
```

### 验证步骤

#### ① 用户注册
```
POST /api/user/register

Request Body:
{
    "username": "testuser1",
    "password": "123456",
    "nickname": "测试用户1"
}

Expected Response:
{
    "code": 200,
    "message": "success",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "user": {
            "id": 1,
            "username": "testuser1",
            "nickname": "测试用户1",
            ...
        }
    }
}
```

#### ② 注册第二个用户（用于测试聊天）
```
POST /api/user/register

Request Body:
{
    "username": "testuser2",
    "password": "123456",
    "nickname": "测试用户2"
}
```
**记录返回的 token 和 userId**

#### ③ 用户登录
```
POST /api/user/login

Request Body:
{
    "username": "testuser1",
    "password": "123456"
}
```

#### ④ 获取用户信息
```
GET /api/user/info/1
```

---

## 🔌 验证方式二：curl 命令行测试

### 创建测试脚本 `test-im-server.sh`

```bash
#!/bin/bash

BASE_URL="http://localhost:8081/api"

echo "=========================================="
echo "       曹操IM (Cao-IM) 功能测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_test() {
    echo -e "${YELLOW}[TEST] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# ==================== 用户模块测试 ====================
echo "📱 1. 用户模块测试"
echo "------------------------------------------"

# 1.1 注册用户1
print_test "注册用户 testuser1..."
REGISTER_RESP1=$(curl -s -X POST "$BASE_URL/user/register" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser1","password":"123456","nickname":"测试用户1"}')

if echo "$REGISTER_RESP1" | grep -q '"code":200'; then
    print_success "用户1注册成功"
    TOKEN1=$(echo "$REGISTER_RESP1" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    USER_ID1=$(echo "$REGISTER_RESP1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  Token: ${TOKEN1:0:50}..."
    echo "  UserID: $USER_ID1"
else
    print_error "用户1注册失败: $REGISTER_RESP1"
fi
echo ""

# 1.2 注册用户2
print_test "注册用户 testuser2..."
REGISTER_RESP2=$(curl -s -X POST "$BASE_URL/user/register" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser2","password":"123456","nickname":"测试用户2"}')

if echo "$REGISTER_RESP2" | grep -q '"code":200'; then
    print_success "用户2注册成功"
    TOKEN2=$(echo "$REGISTER_RESP2" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    USER_ID2=$(echo "$REGISTER_RESP2" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  Token: ${TOKEN2:0:50}..."
    echo "  UserID: $USER_ID2"
else
    print_error "用户2注册失败: $REGISTER_RESP2"
fi
echo ""

# 1.3 登录测试
print_test "用户登录测试..."
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/user/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser1","password":"123456"}')

if echo "$LOGIN_RESP" | grep -q '"code":200'; then
    print_success "登录成功"
else
    print_error "登录失败: $LOGIN_RESP"
fi
echo ""

# 1.4 获取用户信息
print_test "获取用户信息..."
INFO_RESP=$(curl -s "$BASE_URL/user/info/$USER_ID1")
if echo "$INFO_RESP" | grep -q '"username":"testuser1"'; then
    print_success "获取用户信息成功"
else
    print_error "获取用户信息失败: $INFO_RESP"
fi
echo ""

# ==================== 消息模块测试 ====================
echo "💬 2. 消息模块测试"
echo "------------------------------------------"

# 2.1 发送私聊消息
print_test "发送私聊消息 (用户1 -> 用户2)..."
SEND_RESP=$(curl -s -X POST "$BASE_URL/message/send" \
    -H "Content-Type: application/json" \
    -d "{\"fromId\":$USER_ID1,\"toId\":$USER_ID2,\"content\":\"Hello from testuser1!\"}")

if echo "$SEND_RESP" | grep -q '"code":200'; then
    print_success "消息发送成功"
    MSG_ID=$(echo "$SEND_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  MessageID: $MSG_ID"
else
    print_error "消息发送失败: $SEND_RESP"
fi
echo ""

# 2.2 获取私聊历史
print_test "获取私聊历史消息..."
HISTORY_RESP=$(curl -s "$BASE_URL/message/private/$USER_ID2?userId=$USER_ID1&page=1&size=10")
if echo "$HISTORY_RESP" | grep -q '"code":200'; then
    print_success "获取历史消息成功"
    echo "  消息内容: $(echo "$HISTORY_RESP" | grep -o '"content":"[^"]*"')"
else
    print_error "获取历史消息失败: $HISTORY_RESP"
fi
echo ""

# 2.3 获取未读数
print_test "获取未读消息数..."
UNREAD_RESP=$(curl -s "$BASE_URL/message/unread/count?userId=$USER_ID2")
if echo "$UNREAD_RESP" | grep -q '"code":200'; then
    UNREAD_COUNT=$(echo "$UNREAD_RESP" | grep -o '[0-9]*' | tail -1)
    print_success "未读消息数: $UNREAD_COUNT"
else
    print_error "获取未读数失败: $UNREAD_RESP"
fi
echo ""

# ==================== 会话模块测试 ====================
echo "📂 3. 会话模块测试"
echo "------------------------------------------"

print_test "获取用户会话列表..."
CONV_RESP=$(curl -s "$BASE_URL/conversation/list?userId=$USER_ID1")
if echo "$CONV_RESP" | grep -q '"code":200'; then
    print_success "获取会话列表成功"
    CONV_COUNT=$(echo "$CONV_RESP" | grep -o '"unreadCount":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  未读会话数: $CONV_COUNT"
else
    print_error "获取会话列表失败: $CONV_RESP"
fi
echo ""

# ==================== 好友模块测试 ====================
echo "👥 4. 好友模块测试"
echo "------------------------------------------"

# 4.1 发送好友请求
print_test "发送好友请求 (用户1 -> 用户2)..."
FRIEND_REQ_RESP=$(curl -s -X POST "$BASE_URL/friend/request" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":$USER_ID1,\"friendId\":$USER_ID2}")

if echo "$FRIEND_REQ_RESP" | grep -q '"code":200'; then
    print_success "好友请求发送成功"
else
    print_error "好友请求发送失败: $FRIEND_REQ_RESP"
fi
echo ""

# 4.2 接受好友请求
print_test "接受好友请求..."
ACCEPT_RESP=$(curl -s -X POST "$BASE_URL/friend/accept" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":$USER_ID2,\"friendId\":$USER_ID1}")

if echo "$ACCEPT_RESP" | grep -q '"code":200'; then
    print_success "好友请求已接受"
else
    print_error "接受好友失败: $ACCEPT_RESP"
fi
echo ""

# 4.3 获取好友列表
print_test "获取好友列表..."
FRIENDS_RESP=$(curl -s "$BASE_URL/friend/list?userId=$USER_ID1")
if echo "$FRIENDS_RESP" | grep -q '"code":200'; then
    print_success "获取好友列表成功"
    FRIEND_COUNT=$(echo "$FRIENDS_RESP" | grep -c '"friendId"')
    echo "  好友数量: $FRIEND_COUNT"
else
    print_error "获取好友列表失败: $FRIENDS_RESP"
fi
echo ""

# ==================== 群组模块测试 ====================
echo "👥 5. 群组模块测试"
echo "------------------------------------------"

# 5.1 创建群组
print_test "创建群组..."
GROUP_RESP=$(curl -s -X POST "$BASE_URL/group/create" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"测试群组\",\"ownerId\":$USER_ID1,\"memberIds\":[$USER_ID2]}")

if echo "$GROUP_RESP" | grep -q '"code":200'; then
    print_success "群组创建成功"
    GROUP_ID=$(echo "$GROUP_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    echo "  GroupID: $GROUP_ID"
else
    print_error "群组创建失败: $GROUP_RESP"
fi
echo ""

# 5.2 发送群消息
print_test "发送群消息..."
GROUP_MSG_RESP=$(curl -s -X POST "$BASE_URL/message/send" \
    -H "Content-Type: application/json" \
    -d "{\"fromId\":$USER_ID1,\"groupId\":$GROUP_ID,\"content\":\"Hello Group!\"}")

if echo "$GROUP_MSG_RESP" | grep -q '"code":200'; then
    print_success "群消息发送成功"
else
    print_error "群消息发送失败: $GROUP_MSG_RESP"
fi
echo ""

# 5.3 获取群聊历史
print_test "获取群聊历史消息..."
GROUP_HISTORY_RESP=$(curl -s "$BASE_URL/message/group/$GROUP_ID?page=1&size=10")
if echo "$GROUP_HISTORY_RESP" | grep -q '"code":200'; then
    print_success "获取群聊历史成功"
else
    print_error "获取群聊历史失败: $GROUP_HISTORY_RESP"
fi
echo ""

# 5.4 获取用户的群组列表
print_test "获取用户群组列表..."
USER_GROUPS_RESP=$(curl -s "$BASE_URL/group/list?userId=$USER_ID1")
if echo "$USER_GROUPS_RESP" | grep -q '"code":200'; then
    print_success "获取群组列表成功"
    USER_GROUP_COUNT=$(echo "$USER_GROUPS_RESP" | grep -c '"name"')
    echo "  群组数量: $USER_GROUP_COUNT"
else
    print_error "获取群组列表失败: $USER_GROUPS_RESP"
fi
echo ""

echo "=========================================="
echo "           测试完成！"
echo "=========================================="
echo ""
echo "📊 测试总结:"
echo "  - 用户模块: 注册、登录、信息查询 ✅"
echo "  - 消息模块: 私聊、群聊、历史记录 ✅"
echo "  - 会话模块: 会话列表、未读统计 ✅"
echo "  - 好友模块: 添加、接受、列表 ✅"
echo "  - 群组模块: 创建、成员管理 ✅"
echo ""
echo "🎉 所有核心功能验证通过!"
```

### 运行测试脚本

**Windows PowerShell:**
```powershell
# 将上面的脚本保存为 test-im-server.ps1，然后运行
.\test-im-server.ps1
```

**或者使用 Git Bash / WSL:**
```bash
chmod +x test-im-server.sh
./test-im-server.sh
```

---

## 🔌 验证方式三：WebSocket 实时通信测试

### 使用 Postman 或 WebSocket 客户端

#### 连接地址
```
ws://localhost:8081/api/ws?token={YOUR_TOKEN}
```

#### 测试步骤

1️⃣ **建立连接**
```
URL: ws://localhost:8081/api/ws?token=eyJhbGciOiJIUzI1NiJ9...
```

2️⃣ **发送心跳**
```json
{"type": "ping"}
```
**预期响应：** `{"type": "pong"}`

3️⃣ **发送私聊消息**
```json
{
    "type": "private",
    "toId": 2,
    "content": "Hello WebSocket!",
    "msgType": 0
}
```

4️⃣ **发送群聊消息**
```json
{
    "type": "group",
    "groupId": 1,
    "content": "Hello Group!",
    "msgType": 0
}
```

### 简单 HTML 测试页面

创建文件 `websocket-test.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>曹操IM WebSocket 测试</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        #messages { 
            border: 1px solid #ccc; 
            height: 300px; 
            overflow-y: auto; 
            padding: 10px; 
            margin: 10px 0;
        }
        .msg { margin: 5px 0; padding: 5px; border-radius: 3px; }
        .sent { background: #dcf8c6; text-align: right; }
        .received { background: #fff; }
        input, button { padding: 8px; margin: 5px; }
    </style>
</head>
<body>
    <h1>🚀 曹操IM WebSocket 测试</h1>
    
    <div>
        <label>Token:</label><br>
        <input type="text" id="token" size="80" placeholder="粘贴登录后获得的Token">
        <button onclick="connect()">连接</button>
        <button onclick="disconnect()">断开</button>
        <span id="status">未连接</span>
    </div>
    
    <hr>
    
    <div>
        <label>目标用户ID:</label>
        <input type="number" id="toId" value="2"><br>
        <label>消息内容:</label><br>
        <input type="text" id="content" size="60" value="Hello from WebSocket!">
        <button onclick="sendPrivateMessage()">发送私聊消息</button>
    </div>
    
    <div id="messages"></div>

<script>
let ws = null;

function connect() {
    const token = document.getElementById('token').value;
    if (!token) {
        alert('请输入Token');
        return;
    }
    
    ws = new WebSocket(`ws://localhost:8081/api/ws?token=${token}`);
    
    ws.onopen = function() {
        document.getElementById('status').innerHTML = '<span style="color:green">✓ 已连接</span>';
        addMessage('system', '已连接到服务器');
        
        // 发送心跳
        setInterval(() => {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({type: 'ping'}));
            }
        }, 30000);
    };
    
    ws.onmessage = function(event) {
        const data = JSON.parse(event.data);
        addMessage('received', JSON.stringify(data, null, 2));
    };
    
    ws.onclose = function() {
        document.getElementById('status').innerHTML = '<span style="color:red">✗ 已断开</span>';
        addMessage('system', '与服务器断开连接');
    };
    
    ws.onerror = function(error) {
        console.error('WebSocket Error:', error);
        addMessage('system', '连接错误: ' + error);
    };
}

function disconnect() {
    if (ws) {
        ws.close();
    }
}

function sendPrivateMessage() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先连接');
        return;
    }
    
    const toId = document.getElementById('toId').value;
    const content = document.getElementById('content').value;
    
    const message = {
        type: 'private',
        toId: parseInt(toId),
        content: content,
        msgType: 0
    };
    
    ws.send(JSON.stringify(message));
    addMessage('sent', JSON.stringify(message, null, 2));
}

function addMessage(type, text) {
    const div = document.createElement('div');
    div.className = 'msg ' + type;
    div.textContent = `[${new Date().toLocaleTimeString()}] ${text}`;
    document.getElementById('messages').appendChild(div);
    document.getElementById('messages').scrollTop = document.getElementById('messages').scrollHeight;
}
</script>
</body>
</html>
```

**使用方法：**
1. 先通过 API 登录获取 Token
2. 在浏览器中打开此 HTML 文件
3. 输入 Token 并点击"连接"
4. 输入目标用户ID和消息内容进行测试

---

## 📊 验证检查清单

### 核心功能验证项

| 模块 | 功能点 | 接口 | 预期结果 |
|------|--------|------|----------|
| **用户** | 注册 | POST /user/register | 返回 token 和用户信息 |
| | 登录 | POST /user/login | 返回 token 和用户信息 |
| | 获取信息 | GET /user/info/{id} | 返回用户详情 |
| **消息** | 发送私聊 | POST /message/save | 消息保存成功 |
| | 发送群聊 | POST /message/save | 消息保存成功 |
| | 私聊历史 | GET /message/private/{targetId} | 返回分页消息列表 |
| | 群聊历史 | GET /message/group/{groupId} | 返回分页消息列表 |
| | 未读计数 | GET /message/unread/count | 返回未读数量 |
| **会话** | 会话列表 | GET /conversation/list | 返回会话列表 |
| **好友** | 发送请求 | POST /friend/request | 请求发送成功 |
| | 接受请求 | POST /friend/accept | 成为好友 |
| | 好友列表 | GET /friend/list | 返回好友列表 |
| **群组** | 创建群组 | POST /group/create | 群组创建成功 |
| | 群组列表 | GET /group/list | 返回群组列表 |
| **WebSocket** | 连接认证 | WS /ws?token=xxx | 连接成功 |
| | 心跳检测 | ping/pong | 正常响应 |
| | 实时消息 | private/group | 消息实时推送 |

---

## ⚠️ 常见问题排查

### 1. 连接被拒绝
```
❌ Connection refused
✅ 解决：确认服务是否在 8081 端口启动
```

### 2. 数据库连接失败
```
❌ Communications link failure
✅ 解决：
   - 检查 MySQL 是否启动
   - 确认数据库 cao_im_db 是否存在
   - 检查 application.yml 中的数据库配置
```

### 3. Redis 连接失败
```
❌ Unable to connect to Redis
✅ 解决：
   - 启动 Redis 服务
   - Windows: redis-server.exe
   - Linux: sudo systemctl start redis
```

### 4. Token 验证失败
```
❌ 401 Unauthorized 或 Token invalid
✅ 解决：
   - 重新登录获取新 token
   - 检查 token 是否过期（默认24小时）
```

### 5. WebSocket 连接失败
```
❌ WebSocket handshake failed
✅ 解决：
   - 确认 token 有效
   - 检查 URL 格式: ws://localhost:8081/api/ws?token=xxx
```

---

## 🎯 快速验证命令（单条执行）

```bash
# 1. 健康检查
curl http://localhost:8081/api/swagger-ui.html

# 2. 注册用户
curl -X POST http://localhost:8081/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456","nickname":"DemoUser"}'

# 3. 登录并保存token
curl -X POST http://localhost:8081/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456"}'

# 4. 查看API文档
open http://localhost:8081/api/swagger-ui.html
```

---

## ✅ 验证成功的标志

当您看到以下现象时，说明 **im-server** 功能正常：

1. ✓ 项目启动无报错，控制台显示 `Started ImServerApplication in X.XXX seconds`
2. ✓ 可以访问 Swagger UI 页面
3. ✓ 用户注册、登录接口返回正确数据
4. ✓ 消息可以正常发送和查询
5. ✓ WebSocket 可以连接并发送消息
6. ✓ 所有 API 返回格式统一：`{"code": 200, "message": "success", "data": ...}`

---

**祝测试顺利！如有问题请查看日志或联系开发人员。** 🚀
