# 业务洞察+AI问答+人员管理 — 上线变更材料

## 一、数据库变更

### 1.1 新建表（3张）

```sql
-- ============================================
-- 1. 用户机构映射（每日从GBase同步）
-- ============================================
CREATE TABLE user_org (
    id              BIGSERIAL PRIMARY KEY,
    ehr_no          VARCHAR(50)  NOT NULL UNIQUE,   -- 员工号
    user_name       VARCHAR(100),                    -- 姓名
    org_code        VARCHAR(20)  NOT NULL,           -- 所属机构号
    org_name        VARCHAR(100),                    -- 机构名称
    status          VARCHAR(20)  DEFAULT 'active',   -- active/inactive
    gbase_sync_time TIMESTAMP NULL,                  -- 最后同步时间
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_org_org_code ON user_org(org_code);
CREATE INDEX idx_user_org_status  ON user_org(status);

-- ============================================
-- 2. 员工登录记录
-- ============================================
CREATE TABLE user_login_log (
    id          BIGSERIAL PRIMARY KEY,
    ehr_no      VARCHAR(50) NOT NULL,                -- 员工号
    user_name   VARCHAR(100),                        -- 姓名
    org_code    VARCHAR(20),                         -- 登录时机构号
    login_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 登录时间
    ip_address  VARCHAR(50),                         -- IP地址
    session_id  VARCHAR(100)                         -- 会话ID
);
CREATE INDEX idx_login_ehr_no ON user_login_log(ehr_no);
CREATE INDEX idx_login_time   ON user_login_log(login_time);
CREATE INDEX idx_login_org    ON user_login_log(org_code);

-- ============================================
-- 3. AI查询审计日志
-- ============================================
CREATE TABLE ai_query_audit_log (
    id               BIGSERIAL PRIMARY KEY,
    ehr_no           VARCHAR(50)  NOT NULL,          -- 员工号
    org_code         VARCHAR(20)  NOT NULL,          -- 机构号
    question         TEXT,                            -- 用户问题
    capability       VARCHAR(50),                     -- 触发的分析能力
    params           JSON,                            -- 分析参数
    row_count        INT,                             -- 返回行数
    execution_time_ms INT,                            -- 执行耗时(毫秒)
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 1.2 新增索引（1个，已有表）

```sql
-- collection_record 表补充复合索引（支撑员工工作量统计）
-- ⚠️ 生产表数据量较大，建议在低峰期（凌晨2:00-4:00）执行
CREATE INDEX CONCURRENTLY idx_record_operator_time 
    ON collection_record(operator_id, operate_time);
```

> **MySQL 环境**：去掉 `CONCURRENTLY`，直接 `CREATE INDEX`

### 1.3 首次数据初始化

后端部署后，手动触发用户数据同步（从 GBase 拉取全量用户到 user_org 表）：

```bash
curl -X POST http://{host}:8099/admin/scheduler/sync-user-org
```

---

## 二、后端变更

### 2.1 新增文件清单（20个）

| 目录 | 文件 | 说明 |
|------|------|------|
| `entity/` | `UserOrg.java` | 用户机构映射实体 |
| | `UserLoginLog.java` | 登录记录实体 |
| | `AiQueryAuditLog.java` | AI审计日志实体 |
| `repository/` | `UserOrgRepository.java` | 用户查询 |
| | `UserLoginLogRepository.java` | 登录统计聚合 |
| | `AiQueryAuditLogRepository.java` | 审计日志写入 |
| `service/` | `UserOrgSyncService.java` | GBase用户同步 |
| `service/analysis/` | `AnalysisCapability.java` | 13种分析能力枚举 |
| | `AccountPriorityScorer.java` | 智能优先级评分(纯算法) |
| | `SecureAnalysisExecutor.java` | 安全SQL执行器 |
| | `CopilotService.java` | AI问答+简报+摘要+降级 |
| `service/llm/` | `LlmClient.java` | LLM接口 |
| | `NoopLlmClient.java` | 空客户端(降级用) |
| | `OpenAiLlmClient.java` | OpenAI兼容客户端 |
| `controller/` | `UserController.java` | 登录上报+员工统计 |
| | `CopilotController.java` | AI问答/简报/摘要接口 |
| `config/` | `AiQueryContext.java` | ThreadLocal上下文 |
| | `AiQueryInterceptor.java` | 数据权限拦截器 |
| | `LlmConfig.java` | LLM客户端配置 |
| `scheduler/` | `UserOrgSyncScheduler.java` | 用户同步定时任务 |
| `dto/analysis/` | `AiUserScope.java` | 用户范围DTO |
| | `AnalysisResult.java` | 分析结果DTO |

### 2.2 修改文件清单（7个）

| 文件 | 变更内容 |
|------|----------|
| `config/WebConfig.java` | 注册AiQueryInterceptor拦截 /ai/** |
| `dto/request/AccountQueryRequest.java` | 新增 sortBy 字段 |
| `repository/LoanAccountRepository.java` | 新增5个分析查询(排名/账龄/深度逾期/日新增) |
| `repository/CollectionRecordRepository.java` | 新增3个聚合查询(工作量/按方法统计/全记录) |
| `service/LoanAccountService.java` | 支持 sortBy 参数(time/priority/amount) |
| `service/OrgGroupService.java` | lookupUser改为本地user_org优先→GBase降级 |
| `db/init.sql` + `db/init-gaussdb.sql` | 新增3张表+1个索引 |

### 2.3 新增接口（7个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/login-log` | 登录上报 |
| GET | `/user/stats?orgCode=` | 员工统计 |
| GET | `/user/list?orgCode=&page=&size=` | 员工列表 |
| POST | `/ai/chat` | AI问答 |
| POST | `/ai/briefing` | 每日简报 |
| POST | `/ai/summary` | 催收历程摘要 |
| POST | `/admin/scheduler/sync-user-org` | 手动触发用户同步 |

