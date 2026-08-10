package com.bank.lms.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 分析结果
 */
@Data
@AllArgsConstructor
public class AnalysisResult {
    private String capabilityName;
    private List<Map<String, Object>> rows;
    private int rowCount;
}
