# 曹操IM Server 部署指南

## 📦 一、打包可执行 JAR（类似野火IM）

### 方式1️⃣：完整打包（推荐用于部署）

```bash
# 进入项目根目录
cd E:\ProjectsCode\clb_projects\cao_im\im-server

# 打包 im-boot 为可执行 JAR（包含所有依赖）
mvn clean package -pl im-boot -am -DskipTests
```

**✅ 成功后生成的文件：**
```
im-server/im-boot/target/cao-im.jar  ← 这就是完整的可执行 JAR！
```

**📊 JAR 包大小：约 80-100MB（包含所有依赖）**

---

### 方式2️⃣：一键打包+部署脚本

创建 `deploy.sh`（Linux/Mac）或 `deploy.bat`（Windows）：

#### Linux/Mac: deploy.sh
```bash
#!/bin/bash

echo "=========================================="
echo "  曹操IM Server 一键打包部署工具"
echo "=========================================="

# 1. 编译打包
echo ""
echo "[1/4] 正在编译打包..."
mvn clean package -pl im-boot -am -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 打包失败"
    exit 1
fi

# 2. 创建发布目录
RELEASE_DIR="./release/cao-im-server-$(date +%Y%m%d)"
mkdir -p "$RELEASE_DIR"/{bin,config,logs}

# 3. 复制文件
echo "[2/4] 正在复制文件..."
cp im-boot/target/cao-im.jar "$RELEASE_DIR/"
cp bin/caoim-server.sh "$RELEASE_DIR/bin/"
cp config/application.yml.example "$RELEASE_DIR/config/application.yml"

# 4. 复制数据库初始化脚本
if [ -f "im-boot/src/main/resources/schema.sql" ]; then
    cp im-boot/src/main/resources/schema.sql "$RELEASE_DIR/"
fi

# 5. 创建 README
cat > "$RELEASE_DIR/README.txt" << 'EOF'
曹操IM (Cao-IM) Server v1.0.0
============================

快速启动:
  ./bin/caoim-server.sh start

停止服务:
  ./bin/caoim-server.sh stop

查看状态:
  ./bin/caoim-server.sh status

查看日志:
  ./bin/caoim-server.sh logs

服务地址:
  HTTP API: http://localhost:8080/api
  健康检查: http://localhost:8080/api/health
  API文档: http://localhost:8080/api/swagger-ui.html
  WebSocket: ws://localhost:8080/api/ws?token=xxx

配置文件:
  config/application.yml (首次使用请修改数据库密码和端口号)

端口配置:
  默认端口为8080，可在config/application.yml的server.port中修改

初始化数据库:
  mysql -u root -p < schema.sql
EOF

echo "[3/4] 打包完成!"
echo ""
echo "=========================================="
echo "  📦 发布包位置: $RELEASE_DIR"
echo "=========================================="
echo ""
echo "[4/4] 发布包内容:"
ls -lh "$RELEASE_DIR/"
echo ""

# 6. 显示下一步操作
echo "=========================================="
echo "  🚀 下一步操作"
echo "=========================================="
echo ""
echo "1. 进入发布目录:"
echo "   cd $RELEASE_DIR"
echo ""
echo "2. 修改配置:"
echo "   vim config/application.yml"
echo "   # 修改数据库密码、Redis配置等"
echo ""
echo "3. 初始化数据库:"
echo "   mysql -u root -p < schema.sql"
echo ""
echo "4. 启动服务:"
echo "   chmod +x bin/caoim-server.sh"
echo "   ./bin/caoim-server.sh start"
echo ""
echo "=========================================="
```

---

## 🚀 二、快速启动（3种方式）

### 方式A：使用启动脚本（推荐，类似野火IM）

#### Linux / Mac：
```bash
# 1. 进入发布目录
cd release/cao-im-server-20260521/

# 2. 修改配置
cp config/application.yml.example config/application.yml
vim config/application.yml  # 修改数据库密码等

# 3. 初始化数据库
mysql -u root -p < schema.sql

# 4. 启动服务
chmod +x bin/caoim-server.sh
./bin/caoim-server.sh start

# 输出：
# [INFO] Java 版本检查通过: 17.0.13
# [INFO] 正在启动 cao-im-server ...
# ✓ cao-im-server 启动成功! (PID: 12345)
# 日志文件: ./logs/console.log
# 健康检查: http://localhost:8080/api/health  （端口号以配置文件为准）
# API文档: http://localhost:8080/api/swagger-ui.html
```

