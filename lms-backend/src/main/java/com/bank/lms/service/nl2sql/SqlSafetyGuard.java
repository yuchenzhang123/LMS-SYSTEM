package com.bank.lms.service.nl2sql;

import com.bank.lms.dto.analysis.AiUserScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL 安全守卫（NL2SQL 零越权命脉）。
 *
 * 原则：不是「校验 LLM 是否带了行级过滤」，而是「无条件强制注入」WHERE 过滤（聚合前注入），
 * 即使 LLM 没提机构也要注入。用 JSqlParser 4.9 做真实 AST 解析，而非手写字符串匹配。
 *
 * 注意：JSqlParser 4.9 相较旧版 API 有较大重构（{@code Select} 变为抽象类、无 {@code SelectBody}、
 * {@code Limit.getRowCount()} 返回 {@code Expression}、{@code Alias} 迁至 {@code expression} 包），
 * 本类已按 4.9 实际 API 编写。
 *
 * 多层防线：
 * 1. 字符串预处理：多语句(分号)、反引号/双引号标识符、子查询「(SELECT」→ 拒绝；
 * 2. 危险关键字正则兜底（INTO OUTFILE/LOAD_FILE/SLEEP/BENCHMARK/PG_SLEEP 等）→ 拒绝；
 * 3. AST：仅 SELECT、仅单层 PlainSelect（拒绝 UNION/Values/嵌套）、表级白名单（主表 + JOIN 表都在白名单内）；
 * 4. 行级过滤逐表强制注入（DIRECT_BRANCH/DIRECT_ORG/DIRECT_BOTH/VIA_JOIN）+ 软删 is_deleted=0，
 *    支持表别名，查询里出现的每一张表都被各自注入（多表 JOIN 零越权的关键）；
 * 5. 原 WHERE 括号包裹防 OR 优先级绕过 + LIMIT clamp 上限。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlSafetyGuard {

    private final SchemaRegistry schemaRegistry;

    /** 返回行数上限（无 LIMIT 时追加，超上限则 clamp） */
    @Value("${lms.nl2sql.max-rows:1000}")
    private long maxRows;

    /** 空权限集时是否拒绝（fail-closed，默认 true；false 则注入永假条件返回空结果） */
    @Value("${lms.nl2sql.fail-closed:true}")
    private boolean failClosed;

    /** 危险关键字/函数兜底（单词边界，大小写不敏感） */
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "\\b(into\\s+outfile|load_file|dumpfile|sleep|benchmark|pg_sleep|outfile)\\b",
        Pattern.CASE_INSENSITIVE);

    /** 子查询检测：「(SELECT ...」形式，含 WHERE IN/EXISTS/FROM/SELECT 列表里的所有嵌套子查询 */
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile(
        "\\(\\s*select\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 校验并重写 LLM 生成的 SQL。
     * @return 可执行的安全查询；违规则抛 {@link SqlGuardException}（message 供 LLM 修正）
     */
    public SafeQuery enforce(String rawSql, AiUserScope scope) {
        if (rawSql == null || rawSql.trim().isEmpty()) {
            throw new SqlGuardException("SQL 为空");
        }
        if (scope == null) {
            throw new SqlGuardException("缺少用户权限上下文");
        }

        String sql = rawSql.trim();

        // ---------- 1. 字符串预处理 ----------
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.contains(";")) {
            throw new SqlGuardException("禁止多语句");
        }
        if (sql.contains("`") || sql.contains("\"")) {
            throw new SqlGuardException("禁止反引号/双引号标识符");
        }
        if (DANGEROUS_PATTERN.matcher(sql).find()) {
            throw new SqlGuardException("禁止使用危险函数或 OUTFILE 子句");
        }
        if (SUBQUERY_PATTERN.matcher(sql).find()) {
            throw new SqlGuardException("禁止子查询");
        }

        // ---------- 2. AST 解析 ----------
        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new SqlGuardException("SQL 无法解析: " + e.getMessage());
        }
        if (!(stmt instanceof Select)) {
            throw new SqlGuardException("仅允许 SELECT 查询");
        }
        // 4.9 中 PlainSelect/SetOperationList/Values 均 extends Select，此处仅放行 PlainSelect
        Select select = (Select) stmt;
        if (!(select instanceof PlainSelect)) {
            throw new SqlGuardException("禁止 UNION/集合运算或嵌套子查询");
        }
        PlainSelect ps = (PlainSelect) select;

        // ---------- 3. 收集涉及的表（主表 + JOIN 表），表级白名单校验 ----------
        FromItem fromItem = ps.getFromItem();
        if (!(fromItem instanceof Table)) {
            throw new SqlGuardException("FROM 后必须是单个表");
        }
        Table mainTable = (Table) fromItem;
        String mainName = mainTable.getName();
        if (schemaRegistry.getTable(mainName) == null) {
            throw new SqlGuardException("未知表: " + mainName);
        }
        // 表名 → 引用名（别名或表名），保序，用于逐表注入时限定列
        Map<String, String> tableRefs = new LinkedHashMap<>();
        tableRefs.put(mainName, refName(mainTable));

        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                FromItem rightItem = join.getRightItem();
                if (!(rightItem instanceof Table)) {
                    throw new SqlGuardException("JOIN 后必须是表，不支持子查询/派生表");
                }
                Table jt = (Table) rightItem;
                String jtName = jt.getName();
                if (schemaRegistry.getTable(jtName) == null) {
                    throw new SqlGuardException("未知表: " + jtName);
                }
                tableRefs.put(jtName, refName(jt));
            }
        }

        // ---------- 4. 逐表构造行级过滤（每张表都强制注入，别名限定列） ----------
        List<Expression> filters = new ArrayList<>();
        // loan_account 是否已作为查询表出现（VIA_JOIN 表复用其 branch_code，避免重复 JOIN）
        boolean hasLoanAccount = tableRefs.containsKey("loan_account");
        int autoLaSeq = 0; // 自动注入 loan_account 时的别名序号，多张 VIA_JOIN 表时保证唯一

        for (Map.Entry<String, String> e : tableRefs.entrySet()) {
            String name = e.getKey();
            String ref = e.getValue();
            TableMeta meta = schemaRegistry.getTable(name);

            // 软删过滤（继承 BaseEntity 的表，JDBC 查询需手动排除已软删记录）
            if (meta.isSoftDeleted()) {
                filters.add(new EqualsTo(
                    new Column(new Table(ref), "is_deleted"), new LongValue(0)));
            }

            if (meta.getSecurity() == SecurityType.VIA_JOIN) {
                // VIA_JOIN 表无机构列，需借 loan_account 的 branch_code 做权限代理。
                // 若 loan_account 已在查询中，其 branch_code 已由自身 DIRECT_BRANCH 注入，
                // 通过 JOIN 传播到结果集，无需重复；否则自动注入一条 loan_account JOIN。
                if (!hasLoanAccount) {
                    String laAlias = "la" + (autoLaSeq == 0 ? "" : autoLaSeq);
                    autoLaSeq++;
                    injectLoanAccountJoin(ps, meta, ref, laAlias);
                    filters.add(buildInFilter(laAlias, "branch_code", "branchCodes",
                        scope.getAllowedBranchCodes()));
                }
            } else {
                if (meta.getSecurity() == SecurityType.DIRECT_BRANCH
                        || meta.getSecurity() == SecurityType.DIRECT_BOTH) {
                    filters.add(buildInFilter(ref, "branch_code", "branchCodes",
                        scope.getAllowedBranchCodes()));
                }
                if (meta.getSecurity() == SecurityType.DIRECT_ORG
                        || meta.getSecurity() == SecurityType.DIRECT_BOTH) {
                    filters.add(buildInFilter(ref, "org_code", "orgCodes",
                        scope.getAllowedOrgCodes()));
                }
            }
        }

        // ---------- 5. 组合 WHERE（括号包裹原条件，防 OR 优先级绕过） ----------
        Expression combined = combineAnd(filters);
        if (combined != null) {
            Expression original = ps.getWhere();
            if (original != null) {
                ps.setWhere(new AndExpression(new Parenthesis(original), new Parenthesis(combined)));
            } else {
                ps.setWhere(combined);
            }
        }

        // ---------- 6. LIMIT clamp ----------
        Limit limit = ps.getLimit();
        if (limit == null) {
            limit = new Limit();
            limit.setRowCount(new LongValue(maxRows));
            ps.setLimit(limit);
        } else {
            Expression rowCount = limit.getRowCount();
            if (rowCount instanceof LongValue) {
                long n = ((LongValue) rowCount).getValue();
                if (n > maxRows) {
                    limit.setRowCount(new LongValue(maxRows));
                }
            } else {
                // 非数字 LIMIT（如 LIMIT ? 参数）一律替换为上限，杜绝绕过
                limit.setRowCount(new LongValue(maxRows));
            }
        }

        // ---------- 7. deparse + 参数 ----------
        String rewritten = ps.toString();
        Map<String, Object> params = new HashMap<>();
        List<String> branchCodes = scope.getAllowedBranchCodes();
        List<String> orgCodes = scope.getAllowedOrgCodes();
        if (branchCodes != null && !branchCodes.isEmpty()) {
            params.put("branchCodes", branchCodes);
        }
        if (orgCodes != null && !orgCodes.isEmpty()) {
            params.put("orgCodes", orgCodes);
        }

        SafeQuery safeQuery = new SafeQuery();
        safeQuery.setRewrittenSql(rewritten);
        safeQuery.setParams(params);
        log.debug("NL2SQL 安全重写: 原SQL={} → 重写={}", rawSql, rewritten);
        return safeQuery;
    }

    /**
     * VIA_JOIN 表自动注入一条 INNER JOIN loan_account（权限代理）。
     * ON 条件：thisRef.关联列 = laAlias.loan_account；随后用 laAlias.branch_code 过滤。
     */
    private void injectLoanAccountJoin(PlainSelect ps, TableMeta meta, String thisRef, String laAlias) {
        JoinMeta jm = meta.getJoinToLoanAccount();
        if (jm == null) {
            throw new SqlGuardException("表 " + meta.getName() + " 缺少关联配置");
        }
        // 关联列名（去掉表名前缀，如 collection_record.loan_account → loan_account）
        String thisCol = jm.getThisColumn().substring(jm.getThisColumn().indexOf('.') + 1);

        // 4.9 中 JOIN 的别名设置在 rightItem 的 Table 上，Join 无 setAlias
        Table laTable = new Table("loan_account");
        laTable.setAlias(new Alias(laAlias));

        Join join = new Join();
        join.setInner(true);
        join.setRightItem(laTable);
        join.setOnExpression(new EqualsTo(
            new Column(new Table(thisRef), thisCol),
            new Column(laTable, "loan_account")));

        List<Join> joins = ps.getJoins();
        if (joins == null) {
            joins = new ArrayList<>();
            ps.setJoins(joins);
        }
        joins.add(join);
    }

    /** 取表的引用名（别名优先，否则表名），用于注入时限定列 */
    private String refName(Table t) {
        return t.getAlias() != null ? t.getAlias().getName() : t.getName();
    }

    /** 构造「列 IN (:param)」过滤；codes 为 null 表示不限（不注入），空集按 fail-closed 处理 */
    private Expression buildInFilter(String tableRef, String column, String paramName, List<String> codes) {
        if (codes == null) {
            return null; // null = 不限（管理员/全国），不注入
        }
        if (codes.isEmpty()) {
            if (failClosed) {
                throw new SqlGuardException("无权限访问该数据范围（" + column + " 为空）");
            }
            // 非 fail-closed：注入永假条件 1=0，返回空结果
            return new EqualsTo(new LongValue(1), new LongValue(0));
        }
        Column left = new Column(new Table(tableRef), column);
        // 必须用 ParenthesedExpressionList 输出「IN (:param)」带括号形式，
        // 否则 JSqlParser 4.9 deparse 成「IN :param」，Spring NamedParameterJdbcTemplate
        // 展开 Collection 会得到非法「IN ?, ?, ...」
        ParenthesedExpressionList<Expression> items = new ParenthesedExpressionList<>();
        items.addExpression(new JdbcNamedParameter(paramName));
        return new InExpression(left, items);
    }

    /** 把多个过滤条件用 AND 连接，忽略 null；全为 null 返回 null */
    private Expression combineAnd(List<Expression> filters) {
        Expression result = null;
        for (Expression f : filters) {
            if (f == null) {
                continue;
            }
            result = (result == null) ? f : new AndExpression(result, f);
        }
        return result;
    }
}
