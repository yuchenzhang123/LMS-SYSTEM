package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.dto.request.NoticeMarkReadRequest;
import com.bank.lms.dto.request.NoticeQueryRequest;
import com.bank.lms.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 获取通知列表
     */
    @PostMapping("/list")
    public Result<Map<String, Object>> getNoticeList(@RequestBody @Valid NoticeQueryRequest request) {
        log.info("查询通知列表: {}", request);
        return Result.success(noticeService.getNoticeList(request));
    }

    /**
     * 标记通知已读
     */
    @PostMapping("/mark-read")
    public Result<?> markNoticeRead(@RequestBody @Valid NoticeMarkReadRequest request) {
        log.info("标记通知已读: {}", request);
        noticeService.markAsRead(request);
        return Result.success();
    }
}
