package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.service.analysis.CopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 助手控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService copilotService;

    /**
     * AI 问答
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.error("400", "问题不能为空");
        }
        Map<String, Object> answer = copilotService.ask(question.trim());
        return Result.success(answer);
    }

    /**
     * 每日简报
     *
     * @param force 可选，true 时强制重新生成（前端「刷新」按钮），跳过缓存；默认 false 命中缓存
     */
    @PostMapping("/briefing")
    public Result<Map<String, Object>> briefing(@RequestParam(required = false) Boolean force) {
        Map<String, Object> briefing = copilotService.dailyBriefing(force != null && force);
        return Result.success(briefing);
    }

    /**
     * 催收历程摘要
     */
    @PostMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestBody Map<String, String> body) {
        String loanAccount = body.get("loanAccount");
        if (loanAccount == null || loanAccount.trim().isEmpty()) {
            return Result.error("400", "loanAccount 不能为空");
        }
        Map<String, Object> summary = copilotService.collectionSummary(loanAccount.trim());
        return Result.success(summary);
    }
}
