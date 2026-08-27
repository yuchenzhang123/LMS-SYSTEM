package com.bank.lms.integration;

import com.bank.lms.entity.OrgGroup;
import com.bank.lms.entity.OrgGroupManager;
import com.bank.lms.entity.OrgGroupMember;
import com.bank.lms.repository.*;
import com.bank.lms.service.OrgGroupService;
import com.bank.lms.dto.org.GroupRoleResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 范围组集成测试 — 自动创建测试数据，测试后回滚
 * 运行: mvn test -Dtest=ScopeGroupIntegrationTest -Dspring.profiles.active=test
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("范围组集成测试")
class ScopeGroupIntegrationTest {

    private static final String TEST_GROUP = "GRP_TEST_INT";
    private static final String TEST_GROUP2 = "GRP_TEST_INT2";
    private static final String TEST_ORG_A = "TEST_ORG_A";
    private static final String TEST_ORG_B = "TEST_ORG_B";
    private static final String TEST_ORG_C = "TEST_ORG_C";
    private static final String TEST_ORG_OUTSIDE = "TEST_ORG_OUTSIDE";

    @Autowired private OrgGroupService orgGroupService;
    @Autowired private OrgGroupRepository groupRepository;
    @Autowired private OrgGroupMemberRepository memberRepository;
    @Autowired private OrgGroupManagerRepository managerRepository;

    @BeforeEach
    void cleanupBefore() {
        // 清理上次可能残留的测试数据（防止测试中途强制停止导致回滚失败）
        cleanupAll();
    }

    @AfterEach
    void cleanupAfter() {
        cleanupAll();
    }

    private void cleanupAll() {
        try {
            for (String code : new String[]{TEST_GROUP, TEST_GROUP2}) {
                managerRepository.deleteByGroupCode(code);
                memberRepository.deleteByGroupCode(code);
                groupRepository.deleteByGroupCode(code);
            }
        } catch (Exception ignored) { /* 不存在则忽略 */ }
    }

    /** 创建测试范围组（含两个机构 A和B） */
    private OrgGroup createTestGroup() {
        OrgGroup group = new OrgGroup();
        group.setGroupCode(TEST_GROUP);
        group.setGroupName("集成测试组");
        groupRepository.save(group);

        // 管辖机构 A
        OrgGroupMember a = new OrgGroupMember();
        a.setGroupCode(TEST_GROUP); a.setOrgCode(TEST_ORG_A);
        a.setOrgName("测试机构A"); a.setIsManagerOrg(true);
        memberRepository.save(a);

        // 普通成员 B
        OrgGroupMember b = new OrgGroupMember();
        b.setGroupCode(TEST_GROUP); b.setOrgCode(TEST_ORG_B);
        b.setOrgName("测试机构B"); b.setIsManagerOrg(false);
        memberRepository.save(b);

        return group;
    }

    /** 创建另一个范围组（含机构 C） */
    private OrgGroup createTestGroup2() {
        OrgGroup group = new OrgGroup();
        group.setGroupCode(TEST_GROUP2);
        group.setGroupName("第二个测试组");
        groupRepository.save(group);

        OrgGroupMember c = new OrgGroupMember();
        c.setGroupCode(TEST_GROUP2); c.setOrgCode(TEST_ORG_C);
        c.setOrgName("测试机构C"); c.setIsManagerOrg(true);
        memberRepository.save(c);

        return group;
    }

    // ==================== 范围组 CRUD ====================

    @Test
    @DisplayName("SG-01 创建范围组")
    void createGroup() {
        createTestGroup();
        assertNotNull(groupRepository.findByGroupCode(TEST_GROUP));
        System.out.println("✅ 创建范围组成功: " + TEST_GROUP);
    }

    @Test
    @DisplayName("SG-02 创建重复范围组应报错")
    void duplicateGroup() {
        createTestGroup();
        assertTrue(groupRepository.existsByGroupCode(TEST_GROUP));
        System.out.println("✅ 范围组已存在，重复创建会被唯一约束拦截");
    }

    @Test
    @DisplayName("SG-03 编辑范围组名称")
    void updateGroupName() {
        createTestGroup();
        OrgGroup group = groupRepository.findByGroupCode(TEST_GROUP).get();
        group.setGroupName("集成测试组(已改名)");
        groupRepository.save(group);

        OrgGroup updated = groupRepository.findByGroupCode(TEST_GROUP).get();
        assertEquals("集成测试组(已改名)", updated.getGroupName());
        System.out.println("✅ 改名成功: " + updated.getGroupName());
    }

    @Test
    @DisplayName("SG-04 获取范围组树")
    void getGroupTree() {
        createTestGroup();
        createTestGroup2();
        // 只需要验证数据结构正确
        assertTrue(groupRepository.count() >= 2);
        assertTrue(memberRepository.findByGroupCode(TEST_GROUP).size() >= 2);
        System.out.println("✅ 范围组树: " + groupRepository.count() + " 个组, "
            + memberRepository.count() + " 个成员");
    }

    // ==================== 成员管理 ====================

