package com.bank.lms.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 通知标记已读请求
 */
@Data
public class NoticeMarkReadRequest {
    private List<String> noticeIds;
}
