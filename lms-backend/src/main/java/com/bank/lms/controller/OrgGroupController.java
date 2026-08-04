package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.dto.org.GroupRoleResponse;
import com.bank.lms.service.OrgGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 范围组管理控制器
 * 提供范围组、组成员、管理人员的完整 CRUD 及查询接口
 */
@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
public class OrgGroupController {

    private final OrgGroupService orgGroupService;

    // ==================== 角色查询（改造） ====================

    /**
     * 根据机构号获取角色（新格式，含范围组信息）
     * ehrNo 用于判断是否为组管理人员
     */
    @GetMapping("/role")
    public Result<GroupRoleResponse> getRole(@RequestParam String orgCode,
                                              @RequestParam(required = false) String ehrNo) {
        return Result.success(orgGroupService.getRoleByOrgCode(orgCode, ehrNo));
    }

    // ==================== 范围组 CRUD ====================

    /**
     * 获取全部范围组树（含成员和管理人员）
     */
    @GetMapping("/group/tree")
    public Result<List<Map<String, Object>>> getGroupTree() {
        return Result.success(orgGroupService.getGroupTree());
    }

    /**
     * 新建范围组
     */
    @PostMapping("/group")
    public Result<String> createGroup(@RequestBody Map<String, String> body) {
        String groupName = body.get("groupName");
        if (groupName == null || groupName.trim().isEmpty()) {
            return Result.error("400", "范围组名称不能为空");
        }
        try {
            orgGroupService.createGroup(groupName.trim());
            return Result.success("新建范围组成功");
        } catch (IllegalArgumentException e) {
            log.warn("新建范围组失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 编辑范围组名称
     */
    @PutMapping("/group/{groupCode}")
    public Result<String> updateGroup(@PathVariable String groupCode,
                                       @RequestBody Map<String, String> body) {
        String groupName = body.get("groupName");
        if (groupName == null || groupName.trim().isEmpty()) {
            return Result.error("400", "范围组名称不能为空");
        }
        try {
            orgGroupService.updateGroup(groupCode, groupName.trim());
            return Result.success("更新范围组成功");
        } catch (IllegalArgumentException e) {
            log.warn("更新范围组失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 删除范围组（级联删除成员和管理人员）
     */
    @DeleteMapping("/group/{groupCode}")
    public Result<String> deleteGroup(@PathVariable String groupCode) {
        try {
            orgGroupService.deleteGroup(groupCode);
            return Result.success("删除范围组成功");
        } catch (IllegalArgumentException e) {
            log.warn("删除范围组失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    // ==================== 组成员 CRUD ====================

    /**
     * 添加机构至范围组
     */
    @PostMapping("/group/{groupCode}/member")
    public Result<String> addMember(@PathVariable String groupCode,
                                     @RequestBody Map<String, String> body) {
        String orgCode = body.get("orgCode");
        String orgName = body.get("orgName");
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return Result.error("400", "机构号不能为空");
        }
        try {
            orgGroupService.addMember(groupCode, orgCode.trim(),
                    orgName != null ? orgName.trim() : "");
            return Result.success("添加机构成功");
        } catch (IllegalArgumentException e) {
            log.warn("添加机构失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 从范围组移出机构
     */
    @DeleteMapping("/group/{groupCode}/member/{orgCode}")
    public Result<String> removeMember(@PathVariable String groupCode,
                                        @PathVariable String orgCode) {
        try {
            orgGroupService.removeMember(groupCode, orgCode);
            return Result.success("移出机构成功");
        } catch (IllegalArgumentException e) {
            log.warn("移出机构失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 设为管辖机构
     */
    @PutMapping("/group/{groupCode}/member/{orgCode}/manager")
    public Result<String> setManagerOrg(@PathVariable String groupCode,
                                         @PathVariable String orgCode) {
        try {
            orgGroupService.setManagerOrg(groupCode, orgCode);
            return Result.success("设为管辖机构成功");
        } catch (IllegalArgumentException e) {
            log.warn("设为管辖机构失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 取消管辖机构
     */
    @DeleteMapping("/group/{groupCode}/member/{orgCode}/manager")
    public Result<String> unsetManagerOrg(@PathVariable String groupCode,
                                           @PathVariable String orgCode) {
        try {
            orgGroupService.unsetManagerOrg(groupCode, orgCode);
            return Result.success("取消管辖机构成功");
        } catch (IllegalArgumentException e) {
            log.warn("取消管辖机构失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    // ==================== 管理人员 CRUD ====================

    /**
     * 添加管理人员
     */
    @PostMapping("/group/{groupCode}/manager")
    public Result<String> addManager(@PathVariable String groupCode,
                                      @RequestBody Map<String, String> body) {
        String ehrNo = body.get("ehrNo");
        String userName = body.get("userName");
        if (ehrNo == null || ehrNo.trim().isEmpty()) {
            return Result.error("400", "EHR号不能为空");
        }
        try {
            orgGroupService.addManager(groupCode, ehrNo.trim(),
                    userName != null ? userName.trim() : "");
            return Result.success("添加管理人员成功");
        } catch (IllegalArgumentException e) {
            log.warn("添加管理人员失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    /**
     * 移除管理人员
     */
    @DeleteMapping("/group/{groupCode}/manager/{ehrNo}")
    public Result<String> removeManager(@PathVariable String groupCode,
                                         @PathVariable String ehrNo) {
        try {
            orgGroupService.removeManager(groupCode, ehrNo);
            return Result.success("移除管理人员成功");
        } catch (IllegalArgumentException e) {
            log.warn("移除管理人员失败: {}", e.getMessage());
            return Result.error("400", e.getMessage());
        }
    }

    // ==================== 查询辅助 ====================

    /**
     * 根据 ehrNo 查询人员姓名和所属机构
     */
    @GetMapping("/user-lookup")
    public Result<Map<String, Object>> userLookup(@RequestParam String ehrNo) {
        return Result.success(orgGroupService.lookupUser(ehrNo));
    }

    /**
     * 根据机构号查询 GBase 中的机构名称
     */
    @GetMapping("/gbase-lookup")
    public Result<Map<String, Object>> gbaseLookup(@RequestParam String orgCode) {
        return Result.success(orgGroupService.lookupOrgInGbase(orgCode));
    }
}
