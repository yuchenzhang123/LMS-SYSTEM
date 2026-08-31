#!/bin/bash
# ============================================
# 本地集成测试一键跑（Linux/Deepin 开发机）
# 用途：把主数据源 + GBase 兜底数据源覆盖到本地 MariaDB/MySQL，
#       ddl-auto 自动建表（含 knowledge_base），随后运行集成测试。
# 用法：
#   bash scripts/local-integration.sh                     # 跑 3 个 @SpringBootTest 集成类
#   bash scripts/local-integration.sh AiCopilotIntegrationTest   # 指定测试类
#   bash scripts/local-integration.sh all                 # 跑全部测试（单测+MockMvc+集成）
# 说明：
#   - 前提：本机 MariaDB/MySQL 已建好 lms_db，root/12345 可 TCP 登录（见 memory/local-db-environment）
#   - 集成测试 @ActiveProfiles("test") 加载 GaussDB 配置，必须用命令行 -D 覆盖：
#     ① hbm2ddl.auto 走 spring.jpa.properties.* 通道（项目自定义 EMF，spring.jpa.hibernate.ddl-auto 不生效）
#     ② GBase 兜底查询（OrgGroupService.lookupUser 等）会连远程 GaussDB，本地不可达会卡死，须一并覆盖
# ============================================
cd "$(dirname "$0")/.."

MYSQL_URL='jdbc:mysql://localhost:3306/lms_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'

# 默认只跑需要数据库的 3 个集成测试类；传 all 则跑全部测试
if [ "$1" = "all" ]; then
    TEST_CLASS=""
    TEST_FLAG=""
else
    TEST_CLASS="${1:-AiCopilotIntegrationTest,CopilotControllerTest,ScopeGroupIntegrationTest}"
    TEST_FLAG="-Dtest=${TEST_CLASS}"
fi

echo "========================================"
echo "本地测试：DB=localhost:3306/lms_db  ddl-auto=update"
[ -n "$TEST_CLASS" ] && echo "测试类: $TEST_CLASS" || echo "测试范围: 全部测试类"
echo "========================================"

mvn test $TEST_FLAG \
  -Dspring.datasource.main.url="$MYSQL_URL" \
  -Dspring.datasource.main.username=root \
  -Dspring.datasource.main.password=12345 \
  -Dspring.datasource.main.driver-class-name=com.mysql.cj.jdbc.Driver \
  -Dspring.datasource.gbase.url="$MYSQL_URL" \
  -Dspring.datasource.gbase.username=root \
  -Dspring.datasource.gbase.password=12345 \
  -Dspring.datasource.gbase.driver-class-name=com.mysql.cj.jdbc.Driver \
  -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect \
  -Dspring.jpa.properties.hibernate.hbm2ddl.auto=update \
  -Dspring.jpa.properties.hibernate.default_schema= \
  -Dlms.llm.enabled=false \
  -Dlms.embedding.enabled=false
