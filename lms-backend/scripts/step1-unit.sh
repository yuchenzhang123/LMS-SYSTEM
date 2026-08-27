#!/bin/bash
# ============================================
# 第1步：纯单元测试（无需数据库、无需Spring容器）
# ============================================
source "$(dirname "$0")/common.sh"

echo "========================================"
echo "第1步：纯单元测试"
echo "  - AI上下文 ThreadLocal 隔离"
echo "  - JSON解析 / SQL安全守卫"
echo "  - 知识库：切块 / 文档解析 / 向量召回 / CRUD编排"
echo "  - Embedding：装配与响应解析"
echo "========================================"

run_tests "" \
    --select-class com.bank.lms.unit.AiQueryContextTest \
    --select-class com.bank.lms.unit.JsonExtractorTest \
    --select-class com.bank.lms.unit.SqlSafetyGuardTest \
    --select-class com.bank.lms.service.knowledge.TextChunkerTest \
    --select-class com.bank.lms.service.knowledge.DocumentParserTest \
    --select-class com.bank.lms.service.knowledge.KnowledgeVectorStoreTest \
    --select-class com.bank.lms.service.knowledge.KnowledgeBaseServiceTest \
    --select-class com.bank.lms.config.EmbeddingConfigTest \
    --select-class com.bank.lms.service.embedding.OpenAiEmbeddingClientTest

echo "========================================"
echo "第1步完成"
echo "========================================"
