package com.bank.lms.service;

import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.LoanAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * GBase 数据同步服务
 * 针对几十万级数据量优化：分批拉取 + 批量查询 + 批量保存 + 异步通知
 */
@Slf4j
@Service
public class GbaseSyncService {

    private final JdbcTemplate gbaseJdbcTemplate;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanAccountService loanAccountService;
    private final DataSource mainDataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 防止并发执行同步任务 */
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    /** 每批从 GBase 拉取的记录数，可通过配置调整 */
    @Value("${gbase.sync.batch-size:2000}")
    private int batchSize;

    /** 每批写入本地库的记录数，与 JPA batch_size 对齐 */
    @Value("${gbase.sync.save-batch-size:500}")
    private int saveBatchSize;

    @Value("${gbase.sync.view-name:R_V_O_LOAN_ACTINFO_PER_T}")
    private String gbaseViewName;

    public GbaseSyncService(
            @Qualifier("gbaseJdbcTemplate") JdbcTemplate gbaseJdbcTemplate,
            LoanAccountRepository loanAccountRepository,
            LoanAccountService loanAccountService,
            @Qualifier("mainDataSource") DataSource mainDataSource) {
        this.gbaseJdbcTemplate = gbaseJdbcTemplate;
        this.loanAccountRepository = loanAccountRepository;
        this.loanAccountService = loanAccountService;
        this.mainDataSource = mainDataSource;
    }

    // -------------------------------------------------------------------------
    // 主入口
    // -------------------------------------------------------------------------

    public void syncFromGbase() {
        if (!syncing.compareAndSet(false, true)) {
            log.warn("GBase同步任务已在执行中，跳过本次触发");
            return;
        }
        try {
            doSync();
        } finally {
            syncing.set(false);
        }
    }

