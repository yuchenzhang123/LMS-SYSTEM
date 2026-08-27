package com.bank.lms.service;

import com.bank.lms.dto.org.GroupRoleResponse;
import com.bank.lms.entity.BranchOrg;
import com.bank.lms.entity.JurisdictionOrg;
import com.bank.lms.entity.OrgGroup;
import com.bank.lms.entity.OrgGroupManager;
import com.bank.lms.entity.OrgGroupMember;
import com.bank.lms.entity.UserOrg;
import com.bank.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 范围组服务
 * 管理范围组及其成员（机构号）和管理人员，提供角色判断和机构号展开
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgGroupService {

    private final OrgGroupRepository groupRepository;
    private final OrgGroupMemberRepository memberRepository;
    private final OrgGroupManagerRepository managerRepository;
    private final JurisdictionOrgRepository jurisdictionOrgRepository;
    private final BranchOrgRepository branchOrgRepository;
    private final UserOrgRepository userOrgRepository;

    @Qualifier("gbaseJdbcTemplate")
    private final JdbcTemplate gbaseJdbcTemplate;

    @Value("${org.admin.codes:}")
    private String adminCodesConfig;

    private Set<String> adminCodes = Collections.emptySet();

    @javax.annotation.PostConstruct
    public void init() {
        if (adminCodesConfig != null && !adminCodesConfig.trim().isEmpty()) {
            Set<String> codes = new HashSet<>();
            for (String code : adminCodesConfig.split(",")) {
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) codes.add(trimmed);
            }
            adminCodes = Collections.unmodifiableSet(codes);
        }
        log.info("管理员机构号配置：{}", adminCodes);
    }

    // ==================== 角色判断 ====================

    /**
     * 根据机构号判断用户角色，返回 GroupRoleResponse
     */
    public GroupRoleResponse getRoleByOrgCode(String orgCode, String ehrNo) {
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return GroupRoleResponse.simple("unknown");
        }
        String code = orgCode.trim();
        log.debug("角色判断开始: orgCode={}, ehrNo={}", orgCode, ehrNo);

        // 1. 管理员优先
        if (adminCodes.contains(code)) {
            log.debug("角色判断结果: orgCode={} -> admin", code);
            return GroupRoleResponse.simple("admin");
        }

        // 2. 查范围组成员
        List<OrgGroupMember> members = memberRepository.findByOrgCode(code);
        if (!members.isEmpty()) {
            // 取第一个匹配的范围组（一个机构号通常只属于一个组）
            OrgGroupMember myMember = members.get(0);
            String groupCode = myMember.getGroupCode();
            OrgGroup group = groupRepository.findByGroupCode(groupCode).orElse(null);
            String groupName = group != null ? group.getGroupName() : "";

            // 组内所有机构号
            List<String> allOrgCodes = memberRepository.findByGroupCode(groupCode)
                    .stream().map(OrgGroupMember::getOrgCode).collect(Collectors.toList());

            // 判断是否是管辖机构
            boolean isManager = Boolean.TRUE.equals(myMember.getIsManagerOrg());

            // 判断是否是组管理人员（通过 ehrNo 查）
            boolean isGroupManager = false;
            if (ehrNo != null && !ehrNo.trim().isEmpty()) {
                isGroupManager = managerRepository.existsByGroupCodeAndEhrNo(groupCode, ehrNo.trim());
            }

            if (isManager || isGroupManager) {
                log.debug("角色判断结果: orgCode={} -> manager, groupCode={}, isManagerOrg={}, isGroupManager={}",
                    code, groupCode, isManager, isGroupManager);
                return GroupRoleResponse.builder()
                        .role("manager")
                        .groupCode(groupCode)
                        .groupName(groupName)
                        .groupOrgCodes(allOrgCodes)
                        .isGroupManager(isGroupManager)
                        .build();
            }

            // 普通组成员 → staff
            log.debug("角色判断结果: orgCode={} -> staff, groupCode={}", code, groupCode);
            return GroupRoleResponse.builder()
                    .role("staff")
                    .groupCode(groupCode)
                    .groupName(groupName)
                    .groupOrgCodes(allOrgCodes)
                    .isGroupManager(false)
                    .build();
        }

        // 3. 不在范围组内，回退到旧逻辑
        if (jurisdictionOrgRepository.existsByOrgCode(code)) {
            log.debug("角色判断结果: orgCode={} -> manager (旧逻辑·管辖机构)", code);
            return GroupRoleResponse.builder()
                    .role("manager")
                    .groupOrgCodes(Collections.singletonList(code))
                    .build();
        }
        if (branchOrgRepository.existsByBranchCode(code)) {
            log.debug("角色判断结果: orgCode={} -> staff (旧逻辑·分支行)", code);
            return GroupRoleResponse.builder()
                    .role("staff")
                    .groupOrgCodes(Collections.singletonList(code))
                    .build();
        }

        log.debug("角色判断结果: orgCode={} -> unknown", code);
        return GroupRoleResponse.simple("unknown");
    }

    /**
     * 兼容旧接口：只返回角色字符串
     */
    public String getRoleByOrgCodeSimple(String orgCode, String ehrNo) {
        return getRoleByOrgCode(orgCode, ehrNo).getRole();
    }

    // ==================== 权限范围解析（范围组维度） ====================

    /**
     * 全行所有机构号（admin 用）：所有管辖行 + 所有分支行（去重、保持顺序）
     */
    public List<String> getAllOrgCodes() {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (JurisdictionOrg j : jurisdictionOrgRepository.findAll()) {
            if (j.getOrgCode() != null && !j.getOrgCode().trim().isEmpty()) {
                codes.add(j.getOrgCode().trim());
            }
        }
        for (BranchOrg b : branchOrgRepository.findAll()) {
            if (b.getBranchCode() != null && !b.getBranchCode().trim().isEmpty()) {
                codes.add(b.getBranchCode().trim());
            }
        }
        return new ArrayList<>(codes);
    }

    /**
     * 按角色解析可访问的机构号范围（机构排名/账户统计/AI 问答用）
     * admin → 全行；manager → 本范围组；staff → 本机构；unknown/空 → 空（无权限）
     */
    public List<String> resolveAllowedOrgCodes(String orgCode, String ehrNo) {
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        GroupRoleResponse roleResp = getRoleByOrgCode(orgCode, ehrNo);
        String role = roleResp.getRole();
        if ("admin".equals(role)) {
            return getAllOrgCodes();
        }
        if ("manager".equals(role)) {
            List<String> codes = roleResp.getGroupOrgCodes();
            return codes != null ? new ArrayList<>(codes) : new ArrayList<>();
        }
        if ("staff".equals(role)) {
            return Collections.singletonList(orgCode.trim());
        }
        return Collections.emptyList();
    }

    /**
     * 按角色解析"人员总览"的机构范围：仅 admin（全行）和 manager（本组）可见，staff 返回空
     */
    public List<String> resolvePeopleViewOrgCodes(String orgCode, String ehrNo) {
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        GroupRoleResponse roleResp = getRoleByOrgCode(orgCode, ehrNo);
        String role = roleResp.getRole();
        if ("admin".equals(role)) {
            return getAllOrgCodes();
        }
        if ("manager".equals(role)) {
            List<String> codes = roleResp.getGroupOrgCodes();
            return codes != null ? new ArrayList<>(codes) : new ArrayList<>();
        }
        // staff 不展示整组人员及他人工作量；unknown 无权限
        return Collections.emptyList();
    }

    /**
     * 机构号 → 数据查询用的 branchCode 列表（含机构号自身，去重）
     */
    public List<String> resolveBranchCodes(List<String> orgCodes) {
        if (orgCodes == null || orgCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        List<BranchOrg> branches = branchOrgRepository.findByOrgCodeIn(new ArrayList<>(orgCodes));
        for (BranchOrg b : branches) {
            if (b.getBranchCode() != null && !b.getBranchCode().trim().isEmpty()) {
                codes.add(b.getBranchCode().trim());
            }
        }
        // 组内机构号本身也可能是贷款数据中的 branchCode（管辖机构自身做业务）
        for (String oc : orgCodes) {
            if (oc != null && !oc.trim().isEmpty()) {
                codes.add(oc.trim());
            }
        }
        return new ArrayList<>(codes);
    }

    // ==================== 范围组 CRUD ====================

    public List<Map<String, Object>> getGroupTree() {
        List<OrgGroup> groups = groupRepository.findAll();
        return groups.stream().map(this::groupToMap).collect(Collectors.toList());
    }

    @Transactional
    public void createGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("范围组名称不能为空");
        }
        OrgGroup group = new OrgGroup();
        String groupCode = "GRP_" + System.currentTimeMillis();
        group.setGroupCode(groupCode);
        group.setGroupName(groupName.trim());
        groupRepository.save(group);
        log.info("新建范围组：{} {}", groupCode, groupName);
    }

    @Transactional
    public void updateGroup(String groupCode, String groupName) {
        OrgGroup group = groupRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new IllegalArgumentException("范围组不存在：" + groupCode));
        group.setGroupName(groupName.trim());
        groupRepository.save(group);
        log.info("更新范围组：{} -> {}", groupCode, groupName);
    }

    @Transactional
    public void deleteGroup(String groupCode) {
        if (!groupRepository.existsByGroupCode(groupCode)) {
            throw new IllegalArgumentException("范围组不存在：" + groupCode);
        }
        managerRepository.deleteByGroupCode(groupCode);
        memberRepository.deleteByGroupCode(groupCode);
        groupRepository.deleteByGroupCode(groupCode);
        log.info("删除范围组：{} 及其成员和管理人员", groupCode);
    }

    // ==================== 组成员 CRUD ====================

    @Transactional
    public void addMember(String groupCode, String orgCode, String orgName) {
        if (!groupRepository.existsByGroupCode(groupCode)) {
            throw new IllegalArgumentException("范围组不存在：" + groupCode);
        }
        if (memberRepository.existsByGroupCodeAndOrgCode(groupCode, orgCode)) {
            throw new IllegalArgumentException("该机构已在此范围组内");
        }
        OrgGroupMember member = new OrgGroupMember();
        member.setGroupCode(groupCode);
        member.setOrgCode(orgCode.trim());
        member.setOrgName(orgName != null ? orgName.trim() : "");
        member.setIsManagerOrg(false);
        memberRepository.save(member);
        log.info("添加机构至范围组：{}/{} {}", groupCode, orgCode, orgName);
    }

    @Transactional
    public void removeMember(String groupCode, String orgCode) {
        if (!memberRepository.existsByGroupCodeAndOrgCode(groupCode, orgCode)) {
            throw new IllegalArgumentException("该机构不在此范围组内");
        }
        memberRepository.deleteByGroupCodeAndOrgCode(groupCode, orgCode);
        log.info("从范围组 {} 移出机构：{}", groupCode, orgCode);
    }

    @Transactional
    public void setManagerOrg(String groupCode, String orgCode) {
        OrgGroupMember member = memberRepository.findByGroupCodeAndOrgCode(groupCode, orgCode)
                .orElseThrow(() -> new IllegalArgumentException("该机构不在此范围组内"));
        member.setIsManagerOrg(true);
        memberRepository.save(member);
        log.info("设为管辖机构：{}/{}", groupCode, orgCode);
    }

    @Transactional
    public void unsetManagerOrg(String groupCode, String orgCode) {
        OrgGroupMember member = memberRepository.findByGroupCodeAndOrgCode(groupCode, orgCode)
                .orElseThrow(() -> new IllegalArgumentException("该机构不在此范围组内"));
        // 检查是否至少还有一个管辖机构
        long managerCount = memberRepository.findByGroupCode(groupCode).stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsManagerOrg()) && !m.getOrgCode().equals(orgCode))
                .count();
        if (managerCount == 0) {
            throw new IllegalArgumentException("范围组内至少需要保留一个管辖机构，无法取消");
        }
        member.setIsManagerOrg(false);
        memberRepository.save(member);
        log.info("取消管辖机构：{}/{}", groupCode, orgCode);
    }

    // ==================== 管理人员 CRUD ====================

    @Transactional
    public void addManager(String groupCode, String ehrNo, String userName) {
        if (!groupRepository.existsByGroupCode(groupCode)) {
            throw new IllegalArgumentException("范围组不存在：" + groupCode);
        }

        // 校验1：ehrNo 对应的人员是否存在（通过 GBase/HR 查询）
        Map<String, Object> lookup = lookupUser(ehrNo);
        if (!Boolean.TRUE.equals(lookup.get("found"))) {
            throw new IllegalArgumentException("未找到该EHR号对应的人员信息");
        }

        // 校验2：该人员的机构号是否在范围组内
        String personOrgCode = (String) lookup.get("orgCode");
        if (personOrgCode != null && !personOrgCode.isEmpty()
                && !memberRepository.existsByGroupCodeAndOrgCode(groupCode, personOrgCode)) {
            String groupName = groupRepository.findByGroupCode(groupCode)
                    .map(OrgGroup::getGroupName).orElse(groupCode);
            throw new IllegalArgumentException(
                    "该人员所属机构（" + personOrgCode + "）不在「" + groupName + "」范围组内，" +
                    "请先将该机构添加至范围组");
        }

        // 校验3：是否已在组内
        if (managerRepository.existsByGroupCodeAndEhrNo(groupCode, ehrNo)) {
            throw new IllegalArgumentException("该人员已是本组管理人员，无需重复添加");
        }

        OrgGroupManager manager = new OrgGroupManager();
        manager.setGroupCode(groupCode);
        manager.setEhrNo(ehrNo);
        manager.setUserName(userName != null ? userName : (String) lookup.getOrDefault("userName", ""));
        managerRepository.save(manager);
        log.info("添加管理人员至范围组：{}/{} {}", groupCode, ehrNo, userName);
    }

    @Transactional
    public void removeManager(String groupCode, String ehrNo) {
        if (!managerRepository.existsByGroupCodeAndEhrNo(groupCode, ehrNo)) {
            throw new IllegalArgumentException("该人员不是本组管理人员");
        }
        managerRepository.deleteByGroupCodeAndEhrNo(groupCode, ehrNo);
        log.info("从范围组 {} 移除管理人员：{}", groupCode, ehrNo);
    }

    // ==================== 查询辅助 ====================

    /**
     * 根据机构号查询 GBase 中的机构名称（复用现有逻辑）
     */
    public Map<String, Object> lookupOrgInGbase(String orgCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("found", false);
        result.put("orgName", null);
        if (orgCode == null || orgCode.trim().isEmpty()) {
            return result;
        }
        String code = orgCode.trim();

        // 1. 本地分支行 branch_org
        List<BranchOrg> branches = branchOrgRepository.findByBranchCode(code);
        if (!branches.isEmpty() && branches.get(0).getBranchName() != null) {
            result.put("found", true);
            result.put("orgName", branches.get(0).getBranchName());
            return result;
        }

        // 2. 本地管辖行 jurisdiction_org
        Optional<JurisdictionOrg> jurisdiction = jurisdictionOrgRepository.findByOrgCode(code);
        if (jurisdiction.isPresent() && jurisdiction.get().getOrgName() != null) {
            result.put("found", true);
            result.put("orgName", jurisdiction.get().getOrgName());
            return result;
        }

        // 3. 本地用户机构 user_org（HR 同步）
        List<UserOrg> userOrgs = userOrgRepository.findByOrgCode(code);
        if (!userOrgs.isEmpty() && userOrgs.get(0).getOrgName() != null) {
            result.put("found", true);
            result.put("orgName", userOrgs.get(0).getOrgName());
            return result;
        }

        // 4. 降级联机 GBase（本地库尚未覆盖该机构时兜底）
        try {
            String sql = "SELECT ORG_NM FROM rcrms.R_V_O_ORG_BASIC WHERE ORG_ID = ? LIMIT 1";
            List<String> names = gbaseJdbcTemplate.queryForList(sql, String.class, code);
            if (!names.isEmpty() && names.get(0) != null) {
                result.put("found", true);
                result.put("orgName", names.get(0));
            }
        } catch (Exception e) {
            log.warn("GBase机构查询失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 根据 ehrNo 查询人员姓名和所属机构号
     * 优先查本地 user_org 表，miss 时降级查 GBase
     */
    public Map<String, Object> lookupUser(String ehrNo) {
        Map<String, Object> result = new HashMap<>();
        result.put("found", false);
        result.put("userName", null);
        result.put("orgCode", null);

        Optional<UserOrg> localUser = userOrgRepository.findByEhrNo(ehrNo.trim());
        if (localUser.isPresent()) {
            UserOrg u = localUser.get();
            result.put("found", true);
            result.put("userName", u.getUserName());
            result.put("orgCode", u.getOrgCode());
            return result;
        }

        try {
            String sql = "SELECT A.NAME, A.ACT_EMP_ORG_REFNO " +
                         "FROM GDM.G_V_O_C_HRM_TBL_EMPLOYEE_INFO_U A " +
                         "WHERE A.EMPE_REFNO = ? " +
                         "AND A.DATE_ID = (SELECT MAX(DATE_ID) FROM GDM.G_V_O_C_HRM_TBL_EMPLOYEE_INFO_U) " +
                         "LIMIT 1";
            List<Map<String, Object>> rows = gbaseJdbcTemplate.queryForList(sql, ehrNo.trim());
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                result.put("found", true);
                result.put("userName", row.getOrDefault("NAME", ""));
                result.put("orgCode", row.getOrDefault("ACT_EMP_ORG_REFNO", ""));
            }
        } catch (Exception e) {
            log.warn("GBase人员查询失败: {}", e.getMessage());
        }
        return result;
    }

    // ==================== 内部辅助 ====================

    private Map<String, Object> groupToMap(OrgGroup group) {
        Map<String, Object> map = new HashMap<>();
        map.put("groupCode", group.getGroupCode());
        map.put("groupName", group.getGroupName());

        // 组成员
        List<OrgGroupMember> members = memberRepository.findByGroupCode(group.getGroupCode());
        List<Map<String, Object>> memberList = new ArrayList<>();
        for (OrgGroupMember m : members) {
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("orgCode", m.getOrgCode());
            memberMap.put("orgName", m.getOrgName());
            memberMap.put("isManagerOrg", Boolean.TRUE.equals(m.getIsManagerOrg()));
            memberList.add(memberMap);
        }
        map.put("members", memberList);

        // 管理人员
        List<OrgGroupManager> managers = managerRepository.findByGroupCode(group.getGroupCode());
        List<Map<String, Object>> managerList = new ArrayList<>();
        for (OrgGroupManager m : managers) {
            Map<String, Object> managerMap = new HashMap<>();
            managerMap.put("ehrNo", m.getEhrNo());
            managerMap.put("userName", m.getUserName());
            managerList.add(managerMap);
        }
        map.put("managers", managerList);

        return map;
    }
}
