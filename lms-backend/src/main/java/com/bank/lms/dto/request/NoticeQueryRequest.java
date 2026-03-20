package com.bank.lms.dto.request;

import lombok.Data;

/**
 * 通知查询请求
 */
@Data
public class NoticeQueryRequest {
    private PageRequest page;
    private Integer readStatus; // 0-未读 1-已读
}
