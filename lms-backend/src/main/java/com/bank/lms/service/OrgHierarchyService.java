package com.bank.lms.service;

import com.bank.lms.dto.org.GroupRoleResponse;
import com.bank.lms.dto.org.OrgNodeDTO;
import com.bank.lms.entity.BranchOrg;
import com.bank.lms.entity.JurisdictionOrg;
import com.bank.lms.repository.BranchOrgRepository;
import com.bank.lms.repository.JurisdictionOrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机构层级服务
 * 管理管辖行 - 分支行层级关系，提供角色判断和手动树形维护
 * 角色判断和机构号展开核心逻辑已委托至 OrgGroupService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgHierarchyService {

    private final JurisdictionOrgRepository jurisdictionOrgRepository;
    private final BranchOrgRepository branchOrgRepository;
    private final OrgGroupService orgGroupService;

    /**
     * 根据机构号获取角色（兼容旧接口，返回 GroupRoleResponse）
     */
    public GroupRoleResponse getRoleByOrgCode(String orgCode) {
        return getRoleByOrgCode(orgCode, null);
    }

    /**
     * 根据机构号获取角色（含 ehrNo 以判断组管理人员）
     */
    public GroupRoleResponse getRoleByOrgCode(String orgCode, String ehrNo) {
        return orgGroupService.getRoleByOrgCode(orgCode, ehrNo);
    }

    /**
     * 仅返回角色字符串（兼容旧调用）
     */
    public String getRoleByOrgCodeSimple(String orgCode) {
        return orgGroupService.getRoleByOrgCode(orgCode, null).getRole();
    }

    public List<OrgNodeDTO> getBranchesByOrgCode(String orgCode) {
        List<OrgNodeDTO> result = new ArrayList<>();
        // 管辖行自身也可能作为业务机构出现在贷款数据中
        jurisdictionOrgRepository.findByOrgCode(orgCode).ifPresent(j -> {
            OrgNodeDTO self = OrgNodeDTO.branch(j.getOrgCode(), j.getOrgName() + "（本行）", orgCode);
            self.setType("manager");
            self.setBranchCode(j.getOrgCode());
            self.setBranchName(j.getOrgName() + "（本行）");
            result.add(self);
        });
        branchOrgRepository.findByOrgCode(orgCode).stream()
                .map(b -> OrgNodeDTO.branch(b.getBranchCode(), b.getBranchName(), orgCode))
                .forEach(result::add);
        return result;
    }

    public List<OrgNodeDTO> getAllJurisdictions() {
        return jurisdictionOrgRepository.findAll().stream()
                .map(j -> OrgNodeDTO.jurisdiction(j.getOrgCode(), j.getOrgName()))
                .collect(Collectors.toList());
    }

    /**
     * 获取完整机构树（一次性批量查询子节点，避免 N+1）
     */
    public List<OrgNodeDTO> getOrgTree() {
        List<JurisdictionOrg> jurisdictions = jurisdictionOrgRepository.findAll();
        if (jurisdictions.isEmpty()) return Collections.emptyList();

        List<String> orgCodes = jurisdictions.stream().map(JurisdictionOrg::getOrgCode).collect(Collectors.toList());
        Map<String, List<BranchOrg>> branchesByOrg = branchOrgRepository.findByOrgCodeIn(orgCodes)
                .stream().collect(Collectors.groupingBy(BranchOrg::getOrgCode));

        return jurisdictions.stream().map(j -> {
            OrgNodeDTO node = OrgNodeDTO.jurisdiction(j.getOrgCode(), j.getOrgName());
            List<BranchOrg> branches = branchesByOrg.getOrDefault(j.getOrgCode(), Collections.emptyList());
            node.setChildren(branches.stream()
                    .map(b -> OrgNodeDTO.branch(b.getBranchCode(), b.getBranchName(), j.getOrgCode()))
                    .collect(Collectors.toList()));
            return node;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addJurisdiction(String orgCode, String orgName) {
        if (jurisdictionOrgRepository.existsByOrgCode(orgCode))
            throw new IllegalArgumentException("该机构号已是管辖机构，无法重复添加");
        if (branchOrgRepository.existsByBranchCode(orgCode))
            throw new IllegalArgumentException("该机构号已作为业务机构存在，同一机构不能同时是管辖机构和业务机构");
        JurisdictionOrg org = new JurisdictionOrg();
        org.setOrgCode(orgCode);
        org.setOrgName(orgName);
        jurisdictionOrgRepository.save(org);
        log.info("新增管辖行：{} {}", orgCode, orgName);
    }

    @Transactional
    public void updateJurisdiction(String orgCode, String orgName) {
        JurisdictionOrg org = jurisdictionOrgRepository.findByOrgCode(orgCode)
                .orElseThrow(() -> new IllegalArgumentException("管辖机构不存在：" + orgCode));
        org.setOrgName(orgName);
        jurisdictionOrgRepository.save(org);
        log.info("更新管辖行：{} -> {}", orgCode, orgName);
    }

    @Transactional
    public void addBranch(String branchCode, String branchName, String orgCode) {
        if (!jurisdictionOrgRepository.existsByOrgCode(orgCode))
            throw new IllegalArgumentException("管辖机构不存在：" + orgCode);
        if (jurisdictionOrgRepository.existsByOrgCode(branchCode))
            throw new IllegalArgumentException("该机构号已是管辖机构，同一机构不能同时是管辖机构和业务机构");
        if (branchOrgRepository.existsByBranchCodeAndOrgCode(branchCode, orgCode))
            throw new IllegalArgumentException("该业务机构已在此管辖机构下，无法重复添加");
        BranchOrg branch = new BranchOrg();
        branch.setBranchCode(branchCode);
        branch.setBranchName(branchName);
        branch.setOrgCode(orgCode);
        branchOrgRepository.save(branch);
        log.info("新增分支行：{} {} -> 管辖行 {}", branchCode, branchName, orgCode);
    }

    @Transactional
    public void updateBranch(String branchCode, String orgCode, String branchName) {
        List<BranchOrg> branches = branchOrgRepository.findByBranchCode(branchCode);
        if (branches.isEmpty()) throw new IllegalArgumentException("分支行不存在：" + branchCode);
        for (BranchOrg b : branches) {
            if (b.getOrgCode().equals(orgCode)) {
                b.setBranchName(branchName);
                branchOrgRepository.save(b);
                log.info("更新分支行：{}/{} -> {}", branchCode, orgCode, branchName);
                return;
            }
        }
        throw new IllegalArgumentException("分支行 " + branchCode + " 不在管辖行 " + orgCode + " 下");
    }

    @Transactional
    public void deleteJurisdiction(String orgCode) {
        if (!jurisdictionOrgRepository.existsByOrgCode(orgCode))
            throw new IllegalArgumentException("管辖行不存在：" + orgCode);
        List<BranchOrg> branches = branchOrgRepository.findByOrgCode(orgCode);
        branchOrgRepository.deleteAll(branches);
        jurisdictionOrgRepository.findByOrgCode(orgCode).ifPresent(jurisdictionOrgRepository::delete);
        log.info("删除管辖行 {} 及其 {} 个分支行", orgCode, branches.size());
    }

    /** 从指定管辖行下移除分支行（不再全局删除） */
    @Transactional
    public void deleteBranchFromJurisdiction(String branchCode, String orgCode) {
        branchOrgRepository.deleteByBranchCodeAndOrgCode(branchCode, orgCode);
        log.info("从管辖行 {} 下删除分支行：{}", orgCode, branchCode);
    }
}
