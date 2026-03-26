package com.bank.lms.service;

import com.bank.lms.dto.request.NoticeMarkReadRequest;
import com.bank.lms.dto.request.NoticeQueryRequest;
import com.bank.lms.entity.Notice;
import com.bank.lms.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取通知列表
     */
    public Map<String, Object> getNoticeList(NoticeQueryRequest request) {
        Specification<Notice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getReadStatus() != null) {
                if (request.getReadStatus() == 0) {
                    predicates.add(cb.equal(root.get("isRead"), false));
                } else if (request.getReadStatus() == 1) {
                    predicates.add(cb.equal(root.get("isRead"), true));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int current = request.getPage() != null ? request.getPage().getCurrentPage() : 1;
        int size = request.getPage() != null ? request.getPage().getPageSize() : 10;

        Page<Notice> page = noticeRepository.findAll(spec,
                PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Map<String, Object>> records = page.getContent().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        long unreadCount = noticeRepository.countByIsReadFalse();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotalElements());
        result.put("unreadCount", unreadCount);
        result.put("size", size);
        result.put("current", current);

        return result;
    }

    /**
     * 标记通知已读
     */
    @Transactional
    public void markAsRead(NoticeMarkReadRequest request) {
        if (request.getNoticeIds() != null && !request.getNoticeIds().isEmpty()) {
            noticeRepository.markAsRead(request.getNoticeIds());
            log.info("标记通知已读: {}", request.getNoticeIds());
        }
    }

    public void createNotice(String title,
                             String level,
                             String message,
                             String customerId,
                             String loanAccount,
                             String customerName,
                             String productCode,
                             Integer overdueDays,
                             String noticeType) {
        // 去重：相同账号/客户/标题的通知，如果已有最新通知在 30 分钟内则不再重复创建
        Notice existing = noticeRepository.findTopByLoanAccountAndTitleAndCustomerIdOrderByCreatedAtDesc(loanAccount, title, customerId);
        if (existing != null && existing.getCreatedAt() != null && existing.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(30))) {
            log.info("去重通知，已存在最近 30 分钟相同标题通知: {} {}", loanAccount, title);
            return;
        }

        Notice notice = new Notice();
        notice.setNoticeId("N" + System.currentTimeMillis() + (int)(Math.random() * 10000));
        notice.setTitle(title);
        notice.setLevel(level);
        notice.setMessage(message);
        notice.setCustomerId(customerId);
        notice.setLoanAccount(loanAccount);
        notice.setCustomerName(customerName);
        notice.setProductCode(productCode);
        notice.setNoticeType(noticeType);
        notice.setOverdueDays(overdueDays);
        notice.setIsRead(false);
        noticeRepository.save(notice);
        log.info("自动生成通知: {} -> {}", loanAccount, title);
    }

    private Map<String, Object> toMap(Notice notice) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notice.getNoticeId());
        map.put("noticeId", notice.getNoticeId());
        map.put("title", notice.getTitle());
        map.put("level", notice.getLevel());
        map.put("message", notice.getMessage());
        map.put("customerId", notice.getCustomerId());
        map.put("loanAccount", notice.getLoanAccount());
        map.put("customerName", notice.getCustomerName());
        map.put("productCode", notice.getProductCode());
        map.put("noticeType", notice.getNoticeType());
        map.put("overdueDays", notice.getOverdueDays());
        map.put("read", notice.getIsRead());
        map.put("createdAt", notice.getCreatedAt() != null ? notice.getCreatedAt().format(DATE_FORMATTER) : "");
        return map;
    }
}
