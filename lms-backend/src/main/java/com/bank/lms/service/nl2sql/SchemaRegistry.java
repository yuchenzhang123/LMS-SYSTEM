package com.bank.lms.service.nl2sql;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * schema 召回字典（RAG 的「知识库」）。
 *
 * 实体 JPA 无中文字段释义注解，中文含义在此手工维护，是 LLM 理解表结构的唯一来源。
 * 这里集中维护「表 → 列 → 中文/类型/权限标记/软删/JOIN 关系」，SqlSafetyGuard 与 prompt 拼装都依赖它。
 *
 * 后续加表只需改 {@link #registerCoreTables()}，无需改动校验/编排逻辑。
 */
@Component
public class SchemaRegistry {

    /** 表名 → 表元数据，LinkedHashMap 保序 */
    private final Map<String, TableMeta> tables = new LinkedHashMap<>();

    public SchemaRegistry() {
        registerCoreTables();
    }

    public TableMeta getTable(String name) {
        return tables.get(name);
    }

    public Map<String, TableMeta> allTables() {
        return tables;
    }

    public boolean contains(String name) {
        return tables.containsKey(name);
    }

    // ==================== 核心表注册 ====================

    private void registerCoreTables() {
        registerLoanAccount();
        registerCollectionRecord();
        registerLitigation();
        registerNotice();
        registerUserOrg();
        registerUserLoginLog();
        registerJurisdictionOrg();
        registerOrgGroupMember();
        registerBranchOrg();
    }

    private void registerLoanAccount() {
        TableMeta t = register("loan_account", "贷款账户表", SecurityType.DIRECT_BRANCH, true);
        col(t, "loan_account", "贷款账号", "VARCHAR");
        col(t, "customer_id", "客户ID", "VARCHAR");
        col(t, "customer_name", "客户名称", "VARCHAR");
        col(t, "phone", "联系电话", "VARCHAR", false, false, true); // 敏感列
        col(t, "product_code", "产品编码", "VARCHAR");
        col(t, "product_name", "产品名称", "VARCHAR");
        col(t, "loan_date", "放款日期", "DATE");
        col(t, "loan_term", "贷款期限(期数)", "INT");
        col(t, "overdue_days", "逾期天数", "INT");
        col(t, "contract_amount", "合同金额", "DECIMAL");
        col(t, "loan_balance", "贷款余额", "DECIMAL");
        col(t, "unexpired_principal", "未到期本金", "DECIMAL");
        col(t, "overdue_principal", "逾期本金", "DECIMAL");
        col(t, "overdue_interest", "逾期利息", "DECIMAL");
        col(t, "overdue_penalty", "逾期罚息", "DECIMAL");
        col(t, "total_overdue_amount", "逾期总额", "DECIMAL");
        col(t, "status", "状态(uncollected未催收/collecting催收中/completed已完成)", "VARCHAR");
        col(t, "status_update_time", "状态更新时间", "DATETIME");
        col(t, "branch_code", "分支行号", "VARCHAR", true, false, false);
        col(t, "branch_name", "分支行名称", "VARCHAR");
    }

    private void registerCollectionRecord() {
        TableMeta t = register("collection_record", "催收记录表", SecurityType.VIA_JOIN, true);
        col(t, "record_id", "催收记录ID", "VARCHAR");
        col(t, "loan_account", "贷款账号(关联loan_account.loan_account)", "VARCHAR");
        col(t, "customer_id", "客户ID", "VARCHAR");
        col(t, "customer_name", "客户名称", "VARCHAR");
        col(t, "target_type", "催收对象类型", "VARCHAR");
        col(t, "target_name", "催收对象名称", "VARCHAR");
        col(t, "actual_collection_time", "实际催收时间", "DATETIME");
        col(t, "method", "催收方式", "VARCHAR");
        col(t, "method_text", "催收方式说明", "VARCHAR");
        col(t, "result", "催收结果", "VARCHAR");
        col(t, "operator_id", "催收员ID", "VARCHAR");
        col(t, "operator_name", "催收员姓名", "VARCHAR");
        col(t, "operate_time", "操作时间", "DATETIME");
        col(t, "remark", "备注", "VARCHAR");
        col(t, "material_type", "材料类型", "VARCHAR");
        col(t, "material_name", "材料名称", "VARCHAR");
        col(t, "material_url", "材料URL", "VARCHAR");
        setJoin(t, "collection_record.loan_account");
    }

    private void registerLitigation() {
        TableMeta t = register("litigation", "诉讼表", SecurityType.VIA_JOIN, true);
        col(t, "litigation_id", "诉讼ID", "VARCHAR");
        col(t, "loan_account", "贷款账号(关联loan_account.loan_account)", "VARCHAR");
        col(t, "customer_id", "客户ID", "VARCHAR");
        col(t, "customer_name", "客户名称", "VARCHAR");
        col(t, "status_code", "诉讼状态", "VARCHAR");
        col(t, "status_text", "诉讼状态说明", "VARCHAR");
        col(t, "in_litigation", "是否在诉讼中", "INT");
        col(t, "court_name", "涉及法院", "VARCHAR");
        col(t, "law_firm", "律所名称", "VARCHAR");
        col(t, "filing_date", "立案日期", "DATE");
        col(t, "filing_case_no", "立案案号", "VARCHAR");
        col(t, "hearing_date", "开庭日期", "DATE");
        col(t, "judgment_date", "判决日期", "DATE");
        col(t, "litigation_fee", "诉讼费", "DECIMAL");
        col(t, "preservation_fee", "保全费", "DECIMAL");
        col(t, "lawyer_fee", "律师费", "DECIMAL");
        col(t, "remark", "备注", "VARCHAR");
        setJoin(t, "litigation.loan_account");
    }

    private void registerNotice() {
        TableMeta t = register("notice", "通知表", SecurityType.DIRECT_BRANCH, true);
        col(t, "notice_id", "通知ID", "VARCHAR");
        col(t, "title", "标题", "VARCHAR");
        col(t, "level", "级别", "VARCHAR");
        col(t, "message", "内容", "TEXT");
        col(t, "customer_id", "客户ID", "VARCHAR");
        col(t, "loan_account", "贷款账号", "VARCHAR");
        col(t, "customer_name", "客户名称", "VARCHAR");
        col(t, "product_code", "产品编码", "VARCHAR");
        col(t, "notice_type", "通知类型", "VARCHAR");
        col(t, "overdue_days", "逾期天数", "INT");
        col(t, "is_read", "是否已读", "INT");
        col(t, "branch_code", "分支行号", "VARCHAR", true, false, false);
    }

    private void registerUserOrg() {
        TableMeta t = register("user_org", "员工机构表", SecurityType.DIRECT_ORG, false);
        col(t, "id", "主键", "BIGINT");
        col(t, "ehr_no", "员工号", "VARCHAR");
        col(t, "user_name", "员工姓名", "VARCHAR");
        col(t, "org_code", "机构号", "VARCHAR", false, true, false);
        col(t, "org_name", "机构名称", "VARCHAR");
        col(t, "status", "状态(active在职)", "VARCHAR");
    }

    private void registerUserLoginLog() {
        TableMeta t = register("user_login_log", "员工登录日志表", SecurityType.DIRECT_ORG, false);
        col(t, "id", "主键", "BIGINT");
        col(t, "ehr_no", "员工号", "VARCHAR");
        col(t, "user_name", "员工姓名", "VARCHAR");
        col(t, "org_code", "机构号", "VARCHAR", false, true, false);
        col(t, "login_time", "登录时间", "DATETIME");
        col(t, "ip_address", "登录IP", "VARCHAR");
        col(t, "session_id", "会话ID", "VARCHAR");
    }

    private void registerJurisdictionOrg() {
        TableMeta t = register("jurisdiction_org", "管辖机构表", SecurityType.DIRECT_ORG, false);
        col(t, "id", "主键", "BIGINT");
        col(t, "org_code", "机构号", "VARCHAR", false, true, false);
        col(t, "org_name", "机构名称", "VARCHAR");
    }

    private void registerOrgGroupMember() {
        TableMeta t = register("org_group_member", "范围组成员表", SecurityType.DIRECT_ORG, false);
        col(t, "id", "主键", "BIGINT");
        col(t, "group_code", "范围组编码", "VARCHAR");
        col(t, "org_code", "机构号", "VARCHAR", false, true, false);
        col(t, "org_name", "机构名称", "VARCHAR");
        col(t, "is_manager_org", "是否管辖机构", "INT");
    }

    private void registerBranchOrg() {
        TableMeta t = register("branch_org", "分支行-管辖行映射表", SecurityType.DIRECT_BOTH, false);
        col(t, "id", "主键", "BIGINT");
        col(t, "branch_code", "分支行号", "VARCHAR", true, false, false);
        col(t, "branch_name", "分支行名称", "VARCHAR");
        col(t, "org_code", "管辖行机构号", "VARCHAR", false, true, false);
    }

    // ==================== 注册辅助方法 ====================

    private TableMeta register(String name, String comment, SecurityType security, boolean softDeleted) {
        TableMeta t = new TableMeta();
        t.setName(name);
        t.setComment(comment);
        t.setSecurity(security);
        t.setSoftDeleted(softDeleted);
        t.setColumns(new LinkedHashMap<String, ColumnMeta>());
        tables.put(name, t);
        return t;
    }

    private void col(TableMeta t, String name, String comment, String type) {
        col(t, name, comment, type, false, false, false);
    }

    private void col(TableMeta t, String name, String comment, String type,
                     boolean branchCode, boolean orgCode, boolean pii) {
        ColumnMeta c = new ColumnMeta();
        c.setName(name);
        c.setComment(comment);
        c.setJdbcType(type);
        c.setBranchCode(branchCode);
        c.setOrgCode(orgCode);
        c.setPii(pii);
        t.getColumns().put(name, c);
    }

    private void setJoin(TableMeta t, String thisColumn) {
        JoinMeta jm = new JoinMeta();
        jm.setTableName(t.getName());
        jm.setThisColumn(thisColumn);
        jm.setTargetTable("loan_account");
        jm.setTargetColumn("loan_account.loan_account");
        t.setJoinToLoanAccount(jm);
    }

    // ==================== prompt 拼装 ====================

    /**
     * 把 schema 字典拼成给 LLM 的表结构描述（不含敏感列）。
     */
    public String buildPrompt() {
        StringBuilder sb = new StringBuilder();
        for (TableMeta t : tables.values()) {
            sb.append(t.getName()).append("(").append(t.getComment()).append("): ");
            boolean first = true;
            for (ColumnMeta c : t.getColumns().values()) {
                if (c.isPii()) {
                    continue; // 敏感列不暴露给 LLM，从源头降低越权/隐私风险
                }
                if (!first) {
                    sb.append(", ");
                }
                sb.append(c.getName()).append("(").append(c.getComment())
                  .append(":").append(c.getJdbcType()).append(")");
                first = false;
            }
            sb.append("\n");
        }
        // 关联规则（VIA_JOIN 表必须 JOIN loan_account 才能做机构口径过滤）
        sb.append("\n关联规则：\n");
        for (TableMeta t : tables.values()) {
            if (t.getSecurity() == SecurityType.VIA_JOIN && t.getJoinToLoanAccount() != null) {
                JoinMeta jm = t.getJoinToLoanAccount();
                sb.append("- ").append(jm.getTableName()).append(" 的 ").append(jm.getThisColumn())
                  .append(" 关联 ").append(jm.getTargetTable()).append(" 的 ").append(jm.getTargetColumn())
                  .append("（查询该表若需机构口径必须 JOIN loan_account）\n");
            }
        }
        return sb.toString();
    }
}
