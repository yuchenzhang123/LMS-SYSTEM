package com.bank.lms.dto.analysis;

import lombok.Data;
import java.util.List;

/**
 * AI 查询用户范围上下文
 */
@Data
public class AiUserScope {
    private String ehrNo;
    private String orgCode;
    private String userRole;            // admin/manager/staff
    private String groupCode;
    private boolean isGroupManager;
    private List<String> allowedOrgCodes;
    private List<String> allowedBranchCodes;
}