#### Windows：
```powershell
# 1. 进入发布目录
cd release\cao-im-server-20260521\

# 2. 修改配置
copy config\application.yml.example config\application.yml
notepad config\application.yml  # 修改数据库密码等

# 3. 初始化数据库
mysql -u root -p < schema.sql

# 4. 启动服务
bin\caoim-server.bat start

# 输出：
# [INFO] Java 版本检查通过: 17.0.13
# ========================================
#   ✓ cao-im-server 启动成功!
#   PID: 12345
#   日志文件: .\logs\console.log
# ========================================
```

---

### 方式B：直接运行 JAR（最简单）

```bash
# 基本启动
java -jar cao-im.jar

# 指定配置文件
java -jar cao-im.jar --spring.config.location=config/application.yml

# 自定义 JVM 参数
java -Xms512m -Xmx1024m -jar cao-im.jar

# 后台运行（Linux）
nohup java -jar cao-im.jar > console.log 2>&1 &

# 后台运行（Windows PowerShell）
Start-Process java -ArgumentList "-jar","cao-im.jar" -WindowStyle Hidden
```

---

### 方式C：Maven 直接运行（开发调试）

```bash
# 在项目根目录执行
mvn spring-boot:run -pl im-boot

# 或指定 profile
mvn spring-boot:run -pl im-boot -Dspring-boot.run.profiles=dev
```

---

## 🔧 三、管理命令参考

### 使用启动脚本（推荐）

```bash
# Linux / Mac
./bin/caoim-server.sh start      # 启动
./bin/caoim-server.sh stop       # 停止
./bin/caoim-server.sh restart    # 重启
./bin/caoim-server.sh status     # 状态
./bin/caoim-server.sh logs       # 实时日志
./bin/caoim-server.sh help       # 帮助

# Windows
bin\caoim-server.bat start
bin\caoim-server.bat stop
bin\caoim-server.bat restart
bin\caoim-server.bat status
bin\caoim-server.bat logs
bin\caoim-server.bat help
```

### 输出示例：

#### ✅ 成功启动
```
========================================
  ✓ cao-im-server 启动成功!
  PID: 12345
  日志文件: ./logs/console.log
  健康检查: http://localhost:8080/api/health  （端口号以配置为准）
  API文档: http://localhost:8080/api/swagger-ui.html
========================================
```

#### ✅ 查看状态
```
[INFO] cao-im-server 正在运行 (PID: 12345)
[INFO] 健康状态: "data":"pong"
[INFO] 内存使用: 450 MB
```

---

## 📁 四、发布包目录结构

```
release/cao-im-server-1.0.0/
├── bin/
│   ├── caoim-server.sh        # Linux/Mac 启动脚本
│   └── caoim-server.bat       # Windows 启动脚本
├── config/
│   └── application.yml        # 配置文件（需修改）
├── logs/                      # 日志目录（自动创建）
│   └── console.log            # 控制台日志
├── cao-im.jar              # 可执行 JAR 包 ★
├── schema.sql                 # 数据库初始化脚本
└── README.txt                 # 说明文档
```

---

## ⚙️ 五、生产环境配置建议

### 0️⃣ 外部配置文件机制（重要！⭐）

曹操IM采用**外部配置文件**机制，打包后**无需重新编译**即可修改所有配置！

#### 📁 配置文件位置说明

```
发布包目录/
├── cao-im.jar              # 可执行JAR包（包含默认配置）
├── config/
│   └── application.yml     # ★ 外部配置文件（修改这个！）
├── bin/
└── logs/
```

#### ⚙️ 配置加载优先级（从高到低）

| 优先级 | 方式 | 说明 |
|--------|------|------|
| **1（最高）** | 启动命令行参数 | `java -jar cao-im.jar --server.port=9090` |
| **2** | 环境变量 | `SERVER_PORT=9090 java -jar cao-im.jar` |
| **3（推荐）** | **外部配置文件** | 编辑 `config/application.yml` |
| **4（最低）** | JAR包内配置 | 默认值，仅作fallback |

> ✅ **推荐使用方式3（外部配置文件）**，修改后重启即可生效，最简单方便！

#### 🔧 使用步骤

