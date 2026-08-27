package com.bank.lms.common;

/**
 * 测试常量 — 统一管理测试中所有固定值
 */
public final class TestConstants {

    private TestConstants() {}

    // ==================== 接口地址 ====================

    public static final String API_AI_CHAT     = "/ai/chat";
    public static final String API_AI_BRIEFING = "/ai/briefing";
    public static final String API_AI_SUMMARY  = "/ai/summary";
    public static final String API_USER_LOGIN  = "/user/login-log";
    public static final String API_USER_STATS  = "/user/stats";
    public static final String API_USER_LIST   = "/user/list";
    public static final String API_ORG_ROLE    = "/org/role";
    public static final String API_ORG_GROUP   = "/org/group";

    // ==================== 错误码 ====================

    public static final String CODE_SUCCESS      = "0";
    public static final String CODE_PARAM_ERROR  = "400";
    public static final String CODE_UNAUTHORIZED = "1002";
    public static final String CODE_SYSTEM_ERROR = "1004";

    // ==================== 测试用户 ====================

    public static final String TEST_EHR_NO      = "test001";
    public static final String TEST_USER_NAME   = "测试用户";
    public static final String TEST_ORG_CODE    = "TEST_ORG";
    public static final String TEST_BRANCH_CODE = "TEST_BRANCH";
    public static final String TEST_GROUP_CODE  = "GRP_TEST";

    // ==================== 测试账户 ====================

    public static final String TEST_LOAN_ACCOUNT = "AI_TEST_001";
    public static final String TEST_CUSTOMER_ID  = "CUST_AI_001";

    // ==================== 逾期测试数据 ====================

    public static final int    OVERDUE_EXTREME   = 999;
    public static final int    OVERDUE_HIGH      = 120;
    public static final int    OVERDUE_MEDIUM    = 30;
    public static final int    OVERDUE_LOW       = 3;
    public static final int    OVERDUE_ZERO      = 0;
    public static final double AMOUNT_HUGE       = 100_000_000;
    public static final double AMOUNT_LARGE      = 1_000_000;
    public static final double AMOUNT_MEDIUM     = 100_000;
    public static final double AMOUNT_SMALL      = 5_000;
    public static final double AMOUNT_ZERO       = 0;

    // ==================== 角色 ====================

    public static final String ROLE_ADMIN   = "admin";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_STAFF   = "staff";
    public static final String ROLE_UNKNOWN = "unknown";

    // ==================== 催收状态 ====================

    public static final String STATUS_UNCOLLECTED = "uncollected";
    public static final String STATUS_COLLECTING  = "collecting";
    public static final String STATUS_COMPLETED   = "completed";

    // ==================== 分页 ====================

    public static final int PAGE_DEFAULT = 1;
    public static final int SIZE_DEFAULT = 10;
}
