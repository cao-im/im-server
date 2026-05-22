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
im-server/im-boot/target/im-boot-1.0.0.jar  ← 这就是完整的可执行 JAR！
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
cp im-boot/target/im-boot-1.0.0.jar "$RELEASE_DIR/"
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
  HTTP API: http://localhost:8081/api
  健康检查: http://localhost:8081/api/health
  API文档: http://localhost:8081/api/swagger-ui.html
  WebSocket: ws://localhost:8081/api/ws?token=xxx

配置文件:
  config/application.yml (首次使用请修改数据库密码)

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
# 健康检查: http://localhost:8081/api/health
# API文档: http://localhost:8081/api/swagger-ui.html
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
java -jar im-boot-1.0.0.jar

# 指定配置文件
java -jar im-boot-1.0.0.jar --spring.config.location=config/application.yml

# 自定义 JVM 参数
java -Xms512m -Xmx1024m -jar im-boot-1.0.0.jar

# 后台运行（Linux）
nohup java -jar im-boot-1.0.0.jar > console.log 2>&1 &

# 后台运行（Windows PowerShell）
Start-Process java -ArgumentList "-jar","im-boot-1.0.0.jar" -WindowStyle Hidden
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
  健康检查: http://localhost:8081/api/health
  API文档: http://localhost:8081/api/swagger-ui.html
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
├── im-boot-1.0.0.jar          # 可执行 JAR 包 ★
├── schema.sql                 # 数据库初始化脚本
└── README.txt                 # 说明文档
```

---

## ⚙️ 五、生产环境配置建议

### 1. 修改配置文件

编辑 `config/application.yml`：

```yaml
# 必须修改项
spring:
  datasource:
    url: jdbc:mysql://你的数据库IP:3306/cao_im_db?...
    username: your_db_user
    password: your_secure_password  # ← 强密码
  
  data:
    redis:
      host: your_redis_host
      port: 6379
      password: your_redis_password  # ← 如果有密码

jwt:
  secret: your-very-long-production-secret-key-at-least-256-bits  # ← 更换密钥

# 生产环境建议
server:
  port: 8081  # 或根据需要修改

springdoc:
  swagger-ui:
    enabled: false  # 关闭 Swagger UI
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
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/cao-im-server/im-boot-1.0.0.jar --spring.config.location=config/application.yml
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
nssm install CaoImServer "C:\Program Files\Java\jdk-17\bin\java.exe" "-jar C:\cao-im-server\im-boot-1.0.0.jar"
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
# 只开放必要端口（示例）
iptables -A INPUT -p tcp --dport 8081 -s 允许的IP -j ACCEPT
iptables -A INPUT -p tcp --dport 8081 -j DROP
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
        proxy_pass http://127.0.0.1:8081;
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
# 完整健康检查
curl http://localhost:8081/api/health

# 简易心跳（适合负载均衡器）
curl http://localhost:8081/api/health/ping
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
# 查找占用端口的进程
netstat -tlnp | grep 8081  # Linux
netstat -ano | findstr 8081  # Windows

# 杀掉进程或修改 application.yml 的端口
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
cp new-version.jar im-boot-1.0.0.jar

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
- [ ] 验证健康检查：`curl http://localhost:8081/api/health`
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
2. 健康检查：`http://localhost:8081/api/health`
3. API 文档：`http://localhost:8081/api/swagger-ui.html`（开发环境）

---

**🎉 现在您就可以像野火IM一样，通过一个 JAR + 脚本快速部署曹操IM了！**
