# 曹操IM SDK (im-core) 打包与使用指南

## 📦 一、打包 SDK（JAR 文件）

### 方式1️⃣：安装到本地 Maven 仓库（推荐用于本地开发）

#### 步骤1：进入项目根目录
```bash
cd E:\ProjectsCode\clb_projects\cao_im\im-server
```

#### 步骤2：编译并安装到本地仓库
```bash
# Windows PowerShell
mvn clean install -pl im-core -am

# 或使用完整命令
mvn clean install -DskipTests
```

**命令说明：**
- `clean` - 清理之前的构建文件
- `install` - 编译打包并安装到本地 Maven 仓库
- `-pl im-core` - 只构建 im-core 模块
- `-am` - 同时构建 im-core 所依赖的模块
- `-DskipTests` - 跳过测试（可选）

#### ✅ 成功标志
看到以下输出说明打包成功：
```
[INFO] --- maven-install-plugin:3.1.1:install (default-install) @ im-core ---
[INFO] Installing E:\ProjectsCode\clb_projects\cao_im\im-server\im-core\target\im-core-1.0.0.jar 
[INFO] to C:\Users\32143\.m2\repository\com\caoim\im-core\1.0.0\im-core-1.0.0.jar
[INFO] BUILD SUCCESS
```

**生成的 JAR 文件位置：**
- 本地仓库：`C:\Users\你的用户名\.m2\repository\com\caoim\im-core\1.0.0\`
- 目标目录：`E:\ProjectsCode\clb_projects\cao_im\im-server\im-core\target\`

---

### 方式2️⃣：只生成 JAR 文件（不安装到仓库）

```bash
# 只打包，不安装
mvn clean package -pl im-core -am -DskipTests
```

**生成的 JAR 文件位置：**
```
E:\ProjectsCode\clb_projects\cao_im\im-server\im-core\target\im-core-1.0.0.jar
```

---

### 方式3️⃣：发布到私有 Maven 仓库（团队协作）

#### 配置 settings.xml
编辑 `C:\Users\你的用户名\.m2\settings.xml`：

```xml
<servers>
    <server>
        <id>nexus-releases</id>
        <username>admin</username>
        <password>your-password</password>
    </server>
</servers>
```

#### 在父 pom.xml 中添加分发配置
```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>http://your-nexus-server/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</url>
        <url>http://your-nexus-server/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

#### 发布命令
```bash
mvn clean deploy -pl im-core -am -DskipTests
```

---

## 🔌 二、在项目中使用 SDK

### 示例：在 app-server 中引入 im-core

#### 1️⃣ 添加依赖（app-server/pom.xml）

```xml
<dependencies>
    <!-- 其他依赖... -->
    
    <!-- 曹操IM核心SDK -->
    <dependency>
        <groupId>com.caoim</groupId>
        <artifactId>im-core</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### 2️⃣ 使用 SDK 提供的服务

```java
package com.caoim.appserver.service;

