#!/bin/bash
JAR_NAME="lms-backend-1.0.0.jar"
PID=$(ps -ef | grep $JAR_NAME | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    kill -9 $PID
    echo "已停止进程：$PID"
else
    echo "未运行"
fi