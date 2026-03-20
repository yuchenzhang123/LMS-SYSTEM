package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.dto.request.*;
import com.bank.lms.service.CollectionRecordService;
import com.bank.lms.service.LitigationService;
import com.bank.lms.service.LoanAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 催收业务控制器
 */
@Slf4j
@RestController
@RequestMapping("/collection")
@Api(tags = "催收管理")
@RequiredArgsConstructor
public class CollectionController {

    private final LoanAccountService loanAccountService;
    private final CollectionRecordService collectionRecordService;
    private final LitigationService litigationService;

    /**
     * 获取账户列表
     */
    @PostMapping("/account/list")
    @ApiOperation("获取催收账户列表")
    public Result<Map<String, Object>> getAccountList(@RequestBody @Valid AccountQueryRequest request) {
        log.info("查询催收账户列表: {}", request);
        return Result.success(loanAccountService.getAccountList(request));
    }

    /**
     * 获取账户详情
     */
    @GetMapping("/account/detail/{loanAccount}")
    @ApiOperation("获取账户详情")
    public Result<?> getAccountDetail(@PathVariable String loanAccount) {
        log.info("获取账户详情: loanAccount={}", loanAccount);
        return Result.success(loanAccountService.getAccountDetail(loanAccount));
    }

    /**
     * 获取催收记录列表
     */
    @GetMapping("/record/list/{loanAccount}")
    @ApiOperation("获取催收记录列表")
    public Result<List<Map<String, Object>>> getCollectionRecordList(@PathVariable String loanAccount) {
        return Result.success(collectionRecordService.getRecordList(loanAccount));
    }

    /**
     * 新增催收记录
     */
    @PostMapping("/record/add")
    @ApiOperation("新增催收记录")
    public Result<Map<String, Object>> addCollectionRecord(@RequestBody @Valid CollectionRecordAddRequest request) {
        log.info("新增催收记录: {}", request);
        return Result.success(collectionRecordService.addRecord(request));
    }

    /**
     * 获取诉讼信息列表
     */
    @GetMapping("/litigation/list/{loanAccount}")
    @ApiOperation("获取诉讼信息列表")
    public Result<List<Map<String, Object>>> getLitigationList(@PathVariable String loanAccount) {
        return Result.success(litigationService.getLitigationList(loanAccount));
    }

    /**
     * 获取诉讼详情
     */
    @GetMapping("/litigation/detail/{litigationId}")
    @ApiOperation("获取诉讼详情")
    public Result<Map<String, Object>> getLitigationDetail(@PathVariable String litigationId) {
        return Result.success(litigationService.getLitigationDetail(litigationId));
    }

    /**
     * 更新诉讼进度
     */
    @PostMapping("/litigation/update")
    @ApiOperation("更新诉讼进度")
    public Result<Map<String, Object>> updateLitigationInfo(@RequestBody @Valid LitigationUpdateRequest request) {
        log.info("更新诉讼进度: {}", request);
        return Result.success(litigationService.updateLitigation(request));
    }

    /**
     * 发送短信催收
     */
    @PostMapping("/sms/send")
    @ApiOperation("发送催收短信")
    public Result<?> sendSms(@RequestBody @Valid SmsSendRequest request) {
        log.info("发送催收短信: {}", request);
        // 构建催收记录
        CollectionRecordAddRequest recordRequest = new CollectionRecordAddRequest();
        recordRequest.setLoanAccount(request.getLoanAccount());
        recordRequest.setCustomerId(request.getCustomerId());
        recordRequest.setMethod("sms");
        recordRequest.setMethodText("短信");
        recordRequest.setResult("短信催收已执行");
        recordRequest.setRemark(request.getContent());
        // 设置操作员信息
        recordRequest.setOperatorId(request.getOperatorId());
        recordRequest.setOperatorName(request.getOperatorName());
        // 保存催收记录到数据库
        collectionRecordService.addRecord(recordRequest);
        return Result.success();
    }

    /**
     * 导出催收材料
     */
    @GetMapping("/material/export/{materialId}")
    @ApiOperation("导出催收材料")
    public void exportMaterial(@PathVariable String materialId) {
        log.info("导出催收材料: materialId={}", materialId);
        throw new UnsupportedOperationException("文件导出功能待实现");
    }

    /**
     * 更新催收记录材料（补交/重交）
     */
    @PostMapping("/record/update-material")
    @ApiOperation("更新催收记录材料")
    public Result<?> updateMaterial(@RequestBody @Valid CollectionRecordUpdateMaterialRequest request) {
        log.info("更新催收记录材料: {}", request);
        return Result.success(collectionRecordService.updateMaterial(
                request.getRecordId(),
                request.getMaterialType(),
                request.getMaterialName(),
                request.getMaterialUrl()
        ));
    }
}