**Step 1：首次部署时**
```bash
# 发布包中已包含 config/application.yml 模板
# 直接编辑即可：
vim config/application.yml   # Linux/Mac
notepad config\application.yml  # Windows
```

**Step 2：修改配置项**
```yaml
# config/application.yml

# ✅ 修改端口
server:
  port: 8080  # 改成你想要的端口

# ✅ 修改数据库
spring:
  datasource:
    url: jdbc:mysql://你的数据库IP:3306/cao_im_db?...
    username: your_user
    password: your_password  # ← 改成你的密码

# ✅ 修改Redis
  data:
    redis:
      host: your_redis_host
      port: 6379
      password: your_redis_password

# ✅ 修改JWT密钥
jwt:
  secret: your-very-long-secret-key
```

**Step 3：重启服务使配置生效**
```bash
./bin/caoim-server.sh restart
# 或
bin\caoim-server.bat restart
```

#### 💡 常见配置场景示例

**场景1：只改端口**
```bash
# 方式A：编辑 config/application.yml 的 server.port，然后重启

# 方式B：临时测试（不修改配置文件）
java -jar cao-im.jar --server.port=9999
```

**场景2：更换数据库**
```yaml
# 编辑 config/application.yml
spring:
  datasource:
    url: jdbc:mysql://192.168.1.100:3306/cao_im_db?...
    username: prod_user
    password: Prod@SecurePass2024!
# 重启服务
```

**场景3：生产环境关闭Swagger**
```yaml
# 编辑 config/application.yml
springdoc:
  swagger-ui:
    enabled: false
# 重启服务
```

#### ⚠️ 重要提醒

1. **修改IM服务端口后，必须同步更新app-server的配置**
   ```yaml
   # app-server/src/main/resources/application.yml
   im:
     server:
       url: http://localhost:{新端口}/api
   ```

2. **确保端口未被占用**
   ```bash
   netstat -tlnp | grep 8080  # Linux
   netstat -ano | findstr 8080  # Windows
   ```

3. **防火墙放行**
   ```bash
   iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
   ```

4. **推荐的端口范围**
   - 开发环境：`8080`, `8888`, `9000`
   - 生产环境：`8080`, `8081`, `8082` 或根据公司规范
   - 避免使用：`0-1023`（系统保留端口，需要root权限）

5. **配置文件编码**：请确保配置文件使用 **UTF-8** 编码保存

---

### 1️⃣ 端口配置

曹操IM的端口**不再强制锁定**，可以自由配置。默认端口为 **8080**。

#### 修改端口的方法（3种方式）

**方式1：修改外部配置文件（推荐⭐）**
```yaml
# 编辑 config/application.yml
server:
  port: 8080  # ← 修改为你想要的端口号
# 保存后重启服务
```

**方式2：启动命令行参数**
```bash
java -jar cao-im.jar --server.port=9090
```

**方式3：环境变量**
```bash
# Linux/Mac
export SERVER_PORT=9090
java -jar cao-im.jar

# Windows (PowerShell)
$env:SERVER_PORT="9090"
java -jar cao-im.jar
```

### 2. JVM 参数优化

根据服务器内存调整启动脚本中的 `JAVA_OPTS`：

```bash
# 2GB 内存服务器
JAVA_OPTS="-Xms512m -Xmx1024m"

# 4GB 内存服务器
JAVA_OPTS="-Xms1024m -Xmx2048m"

# 8GB 内存服务器
JAVA_OPTS="-Xms2048m -Xmx4096m"

# 高并发优化参数
JAVA_OPTS="-Xms1024m -Xmx2048m \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -XX:+HeapDumpOnOutOfMemoryError \
           -Djava.awt.headless=true"
```

### 3. 系统服务配置（可选）

#### systemd 服务（CentOS/Ubuntu）

创建 `/etc/systemd/system/cao-im.service`：

```ini
[Unit]
Description=Cao IM Server
After=network.target mysql.service redis.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/cao-im-server
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/cao-im-server/cao-im.jar --spring.config.location=config/application.yml
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
# 启用并启动服务
sudo systemctl daemon-reload
sudo systemctl enable cao-im
sudo systemctl start cao-im

# 查看状态和日志
sudo systemctl status cao-im
sudo journalctl -u cao-im -f
```

#### Windows 服务（使用 NSSM）

