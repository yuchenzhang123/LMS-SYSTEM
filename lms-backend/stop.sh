#!/bin/bash

# LMS后端服务停止脚本

APP_NAME="lms-backend"
PID_FILE="lms-backend.pid"

# 获取当前目录
APP_HOME=$(cd "$(dirname "$0")"; pwd)
cd $APP_HOME

# 停止服务
stop() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null 2>&1; then
            echo "正在停止 $APP_NAME (PID: $PID)..."
            
            # 发送SIGTERM信号，优雅停止
            kill $PID
            
            # 等待进程结束
            for i in {1..10}; do
                if ! ps -p $PID > /dev/null 2>&1; then
                    break
                fi
                sleep 1
            done
            
            # 如果进程还在，强制停止
            if ps -p $PID > /dev/null 2>&1; then
                echo "进程未响应，强制停止..."
                kill -9 $PID
                sleep 1
            fi
            
            rm -f $PID_FILE
            echo "$APP_NAME 已停止"
        else
            echo "$APP_NAME 未运行 (PID文件存在但进程已退出)"
            rm -f $PID_FILE
        fi
    else
        echo "$APP_NAME 未运行 (无PID文件)"
        
        # 尝试查找并停止可能的残留进程
        PIDS=$(ps aux | grep "lms-backend-1.0.0.jar" | grep -v grep | awk '{print $2}')
        if [ -n "$PIDS" ]; then
            echo "发现残留进程: $PIDS"
            echo "是否停止? (y/n)"
            read -t 5 answer
            if [ "$answer" = "y" ]; then
                kill $PIDS
                echo "残留进程已停止"
            fi
        fi
    fi
}

# 执行停止
stop
