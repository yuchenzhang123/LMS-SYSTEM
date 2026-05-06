#!/bin/bash

# ====================== 配置项（只改这里）======================
# Jar 包名称
JAR_NAME="lms-backend-1.0.0.jar"
# 生产环境 JVM 参数
JVM_OPTS="-Xms512m -Xmx1024m -Dspring.profiles.active=prod"
# 外部配置文件路径（可选，如果config目录在jar包同级目录）
CONFIG_LOCATION="--spring.config.additional-location=file:./config/"
# =================================================================

# 颜色输出
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
RES="\033[0m"

echo -e "${YELLOW}==================== 生产环境 Jar 启动脚本 ====================${RES}"
echo -e "Jar 包：${GREEN}$JAR_NAME${RES}"
echo -e "启动环境：${GREEN}prod${RES}"
echo -e "JVM 参数：${GREEN}$JVM_OPTS${RES}"
echo -e "================================================================${RES}"

# 检查 jar 是否存在
if [ ! -f "$JAR_NAME" ]; then
    echo -e "${RED}错误：当前目录未找到 $JAR_NAME${RES}"
    exit 1
fi

# 检查是否已启动，有就杀掉旧进程
echo -e "${YELLOW}[1/4] 检查旧进程...${RES}"
PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo -e "发现进程：$PID，正在停止..."
    kill -15 $PID
    # 等待最多30秒让进程优雅关闭
    for i in {1..30}; do
        if ! ps -p $PID > /dev/null 2>&1; then
            echo -e "${GREEN}旧进程已优雅停止${RES}"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${YELLOW}进程未响应，强制停止...${RES}"
            kill -9 $PID
            sleep 1
        fi
        sleep 1
    done
else
    echo -e "未运行，无需停止"
fi

# 创建日志目录
echo -e "${YELLOW}[2/4] 创建日志目录...${RES}"
mkdir -p logs/archives
echo -e "${GREEN}日志目录已就绪${RES}"

# 启动 jar（输出重定向到 /dev/null，所有日志由logback管理）
echo -e "${YELLOW}[3/4] 正在启动 Jar...${RES}"
nohup java $JVM_OPTS -jar $JAR_NAME $CONFIG_LOCATION > /dev/null 2>&1 &

# 等待启动
echo -e "等待应用启动..."
sleep 3

# 检查启动状态
NEW_PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')

if [ -n "$NEW_PID" ]; then
    echo -e "${GREEN}[4/4] 启动成功！${RES}"
    echo -e "================================================================"
    echo -e "进程 PID：${GREEN}$NEW_PID${RES}"
    echo -e "应用日志：${GREEN}tail -f logs/lms-backend.log${RES}"
    echo -e "================================================================"
else
    echo -e "${RED}[4/4] 启动失败，请检查日志：logs/lms-backend.log${RES}"
    exit 1
fi
