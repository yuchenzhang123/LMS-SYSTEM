package com.bank.lms.unit;

import com.bank.lms.service.analysis.AnalysisCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 单元测试 — 分析能力枚举
 */
@DisplayName("分析能力枚举 (单元测试)")
class AnalysisCapabilityTest {

    @Test @DisplayName("包含全部13种能力")
    void all13CapabilitiesDefined() {
        assertThat(AnalysisCapability.values()).hasSize(13);
    }

    @Test @DisplayName("按名称查找（大小写不敏感）")
    void findByName() {
        assertThat(AnalysisCapability.findByName("ORG_RANKING"))
            .isEqualTo(AnalysisCapability.ORG_RANKING);
        assertThat(AnalysisCapability.findByName("overdue_trend"))
            .isEqualTo(AnalysisCapability.OVERDUE_TREND);
        assertThat(AnalysisCapability.findByName("NOT_EXIST")).isNull();
    }

    @Test @DisplayName("每个能力都有 displayName 和 description")
    void eachHasMetadata() {
        assertThat(AnalysisCapability.values())
            .allMatch(c -> c.getDisplayName() != null && !c.getDisplayName().isEmpty())
            .allMatch(c -> c.getDescription() != null && !c.getDescription().isEmpty());
    }

    @Test @DisplayName("LLM意图描述包含所有能力名")
    void llmDescriptionContainsAllNames() {
        String desc = AnalysisCapability.buildCapabilityDescriptionForLlm();
        assertThat(desc).isNotNull();
        for (AnalysisCapability cap : AnalysisCapability.values()) {
            assertThat(desc).contains(cap.name());
        }
    }
}
