#!/bin/bash
# ============================================
# 离线内网测试主入口 — 一步步执行
#
# 用法:
#   ./run-tests.sh        # 交互式：每步之间暂停确认
#   ./run-tests.sh 1      # 只跑第1步（1=单元 2=Controller 3=集成 4=LLM）
#   ./run-tests.sh all    # 顺序跑完1-3（跳过LLM）
# ============================================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

MODE=${1:-}

run_step() {
    echo ""
    echo "============================================================"
    echo "  步骤 $1"
    echo "============================================================"
    bash "$SCRIPT_DIR/$2"
    local rc=$?
    if [ $rc -ne 0 ]; then
        echo ""
        echo "✗ 步骤 $1 失败（退出码 $rc），停止后续步骤"
        exit $rc
    fi
    echo ""
}

case "$MODE" in
    1) run_step 1 step1-unit.sh ;;
    2) run_step 2 step2-controller.sh ;;
    3) run_step 3 step3-integration.sh ;;
    4) run_step 4 step4-llm.sh ;;
    all)
        run_step 1 step1-unit.sh
        run_step 2 step2-controller.sh
        run_step 3 step3-integration.sh
        ;;
    *)
        echo "========================================"
        echo " 离线测试向导（一步步执行）"
        echo "========================================"
        echo " 第1步: 纯单元测试（无需数据库）"
        echo " 第2步: Controller MockMvc（无需数据库）"
        echo " 第3步: 集成测试（需要测试库 GaussDB/GBase）"
        echo " 第4步: LLM连通性（需要内网千问）"
        echo "========================================"
        echo " 直接运行: ./run-tests.sh 1 | 2 | 3 | 4 | all"
        echo ""
        echo " 按回车开始第1步..."
        read -r _
        run_step 1 step1-unit.sh
        echo " 第1步通过。按回车继续第2步..."
        read -r _
        run_step 2 step2-controller.sh
        echo " 第2步通过。第3步需要测试库，按回车继续（无测试库则 Ctrl+C 退出）..."
        read -r _
        run_step 3 step3-integration.sh
        echo " 第3步通过。第4步LLM可选，按回车执行或 Ctrl+C 跳过..."
        read -r _
        run_step 4 step4-llm.sh
        ;;
esac

echo ""
echo "========================================"
echo " 测试流程结束"
echo "========================================"
