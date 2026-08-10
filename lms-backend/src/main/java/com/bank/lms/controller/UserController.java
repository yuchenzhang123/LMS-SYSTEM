package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.entity.UserLoginLog;
import com.bank.lms.entity.UserOrg;
import com.bank.lms.repository.UserLoginLogRepository;
import com.bank.lms.repository.UserOrgRepository;
import com.bank.lms.service.OrgHierarchyService;
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
    private final OrgHierarchyService orgHierarchyService;

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

    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats(
            @RequestParam(required = false) String orgCode) {

        Set<String> expandedOrgCodes = orgHierarchyService.expandOrgCodes(orgCode != null ? orgCode : "");
        List<String> orgCodeList = new ArrayList<>(expandedOrgCodes);

        long totalUsers = userOrgRepository.countActiveByOrgCodeIn(orgCodeList);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsers30d = userLoginLogRepository.countDistinctEhrNoByOrgCodeInSince(orgCodeList, thirtyDaysAgo);
        long activeUsers7d = userLoginLogRepository.countDistinctEhrNoByOrgCodeInSince(orgCodeList, sevenDaysAgo);

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("activeUsers30d", activeUsers30d);
        result.put("activeUsers7d", activeUsers7d);
        result.put("activeRate30d", totalUsers > 0 ? Math.round(activeUsers30d * 10000.0 / totalUsers) / 100.0 : 0);
        result.put("activeRate7d", totalUsers > 0 ? Math.round(activeUsers7d * 10000.0 / totalUsers) / 100.0 : 0);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(required = false) String orgCode,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Set<String> expandedOrgCodes = orgHierarchyService.expandOrgCodes(orgCode != null ? orgCode : "");
        List<String> orgCodeList = new ArrayList<>(expandedOrgCodes);

        List<UserOrg> allUsers = userOrgRepository.findActiveByOrgCodeIn(orgCodeList);

        List<String> ehrNos = new ArrayList<>();
        for (UserOrg u : allUsers) ehrNos.add(u.getEhrNo());
        List<Object[]> loginRows = ehrNos.isEmpty() ? Collections.emptyList() :
            userLoginLogRepository.findLastLoginByEhrNoIn(ehrNos);
        Map<String, LocalDateTime> lastLoginMap = new HashMap<>();
        if (loginRows != null) {
            for (Object[] row : loginRows) {
                lastLoginMap.put((String) row[0], (LocalDateTime) row[1]);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (UserOrg u : allUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("ehrNo", u.getEhrNo());
            item.put("userName", u.getUserName());
            item.put("orgCode", u.getOrgCode());
            item.put("orgName", u.getOrgName());
            item.put("lastLogin", lastLoginMap.getOrDefault(u.getEhrNo(), null));
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