```powershell
# 下载 nssm: https://nssm.cc/download
nssm install CaoImServer "C:\Program Files\Java\jdk-17\bin\java.exe" "-jar C:\cao-im-server\cao-im.jar"
nssm set CaoImServer AppDirectory "C:\cao-im-server"
nssm set CaoImServer AppStdout "C:\cao-im-server\logs\service.log"
nssm set CaoImServer AppStderr "C:\cao-im-server\logs\service-error.log"
nssm start CaoImServer
```

---

## 🔒 六、安全建议

### 1. 生产环境必须修改的配置

- [ ] **数据库密码** - 使用强密码
- [ ] **Redis 密码** - 启用 Redis 认证
- [ ] **JWT Secret** - 更换为长随机字符串（至少256位）
- [ ] **服务端口** - 根据防火墙规则调整
- [ ] **关闭 Swagger** - 避免暴露 API 文档

### 2. 防火墙配置

```bash
# 只开放必要端口（示例，请将8080替换为实际配置的端口）
iptables -A INPUT -p tcp --dport 8080 -s 允许的IP -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j DROP
```

### 3. HTTPS 配置（推荐）

在生产环境建议使用 Nginx 反向代理 + HTTPS：

```nginx
server {
    listen 443 ssl;
    server_name im.yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;  # ← 修改为实际的IM服务端口
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_read_timeout 86400s;
        proxy_send_timeout 86400s;
    }
}
```

---

## 📊 七、监控与运维

### 1. 健康检查接口

```bash
# 完整健康检查（请将8080替换为实际配置的端口）
curl http://localhost:8080/api/health

# 简易心跳（适合负载均衡器）
curl http://localhost:8080/api/health/ping
```

### 2. 监控指标（可集成 Prometheus）

添加 Micrometer 依赖后，可暴露 `/actuator/metrics` 端点。

### 3. 日志管理

```bash
# 查看实时日志
tail -f logs/console.log

# 搜索错误日志
grep ERROR logs/console.log | tail -100

# 日志轮转（logback 配置）
# 在 src/main/resources/logback-spring.xml 中配置
```

---

## ❓ 八、常见问题

### Q1: 端口被占用？
```bash
# 查找占用端口的进程（请将8080替换为你要使用的端口）
netstat -tlnp | grep 8080  # Linux
netstat -ano | findstr 8080  # Windows

# 解决方案（任选其一）:
# 1. 杀掉占用端口的进程
# 2. 修改 application.yml 的 server.port 为其他端口
# 3. 使用启动参数: java -jar cao-im.jar --server.port=其他端口
```

### Q2: 内存不足？
```bash
# 减小 JVM 内存
JAVA_OPTS="-Xms256m -Xmx512m"
```

### Q3: 数据库连接失败？
- 检查 MySQL 是否启动
- 检查数据库 `cao_im_db` 是否存在
- 检查用户名密码是否正确
- 检查防火墙是否放行 3306 端口

### Q4: 如何更新版本？
```bash
# 1. 停止服务
./bin/caoim-server.sh stop

# 2. 替换 JAR 文件
cp new-version.jar cao-im.jar

# 3. 重启服务
./bin/caoim-server.sh start
```

---

## 🎯 九、完整部署流程清单

### 首次部署：
- [ ] 准备服务器（JDK 17+, MySQL, Redis）
- [ ] 执行打包命令：`mvn clean package`
- [ ] 上传发布包到服务器
- [ ] 修改 `config/application.yml` 配置
- [ ] 初始化数据库：`mysql -u root -p < schema.sql`
- [ ] 启动服务：`./bin/caoim-server.sh start`
- [ ] 验证健康检查：`curl http://localhost:8080/api/health` （端口号以配置为准）
- [ ] 配置防火墙规则
- [ ] （可选）配置 Nginx 反向代理 + HTTPS
- [ ] （可选）配置系统服务开机自启

### 日常维护：
- [ ] 定期备份数据库
- [ ] 监控服务状态和日志
- [ ] 及时更新安全补丁
- [ ] 监控系统资源使用情况

---

## 📞 技术支持

如遇问题请检查：
1. 日志文件：`logs/console.log`
2. 健康检查：`http://localhost:8080/api/health` （端口号以配置为准）
3. API 文档：`http://localhost:8080/api/swagger-ui.html`（开发环境，端口号以配置为准）

---

**🎉 现在您就可以像野火IM一样，通过一个 JAR + 脚本快速部署曹操IM了！端口可自由配置，默认使用8080端口。**
