package com.bank.lms.service;

import com.bank.lms.dto.request.CollectionRecordAddRequest;
import com.bank.lms.entity.CollectionRecord;
import com.bank.lms.entity.LoanAccount;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 催收记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionRecordService {

    private final CollectionRecordRepository collectionRecordRepository;
    private final LoanAccountRepository loanAccountRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random RANDOM = new Random();

    /**
     * 获取催收记录列表
     */
    public List<Map<String, Object>> getRecordList(String loanAccount) {
        List<CollectionRecord> records = collectionRecordRepository.findByLoanAccountOrderByOperateTimeDesc(loanAccount);
        return records.stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    /**
     * 新增催收记录
     */
    @Transactional
    public Map<String, Object> addRecord(CollectionRecordAddRequest request) {
        // 获取账户信息
        LoanAccount account = loanAccountRepository.findById(request.getLoanAccount())
                .orElseThrow(() -> new RuntimeException("账户不存在"));

        CollectionRecord record = new CollectionRecord();
        record.setRecordId(generateRecordId());
        record.setLoanAccount(request.getLoanAccount());
        record.setCustomerId(request.getCustomerId());
        record.setCustomerName(account.getCustomerName());
        record.setMethod(request.getMethod());
        record.setMethodText(request.getMethodText());
        record.setResult(request.getResult());
        record.setOperatorId(request.getOperatorId() != null ? request.getOperatorId() : "");
        record.setOperatorName(request.getOperatorName() != null ? request.getOperatorName() : "当前用户");
        record.setOperateTime(request.getTime() != null ? parseDateTime(request.getTime()) : LocalDateTime.now());
        record.setRemark(request.getRemark() != null ? request.getRemark() : "");
        record.setMaterialType(request.getMaterialType() != null ? request.getMaterialType() : "");
        record.setMaterialName(request.getMaterialName() != null ? request.getMaterialName() : "");
        record.setMaterialUrl(request.getMaterialUrl() != null ? request.getMaterialUrl() : "");

        CollectionRecord saved = collectionRecordRepository.save(record);
        log.info("新增催收记录: {}", saved.getRecordId());

        return toMap(saved);
    }

    private Map<String, Object> toMap(CollectionRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getRecordId());
        map.put("recordId", record.getRecordId());
        map.put("loanAccount", record.getLoanAccount());
        map.put("customerId", record.getCustomerId());
        map.put("customerName", record.getCustomerName());
        map.put("method", record.getMethod());
        map.put("methodText", record.getMethodText());
        map.put("result", record.getResult());
        map.put("operatorId", record.getOperatorId());
        map.put("operatorName", record.getOperatorName());
        map.put("time", record.getOperateTime() != null ? record.getOperateTime().format(DATE_FORMATTER) : "");
        map.put("remark", record.getRemark());
        map.put("materialType", record.getMaterialType());
        map.put("materialName", record.getMaterialName());
        map.put("materialUrl", record.getMaterialUrl());
        return map;
    }

    private String generateRecordId() {
        return "R" + System.currentTimeMillis() + String.format("%04d", RANDOM.nextInt(10000));
    }

    private LocalDateTime parseDateTime(String time) {
        try {
            return LocalDateTime.parse(time, DATE_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
