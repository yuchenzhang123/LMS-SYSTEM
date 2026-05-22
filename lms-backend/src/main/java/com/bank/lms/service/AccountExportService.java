package com.bank.lms.service;

import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.Litigation;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LitigationRepository;
import com.bank.lms.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountExportService {

    private final LoanAccountRepository loanAccountRepository;
    private final LitigationRepository litigationRepository;
    private final CollectionRecordRepository collectionRecordRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(ExportFilter filter) {
        // 查询账户
        List<LoanAccount> accounts = queryAccounts(filter);
        if (accounts.isEmpty()) {
            return createEmptyExcel();
        }

        // 批量预加载诉讼和催收记录，避免 N+1
        List<String> loanAccounts = accounts.stream()
                .map(LoanAccount::getLoanAccount).collect(Collectors.toList());
        Map<String, Litigation> litigationMap = buildLitigationMap(loanAccounts);
        Map<String, CollectionRecord> recordMap = buildCollectionRecordMap(loanAccounts);

        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        Sheet sheet = wb.createSheet("催收账户导出");

        // 表头样式
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 表头
        String[] headers = {
            "客户号", "客户名", "贷款账户", "产品码", "逾期天数",
            "贷款余额", "未到期本金", "逾期本金", "逾期利息", "逾期罚息", "总逾期金额",
            "状态", "机构名称",
            // 诉讼信息（全字段）
            "是否诉讼中", "诉讼状态码", "诉讼状态", "提交律所时间", "律所名称",
            "提交法院时间", "涉及法院", "诉讼立案案号", "是否开庭", "开庭时间",
            "判决时间", "执行申请提交时间", "执行立案时间", "执行立案案号", "拍卖状态",
            "诉讼费", "诉讼费客户已支付", "保全费", "保全费客户已支付", "评估费",
            "诉讼和保全支付时间", "诉讼和保全销账时间", "律师费", "律师费客户已支付",
            "诉讼备注", "最近诉讼更新时间",
            // 催收记录
            "最近催收时间", "最近催收方式", "最近催收结果"
        };
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据行
        int rowIdx = 1;
        for (LoanAccount acc : accounts) {
            Litigation latestLitigation = litigationMap.get(acc.getLoanAccount());
            CollectionRecord latestRecord = recordMap.get(acc.getLoanAccount());

            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            row.createCell(c++).setCellValue(nvl(acc.getCustomerId()));
            row.createCell(c++).setCellValue(nvl(acc.getCustomerName()));
            row.createCell(c++).setCellValue(nvl(acc.getLoanAccount()));
            row.createCell(c++).setCellValue(nvl(acc.getProductCode()));
            row.createCell(c++).setCellValue(acc.getOverdueDays() != null ? acc.getOverdueDays() : 0);
            row.createCell(c++).setCellValue(nvl(acc.getLoanBalance()));
            row.createCell(c++).setCellValue(nvl(acc.getUnexpiredPrincipal()));
            row.createCell(c++).setCellValue(nvl(acc.getOverduePrincipal()));
            row.createCell(c++).setCellValue(nvl(acc.getOverdueInterest()));
            row.createCell(c++).setCellValue(nvl(acc.getOverduePenalty()));
            row.createCell(c++).setCellValue(nvl(acc.getTotalOverdueAmount()));
            row.createCell(c++).setCellValue(statusText(acc.getStatus()));
            row.createCell(c++).setCellValue(nvl(acc.getBranchName()));

            // 诉讼信息（全字段）
            if (latestLitigation != null) {
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(latestLitigation.getInLitigation()) ? "是" : "否");
                row.createCell(c++).setCellValue(nvl(latestLitigation.getStatusCode()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getStatusText()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getSubmitToLawFirmDate()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getLawFirm()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getSubmitToCourtDate()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getCourtName()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getFilingCaseNo()));
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(latestLitigation.getIsHearing()) ? "是" : "否");
                row.createCell(c++).setCellValue(nvl(latestLitigation.getHearingDate()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getJudgmentDate()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getExecutionApplyToCourtDate()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getExecutionFilingDate()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getExecutionCaseNo()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getAuctionStatus()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getLitigationFee()));
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(latestLitigation.getLitigationFeePaidByCustomer()) ? "是" : "否");
                row.createCell(c++).setCellValue(nvl(latestLitigation.getPreservationFee()));
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(latestLitigation.getPreservationFeePaidByCustomer()) ? "是" : "否");
                row.createCell(c++).setCellValue(nvl(latestLitigation.getAppraisalFee()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getLitigationPreservationPaidAt()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getLitigationPreservationWriteOffAt()));
                row.createCell(c++).setCellValue(nvl(latestLitigation.getLawyerFee()));
                row.createCell(c++).setCellValue(Boolean.TRUE.equals(latestLitigation.getLawyerFeePaidByCustomer()) ? "是" : "否");
                row.createCell(c++).setCellValue(nvl(latestLitigation.getRemark()));
                row.createCell(c++).setCellValue(latestLitigation.getUpdatedAt() != null
                        ? latestLitigation.getUpdatedAt().format(DT_FMT) : "");
            } else {
                for (int i = 0; i < 26; i++) row.createCell(c++).setCellValue("");
            }

            // 最近催收记录
            if (latestRecord != null) {
                row.createCell(c++).setCellValue(latestRecord.getOperateTime() != null
                        ? latestRecord.getOperateTime().format(DT_FMT) : "");
                row.createCell(c++).setCellValue(nvl(latestRecord.getMethodText()));
                row.createCell(c++).setCellValue(nvl(latestRecord.getResult()));
            } else {
                for (int i = 0; i < 3; i++) row.createCell(c++).setCellValue("");
            }
        }

        // 自动调整列宽
        if (sheet instanceof org.apache.poi.xssf.streaming.SXSSFSheet) {
            ((org.apache.poi.xssf.streaming.SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            wb.dispose();
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    private List<LoanAccount> queryAccounts(ExportFilter filter) {
        Specification<LoanAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.getStatuses()));
            }
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("loanDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("loanDate"), filter.getEndDate()));
            }
            if (filter.getBranchCode() != null && !filter.getBranchCode().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("branchCode"), filter.getBranchCode().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return loanAccountRepository.findAll(spec);
    }

    /**
     * 批量查询诉讼信息，按 loanAccount 取最近一条（优先进行中）
     */
    private Map<String, Litigation> buildLitigationMap(List<String> loanAccounts) {
        List<Litigation> all = litigationRepository.findByLoanAccountInOrderByUpdatedAtDesc(loanAccounts);
        // 按 loanAccount 分组，每组取第一个 inLitigation=true，或最近一条
        Map<String, List<Litigation>> grouped = all.stream()
                .collect(Collectors.groupingBy(Litigation::getLoanAccount, LinkedHashMap::new, Collectors.toList()));
        Map<String, Litigation> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Litigation>> e : grouped.entrySet()) {
            Litigation pick = null;
            for (Litigation l : e.getValue()) {
                if (Boolean.TRUE.equals(l.getInLitigation())) {
                    pick = l; break;
                }
            }
            if (pick == null) pick = e.getValue().get(0);
            result.put(e.getKey(), pick);
        }
        return result;
    }

    /**
     * 批量查询催收记录，按 loanAccount 取最近一条
     */
    private Map<String, CollectionRecord> buildCollectionRecordMap(List<String> loanAccounts) {
        List<CollectionRecord> all = collectionRecordRepository.findByLoanAccountInOrderByOperateTimeDesc(loanAccounts);
        Map<String, CollectionRecord> result = new LinkedHashMap<>();
        for (CollectionRecord r : all) {
            result.putIfAbsent(r.getLoanAccount(), r);
        }
        return result;
    }

    private byte[] createEmptyExcel() {
        try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
            Sheet sheet = wb.createSheet("催收账户导出");
            sheet.createRow(0).createCell(0).setCellValue("无符合条件的数据");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            wb.dispose();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    private String nvl(Object val) {
        return val == null ? "" : String.valueOf(val);
    }

    private String nvl(BigDecimal val) {
        return val == null ? "" : val.toPlainString();
    }

    private String statusText(String status) {
        if ("uncollected".equals(status)) return "未催收";
        if ("collecting".equals(status)) return "催收中";
        if ("completed".equals(status)) return "已还款";
        return status == null ? "" : status;
    }

    public static class ExportFilter {
        private List<String> statuses;
        private LocalDate startDate;
        private LocalDate endDate;
        private String branchCode;

        public List<String> getStatuses() { return statuses; }
        public void setStatuses(List<String> statuses) { this.statuses = statuses; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getBranchCode() { return branchCode; }
        public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    }
}
