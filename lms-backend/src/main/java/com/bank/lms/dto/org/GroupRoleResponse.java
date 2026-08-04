package com.bank.lms.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * /org/role 接口的新返回格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRoleResponse {

    private String role;                       // "admin" | "manager" | "staff"
    private String groupCode;                  // 所属范围组编号，null=不在组内
    private String groupName;                  // 范围组名称
    private List<String> groupOrgCodes;        // 组内所有机构号
    private boolean isGroupManager;            // 是否组管理人员（可绕过自身机构限制）

    public static GroupRoleResponse simple(String role) {
        return GroupRoleResponse.builder()
                .role(role)
                .groupOrgCodes(Collections.emptyList())
                .build();
    }
}