import com.caoim.imcore.api.ImService;
import com.caoim.imcore.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppImBridgeService {

    // 注入IM核心服务接口
    @Autowired
    private ImService imService;

    /**
     * 获取IM用户信息
     */
    public User getImUser(Long userId) {
        return imService.getUser(userId);
    }

    /**
     * 发送私聊消息
     */
    public Message sendPrivateMessage(Long fromId, Long toId, String content) {
        return imService.sendMessage(fromId, toId, null, content, 0);
    }

    /**
     * 发送群聊消息
     */
    public Message sendGroupMessage(Long fromId, Long groupId, String content) {
        return imService.sendMessage(fromId, null, groupId, content, 0);
    }

    /**
     * 获取私聊历史记录
     */
    public List<Message> getPrivateHistory(Long userId, Long targetId, int page, int size) {
        return imService.getPrivateHistory(userId, targetId, page, size);
    }

    /**
     * 获取会话列表
     */
    public List<Conversation> getConversations(Long userId) {
        return imService.getConversations(userId);
    }

    /**
     * 获取未读消息数
     */
    public long getUnreadCount(Long userId) {
        return imService.getUnreadCount(userId);
    }
}
```

---

## 📋 三、SDK 包含的功能模块

### 🎯 核心API接口（ImService）

| 功能分类 | 方法 | 说明 |
|---------|------|------|
| **用户管理** | `getUser(Long userId)` | 获取用户信息 |
| | `findByUsername(String username)` | 根据用户名查找 |
| **消息服务** | `sendMessage(...)` | 发送消息（私聊/群聊） |
| | `getPrivateHistory(...)` | 获取私聊历史 |
| | `getGroupHistory(...)` | 获取群聊历史 |
| | `getUnreadCount(Long userId)` | 获取未读数 |
| **会话管理** | `getConversations(Long userId)` | 获取会话列表 |
| | `markAsRead(...)` | 标记已读 |
| **群组功能** | `createGroup(...)` | 创建群组 |
| | `getUserGroups(Long userId)` | 获取用户的群组 |
| | `addGroupMembers(...)` | 添加群成员 |
| | `removeGroupMember(...)` | 移除群成员 |
| **好友系统** | `sendFriendRequest(...)` | 发送好友请求 |
| | `acceptFriendRequest(...)` | 接受好友请求 |
| | `rejectFriendRequest(...)` | 拒绝好友请求 |
| | `getFriends(Long userId)` | 获取好友列表 |
| | `deleteFriend(...)` | 删除好友 |

### 🩺 IM服务健康检查（HTTP接口）

SDK 的运行依赖 im-boot 服务，在使用 SDK 前，应先确认 IM 服务是否正常。im-boot 提供以下健康检查接口：

| 接口 | 方法 | URL | 说明 |
|------|------|-----|------|
| **完整健康检查** | GET | `/api/health/check` | 返回服务状态、版本、时间等详细信息 |
| **心跳检测** | GET | `/api/health/ping` | 返回 `pong`，用于快速探活 |

#### 健康检查响应示例

**GET /api/health/check**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "status": "UP",
        "service": "曹操IM (Cao-IM) Server",
        "version": "1.0.0",
        "timestamp": "2026-05-22T00:21:00",
        "description": "即时通讯核心服务运行正常"
    }
}
```

**GET /api/health/ping**
```json
{
    "code": 200,
    "message": "success",
    "data": "pong"
}
```

#### 在 app-server 中集成健康检查

```java
package com.caoim.appserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ImServerHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(ImServerHealthCheck.class);

    @Value("${im.server.url:http://localhost:8081}")
    private String imServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 检查IM服务是否可用
     * @return true=服务正常, false=服务不可用
     */
    public boolean isImServerHealthy() {
        try {
            String url = imServerUrl + "/api/health/ping";
            String response = restTemplate.getForObject(url, String.class);
            return response != null && response.contains("pong");
        } catch (Exception e) {
            log.warn("IM服务不可用: {} - {}", imServerUrl, e.getMessage());
            return false;
        }
    }

    /**
     * 获取IM服务详细状态
     */
    public Map<String, Object> getImServerStatus() {
        try {
            String url = imServerUrl + "/api/health/check";
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("获取IM服务状态失败: {}", e.getMessage());
            return null;
        }
    }
}
```

#### application.yml 配置

```yaml
# IM服务地址（app-server 配置）
im:
  server:
    url: http://localhost:8081  # im-boot 服务的地址
```

#### 启动时自动检查示例

```java
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImServerStartupCheck {

    @Autowired
    private ImServerHealthCheck healthCheck;

    @PostConstruct
    public void check() {
        if (healthCheck.isImServerHealthy()) {
            log.info("✓ IM服务连接正常");
        } else {
            log.warn("✗ IM服务不可用，部分功能将无法使用");
        }
    }
}
```

#### 使用场景说明

| 场景 | 推荐接口 | 说明 |
|------|----------|------|
| **应用启动前检查** | `/api/health/ping` | 轻量快速，适合启动探活 |
| **负载均衡器探活** | `/api/health/ping` | K8s/Nginx 就绪探针 |
| **监控告警** | `/api/health/check` | 可获取版本号等详细信息 |
| **运维排查** | `/api/health/check` | 查看服务状态和时间戳 |

### 📦 包含的实体类

- `User` - 用户实体
- `Message` - 消息实体
- `Conversation` - 会话实体
- `Group` - 群组实体
- `GroupMember` - 群成员实体
- `Friend` - 好友关系实体

### 🔧 工具类

- `JwtUtil` - JWT Token 工具类
- `Constants` - 常量定义
- `Result` / `ErrorCode` / `BusinessException` - 统一响应和异常处理

---

## ⚙️ 四、配置要求

### 使用 SDK 的项目需要配置：

#### 1️⃣ 数据库配置（application.yml）
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/cao_im_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

#### 2️⃣ Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
```

#### 3️⃣ MyBatis-Plus 配置
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
```

