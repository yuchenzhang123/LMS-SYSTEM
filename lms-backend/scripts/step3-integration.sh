#!/bin/bash
# ============================================
# 第3步：集成测试（需要测试库 GaussDB + GBase）
#   - 范围组 CRUD/角色/展开（自动回滚）
#   - AI引擎分析能力（数据工厂自造数据）
#   - Controller 真实Service链路
# 前置：测试库已执行建表SQL（见 db/init-gaussdb.sql）
# ============================================
source "$(dirname "$0")/common.sh"

echo "========================================"
echo "第3步：集成测试（连测试库）"
echo "  数据库配置: application-test.yml（主JAR内）"
echo "========================================"

# 先检查数据库连通
echo "--- 检查测试库连通性 ---"
DB_HOST=$(grep -oP 'jdbc:gaussdb://\K[^:/]+' "$APP_JAR" 2>/dev/null || true)
echo "请确认测试虚拟机可访问 GaussDB/GBase（见 DEPLOYMENT.md 网络策略）"

run_tests "-Dspring.profiles.active=test" \
    --select-class com.bank.lms.integration.ScopeGroupIntegrationTest \
    --select-class com.bank.lms.integration.AiCopilotIntegrationTest \
    --select-class com.bank.lms.integration.controller.CopilotControllerTest

echo "========================================"
echo "第3步完成"
echo "========================================"