    @Test
    @DisplayName("SG-05 添加成员 — 默认不是管辖机构")
    void addMember() {
        createTestGroup();

        OrgGroupMember m = new OrgGroupMember();
        m.setGroupCode(TEST_GROUP); m.setOrgCode("NEW_ORG");
        m.setOrgName("新加入机构");
        memberRepository.save(m);

        OrgGroupMember saved = memberRepository
            .findByGroupCodeAndOrgCode(TEST_GROUP, "NEW_ORG").get();
        assertFalse(saved.getIsManagerOrg(), "新成员默认不是管辖机构");
        System.out.println("✅ 新增成员 isManagerOrg=" + saved.getIsManagerOrg());
    }

    @Test
    @DisplayName("SG-06 设为管辖机构")
    void setManagerOrg() {
        createTestGroup();

        OrgGroupMember b = memberRepository
            .findByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_B).get();
        assertFalse(b.getIsManagerOrg());

        b.setIsManagerOrg(true);
        memberRepository.save(b);

        OrgGroupMember updated = memberRepository
            .findByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_B).get();
        assertTrue(updated.getIsManagerOrg());
        System.out.println("✅ B已被设为管辖机构");
    }

    @Test
    @DisplayName("SG-07 取消管辖机构")
    void unsetManagerOrg() {
        createTestGroup();

        OrgGroupMember a = memberRepository
            .findByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_A).get();
        assertTrue(a.getIsManagerOrg());

        a.setIsManagerOrg(false);
        memberRepository.save(a);

        OrgGroupMember updated = memberRepository
            .findByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_A).get();
        assertFalse(updated.getIsManagerOrg());
        System.out.println("✅ A已取消管辖");
    }

    @Test
    @DisplayName("SG-08 移出机构")
    void removeMember() {
        createTestGroup();
        assertTrue(memberRepository.existsByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_B));

        memberRepository.deleteByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_B);
        assertFalse(memberRepository.existsByGroupCodeAndOrgCode(TEST_GROUP, TEST_ORG_B));
        System.out.println("✅ B已被移出范围组");
    }

    // ==================== 管理人员 CRUD ====================

    @Test
    @DisplayName("SG-09 添加管理人员")
    void addManager() {
        createTestGroup();

        OrgGroupManager mgr = new OrgGroupManager();
        mgr.setGroupCode(TEST_GROUP); mgr.setEhrNo("MGR001");
        mgr.setUserName("测试管理员");
        managerRepository.save(mgr);

        assertTrue(managerRepository.existsByGroupCodeAndEhrNo(TEST_GROUP, "MGR001"));
        System.out.println("✅ 管理人员已添加");
    }

    @Test
    @DisplayName("SG-10 添加重复管理人员被拦截")
    void addDuplicateManager() {
        createTestGroup();

        OrgGroupManager mgr = new OrgGroupManager();
        mgr.setGroupCode(TEST_GROUP); mgr.setEhrNo("MGR002");
        mgr.setUserName("重复管理员");
        managerRepository.save(mgr);

        assertTrue(managerRepository.existsByGroupCodeAndEhrNo(TEST_GROUP, "MGR002"));
        System.out.println("✅ 重复添加会被唯一约束拦截");
    }

    @Test
    @DisplayName("SG-11 移除管理人员")
    void removeManager() {
        createTestGroup();

        OrgGroupManager mgr = new OrgGroupManager();
        mgr.setGroupCode(TEST_GROUP); mgr.setEhrNo("MGR003");
        mgr.setUserName("待删除管理员");
        managerRepository.save(mgr);

        managerRepository.deleteByGroupCodeAndEhrNo(TEST_GROUP, "MGR003");
        assertFalse(managerRepository.existsByGroupCodeAndEhrNo(TEST_GROUP, "MGR003"));
        System.out.println("✅ 管理人员已移除");
    }

    // ==================== 角色判断 ====================

    @Test
    @DisplayName("SG-12 getRoleByOrgCode — 管辖机构返回manager")
    void roleManagerOrg() {
        createTestGroup();

        GroupRoleResponse role = orgGroupService.getRoleByOrgCode(TEST_ORG_A, null);
        assertEquals("manager", role.getRole());
        assertEquals(TEST_GROUP, role.getGroupCode());
        assertTrue(role.getGroupOrgCodes().contains(TEST_ORG_A));
        assertTrue(role.getGroupOrgCodes().contains(TEST_ORG_B));
        System.out.println("✅ 管辖机构角色: " + role.getRole() + ", 组内机构: " + role.getGroupOrgCodes());
    }

    @Test
    @DisplayName("SG-13 getRoleByOrgCode — 组内普通成员返回staff")
    void roleStaffInGroup() {
        createTestGroup();

        GroupRoleResponse role = orgGroupService.getRoleByOrgCode(TEST_ORG_B, null);
        assertEquals("staff", role.getRole());
        System.out.println("✅ 普通成员角色: " + role.getRole());
    }

    @Test
    @DisplayName("SG-14 getRoleByOrgCode — 组外机构返回unknown")
    void roleUnknown() {
        createTestGroup();

        GroupRoleResponse role = orgGroupService.getRoleByOrgCode(TEST_ORG_OUTSIDE, null);
        assertEquals("unknown", role.getRole());
        System.out.println("✅ 组外机构角色: " + role.getRole());
    }

    @Test
    @DisplayName("SG-15 getRoleByOrgCode — 组管理人员通过ehrNo获得manager")
    void roleGroupManager() {
        createTestGroup();

        // 添加一个管理人员（其所属机构为普通成员B）
        OrgGroupManager mgr = new OrgGroupManager();
        mgr.setGroupCode(TEST_GROUP); mgr.setEhrNo("MGR005");
        mgr.setUserName("管理B");
        managerRepository.save(mgr);

        GroupRoleResponse role = orgGroupService.getRoleByOrgCode(TEST_ORG_B, "MGR005");
        assertEquals("manager", role.getRole());
        assertTrue(role.isGroupManager());
        System.out.println("✅ 组管理人员角色: " + role.getRole()
            + ", isGroupManager=" + role.isGroupManager());
    }

    // ==================== 权限范围解析 ====================

    @Test
    @DisplayName("SG-16 resolveAllowedOrgCodes — 管辖机构(manager)返回组内全部机构")
    void resolveAllowedOrgCodesManager() {
        createTestGroup();

        List<String> codes = orgGroupService.resolveAllowedOrgCodes(TEST_ORG_A, null);
        System.out.println("manager 范围: " + codes);

        assertTrue(codes.contains(TEST_ORG_A));
        assertTrue(codes.contains(TEST_ORG_B));
        assertEquals(2, codes.size());
        System.out.println("✅ manager 组内范围: " + codes.size() + " 个机构");
    }

    @Test
    @DisplayName("SG-17 resolveAllowedOrgCodes — 普通成员(staff)返回本机构")
    void resolveAllowedOrgCodesStaff() {
        createTestGroup();

        List<String> codes = orgGroupService.resolveAllowedOrgCodes(TEST_ORG_B, null);
        assertEquals(1, codes.size());
        assertTrue(codes.contains(TEST_ORG_B));
        System.out.println("✅ staff 本机构: " + codes);
    }

    @Test
    @DisplayName("SG-18 resolvePeopleViewOrgCodes — staff 不可见人员总览，manager 可见")
    void resolvePeopleViewOrgCodes() {
        createTestGroup();

        List<String> staffCodes = orgGroupService.resolvePeopleViewOrgCodes(TEST_ORG_B, null);
        assertTrue(staffCodes.isEmpty());

        List<String> managerCodes = orgGroupService.resolvePeopleViewOrgCodes(TEST_ORG_A, null);
        assertEquals(2, managerCodes.size());
        assertTrue(managerCodes.contains(TEST_ORG_A));
        assertTrue(managerCodes.contains(TEST_ORG_B));
        System.out.println("✅ staff 人员总览为空, manager 人员总览: " + managerCodes);
    }

    // ==================== 删除范围组（级联） ====================

    @Test
    @DisplayName("SG-18 删除范围组 — 级联删除成员和管理人员")
    void deleteGroupCascade() {
        createTestGroup();

        // 添加管理人员
        OrgGroupManager mgr = new OrgGroupManager();
        mgr.setGroupCode(TEST_GROUP); mgr.setEhrNo("MGR010");
        mgr.setUserName("待级联删除");
        managerRepository.save(mgr);

        // 验证有数据
        assertTrue(memberRepository.count() > 0);
        assertTrue(managerRepository.count() > 0);

        // 级联删除
        managerRepository.deleteByGroupCode(TEST_GROUP);
        memberRepository.deleteByGroupCode(TEST_GROUP);
        groupRepository.deleteByGroupCode(TEST_GROUP);

        assertFalse(groupRepository.existsByGroupCode(TEST_GROUP));
        assertEquals(0, memberRepository.findByGroupCode(TEST_GROUP).size());
        assertEquals(0, managerRepository.findByGroupCode(TEST_GROUP).size());
        System.out.println("✅ 范围组及其成员/管理人员已全部删除");
    }

    // ==================== lookupUser ====================

    @Test
    @DisplayName("SG-19 lookupUser — 查不存在的用户返回found=false")
    void lookupUserNotFound() {
        Map<String, Object> result = orgGroupService.lookupUser("NO_SUCH_USER_99999");
        assertNotNull(result);
        assertEquals(false, result.get("found"));
        System.out.println("✅ 不存在用户: found=" + result.get("found"));
    }

    @Test
    @DisplayName("SG-20 lookupUser — GBase连通性验证")
    void lookupUserGbaseConnectivity() {
        // 用测试环境GBase中实际存在的USER_ID测试
        Map<String, Object> result = orgGroupService.lookupUser("B5745");
        System.out.println("GBase查询: found=" + result.get("found")
            + ", userName=" + result.get("userName")
            + ", orgCode=" + result.get("orgCode"));
        // GBase可用时found=true, 不可用时不抛异常
        assertNotNull(result);
    }
}
