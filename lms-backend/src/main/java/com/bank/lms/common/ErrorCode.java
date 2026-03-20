package com.bank.lms.common;

/**
 * 错误码枚举
 */
public enum ErrorCode {
    SUCCESS("0", "成功"),
    PARAM_ERROR("1001", "参数错误"),
    UNAUTHORIZED("1002", "权限不足"),
    NOT_LOGIN("1003", "未登录"),
    SYSTEM_ERROR("1004", "系统异常"),
    BUSINESS_ERROR("1005", "业务异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}