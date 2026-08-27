# 范围组 + 业务洞察+AI问答+人员管理 — 上线变更材料

> **说明**：本次为累积大更新，包含两次提交的所有变更：范围组（83ee003）和 AI 业务洞察（a903476）。以下变更均基于 `init.sql`/`init-gaussdb.sql` 中 `CREATE TABLE IF NOT EXISTS` 语句编写。

---

## 一、数据库变更

### 1.1 新建表（6张）

**范围组相关（3张）：**

```sql
-- 范围组
CREATE TABLE IF NOT EXISTS org_group (
    id         BIGSERIAL PRIMARY KEY,
    group_code VARCHAR(50)  UNIQUE NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 范围组成员机构
CREATE TABLE IF NOT EXISTS org_group_member (
    id              BIGSERIAL PRIMARY KEY,
    group_code      VARCHAR(50)  NOT NULL,
    org_code        VARCHAR(20)  NOT NULL,
    org_name        VARCHAR(100),
    is_manager_org  SMALLINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_code, org_code)
);
CREATE INDEX IF NOT EXISTS idx_member_group_code ON org_group_member(group_code);
CREATE INDEX IF NOT EXISTS idx_member_org_code   ON org_group_member(org_code);

-- 范围组管理人员
CREATE TABLE IF NOT EXISTS org_group_manager (
    id         BIGSERIAL PRIMARY KEY,
    group_code VARCHAR(50)  NOT NULL,
    ehr_no     VARCHAR(50)  NOT NULL,
    user_name  VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_code, ehr_no)
);
CREATE INDEX IF NOT EXISTS idx_manager_group_code ON org_group_manager(group_code);
CREATE INDEX IF NOT EXISTS idx_manager_ehr_no      ON org_group_manager(ehr_no);
```

**AI 相关（3张）：**

```sql
-- 用户机构映射（每日从GBase同步）
CREATE TABLE IF NOT EXISTS user_org (
    id              BIGSERIAL PRIMARY KEY,
    ehr_no          VARCHAR(50)  NOT NULL UNIQUE,
    user_name       VARCHAR(100),
    org_code        VARCHAR(20)  NOT NULL,
    org_name        VARCHAR(500),
    status          VARCHAR(20)  DEFAULT 'active',
    gbase_sync_time TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_org_org_code ON user_org(org_code);
CREATE INDEX IF NOT EXISTS idx_user_org_status  ON user_org(status);

-- 员工登录记录
CREATE TABLE IF NOT EXISTS user_login_log (
    id         BIGSERIAL PRIMARY KEY,
    ehr_no     VARCHAR(50) NOT NULL,
    user_name  VARCHAR(100),
    org_code   VARCHAR(20),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    session_id VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_login_ehr_no ON user_login_log(ehr_no);
CREATE INDEX IF NOT EXISTS idx_login_time   ON user_login_log(login_time);
CREATE INDEX IF NOT EXISTS idx_login_org    ON user_login_log(org_code);

-- AI查询审计日志
CREATE TABLE IF NOT EXISTS ai_query_audit_log (
    id               BIGSERIAL PRIMARY KEY,
    ehr_no           VARCHAR(50) NOT NULL,
    org_code         VARCHAR(20) NOT NULL,
    question         TEXT,
    capability       VARCHAR(50),
    params           JSON,
    row_count        INT,
    execution_time_ms INT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 1.2 已有表新增索引（1个）

```sql
-- collection_record 补充复合索引（支撑员工工作量统计）
-- ⚠️ 生产表数据量较大，建议在低峰期执行
CREATE INDEX IF NOT EXISTS idx_record_operator_time 
    ON collection_record(operator_id, operate_time);
```

> MySQL 环境：去掉 `IF NOT EXISTS`，直接 `CREATE INDEX`

### 1.3 首次数据初始化

部署后端后执行：

```bash
# 1. 用户数据从GBase同步到本地user_org表：由定时任务 UserOrgSyncScheduler 自动执行（每日 6:15）
#    （无手动触发接口；如需立即同步，可临时调整 lms.user-org-sync.cron 或重启等待首次调度）

