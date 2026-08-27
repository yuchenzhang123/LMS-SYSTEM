package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.entity.UserLoginLog;
import com.bank.lms.entity.UserOrg;
import com.bank.lms.repository.CollectionRecordRepository;
import com.bank.lms.repository.UserLoginLogRepository;
import com.bank.lms.repository.UserOrgRepository;
import com.bank.lms.service.OrgGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户相关接口：登录上报 + 员工统计
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserLoginLogRepository userLoginLogRepository;
    private final UserOrgRepository userOrgRepository;
    private final OrgGroupService orgGroupService;
    private final CollectionRecordRepository collectionRecordRepository;

    @PostMapping("/login-log")
    public Result<Void> loginLog(@RequestBody Map<String, String> body, HttpServletRequest request) {
        UserLoginLog logEntry = new UserLoginLog();
        logEntry.setEhrNo(body.getOrDefault("ehrNo", ""));
        logEntry.setUserName(body.getOrDefault("userName", ""));
        logEntry.setOrgCode(body.getOrDefault("orgCode", ""));
        logEntry.setIpAddress(getClientIp(request));
        logEntry.setSessionId(request.getSession().getId());
        logEntry.setLoginTime(LocalDateTime.now());
        userLoginLogRepository.save(logEntry);
        return Result.success(null);
    }

    /**
     * 员工统计（人员总览卡片）：admin 全行、manager 本范围组；staff/unknown 返回空
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats(
            @RequestParam(required = false) String orgCode,
            @RequestParam(required = false) String ehrNo) {

        List<String> orgCodeList = orgGroupService.resolvePeopleViewOrgCodes(orgCode, ehrNo);

        Map<String, Object> result = new HashMap<>();
        if (orgCodeList.isEmpty()) {
            result.put("totalUsers", 0L);
            result.put("activeUsers30d", 0L);
            result.put("activeUsers7d", 0L);
            result.put("activeRate30d", 0);
            result.put("activeRate7d", 0);
            return Result.success(result);
        }

        long totalUsers = userOrgRepository.countActiveByOrgCodeIn(orgCodeList);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsers30d = userLoginLogRepository.countDistinctEhrNoByOrgCodeInSince(orgCodeList, thirtyDaysAgo);
        long activeUsers7d = userLoginLogRepository.countDistinctEhrNoByOrgCodeInSince(orgCodeList, sevenDaysAgo);

        result.put("totalUsers", totalUsers);
        result.put("activeUsers30d", activeUsers30d);
        result.put("activeUsers7d", activeUsers7d);
        result.put("activeRate30d", totalUsers > 0 ? Math.round(activeUsers30d * 10000.0 / totalUsers) / 100.0 : 0);
        result.put("activeRate7d", totalUsers > 0 ? Math.round(activeUsers7d * 10000.0 / totalUsers) / 100.0 : 0);
        return Result.success(result);
    }

    /**
     * 员工列表（人员总览）：admin 全行、manager 本范围组；staff/unknown 返回空
     * 每行含：活跃情况(30d/7d)、最后登录时间、催收数量
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(required = false) String orgCode,
            @RequestParam(required = false) String ehrNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        List<String> orgCodeList = orgGroupService.resolvePeopleViewOrgCodes(orgCode, ehrNo);

        if (orgCodeList.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("records", new ArrayList<>());
            empty.put("total", 0);
            empty.put("size", size);
            empty.put("current", page);
            return Result.success(empty);
        }

        List<UserOrg> allUsers = userOrgRepository.findActiveByOrgCodeIn(orgCodeList);

        List<String> ehrNos = new ArrayList<>();
        for (UserOrg u : allUsers) ehrNos.add(u.getEhrNo());

        // 最后登录时间（按 ehrNo）
        List<Object[]> loginRows = ehrNos.isEmpty() ? Collections.emptyList() :
            userLoginLogRepository.findLastLoginByEhrNoIn(ehrNos);
        Map<String, LocalDateTime> lastLoginMap = new HashMap<>();
        if (loginRows != null) {
            for (Object[] row : loginRows) {
                lastLoginMap.put((String) row[0], (LocalDateTime) row[1]);
            }
        }

        // 催收数量：按 operator（=ehrNo）统计，限定本范围组机构（可选时间范围）
        List<String> branchCodes = orgGroupService.resolveBranchCodes(orgCodeList);
        LocalDateTime start = parseDateStart(startDate);
        LocalDateTime end = parseDateEnd(endDate);
        Map<String, Long> collectionCountMap = new HashMap<>();
        if (!ehrNos.isEmpty() && !branchCodes.isEmpty()) {
            List<Object[]> countRows = (start != null && end != null)
                    ? collectionRecordRepository.operatorCollectionCountBetween(branchCodes, ehrNos, start, end)
                    : collectionRecordRepository.operatorCollectionCount(branchCodes, ehrNos);
            if (countRows != null) {
                for (Object[] row : countRows) {
                    String operatorId = (String) row[0];
                    long c = row[1] == null ? 0L : ((Number) row[1]).longValue();
                    collectionCountMap.put(operatorId, c);
                }
            }
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<Map<String, Object>> list = new ArrayList<>();
        for (UserOrg u : allUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("ehrNo", u.getEhrNo());
            item.put("userName", u.getUserName());
            item.put("orgCode", u.getOrgCode());
            item.put("orgName", u.getOrgName());
            LocalDateTime lastLogin = lastLoginMap.getOrDefault(u.getEhrNo(), null);
            item.put("lastLogin", lastLogin);
            item.put("active30d", lastLogin != null && lastLogin.isAfter(thirtyDaysAgo));
            item.put("active7d", lastLogin != null && lastLogin.isAfter(sevenDaysAgo));
            item.put("collectionCount", collectionCountMap.getOrDefault(u.getEhrNo(), 0L));
            list.add(item);
        }

        int total = list.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> paged = fromIndex < total ? list.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", paged);
        result.put("total", total);
        result.put("size", size);
        result.put("current", page);
        return Result.success(result);
    }

    /** 解析起始日期（yyyy-MM-dd），失败或空返回 null */
    private LocalDateTime parseDateStart(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            return java.time.LocalDate.parse(dateStr.trim()).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析结束日期（yyyy-MM-dd，含当日 23:59:59.999），失败或空返回 null */
    private LocalDateTime parseDateEnd(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            return java.time.LocalDate.parse(dateStr.trim()).atTime(java.time.LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
