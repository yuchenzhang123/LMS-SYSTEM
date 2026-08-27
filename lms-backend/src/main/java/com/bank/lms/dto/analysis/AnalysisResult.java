package com.bank.lms.dto.analysis;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 分析结果
 */
@Data
public class AnalysisResult {
    private String capabilityName;
    private List<Map<String, Object>> rows;
    private int rowCount;
    /** 列名（前端表头，NL2SQL 路径会填充，预编译指标路径为 null） */
    private List<String> columns;

    public AnalysisResult(String capabilityName, List<Map<String, Object>> rows, int rowCount) {
        this(capabilityName, rows, rowCount, null);
    }

    public AnalysisResult(String capabilityName, List<Map<String, Object>> rows, int rowCount, List<String> columns) {
        this.capabilityName = capabilityName;
        this.rows = rows;
        this.rowCount = rowCount;
        this.columns = columns;
    }
}
