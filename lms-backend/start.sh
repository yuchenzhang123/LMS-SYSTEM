#!/bin/bash

# ====================== 配置项（只改这里）======================
# Jar 包名称（把你的 jar 名字替换这里）
JAR_NAME="lms-backend-1.0.0.jar"
# 测试环境 JVM 参数（测试环境推荐配置，可改）
JVM_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=test"
# 日志输出文件
LOG_FILE="nohup.out"
# =================================================================

# 颜色输出（方便看日志）
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
RES="\033[0m"

echo -e "${YELLOW}==================== 测试环境 Jar 启动脚本 ====================${RES}"
echo -e "Jar 包：${GREEN}$JAR_NAME${RES}"
echo -e "启动环境：${GREEN}test${RES}"
echo -e "================================================================${RES}"

# 检查 jar 是否存在
if [ ! -f "$JAR_NAME" ]; then
    echo -e "${RED}错误：当前目录未找到 $JAR_NAME${RES}"
    exit 1
fi

# 检查是否已启动，有就杀掉旧进程
echo -e "${YELLOW}[1/3] 检查旧进程...${RES}"
PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo -e "发现进程：$PID，正在停止..."
    kill -9 $PID
    sleep 2
    echo -e "${GREEN}旧进程已停止${RES}"
else
    echo -e "未运行，无需停止${RES}"
fi

# 启动 jar（测试环境标准命令）
echo -e "${YELLOW}[2/3] 正在启动 Jar...${RES}"
nohup java $JVM_OPTS -jar $JAR_NAME > $LOG_FILE 2>&1 &

# 等待 2 秒检查是否启动成功
sleep 2
NEW_PID=$(ps -ef | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')

if [ -n "$NEW_PID" ]; then
    echo -e "${GREEN}[3/3] 启动成功！进程 PID：$NEW_PID${RES}"
    echo -e "实时日志：tail -f $LOG_FILE"
else
    echo -e "${RED}[3/3] 启动失败，请检查日志：$LOG_FILE${RES}"
    exit 1
fi