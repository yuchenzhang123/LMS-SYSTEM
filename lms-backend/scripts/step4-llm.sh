#!/bin/bash
# ============================================
# 第4步：LLM 连通性测试（需要内网千问，无需数据库）
# 配置来源（优先级从高到低）：
#   1. 命令行参数:   ./step4-llm.sh http://地址/v1/chat/completions sk-xxx model
#   2. 外置配置:     scripts/config/application-test.yml（lms.llm.* 段，覆盖内置）
#   3. 内置配置:     主JAR内 application-test.yml（占位符则自动跳过）
# 说明：直接复用 application-test.yml 的 lms.llm 配置，不再单独维护 llmtest 模板
# ============================================
source "$(dirname "$0")/common.sh"

API_URL=${1:-}
API_KEY=${2:-}
MODEL=${3:-}

echo "========================================"
echo "第4步：LLM 连通性测试"
if [ -n "$API_URL" ]; then
    echo "  使用命令行参数: $API_URL / ${MODEL:-默认}"
    EXTRA="-Dlms.llm.api-url=$API_URL -Dlms.llm.api-key=$API_KEY -Dlms.llm.model=$MODEL"
else
    echo "  使用 application-test.yml 的 lms.llm 配置（外置 config/ 优先，未配置则跳过）"
    EXTRA=""
fi
echo "========================================"

run_tests "$EXTRA" \
    --select-class com.bank.lms.unit.LlmConnectivityTest

echo "========================================"
echo "第4步完成"
echo "========================================"
