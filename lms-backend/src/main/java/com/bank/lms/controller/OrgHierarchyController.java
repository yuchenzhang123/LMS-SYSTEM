package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.dto.org.OrgNodeDTO;
import com.bank.lms.service.GbaseSyncService;
import com.bank.lms.service.OrgHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
public class OrgHierarchyController {

    private final OrgHierarchyService orgHierarchyService;
    private final GbaseSyncService gbaseSyncService;

    @GetMapping("/role")
    public Result<String> getRole(@RequestParam String orgCode) {
        return Result.success(orgHierarchyService.getRoleByOrgCode(orgCode));
    }

    // ---- 查询 ----

    @GetMapping("/branches")
    public Result<List<OrgNodeDTO>> getBranches(@RequestParam String orgCode) {
        return Result.success(orgHierarchyService.getBranchesByOrgCode(orgCode));
    }

    @GetMapping("/jurisdictions")
    public Result<List<OrgNodeDTO>> getJurisdictions() {
        return Result.success(orgHierarchyService.getAllJurisdictions());
    }

    @GetMapping("/tree")
    public Result<List<OrgNodeDTO>> getOrgTree() {
        return Result.success(orgHierarchyService.getOrgTree());
    }

    // ---- 管辖行 CRUD ----

    @PostMapping("/jurisdiction")
    public Result<String> addJurisdiction(@RequestBody Map<String, String> body) {
        String orgCode = body.get("orgCode");
        String orgName = body.get("orgName");
        if (orgCode == null || orgCode.trim().isEmpty())
            return Result.error("400", "管辖行号不能为空");
        try {
            orgHierarchyService.addJurisdiction(orgCode.trim(), orDefault(orgName, ""));
            return Result.success("新增管辖行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @PutMapping("/jurisdiction/{orgCode}")
    public Result<String> updateJurisdiction(@PathVariable String orgCode, @RequestBody Map<String, String> body) {
        String orgName = body.get("orgName");
        if (orgName == null || orgName.trim().isEmpty())
            return Result.error("400", "管辖行名称不能为空");
        try {
            orgHierarchyService.updateJurisdiction(orgCode.trim(), orgName.trim());
            return Result.success("更新管辖行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @DeleteMapping("/jurisdiction/{orgCode}")
    public Result<String> deleteJurisdiction(@PathVariable String orgCode) {
        try {
            orgHierarchyService.deleteJurisdiction(orgCode);
            return Result.success("删除管辖行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    // ---- 分支行 CRUD ----

    @PostMapping("/branch")
    public Result<String> addBranch(@RequestBody Map<String, String> body) {
        String branchCode = body.get("branchCode");
        String branchName = body.get("branchName");
        String orgCode = body.get("orgCode");
        if (branchCode == null || branchCode.trim().isEmpty())
            return Result.error("400", "分支行号不能为空");
        if (orgCode == null || orgCode.trim().isEmpty())
            return Result.error("400", "所属管辖行号不能为空");
        try {
            orgHierarchyService.addBranch(branchCode.trim(), orDefault(branchName, ""), orgCode.trim());
            return Result.success("新增分支行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @PutMapping("/branch/{branchCode}/jurisdiction/{orgCode}")
    public Result<String> updateBranch(@PathVariable String branchCode, @PathVariable String orgCode,
                                       @RequestBody Map<String, String> body) {
        String branchName = body.get("branchName");
        if (branchName == null || branchName.trim().isEmpty())
            return Result.error("400", "分支行名称不能为空");
        try {
            orgHierarchyService.updateBranch(branchCode.trim(), orgCode.trim(), branchName.trim());
            return Result.success("更新分支行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @DeleteMapping("/branch/{branchCode}/jurisdiction/{orgCode}")
    public Result<String> deleteBranch(@PathVariable String branchCode, @PathVariable String orgCode) {
        try {
            orgHierarchyService.deleteBranchFromJurisdiction(branchCode.trim(), orgCode.trim());
            return Result.success("删除分支行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    // ---- 辅助 ----

    @GetMapping("/gbase-lookup")
    public Result<Map<String, Object>> gbaseLookup(@RequestParam String orgCode) {
        return Result.success(gbaseSyncService.lookupOrgInGbase(orgCode));
    }

    private static String orDefault(String val, String def) {
        return val == null || val.trim().isEmpty() ? def : val.trim();
    }
}
