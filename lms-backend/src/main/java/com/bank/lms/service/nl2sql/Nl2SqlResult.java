package com.bank.lms.service.nl2sql;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * NL2SQL 最终结果（返回给前端）。
 */
@Data
public class Nl2SqlResult {
    private String intent;
    /** 润色后的自然语言回答 */
    private String answerText;
    /** 列名（前端表头，从结果集推断） */
    private List<String> columns;
    /** 结果行 */
    private List<Map<String, Object>> rows;
    private int rowCount;
    private boolean success;
    private String errorMsg;
}