### 2.4 新增配置项

```yaml
lms:
  llm:
    enabled: false                    # 默认关闭
    provider: openai
    api-key: ${LLM_API_KEY:}          # 从环境变量注入
    api-url: https://api.openai.com/v1/chat/completions
    model: gpt-4o-mini
    timeout: 30000
  user-org-sync:
    cron: 0 15 6 * * ?               # 每日6:15
```

---

## 三、前端变更

### 3.1 新增文件（4个）

| 文件 | 说明 |
|------|------|
| `src/api/ai.js` | AI接口(chat/briefing/summary) |
| `src/api/user.js` | 用户接口(loginLog/stats/list) |
| `src/store/ai.js` | AI状态管理(问答消息/简报) |
| `src/views/insight/index.vue` | **业务洞察页**(简报+机构排名+人员列表+AI问答) |

### 3.2 修改文件（6个）

| 文件 | 变更内容 |
|------|----------|
| `src/config/dev-menus.js` | 三套角色菜单均新增"业务洞察" |
| `src/router/index.js` | 新增 /insight 路由 |
| `src/store/index.js` | 注册 ai store模块 |
| `src/layout/index.vue` | created时自动上报登录 |
| `src/views/admin/account-list.vue` | 新增智能排序切换(time/priority/amount) |
| `src/views/collection/account-list.vue` | 新增智能排序切换 |

### 3.3 部署方式

```bash
cd LMS-SYSTEM-master
npm run build
# 将 dist/ 目录部署到 nginx 静态目录
```

---

## 四、网络策略

### 4.1 是否需要开通

| 目标 | 方向 | 端口 | 说明 | 是否必需 |
|------|:--:|------|------|:--:|
| LLM API (如通义千问) | 后端 → 外网 | 443 | `lms.llm.api-url` 指向的地址 | ❌ 可选，LLM关闭时不需要 |
| GBase 数据库 | 后端 → GBase | 5258 | 已有连接，无需新增 | - |
| 应用端口 | nginx → 后端 | 8099 | 已有，无需新增 | - |

### 4.2 LLM API 地址参考

| 模型 | api-url |
|------|------|
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` |
| OpenAI | `https://api.openai.com/v1/chat/completions` |
| 企业内部大模型 | 按实际地址配置 |

---

## 五、上线步骤

```
1. 数据库变更（低峰期）
   ├── 执行建表DDL（3张新表）
   ├── collection_record 加索引（低峰期，注意锁表）
   └── 验证：SELECT * FROM user_org 等表正常

2. 后端部署
   ├── 打包：mvn clean package -DskipTests
   ├── 停旧服务 → 部署新JAR → 启动
   ├── 验证新接口：curl http://host:8099/user/stats?orgCode=...
   └── 手动触发用户同步：curl -X POST http://host:8099/admin/scheduler/sync-user-org

3. 前端部署
   ├── npm run build
   ├── 部署 dist/ 到 nginx
   └── 验证：登录 → 菜单可见"业务洞察" → 首页改版

4. （可选）开启AI
   ├── 配置 LLM_API_KEY 环境变量
   ├── 改配置 lms.llm.enabled=true
   ├── 重启后端
   └── 验证：业务洞察页AI问答正常
```

---

## 六、回滚方案

| 场景 | 操作 |
|------|------|
| 后端启动失败 | 回滚JAR到旧版本，新表不删（纯增量不影响旧逻辑） |
| 前端异常 | 回滚dist/到旧版本 |
| 索引导致性能问题 | `DROP INDEX idx_record_operator_time` |
| LLM异常 | `lms.llm.enabled=false` 重启，无需回滚代码 |
