package com.bank.lms.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 催收记录材料更新请求
 */
@Data
public class CollectionRecordUpdateMaterialRequest {
    @NotBlank(message = "记录ID不能为空")
    private String recordId;

    private String materialType;

    private String materialName;

    private String materialUrl;
}
