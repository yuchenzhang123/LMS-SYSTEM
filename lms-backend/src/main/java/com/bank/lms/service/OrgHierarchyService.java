package com.bank.lms.service;

import com.bank.lms.entity.BranchOrg;
import com.bank.lms.entity.JurisdictionOrg;
import com.bank.lms.repository.BranchOrgRepository;
import com.bank.lms.repository.JurisdictionOrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 机构层级服务
 * 管理管辖行 - 分支行层级关系，提供角色判断和手动树形维护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgHierarchyService {

    private final JurisdictionOrgRepository jurisdictionOrgRepository;
    private final BranchOrgRepository branchOrgRepository;

    @Value("${org.admin.codes:}")
    private String adminCodesConfig;

    private Set<String> adminCodes;

    @PostConstruct
    public void init() {
        if (adminCodesConfig == null || adminCodesConfig.isBlank()) {
            adminCodes = Collections.emptySet();
        } else {
            adminCodes = Arrays.stream(adminCodesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }
        log.info("管理员机构号配置：{}", adminCodes);
    }

    /**
     * 根据机构号判断角色
     * @return "admin" 系统管理员 / "manager" 管辖行管理员 / "staff" 分支行业务员 / "unknown"
     */
    public String getRoleByOrgCode(String orgCode) {
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return "unknown";
        }
        if (adminCodes.contains(orgCode)) {
            return "admin";
        }
        if (jurisdictionOrgRepository.existsByOrgCode(orgCode)) {
            return "manager";
        }
        if (branchOrgRepository.existsByBranchCode(orgCode)) {
            return "staff";
        }
        return "unknown";
    }

    /**
     * 获取管辖行下所有分支行
     */
    public List<Map<String, String>> getBranchesByOrgCode(String orgCode) {
        return branchOrgRepository.findByOrgCode(orgCode).stream()
                .map(b -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("branchCode", b.getBranchCode());
                    m.put("branchName", b.getBranchName());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有管辖行列表
     */
    public List<Map<String, String>> getAllJurisdictions() {
        return jurisdictionOrgRepository.findAll().stream()
                .map(j -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("orgCode", j.getOrgCode());
                    m.put("orgName", j.getOrgName());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取完整机构树（管辖行 + 各自的分支行列表）
     */
    public List<Map<String, Object>> getOrgTree() {
        List<JurisdictionOrg> jurisdictions = jurisdictionOrgRepository.findAll();
        List<Map<String, Object>> tree = new ArrayList<>();
        for (JurisdictionOrg j : jurisdictions) {
            Map<String, Object> node = new HashMap<>();
            node.put("orgCode", j.getOrgCode());
            node.put("orgName", j.getOrgName());
            node.put("type", "manager");
            List<Map<String, Object>> children = branchOrgRepository.findByOrgCode(j.getOrgCode()).stream()
                    .map(b -> {
                        Map<String, Object> bn = new HashMap<>();
                        bn.put("branchCode", b.getBranchCode());
                        bn.put("branchName", b.getBranchName());
                        bn.put("orgCode", b.getOrgCode());
                        bn.put("type", "staff");
                        return bn;
                    })
                    .collect(Collectors.toList());
            node.put("children", children);
            tree.add(node);
        }
        return tree;
    }

    /**
     * 新增管辖行
     */
    @Transactional
    public void addJurisdiction(String orgCode, String orgName) {
        if (jurisdictionOrgRepository.existsByOrgCode(orgCode)) {
            throw new IllegalArgumentException("管辖行号已存在：" + orgCode);
        }
        JurisdictionOrg org = new JurisdictionOrg();
        org.setOrgCode(orgCode);
        org.setOrgName(orgName);
        jurisdictionOrgRepository.save(org);
        log.info("新增管辖行：{} {}", orgCode, orgName);
    }

    /**
     * 新增分支行（挂在指定管辖行下）
     */
    @Transactional
    public void addBranch(String branchCode, String branchName, String orgCode) {
        if (!jurisdictionOrgRepository.existsByOrgCode(orgCode)) {
            throw new IllegalArgumentException("管辖行不存在：" + orgCode);
        }
        if (branchOrgRepository.existsByBranchCode(branchCode)) {
            throw new IllegalArgumentException("分支行号已存在：" + branchCode);
        }
        BranchOrg branch = new BranchOrg();
        branch.setBranchCode(branchCode);
        branch.setBranchName(branchName);
        branch.setOrgCode(orgCode);
        branchOrgRepository.save(branch);
        log.info("新增分支行：{} {} -> 管辖行 {}", branchCode, branchName, orgCode);
    }

    /**
     * 删除管辖行（同时删除其下所有分支行）
     */
    @Transactional
    public void deleteJurisdiction(String orgCode) {
        if (!jurisdictionOrgRepository.existsByOrgCode(orgCode)) {
            throw new IllegalArgumentException("管辖行不存在：" + orgCode);
        }
        List<BranchOrg> branches = branchOrgRepository.findByOrgCode(orgCode);
        branchOrgRepository.deleteAll(branches);
        jurisdictionOrgRepository.findByOrgCode(orgCode).ifPresent(jurisdictionOrgRepository::delete);
        log.info("删除管辖行 {} 及其 {} 个分支行", orgCode, branches.size());
    }

    /**
     * 删除分支行
     */
    @Transactional
    public void deleteBranch(String branchCode) {
        branchOrgRepository.findByBranchCode(branchCode)
                .orElseThrow(() -> new IllegalArgumentException("分支行不存在：" + branchCode));
        branchOrgRepository.findByBranchCode(branchCode).ifPresent(branchOrgRepository::delete);
        log.info("删除分支行：{}", branchCode);
    }
}
