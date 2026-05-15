package com.bank.lms.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgNodeDTO {
    private String orgCode;
    private String orgName;
    private String branchCode;
    private String branchName;
    private String type; // "manager" | "staff"
    private String parentOrgCode; // 仅 branch 类型

    @Builder.Default
    private List<OrgNodeDTO> children = new ArrayList<>();

    // Flat constructors for list responses
    public static OrgNodeDTO jurisdiction(String orgCode, String orgName) {
        return OrgNodeDTO.builder().type("manager").orgCode(orgCode).orgName(orgName).build();
    }

    public static OrgNodeDTO branch(String branchCode, String branchName, String parentOrgCode) {
        return OrgNodeDTO.builder().type("staff").branchCode(branchCode).branchName(branchName).parentOrgCode(parentOrgCode).build();
    }
}