#### 4️⃣ 组件扫描（重要！）
```java
@SpringBootApplication
@MapperScan("com.caoim.imcore.dao")
@ComponentScan(basePackages = {"com.caoim.yourproject", "com.caoim.imcore"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

---

## 🚀 五、快速开始示例

### 完整的使用流程

#### Step 1: 打包 SDK
```bash
cd im-server
mvn clean install -pl im-core -am -DskipTests
```

#### Step 2: 在新项目中引用
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.caoim</groupId>
    <artifactId>im-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Step 3: 初始化数据库
```sql
-- 执行 schema.sql 创建表结构
source im-server/im-boot/src/main/resources/schema.sql
```

#### Step 4: 编写业务代码
```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ImService imService;

    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestBody MessageDTO dto) {
        Message msg = imService.sendMessage(
            dto.getFromId(), 
            dto.getToId(), 
            dto.getGroupId(),
            dto.getContent(), 
            0
        );
        return Result.success(msg);
    }
    
    @GetMapping("/history/{targetId}")
    public Result<List<Message>> getHistory(
            @RequestParam Long userId,
            @PathVariable Long targetId) {
        return Result.success(imService.getPrivateHistory(userId, targetId, 1, 20));
    }
}
```

#### Step 5: 启动项目测试
```bash
# 启动您的应用
mvn spring-boot:run

# 测试接口
curl http://localhost:8080/api/health
```

---

## 🔍 六、验证 SDK 是否正确引入

### 检查清单

✅ **1. JAR 文件存在**
```bash
# 检查本地Maven仓库
dir C:\Users\你的用户名\.m2\repository\com\caoim\im-core\1.0.0\
# 应该能看到：
# - im-core-1.0.0.jar
# - im-core-1.0.0.pom
```

✅ **2. IDE 中无报错**
- IDEA/Eclipse 中导入 `com.caoim.imcore.api.ImService` 无红色
- 可以正常使用 `@Autowired` 注入

✅ **3. 项目可以启动**
```bash
mvn spring-boot:run
# 控制台无 Bean 注入错误
```

✅ **4. 功能可用**
```java
// 测试代码
@Autowired
ImService imService;

// 调用方法不抛异常
User user = imService.getUser(1L);
System.out.println(user.getUsername());
```

---

## ❓ 七、常见问题

### Q1: 找不到 im-core 依赖？
**A:** 确保已执行 `mvn install` 命令，并且版本号正确。

### Q2: Bean 注入失败？
**A:** 检查是否添加了 `@ComponentScan("com.caoim.imcore")` 和 `@MapperScan("com.caoim.imcore.dao")`。

### Q3: 数据库连接失败？
**A:** 确保 MySQL 服务已启动，数据库 `cao_im_db` 已创建，且配置正确。

### Q4: Redis 连接失败？
**A:** 确保 Redis 服务已启动，检查 application.yml 中的 Redis 配置。

### Q5: 想更新 SDK 版本？
**A:** 
1. 修改 `pom.xml` 中的 `<version>1.0.1</version>`
2. 重新执行 `mvn clean install`
3. 在使用方更新版本号

---

## 📊 八、版本管理建议

### 版本号规则
- `1.0.0` - 正式版
- `1.0.1-SNAPSHOT` - 开发版
- `1.0.1-RC1` - 发布候选版

### 发布流程
```bash
# 开发阶段
mvn clean install  # 1.0.1-SNAPSHOT

# 准备发布
# 修改版本号为 1.0.1
mvn clean deploy   # 发布到正式仓库
```

---

## 🎯 九、最佳实践

### ✅ DO（推荐做法）
1. **先 install 再使用** - 确保本地仓库有 JAR
2. **统一版本号** - 所有项目使用相同版本的 SDK
3. **编写文档** - 为团队提供使用指南
4. **单元测试** - SDK 自身要有完善的测试覆盖
5. **语义化版本** - 遵循 SemVer 规范（主版本.次版本.修订号）

### ❌ DON'T（避免做法）
1. 不要直接复制 JAR 文件到项目
2. 不要修改 SDK 内部代码后不更新版本号
3. 不要在生产环境使用 SNAPSHOT 版本
4. 不要忽略 SDK 的配置要求

---

## 📞 技术支持

如遇到问题：
1. 检查日志输出
2. 查看 [VERIFY_GUIDE.md](./VERIFY_GUIDE.md)
3. 联系开发团队

---

**🎉 现在就开始打包和使用曹操IM SDK吧！**
