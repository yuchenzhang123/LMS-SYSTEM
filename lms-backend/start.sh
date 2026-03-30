#!/bin/bash

# LMS后端服务启动脚本

APP_NAME="lms-backend"
JAR_FILE="lms-backend-1.0.0.jar"
LOG_DIR="logs"
LOG_FILE="$LOG_DIR/lms-backend.log"
PID_FILE="lms-backend.pid"
UPLOAD_DIR="upload"
CONFIG_DIR="config"

# JVM参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Spring配置 - 会自动加载 ./config/application-test.yml
SPRING_OPTS="--spring.profiles.active=test"

# 获取当前目录
APP_HOME=$(cd "$(dirname "$0")"; pwd)
cd $APP_HOME

# 初始化目录
init_dirs() {
    # 创建日志目录
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        chmod 755 "$LOG_DIR"
        echo "创建日志目录: $APP_HOME/$LOG_DIR"
    fi
    
    # 创建上传目录
    if [ ! -d "$UPLOAD_DIR" ]; then
        mkdir -p "$UPLOAD_DIR"
        chmod 755 "$UPLOAD_DIR"
        echo "创建上传目录: $APP_HOME/$UPLOAD_DIR"
    fi
    
    # 创建配置目录
    if [ ! -d "$CONFIG_DIR" ]; then
        mkdir -p "$CONFIG_DIR"
        chmod 755 "$CONFIG_DIR"
        echo "创建配置目录: $APP_HOME/$CONFIG_DIR"
        echo "请将 application-test.yml 放入 $CONFIG_DIR 目录"
    fi
}

# 检查是否已运行
check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null 2>&1; then
            return 0  # 已运行
        else
            rm -f $PID_FILE
        fi
    fi
    return 1  # 未运行
}

# 主启动逻辑
start() {
    # 检查是否已运行
    if check_running; then
        echo "$APP_NAME 已经在运行中 (PID: $(cat $PID_FILE))"
        exit 1
    fi
    
    # 检查配置文件
    if [ ! -f "$CONFIG_DIR/application-test.yml" ]; then
        echo "警告: 未找到 $CONFIG_DIR/application-test.yml"
        echo "将使用jar包内的默认配置"
    fi
    
    # 检查jar文件
    if [ ! -f "$JAR_FILE" ]; then
        echo "错误: 未找到 $JAR_FILE"
        exit 1
    fi
    
    # 初始化目录
    init_dirs
    
    echo "=========================================="
    echo "启动 $APP_NAME"
    echo "=========================================="
    echo "应用目录: $APP_HOME"
    echo "日志文件: $APP_HOME/$LOG_FILE"
    echo "上传目录: $APP_HOME/$UPLOAD_DIR"
    echo "配置目录: $APP_HOME/$CONFIG_DIR"
    echo "=========================================="
    
    # 启动服务
    nohup java $JVM_OPTS -jar $JAR_FILE $SPRING_OPTS > $LOG_FILE 2>&1 &
    echo $! > $PID_FILE
    sleep 3
    
    # 验证启动结果
    if check_running; then
        echo "$APP_NAME 启动成功 (PID: $(cat $PID_FILE))"
        echo ""
        echo "查看日志: tail -f $LOG_FILE"
        echo "停止服务: ./stop.sh"
    else
        echo "$APP_NAME 启动失败，请检查日志:"
        tail -50 $LOG_FILE
        exit 1
    fi
}

# 执行启动
start
