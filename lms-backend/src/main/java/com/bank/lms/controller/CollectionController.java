package com.bank.lms.controller;

import com.bank.lms.common.Result;
import com.bank.lms.dto.request.*;
import com.bank.lms.service.AccountExportService;
import com.bank.lms.service.CollectionRecordService;
import com.bank.lms.service.FileService;
import com.bank.lms.service.LitigationService;
import com.bank.lms.service.LoanAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 催收业务控制器
 */
@Slf4j
@RestController
@RequestMapping("/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final LoanAccountService loanAccountService;
    private final CollectionRecordService collectionRecordService;
    private final LitigationService litigationService;
    private final FileService fileService;
    private final AccountExportService accountExportService;

    /**
     * 获取账户列表
     */
    @PostMapping("/account/list")
    public Result<Map<String, Object>> getAccountList(@RequestBody @Valid AccountQueryRequest request) {
        log.info("查询催收账户列表: {}", request);
        return Result.success(loanAccountService.getAccountList(request));
    }

    /**
     * 统计未完成催收的客户数和贷款余额合计
     */
    @GetMapping("/account/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String orgCode) {
        return Result.success(loanAccountService.getStats(branchCode, orgCode));
    }

    /**
     * 获取账户详情
     */
    @GetMapping("/account/detail/{loanAccount}")
    public Result<?> getAccountDetail(@PathVariable String loanAccount) {
        log.info("获取账户详情: loanAccount={}", loanAccount);
        return Result.success(loanAccountService.getAccountDetail(loanAccount));
    }

    /**
     * 获取催收记录列表
     */
    @GetMapping("/record/list/{loanAccount}")
    public Result<List<Map<String, Object>>> getCollectionRecordList(@PathVariable String loanAccount) {
        return Result.success(collectionRecordService.getRecordList(loanAccount));
    }

    /**
     * 新增催收记录
     */
    @PostMapping("/record/add")
    public Result<Map<String, Object>> addCollectionRecord(
            @RequestParam("loanAccount") String loanAccount,
            @RequestParam("customerId") String customerId,
            @RequestParam("targetType") String targetType,
            @RequestParam("targetName") String targetName,
            @RequestParam(value = "actualCollectionTime", required = false) String actualCollectionTime,
            @RequestParam("method") String method,
            @RequestParam("methodText") String methodText,
            @RequestParam("result") String result,
            @RequestParam(value = "operatorId", required = false) String operatorId,
            @RequestParam(value = "operatorName", required = false) String operatorName,
            @RequestParam(value = "time", required = false) String time,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "materialType", required = false) String materialType,
            @RequestParam(value = "materialName", required = false) String materialName,
            @RequestParam(value = "materialUrl", required = false) String materialUrl,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        log.info("新增催收记录: loanAccount={}, method={}", loanAccount, method);

        // 构建请求对象
        CollectionRecordAddRequest request = new CollectionRecordAddRequest();
        request.setLoanAccount(loanAccount);
        request.setCustomerId(customerId);
        request.setTargetType(targetType);
        request.setTargetName(targetName);
        request.setActualCollectionTime(actualCollectionTime);
        request.setMethod(method);
        request.setMethodText(methodText);
        request.setResult(result);
        request.setOperatorId(operatorId);
        request.setOperatorName(operatorName);
        request.setTime(time);
        request.setRemark(remark);
        request.setMaterialType(materialType);
        request.setMaterialName(materialName);
        request.setMaterialUrl(materialUrl);

        // 如果有文件，使用带文件上传的方法
        if (file != null && !file.isEmpty()) {
            return Result.success(collectionRecordService.addRecordWithFile(request, file));
        } else {
            return Result.success(collectionRecordService.addRecord(request));
        }
    }

    /**
     * 获取诉讼信息列表
     */
    @GetMapping("/litigation/list/{loanAccount}")
    public Result<List<Map<String, Object>>> getLitigationList(@PathVariable String loanAccount) {
        return Result.success(litigationService.getLitigationList(loanAccount));
    }

    /**
     * 获取诉讼详情
     */
    @GetMapping("/litigation/detail/{litigationId}")
    public Result<Map<String, Object>> getLitigationDetail(@PathVariable String litigationId) {
        return Result.success(litigationService.getLitigationDetail(litigationId));
    }

    /**
     * 更新诉讼进度
     */
    @PostMapping("/litigation/update")
    public Result<Map<String, Object>> updateLitigationInfo(@RequestBody @Valid LitigationUpdateRequest request) {
        log.info("更新诉讼进度: {}", request);
        return Result.success(litigationService.updateLitigation(request));
    }

    /**
     * 发送短信催收
     */
    @PostMapping("/sms/send")
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
     * 上传文件
     */
    @PostMapping("/material/upload")
    public Result<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("materialType") String materialType,
            @RequestParam(value = "materialName", required = false) String materialName) {
        log.info("上传文件: fileName={}, materialType={}, materialName={}", file.getOriginalFilename(), materialType, materialName);
        Map<String, String> result = fileService.uploadFile(file, materialType, materialName);
        return Result.success(result);
    }

    /**
     * 下载文件
     */
    @GetMapping("/material/download/{recordId}")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable String recordId) {
        log.info("下载催收材料: recordId={}", recordId);
        Map<String, Object> record = collectionRecordService.getRecordById(recordId);
        String materialUrl = (String) record.get("materialUrl");
        String materialName = (String) record.get("materialName");

        if (materialUrl == null || materialUrl.isEmpty()) {
            throw new RuntimeException("该催收记录没有上传材料");
        }

        File file = fileService.getFileByUrl(materialUrl);
        Resource resource = new FileSystemResource(file);

        String contentType = fileService.getContentType(materialUrl);
        String encodedFileName = "";
        try {
            encodedFileName = URLEncoder.encode(materialName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            log.warn("文件名编码失败: {}", materialName);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .body(resource);
    }

    /**
     * 更新催收记录材料（补交/重交）
     */
    @PostMapping("/record/update-material")
    public Result<?> updateMaterial(
            @RequestParam("recordId") String recordId,
            @RequestParam("materialType") String materialType,
            @RequestParam(value = "materialName", required = false) String materialName,
            @RequestParam("file") MultipartFile file) {
        log.info("更新催收材料: recordId={}, materialType={}, fileName={}",
                recordId, materialType, file.getOriginalFilename());

        // 上传文件
        Map<String, String> uploadResult = fileService.uploadFile(file, materialType, materialName);

        // 更新数据库
        return Result.success(collectionRecordService.updateMaterial(
                recordId,
                materialType,
                materialName,
                uploadResult.get("url")
        ));
    }

    /**
     * 导出账户列表（Excel），含诉讼和催收记录
     */
    @PostMapping("/account/export")
    public ResponseEntity<byte[]> exportAccounts(@RequestBody AccountExportService.ExportFilter filter) {
        String fileName = "催收账户导出_" + java.time.LocalDate.now() + ".xlsx";
        byte[] data = accountExportService.export(filter);
        String encodedFileName;
        try {
            encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            encodedFileName = "export.xlsx";
        }
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