# 2. 验证同步结果
# SELECT COUNT(*) FROM user_org WHERE status = 'active';
```

### 1.4 数据迁移：旧机构数据导入范围组（如生产有 jurisdiction_org / branch_org 数据）

```sql
-- 每个管辖机构自动成为一个范围组
INSERT INTO org_group (group_code, group_name)
SELECT org_code, org_name FROM jurisdiction_org
WHERE org_code NOT IN (SELECT group_code FROM org_group);

-- 管辖机构自身作为管辖成员加入
INSERT INTO org_group_member (group_code, org_code, org_name, is_manager_org)
SELECT org_code, org_code, org_name, 1 FROM jurisdiction_org
WHERE (org_code, org_code) NOT IN (SELECT group_code, org_code FROM org_group_member);

-- 分支行归入对应管辖机构所在范围组
INSERT INTO org_group_member (group_code, org_code, org_name, is_manager_org)
SELECT bo.org_code, bo.branch_code, bo.branch_name, 0
FROM branch_org bo
WHERE (bo.org_code, bo.branch_code) NOT IN (SELECT group_code, org_code FROM org_group_member);
```

---

## 二、后端变更

### 2.1 新增文件（34个）

| 模块 | 文件 | 说明 |
|------|------|------|
| 范围组 | `entity/OrgGroup.java` | 范围组实体 |
| | `entity/OrgGroupMember.java` | 组成员实体 |
| | `entity/OrgGroupManager.java` | 管理人员实体 |
| | `repository/OrgGroupRepository.java` | 范围组CRUD |
| | `repository/OrgGroupMemberRepository.java` | 组成员CRUD |
| | `repository/OrgGroupManagerRepository.java` | 管理人员CRUD |
| | `service/OrgGroupService.java` | 范围组核心逻辑+角色判断+机构展开 |
| | `controller/OrgGroupController.java` | 范围组API |
| | `dto/org/GroupRoleResponse.java` | 角色响应DTO |
| 用户 | `entity/UserOrg.java` | 用户机构映射实体 |
| | `entity/UserLoginLog.java` | 登录记录实体 |
| | `repository/UserOrgRepository.java` | 用户查询 |
| | `repository/UserLoginLogRepository.java` | 登录统计 |
| | `service/UserOrgSyncService.java` | GBase用户同步 |
| | `scheduler/UserOrgSyncScheduler.java` | 同步定时任务 |
| AI | `entity/AiQueryAuditLog.java` | AI审计实体 |
| | `repository/AiQueryAuditLogRepository.java` | 审计日志写入 |
| | `service/analysis/AccountPriorityScorer.java` | 智能优先级评分 |
| | `service/analysis/CopilotService.java` | AI问答+简报+摘要 |
| | `service/llm/LlmClient.java` | LLM接口 |
| | `service/llm/NoopLlmClient.java` | 空客户端(降级) |
| | `service/llm/OpenAiLlmClient.java` | OpenAI兼容客户端 |
| | `config/AiQueryContext.java` | ThreadLocal上下文 |
| | `config/AiQueryInterceptor.java` | 数据权限拦截器 |
| | `config/LlmConfig.java` | LLM配置 |
| | `controller/UserController.java` | 登录上报+员工统计 |
| | `controller/CopilotController.java` | AI接口 |

### 2.2 改造文件（9个）

| 文件 | 变更 |
|------|------|
| `config/WebConfig.java` | 注册AiQueryInterceptor |
| `dto/request/AccountQueryRequest.java` | 新增 sortBy 字段 |
| `repository/LoanAccountRepository.java` | 新增分析查询 |
| `repository/CollectionRecordRepository.java` | 新增聚合查询 |
| `service/LoanAccountService.java` | sortBy + expandOrgCodes |
| `service/OrgHierarchyService.java` | 委托OrgGroupService做角色判断和范围展开 |
| `controller/OrgHierarchyController.java` | /org/role 返回 GroupRoleResponse |
| `db/init.sql` | 建表+索引 |
| `db/init-gaussdb.sql` | 建表+索引+触发器 |

### 2.3 新增接口（21个）

**范围组管理：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/org/group` | 范围组CRUD |
| GET | `/org/group/tree` | 范围组树（含成员+管理人员） |
| POST/DELETE | `/org/group/{code}/member` | 组成员管理 |
| PUT/DELETE | `/org/group/{code}/member/{orgCode}/manager` | 管辖机构标记 |
| POST/DELETE | `/org/group/{code}/manager` | 管理人员管理 |
| GET | `/org/user-lookup` | EHR查姓名 |
| GET | `/org/gbase-lookup` | 机构号查名称 |

