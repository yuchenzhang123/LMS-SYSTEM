package com.bank.lms.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class Result<T> implements Serializable {
    private String code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>("0", "成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>("0", "成功", data);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}