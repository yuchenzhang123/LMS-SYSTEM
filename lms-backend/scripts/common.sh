#!/bin/bash
# 公共函数：定位JAR、执行测试
# 用法: source common.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 定位主JAR（排除 tests）
APP_JAR=$(ls rcrms-backend-*.jar 2>/dev/null | grep -v -- -tests.jar | head -1)
TESTS_JAR=$(ls *-tests.jar 2>/dev/null | head -1)
TEST_LIB_DIR="test-lib"

if [ -z "$APP_JAR" ]; then
    echo "✗ 找不到主JAR (rcrms-backend-*.jar)"
    exit 1
fi
if [ -z "$TESTS_JAR" ]; then
    echo "✗ 找不到测试JAR (*-tests.jar)"
    exit 1
fi
if [ ! -d "$TEST_LIB_DIR" ]; then
    echo "✗ 找不到测试依赖目录 ($TEST_LIB_DIR)"
    exit 1
fi

echo "主JAR:   $APP_JAR"
echo "测试JAR: $TESTS_JAR"
echo "依赖目录: $TEST_LIB_DIR"
echo "----------------------------------------"

# 运行测试（统一走 Spring Boot PropertiesLauncher）
# 类路径 = 主JAR(BOOT-INF/classes + BOOT-INF/lib) + 测试JAR + test-lib/*.jar
# 测试依赖保持独立 jar：SpringFactoriesLoader 才能正确合并 META-INF/spring.factories
# 参数1: 附加 -D 参数（可为空字符串）
# 参数2+: --select-class 列表
run_tests() {
    local extra_props="$1"
    shift
    java -cp "$APP_JAR" \
        -Dloader.path="$TESTS_JAR,$TEST_LIB_DIR/" \
        -Dloader.main=org.junit.platform.console.ConsoleLauncher \
        $extra_props \
        org.springframework.boot.loader.PropertiesLauncher \
        "$@"
}
