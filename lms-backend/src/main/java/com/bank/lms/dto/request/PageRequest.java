package com.bank.lms.dto.request;

import lombok.Data;

/**
 * 分页请求
 */
@Data
public class PageRequest {
    private Integer currentPage = 1;
    private Integer pageSize = 10;
}
