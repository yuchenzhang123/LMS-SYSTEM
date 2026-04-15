package com.bank.lms.dto.request;

import lombok.Data;

/**
 * 通知查询请求
 */
@Data
public class NoticeQueryRequest {
    private PageRequest page;
    private Integer readStatus; // 0-未读 1-已读
    private String branchCode;  // 业务员传自己的机构号，管理员/管辖行不传
}
