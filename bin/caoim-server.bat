@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================================
:: 曹操IM (Cao-IM) Server 启动脚本 (Windows版)
:: 类似野火IM的 wildfirechat.bat
:: ============================================================================

:: 设置脚本目录
cd /d "%~dp0.."
set APP_HOME=%CD%

:: 应用配置
set APP_NAME=cao-im-server
set JAR_FILE=cao-im.jar
set CONFIG_FILE=config\application.yml

:: JVM 参数（可根据服务器配置调整）
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/

:: 日志目录
set LOG_DIR=.\logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set PID_FILE=%APP_HOME%\app.pid

:: 颜色定义（Windows 10+）
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set RED=%ESC%[31m
set GREEN=%ESC%[32m
set YELLOW=%ESC%[33m
set NC=%ESC%[0m

:: 检查 Java 环境
:check_java
where java >nul 2>&1
if %errorlevel% neq 0 (
    if defined JAVA_HOME (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
        echo %RED%[ERROR] 未找到 Java 运行环境，请安装 JDK 17+%NC%
        exit /b 1
    )
) else (
    set "JAVA_CMD=java"
)

:: 获取 Java 版本
for /f "tokens=3" %%v in ('"%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%v"
)

echo %JAVA_VERSION% | findstr "17 18 19 20 21" >nul
if %errorlevel% neq 0 (
    echo %RED%[ERROR] Java 版本过低（当前: %JAVA_VERSION%），需要 JDK 17+%NC%
    exit /b 1
)

echo %GREEN%[INFO] Java 版本检查通过: %JAVA_VERSION%%NC%

:: 获取 PID 函数模拟
:get_pid
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
) else (
    set PID=
)
goto :eof

:: 启动服务
:start
call :get_pid
if defined PID (
    tasklist /FI "PID eq %PID%" 2>nul | find /I "%PID%" >nul
    if !errorlevel! equ 0 (
        echo %YELLOW%[WARN] %APP_NAME% 正在运行中 (PID: %PID%)%NC%
        goto :eof
    )
)

call :check_java

:: 检查 JAR 文件是否存在
if not exist "%JAR_FILE%" (
    echo %RED%[ERROR] 找不到 JAR 文件: %JAR_FILE%%NC%
    echo %RED%[ERROR] 请先执行 mvn clean package 或从发布包解压%NC%
    exit /b 1
)

echo %GREEN%[INFO] 正在启动 %APP_NAME% ...%NC%
echo %GREEN%[INFO] JVM 参数: %JAVA_OPTS%%NC%

:: 后台启动
start /B "" "%JAVA_CMD%" %JAVA_OPTS% -jar "%JAR_FILE%" --spring.profiles.active=prod > "%LOG_DIR%\console.log" 2>&1

:: 获取新进程的 PID（Windows 方法）
timeout /t 2 /nobreak >nul
for /f "tokens=2" %%i in ('wmic process where "commandline like '%%im-boot%%' and name='java.exe'" get ProcessId /value ^| findstr ProcessId') do (
    set NEW_PID=%%i
)

if defined NEW_PID (
    echo %NEW_PID%>"%PID_FILE%"
    
    :: 等待启动完成
    timeout /t 3 /nobreak >nul
    
    :: 简单检查进程是否还在运行
    tasklist /FI "PID eq %NEW_PID%" 2>nul | find /I "%NEW_PID%" >nul
    if !errorlevel! equ 0 (
        echo.
        echo %GREEN%========================================%NC%
        echo %GREEN%  ✓ %APP_NAME% 启动成功!%NC%
        echo %GREEN%  PID: %NEW_PID%%NC%
        echo %GREEN%  日志文件: %LOG_DIR%\console.log%NC%
        echo %GREEN%  健康检查: http://localhost/api/health/check%NC%
        echo %GREEN%  API文档: http://localhost/api/swagger-ui.html%NC%
        echo %GREEN%========================================%NC%
        echo.
    ) else (
        echo %RED%[ERROR] %APP_NAME% 启动失败，请查看日志: %LOG_DIR%\console.log%NC%
        exit /b 1
    )
) else (
    echo %RED%[ERROR] 无法获取进程ID，启动可能失败%NC%
    exit /b 1
)

goto :eof

:: 停止服务
:stop
call :get_pid
if not defined PID (
    echo %YELLOW%[WARN] %APP_NAME% 未运行%NC%
    goto :eof
)

echo %GREEN%[INFO] 正在停止 %APP_NAME% (PID: %PID%) ...%NC%

taskkill /PID %PID% /F >nul 2>&1
if %errorlevel% equ 0 (
    del "%PID_FILE%" 2>nul
    echo %GREEN%[INFO] %APP_NAME% 已停止%NC%
) else (
    echo %YELLOW%[WARN] 进程可能已结束%NC%
    del "%PID_FILE%" 2>nul
)

goto :eof

:: 重启服务
:restart
call :stop
timeout /t 3 /nobreak >nul
call :start
goto :eof

:: 查看状态
:status
call :get_pid
if defined PID (
    tasklist /FI "PID eq %PID%" 2>nul | find /I "%PID%" >nul
    if !errorlevel! equ 0 (
        echo %GREEN%[INFO] %APP_NAME% 正在运行 (PID: %PID%)%NC%
        
        :: 尝试使用 curl 检查健康状态
        where curl >nul 2>&1
        if !errorlevel! equ 0 (
            for /f "delims=" %%h in ('curl -s http://localhost/api/health/ping 2^>nul') do (
                echo %%h | findstr "pong" >nul && echo %GREEN%[INFO] 健康状态: OK%NC%
            )
        )
        
        :: 显示内存使用
        for /f "tokens=5" %%m in ('tasklist /FI "PID eq %PID%" /FO LIST ^| findstr "内存使用"') do (
            echo %GREEN%[INFO] 内存使用: %%m%NC%
        )
    ) else (
        echo %YELLOW%[WARN] %APP_NAME% 未运行（PID文件存在但进程不存在）%NC%
        del "%PID_FILE%" 2>nul
    )
) else (
    echo %YELLOW%[WARN] %APP_NAME% 未运行%NC%
)

goto :eof

:: 查看日志
:logs
if exist "%LOG_DIR%\console.log" (
    type "%LOG_DIR%\console.log" | more
) else (
    echo %YELLOW%[WARN] 日志文件不存在%NC%
)

goto :eof

:: 显示帮助信息
:help
echo.
echo ================================================================================
echo   曹操IM (Cao-IM) Server 管理脚本 (Windows)
echo ================================================================================
echo.
echo 用法: %~nx0 {start^|stop^|restart^|status^|logs^|help}
echo.
echo 命令:
echo   start   - 启动服务
echo   stop    - 停止服务
echo   restart - 重启服务
echo   status  - 查看运行状态
echo   logs    - 查看日志
echo   help    - 显示帮助信息
echo.
echo 示例:
echo   bin\caoim-server.bat start     # 启动服务
echo   bin\caoim-server.bat status    # 查看状态
echo   bin\caoim-server.bat logs      # 查看日志
echo.
echo 配置文件:
echo   config\application.yml          # 自定义配置（可选）
echo.
echo 服务端口:
echo   HTTP: 80 (标准HTTP端口，不可修改)
echo   WebSocket: ws://host/api/ws
echo.
echo ⚠️  重要提示:
echo   - 本服务强制使用 80 端口，无法通过配置修改
echo   - 建议单独使用一台服务器部署 IM 服务
echo   - 避免与其他 Web 服务（Nginx/Apache）端口冲突
echo   - 生产环境建议使用域名 + HTTPS 反向代理
echo.
echo ================================================================================
goto :eof

:: 主逻辑
if "%1"=="" goto help
if "%1"=="start" goto start
if "%1"=="stop" goto stop
if "%1"=="restart" goto restart
if "%1"=="status" goto status
if "%1"=="logs" goto logs
goto help

endlocal