    private void doSync() {
        log.info("开始执行GBase数据同步任务，视图：{}，每批拉取：{}条", gbaseViewName, batchSize);
        long startTime = System.currentTimeMillis();

        // 首次入库检测：本地表为空时走快速全量导入路径
        long localCount = loanAccountRepository.count();
        if (localCount == 0) {
            log.info("本地表为空，启用首次全量快速导入模式（JDBC 原生批量 INSERT）");
            fullImport();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("首次全量导入完成，耗时={}ms", elapsed);
            return;
        }

        int totalInserted = 0;
        int totalUpdated = 0;
        int totalSkipped = 0;
        int offset = 0;

        try {
            while (true) {
                List<LoanAccount> batch = fetchBatchFromGbase(offset, batchSize);
                if (batch.isEmpty()) break;

                int[] result = processBatch(batch);
                totalInserted += result[0];
                totalUpdated  += result[1];
                totalSkipped  += result[2];

                log.info("已处理 offset={} 批次：新增={}，更新={}，跳过={}",
                        offset, result[0], result[1], result[2]);

                offset += batch.size();
                if (batch.size() < batchSize) break;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("GBase数据同步完成：总处理={}，新增={}，更新={}，跳过={}，耗时={}ms",
                    offset, totalInserted, totalUpdated, totalSkipped, elapsed);

        } catch (Exception e) {
            log.error("GBase数据同步失败，已处理offset={}", offset, e);
            throw new RuntimeException("GBase数据同步失败", e);
        }
    }

    // -------------------------------------------------------------------------
    // 首次全量快速导入：JDBC 原生批量 INSERT，绕过 JPA 开销
    // -------------------------------------------------------------------------

    private void fullImport() {
        String insertSql = "INSERT INTO loan_account (" +
                "loan_account, customer_id, customer_name, phone, product_code, " +
                "loan_date, loan_term, overdue_days, contract_amount, loan_balance, " +
                "unexpired_principal, overdue_principal, overdue_interest, overdue_penalty, " +
                "total_overdue_amount, status, status_update_time, gbase_sync_time, " +
                "gbase_raw_data, extra_data, branch_code, branch_name, " +
                "created_at, updated_at, is_deleted" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        int offset = 0;
        int totalInserted = 0;
        int totalSkipped = 0;
        LocalDateTime now = LocalDateTime.now();
        // 跨批去重：GBase 视图可能存在重复账户号
        Set<String> inserted = new HashSet<>();

        try (Connection conn = mainDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                while (true) {
                    List<LoanAccount> batch = fetchBatchFromGbase(offset, batchSize);
                    if (batch.isEmpty()) break;

                    int batchInserted = 0;
                    for (LoanAccount a : batch) {
                        // gracePeriod 为 null 的跳过
                        if (isGracePeriodNull(a.getExtraData())) {
                            totalSkipped++;
                            continue;
                        }
                        // 跨批去重，跳过重复账户号
                        if (!inserted.add(a.getLoanAccount())) {
                            log.warn("全量导入跳过重复账户号：{} 客户：{}", a.getLoanAccount(), a.getCustomerName());
                            totalSkipped++;
                            continue;
                        }

                        ps.setString(1, a.getLoanAccount());
                        ps.setString(2, a.getCustomerId());
                        ps.setString(3, a.getCustomerName());
                        ps.setString(4, a.getPhone());
                        ps.setString(5, a.getProductCode());
                        ps.setObject(6, a.getLoanDate());
                        ps.setObject(7, a.getLoanTerm());
                        ps.setObject(8, a.getOverdueDays() != null ? a.getOverdueDays() : 0);
                        ps.setBigDecimal(9, a.getContractAmount());
                        ps.setBigDecimal(10, a.getLoanBalance());
                        ps.setBigDecimal(11, a.getUnexpiredPrincipal());
                        ps.setBigDecimal(12, a.getOverduePrincipal());
                        ps.setBigDecimal(13, a.getOverdueInterest());
                        ps.setBigDecimal(14, a.getOverduePenalty());
                        ps.setBigDecimal(15, a.getTotalOverdueAmount());
                        ps.setString(16, "uncollected");
                        ps.setObject(17, now);
                        ps.setObject(18, now);
                        ps.setString(19, safeToJson(a));
                        ps.setString(20, a.getExtraData());
                        ps.setString(21, a.getBranchCode());
                        ps.setString(22, a.getBranchName());
                        ps.setObject(23, now);
                        ps.setObject(24, now);
                        ps.setInt(25, 0);
                        ps.addBatch();
                        batchInserted++;
                    }

                    if (batchInserted > 0) {
                        ps.executeBatch();
                        conn.commit();
                        totalInserted += batchInserted;
                        log.info("全量导入进度：已写入 {} 条（本批 {}）", totalInserted, batchInserted);
                    }

                    offset += batch.size();
                    if (batch.size() < batchSize) break;
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("首次全量导入失败，已写入 {} 条，跳过 {} 条", totalInserted, totalSkipped, e);
            throw new RuntimeException("首次全量导入失败", e);
        }

        log.info("首次全量导入完成，共写入 {} 条，跳过 {} 条（重复或无效）", totalInserted, totalSkipped);
    }

    // -------------------------------------------------------------------------
    // 分批拉取
    // -------------------------------------------------------------------------

    private List<LoanAccount> fetchBatchFromGbase(int offset, int limit) {
        String sql = "SELECT LOAN_ACCT_NO, CUST_NO, CUST_NAME, MOBILE_NO, LOAN_TYPE, " +
                "DUE_STRT_DATE, LOAN_LIFE_TRM, UNPD_DAYS, APP_AMT, LOAN_BAL, " +
                "THEO_LOAN_BAL, UNPD_PRIN_BAL, CAP_UNPD_INT, UNPD_ARRS_INT_BAL, " +
                "UNPD_INT_BAL, AUTO_RISK_GRADE, GRACE_PERIOD, LOAN_BRANCH_NO, LOAN_BRANCH_NAME " +
                "FROM " + gbaseViewName +
                " LIMIT ? OFFSET ?";
        return gbaseJdbcTemplate.query(sql, new GbaseLoanAccountRowMapper(), limit, offset);
    }

    // -------------------------------------------------------------------------
    // 批量处理一批数据
    // -------------------------------------------------------------------------

    @Transactional
    public int[] processBatch(List<LoanAccount> sourceBatch) {
        int inserted = 0, updated = 0, skipped = 0;

        // 批内去重：GBase 视图可能存在重复账户号，保留最后一条
        Map<String, LoanAccount> deduped = new LinkedHashMap<>();
        for (LoanAccount a : sourceBatch) {
            if (deduped.put(a.getLoanAccount(), a) != null) {
                log.warn("增量同步跳过批内重复账户号：{} 客户：{}", a.getLoanAccount(), a.getCustomerName());
                skipped++;
            }
        }
        List<LoanAccount> dedupedBatch = new ArrayList<>(deduped.values());

        // 收集本批所有账户号
        List<String> accountNos = dedupedBatch.stream()
                .map(LoanAccount::getLoanAccount)
                .collect(Collectors.toList());

        // 一次批量查询本地库（替代 N 次 findById）
        Map<String, LoanAccount> existingMap = loanAccountRepository
                .findAllByLoanAccountIn(accountNos)
                .stream()
                .collect(Collectors.toMap(LoanAccount::getLoanAccount, a -> a));

        List<LoanAccount> toSave = new ArrayList<>();

        // 收集需要异步通知的事件，避免在循环中同步调用
        List<OverdueNotifyEvent> overdueEvents = new ArrayList<>();
        List<LoanAccount> completedEvents = new ArrayList<>();

        for (LoanAccount source : dedupedBatch) {
            LoanAccount existing = existingMap.get(source.getLoanAccount());

            if (existing == null) {
                // 新增：GBase GRACE_PERIOD 为 null 的跳过
                if (isGracePeriodNull(source.getExtraData())) {
                    log.debug("跳过新增账户 {}：GBase GRACE_PERIOD 为 null", source.getLoanAccount());
                    skipped++;
                    continue;
                }
                source.setStatus(source.getStatus() == null || source.getStatus().isEmpty()
                        ? "uncollected" : source.getStatus());
                source.setStatusUpdateTime(LocalDateTime.now());
                source.setGbaseSyncTime(LocalDateTime.now());
                source.setGbaseRawData(safeToJson(source));
                toSave.add(source);
                inserted++;
                // 新增账户不发通知：入库时 gracePeriod 已为1说明是历史逾期数据，不是今天新发生的事件
            } else {
                // 更新：比对字段变化
                boolean changed = mergeFields(source, existing);

                Integer oldGp = getGracePeriodFromExtraData(existing.getExtraData());
                Integer newGp = getGracePeriodFromExtraData(source.getExtraData());

                // GRACE_PERIOD 1→0：逾期了结，转 completed
                if (isOne(oldGp) && isZero(newGp)) {
                    if ("collecting".equalsIgnoreCase(existing.getStatus())
                            || "uncollected".equalsIgnoreCase(existing.getStatus())) {
                        existing.setStatus("completed");
                        existing.setStatusUpdateTime(LocalDateTime.now());
                        changed = true;
                        completedEvents.add(existing);
                    }
                }

                // GRACE_PERIOD 0/null→1：新增逾期，completed 转 uncollected；collecting 状态不重复通知
                if (!isOne(oldGp) && isOne(newGp)) {
                    if ("completed".equalsIgnoreCase(existing.getStatus())) {
                        existing.setStatus("uncollected");
                        existing.setStatusUpdateTime(LocalDateTime.now());
                        changed = true;
                    }
                    // collecting 说明已在催收中，不重复发逾期通知
                    if (!"collecting".equalsIgnoreCase(existing.getStatus())) {
                        overdueEvents.add(new OverdueNotifyEvent(existing,
                                source.getOverdueDays() != null ? source.getOverdueDays() : 0));
                    }
                } else if (isOne(newGp) && "completed".equalsIgnoreCase(existing.getStatus())) {
                    // 兜底：gracePeriod 持续为1但状态异常为 completed（数据修正）
                    existing.setStatus("uncollected");
                    existing.setStatusUpdateTime(LocalDateTime.now());
                    changed = true;
                    overdueEvents.add(new OverdueNotifyEvent(existing,
                            source.getOverdueDays() != null ? source.getOverdueDays() : 0));
                    log.warn("数据一致性修正：账户 {} gracePeriod=1 但状态为 completed，已回退为 uncollected",
                            existing.getLoanAccount());
                }

                existing.setGbaseSyncTime(LocalDateTime.now());
                existing.setGbaseRawData(safeToJson(source));

                if (changed) {
                    toSave.add(existing);
                    updated++;
                }
            }
        }

        // 分批写入本地库
        batchSave(toSave);

        // 批量处理完成后统一发通知（避免在事务中同步调用外部服务）
        for (OverdueNotifyEvent e : overdueEvents) {
            try {
                loanAccountService.notifyNewOverdue(e.account, e.overdueDays);
            } catch (Exception ex) {
                log.warn("发送逾期通知失败，账户：{}", e.account.getLoanAccount(), ex);
            }
        }
        for (LoanAccount account : completedEvents) {
            try {
                loanAccountService.notifyCollectingCompleted(account);
            } catch (Exception ex) {
                log.warn("发送完成通知失败，账户：{}", account.getLoanAccount(), ex);
            }
        }

        return new int[]{inserted, updated, skipped};
    }

    // -------------------------------------------------------------------------
    // 分批写入（避免单次 saveAll 几千条撑爆内存/事务）
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // 分批写入（使用 JDBC 原生批量操作，避免 Hibernate 行数校验问题）
    // -------------------------------------------------------------------------

    private void batchSave(List<LoanAccount> list) {
        if (list.isEmpty()) return;

        // 区分新增和更新
        List<LoanAccount> toInsert = new ArrayList<>();
        List<LoanAccount> toUpdate = new ArrayList<>();

        for (LoanAccount account : list) {
            if (account.getCreatedAt() == null) {
                // createdAt 为空说明是新增
                account.setCreatedAt(LocalDateTime.now());
                account.setUpdatedAt(LocalDateTime.now());
                toInsert.add(account);
            } else {
                // 已有 createdAt 说明是更新
                account.setUpdatedAt(LocalDateTime.now());
                toUpdate.add(account);
            }
        }

        try (Connection conn = mainDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!toInsert.isEmpty()) {
                    batchInsert(conn, toInsert);
                }
                if (!toUpdate.isEmpty()) {
                    batchUpdate(conn, toUpdate);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("JDBC 批量保存失败，数量：{}", list.size(), e);
            throw new RuntimeException("批量保存失败", e);
        }
    }

    private void batchInsert(Connection conn, List<LoanAccount> list) throws SQLException {
        String sql = "INSERT INTO loan_account (" +
                "loan_account, customer_id, customer_name, phone, product_code, " +
                "loan_date, loan_term, overdue_days, contract_amount, loan_balance, " +
                "unexpired_principal, overdue_principal, overdue_interest, overdue_penalty, " +
                "total_overdue_amount, status, status_update_time, gbase_sync_time, " +
                "gbase_raw_data, extra_data, branch_code, branch_name, " +
                "created_at, updated_at, is_deleted" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (LoanAccount a : list) {
                ps.setString(1, a.getLoanAccount());
                ps.setString(2, a.getCustomerId());
                ps.setString(3, a.getCustomerName());
                ps.setString(4, a.getPhone());
                ps.setString(5, a.getProductCode());
                ps.setObject(6, a.getLoanDate());
                ps.setObject(7, a.getLoanTerm());
                ps.setObject(8, a.getOverdueDays());
                ps.setBigDecimal(9, a.getContractAmount());
                ps.setBigDecimal(10, a.getLoanBalance());
                ps.setBigDecimal(11, a.getUnexpiredPrincipal());
                ps.setBigDecimal(12, a.getOverduePrincipal());
                ps.setBigDecimal(13, a.getOverdueInterest());
                ps.setBigDecimal(14, a.getOverduePenalty());
                ps.setBigDecimal(15, a.getTotalOverdueAmount());
                ps.setString(16, a.getStatus());
                ps.setObject(17, a.getStatusUpdateTime());
                ps.setObject(18, a.getGbaseSyncTime());
                ps.setString(19, a.getGbaseRawData());
                ps.setString(20, a.getExtraData());
                ps.setString(21, a.getBranchCode());
                ps.setString(22, a.getBranchName());
                ps.setObject(23, a.getCreatedAt());
                ps.setObject(24, a.getUpdatedAt());
                ps.setInt(25, a.getIsDeleted() != null ? a.getIsDeleted() : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchUpdate(Connection conn, List<LoanAccount> list) throws SQLException {
        String sql = "UPDATE loan_account SET " +
                "customer_id=?, customer_name=?, phone=?, product_code=?, " +
                "loan_date=?, loan_term=?, overdue_days=?, contract_amount=?, loan_balance=?, " +
                "unexpired_principal=?, overdue_principal=?, overdue_interest=?, overdue_penalty=?, " +
                "total_overdue_amount=?, status=?, status_update_time=?, gbase_sync_time=?, " +
                "gbase_raw_data=?, extra_data=?, branch_code=?, branch_name=?, updated_at=? " +
                "WHERE loan_account=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (LoanAccount a : list) {
                ps.setString(1, a.getCustomerId());
                ps.setString(2, a.getCustomerName());
                ps.setString(3, a.getPhone());
                ps.setString(4, a.getProductCode());
                ps.setObject(5, a.getLoanDate());
                ps.setObject(6, a.getLoanTerm());
                ps.setObject(7, a.getOverdueDays());
                ps.setBigDecimal(8, a.getContractAmount());
                ps.setBigDecimal(9, a.getLoanBalance());
                ps.setBigDecimal(10, a.getUnexpiredPrincipal());
                ps.setBigDecimal(11, a.getOverduePrincipal());
                ps.setBigDecimal(12, a.getOverdueInterest());
                ps.setBigDecimal(13, a.getOverduePenalty());
                ps.setBigDecimal(14, a.getTotalOverdueAmount());
                ps.setString(15, a.getStatus());
                ps.setObject(16, a.getStatusUpdateTime());
                ps.setObject(17, a.getGbaseSyncTime());
                ps.setString(18, a.getGbaseRawData());
                ps.setString(19, a.getExtraData());
                ps.setString(20, a.getBranchCode());
                ps.setString(21, a.getBranchName());
                ps.setObject(22, a.getUpdatedAt());
                ps.setString(23, a.getLoanAccount());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // -------------------------------------------------------------------------
    // 字段合并（只更新有变化的字段，返回是否有变化）
    // -------------------------------------------------------------------------

    private boolean mergeFields(LoanAccount source, LoanAccount existing) {
        boolean changed = false;
        if (source.getCustomerId() != null && !source.getCustomerId().equals(existing.getCustomerId())) {
            existing.setCustomerId(source.getCustomerId()); changed = true;
        }
        if (source.getCustomerName() != null && !source.getCustomerName().equals(existing.getCustomerName())) {
            existing.setCustomerName(source.getCustomerName()); changed = true;
        }
        if (source.getPhone() != null && !source.getPhone().equals(existing.getPhone())) {
            existing.setPhone(source.getPhone()); changed = true;
        }
        if (source.getProductCode() != null && !source.getProductCode().equals(existing.getProductCode())) {
            existing.setProductCode(source.getProductCode()); changed = true;
        }
        if (source.getProductName() != null && !source.getProductName().equals(existing.getProductName())) {
            existing.setProductName(source.getProductName()); changed = true;
        }
        if (source.getLoanDate() != null && !source.getLoanDate().equals(existing.getLoanDate())) {
            existing.setLoanDate(source.getLoanDate()); changed = true;
        }
        if (source.getLoanTerm() != null && !source.getLoanTerm().equals(existing.getLoanTerm())) {
            existing.setLoanTerm(source.getLoanTerm()); changed = true;
        }
        if (source.getOverdueDays() != null && !source.getOverdueDays().equals(existing.getOverdueDays())) {
            existing.setOverdueDays(source.getOverdueDays()); changed = true;
        }
        if (source.getContractAmount() != null && !source.getContractAmount().equals(existing.getContractAmount())) {
            existing.setContractAmount(source.getContractAmount()); changed = true;
        }
        if (source.getLoanBalance() != null && !source.getLoanBalance().equals(existing.getLoanBalance())) {
            existing.setLoanBalance(source.getLoanBalance()); changed = true;
        }
        if (source.getUnexpiredPrincipal() != null && !source.getUnexpiredPrincipal().equals(existing.getUnexpiredPrincipal())) {
            existing.setUnexpiredPrincipal(source.getUnexpiredPrincipal()); changed = true;
        }
        if (source.getOverduePrincipal() != null && !source.getOverduePrincipal().equals(existing.getOverduePrincipal())) {
            existing.setOverduePrincipal(source.getOverduePrincipal()); changed = true;
        }
        if (source.getOverdueInterest() != null && !source.getOverdueInterest().equals(existing.getOverdueInterest())) {
            existing.setOverdueInterest(source.getOverdueInterest()); changed = true;
        }
        if (source.getOverduePenalty() != null && !source.getOverduePenalty().equals(existing.getOverduePenalty())) {
            existing.setOverduePenalty(source.getOverduePenalty()); changed = true;
        }
        if (source.getTotalOverdueAmount() != null && !source.getTotalOverdueAmount().equals(existing.getTotalOverdueAmount())) {
            existing.setTotalOverdueAmount(source.getTotalOverdueAmount()); changed = true;
        }
        if (source.getExtraData() != null && !source.getExtraData().equals(existing.getExtraData())) {
            existing.setExtraData(source.getExtraData()); changed = true;
        }
        return changed;
    }

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

    private Integer getGracePeriodFromExtraData(String extraData) {
        if (extraData == null || extraData.trim().isEmpty()) return 0;
        try {
            Map<String, Object> extra = objectMapper.readValue(extraData, Map.class);
            Object gp = extra.get("gracePeriod");
            if (gp instanceof Integer) return (Integer) gp;
            if (gp instanceof Number) return ((Number) gp).intValue();
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean isGracePeriodNull(String extraData) {
        if (extraData == null || extraData.trim().isEmpty()) return true;
        try {
            Map<String, Object> extra = objectMapper.readValue(extraData, Map.class);
            return !extra.containsKey("gracePeriod") || extra.get("gracePeriod") == null;
        } catch (Exception ignored) {}
        return true;
    }

    private boolean isOne(Integer val) {
        return val != null && val == 1;
    }

    private boolean isZero(Integer val) {
        return val != null && val == 0;
    }

    private String safeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /** 通知事件暂存对象 */
    private static class OverdueNotifyEvent {
        final LoanAccount account;
        final int overdueDays;
        OverdueNotifyEvent(LoanAccount account, int overdueDays) {
            this.account = account;
            this.overdueDays = overdueDays;
        }
    }

    // -------------------------------------------------------------------------
    // GBase RowMapper
    // -------------------------------------------------------------------------

    private static class GbaseLoanAccountRowMapper implements RowMapper<LoanAccount> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public LoanAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
            LoanAccount account = new LoanAccount();
            account.setLoanAccount(rs.getString("LOAN_ACCT_NO"));
            account.setCustomerId(rs.getString("CUST_NO"));
            account.setCustomerName(rs.getString("CUST_NAME"));
            account.setPhone(rs.getString("MOBILE_NO"));
            account.setProductCode(rs.getString("LOAN_TYPE"));
            account.setLoanDate(rs.getDate("DUE_STRT_DATE") != null
                    ? rs.getDate("DUE_STRT_DATE").toLocalDate() : null);
            account.setLoanTerm(rs.getObject("LOAN_LIFE_TRM") != null
                    ? rs.getInt("LOAN_LIFE_TRM") : null);
            account.setOverdueDays(rs.getObject("UNPD_DAYS") != null
                    ? rs.getInt("UNPD_DAYS") : 0);
            account.setContractAmount(rs.getBigDecimal("APP_AMT"));
            account.setLoanBalance(rs.getBigDecimal("LOAN_BAL"));
            account.setUnexpiredPrincipal(rs.getBigDecimal("THEO_LOAN_BAL"));
            account.setOverduePrincipal(rs.getBigDecimal("UNPD_PRIN_BAL"));
            account.setOverdueInterest(rs.getBigDecimal("CAP_UNPD_INT"));
            account.setOverduePenalty(rs.getBigDecimal("UNPD_ARRS_INT_BAL"));
            account.setTotalOverdueAmount(rs.getBigDecimal("UNPD_INT_BAL"));

            Integer gracePeriod = rs.getObject("GRACE_PERIOD") != null
                    ? rs.getInt("GRACE_PERIOD") : null;
            // 新账户统一入库为 uncollected，collecting 由催收员手动操作触发
            account.setStatus("uncollected");
            account.setStatusUpdateTime(LocalDateTime.now());
            account.setBranchCode(rs.getString("LOAN_BRANCH_NO"));
            account.setBranchName(rs.getString("LOAN_BRANCH_NAME"));

            Map<String, Object> extra = new HashMap<>();
            extra.put("autoRiskGrade", rs.getString("AUTO_RISK_GRADE"));
            extra.put("gracePeriod", gracePeriod);
            try {
                account.setExtraData(mapper.writeValueAsString(extra));
            } catch (Exception ignore) {
                account.setExtraData(null);
            }
            return account;
        }
    }

    /**
     * 从 GBase rcrms.R_V_O_ORG_BASIC 表按机构号查询机构名
     */
    public Map<String, Object> lookupOrgInGbase(String code) {
        Map<String, Object> result = new HashMap<>();
        result.put("found", false);
        result.put("orgName", null);
        try {
            String sql = "SELECT ORG_NM FROM rcrms.R_V_O_ORG_BASIC WHERE ORG_ID = ? LIMIT 1";
            List<String> names = gbaseJdbcTemplate.queryForList(sql, String.class, code);
            if (!names.isEmpty() && names.get(0) != null) {
                result.put("found", true);
                result.put("orgName", names.get(0));
            }
        } catch (Exception e) {
            log.warn("GBase机构查询失败: {}", e.getMessage());
        }
        return result;
    }
}
