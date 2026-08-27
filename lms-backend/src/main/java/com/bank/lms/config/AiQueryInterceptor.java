package com.bank.lms.config;

import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.dto.org.GroupRoleResponse;
import com.bank.lms.service.OrgGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 查询拦截器
 * 从请求参数中提取用户身份 → 预计算数据访问范围 → 注入 AiQueryContext
 * 数据范围按范围组维度解析，区分 admin/manager/staff
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiQueryInterceptor implements HandlerInterceptor {

    private final OrgGroupService orgGroupService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String orgCode = request.getParameter("orgCode");
        String ehrNo = request.getParameter("ehrNo");

        if (orgCode == null || orgCode.isEmpty()) {
            log.warn("AI请求缺少 orgCode 参数: {}", request.getRequestURI());
            return true; // 放行，由 Controller 层处理
        }

        AiUserScope scope = new AiUserScope();
        scope.setOrgCode(orgCode);
        scope.setEhrNo(ehrNo != null ? ehrNo : "");

        // 计算用户角色和数据范围
        try {
            GroupRoleResponse roleResp = orgGroupService.getRoleByOrgCode(orgCode, ehrNo != null ? ehrNo : "");
            scope.setUserRole(roleResp.getRole());
            scope.setGroupCode(roleResp.getGroupCode());
            scope.setGroupManager(roleResp.isGroupManager());

            // 按角色解析可访问机构范围（范围组维度，区分 admin/manager/staff）
            List<String> allowedOrgCodes = orgGroupService.resolveAllowedOrgCodes(orgCode, ehrNo != null ? ehrNo : "");
            List<String> allowedBranchCodes = orgGroupService.resolveBranchCodes(allowedOrgCodes);
            scope.setAllowedOrgCodes(allowedOrgCodes);
            scope.setAllowedBranchCodes(allowedBranchCodes);

        } catch (Exception e) {
            log.warn("计算AiUserScope失败: {}", e.getMessage());
            // 降级：只允许自己的 orgCode
            scope.setAllowedOrgCodes(new ArrayList<>(Collections.singletonList(orgCode)));
            scope.setAllowedBranchCodes(new ArrayList<>(Collections.singletonList(orgCode)));
        }

        AiQueryContext.set(scope);
        log.debug("AI请求权限范围: orgCode={}, ehrNo={}, role={}, groupCode={}, isGroupManager={}, allowedOrgCodes={}, allowedBranchCodes={}",
            orgCode, ehrNo, scope.getUserRole(), scope.getGroupCode(), scope.isGroupManager(),
            scope.getAllowedOrgCodes(), scope.getAllowedBranchCodes());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AiQueryContext.clear();
    }
}
