#!/bin/bash

# ====================== 配置项（只改这里）======================
# Jar 包名称
JAR_NAME="lms-backend-1.0.0.jar"
# =================================================================

# 颜色输出
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
RES="\033[0m"

echo -e "${YELLOW}==================== 生产环境 Jar 停止脚本 ====================${RES}"
echo -e "Jar 包：${GREEN}$JAR_NAME${RES}"
echo -e "================================================================${RES}"

# 查找进程
echo -e "${YELLOW}[1/2] 查找进程...${RES}"
PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo -e "${YELLOW}未找到运行中的进程${RES}"
    exit 0
fi

echo -e "找到进程：${GREEN}$PID${RES}"

# 优雅停止
echo -e "${YELLOW}[2/2] 正在停止进程...${RES}"
kill -15 $PID

# 等待最多30秒让进程优雅关闭
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo -e "${GREEN}进程已优雅停止${RES}"
        echo -e "================================================================"
        exit 0
    fi

    if [ $i -eq 30 ]; then
        echo -e "${YELLOW}进程未响应，强制停止...${RES}"
        kill -9 $PID
        sleep 1
        if ! ps -p $PID > /dev/null 2>&1; then
            echo -e "${GREEN}进程已强制停止${RES}"
            echo -e "================================================================"
            exit 0
        else
            echo -e "${RED}无法停止进程${RES}"
            exit 1
        fi
    fi

    echo -e "等待进程停止... ($i/30)"
    sleep 1
done
