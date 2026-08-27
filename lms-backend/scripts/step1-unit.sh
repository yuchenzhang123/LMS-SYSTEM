#!/bin/bash
# ============================================
# 第1步：纯单元测试（无需数据库、无需Spring容器）
# ============================================
source "$(dirname "$0")/common.sh"

echo "========================================"
echo "第1步：纯单元测试"
echo "  - 优先级评分（jqwik 属性测试 500+ 随机用例）"
echo "  - 分析能力枚举"
echo "  - AI上下文 ThreadLocal 隔离"
echo "========================================"

run_tests "" \
    --select-class com.bank.lms.unit.AccountPriorityScorerTest \
    --select-class com.bank.lms.unit.AnalysisCapabilityTest \
    --select-class com.bank.lms.unit.AiQueryContextTest

echo "========================================"
echo "第1步完成"
echo "========================================"
