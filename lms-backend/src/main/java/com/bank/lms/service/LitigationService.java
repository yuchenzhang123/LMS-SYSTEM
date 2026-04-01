package com.bank.lms.service;

import com.bank.lms.dto.request.LitigationUpdateRequest;
import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.Litigation;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LitigationRepository;
import com.bank.lms.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 诉讼信息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LitigationService {

    private final LitigationRepository litigationRepository;
    private final CollectionRecordRepository collectionRecordRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanAccountService loanAccountService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random RANDOM = new Random();

    /**
     * 获取诉讼信息列表
     */
    public List<Map<String, Object>> getLitigationList(String loanAccount) {
        List<Litigation> list = litigationRepository.findByLoanAccountOrderByUpdatedAtDesc(loanAccount);
        return list.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * 获取诉讼详情
     */
    public Map<String, Object> getLitigationDetail(String litigationId) {
        Litigation litigation = litigationRepository.findById(litigationId)
                .orElseThrow(() -> new RuntimeException("未找到诉讼记录"));
        return toDetailMap(litigation);
    }

    /**
     * 更新诉讼信息（新增或修改）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateLitigation(LitigationUpdateRequest request) {
        LoanAccount account = loanAccountRepository.findById(request.getLoanAccount())
                .orElseThrow(() -> new RuntimeException("账户不存在"));

        String litigationId = request.getLitigationId();
        Litigation litigation = null;
        String previousStatus = "未登记";
        boolean isNew = (litigationId == null || litigationId.trim().isEmpty());

        if (!isNew) {
            litigation = litigationRepository.findById(litigationId).orElse(null);
            if (litigation != null) {
                previousStatus = litigation.getStatusText();
            } else {
                isNew = true;
            }
        }

        if (isNew) {
            litigation = new Litigation();
            litigation.setLitigationId(generateLitigationId());
        }

        LocalDateTime now = LocalDateTime.now();

        litigation.setLoanAccount(request.getLoanAccount());
        litigation.setCustomerId(request.getCustomerId());
        litigation.setCustomerName(account.getCustomerName());
        litigation.setStatusCode(request.getStatusCode());
        litigation.setStatusText(request.getStatusText());
        litigation.setInLitigation(request.getInLitigation() != null ? request.getInLitigation() : false);
        litigation.setSubmitToLawFirmDate(emptyToDefault(request.getSubmitToLawFirmDate()));
        litigation.setSubmitToCourtDate(emptyToDefault(request.getSubmitToCourtDate()));
        litigation.setFilingCaseNo(emptyToDefault(request.getFilingCaseNo()));
        litigation.setIsHearing(request.getIsHearing() != null ? request.getIsHearing() : false);
        litigation.setHearingDate(emptyToDefault(request.getHearingDate()));
        litigation.setJudgmentDate(emptyToDefault(request.getJudgmentDate()));
        litigation.setExecutionApplyToCourtDate(emptyToDefault(request.getExecutionApplyToCourtDate()));
        litigation.setExecutionFilingDate(emptyToDefault(request.getExecutionFilingDate()));
        litigation.setExecutionCaseNo(emptyToDefault(request.getExecutionCaseNo()));
        litigation.setAuctionStatus(emptyToDefault(request.getAuctionStatus()));
        litigation.setLitigationFee(parseDecimal(request.getLitigationFee()));
        litigation.setLitigationFeePaidByCustomer(request.getLitigationFeePaidByCustomer() != null ? request.getLitigationFeePaidByCustomer() : false);
        litigation.setPreservationFee(parseDecimal(request.getPreservationFee()));
        litigation.setPreservationFeePaidByCustomer(request.getPreservationFeePaidByCustomer() != null ? request.getPreservationFeePaidByCustomer() : false);
        litigation.setAppraisalFee(parseDecimal(request.getAppraisalFee()));
        litigation.setLitigationPreservationPaidAt(emptyToDefault(request.getLitigationPreservationPaidAt()));
        litigation.setLitigationPreservationWriteOffAt(emptyToDefault(request.getLitigationPreservationWriteOffAt()));
        litigation.setLawyerFee(parseDecimal(request.getLawyerFee()));
        litigation.setLawyerFeePaidByCustomer(request.getLawyerFeePaidByCustomer() != null ? request.getLawyerFeePaidByCustomer() : false);
        litigation.setCourtName(emptyToDefault(request.getCourtName()));
        litigation.setLawFirm(emptyToDefault(request.getLawFirm()));
        litigation.setRemark(emptyToDefault(request.getRemark()));

        litigationRepository.save(litigation);
        log.info("{}诉讼信息: {}", isNew ? "新增" : "更新", litigation.getLitigationId());

        // 在同一事务内更新账户状态
        if ("uncollected".equalsIgnoreCase(account.getStatus())) {
            account.setStatus("collecting");
            account.setStatusUpdateTime(LocalDateTime.now());
            loanAccountRepository.save(account);
        }

        // 创建催收记录
        CollectionRecord record = new CollectionRecord();
        record.setRecordId(generateRecordId());
        record.setLoanAccount(request.getLoanAccount());
        record.setCustomerId(request.getCustomerId());
        record.setCustomerName(account.getCustomerName());
        record.setMethod("litigation");
        record.setMethodText("诉讼");
        record.setResult("诉讼状态变动：" + previousStatus + "->" + request.getStatusText());
        record.setOperatorId(emptyToDefault(request.getOperatorId()));
        record.setOperatorName(request.getOperatorName() != null ? request.getOperatorName() : "当前用户");
        record.setOperateTime(now);
        record.setRemark(emptyToDefault(request.getRemark()));
        collectionRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("litigationInfo", toDetailMap(litigation));
        result.put("record", toRecordMap(record));
        return result;
    }

    private Map<String, Object> toListItem(Litigation litigation) {
        Map<String, Object> map = new HashMap<>();
        map.put("litigationId", litigation.getLitigationId());
        map.put("loanAccount", litigation.getLoanAccount());
        map.put("statusCode", litigation.getStatusCode());
        map.put("statusText", litigation.getStatusText());
        map.put("remark", litigation.getRemark());
        map.put("updatedAt", litigation.getUpdatedAt() != null ? litigation.getUpdatedAt().format(DATE_FORMATTER) : "");
        return map;
    }

    private Map<String, Object> toDetailMap(Litigation litigation) {
        Map<String, Object> map = new HashMap<>();
        map.put("litigationId", litigation.getLitigationId());
        map.put("loanAccount", litigation.getLoanAccount());
        map.put("customerId", litigation.getCustomerId());
        map.put("customerName", litigation.getCustomerName());
        map.put("statusCode", litigation.getStatusCode());
        map.put("statusText", litigation.getStatusText());
        map.put("inLitigation", litigation.getInLitigation());
        map.put("submitToLawFirmDate", litigation.getSubmitToLawFirmDate());
        map.put("submitToCourtDate", litigation.getSubmitToCourtDate());
        map.put("filingCaseNo", litigation.getFilingCaseNo());
        map.put("isHearing", litigation.getIsHearing());
        map.put("hearingDate", litigation.getHearingDate());
        map.put("judgmentDate", litigation.getJudgmentDate());
        map.put("executionApplyToCourtDate", litigation.getExecutionApplyToCourtDate());
        map.put("executionFilingDate", litigation.getExecutionFilingDate());
        map.put("executionCaseNo", litigation.getExecutionCaseNo());
        map.put("auctionStatus", litigation.getAuctionStatus());
        map.put("litigationFee", litigation.getLitigationFee() != null ? litigation.getLitigationFee().toString() : "0.00");
        map.put("litigationFeePaidByCustomer", litigation.getLitigationFeePaidByCustomer());
        map.put("preservationFee", litigation.getPreservationFee() != null ? litigation.getPreservationFee().toString() : "0.00");
        map.put("preservationFeePaidByCustomer", litigation.getPreservationFeePaidByCustomer());
        map.put("appraisalFee", litigation.getAppraisalFee() != null ? litigation.getAppraisalFee().toString() : "0.00");
        map.put("litigationPreservationPaidAt", litigation.getLitigationPreservationPaidAt());
        map.put("litigationPreservationWriteOffAt", litigation.getLitigationPreservationWriteOffAt());
        map.put("lawyerFee", litigation.getLawyerFee() != null ? litigation.getLawyerFee().toString() : "0.00");
        map.put("lawyerFeePaidByCustomer", litigation.getLawyerFeePaidByCustomer());
        map.put("courtName", litigation.getCourtName());
        map.put("lawFirm", litigation.getLawFirm());
        map.put("remark", litigation.getRemark());
        map.put("createdAt", litigation.getCreatedAt() != null ? litigation.getCreatedAt().format(DATE_FORMATTER) : "");
        map.put("updatedAt", litigation.getUpdatedAt() != null ? litigation.getUpdatedAt().format(DATE_FORMATTER) : "");
        return map;
    }

    private Map<String, Object> toRecordMap(CollectionRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getRecordId());
        map.put("loanAccount", record.getLoanAccount());
        map.put("customerId", record.getCustomerId());
        map.put("method", record.getMethod());
        map.put("methodText", record.getMethodText());
        map.put("result", record.getResult());
        map.put("operatorId", record.getOperatorId());
        map.put("operatorName", record.getOperatorName());
        map.put("time", record.getOperateTime() != null ? record.getOperateTime().format(DATE_FORMATTER) : "");
        map.put("remark", record.getRemark());
        return map;
    }

    private String generateLitigationId() {
        return "L" + System.currentTimeMillis() + String.format("%04d", RANDOM.nextInt(10000));
    }

    private String generateRecordId() {
        return "R" + System.currentTimeMillis() + String.format("%04d", RANDOM.nextInt(10000));
    }

    private String emptyToDefault(String value) {
        return value != null ? value : "";
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
