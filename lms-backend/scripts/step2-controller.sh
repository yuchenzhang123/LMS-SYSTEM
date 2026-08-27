#!/bin/bash
# ============================================
# 第2步：Controller MockMvc 测试（无需数据库，需Spring Web层）
# ============================================
source "$(dirname "$0")/common.sh"

echo "========================================"
echo "第2步：Controller MockMvc 测试"
echo "  - CopilotController 接口（Mock Service）"
echo "  - KnowledgeBaseController 接口（Mock Service）"
echo "  - 安全过滤 + 全局异常覆盖"
echo "========================================"

run_tests "" \
    --select-class com.bank.lms.integration.controller.CopilotControllerMockTest \
    --select-class com.bank.lms.integration.controller.KnowledgeBaseControllerMockTest \
    --select-class com.bank.lms.integration.controller.SecurityFilterTest

echo "========================================"
echo "第2步完成"
echo "========================================"