**用户与AI：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/login-log` | 登录上报 |
| GET | `/user/stats` | 员工统计 |
| GET | `/user/list` | 员工列表 |
| POST | `/ai/chat` | AI问答 |
| POST | `/ai/briefing` | 每日简报 |
| POST | `/ai/summary` | 催收历程摘要 |

---

## 三、前端变更

### 3.1 新增文件（5个）

| 文件 | 说明 |
|------|------|
| `src/api/ai.js` | AI接口 |
| `src/api/user.js` | 用户接口 |
| `src/store/ai.js` | AI状态管理 |
| `src/views/insight/index.vue` | 业务洞察页 |

### 3.2 改造文件（10个）

| 文件 | 变更 |
|------|------|
| `src/api/org.js` | 新增范围组所有API函数 |
| `src/api/collection.js` | 扩展stats/trend API |
| `src/store/permission.js` | 扩展groupCode/groupOrgCodes/isGroupManager |
| `src/store/index.js` | 注册ai模块 |
| `src/config/dev-menus.js` | 三套菜单新增"业务洞察" |
| `src/router/index.js` | 新增/insight路由 |
| `src/layout/index.vue` | 登录上报 |
| `src/views/admin/account-list.vue` | 智能排序+范围组下拉 |
| `src/views/collection/account-list.vue` | 智能排序 |
| `src/views/org/hierarchy.vue` | 重做为卡片式范围组管理 |

### 3.3 部署

```bash
cd LMS-SYSTEM-master
npm install    # 如有新依赖
npm run build
# dist/ 部署到 nginx
```

---

## 四、新增配置项

```yaml
# application.yml 的 lms 段下新增

lms:
  llm:
    enabled: false                              # AI总开关，默认关闭
    provider: openai                            # 模型提供商
    api-key: ${LLM_API_KEY:}                    # 从环境变量注入
    api-url: https://api.openai.com/v1/chat/completions
    model: gpt-4o-mini
    timeout: 30000
  user-org-sync:
    cron: 0 15 6 * * ?                          # 用户数据每日6:15同步
```

> AI 默认关闭，所有功能降级到规则模板正常工作，无需额外配置。

---

## 五、网络策略

| 目标 | 方向 | 端口 | 用途 | 是否必需 |
|------|:--:|------|------|:--:|
| LLM API | 后端→外网 | 443 | AI调用 | ❌ 可选，关闭LLM不需要 |
| GBase | 后端→GBase | 5258 | 已有连接 | - |
| 应用 | nginx→后端 | 8099 | 已有连接 | - |

国内 LLM 推荐使用通义千问，api-url 改为：
`https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`

---

## 六、上线步骤

```
1. 数据库变更（低峰期 2:00-4:00）
   ├── 执行建表DDL（6张新表，全部 CREATE IF NOT EXISTS，幂等安全）
   ├── collection_record 加索引（注意大表锁表时间）
   └── 执行数据迁移SQL（jurisdiction_org → org_group）

2. 后端部署
   ├── mvn clean package -DskipTests
   ├── 停旧服务 → 部署新JAR → 启动
   └── 用户同步由定时任务 UserOrgSyncScheduler 自动执行（每日 6:15）

3. 前端部署
   ├── npm run build → dist/ 部署到 nginx
   └── 验证：登录 → "业务洞察"菜单可见 → 首页改版

4. （可选）开启AI
   ├── 配置 LLM_API_KEY 环境变量
   ├── lms.llm.enabled=true → 重启
   └── 验证：业务洞察页AI问答正常
```

---

## 七、回滚方案

| 场景 | 操作 |
|------|------|
| 后端启动失败 | 回滚JAR到旧版，新表不删（CREATE IF NOT EXISTS 幂等，旧代码不访问新表） |
| 前端异常 | 回滚dist/到旧版 |
| 索引性能问题 | `DROP INDEX idx_record_operator_time` |
| LLM异常 | `lms.llm.enabled=false` 重启即可，无需回滚代码 |
| 范围组数据问题 | 删除 org_group/or_group_member/org_group_manager 三张新表数据重导 |
