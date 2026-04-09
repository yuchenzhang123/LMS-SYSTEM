package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.service.GbaseSyncService;
import com.bank.lms.service.OrgHierarchyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Api(tags = "机构层级管理")
public class OrgHierarchyController {

    private final OrgHierarchyService orgHierarchyService;
    private final GbaseSyncService gbaseSyncService;

    @GetMapping("/role")
    @ApiOperation("根据机构号判断角色（manager/staff/unknown）")
    public Result<String> getRole(@RequestParam String orgCode) {
        return Result.success(orgHierarchyService.getRoleByOrgCode(orgCode));
    }

    @GetMapping("/branches")
    @ApiOperation("获取管辖行下所有分支行列表")
    public Result<List<Map<String, String>>> getBranches(@RequestParam String orgCode) {
        return Result.success(orgHierarchyService.getBranchesByOrgCode(orgCode));
    }

    @GetMapping("/jurisdictions")
    @ApiOperation("获取所有管辖行列表")
    public Result<List<Map<String, String>>> getJurisdictions() {
        return Result.success(orgHierarchyService.getAllJurisdictions());
    }

    @GetMapping("/tree")
    @ApiOperation("获取完整机构树（管辖行 + 分支行）")
    public Result<List<Map<String, Object>>> getOrgTree() {
        return Result.success(orgHierarchyService.getOrgTree());
    }

    @PostMapping("/jurisdiction")
    @ApiOperation("新增管辖行")
    public Result<String> addJurisdiction(@RequestBody Map<String, String> body) {
        String orgCode = body.get("orgCode");
        String orgName = body.get("orgName");
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return Result.error("400", "管辖行号不能为空");
        }
        try {
            orgHierarchyService.addJurisdiction(orgCode.trim(), orgName == null ? "" : orgName.trim());
            return Result.success("新增管辖行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @PostMapping("/branch")
    @ApiOperation("新增分支行")
    public Result<String> addBranch(@RequestBody Map<String, String> body) {
        String branchCode = body.get("branchCode");
        String branchName = body.get("branchName");
        String orgCode = body.get("orgCode");
        if (branchCode == null || branchCode.trim().isEmpty()) {
            return Result.error("400", "分支行号不能为空");
        }
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return Result.error("400", "所属管辖行号不能为空");
        }
        try {
            orgHierarchyService.addBranch(branchCode.trim(), branchName == null ? "" : branchName.trim(), orgCode.trim());
            return Result.success("新增分支行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @DeleteMapping("/jurisdiction/{orgCode}")
    @ApiOperation("删除管辖行（同时删除其下所有分支行）")
    public Result<String> deleteJurisdiction(@PathVariable String orgCode) {
        try {
            orgHierarchyService.deleteJurisdiction(orgCode);
            return Result.success("删除管辖行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @DeleteMapping("/branch/{branchCode}")
    @ApiOperation("删除分支行")
    public Result<String> deleteBranch(@PathVariable String branchCode) {
        try {
            orgHierarchyService.deleteBranch(branchCode);
            return Result.success("删除分支行成功");
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        }
    }

    @GetMapping("/gbase-lookup")
    @ApiOperation("从GBase查询机构号对应名称（辅助提示）")
    public Result<Map<String, Object>> gbaseLookup(@RequestParam String orgCode) {
        return Result.success(gbaseSyncService.lookupOrgInGbase(orgCode));
    }
}
