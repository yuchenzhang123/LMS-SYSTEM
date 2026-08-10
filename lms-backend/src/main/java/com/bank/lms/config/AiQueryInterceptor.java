package com.bank.lms.config;

import com.bank.lms.dto.analysis.AiUserScope;
import com.bank.lms.repository.BranchOrgRepository;
import com.bank.lms.service.OrgGroupService;
import com.bank.lms.service.OrgHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 查询拦截器
 * 从请求参数中提取用户身份 → 预计算数据访问范围 → 注入 AiQueryContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiQueryInterceptor implements HandlerInterceptor {

    private final OrgHierarchyService orgHierarchyService;
    private final OrgGroupService orgGroupService;
    private final BranchOrgRepository branchOrgRepository;

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
            var roleResp = orgGroupService.getRoleByOrgCode(orgCode, ehrNo != null ? ehrNo : "");
            scope.setUserRole(roleResp.getRole());
            scope.setGroupCode(roleResp.getGroupCode());
            scope.setGroupManager(roleResp.isGroupManager());

            // 展开机构号范围
            Set<String> expandedOrgCodes = orgHierarchyService.expandOrgCodes(orgCode);
            scope.setAllowedOrgCodes(new ArrayList<>(expandedOrgCodes));

            // 展开为 branchCode
            List<String> branchCodes = branchOrgRepository.findByOrgCodeIn(new ArrayList<>(expandedOrgCodes))
                    .stream().map(b -> b.getBranchCode()).collect(Collectors.toList());
            // 组内机构号本身也可能是 branchCode
            for (String oc : expandedOrgCodes) {
                if (!branchCodes.contains(oc)) {
                    branchCodes.add(oc);
                }
            }
            scope.setAllowedBranchCodes(branchCodes);

        } catch (Exception e) {
            log.warn("计算AiUserScope失败: {}", e.getMessage());
            // 降级：只允许自己的 orgCode
            scope.setAllowedOrgCodes(new ArrayList<>(List.of(orgCode)));
            scope.setAllowedBranchCodes(new ArrayList<>(List.of(orgCode)));
        }

        AiQueryContext.set(scope);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AiQueryContext.clear();
    }
}
