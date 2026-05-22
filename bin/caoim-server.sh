#!/bin/bash

# ============================================================================
# 曹操IM (Cao-IM) Server 启动脚本
# 类似野火IM的 wildfirechat.sh
# ============================================================================

# 设置脚本目录
cd "$(dirname "$0")/.."
APP_HOME="$(pwd)"

# 应用配置
APP_NAME="cao-im-server"
JAR_FILE="cao-im.jar"
CONFIG_FILE="config/application.yml"

# JVM 参数（可根据服务器配置调整）
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/"

# 如果存在自定义配置文件，则使用
if [ -f "$CONFIG_FILE" ]; then
    JAVA_OPTS="$JAVA_OPTS --spring.config.location=file:$CONFIG_FILE"
fi

# 日志目录
LOG_DIR="./logs"
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

PID_FILE="$APP_HOME/app.pid"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_info() {
    echo -e "${GREEN}[INFO] $1${NC}"
}

print_warn() {
    echo -e "${YELLOW}[WARN] $1${NC}"
}

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

# 检查 Java 环境
check_java() {
    if type -p java > /dev/null 2>&1; then
        _java=java
    elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
        _java="$JAVA_HOME/bin/java"
    else
        print_error "未找到 Java 运行环境，请安装 JDK 17+"
        exit 1
    fi
    
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$version" < "17" ]]; then
        print_error "Java 版本过低 (当前: $version)，需要 JDK 17+"
        exit 1
    fi
    
    print_info "Java 版本检查通过: $version"
}

# 获取 PID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    else
        echo ""
    fi
}

# 检查进程是否运行
is_running() {
    pid=$(get_pid)
    if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 启动服务
start() {
    if is_running; then
        print_warn "$APP_NAME 正在运行中 (PID: $(get_pid))"
        return 1
    fi
    
    check_java
    
    # 检查 JAR 文件是否存在
    if [ ! -f "$JAR_FILE" ]; then
        print_error "找不到 JAR 文件: $JAR_FILE"
        print_error "请先执行 mvn clean package 或从发布包解压"
        exit 1
    fi
    
    print_info "正在启动 $APP_NAME ..."
    print_info "JVM 参数: $JAVA_OPTS"
    
    # 后台启动
    nohup $_java $JAVA_OPTS -jar "$JAR_FILE" \
        --spring.profiles.active=prod \
        > "$LOG_DIR/console.log" 2>&1 &
    
    echo $! > "$PID_FILE"
    
    # 等待启动完成
    sleep 3
    
    if is_running; then
        print_info "$APP_NAME 启动成功! (PID: $(get_pid))"
        print_info "日志文件: $LOG_DIR/console.log"
        print_info "健康检查: http://localhost/api/health/check"
        print_info "API文档: http://localhost/api/swagger-ui.html"
    else
        print_error "$APP_NAME 启动失败，请查看日志: $LOG_DIR/console.log"
        exit 1
    fi
}

# 停止服务
stop() {
    if ! is_running; then
        print_warn "$APP_NAME 未运行"
        return 0
    fi
    
    pid=$(get_pid)
    print_info "正在停止 $APP_NAME (PID: $pid) ..."
    
    # 发送终止信号
    kill "$pid" 2>/dev/null
    
    # 等待进程结束
    for i in {1..30}; do
        if ! is_running; then
            rm -f "$PID_FILE"
            print_info "$APP_NAME 已停止"
            return 0
        fi
        sleep 1
    done
    
    # 强制杀死
    print_warn "正常停止失败，强制结束进程..."
    kill -9 "$pid" 2>/dev/null
    rm -f "$PID_FILE"
    print_info "$APP_NAME 已强制停止"
}

# 重启服务
restart() {
    stop
    sleep 3
    start
}

# 查看状态
status() {
    if is_running; then
        pid=$(get_pid)
        print_info "$APP_NAME 正在运行 (PID: $pid)"
        
        # 尝试获取健康状态
        if command -v curl > /dev/null 2>&1; then
            health=$(curl -s http://localhost/api/health/ping 2>/dev/null | grep -o '"data":"[^"]*"')
            if [ -n "$health" ]; then
                print_info "健康状态: $health"
            fi
        fi
        
        # 显示内存使用
        if [ -n "$pid" ]; then
            mem=$(ps -o rss= -p "$pid" 2>/dev/null | awk '{printf "%.0f MB", $1/1024}')
            print_info "内存使用: $mem"
        fi
    else
        print_warn "$APP_NAME 未运行"
    fi
}

# 查看实时日志
logs() {
    tail -f "$LOG_DIR/console.log"
}

# 显示帮助信息
help() {
    echo ""
    echo "================================================================================"
    echo "  曹操IM (Cao-IM) Server 管理脚本"
    echo "================================================================================"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|help}"
    echo ""
    echo "命令:"
    echo "  start   - 启动服务"
    echo "  stop    - 停止服务"
    echo "  restart - 重启服务"
    echo "  status  - 查看运行状态"
    echo "  logs    - 查看实时日志 (Ctrl+C 退出)"
    echo "  help    - 显示帮助信息"
    echo ""
    echo "示例:"
    echo "  ./bin/caoim-server.sh start     # 启动服务"
    echo "  ./bin/caoim-server.sh status    # 查看状态"
    echo "  ./bin/caoim-server.sh logs      # 查看日志"
    echo ""
    echo "配置文件:"
    echo "  config/application.yml          # 自定义配置（可选）"
    echo ""
    echo "服务端口:"
    echo "  HTTP: 80 (标准HTTP端口，不可修改)"
    echo "  WebSocket: ws://host/api/ws"
    echo ""
    echo "⚠️  重要提示:"
    echo "  - 本服务强制使用 80 端口，无法通过配置修改"
    echo "  - 建议单独使用一台服务器部署 IM 服务"
    echo "  - 避免与其他 Web 服务（Nginx/Apache）端口冲突"
    echo "  - 生产环境建议使用域名 + HTTPS 反向代理"
    echo ""
    echo "================================================================================"
}

# 主逻辑
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    *)
        help
        ;;
esac

exit 0
