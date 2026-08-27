#!/bin/bash
# ============================================
# 第4步：LLM 连通性测试（需要内网千问，无需数据库）
# 前置：修改 application-llmtest.yml 的 api-url/api-key/model
#       （该文件在 tests-all.jar 内，解压修改后重新打包：
#         jar -uf *-tests-all.jar application-llmtest.yml）
# 或直接传参数:
#   ./step4-llm.sh http://千问地址/v1/chat/completions sk-xxx qwen-plus
# ============================================
source "$(dirname "$0")/common.sh"

API_URL=${1:-}
API_KEY=${2:-}
MODEL=${3:-}

echo "========================================"
echo "第4步：LLM 连通性测试"
if [ -n "$API_URL" ]; then
    echo "  使用命令行参数: $API_URL / $MODEL"
    EXTRA="-Dlms.llm.api-url=$API_URL -Dlms.llm.api-key=$API_KEY -Dlms.llm.model=$MODEL"
else
    echo "  使用 application-llmtest.yml 配置"
    EXTRA=""
fi
echo "========================================"

run_tests "-Dspring.profiles.active=llmtest $EXTRA" \
    --select-class com.bank.lms.unit.LlmConnectivityTest

echo "========================================"
echo "第4步完成"
echo "========================================"
